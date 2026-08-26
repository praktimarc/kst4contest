package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class On4KstConnectionProbeTest {

    @Test
    void buildsMainChatProbeAndAcceptsExpectedResponse() {
        assertEquals("RDXQ|2|", On4KstProtocol.connectionProbe(2));
        assertTrue(On4KstProtocol.isConnectionProbeResponse("DXQ|2|data|"));
        assertFalse(On4KstProtocol.isConnectionProbeResponse(
                "CH|2|123|DL1ABC|Name|0|text|0|"));
    }

    @Test
    void selectsHeartbeatProbeAndTimeoutAtIdleBoundaries() {
        assertEquals(
                On4KstConnectionManager.IdleAction.NONE,
                idleAction(90_000L, false, false));
        assertEquals(
                On4KstConnectionManager.IdleAction.HEARTBEAT,
                idleAction(90_001L, false, false));
        assertEquals(
                On4KstConnectionManager.IdleAction.NONE,
                idleAction(179_999L, true, false));
        assertEquals(
                On4KstConnectionManager.IdleAction.CONNECTION_PROBE,
                idleAction(180_000L, true, false));
        assertEquals(
                On4KstConnectionManager.IdleAction.NONE,
                idleAction(210_000L, true, true));
        assertEquals(
                On4KstConnectionManager.IdleAction.TIMEOUT,
                idleAction(210_001L, true, true));
    }

    @Test
    void oneSessionProbeIsAcknowledgedByAnyInboundTraffic() {
        On4KstConnectionManager.ConnectionProbeState probe =
                new On4KstConnectionManager.ConnectionProbeState();

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
    void writerUsesExactCrLfForHeartbeatAndConnectionProbe() throws Exception {
        byte[] expected = "\r\nRDXQ|2|\r\n".getBytes(StandardCharsets.UTF_8);

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

                queue.add(serverFrame(""));
                queue.add(serverFrame(On4KstProtocol.connectionProbe(2)));

                assertArrayEquals(
                        expected,
                        received.get(2, TimeUnit.SECONDS));

                active.set(false);
                writer.interrupt();
                writer.join(Duration.ofSeconds(2).toMillis());
            }
        }
    }

    private On4KstConnectionManager.IdleAction idleAction(
            long inboundIdleMillis,
            boolean heartbeatSent,
            boolean probeOutstanding
    ) {
        return On4KstConnectionManager.determineIdleAction(
                inboundIdleMillis,
                heartbeatSent,
                probeOutstanding);
    }

    private ChatMessage serverFrame(String text) {
        ChatMessage message = new ChatMessage();
        message.setMessageDirectedToServer(true);
        message.setMessageText(text);
        return message;
    }
}
