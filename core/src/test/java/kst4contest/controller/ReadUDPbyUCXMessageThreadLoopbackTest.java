package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import kst4contest.model.Band;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatPreferences;
import kst4contest.observe.DirectUiDispatcher;
import kst4contest.observe.SimpleValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

/**
 * Loopback tests for the UCXLog/DXLog UDP listener. Every socket is bound to
 * port 0 and every datagram goes to the loopback address, so the tests neither
 * collide with a running logger nor send anything to a broadcast address.
 */
class ReadUDPbyUCXMessageThreadLoopbackTest {

	private final ChatController controller = mock(ChatController.class);
	private final ChatPreferences preferences = mock(ChatPreferences.class);
	private final ThreadStatusCallback statusCallback = (threadName, message) -> { };
	/** First-category QRG that the RadioInfo ordering marker sets. */
	private final SimpleValue<String> markerQrg = new SimpleValue<>("");

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

	/**
	 * A UCXLog QSO packet marks the station worked, registers the gross field
	 * and stores an unknown station in the database and the UDP backup file.
	 * Bytes before the XML declaration are transport framing and are dropped.
	 */
	@Test
	void contactInfoPacketMarksTheStationWorkedAndStoresIt(@TempDir Path tempDir) throws Exception {
		Path backupFile = tempDir.resolve("udp-backup.txt");
		DBController database = givenQsoProcessingEnabled(backupFile);
		when(database.updateWkdInfoOnChatMember(any())).thenReturn(false);

		sendThenAwaitMarker("\u0001\u0002UCX"
				+ "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
				+ "<contactinfo><app>UCXLog</app><mycall>DL0TEST</mycall><band>144</band>"
				+ "<rxfreq>14420000</rxfreq><txfreq>14420000</txfreq><mode>CW</mode>"
				+ "<call>OK1ABC</call><snt>599</snt><sntnr>001</sntnr><rcv>599</rcv>"
				+ "<rcvnr>001JO70FD</rcvnr><gridsquare>jo70fd</gridsquare><points>350</points>"
				+ "</contactinfo>");

		ArgumentCaptor<ExternalLoggedQso> qso = ArgumentCaptor.forClass(ExternalLoggedQso.class);
		verify(controller).applyExternalLoggedQso(qso.capture());
		assertEquals("OK1ABC", qso.getValue().getCallSign());
		assertEquals(LoggedQsoBand.BAND_144, qso.getValue().getBand());
		assertEquals("JO70FD", qso.getValue().getLocator());
		assertEquals("UCXLOG", qso.getValue().getSource());
		verify(controller).registerWorkedGrossField(
				eq(Band.B_144), eq("JO70FD"), any(ChatMember.class), eq("UCXLOG"));

		ArgumentCaptor<ChatMember> stored = ArgumentCaptor.forClass(ChatMember.class);
		verify(database).storeChatMember(stored.capture());
		assertEquals("OK1ABC", stored.getValue().getCallSign());
		assertEquals("unknown", stored.getValue().getName());
		assertEquals("JO70FD", stored.getValue().getQra());
		assertTrue(stored.getValue().isWorked());
		assertTrue(stored.getValue().isWorked144());

		String backup = Files.readString(backupFile, StandardCharsets.UTF_8);
		assertTrue(backup.startsWith("\nOK1ABC;unknown;JO70FD;"), backup);
	}

	/**
	 * DXLog sends contactreplace when it broadcasts the whole log. Without a
	 * gridsquare the locator comes from the received exchange, and a station
	 * already in the database is updated rather than stored again.
	 */
	@Test
	void contactReplaceTakesTheLocatorFromTheReceivedExchange(@TempDir Path tempDir) throws Exception {
		DBController database = givenQsoProcessingEnabled(tempDir.resolve("udp-backup.txt"));
		when(database.updateWkdInfoOnChatMember(any())).thenReturn(true);

		sendThenAwaitMarker("<?xml version=\"1.0\" encoding=\"utf-8\"?>"
				+ "<contactreplace><app>DXLog</app><band>432</band><call>SP2XYZ</call>"
				+ "<rcvnr>042JO93AB</rcvnr></contactreplace>");

		verify(controller).registerWorkedGrossField(
				eq(Band.B_432), eq("JO93AB"), any(ChatMember.class), eq("UCXLOG"));
		verify(database).updateWkdInfoOnChatMember(any());
		verify(database, never()).storeChatMember(any());
	}

	@Test
	void contactInfoIsIgnoredWhileLogSyncIsDisabled(@TempDir Path tempDir) throws Exception {
		DBController database = givenQsoProcessingEnabled(tempDir.resolve("udp-backup.txt"));
		when(preferences.isLogsynch_ucxUDPWkdCallListenerEnabled()).thenReturn(false);

		sendThenAwaitMarker("<contactinfo><band>144</band><call>OK1ABC</call></contactinfo>");

		verify(controller, never()).applyExternalLoggedQso(any());
		verify(database, never()).updateWkdInfoOnChatMember(any());
	}

	/**
	 * Wires the mocks for QSO processing. TRX sync is enabled as well, so a
	 * RadioInfo packet can serve as an ordering marker: the listener handles
	 * datagrams one by one, so once the marker's QRG is set, every packet sent
	 * before it has been processed completely.
	 */
	private DBController givenQsoProcessingEnabled(Path backupFile) {
		DBController database = mock(DBController.class);
		when(controller.getChatPreferences()).thenReturn(preferences);
		when(controller.uiDispatcher()).thenReturn(new DirectUiDispatcher());
		when(controller.getDbHandler()).thenReturn(database);
		when(preferences.isLogsynch_ucxUDPWkdCallListenerEnabled()).thenReturn(true);
		when(preferences.isTrxSynch_ucxLogUDPListenerEnabled()).thenReturn(true);
		when(preferences.getLogSynch_storeWorkedCallSignsFileNameUDPMessageBackup())
				.thenReturn(backupFile.toString());
		when(preferences.getMYQRGFirstCat()).thenReturn(markerQrg);
		return database;
	}

	private void sendThenAwaitMarker(String packet) throws Exception {
		CountDownLatch marker = new CountDownLatch(1);
		markerQrg.addListener(value -> {
			if (value.equals("1296.200.00")) marker.countDown();
		});

		InetAddress loopback = InetAddress.getLoopbackAddress();
		DatagramSocket bound = new DatagramSocket(new InetSocketAddress(loopback, 0));
		ReadUDPbyUCXMessageThread listener = new ReadUDPbyUCXMessageThread(bound, controller, statusCallback);
		listener.setDaemon(true);
		listener.start();
		try (DatagramSocket sender = new DatagramSocket(new InetSocketAddress(loopback, 0))) {
			InetSocketAddress target = new InetSocketAddress(loopback, bound.getLocalPort());
			send(sender, target, packet);
			send(sender, target, "<RadioInfo><Freq>129620000</Freq><Mode>CW</Mode></RadioInfo>");
			assertTrue(marker.await(5, TimeUnit.SECONDS), "marker packet must be processed");
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
