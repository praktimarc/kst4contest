package kst4contest.controller;

import static kst4contest.testing.Await.awaitUntil;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Timeout;

import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMessage;
import kst4contest.model.ChatPreferences;
import kst4contest.testing.ManualScheduler;
import java.nio.file.Path;

class On4KstConnectionProbeTest {

    @TempDir
    Path tempDir;

    @Test
    void separatesClientProbeServerProbeAndInternalResponses() {
        assertEquals("CK|", On4KstProtocol.clientLivenessProbe());
        assertTrue(On4KstProtocol.isClientLivenessProbeResponse("OK"));
        assertTrue(On4KstProtocol.isClientLivenessProbeResponse("OK|"));
        assertFalse(On4KstProtocol.isClientLivenessProbeResponse(
                "OK|unexpected|"));
        assertTrue(On4KstProtocol.isServerLivenessProbe("CK|"));
        assertEquals("", On4KstProtocol.serverLivenessProbeResponse());
        assertTrue(On4KstProtocol.isInternalDxqResponse("DXQ|2|data|"));
    }

    @Test
    void selectsOneClientProbeAndTimeoutAtIdleBoundaries() {
        assertEquals(
                On4KstConnectionManager.IdleAction.NONE,
                idleAction(90_000L, true, false));
        assertEquals(
                On4KstConnectionManager.IdleAction.NONE,
                idleAction(90_001L, false, false),
                "No client probe may be sent before the session is online");
        assertEquals(
                On4KstConnectionManager.IdleAction.CLIENT_LIVENESS_PROBE,
                idleAction(90_001L, true, false));
        assertEquals(
                On4KstConnectionManager.IdleAction.NONE,
                idleAction(180_000L, true, true),
                "The 90-second CK remains the only probe in this idle phase");
        assertEquals(
                On4KstConnectionManager.IdleAction.CLIENT_LIVENESS_PROBE,
                idleAction(180_000L, true, false),
                "Even without probe state, the only 180-second action is CK");
        assertEquals(
                On4KstConnectionManager.IdleAction.NONE,
                idleAction(210_000L, true, true));
        assertEquals(
                On4KstConnectionManager.IdleAction.TIMEOUT,
                idleAction(210_001L, true, true));
    }

    @Test
    void sessionProbeCoversTwoCategoriesAndRepeatedIdlePhases() {
        On4KstConnectionManager.ClientLivenessProbeState probe =
                new On4KstConnectionManager.ClientLivenessProbeState();

        assertTrue(probe.tryStart(1_000L));
        assertFalse(probe.tryStart(1_001L),
                "A second category must not start another session probe");
        assertTrue(probe.isOutstanding());

        assertEquals(250L, probe.acknowledge(1_250L));
        assertFalse(probe.isOutstanding());
        assertEquals(-1L, probe.acknowledge(1_500L));

        assertTrue(probe.tryStart(2_000L),
                "New inbound activity starts a new idle phase");
    }

    @Test
    @Timeout(5)
    void writerUsesExactBytesForClientAndServerLivenessFrames() throws Exception {
        byte[] expected = "CK|\r\n\r\n".getBytes(StandardCharsets.UTF_8);

        try (ServerSocket server = new ServerSocket(0)) {
            CompletableFuture<byte[]> received = CompletableFuture.supplyAsync(() -> {
                try (Socket accepted = server.accept()) {
                    accepted.setSoTimeout(2_000);
                    return accepted.getInputStream().readNBytes(expected.length);
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });

            try (Socket client = new Socket("127.0.0.1", server.getLocalPort())) {
                LinkedBlockingQueue<ChatMessage> queue =
                        new LinkedBlockingQueue<>();
                AtomicBoolean active = new AtomicBoolean(true);
                WriteThread writer = new WriteThread(
                        11L,
                        client,
                        queue,
                        2,
                        ignored -> active.get(),
                        ignored -> { },
                        ignored -> { });
                writer.start();

                queue.add(serverFrame(On4KstProtocol.clientLivenessProbe()));
                queue.add(serverFrame(
                        On4KstProtocol.serverLivenessProbeResponse()));

                assertArrayEquals(
                        expected,
                        received.get(2, TimeUnit.SECONDS));

                active.set(false);
                writer.interrupt();
                writer.join(Duration.ofSeconds(2).toMillis());
            }
        }
    }

    @Test
    @Timeout(5)
    void readerRecordsOkAsActivityWithoutPublishingIt() throws Exception {
        CountDownLatch releaseChatFrame = new CountDownLatch(1);
        String chatFrame = "CH|2|123|DL1ABC|Name|0|text|0|";

        try (ServerSocket server = new ServerSocket(0)) {
            CompletableFuture<Void> serverDone = CompletableFuture.runAsync(() -> {
                try (Socket accepted = server.accept();
                     OutputStreamWriter out = new OutputStreamWriter(
                             accepted.getOutputStream(), StandardCharsets.UTF_8)) {
                    out.write("OK|\r\n");
                    out.flush();
                    assertTrue(releaseChatFrame.await(2, TimeUnit.SECONDS),
                            "the test never released the chat frame");
                    out.write(chatFrame + "\r\n");
                    out.flush();
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });

            try (Socket client = new Socket("127.0.0.1", server.getLocalPort())) {
                LinkedBlockingQueue<ChatMessage> messages =
                        new LinkedBlockingQueue<>();
                LinkedBlockingQueue<String> activity =
                        new LinkedBlockingQueue<>();
                AtomicBoolean active = new AtomicBoolean(true);
                ReadThread reader = new ReadThread(
                        21L,
                        client,
                        messages,
                        ignored -> active.get(),
                        activity::offer,
                        ignored -> { });
                reader.start();

                assertEquals("OK|", activity.poll(2, TimeUnit.SECONDS));
                assertNull(messages.poll(200, TimeUnit.MILLISECONDS));

                releaseChatFrame.countDown();
                assertEquals(chatFrame,
                        messages.poll(2, TimeUnit.SECONDS).getMessageText());

                active.set(false);
                reader.join(Duration.ofSeconds(2).toMillis());
            }
            serverDone.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    @Timeout(5)
    void readerIgnoresDelayedResponseFromReplacedSession() throws Exception {
        CountDownLatch releaseOldResponse = new CountDownLatch(1);
        AtomicBoolean inboundCallbackUsed = new AtomicBoolean();

        try (ServerSocket server = new ServerSocket(0)) {
            CompletableFuture<Void> serverDone = CompletableFuture.runAsync(() -> {
                try (Socket accepted = server.accept();
                     OutputStreamWriter out = new OutputStreamWriter(
                             accepted.getOutputStream(), StandardCharsets.UTF_8)) {
                    assertTrue(releaseOldResponse.await(2, TimeUnit.SECONDS),
                            "the test never released the old session's response");
                    out.write("OK\r\n");
                    out.flush();
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });

            try (Socket client = new Socket("127.0.0.1", server.getLocalPort())) {
                LinkedBlockingQueue<ChatMessage> messages =
                        new LinkedBlockingQueue<>();
                AtomicBoolean active = new AtomicBoolean(true);
                ReadThread reader = new ReadThread(
                        22L,
                        client,
                        messages,
                        ignored -> active.get(),
                        ignored -> inboundCallbackUsed.set(true),
                        ignored -> { });
                reader.start();

                active.set(false);
                releaseOldResponse.countDown();
                reader.join(Duration.ofSeconds(2).toMillis());

                assertFalse(inboundCallbackUsed.get());
                assertNull(messages.poll());
            }
            serverDone.get(2, TimeUnit.SECONDS);
        }
    }

    /**
     * The whole idle flow against a server that never answers: the CK probe goes out
     * after 90 s of silence, the 210 s stale timeout fails the session while the probe
     * is still outstanding, and the reconnect backoff opens a second connection.
     *
     * <p>Time and the supervisor's schedule are the test's: the clock is advanced and the
     * five-second monitor tick and the backoff are run on demand, so the test takes
     * milliseconds and does not depend on the machine's speed. The session state itself
     * is reached the way production reaches it, not set up by reflection.</p>
     */
    @Test
    @Timeout(15)
    void unansweredProbeUsesExistingReconnectFlow() throws Exception {
        ChatPreferences preferences = localPreferences();
        ChatController controller = org.mockito.Mockito.mock(
                ChatController.class);
        org.mockito.Mockito.when(controller.getChatPreferences())
                .thenReturn(preferences);
        org.mockito.Mockito.when(controller.getChatCategoryMain())
                .thenReturn(preferences.getLoginChatCategoryMain());
        org.mockito.Mockito.when(controller.getChatCategorySecondChat())
                .thenReturn(preferences.getLoginChatCategorySecond());

        AtomicLong clock = new AtomicLong(1_700_000_000_000L);
        ManualScheduler scheduler = new ManualScheduler();

        try (ServerSocket server = new ServerSocket(0)) {
            preferences.setStn_on4kstServersPort(server.getLocalPort());
            CompletableFuture<Socket> firstConnection =
                    CompletableFuture.supplyAsync(() -> accept(server));

            On4KstConnectionManager manager = new On4KstConnectionManager(
                    controller, scheduler, clock::get);
            try {
                manager.start();
                try (Socket first = firstConnection.get(2, TimeUnit.SECONDS)) {
                    first.setSoTimeout(2_000);
                    awaitState(manager,
                            On4KstConnectionState.WAITING_FOR_LOGIN_PROMPT);

                    // The server stays silent, so the login goes out on the fallback delay.
                    awaitUntil(Duration.ofSeconds(2),
                            "the login fallback was never scheduled",
                            scheduler::hasDelayedTasks);
                    scheduler.runDelayedTasks();
                    assertEquals(On4KstConnectionState.AUTHENTICATING,
                            manager.getState());

                    manager.onLogstat(1L, new String[] {"LOGSTAT", "100"});
                    awaitState(manager, On4KstConnectionState.SYNCING_MAIN_CHAT);
                    manager.onInitialUserListCompleted(1L,
                            preferences.getLoginChatCategoryMain());
                    awaitState(manager, On4KstConnectionState.ONLINE);

                    // 90 s of silence: the monitor sends the client liveness probe.
                    clock.addAndGet(
                            On4KstConnectionManager.CLIENT_LIVENESS_PROBE_AFTER_MILLIS + 1L);
                    scheduler.runPeriodicTask();
                    assertTrue(readUntil(first, "CK|\r\n").contains("CK|\r\n"),
                            "the probe must have been written to the socket");
                    assertEquals(On4KstConnectionState.ONLINE, manager.getState(),
                            "an outstanding probe alone is not a failure");

                    // 210 s of silence with the probe unanswered: the session is failed.
                    CompletableFuture<Socket> secondConnection =
                            CompletableFuture.supplyAsync(() -> accept(server));
                    clock.addAndGet(
                            On4KstConnectionManager.INBOUND_STALE_AFTER_MILLIS
                                    - On4KstConnectionManager.CLIENT_LIVENESS_PROBE_AFTER_MILLIS);
                    scheduler.runPeriodicTask();
                    assertEquals(On4KstConnectionState.RECONNECT_WAIT, manager.getState());
                    assertTrue(scheduler.hasDelayedTasks(), "a reconnect must be scheduled");

                    // The backoff elapses: the existing reconnect flow opens a new socket.
                    scheduler.runDelayedTasks();
                    try (Socket second = secondConnection.get(2, TimeUnit.SECONDS)) {
                        assertTrue(second.isConnected());
                    }
                    awaitState(manager, On4KstConnectionState.WAITING_FOR_LOGIN_PROMPT);
                }
            } finally {
                manager.stopByUser();
                manager.shutdown();
            }
        }
    }

    private static Socket accept(ServerSocket server) {
        try {
            return server.accept();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /** Reads from the socket until {@code marker} has arrived, or its 2 s read timeout fires. */
    private static String readUntil(Socket socket, String marker) throws IOException {
        StringBuilder text = new StringBuilder();
        InputStream in = socket.getInputStream();
        byte[] buffer = new byte[256];
        while (!text.toString().contains(marker)) {
            int read = in.read(buffer);
            if (read < 0) {
                break;
            }
            text.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
        }
        return text.toString();
    }

    private On4KstConnectionManager.IdleAction idleAction(
            long inboundIdleMillis,
            boolean online,
            boolean probeOutstanding
    ) {
        return On4KstConnectionManager.determineIdleAction(
                inboundIdleMillis,
                online,
                probeOutstanding);
    }

    private ChatMessage serverFrame(String text) {
        ChatMessage message = new ChatMessage();
        message.setMessageDirectedToServer(true);
        message.setMessageText(text);
        return message;
    }

    private ChatPreferences localPreferences() {
        ChatPreferences preferences = new ChatPreferences(tempDir.resolve("preferences.xml"));
        preferences.setStn_on4kstServersDns("127.0.0.1");
        preferences.setStn_loginCallSign("DL1ABC");
        preferences.setStn_loginPassword("test-password");
        preferences.setStn_loginNameMainCat("");
        preferences.setStn_loginLocatorMainCat("JO50AA");
        preferences.setLoginChatCategoryMain(new ChatCategory(2));
        preferences.setLoginChatCategorySecond(new ChatCategory(3));
        preferences.setLoginToSecondChatEnabled(false);
        return preferences;
    }

    private void awaitState(
            On4KstConnectionManager manager,
            On4KstConnectionState expected
    ) {
        awaitUntil(Duration.ofSeconds(5),
                () -> "expected state " + expected + " but was " + manager.getState(),
                () -> manager.getState() == expected);
    }
}
