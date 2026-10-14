package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import kst4contest.model.ChatPreferences;
import kst4contest.observe.DirectUiDispatcher;
import kst4contest.observe.SimpleValue;
import org.junit.jupiter.api.Test;

/**
 * Loopback tests for the UCXLog/DXLog UDP listener. Every socket is bound to
 * port 0 and every datagram goes to the loopback address, so the tests neither
 * collide with a running logger nor send anything to a broadcast address.
 */
class ReadUDPbyUCXMessageThreadLoopbackTest {

	private final ChatController controller = mock(ChatController.class);
	private final ChatPreferences preferences = mock(ChatPreferences.class);
	private final ThreadStatusCallback statusCallback = (threadName, message) -> { };

	@Test
	void interruptEndsTheReceiveLoopBeforeTheReceiveTimeout() throws Exception {
		when(controller.getChatPreferences()).thenReturn(preferences);
		ReadUDPbyUCXMessageThread listener = new ReadUDPbyUCXMessageThread(0, controller, statusCallback);
		listener.setDaemon(true);
		listener.start();
		// Let the thread bind its socket and block in receive() before interrupting it.
		Thread.sleep(200);

		/*
		 * The receive timeout is 2 s. Waiting only 1 s proves that interrupt()
		 * closed the socket, rather than the loop ending on the next timeout.
		 */
		listener.interrupt();
		listener.join(1_000);

		assertFalse(listener.isAlive(), "listener must stop once interrupted");
	}

	/**
	 * A DatagramPacket remembers the length of the last datagram it received.
	 * Without resetting it before every receive(), a long RadioInfo packet that
	 * follows a short one is cut to the short one's length and fails to parse.
	 * The QRG probe needs no database: RadioInfo only sets the first-category
	 * frequency through the UI dispatcher.
	 */
	@Test
	void longDatagramAfterShortOneIsProcessedCompletely() throws Exception {
		when(controller.getChatPreferences()).thenReturn(preferences);
		when(controller.uiDispatcher()).thenReturn(new DirectUiDispatcher());
		when(preferences.isTrxSynch_ucxLogUDPListenerEnabled()).thenReturn(true);
		SimpleValue<String> qrg = new SimpleValue<>("");
		when(preferences.getMYQRGFirstCat()).thenReturn(qrg);
		CountDownLatch firstQrg = new CountDownLatch(1);
		CountDownLatch secondQrg = new CountDownLatch(1);
		qrg.addListener(value -> {
			if (value.equals("144.200.00")) firstQrg.countDown();
			if (value.equals("432.200.00")) secondQrg.countDown();
		});

		String shortPacket = "<RadioInfo><Freq>14420000</Freq><Mode>CW</Mode></RadioInfo>";
		String longPacket = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
				+ "<RadioInfo><StationName>CONTEST-PC-WITH-A-LONG-NAME</StationName>"
				+ "<RadioNr>1</RadioNr><Freq>43220000</Freq><TXFreq>43220000</TXFreq>"
				+ "<Mode>SSB</Mode><OpCall>DL0TEST</OpCall><IsRunning>False</IsRunning>"
				+ "<FocusEntry>0</FocusEntry><Antenna>1</Antenna><Rotors>1</Rotors>"
				+ "<FocusRadioNr>1</FocusRadioNr><IsStereo>False</IsStereo>"
				+ "<ActiveRadioNr>1</ActiveRadioNr></RadioInfo>";
		assertTrue(longPacket.length() > shortPacket.length());

		InetAddress loopback = InetAddress.getLoopbackAddress();
		DatagramSocket bound = new DatagramSocket(new InetSocketAddress(loopback, 0));
		ReadUDPbyUCXMessageThread listener = new ReadUDPbyUCXMessageThread(bound, controller, statusCallback);
		listener.setDaemon(true);
		listener.start();
		try (DatagramSocket sender = new DatagramSocket(new InetSocketAddress(loopback, 0))) {
			InetSocketAddress target = new InetSocketAddress(loopback, bound.getLocalPort());

			send(sender, target, shortPacket);
			assertTrue(firstQrg.await(5, TimeUnit.SECONDS), "short packet must be processed");
			assertEquals("144.200.00", qrg.get());

			send(sender, target, longPacket);
			assertTrue(secondQrg.await(5, TimeUnit.SECONDS), "long packet must be processed");
			assertEquals("432.200.00", qrg.get());
		} finally {
			listener.interrupt();
			listener.join(2_000);
		}
	}

	private static void send(DatagramSocket sender, InetSocketAddress target, String payload) throws Exception {
		byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
		sender.send(new DatagramPacket(bytes, bytes.length, target));
	}
}
