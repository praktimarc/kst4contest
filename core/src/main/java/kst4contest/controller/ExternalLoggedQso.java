package kst4contest.controller;

import java.util.Optional;

import kst4contest.model.ChatMember;

/**
 * Validated QSO state shared by external logger listeners and the controller.
 */
public final class ExternalLoggedQso {
	private final String callSign;
	private final LoggedQsoBand band;
	private final String locator;
	private final String source;

	private ExternalLoggedQso(
			String callSign,
			LoggedQsoBand band,
			String locator,
			String source
	) {
		this.callSign = callSign;
		this.band = band;
		this.locator = locator;
		this.source = source;
	}

	/**
	 * Creates a QSO only when the logger supplied a usable callsign. Missing band
	 * and locator values remain unavailable and never receive guessed defaults.
	 *
	 * @param rawCallSign callsign received from the logger
	 * @param band normalized band or {@code null}
	 * @param rawLocator locator received from the logger or {@code null}
	 * @param source logger name used for persistence diagnostics
	 * @return validated QSO, or an empty value for a missing callsign
	 */
	public static Optional<ExternalLoggedQso> create(
			String rawCallSign,
			LoggedQsoBand band,
			String rawLocator,
			String source
	) {
		if (rawCallSign == null) {
			return Optional.empty();
		}

		String normalizedCallSign = rawCallSign.trim();
		if (normalizedCallSign.isEmpty()) {
			return Optional.empty();
		}
		String normalizedLocator = WorkedGrossFieldCache.extractLocator6(rawLocator);
		return Optional.of(new ExternalLoggedQso(
				normalizedCallSign,
				band,
				normalizedLocator,
				source
		));
	}

	public String getCallSign() {
		return callSign;
	}

	public LoggedQsoBand getBand() {
		return band;
	}

	public String getLocator() {
		return locator;
	}

	public String getSource() {
		return source;
	}

	/**
	 * Builds the database representation of this QSO.
	 *
	 * @return chat-member state with global and optional band Worked flags
	 */
	public ChatMember toWorkedChatMember() {
		ChatMember workedCall = new ChatMember();
		workedCall.setCallSign(callSign);
		workedCall.setWorked(true);
		if (band != null) {
			band.applyWorkedFlag(workedCall);
		}
		if (locator != null) {
			workedCall.setQra(locator);
		}
		return workedCall;
	}

	/**
	 * Applies this QSO to one active callsign variant.
	 *
	 * @param member active variant with the same base callsign
	 */
	public void applyToActiveMember(ChatMember member) {
		if (member == null) {
			return;
		}

		member.setWorked(true);
		if (band != null) {
			band.applyWorkedFlag(member);
		}
		if (locator != null
				&& (member.getQra() == null
				|| member.getQra().isBlank()
				|| "unknown".equalsIgnoreCase(member.getQra()))) {
			member.setQra(locator);
		}
	}
}
