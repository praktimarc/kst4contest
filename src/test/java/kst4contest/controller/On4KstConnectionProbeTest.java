package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
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
import org.junit.jupiter.api.Timeout;

import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMessage;
import kst4contest.model.ChatPreferences;

class On4KstConnectionProbeTest {

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
                    releaseChatFrame.await(2, TimeUnit.SECONDS);
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
                    releaseOldResponse.await(2, TimeUnit.SECONDS);
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

        try (ServerSocket server = new ServerSocket(0)) {
            preferences.setStn_on4kstServersPort(server.getLocalPort());
            CompletableFuture<Integer> acceptedConnections =
                    CompletableFuture.supplyAsync(() -> {
                        try (Socket first = server.accept();
                             Socket second = server.accept()) {
                            return 2;
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    });

            On4KstConnectionManager manager =
                    new On4KstConnectionManager(controller);
            try {
                manager.start();
                awaitState(manager, On4KstConnectionState.AUTHENTICATING);
                manager.onLogstat(1L, new String[] {"LOGSTAT", "100"});
                awaitState(manager, On4KstConnectionState.SYNCING_MAIN_CHAT);
                manager.onInitialUserListCompleted(1L,
                        preferences.getLoginChatCategoryMain());
                awaitState(manager, On4KstConnectionState.ONLINE);

                setTimedOutProbe(manager, System.currentTimeMillis());

                awaitState(manager, On4KstConnectionState.RECONNECT_WAIT);
                assertEquals(2, acceptedConnections.get(7, TimeUnit.SECONDS));
            } finally {
                manager.stopByUser();
            }
        }
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
        ChatPreferences preferences = new ChatPreferences();
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
    ) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(7L);
        while (System.nanoTime() < deadline) {
            if (manager.getState() == expected) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(25L);
        }
        assertEquals(expected, manager.getState());
    }

    private void setTimedOutProbe(
            On4KstConnectionManager manager,
            long now
    ) throws ReflectiveOperationException {
        Field activeSession = On4KstConnectionManager.class
                .getDeclaredField("activeSession");
        activeSession.setAccessible(true);
        Object session = activeSession.get(manager);

        Field lastInboundMillis = session.getClass()
                .getDeclaredField("lastInboundMillis");
        lastInboundMillis.setAccessible(true);
        ((AtomicLong) lastInboundMillis.get(session)).set(
                now - On4KstConnectionManager.INBOUND_STALE_AFTER_MILLIS - 1L);

        Field clientLivenessProbe = session.getClass()
                .getDeclaredField("clientLivenessProbe");
        clientLivenessProbe.setAccessible(true);
        ((On4KstConnectionManager.ClientLivenessProbeState)
                clientLivenessProbe.get(session)).tryStart(
                        now - On4KstConnectionManager
                                .CLIENT_LIVENESS_PROBE_AFTER_MILLIS);
    }
}
