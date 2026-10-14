package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import kst4contest.controller.interfaces.PstRotatorEventListener;
import kst4contest.model.ThreadStateMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Loopback tests for the PSTRotator UDP client. A socket on an ephemeral
 * loopback port plays PSTRotator; the client is built with the explicit-port
 * constructor and local port 0, so it never binds the real PSTRotator ports.
 *
 * <p>The reply format used here ({@code AZ:145.0}, {@code EL:010.0},
 * {@code MODE:1}, each terminated by a carriage return) is derived from the
 * client's parser and from the PSTRotator UDP documentation, which describes
 * the answer as {@code AZ:xxx.x<CR>} on the command port plus one. It is not
 * taken from a capture of a running PSTRotator; none was available.</p>
 */
class PstRotatorClientLoopbackTest {

	private static final String POLL_AZIMUTH = "<PST>AZ?</PST>";
	private static final String POLL_MODE = "<PST>MODE?</PST>";

	private final List<ThreadStateMessage> statusMessages = new CopyOnWriteArrayList<>();
	private final RecordingListener listener = new RecordingListener();

	private DatagramSocket pstRotator;
	private PstRotatorClient client;

	@BeforeEach
	void startClientAgainstFakePstRotator() throws Exception {
		InetAddress loopback = InetAddress.getLoopbackAddress();
		pstRotator = new DatagramSocket(new InetSocketAddress(loopback, 0));
		pstRotator.setSoTimeout(5_000);
		client = new PstRotatorClient(loopback.getHostAddress(), pstRotator.getLocalPort(), 0, listener,
				(threadName, message) -> statusMessages.add(message));
		client.start();
	}

	/**
	 * The client does not expose its listener thread, so it is found by the
	 * name start() gives it, which contains the remote port of this test's
	 * fake PSTRotator.
	 */
	@AfterEach
	void stopClient() throws Exception {
		String listenerName = "PSTRotator-Listener-" + pstRotator.getLocalPort();
		List<Thread> listeners = Thread.getAllStackTraces().keySet().stream()
				.filter(thread -> thread.getName().equals(listenerName))
				.toList();
		try {
			client.stop();
			assertEquals(1, listeners.size(), "exactly one listener thread expected");
			for (Thread listenerThread : listeners) {
				listenerThread.join(2_000);
				assertFalse(listenerThread.isAlive(), "listener thread must end after stop()");
			}
		} finally {
			pstRotator.close();
		}
	}

	/**
	 * The explicit-port constructor once left the status callback null, so
	 * start() threw before it could report anything (#115).
	 */
	@Test
	void explicitPortConstructorReportsInitializedAfterStart() {
		assertFalse(statusMessages.isEmpty(), "start() must report its status");
		ThreadStateMessage first = statusMessages.get(0);
		assertEquals("PSTRotator", first.getThreadNickName());
		assertEquals("initialized", first.getRunningInformation());
		assertTrue(first.isRunning());
		assertFalse(first.isCriticalState());
	}

	/** The poller asks for azimuth, then mode, as two separate datagrams. */
	@Test
	void pollerQueriesAzimuthThenMode() throws Exception {
		assertEquals(POLL_AZIMUTH, receive().text);
		assertEquals(POLL_MODE, receive().text);
	}

	@Test
	void controlCommandsAreSentAsPstXmlFrames() throws Exception {
		client.setAzimuth(145.7);
		client.setElevation(10.5);
		client.stopRotor();
		client.park();
		client.setTrackingMode(true);
		client.setTrackingMode(false);

		// The azimuth is truncated to whole degrees; the elevation is not.
		assertEquals("<PST><AZIMUTH>145</AZIMUTH></PST>", receiveCommand());
		assertEquals("<PST><ELEVATION>10.5</ELEVATION></PST>", receiveCommand());
		assertEquals("<PST><STOP>1</STOP></PST>", receiveCommand());
		assertEquals("<PST><PARK>1</PARK></PST>", receiveCommand());
		assertEquals("<PST><TRACK>1</TRACK></PST>", receiveCommand());
		assertEquals("<PST><TRACK>0</TRACK></PST>", receiveCommand());
	}

	@Test
	void repliesToTheClientSocketReachTheListener() throws Exception {
		SocketAddress clientAddress = receive().source;

		reply(clientAddress, "AZ:145.0\r");
		reply(clientAddress, "EL:010.0\r");
		reply(clientAddress, "MODE:1\r");
		reply(clientAddress, "MODE:0\r");

		assertEquals(145.0, poll(listener.azimuths));
		assertEquals(10.0, poll(listener.elevations));
		assertEquals(Boolean.TRUE, poll(listener.modes));
		assertEquals(Boolean.FALSE, poll(listener.modes));
		// The raw message is handed over trimmed, i.e. without the CR.
		assertEquals("AZ:145.0", poll(listener.rawMessages));
	}

	/** An unparsable value is dropped; the receive loop keeps running. */
	@Test
	void unparsableReplyDoesNotStopTheListener() throws Exception {
		SocketAddress clientAddress = receive().source;

		reply(clientAddress, "AZ:not-a-number\r");
		reply(clientAddress, "AZ:90.0\r");

		assertEquals(90.0, poll(listener.azimuths));
		assertTrue(listener.azimuths.isEmpty());
	}

	private Received receive() throws Exception {
		byte[] buffer = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		pstRotator.receive(packet);
		return new Received(new String(packet.getData(), 0, packet.getLength(), StandardCharsets.US_ASCII),
				packet.getSocketAddress());
	}

	/** Receives the next datagram that is not one of the poller's queries. */
	private String receiveCommand() throws Exception {
		while (true) {
			String text = receive().text;
			if (!text.equals(POLL_AZIMUTH) && !text.equals(POLL_MODE)) {
				return text;
			}
		}
	}

	private void reply(SocketAddress target, String text) throws Exception {
		byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
		pstRotator.send(new DatagramPacket(bytes, bytes.length, target));
	}

	private static <T> T poll(BlockingQueue<T> queue) throws InterruptedException {
		T value = queue.poll(5, TimeUnit.SECONDS);
		assertNotNull(value, "expected a listener callback");
		return value;
	}

	private record Received(String text, SocketAddress source) {
	}

	private static final class RecordingListener implements PstRotatorEventListener {
		final BlockingQueue<Double> azimuths = new LinkedBlockingQueue<>();
		final BlockingQueue<Double> elevations = new LinkedBlockingQueue<>();
		final BlockingQueue<Boolean> modes = new LinkedBlockingQueue<>();
		final BlockingQueue<String> rawMessages = new LinkedBlockingQueue<>();

		@Override
		public void onAzimuthUpdate(double azimuth) {
			azimuths.add(azimuth);
		}

		@Override
		public void onElevationUpdate(double elevation) {
			elevations.add(elevation);
		}

		@Override
		public void onModeUpdate(boolean isTracking) {
			modes.add(isTracking);
		}

		@Override
		public void onMessageReceived(String rawMessage) {
			rawMessages.add(rawMessage);
		}
	}
}
