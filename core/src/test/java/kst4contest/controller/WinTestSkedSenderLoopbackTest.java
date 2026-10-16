package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kst4contest.model.Band;
import kst4contest.model.ContestSked;
import kst4contest.model.ThreadStateMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Loopback tests for the Win-Test sked sender. The production code sends to a
 * broadcast address; here the "broadcast address" is the loopback address and
 * the receiver is a socket on an ephemeral loopback port, so nothing leaves the
 * machine and no running Win-Test sees the datagrams.
 *
 * <p>The expected frames are written out literally and their checksum is
 * computed here independently of {@link WinTestMessage}: the test pins the wire
 * format, not the serializer's current implementation.</p>
 */
class WinTestSkedSenderLoopbackTest {

	private static final String STATION = "KST4Contest";
	private static final long SKED_TIME_MS = 1_760_000_000_000L;

	private final List<ThreadStateMessage> statusMessages = new ArrayList<>();

	private DatagramSocket winTest;
	private WinTestSkedSender sender;

	@BeforeEach
	void bindFakeWinTest() throws Exception {
		InetAddress loopback = InetAddress.getLoopbackAddress();
		winTest = new DatagramSocket(new InetSocketAddress(loopback, 0));
		winTest.setSoTimeout(5_000);
		sender = new WinTestSkedSender(STATION, loopback, winTest.getLocalPort(),
				(threadName, message) -> statusMessages.add(message));
	}

	@AfterEach
	void closeFakeWinTest() {
		winTest.close();
	}

	@Test
	void pushSendsLockAddAndUnlockFramesInThisOrder() throws Exception {
		ContestSked sked = new ContestSked("DL1ABC", 47.0, SKED_TIME_MS, Band.B_432);

		sender.pushSkedToWinTest(sked, "DL1ABC", 432200.0, "[JO62QM - 47°] sked via KST4Contest", 0);

		assertFrame("LOCKSKED: \"KST4Contest\" \"\" \"KST4Contest\"", receive());
		/*
		 * Unix seconds, frequency in 0.1 kHz, Win-Test band ID 14 (432 MHz),
		 * mode 0 (CW), then the quoted callsign and notes. The degree sign is
		 * escaped as the four ASCII characters \260.
		 */
		assertFrame("ADDSKED: \"KST4Contest\" \"\" 1760000000 4322000 14 0 \"DL1ABC\""
				+ " \"[JO62QM - 47\\260] sked via KST4Contest\"", receive());
		assertFrame("UNLOCKSKED: \"KST4Contest\" \"\" \"KST4Contest\"", receive());

		assertEquals(1, statusMessages.size());
		assertEquals("Sked pushed to WT: DL1ABC", statusMessages.get(0).getRunningInformation());
		assertFalse(statusMessages.get(0).isCriticalState());
	}

	@Test
	void unexpectedModeIdIsSentAsSsbAndMissingNotesAsEmptyQuotes() throws Exception {
		ContestSked sked = new ContestSked("OK1XYZ", 120.0, SKED_TIME_MS, Band.B_144);

		sender.pushSkedToWinTest(sked, "OK1XYZ", 144300.0, null, 7);

		receive(); // LOCKSKED
		assertFrame("ADDSKED: \"KST4Contest\" \"\" 1760000000 1443000 12 1 \"OK1XYZ\" \"\"", receive());
		receive(); // UNLOCKSKED
	}

	private byte[] receive() throws Exception {
		byte[] buffer = new byte[2048];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		winTest.receive(packet);
		return Arrays.copyOf(packet.getData(), packet.getLength());
	}

	/**
	 * Asserts the Win-Test framing: the ASCII text, one checksum byte (the sum
	 * of all preceding bytes, OR 0x80, truncated to a byte) and a NUL.
	 */
	private static void assertFrame(String expectedText, byte[] actual) {
		byte[] text = expectedText.getBytes(StandardCharsets.US_ASCII);
		int sum = 0;
		for (byte b : text) {
			sum += b & 0xFF;
		}
		byte[] expected = Arrays.copyOf(text, text.length + 2);
		expected[text.length] = (byte) ((sum | 0x80) & 0xFF);
		expected[text.length + 1] = 0;

		assertEquals(expectedText, new String(actual, 0, Math.max(0, actual.length - 2), StandardCharsets.US_ASCII));
		assertArrayEquals(expected, actual);
	}
}
