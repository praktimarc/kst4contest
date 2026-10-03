package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import kst4contest.ApplicationConstants;
import kst4contest.model.ChatMessage;

class MessageBusRawRxLoggingTest {

    @Test
    @Timeout(5)
    void rawInboundFrameIsNotDuplicatedInDiagnosticLogging()
            throws InterruptedException {
        Logger logger = Logger.getLogger(
                MessageBusManagementThread.class.getName());
        Level previousLevel = logger.getLevel();
        boolean previousUseParentHandlers = logger.getUseParentHandlers();
        RecordingHandler handler = new RecordingHandler();

        logger.setLevel(Level.FINE);
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);

        try {
            LinkedBlockingQueue<ChatMessage> queue =
                    new LinkedBlockingQueue<>();
            MessageBusManagementThread thread =
                    new MessageBusManagementThread(
                            mock(ChatController.class),
                            mock(ThreadStatusCallback.class),
                            41L,
                            queue,
                            ignored -> true);

            queue.add(message("ZZ|sensitive raw payload|"));
            queue.add(poisonPill());

            thread.start();
            thread.join();

            assertFalse(handler.messages().stream()
                    .anyMatch(message -> message.startsWith("ON4KST RX:")));
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(previousUseParentHandlers);
            logger.setLevel(previousLevel);
        }
    }

    private ChatMessage message(final String text) {
        ChatMessage message = new ChatMessage();
        message.setMessageText(text);
        return message;
    }

    private ChatMessage poisonPill() {
        ChatMessage message = message(
                ApplicationConstants.DISCONNECT_RDR_POISONPILL);
        message.setMessageSenderName(
                ApplicationConstants.DISCONNECT_RDR_POISONPILL);
        return message;
    }

    private static final class RecordingHandler extends Handler {
        private final List<String> messages = new ArrayList<>();

        private RecordingHandler() {
            setLevel(Level.ALL);
        }

        @Override
        public void publish(final LogRecord record) {
            messages.add(record.getMessage());
        }

        @Override
        public void flush() {
            // No buffered output.
        }

        @Override
        public void close() {
            // No external resource.
        }

        private List<String> messages() {
            return List.copyOf(messages);
        }
    }
}
