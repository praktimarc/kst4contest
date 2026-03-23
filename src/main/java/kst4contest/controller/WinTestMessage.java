package kst4contest.controller;

import java.nio.charset.StandardCharsets;

/**
 * Represents a Win-Test network protocol message.
 * <p>
 * Ported from the C# wtMessage class in wtKST.
 * <p>
 * Win-Test uses a simple ASCII-based UDP protocol with a checksum byte.
 * Message format (for sending):
 * <pre>
 *   MESSAGETYPE: "src" "dst" data{checksum}\0
 * </pre>
 * The checksum is calculated over all bytes before the checksum position,
 * then OR'd with 0x80.
 */
public class WinTestMessage {

    /** Win-Test message types relevant for SKED management. */
    public enum MessageType {
        LOCKSKED,
        UNLOCKSKED,
        ADDSKED,
        DELETESKED,
        UPDATESKED
    }

    private final MessageType type;
    private final String src;
    private final String dst;
    private final String data;

    public WinTestMessage(MessageType type, String src, String dst, String data) {
        this.type = type;
        this.src = src;
        this.dst = dst;
        this.data = data;
    }

    /**
     * Serializes this message to bytes for UDP transmission.
     * <p>
     * Format: {@code MESSAGETYPE: "src" "dst" data{checksum}\0}
     * <p>
     * The '?' placeholder is replaced by the calculated checksum,
     * followed by a NUL terminator.
     * Degree signs (°) are escaped as \260 per Win-Test convention.
     */
    public byte[] toBytes() {
        String escapedData = data.replace("°", "\\260");

        // Format: MESSAGETYPE: "src" "dst" data?\0
        // The '?' is a placeholder for the checksum byte
        String raw = type.name() + ": \"" + src + "\" \"" + dst + "\" " + escapedData + "?\0";

        byte[] bytes = raw.getBytes(StandardCharsets.US_ASCII);

        // Calculate checksum over everything before the checksum position (length - 2)
        int sum = 0;
        for (int i = 0; i < bytes.length - 2; i++) {
            sum += (bytes[i] & 0xFF);
        }
        byte checksum = (byte) ((sum | 0x80) & 0xFF);
        bytes[bytes.length - 2] = checksum;

        return bytes;
    }

    // Getters for debugging/logging
    public MessageType getType() { return type; }
    public String getSrc() { return src; }
    public String getDst() { return dst; }
    public String getData() { return data; }

    @Override
    public String toString() {
        return type.name() + ": src=" + src + " dst=" + dst + " data=" + data;
    }
}
