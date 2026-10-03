package kst4contest.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** Controls the persistent application log without redirecting stdout/stderr. */
public final class ApplicationFileLogging implements AutoCloseable {
    /** Logger namespace owned by the application. */
    private static final String APP_LOGGER_NAME = "kst4contest";

    /** Runtime switch for additional application diagnostics. */
    private final AtomicBoolean debugEnabled = new AtomicBoolean();
    /** Strong reference to the root logger that owns the file handler. */
    private final Logger rootLogger = Logger.getLogger("");
    /** Strong reference to the application logger whose level is managed. */
    private final Logger appLogger = Logger.getLogger(APP_LOGGER_NAME);
    /** Application logger level that must be restored when this handler closes. */
    private final Level previousAppLevel;
    /** File handler installed on the root logger. */
    private final Handler handler;

    /**
     * Installs one append-only file handler.
     *
     * @param logFile target log file
     * @param debugEnabled whether INFO/FINE application diagnostics are included
     * @throws IOException if the handler cannot be created
     */
    public ApplicationFileLogging(
            final Path logFile,
            final boolean debugEnabled
    )
            throws IOException {
        final Path parent = logFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        this.debugEnabled.set(debugEnabled);
        previousAppLevel = appLogger.getLevel();
        appLogger.setLevel(Level.FINE);

        final FileHandler fileHandler =
                new FileHandler(logFile.toString(), true);
        fileHandler.setLevel(Level.ALL);
        fileHandler.setFilter(this::isLoggable);
        fileHandler.setFormatter(new UtcLogFormatter());
        handler = fileHandler;
        rootLogger.addHandler(handler);
    }

    /** Applies the runtime debug preference to subsequent file log records. */
    public void setDebugEnabled(final boolean enabled) {
        debugEnabled.set(enabled);
    }

    private boolean isLoggable(final LogRecord record) {
        boolean loggable = record.getThrown() != null
                || record.getLevel().intValue() >= Level.WARNING.intValue();
        if (!loggable) {
            final String loggerName = record.getLoggerName();
            final boolean appRecord = loggerName != null
                    && (APP_LOGGER_NAME.equals(loggerName)
                    || loggerName.startsWith(APP_LOGGER_NAME + "."));
            loggable = debugEnabled.get()
                    && appRecord
                    && record.getLevel().intValue() >= Level.FINE.intValue();
        }
        return loggable;
    }

    /** Removes and closes this handler. */
    @Override
    public void close() {
        rootLogger.removeHandler(handler);
        handler.close();
        appLogger.setLevel(previousAppLevel);
    }

    /** Formats file-log records with comparable UTC millisecond timestamps. */
    private static final class UtcLogFormatter extends Formatter {
        /** Timestamp format for every file-log record. */
        private static final DateTimeFormatter TIMESTAMP =
                DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'")
                        .withZone(ZoneOffset.UTC);

        @Override
        public String format(final LogRecord record) {
            final StringBuilder line = new StringBuilder()
                    .append(TIMESTAMP.format(Instant.ofEpochMilli(
                            record.getMillis())))
                    .append('\t')
                    .append(record.getLevel().getName())
                    .append('\t')
                    .append(record.getLoggerName())
                    .append('\t')
                    .append(formatMessage(record))
                    .append(System.lineSeparator());
            if (record.getThrown() != null) {
                final StringWriter stackTrace = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(stackTrace));
                line.append(stackTrace);
            }
            return line.toString();
        }
    }
}
