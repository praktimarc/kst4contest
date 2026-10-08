package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;

import kst4contest.logic.PriorityCalculator;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;

class ScoreServiceRefreshTest {
    private static final String REMOVED_DIAGNOSTIC =
            "Priority scores projected to ChatMember";

    @Test
    void scoreProjectionRefreshesUserListWithoutPeriodicDiagnostic()
            throws Exception {
        ChatController controller = mock(ChatController.class);
        ChatMember member = new ChatMember();
        member.setCallSign("DL1ABC-2");
        when(controller.snapshotChatMembers()).thenReturn(List.of(member));

        ScoreService service = new ScoreService(
                controller, mock(PriorityCalculator.class), 15);
        ScoreService.ScoreSnapshot snapshot = new ScoreService.ScoreSnapshot(
                1L,
                Map.of("DL1ABC", 42.5),
                Map.<String, ChatCategory>of(),
                List.of());

        applySnapshot(service, snapshot);

        assertEquals(42.5, member.getCurrentPriorityScore());
        List<Invocation> controllerInvocations =
                List.copyOf(mockingDetails(controller).getInvocations());
        assertTrue(controllerInvocations.stream().anyMatch(invocation ->
                invocation.getMethod().getName().equals("fireUserListUpdate")
                        && invocation.getArguments().length == 0));
        assertFalse(controllerInvocations.stream().anyMatch(invocation ->
                List.of(invocation.getArguments()).contains(REMOVED_DIAGNOSTIC)));
    }

    @Test
    void userListRefreshKeepsDiagnosticAndReasonlessCallbacksSeparate() throws Exception {
        ChatController controller = mock(
                ChatController.class,
                org.mockito.Answers.CALLS_REAL_METHODS);
        /*
         * CALLS_REAL_METHODS runs no constructor, so the dispatcher field stays null. Give
         * the mock the direct dispatcher the no-arg constructor installs; it runs inline and
         * reports the calling thread as the UI thread, which is what the fire methods branch
         * on. This replaces the old static Platform mock -- core no longer touches JavaFX.
         */
        java.lang.reflect.Field dispatcherField =
                ChatController.class.getDeclaredField("uiDispatcher");
        dispatcherField.setAccessible(true);
        dispatcherField.set(controller, new kst4contest.observe.DirectUiDispatcher());

        StatusUpdateListener listener = mock(StatusUpdateListener.class);
        controller.setStatusListener(listener);

        controller.fireUserListUpdate();
        controller.fireUserListUpdate("Reachability calculated");

        verify(listener).onUserListUpdated();
        verify(listener).onUserListUpdated("Reachability calculated");
    }

    private void applySnapshot(
            ScoreService service,
            ScoreService.ScoreSnapshot snapshot
    ) throws Exception {
        Method method = ScoreService.class.getDeclaredMethod(
                "applyScoreSnapshotToChatMembers",
                ScoreService.ScoreSnapshot.class);
        method.setAccessible(true);
        method.invoke(service, snapshot);
    }
}
