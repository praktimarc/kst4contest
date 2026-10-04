package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WinTestLogSyncServiceTest {

	private static final String LOG_ID = "STN1@44510";

	private final List<String> sentRequests = new ArrayList<>();

	private long currentTimeMs = 1_000_000L;

	private WinTestLogSyncService service;

	@BeforeEach
	void createService() {
		sentRequests.clear();
		service = new WinTestLogSyncService(
				(targetStation, logId, countFrom, countTo) ->
						sentRequests.add(targetStation + " " + logId + " " + countFrom + "-" + countTo),
				() -> "KST4Contest",
				() -> currentTimeMs
		);
	}

	private void receiveIhave(String messageText) {
		service.onIhaveReceived(WinTestPacket.fromMessageText(messageText));
	}

	private void receiveQsos(long countFrom, long countTo) {
		for (long qsoNumber = countFrom; qsoNumber <= countTo; qsoNumber++) {
			service.registerReceivedQso(LOG_ID, qsoNumber);
		}
	}

	@Test
	void firstBlockOfAnAnnouncedLogIsRequested() {
		receiveIhave("IHAVE: \"STN1\" \"\" \"" + LOG_ID + "\" O 1 1 120");

		service.tick();

		assertEquals(List.of("STN1 " + LOG_ID + " 1-50"), sentRequests);
		assertEquals(WinTestLogSyncService.SyncState.SYNCING, service.getState());
	}

	@Test
	void answeredBlockTriggersTheNextBlockUntilTheLogIsComplete() {
		receiveIhave("IHAVE: \"STN1\" \"\" \"" + LOG_ID + "\" O 1 1 120");

		service.tick();
		receiveQsos(1L, 50L);
		service.tick();
		receiveQsos(51L, 100L);
		service.tick();
		receiveQsos(101L, 120L);
		service.tick();

		assertEquals(
				List.of(
						"STN1 " + LOG_ID + " 1-50",
						"STN1 " + LOG_ID + " 51-100",
						"STN1 " + LOG_ID + " 101-120"
				),
				sentRequests);
		assertEquals(WinTestLogSyncService.SyncState.IN_SYNC, service.getState());
	}

	@Test
	void onlyMissingQsoNumbersAreRequested() {
		receiveQsos(1L, 10L);
		receiveQsos(21L, 30L);
		receiveIhave("IHAVE: \"STN1\" \"\" \"" + LOG_ID + "\" O 1 1 30");

		service.tick();

		assertEquals(List.of("STN1 " + LOG_ID + " 11-20"), sentRequests);
	}

	@Test
	void alreadyKnownQsoIsReportedAsKnown() {
		assertTrue(service.registerReceivedQso(LOG_ID, 5L));
		assertFalse(service.registerReceivedQso(LOG_ID, 5L));
	}

	@Test
	void qsoWithoutUsableIdentityIsAlwaysTreatedAsNew() {
		assertTrue(service.registerReceivedQso(null, 5L));
		assertTrue(service.registerReceivedQso("", 5L));
		assertTrue(service.registerReceivedQso(LOG_ID, 0L));
	}

	@Test
	void unansweredRequestIsRepeatedAtAnotherStationHoldingTheSameLog() {
		receiveIhave("IHAVE: \"STN1\" \"\" \"" + LOG_ID + "\" O 1 1 120");
		receiveIhave("IHAVE: \"STN2\" \"\" \"" + LOG_ID + "\" E 1 1 120");

		service.tick();
		currentTimeMs += WinTestLogSyncService.REQUEST_TIMEOUT_MS;
		service.tick();

		assertEquals(
				List.of("STN1 " + LOG_ID + " 1-50", "STN2 " + LOG_ID + " 1-50"),
				sentRequests);
	}

	@Test
	void silentStationIsDroppedWhenNobodyElseHoldsTheLog() {
		receiveIhave("IHAVE: \"STN1\" \"\" \"" + LOG_ID + "\" O 1 1 120");

		service.tick();
		currentTimeMs += WinTestLogSyncService.REQUEST_TIMEOUT_MS;
		service.tick();
		currentTimeMs += WinTestLogSyncService.REQUEST_TIMEOUT_MS;
		service.tick();

		assertEquals(List.of("STN1 " + LOG_ID + " 1-50"), sentRequests);
	}

	@Test
	void blindFallbackRequestsFixedBlocksWithoutInventory() {
		service.onStationSeen("STN1");
		service.registerReceivedQso(LOG_ID, 7L);

		service.tick();
		assertEquals(List.of(), sentRequests);

		currentTimeMs += WinTestLogSyncService.INVENTORY_GRACE_PERIOD_MS;
		service.tick();
		receiveQsos(1L, 50L);
		service.tick();

		currentTimeMs += WinTestLogSyncService.REQUEST_TIMEOUT_MS;
		service.tick();
		currentTimeMs += WinTestLogSyncService.REQUEST_TIMEOUT_MS;
		service.tick();

		assertEquals(
				List.of("STN1 " + LOG_ID + " 1-50", "STN1 " + LOG_ID + " 51-100"),
				sentRequests);
	}

	@Test
	void inventoryStopsTheBlindFallback() {
		service.onStationSeen("STN1");
		service.registerReceivedQso(LOG_ID, 7L);
		currentTimeMs += WinTestLogSyncService.INVENTORY_GRACE_PERIOD_MS;

		receiveIhave("IHAVE: \"STN1\" \"\" \"" + LOG_ID + "\" O 1 1 10");
		service.tick();

		assertEquals(List.of("STN1 " + LOG_ID + " 1-6"), sentRequests);
	}

	@Test
	void ownPacketsDoNotStartASynchronization() {
		service.onStationSeen("KST4Contest");

		service.tick();

		assertEquals(List.of(), sentRequests);
		assertEquals(WinTestLogSyncService.SyncState.IDLE, service.getState());
	}
}
