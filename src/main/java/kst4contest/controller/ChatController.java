package kst4contest.controller;

import java.net.*;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatMessage;
import kst4contest.model.ChatPreferences;
import kst4contest.model.ClusterMessage;

import java.io.*;

/**
 * 
 * Central Chat kst4contest.controller. Instantiate only one time per category of kst Chat.
 * Provides complex data types for communication with the gui and drives the
 * threads of telnet tx, telnet rx and message processing. <br/>
 * <b>SINGLETON</b>
 * 
 */
public class ChatController {

	/**
	 * Chat selection ? 50/70 MHz..............1 144/432 MHz............2
	 * Microwave..............3 EME/JT65...............4 Low Band...............5 50
	 * MHz IARU Region 3...6 50 MHz IARU Region 2...7 144/432 MHz IARU R 2...8
	 * 144/432 MHz IARU R 3...9 kHz (2000-630m).......10 Warc (30,17,12m)......11 28
	 * MHz................12 Your choice :
	 * 
	 */
//	private int category = ChatCategory.VUHF;

	private ChatPreferences chatPreferences;

	private ChatCategory category;
	boolean connectedAndLoggedIn;
	boolean connectedAndNOTLoggedIn;
	boolean disconnected;
	boolean disconnectionPerformedByUser = false;

	public boolean isDisconnectionPerformedByUser() {
		return disconnectionPerformedByUser;
	}

	public void setDisconnectionPerformedByUser(boolean disconnectionPerformedByUser) {
		this.disconnectionPerformedByUser = disconnectionPerformedByUser;
	}

	public String getChatState() {
		return chatState;
	}

	public void setChatState(String chatState) {
		this.chatState = chatState;
	}

	public boolean isConnectedAndLoggedIn() {
		return connectedAndLoggedIn;
	}

	public void setConnectedAndLoggedIn(boolean connectedAndLoggedIn) {
		this.connectedAndLoggedIn = connectedAndLoggedIn;
	}

	public boolean isConnectedAndNOTLoggedIn() {
		return connectedAndNOTLoggedIn;
	}

	public void setConnectedAndNOTLoggedIn(boolean connectedAndNOTLoggedIn) {
		this.connectedAndNOTLoggedIn = connectedAndNOTLoggedIn;
	}

	public boolean isDisconnected() {
		return disconnected;
	}

	public void setDisconnected(boolean disconnected) {
		this.disconnected = disconnected;
	}

	/**
	 * Handles the disconnect of either the chat (Case DISCONNECTONLY) or the
	 * complete application life including all threads (case CLOSEALL)
	 * 
	 * @param action: "CLOSEALL" or "DISCONNECTONLYCHAT", on application close event
	 *                (Settings Window closed), Disconnect on Disconnect-Button
	 *                clicked (new connection may follow)
	 */
	public void disconnect(String action) {

		this.setDisconnectionPerformedByUser(true);

		if (action.equals("CLOSEALL")) {
			this.setDisconnected(true);
			this.setConnectedAndLoggedIn(false);
			this.setConnectedAndNOTLoggedIn(false);
			// disconnect telnet and kill all sockets and connections
			
			keepAliveTimer.cancel();
			keepAliveTimer.purge();
			
			ChatMessage killThreadPoisonPillMsg = new ChatMessage();
			killThreadPoisonPillMsg.setMessageText("POISONPILL_KILLTHREAD");
			killThreadPoisonPillMsg.setMessageSenderName("POISONPILL_KILLTHREAD");
			
			messageRXBus.clear();
			messageTXBus.clear();
			messageRXBus.add(killThreadPoisonPillMsg); //kills messageprocessor
			messageTXBus.add(killThreadPoisonPillMsg); //kills writethread

			writeThread.interrupt();

			readThread.interrupt();

			beaconTimer.purge();
			beaconTimer.cancel();
			ASQueryTimer.purge();
			ASQueryTimer.cancel();
			socketCheckTimer.purge();
			socketCheckTimer.cancel();

			userActualizationtimer.purge();
			userActualizationtimer.cancel();

			userActualizationtimer.purge();
			userActualizationtimer.cancel();

//			consoleReader.interrupt();
			messageProcessor.interrupt();
			
			readUDPbyUCXThread.interrupt();
			
			airScoutUDPReaderThread.interrupt();
			
			dbHandler.closeDBConnection();
			

			try {

				if (socket != null) {
					socket.close();
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		} else if (action.equals("JUSTDSICCAUSEPWWRONG")){

			this.setDisconnected(true);
			this.setConnectedAndLoggedIn(false);
			this.setConnectedAndNOTLoggedIn(true);
			// disconnect telnet and kill all sockets and connections
			
			keepAliveTimer.cancel();
			keepAliveTimer.purge();
			
			ChatMessage killThreadPoisonPillMsg = new ChatMessage();
			killThreadPoisonPillMsg.setMessageText("POISONPILL_KILLTHREAD");
			killThreadPoisonPillMsg.setMessageSenderName("POISONPILL_KILLTHREAD");
			
			messageRXBus.clear();
			messageTXBus.clear();
			messageRXBus.add(killThreadPoisonPillMsg); //kills messageprocessor
			messageTXBus.add(killThreadPoisonPillMsg); //kills writethread

			writeThread.interrupt();

			readThread.interrupt();

			beaconTimer.purge();
			beaconTimer.cancel();
			ASQueryTimer.purge();
			ASQueryTimer.cancel();
			socketCheckTimer.purge();
			socketCheckTimer.cancel();

			userActualizationtimer.purge();
			userActualizationtimer.cancel();

			userActualizationtimer.purge();
			userActualizationtimer.cancel();

//			consoleReader.interrupt();
			messageProcessor.interrupt();
			
			readUDPbyUCXThread.interrupt();
			
			airScoutUDPReaderThread.interrupt();
			
			dbHandler.closeDBConnection();

		}

	}

	// private String userName = "DO5AMF";
//	private String password = "uxskezcj";
	private String userName;
	private String password;
	private String showedName;
	private String qra;

	private String chatState;

	private String hostname = "109.90.0.130";
	private String praktiKSTVersion = "wtKST 3.1.4.6";
//	private String praktiKSTVersion = "praktiKST 1.0";
	private String praktiKSTVersionInfo = "2022-10 - 2022-12\ndeveloped by DO5AMF, Marc\nContact: praktimarc@gmail.com\nDonations via paypal are welcome";

	private int port = 23001; // kst4contest.test 4 23001
	private ReadUDPbyUCXMessageThread readUDPbyUCXThread;
	private WriteThread writeThread;
	private ReadThread readThread;
	private InputReaderThread consoleReader;
	private ChatMember ownChatMemberObject; // Todo: set at startup
	private ChatController chatController;
	private MessageBusManagementThread messageProcessor;
	private ReadUDPbyAirScoutMessageThread airScoutUDPReaderThread;

	private TimerTask userActualizationTask;

	private TimerTask keepAliveMessageSenderTask;

	private LinkedBlockingQueue<ChatMessage> messageRXBus; // Queue in which all Chatmessages are buffered, sources are
															// read- and write-thread
	private LinkedBlockingQueue<ChatMessage> messageTXBus; // Queue in which all Chatmessages are buffered, sources are
															// read- and write-thread
	private String observedSendThisMessageString;

	private DBController dbHandler;

	private Socket socket;

	private Timer userActualizationtimer;

	private Timer keepAliveTimer;

	private Timer beaconTimer;

	private Timer ASQueryTimer;

	private Timer socketCheckTimer;

	// ******All abstract types below here are used by the messageprocessor!
	// ***************
//	private Hashtable<String, ChatMember> chatMemberTable = new Hashtable<String, ChatMember>();
//	private HashMap<String, ChatMember> chatMemberTable = new HashMap<String, ChatMember>();
//	private Hashtable<String, ChatMember> dXClusterMemberTable = new Hashtable<String, ChatMember>();

	private ObservableList<ChatMessage> lst_toAllMessageList = FXCollections.observableArrayList(); // directed to all
																									// (beacon)
	private ObservableList<ChatMessage> lst_toMeMessageList = FXCollections.observableArrayList(); // directed to my
																									// call
	private ObservableList<ChatMessage> lst_toOtherMessageList = FXCollections.observableArrayList(); // directed to a
																										// call but not
																										// mine
	private ObservableList<ChatMember> chatMemberList = FXCollections.observableArrayList(); // List of active stations
																								// in chat
	private ObservableList<ChatMember> lst_chatMemberList = FXCollections.synchronizedObservableList(chatMemberList); // List
																														// of
																														// active
																														// stations
																														// in
																														// chat
	private ObservableList<ClusterMessage> lst_clusterMemberList = FXCollections.observableArrayList();

	private ObservableList<ChatMember> lst_DBBasedWkdCallSignList = FXCollections.observableArrayList();

//	private HashMap<String, ChatMember> map_ucxLogInfoWorkedCalls = new HashMap<String, ChatMember>(); //Destination of ucx-log worked-messages

	// ******************************************************************************************************************************************

	/**
	 * checks if the callsign-String of a given chatmember instance and a given list
	 * instance is in the list. If yes, returns the index in the List, <b>if not,
	 * returns -1.</b>
	 * 
	 * @param lookForThis
	 * @return Integer (index), -1 for not found
	 */
	public int checkListForChatMemberIndexByCallSign(ChatMember lookForThis) {

		if (lookForThis == null) {

//			System.out.println("[ChatCtrl] ERROR: null Value for Chatmember detected! Member cannot be in the list!");
			return -1;
		} else if (lookForThis.getCallSign() == null) {
			System.out.println("[ChatCtrl] ERROR: null Value in Callsign detected! Member cannot be in the list!");
			return -1;
		}

		for (Iterator iterator = lst_chatMemberList.iterator(); iterator.hasNext();) {
			ChatMember chatMember = (ChatMember) iterator.next();
			if (chatMember.getCallSign().equals(lookForThis.getCallSign())) {
//				System.out
//						.println("MSGBUSHELPER: Found " + chatMember.getCallSign() + " at " + lst_chatMemberList.indexOf(chatMember));

				return lst_chatMemberList.indexOf(chatMember);
			} else {

			}
		}
		/**
		 * At this point we know, the callsign is not active in the chat.
		 */
//		System.out
//				.println("[ChatCtrl, ERROR:] ChecklistForChatMemberIndexByCallsign: " + lookForThis.getCallSign() + "\n" + "List: ");
//		for (Iterator iterator = lst_chatMemberList.iterator(); iterator.hasNext();) {
//			ChatMember chatMember = (ChatMember) iterator.next();
//			System.out.println(lst_chatMemberList.indexOf(lookForThis) + ": " + chatMember.getCallSign());
//		}

		return -1;

	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public void setMessageTXBus(LinkedBlockingQueue<ChatMessage> messageTXBus) {
		this.messageTXBus = messageTXBus;
	}

	public String getPraktiKSTVersion() {
		return praktiKSTVersion;
	}

	public void setPraktiKSTVersion(String praktiKSTVersion) {
		this.praktiKSTVersion = praktiKSTVersion;
	}

	public String getPraktiKSTVersionInfo() {
		return praktiKSTVersionInfo;
	}

	public void setPraktiKSTVersionInfo(String praktiKSTVersionInfo) {
		this.praktiKSTVersionInfo = praktiKSTVersionInfo;
	}

//	public HashMap getMap_ucxLogInfoWorkedCalls() {
//		return map_ucxLogInfoWorkedCalls;
//	}

//	public void setMap_ucxLogInfoWorkedCalls(HashMap map_ucxLogInfoWorkedCalls) {
//		this.map_ucxLogInfoWorkedCalls = map_ucxLogInfoWorkedCalls;
//	}

	public ObservableList<ChatMember> getLst_chatMemberList() {
		return lst_chatMemberList;
	}

	public ObservableList<ChatMember> getLst_DBBasedWkdCallSignList() {
		return lst_DBBasedWkdCallSignList;
	}

	public void setLst_DBBasedWkdCallSignList(ObservableList<ChatMember> lst_DBBasedWkdCallSignList) {
		this.lst_DBBasedWkdCallSignList = lst_DBBasedWkdCallSignList;
	}

	public void setLst_chatMemberList(ObservableList<ChatMember> lst_chatMemberList) {
		this.lst_chatMemberList = lst_chatMemberList;
	}

	public ObservableList<ClusterMessage> getLst_clusterMemberList() {
		return lst_clusterMemberList;
	}

	public void setLst_clusterMemberList(ObservableList<ClusterMessage> lst_clusterMemberList) {
		this.lst_clusterMemberList = lst_clusterMemberList;
	}

	public ObservableList<ChatMessage> getLst_toAllMessageList() {
		return lst_toAllMessageList;
	}

	public void setLst_toAllMessageList(ObservableList<ChatMessage> lst_toAllMessageList) {
		this.lst_toAllMessageList = lst_toAllMessageList;
	}

	public ObservableList<ChatMessage> getLst_toMeMessageList() {
		return lst_toMeMessageList;
	}

	public void setLst_toMeMessageList(ObservableList<ChatMessage> lst_toMeMessageList) {
		this.lst_toMeMessageList = lst_toMeMessageList;
	}

	public ObservableList<ChatMessage> getLst_toOtherMessageList() {
		return lst_toOtherMessageList;
	}

	public void setLst_toOtherMessageList(ObservableList<ChatMessage> lst_toOtherMessageList) {
		this.lst_toOtherMessageList = lst_toOtherMessageList;
	}

	public LinkedBlockingQueue<ChatMessage> getMessageTXBus() {
		return messageTXBus;
	}

	public ChatController() {
		super();

		category = new ChatCategory(2);
		ownChatMemberObject = new ChatMember();
		ownChatMemberObject.setCallSign(userName);
		ownChatMemberObject.setName(showedName);
		ownChatMemberObject.setQra(qra);

//		this.category = ChatCategory.VUHF;
		this.userName = ownChatMemberObject.getName();
//		this.password = "uxskezcj";
		this.hostname = "www.on4kst.info";
		this.port = port;
	}

	/**
	 * This constructor is used by the Main()-Class of the praktiKST javaFX-gui.
	 * 
	 * @param setCategory
	 * @param setOwnChatMemberObject
	 */
	public ChatController(ChatMember setOwnChatMemberObject) {
		super();

		chatPreferences = new ChatPreferences();
		chatPreferences.readPreferencesFromXmlFile(); // set the praktikst Prefs by file or default if file is corrupted

		category = chatPreferences.getLoginChatCategory();
		this.userName = chatPreferences.getLoginCallSign();
		this.password = chatPreferences.getLoginPassword();
//		category = setCategory;
		ownChatMemberObject = setOwnChatMemberObject;

//		this.userName = ownChatMemberObject.getName();
//		this.password = ownChatMemberObject.getPassword();
		this.hostname = "www.on4kst.info";
		this.port = port;

	}

	public ChatPreferences getChatPreferences() {
		return chatPreferences;
	}

	public void setChatPreferences(ChatPreferences chatPreferences) {
		this.chatPreferences = chatPreferences;
	}

	public ChatMember getownChatMemberObject() {
		return ownChatMemberObject;
	}

	public void setOwnCall(ChatMember ownCall) {
		this.ownChatMemberObject = ownCall;
	}

	public LinkedBlockingQueue<ChatMessage> getMessageRXBus() {
		return messageRXBus;
	}

	public void setMessageRXBus(LinkedBlockingQueue<ChatMessage> messageBus) {
		this.messageRXBus = messageBus;
	}

	public WriteThread getWriteThread() {
		return writeThread;
	}

	public void setWriteThread(WriteThread writeThread) {
		this.writeThread = writeThread;
	}

	public ReadThread getReadThread() {
		return readThread;
	}

	public void setReadThread(ReadThread readThread) {
		this.readThread = readThread;
	}

	public ChatCategory getCategory() {
		return category;
	}

	public void setCategory(ChatCategory category) {
		this.category = category;
	}

//	public void setChatMemberTable(Hashtable<String, ChatMember> chatMemberTable) {
//		this.chatMemberTable = chatMemberTable;
//	}
//	
//	public void setChatMemberTable(HashMap<String, ChatMember> chatMemberTable) {
//		this.chatMemberTable = chatMemberTable;
//	}

	public DBController getDbHandler() {
		return dbHandler;
	}

	public void setDbHandler(DBController dbHandler) {
		this.dbHandler = dbHandler;
	}

	public void execute() throws InterruptedException, IOException {

		chatController = this;

		// This block constructs a sample message
//		ChatMessage Test = new ChatMessage();
//		Test.setMessage("kst4contest.test");
//		Test.setMessageDirectedToCommunity(false);
//		Test.setMessageGeneratedTime(new Utils4KST().time_convertEpochToReadable("1664669836"));
//		Test.setMessageSenderName("marc");
//		Test.setMessageText("test2");
//		Test.setSender(ownChatMemberObject);
//		getLst_toAllMessageList().add(Test);

		try {
			dbHandler = new DBController();

			messageRXBus = new LinkedBlockingQueue<ChatMessage>();
			messageTXBus = new LinkedBlockingQueue<ChatMessage>();
//        	messageBus.add("");

			socket = new Socket(hostname, port);
			System.out.println("Connected to the chat server: " + socket.isConnected());

//			consoleReader = new InputReaderThread(this);
//			consoleReader.start();

			readThread = new ReadThread(socket, this);
			readThread.setName("ReadThread-telnetreader");
			readThread.start();

			writeThread = new WriteThread(socket, this);
			writeThread.setName("Writethread-telnetwriter");
			writeThread.start();

			readUDPbyUCXThread = new ReadUDPbyUCXMessageThread(12060, this);
			readUDPbyUCXThread.setName("readUDPbyUCXThread");
			readUDPbyUCXThread.start();

			messageProcessor = new MessageBusManagementThread(this);
			messageProcessor.setName("messagebusManagementThread");
			messageProcessor.start();

			airScoutUDPReaderThread = new ReadUDPbyAirScoutMessageThread(9872, this, "AS", "KST");
			airScoutUDPReaderThread.setName("airscoutudpreaderThread");
			airScoutUDPReaderThread.start();

			userActualizationtimer = new Timer();
			userActualizationtimer.schedule(new UserActualizationTask(this), 4000, 60000);// TODO: Temporary
																							// userlistoutput
																							// with
			// known qrgs

			keepAliveTimer = new Timer();
			keepAliveTimer.schedule(new keepAliveMessageSenderTask(this), 4000, 60000);// TODO: Temporary
			// userlistoutput
			// with

//			keepAliveMessageSenderTask = new keepAliveMessageSenderTask(this);
//			keepAliveMessageSenderTask.run();

//			userActualizationTask = new UserActualizationTask(this); // kst4contest.test 4 23001
//			userActualizationTask.run();// kst4contest.test 4 23001

			this.setConnectedAndLoggedIn(true);

			/**
			 * The CQ-beacon-Task will be executed every time but checks for itself whether
			 * CQ messages are enabled or not
			 */
//			Timer beaconTimer;
			beaconTimer = new Timer();
			beaconTimer.schedule(new BeaconTask(this), 10000,
					this.getChatPreferences().getBcn_beaconIntervalInMinutes() * 60000);
			// 60000 * intervalInMinutes = IntervalInMillis

			/**
			 * The AS querier task will be executed every time but checks for itself whether
			 * AS usage is enabled or not
			 */
//			Timer ASQueryTimer;
			ASQueryTimer = new Timer();
			ASQueryTimer.schedule(new AirScoutPeriodicalAPReflectionInquirerTask(this), 10000, 12000);
			// 60000 * intervalInMinutes = IntervalInMillis

			/**
			 * Check if socket works
			 */
//			Timer socketCheckTimer;
			socketCheckTimer = new Timer();
			socketCheckTimer.schedule(new TimerTask() {

				@Override
				public void run() {
//					System.out.println("[Chatcontroller, info: ] periodical socketcheck");

					Thread.currentThread().setName("SocketcheckTimer");

					if (!socket.isConnected() || socket.isClosed()) {
						try {
							messageRXBus.clear();
							messageTXBus.clear();
							socket.close();
							
							chatController.setConnectedAndLoggedIn(false);
							chatController.getLst_chatMemberList().clear();

//							messageProcessor.interrupt();
//							chatController.getReadThread().interrupt();
//							chatController.getWriteThread().interrupt();

//							keepAliveTimer.wait();

//							chatController.getstat
							System.out.println("[Chatcontroller, Warning: ] Socket closed or disconnected");
						
							ChatMessage killThreadPoisonPillMsg = new ChatMessage();
							killThreadPoisonPillMsg.setMessageText("POISONPILL_KILLTHREAD");
							killThreadPoisonPillMsg.setMessageSenderName("POISONPILL_KILLTHREAD");
							
							ChatMessage killThreadPoisonPillMsg2 = new ChatMessage();
							killThreadPoisonPillMsg2.setMessageText("POISONPILL_KILLTHREAD");
							killThreadPoisonPillMsg2.setMessageSenderName("POISONPILL_KILLTHREAD");
							
							
							messageRXBus.add(killThreadPoisonPillMsg);
							
							messageTXBus.add(killThreadPoisonPillMsg2);
							chatController.getReadThread().interrupt();

						
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						/**
						 * The next block will attempt to reinit the chatclient after accidental
						 * disconnection
						 */
						try {

							if (!disconnectionPerformedByUser) {

								messageRXBus.clear();
								messageTXBus.clear();

								socket = new Socket(hostname, port);

//							readThread.interrupt();

								chatController.setReadThread(new ReadThread(socket, chatController));
								chatController.readThread.start();

								chatController.setWriteThread(new WriteThread(socket, chatController));
								chatController.writeThread.start();
								
								messageProcessor = new MessageBusManagementThread(chatController);
								messageProcessor.start();
								
//								chatController.setMessageProcessor= new MessageBusManagementThread(chatController);
//								messageProcessor.start();
								System.out.println("[Chatcontroller, info: initialized new socket, is connected? ] "
										+ socket.isConnected() + " " + socket.isClosed());

								initialize23001();

								Timer waitABit = new Timer();
								socketCheckTimer.schedule(new TimerTask() {

									@Override
									public void run() {
										Thread.currentThread().setName("waiting");
										
										if (socket.isConnected()) {
											chatController.setConnectedAndLoggedIn(true);
										}

										//just take a breath
									}
								}, 5000);


//								keepAliveTimer.notify();

							}

						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}

				}
			}, 10000, 10000);

		} catch (UnknownHostException ex) {
			System.out.println("Server not found: " + ex.getMessage());
		} catch (IOException ex) {
			System.out.println("I/O Error: " + ex.getMessage());
		}

		while (readThread == null) {
			// do nothing, wait!
			System.out.println("Reader not ready.");
		}

//		initialize();//kst4contest.test 4 23001
		initialize23001(); // init Chatcontroller for using port 23001

	}

	public long getCurrentEpochTime() {

		OffsetDateTime currentTimeInUtc = OffsetDateTime.now(ZoneOffset.UTC);

		System.out.println(currentTimeInUtc.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm X")));

		long millisecondsSinceEpoch = currentTimeInUtc.toInstant().toEpochMilli() / 1000;
//	    System.out.println(millisecondsSinceEpoch);
		return millisecondsSinceEpoch;
	}

	/**
	 * Setting the initial parameters at the chat via port 23001 <br/>
	 * <br/>
	 * <b>Login parameter format is like that: </b><br/>
	 * LOGINC|do5amf|uxskezcj|2|wtKST 3.1.4.6|25|0|1|1663879818|0| <br/>
	 * SDONE|2| <br/>
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void initialize23001() throws InterruptedException, IOException {

		messageTXBus.clear();

		ChatMessage message = new ChatMessage();

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {

				Thread.currentThread().setName("LoginStringTimer");

				String loginString = "";
				loginString = "LOGINC|" + chatPreferences.getLoginCallSign() + "|" + chatPreferences.getLoginPassword()
						+ "|" + chatPreferences.getLoginChatCategory().getCategoryNumber() + "|" + praktiKSTVersion
						+ "|25|0|1|" + getCurrentEpochTime() + "|0|";

				// System.out.println(loginString);
				ChatMessage message = new ChatMessage();
				message.setMessageText(loginString);
				message.setMessageDirectedToServer(true);
				getMessageTXBus().add(message);

			}
		}, 2000);

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {

				Thread.currentThread().setName("SDONEStringTimer");
				ChatMessage message = new ChatMessage();
				message.setMessageText("SDONE|" + chatPreferences.getLoginChatCategory().getCategoryNumber() + "|\r");
				message.setMessageDirectedToServer(true);
				getMessageTXBus().add(message);

			}
		}, 3000);

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				Thread.currentThread().setName("SETLOCTIMER");
				ChatMessage message = new ChatMessage();
				message.setMessageText("MSG|" + chatPreferences.getLoginChatCategory().getCategoryNumber()
						+ "|0|/SETLOC " + chatPreferences.getLoginLocator() + "|0|\r");
				message.setMessageDirectedToServer(true);
				getMessageTXBus().add(message);

			}
		}, 4000);

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				Thread.currentThread().setName("SETNAMETIMER");
				ChatMessage message = new ChatMessage();
				message.setMessageText("MSG|" + chatPreferences.getLoginChatCategory().getCategoryNumber()
						+ "|0|/SETNAME " + chatPreferences.getLoginName() + "|0|\r");
				message.setMessageDirectedToServer(true);
				getMessageTXBus().add(message);

			}
		}, 5000);

		new Timer().schedule(new TimerTask() {
			HashMap<String, ChatMember> getWorkedDataFromDb;

			@Override
			public void run() {

				Thread.currentThread().setName("fetchWorkedFromDBTimer");

				try {
					getWorkedDataFromDb = dbHandler.fetchChatMemberWkdDataFromDB();
				} catch (SQLException e) {
					System.out.println("[Chatctrl, Error: ] got no worked data from DB due to communication error");
				}

				for (Iterator iterator = getLst_chatMemberList().iterator(); iterator.hasNext();) {
					ChatMember chatMember = (ChatMember) iterator.next();
					System.out.println("[Chatctrl]: Marking ChatMembers wkd information: "
							+ getWorkedDataFromDb.get(chatMember.getCallSign()).getCallSign());
					chatMember.setWorked(getWorkedDataFromDb.get(chatMember.getCallSign()).isWorked());
					chatMember.setWorked144(getWorkedDataFromDb.get(chatMember.getCallSign()).isWorked144());
					;
					chatMember.setWorked432(getWorkedDataFromDb.get(chatMember.getCallSign()).isWorked432());
					;
					chatMember.setWorked1240(getWorkedDataFromDb.get(chatMember.getCallSign()).isWorked1240());
					;
					chatMember.setWorked2300(getWorkedDataFromDb.get(chatMember.getCallSign()).isWorked2300());
					;
					chatMember.setWorked3400(getWorkedDataFromDb.get(chatMember.getCallSign()).isWorked3400());
					;
					chatMember.setWorked5600(getWorkedDataFromDb.get(chatMember.getCallSign()).isWorked5600());
					;
					chatMember.setWorked10G(getWorkedDataFromDb.get(chatMember.getCallSign()).isWorked10G());
					;
				}

				/**
				 * 
				 * This creates the list of the worked stations which had to be displayed in the
				 * settings menu. TODO: May make this List editable
				 * 
				 */

				getWorkedDataFromDb.forEach((key, value) -> {

					chatController.getLst_DBBasedWkdCallSignList().add(value);

//			        System.out.println("Key=" + key + ", Value=" + value);
				});

//				for (Iterator iterator = getWorkedDataFromDb.entrySet().iterator(); iterator.hasNext();) {
//					ChatMember chatMember = (ChatMember) iterator.next();
//					getLst_DBBasedWkdCallSignList().add(chatMember);
//				}

				/* Try the not exceptional way to iterate */
//				for (ChatMember chatMemberAvl : new ArrayList<ChatMember>(getLst_chatMemberList())) {
//					if (getWorkedDataFromDb.containsKey(chatMemberAvl.getCallSign())) {
//						
//					}
//				}

			}
		}, 10000);

//		message = new ChatMessage();
//		message.setMessageText("MSG|2|0|/SETNAME " + ownChatMemberObject.getName() + "|0|\r");
//		message.setMessageDirectedToServer(true);
//		this.getMessageTXBus().add(message);

		// message.setMessageText(ownCall.getCallSign());
//		this.getMessageTXBus().add(message);

	}

	
	public void resetWorkedInfoInGuiLists() {
		
		this.chatController.getLst_chatMemberList().forEach(
			chatMember -> chatMember.resetWorkedInformationAtAllBands());
		
	}
	
	/**
	 * Setting the initial parameters at the chat
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void initialize23000() throws InterruptedException, IOException {

		ChatMessage message = new ChatMessage();

//		message.setDirectedToServer(true);
		message.setMessageText(ownChatMemberObject.getCallSign());

		this.getMessageTXBus().add(message);

		message = new ChatMessage();
//		message.setDirectedToServer(true);
		message.setMessageText(password);
		this.getMessageTXBus().add(message);
//    	
		message = new ChatMessage();
//		message.setDirectedToServer(true);
		message.setMessageText(category + "");
		this.getMessageTXBus().add(message);
//    	
		message = new ChatMessage();
//		message.setDirectedToServer(true);
		message.setMessageText("/set qra " + ownChatMemberObject.getQra());
		this.getMessageTXBus().add(message);
//    
		message = new ChatMessage();
//		message.setDirectedToServer(true);
		message.setMessageText("/set name " + ownChatMemberObject.getName());
		this.getMessageTXBus().add(message);
//    	
		message = new ChatMessage();
//		message.setDirectedToServer(true);
		message.setMessageText("/set here");
		this.getMessageTXBus().add(message);

//		message = new ChatMessage();
//		message.setDirectedToServer(true);
//		message.setMessageText("/show user");
//		this.getMessageTXBus().add(message);
		// will done by another Thread
	}

}