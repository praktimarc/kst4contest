package kst4contest.controller;

import kst4contest.ApplicationConstants;
import kst4contest.controller.MessageBusManagementThread.DirectionalOpportunity;
import org.junit.jupiter.api.Test;

import static kst4contest.controller.ChatRoute.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatFrameDecisionTest {

    @Test
    void frameFieldsAreReadFromTheTraditionalIndices() {
        InboundChatFrame frame = InboundChatFrame.of(
                "CH|2|1700000000|DL1ABC|Hans|0|qrv 144.300|DN9APW|".split("\\|"));

        assertEquals(new InboundChatFrame("2", "1700000000", "DL1ABC", "Hans", "qrv 144.300", "DN9APW"), frame);
    }

    @Test
    void receiverZeroIsPublic() {
        assertEquals(PUBLIC, MessageBusManagementThread.classifyRoute("0", "DL1ABC", null, "DN9APW"));
    }

    @Test
    void toMeComparesCaseSensitively() {
        assertEquals(TO_ME, MessageBusManagementThread.classifyRoute("DN9APW", "DL1ABC", "DN9APW", "DN9APW"));
        assertEquals(BETWEEN_OTHERS, MessageBusManagementThread.classifyRoute("dn9apw", "DL1ABC", "dn9apw", "DN9APW"));
    }

    @Test
    void fromMeComparesCaseInsensitively() {
        assertEquals(FROM_ME, MessageBusManagementThread.classifyRoute("DL1ABC", "dn9apw", "DL1ABC", "DN9APW"));
    }

    @Test
    void toMeWinsOverFromMe() {
        assertEquals(TO_ME, MessageBusManagementThread.classifyRoute("DN9APW", "DN9APW", "DN9APW", "DN9APW"));
    }

    @Test
    void anythingElseIsBetweenOthers() {
        assertEquals(BETWEEN_OTHERS, MessageBusManagementThread.classifyRoute("DL2XYZ", "DL1ABC", "DL2XYZ", "DN9APW"));
    }

    private static final MessageBusManagementThread.ChatFrameSettings ALL_ON =
            new MessageBusManagementThread.ChatFrameSettings(true, true, true, true, "/CQ DL1ABC [auto] QRG is: 144.300");

    @Test
    void publicAndFromMeDoNothing() {
        for (boolean live : new boolean[] { true, false }) {
            assertEquals(ChatMessageEffects.NONE, MessageBusManagementThread.decideChatFrameEffects(PUBLIC, live, "//ver", null, false, ALL_ON));
            assertEquals(ChatMessageEffects.NONE, MessageBusManagementThread.decideChatFrameEffects(FROM_ME, live, "//ver", null, false, ALL_ON));
        }
    }

    @Test
    void liveMessageToMeRingsAnswersAndReplies() {
        ChatMessageEffects effects = MessageBusManagementThread.decideChatFrameEffects(TO_ME, true, "pse //Bell //ver", null, false, ALL_ON);

        assertEquals(new ChatMessageEffects(true, true, true, true, true, "/CQ DL1ABC [auto] QRG is: 144.300", null, false, false), effects);
    }

    @Test
    void bellNeedsSimpleSounds() {
        var noSounds = new MessageBusManagementThread.ChatFrameSettings(false, false, false, false, null);
        ChatMessageEffects effects = MessageBusManagementThread.decideChatFrameEffects(TO_ME, true, "//bell", null, false, noSounds);

        assertEquals(false, effects.playBell());
        assertEquals(false, effects.playPmSound());
    }

    @Test
    void anAutomaticMessageIsNeverAnswered() {
        String auto = ApplicationConstants.AUTOANSWER_PREFIX + " QRG is: 432.100";
        assertEquals(null, MessageBusManagementThread.decideChatFrameEffects(TO_ME, true, auto, null, false, ALL_ON).autoAnswerText());
    }

    @Test
    void historyMessageToMeDoesNothing() {
        assertEquals(ChatMessageEffects.NONE, MessageBusManagementThread.decideChatFrameEffects(TO_ME, false, "//bell //ver", null, false, ALL_ON));
    }

    @Test
    void liveOpportunityTicksOnceAndSpots() {
        ChatMessageEffects first = MessageBusManagementThread.decideChatFrameEffects(
                BETWEEN_OTHERS, true, "", DirectionalOpportunity.IN_ANGLE_AND_RANGE, false, ALL_ON);
        ChatMessageEffects again = MessageBusManagementThread.decideChatFrameEffects(
                BETWEEN_OTHERS, true, "", DirectionalOpportunity.IN_ANGLE_AND_RANGE, true, ALL_ON);

        assertEquals(Boolean.TRUE, first.inAngleAndRange());
        assertEquals(true, first.playDirectionalTick());
        assertEquals(true, first.evaluateDxClusterSpot());
        assertEquals(false, again.playDirectionalTick());
    }

    @Test
    void historyOpportunityOnlySetsTheFlag() {
        ChatMessageEffects effects = MessageBusManagementThread.decideChatFrameEffects(
                BETWEEN_OTHERS, false, "", DirectionalOpportunity.IN_ANGLE_AND_RANGE, false, ALL_ON);

        assertEquals(new ChatMessageEffects(false, false, false, false, false, null, Boolean.TRUE, false, false), effects);
    }

    @Test
    void outOfAngleOrMissingLocatorClearsTheFlag() {
        for (DirectionalOpportunity o : new DirectionalOpportunity[] {
                DirectionalOpportunity.OUT_OF_ANGLE_OR_RANGE, DirectionalOpportunity.MISSING_LOCATOR }) {
            ChatMessageEffects effects = MessageBusManagementThread.decideChatFrameEffects(BETWEEN_OTHERS, true, "", o, true, ALL_ON);
            assertEquals(Boolean.FALSE, effects.inAngleAndRange());
            assertEquals(false, effects.playDirectionalTick());
            assertEquals(false, effects.evaluateDxClusterSpot());
        }
    }
}
