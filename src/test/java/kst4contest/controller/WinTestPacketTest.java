package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class WinTestPacketTest {

	private static final String ADDQSO_MESSAGE =
			"ADDQSO: \"STN1\" \"\" \"STN1\" 1762202297 1440000 0 12 0 0 0 2 2 "
					+ "\"DM2RN\" \"599\" \"599001\" \"JO51UM\" \"\" \"\" 0 \"\" \"\" \"\" 44510";

	/**
	 * Builds a datagram exactly like Win-Test does: message text, checksum byte
	 * replacing the placeholder, NUL terminator.
	 */
	private static byte[] toDatagram(String messageText) {
		byte[] datagram = (messageText + "?\0").getBytes(StandardCharsets.US_ASCII);

		int sum = 0;
		for (int index = 0; index < datagram.length - 2; index++) {
			sum += datagram[index] & 0xFF;
		}

		datagram[datagram.length - 2] = (byte) ((sum | 0x80) & 0xFF);
		return datagram;
	}

	@Test
	void checksumByteAndTerminatorAreRemovedFromMessageText() {
		byte[] datagram = toDatagram(ADDQSO_MESSAGE);

		WinTestPacket packet = WinTestPacket.fromDatagram(datagram, datagram.length);

		assertEquals(ADDQSO_MESSAGE, packet.getMessageText());
		assertTrue(packet.isChecksumPresent());
		assertTrue(packet.isChecksumValid());
	}

	@Test
	void trailingLogIdStaysReadableAfterFramingIsResolved() {
		byte[] datagram = toDatagram(ADDQSO_MESSAGE);

		WinTestPacket packet = WinTestPacket.fromDatagram(datagram, datagram.length);
		List<String> packetFields = WinTestPacket.tokenize(packet.getMessageText());

		assertEquals("STN1@44510",
				ReadUDPByWintestThread.extractLogIdFromWinTestAddQso(packetFields));
		assertEquals(2L,
				ReadUDPByWintestThread.extractQsoNumberFromWinTestAddQso(packetFields));
	}

	@Test
	void manipulatedChecksumIsDetected() {
		byte[] datagram = toDatagram(ADDQSO_MESSAGE);
		datagram[datagram.length - 2] = (byte) 0xFF;

		WinTestPacket packet = WinTestPacket.fromDatagram(datagram, datagram.length);

		assertTrue(packet.isChecksumPresent());
		assertFalse(packet.isChecksumValid());
	}

	@Test
	void sourceAndDestinationAreSeparatedFromPayload() {
		byte[] datagram = toDatagram(
				"IHAVE: \"STN1\" \"KST4Contest\" \"STN1@44510\" O 1 1 120");

		WinTestPacket packet = WinTestPacket.fromDatagram(datagram, datagram.length);

		assertEquals("IHAVE", packet.getMessageType());
		assertEquals("STN1", packet.getSource());
		assertEquals("KST4Contest", packet.getDestination());
		assertEquals(List.of("STN1@44510", "O", "1", "1", "120"), packet.getDataTokens());
		assertTrue(packet.isAddressedTo("KST4Contest"));
		assertFalse(packet.isAddressedTo("STN2"));
	}

	@Test
	void emptyQuotedFieldsKeepFieldPositions() {
		List<String> packetFields = WinTestPacket.tokenize(ADDQSO_MESSAGE);

		assertEquals("ADDQSO:", packetFields.get(0));
		assertEquals("STN1", packetFields.get(1));
		assertEquals("", packetFields.get(2));
		assertEquals("12", packetFields.get(7));
		assertEquals("DM2RN", packetFields.get(13));
		assertEquals(24, packetFields.size());
	}

	@Test
	void messageWithoutWinTestFramingIsRejected() {
		assertNull(WinTestPacket.fromMessageText("no win-test message"));
		assertNull(WinTestPacket.fromDatagram(new byte[] { 0 }, 1));
		assertNull(WinTestPacket.fromDatagram(null, 0));
	}

	@Test
	void broadcastPacketIsAcceptedForEveryStationName() {
		WinTestPacket packet = WinTestPacket.fromMessageText(ADDQSO_MESSAGE);

		assertTrue(packet.isAddressedTo("KST4Contest"));
		assertFalse(packet.isChecksumPresent());
	}
}
