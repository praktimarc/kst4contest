package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import kst4contest.ApplicationConstants;
import kst4contest.model.AirPlane;
import kst4contest.model.AirPlaneReflectionInfo;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatPreferences;
import kst4contest.model.ThreadStateMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Loopback tests for the AirScout UDP listener. The listener runs on a socket
 * bound to an ephemeral loopback port, and every datagram goes to the loopback
 * address, so the tests neither collide with a running AirScout nor broadcast.
 *
 * <p>The responses follow the {@code ASNEAREST} format documented in
 * {@link ReadUDPbyAirScoutMessageThread#processASUDPMessage(String)}.</p>
 */
class ReadUDPbyAirScoutMessageThreadLoopbackTest {

	/** Configured client name with a non-ASCII character: the listener decodes UTF-8 (#115). */
	private static final String CLIENT = "KST-Ä";
	private static final String SERVER = "AS";
	private static final String PLANES = "2023-04-01 21:33:42Z,DO5AMF,JN49GL,G4CLA,IO92JL,2,"
			+ "BAW809,M,250,50,18,KLM1678,L,313,75,22";

	private final ChatController controller = mock(ChatController.class);
	private final ChatPreferences preferences = mock(ChatPreferences.class);
	private final ScoreService scoreService = mock(ScoreService.class);
	private final BlockingQueue<ThreadStateMessage> statusMessages = new LinkedBlockingQueue<>();

	private DatagramSocket sender;
	private InetSocketAddress target;
	private ReadUDPbyAirScoutMessageThread listener;

	@BeforeEach
	void startListener() throws Exception {
		when(controller.getChatPreferences()).thenReturn(preferences);
		when(controller.getScoreService()).thenReturn(scoreService);
		when(preferences.isAirScout_asUDPListenerEnabled()).thenReturn(true);
		when(preferences.getAirScout_asServerNameString()).thenReturn(SERVER);
		when(preferences.getAirScout_asClientNameString()).thenReturn(CLIENT);

		InetAddress loopback = InetAddress.getLoopbackAddress();
		DatagramSocket bound = new DatagramSocket(new InetSocketAddress(loopback, 0));
		target = new InetSocketAddress(loopback, bound.getLocalPort());
		sender = new DatagramSocket(new InetSocketAddress(loopback, 0));
		listener = new ReadUDPbyAirScoutMessageThread(bound, controller,
				(threadName, message) -> statusMessages.add(message));
		listener.setDaemon(true);
	}

	@AfterEach
	void stopListener() throws Exception {
		listener.interrupt();
		listener.join(2_000);
		sender.close();
	}

	@Test
	void matchingResponseIsAppliedToEveryActiveVariantOfTheReceiver() throws Exception {
		ChatMember plain = new ChatMember();
		ChatMember portable = new ChatMember();
		when(controller.findActiveChatMembersByRawCall("G4CLA")).thenReturn(List.of(plain, portable));
		listener.start();

		send(response(SERVER, CLIENT, PLANES));

		awaitResponseProcessed();
		AirPlaneReflectionInfo info = plain.getAirPlaneReflectInfo();
		assertSame(info, portable.getAirPlaneReflectInfo());
		assertEquals("DO5AMF", info.getSender().getCallSign());
		assertEquals("JN49GL", info.getSender().getQra());
		assertEquals("G4CLA", info.getReceiver().getCallSign());
		assertEquals("IO92JL", info.getReceiver().getQra());
		assertEquals(2, info.getAirPlanesReachableCntr());

		// Sorted by potential, highest first.
		List<AirPlane> planes = info.getRisingAirplanes();
		assertEquals(2, planes.size());
		assertPlane(planes.get(0), "KLM1678", "L", 313, 75, 22);
		assertPlane(planes.get(1), "BAW809", "M", 250, 50, 18);

		verify(scoreService).requestRecompute("airscout-update");
	}

	/**
	 * The parser strips every space from the datagram, including the one
	 * between date and time. The date is only shown in toString(), so this is
	 * pinned as current behaviour rather than fixed here.
	 */
	@Test
	void dateLosesTheSpaceBetweenDayAndTimeBecauseTheParserStripsAllSpaces() throws Exception {
		ChatMember member = new ChatMember();
		when(controller.findActiveChatMembersByRawCall("G4CLA")).thenReturn(List.of(member));
		listener.start();

		send(response(SERVER, CLIENT, PLANES));

		awaitResponseProcessed();
		assertEquals("2023-04-0121:33:42Z", member.getAirPlaneReflectInfo().getDate());
	}

	/**
	 * The poison pill and the ignored responses travel the same loopback path
	 * in order, so once the listener has ended on the pill, the earlier
	 * datagrams have been read and discarded.
	 */
	@Test
	void responsesForAnotherClientOrServerAreIgnored() throws Exception {
		listener.start();

		send(response(SERVER, "KST-A", PLANES));
		send(response("AS2", CLIENT, PLANES));
		send(response(SERVER, "kst-ä", PLANES));
		send(ApplicationConstants.DISCONNECT_RDR_POISONPILL);

		listener.join(5_000);
		assertFalse(listener.isAlive(), "the poison pill must end the listener");
		verify(controller, never()).findActiveChatMembersByRawCall(anyString());
		assertTrue(statusMessages.isEmpty());
	}

	@Test
	void disabledListenerDiscardsResponsesButStillHonoursThePoisonPill() throws Exception {
		when(preferences.isAirScout_asUDPListenerEnabled()).thenReturn(false);
		listener.start();

		send(response(SERVER, CLIENT, PLANES));
		send(ApplicationConstants.DISCONNECT_RDR_POISONPILL);

		listener.join(5_000);
		assertFalse(listener.isAlive(), "the poison pill must end the listener");
		verify(controller, never()).findActiveChatMembersByRawCall(anyString());
	}

	/** A malformed response is dropped; the next valid one is still processed. */
	@Test
	void malformedResponseDoesNotStopTheListener() throws Exception {
		ChatMember member = new ChatMember();
		when(controller.findActiveChatMembersByRawCall("G4CLA")).thenReturn(List.of(member));
		listener.start();

		send(response(SERVER, CLIENT, "2023-04-01 21:33:42Z,DO5AMF"));
		send(response(SERVER, CLIENT, PLANES));

		awaitResponseProcessed();
		assertEquals("G4CLA", member.getAirPlaneReflectInfo().getReceiver().getCallSign());
		assertNull(statusMessages.poll());
	}

	private static String response(String server, String client, String body) {
		return "ASNEAREST: \"" + server + "\" \"" + client + "\" \"" + body + "\"";
	}

	private void send(String payload) throws Exception {
		byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
		sender.send(new DatagramPacket(bytes, bytes.length, target));
	}

	private void awaitResponseProcessed() throws InterruptedException {
		ThreadStateMessage message = statusMessages.poll(5, TimeUnit.SECONDS);
		assertTrue(message != null && message.getRunningInformation().startsWith("Received AirScout response"),
				"listener must report the processed response");
	}

	private static void assertPlane(AirPlane plane, String callSign, String size, int distanceKm,
			int potential, int minutes) {
		assertEquals(callSign, plane.getApCallSign());
		assertEquals(size, plane.getApSizeCategory());
		assertEquals(distanceKm, plane.getDistanceKm());
		assertEquals(potential, plane.getPotential());
		assertEquals(minutes, plane.getArrivingDurationMinutes());
	}
}
