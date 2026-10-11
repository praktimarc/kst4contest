package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import kst4contest.ApplicationConstants;
import kst4contest.model.AirPlaneReflectionInfo;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatMessage;
import kst4contest.model.ChatPreferences;
import kst4contest.utils.PlayAudioUtils;

/**
 * Drives the autoanswer cooldown of a running {@link MessageBusManagementThread}
 * with an injected clock instead of waiting two minutes of wall-clock time.
 *
 * <p>The clock is advanced by a marker frame ({@code UE}) placed between the
 * questions: the thread processes frames strictly in order, and the stubbed
 * completion callback of the marker moves the clock before the next question is
 * taken from the queue.</p>
 */
class MessageBusManagementThreadAutoAnswerCooldownTest {

    @TempDir
    Path tempDir;

    @Test
    @Timeout(10)
    void questionIsAnsweredAgainOnceTheCooldownHasElapsed() throws InterruptedException {
        AtomicLong clock = new AtomicLong(1_700_000_000_000L);
        LinkedBlockingQueue<ChatMessage> txBus = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<ChatMessage> rxQueue = new LinkedBlockingQueue<>();
        ChatController controller = controller(txBus);

        // Each marker frame advances the clock by one step before the next question.
        long[] steps = {
                MessageBusManagementThread.AUTOANSWER_COOLDOWN_MS - 1,   // still closed
                1L,                                                       // now exactly elapsed
                MessageBusManagementThread.AUTOANSWER_COOLDOWN_MS         // elapsed again
        };
        AtomicLong markerIndex = new AtomicLong();
        when(controller.getChatCategoryMain()).thenReturn(new ChatCategory(ChatCategory.VUHF));
        org.mockito.Mockito.doAnswer(invocation -> {
            clock.addAndGet(steps[(int) markerIndex.getAndIncrement()]);
            return null;
        }).when(controller).onOn4KstInitialUserListCompleted(
                org.mockito.ArgumentMatchers.anyLong(), any());

        MessageBusManagementThread thread = new MessageBusManagementThread(
                controller, mock(ThreadStatusCallback.class), 9L, rxQueue,
                ignored -> true, clock::get);

        rxQueue.add(question());            // t0: answered
        rxQueue.add(marker());              // +119_999 ms
        rxQueue.add(question());            // t0 + 119_999: suppressed
        rxQueue.add(marker());              // +1 ms
        rxQueue.add(question());            // t0 + 120_000: answered
        rxQueue.add(marker());              // +120_000 ms
        rxQueue.add(question());            // t0 + 240_000: answered
        rxQueue.add(poisonPill());

        thread.start();
        thread.join();

        assertEquals(3, txBus.size(), "first, third and fourth question are answered");
    }

    private ChatController controller(LinkedBlockingQueue<ChatMessage> txBus) {
        ChatPreferences prefs = new ChatPreferences(tempDir.resolve("preferences.xml"));
        prefs.setStn_loginCallSign("DN9APW");
        prefs.setMessageHandling_autoAnswerToQRGRequestEnabled(true);
        prefs.setMYQRGFirstCat("144.300");

        ChatController controller = mock(ChatController.class);
        when(controller.getChatPreferences()).thenReturn(prefs);
        when(controller.getMessageTXBus()).thenReturn(txBus);
        when(controller.getChatCategorySecondChat())
                .thenReturn(new ChatCategory(ChatCategory.MICROWAVE));
        when(controller.getPlayAudioUtils()).thenReturn(mock(PlayAudioUtils.class));
        when(controller.getStationMetricsService())
                .thenReturn(mock(StationMetricsService.class));
        when(controller.getScoreService()).thenReturn(mock(ScoreService.class));
        when(controller.findActiveChatMember(any(ChatMember.class)))
                .thenAnswer(invocation -> {
                    ChatMember lookup = invocation.getArgument(0);
                    ChatMember member = new ChatMember();
                    member.setCallSign(lookup.getCallSign());
                    member.setChatCategory(lookup.getChatCategory());
                    member.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());
                    return member;
                });
        return controller;
    }

    private static ChatMessage question() {
        return message("CH|2|1700000000|DL1ABC|Hans|0|ur qrg?|DN9APW|");
    }

    private static ChatMessage marker() {
        return message("UE|2|22562|");
    }

    private static ChatMessage message(String text) {
        ChatMessage message = new ChatMessage();
        message.setMessageText(text);
        return message;
    }

    private static ChatMessage poisonPill() {
        ChatMessage message = message(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
        message.setMessageSenderName(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
        return message;
    }
}
