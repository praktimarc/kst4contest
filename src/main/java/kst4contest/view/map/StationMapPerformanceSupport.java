package kst4contest.view.map;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Small JavaFX-independent state helpers for station-map refresh decisions. */
final class StationMapPerformanceSupport {

    /** Minimum duration for a slow-refresh diagnostic. */
    private static final long SLOW_NANOS =
            TimeUnit.MILLISECONDS.toNanos(100L);

    private StationMapPerformanceSupport() {
    }

    /* default */ static void runDebugIfEnabled(
            final boolean enabled,
            final Runnable operation
    ) {
        Objects.requireNonNull(operation, "operation");
        if (enabled) {
            operation.run();
        }
    }

    /* default */ static boolean isSlowRefresh(final long totalNanos) {
        return totalNanos >= SLOW_NANOS;
    }

    /* default */ static String formatSlowRefreshMessage(
            final long totalNanos,
            final long snapshotNanos,
            final long renderNanos,
            final int visibleCount,
            final int stationCount
    ) {
        return "Station map refresh slow: total="
                + TimeUnit.NANOSECONDS.toMillis(totalNanos)
                + " ms, snapshots="
                + TimeUnit.NANOSECONDS.toMillis(snapshotNanos)
                + " ms, render="
                + TimeUnit.NANOSECONDS.toMillis(renderNanos)
                + " ms, visibleMembers="
                + visibleCount
                + ", stations="
                + stationCount;
    }

    /** Tracks deferred refresh work while the map window is hidden. */
    /* default */ static final class RefreshState {
        /** Whether current application state still needs to reach the map. */
        private boolean dirty = true;

        /* default */ boolean shouldRefreshNow(final boolean visible) {
            dirty = !visible;
            return visible;
        }

        /* default */ void markDirty() {
            dirty = true;
        }

        /* default */ boolean consumeDirtyOnShow() {
            final boolean refreshRequired = dirty;
            dirty = false;
            return refreshRequired;
        }
    }

    /** Complete set of inputs that can change the visible grid rendering. */
    /* default */ record GridRenderInput(
            double southLatitude,
            double westLongitude,
            double northLatitude,
            double eastLongitude,
            int zoom,
            double viewportWidth,
            double viewportHeight
    ) {
    }

    /** Rejects duplicate grid inputs before cell construction starts. */
    /* default */ static final class GridRenderState {
        /** Inputs used for the last planned grid rendering. */
        private GridRenderInput lastInput;

        /* default */ boolean shouldRender(final GridRenderInput input) {
            Objects.requireNonNull(input, "input");
            final boolean renderRequired = !input.equals(lastInput);
            if (renderRequired) {
                lastInput = input;
            }
            return renderRequired;
        }
    }
}
