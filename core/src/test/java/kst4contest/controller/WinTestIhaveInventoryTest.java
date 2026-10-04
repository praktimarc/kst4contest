package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class WinTestIhaveInventoryTest {

	private static Optional<WinTestIhaveInventory> parse(String messageText) {
		return WinTestIhaveInventory.fromPacket(WinTestPacket.fromMessageText(messageText));
	}

	@Test
	void inventoryStartingWithPresentQsosIsExpanded() {
		Optional<WinTestIhaveInventory> inventory =
				parse("IHAVE: \"STN1\" \"\" \"STN1@9\" E 1 1 911-1-117");

		assertTrue(inventory.isPresent());
		assertEquals("STN1@9", inventory.get().getLogId());
		assertEquals(WinTestIhaveInventory.Origin.LOGGED_ELSE, inventory.get().getOrigin());
		assertEquals(
				List.of(new WinTestLogSegment(1L, 911L), new WinTestLogSegment(913L, 1029L)),
				inventory.get().getSegments());
		assertEquals(1029L, inventory.get().getHighestQsoNumber());
	}

	@Test
	void inventoryStartingWithMissingQsosIsExpanded() {
		Optional<WinTestIhaveInventory> inventory =
				parse("IHAVE: \"STN1\" \"\" \"STN1@9\" O 1 0 30-5");

		assertTrue(inventory.isPresent());
		assertEquals(WinTestIhaveInventory.Origin.OWNER, inventory.get().getOrigin());
		assertEquals(List.of(new WinTestLogSegment(31L, 35L)), inventory.get().getSegments());
	}

	@Test
	void splitInventoryStartsAtItsFirstRow() {
		/*
		 * Win-Test splits long inventories. The documented example "100 1 10-5-5"
		 * means: ten QSOs from 100, five missing, five present again.
		 */
		Optional<WinTestIhaveInventory> inventory =
				parse("IHAVE: \"STN1\" \"\" \"STN1@9\" O 100 1 10-5-5");

		assertTrue(inventory.isPresent());
		assertEquals(
				List.of(new WinTestLogSegment(100L, 109L), new WinTestLogSegment(115L, 119L)),
				inventory.get().getSegments());
	}

	@Test
	void runLengthChainWithWrongParityIsRejected() {
		assertTrue(parse("IHAVE: \"STN1\" \"\" \"STN1@9\" O 1 1 10-5").isEmpty());
		assertTrue(parse("IHAVE: \"STN1\" \"\" \"STN1@9\" O 1 0 10-5-5").isEmpty());
	}

	@Test
	void legacyInventoryWithoutRunLengthsIsRejected() {
		assertTrue(parse("IHAVE: \"STN1\" \"\" \"STN1@169\" \"OWNER\" 2").isEmpty());
	}

	@Test
	void otherMessageTypesAreRejected() {
		assertTrue(parse("STATUS: \"STN1\" \"\" 0 12 0 0 0 1443210 0").isEmpty());
	}
}
