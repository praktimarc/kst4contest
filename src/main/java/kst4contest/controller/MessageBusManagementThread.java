package kst4contest.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
//import java.net.Socket;
//import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import kst4contest.model.AirPlaneReflectionInfo;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatMessage;
import kst4contest.model.ClusterMessage;

/**
 * 
 * This thread is responsible for processing received messages.
 * It checks all messages from server for their functional contest, such as commands to build or change the userlist
 * or their settings, get clustermessages and sure the content of all chatmessages, which are delivered.
 * 
 */
public class MessageBusManagementThread extends Thread {

	int index;

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

	public MessageBusManagementThread(ChatController client) {

		this.client = client;

	}

	/**
	 * check if a chatmessage is part of the userlist via telnet 23000 port<br/>
	 * <b>Updates userlist!</b>
	 * 
	 * @param chatMessage
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
	 * <b>Updates userlist!</b>
	 * 
	 * @param chatMessage
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
	 * @param chatMessage
	 */
	private String checkIfMessageInhibitsFrequency(ChatMessage messageToProcess) {

		Pattern pattern = Pattern.compile(PTRN_QRG_CAT2); // TODO: PTRN should depend to category-selection of own stn
		Matcher matcher = pattern.matcher(messageToProcess.getMessageText());
		String[] splittedQRGString;
//		splittedQRGString[0] = "0";

		String stringAggregation = "";

//		if (matcher.) {
//			stringAggregation = ""; //reset aggregated string			
//		}

		while (matcher.find()) {
//				System.out.println("QRG detected: "+ matcher.group() + " " + matcher.start());

//			ChatMember member = new ChatMember();
			String matchedString = matcher.group();

//			splittedQRGString = new String[0];
			splittedQRGString = matchedString.split(" ");

			for (int i = 0; i < splittedQRGString.length; i++) {
				stringAggregation += splittedQRGString[i] + " ";
			}

			System.out.println("[MSGBUSMGT:] Processed qrg info: " + stringAggregation);

//				if (member.getName().)

//			System.out.println("Processed QRG Entry [" + this.client.getChatMemberTable().size() + "]: Call: "
//					+ member.getCallSign() + ", QRA: " + member.getQra() + ", Name: " + member.getName());
		}
		return stringAggregation;
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
			System.out.println("[MSGBUSMGTT:] ######################no processable data");
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
		 * Old mechanic for index search,new one implemented due concurrentmodificationexcm which works - end
		 * 
		 */

		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getCallSign().equals(lookForThis.getCallSign())) {
//				System.out
//						.println("MSGBUSHELBER: Found " + chatMember.getCallSign() + " at " + list.indexOf(chatMember));

				return list.indexOf(list.get(i));
			}
		}

//		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
//			ChatMember chatMember = (ChatMember) iterator.next();
//			System.out.println(list.indexOf(lookForThis) + ": " + chatMember.getCallSign());
//		}

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

		final String INITIALUSERLISTENTRY = "UA0";
		final String USERENTEREDCHAT = "UA5";
		final String USERENTEREDCHAT2 = "UA2"; // seen at 50MHZ Chat
		final String initialChatHistoryEntry = "CR";
		final String SERVERMESSAGE = "CR";
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

		if (messageToProcess.getMessageText().isEmpty()) {
			System.out.println("[MSGBUSMGTT:] ######################no processable data");

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
			 */
			if (splittedMessageLine[0].contains(INITIALUSERLISTENTRY)) {
//				System.out.println("MSGBUS: User detected");

				ChatMember newMember = new ChatMember();

				newMember.setAirPlaneReflectInfo(new AirPlaneReflectionInfo()); // TODO: Only bugfix, check

				newMember.setCallSign(splittedMessageLine[2]);
				newMember.setName(splittedMessageLine[3]);
				newMember.setQra(splittedMessageLine[4]);
				newMember.setState(Integer.parseInt(splittedMessageLine[5]));
				newMember.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());

//				this.client.getChatMemberTable().put(splittedMessageLine[2], newMember); //TODO: map -> List

				//the own call will not be in the list
				if (!client.getChatPreferences().getLoginCallSign().equals(newMember.getCallSign())) {
					this.client.getLst_chatMemberList().add(newMember);
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
			 */
			if (splittedMessageLine[0].contains(USERENTEREDCHAT) || splittedMessageLine[0].contains(USERENTEREDCHAT2)) {
//				System.out.println("MSGBUS: User detected");

				/**
				 * The own callsign will not be hold in the userlist any more
				 */
				if (!client.getChatPreferences().getLoginCallSign().equals(splittedMessageLine[2])) {


					ChatMember newMember = new ChatMember();

					newMember.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());
					newMember.setCallSign(splittedMessageLine[2]);
					newMember.setName(splittedMessageLine[3]);
					newMember.setQra(splittedMessageLine[4]);
					newMember.setState(Integer.parseInt(splittedMessageLine[5]));
					newMember.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());

					newMember = this.client.getDbHandler().fetchChatMemberWkdDataForOnlyOneCallsignFromDB(newMember);

					this.client.getLst_chatMemberList().add(newMember);
					this.client.getDbHandler().storeChatMember(newMember);
				}


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

				newMember.setCallSign(splittedMessageLine[2]);

//				this.client.getChatMemberTable().remove(newMember.getCallSign());

				System.out.println("[MSGBUSMGT, Info:] User left Chat and will be removed from list ["
						+ this.client.getLst_chatMemberList().size() + "] :" + newMember.getCallSign());
				try {
					this.client.getLst_chatMemberList().remove(
							checkListForChatMemberIndexByCallSign(this.client.getLst_chatMemberList(), newMember));
					
				} catch (Exception e) {
					System.out.println("[MSGBUSMGT, EXC!, Error:] User sent left chat but had not been there ... ["
							+ this.client.getLst_chatMemberList().size() + "] :" + newMember.getCallSign() + "\n"
							+ e.getStackTrace());
//					e.printStackTrace();
				}

//				int indexToDelete = checkListForChatMemberIndexByCallSign(this.client.getLst_chatMemberList(),
//						newMember);
//				if (indexToDelete != -1) {
//					System.out.println("[MSGBUSMGT:] User left Chat and is removed from list ["
//							+ this.client.getLst_chatMemberList().size() + "] :" + newMember.getCallSign());
//
//					this.client.getLst_chatMemberList().remove(indexToDelete);
//
//				} else {
//					System.out.println("[MSGBUSMGT:] Error, user sent left chat but had not been there ... ["
//							+ this.client.getLst_chatMemberList().size() + "] :" + newMember.getCallSign());
//
//				}

			} else

			/**
			 * Chatmessage dm5m to do5amf CH|2|1663966534|DM5M|dm5m-team|0|kst4contest.test|DO5AMF|
			 * 
			 * CH|2|1663966535|DM5M|dm5m-team|0|kst4contest.test|0|
			 */
			if (splittedMessageLine[0].contains(CHATCHANNELMESSAGE)) {
//					System.out.println("MSGBUS: User detected");

				ChatMessage newMessage = new ChatMessage();
				newMessage.setChatCategory(this.client.getCategory());
				newMessage.setMessageGeneratedTime(splittedMessageLine[2]); // TODO: insert readable time?

				if (splittedMessageLine[3].equals("SERVER")) {
					ChatMember dummy = new ChatMember();
					dummy.setCallSign("SERVER");
					dummy.setName("Sysop");
					newMessage.setSender(dummy);

				} else {

					ChatMember sender = new ChatMember();
					sender.setCallSign(splittedMessageLine[3]);

					int index = checkListForChatMemberIndexByCallSign(this.client.getLst_chatMemberList(), sender);

					if (index != -1) {
						//user found in the chatmember list
						newMessage.setSender(this.client.getLst_chatMemberList().get(index)); // set sender to member of
																								// b4 init list
					} else {
						//user not found in chatmember list
						if (!sender.getCallSign().equals(this.client.getChatPreferences().getLoginCallSign().toUpperCase())) {
							sender.setCallSign("[n/a]" + sender.getCallSign());
							// if someone sent a message without being in the userlist (cause
							// on4kst missed implementing....), callsign will be marked
						} else {
							newMessage.setSender(sender); //my own call is the sender
						}
					}

//					newMessage.setSender(this.client.getChatMemberTable().get(splittedMessageLine[3]));
				}

				newMessage.setMessageSenderName(splittedMessageLine[4]);
				newMessage.setMessageText(splittedMessageLine[6]);

				if (splittedMessageLine[7].equals("0")) {
					// message is not directed to anyone, move it to the cq messages
					ChatMember dummy = new ChatMember();
					dummy.setCallSign("ALL");
					newMessage.setReceiver(dummy);

					this.client.getLst_toAllMessageList().add(0, newMessage); // sdtout to all message-List

				} else {

					ChatMember receiver = new ChatMember();
					receiver.setCallSign(splittedMessageLine[7]);

					int index = checkListForChatMemberIndexByCallSign(this.client.getLst_chatMemberList(), receiver);

					if (index != -1) {
						newMessage.setReceiver(this.client.getLst_chatMemberList().get(index));// -1: Member left Chat
																								// before...
					} else {


						if (receiver.getCallSign().equals(client.getChatPreferences().getLoginCallSign())) {
							/**
							 * If mycallsign sent a message to the server, server will publish that message and
							 * send it to all chatmember including me.
							 * As mycall is not in the userlist,  the message would not been displayed if I handle
							 * it in the next case (marking left user, just for information). But I want an echo.
							 */

							receiver.setCallSign(client.getChatPreferences().getLoginCallSign());
							newMessage.setReceiver(receiver);
						} else {
							//this are user which left chat but had been adressed by this message
							receiver.setCallSign(receiver.getCallSign() + "(left)");
							newMessage.setReceiver(receiver);
						}
					}

//					System.out.println("message directed to: " + newMessage.getReceiver().getCallSign() + ". EQ?: " + this.client.getownChatMemberObject().getCallSign() + " sent by: " + newMessage.getSender().getCallSign().toUpperCase() + " -> EQ?: "+ this.client.getChatPreferences().getLoginCallSign().toUpperCase());

					if (newMessage.getReceiver().getCallSign()
							.equals(this.client.getChatPreferences().getLoginCallSign())) {

						this.client.getLst_toMeMessageList().add(0, newMessage);

						System.out.println("message directed to me: " + newMessage.getReceiver().getCallSign() + ".");

					} else if (newMessage.getSender().getCallSign().toUpperCase() // if you sent the message to another station, it will be sorted in to
																	// the "to me message list" with modified messagetext, added rxers callsign
							.equals(this.client.getChatPreferences().getLoginCallSign().toUpperCase())) {
						String originalMessage = newMessage.getMessageText();
						newMessage
								.setMessageText("(>" + newMessage.getReceiver().getCallSign() + ")" + originalMessage);
						this.client.getLst_toMeMessageList().add(0, newMessage); // TODO:check

					} else {
						this.client.getLst_toOtherMessageList().add(0, newMessage);

//						System.out.println("MSGBS bgfx: tx call = " + newMessage.getSender().getCallSign() + " / rx call = " + newMessage.getReceiver().getCallSign());
					}

					// sdtout to me message-List

//					newMessage.setReceiver(this.client.getChatMemberTable().get(splittedMessageLine[7])); // set sender
					// to the
					// member of
					// before
					// initialized
					// list
				}

//				System.out.println("[MSGBUSMGT:] processed message: " + newMessage.getChatCategory().getCategoryNumber()
//						+ " " + newMessage.getSender().getCallSign() + ", " + newMessage.getMessageSenderName() + " -> "
//						+ newMessage.getReceiver().getCallSign() + ": " + newMessage.getMessageText());

				String locatedFrequencies = checkIfMessageInhibitsFrequency(newMessage);
				
				SimpleStringProperty qrg = new SimpleStringProperty(locatedFrequencies);

				if (!splittedMessageLine[3].equals("SERVER")) {

					if (locatedFrequencies.equals("")) {
						// no qrg found, nothing to do
					} else {

//						String stringAggregation = "";
//
//						for (int i = 0; i < locatedFrequencies.length; i++) {
//							stringAggregation += locatedFrequencies[i] + " ";
//						}

						ChatMember temp3 = new ChatMember();
						temp3.setCallSign(splittedMessageLine[3]);
						int index = checkListForChatMemberIndexByCallSign(this.client.getLst_chatMemberList(), temp3);

						if (index == -1) { // user is not in the userlist but sent message...

							/**
							 * CH|2|1664663240|IK7LMX|Gilberto QRO|0|pse ant to jn80|YT5W| Caused this line
							 */
							System.out.println("[MSGBUSMGT:] ERROR, Frequency for " + splittedMessageLine[3]
									+ " is not settable, Callsign is not in the Member-list!");

							ChatMember newMember = new ChatMember();
							newMember.setCallSign(splittedMessageLine[3]);
							newMember.setName(splittedMessageLine[4]);
							newMember.setFrequency(qrg);
//							newMember.setFrequency(locatedFrequencies);
//							this.client.getLst_chatMemberList().add(newMember);

						} else {
							/**
							 * User is in the list...
							 * 
							 */
							this.client.getLst_chatMemberList().get(index).setFrequency(qrg);
							System.out.println("[MSGBUSMGT:] Frequency for " + splittedMessageLine[3] + " setted: "
									+ locatedFrequencies);

//							this.client.getLst_chatMemberList().

//							ChatMember dummy = new ChatMember();
//							
//							dummy.setAirPlaneReflectInfo(new AirPlaneReflectionInfo()); //TODO: check if this is neccessary
//							this.client.getLst_chatMemberList().add(dummy); // TODO: Bugfix for UI actualization, maybe we dont need that any more
//							this.client.getLst_chatMemberList().remove(dummy);
//							this.client.getLst_chatMemberList().sorted();

//							
						}
					}
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
				temp4.setCallSign(splittedMessageLine[2]);
				temp4.setQra(splittedMessageLine[3]);
				temp4.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());

				int index = checkListForChatMemberIndexByCallSign(this.client.getLst_chatMemberList(), temp4);

				if (index != -1) {

					System.out.println("[MSGBUSMGT:] Locator Change of [" + (splittedMessageLine[2] + "], old was: "
							+ this.client.getLst_chatMemberList().get(index).getQra() + " new is: "
							+ splittedMessageLine[3]));

					this.client.getLst_chatMemberList().get(index).setQra(splittedMessageLine[3]);

				} else {
					System.out.println("[MSGBUSMGT:] ERROR! Locator Change of ["
							+ (splittedMessageLine[2] + "] is not possible, user is not in the Table!"));

//					ChatMember newMember = new ChatMember();
//					newMember.setCallSign(splittedMessageLine[2]);
//					newMember.setQra(splittedMessageLine[3]);
//					this.client.getChatMemberTable().put(newMember.getCallSign(), newMember);

//					this.client.getLst_chatMemberList().add(temp4);
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

				stateChangeMember.setCallSign(splittedMessageLine[2]);
				stateChangeMember.setName(splittedMessageLine[3]);
				stateChangeMember.setQra(splittedMessageLine[4]);
				stateChangeMember.setState(Integer.parseInt(splittedMessageLine[5]));
				stateChangeMember.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());

				this.client.getDbHandler().storeChatMember(stateChangeMember); // TODO: not clean, it should be an
																				// upodate

//					System.out.println("[MSGBUSMGT:] DXCluster Message detected ");

				int index = checkListForChatMemberIndexByCallSign(this.client.getLst_chatMemberList(),
						stateChangeMember);

				this.client.getLst_chatMemberList().get(index).setName(stateChangeMember.getName());
				this.client.getLst_chatMemberList().get(index).setQra(stateChangeMember.getQra());
				this.client.getLst_chatMemberList().get(index).setState(stateChangeMember.getState());

//				this.client.getChatMemberTable().get(stateChangeMember.getCallSign())
//						.setName(stateChangeMember.getName());
//				this.client.getChatMemberTable().get(stateChangeMember.getCallSign())
//						.setQra(stateChangeMember.getQra());
//				this.client.getChatMemberTable().get(stateChangeMember.getCallSign())
//						.setState(stateChangeMember.getState());

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
					splittedMessageLine[2] += "pse disc- and reconnect";
				}
				
				ChatMember server = new ChatMember();
				server.setCallSign("SERVER");
				server.setName("SERVER");
				
				ChatMessage pwErrorMsg = new ChatMessage();
				
				pwErrorMsg.setMessageGeneratedTime(client.getCurrentEpochTime()+"");
				pwErrorMsg.setSender(server);
				pwErrorMsg.setMessageText(splittedMessageLine[2]);
				
				for (int i = 0; i < 10; i++) {
					client.getLst_toMeMessageList().add(pwErrorMsg);
					client.getLst_toAllMessageList().add(pwErrorMsg);
				}
				
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

	@Override
	public void interrupt() {
		super.interrupt();
		
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
					
					if (messageTextRaw.getMessageText().equals("POISONPILL_KILLTHREAD") && messageTextRaw.getMessageSenderName().equals("POISONPILL_KILLTHREAD")) {
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
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
					}
					
				} catch (InterruptedException e1) {
					this.interrupt();
					// TODO Auto-generated catch block
					e1.printStackTrace();
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

//			System.out.println("messagebusmgt while performed");
			
		} // while true end
		System.out.println("Msgbusmgt: interrupt");
		this.interrupt();
	}
}
