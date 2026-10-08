package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import kst4contest.model.ChatMessage;

class On4KstHistoryIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    @Timeout(5)
    void readerRecordsInternalOkBeforeFilteringIt() throws Exception {
        try (MessageHistoryRecorder recorder = recorder();
             ServerSocket server = new ServerSocket(0)) {
            CompletableFuture<Void> serverDone = CompletableFuture.runAsync(() -> {
                try (Socket accepted = server.accept();
                     OutputStreamWriter out = new OutputStreamWriter(
                             accepted.getOutputStream(), StandardCharsets.UTF_8)) {
                    out.write("OK|\r\n");
                    out.flush();
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });

            try (Socket client = new Socket("127.0.0.1", server.getLocalPort())) {
                LinkedBlockingQueue<ChatMessage> messages =
                        new LinkedBlockingQueue<>();
                AtomicBoolean active = new AtomicBoolean(true);
                ReadThread reader = new ReadThread(
                        31L, client, messages, ignored -> active.get(),
                        ignored -> { }, ignored -> { }, recorder);
                reader.start();

                awaitHistoryLines(1);
                assertNull(messages.poll(200, TimeUnit.MILLISECONDS));
                active.set(false);
                reader.join(Duration.ofSeconds(2).toMillis());
            }
            serverDone.get(2, TimeUnit.SECONDS);
        }

        assertTrue(Files.readAllLines(historyFile()).get(0)
                .endsWith("\tRX\tOK|"));
    }

    @Test
    @Timeout(5)
    void writerRecordsOnlySuccessfullyFlushedFrames() throws Exception {
        try (MessageHistoryRecorder recorder = recorder();
             ServerSocket server = new ServerSocket(0)) {
            CompletableFuture<String> received = CompletableFuture.supplyAsync(() -> {
                try (Socket accepted = server.accept()) {
                    return new String(
                            accepted.getInputStream().readNBytes(18),
                            StandardCharsets.UTF_8);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });

            try (Socket client = new Socket("127.0.0.1", server.getLocalPort())) {
                LinkedBlockingQueue<ChatMessage> queue = new LinkedBlockingQueue<>();
                AtomicBoolean active = new AtomicBoolean(true);
                WriteThread writer = new WriteThread(
                        32L, client, queue, 2, ignored -> active.get(),
                        ignored -> { }, ignored -> { }, recorder);
                writer.start();
                queue.add(chatMessage("hello"));

                assertEquals("MSG|2|0|hello|0|\r\n",
                        received.get(2, TimeUnit.SECONDS));
                awaitHistoryLines(1);
                active.set(false);
                writer.interrupt();
                writer.join(Duration.ofSeconds(2).toMillis());
            }
        }

        assertTrue(Files.readAllLines(historyFile()).get(0)
                .endsWith("\tTX\tMSG|2|0|hello|0|"));
    }

    @Test
    @Timeout(5)
    void rejectedAndFailedWritesAreNotRecordedAsTx() throws Exception {
        try (MessageHistoryRecorder recorder = recorder()) {
            LinkedBlockingQueue<ChatMessage> rejectedQueue =
                    new LinkedBlockingQueue<>();
            AtomicBoolean rejectedActive = new AtomicBoolean(true);
            CompletableFuture<String> rejection = new CompletableFuture<>();
            try (Socket socket = new FlushFailingSocket(false)) {
                WriteThread writer = new WriteThread(
                        33L, socket, rejectedQueue, 2,
                        ignored -> rejectedActive.get(), ignored -> { },
                        rejection::complete, recorder);
                writer.start();
                rejectedQueue.add(serverFrame("BROKEN\rFRAME"));
                rejection.get(2, TimeUnit.SECONDS);
                rejectedActive.set(false);
                writer.interrupt();
                writer.join(Duration.ofSeconds(2).toMillis());
            }

            LinkedBlockingQueue<ChatMessage> failedQueue =
                    new LinkedBlockingQueue<>();
            CompletableFuture<Throwable> failure = new CompletableFuture<>();
            try (Socket socket = new FlushFailingSocket(true)) {
                WriteThread writer = new WriteThread(
                        34L, socket, failedQueue, 2,
                        ignored -> true, failure::complete,
                        ignored -> { }, recorder);
                writer.start();
                failedQueue.add(chatMessage("not-sent"));
                failure.get(2, TimeUnit.SECONDS);
                writer.join(Duration.ofSeconds(2).toMillis());
            }
        }

        assertTrue(Files.exists(historyFile()));
        assertTrue(Files.readAllLines(historyFile()).isEmpty());
    }

    private MessageHistoryRecorder recorder() {
        return new MessageHistoryRecorder(
                temporaryDirectory,
                true,
                Clock.fixed(
                        Instant.parse("2026-10-03T10:15:27.384Z"),
                        ZoneOffset.UTC));
    }

    private Path historyFile() {
        return temporaryDirectory.resolve("Messagehistory.raw");
    }

    private void awaitHistoryLines(int expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (Files.exists(historyFile())
                    && Files.readAllLines(historyFile()).size() >= expected) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(10);
        }
        assertEquals(expected,
                Files.exists(historyFile())
                        ? Files.readAllLines(historyFile()).size() : 0);
    }

    private ChatMessage chatMessage(String text) {
        ChatMessage message = new ChatMessage();
        message.setMessageText(text);
        return message;
    }

    private ChatMessage serverFrame(String text) {
        ChatMessage message = new ChatMessage();
        message.setMessageDirectedToServer(true);
        message.setMessageText(text);
        return message;
    }

    private static final class FlushFailingSocket extends Socket {
        private final boolean failOnFlush;

        private FlushFailingSocket(boolean failOnFlush) {
            this.failOnFlush = failOnFlush;
        }

        @Override
        public OutputStream getOutputStream() {
            return new OutputStream() {
                @Override
                public void write(int value) {
                    // The test socket intentionally discards all bytes.
                }

                @Override
                public void flush() throws IOException {
                    if (failOnFlush) {
                        throw new IOException("simulated flush failure");
                    }
                }
            };
        }
    }
}
