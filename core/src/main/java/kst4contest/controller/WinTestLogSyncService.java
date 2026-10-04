package kst4contest.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Recovers the part of a Win-Test log that was written before KST4Contest was
 * started.
 *
 * <p>Win-Test broadcasts every new QSO as an {@code ADDQSO} packet. A client
 * that joins the network later never sees the QSOs logged before it started, so
 * stations already worked would still be shown as not worked. Win-Test also
 * offers a pull mechanism for exactly this situation, and this service is the
 * port of the wtKST {@code WtLogSync} implementation of it:</p>
 *
 * <ol>
 *     <li>a Win-Test station announces itself with {@code HELLO} or, if its log
 *         was opened before we started listening, with its periodic
 *         {@code STATUS};</li>
 *     <li>its periodic {@code IHAVE} packets announce which QSO numbers of
 *         which log it holds;</li>
 *     <li>missing ranges are requested with {@code NEEDQSO}, at most
 *         {@value #MAX_QSOS_PER_REQUEST} QSOs per request;</li>
 *     <li>Win-Test answers with ordinary {@code ADDQSO} packets that are
 *         addressed to us instead of being broadcast.</li>
 * </ol>
 *
 * <p>Because the answers are ordinary {@code ADDQSO} packets, the recovered
 * QSOs run through the same Worked handling as live QSOs. This service only
 * decides what still has to be requested; it neither touches the database nor
 * the user interface.</p>
 *
 * <p>If a station is known but no usable {@code IHAVE} inventory arrives within
 * {@value #INVENTORY_GRACE_PERIOD_MS} ms, a blind fallback requests fixed
 * blocks starting at QSO number one until a block stays unanswered. That covers
 * Win-Test versions whose {@code IHAVE} format carries no run lengths.</p>
 *
 * <p>Deviation from wtKST: wtKST discards its whole QSO table whenever a
 * {@code HELLO} arrives, because it displays that table. KST4Contest only
 * accumulates Worked state, where a stale entry is harmless while a discarded
 * one would cause the complete log to be requested and written again. The log
 * identity {@code StationName@LogUniqueID} already changes when Win-Test opens
 * a different log, so nothing is cleared here.</p>
 */
public class WinTestLogSyncService {

	/** Win-Test answers at most this many QSOs for one NEEDQSO request. */
	static final int MAX_QSOS_PER_REQUEST = 50;

	/** Time after which an unanswered request is retried elsewhere. */
	static final long REQUEST_TIMEOUT_MS = 5000L;

	/** Shortest distance between two evaluations without a pending trigger. */
	static final long TICK_INTERVAL_MS = 2000L;

	/** Waiting time for a usable IHAVE before the blind fallback starts. */
	static final long INVENTORY_GRACE_PERIOD_MS = 15000L;

	/** Upper bound for the blind fallback, equals 10000 QSOs. */
	static final int MAX_BLIND_BLOCKS = 200;

	/** Guards the gap search against a corrupted inventory. */
	private static final long MAX_SCANNED_QSO_NUMBERS = 200000L;

	/**
	 * Sends a NEEDQSO request to a Win-Test station.
	 */
	@FunctionalInterface
	public interface NeedQsoSender {

		/**
		 * @param targetStation Win-Test station name the request is sent to
		 * @param logId log identity in the form {@code StationName@LogUniqueID}
		 * @param countFrom first requested QSO number
		 * @param countTo last requested QSO number
		 */
		void sendNeedQso(String targetStation, String logId, long countFrom, long countTo);
	}

	/** Progress of the log recovery, used for status reporting. */
	public enum SyncState {
		/** No Win-Test station seen yet. */
		IDLE,
		/** A station is known, but nothing has been requested yet. */
		STATION_DETECTED,
		/** QSOs are being requested. */
		SYNCING,
		/** Everything announced by the known stations has been received. */
		IN_SYNC
	}

	private final NeedQsoSender needQsoSender;
	private final Supplier<String> ownStationNameSupplier;
	private final LongSupplier clock;

	/** Inventories per Win-Test station, keyed by log identity. */
	private final Map<String, Map<String, WinTestIhaveInventory>> inventoriesByStation =
			new LinkedHashMap<>();

	/** QSO numbers already received, keyed by log identity. */
	private final Map<String, NavigableSet<Long>> receivedQsoNumbersByLogId = new HashMap<>();

	/** State of the blind fallback, keyed by log identity. */
	private final Map<String, BlindScan> blindScansByLogId = new LinkedHashMap<>();

	private PendingRequest pendingRequest;
	private long firstStationSeenAtMs;
	private boolean usableInventorySeen;
	private long lastTickMs;
	private boolean tickDueImmediately;
	private SyncState state = SyncState.IDLE;

	/**
	 * @param needQsoSender transport used for NEEDQSO requests
	 * @param ownStationNameSupplier own Win-Test station name, read late because
	 *                               it can be changed in the settings at runtime
	 */
	public WinTestLogSyncService(
			NeedQsoSender needQsoSender,
			Supplier<String> ownStationNameSupplier
	) {
		this(needQsoSender, ownStationNameSupplier, System::currentTimeMillis);
	}

	/**
	 * @param needQsoSender transport used for NEEDQSO requests
	 * @param ownStationNameSupplier own Win-Test station name
	 * @param clock time source in milliseconds
	 */
	WinTestLogSyncService(
			NeedQsoSender needQsoSender,
			Supplier<String> ownStationNameSupplier,
			LongSupplier clock
	) {
		this.needQsoSender = needQsoSender;
		this.ownStationNameSupplier = ownStationNameSupplier;
		this.clock = clock;
	}

	/**
	 * Registers a Win-Test station seen in a HELLO or STATUS packet.
	 *
	 * @param stationName Win-Test station name
	 */
	public synchronized void onStationSeen(String stationName) {
		if (stationName == null || stationName.isBlank()) {
			return;
		}

		String normalizedStationName = stationName.trim();
		if (normalizedStationName.equalsIgnoreCase(resolveOwnStationName())) {
			// our own packets, nothing to synchronize from
			return;
		}

		if (inventoriesByStation.putIfAbsent(normalizedStationName, new LinkedHashMap<>()) == null) {
			tickDueImmediately = true;
		}

		if (firstStationSeenAtMs == 0L) {
			firstStationSeenAtMs = clock.getAsLong();
		}

		if (state == SyncState.IDLE) {
			state = SyncState.STATION_DETECTED;
		}
	}

	/**
	 * Takes over the inventory of an IHAVE packet.
	 *
	 * @param packet received IHAVE packet
	 */
	public synchronized void onIhaveReceived(WinTestPacket packet) {
		if (packet == null || !packet.isAddressedTo(resolveOwnStationName())) {
			return;
		}

		Optional<WinTestIhaveInventory> parsedInventory = WinTestIhaveInventory.fromPacket(packet);
		if (parsedInventory.isEmpty()) {
			return;
		}

		WinTestIhaveInventory inventory = parsedInventory.get();
		onStationSeen(packet.getSource());

		Map<String, WinTestIhaveInventory> stationInventories =
				inventoriesByStation.get(packet.getSource() == null ? "" : packet.getSource().trim());

		if (stationInventories == null) {
			return;
		}

		WinTestIhaveInventory previousInventory =
				stationInventories.put(inventory.getLogId(), inventory);

		if (previousInventory == null
				|| !previousInventory.getSegments().equals(inventory.getSegments())) {
			System.out.println("[WinTest LogSync] inventory of " + inventory.getLogId()
					+ " from " + packet.getSource()
					+ " (" + inventory.getOrigin() + "): " + inventory.getSegments());
		}

		usableInventorySeen = true;
		blindScansByLogId.remove(inventory.getLogId());
		tickDueImmediately = true;
	}

	/**
	 * Registers a QSO received in an ADDQSO packet.
	 *
	 * @param logId log identity in the form {@code StationName@LogUniqueID}
	 * @param qsoNumber Win-Test QSO number inside that log
	 * @return {@code true} when this QSO was not known before, and therefore
	 *         still has to be applied to Worked state and database
	 */
	public synchronized boolean registerReceivedQso(String logId, long qsoNumber) {
		if (logId == null || logId.isBlank() || qsoNumber <= 0L) {
			// without a usable identity the QSO cannot be deduplicated
			return true;
		}

		NavigableSet<Long> receivedQsoNumbers =
				receivedQsoNumbersByLogId.computeIfAbsent(logId.trim(), key -> new TreeSet<>());
		boolean isNewQso = receivedQsoNumbers.add(qsoNumber);

		if (pendingRequest != null
				&& pendingRequest.logId.equals(logId.trim())
				&& qsoNumber >= pendingRequest.countFrom
				&& qsoNumber <= pendingRequest.countTo) {

			pendingRequest.answeredQsoCount++;

			if (qsoNumber == pendingRequest.countTo
					|| pendingRequest.answeredQsoCount >= pendingRequest.getRequestedQsoCount()) {
				PendingRequest completedRequest = pendingRequest;
				pendingRequest = null;
				if (completedRequest.blind) {
					finishBlindBlock(completedRequest);
				}
				tickDueImmediately = true;
			}
		}

		return isNewQso;
	}

	/**
	 * Advances the recovery. Called after every received packet and on every
	 * receive timeout of the listener; an internal interval keeps the actual
	 * work rare while a satisfied request triggers the next one immediately.
	 */
	public synchronized void tick() {
		long now = clock.getAsLong();

		if (!tickDueImmediately && now - lastTickMs < TICK_INTERVAL_MS) {
			return;
		}

		lastTickMs = now;
		tickDueImmediately = false;

		if (pendingRequest != null) {
			if (now - pendingRequest.sentAtMs < REQUEST_TIMEOUT_MS) {
				return;
			}
			handlePendingTimeout();
			return;
		}

		if (requestNextMissingRange(now)) {
			return;
		}

		if (requestNextBlindBlock(now)) {
			return;
		}

		if (state == SyncState.SYNCING) {
			state = SyncState.IN_SYNC;
		}
	}

	/**
	 * @return current progress of the recovery
	 */
	public synchronized SyncState getState() {
		return state;
	}

	/**
	 * @return number of QSO numbers known for the given log
	 */
	synchronized int getKnownQsoCount(String logId) {
		NavigableSet<Long> receivedQsoNumbers = receivedQsoNumbersByLogId.get(logId);
		return receivedQsoNumbers == null ? 0 : receivedQsoNumbers.size();
	}

	private void handlePendingTimeout() {
		PendingRequest timedOutRequest = pendingRequest;
		pendingRequest = null;

		if (timedOutRequest.blind) {
			finishBlindBlock(timedOutRequest);
			return;
		}

		String alternativeStation =
				findAlternativeStation(timedOutRequest.logId, timedOutRequest.targetStation);

		System.out.println("[WinTest LogSync] no answer from " + timedOutRequest.targetStation
				+ " for " + timedOutRequest.logId + " "
				+ timedOutRequest.countFrom + "-" + timedOutRequest.countTo);

		if (alternativeStation != null) {
			sendRequest(
					alternativeStation,
					timedOutRequest.logId,
					timedOutRequest.countFrom,
					timedOutRequest.countTo,
					false,
					clock.getAsLong()
			);
			return;
		}

		/*
		 * Nobody else holds this log. The silent station is dropped and returns
		 * with its next STATUS or IHAVE packet.
		 */
		System.out.println("[WinTest LogSync] dropping silent station "
				+ timedOutRequest.targetStation + ", waiting for its next STATUS or IHAVE");
		inventoriesByStation.remove(timedOutRequest.targetStation);
		tickDueImmediately = true;
	}

	private String findAlternativeStation(String logId, String excludedStation) {
		for (Map.Entry<String, Map<String, WinTestIhaveInventory>> station
				: inventoriesByStation.entrySet()) {

			if (station.getKey().equals(excludedStation)) {
				continue;
			}
			if (station.getValue().containsKey(logId)) {
				return station.getKey();
			}
		}
		return null;
	}

	/**
	 * Looks for the first announced QSO range that is still missing and
	 * requests it. Stations that own a log are preferred over stations that
	 * only mirror it.
	 *
	 * @param now current time in milliseconds
	 * @return {@code true} when a request was sent
	 */
	private boolean requestNextMissingRange(long now) {
		for (int pass = 0; pass < 2; pass++) {
			boolean preferOwner = pass == 0;

			for (Map.Entry<String, Map<String, WinTestIhaveInventory>> station
					: new ArrayList<>(inventoriesByStation.entrySet())) {

				for (WinTestIhaveInventory inventory : new ArrayList<>(station.getValue().values())) {
					boolean isOwner = inventory.getOrigin() == WinTestIhaveInventory.Origin.OWNER;
					if (preferOwner != isOwner) {
						continue;
					}

					long[] missingRange = findMissingRange(
							inventory.getSegments(),
							receivedQsoNumbersByLogId.get(inventory.getLogId())
					);

					if (missingRange == null) {
						continue;
					}

					sendRequest(
							station.getKey(),
							inventory.getLogId(),
							missingRange[0],
							missingRange[1],
							false,
							now
					);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Determines the next missing QSO range of one log.
	 *
	 * <p>wtKST compares its own segment list against the announced one and
	 * derives the request bounds from the segment indices. Searching the gap
	 * directly produces the same ranges for the ordinary cases, cannot run past
	 * the end of either list, and never asks for QSO numbers that are already
	 * known.</p>
	 *
	 * @param segments ranges announced by the station
	 * @param receivedQsoNumbers QSO numbers already received for this log
	 * @return first missing range as {@code {countFrom, countTo}}, or
	 *         {@code null} when nothing is missing
	 */
	static long[] findMissingRange(
			List<WinTestLogSegment> segments,
			NavigableSet<Long> receivedQsoNumbers
	) {
		if (segments == null) {
			return null;
		}

		long remainingScanBudget = MAX_SCANNED_QSO_NUMBERS;

		for (WinTestLogSegment segment : segments) {
			for (long qsoNumber = segment.getCountFrom();
					qsoNumber <= segment.getCountTo();
					qsoNumber++) {

				remainingScanBudget--;
				if (remainingScanBudget < 0L) {
					return null;
				}

				if (receivedQsoNumbers != null && receivedQsoNumbers.contains(qsoNumber)) {
					continue;
				}

				long countFrom = qsoNumber;
				long countTo = countFrom;

				while (countTo < segment.getCountTo()
						&& countTo - countFrom + 1L < MAX_QSOS_PER_REQUEST
						&& (receivedQsoNumbers == null || !receivedQsoNumbers.contains(countTo + 1L))) {
					countTo++;
				}

				return new long[] { countFrom, countTo };
			}
		}

		return null;
	}

	/**
	 * Requests the next fixed block of a log whose station never sent a usable
	 * inventory.
	 *
	 * @param now current time in milliseconds
	 * @return {@code true} when a request was sent
	 */
	private boolean requestNextBlindBlock(long now) {
		if (usableInventorySeen || firstStationSeenAtMs == 0L) {
			return false;
		}
		if (now - firstStationSeenAtMs < INVENTORY_GRACE_PERIOD_MS) {
			return false;
		}

		for (String logId : new ArrayList<>(receivedQsoNumbersByLogId.keySet())) {
			BlindScan blindScan = blindScansByLogId.computeIfAbsent(logId, key -> new BlindScan());

			if (blindScan.completed || blindScan.requestedBlockCount >= MAX_BLIND_BLOCKS) {
				continue;
			}

			String targetStation = resolveStationForLogId(logId);
			if (targetStation == null) {
				continue;
			}

			blindScan.requestedBlockCount++;
			sendRequest(
					targetStation,
					logId,
					blindScan.nextCountFrom,
					blindScan.nextCountFrom + MAX_QSOS_PER_REQUEST - 1L,
					true,
					now
			);
			return true;
		}

		return false;
	}

	private void finishBlindBlock(PendingRequest finishedRequest) {
		BlindScan blindScan = blindScansByLogId.get(finishedRequest.logId);
		if (blindScan == null) {
			return;
		}

		System.out.println("[WinTest LogSync] blind block " + finishedRequest.countFrom
				+ "-" + finishedRequest.countTo + " of " + finishedRequest.logId
				+ " answered with " + finishedRequest.answeredQsoCount + " QSOs");

		if (finishedRequest.answeredQsoCount == 0) {
			// the log ends before this block, nothing left to fetch
			blindScan.completed = true;
		} else {
			blindScan.nextCountFrom = finishedRequest.countTo + 1L;
		}

		tickDueImmediately = true;
	}

	/**
	 * Resolves the Win-Test station a log belongs to. The log identity carries
	 * the owning station name in front of the {@code @} separator.
	 *
	 * @param logId log identity
	 * @return station name to ask, or {@code null} when none is known
	 */
	private String resolveStationForLogId(String logId) {
		int separatorIndex = logId.indexOf('@');
		String ownerStationName = separatorIndex > 0 ? logId.substring(0, separatorIndex) : logId;

		for (String stationName : inventoriesByStation.keySet()) {
			if (stationName.equalsIgnoreCase(ownerStationName)) {
				return stationName;
			}
		}

		return ownerStationName.isBlank() ? null : ownerStationName;
	}

	private void sendRequest(
			String targetStation,
			String logId,
			long countFrom,
			long countTo,
			boolean blind,
			long now
	) {
		pendingRequest = new PendingRequest(targetStation, logId, countFrom, countTo, blind, now);
		state = SyncState.SYNCING;

		try {
			needQsoSender.sendNeedQso(targetStation, logId, countFrom, countTo);
		} catch (RuntimeException exception) {
			/*
			 * A failed transmission must not stop the receive loop. The pending
			 * request runs into its timeout and is retried from there.
			 */
			System.out.println(
					"[WinTest LogSync] NEEDQSO could not be sent: " + exception.getMessage()
			);
		}
	}

	private String resolveOwnStationName() {
		if (ownStationNameSupplier == null) {
			return "";
		}

		String ownStationName = ownStationNameSupplier.get();
		return ownStationName == null ? "" : ownStationName.trim();
	}

	/** Request that is waiting for its answer. */
	private static final class PendingRequest {

		private final String targetStation;
		private final String logId;
		private final long countFrom;
		private final long countTo;
		private final boolean blind;
		private final long sentAtMs;
		private int answeredQsoCount;

		private PendingRequest(
				String targetStation,
				String logId,
				long countFrom,
				long countTo,
				boolean blind,
				long sentAtMs
		) {
			this.targetStation = targetStation;
			this.logId = logId;
			this.countFrom = countFrom;
			this.countTo = countTo;
			this.blind = blind;
			this.sentAtMs = sentAtMs;
		}

		private long getRequestedQsoCount() {
			return countTo - countFrom + 1L;
		}
	}

	/** Progress of the blind fallback for one log. */
	private static final class BlindScan {
		private long nextCountFrom = 1L;
		private int requestedBlockCount;
		private boolean completed;
	}
}
