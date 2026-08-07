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

	/*
	 * Frequency formats handled by the smart parser:
	 *
	 * Group 1: full frequencies, for example 144.210 or 10368.100
	 * Group 2: relative frequencies with a separator, for example .210 or ,210
	 * Group 3: bare three-digit values, for example 210
	 *
	 * Group 3 is deliberately subjected to an additional context check. Without
	 * that check, ordinary chat values such as 599 or a bare band name such as 144
	 * would be converted into plausible but incorrect frequencies.
	 */
	private static final Pattern SMART_FREQUENCY_PATTERN = Pattern.compile(
			"(?<![\\d])(\\d{3,5}[.,]\\d{1,3}(?:[.,]\\d{1,3})?)(?![\\d])"
					+ "|(?<![\\d])([.,]\\d{3}(?:[.,]\\d{1,3})?)(?![\\d])"
					+ "|(?<=\\s|^)(\\d{3})(?=\\s|$)"
	);

	/*
	 * A bare three-digit value is accepted only when the nearby text makes its
	 * meaning sufficiently clear. Examples:
	 *
	 * qrg 210
	 * QRG: 210
	 * freq is 210
	 * on 210
	 * pse 210
	 * at 210
	 * 210 MHz
	 * 210 qrg
	 */
	private static final Pattern BARE_FREQUENCY_PREFIX_CONTEXT_PATTERN =
			Pattern.compile(
					"(?i)\\b(?:qrg|freq(?:uency)?|on|pse|at)\\b"
							+ "\\s*(?:is\\s*)?[:=@-]?\\s*$"
			);

	private static final Pattern BARE_FREQUENCY_SUFFIX_CONTEXT_PATTERN =
			Pattern.compile(
					"(?i)^\\s*(?:mhz|qrg|freq(?:uency)?)\\b"
			);

	private static final int BARE_FREQUENCY_CONTEXT_CHARACTERS = 32;


	// ==== Auto-answer flood/ping-pong protection ====
	private static final String AUTOANSWER_PREFIX = ApplicationConstants.AUTOANSWER_PREFIX;
	private static final long AUTOANSWER_COOLDOWN_MS = 120_000L; // two minutes

	// Cooldown per remote station and chat category; updated only after this client sends.
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
	 * Detects complete and relative frequencies in a chat message and stores the
	 * result on the sender.
	 *
	 * <p>Complete frequencies determine their band directly. Relative frequencies
	 * first use the sender's most recent band context if that context is not older
	 * than 30 minutes. Only when no suitable sender context exists does the parser
	 * use the globally configured fallback band.</p>
	 *
	 * <p>A relative value beginning with a dot or comma is sufficiently explicit
	 * on its own. A bare three-digit value is ambiguous and is therefore accepted
	 * only with nearby frequency-related text.</p>
	 *
	 * @param message message whose text is inspected
	 * @param prefs preferences containing the global fallback band
	 */
	private void smartFrequencyExtraction(ChatMessage message, ChatPreferences prefs) {
		if (message == null || message.getMessageText() == null) {
			return;
		}

		ChatMember sender = message.getSender();
		if (sender == null) {
			return;
		}

		String messageText = message.getMessageText();
		Matcher matcher = SMART_FREQUENCY_PATTERN.matcher(messageText);

		while (matcher.find()) {
			boolean dottedShortForm = matcher.group(2) != null;
			boolean bareShortForm = matcher.group(3) != null;
			boolean shortForm = dottedShortForm || bareShortForm;

			if (bareShortForm
					&& !hasExplicitBareFrequencyContext(
					messageText,
					matcher.start(),
					matcher.end()
			)) {
				continue;
			}

			String foundRaw = matcher.group().trim().replace(',', '.');

			double finalDetectedFrequency = 0.0;
			Band finalDetectedBand = null;

			if (shortForm) {
				if (foundRaw.startsWith(".")) {
					foundRaw = foundRaw.substring(1);
				}

				/*
				 * Priority 1: use this sender's most recently observed band if its
				 * information is not older than 30 minutes and the reconstructed
				 * frequency lies inside that band.
				 */
				long bestTimestamp = 0L;

				if (sender.getKnownActiveBands() != null) {
					for (java.util.Map.Entry<Band, ChatMember.ActiveFrequencyInfo> entry
							: sender.getKnownActiveBands().entrySet()) {

						Band candidateBand = entry.getKey();
						ChatMember.ActiveFrequencyInfo info = entry.getValue();

						if (candidateBand == null || info == null) {
							continue;
						}

						if (System.currentTimeMillis() - info.timestampEpoch > 1_800_000L) {
							continue;
						}

						try {
							String reconstructed =
									candidateBand.getPrefix() + "." + foundRaw;
							double candidateFrequency = Double.parseDouble(
									normalizeFrequencyString(reconstructed)
							);

							if (candidateBand.isPlausible(candidateFrequency)
									&& info.timestampEpoch > bestTimestamp) {
								finalDetectedFrequency = candidateFrequency;
								finalDetectedBand = candidateBand;
								bestTimestamp = info.timestampEpoch;
							}
						} catch (NumberFormatException ignored) {
							// Try the next known band.
						}
					}
				}

				/*
				 * Priority 2: use the configured fallback band. Invalid values from
				 * an older hand-edited configuration fall back safely to 144 MHz.
				 * The current UI itself offers only values from Band.values().
				 */
				if (finalDetectedBand == null) {
					String configuredPrefix = null;

					if (prefs != null
							&& prefs.getNotify_optionalFrequencyPrefix() != null) {
						configuredPrefix =
								prefs.getNotify_optionalFrequencyPrefix().get();
					}

					Band fallbackBand = Band.fromPrefix(configuredPrefix);
					if (fallbackBand == null) {
						fallbackBand = Band.B_144;
					}

					try {
						String reconstructed =
								fallbackBand.getPrefix() + "." + foundRaw;
						double candidateFrequency = Double.parseDouble(
								normalizeFrequencyString(reconstructed)
						);

						if (fallbackBand.isPlausible(candidateFrequency)) {
							finalDetectedFrequency = candidateFrequency;
							finalDetectedBand = fallbackBand;
						}
					} catch (NumberFormatException ignored) {
						// The matched value cannot be converted into a frequency.
					}
				}
			} else {
				try {
					finalDetectedFrequency = Double.parseDouble(
							normalizeFrequencyString(foundRaw)
					);
					finalDetectedBand = Band.fromFrequency(finalDetectedFrequency);
				} catch (NumberFormatException ignored) {
					// Continue with the next possible match in the message.
				}
			}

			if (finalDetectedBand == null || finalDetectedFrequency <= 0.0) {
				continue;
			}

			/*
			 * Store the result in the thread-safe active-member model. The existing
			 * compatibility property used by the TableView and DX-Cluster code is
			 * updated by applyDetectedFrequencyToActiveMembers(...), too.
			 */
			client.applyDetectedFrequencyToActiveMembers(
					sender,
					finalDetectedBand,
					finalDetectedFrequency
			);

			System.out.println(
					"[SmartParser] Detected for "
							+ sender.getCallSign()
							+ ": "
							+ finalDetectedFrequency
							+ " MHz ("
							+ finalDetectedBand
							+ ") "
							+ (shortForm
							? "[derived from " + foundRaw + "]"
							: "[full match]")
			);
		}
	}


	/**
	 * Checks whether a bare three-digit value is surrounded by text that identifies
	 * it as a frequency.
	 *
	 * <p>The check intentionally uses only a small area around the value. A remote
	 * occurrence of the word "QRG" elsewhere in a long message must not turn every
	 * three-digit number in that message into a frequency.</p>
	 *
	 * @param messageText complete chat message
	 * @param matchStart start index of the three-digit match
	 * @param matchEnd end index of the three-digit match
	 * @return {@code true} if the value has explicit frequency context
	 */
	static boolean hasExplicitBareFrequencyContext(
			String messageText,
			int matchStart,
			int matchEnd
	) {
		if (messageText == null
				|| matchStart < 0
				|| matchEnd < matchStart
				|| matchEnd > messageText.length()) {
			return false;
		}

		int prefixStart = Math.max(
				0,
				matchStart - BARE_FREQUENCY_CONTEXT_CHARACTERS
		);
		int suffixEnd = Math.min(
				messageText.length(),
				matchEnd + BARE_FREQUENCY_CONTEXT_CHARACTERS
		);

		String prefixContext = messageText.substring(prefixStart, matchStart);
		String suffixContext = messageText.substring(matchEnd, suffixEnd);

		return BARE_FREQUENCY_PREFIX_CONTEXT_PATTERN
				.matcher(prefixContext)
				.find()
				|| BARE_FREQUENCY_SUFFIX_CONTEXT_PATTERN
				.matcher(suffixContext)
				.find();
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
	 * Creates a minimal receiver object for ON4KST broadcast messages.
	 * Keeping this as an object, not null, protects all downstream chat filters
	 * and debug logging from null receiver paths.
	 */
	private ChatMember createAllReceiver() {
		ChatMember dummy = new ChatMember();
		dummy.setCallSign("ALL");
		dummy.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());
		return dummy;
	}

	/**
	 * Resolves a message sender from the thread-safe active-member model.
	 *
	 * <p>The local login is handled before the active-user lookup because the own
	 * ChatMember is intentionally not stored in the visible user list. This also
	 * prevents another active login with the same base callsign from replacing the
	 * identity of our own message echo.</p>
	 */
	private ChatMember resolveInboundSender(
			String senderCallSign,
			ChatCategory category,
			ChatMessage message
	) {

		String myCall =
				this.client.getChatPreferences().getStn_loginCallSign();

		if (senderCallSign != null
				&& myCall != null
				&& senderCallSign.equalsIgnoreCase(myCall)) {

			ChatMember ownSender = new ChatMember();
			ownSender.setCallSign(senderCallSign);
			ownSender.setChatCategory(category);
			ownSender.setAirPlaneReflectInfo(
					new AirPlaneReflectionInfo()
			);

			return ownSender;
		}

		ChatMember lookup = new ChatMember();
		lookup.setCallSign(senderCallSign);
		lookup.setChatCategory(category);

		ChatMember senderObj =
				this.client.findActiveChatMember(lookup);

		if (senderObj != null) {
			senderObj.setActivityTimeLastInEpoch(
					new Utils4KST().time_generateCurrentEpochTime()
			);

			this.client.rememberLastInboundCategory(
					senderObj.getCallSignRaw(),
					senderObj.getChatCategory()
			);

			this.client.getStationMetricsService().onInboundMessage(
					senderObj.getCallSignRaw(),
					System.currentTimeMillis(),
					message == null ? null : message.getMessageText(),
					this.client.getChatPreferences(),
					myCall
			);

			this.client.getScoreService().requestRecompute(
					"rx-chat-message"
			);

			return senderObj;
		}

		ChatMember fallbackSender = new ChatMember();
		fallbackSender.setCallSign("[n/a]" + senderCallSign);
		fallbackSender.setChatCategory(category);
		fallbackSender.setAirPlaneReflectInfo(
				new AirPlaneReflectionInfo()
		);

		return fallbackSender;
	}

	/**
	 * Resolves a message receiver from the thread-safe active-member model.
	 *
	 * <p>The local login is handled before the active-user lookup. The receiver
	 * from the ON4KST packet therefore remains authoritative even if another
	 * station with the same base callsign is logged into the same category.</p>
	 */
	private ChatMember resolveInboundReceiver(
			String receiverCallSign,
			ChatCategory category
	) {

		if (receiverCallSign == null
				|| receiverCallSign.equals("0")) {
			return createAllReceiver();
		}

		String myCall =
				this.client.getChatPreferences().getStn_loginCallSign();

		if (myCall != null
				&& receiverCallSign.equalsIgnoreCase(myCall)) {

			ChatMember ownReceiver = new ChatMember();
			ownReceiver.setCallSign(receiverCallSign);
			ownReceiver.setChatCategory(category);
			ownReceiver.setAirPlaneReflectInfo(
					new AirPlaneReflectionInfo()
			);

			return ownReceiver;
		}

		ChatMember lookup = new ChatMember();
		lookup.setCallSign(receiverCallSign);
		lookup.setChatCategory(category);

		ChatMember receiverObj =
				this.client.findActiveChatMember(lookup);

		if (receiverObj != null) {
			return receiverObj;
		}

		ChatMember fallbackReceiver = new ChatMember();
		fallbackReceiver.setCallSign(receiverCallSign + "(left)");
		fallbackReceiver.setChatCategory(category);
		fallbackReceiver.setAirPlaneReflectInfo(
				new AirPlaneReflectionInfo()
		);

		return fallbackReceiver;
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
					this.client.addOrUpdateActiveChatMember(newMember); // the own call will not be in the list
//					this.client.getReachabilityService().ensureAutoTropoMarginCalculated(newMember);
					// Reachability is calculated on demand only: map click, selected station, or manual request.
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

						this.client.addOrUpdateActiveChatMember(newMember);
//					this.client.getReachabilityService().ensureAutoTropoMarginCalculated(newMember);

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
								+ this.client.getActiveChatMemberCount() + "] :" + newMember.getCallSign());

						if (!this.client.removeActiveChatMember(newMember)) {
							System.out.println("[MSGBUSMGT, Info:] User sent left chat but was not active: "
									+ newMember.getCallSign() + " / " + newMember.getChatCategory());
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
							newMessageArrived.setMessageSenderName(splittedMessageLine[4]);
							newMessageArrived.setMessageText(splittedMessageLine[6]);

							if (splittedMessageLine[3].equals("SERVER")) {
								ChatMember dummy = new ChatMember();
								dummy.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());
								dummy.setCallSign("SERVER");
								dummy.setName("Sysop");
								newMessageArrived.setSender(dummy);
								newMessageArrived.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));
								dummy.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));
//					System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> servers cat " + newMessageArrived.getChatCategory());

							} else {

								newMessageArrived.setSender(resolveInboundSender(
										splittedMessageLine[3],
										chategoryForMessageAndMessageSender,
										newMessageArrived));
							}

							/*
							 * Detect and store the sender's QRG before any directional-opportunity
							 * or DX-Cluster processing. A frequency first mentioned in this directed
							 * message is then immediately available for the resulting spot.
							 *
							 * The parser needs only the message text, sender and preferences. The
							 * receiver does not have to be resolved yet.
							 */
							smartFrequencyExtraction(
									newMessageArrived,
									this.client.getChatPreferences());

							// TODO: Next: get frequency infos out of name?

							if (splittedMessageLine[7].equals("0")) {
								// message is not directed to anyone, move it to the cq messages.
								newMessageArrived.setReceiver(createAllReceiver());

								this.client.publishChatMessage(newMessageArrived); // sdtout to all message-List (new from v1.7)

//					this.client.getLst_globalChatMessageList().add(0, newMessageArrived); // sdtout to all message-List

							} else {
								//message is directed to another chatmember, process as such!

								newMessageArrived.setReceiver(resolveInboundReceiver(
										splittedMessageLine[7],
										chategoryForMessageAndMessageSender));

//					System.out.println("message directed to: " + newMessageArrived.getReceiver().getCallSign() + ". EQ?: " + this.client.getownChatMemberObject().getCallSign() + " sent by: " + newMessageArrived.getSender().getCallSign().toUpperCase() + " -> EQ?: "+ this.client.getChatPreferences().getLoginCallSign().toUpperCase());

								try {
									/**
									 * message is directed to me, will be put in the "to me" messagelist
									 */
									if (newMessageArrived.getReceiver().getCallSign()
											.equals(this.client.getChatPreferences().getStn_loginCallSign())) {

//							this.client.getLst_globalChatMessageList().add(0, newMessageArrived);

										this.client.publishChatMessage(newMessageArrived); // sdtout to all message-List (new from v1.7)

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
											versionInfo.setChatCategory(newMessageArrived.getChatCategory());
											versionInfo.setMessageText("/CQ " + newMessageArrived.getSender().getCallSign()
													+ " " + AUTOANSWER_PREFIX
													+ " KST4Contest v" + ApplicationConstants.APPLICATION_CURRENT_VERSION
													+ " by DO5AMF");

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

										// ==== Unified auto-answer (generic + QRG) with ping-pong guard and per-remote cooldown ====
										final String incomingText = newMessageArrived.getMessageText();
										final String incomingLower = (incomingText == null) ? "" : incomingText.toLowerCase(Locale.ROOT);

										// Never answer another automatically generated message.
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

											// A QRG reply takes precedence over the generic reply.
											String payload = null;

											if (qrgRequested) {
												payload = "QRG is: " + getAutoAnswerQrgForCategory(newMessageArrived.getChatCategory());
											} else if (genericEnabled) {

												payload = this.client.getChatPreferences().getMessageHandling_autoAnswerTextMainCat();
											}

											// Apply the cooldown only when this client is about to send a reply.
											if (payload != null && isAutoAnswerAllowedNow(newMessageArrived)) {

												ChatMessage automaticAnswer = new ChatMessage();
												ChatMember itsMe = new ChatMember();
												itsMe.setCallSign(this.client.getChatPreferences().getStn_loginCallSign());

												automaticAnswer.setSender(itsMe);
												automaticAnswer.setReceiver(newMessageArrived.getSender());
												automaticAnswer.setChatCategory(newMessageArrived.getChatCategory());

												// The fixed prefix prevents automatic clients from answering each other.
												automaticAnswer.setMessageText("/CQ " + newMessageArrived.getSender().getCallSign()
														+ " " + AUTOANSWER_PREFIX + " " + payload);

												this.client.getMessageTXBus().add(automaticAnswer);

												// Record only locally generated replies, not the later server echo.
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
//							this.client.getLst_globalChatMessageList().add(0,newMessageArrived);
										this.client.publishChatMessage(newMessageArrived); // sdtout to all message-List (new from v1.7)


										// if you sent the message to another station, it will be sorted in to
										// the "to me message list" with modified messagetext, added rxers callsign

									} else {
										/*
										 * Message sent from one other station to another other station.
										 *
										 * The old code reached this point only with members from the visible user list.
										 * With explicit fallback sender/receiver objects, this path may also see
										 * stations that already left the chat or arrived before their user-list entry.
										 * In that case locators may be missing, so angle/range analysis and DX spotting
										 * must be skipped while the chat message itself is still published.
										 */
										boolean senderHasLocator = newMessageArrived.getSender().getQra() != null
												&& !newMessageArrived.getSender().getQra().isBlank();
										boolean receiverHasLocator = newMessageArrived.getReceiver().getQra() != null
												&& !newMessageArrived.getReceiver().getQra().isBlank();

										if (senderHasLocator && receiverHasLocator) {
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
															//TODO: testing for next version 3.33: additional information will be displayed in cluster if there is such information
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

															this.client.getDxClusterServer().broadcastSingleDXClusterEntryToLoggers(onlyForSpottingObject);
														}
													} catch (Exception exception) {
														System.out.println("[MSGBUSMGT, ERROR:] DXCluster messageserver error while processing spot for 0: " + newMessageArrived.getSender().getCallSign() + " // " + exception.getMessage());
//											exception.printStackTrace();
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
										} else {
											newMessageArrived.getSender().setInAngleAndRange(false);
											System.out.println("[MSGBUSMGT, Info:] Skipping angle/range analysis for message with missing locator: "
													+ newMessageArrived.getSender().getCallSign() + " -> "
													+ newMessageArrived.getReceiver().getCallSign());
										}

//							this.client.getLst_globalChatMessageList().add(0, newMessageArrived);
										this.client.publishChatMessage(newMessageArrived); // sdtout to all message-List (new from v1.7)
//						System.out.println("MSGBS bgfx: tx call = " + newMessageArrived.getSender().getCallSign() + " / rx call = " + newMessageArrived.getReceiver().getCallSign());
									}
								} catch (NullPointerException referenceDeletedByUserLeftChatDuringMessageprocessing) {
									System.out.println("MSGBS bgfx, <<<catched error>>>: referenced user left the chat during messageprocessing or message got before user entered chat message: "
											+ referenceDeletedByUserLeftChatDuringMessageprocessing.getMessage());
									referenceDeletedByUserLeftChatDuringMessageprocessing.printStackTrace();
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
								System.out.println("[MSGMgtBus: ERROR CHATCHED ON MAYBE NULL ISSUE]: " + exceptionOccured.getMessage());
								exceptionOccured.printStackTrace();
							}

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

								ChatMember foundThisInChatMemberList = this.client.findActiveChatMember(temp4);
								if (foundThisInChatMemberList == null && this.client.getChatCategorySecondChat() != null) {
									/*
									 * LOC messages do not carry the category. The old implementation checked
									 * only the main category. Try the second active category as a fallback so
									 * dual-channel operation can still update locator changes.
									 */
									temp4.setChatCategory(this.client.getChatCategorySecondChat());
									foundThisInChatMemberList = this.client.findActiveChatMember(temp4);
								}

								if (foundThisInChatMemberList != null) {
									System.out.println("[MSGBUSMGT:] Locator Change of [" + (splittedMessageLine[2] + "], old was: "
											+ foundThisInChatMemberList.getQra() + " new is: " + splittedMessageLine[3]));
									this.client.updateActiveChatMemberLocator(temp4, splittedMessageLine[3]);
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

//				this.client.getLst_clusterMemberList().add(0, dxcMsg);
									this.client.publishClusterMessage(dxcMsg);

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

//				this.client.getLst_clusterMemberList().add(0, dxcMsg2);
										this.client.publishClusterMessage(dxcMsg2);

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
											newDXCListReceiver3.setCallSign(splittedMessageLine[4]);
											/*
											 * MA format:
											 * MA|0|epoch|sender|receiver|sender locator|receiver locator|
											 */
											newDXCListReceiver3.setQra(splittedMessageLine[6]);

											dxcMsg3.setSender(newDXCListSender3);
											dxcMsg3.setReceiver(newDXCListReceiver3);

											dxcMsg3.setMessageInhibited("");
											dxcMsg3.setQrgSpotted("");

//				this.client.getLst_clusterMemberList().add(0, dxcMsg3);
											this.client.publishClusterMessage(dxcMsg3);
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

												if (!this.client.updateActiveChatMemberState(stateChangeMember, stateChangeMember.getState())) {
													System.out.println("[MSGBUSMGT, Info:] State change for inactive user: "
															+ stateChangeMember.getCallSign() + " / " + stateChangeMember.getChatCategory());
												}

//				this.client.getChatMemberTable().get(stateChangeMember.getCallSign())
//						.setState(stateChangeMember.getState());

											} else

											/**
											 * Userinfo-update: UM3|2|HA4XN|Zoli 2m SSB/CW|JN96LX|2|
											 */
												if (splittedMessageLine[0].contains(USERINFOUPDATEORUSERISBACK)) {

													if (splittedMessageLine.length < 6) {
														System.out.println("[MSGBUSMGT, warning:] Malformed UM3 message ignored: "
																+ messageToProcess.getMessageText());
													} else {
														ChatMember stateChangeMember = new ChatMember();

														stateChangeMember.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));
														stateChangeMember.setCallSign(splittedMessageLine[2]);
														stateChangeMember.setName(splittedMessageLine[3]);
														stateChangeMember.setQra(splittedMessageLine[4]);
														stateChangeMember.setState(Integer.parseInt(splittedMessageLine[5]));
														stateChangeMember.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());
														stateChangeMember.setQrb(new Location().getDistanceKmByTwoLocatorStrings(
																client.getChatPreferences().getStn_loginLocatorMainCat(),
																stateChangeMember.getQra()));
														stateChangeMember.setQTFdirection(new Location(client.getChatPreferences().getStn_loginLocatorMainCat())
																.getBearing(new Location(stateChangeMember.getQra())));

														/*
														 * UM3 is only an info/profile update. ON4KST also sends it for stations
														 * that are not logged into this channel. Such stations must not become
														 * visible chat members, otherwise the user could try to address an
														 * offline callsign and receive server-side errors.
														 *
														 * Therefore UM3 updates existing active members only. Unknown UM3 calls
														 * are intentionally ignored; if the station joins later, UA0/UA5/UA2
														 * will deliver the complete current user information again.
														 */
														if (!client.getChatPreferences().getStn_loginCallSign().equalsIgnoreCase(stateChangeMember.getCallSign())) {
															boolean updatedActiveMember = this.client.updateActiveChatMemberInfoIfPresent(stateChangeMember);
															if (updatedActiveMember) {
																this.client.getDbHandler().storeChatMember(stateChangeMember); // TODO: not clean, it should be an update
															} else {
																System.out.println("[MSGBUSMGT, Info:] UM3 ignored for inactive user: "
																		+ stateChangeMember.getCallSign() + " / " + stateChangeMember.getChatCategory());
															}
														}
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
														newMessageArrived.setMessageSenderName(splittedMessageLine[4]);
														newMessageArrived.setMessageText(splittedMessageLine[6]);

														if (splittedMessageLine[3].equals("SERVER")) {
															ChatMember dummy = new ChatMember();
															dummy.setCallSign("SERVER");
															dummy.setName("Sysop");
															newMessageArrived.setSender(dummy);
															newMessageArrived.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));
															dummy.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));
//					System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> servers cat " + newMessageArrived.getChatCategory());

														} else {

															newMessageArrived.setSender(resolveInboundSender(
																	splittedMessageLine[3],
																	chategoryForMessageAndMessageSender,
																	newMessageArrived));
														}


														if (splittedMessageLine[7].equals("0")) {
															// message is not directed to anyone, move it to the cq messages.
															newMessageArrived.setReceiver(createAllReceiver());

//					this.client.getLst_globalChatMessageList().add(0, newMessageArrived); // sdtout to all message-List
															this.client.publishChatMessage(newMessageArrived); // sdtout to all message-List (new from v1.7)

														} else {
															//message is directed to another chatmember, process as such!

															newMessageArrived.setReceiver(resolveInboundReceiver(
																	splittedMessageLine[7],
																	chategoryForMessageAndMessageSender));

//					System.out.println("message directed to: " + newMessageArrived.getReceiver().getCallSign() + ". EQ?: " + this.client.getownChatMemberObject().getCallSign() + " sent by: " + newMessageArrived.getSender().getCallSign().toUpperCase() + " -> EQ?: "+ this.client.getChatPreferences().getLoginCallSign().toUpperCase());

															try {
																/**
																 * message is directed to me, will be put in the "to me" messagelist
																 */
																if (newMessageArrived.getReceiver().getCallSign()
																		.equals(this.client.getChatPreferences().getStn_loginCallSign())) {

//							this.client.getLst_globalChatMessageList().add(0, newMessageArrived);
																	this.client.publishChatMessage(newMessageArrived); // sdtout to all message-List (new from v1.7)

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
//							this.client.getLst_globalChatMessageList().add(0,newMessageArrived);

																	this.client.publishChatMessage(newMessageArrived); // sdtout to all message-List (new from v1.7)
																	// if you sent the message to another station, it will be sorted in to
																	// the "to me message list" with modified messagetext, added rxers callsign

																} else {
																	//message sent to other user
																	// message sent from one other station to another other station
																	boolean senderHasLocator = newMessageArrived.getSender().getQra() != null
																			&& !newMessageArrived.getSender().getQra().isBlank();

																	boolean receiverHasLocator = newMessageArrived.getReceiver().getQra() != null
																			&& !newMessageArrived.getReceiver().getQra().isBlank();

																	if (senderHasLocator && receiverHasLocator) {
																		if (DirectionUtils.isInAngleAndRange(
																				client.getChatPreferences().getStn_loginLocatorMainCat(),
																				newMessageArrived.getSender().getQra(),
																				newMessageArrived.getReceiver().getQra(),
																				client.getChatPreferences().getStn_maxQRBDefault(),
																				client.getChatPreferences().getStn_antennaBeamWidthDeg())) {

																			newMessageArrived.getSender().setInAngleAndRange(true);

																		} else {
																			newMessageArrived.getSender().setInAngleAndRange(false);
																		}
																	} else {
																		/*
																		 * Historic fallback senders/receivers may not have locators.
																		 * The message is still valid and should be displayed, but angle/range
																		 * analysis cannot be calculated without both locators.
																		 */
																		newMessageArrived.getSender().setInAngleAndRange(false);
																	}

																	this.client.publishChatMessage(newMessageArrived);
//						System.out.println("MSGBS bgfx: tx call = " + newMessageArrived.getSender().getCallSign() + " / rx call = " + newMessageArrived.getReceiver().getCallSign());
																}
															} catch (NullPointerException referenceDeletedByUserLeftChatDuringMessageprocessing) {
																System.out.println("MSGBS bgfx, <<<catched error>>>: referenced user left the chat during messageprocessing or message got before user entered chat message: "
																		+ referenceDeletedByUserLeftChatDuringMessageprocessing.getMessage());
																referenceDeletedByUserLeftChatDuringMessageprocessing.printStackTrace();
															}

															// sdtout to me message-List

														}

														try {

															System.out.println("[MSGBUSMGT:] processed message: " + newMessageArrived.getChatCategory().getCategoryNumber()
																	+ " " + newMessageArrived.getSender().getCallSign() + ", " + newMessageArrived.getMessageSenderName() + " -> "
																	+ newMessageArrived.getReceiver().getCallSign() + ": " + newMessageArrived.getMessageText());
														} catch (Exception exceptionOccured) {
															System.out.println("[MSGMgtBus: ERROR CHATCHED ON MAYBE NULL ISSUE]: " + exceptionOccured.getMessage());
															exceptionOccured.printStackTrace();
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
																	client.publishChatMessage(pwErrorMsg);
//					client.getLst_toMeMessageList().add(pwErrorMsg);
//					client.getLst_toAllMessageList().add(pwErrorMsg);
																}

//				Kst4ContestApplication.alertWindowEvent("Password was wrong. Pse check!");

																client.disconnect(ApplicationConstants.DISCSTRING_DISCONNECTONLY);

//				this.client.disconnect();
															}

															else if (splittedMessageLine[0].equals("DE")) {
																// DXCluster delimiter/end marker; intentionally ignored.
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
	 * Returns whether a message carries the fixed marker used for automatic replies.
	 */
	private boolean isAutoMessage(ChatMessage msg) {
		return msg != null
				&& msg.getMessageText() != null
				&& msg.getMessageText().contains(AUTOANSWER_PREFIX);
	}

	/**
	 * Returns the configured QRG for the category in which the request was received.
	 * If the category cannot be resolved, the main category remains the safe fallback.
	 */
	private String getAutoAnswerQrgForCategory(ChatCategory incomingCategory) {
		ChatCategory secondCategory = this.client.getChatCategorySecondChat();

		if (incomingCategory != null
				&& secondCategory != null
				&& incomingCategory.getCategoryNumber() == secondCategory.getCategoryNumber()) {
			return this.client.getChatPreferences().getMYQRGSecondCat().getValue();
		}

		return this.client.getChatPreferences().getMYQRGFirstCat().getValue();
	}

	private String autoAnswerCooldownKey(ChatMessage incoming) {

		String remoteCall = "UNKNOWN";
		if (incoming != null && incoming.getSender() != null && incoming.getSender().getCallSign() != null) {
			remoteCall = incoming.getSender().getCallSign().toUpperCase();
		}

		int categoryNumber = 0;
		if (incoming != null && incoming.getChatCategory() != null) {
			categoryNumber = incoming.getChatCategory().getCategoryNumber();
		} else if (incoming != null
				&& incoming.getSender() != null
				&& incoming.getSender().getChatCategory() != null) {
			categoryNumber = incoming.getSender().getChatCategory().getCategoryNumber();
		}

		return remoteCall + "|" + categoryNumber;
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
					} catch (RuntimeException e) {
						System.out.println("MsgBusMgt: process23001 went wrong / Runtime Error while processing: "
								+ messageTextRaw.getMessageText());
						e.printStackTrace();
					}
				}

			} catch (InterruptedException e1) {
				this.interrupt();

				e1.printStackTrace();
				break;// TODO Change at may24, avoid uncloadability. Check if this could lead to further errors on instable link!
				//				client.getMessageRXBus().clear();
			}

		} // while true end
		System.out.println("Msgbusmgt: interrupt");
		this.interrupt();
	}
}