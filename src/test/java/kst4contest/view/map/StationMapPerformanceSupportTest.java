package kst4contest.view.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class StationMapPerformanceSupportTest {

    @Test
    void hiddenRefreshIsDeferredUntilTheNextShowOnly() {
        StationMapPerformanceSupport.RefreshState state =
                new StationMapPerformanceSupport.RefreshState();

        assertFalse(state.shouldRefreshNow(false));
        assertTrue(state.consumeDirtyOnShow());
        assertFalse(state.consumeDirtyOnShow());
    }

    @Test
    void visibleRefreshCanRunImmediately() {
        StationMapPerformanceSupport.RefreshState state =
                new StationMapPerformanceSupport.RefreshState();

        assertTrue(state.shouldRefreshNow(true));
        assertFalse(state.consumeDirtyOnShow());
    }

    @Test
    void unchangedGridInputsAreRejectedBeforeRendering() {
        StationMapPerformanceSupport.GridRenderState state =
                new StationMapPerformanceSupport.GridRenderState();
        StationMapPerformanceSupport.GridRenderInput input = gridInput();

        assertTrue(state.shouldRender(input));
        assertFalse(state.shouldRender(input));
    }

    @Test
    void everyGridInputCanTriggerANewRender() {
        assertChanged(new StationMapPerformanceSupport.GridRenderInput(
                49.1, 6.0, 55.0, 16.0, 7, 1024.0, 768.0));
        assertChanged(new StationMapPerformanceSupport.GridRenderInput(
                49.0, 6.1, 55.0, 16.0, 7, 1024.0, 768.0));
        assertChanged(new StationMapPerformanceSupport.GridRenderInput(
                49.0, 6.0, 55.1, 16.0, 7, 1024.0, 768.0));
        assertChanged(new StationMapPerformanceSupport.GridRenderInput(
                49.0, 6.0, 55.0, 16.1, 7, 1024.0, 768.0));
        assertChanged(new StationMapPerformanceSupport.GridRenderInput(
                49.0, 6.0, 55.0, 16.0, 8, 1024.0, 768.0));
        assertChanged(new StationMapPerformanceSupport.GridRenderInput(
                49.0, 6.0, 55.0, 16.0, 7, 1200.0, 768.0));
        assertChanged(new StationMapPerformanceSupport.GridRenderInput(
                49.0, 6.0, 55.0, 16.0, 7, 1024.0, 800.0));
    }

    @Test
    void disabledMapDebugSkipsTheDiagnosticOperation() {
        AtomicBoolean executed = new AtomicBoolean();

        StationMapPerformanceSupport.runDebugIfEnabled(
                false,
                () -> executed.set(true));

        assertFalse(executed.get());
    }

    @Test
    void enabledMapDebugRunsTheDiagnosticOperation() {
        AtomicBoolean executed = new AtomicBoolean();

        StationMapPerformanceSupport.runDebugIfEnabled(
                true,
                () -> executed.set(true));

        assertTrue(executed.get());
    }

    @Test
    void slowRefreshStartsAtOneHundredMilliseconds() {
        assertFalse(StationMapPerformanceSupport.isSlowRefresh(99_999_999L));
        assertTrue(StationMapPerformanceSupport.isSlowRefresh(100_000_000L));
    }

    @Test
    void slowRefreshMessageSeparatesSnapshotAndRenderTimes() {
        assertEquals(
                "Station map refresh slow: total=245 ms, snapshots=42 ms, "
                        + "render=198 ms, visibleMembers=500, stations=487",
                StationMapPerformanceSupport.formatSlowRefreshMessage(
                        245_000_000L,
                        42_000_000L,
                        198_000_000L,
                        500,
                        487
                )
        );
    }

    private void assertChanged(
            StationMapPerformanceSupport.GridRenderInput changed) {
        StationMapPerformanceSupport.GridRenderState state =
                new StationMapPerformanceSupport.GridRenderState();
        assertTrue(state.shouldRender(gridInput()));
        assertTrue(state.shouldRender(changed));
    }

    private StationMapPerformanceSupport.GridRenderInput gridInput() {
        return new StationMapPerformanceSupport.GridRenderInput(
                49.0,
                6.0,
                55.0,
                16.0,
                7,
                1024.0,
                768.0
        );
    }
}
