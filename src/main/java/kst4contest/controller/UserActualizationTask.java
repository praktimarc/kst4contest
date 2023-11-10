package kst4contest.controller;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

import javafx.collections.ObservableList;
import kst4contest.model.ChatMember;
import kst4contest.model.ClusterMessage;

public class UserActualizationTask extends TimerTask {

	private ChatController client;

	public UserActualizationTask(ChatController client) {

		this.client = client;

	}

	@Override
	public void run() {

		Thread.currentThread().setName("UserActualizationTask");

//		System.out.println("[Useract: ] Thread runned now");

//		System.out.println("***********************Useract started");

		/**
		 * ******************************************since here: old mechanic for
		 * marking worked stations by .ucx-file
		 */

		HashMap<String, String> fetchedWorkedSet = new HashMap<>();
//		HashMap<String, String> fetchedWorkedSetUdpBckup = new HashMap<>();

		File f = new File(this.client.getChatPreferences().getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly());
		if (!f.exists() && !f.isDirectory()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		UCXLogFileToHashsetParser getWorkedCallsignsOfUCXLogFile = new UCXLogFileToHashsetParser(
				this.client.getChatPreferences().getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly());

//		UCXLogFileToHashsetParser getWorkedCallsignsOfUDPBackupFile = new UCXLogFileToHashsetParser(
//				this.client.getChatPreferences().getLogSynch_storeWorkedCallSignsFileNameUDPMessageBackup());		

		try {
			fetchedWorkedSet = getWorkedCallsignsOfUCXLogFile.parse();
//			fetchedWorkedSetUdpBckup = getWorkedCallsignsOfUDPBackupFile.parse();

//			for (HashMap.Entry entry : fetchedWorkedSet.entrySet()) {
//			    String key = (String) entry.getKey();
//			    Object value = entry.getValue();
//			    System.out.println("key " + key);
//			}

			System.out.println("USERACT: fetchedWorkedSet size: " + fetchedWorkedSet.size());
//			System.out.println("USERACT: fetchedWorkedSetudpbckup size: " + fetchedWorkedSetUdpBckup.size());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ObservableList<ChatMember> praktiKSTActiveUserList = this.client.getLst_chatMemberList();

		for (Iterator iterator = praktiKSTActiveUserList.iterator(); iterator.hasNext();) {
			ChatMember chatMember = (ChatMember) iterator.next();

//			System.out.println(chatMember.getCallSign());
//			System.out.println("USERACT active user list entries " + praktiKSTActiveUserList.size());

			if (fetchedWorkedSet.containsKey(chatMember.getCallSign())) {
				chatMember.setWorked(true);
				System.out.println("[USERACT, info:] marking Chatuser " + chatMember.getCallSign()
						+ " as worked, based on READONLY-Logfile.");
			}

//			if (fetchedWorkedSetUdpBckup.containsKey(chatMember.getCallSign())) {
//				chatMember.setWorked(true);
//				System.out.println("[USERACT, info:] marking Chatuser " + chatMember.getCallSign() + " as worked, based on UDPLsnBackup-Logfile.");
//			}
		}

		ObservableList<ClusterMessage> praktiKSTClusterList = this.client.getLst_clusterMemberList();

		for (Iterator iterator = praktiKSTClusterList.iterator(); iterator.hasNext();) {
			ClusterMessage clusterMessage = (ClusterMessage) iterator.next();

			if (fetchedWorkedSet.containsKey(clusterMessage.getReceiver().getCallSign())) {
				clusterMessage.setReceiverWkd(true);
				System.out.println("[USERACT, info:] marking Clusterspotted "
						+ clusterMessage.getReceiver().getCallSign() + " as worked.");
			}

//			if (fetchedWorkedSetUdpBckup.containsKey(clusterMessage.getReceiver().getCallSign())) {
//				clusterMessage.setReceiverWkd(true);
//				System.out.println("[USERACT, info:] marking Clusterspotted "
//						+ clusterMessage.getReceiver().getCallSign() + " as worked.");
//			}

		}

		/**
		 * ******************************************end here: old mechanic for marking
		 * worked stations by .ucx-file
		 * 
		 */

		/**
		 * ******************************************since here: new mechanic for
		 * marking worked stations by udp/adif based information
		 */
//		HashMap<String, String> fetchedWorkedMap = new HashMap<>();
//
//		fetchedWorkedMap = this.client.getMap_ucxLogInfoWorkedCalls();

//		ObservableList<ChatMember> praktiKSTActiveUserList1 = this.client.getLst_chatMemberList();
//
//		for (Iterator iterator = praktiKSTActiveUserList.iterator(); iterator.hasNext();) {
//			ChatMember chatMember = (ChatMember) iterator.next();
//
//			if (fetchedWorkedMap.containsKey(chatMember.getCallSign())) {
//				chatMember.setWorked(true);
//				System.out.println("[USERACT, info:] marking Chatuser " + chatMember.getCallSign() + " as worked based on UDP Log Info Collector.");
//			}
//		}

//		ObservableList<ClusterMessage> praktiKSTClusterList1 = this.client.getLst_clusterMemberList();
//
//		for (Iterator iterator = praktiKSTClusterList.iterator(); iterator.hasNext();) {
//			ClusterMessage clusterMessage = (ClusterMessage) iterator.next();
//
//			if (fetchedWorkedMap.containsKey(clusterMessage.getReceiver().getCallSign())) {
//				clusterMessage.setReceiverWkd(true);
//				System.out.println("[USERACT, info:] marking Clusterspotted "
//						+ clusterMessage.getReceiver().getCallSign() + " as worked based on UDP Log Info Collector.");
//			}
//
//		}

//
//		UCXLogFileToHashsetParser getWorkedCallsignsOfUCXLogFile = new UCXLogFileToHashsetParser(
//				"C:\\UcxLog\\Logs\\DO5AMF\\DVU322_I.UCX");
//		try {
//			fetchedWorkedSet = getWorkedCallsignsOfUCXLogFile.parse();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		ObservableList<ChatMember> praktiKSTActiveUserList = this.client.getLst_chatMemberList();
//
//		for (Iterator iterator = praktiKSTActiveUserList.iterator(); iterator.hasNext();) {
//			ChatMember chatMember = (ChatMember) iterator.next();
//
//			if (fetchedWorkedSet.containsKey(chatMember.getCallSign())) {
//				chatMember.setWorked(true);
//				System.out.println("[USERACT, info:] marking Chatuser " + chatMember.getCallSign() + " as worked.");
//			}
//		}
//
//		
//		ObservableList<ClusterMessage> praktiKSTClusterList = this.client.getLst_clusterMemberList();
//
//		for (Iterator iterator = praktiKSTClusterList.iterator(); iterator.hasNext();) {
//			ClusterMessage clusterMessage = (ClusterMessage) iterator.next();
//
//			if (fetchedWorkedSet.containsKey(clusterMessage.getReceiver().getCallSign())) {
//				clusterMessage.setReceiverWkd(true);
//				System.out.println("[USERACT, info:] marking Clusterspotted "
//						+ clusterMessage.getReceiver().getCallSign() + " as worked.");
//			}
//
//		}

		/**
		 * ******************************************end here: new mechanic for marking
		 * worked stations by udp/adif based information
		 * 
		 */

//		System.out.println("[UserActualizationtask:] Userlist actualization will be performed now. " 
//		          + LocalDateTime.ofInstant(Instant.ofEpochMilli(scheduledExecutionTime()), 
//		          ZoneId.systemDefault()));

//		ChatMessage actualizeUserMsg = new ChatMessage();
//		actualizeUserMsg.setDirectedToServer(true);
//		actualizeUserMsg.setMessage("/show users");

//		client.getMessageTXBus().add(actualizeUserMsg);

//		Enumeration<String> e = this.client.getChatMemberTable().keys();
//
//		while (e.hasMoreElements()) {
//			String key = e.nextElement();
//
//			System.out.println(this.client.getChatMemberTable().get(key).getCallSign() + ", "
//					+ this.client.getChatMemberTable().get(key).getQra() + ": "
//					+ this.client.getChatMemberTable().get(key).getFrequency());
//
//		}

//		System.out.println("[UserAct]: Show the Cluster with known frequencies now: ");
//
//		Enumeration<String> e2 = this.client.getdXClusterMemberTable().keys();
//
//		while (e2.hasMoreElements()) {
//			String key = e2.nextElement();
//
//			System.out.println(this.client.getdXClusterMemberTable().get(key).getCallSign() + ", "
//					+ this.client.getdXClusterMemberTable().get(key).getQra() + ": "
//					+ this.client.getdXClusterMemberTable().get(key).getFrequency());
//
//		}

//		for (int i = 0; i < 100; i++) {
//			
//			System.out.print("\n");  
//		}

		/**
		 * keeepalive start
		 */
//		ChatMessage keepAliveMSG = new ChatMessage();
//		keepAliveMSG.setMessageText("\r");
//		keepAliveMSG.setMessageDirectedToServer(true);
//
//		System.out.println(new Utils4KST().time_generateCurrentMMDDhhmmTimeString() + " [UserAct]: Sending keepalive: "
//				+ keepAliveMSG.getMessageText());
//		/**
//		 * Sending keepalive
//		 */
//		this.client.getMessageTXBus().add(keepAliveMSG);

		/**
		 * keeepalive end
		 */

//		System.out.println("[UserAct]: Show the Userlist with known frequencies sorted now: ");

//		ObservableList<ChatMember> userlist = this.client.getLst_chatMemberList();

//		for (Iterator iterator = userlist.iterator(); iterator.hasNext();) {
//			ChatMember chatMember = (ChatMember) iterator.next();
//			System.out.println("[Useract] Entry " + this.client.getLst_chatMemberList().indexOf(chatMember) + ": " + chatMember.getCallSign()); 
//		}

//
//		String chatMembers ="";

//		SortedSet<String> keys = new TreeSet<>(this.client.getChatMemberTable().keySet());
//		for (String key : keys) {
//			
//			chatMembers += this.client.getChatMemberTable().get(key).getCallSign() + ", "
//					+ this.client.getChatMemberTable().get(key).getName() + " in "
//					+ this.client.getChatMemberTable().get(key).getQra() + " @ QRG: "
//					+ this.client.getChatMemberTable().get(key).getFrequency() + "\n";
//					
//			System.out.println(this.client.getChatMemberTable().get(key).getCallSign() + ", "
//					+ this.client.getChatMemberTable().get(key).getName() + " in "
//					+ this.client.getChatMemberTable().get(key).getQra() + " @ QRG: "
//					+ this.client.getChatMemberTable().get(key).getFrequency());
//		}

//		System.out.println("\n[UserAct]: Show the Clusterlist with known frequencies sorted now: ");

//		String dxcMembers ="";

//		SortedSet<String> keys2 = new TreeSet<>(this.client.getdXClusterMemberTable().keySet());
//		for (String key : keys2) {
//			System.out.println(this.client.getdXClusterMemberTable().get(key).getCallSign() + " in "
//					+ this.client.getdXClusterMemberTable().get(key).getQra() + " @ QRG: "
//					+ this.client.getdXClusterMemberTable().get(key).getFrequency());
//			
//			dxcMembers += this.client.getdXClusterMemberTable().get(key).getCallSign() + " in "
//					+ this.client.getdXClusterMemberTable().get(key).getQra() + " @ QRG: "
//					+ this.client.getdXClusterMemberTable().get(key).getFrequency();
//			
//		}

//		File userListLogger = new File(new Utils4KST().time_generateCurrentMMddString() + "_praktiKST_userlist.txt");
//
//		FileWriter fileWriterRAWChatMSGOut = null;
//		
//		try {
//			fileWriterRAWChatMSGOut = new FileWriter(userListLogger, true);
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//
//		BufferedWriter bufwrtrRawMSGOut;
//		
//		bufwrtrRawMSGOut = new BufferedWriter(fileWriterRAWChatMSGOut);

//		System.out.println("#######################################" + chatMembers);
//		try {
//			bufwrtrRawMSGOut.write(new Utils4KST().time_generateCurrentMMDDhhmmTimeString() + " " +this.client.getChatMemberTable().size() + " Chatmembers:\n" + chatMembers+ "\n");
//			bufwrtrRawMSGOut.write(new Utils4KST().time_generateCurrentMMDDhhmmTimeString() + " " + this.client.getdXClusterMemberTable().size() + " Clusterentries:\n" + dxcMembers + "\n");

//			bufwrtrRawMSGOut.flush();

//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

//		try {
//			bufwrtrRawMSGOut.close();
//		} catch (IOException e) {
		// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

}
