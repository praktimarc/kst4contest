package kst4contest.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kst4contest.model.ChatMember;

public class UCXLogFileToHashsetParser {

	public BufferedReader fileReader;
	private final String PTRN_CallSign = "(([a-zA-Z]{1,2}[\\d{1}]?\\/)?(\\d{1}[a-zA-Z][\\d{1}][a-zA-Z]{1,3})((\\/p)|(\\/\\d))?)|(([a-zA-Z0-9]{1,2}[\\d{1}]?\\/)?(([a-zA-Z]{1,2}(\\d{1}[a-zA-Z]{1,4})))((\\/p)|(\\/\\d))?)";

	public UCXLogFileToHashsetParser(String filePathAndName) {

		try {
			fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePathAndName))));

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * check if a line of the ucxlog-Logfile inhibits a Callsign<br/>
	 * <b>returns ChatMember = null, if no frequency found</b>
	 * 
	 * @param chatMessage
	 */
	private ChatMember checkIfLineInhibitsCallSign(String line) {

		Pattern pattern = Pattern.compile(PTRN_CallSign);
		Matcher matcher = pattern.matcher(line);

		String matchedString = "";

		while (matcher.find()) {

			matchedString = matcher.group();
//			System.out.println("[UCXLogFile:] Processed worked Callsign from file: " + matchedString);

		}

		ChatMember newChatMember = new ChatMember();

		newChatMember.setCallSign(matchedString.toUpperCase());

		return newChatMember;

	}

	/**
	 * Parses an ucxlog-live-file (full qualified path given by constructor
	 * argument), looks by regex for callsigns and builds a hashmap with only one
	 * entry by callsign
	 */
	public HashMap<String, String> parse() throws IOException {

		HashMap<String, String> chatMemberMap = new HashMap();

		String line;
		while ((line = fileReader.readLine()) != null) {
//			System.out.println("raw: " + line);
			ChatMember temp = checkIfLineInhibitsCallSign(line);

			if (temp.getCallSign() != "") {
				chatMemberMap.put(temp.getCallSign(), temp.getCallSign());
			}
		}
//		System.out.println(chatMemberMap.size());
		return chatMemberMap;

	}

}
