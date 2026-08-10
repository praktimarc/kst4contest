package kst4contest.controller;

import javafx.beans.property.SimpleStringProperty;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils4KST {

	public long time_generateCurrentEpochTime() {

		OffsetDateTime currentTimeInUtc = OffsetDateTime.now(ZoneOffset.UTC);

//	    System.out.println(currentTimeInUtc.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm X")));
		long millisecondsSinceEpoch = currentTimeInUtc.toInstant().toEpochMilli() / 1000;
//	    System.out.println(millisecondsSinceEpoch);
		return millisecondsSinceEpoch;
	}

	public String time_generateCurrenthhmmZTimeStringForClusterMessage() {

		OffsetDateTime currentTimeInUtc = OffsetDateTime.now(ZoneOffset.UTC);
		System.out.println("Utils generated current time " + currentTimeInUtc + " --> " + currentTimeInUtc.format(DateTimeFormatter.ofPattern("HHmm"))+"Z");
		return currentTimeInUtc.format(DateTimeFormatter.ofPattern("HHmm"))+"Z";

	}

	public String time_generateCurrentMMDDhhmmTimeString() {

		OffsetDateTime currentTimeInUtc = OffsetDateTime.now(ZoneOffset.UTC);
		return currentTimeInUtc.format(DateTimeFormatter.ofPattern("MM-dd hh:mm"));

	}
	
	public String time_generateCurrentMMddString() {

		OffsetDateTime currentTimeInUtc = OffsetDateTime.now(ZoneOffset.UTC);
		return currentTimeInUtc.format(DateTimeFormatter.ofPattern("MM-dd"));

	}

	public String time_convertEpochToReadable(String epochFromServer) {
		
		long epoch = Long.parseLong(epochFromServer);
//		Instant instant = Instant.ofEpochSecond(epoch);

		Date date = new Date(epoch * 1000L);
//		DateFormat format = new SimpleDateFormat("dd.MM HH:mm:ss"); //old value which is too long
        DateFormat format = new SimpleDateFormat("H:mm:ss");
		format.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
		String formatted = format.format(date);
		  
//		System.out.println("UTIL " + formatted);
		  
		return formatted;
		
	}

	public static long time_getSecondsBetweenEpochAndNow(String epoch1) {

		long epoch1Long = Long.parseLong(epoch1);
		long epoch2Long = new Utils4KST().time_generateCurrentEpochTime();
//		Instant instant = Instant.ofEpochSecond(epoch);

		Date date = new Date(epoch1Long * 1000L);
		Date date2 = new Date(epoch2Long * 1000L);

		long seconds = Math.abs(date.getTime()-date2.getTime())/1000;

		return seconds;

	}
	
	public Date time_generateActualTimeInDateFormat() {
		Date date = new Date(time_generateCurrentEpochTime() * 1000L);
		return date;

	}


	/**
	 * This method tests a regexp-pattern against a given string
	 *
	 * @param testString: check if this string matches a given pattern
	 * @param regExPattern: pattern which should be checked
	 * @return true if match, else false
	 */
	private static boolean testPattern(String testString, String regExPattern) {

		Pattern pattern = Pattern.compile(regExPattern);
		Matcher matcher = pattern.matcher(testString);

		return matcher.find();
	}

	/**
	 * Converts a frequency detected in a chat message into the kHz representation
	 * used by the DX Cluster protocol.
	 *
	 * <p>Complete frequencies with two to five MHz digits are supported, for
	 * example 50.200, 144.205, 1296.338, 10368.100 and 24048.100. An optional
	 * second fractional group is retained as sub-kHz precision, for example
	 * 144.205.2 becomes 144205.2 kHz.</p>
	 *
	 * <p>Relative values such as .205 or 205 use the configured fallback-band
	 * prefix. The method only performs this fallback conversion after the general
	 * message parser has already accepted the value as a frequency.</p>
	 *
	 * @param qrgString frequency as detected by the chat parser
	 * @param optionalPrefix fallback MHz prefix for relative frequencies
	 * @return frequency in kHz for a DX Cluster spot, or an empty string if the
	 *         value cannot be converted safely
	 */
	public static String normalizeFrequencyString(
			String qrgString,
			SimpleStringProperty optionalPrefix
	) {
		if (qrgString == null || qrgString.isBlank()) {
			return "";
		}

		/*
		 * A comma is accepted as a decimal separator. Spaces are removed so older
		 * stored formats such as "432 088" remain usable.
		 */
		String normalizedValue = qrgString
				.trim()
				.replace(" ", "")
				.replace(',', '.');

		/*
		 * Complete frequency with an explicit separator:
		 *
		 * 50.200
		 * 144.205
		 * 1296.338
		 * 10368.100
		 * 144.205.2
		 */
		Matcher completeFrequencyMatcher = Pattern.compile(
				"^(\\d{2,5})\\.(\\d{1,3})(?:\\.(\\d{1,2}))?$"
		).matcher(normalizedValue);

		if (completeFrequencyMatcher.matches()) {
			return formatDxClusterFrequency(
					completeFrequencyMatcher.group(1),
					completeFrequencyMatcher.group(2),
					completeFrequencyMatcher.group(3)
			);
		}

		/*
		 * Compact complete frequency retained for compatibility:
		 *
		 * 144205
		 * 1296338
		 * 10368100
		 * 432088.2
		 */
		Matcher compactFrequencyMatcher = Pattern.compile(
				"^(\\d{2,5})(\\d{3})(?:\\.(\\d{1,2}))?$"
		).matcher(normalizedValue);

		if (compactFrequencyMatcher.matches()) {
			return formatDxClusterFrequency(
					compactFrequencyMatcher.group(1),
					compactFrequencyMatcher.group(2),
					compactFrequencyMatcher.group(3)
			);
		}

		/*
		 * Relative frequency. Values above 499 are deliberately rejected, matching
		 * the previous behaviour and preventing a report such as 599 from becoming
		 * a plausible-looking cluster frequency.
		 */
		Matcher relativeFrequencyMatcher = Pattern.compile(
				"^\\.?([0-4]\\d{2})(?:\\.(\\d{1,2}))?$"
		).matcher(normalizedValue);

		if (!relativeFrequencyMatcher.matches()) {
			return "";
		}

		String fallbackPrefix = optionalPrefix == null
				? null
				: optionalPrefix.getValue();

		if (fallbackPrefix == null) {
			return "";
		}

		fallbackPrefix = fallbackPrefix.trim();

		if (!fallbackPrefix.matches("\\d{2,5}")) {
			return "";
		}

		return formatDxClusterFrequency(
				fallbackPrefix,
				relativeFrequencyMatcher.group(1),
				relativeFrequencyMatcher.group(2)
		);
	}

	/**
	 * Formats an MHz part, a fractional MHz part and optional sub-kHz digits as a
	 * DX Cluster frequency in kHz.
	 *
	 * <p>The fractional MHz part is padded on the right because 144.2 means
	 * 144.200 MHz, while 144.21 means 144.210 MHz.</p>
	 *
	 * @param mhzPart complete MHz part
	 * @param fractionalMhzPart one to three digits following the first separator
	 * @param subKhzPart optional digits following a second separator
	 * @return DX Cluster frequency in kHz
	 */
	private static String formatDxClusterFrequency(
			String mhzPart,
			String fractionalMhzPart,
			String subKhzPart
	) {
		String paddedFraction =
				(fractionalMhzPart + "000").substring(0, 3);

		String subKhz = subKhzPart == null
				? "0"
				: subKhzPart;

		return mhzPart
				+ paddedFraction
				+ "."
				+ subKhz;
	}

}
