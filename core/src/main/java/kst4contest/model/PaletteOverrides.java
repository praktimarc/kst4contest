package kst4contest.model;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A set of palette overrides, encoded as one string.
 *
 * <p>One string per design rather than twelve XML elements: the preferences file is written
 * element by element by hand, and twelve more would be twelve write blocks, twelve read
 * blocks and twelve fields in a class that is already three thousand lines long.</p>
 *
 * <p>Reading is deliberately forgiving and never throws. The input is not always this
 * version's own: a hand-edited XML, a file from an older release, or a write interrupted
 * half way. An unknown key or an unparsable colour is skipped and the rest survives --
 * losing one colour is a nuisance, losing the readability of the client is not.</p>
 */
public final class PaletteOverrides {

	private static final Pattern SIX_DIGIT_HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");

	private static final String PAIR_SEPARATOR = ";";
	private static final String KEY_VALUE_SEPARATOR = "=";

	private PaletteOverrides() {
		// Utility class.
	}

	/**
	 * Reads a stored set of overrides.
	 *
	 * @param stored the stored string, may be null or malformed
	 * @return the roles actually named with a usable colour, never null
	 */
	public static Map<PaletteRole, String> parse(final String stored) {

		Map<PaletteRole, String> overrides = new LinkedHashMap<>();

		if (stored == null || stored.isBlank()) {
			return overrides;
		}

		for (String pair : stored.split(PAIR_SEPARATOR)) {

			int separator = pair.indexOf(KEY_VALUE_SEPARATOR);

			if (separator <= 0) {
				continue;
			}

			String key = pair.substring(0, separator).trim();
			String value = pair.substring(separator + 1).trim();

			if (!isValidColour(value)) {
				continue;
			}

			for (PaletteRole role : PaletteRole.values()) {
				if (role.key().equals(key)) {
					overrides.put(role, normalise(value));
					break;
				}
			}
		}

		return overrides;
	}

	/**
	 * Writes a set of overrides for storage.
	 *
	 * @param overrides the roles to store; entries with an unusable colour are left out
	 * @return the string to store, empty when there is nothing to store
	 */
	public static String format(final Map<PaletteRole, String> overrides) {

		if (overrides == null || overrides.isEmpty()) {
			return "";
		}

		StringBuilder stored = new StringBuilder();

		for (PaletteRole role : PaletteRole.values()) {

			String colour = overrides.get(role);

			if (!isValidColour(colour)) {
				continue;
			}

			if (stored.length() > 0) {
				stored.append(PAIR_SEPARATOR);
			}

			stored.append(role.key()).append(KEY_VALUE_SEPARATOR).append(normalise(colour));
		}

		return stored.toString();
	}

	/**
	 * Returns whether a string is a colour this application can use.
	 *
	 * <p>Six hexadecimal digits with a leading hash, and nothing else. The stylesheets use
	 * named colours and functions too, but those are the shipped sheets' business; what an
	 * operator types has one form so that what they typed is what they get.</p>
	 *
	 * @param colour the text to check, may be null
	 * @return true when it names a colour
	 */
	public static boolean isValidColour(final String colour) {
		return colour != null && SIX_DIGIT_HEX.matcher(colour.trim()).matches();
	}

	private static String normalise(final String colour) {
		return colour.trim().toUpperCase(Locale.ROOT);
	}
}
