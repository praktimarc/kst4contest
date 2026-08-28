package kst4contest.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kst4contest.model.ChatMember;

public class UCXLogFileToHashsetParser {

	private static final Pattern CALL_SIGN_PATTERN = Pattern.compile(
			"(([a-zA-Z]{1,2}[\\d]{1}?\\/)?(\\d{1}[a-zA-Z][\\d]{1}[a-zA-Z]{1,3})((\\/p)|(\\/\\d))?)"
					+ "|(([a-zA-Z0-9]{1,2}[\\d]{1}?\\/)?(([a-zA-Z]{1,2}(\\d{1}[a-zA-Z]{1,4})))((\\/p)|(\\/\\d))?)"
					+ "|([A-Z]\\d{2}[A-Z]{1,3})");

	private final Path logFile;

	public UCXLogFileToHashsetParser(String filePathAndName) {
		this.logFile = Path.of(filePathAndName);
	}

	private String findLastCallSign(String line) {
		Matcher matcher = CALL_SIGN_PATTERN.matcher(line);
		String matchedCallSign = "";

		while (matcher.find()) {
			matchedCallSign = matcher.group();
		}

		return matchedCallSign.toUpperCase(Locale.ROOT);
	}

	/**
	 * Parses the selected log file and returns every detected station as a
	 * normalized base callsign. The reader is closed after each pass so the logging
	 * application can continue replacing or rotating the file.
	 *
	 * @return unique normalized base callsigns found in the file
	 * @throws IOException if the file cannot be read
	 */
	public Set<String> parse() throws IOException {
		Set<String> workedBaseCalls = new HashSet<>();

		try (BufferedReader fileReader = Files.newBufferedReader(logFile, Charset.defaultCharset())) {
			String line;
			while ((line = fileReader.readLine()) != null) {
				String matchedCallSign = findLastCallSign(line);
				String baseCallSign = ChatMember.normalizeCallSignToBaseCallSign(matchedCallSign);
				if (baseCallSign != null && !baseCallSign.isBlank()) {
					workedBaseCalls.add(baseCallSign.toUpperCase(Locale.ROOT));
				}
			}
		}

		return workedBaseCalls;
	}
}
