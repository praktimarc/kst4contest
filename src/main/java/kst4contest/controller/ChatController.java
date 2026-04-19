package kst4contest.controller;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import kst4contest.ApplicationConstants;
import kst4contest.controller.interfaces.PstRotatorEventListener;
import kst4contest.locatorUtils.DirectionUtils;
import kst4contest.logic.PriorityCalculator;
import kst4contest.model.*;
import kst4contest.test.MockKstServer;
import kst4contest.utils.PlayAudioUtils;
import kst4contest.view.Kst4ContestApplication;

import java.io.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 
 * Central Chat kst4contest.controller. Instantiate only one time per category of kst Chat.
 * Provides complex data types for communication with the gui and drives the
 * threads of telnet tx, telnet rx and message processing. <br/>
 * <b>SINGLETON</b>
 * 
 */
public class ChatController implements ThreadStatusCallback, PstRotatorEventListener {

	/**
	 * Chat selection ? 50/70 MHz..............1 144/432 MHz............2
	 * Microwave..............3 EME/JT65...............4 Low Band...............5 50
	 * MHz IARU Region 3...6 50 MHz IARU Region 2...7 144/432 MHz IARU R 2...8
	 * 144/432 MHz IARU R 3...9 kHz (2000-630m).......10 Warc (30,17,12m)......11 28
	 * MHz................12 Your choice :
	 * 
	 */

	private static final boolean DEBUG_BAND_UPGRADE_HINT = true; //for new band hint


	private PstRotatorClient rotatorClient;
    private Consumer<Double> viewRotorCallback;

    private Kst4ContestApplication view; //effectively final, for recoupling of the controller to the view

    private StatusUpdateListener statusListener; //update info interface for the threads


    public void setView(Kst4ContestApplication view) {
        this.view = view;
    }

    private UpdateInformation updateInformation;
	private ChatPreferences chatPreferences;

	private ChatCategory chatCategoryMain;
	private ChatCategory chatCategorySecondChat;
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


	public ChatCategory getChatCategorySecondChat() {
		return chatCategorySecondChat;
	}

	public void setChatCategorySecondChat(ChatCategory chatCategorySecondChat) {
		this.chatCategorySecondChat = chatCategorySecondChat;
	}

	public UpdateInformation getUpdateInformation() {
		return updateInformation;
	}

	public void setUpdateInformation(UpdateInformation updateInformation) {
		this.updateInformation = updateInformation;
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


    public StatusUpdateListener getStatusListener() {
        return statusListener;
    }

    public void setStatusListener(StatusUpdateListener statusListener) {
        this.statusListener = statusListener;
    }

    @Override
    public void onThreadStatus(String threadName, ThreadStateMessage threadStateMessage) {
        // Weiterleiten an die View
        if (statusListener != null) {
            statusListener.onThreadStatusChanged(threadName, threadStateMessage);
        } else System.out.println("ERRRRRRRRRRRRRRRRRRRRRRRRRRRÖRRRRRRRRRRRRRRRRRRR");
    }


    /********************************************************************************
     * PSTRotator controlling
     *******************************************************************************/




    public void initRotor() {
        // Beispiel: PSTRotator läuft lokal auf Port 12060
        // Der Client wird automatisch auf 12061 hören.
        rotatorClient = new PstRotatorClient("127.0.0.1", 12000, this, this); //TODO: IP anpassen, Port auch aus den prefs holen, default 12000

        // Startet den Thread und das Polling
        rotatorClient.start();
    }

    /**
     * sets rotator to "AZ DEGREE" by button click <br/><br/>
     * <b>Note that there is a workaround for spid rotators: <br/>
     * The AZ will be set, after 'time' secs it will be controlled if the rotator started, If not, the rotator will<br/>
     * be homed to 0 deg for very shord period, then the AZ value will be set again.
     * </b>
     * @param azimuth
     */
    public void rotateTo(double azimuth) {

        double beforeRotateAzWas = chatPreferences.getActualQTF().getValue();

        if (rotatorClient != null) {
            rotatorClient.setTrackingMode(false);
            System.out.println("Chatcontroller, Info: turning ant to " + azimuth + " by user request");
            rotatorClient.setAzimuth(azimuth);

            Object lockDelay = new Object();
            synchronized (lockDelay) {
                try{

                    TimeUnit.SECONDS.sleep(2);; //wait 2s, then check if rotator does anything due SPID
                    // sometimes does simply not accept a rotating value for first try!
                } catch (InterruptedException e) {

                }
            }

            if (chatPreferences.getActualQTF().getValue() == beforeRotateAzWas) {
                rotatorClient.setAzimuth(0); //do some reset
                rotatorClient.setAzimuth(azimuth); //then rotate
            }

        }
    }

	/**
	 * Called when an external logger (Win-Test or UCXLog interface) reports that a QSO was logged.
	 *
	 * Process goal:
	 * 1) Detect whether the logged station is *still active (QRV)* on at least one *other* band
	 *    that is enabled for "my station" (stn_bandActive[Band]) AND not worked yet (worked144/432/...).
	 * 2) If yes: trigger an on-screen hint (blinking status button) and play the existing sked-notification sound.
	 * 3) Request a score recompute so the station can become visible again (optional boost is applied in PriorityCalculator).
	 *
	 * IMPORTANT:
	 * - We do NOT use ChatMember.worked (UI-only filter flag) for scoring decisions.
	 * - We only use per-band worked flags (worked144, worked432, ...).
	 * - "QRV on band" is derived from recent entries in ChatMember.knownActiveBands.
	 */
	public void onExternalLogEntryReceived(String callSignRaw) {

		if (callSignRaw == null || callSignRaw.isBlank()) return;
		if (chatPreferences == null) return;
		if (!chatPreferences.isNotify_bandUpgradeHintOnLogEnabled()) return;

		final String callRaw = normalizeCallRaw(callSignRaw);

		if (DEBUG_BAND_UPGRADE_HINT) {
			System.out.println("[BandUpgradeHint] LOG received for call=" + callRaw);
		}

		// 1) Determine which bands I am active on (configured at startup via stn_bandActive* flags)
		EnumSet<Band> myEnabledBands = getMyEnabledBandsFromPrefs(chatPreferences);
		if (myEnabledBands.isEmpty()) return;

		// 2) Determine which bands the station was recently seen active on (from Smart Frequency Extraction history)
		final long now = System.currentTimeMillis();
		final long maxAgeMs = TimeUnit.MINUTES.toMillis(30); // keep consistent with "recent activity" semantics
		EnumSet<Band> stationOfferedBands = collectStationOfferedBandsFromHistory(callRaw, now, maxAgeMs);
		if (stationOfferedBands.isEmpty()) return;

		// 3) Keep only bands that I can actually work
		stationOfferedBands.retainAll(myEnabledBands);
		if (stationOfferedBands.isEmpty()) return;

		// 4) Determine already worked bands (per-band flags only)
		EnumSet<Band> workedBands = collectWorkedBands(callRaw);

		// 5) Remaining bands = offered ∩ enabled - worked
		EnumSet<Band> remainingBands = EnumSet.copyOf(stationOfferedBands);
		remainingBands.removeAll(workedBands);
		if (remainingBands.isEmpty()) return;

		if (DEBUG_BAND_UPGRADE_HINT) {
			System.out.println("[BandUpgradeHint] call=" + callRaw
					+ " enabled=" + formatBandsHuman(myEnabledBands)
					+ " offered=" + formatBandsHuman(stationOfferedBands)
					+ " worked=" + (workedBands.isEmpty() ? "-" : formatBandsHuman(workedBands))
					+ " remaining=" + formatBandsHuman(remainingBands));
		}

		// 6) Build UI text (button + tooltip)
		String remainingHuman = formatBandsHuman(remainingBands);
		String shortText = "BAND+ " + callRaw + " " + remainingHuman;

		String tooltip = "Logged " + callRaw + ", but station is still QRV on additional band(s): "
				+ remainingHuman
				+ "\n(Enabled: " + formatBandsHuman(myEnabledBands)
				+ " | Worked: " + (workedBands.isEmpty() ? "-" : formatBandsHuman(workedBands)) + ")";

		ThreadStateMessage msg = new ThreadStateMessage("BandUpgradeHint", true, tooltip, false);
		msg.setRunningInformationTextDescription(shortText);

		// 7) Trigger status update -> View will blink a dedicated indicator button
		onThreadStatus("BandUpgradeHint", msg);

		// 8) Sound (re-use existing sked notification sound) - respects global simple-sound flag
		if (chatPreferences.isNotify_playSimpleSounds()) {
			try {
				getPlayAudioUtils().playNoiseLauncher('!'); // same as SkedReminderService
			} catch (Exception e) {
				System.out.println("[ChatController, warning]: failed to play band-upgrade hint sound: " + e.getMessage());
			}
		}

		// 9) Make sure score reacts quickly (boost is applied in PriorityCalculator if enabled)
		if (getScoreService() != null) {
			getScoreService().requestRecompute("BandUpgradeHint");
		}
	}

	/** Normalize callsign raw to a stable key for comparisons. */
	private static String normalizeCallRaw(String callRaw) {
		return callRaw.trim().toUpperCase(Locale.ROOT);
	}

	/** Helper: create enabled-band set from preferences. */
	private static EnumSet<Band> getMyEnabledBandsFromPrefs(ChatPreferences prefs) {
		EnumSet<Band> s = EnumSet.noneOf(Band.class);
		if (prefs.isStn_bandActive144())  s.add(Band.B_144);
		if (prefs.isStn_bandActive432())  s.add(Band.B_432);
		if (prefs.isStn_bandActive1240()) s.add(Band.B_1296);
		if (prefs.isStn_bandActive2300()) s.add(Band.B_2320);
		if (prefs.isStn_bandActive3400()) s.add(Band.B_3400);
		if (prefs.isStn_bandActive5600()) s.add(Band.B_5760);
		if (prefs.isStn_bandActive10G())  s.add(Band.B_10G);
		return s;
	}

	/**
	 * Helper: union of all recently detected "QRV on band" entries across *all* ChatMember instances
	 * having the same callSignRaw (because a callsign may exist multiple times with different categories).
	 */
	private EnumSet<Band> collectStationOfferedBandsFromHistory(String callRaw, long nowMs, long maxAgeMs) {

		EnumSet<Band> offered = EnumSet.noneOf(Band.class);

		synchronized (getLst_chatMemberList()) {
			for (ChatMember cm : getLst_chatMemberList()) {
				if (cm == null || cm.getCallSignRaw() == null) continue;
				if (!normalizeCallRaw(cm.getCallSignRaw()).equals(callRaw)) continue;

				Map<Band, ChatMember.ActiveFrequencyInfo> map = cm.getKnownActiveBands();
				if (map == null || map.isEmpty()) continue;

				for (Map.Entry<Band, ChatMember.ActiveFrequencyInfo> e : map.entrySet()) {
					if (e.getKey() == null || e.getValue() == null) continue;

					long age = nowMs - e.getValue().timestampEpoch;
					if (age >= 0 && age <= maxAgeMs) {
						offered.add(e.getKey());
					}

					if (DEBUG_BAND_UPGRADE_HINT) {
						System.out.println("[BandUpgradeHint] history call=" + callRaw
								+ " band=" + e.getKey()
								+ " freq=" + e.getValue().frequency
								+ " ageMs=" + age);
					}


				}
			}
		}
		return offered;
	}

	/**
	 * Helper: union of per-band worked flags across all ChatMember instances for the same call.
	 * IMPORTANT: ChatMember.worked is UI-only and NOT used here.
	 */
	private EnumSet<Band> collectWorkedBands(String callRaw) {

		EnumSet<Band> worked = EnumSet.noneOf(Band.class);

		synchronized (getLst_chatMemberList()) {
			for (ChatMember cm : getLst_chatMemberList()) {
				if (cm == null || cm.getCallSignRaw() == null) continue;
				if (!normalizeCallRaw(cm.getCallSignRaw()).equals(callRaw)) continue;

				if (cm.isWorked144())  worked.add(Band.B_144);
				if (cm.isWorked432())  worked.add(Band.B_432);
				if (cm.isWorked1240()) worked.add(Band.B_1296);
				if (cm.isWorked2300()) worked.add(Band.B_2320);
				if (cm.isWorked3400()) worked.add(Band.B_3400);
				if (cm.isWorked5600()) worked.add(Band.B_5760);
				if (cm.isWorked10G())  worked.add(Band.B_10G);
				if (cm.isWorked24G())  worked.add(Band.B_24G); // optional, only if your Band enum supports it
			}
		}
		return worked;
	}

	private static String formatBandsHuman(EnumSet<Band> bands) {
		if (bands == null || bands.isEmpty()) return "-";
		return bands.stream().map(ChatController::bandToHumanLabel).sorted().reduce((a, b) -> a + ", " + b).orElse("-");
	}

	private static String bandToHumanLabel(Band b) {
		if (b == null) return "?";
		return switch (b) {
			case B_144 -> "2m";
			case B_432 -> "70cm";
			case B_1296 -> "23cm";
			case B_2320 -> "13cm";
			case B_3400 -> "9cm";
			case B_5760 -> "6cm";
			case B_10G -> "3cm";
			case B_24G -> "1.2cm";
			default -> b.name();
		};
	}



    public void stopRotator() {
        if (rotatorClient != null) {
            rotatorClient.stop();
        }
    }

	@Override
	public void onAzimuthUpdate(double azimuth) {
		// We are in the rotor client thread. JavaFX properties must be updated on the FX thread.
		Runnable fxUpdate = () -> chatPreferences.getActualQTF().setValue(azimuth);

		if (Platform.isFxApplicationThread()) {
			fxUpdate.run();
		} else {
			Platform.runLater(fxUpdate);
		}
	}

    @Override
    public void onElevationUpdate(double elevation) {
//        System.out.println("Neue Elevation: " + elevation);
        //not used in first version
    }

    @Override
    public void onModeUpdate(boolean isTracking) {
//        System.out.println("Modus: " + (isTracking ? "Tracking" : "Manuell"));
        //not used in first version
    }

    @Override
    public void onMessageReceived(String raw) {
        // Logging
    }




	/**
	 * Helping
	 * @param targetCallSignRaw
	 * @param preferredCategory
	 * @param messageAfterCq
	 */
	public void queuePrivateCqMessage(String targetCallSignRaw, ChatCategory preferredCategory, String messageAfterCq) {
		if (targetCallSignRaw == null || targetCallSignRaw.isBlank()) return;

		ChatCategory categoryToUse = preferredCategory;
		if (categoryToUse == null) {
			ChatCategory last = lastInboundCategoryByCallSignRaw.get(targetCallSignRaw.trim().toUpperCase());
			categoryToUse = (last != null) ? last : chatCategoryMain;
		}

		String text = "/cq " + targetCallSignRaw.trim().toUpperCase() + " " + (messageAfterCq == null ? "" : messageAfterCq);

		ChatMessage msg = new ChatMessage();
		msg.setChatCategory(categoryToUse);
		msg.setMessageText(text);
		msg.setMessageDirectedToServer(false);

		messageTXBus.add(msg);

		// Metrics: treat this as an outbound ping
		stationMetricsService.tryRecordOutboundCq(text, System.currentTimeMillis());

		// Scoring should react quickly to outbound actions
		if (scoreService != null) {
			scoreService.requestRecompute("outbound-cq");
		}
	}



	/**
     *
     * @param remoteChatMember with callsign of the foreign station
     */

    public void airScout_SendAsShowPathPacket(ChatMember remoteChatMember) {

		DatagramSocket dsocket;

		String prefix_asSetpath ="ASSHOWPATH: \""+ this.getChatPreferences().getAirScout_asClientNameString()+ "\" \"" + this.getChatPreferences().getAirScout_asServerNameString() + "\" ";

//		String prefix_asSetpath ="ASSHOWPATH: \"KST\" \"AS\" "; Old hard coded
		String bandString = "1440000";
//        String myCallAndMyLocString = chatPreferences.getStn_loginCallSign() + "," + chatPreferences.getStn_loginLocatorMainCat(); // original b4 bugfix 1266
		String remoteCallAndLocString = remoteChatMember.getCallSign() +"," + remoteChatMember.getQra();

        String ownCallSign="";
        try {
            if (chatPreferences.getStn_loginCallSign().contains("-")) {
                ownCallSign = chatPreferences.getStn_loginCallSign().split("-")[0];
            } else {
                ownCallSign = chatPreferences.getStn_loginCallSign();
            }
        } catch (Exception e) {
            System.out.println("[ASPERIODICAL, Error]: " + e.getMessage());
        }

		String myCallAndMyLocString = ownCallSign + "," + chatPreferences.getStn_loginLocatorMainCat(); // original b4 bugfix 1266

		String host = "255.255.255.255";
//		int port = 9872;
		int port = chatPreferences.getAirScout_asCommunicationPort();
//		System.out.println("<<<<<<<<<<<<<<<<<<<<ASPERI: " + port);

//		byte[] message = "ASSETPATH: \"KST\" \"AS\" 1440000,DO5AMF,JN49GL,OK1MZM,JN89IW ".getBytes(); Original, ging
		InetAddress address;

			String queryStringToAirScout = "";

			queryStringToAirScout += prefix_asSetpath + bandString + "," + myCallAndMyLocString + "," + remoteCallAndLocString+ "Å";

			byte[] queryStringToAirScoutMSG = queryStringToAirScout.getBytes();

			try {
				address = InetAddress.getByName("255.255.255.255");
				DatagramPacket packet = new DatagramPacket(queryStringToAirScoutMSG, queryStringToAirScoutMSG.length, address, port);
				dsocket = new DatagramSocket();
				dsocket.setBroadcast(true);
				dsocket.send(packet);
				dsocket.close();
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (NoRouteToHostException e) {
				e.printStackTrace();
			}
			catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


	}

	/**
	 * starts the calculation scheduler for scores / priorities of skeds to be made
	 */
	private void startScoreScheduler() {

		if (scoreScheduler != null && !scoreScheduler.isShutdown()) return;

		scoreScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r);
			t.setName("ScoreServiceScheduler");
			t.setDaemon(true);
			return t;
		});

		scoreScheduler.scheduleAtFixedRate(() -> {
			try {
				scoreService.tick();
			} catch (Exception e) {
				System.err.println("[ChatController] CRITICAL ERROR in ScoreService tick:");
				e.printStackTrace();
			}
		}, 1, 3, TimeUnit.SECONDS);

		scoreService.requestRecompute("startup");
		System.out.println("[ChatController] ScoreService scheduler started.");
	}

	private void stopScoreScheduler() {
		if (scoreScheduler != null) scoreScheduler.shutdownNow();
		scoreScheduler = null;
	}


	/**
	 * Handles the disconnect of either the chat (Case DISCONNECTONLY) or the
	 * complete application life including all threads (case CLOSEALL)<br/><br/>
	 * Look in ApplicationConstants for the DISCSTRINGS
	 * 
	 * @param action: "CLOSEALL" or "DISCONNECTONLYCHAT", on application close event
	 *                (Settings Window closed), Disconnect on Disconnect-Button
	 *                clicked (new connection may follow)
	 */
	public void disconnect(String action) {

//		stopContextLoop(); //stops thread for calculating sked priorities

		stopScoreScheduler();

		this.dxClusterServer.stop();

		this.setDisconnectionPerformedByUser(true);

		try {
			/**
			 * Kill UCX and Wintest packetreader by sending poison pill to the reader threads
			 */
			DatagramSocket dsocket;

			String host = "255.255.255.255";
			int port = chatPreferences.getLogsynch_ucxUDPWkdCallListenerPort();
			InetAddress address;

			address = InetAddress.getByName("255.255.255.255");
			DatagramPacket packet = new DatagramPacket(ApplicationConstants.DISCONNECT_RDR_POISONPILL.getBytes(), ApplicationConstants.DISCONNECT_RDR_POISONPILL.length(), address, port);
            DatagramPacket killWintestReaderPacket = new DatagramPacket(ApplicationConstants.DISCONNECT_RDR_POISONPILL.getBytes(), ApplicationConstants.DISCONNECT_RDR_POISONPILL.length(), address, chatPreferences.getLogsynch_wintestNetworkPort());

            dsocket = new DatagramSocket();
			dsocket.setBroadcast(true);
			dsocket.send(packet);
			dsocket.close();

            dsocket = new DatagramSocket();
            dsocket.setBroadcast(true);
            dsocket.send(killWintestReaderPacket);
            dsocket.close();

			readUDPbyUCXThread.interrupt();
			stopWintestUdpListener();


		} catch (Exception error) {
			System.out.println("Chatcrontroller, ERROR: unable to send poison pill to ucxThread");
		}

		try {
			/**
			 * Kill AS packetreader by sending poison pill to the reader thread
			 */
			DatagramSocket dsocket;

			String host = "255.255.255.255";
			int port = chatPreferences.getAirScout_asCommunicationPort();
			InetAddress address;

			address = InetAddress.getByName("255.255.255.255");
			DatagramPacket packet = new DatagramPacket(ApplicationConstants.DISCONNECT_RDR_POISONPILL.getBytes(), ApplicationConstants.DISCONNECT_RDR_POISONPILL.length(), address, port);
			dsocket = new DatagramSocket();
			dsocket.setBroadcast(true);
			dsocket.send(packet);
			dsocket.close();
		} catch (Exception error) {
			System.out.println("Chatcrontroller, ERROR: unable to send poison pill to ucxThread");
		}


		if (action.equals(ApplicationConstants.DISCSTRING_DISCONNECT_AND_CLOSE)) {

//            rotatorClient.

			this.lst_chatMemberList.clear();;
			this.lst_clusterMemberList.clear();

			this.setDisconnected(true);
			this.setConnectedAndLoggedIn(false);
			this.setConnectedAndNOTLoggedIn(false);
			// disconnect telnet and kill all sockets and connections
			
			keepAliveTimer.cancel();
			keepAliveTimer.purge();
			
			ChatMessage killThreadPoisonPillMsg = new ChatMessage();
			killThreadPoisonPillMsg.setMessageText(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
			killThreadPoisonPillMsg.setMessageSenderName(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
			
			messageRXBus.clear();
			messageTXBus.clear();
			messageRXBus.add(killThreadPoisonPillMsg); //kills messageprocessor
			messageTXBus.add(killThreadPoisonPillMsg); //kills writethread

//			writeThread.interrupt();
//			readThread.interrupt();

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
			stopWintestUdpListener();


			
			airScoutUDPReaderThread.interrupt();
			
			dbHandler.closeDBConnection();

			dxClusterServer.stop();

            rotatorClient.stopRotor();
            rotatorClient.stop();

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
		} else if (action.equals(ApplicationConstants.DISCSTRING_DISCONNECTONLY)){

			this.lst_chatMemberList.clear();;
			this.lst_clusterMemberList.clear();


			this.setDisconnected(true);
			this.setConnectedAndLoggedIn(false);
			this.setConnectedAndNOTLoggedIn(false);
			// disconnect telnet and kill all sockets and connections

			keepAliveTimer.cancel();
			keepAliveTimer.purge();

			ChatMessage killThreadPoisonPillMsg = new ChatMessage();
			killThreadPoisonPillMsg.setMessageText(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
			killThreadPoisonPillMsg.setMessageSenderName(ApplicationConstants.DISCONNECT_RDR_POISONPILL);

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

//			consoleReader.interrupt();
//			messageProcessor.interrupt();

			readUDPbyUCXThread.interrupt(); //need poisonpill?
			stopWintestUdpListener();

			airScoutUDPReaderThread.interrupt(); //need poisonpill?

//			dbHandler.closeDBConnection();
//			this.dbHandler = null;


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

		}

	}

//	private ObservableList<ContestSked> activeSkeds = FXCollections.observableArrayList();
//	public ObservableList<ContestSked> getActiveSkeds() {
//		return activeSkeds;
//	}

	// SIGNAL: Ein Property, das wir hochzählen, um der GUI zu sagen "Daten haben sich geändert"
//	private LongProperty uiRefreshSignal = new SimpleLongProperty(0);
//	public LongProperty uiRefreshSignalProperty() {
//		return uiRefreshSignal;
//	}

//	public void addSked(ContestSked sked) {
//		Platform.runLater(() -> {
//			this.activeSkeds.add(sked);
//			runContextLoopCycle(); // Trigger sofort
//		});
//	}

//	private PriorityCalculator priorityCalculator = new PriorityCalculator();
//	private ScheduledExecutorService contextLoopService;


	private ObservableList<ContestSked> activeSkeds =
			FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

	public ObservableList<ContestSked> getActiveSkeds() {
		return activeSkeds;
	}

	/**
	 * Priority score pipeline (replaces the former 1-second ContextLoop).
	 */
	private final Map<String, ChatCategory> lastInboundCategoryByCallSignRaw =
			new java.util.concurrent.ConcurrentHashMap<>();

	/** Tracks the last time WE sent a message containing a QRG to a specific callsign (UPPERCASE).
	 *  Compared against knownActiveBands.timestampEpoch to decide whose QRG to use in a SKED. */
	private final Map<String, Long> lastSentQRGToCallsign =
			new java.util.concurrent.ConcurrentHashMap<>();

	/** Call this whenever we send a PM to {@code receiverCallsign} that contains our QRG. */
	public void recordOutboundQRG(String receiverCallsign) {
		if (receiverCallsign == null) return;
		lastSentQRGToCallsign.put(receiverCallsign.trim().toUpperCase(), System.currentTimeMillis());
		System.out.println("[ChatController] Recorded outbound QRG to: " + receiverCallsign);
	}

	/** Returns epoch-ms of when we last sent our QRG to this callsign, or 0 if never. */
	public long getLastSentQRGTimestamp(String callsign) {
		if (callsign == null) return 0L;
		return lastSentQRGToCallsign.getOrDefault(callsign.trim().toUpperCase(), 0L);
	}

	private final ScoreService scoreService = new ScoreService(this, new PriorityCalculator(), 15);
	private ScheduledExecutorService scoreScheduler;
	private final StationMetricsService stationMetricsService = new StationMetricsService();
	private final SkedReminderService skedReminderService = new SkedReminderService(this);


	public ScoreService getScoreService() {
		return scoreService;
	}

	public void addSked(ContestSked sked) {
		Platform.runLater(() -> {
			this.activeSkeds.add(sked);
			scoreService.requestRecompute("sked-added");
		});

		// Push sked to Win-Test via UDP if enabled
		if (chatPreferences.isLogsynch_wintestNetworkListenerEnabled()) {
			pushSkedToWinTest(sked);
		}
	}

	/**
	 * Pushes a sked to Win-Test via UDP broadcast (LOCKSKED / ADDSKED / UNLOCKSKED).
	 * Runs on a background thread to avoid blocking the UI.
	 */
	private void pushSkedToWinTest(ContestSked sked) {
		new Thread(() -> {
			try {
				InetAddress broadcastAddr = InetAddress.getByName(
						chatPreferences.getLogsynch_wintestNetworkBroadcastAddress());
				int port = chatPreferences.getLogsynch_wintestNetworkPort();
				String stationName = chatPreferences.getLogsynch_wintestNetworkStationNameOfKST();

				WinTestSkedSender sender = new WinTestSkedSender(stationName, broadcastAddr, port, this);

				// Frequency resolution:
				// Compare WHO sent a QRG most recently in the PM conversation:
				//   - OM sent their QRG last  → use OM's Last Known QRG (ChatMember.frequency)
				//   - WE sent our QRG last    → use our own Win-Test QRG (MYQRG)
				// Fallback chain if no timestamps exist: OM's Last Known QRG → hardcoded default
				double freqKHz = -1.0;
				final long SKED_FREQ_MAX_AGE_MS = 60 * 60 * 1000L; // 60 minutes

				ChatMember targetMember = resolveSkedTargetMember(sked.getTargetCallsign());

				// Collect timestamps: when did the OM last mention their QRG? When did WE last send ours?
				long omLastQRGTimestamp = 0L;
				double omLastQRGMhz = 0.0;
				if (targetMember != null && sked.getBand() != null) {
					ChatMember.ActiveFrequencyInfo fi = targetMember.getKnownActiveBands().get(sked.getBand());
					if (fi != null && fi.frequency > 0
							&& (System.currentTimeMillis() - fi.timestampEpoch) <= SKED_FREQ_MAX_AGE_MS) {
						omLastQRGTimestamp = fi.timestampEpoch;
						omLastQRGMhz = fi.frequency;
					}
				}
				long ourLastQRGTimestamp = getLastSentQRGTimestamp(sked.getTargetCallsign());

				// Decision: who was more recent?
				if (omLastQRGTimestamp > 0 && omLastQRGTimestamp >= ourLastQRGTimestamp) {
					// OM mentioned their QRG MORE RECENTLY (or at same time) → use their QRG
					freqKHz = omLastQRGMhz * 1000.0;
					System.out.println("[ChatController] SKED freq: OM sent last → "
							+ omLastQRGMhz + " MHz → " + freqKHz + " kHz");

				} else if (ourLastQRGTimestamp > 0) {
					// WE sent our QRG more recently → use our Win-Test QRG
					try {
						String qrgStr = chatPreferences.getMYQRGFirstCat().get();
						if (qrgStr != null && !qrgStr.isBlank()) {
							String cleaned = qrgStr.trim().replace(".", "");
							double parsed = Double.parseDouble(cleaned) / 100.0;
							if (parsed > 50000) {
								freqKHz = parsed;
								System.out.println("[ChatController] SKED freq: WE sent last → "
										+ freqKHz + " kHz (raw: " + qrgStr + ")");
							}
						}
					} catch (NumberFormatException ignored) { }
				}

				// Fallback A: OM's Last Known QRG from KST field (if no PM QRG exchange found at all)
				if (freqKHz < 0 && targetMember != null) {
					try {
						String memberQrg = targetMember.getFrequency().get();
						if (memberQrg != null && !memberQrg.isBlank()) {
							double mhz = Double.parseDouble(memberQrg.trim());
							freqKHz = mhz * 1000.0;
							System.out.println("[ChatController] SKED freq: fallback Last Known QRG → "
									+ mhz + " MHz → " + freqKHz + " kHz");
						}
					} catch (NumberFormatException ignored) { }
				}

				// Fallback B: hardcoded default
				if (freqKHz < 0) {
					freqKHz = 144300.0;
				}

				// Build notes string with target locator/azimuth info like reference: [JO02OB - 279°]
				String targetLocator = resolveSkedTargetLocator(sked.getTargetCallsign());
				String notes = "sked via KST4Contest";
				if (targetLocator != null && !targetLocator.isBlank() && sked.getTargetAzimuth() > 0) {
					notes = String.format("[%s - %.0f°] %s", targetLocator, sked.getTargetAzimuth(), notes);
				} else if (targetLocator != null && !targetLocator.isBlank()) {
					notes = String.format("[%s] %s", targetLocator, notes);
				} else if (sked.getTargetAzimuth() > 0) {
					notes = String.format("[%.0f°] %s", sked.getTargetAzimuth(), notes);
				}

				// Determine mode: -1 = auto-detect, 0 = CW, 1 = SSB
				String modeStr = chatPreferences.getLogsynch_wintestSkedMode();
				int modeOverride = -1; // AUTO
				if ("CW".equalsIgnoreCase(modeStr)) modeOverride = 0;
				else if ("SSB".equalsIgnoreCase(modeStr)) modeOverride = 1;

				sender.pushSkedToWinTest(sked, freqKHz, notes, modeOverride);
			} catch (Exception e) {
				System.out.println("[ChatController] Error pushing sked to Win-Test: " + e.getMessage());
				e.printStackTrace();
			}
		}, "WinTestSkedPush").start();
	}

	private ChatMember resolveSkedTargetMember(String targetCallsignRaw) {
		if (targetCallsignRaw == null || targetCallsignRaw.isBlank()) {
			return null;
		}
		String normalizedTargetCall = normalizeCallRaw(targetCallsignRaw);
		synchronized (getLst_chatMemberList()) {
			for (ChatMember member : getLst_chatMemberList()) {
				if (member == null || member.getCallSignRaw() == null) continue;
				if (normalizeCallRaw(member.getCallSignRaw()).equals(normalizedTargetCall)) {
					return member;
				}
			}
		}
		return null;
	}

	private String resolveSkedTargetLocator(String targetCallsignRaw) {
		if (targetCallsignRaw == null || targetCallsignRaw.isBlank()) {
			return null;
		}

		String normalizedTargetCall = normalizeCallRaw(targetCallsignRaw);
		synchronized (getLst_chatMemberList()) {
			for (ChatMember member : getLst_chatMemberList()) {
				if (member == null || member.getCallSignRaw() == null) continue;
				if (!normalizeCallRaw(member.getCallSignRaw()).equals(normalizedTargetCall)) continue;

				String locator = member.getQra();
				if (locator != null && !locator.isBlank()) {
					return locator.trim().toUpperCase(Locale.ROOT);
				}
			}
		}

		return null;
	}

	public StationMetricsService getStationMetricsService() {
		return stationMetricsService;
	}

	public SkedReminderService getSkedReminderService() {
		return skedReminderService;
	}

	/**
	 * saves the last recognized chat category for a chatmember, for example when we seen a message
	 * @param callSignRaw
	 * @param category
	 */
	public void rememberLastInboundCategory(String callSignRaw, ChatCategory category) {
		if (callSignRaw == null || category == null) return;
		lastInboundCategoryByCallSignRaw.put(callSignRaw.trim().toUpperCase(), category);
	}

	public Map<String, ChatCategory> snapshotLastInboundCategoryMap() {
		return new HashMap<>(lastInboundCategoryByCallSignRaw);
	}

	public List<ChatMember> snapshotChatMembers() {
		synchronized (getLst_chatMemberList()) {
			return new ArrayList<>(getLst_chatMemberList());
		}
	}

	public List<ContestSked> snapshotActiveSkeds() {
		synchronized (activeSkeds) {
			return new ArrayList<>(activeSkeds);
		}
	}

	public void requestRemoveExpiredSkeds(long nowEpochMs) {
		Platform.runLater(() -> {
			synchronized (activeSkeds) {
				activeSkeds.removeIf(sked -> (nowEpochMs - sked.getSkedTimeEpoch()) > 300_000);
			}
		});
	}

	private String userName;
	private String password;
	private String showedName;
	private String qra;

	private String chatState;

//	private String hostname = "109.90.0.130";
    private String hostname;
//	private String praktiKSTVersion = "praktiKST 1.0";
	private String praktiKSTVersionInfo = "2022-10 - 2022-12\ndeveloped by DO5AMF, Marc\nContact: praktimarc@gmail.com\nDonations via paypal are welcome";

	private int port = 23001; // kst4contest.test 4 23001 //TODO: auslagern in Chatprefs
	private ReadUDPbyUCXMessageThread readUDPbyUCXThread;
    private ReadUDPByWintestThread readUDPByWintestThread;
	private WriteThread writeThread;
	private ReadThread readThread;
	private InputReaderThread consoleReader;
	private ChatMember ownChatMemberObject; // Todo: set at startup
	private ChatController chatController;
	private MessageBusManagementThread messageProcessor;
	private ReadUDPbyAirScoutMessageThread airScoutUDPReaderThread;
	private DXClusterThreadPooledServer dxClusterServer;

	private PlayAudioUtils playAudioUtils = new PlayAudioUtils();

	public PlayAudioUtils getPlayAudioUtils() {
		return playAudioUtils;
	}


	private TimerTask userActualizationTask;

	private TimerTask keepAliveMessageSenderTask;

	private LinkedBlockingQueue<ChatMessage> messageRXBus; // Queue in which all Chatmessages are buffered, sources are
															// read- and write-thread
	private LinkedBlockingQueue<ChatMessage> messageTXBus; // Queue in which all Chatmessages are buffered, sources are
															// read- and write-thread
	private String observedSendThisMessageString;

	private DBController dbHandler;

	private Socket socket;
	private ServerSocket cluster_telnetServerSocket; // socket that accepts telnet client connects (cluster client)
//	private ServerSocketChannel cluster_telnetServerSocketChannel;


	private Timer userActualizationtimer;

	private Timer keepAliveTimer;

	private Timer beaconTimer;

	private Timer ASQueryTimer;

	private Timer socketCheckTimer;

	// ******All abstract types below here are used by the messageprocessor!
	// ***************

	private ObservableList<ChatMessage> lst_globalChatMessageList = FXCollections.observableArrayList(); //All chatmessages will be put in there, later create filtered message lists
//	private ObservableList<ChatMessage> lst_toAllMessageList = FXCollections.observableArrayList(); // directed to all
																									// (beacon)
	private FilteredList<ChatMessage> lst_toAllMessageList = new FilteredList<>(lst_globalChatMessageList); // directed to all

//	private ObservableList<ChatMessage> lst_toMeMessageList = FXCollections.observableArrayList(); // directed to my
																									// call
	private FilteredList<ChatMessage> lst_toMeMessageList = new FilteredList<>(lst_globalChatMessageList);

	private FilteredList<ChatMessage> lst_selectedCallSignInfofilteredMessageList = new FilteredList<>(lst_globalChatMessageList); // directed to all

//	private ObservableList<ChatMessage> lst_toOtherMessageList = FXCollections.observableArrayList(); // directed to a
																										// call but not
																										// mine
	private FilteredList<ChatMessage> lst_toOtherMessageList = new FilteredList<>(lst_globalChatMessageList);

    private ObservableList<String> lstNotify_QSOSniffer_sniffedCallSignList = FXCollections.observableArrayList();
	/**
	 * we do some trick here with the chatmemberlist to not make it neccessary to change all boolean properties if the
	 * chatmember object to observables. We trigger the list for changes on an object which we change whenever a list
	 * update will be neccessary to process (important for correct lifetime filtering!)
	 */
//	private ObservableList<ChatMember> chatMemberList = FXCollections.observableArrayList(workedInfoChange -> new Observable[] {workedInfoChange.workedInfoChangeFireListEventTriggerProperty()}); // List of active stations
																								// in chat
	private ObservableList<ChatMember> chatMemberList = FXCollections.observableArrayList(); // List of active stations


	private ObservableList<ChatMember> lst_chatMemberList = FXCollections.synchronizedObservableList(chatMemberList); // List
																														// of active stn in chat
	private FilteredList<ChatMember> lst_chatMemberListFiltered = new FilteredList<ChatMember>(chatMemberList);
	private SortedList<ChatMember> lst_chatMemberSortedFilteredList = new SortedList<ChatMember>(lst_chatMemberListFiltered);
	private ObservableList<Predicate<ChatMember>> lst_chatMemberListFilterPredicates = FXCollections.observableArrayList();
	private ObservableList<ClusterMessage> lst_clusterMemberList = FXCollections.observableArrayList();

	private ObservableList<ChatMember> lst_DBBasedWkdCallSignList = FXCollections.observableArrayList();

//	private HashMap<String, ChatMember> map_ucxLogInfoWorkedCalls = new HashMap<String, ChatMember>(); //Destination of ucx-log worked-messages

	// ******************************************************************************************************************************************





	/**
	 * checks if the callsign-String of a given chatmember instance and a given list
	 * instance is in the list (multiple entries are possible to find by this method! <br/>
	 * If yes, returns an Array of int with the list indexes <b>if not, returns empty array</b>
	 * <br/>
	 * <br/>Also gives back indexes for callsign-70 or callsign-2 etc.<br/>
	 *
	 * @param lookForThis
	 * @return int[]
	 */
	public ArrayList<Integer> checkListForChatMemberIndexesByCallSign(ChatMember lookForThis) {

		ArrayList<Integer> resultingIndexes = new ArrayList<Integer>();

		if (lookForThis == null) {

//			System.out.println("[ChatCtrl] ERROR: null Value for Chatmember detected! Member cannot be in the list!");
			return resultingIndexes;

		} else if (lookForThis.getCallSignRaw() == null) {
			System.out.println("[ChatCtrl] ERROR: null Value in Callsign detected! Member cannot be in the list!");
			return resultingIndexes;
		}

		for (Iterator iterator = lst_chatMemberList.iterator(); iterator.hasNext();) {
			ChatMember chatMember = (ChatMember) iterator.next();
			if (chatMember.getCallSignRaw().equals(lookForThis.getCallSignRaw())) { //Change for stations with -2 or -70 in logincallsign
//				System.out
//						.println("chtctrlr: Found raw " + chatMember.getCallSignRaw() + " //  " + lookForThis.getCallSign());

				resultingIndexes.add(lst_chatMemberList.indexOf(chatMember));

			} else {

			}
		}
		return resultingIndexes;

	}


    public void fireUserListUpdate(String reason) {
        if (statusListener != null) {
            // Da UI Updates im JavaFX Thread passieren müssen, hier oder im Listener Platform.runLater nutzen
            statusListener.onUserListUpdated(reason);
        }
    }

//	/**
//	 * checks if the callsign-String of a given chatmember instance and a given list
//	 * instance is in the list. If yes, returns the index in the List, <b>if not,
//	 * returns -1.</b>
//	 *
//	 * @param lookForThis
//	 * @return Integer (index), -1 for not found
//	 */
//	public int checkListForChatMemberIndexByCallSign(ChatMember lookForThis) {
//
//		if (lookForThis == null) {
//
////			System.out.println("[ChatCtrl] ERROR: null Value for Chatmember detected! Member cannot be in the list!");
//			return -1;
//		} else if (lookForThis.getCallSign() == null) {
//			System.out.println("[ChatCtrl] ERROR: null Value in Callsign detected! Member cannot be in the list!");
//			return -1;
//		}
//
//		for (Iterator iterator = lst_chatMemberList.iterator(); iterator.hasNext();) {
//			ChatMember chatMember = (ChatMember) iterator.next();
////			if (chatMember.getCallSign().equals(lookForThis.getCallSign())) {
//			if (chatMember.getCallSignRaw().equals(lookForThis.getCallSignRaw())) { //TODO: Change for stations with -2 or -70 in logincallsign
////				System.out
////						.println("chtctrlr: Found raw " + chatMember.getCallSignRaw() + " //  " + lookForThis.getCallSign());
//
//				return lst_chatMemberList.indexOf(chatMember);
//			} else {
//
//			}
//		}
		/**
		 * At this point we know, the callsign is not active in the chat.
		 */
//		System.out
//				.println("[ChatCtrl, ERROR:] ChecklistForChatMemberIndexByCallsign: " + lookForThis.getCallSign() + "\n" + "List: ");
//		for (Iterator iterator = lst_chatMemberList.iterator(); iterator.hasNext();) {
//			ChatMember chatMember = (ChatMember) iterator.next();
//			System.out.println(lst_chatMemberList.indexOf(lookForThis) + ": " + chatMember.getCallSign());
//		}

//		return -1;

//	}

	public FilteredList<ChatMessage> getLst_selectedCallSignInfofilteredMessageList() {
		return lst_selectedCallSignInfofilteredMessageList;
	}

	public void setLst_selectedCallSignInfofilteredMessageList(FilteredList<ChatMessage> lst_selectedCallSignInfofilteredMessageList) {
		this.lst_selectedCallSignInfofilteredMessageList = lst_selectedCallSignInfofilteredMessageList;
	}

	public ObservableList<ChatMessage> getLst_globalChatMessageList() {
		return lst_globalChatMessageList;
	}

	public void setLst_globalChatMessageList(ObservableList<ChatMessage> lst_globalChatMessageList) {
		this.lst_globalChatMessageList = lst_globalChatMessageList;
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

//	public String getPraktiKSTVersion() {
//		return praktiKSTVersion;
//	}

//	public void setPraktiKSTVersion(String praktiKSTVersion) {
//		this.praktiKSTVersion = praktiKSTVersion;
//	}

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

	public FilteredList<ChatMember> getLst_chatMemberListFiltered() {
		return lst_chatMemberListFiltered;
	}

	public SortedList<ChatMember> getLst_chatMemberSortedFilteredList() {
		return lst_chatMemberSortedFilteredList;
	}

	public ObservableList<Predicate<ChatMember>> getLst_chatMemberListFilterPredicates() {
		return lst_chatMemberListFilterPredicates;
	}

	public void setLst_chatMemberListFilterPredicates(ObservableList<Predicate<ChatMember>> lst_chatMemberListFilterPredicates) {
		this.lst_chatMemberListFilterPredicates = lst_chatMemberListFilterPredicates;
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

//	public void setLst_toAllMessageList(ObservableList<ChatMessage> lst_toAllMessageList) {
//		this.lst_toAllMessageList = lst_toAllMessageList;
//	}
	public void setLst_toAllMessageList(FilteredList<ChatMessage> lst_toAllMessageList) {
	this.lst_toAllMessageList = lst_toAllMessageList;
	}

	public ObservableList<ChatMessage> getLst_toMeMessageList() {
		return lst_toMeMessageList;
	}

//	public void setLst_toMeMessageList(ObservableList<ChatMessage> lst_toMeMessageList) {
//		this.lst_toMeMessageList = lst_toMeMessageList;
//	}
	public void setLst_toMeMessageList(FilteredList<ChatMessage> lst_toMeMessageList) {
		this.lst_toMeMessageList = lst_toMeMessageList;
	}

	public ObservableList<ChatMessage> getLst_toOtherMessageList() {
		return lst_toOtherMessageList;
	}

//	public void setLst_toOtherMessageList(ObservableList<ChatMessage> lst_toOtherMessageList) {
//		this.lst_toOtherMessageList = lst_toOtherMessageList;
//	}

	public void setLst_toOtherMessageList(FilteredList<ChatMessage> lst_toOtherMessageList) {
		this.lst_toOtherMessageList = lst_toOtherMessageList;
	}

	public LinkedBlockingQueue<ChatMessage> getMessageTXBus() {
		return messageTXBus;
	}

	public ChatController() {

		super();
		chatCategoryMain = new ChatCategory(2);//Todo: selectable chatcategory, switched by user
		chatCategorySecondChat = new ChatCategory(3); //Todo: selectable chatcategory, switched by user

		ownChatMemberObject = new ChatMember();
		ownChatMemberObject.setCallSign(userName);
		ownChatMemberObject.setName(showedName);
		ownChatMemberObject.setQra(qra);

//		this.category = ChatCategory.VUHF;
		this.userName = ownChatMemberObject.getName();
//		this.hostname = "www.on4kst.org";
		this.port = port;
	}

	/**
	 * This constructor is used by the Main()-Class of the praktiKST javaFX-gui.
	 * 
	 *
	 * @param setOwnChatMemberObject
	 */
	public ChatController(ChatMember setOwnChatMemberObject,StatusUpdateListener listener) {
		super();

        chatPreferences = new ChatPreferences();
        chatPreferences.readPreferencesFromXmlFile();
//        this.statusListener = listener;

        String dnsFromPrefs = chatPreferences.getStn_on4kstServersDns();
        if (dnsFromPrefs != null && !dnsFromPrefs.isEmpty()) {
            this.hostname = dnsFromPrefs;
        } else {
            this.hostname = "109.90.0.130";
        }

		UpdateChecker checkForUpdates = new UpdateChecker(this);

		if (checkForUpdates.downloadLatestVersionInfoXML()) {
			updateInformation = checkForUpdates.parseUpdateXMLFile();
		};

        initLst_toMeMessageList();



		lst_toAllMessageList.setPredicate(new Predicate<ChatMessage>() {
			@Override
			public boolean test(ChatMessage chatMessage) {

				try {
				if (chatMessage.getReceiver().getCallSign().equals("ALL")) { //TODO: ALL have to be an application-constant
					return true;
				} else return false;

				}
				catch (Exception nullPointerExc) {
					nullPointerExc.printStackTrace();
					System.out.println("ChatController, ERROR: maybe the receiver was null, mostly like a cq message!");
					return true;
				}

			}
		});

		lst_toOtherMessageList.setPredicate(new Predicate<ChatMessage>() {
			@Override
			public boolean test(ChatMessage chatMessage) {
				try {
					if ((!chatMessage.getSender().getCallSign().equals(getChatPreferences().getStn_loginCallSign())) &&
							(!chatMessage.getReceiver().getCallSign().equals(getChatPreferences().getStn_loginCallSign())) && (!chatMessage.getReceiver().getCallSign().equals("ALL")) )
					//RX not own callsign and TX not own callsign and callsign is not "ALL" (that means, directed to public)
					{
						return true;
					} else return false;

				} catch (Exception nullPointerExc) {
//					nullPointerExc.printStackTrace();
					System.out.println("ChatController, <<<catched ERROR>>>: maybe the receiver was null!");
					return false;
				}
			}
		});

		dbHandler = new DBController();

//		chatPreferences = new ChatPreferences();
//		chatPreferences.readPreferencesFromXmlFile(); // set the praktikst Prefs by file or default if file is corrupted

		chatCategoryMain = chatPreferences.getLoginChatCategoryMain();
		chatCategorySecondChat = chatPreferences.getLoginChatCategorySecond();
		this.userName = chatPreferences.getStn_loginCallSign();
		this.password = chatPreferences.getStn_loginPassword();
//		category = setCategory;
		ownChatMemberObject = setOwnChatMemberObject;

//		this.userName = ownChatMemberObject.getName();
//		this.password = ownChatMemberObject.getPassword();
		this.hostname = this.getChatPreferences().getStn_on4kstServersDns(); //default: www.on4kst.org

    }

    private void initLst_toMeMessageList() {
//        ObservableList<String> sniffedList = chatPreferences.getLstNotify_QSOSniffer_sniffedCallSignList();

        Predicate<ChatMessage> chatFilterPredicate = chatMessage -> {
            // Sicherheits-Checks gegen NullPointer (statt try-catch)
            if (chatMessage == null || chatMessage.getSender() == null || chatMessage.getReceiver() == null) {
                return false;
            }

            String myCallSign = getChatPreferences().getStn_loginCallSign();
            String senderCall = chatMessage.getSender().getCallSign();
            String receiverCall = chatMessage.getReceiver().getCallSign();
            String msgText = chatMessage.getMessageText();

            // --- NEUE LOGIK: Sniffer Liste prüfen ---
            // Wenn Absender ODER Empfänger in der Beobachtungsliste stehen -> Anzeigen
            if ((lstNotify_QSOSniffer_sniffedCallSignList.contains(senderCall) ||
                    lstNotify_QSOSniffer_sniffedCallSignList.contains(receiverCall)) &&
                    (!receiverCall.equals(this.getChatPreferences().getStn_loginCallSignRaw()))) {

                msgText = ("Sniffed: " + "(" + senderCall + " > ") + receiverCall +") " + msgText;
                chatMessage.setMessageText(msgText);
                return true;
            }

            // --- BESTEHENDE LOGIK ---

            // 1. Nachrichten direkt an dich
            if (receiverCall.equals(myCallSign)) {
                return true;
            }

            // 2. Deine eigenen Nachrichten (außer an ALL)
            if (senderCall.equals(myCallSign) && !receiverCall.equals("ALL")) {
                return true;
            }

            // 3. Mentions im Text (jemand schreibt über dich)
            // Nur prüfen, wenn Text nicht null ist und du nicht selbst der Absender bist
            if (msgText != null && !senderCall.equals(myCallSign)) {
                // containsIgnoreCase Logik (etwas robuster als deine Variante)
                if (msgText.toLowerCase().contains(myCallSign.toLowerCase())) {
                    return true;
                }
            }

            return false;
        };

        lstNotify_QSOSniffer_sniffedCallSignList.addListener((ListChangeListener<String>) c -> {

//            System.out.println(c.toString());

            // Wir zwingen die FilteredList zum Neuscannen, indem wir das Prädikat neu setzen.
            lst_toMeMessageList.setPredicate(null); // kurz resetten (manchmal nötig in älteren JavaFX Versionen)
            lst_toMeMessageList.setPredicate(chatFilterPredicate);

        });

        lstNotify_QSOSniffer_sniffedCallSignList.add("DF0GEB");


        lst_toMeMessageList.setPredicate(chatFilterPredicate); //sniffed callsign filter predicate is here!

//        lst_toMeMessageList.setPredicate(new Predicate<ChatMessage>() {
//            @Override
//            public boolean test(ChatMessage chatMessage) {
//
//                try {
//
//                    if (chatMessage.getReceiver().getCallSign().equals(getChatPreferences().getStn_loginCallSign())) {
//                        return true; //messages addressed to you
//                    }
//                    if ((chatMessage.getSender().getCallSign().equals(getChatPreferences().getStn_loginCallSign())) && (!chatMessage.getReceiver().getCallSign().equals("ALL"))){
//                        return true; //your own echo except texts to all (CQ)
//                    }
//
//                    String ignoreCaseString = chatMessage.getMessageText();
//
//                    if ((chatMessage.getMessageText().contains(chatPreferences.getStn_loginCallSign().toLowerCase()) || (chatMessage.getMessageText().contains(chatPreferences.getStn_loginCallSign().toUpperCase())))
//                            && (!chatMessage.getSender().getCallSign().equals(getChatPreferences().getStn_loginCallSign()))) {
//                        return true; //if someone writes about you, you will get the mail, too, except you are the sender...!
//                    }
//
//                    else {
//                        return false;
//                    }
//                }
//                catch (Exception nullPointerExc) {
//                    nullPointerExc.printStackTrace();
//                    System.out.println("ChatController, <<<catched ERROR>>>: maybe the receiver was null, message received b4 user entered chatmessage...!" + nullPointerExc.getMessage());
//                    return false;
//                }
//            }
//        });



    }

	/**
	 * starts wintest udp listener thread
	 */
	public synchronized void startWintestUdpListener() {
		if (readUDPByWintestThread != null && readUDPByWintestThread.isAlive()) {
			return;
		}

		readUDPByWintestThread = new ReadUDPByWintestThread(this, this);
		readUDPByWintestThread.setName("readUDPByWintestThread");
		readUDPByWintestThread.start();

		System.out.println("[ChatController] Win-Test UDP listener started.");
	}

	/**
	 * stops wintest udp listener thread
	 */
	public synchronized void stopWintestUdpListener() {
		if (readUDPByWintestThread == null) return;

		try {
			readUDPByWintestThread.interrupt();
		} catch (Exception ignored) { }

		readUDPByWintestThread = null;
		System.out.println("[ChatController] Win-Test UDP listener stopped.");
	}

	/**
	 * restarts wintest udp listener thread
	 */
	public synchronized void restartWintestUdpListenerIfEnabled() {
		stopWintestUdpListener();
		if (chatPreferences.isLogsynch_wintestNetworkListenerEnabled()) {
			startWintestUdpListener();
		}
	}



    public ObservableList<String> getLstNotify_QSOSniffer_sniffedCallSignList() {
        return lstNotify_QSOSniffer_sniffedCallSignList;
    }

    public void setLstNotify_QSOSniffer_sniffedCallSignList(ObservableList<String> lstNotify_QSOSniffer_sniffedCallSignList) {
        this.lstNotify_QSOSniffer_sniffedCallSignList = lstNotify_QSOSniffer_sniffedCallSignList;
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

	public ChatCategory getChatCategoryMain() {
		return chatCategoryMain;
	}

	public void setChatCategoryMain(ChatCategory chatCategoryMain) {
		this.chatCategoryMain = chatCategoryMain;
	}

	public DXClusterThreadPooledServer getDxClusterServer() {
		return dxClusterServer;
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

	/**
	 * execute is the main entry point where the application starts.
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void execute() throws InterruptedException, IOException {


		chatController = this;

//		ApplicationConstants constants = new ApplicationConstants();

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
			setDisconnectionPerformedByUser(false);

			startScoreScheduler();
			//runs sked priority thread

			messageRXBus = new LinkedBlockingQueue<ChatMessage>();
			messageTXBus = new LinkedBlockingQueue<ChatMessage>();

//			socket = new Socket(hostname, port);//socket for the on4kst chat server
            socket = new Socket(chatController.chatPreferences.getStn_on4kstServersDns(), port);//socket for the on4kst chat server
			System.out.println("Connected to the chat server: " + socket.isConnected());

			ByteBuffer buffer = ByteBuffer.allocate(1024);
			Selector selector = Selector.open();

//			consoleReader = new InputReaderThread(this);
//			consoleReader.start();

			readThread = new ReadThread(socket, this);
			readThread.setName("ReadThread-telnetreader");
			readThread.start();

			writeThread = new WriteThread(socket, this);
			writeThread.setName("Writethread-telnetwriter");
			writeThread.start();

			readUDPbyUCXThread = new ReadUDPbyUCXMessageThread(chatPreferences.getLogsynch_ucxUDPWkdCallListenerPort(), this, this);
			readUDPbyUCXThread.setName("readUDPbyUCXThread");
			readUDPbyUCXThread.start();

			if (chatPreferences.isLogsynch_wintestNetworkListenerEnabled()) {
				startWintestUdpListener();
			} else {
				System.out.println("[ChatController] Win-Test listener disabled by preference -> not starting.");
			}

			messageProcessor = new MessageBusManagementThread(this, this);
			messageProcessor.setName("messagebusManagementThread");
			messageProcessor.start();

			airScoutUDPReaderThread = new ReadUDPbyAirScoutMessageThread(chatPreferences.getAirScout_asCommunicationPort(), this, this.getChatPreferences().getAirScout_asServerNameString(), this.getChatPreferences().getAirScout_asServerNameString(), this); //working original
			airScoutUDPReaderThread.setName("airscoutudpreaderThread");
			airScoutUDPReaderThread.start();

			userActualizationtimer = new Timer();
			userActualizationtimer.schedule(new UserActualizationTask(this), 4000, 60000);// TODO: Temporary userlistoutput known qrgs

			keepAliveTimer = new Timer();
			keepAliveTimer.schedule(new keepAliveMessageSenderTask(this), 4000, 60000);//

			if (chatPreferences.isStn_pstRotatorEnabled()) {
				initRotor();
			} else {
				System.out.println("[ChatController, info]: PSTRotator disabled by user preference -> not starting rotator client.");
			}

			/**
			 * DX cluster service running config
			 */
			dxClusterServer = new DXClusterThreadPooledServer(this.getChatPreferences().getNotify_dxclusterServerPort(), this, this);
			new Thread(dxClusterServer).start();


			this.setConnectedAndLoggedIn(true);

			/**
			 * The CQ-beacon-Task will be executed every time but checks for itself whether
			 * CQ messages are enabled or not
			 */
//			Timer beaconTimer;
			beaconTimer = new Timer();
			beaconTimer.schedule(new BeaconTask(this, this), 10000,
					this.getChatPreferences().getBcn_beaconIntervalInMinutesMainCat() * 60000);
			// 60000 * intervalInMinutes = IntervalInMillis

			/**
			 * The AS querier task will be executed every time but checks for itself whether
			 * AS usage is enabled or not
			 */
//			Timer ASQueryTimer;
			ASQueryTimer = new Timer();
			ASQueryTimer.schedule(new AirScoutPeriodicalAPReflectionInquirerTask(this), 10000, 60000);
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

							System.out.println("[Chatcontroller, Warning: ] Socket closed or disconnected");
						
							ChatMessage killThreadPoisonPillMsg = new ChatMessage();
							killThreadPoisonPillMsg.setMessageText(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
							killThreadPoisonPillMsg.setMessageSenderName(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
							
							ChatMessage killThreadPoisonPillMsg2 = new ChatMessage();
							killThreadPoisonPillMsg2.setMessageText(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
							killThreadPoisonPillMsg2.setMessageSenderName(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
							
							
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
								
								messageProcessor = new MessageBusManagementThread(chatController, chatController);
								messageProcessor.start();

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

//		System.out.println(currentTimeInUtc.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm X")));

		long millisecondsSinceEpoch = currentTimeInUtc.toInstant().toEpochMilli() / 1000;
//	    System.out.println(millisecondsSinceEpoch);
		return millisecondsSinceEpoch;
	}

	/**
	 * Setting the initial parameters at the chat via port 23001 <br/>
	 * <br/>
	 * <b>Login parameter format is like that: </b><br/>
	 * LOGINC|do5amf|password|2|kst4contest1251|25|0|1|1663879818|0| <br/>
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

				//this is the original loginC without history abonnement
//				String loginString = "";
//				loginString = "LOGINC|" + chatPreferences.getStn_loginCallSign() + "|" + chatPreferences.getStn_loginPassword()
//						+ "|" + chatPreferences.getLoginChatCategoryMain().getCategoryNumber() + "|praktiKST v" + ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER
//						+ "|25|0|1|" + getCurrentEpochTime() + "|0|";

				String loginString = "";
				loginString = "LOGINC|" + chatPreferences.getStn_loginCallSign() + "|" + chatPreferences.getStn_loginPassword()
						+ "|" + chatPreferences.getLoginChatCategoryMain().getCategoryNumber() + "|praktiKST v" + ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER
						+ "|25|0|1|" + "0" + "|0|";

				// System.out.println(loginString);
				ChatMessage message = new ChatMessage();
				message.setMessageText(loginString);
				message.setMessageDirectedToServer(true);
				getMessageTXBus().add(message);

			}
		}, 2000);

		/**
		 * Entering second chat
		 *
		 * ACHAT|chat id|past messages number|past dx/map number|users list/update flags|last Unix timestamp for messages|last Unix timestamp for dx/map|
		 */

		if (this.chatController.getChatPreferences().isLoginToSecondChatEnabled()) { //only login to second if wished

			new Timer().schedule(new TimerTask() {

				@Override
				public void run() { //test second chat

					Thread.currentThread().setName("LoginStringTimerSecond");

					String loginString = "";
					loginString = "ACHAT|" + chatController.getChatPreferences().getLoginChatCategorySecond().getCategoryNumber() + "|" + "25"
							+ "|" + "10" + "|2|" + getCurrentEpochTime() + "|" + getCurrentEpochTime();

					// System.out.println(loginString);
					ChatMessage message = new ChatMessage();
					message.setMessageText(loginString);
					message.setMessageDirectedToServer(true);
					getMessageTXBus().add(message);

				}
			}, 5000);
		}
		/**
		 * end testing second chat
		 *
		 */

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {

				Thread.currentThread().setName("SDONEStringTimer");
				ChatMessage message = new ChatMessage();
				message.setMessageText("SDONE|" + chatPreferences.getLoginChatCategoryMain().getCategoryNumber() + "|\r");
				message.setMessageDirectedToServer(true);
				getMessageTXBus().add(message);

			}
		}, 3000);

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				Thread.currentThread().setName("SETLOCTIMER");
				ChatMessage message = new ChatMessage();
				message.setMessageText("MSG|" + chatPreferences.getLoginChatCategoryMain().getCategoryNumber()
						+ "|0|/SETLOC " + chatPreferences.getStn_loginLocatorMainCat() + "|0|\r");
				message.setMessageDirectedToServer(true);
				getMessageTXBus().add(message);
			}
		}, 4000);

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				Thread.currentThread().setName("SETNAMETIMER");
				ChatMessage message = new ChatMessage();
				message.setMessageText("MSG|" + chatPreferences.getLoginChatCategoryMain().getCategoryNumber()
						+ "|0|/SETNAME " + chatPreferences.getStn_loginNameMainCat() + "|0|\r");
				message.setMessageDirectedToServer(true);
				getMessageTXBus().add(message);
			}
		}, 5000);

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				Thread.currentThread().setName("SETHereTimerMain");
				ChatMessage message = new ChatMessage();
				message.setMessageText("MSG|" + chatPreferences.getLoginChatCategoryMain().getCategoryNumber()
						+ "|0|/BACK" + "|0|\r");
				message.setMessageDirectedToServer(true);
				getMessageTXBus().add(message);
			}
		}, 6500);


		if (chatPreferences.isLoginToSecondChatEnabled()) { //only if second category had been enabled

			new Timer().schedule(new TimerTask() {

				@Override
				public void run() {
					Thread.currentThread().setName("SETNAMETIMER2nd");
					ChatMessage message = new ChatMessage();
					message.setMessageText("MSG|" + chatPreferences.getLoginChatCategorySecond().getCategoryNumber()
							+ "|0|/SETNAME " + chatPreferences.getStn_loginNameSecondCat() + "|0|\r");
					message.setMessageDirectedToServer(true);
					getMessageTXBus().add(message);
				}
			}, 5500);

			new Timer().schedule(new TimerTask() {

				@Override
				public void run() {
					Thread.currentThread().setName("SETHereTimerSecond");
					ChatMessage message = new ChatMessage();
					message.setMessageText("MSG|" + chatPreferences.getLoginChatCategorySecond().getCategoryNumber()
							+ "|0|/BACK" + "|0|\r");
					message.setMessageDirectedToServer(true);
					getMessageTXBus().add(message);
				}
			}, 7000);

		}

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				Thread.currentThread().setName("fetchWorkedFromDBTimer");
				refreshWorkedStateAndDatabaseListFromDatabase();
			}
		}, 10000);

//		new Timer().schedule(new TimerTask() {
//			HashMap<String, ChatMember> getWorkedDataFromDb;
//
//			@Override
//			public void run() {
//
//				Thread.currentThread().setName("fetchWorkedFromDBTimer");
//
//				try {
//					getWorkedDataFromDb = dbHandler.fetchChatMemberWkdDataFromDB();
//				} catch (SQLException e) {
//					System.out.println("[Chatctrl, Error: ] got no worked data from DB due to communication error");
//				}
//
//				for (Iterator iterator = getLst_chatMemberList().iterator(); iterator.hasNext();) {
//
//
//					ChatMember chatMember = (ChatMember) iterator.next();
//					System.out.println("[Chatctrl]: Marking ChatMembers wkd information: "
//							+ getWorkedDataFromDb.get(chatMember.getCallSign()).getCallSign());
//					chatMember.setWorked(getWorkedDataFromDb.get(chatMember.getCallSign()).isWorked());
//					chatMember.setWorked144(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isWorked144());
//					;
//					chatMember.setWorked432(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isWorked432());
//					;
//					chatMember.setWorked1240(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isWorked1240());
//					;
//					chatMember.setWorked2300(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isWorked2300());
//					;
//					chatMember.setWorked3400(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isWorked3400());
//					;
//					chatMember.setWorked5600(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isWorked5600());
//					;
//					chatMember.setWorked10G(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isWorked10G());
//					/**
//					 * v1.2 since here
//					 * TODO: Change that, this ins not generative
//					 */
//
//					chatMember.setQrv144(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isQrv144());
//					;
//					chatMember.setQrv432(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isQrv432());
//					;
//					chatMember.setQrv1240(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isQrv1240());
//					;
//					chatMember.setQrv2300(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isQrv2300());
//					;
//					chatMember.setQrv3400(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isQrv3400());
//					;
//					chatMember.setQrv5600(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isQrv5600());
//					;
//					chatMember.setQrv10G(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isQrv10G());
//					;
//				}
//
//				/**
//				 *
//				 * This creates the list of the worked stations which had to be displayed in the
//				 * settings menu. TODO: May make this List editable
//				 *
//				 */
//
//				getWorkedDataFromDb.forEach((key, value) -> {
//
//					chatController.getLst_DBBasedWkdCallSignList().add(value);
//
////			        System.out.println("Key=" + key + ", Value=" + value);
//				});
//			}
//		}, 10000);

//		message = new ChatMessage();
//		message.setMessageText("MSG|2|0|/SETNAME " + ownChatMemberObject.getName() + "|0|\r");
//		message.setMessageDirectedToServer(true);
//		this.getMessageTXBus().add(message);

		// message.setMessageText(ownCall.getCallSign());
//		this.getMessageTXBus().add(message);

	}

	/**
	 * Reloads the worked/not-QRV state from the internal database and applies the
	 * result both to the active chatmember list and to the database table list in the
	 * settings dialog. UI-bound list modifications are executed on the JavaFX thread.
	 */
	public void refreshWorkedStateAndDatabaseListFromDatabase() {

		HashMap<String, ChatMember> workedDataFromDatabase;

		try {
			workedDataFromDatabase = dbHandler.fetchChatMemberWkdDataFromDB();
		} catch (SQLException e) {
			System.out.println("[Chatctrl, Error: ] got no worked data from DB due to communication error");
			e.printStackTrace();
			return;
		}

		HashMap<String, ChatMember> finalWorkedDataFromDatabase = workedDataFromDatabase;

		Platform.runLater(() -> {
			helper_applyWorkedAndQrvStateFromDatabase(finalWorkedDataFromDatabase);
			getLst_DBBasedWkdCallSignList().setAll(finalWorkedDataFromDatabase.values());
			fireUserListUpdate("Worked database state refreshed");
		});
	}

	/**
	 * Applies the worked and not-QRV state from the database snapshot to all active
	 * chatmember objects that are currently visible in the live chat list.
	 *
	 * @param workedDataFromDatabase map keyed by normalized raw callsign
	 */
	private void helper_applyWorkedAndQrvStateFromDatabase(HashMap<String, ChatMember> workedDataFromDatabase) {

		for (Iterator iterator = getLst_chatMemberList().iterator(); iterator.hasNext();) {

			ChatMember activeChatMember = (ChatMember) iterator.next();
			ChatMember storedChatMemberState = workedDataFromDatabase.get(activeChatMember.getCallSignRaw());

			if (storedChatMemberState == null) {
				continue;
			}

			activeChatMember.setWorked(storedChatMemberState.isWorked());
			activeChatMember.setWorked144(storedChatMemberState.isWorked144());
			activeChatMember.setWorked432(storedChatMemberState.isWorked432());
			activeChatMember.setWorked1240(storedChatMemberState.isWorked1240());
			activeChatMember.setWorked2300(storedChatMemberState.isWorked2300());
			activeChatMember.setWorked3400(storedChatMemberState.isWorked3400());
			activeChatMember.setWorked5600(storedChatMemberState.isWorked5600());
			activeChatMember.setWorked10G(storedChatMemberState.isWorked10G());
			activeChatMember.setQrv144(storedChatMemberState.isQrv144());
			activeChatMember.setQrv432(storedChatMemberState.isQrv432());
			activeChatMember.setQrv1240(storedChatMemberState.isQrv1240());
			activeChatMember.setQrv2300(storedChatMemberState.isQrv2300());
			activeChatMember.setQrv3400(storedChatMemberState.isQrv3400());
			activeChatMember.setQrv5600(storedChatMemberState.isQrv5600());
			activeChatMember.setQrv10G(storedChatMemberState.isQrv10G());
		}
	}

	/**
	 * Resets all worked flags in the live GUI chatmember list.
	 */
	public void resetWorkedInfoInGuiLists() {

		this.chatController.getLst_chatMemberList().forEach(
				chatMember -> chatMember.resetWorkedInformationAtAllBands());
	}

	/**
	 * Resets all not-QRV flags in the live GUI chatmember list.
	 */
	public void resetQRVInfoInGuiLists() {

		this.chatController.getLst_chatMemberList().forEach(
				chatMember -> chatMember.resetQRVInformationAtAllBands());
	}

	/**
	 * Resets both worked and not-QRV flags in the live GUI chatmember list.
	 */
	public void resetWorkedAndQrvInfoInGuiLists() {
		resetWorkedInfoInGuiLists();
		resetQRVInfoInGuiLists();
	}



	
//	public void resetWorkedInfoInGuiLists() {
//
//		this.chatController.getLst_chatMemberList().forEach(
//			chatMember -> chatMember.resetWorkedInformationAtAllBands());
//
//	}
//
//	public void resetQRVInfoInGuiLists() {
//
//		this.chatController.getLst_chatMemberList().forEach(
//				chatMember -> chatMember.resetQRVInformationAtAllBands());
//
//	}
	
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
		message.setMessageText(chatCategoryMain + "");
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

	public static final class UiReminderEvent {
		private final String callSignRaw;
		private final int minutesBefore;
		private final long epochMs;

		public UiReminderEvent(String callSignRaw, int minutesBefore, long epochMs) {
			this.callSignRaw = callSignRaw;
			this.minutesBefore = minutesBefore;
			this.epochMs = epochMs;
		}

		public String getCallSignRaw() { return callSignRaw; }
		public int getMinutesBefore() { return minutesBefore; }
		public long getEpochMs() { return epochMs; }
	}

	public void fireUiReminderEvent(String callSignRaw, int minutesBefore) {
		final String raw = callSignRaw == null ? null : callSignRaw.trim().toUpperCase();
		final long now = System.currentTimeMillis();

		// Ensure property updates happen on FX thread
		if (Platform.isFxApplicationThread()) {
			lastUiReminderEvent.set(new UiReminderEvent(raw, minutesBefore, now));
		} else {
			Platform.runLater(() -> lastUiReminderEvent.set(new UiReminderEvent(raw, minutesBefore, now)));
		}
	}


	private final ObjectProperty<UiReminderEvent> lastUiReminderEvent = new SimpleObjectProperty<>(null);

	public ReadOnlyObjectProperty<UiReminderEvent> lastUiReminderEventProperty() {
		return lastUiReminderEvent;
	}

	/**
	 * Helper method to check if a chatmember is in my beam(range)
	 *
	 * @param member
	 * @return
	 */
	public boolean isChatMemberInMyBeam(ChatMember member) {
		if (member == null || member.getQTFdirection() == null) return false;

		double targetAz = member.getQTFdirection();
		double myAz = getChatPreferences().getActualQTF().get();
		double beamWidth = getChatPreferences().getStn_antennaBeamWidthDeg();

		return DirectionUtils.isAngleInRange(targetAz, myAz, beamWidth);
	}
}