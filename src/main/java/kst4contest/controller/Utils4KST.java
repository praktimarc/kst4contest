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
	 * Normalizes a chatmembers frequency-string for cluster usage<br/>
	 * <b>returns a frequency String in KHz like = "144300" or "144300.0" to match DXC protocol needs</b>
	 *
	 * @param optionalPrefix: if there is a value like ".300", it have to be decided, wich ".300": 144.300, 432.300, 1296.300 .... prefix means for example "144."
	 */
	public static String normalizeFrequencyString(String qrgString, SimpleStringProperty optionalPrefix) {

//        final String PTRN_QRG_CAT2 = "(([0-9]{3,4}[\\.|,| ]?[0-9]{3})([\\.|,][\\d]{1,2})?)|(([a-zA-Z][0-4]{1}[\\d]{2}\\b)([\\.|,][\\d]{1,2}\\b)?)|((\\b[0-4]{1}[\\d]{2}\\b)([\\.|,][\\d]{1,2}\\b)?)";

		try {
			qrgString = qrgString.replace(" ","");
		} catch (Exception e) {
			System.out.println("UTILS: QRG NULL, nothing to convert");
//			e.printStackTrace();
		}


		final String PTRN_QRG_CAT2_wholeQRGMHz4Digits = "(([0-9]{4}[\\.|,| ]?[0-9]{3})([\\.|,][\\d]{1,2})?)"; //1296.300.3 etc
		final String PTRN_QRG_CAT2_wholeQRGMHz3Digits = "(([0-9]{3}[\\.|,| ]?[0-9]{3})([\\.][\\d]{1,2})?)"; //144.300.3 etc
		final String PTRN_QRG_CAT2_QRGwithoutPrefix = "((\\b[0-4]{1}[\\d]{2}\\b)([\\.|,][\\d]{1,2}\\b)?)"; //144.300.3 etc

		String stringAggregation = "";

		if (testPattern(qrgString, PTRN_QRG_CAT2_wholeQRGMHz4Digits)) {//case 1296.200 or 1296.200.2 etc.
			stringAggregation = qrgString;

			stringAggregation = stringAggregation.replace(".","");
			stringAggregation = stringAggregation.replace(",","");
			stringAggregation = stringAggregation.replace(" ", "");

			if (stringAggregation.length() == 8) {
				String stringAggregationNew = stringAggregation.substring(0, stringAggregation.length()-1) + "." + stringAggregation.substring(stringAggregation.length()-1, stringAggregation.length());
				stringAggregation = stringAggregationNew + ".0";
				return stringAggregation;

			} else if (stringAggregation.length() == 9) {
				String stringAggregationNew = stringAggregation.substring(0, stringAggregation.length()-2) + "." + stringAggregation.substring(stringAggregation.length()-2, stringAggregation.length());
				stringAggregation = stringAggregationNew;
				return stringAggregation;
			}

		} else

		if (testPattern(qrgString, PTRN_QRG_CAT2_wholeQRGMHz3Digits)) { //case 144.300 or 144.300.2
			stringAggregation = qrgString;

			stringAggregation = stringAggregation.replace(".","");
			stringAggregation = stringAggregation.replace(",","");
			stringAggregation = stringAggregation.replace(" ", "");

			if (stringAggregation.length() == 6) {
				stringAggregation = stringAggregation + ".0";
				return stringAggregation;
			}
				if (stringAggregation.length() == 7) {
				String stringAggregationNew = stringAggregation.substring(0, stringAggregation.length()-1) + "." + stringAggregation.substring(stringAggregation.length()-1, stringAggregation.length());
				stringAggregation = stringAggregationNew + ".0";
				return stringAggregation;

			} else if (stringAggregation.length() == 8) {
				String stringAggregationNew = stringAggregation.substring(0, stringAggregation.length()-2) + "." + stringAggregation.substring(stringAggregation.length()-2, stringAggregation.length());
				stringAggregation = stringAggregationNew;
				return stringAggregation;
			}
		}
		else

		if (testPattern(qrgString, PTRN_QRG_CAT2_QRGwithoutPrefix)) { //case ".050 or .300 or something like that"
			stringAggregation = qrgString;

			stringAggregation = stringAggregation.replace(".", "");
			stringAggregation = stringAggregation.replace(",", "");
			stringAggregation = stringAggregation.replace(" ", "");

			if (stringAggregation.length() == 3) { // like 050 or 300
				String stringAggregationNew = optionalPrefix.getValue() + stringAggregation;
				stringAggregation = stringAggregationNew + ".0";
				return stringAggregation;

			} else if (stringAggregation.length() == 4) { //like 050.2 --> 0502

				stringAggregation = optionalPrefix.getValue() + stringAggregation;
				String stringAggregationNew = stringAggregation.substring(0, stringAggregation.length() - 1) + "." + stringAggregation.substring(stringAggregation.length() - 1, stringAggregation.length());
				stringAggregation = stringAggregationNew;
				return stringAggregation;

			} else if (stringAggregation.length() == 5) { //like 050.20 --> 05020

				stringAggregation = optionalPrefix.getValue() + stringAggregation;
				String stringAggregationNew = stringAggregation.substring(0, stringAggregation.length() - 2) + "." + stringAggregation.substring(stringAggregation.length() - 2, stringAggregation.length());
				stringAggregation = stringAggregationNew;
				return stringAggregation;
			}
		}

		return stringAggregation; //if nothing else helps
	}

}
