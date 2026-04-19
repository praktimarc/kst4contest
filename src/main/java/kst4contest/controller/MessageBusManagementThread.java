package kst4contest.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
//import java.net.Socket;
//import java.util.ArrayList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import kst4contest.ApplicationConstants;
import kst4contest.locatorUtils.DirectionUtils;
import kst4contest.locatorUtils.Location;
import kst4contest.model.*;

/**
 * 
 * This thread is responsible for processing received messages.
 * It checks all messages from server for their functional contest, such as commands to build or change the userlist
 * or their settings, get clustermessages and sure the content of all chatmessages, which are delivered.
 * 
 */
public class MessageBusManagementThread extends Thread {

	int index;

    private String ThreadNickName = "MessageBus";
    private ThreadStatusCallback callBackToController;

	private PrintWriter writer;
//	private Socket socket;
	private ChatController client;
//	private File fileLogRAW;
//	private TimerTask userActualizationTask; // Is used as a temporary userout-print
//	private TimerTask userActualizationTask; //kst4contest.test 4 23001
	// private boolean serverReady = false; //kst4contest.test 4 23001
	private boolean serverReady = true;
	private Hashtable<String, ChatMember> chatMemberTable;
	private final String PTRN_USERLISTENTRY = "([a-zA-Z0-9]{2}/{1})?([a-zA-Z0-9]{1,3}[0-9][a-zA-Z0-9]{0,3}[a-zA-Z]{0,3})(/p)? [a-zA-Z]{2}[0-9]{2}[a-zA-Z]{2} [ -~]{1,20}";
	private final String PTRN_QRG_CAT2 = "(([0-9]{3,4}[\\.|,| ]?[0-9]{3})([\\.|,][\\d]{1,2})?)|(([a-zA-Z][0-4]{1}[\\d]{2}\\b)([\\.|,][\\d]{1,2}\\b)?)|((\\b[0-4]{1}[\\d]{2}\\b)([\\.|,][\\d]{1,2}\\b)?)";
	private final String PTRN_QRG_CAT3 = "(([0-9]{3,5}[\\.|,| ]?[0-9]{3})([\\.|,][\\d]{1,2})?)|(([a-zA-Z][0-4]{1}[\\d]{2}\\b)([\\.|,][\\d]{1,2}\\b)?)|((\\b[0-4]{1}[\\d]{2}\\b)([\\.|,][\\d]{1,2}\\b)?)";


	// ==== Autoanswer Flood/Pingpong Protection ====
	private static final String AUTOANSWER_PREFIX = ApplicationConstants.AUTOANSWER_PREFIX;   // hard-coded marker (user can't remove it)
	private static final long AUTOANSWER_COOLDOWN_MS = 45_000L;            //  45_000L = 45s

	// Cooldown per opponent station (and ChatCategory) – only setted if this client sends
	private final Hashtable<String, Long> lastLocalAutoAnswerPerRemoteMs = new Hashtable<>();

//	BufferedWriter bufwrtrDBGMSGOut;

//	    private String text;

//	public BufferedWriter getBufwrtrDBGMSGOut() {
//		return bufwrtrDBGMSGOut;
//	}
//
//	public void setBufwrtrDBGMSGOut(BufferedWriter bufwrtrDBGMSGOut) {
//		this.bufwrtrDBGMSGOut = bufwrtrDBGMSGOut;
//	}

	public boolean isServerready() {
		return serverReady;
	}

	public void setServerready(boolean serverReady) {
		this.serverReady = serverReady;
	}

	public MessageBusManagementThread(ChatController client, ThreadStatusCallback callBack) {

        this.callBackToController = callBack;
		this.client = client;

        ThreadStateMessage threadStateMessage = new ThreadStateMessage(this.ThreadNickName, true, "initialized", false);
        callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);

	}

	/**
	 * check if a chatmessage is part of the userlist via telnet 23000 port<br/>
	 * <b>Updates userlist!</b>
	 *
	 */
	private void checkIfItsUserListEntry(ChatMessage messageToProcess) {

		Pattern pattern = Pattern.compile(PTRN_USERLISTENTRY);
		Matcher matcher = pattern.matcher(messageToProcess.getMessageText());
		/**
		 * "([a-zA-Z0-9]{1,2}\/)?[a-zA-Z0-9]{1,3}[0-9][a-zA-Z0-9]{0,3}[a-zA-Z](\/(p|m))?(
		 * )[a-zA-Z]{2}[0-9]{2}[a-zA-Z]{2}[ -~]{0,30}" Thats a line of the show users
		 * list
		 */

		while (matcher.find()) {
//			System.out.println("Chatmember detected: "+ matcher.group() + " " + matcher.start());

			ChatMember member = new ChatMember();
			String matchedString = matcher.group();

			String[] splittedUserString;
			splittedUserString = matchedString.split(" ");

			member.setCallSign(splittedUserString[0]);
			member.setQra(splittedUserString[1]);

			String stringAggregation = "";
			for (int i = 2; i < splittedUserString.length; i++) {
				stringAggregation += splittedUserString[i] + " ";
			}
			member.setName(stringAggregation);

//			this.client.getChatMemberTable().put(member.getCallSign(), member);//deleted cause change if list type

//			if (member.getName().)

//			System.out.println("Processed Userlist Entry [" + this.client.getChatMemberTable().size() + "]: Call: "
//					+ member.getCallSign() + ", QRA: " + member.getQra() + ", Name: " + member.getName());
		}
	}

	/**
	 * check if a chatmessage is part of the userlist via telnet 23000 port<br/>
	 * <b>This method updates the userlist!</b>
	 */
	private void checkIfItsUserListEntry23001(ChatMessage messageToProcess) {

		Pattern pattern = Pattern.compile(PTRN_USERLISTENTRY);
		Matcher matcher = pattern.matcher(messageToProcess.getMessageText());
		/**
		 * "([a-zA-Z0-9]{1,2}\/)?[a-zA-Z0-9]{1,3}[0-9][a-zA-Z0-9]{0,3}[a-zA-Z](\/(p|m))?(
		 * )[a-zA-Z]{2}[0-9]{2}[a-zA-Z]{2}[ -~]{0,30}" Thats a line of the show users
		 * list
		 */

		while (matcher.find()) {
//			System.out.println("Chatmember detected: "+ matcher.group() + " " + matcher.start());

			ChatMember member = new ChatMember();
			String matchedString = matcher.group();

			String[] splittedUserString;
			splittedUserString = matchedString.split(" ");

			member.setCallSign(splittedUserString[0]);
			member.setQra(splittedUserString[1]);

			String stringAggregation = "";
			for (int i = 2; i < splittedUserString.length; i++) {
				stringAggregation += splittedUserString[i] + " ";
			}
			member.setName(stringAggregation);

//			this.client.getChatMemberTable().put(member.getCallSign(), member);

//			if (member.getName().)

//			System.out.println("[MSGBUSMGT:] Processed Userlist Entry [" + this.client.getChatMemberTable().size()
//					+ "]: Call: " + member.getCallSign() + ", QRA: " + member.getQra() + ", Name: " + member.getName());
		}
	}

	/**
	 * check if a chatmessage or a name of a chatmember contains a frequency<br/>
	 * <b>returns String = "" if no frequency found</b>
	 * 

	 */
	private String checkIfMessageInhibitsFrequency(ChatMessage messageToProcess) {

		Pattern pattern = Pattern.compile(PTRN_QRG_CAT2); // TODO: PTRN should depend to category-selection of own stn, it´s not the case now
		Matcher matcher = pattern.matcher(messageToProcess.getMessageText());
		String[] splittedQRGString;
//		splittedQRGString[0] = "0";

		String stringAggregation = "";


		while (matcher.find()) {
			String matchedString = matcher.group();

			splittedQRGString = matchedString.split(" ");

            for (String s : splittedQRGString) {
                stringAggregation += s + " ";
            }

			System.out.println("[MSGBUSMGT:] Processed qrg info: " + stringAggregation);

//			System.out.println("Processed QRG Entry [" + this.client.getChatMemberTable().size() + "]: Call: "
//					+ member.getCallSign() + ", QRA: " + member.getQra() + ", Name: " + member.getName());
		}
		return stringAggregation;
	}

	/**
	 * Smart Frequency Parser (V1.32)
	 * Replaces the old RegEx logic.
	 * Features:
	 * 1. Handles full frequencies (144.210) and short forms (.210, 210).
	 * 2. Handles extended precision/weird formatting (144.210.10, 144,210,10).
	 * 3. Prioritizes USER CONTEXT (History) over GLOBAL CONTEXT (Preferences).
	 */
	private void smartFrequencyExtraction(ChatMessage message, ChatPreferences prefs) {

		// Regex Explanation:
		// Part 1 (Full): Start (not digit), 3-5 digits, sep, 1-3 digits, OPTIONAL (sep, 1-3 digits)
		//                Matches: 144.210, 144.210.10, 10368.100
		// Part 2 (Short1): Start (not digit), sep, 3 digits, OPTIONAL (sep, 1-3 digits)
		//                Matches: .210, .210.10, ,210
		// Part 3 (Short2): Whitespace/Start, 3 digits, Whitespace/End
		//                Matches: " 210 ", " 144 "
		String smartPattern = "(?<![\\d])(\\d{3,5}[.,]\\d{1,3}(?:[.,]\\d{1,3})?)(?![\\d])|(?<![\\d])([.,]\\d{3}(?:[.,]\\d{1,3})?)(?![\\d])|(?<=\\s|^)(\\d{3})(?=\\s|$)";

		Pattern pattern = Pattern.compile(smartPattern);
		Matcher matcher = pattern.matcher(message.getMessageText());

		ChatMember sender = message.getSender();
		// Safety check, in case sender is null (e.g., server message)
		if (sender == null) return;

		while (matcher.find()) {
			String foundRaw = matcher.group().trim();

			// --- PRE-PROCESSING: Normalize separators ---
			// 1. Replace all commas with dots to unify format (144,210,10 -> 144.210.10)
			foundRaw = foundRaw.replace(",", ".");

			double finalDetectedFrequency = 0.0;
			Band finalDetectedBand = null;
			boolean isShortForm = false;

			// --- STEP 1: Type Determination (Short or Full?) ---

			// Check if it starts with a dot (e.g. ".210") OR is just 3 digits ("210")
			if (foundRaw.startsWith(".") || foundRaw.length() == 3) {

				// It is a short form.
				// We strip the leading dot for calculation if present -> "210.10" or "210"
				if (foundRaw.startsWith(".")) foundRaw = foundRaw.substring(1);
				isShortForm = true;

			} else {
				// It is a full frequency (e.g., 144.210.10 or 144.210)
				try {
					// Normalize "144.210.10" to "144.21010" for Double.parseDouble
					String normalizedFull = normalizeFrequencyString(foundRaw);

					finalDetectedFrequency = Double.parseDouble(normalizedFull);
					finalDetectedBand = Band.fromFrequency(finalDetectedFrequency);
				} catch (NumberFormatException e) { continue; }
			}

			// --- STEP 2: Context Resolution (Only needed for Short Forms) ---
			if (isShortForm) {

				// A) HISTORY CHECK (Priority 1: What did THIS USER do recently?)
				// We search for the most recent band where this short form makes physical sense.
				long bestTimestamp = 0;

				// Iterate over all bands where the user is known
				// (Assumption: ChatMember has a getter getKnownActiveBands())
				if (sender.getKnownActiveBands() != null) {
					for (java.util.Map.Entry<Band, ChatMember.ActiveFrequencyInfo> entry : sender.getKnownActiveBands().entrySet()) {

						Band candidateBand = entry.getKey();
						ChatMember.ActiveFrequencyInfo info = entry.getValue();

						// Timeout Check: Info must not be older than 30 mins (1,800,000 ms)
						if (System.currentTimeMillis() - info.timestampEpoch > 1800000) continue;

						// Try Reconstruction: Band Prefix + ShortForm
						// Example: Band 144 (Prefix "144") + "." + "210.10" -> "144.210.10"
						try {
							String reconstructedStr = candidateBand.getPrefix() + "." + foundRaw;
							String normalizedReconstruction = normalizeFrequencyString(reconstructedStr);

							double attemptFreq = Double.parseDouble(normalizedReconstruction);

							// Does this frequency fit into the candidate band?
							if (candidateBand.isPlausible(attemptFreq)) {
								// If we have multiple matches, pick the most recent one
								if (info.timestampEpoch > bestTimestamp) {
									finalDetectedFrequency = attemptFreq;
									finalDetectedBand = candidateBand;
									bestTimestamp = info.timestampEpoch;
								}
							}
						} catch (Exception e) { /* Ignore parsing errors */ }
					}
				}

				// B) GLOBAL PREFERENCES CHECK (Priority 2: Fallback if history is empty/old)
				if (finalDetectedBand == null) {
					// Get standard band from prefs (e.g., "144" or "432")
					String defaultPrefix = prefs.getNotify_optionalFrequencyPrefix().get();
					try {
						String reconstructedStr = defaultPrefix + "." + foundRaw;
						String normalizedReconstruction = normalizeFrequencyString(reconstructedStr);

						double attemptFreq = Double.parseDouble(normalizedReconstruction);

						// Check if this results in a valid amateur radio band
						Band defaultBandCandidate = Band.fromFrequency(attemptFreq);

						if (defaultBandCandidate != null) {
							finalDetectedFrequency = attemptFreq;
							finalDetectedBand = defaultBandCandidate;
						}
					} catch (NumberFormatException e) {
						// Number was likely not a frequency (e.g., "73" or "599") and didn't fit any band
						continue;
					}
				}
			}

			// --- STEP 3: Process Result ---
			if (finalDetectedBand != null && finalDetectedFrequency > 0) {

				// 1. Store in the new Map (for future context/history)

				sender.addKnownFrequency(finalDetectedBand, finalDetectedFrequency);

				//propagate known frequency to all instances of the same callsign (callRaw may exist multiple times)
				try {
					ArrayList<Integer> sameCallIdx = client.checkListForChatMemberIndexesByCallSign(sender);
					for (int idx : sameCallIdx) {
						ChatMember cm = client.getLst_chatMemberList().get(idx);
						if (cm != null && cm != sender) {
							cm.addKnownFrequency(finalDetectedBand, finalDetectedFrequency);
						}
					}
				} catch (Exception e) {
					System.out.println("[SmartParser, warning]: failed to propagate known frequency across duplicates: " + e.getMessage());
				}


				// 2. Set the old String-Property for GUI compatibility
				// We assume standard display format (MHz)
				sender.setFrequency(new javafx.beans.property.SimpleStringProperty(String.valueOf(finalDetectedFrequency)));

				System.out.println("[SmartParser] Detected for " + sender.getCallSign() + ": " +
						finalDetectedFrequency + " MHz (" + finalDetectedBand + ") " +
						(isShortForm ? "[derived from " + foundRaw + "]" : "[full match]"));

				// Optional: Trigger Cluster-Spot here if enabled
			}
		}
	}

	/**
	 * Helper: Normalizes weird frequency formats to valid Double strings.
	 * Example: "144.210.10" -> "144.21010"
	 * Example: "144.210"    -> "144.210"
	 */
	private String normalizeFrequencyString(String rawInput) {
		// Input is already guaranteed to have only dots as separators (commas replaced earlier)

		int firstDotIndex = rawInput.indexOf(".");

		if (firstDotIndex != -1) {
			// Check if there are more dots after the first one
			String decimalPart = rawInput.substring(firstDotIndex + 1);
			if (decimalPart.contains(".")) {
				// Remove all subsequent dots to make it a valid double
				decimalPart = decimalPart.replace(".", "");
				return rawInput.substring(0, firstDotIndex) + "." + decimalPart;
			}
		}
		return rawInput;
	}


	/**
	 * Builds UserList and gets meta informations out of the chat, as far as it is
	 * possible. \n This is the only place where the Chatmember-List will be written
	 *
	 * Old Method for port 23000, raw text interface without any comfort, no longer used
	 * @param messageToProcess
	 */
	private void processRXMessage23000(ChatMessage messageToProcess) {

		String reduce;

		reduce = new String(messageToProcess.getMessageText());
		reduce = reduce.replaceAll("\\s+", " "); // reduce bursts of spaces to one space sign

		messageToProcess.setMessageText(reduce);

		if (messageToProcess.getMessageText().isEmpty()) {
//			System.out.println("[MSGBUSMGTT:] ###################### no processable data");
		} else {

			if (reduce.length() >= 14 && reduce.length() <= 40) {
				checkIfItsUserListEntry(messageToProcess); // 23001 kst4contest.test unneccessary
			}

			checkIfMessageInhibitsFrequency(messageToProcess);
		}
	}

//	private boolean isUserInTheUserTable(String chatMemberCallsign) {
//
//		String checkThisCallsign = chatMemberCallsign;
//
//		if (this.client.getChatMemberTable().containsKey(checkThisCallsign)) {
//			return true;
//		} else
//			return false;
//
//	}

	/**
	 * checks if the callsign-String of a given chatmember instance and a given list
	 * instance is in the list. If yes, returns the index in the List, <b>if not,
	 * returns -1.</b>
	 * 
	 * @param lookForThis
	 * @return Integer (index), -1 for not found
	 */
	private int checkListForChatMemberIndexByCallSign(ObservableList<ChatMember> list, ChatMember lookForThis) {

		if (lookForThis == null) {

			System.out.println(
					"[ChecklistForChatMemberIndexByCallsign] ERROR: null Value for Chatmember detected! Member cannot be in the list!");
			return -1;
		} else if (lookForThis.getCallSign() == null) {
			System.out.println(
					"[ChecklistForChatMemberIndexByCallsign] ERROR: null Value in Callsign detected! Member cannot be in the list!");
			return -1;
		}
		/***
		 * Old mechanic for index search, new one implemented due concurrentmodificationexc, which works - start
		 * 
		 */
//		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
//			ChatMember chatMember = (ChatMember) iterator.next();
//			if (chatMember.getCallSign().equals(lookForThis.getCallSign())) {
////				System.out
////						.println("MSGBUSHELBER: Found " + chatMember.getCallSign() + " at " + list.indexOf(chatMember));
//
//				return list.indexOf(chatMember);
//			} else {
//
//			}
//		}
//
//		System.out.println("[MsgBusMgr, ERROR:] ChecklistForChatMemberIndexByCallsign, not found: "
//				+ lookForThis.getCallSign() + "\n ");
		/***
		 * /Old mechanic for index search,new one implemented due concurrentmodificationexc which works - end
		 * 
		 */

		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getCallSign().equals(lookForThis.getCallSign())) {
				//TODO: New since 1.26! Check against category!

				System.out.println("MSGBUSMGT, DEBUG: Checking Chatcategories of found list member " + list.get(i).getCallSign() + " / " + list.get(i).getChatCategory() +  " against " + lookForThis.getCallSign() + " / " + lookForThis.getChatCategory());

//				System.out
//						.println("MSGBUSHELBER: Found " + chatMember.getCallSign() + " at " + list.indexOf(chatMember));

				if (list.get(i).getChatCategory().equals(lookForThis.getChatCategory())) { //new 1.26

					return list.indexOf(list.get(i));
				} //new 1.26
				else {
					System.out.println("MSGBUSMGT, DEBUG: Category does not match");

				}

//				System.out.println("--------------------------- chatcategory of list.get(i) = " + list.get(i).getChatCategory().getCategoryNumber());
				System.out.println("--------------------------- chatcategory of lookforthisChatMember = " + lookForThis.getChatCategory().getCategoryNumber() );
			}
//				return list.indexOf(list.get(i)); //if no category found, return entry //TODO: ERROR detected here! Should work now, needs some proof
//				return -1; //if category dont match, return: member not found
		}
		return -1; // if it´s not found, the method will always end here and return -1
	}

	/**
	 * Processes received messages via port 23001 (improved telnet Interface)
	 * 
	 * @param messageToProcess
	 * @throws IOException
	 * @throws SQLException
	 */
	private void processRXMessage23001(ChatMessage messageToProcess) throws IOException, SQLException {

        ThreadStateMessage threadStateMessage = new ThreadStateMessage(this.ThreadNickName, true, "Last message processed:\n" + messageToProcess.getMessageText(), false);
        callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);

		final String INITIALUSERLISTENTRY = "UA0";
		final String USERENTEREDCHAT = "UA5";
		final String USERENTEREDCHAT2 = "UA2"; // seen at 50MHZ Chat
		final String initialChatHistoryEntry = "CR";
		final String SERVERMESSAGEHISTORIC = "CR"; //takes messages out of the ON4KST history
		final String USERLEFTCHAT = "UR6";
		final String USERLEFTCHAT2 = "UR7";
		final String CHATCHANNELMESSAGE = "CH";
		final String REGISTREDUSERCOUNT = "UE";
		final String USERSTATECHANGE = "US4";
		final String USERLOCATORCHANGE = "LOC";
		final String USERINFOUPDATEORUSERISBACK = "UM3";
		final String DXCLUSTERMESSAGE1 = "DM";
		final String DXCLUSTERMESSAGE2 = "DL";
		final String DXCLUSTERMESSAGE3 = "MA";
		final String SRVR_DXCEND = "DF";
		final String SRVR_USERLISTEND = "UE";
		final String SRVR_COMMUNICATIONK = "CK";
		final String SRVR_LOGSTAT = "LOGSTAT";
		final String SRVR_LOGSTAT_WRONGPASSWORD = "Wrong password!";
		final String SRVR_LOGINOK = "100";
		final String SRVR_LOGINWRONGPW = "114";
		final String SRVR_LOGINWRONGEMPTYCALL = "102";
		final String SRVR_LOGINWRONGCALLSYNTAX = "103";
		final String SRVR_LOGINWRONGCALLUNKNOWN = "101";

		/**
		 * here we have a helper Set for identifying questions for my qrg which can be autoanswered later // TODO: move to an extra method
		 */
		final HashSet<String> qrgQuestionTexts = new HashSet<String>();
//		final ArrayList<String> qrgQuestionTexts = new ArrayList<String>();
		qrgQuestionTexts.add("ur qrg?");
		qrgQuestionTexts.add("your qrg?");
		qrgQuestionTexts.add("qrg?");
		qrgQuestionTexts.add("freq?");
		qrgQuestionTexts.add("pse qrg");


		/**
		 * here we have a helper list for identifying questions for my qrg which can be autoanswered later
		 */

		if (messageToProcess.getMessageText().isEmpty()) {
//			System.out.println("[MSGBUSMGTT:] no processable data");

		} else {

			if (messageToProcess.getMessageText().contains(SRVR_LOGSTAT)) {
				String logstatMessage[];
				logstatMessage = messageToProcess.getMessageText().split("\\|");
				if (logstatMessage[1].contains(SRVR_LOGINOK)) {
					this.client.setConnectedAndLoggedIn(true);
				} else {
					this.client.setConnectedAndNOTLoggedIn(true);
					this.client.setConnectedAndLoggedIn(false);
				}
			}

			String splittedMessageLine[] = messageToProcess.getMessageText().split("\\|");

			/**
			 * Initializes the Userlist if entry fits UA0
			 * UA0|3|DL6SAQ|walter not qrv|JN58CK|1| <- RXed
			 *
             * 
			 */
			if (splittedMessageLine[0].contains(INITIALUSERLISTENTRY)) {
//				System.out.println("MSGBUS: User detected");

				ChatMember newMember = new ChatMember();

				newMember.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());

				newMember.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));

				newMember.setCallSign(splittedMessageLine[2]);
				newMember.setName(splittedMessageLine[3]);
				newMember.setQra(splittedMessageLine[4]);
				newMember.setState(Integer.parseInt(splittedMessageLine[5]));
//				newMember.setQTFdirection(LocatorUtils);
				newMember.setQrb(new Location().getDistanceKmByTwoLocatorStrings(client.getChatPreferences().getStn_loginLocatorMainCat(), newMember.getQra()));
				newMember.setQTFdirection(new Location(client.getChatPreferences().getStn_loginLocatorMainCat()).getBearing(new Location(newMember.getQra())));
				newMember.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());//TODO evt obsolete!
				newMember.setActivityTimeLastInEpoch(new Utils4KST().time_generateCurrentEpochTime());


				if (!client.getChatPreferences().getStn_loginCallSign().equals(newMember.getCallSign())) {
					this.client.getLst_chatMemberList().add(newMember); //the own call will not be in the list
				}


				this.client.getDbHandler().storeChatMember(newMember);


//				bufwrtrDBGMSGOut.write(new Utils4KST().time_generateCurrentMMDDhhmmTimeString()
//						+ "[MSGBUSMGT:] User detected and added to list [" + this.client.getChatMemberTable().size()
//						+ "] :" + newMember.getCallSign() + "\n");
//				bufwrtrDBGMSGOut.flush();
//				System.out.println("[MSGBUSMGT:] User detected and added to list ["
//						+ this.client.getChatMemberTable().size() + "] :" + newMember.getCallSign());
			} else

			/**
			 * Actualize Userlist, add new entry UA5 or UA2
			 *
			 * UA5|2|IU4CHE|Giorgio 2-70-23|JN64GB|2|
			 * UA2|2|W5ADD|Parker|EM40WL|2|
			 *
			 */
			if (splittedMessageLine[0].contains(USERENTEREDCHAT) || splittedMessageLine[0].contains(USERENTEREDCHAT2)) {
//				System.out.println("MSGBUS: User detected");


				if (!client.getChatPreferences().getStn_loginCallSign().equals(splittedMessageLine[2])) { //own call ignore

					ChatMember newMember = new ChatMember();

					newMember.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());

					newMember.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));

					newMember.setCallSign(splittedMessageLine[2]);
					newMember.setName(splittedMessageLine[3]);
					newMember.setQra(splittedMessageLine[4]);
					newMember.setState(Integer.parseInt(splittedMessageLine[5]));
					newMember.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());
					newMember.setActivityTimeLastInEpoch(new Utils4KST().time_generateCurrentEpochTime());
					newMember.setQrb(new Location().getDistanceKmByTwoLocatorStrings(client.getChatPreferences().getStn_loginLocatorMainCat(), newMember.getQra()));
					newMember.setQTFdirection(new Location(client.getChatPreferences().getStn_loginLocatorMainCat()).getBearing(new Location(newMember.getQra())));

					newMember = this.client.getDbHandler().fetchChatMemberWkdDataForOnlyOneCallsignFromDB(newMember);

					this.client.getLst_chatMemberList().add(newMember);

					this.client.getDbHandler().storeChatMember(newMember);
				}


                this.client.fireUserListUpdate("User entered the chat");

//				this.client.getChatMemberTable().put(splittedMessageLine[2], newMember);

//				System.out.println("[MSGBUSMGT:] New entered User detected and added to list ["
//						+ this.client.getChatMemberTable().size() + "] :" + newMember.getCallSign());
			} else

			/**
			 * Actualize Userlist, remove entry UR6, UR7
			 */
			if (splittedMessageLine[0].contains(USERLEFTCHAT) || splittedMessageLine[0].contains(USERLEFTCHAT2)) {
//					System.out.println("MSGBUS: User detected");

				ChatMember newMember = new ChatMember();

				newMember.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));

				newMember.setCallSign(splittedMessageLine[2]);

				System.out.println("[MSGBUSMGT, Info:] User left Chat and will be removed from list ["
						+ this.client.getLst_chatMemberList().size() + "] :" + newMember.getCallSign());
				try {
					this.client.getLst_chatMemberList().remove(
							checkListForChatMemberIndexByCallSign(this.client.getLst_chatMemberList(), newMember));

					//since 1.26 new method design to detect chatcategory, too!

				} catch (Exception e) {
					System.out.println("[MSGBUSMGT, EXC!, Error:] User sent left chat but had not been there ... ["
							+ this.client.getLst_chatMemberList().size() + "] :" + newMember.getCallSign() + "\n"
							+ e.getStackTrace());
				}

			} else

			/**
			 * Chatmessage dm5m to do5amf CH|2|1663966534|DM5M|dm5m-team|0|kst4contest.test|DO5AMF|
			 * 
			 * CH|2|1663966535|DM5M|dm5m-team|0|kst4contest.test|0|
			 */
			if (splittedMessageLine[0].contains(CHATCHANNELMESSAGE)) {

				//experimental 1.26: multi channel messages
				ChatMessage newMessageArrived = new ChatMessage();
				ChatCategory chategoryForMessageAndMessageSender;

				newMessageArrived.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));

				chategoryForMessageAndMessageSender = newMessageArrived.getChatCategory();
				newMessageArrived.setMessageGeneratedTime(splittedMessageLine[2]);

				if (splittedMessageLine[3].equals("SERVER")) {
					ChatMember dummy = new ChatMember();
					dummy.setCallSign("SERVER");
					dummy.setName("Sysop");
					newMessageArrived.setSender(dummy);
					newMessageArrived.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));
					dummy.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));
//					System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> servers cat " + newMessageArrived.getChatCategory());

				} else {

					ChatMember sender = new ChatMember();
					sender.setCallSign(splittedMessageLine[3]);
					sender.setChatCategory(chategoryForMessageAndMessageSender);

					int index = checkListForChatMemberIndexByCallSign(this.client.getLst_chatMemberList(), sender);


					if (index != -1) {
						//user not found in the chatmember list
						try {
//							newMessageArrived.setSender(this.client.getLst_chatMemberList().get(index)); // set sender to member of
//							this.client.getLst_chatMemberList().get(index).setActivityTimeLastInEpoch(new Utils4KST().time_generateCurrentEpochTime());

							ChatMember senderObj = this.client.getLst_chatMemberList().get(index);
							newMessageArrived.setSender(senderObj);
							senderObj.setActivityTimeLastInEpoch(new Utils4KST().time_generateCurrentEpochTime());

							// Remember last inbound category per callsignRaw (required for correct send-routing later)
							this.client.rememberLastInboundCategory(senderObj.getCallSignRaw(), senderObj.getChatCategory());

							// Metrics for scoring: momentum, response-time, no-reply, positive signals
							this.client.getStationMetricsService().onInboundMessage(
									senderObj.getCallSignRaw(),
									System.currentTimeMillis(),
									newMessageArrived.getMessageText(),
									this.client.getChatPreferences(),
									this.client.getChatPreferences().getStn_loginCallSign()
							);

							// Activity/category changes influence priority => request recompute
							this.client.getScoreService().requestRecompute("rx-chat-message");

						} catch (Exception exc) {
							ChatMember aSenderDummy = new ChatMember();
							aSenderDummy.setCallSign(splittedMessageLine[3] + "[n/a]");
							aSenderDummy.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());
							newMessageArrived.setSender(aSenderDummy);
							System.out.println("MsgBusmgtT: Catched Error! " + exc.getMessage() + " // " + splittedMessageLine[3] + " is not in the list! Faking sender!");
							exc.printStackTrace();
						}
																								// b4 init list
					} else {
						//user not found in chatmember list, mark it, sender can not be set
						if (!sender.getCallSign().equals(this.client.getChatPreferences().getStn_loginCallSign().toUpperCase())) {
							sender.setCallSign("[n/a]" + sender.getCallSign());
							// if someone sent a message without being in the userlist (cause
							// on4kst missed implementing....), callsign will be marked
						} else {
							//that means, message was by own station, broadcasted to all other
							ChatMember dummy = new ChatMember();
							dummy.setCallSign("ALL");
							newMessageArrived.setReceiver(dummy);

							AirPlaneReflectionInfo preventNullpointerExc = new AirPlaneReflectionInfo();
							preventNullpointerExc.setAirPlanesReachableCntr(0);
							sender.setAirPlaneReflectInfo(preventNullpointerExc);
							newMessageArrived.setSender(sender); //my own call is the sender
						}
					}

//					newMessageArrived.setSender(this.client.getChatMemberTable().get(splittedMessageLine[3]));
				}

				newMessageArrived.setMessageSenderName(splittedMessageLine[4]);
				newMessageArrived.setMessageText(splittedMessageLine[6]);

				if (splittedMessageLine[7].equals("0")) {
					// message is not directed to anyone, move it to the cq messages!
					ChatMember dummy = new ChatMember();
					dummy.setCallSign("ALL");
					newMessageArrived.setReceiver(dummy);

					this.client.getLst_globalChatMessageList().add(0, newMessageArrived); // sdtout to all message-List

				} else {
					//message is directed to another chatmember, process as such!

					ChatMember receiver = new ChatMember();

					receiver.setChatCategory(chategoryForMessageAndMessageSender); //got out of message itself

					receiver.setCallSign(splittedMessageLine[7]);

					int index = checkListForChatMemberIndexByCallSign(this.client.getLst_chatMemberList(), receiver);

					if (index != -1) {
						newMessageArrived.setReceiver(this.client.getLst_chatMemberList().get(index));// -1: Member left Chat
																								// before...
					} else { //found in active member list


						if (receiver.getCallSign().equals(client.getChatPreferences().getStn_loginCallSign())) {
							/**
							 * If mycallsign sent a message to the server, server will publish that message and
							 * send it to all chatmember including me.
							 * As mycall is not in the userlist,  the message would not been displayed if I handle
							 * it in the next case (marking left user, just for information). But I want an echo.
							 */

							receiver.setCallSign(client.getChatPreferences().getStn_loginCallSign());
							newMessageArrived.setReceiver(receiver);
						} else {
							//this are user which left chat but had been adressed by this message
							receiver.setCallSign(receiver.getCallSign() + "(left)");
							newMessageArrived.setReceiver(receiver);
						}
					}

//					System.out.println("message directed to: " + newMessageArrived.getReceiver().getCallSign() + ". EQ?: " + this.client.getownChatMemberObject().getCallSign() + " sent by: " + newMessageArrived.getSender().getCallSign().toUpperCase() + " -> EQ?: "+ this.client.getChatPreferences().getLoginCallSign().toUpperCase());

					try {
						/**
						 * message is directed to me, will be put in the "to me" messagelist
						 */
						if (newMessageArrived.getReceiver().getCallSign()
								.equals(this.client.getChatPreferences().getStn_loginCallSign())) {

							this.client.getLst_globalChatMessageList().add(0, newMessageArrived);

							if (this.client.getChatPreferences().isNotify_playSimpleSounds()) {
								this.client.getPlayAudioUtils().playNoiseLauncher('P');
							}
							if (this.client.getChatPreferences().isNotify_playCWCallsignsOnRxedPMs()) {
								this.client.getPlayAudioUtils().playCWLauncher(" " + " " + newMessageArrived.getSender().getCallSign().toUpperCase());
							}
							if (this.client.getChatPreferences().isNotify_playVoiceCallsignsOnRxedPMs()) {
								this.client.getPlayAudioUtils().playVoiceLauncher( "?" + newMessageArrived.getSender().getCallSign().toUpperCase());
							}

							if (this.client.getChatPreferences().isNotify_playSimpleSounds()) {
								if (newMessageArrived.getMessageText().toUpperCase().contains("//BELL")) {
									this.client.getPlayAudioUtils().playVoiceLauncher("!");
								}
							}
							if (newMessageArrived.getMessageText().toUpperCase().contains("//VER")) {

								ChatMessage versionInfo = new ChatMessage();
								ChatMember itsMe = new ChatMember();
								itsMe.setCallSign(this.client.getChatPreferences().getStn_loginCallSign());

								versionInfo.setSender(itsMe);
								versionInfo.setReceiver(newMessageArrived.getSender());
								versionInfo.setMessageText("/CQ " + newMessageArrived.getSender().getCallSign() + " " + ApplicationConstants.AUTOANSWER_PREFIX + " " + "KST4Contest " + " v" + ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER + " by DO5AMF");

								this.client.getMessageTXBus().add(versionInfo);
							}

//							if (this.client.getChatPreferences().isMsgHandling_autoAnswerEnabled()) {
//
//								ChatMessage automaticAnswer = new ChatMessage();
//								ChatMember itsMe = new ChatMember();
//								itsMe.setCallSign(this.client.getChatPreferences().getStn_loginCallSign());
//
//								automaticAnswer.setSender(itsMe);
//								automaticAnswer.setReceiver(newMessageArrived.getSender());
//								automaticAnswer.setMessageText("/CQ " + newMessageArrived.getSender().getCallSign() + " " + this.client.getChatPreferences().getMessageHandling_autoAnswerTextMainCat());
//
//								this.client.getMessageTXBus().add(automaticAnswer);
//
//							}

							/**
							 * auto reply/answer to QRG requests is here
							 */
//							if (this.client.getChatPreferences().isMessageHandling_autoAnswerToQRGRequestEnabled()) {
//
//								for (String lookForQRGString : qrgQuestionTexts) {
//									if (newMessageArrived.getMessageText().contains(lookForQRGString)) {
//
//										ChatMessage automaticAnswer = new ChatMessage();
//										ChatMember itsMe = new ChatMember();
//										itsMe.setCallSign(this.client.getChatPreferences().getStn_loginCallSign());
//
//										automaticAnswer.setSender(itsMe);
//										automaticAnswer.setReceiver(newMessageArrived.getSender());
//										automaticAnswer.setMessageText("/CQ " + newMessageArrived.getSender().getCallSign() + " KST4Contest Auto: QRG is: " + this.client.getChatPreferences().getMYQRGFirstCat().getValue());
//
//										if (this.client.getChatPreferences().isLoginToSecondChatEnabled()) {
//											automaticAnswer.setMessageText("/CQ " + newMessageArrived.getSender().getCallSign() + " KST4Contest Auto: QRGs: " + this.client.getChatPreferences().getMYQRGFirstCat().getValue() + " / " + this.client.getChatPreferences().getMYQRGSecondCat().getValue());
//										} else {
//											automaticAnswer.setMessageText("/CQ " + newMessageArrived.getSender().getCallSign() + " KST4Contest Auto: QRG is: " + this.client.getChatPreferences().getMYQRGFirstCat().getValue());
//										}
//
//										this.client.getMessageTXBus().add(automaticAnswer);
//
//									}
//								}
//							}

							// ==== Unified Autoanswer (Generic + QRG) with Pingpong-Guard + per-Remote Cooldown ====
							final String incomingText = newMessageArrived.getMessageText();
							final String incomingLower = (incomingText == null) ? "" : incomingText.toLowerCase(Locale.ROOT);

							// 1) Pingpong-security: never ever react to auto generated messages
							if (!isAutoMessage(newMessageArrived)) {

								boolean qrgRequested = false;

								if (this.client.getChatPreferences().isMessageHandling_autoAnswerToQRGRequestEnabled()) {
									for (String lookForQRGString : qrgQuestionTexts) {
										if (incomingLower.contains(lookForQRGString)) {
											qrgRequested = true;
											break;
										}
									}
								}

								boolean genericEnabled = this.client.getChatPreferences().isMsgHandling_autoAnswerEnabled();

								// 2) Entscheide, ob überhaupt geantwortet wird (QRG hat Vorrang vor Generic)
								String payload = null;

								if (qrgRequested) {

									if (this.client.getChatPreferences().isLoginToSecondChatEnabled()) {
										payload = "QRGs: " + this.client.getChatPreferences().getMYQRGFirstCat().getValue()
												+ " / " + this.client.getChatPreferences().getMYQRGSecondCat().getValue();
									} else {
										payload = "QRG is: " + this.client.getChatPreferences().getMYQRGFirstCat().getValue();
									}

								} else if (genericEnabled) {

									payload = this.client.getChatPreferences().getMessageHandling_autoAnswerTextMainCat();
								}

								// 3) Cooldown pro Gegenstation: nur wenn DIESER Client jetzt wirklich sendet
								if (payload != null && isAutoAnswerAllowedNow(newMessageArrived)) {

									ChatMessage automaticAnswer = new ChatMessage();
									ChatMember itsMe = new ChatMember();
									itsMe.setCallSign(this.client.getChatPreferences().getStn_loginCallSign());

									automaticAnswer.setSender(itsMe);
									automaticAnswer.setReceiver(newMessageArrived.getSender());

									// Prefix fest + nicht entfernbar, damit Auto↔Auto nicht pingpongt
									automaticAnswer.setMessageText("/CQ " + newMessageArrived.getSender().getCallSign()
											+ " " + AUTOANSWER_PREFIX + " " + payload);

									this.client.getMessageTXBus().add(automaticAnswer);

									// Cooldown wird NUR hier gesetzt (nicht bei 'message sent by me' Echo),
									// damit nur lokale Auto-Sends zählen.
									markLocalAutoAnswerSent(newMessageArrived);
								}
							}


							System.out.println("message directed to me: " + newMessageArrived.getReceiver().getCallSign() + ".");

						} else if (newMessageArrived.getSender().getCallSign().toUpperCase()
								.equals(this.client.getChatPreferences().getStn_loginCallSign().toUpperCase())) {
								/**
								 * message sent by me!
								 * message from me will appear in the PM window, too, with (>CALLSIGN) before
								 */
							String originalMessage = newMessageArrived.getMessageText();
							newMessageArrived
									.setMessageText("(>" + newMessageArrived.getReceiver().getCallSign() + ")" + originalMessage);
							this.client.getLst_globalChatMessageList().add(0,newMessageArrived);

							// If our message contained a frequency (e.g. "QRG is: 144.375"), record that
							// WE sent our QRG to this OM – used by SKED frequency resolution.
							if (originalMessage != null && newMessageArrived.getReceiver() != null
									&& originalMessage.matches(".*\\b\\d{3,5}[.,]\\d{1,3}.*")) {
								this.client.recordOutboundQRG(newMessageArrived.getReceiver().getCallSign());
							}

							// if you sent the message to another station, it will be sorted in to
							// the "to me message list" with modified messagetext, added rxers callsign

						} else {
							//message sent to other user
							if (DirectionUtils.isInAngleAndRange(client.getChatPreferences().getStn_loginLocatorMainCat(),
									newMessageArrived.getSender().getQra(),
									newMessageArrived.getReceiver().getQra(),
									client.getChatPreferences().getStn_maxQRBDefault(),
									client.getChatPreferences().getStn_antennaBeamWidthDeg())) {

								if (this.client.getChatPreferences().isNotify_playSimpleSounds()) {
									//play only tick sound if the sender was not set directedtome before
									if (!newMessageArrived.getSender().isInAngleAndRange()) {
										this.client.getPlayAudioUtils().playNoiseLauncher('-');
									}
								}

								newMessageArrived.getSender().setInAngleAndRange(true);

								if (client.getChatPreferences().isNotify_dxClusterServerEnabled()) {
									try {
										if (newMessageArrived.getSender().getFrequency() != null) {
                                            //TODO: testing for next version 3.33: addinitional information will be displayed in cluster if there is such an information
                                            ChatMember onlyForSpottingObject = new ChatMember();
                                            onlyForSpottingObject.setCallSign(newMessageArrived.getSender().getCallSign());
                                            onlyForSpottingObject.setFrequency(newMessageArrived.getSender().getFrequency());

                                            if (newMessageArrived.getSender().getAirPlaneReflectInfo().getAirPlanesReachableCntr() > 0) {
                                                onlyForSpottingObject.setQra(newMessageArrived.getSender().getQra() + " , AP: " +
                                                        newMessageArrived.getSender().getAirPlaneReflectInfo().getRisingAirplanes().get(0).getArrivingDurationMinutes() + "min, " +
                                                        newMessageArrived.getSender().getAirPlaneReflectInfo().getRisingAirplanes().get(0).getPotential() + "%");

                                                if (newMessageArrived.getSender().getAirPlaneReflectInfo().getAirPlanesReachableCntr() > 1) {
                                                    onlyForSpottingObject.setQra(newMessageArrived.getSender().getQra() + "; " +
                                                            newMessageArrived.getSender().getAirPlaneReflectInfo().getRisingAirplanes().get(1).getArrivingDurationMinutes() + "min, " +
                                                            newMessageArrived.getSender().getAirPlaneReflectInfo().getRisingAirplanes().get(1).getPotential() + "%");
                                                }
                                            } else {

                                                onlyForSpottingObject.setQra(newMessageArrived.getSender().getQra());
                                            }

											this.client.getDxClusterServer().broadcastSingleDXClusterEntryToLoggers(onlyForSpottingObject); //tells the DXCluster server to send a DXC message for this member to the logbook software
										}
									} catch (Exception exception) {
										System.out.println("[MSGBUSMGT, ERROR:] DXCluster messageserver error while processing spot for 0: " + newMessageArrived.getSender().getCallSign() + " // " + exception.getMessage());
//										exception.printStackTrace();
									}
								}

								System.out.println(">>>>>>>>>> Anglewarning <<<<<<<<<< " +  newMessageArrived.getSender().getCallSign() + ", " + newMessageArrived.getSender().getQra() + " -> " + newMessageArrived.getReceiver().getCallSign() + ", " + newMessageArrived.getReceiver().getQra() + " = " +
										new Location(newMessageArrived.getSender().getQra()).getBearing(new Location(newMessageArrived.getReceiver().getQra())) +
										" / sender bearing to me: " + new Location(newMessageArrived.getSender().getQra()).getBearing(new Location(client.getChatPreferences().getStn_loginLocatorMainCat())));

							} else {
								System.out.println("-notinangle- " +  newMessageArrived.getSender().getCallSign() + ", " + newMessageArrived.getSender().getQra() + " -> " + newMessageArrived.getReceiver().getCallSign() + ", " + newMessageArrived.getReceiver().getQra() + " = " +
										new Location(newMessageArrived.getSender().getQra()).getBearing(new Location(newMessageArrived.getReceiver().getQra())) +
										" ; sender bearing to me: " + new Location(newMessageArrived.getSender().getQra()).getBearing(new Location(client.getChatPreferences().getStn_loginLocatorMainCat())));
								newMessageArrived.getSender().setInAngleAndRange(false);
							}

							this.client.getLst_globalChatMessageList().add(0, newMessageArrived);
//						System.out.println("MSGBS bgfx: tx call = " + newMessageArrived.getSender().getCallSign() + " / rx call = " + newMessageArrived.getReceiver().getCallSign());
						}
					} catch (NullPointerException referenceDeletedByUserLeftChatDuringMessageprocessing) {
						System.out.println("MSGBS bgfx, <<<catched error>>>: referenced user left the chat during messageprocessing or message got before user entered chat message: " + referenceDeletedByUserLeftChatDuringMessageprocessing.getStackTrace());
//						referenceDeletedByUserLeftChatDuringMessageprocessing.printStackTrace();
					}

					// sdtout to me message-List

//					newMessageArrived.setReceiver(this.client.getChatMemberTable().get(splittedMessageLine[7])); // set sender
					// to the
					// member of
					// before
					// initialized
					// list
				}

				try {

				System.out.println("[MSGBUSMGT:] processed message: " + newMessageArrived.getChatCategory().getCategoryNumber()
						+ " " + newMessageArrived.getSender().getCallSign() + ", " + newMessageArrived.getMessageSenderName() + " -> "
						+ newMessageArrived.getReceiver().getCallSign() + ": " + newMessageArrived.getMessageText());
				} catch (Exception exceptionOccured) {
					System.out.println("[MSGMgtBus: ERROR CHATCHED ON MAYBE NULL ISSUE]: " + exceptionOccured.getMessage() + "\n" + exceptionOccured.getStackTrace());
				}

				// --- Band/QRG recognition (fills ChatMember.knownActiveBands) ---
				smartFrequencyExtraction(newMessageArrived, this.client.getChatPreferences());

				// TODO: Next: get frequency infos out of name?
			} else

			/**
			 * LOC|1664012560|I4GHG/6|JN63DT| Actualize singleton Userlist, changes locator
			 * of existing user or add him with this locator
			 */
			if (splittedMessageLine[0].contains(USERLOCATORCHANGE)) {
//					System.out.println("MSGBUS: User detected");

				ChatMember temp4 = new ChatMember();
				temp4.setChatCategory(this.client.getChatCategoryMain()); //not really detectable and not really neccessarry to detect

				temp4.setCallSign(splittedMessageLine[2]);
				temp4.setQra(splittedMessageLine[3]);
				temp4.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());

				int index = checkListForChatMemberIndexByCallSign(this.client.getLst_chatMemberList(), temp4);

				if (index != -1) {

					System.out.println("[MSGBUSMGT:] Locator Change of [" + (splittedMessageLine[2] + "], old was: "
							+ this.client.getLst_chatMemberList().get(index).getQra() + " new is: "
							+ splittedMessageLine[3]));

					ChatMember foundThisInChatMemberList = this.client.getLst_chatMemberList().get(index); //make less list accesses

//					this.client.getLst_chatMemberList().get(index).setQra(splittedMessageLine[3]);
//					this.client.getLst_chatMemberList().get(index).setQrb(new Location().getDistanceKmByTwoLocatorStrings(client.getChatPreferences().getLoginLocator(), splittedMessageLine[3]));
//					this.client.getLst_chatMemberList().get(index).setQTFdirection(new Location(client.getChatPreferences().getLoginLocator()).getBearing(new Location(splittedMessageLine[3])));

					foundThisInChatMemberList.setQra(splittedMessageLine[3]);
					foundThisInChatMemberList.setQrb(new Location().getDistanceKmByTwoLocatorStrings(client.getChatPreferences().getStn_loginLocatorMainCat(), splittedMessageLine[3]));
					foundThisInChatMemberList.setQTFdirection(new Location(client.getChatPreferences().getStn_loginLocatorMainCat()).getBearing(new Location(splittedMessageLine[3])));

				} else {
					System.out.println("[MSGBUSMGT:] ERROR! Locator Change of ["
							+ (splittedMessageLine[2] + "] is not possible, user is not in the Table!"));

				}

				this.client.getDbHandler().storeChatMember(temp4); // TODO thats a bit unclean, its less an insert but a
																	// locator update

			} else

			/**
			 * DX-Cluster-Message type 1
			 * DM|0|1664050013|2006|w4cwf|144118.0|PA2CHR|EM85WH<>JO22 hrd
			 * -21db|EM85WH|JO32DB|
			 */
			if (splittedMessageLine[0].contains(DXCLUSTERMESSAGE1)) {
//					System.out.println("MSGBUS: User detected");

				ClusterMessage dxcMsg = new ClusterMessage();

				dxcMsg.setTimeGenerated(splittedMessageLine[2]);

				ChatMember newDXCListSender = new ChatMember();
				newDXCListSender.setCallSign(splittedMessageLine[4]);
				newDXCListSender.setQra(splittedMessageLine[8]);

				ChatMember newDXCListReceiver = new ChatMember();
				newDXCListReceiver.setFrequency(new SimpleStringProperty(splittedMessageLine[5]));
				newDXCListReceiver.setCallSign(splittedMessageLine[6]);
				newDXCListReceiver.setQra(splittedMessageLine[9]);

				dxcMsg.setSender(newDXCListSender);
				dxcMsg.setReceiver(newDXCListReceiver);

				dxcMsg.setMessageInhibited(splittedMessageLine[7]);
				dxcMsg.setQrgSpotted(splittedMessageLine[5]);

				this.client.getLst_clusterMemberList().add(0, dxcMsg);

//				System.out.println("[MSGBUSMGT:] DXCluster Message detected ");

//				if (!this.client.getdXClusterMemberTable().contains(splittedMessageLine[6])) {
//					this.client.getdXClusterMemberTable().put(newDXCListMember.getCallSign(), newDXCListMember);
//				}

			} else

			/**
			 * DX-Cluster-Message type 2 <br/>
			 * DL|1664047594|1926|dg9yih|144000.0|DL6BF|JO32PC
			 * <TR>
			 * JO32QI zerstoert qso|JO32PC|JO32QI| -> Clustermessage
			 * DL|1664048232|1937|pu2pyb|144500.0|PU2NEZ|FM| | |
			 */
			if (splittedMessageLine[0].contains(DXCLUSTERMESSAGE2)) {
//					System.out.println("MSGBUS: User detected");

				ClusterMessage dxcMsg2 = new ClusterMessage();

				dxcMsg2.setTimeGenerated(splittedMessageLine[1]);

				ChatMember newDXCListSender2 = new ChatMember();
				newDXCListSender2.setCallSign(splittedMessageLine[3]);
				newDXCListSender2.setQra(splittedMessageLine[7]);

				ChatMember newDXCListReceiver2 = new ChatMember();
				newDXCListReceiver2.setFrequency(new SimpleStringProperty(splittedMessageLine[4]));
				newDXCListReceiver2.setCallSign(splittedMessageLine[5]);
				newDXCListReceiver2.setQra(splittedMessageLine[8]);

				dxcMsg2.setSender(newDXCListSender2);
				dxcMsg2.setReceiver(newDXCListReceiver2);

				dxcMsg2.setMessageInhibited(splittedMessageLine[6]);
				dxcMsg2.setQrgSpotted(splittedMessageLine[4]);

				this.client.getLst_clusterMemberList().add(0, dxcMsg2);

			} else

			/**
			 * DX-Cluster-Message type 3 <br/>
			 * MA|0|1687204743|e77ar|OK2AF|JN94AS|JN89AR|
			 */
			if (splittedMessageLine[0].contains(DXCLUSTERMESSAGE3)) {
//						System.out.println("MSGBUS: User detected");

				ClusterMessage dxcMsg3 = new ClusterMessage();

				dxcMsg3.setTimeGenerated(splittedMessageLine[2]);

				ChatMember newDXCListSender3 = new ChatMember();
				newDXCListSender3.setCallSign(splittedMessageLine[3]);
				newDXCListSender3.setQra(splittedMessageLine[5]);

				ChatMember newDXCListReceiver3 = new ChatMember();
//					newDXCListReceiver3.setFrequency(splittedMessageLine[4]);
				newDXCListReceiver3.setCallSign(splittedMessageLine[4]);
				newDXCListReceiver3.setQra(splittedMessageLine[5]);

				dxcMsg3.setSender(newDXCListSender3);
				dxcMsg3.setReceiver(newDXCListReceiver3);

				dxcMsg3.setMessageInhibited("");
				dxcMsg3.setQrgSpotted("");

				this.client.getLst_clusterMemberList().add(0, dxcMsg3);

			} else

			/**
			 * Userstatechange:, last digit 0 = in chat, 1 away, 2 here, 3 also away...
			 * US4|2|DM5M|0|
			 */
			if (splittedMessageLine[0].contains(USERSTATECHANGE)) {
//					System.out.println("MSGBUS: User detected");

				ChatMember stateChangeMember = new ChatMember();

				stateChangeMember.setCallSign(splittedMessageLine[2]);
				stateChangeMember.setState(Integer.parseInt(splittedMessageLine[3]));

//				System.out.println("[MSGBUSMGT:] DXCluster Message detected ");

				stateChangeMember.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));

				int index = checkListForChatMemberIndexByCallSign(this.client.getLst_chatMemberList(),
						stateChangeMember);

				if (index != -1 && index != 0) {
					this.client.getLst_chatMemberList().get(index).setState(stateChangeMember.getState());
				}

//				this.client.getChatMemberTable().get(stateChangeMember.getCallSign())
//						.setState(stateChangeMember.getState());

			} else

			/**
			 * Userinfo-update: UM3|2|HA4XN|Zoli 2m SSB/CW|JN96LX|2|
			 */
			if (splittedMessageLine[0].contains(USERINFOUPDATEORUSERISBACK)) {

				ChatMember stateChangeMember = new ChatMember();

				stateChangeMember.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));

				stateChangeMember.setCallSign(splittedMessageLine[2]);
				stateChangeMember.setName(splittedMessageLine[3]);
				stateChangeMember.setQra(splittedMessageLine[4]);
				stateChangeMember.setState(Integer.parseInt(splittedMessageLine[5]));
				stateChangeMember.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());
				stateChangeMember.setQrb(new Location().getDistanceKmByTwoLocatorStrings(client.getChatPreferences().getStn_loginLocatorMainCat(), stateChangeMember.getQra()));
				stateChangeMember.setQTFdirection(new Location(client.getChatPreferences().getStn_loginLocatorMainCat()).getBearing(new Location(stateChangeMember.getQra())));

				this.client.getDbHandler().storeChatMember(stateChangeMember); // TODO: not clean, it should be an
																				// upodate

//					System.out.println("[MSGBUSMGT:] DXCluster Message detected ");

				int index = checkListForChatMemberIndexByCallSign(this.client.getLst_chatMemberList(),
						stateChangeMember);

				//-1 could be the case if mycall is processed
				if (index != -1) {
					this.client.getLst_chatMemberList().get(index).setName(stateChangeMember.getName());
					this.client.getLst_chatMemberList().get(index).setQra(stateChangeMember.getQra());
					this.client.getLst_chatMemberList().get(index).setState(stateChangeMember.getState());
				}

			} else

			/**
			 * Handled like normal messages, but historic...will not trigger any functions
			 *
			 * Chat history line like:
			 * CR|6|1771165971|DF0GEB|test|0|ok|0|
			 * ^^hist
			 * 	  ^chan
			 * 	    ^^^^^^^^^^time ...
			 */
			if (splittedMessageLine[0].contains(SERVERMESSAGEHISTORIC)) {


				ChatMessage newMessageArrived = new ChatMessage();
				ChatCategory chategoryForMessageAndMessageSender;

				newMessageArrived.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));

				chategoryForMessageAndMessageSender = newMessageArrived.getChatCategory();
				newMessageArrived.setMessageGeneratedTime(splittedMessageLine[2]);

				if (splittedMessageLine[3].equals("SERVER")) {
					ChatMember dummy = new ChatMember();
					dummy.setCallSign("SERVER");
					dummy.setName("Sysop");
					newMessageArrived.setSender(dummy);
					newMessageArrived.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));
					dummy.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));
//					System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> servers cat " + newMessageArrived.getChatCategory());

				} else {

					ChatMember sender = new ChatMember();
					sender.setCallSign(splittedMessageLine[3]);
					sender.setChatCategory(chategoryForMessageAndMessageSender);

					int index = checkListForChatMemberIndexByCallSign(this.client.getLst_chatMemberList(), sender);

					if (index != -1) {
						//user not found in the chatmember list
						try {
//							newMessageArrived.setSender(this.client.getLst_chatMemberList().get(index)); // set sender to member of
//							this.client.getLst_chatMemberList().get(index).setActivityTimeLastInEpoch(new Utils4KST().time_generateCurrentEpochTime());

							ChatMember senderObj = this.client.getLst_chatMemberList().get(index);
							newMessageArrived.setSender(senderObj);
							senderObj.setActivityTimeLastInEpoch(new Utils4KST().time_generateCurrentEpochTime());

							// Remember last inbound category per callsignRaw (required for correct send-routing later)
							this.client.rememberLastInboundCategory(senderObj.getCallSignRaw(), senderObj.getChatCategory());

							// Metrics for scoring: momentum, response-time, no-reply, positive signals
							this.client.getStationMetricsService().onInboundMessage(
									senderObj.getCallSignRaw(),
									System.currentTimeMillis(),
									newMessageArrived.getMessageText(),
									this.client.getChatPreferences(),
									this.client.getChatPreferences().getStn_loginCallSign()
							);

							// Activity/category changes influence priority => request recompute
							this.client.getScoreService().requestRecompute("rx-chat-message");

						} catch (Exception exc) {
							ChatMember aSenderDummy = new ChatMember();
							aSenderDummy.setCallSign(splittedMessageLine[3] + "[n/a]");
							aSenderDummy.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());
							newMessageArrived.setSender(aSenderDummy);
							System.out.println("MsgBusmgtT: Catched Error! " + exc.getMessage() + " // " + splittedMessageLine[3] + " is not in the list! Faking sender!");
							exc.printStackTrace();
						}
						// b4 init list
					} else {
						//user not found in chatmember list, mark it, sender can not be set
						if (!sender.getCallSign().equals(this.client.getChatPreferences().getStn_loginCallSign().toUpperCase())) {
							sender.setCallSign("[n/a]" + sender.getCallSign());
							// if someone sent a message without being in the userlist (cause
							// on4kst missed implementing....), callsign will be marked
						} else {
							//that means, message was by own station, broadcasted to all other
							ChatMember dummy = new ChatMember();
							dummy.setCallSign("ALL");
							newMessageArrived.setReceiver(dummy);

							AirPlaneReflectionInfo preventNullpointerExc = new AirPlaneReflectionInfo();
							preventNullpointerExc.setAirPlanesReachableCntr(0);
							sender.setAirPlaneReflectInfo(preventNullpointerExc);
							newMessageArrived.setSender(sender); //my own call is the sender
						}
					}

//					newMessageArrived.setSender(this.client.getChatMemberTable().get(splittedMessageLine[3]));
				}

				newMessageArrived.setMessageSenderName(splittedMessageLine[4]);
				newMessageArrived.setMessageText(splittedMessageLine[6]);

				if (splittedMessageLine[7].equals("0")) {
					// message is not directed to anyone, move it to the cq messages!
					ChatMember dummy = new ChatMember();
					dummy.setCallSign("ALL");
					newMessageArrived.setReceiver(dummy);

					this.client.getLst_globalChatMessageList().add(0, newMessageArrived); // sdtout to all message-List

				} else {
					//message is directed to another chatmember, process as such!

					ChatMember receiver = new ChatMember();

					receiver.setChatCategory(chategoryForMessageAndMessageSender); //got out of message itself

					receiver.setCallSign(splittedMessageLine[7]);

					int index = checkListForChatMemberIndexByCallSign(this.client.getLst_chatMemberList(), receiver);

					if (index != -1) {
						newMessageArrived.setReceiver(this.client.getLst_chatMemberList().get(index));// -1: Member left Chat
						// before...
					} else { //found in active member list

						if (receiver.getCallSign().equals(client.getChatPreferences().getStn_loginCallSign())) {
							/**
							 * If mycallsign sent a message to the server, server will publish that message and
							 * send it to all chatmember including me.
							 * As mycall is not in the userlist,  the message would not been displayed if I handle
							 * it in the next case (marking left user, just for information). But I want an echo.
							 */

							receiver.setCallSign(client.getChatPreferences().getStn_loginCallSign());
							newMessageArrived.setReceiver(receiver);
						} else {
							//this are user which left chat but had been adressed by this message
							receiver.setCallSign(receiver.getCallSign() + "(left)");
							newMessageArrived.setReceiver(receiver);
						}
					}

//					System.out.println("message directed to: " + newMessageArrived.getReceiver().getCallSign() + ". EQ?: " + this.client.getownChatMemberObject().getCallSign() + " sent by: " + newMessageArrived.getSender().getCallSign().toUpperCase() + " -> EQ?: "+ this.client.getChatPreferences().getLoginCallSign().toUpperCase());

					try {
						/**
						 * message is directed to me, will be put in the "to me" messagelist
						 */
						if (newMessageArrived.getReceiver().getCallSign()
								.equals(this.client.getChatPreferences().getStn_loginCallSign())) {

							this.client.getLst_globalChatMessageList().add(0, newMessageArrived);

							System.out.println("Historic message directed to me: " + newMessageArrived.getReceiver().getCallSign() + ".");

						} else if (newMessageArrived.getSender().getCallSign().toUpperCase()
								.equals(this.client.getChatPreferences().getStn_loginCallSign().toUpperCase())) {
							/**
							 * message sent by me!
							 * message from me will appear in the PM window, too, with (>CALLSIGN) before
							 */
							String originalMessage = newMessageArrived.getMessageText();
							newMessageArrived
									.setMessageText("(>" + newMessageArrived.getReceiver().getCallSign() + ")" + originalMessage);
							this.client.getLst_globalChatMessageList().add(0,newMessageArrived);

							// if you sent the message to another station, it will be sorted in to
							// the "to me message list" with modified messagetext, added rxers callsign

						} else {
							//message sent to other user
							if (DirectionUtils.isInAngleAndRange(client.getChatPreferences().getStn_loginLocatorMainCat(),
									newMessageArrived.getSender().getQra(),
									newMessageArrived.getReceiver().getQra(),
									client.getChatPreferences().getStn_maxQRBDefault(),
									client.getChatPreferences().getStn_antennaBeamWidthDeg())) {

								newMessageArrived.getSender().setInAngleAndRange(true);

							} else {

								newMessageArrived.getSender().setInAngleAndRange(false);
							}

							this.client.getLst_globalChatMessageList().add(0, newMessageArrived);
//						System.out.println("MSGBS bgfx: tx call = " + newMessageArrived.getSender().getCallSign() + " / rx call = " + newMessageArrived.getReceiver().getCallSign());
						}
					} catch (NullPointerException referenceDeletedByUserLeftChatDuringMessageprocessing) {
						System.out.println("MSGBS bgfx, <<<catched error>>>: referenced user left the chat during messageprocessing or message got before user entered chat message: " + referenceDeletedByUserLeftChatDuringMessageprocessing.getStackTrace());
//						referenceDeletedByUserLeftChatDuringMessageprocessing.printStackTrace();
					}

					// sdtout to me message-List

				}

				try {

					System.out.println("[MSGBUSMGT:] processed message: " + newMessageArrived.getChatCategory().getCategoryNumber()
							+ " " + newMessageArrived.getSender().getCallSign() + ", " + newMessageArrived.getMessageSenderName() + " -> "
							+ newMessageArrived.getReceiver().getCallSign() + ": " + newMessageArrived.getMessageText());
				} catch (Exception exceptionOccured) {
					System.out.println("[MSGMgtBus: ERROR CHATCHED ON MAYBE NULL ISSUE]: " + exceptionOccured.getMessage() + "\n" + exceptionOccured.getStackTrace());
				}

				// --- Band/QRG recognition (fills ChatMember.knownActiveBands) ---
				smartFrequencyExtraction(newMessageArrived, this.client.getChatPreferences());




			} else

			/**
			 * Userinfo-update: UE|2|22562|
			 */
			if (splittedMessageLine[0].contains(SRVR_USERLISTEND)) {

				// No worthy information, count of users
			} else

			if (splittedMessageLine[0].contains(SRVR_DXCEND)) {

				// No worthy information, count of users
			} else

			if (splittedMessageLine[0].contains(SRVR_COMMUNICATIONK)) {
				// No worthy information, end of srvrmsgs
			} else 
				
			//-> LOGSTAT|114|Wrong password!|
			if (splittedMessageLine[0].contains(SRVR_LOGSTAT) && splittedMessageLine.length <= 5) {
				System.out.println("Passwort falsch!");
				
				if (splittedMessageLine[2].contains("password")) {
					splittedMessageLine[2] += " pse disc- and reconnect";
				}
				
				ChatMember server = new ChatMember();
				server.setCallSign("SERVER");
				server.setName("SERVER");
				
				ChatMessage pwErrorMsg = new ChatMessage();
				
				pwErrorMsg.setMessageGeneratedTime(client.getCurrentEpochTime()+"");
				pwErrorMsg.setSender(server);
				pwErrorMsg.setMessageText(splittedMessageLine[2]);

				ChatMember receiverDummy = new ChatMember();
				receiverDummy.setCallSign(client.getChatPreferences().getStn_loginCallSign());
				receiverDummy.setQrb(0.);
				receiverDummy.setQTFdirection(0.);
				pwErrorMsg.setReceiver(receiverDummy);


				
				for (int i = 0; i < 10; i++) {
					client.getLst_globalChatMessageList().add(pwErrorMsg);
//					client.getLst_toMeMessageList().add(pwErrorMsg);
//					client.getLst_toAllMessageList().add(pwErrorMsg);
				}

//				Kst4ContestApplication.alertWindowEvent("Password was wrong. Pse check!");

				client.disconnect(ApplicationConstants.DISCSTRING_DISCONNECTONLY);
				
//				this.client.disconnect();
			}

			else {

//				bufwrtrDBGMSGOut.write(new Utils4KST().time_generateCurrentMMDDhhmmTimeString()
//						+ "[MSGBUSMGT:] Critical, detected unhandled Chatmessage -> "
//						+ messageToProcess.getMessageText() + "\n");
//				bufwrtrDBGMSGOut.flush();

				System.out.print(new Utils4KST().time_generateCurrentMMDDhhmmTimeString()
						+ " [MSGBUSMGT:] Critical, detected unhandled Chatmessage -> "
						+ messageToProcess.getMessageText() + "\n");

			}

			// ******************************************************************QUICKNDIRTY........
//			ChatMember thisMemberActualizesUserListForRefreshingIntheGuy = new ChatMember();
//			thisMemberActualizesUserListForRefreshingIntheGuy.setCallSign("REFR");
//			thisMemberActualizesUserListForRefreshingIntheGuy.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());
//			this.client.getLst_chatMemberList().add(thisMemberActualizesUserListForRefreshingIntheGuy);
//			this.client.getLst_chatMemberList().isEmpty();
//			System.out.println("MSGBUS BGFX Listactualizer");
//			this.client.getLst_chatMemberList().remove(thisMemberActualizesUserListForRefreshingIntheGuy);
			// ******************************************************************QUICKNDIRTY........

//			checkIfMessageInhibitsFrequency(messageToProcess);
		}
	}

	/**
	 * Method gets a String with a messagecategory-number and returns out of which of the existing categories
	 * (chat channels) this message/user had written from
	 *
	 * @param categoryNumber
	 * @return used Chatcategory (instance of singletons)
	 */
	private ChatCategory util_getChatCategoryByCategoryNrString(String categoryNumber) {

//		System.out.println("MSGBSMGT Debug: >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> try to find out category for a member; category is " + categoryNumber + " // 1st is " + this.client.getChatCategoryMain().getCategoryNumber() + " // 2nd is " + this.client.getChatCategorySecondChat().getCategoryNumber());

		if (categoryNumber.equals(this.client.getChatCategoryMain().getCategoryNumber() + "")) {
			return this.client.getChatCategoryMain();
		} else if (categoryNumber.equals(this.client.getChatCategorySecondChat().getCategoryNumber() + "")) {
			return this.client.getChatCategorySecondChat();
		} else {
			System.out.println("Msgbusmgt: ERROR!!! -> category for this message does not exist!");
			return this.client.getChatCategoryMain(); //Chatcategory default decision
		}

	}

	@Override
	public void interrupt() {
		super.interrupt();
		
	}


	/**
	 * check if message had been auto generated
	 * @param msg
	 * @return
	 */
	private boolean isAutoMessage(ChatMessage msg) {
		return msg != null
				&& msg.getMessageText() != null
				&& msg.getMessageText().contains(AUTOANSWER_PREFIX);
	}

	private String autoAnswerCooldownKey(ChatMessage incoming) {

		String remoteCall = "UNKNOWN";
		if (incoming != null && incoming.getSender() != null && incoming.getSender().getCallSign() != null) {
			remoteCall = incoming.getSender().getCallSign().toUpperCase();
		}

		int cat = 0; // fallback
		if (incoming != null && incoming.getSender() != null && incoming.getSender().getChatCategory() != null) {
			cat = incoming.getSender().getChatCategory().getCategoryNumber();
		}

		// pro Gegenstation + pro Chat-Kategorie (falls derselbe Call in Cat2/Cat3 PMs macht)
		return remoteCall + "|" + cat;
	}

	private boolean isAutoAnswerAllowedNow(ChatMessage incoming) {

		String key = autoAnswerCooldownKey(incoming);
		Long last = lastLocalAutoAnswerPerRemoteMs.get(key);

		long now = System.currentTimeMillis();
		return last == null || (now - last) >= AUTOANSWER_COOLDOWN_MS;
	}

	private void markLocalAutoAnswerSent(ChatMessage incoming) {
		lastLocalAutoAnswerPerRemoteMs.put(autoAnswerCooldownKey(incoming), System.currentTimeMillis());
	}

	
	public void run() {

//		fileLogRAW = new File(new Utils4KST().time_generateCurrentMMddString() + "_praktiKST_raw.txt");

//		FileWriter fileWriterRAWChatMSGOut = null;
//		BufferedWriter bufwrtrRawMSGOut;

//		try {
//			fileWriterRAWChatMSGOut = new FileWriter(fileLogRAW, true);
//
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}

//		bufwrtrRawMSGOut = new BufferedWriter(fileWriterRAWChatMSGOut);

//		File fileLogClientOut = new File(new Utils4KST().time_generateCurrentMMddString() + "_praktiKST_out.txt");

//		FileWriter fileWriterOutChatMSGOut = null;
//
//		try {
//			fileWriterOutChatMSGOut = new FileWriter(fileLogClientOut, true);
//		} catch (IOException e2) {
//			// TODO Auto-generated catch block
//			e2.printStackTrace();
//		}
//
//		try {
//			fileWriterRAWChatMSGOut = new FileWriter(fileLogClientOut, true);
//
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//			try {
//				fileWriterRAWChatMSGOut.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}

//		bufwrtrDBGMSGOut = new BufferedWriter(fileWriterOutChatMSGOut);

//		new Timer().schedule(new UserActualizationTask(client), 4000, 60000);// TODO: Temporary userlistoutput with
//																				// known qrgs
//
//		userActualizationTask = new UserActualizationTask(client); // kst4contest.test 4 23001
//		userActualizationTask.run();// kst4contest.test 4 23001

		ChatMessage messageTextRaw = new ChatMessage(); // moved out of the while
		String messageLine;

		while (true) {

				try {
					messageTextRaw = client.getMessageRXBus().take();
					
					if (messageTextRaw.getMessageText().equals(ApplicationConstants.DISCONNECT_RDR_POISONPILL) && messageTextRaw.getMessageSenderName().equals(ApplicationConstants.DISCONNECT_RDR_POISONPILL)) {
						client.getMessageRXBus().clear();
						break;
					}
					else {
						messageLine = messageTextRaw.getMessageText();

						/***********************************************
						 * CASE RX
						 ***********************************************/

//						if (client.getMessageRXBus().peek() != null) {

//							try {
//								messageTextRaw = client.getMessageRXBus().take();
		//
////								System.out.println("MSBGBUS: rxed: " + messageTextRaw);
//							} catch (InterruptedException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}

							if (messageTextRaw.getMessageText() == null) {
								System.out.println("[MSGBUSMGT:] ERROR! got NULL message! BYE!");
//							this.interrupt();
//							break;
							}

							messageLine = messageTextRaw.getMessageText();

//							try {
//								bufwrtrRawMSGOut.write(messageLine + "\n");
//								bufwrtrRawMSGOut.flush();
		//
//							} catch (IOException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}

							System.out.println(messageTextRaw.getMessageText() + " <- RXed"); // Stdout at
							// Console#######################################################TODO:Wichtig

							try {
								processRXMessage23001(messageTextRaw);
							} catch (IOException e) {
								System.out.println("MsgBusMgt: process23001 went wrong / IO Error");
								e.printStackTrace();
							} catch (SQLException e) {
								System.out.println("MsgBusMgt: process23001 went wrong / SQL Error");
								e.printStackTrace();
							}
					}
					
				} catch (InterruptedException e1) {
					this.interrupt();

					e1.printStackTrace();
					break;// TODO Change at may24, avoid uncloadability. Check if this could lead to further errors on instable link!
	//				client.getMessageRXBus().clear();
				}
			{
//				System.out.println("MessagebusmgtThread: Readthread is interrupted! Queue will be resetted");
//				this.interrupt();
//				client.getMessageRXBus().clear();
			} 

//			if (client.getMessageRXBus().peek() == null) {
//
//				Timer doNothingTimer = new Timer();
//				doNothingTimer.schedule(new TimerTask() {
//
//					@Override
//					public void run() {
//
//						//do nothing
//
//					}
//				}, 100);// TODO: Temporary
//			}
//
//
//			if (client.getMessageRXBus().peek() == null && client.getMessageTXBus().peek() == null) {
//
//				if (this.client.isDisconnectionPerformedByUser()) {
//					break;//TODO: what if it´s not the finally closage but a band channel change?
//				}
//				// do nothing
////				try {
////					this.sleep(20);
////				} catch (InterruptedException e) {
////					// TODO Auto-generated catch block
////					e.printStackTrace();
////				} catch (Exception e2) {
////					// TODO Auto-generated catch block
////					e2.printStackTrace();
////				}
//			}
//			else
			{

//				messageLine = messageTextRaw.getMessageText();
//
//				/***********************************************
//				 * CASE RX
//				 ***********************************************/
//
////				if (client.getMessageRXBus().peek() != null) {
//
////					try {
////						messageTextRaw = client.getMessageRXBus().take();
////
//////						System.out.println("MSBGBUS: rxed: " + messageTextRaw);
////					} catch (InterruptedException e) {
////						// TODO Auto-generated catch block
////						e.printStackTrace();
////					}
//
//					if (messageTextRaw.getMessageText() == null) {
//						System.out.println("[MSGBUSMGT:] ERROR! got NULL message! BYE!");
////					this.interrupt();
////					break;
//					}
//
//					messageLine = messageTextRaw.getMessageText();
//
////					try {
////						bufwrtrRawMSGOut.write(messageLine + "\n");
////						bufwrtrRawMSGOut.flush();
////
////					} catch (IOException e) {
////						// TODO Auto-generated catch block
////						e.printStackTrace();
////					}
//
//					System.out.println(messageTextRaw.getMessageText() + " <- RXed"); // Stdout at
//					// Console#######################################################TODO:Wichtig
//
//					try {
//						processRXMessage23001(messageTextRaw);
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} catch (SQLException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}

//				} //end peek != null

				/**************************************************************
				 * End of case RX
				 **************************************************************/

				/**************************************************************
				 * Start of case TX
				 **************************************************************/

//				if (client.getMessageTXBus().peek() != null) {
//					/***********************************************
//					 * CASE TX
//					 ***********************************************/
//
//					if (this.isServerready()) {
//						// then send the line
//
//						try {
//							messageTextRaw = client.getMessageTXBus().take();
////						this.setServerready(false); // after tx always wait for an answer prompt //23000
//							this.setServerready(true);
//
//						} catch (InterruptedException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//
//						messageLine = messageTextRaw.getMessageText();
//
//						if (messageTextRaw.isMessageDirectedToServer()) {
//							/**
//							 * We have to check if we only commands the server (keepalive) or want do talk
//							 * to the community
//							 */
//
//							try {
//								client.getWriteThread().tx(messageTextRaw);
//								System.out.println("BUS: tx: " + messageTextRaw.getMessageText());
//
//							} catch (InterruptedException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//
//							//////////////////////// bgfx ab here//////////////////////////////
////						try {
////							bufwrtrRawMSGOut.write(messageLine + "\r");
//////							bw.write(messageLine + "\n");//kst4contest.test 4 23001
////							bufwrtrRawMSGOut.flush();
////
////						} catch (IOException e) {
////							// TODO Auto-generated catch block
////							e.printStackTrace();
////						}
//							///////////////////////////////////////////////////////////////////
//						} else {
//
//							ChatMessage ownMSG = new ChatMessage();
//
////						ownMSG.setMessageText(
////								"MSG|" + this.client.getCategory().getCategoryNumber() + "|0|" + messageLine + "|0|");
//
//							ownMSG.setMessageText(
//									"MSG|" + this.client.getChatPreferences().getLoginChatCategory().getCategoryNumber()
//											+ "|0|" + messageLine + "|0|");
//
//							try {
//								client.getWriteThread().tx(ownMSG);
//								System.out.println("BUS: tx: " + ownMSG.getMessageText());
//
//							} catch (InterruptedException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//						}
//
//						if (messageTextRaw.equals("/QUIT")) {
//							try {
//								this.client.getReadThread().terminateConnection();
//								this.client.getReadThread().interrupt();
//								this.client.getWriteThread().terminateConnection();
//								this.client.getWriteThread().interrupt();
//								this.interrupt();
//
//							} catch (IOException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//						}
//
//					} else {
////					System.out.println("msgbus no elements yet");
//					}
//				} //end tx.peek != null
			}

			
		} // while true end
		System.out.println("Msgbusmgt: interrupt");
		this.interrupt();
	}
}
