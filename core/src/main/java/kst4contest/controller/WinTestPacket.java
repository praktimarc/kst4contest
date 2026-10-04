package kst4contest.controller;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Win-Test network packet received over UDP.
 *
 * <p>This is the receiving counterpart of {@link WinTestMessage} and follows the
 * same framing:</p>
 *
 * <pre>
 *   MESSAGETYPE: "src" "dst" data{checksum}\0
 * </pre>
 *
 * <p>The checksum byte always has bit 7 set and is therefore not valid ASCII.
 * Decoding the datagram as text before removing it turns the byte into a
 * replacement character that sticks to the last data field. That is harmless
 * for fields KST4Contest never reads, but the log synchronization needs exactly
 * those trailing fields: the log ID of an {@code ADDQSO} packet and the
 * run-length inventory of an {@code IHAVE} packet. The framing is therefore
 * resolved on the raw bytes here, once, before any text parsing.</p>
 */
public final class WinTestPacket {

	/** Quoted values stay one token, unquoted values are split at whitespace. */
	private static final Pattern TOKEN_PATTERN = Pattern.compile("\"([^\"]*)\"|(\\S+)");

	private final String messageType;
	private final String source;
	private final String destination;
	private final String messageText;
	private final List<String> dataTokens;
	private final boolean checksumPresent;
	private final boolean checksumValid;

	private WinTestPacket(
			String messageType,
			String source,
			String destination,
			String messageText,
			List<String> dataTokens,
			boolean checksumPresent,
			boolean checksumValid
	) {
		this.messageType = messageType;
		this.source = source;
		this.destination = destination;
		this.messageText = messageText;
		this.dataTokens = dataTokens;
		this.checksumPresent = checksumPresent;
		this.checksumValid = checksumValid;
	}

	/**
	 * Builds a packet from a received datagram.
	 *
	 * <p>Trailing NUL bytes are removed first. If the resulting last byte has
	 * bit 7 set it is the Win-Test checksum: it is verified against the sum of
	 * all preceding bytes and removed before the message text is decoded.</p>
	 *
	 * @param datagram raw datagram buffer
	 * @param length number of valid bytes in the buffer
	 * @return parsed packet, or {@code null} when the datagram carries no message
	 */
	public static WinTestPacket fromDatagram(byte[] datagram, int length) {
		if (datagram == null || length <= 0 || length > datagram.length) {
			return null;
		}

		int endIndex = length;
		while (endIndex > 0 && datagram[endIndex - 1] == 0) {
			endIndex--;
		}

		if (endIndex == 0) {
			return null;
		}

		boolean hasChecksum = (datagram[endIndex - 1] & 0x80) != 0;
		boolean isChecksumValid = false;
		int textEndIndex = endIndex;

		if (hasChecksum) {
			int sum = 0;
			for (int index = 0; index < endIndex - 1; index++) {
				sum += datagram[index] & 0xFF;
			}
			byte expectedChecksum = (byte) ((sum | 0x80) & 0xFF);
			isChecksumValid = expectedChecksum == datagram[endIndex - 1];
			textEndIndex = endIndex - 1;
		}

		String text = new String(datagram, 0, textEndIndex, StandardCharsets.US_ASCII);
		return fromMessageText(text, hasChecksum, isChecksumValid);
	}

	/**
	 * Builds a packet from an already decoded message text without checksum
	 * information. Used for messages that reach the listener as text.
	 *
	 * @param messageText complete message text
	 * @return parsed packet, or {@code null} for an unusable message
	 */
	public static WinTestPacket fromMessageText(String messageText) {
		return fromMessageText(messageText, false, false);
	}

	private static WinTestPacket fromMessageText(
			String rawMessageText,
			boolean checksumPresent,
			boolean checksumValid
	) {
		if (rawMessageText == null) {
			return null;
		}

		String text = rawMessageText.trim();
		int typeEndIndex = text.indexOf(": ");
		if (typeEndIndex <= 0) {
			return null;
		}

		String type = text.substring(0, typeEndIndex);
		List<String> tokens = tokenize(text.substring(typeEndIndex + 2));

		String packetSource = tokens.isEmpty() ? "" : tokens.get(0);
		String packetDestination = tokens.size() > 1 ? tokens.get(1) : "";
		List<String> data = tokens.size() > 2
				? new ArrayList<>(tokens.subList(2, tokens.size()))
				: new ArrayList<>();

		return new WinTestPacket(
				type,
				packetSource,
				packetDestination,
				text,
				data,
				checksumPresent,
				checksumValid
		);
	}

	/**
	 * Splits Win-Test payload text into fields. Quoted values are kept together
	 * and empty quoted values are preserved, so field positions stay stable.
	 *
	 * @param text payload text
	 * @return field values without their surrounding quotes
	 */
	static List<String> tokenize(String text) {
		List<String> tokens = new ArrayList<>();
		if (text == null) {
			return tokens;
		}

		Matcher matcher = TOKEN_PATTERN.matcher(text);
		while (matcher.find()) {
			tokens.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
		}
		return tokens;
	}

	/**
	 * @return message type such as {@code ADDQSO}, never {@code null}
	 */
	public String getMessageType() {
		return messageType;
	}

	/**
	 * @return Win-Test station that sent the packet
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @return addressed Win-Test station, empty for a broadcast
	 */
	public String getDestination() {
		return destination;
	}

	/**
	 * @return complete message text without checksum byte and NUL terminator
	 */
	public String getMessageText() {
		return messageText;
	}

	/**
	 * @return payload fields following source and destination
	 */
	public List<String> getDataTokens() {
		return Collections.unmodifiableList(dataTokens);
	}

	/**
	 * @param index payload field position
	 * @return field value, or {@code null} when the field is missing
	 */
	public String getDataToken(int index) {
		return index >= 0 && index < dataTokens.size() ? dataTokens.get(index) : null;
	}

	public boolean isChecksumPresent() {
		return checksumPresent;
	}

	public boolean isChecksumValid() {
		return checksumValid;
	}

	/**
	 * Checks whether this packet is meant for us.
	 *
	 * @param ownStationName own Win-Test station name
	 * @return {@code true} for a broadcast or for a packet addressed to us
	 */
	public boolean isAddressedTo(String ownStationName) {
		if (destination == null || destination.isEmpty()) {
			return true;
		}
		return ownStationName != null && destination.equalsIgnoreCase(ownStationName.trim());
	}

	@Override
	public String toString() {
		return messageType + ": src=" + source + " dst=" + destination
				+ " fields=" + dataTokens.size()
				+ (checksumPresent ? (checksumValid ? " checksum=ok" : " checksum=bad") : " checksum=none");
	}
}
