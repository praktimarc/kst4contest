package kst4contest.controller;

/**
 * The fields of an ON4KST chat frame ({@code CH} live, {@code CR} history), as split on
 * {@code |}: {@code opcode|category|epoch|senderCall|senderName|0|text|receiver|}.
 *
 * <p>Read exactly as the frame branches always did: no trimming and no validation, so a
 * short frame fails where it failed before.</p>
 */
record InboundChatFrame(
        String categoryNumber,
        String epoch,
        String senderCall,
        String senderName,
        String text,
        String receiverField) {

    static InboundChatFrame of(String[] fields) {
        return new InboundChatFrame(fields[1], fields[2], fields[3], fields[4], fields[6], fields[7]);
    }
}
