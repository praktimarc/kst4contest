package kst4contest.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApplicationFileLoggingTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void warningsAreAlwaysLoggedAndDiagnosticsFollowDebugMode()
            throws Exception {
        Path logFile = temporaryDirectory.resolve("kst4contest-errors.log");
        Logger logger = Logger.getLogger(
                "kst4contest.test.filelogging." + UUID.randomUUID());

        try (ApplicationFileLogging logging =
                     new ApplicationFileLogging(logFile, false)) {
            logger.log(Level.INFO, "diagnostic while disabled");
            logger.log(Level.WARNING, "warning while disabled");
            logger.log(Level.INFO, "exception while disabled",
                    new IllegalStateException("simulated failure"));

            logging.setDebugEnabled(true);
            logger.log(Level.FINE, "fine diagnostic while enabled");
            logger.log(Level.INFO, "info diagnostic while enabled");
            logger.log(Level.SEVERE, "error while enabled");
        }

        String log = Files.readString(logFile);
        assertFalse(log.contains("diagnostic while disabled"));
        assertTrue(log.contains("warning while disabled"));
        assertTrue(log.contains("exception while disabled"));
        assertTrue(log.contains("simulated failure"));
        assertTrue(log.contains("fine diagnostic while enabled"));
        assertTrue(log.contains("info diagnostic while enabled"));
        assertTrue(log.contains("error while enabled"));
        assertTrue(log.contains("Z\tWARNING\t"));
    }
}
