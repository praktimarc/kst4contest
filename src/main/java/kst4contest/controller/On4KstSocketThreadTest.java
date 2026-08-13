package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import kst4contest.model.ChatMessage;

class On4KstSocketThreadTest {
    @Test
    @Timeout(5)
    void eofIsReportedImmediately() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            CompletableFuture<Void> serverDone = CompletableFuture.runAsync(() -> {
                try (Socket accepted = server.accept();
                     OutputStreamWriter out = new OutputStreamWriter(
                             accepted.getOutputStream(), StandardCharsets.UTF_8)) {
                    out.write("CK|\r\n");
                    out.flush();
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });

            try (Socket client = new Socket("127.0.0.1", server.getLocalPort())) {
                LinkedBlockingQueue<ChatMessage> queue = new LinkedBlockingQueue<>();
                AtomicBoolean active = new AtomicBoolean(true);
                CompletableFuture<Throwable> failure = new CompletableFuture<>();
                ReadThread reader = new ReadThread(
                        7L, client, queue, ignored -> active.get(), ignored -> { },
                        failure::complete);
                reader.start();

                assertEquals("CK|", queue.poll(2, TimeUnit.SECONDS).getMessageText());
                failure.get(2, TimeUnit.SECONDS);
                active.set(false);
                reader.join(Duration.ofSeconds(2).toMillis());
            }
            serverDone.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    @Timeout(5)
    void writerUsesOneExactCrLfPerFrameIncludingHeartbeat() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            CompletableFuture<String> firstLine = new CompletableFuture<>();
            CompletableFuture<String> secondLine = new CompletableFuture<>();
            CompletableFuture<Void> serverDone = CompletableFuture.runAsync(() -> {
                try (Socket accepted = server.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(
                             accepted.getInputStream(), StandardCharsets.UTF_8))) {
                    firstLine.complete(in.readLine());
                    secondLine.complete(in.readLine());
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });

            try (Socket client = new Socket("127.0.0.1", server.getLocalPort())) {
                LinkedBlockingQueue<ChatMessage> queue = new LinkedBlockingQueue<>();
                AtomicBoolean active = new AtomicBoolean(true);
                WriteThread writer = new WriteThread(
                        11L, client, queue, 2, ignored -> active.get(),
                        ignored -> { }, ignored -> { });
                writer.start();

                queue.add(serverFrame(""));
                queue.add(serverFrame("SDONE|2|\r"));
                assertEquals("", firstLine.get(2, TimeUnit.SECONDS));
                assertEquals("SDONE|2|", secondLine.get(2, TimeUnit.SECONDS));

                active.set(false);
                writer.interrupt();
                writer.join(Duration.ofSeconds(2).toMillis());
            }
            serverDone.get(2, TimeUnit.SECONDS);
        }
    }

    private ChatMessage serverFrame(String text) {
        ChatMessage message = new ChatMessage();
        message.setMessageDirectedToServer(true);
        message.setMessageText(text);
        return message;
    }
}