package kst4contest.locatorUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class DirectionUtilsConsoleOutputTest {

    @Test
    void outOfRangeCheckDoesNotWriteToStandardOutput() {
        PrintStream originalOutput = System.out;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();

        try (PrintStream testOutput = new PrintStream(
                capturedOutput,
                true,
                StandardCharsets.UTF_8)) {
            System.setOut(testOutput);

            assertFalse(DirectionUtils.isInAngleAndRange(
                    "JN49FL",
                    "JO43XM",
                    "JO30SA",
                    1.0,
                    50.0));
        } finally {
            System.setOut(originalOutput);
        }

        assertEquals("", capturedOutput.toString(StandardCharsets.UTF_8));
    }
}
