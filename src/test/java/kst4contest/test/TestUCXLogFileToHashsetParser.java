package kst4contest.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import kst4contest.controller.UCXLogFileToHashsetParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestUCXLogFileToHashsetParser {

	@TempDir
	Path temporaryDirectory;

	@Test
	void parsesUniqueWorkedBaseCallsignsWithFixedPattern() throws IOException {
		Path logFile = temporaryDirectory.resolve("contest.log");
		Files.writeString(logFile, String.join(System.lineSeparator(),
				"QSO 001 S53CC 144 MHz",
				"QSO 002 9A0BB-70 432 MHz",
				"QSO 003 s53cc repeated",
				"no station here"));

		Set<String> result = new UCXLogFileToHashsetParser(logFile.toString()).parse();

		assertEquals(Set.of("S53CC", "9A0BB"), result);
	}

	@Test
	void closesFileAfterEveryParsePass() throws IOException {
		Path logFile = temporaryDirectory.resolve("replaceable.log");
		Files.writeString(logFile, "QSO DL1ABC");

		UCXLogFileToHashsetParser parser = new UCXLogFileToHashsetParser(logFile.toString());
		assertEquals(Set.of("DL1ABC"), parser.parse());

		Path replacement = temporaryDirectory.resolve("replacement.log");
		Files.move(logFile, replacement);
		assertFalse(Files.exists(logFile));
		assertTrue(Files.exists(replacement));
	}
}
