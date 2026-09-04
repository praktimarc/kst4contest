package kst4contest.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Inventory of one Win-Test log, transported in an {@code IHAVE} packet.
 *
 * <p>Win-Test announces which QSO numbers of a log a station currently holds.
 * To keep the packet short the inventory is run-length encoded:</p>
 *
 * <pre>
 *   IHAVE: "Shack" "" "Shack@9" E 1 1 911-1-117
 *                     ^logId    ^  ^ ^ ^run lengths
 *                            origin | initial state
 *                                   first row
 * </pre>
 *
 * <p>The run lengths alternate between present and missing QSOs, starting with
 * the state given by {@code InitialState} at QSO number {@code FirstRow}. The
 * example above therefore means: QSOs 1 to 911 are present, QSO 912 is missing
 * and QSOs 913 to 1029 are present again.</p>
 *
 * <p>Unlike the wtKST implementation this parser honours {@code FirstRow}
 * instead of assuming that every inventory starts at QSO number one. Win-Test
 * splits long inventories into several packets, and a split inventory starts at
 * a higher first row.</p>
 */
public final class WinTestIhaveInventory {

	/** Where the sending station got the log from. */
	public enum Origin {
		/** The station owns the log or the operator is logged on there. */
		OWNER,
		/** The station only mirrors a log owned by somebody else. */
		LOGGED_ELSE
	}

	/** Protects against endless loops caused by a corrupted run-length chain. */
	private static final int MAX_SEGMENTS = 512;

	private static final int EXPECTED_FIELD_COUNT = 5;

	private final String logId;
	private final Origin origin;
	private final List<WinTestLogSegment> segments;

	private WinTestIhaveInventory(String logId, Origin origin, List<WinTestLogSegment> segments) {
		this.logId = logId;
		this.origin = origin;
		this.segments = segments;
	}

	/**
	 * Parses an {@code IHAVE} packet.
	 *
	 * @param packet received packet
	 * @return inventory, or an empty value when the packet is not a usable
	 *         {@code IHAVE} announcement
	 */
	public static Optional<WinTestIhaveInventory> fromPacket(WinTestPacket packet) {
		if (packet == null || !"IHAVE".equals(packet.getMessageType())) {
			return Optional.empty();
		}

		List<String> fields = packet.getDataTokens();
		if (fields.size() != EXPECTED_FIELD_COUNT) {
			/*
			 * Win-Test versions before 1.29 use a shorter IHAVE format without
			 * run-length encoding. It carries no usable range information, so
			 * the blind fallback of the sync service has to take over.
			 */
			return Optional.empty();
		}

		String parsedLogId = fields.get(0) == null ? "" : fields.get(0).trim();
		if (parsedLogId.isEmpty()) {
			return Optional.empty();
		}

		Origin parsedOrigin = parseOrigin(fields.get(1));

		long firstRow = parseUnsignedValue(fields.get(2));
		long initialState = parseUnsignedValue(fields.get(3));

		if (firstRow < 1L || initialState < 0L || initialState > 1L) {
			return Optional.empty();
		}

		List<WinTestLogSegment> parsedSegments =
				parseRunLengths(fields.get(4), firstRow, initialState == 1L);

		if (parsedSegments == null) {
			return Optional.empty();
		}

		return Optional.of(new WinTestIhaveInventory(parsedLogId, parsedOrigin, parsedSegments));
	}

	private static Origin parseOrigin(String rawOrigin) {
		if (rawOrigin == null) {
			return Origin.OWNER;
		}

		String normalizedOrigin = rawOrigin.trim().toUpperCase(java.util.Locale.ROOT);
		if ("E".equals(normalizedOrigin) || "LOGGEDELSE".equals(normalizedOrigin)) {
			return Origin.LOGGED_ELSE;
		}
		return Origin.OWNER;
	}

	/**
	 * Expands the hyphen-separated run lengths into ranges.
	 *
	 * @param rawRunLengths run-length chain such as {@code 911-1-117}
	 * @param firstRow QSO number the first run starts at
	 * @param startsPresent {@code true} when the first run describes present QSOs
	 * @return ranges of present QSOs, or {@code null} for an unusable chain
	 */
	private static List<WinTestLogSegment> parseRunLengths(
			String rawRunLengths,
			long firstRow,
			boolean startsPresent
	) {
		if (rawRunLengths == null || rawRunLengths.isBlank()) {
			return null;
		}

		String[] runLengths = rawRunLengths.trim().split("-");

		/*
		 * A chain that starts with present QSOs has to end with a present run,
		 * so its length is odd. A chain that starts with missing QSOs needs an
		 * even length for the same reason.
		 */
		if (startsPresent) {
			if (runLengths.length % 2 == 0) {
				return null;
			}
		} else if (runLengths.length % 2 == 1 || runLengths.length < 2) {
			return null;
		}

		List<WinTestLogSegment> parsedSegments = new ArrayList<>();
		long cursor = firstRow;
		boolean present = startsPresent;

		for (String runLength : runLengths) {
			long count = parseUnsignedValue(runLength);
			if (count < 0L) {
				return null;
			}

			if (present && count > 0L) {
				if (parsedSegments.size() >= MAX_SEGMENTS) {
					return null;
				}
				parsedSegments.add(new WinTestLogSegment(cursor, cursor + count - 1L));
			}

			cursor += count;
			present = !present;
		}

		return parsedSegments;
	}

	private static long parseUnsignedValue(String rawValue) {
		if (rawValue == null) {
			return -1L;
		}

		try {
			return Long.parseLong(rawValue.trim());
		} catch (NumberFormatException exception) {
			return -1L;
		}
	}

	/**
	 * @return log identity in the form {@code StationName@LogUniqueID}
	 */
	public String getLogId() {
		return logId;
	}

	public Origin getOrigin() {
		return origin;
	}

	/**
	 * @return ranges of QSO numbers the announcing station holds
	 */
	public List<WinTestLogSegment> getSegments() {
		return Collections.unmodifiableList(segments);
	}

	/**
	 * @return highest announced QSO number, or {@code 0} for an empty inventory
	 */
	public long getHighestQsoNumber() {
		long highestQsoNumber = 0L;
		for (WinTestLogSegment segment : segments) {
			if (segment.getCountTo() > highestQsoNumber) {
				highestQsoNumber = segment.getCountTo();
			}
		}
		return highestQsoNumber;
	}

	@Override
	public String toString() {
		return logId + " " + origin + " " + segments;
	}
}
