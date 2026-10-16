package kst4contest.controller;

/**
 * Side effects a chat frame asks for, decided without touching sound, network or model.
 *
 * @param playPmSound whether the private message sound ('P') is played
 * @param playCwCallsign whether the sender's call is sent in CW
 * @param playVoiceCallsign whether the sender's call is spoken
 * @param playBell whether the voice bell ("!") is played
 * @param sendVersionReply whether the version reply is sent
 * @param autoAnswerText the auto answer to send, {@code null} for none; the per-remote cooldown is still checked by the executor
 * @param inAngleAndRange the new in-angle flag of the sender, {@code null} to leave it unchanged
 * @param playDirectionalTick whether the short directional tick is played
 * @param evaluateDxClusterSpot whether a DX Cluster spot is to be evaluated
 */
record ChatMessageEffects(
        boolean playPmSound,
        boolean playCwCallsign,
        boolean playVoiceCallsign,
        boolean playBell,
        boolean sendVersionReply,
        String autoAnswerText,
        Boolean inAngleAndRange,
        boolean playDirectionalTick,
        boolean evaluateDxClusterSpot) {

    /** No effect at all. */
    static final ChatMessageEffects NONE =
            new ChatMessageEffects(false, false, false, false, false, null, null, false, false);
}
