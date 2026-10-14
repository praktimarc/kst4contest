package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import kst4contest.model.ChatMember;
import kst4contest.model.ChatPreferences;
import kst4contest.model.ThreadStateMessage;
import kst4contest.observe.SimpleValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Loopback tests for the local DX Cluster server that logging programs
 * connect to. The server runs on a socket bound to an ephemeral loopback port,
 * and the clients are plain sockets reading what a telnet client would see.
 *
 * <p>The column layout itself is pinned by {@link DXClusterSpotFormatterTest};
 * these tests pin what reaches a connected client and when.</p>
 */
class DXClusterThreadPooledServerLoopbackTest {

	/** Prompt sent on connect; the server never reads the answer. */
	private static final String LOGIN_PROMPT = "login: ";
	/** 75 characters, two BEL and CR LF. */
	private static final int SPOT_LENGTH = 79;

	private final ChatController controller = mock(ChatController.class);
	private final ChatPreferences preferences = mock(ChatPreferences.class);
	private final BlockingQueue<ThreadStateMessage> statusMessages = new LinkedBlockingQueue<>();
	private final List<Socket> clients = new ArrayList<>();

	private int port;
	private DXClusterThreadPooledServer server;
	private Thread serverThread;

	@BeforeEach
	void startServer() throws Exception {
		when(controller.getChatPreferences()).thenReturn(preferences);
		when(preferences.getNotify_DXCSrv_SpottersCallSign()).thenReturn(new SimpleValue<>("dl0kst"));
		when(preferences.getNotify_optionalFrequencyPrefix()).thenReturn(new SimpleValue<>("144"));

		ServerSocket bound = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
		port = bound.getLocalPort();
		server = new DXClusterThreadPooledServer(bound, controller,
				(threadName, message) -> statusMessages.add(message));
		serverThread = new Thread(server, "dxcluster-loopback-test");
		serverThread.setDaemon(true);
		serverThread.start();
	}

	@AfterEach
	void stopServer() throws Exception {
		server.stop();
		serverThread.join(2_000);
		for (Socket client : clients) {
			client.close();
		}
	}

	@Test
	void serverReportsTheListeningPort() throws Exception {
		ThreadStateMessage message = statusMessages.poll(5, TimeUnit.SECONDS);
		assertNotNull(message);
		assertEquals("Listening on TCP port " + port, message.getRunningInformation());
		assertTrue(message.isRunning());
	}

	@Test
	void connectingClientIsGreetedWithALoginPrompt() throws Exception {
		Socket client = connect();

		assertEquals(LOGIN_PROMPT, read(client, LOGIN_PROMPT.length()));
		assertTrue(server.hasConnectedClients());
	}

	/**
	 * The spot is one DXSpider-style line: spotter in upper case, frequency in
	 * kHz with one decimal right-aligned to column 24, the DX call from column
	 * 27, the locator as comment, HHMMZ from column 71, then BEL BEL CR LF.
	 * A relative frequency is completed with the configured prefix. What the
	 * client sends after the prompt is ignored.
	 */
	@Test
	void spotReachesTheClientAsOneDxSpiderLine() throws Exception {
		Socket client = connect();
		assertEquals(LOGIN_PROMPT, read(client, LOGIN_PROMPT.length()));
		client.getOutputStream().write("DL1TEST\r\n".getBytes(StandardCharsets.US_ASCII));
		client.getOutputStream().flush();
		statusMessages.poll(5, TimeUnit.SECONDS); // "Listening on ..."

		assertTrue(server.broadcastSingleDXClusterEntryToLoggers(station("ok1abc", "JO70FD", ".205")));

		String spot = read(client, SPOT_LENGTH);
		assertEquals("DX de DL0KST:   144205.0  OK1ABC       JO70FD                         ",
				spot.substring(0, 70));
		assertTrue(spot.substring(70, 75).matches("[0-2]\\d[0-5]\\dZ"), spot);
		assertEquals("\u0007\u0007\r\n", spot.substring(75));

		ThreadStateMessage status = statusMessages.poll(5, TimeUnit.SECONDS);
		assertNotNull(status);
		assertEquals("Last spot sent to 1 DX Cluster client(s):\n" + spot, status.getRunningInformation());
	}

	@Test
	void spotIsSentToEveryConnectedClient() throws Exception {
		Socket first = connect();
		Socket second = connect();
		assertEquals(LOGIN_PROMPT, read(first, LOGIN_PROMPT.length()));
		assertEquals(LOGIN_PROMPT, read(second, LOGIN_PROMPT.length()));

		assertTrue(server.broadcastSingleDXClusterEntryToLoggers(station("SP2XYZ", "JO93AB", "432.088")));

		String firstSpot = read(first, SPOT_LENGTH);
		assertEquals(firstSpot, read(second, SPOT_LENGTH));
		assertTrue(firstSpot.startsWith("DX de DL0KST:   432088.0  SP2XYZ "), firstSpot);
	}

	@Test
	void spotWithoutConnectedClientsIsNotDelivered() {
		assertFalse(server.hasConnectedClients());
		assertFalse(server.broadcastSingleDXClusterEntryToLoggers(station("OK1ABC", "JO70FD", "144.205")));
	}

	/** A spot that cannot be formatted is rejected, and the client receives nothing. */
	@Test
	void unformattableSpotIsRejectedWithoutWritingToTheClient() throws Exception {
		Socket client = connect();
		assertEquals(LOGIN_PROMPT, read(client, LOGIN_PROMPT.length()));

		assertFalse(server.broadcastSingleDXClusterEntryToLoggers(station("OK1ABC", "JO70FD", "")));
		assertFalse(server.broadcastSingleDXClusterEntryToLoggers(
				station("OK1ABCDEFGHIJ", "JO70FD", "144.205")));

		// The next valid spot is the first thing after the prompt.
		assertTrue(server.broadcastSingleDXClusterEntryToLoggers(station("OK1ABC", "JO70FD", "144.205")));
		assertTrue(read(client, SPOT_LENGTH).startsWith("DX de DL0KST:"));
	}

	@Test
	void stopClosesTheClientConnectionsAndEndsTheServerThread() throws Exception {
		Socket client = connect();
		assertEquals(LOGIN_PROMPT, read(client, LOGIN_PROMPT.length()));

		server.stop();
		serverThread.join(5_000);

		assertFalse(serverThread.isAlive(), "server thread must end after stop()");
		assertEquals(-1, client.getInputStream().read(), "client must see end of stream");
		assertFalse(server.hasConnectedClients());
	}

	private Socket connect() throws IOException {
		Socket client = new Socket(InetAddress.getLoopbackAddress(), port);
		client.setSoTimeout(5_000);
		clients.add(client);
		return client;
	}

	private static String read(Socket client, int length) throws IOException {
		InputStream input = client.getInputStream();
		byte[] bytes = input.readNBytes(length);
		assertEquals(length, bytes.length, "connection ended early");
		return new String(bytes, StandardCharsets.US_ASCII);
	}

	private static ChatMember station(String callSign, String locator, String frequency) {
		ChatMember member = new ChatMember();
		member.setCallSign(callSign);
		member.setQra(locator);
		member.setFrequency(new SimpleValue<>(frequency));
		return member;
	}
}
