package kst4contest.controller;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

public class Utils4KST {

	public long time_generateCurrentEpochTime() {

		OffsetDateTime currentTimeInUtc = OffsetDateTime.now(ZoneOffset.UTC);

//	    System.out.println(currentTimeInUtc.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm X")));
		long millisecondsSinceEpoch = currentTimeInUtc.toInstant().toEpochMilli() / 1000;
//	    System.out.println(millisecondsSinceEpoch);
		return millisecondsSinceEpoch;
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
		DateFormat format = new SimpleDateFormat("dd.MM HH:mm:ss");
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

}
