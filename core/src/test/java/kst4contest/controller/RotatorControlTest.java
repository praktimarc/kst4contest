package kst4contest.controller;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * The rotator command rules moved out of ChatController unchanged: the 1 degree
 * "reached" tolerance, no retry after reported movement, and the compatibility retry
 * when nothing moved. The scheduler is captured, so the delayed check runs on demand.
 */
class RotatorControlTest {

    private final List<String> commands = new ArrayList<>();
    private final List<Double> reported = new ArrayList<>();
    private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    private Runnable pendingRetry;

    private RotatorControl control() {
        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenAnswer(call -> {
            pendingRetry = call.getArgument(0);
            return mock(ScheduledFuture.class);
        });
        RotatorControl control = new RotatorControl(
                "PSTRotator",
                ignored -> new RotatorControl.Link() {
                    @Override public void setTrackingMode(boolean on) { commands.add("tracking " + on); }
                    @Override public void setAzimuth(double azimuth) { commands.add("az " + azimuth); }
                    @Override public void stopRotation() { commands.add("halt"); }
                    @Override public void stop() { commands.add("stop"); }
                },
                scheduler,
                reported::add);
        control.start();
        return control;
    }

    @Test
    void rotateSendsTheNormalisedTargetAndSchedulesTheCheckAfterTwoSeconds() {
        RotatorControl control = control();

        control.rotateTo(-10.0);

        assertEquals(List.of("tracking false", "az 350.0"), commands);
        verify(scheduler).schedule(any(Runnable.class), eq(2L), eq(TimeUnit.SECONDS));
    }

    @Test
    void aReportWithinOneDegreeCountsAsReachedAndSendsNoRetry() {
        RotatorControl control = control();
        control.onAzimuthUpdate(100.0);
        control.rotateTo(145.7);
        control.onAzimuthUpdate(145.0);
        commands.clear();

        pendingRetry.run();

        assertTrue(commands.isEmpty());
    }

    @Test
    void reportedMovementSendsNoRetry() {
        RotatorControl control = control();
        control.onAzimuthUpdate(100.0);
        control.rotateTo(200.0);
        control.onAzimuthUpdate(120.0);
        commands.clear();

        pendingRetry.run();

        assertTrue(commands.isEmpty());
    }

    @Test
    void noMovementSendsTheCompatibilityRetry() {
        RotatorControl control = control();
        control.onAzimuthUpdate(100.0);
        control.rotateTo(200.0);
        commands.clear();

        pendingRetry.run();

        assertEquals(List.of("az 0.0", "az 200.0"), commands);
    }

    @Test
    void reportsAreNormalisedAndPassedOnAndInvalidOnesIgnored() {
        RotatorControl control = control();

        control.onAzimuthUpdate(370.0);
        control.onAzimuthUpdate(Double.NaN);

        assertEquals(List.of(10.0), reported);
        assertEquals(10.0, control.lastReportedAzimuth());
    }

    @Test
    void stopClosesTheLinkForgetsThePositionAndIgnoresLaterCommands() {
        RotatorControl control = control();
        control.onAzimuthUpdate(100.0);

        control.stop();
        control.rotateTo(50.0);

        assertEquals(List.of("stop"), commands);
        assertFalse(control.isRunning());
        assertTrue(Double.isNaN(control.lastReportedAzimuth()));
    }
}
