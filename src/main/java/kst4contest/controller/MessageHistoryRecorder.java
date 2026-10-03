package kst4contest.controller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Records the raw ON4KST transport frames of one application session.
 *
 * <p>The recorder is shared by the reader and writer threads. Every event is
 * written under one lock so concurrent RX and TX traffic cannot interleave.
 * File-system failures are contained here and never propagated to connection
 * threads.</p>
 */
public final class MessageHistoryRecorder implements AutoCloseable {
    /** File name of the active program-session history. */
    /* package */ static final String CURRENT_FILE_NAME = "Messagehistory.raw";

    /** Component logger. */
    private static final Logger LOGGER =
            Logger.getLogger(MessageHistoryRecorder.class.getName());
    /** Timestamp format for individual history events. */
    private static final DateTimeFormatter EVENT_TIMESTAMP =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .withZone(ZoneOffset.UTC);
    /** Timestamp format used in archived history file names. */
    private static final DateTimeFormatter ARCHIVE_TIMESTAMP =
            DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss'Z'")
                    .withZone(ZoneOffset.UTC);
    /** Shared recorder instance that deliberately discards all frames. */
    private static final MessageHistoryRecorder NO_OP_RECORDER =
            new MessageHistoryRecorder();

    /** Serializes state transitions and complete line writes. */
    private final ReentrantLock lock = new ReentrantLock();
    /** Directory containing the active and archived history files. */
    private final Path appDirectory;
    /** Path of the active program-session history. */
    private final Path currentFile;
    /** Time source for event and archive timestamps. */
    private final Clock clock;
    /** Whether this instance is the shared no-op recorder. */
    private final boolean noOp;

    /** Whether new frames should currently be recorded. */
    private boolean enabled;
    /** Whether this program session has completed its one-time rotation. */
    private boolean sessionReady;
    /** Whether opening the active file has already been attempted. */
    private boolean openAttempted;
    /** Writer for the active history file, or {@code null} while closed. */
    private BufferedWriter writer;

    private MessageHistoryRecorder() {
        appDirectory = null;
        currentFile = null;
        clock = Clock.systemUTC();
        noOp = true;
    }

    /**
     * Creates a recorder for one application program session.
     *
     * @param appDirectory directory containing the current and archived history
     *                     files
     * @param enabled whether recording starts immediately
     * @param clock time source used for event and archive timestamps
     */
    public MessageHistoryRecorder(
            final Path appDirectory,
            final boolean enabled,
            final Clock clock
    ) {
        this.appDirectory = appDirectory;
        this.currentFile = appDirectory.resolve(CURRENT_FILE_NAME);
        this.clock = clock;
        this.noOp = false;
        setEnabled(enabled);
    }

    /** Returns a recorder that deliberately discards every frame. */
    /* package */ static MessageHistoryRecorder disabled() {
        return NO_OP_RECORDER;
    }

    /** Enables or disables recording immediately for the current program session. */
    public void setEnabled(final boolean requestedEnabled) {
        lock.lock();
        try {
            if (!noOp && enabled != requestedEnabled) {
                enabled = requestedEnabled;
                if (requestedEnabled) {
                    if (!sessionReady) {
                        openAttempted = false;
                    }
                    ensureWriter();
                } else {
                    closeWriter();
                    openAttempted = false;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /** Records one frame received from ON4KST without its transport CR/LF. */
    public void recordRx(final String frame) {
        record("RX", frame);
    }

    /** Records one frame successfully sent to ON4KST without its transport CR/LF. */
    public void recordTx(final String frame) {
        record("TX", frame);
    }

    private void record(final String direction, final String frame) {
        lock.lock();
        try {
            if (enabled && !noOp) {
                ensureWriter();
                if (writer != null) {
                    final String safeFrame =
                            On4KstProtocol.redactSecretsForRecording(frame);
                    try {
                        writer.write(EVENT_TIMESTAMP.format(Instant.now(clock)));
                        writer.write('\t');
                        writer.write(direction);
                        writer.write('\t');
                        writer.write(safeFrame == null ? "" : safeFrame);
                        writer.newLine();
                        writer.flush();
                    } catch (IOException exception) {
                        if (LOGGER.isLoggable(Level.WARNING)) {
                            LOGGER.log(Level.WARNING,
                                    "Could not write ON4KST message history; "
                                            + "recording is disabled until it "
                                            + "is enabled again",
                                    exception);
                        }
                        closeWriter();
                        openAttempted = false;
                        enabled = false;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void ensureWriter() {
        if (!enabled || writer != null || openAttempted) {
            return;
        }
        openAttempted = true;

        try {
            Files.createDirectories(appDirectory);
            if (!sessionReady) {
                rotatePreviousSession();
                sessionReady = true;
            }
            writer = Files.newBufferedWriter(
                    currentFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException exception) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING,
                        "Could not initialize ON4KST message history at "
                                + currentFile,
                        exception);
            }
            closeWriter();
        }
    }

    private void rotatePreviousSession() throws IOException {
        if (!Files.isRegularFile(currentFile) || Files.size(currentFile) == 0L) {
            return;
        }

        final String baseName = "Messagehistory-"
                + ARCHIVE_TIMESTAMP.format(Instant.now(clock));
        Path archive = appDirectory.resolve(baseName + ".raw");
        int suffix = 1;
        while (Files.exists(archive)) {
            archive = appDirectory.resolve(
                    baseName + "-" + suffix + ".raw");
            suffix++;
        }
        try {
            Files.move(currentFile, archive, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(currentFile, archive);
        }
    }

    private void closeWriter() {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING,
                    "Could not close ON4KST message history", exception);
        } finally {
            writer = null;
        }
    }

    /** Stops recording and closes the current history file. */
    @Override
    public void close() {
        lock.lock();
        try {
            enabled = false;
            closeWriter();
        } finally {
            lock.unlock();
        }
    }
}
