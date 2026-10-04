package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatPreferences;

/**
 * Opt-in practice test against the configured ON4KST server.
 *
 * <p>The normal test suite skips this class. Run it explicitly with
 * {@code -Don4kst.live=true -Dtest=On4KstIdlePracticeTest} from the IDE or
 * Maven when the locally stored test credentials may be used. The practice
 * connection uses only the usually quiet category 9.</p>
 */
class On4KstIdlePracticeTest {
    private static final int LIVE_CATEGORY = ChatCategory.VUHFR3;
    private static final Duration ONLINE_TIMEOUT = Duration.ofSeconds(90);
    private static final Duration QUIET_PHASE_TIMEOUT = Duration.ofMinutes(45);
    private static final Duration POST_RESPONSE_OBSERVATION = Duration.ofSeconds(135);

    @Test
    @Timeout(value = 50, unit = TimeUnit.MINUTES)
    void observesCkOkWithoutQuietReconnect() throws Exception {
        assumeTrue(Boolean.getBoolean("on4kst.live"),
                "Live ON4KST test requires -Don4kst.live=true");

        ChatPreferences preferences = new ChatPreferences();
        assumeTrue(preferences.readPreferencesFromXmlFile(),
                "No readable local ON4KST test configuration");
        assumeTrue(hasText(preferences.getStn_loginCallSign())
                        && hasText(preferences.getStn_loginPassword()),
                "Local ON4KST test credentials are incomplete");
        preferences.setLoginChatCategoryMain(
                new ChatCategory(LIVE_CATEGORY));
        preferences.setLoginToSecondChatEnabled(false);

        LinkedBlockingQueue<StateEvent> stateEvents =
                new LinkedBlockingQueue<>();
        LinkedBlockingQueue<ProbeEvent> probeEvents =
                new LinkedBlockingQueue<>();
        AtomicBoolean reconnectObserved = new AtomicBoolean();
        AtomicReference<On4KstConnectionManager> managerReference =
                new AtomicReference<>();

        ChatController controller = mock(ChatController.class);
        when(controller.getChatPreferences()).thenReturn(preferences);
        ChatCategory mainCategory = preferences.getLoginChatCategoryMain();
        ChatCategory secondCategory = preferences.getLoginChatCategorySecond();
        if (secondCategory == null) {
            secondCategory = new ChatCategory(
                    mainCategory.getCategoryNumber() == 2 ? 3 : 2);
        }
        when(controller.getChatCategoryMain()).thenReturn(mainCategory);
        when(controller.getChatCategorySecondChat()).thenReturn(secondCategory);

        DBController database = mock(DBController.class);
        when(database.fetchChatMemberWkdDataForOnlyOneCallsignFromDB(
                any(ChatMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(database).storeChatMember(any(ChatMember.class));
        when(controller.getDbHandler()).thenReturn(database);
        doAnswer(invocation -> {
            On4KstConnectionState state = invocation.getArgument(0);
            stateEvents.offer(new StateEvent(state, ZonedDateTime.now()));
            if (state == On4KstConnectionState.RECONNECT_WAIT) {
                reconnectObserved.set(true);
            }
            return null;
        }).when(controller).updateOn4KstConnectionState(
                any(On4KstConnectionState.class), anyString(), anyBoolean());
        doAnswer(invocation -> {
            managerReference.get().onLogstat(
                    invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(controller).onOn4KstLogstat(anyLong(), any(String[].class));
        doAnswer(invocation -> {
            managerReference.get().stageInitialChatMember(
                    invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(controller).stageInitialOn4KstChatMember(
                anyLong(), any(ChatMember.class));
        doAnswer(invocation -> {
            managerReference.get().onInitialUserListCompleted(
                    invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(controller).onOn4KstInitialUserListCompleted(
                anyLong(), any(ChatCategory.class));

        On4KstConnectionManager manager =
                new On4KstConnectionManager(controller);
        managerReference.set(manager);

        Logger logger = Logger.getLogger(
                On4KstConnectionManager.class.getName());
        Level previousLevel = logger.getLevel();
        Handler probeHandler = probeHandler(probeEvents);
        logger.setLevel(Level.INFO);
        logger.addHandler(probeHandler);

        try {
            manager.start();
            StateEvent online = awaitOnline(stateEvents);
            System.out.println("[ON4KST live] Category " + LIVE_CATEGORY
                    + " ONLINE at "
                    + timestamp(online.at()));
            reconnectObserved.set(false);

            ProbeCycle successfulCycle = awaitCkOkCycle(
                    probeEvents, reconnectObserved);
            System.out.println("[ON4KST live] CK sent at "
                    + timestamp(successfulCycle.sentAt())
                    + ", OK received at "
                    + timestamp(successfulCycle.confirmedAt())
                    + ", response time "
                    + successfulCycle.responseMillis() + " ms");

            reconnectObserved.set(false);
            long observationDeadline = System.nanoTime()
                    + POST_RESPONSE_OBSERVATION.toNanos();
            while (System.nanoTime() < observationDeadline) {
                if (reconnectObserved.get()) {
                    fail("ON4KST entered reconnect after the confirmed CK/OK cycle");
                }
                TimeUnit.SECONDS.sleep(1L);
            }

            System.out.println("[ON4KST live] Observation completed at "
                    + timestamp(ZonedDateTime.now())
                    + "; no reconnect followed the quiet CK/OK cycle");
        } finally {
            manager.stopByUser();
            logger.removeHandler(probeHandler);
            logger.setLevel(previousLevel);
        }
    }

    private StateEvent awaitOnline(
            LinkedBlockingQueue<StateEvent> stateEvents
    ) throws InterruptedException {
        long deadline = System.nanoTime() + ONLINE_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            StateEvent event = stateEvents.poll(1L, TimeUnit.SECONDS);
            if (event == null) {
                continue;
            }
            if (event.state() == On4KstConnectionState.ONLINE) {
                return event;
            }
            if (event.state() == On4KstConnectionState.DISCONNECTED) {
                fail("ON4KST live test disconnected before reaching ONLINE");
            }
        }
        fail("ON4KST live test did not reach ONLINE within "
                + ONLINE_TIMEOUT.toSeconds() + " seconds");
        throw new IllegalStateException("unreachable");
    }

    private ProbeCycle awaitCkOkCycle(
            LinkedBlockingQueue<ProbeEvent> probeEvents,
            AtomicBoolean reconnectObserved
    ) throws InterruptedException {
        long deadline = System.nanoTime() + QUIET_PHASE_TIMEOUT.toNanos();
        ProbeEvent sent = null;

        while (System.nanoTime() < deadline) {
            if (reconnectObserved.get()) {
                fail("ON4KST reconnected before a CK/OK idle cycle was observed");
            }

            ProbeEvent event = probeEvents.poll(1L, TimeUnit.SECONDS);
            if (event == null) {
                continue;
            }
            if (event.type() == ProbeEventType.SENT) {
                sent = event;
                continue;
            }
            if (sent != null && event.sessionId() == sent.sessionId()) {
                if ("OK".equals(event.opcode())) {
                    return new ProbeCycle(
                            sent.at(), event.at(), event.responseMillis());
                }
                // Normal server data ended this idle phase before OK arrived.
                sent = null;
            }
        }

        fail("No uninterrupted CK/OK idle cycle was observed within "
                + QUIET_PHASE_TIMEOUT.toMinutes() + " minutes");
        throw new IllegalStateException("unreachable");
    }

    private Handler probeHandler(
            LinkedBlockingQueue<ProbeEvent> probeEvents
    ) {
        return new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record == null || record.getParameters() == null) {
                    return;
                }
                Object[] parameters = record.getParameters();
                if (record.getMessage().startsWith(
                        "Sending ON4KST client liveness probe")
                        && parameters.length >= 1) {
                    probeEvents.offer(new ProbeEvent(
                            ProbeEventType.SENT,
                            ((Number) parameters[0]).longValue(),
                            "CK",
                            -1L,
                            ZonedDateTime.now()));
                } else if (record.getMessage().startsWith(
                        "ON4KST client liveness probe confirmed")
                        && parameters.length >= 3) {
                    probeEvents.offer(new ProbeEvent(
                            ProbeEventType.CONFIRMED,
                            ((Number) parameters[0]).longValue(),
                            String.valueOf(parameters[1]),
                            ((Number) parameters[2]).longValue(),
                            ZonedDateTime.now()));
                }
            }

            @Override
            public void flush() {
                // Nothing is buffered.
            }

            @Override
            public void close() {
                // The handler owns no external resource.
            }
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String timestamp(ZonedDateTime value) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value);
    }

    private record StateEvent(
            On4KstConnectionState state,
            ZonedDateTime at
    ) {
    }

    private enum ProbeEventType {
        SENT,
        CONFIRMED
    }

    private record ProbeEvent(
            ProbeEventType type,
            long sessionId,
            String opcode,
            long responseMillis,
            ZonedDateTime at
    ) {
    }

    private record ProbeCycle(
            ZonedDateTime sentAt,
            ZonedDateTime confirmedAt,
            long responseMillis
    ) {
    }
}
