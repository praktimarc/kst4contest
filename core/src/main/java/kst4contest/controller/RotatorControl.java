package kst4contest.controller;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One PSTRotator as the operator drives it: start and stop, a heading command with its
 * delayed compatibility check, and the last reported position.
 *
 * <p>Moved out of ChatController unchanged so SO2R can run one per radio. The command
 * scheduler is shared and passed in; nothing here blocks the UI thread.</p>
 */
public final class RotatorControl {

    /** What the control needs from a rotator connection; PstRotatorClient in production. */
    public interface Link {
        void setTrackingMode(boolean on);
        void setAzimuth(double azimuth);
        /** Tells the rotor to stop turning. */
        void stopRotation();
        /** Closes the connection and its polling. */
        void stop();
    }

    private static final Logger LOGGER = Logger.getLogger(RotatorControl.class.getName());

    /**
     * How close the reported position must be to count as "reached". The target carries a
     * decimal, while many rotor controllers report whole degrees: with 0.5 a target of 145.7
     * and a report of 145 would count as missed and could trigger the retry.
     */
    public static final double REACHED_TOLERANCE_DEGREES = 1.0;

    private final String threadName;
    private final Function<RotatorControl, Link> linkFactory;
    private final ScheduledExecutorService retryScheduler;
    private final Consumer<Double> reportedAzimuthSink;

    private volatile Link link;
    private ScheduledFuture<?> pendingRetry;

    /* Updated by the receiver thread; read by the retry check, never from UI state. */
    private volatile double lastReportedAzimuth = Double.NaN;

    /**
     * @param threadName names the rotator in logs and in the status bar
     * @param linkFactory opens the connection; receives this control as the event target
     * @param retryScheduler runs the delayed compatibility check
     * @param reportedAzimuthSink receives every valid, normalised reported azimuth on the
     *        receiver thread; the caller moves it to the UI thread
     */
    public RotatorControl(String threadName, Function<RotatorControl, Link> linkFactory,
                          ScheduledExecutorService retryScheduler, Consumer<Double> reportedAzimuthSink) {
        this.threadName = threadName;
        this.linkFactory = linkFactory;
        this.retryScheduler = retryScheduler;
        this.reportedAzimuthSink = reportedAzimuthSink;
    }

    /** Opens the connection unless one is open. */
    public synchronized void start() {
        if (link == null) {
            link = linkFactory.apply(this);
        }
    }

    public boolean isRunning() {
        return link != null;
    }

    /** Cancels a pending check, closes the connection and forgets the position. */
    public synchronized void stop() {
        ScheduledFuture<?> retry = pendingRetry;
        if (retry != null) {
            retry.cancel(false);
            pendingRetry = null;
        }
        Link active = link;
        link = null;
        lastReportedAzimuth = Double.NaN;
        if (active != null) {
            active.stop();
        }
    }

    public double lastReportedAzimuth() {
        return lastReportedAzimuth;
    }

    /** Tells the rotor to stop turning; used when the session ends. */
    public void stopRotation() {
        Link active = link;
        if (active != null) {
            active.stopRotation();
        }
    }

    /**
     * Sends a new azimuth without blocking the caller.
     *
     * <p>Some SPID configurations occasionally ignore the first azimuth command. The latest
     * reported position is therefore checked after two seconds. If no movement was reported
     * and the target has not already been reached, the original compatibility sequence is
     * sent again.</p>
     */
    public synchronized void rotateTo(double azimuth) {
        if (!Double.isFinite(azimuth)) {
            LOGGER.log(Level.WARNING, "Ignoring invalid {0} azimuth: {1}", new Object[] { threadName, azimuth });
            return;
        }
        Link active = link;
        if (active == null) {
            LOGGER.log(Level.WARNING, "Cannot rotate antenna to {0} degrees: {1} integration is not active.",
                    new Object[] { azimuth, threadName });
            return;
        }

        double target = normalizeAzimuth(azimuth);
        double positionBeforeCommand = lastReportedAzimuth;

        active.setTrackingMode(false);
        LOGGER.log(Level.INFO, "Sending {0} azimuth requested by the operator: {1}", new Object[] { threadName, target });
        active.setAzimuth(target);

        ScheduledFuture<?> previous = pendingRetry;
        if (previous != null) {
            previous.cancel(false);
        }
        pendingRetry = retryScheduler.schedule(
                () -> retryIfRequired(positionBeforeCommand, target), 2, TimeUnit.SECONDS);
    }

    /** Called by the receiver thread for every reported position. */
    public void onAzimuthUpdate(double azimuth) {
        if (!Double.isFinite(azimuth)) {
            LOGGER.log(Level.WARNING, "Ignoring invalid azimuth reported by {0}: {1}", new Object[] { threadName, azimuth });
            return;
        }
        double normalized = normalizeAzimuth(azimuth);
        lastReportedAzimuth = normalized;
        reportedAzimuthSink.accept(normalized);
    }

    private void retryIfRequired(double positionBeforeCommand, double target) {
        Link active = link;
        if (active == null) {
            return;
        }
        double current = lastReportedAzimuth;

        if (Double.isFinite(current) && angularDistance(current, target) < REACHED_TOLERANCE_DEGREES) {
            LOGGER.log(Level.FINE, "{0} reached the requested azimuth without retry: {1}", new Object[] { threadName, target });
            return;
        }

        boolean noPositionFeedback = !Double.isFinite(current);
        boolean positionUnchanged = Double.isFinite(positionBeforeCommand)
                && Double.isFinite(current)
                && angularDistance(positionBeforeCommand, current) < 0.5;

        if (!noPositionFeedback && !positionUnchanged) {
            LOGGER.log(Level.FINE, "{0} reported movement towards {1}; no retry required.", new Object[] { threadName, target });
            return;
        }

        LOGGER.log(Level.WARNING, "{0} reported no movement after the command for {1} degrees; "
                + "sending the SPID compatibility retry.", new Object[] { threadName, target });
        active.setAzimuth(0.0);
        active.setAzimuth(target);
    }

    static double angularDistance(double first, double second) {
        double difference = Math.abs(normalizeAzimuth(first) - normalizeAzimuth(second));
        return Math.min(difference, 360.0 - difference);
    }

    static double normalizeAzimuth(double azimuth) {
        double normalized = azimuth % 360.0;
        return normalized < 0.0 ? normalized + 360.0 : normalized;
    }
}
