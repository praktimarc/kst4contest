package kst4contest.controller;

/**
 * Consecutive range of Win-Test QSO numbers inside one log.
 *
 * <p>Win-Test numbers the QSOs of every log continuously. The {@code IHAVE}
 * inventory of a log is therefore expressed as a list of ranges that are
 * present in that log. A range is inclusive on both ends.</p>
 */
public final class WinTestLogSegment {

	private final long countFrom;
	private final long countTo;

	/**
	 * @param countFrom first QSO number of the range
	 * @param countTo last QSO number of the range
	 */
	public WinTestLogSegment(long countFrom, long countTo) {
		this.countFrom = countFrom;
		this.countTo = countTo;
	}

	public long getCountFrom() {
		return countFrom;
	}

	public long getCountTo() {
		return countTo;
	}

	/**
	 * @return number of QSOs covered by this range, never negative
	 */
	public long getCount() {
		return countTo < countFrom ? 0L : countTo - countFrom + 1L;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WinTestLogSegment)) {
			return false;
		}
		WinTestLogSegment otherSegment = (WinTestLogSegment) other;
		return countFrom == otherSegment.countFrom && countTo == otherSegment.countTo;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(countFrom) * 31 + Long.hashCode(countTo);
	}

	@Override
	public String toString() {
		return countFrom + "-" + countTo;
	}
}
