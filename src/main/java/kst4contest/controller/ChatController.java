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
import kst4contest.logic.BandOpportunityResolver;
import kst4contest.logic.PriorityCalculator;
import kst4contest.model.*;
import kst4contest.test.MockKstServer;
import kst4contest.utils.PlayAudioUtils;
import kst4contest.locatorUtils.Location;
import kst4contest.view.Kst4ContestApplication;

import java.io.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.nio.charset.StandardCharsets;




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


		public static final int MIN_BEACON_INTERVAL_MINUTES = 1;
	public static final int MAX_BEACON_TEXT_LENGTH = 120;
	private static final long INITIAL_BEACON_DELAY_MILLIS = 10_000L;

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
		rotatorClient = new PstRotatorClient(
				chatPreferences.getStn_pstRotatorHost(),
				chatPreferences.getStn_pstRotatorPort(),
				this,
				this
		);

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
	 * Called when an external logger reports that a QSO was logged.
	 *
	 * <p>The common resolver combines recent QRG evidence and station-name bands
	 * across every active callsignRaw variant. A manual NOT-QRV tag overrides this
	 * automatic evidence before the hint and priority boost are evaluated.</p>
	 */
	public void onExternalLogEntryReceived(String callSignRaw) {

		if (callSignRaw == null || callSignRaw.isBlank()) return;
		if (chatPreferences == null) return;
		if (!chatPreferences.isNotify_bandUpgradeHintOnLogEnabled()) return;

		final String callRaw = normalizeCallRaw(callSignRaw);

		if (DEBUG_BAND_UPGRADE_HINT) {
			System.out.println("[BandUpgradeHint] LOG received for call=" + callRaw);
		}

		EnumSet<Band> myEnabledBands =
				BandOpportunityResolver.getEnabledStationBands(chatPreferences);
		if (myEnabledBands.isEmpty()) return;

		List<ChatMember> variants = findActiveChatMembersByRawCall(callRaw);
		BandOpportunityResolver.Resolution bandResolution =
				BandOpportunityResolver.resolve(variants, System.currentTimeMillis());

		EnumSet<Band> stationOfferedBands = bandResolution.getOfferedBands();
		if (stationOfferedBands.isEmpty()) return;

		EnumSet<Band> workedBands = bandResolution.getWorkedBands();
		EnumSet<Band> remainingBands =
				bandResolution.getUnworkedEnabledBands(myEnabledBands);
		if (remainingBands.isEmpty()) return;

		if (DEBUG_BAND_UPGRADE_HINT) {
			System.out.println("[BandUpgradeHint] call=" + callRaw
					+ " enabled=" + formatBandsHuman(myEnabledBands)
					+ " offered=" + formatBandsHuman(stationOfferedBands)
					+ " worked=" + (workedBands.isEmpty() ? "-" : formatBandsHuman(workedBands))
					+ " NOT-QRV=" + formatBandsHuman(bandResolution.getNotQrvBands())
					+ " remaining=" + formatBandsHuman(remainingBands));
		}

		String remainingHuman = formatBandsHuman(remainingBands);
		String shortText = "BAND+ " + callRaw + " " + remainingHuman;

		String tooltip = "Logged " + callRaw
				+ ", but the station still offers additional band(s): "
				+ remainingHuman
				+ "\n(Enabled: " + formatBandsHuman(myEnabledBands)
				+ " | Worked: " + (workedBands.isEmpty() ? "-" : formatBandsHuman(workedBands))
				+ " | NOT QRV: " + formatBandsHuman(bandResolution.getNotQrvBands()) + ")";

		ThreadStateMessage msg = new ThreadStateMessage("BandUpgradeHint", true, tooltip, false);
		msg.setRunningInformationTextDescription(shortText);
		onThreadStatus("BandUpgradeHint", msg);

		if (chatPreferences.isNotify_playSimpleSounds()) {
			try {
				getPlayAudioUtils().playNoiseLauncher('!');
			} catch (Exception e) {
				System.out.println(
						"[ChatController, warning]: failed to play band-upgrade hint sound: "
								+ e.getMessage()
				);
			}
		}

		if (getScoreService() != null) {
			getScoreService().requestRecompute("BandUpgradeHint");
		}
	}

	/** Normalize callsign raw to a stable key for comparisons. */
	private static String normalizeCallRaw(String callRaw) {
		return callRaw.trim().toUpperCase(Locale.ROOT);
	}

	private static String formatBandsHuman(EnumSet<Band> bands) {
		if (bands == null || bands.isEmpty()) return "-";
		return bands.stream().map(ChatController::bandToHumanLabel).sorted().reduce((a, b) -> a + ", " + b).orElse("-");
	}

	private static String bandToHumanLabel(Band b) {
		if (b == null) return "?";
		return switch (b) {
			case B_50 -> "6m";
			case B_70 -> "4m";
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


	/**
	 * Chooses a useful initial band for a new sked.
	 *
	 * <p>Recent frequency evidence has priority over station-name information.
	 * Manual NOT-QRV exclusions are respected. The operator can still select
	 * another locally enabled band from the dropdown.</p>
	 */
	private Band resolveDefaultSkedBand(ChatMember selectedMember,
	                                    EnumSet<Band> enabledBands) {

		if (selectedMember == null || enabledBands == null || enabledBands.isEmpty()) {
			return null;
		}

		List<ChatMember> variants =
				chatcontroller.findActiveChatMembersByRawCall(
						selectedMember.getCallSignRaw()
				);

		if (variants.isEmpty()) {
			variants = List.of(selectedMember);
		}

		BandOpportunityResolver.Resolution resolution =
				BandOpportunityResolver.resolve(
						variants,
						System.currentTimeMillis()
				);

		EnumSet<Band> availableBands = resolution.getAvailableBands();
		availableBands.retainAll(enabledBands);

		Band newestFrequencyBand = null;
		long newestTimestamp = Long.MIN_VALUE;
		long now = System.currentTimeMillis();

		for (ChatMember member : variants) {
			if (member == null || member.getKnownActiveBands() == null) {
				continue;
			}

			for (Map.Entry<Band, ChatMember.ActiveFrequencyInfo> entry
					: member.getKnownActiveBands().entrySet()) {

				Band band = entry.getKey();
				ChatMember.ActiveFrequencyInfo info = entry.getValue();

				if (band == null
						|| info == null
						|| !availableBands.contains(band)
						|| !band.isPlausible(info.frequency)) {
					continue;
				}

				long ageMs = now - info.timestampEpoch;
				if (ageMs < 0L
						|| ageMs > BandOpportunityResolver.RECENT_DYNAMIC_EVIDENCE_MAX_AGE_MS) {
					continue;
				}

				if (info.timestampEpoch > newestTimestamp) {
					newestTimestamp = info.timestampEpoch;
					newestFrequencyBand = band;
				}
			}
		}

		if (newestFrequencyBand != null) {
			return newestFrequencyBand;
		}

		if (!availableBands.isEmpty()) {
			return availableBands.iterator().next();
		}

		return enabledBands.iterator().next();
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

	/**
	 * Requests AirScout to display the path between the own station and the
	 * selected remote station.
	 *
	 * The configured server identifier, client identifier, band and UDP port are
	 * used for every request. This allows several AirScout servers or clients to
	 * coexist in the same network.
	 *
	 * @param remoteChatMember selected remote station
	 */
	public void airScout_SendAsShowPathPacket(
			ChatMember remoteChatMember
	) {
		if (!chatPreferences.isAirScout_asUDPListenerEnabled()) {
			System.out.println(
					"[AirScout, info]: Show-path request ignored because "
							+ "the AirScout integration is disabled."
			);
			return;
		}

		if (remoteChatMember == null
				|| remoteChatMember.getCallSign() == null
				|| remoteChatMember.getCallSign().isBlank()
				|| remoteChatMember.getQra() == null
				|| remoteChatMember.getQra().isBlank()) {
			System.out.println(
					"[AirScout, warning]: Show-path request ignored because "
							+ "the selected station has no usable callsign or locator."
			);
			return;
		}

		String ownCallSign = chatPreferences.getStn_loginCallSign();
		if (ownCallSign == null || ownCallSign.isBlank()) {
			return;
		}

		int suffixSeparator = ownCallSign.indexOf("-");
		if (suffixSeparator > 0) {
			ownCallSign = ownCallSign.substring(0, suffixSeparator);
		}

		String ownLocator =
				chatPreferences.getStn_loginLocatorMainCat();

		if (ownLocator == null || ownLocator.isBlank()) {
			return;
		}

		String clientIdentifier =
				chatPreferences.getAirScout_asClientNameString();
		String serverIdentifier =
				chatPreferences.getAirScout_asServerNameString();
		String bandValue =
				chatPreferences.getAirScout_asBandString();
		int port =
				chatPreferences.getAirScout_asCommunicationPort();

		String query =
				"ASSHOWPATH: \""
						+ clientIdentifier
						+ "\" \""
						+ serverIdentifier
						+ "\" "
						+ bandValue
						+ ","
						+ ownCallSign
						+ ","
						+ ownLocator
						+ ","
						+ remoteChatMember.getCallSign()
						+ ","
						+ remoteChatMember.getQra()
						+ " ";

		byte[] payload = query.getBytes(StandardCharsets.UTF_8);

		try (
				DatagramSocket socket = new DatagramSocket()
		) {
			socket.setBroadcast(true);

			InetAddress address =
					InetAddress.getByName("255.255.255.255");

			DatagramPacket packet = new DatagramPacket(
					payload,
					payload.length,
					address,
					port
			);

			socket.send(packet);
		} catch (IOException exception) {
			System.out.println(
					"[AirScout, error]: Could not send show-path request: "
							+ exception.getMessage()
			);
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

		stopDxClusterServer();

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

//			this.lst_chatMemberList.clear();;
			this.clearActiveChatMembers();
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

			stopBeaconTimer();
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

			this.clearActiveChatMembers();
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
	 * Pushes a sked to Win-Test via UDP broadcast.
	 *
	 * <p>The internal sked is independent from this handover. If no frequency can
	 * be resolved safely for the selected band, the internal sked remains active,
	 * but no misleading Win-Test entry is created.</p>
	 */
	private void pushSkedToWinTest(ContestSked sked) {
		new Thread(() -> {
			try {
				Double frequencyKHz = resolveSkedFrequencyKHz(sked);

				if (frequencyKHz == null) {
					reportSkippedWinTestSked(
							sked,
							"no recent or configured QRG matches "
									+ sked.getBand().getDisplayLabel()
					);
					return;
				}

				String winTestCallsign =
						toWinTestSkedCallsign(
								sked.getTargetChatCallsign()
						);

				if (winTestCallsign == null || winTestCallsign.isBlank()) {
					reportSkippedWinTestSked(
							sked,
							"the target callsign could not be converted"
					);
					return;
				}

				InetAddress broadcastAddress = InetAddress.getByName(
						chatPreferences
								.getLogsynch_wintestNetworkBroadcastAddress()
				);

				int port =
						chatPreferences.getLogsynch_wintestNetworkPort();

				String stationName =
						chatPreferences
								.getLogsynch_wintestNetworkStationNameOfKST();

				WinTestSkedSender sender = new WinTestSkedSender(
						stationName,
						broadcastAddress,
						port,
						this
				);

				String targetLocator =
						resolveSkedTargetLocator(
								sked.getTargetCallsign()
						);

				String notes = "sked via KST4Contest";

				if (targetLocator != null
						&& !targetLocator.isBlank()
						&& sked.getTargetAzimuth() > 0) {

					notes = String.format(
							"[%s - %.0f°] %s",
							targetLocator,
							sked.getTargetAzimuth(),
							notes
					);

				} else if (targetLocator != null
						&& !targetLocator.isBlank()) {

					notes = String.format(
							"[%s] %s",
							targetLocator,
							notes
					);

				} else if (sked.getTargetAzimuth() > 0) {

					notes = String.format(
							"[%.0f°] %s",
							sked.getTargetAzimuth(),
							notes
					);
				}

				String modeText =
						chatPreferences.getLogsynch_wintestSkedMode();

				int modeOverride = -1;

				if ("CW".equalsIgnoreCase(modeText)) {
					modeOverride = 0;
				} else if ("SSB".equalsIgnoreCase(modeText)) {
					modeOverride = 1;
				}

				sender.pushSkedToWinTest(
						sked,
						winTestCallsign,
						frequencyKHz,
						notes,
						modeOverride
				);

			} catch (Exception exception) {
				String message =
						"Error pushing sked to Win-Test: "
								+ exception.getMessage();

				System.out.println(
						"[ChatController] " + message
				);

				onThreadStatus(
						"WT-SkedSend",
						new ThreadStateMessage(
								"WT-SkedSend",
								false,
								message,
								true
						)
				);

				exception.printStackTrace();
			}
		}, "WinTestSkedPush").start();
	}

	/**
	 * Resolves the frequency used for the Win-Test sked.
	 *
	 * <ol>
	 *     <li>Newest QRG detected for the target station on the selected band</li>
	 *     <li>Own QRG belonging to the target's chat category</li>
	 *     <li>No result: do not send the Win-Test sked</li>
	 * </ol>
	 */
	private Double resolveSkedFrequencyKHz(ContestSked sked) {
		if (sked == null || sked.getBand() == null) {
			return null;
		}

		Double targetFrequencyKHz =
				resolveRecentTargetFrequencyKHz(sked);

		if (targetFrequencyKHz != null) {
			System.out.println(
					"[ChatController] SKED frequency from target: "
							+ targetFrequencyKHz
							+ " kHz on "
							+ sked.getBand()
			);
			return targetFrequencyKHz;
		}

		String ownQrg =
				resolveOwnQrgForSkedCategory(
						sked.getTargetChatCategory()
				);

		Double ownFrequencyKHz =
				parseSkedFrequencyKHz(
						ownQrg,
						sked.getBand()
				);

		if (ownFrequencyKHz != null) {
			System.out.println(
					"[ChatController] SKED frequency from own category QRG: "
							+ ownFrequencyKHz
							+ " kHz on "
							+ sked.getBand()
			);
			return ownFrequencyKHz;
		}

		return null;
	}

	/**
	 * Returns the newest QRG detected on the selected band across all active
	 * suffix and category variants of the base callsign.
	 */
	private Double resolveRecentTargetFrequencyKHz(ContestSked sked) {
		List<ChatMember> variants =
				findActiveChatMembersByRawCall(
						sked.getTargetCallsign()
				);

		long now = System.currentTimeMillis();
		long newestTimestamp = Long.MIN_VALUE;
		Double newestFrequencyKHz = null;

		for (ChatMember member : variants) {
			if (member == null || member.getKnownActiveBands() == null) {
				continue;
			}

			ChatMember.ActiveFrequencyInfo frequencyInfo =
					member.getKnownActiveBands().get(
							sked.getBand()
					);

			if (frequencyInfo == null
					|| frequencyInfo.frequency <= 0.0
					|| !sked.getBand().isPlausible(
					frequencyInfo.frequency
			)) {
				continue;
			}

			long ageMs = now - frequencyInfo.timestampEpoch;

			if (ageMs < 0L
					|| ageMs > BandOpportunityResolver
					.RECENT_DYNAMIC_EVIDENCE_MAX_AGE_MS) {
				continue;
			}

			if (frequencyInfo.timestampEpoch > newestTimestamp) {
				newestTimestamp = frequencyInfo.timestampEpoch;
				newestFrequencyKHz =
						frequencyInfo.frequency * 1000.0;
			}
		}

		return newestFrequencyKHz;
	}

	/**
	 * Returns the own QRG belonging to the chat category in which the target
	 * station was selected.
	 */
	private String resolveOwnQrgForSkedCategory(
			ChatCategory targetCategory) {

		if (sameChatCategory(
				targetCategory,
				getChatCategorySecondChat())) {

			return chatPreferences
					.getMYQRGSecondCat()
					.get();
		}

		return chatPreferences
				.getMYQRGFirstCat()
				.get();
	}

	private boolean sameChatCategory(ChatCategory first,
	                                 ChatCategory second) {

		return first != null
				&& second != null
				&& first.getCategoryNumber()
				== second.getCategoryNumber();
	}

	/**
	 * Parses the QRG formats used by KST4Contest and Win-Test.
	 *
	 * <p>Examples: 144.300, 144.300.03, 144300 and 144300.0.</p>
	 */
	private Double parseSkedFrequencyKHz(String value,
	                                     Band expectedBand) {

		if (value == null
				|| value.isBlank()
				|| expectedBand == null) {
			return null;
		}

		String normalized = value
				.trim()
				.replace(',', '.')
				.replaceAll("\\s+", "");

		try {
			java.util.regex.Matcher groupedFrequency =
					java.util.regex.Pattern.compile(
							"^(\\d{2,5})\\.(\\d{3})(?:\\.(\\d{1,2}))?$"
					).matcher(normalized);

			if (groupedFrequency.matches()) {
				double frequencyKHz =
						Integer.parseInt(groupedFrequency.group(1))
								* 1000.0
								+ Integer.parseInt(
								groupedFrequency.group(2)
						);

				String subKHzPart =
						groupedFrequency.group(3);

				if (subKHzPart != null) {
					double subKHz =
							Integer.parseInt(subKHzPart);

					frequencyKHz +=
							subKHzPart.length() == 1
									? subKHz / 10.0
									: subKHz / 100.0;
				}

				return expectedBand.isPlausible(
						frequencyKHz / 1000.0
				) ? frequencyKHz : null;
			}

			double numericValue =
					Double.parseDouble(normalized);

			if (expectedBand.isPlausible(numericValue)) {
				return numericValue * 1000.0;
			}

			if (expectedBand.isPlausible(
					numericValue / 1000.0)) {
				return numericValue;
			}

		} catch (NumberFormatException ignored) {
			// Invalid or unsupported QRG format.
		}

		return null;
	}

	/**
	 * Removes KST dash suffixes while retaining ordinary amateur-radio slash
	 * notation such as /P, /M or a country prefix.
	 *
	 * <p>Examples:
	 * DN9APW-2 -> DN9APW
	 * EA5/G8MBI/P-70 -> EA5/G8MBI/P
	 * DN9APW-2/P -> DN9APW/P</p>
	 */
	private String toWinTestSkedCallsign(String chatCallsign) {
		if (chatCallsign == null || chatCallsign.isBlank()) {
			return null;
		}

		String normalized =
				chatCallsign.trim().toUpperCase(Locale.ROOT);

		return normalized.replaceAll(
				"-[^/]*(?=/|$)",
				""
		);
	}

	private void reportSkippedWinTestSked(ContestSked sked,
	                                      String reason) {

		String target =
				sked == null
						? "unknown station"
						: sked.getTargetChatCallsign();

		String message =
				"Win-Test sked not sent for "
						+ target
						+ ": "
						+ reason
						+ ". The internal KST4Contest sked remains active.";

		System.out.println(
				"[ChatController] " + message
		);

		onThreadStatus(
				"WT-SkedSend",
				new ThreadStateMessage(
						"WT-SkedSend",
						false,
						message,
						true
				)
		);
	}

	private String resolveSkedTargetLocator(
			String targetCallsignRaw) {

		if (targetCallsignRaw == null
				|| targetCallsignRaw.isBlank()) {
			return null;
		}

		for (ChatMember member
				: findActiveChatMembersByRawCall(
				targetCallsignRaw)) {

			String locator = member.getQra();

			if (locator != null && !locator.isBlank()) {
				return locator
						.trim()
						.toUpperCase(Locale.ROOT);
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

//	public List<ChatMember> snapshotChatMembers() {
//		synchronized (getLst_chatMemberList()) {
//			return new ArrayList<>(getLst_chatMemberList());
//		}
//	}

	public List<ChatMember> snapshotChatMembers() {
		return new ArrayList<>(activeChatMembersByCallAndCategory.values());
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

	private ReachabilityService reachabilityService;
	private final WorkedGrossFieldCache workedGrossFieldCache = new WorkedGrossFieldCache();


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

//    private ObservableList<String> lstNotify_QSOSniffer_sniffedCallSignList = FXCollections.observableArrayList();
private ObservableList<String>
		lstNotify_QSOSniffer_sniffedCallSignList;
	/**
	 * we do some trick here with the chatmemberlist to not make it neccessary to change all boolean properties if the
	 * chatmember object to observables. We trigger the list for changes on an object which we change whenever a list
	 * update will be neccessary to process (important for correct lifetime filtering!)
	 */
//	private ObservableList<ChatMember> chatMemberList = FXCollections.observableArrayList(workedInfoChange -> new Observable[] {workedInfoChange.workedInfoChangeFireListEventTriggerProperty()}); // List of active stations
																								// in chat
	private ObservableList<ChatMember> chatMemberList = FXCollections.observableArrayList(); // List of active stations


																														// of active stn in chat
	private FilteredList<ChatMember> lst_chatMemberListFiltered = new FilteredList<ChatMember>(chatMemberList);
	private SortedList<ChatMember> lst_chatMemberSortedFilteredList = new SortedList<ChatMember>(lst_chatMemberListFiltered);

	private ObservableList<ChatMember> lst_chatMemberList = FXCollections.synchronizedObservableList(chatMemberList); // List
	private ObservableList<Predicate<ChatMember>> lst_chatMemberListFilterPredicates = FXCollections.observableArrayList();
	private ObservableList<ClusterMessage> lst_clusterMemberList = FXCollections.observableArrayList();

	/*
	 * Thread-safe active-member model.
	 *
	 * MessageBusManagementThread must not use the JavaFX TableView backing list as
	 * its primary data model. Otherwise every add/remove fires FilteredList,
	 * SortedList and TableView selection listeners on the MessageBus thread and JavaFX
	 * can throw "Not on FX application thread" or internal listener NPEs.
	 *
	 * This map is the worker-thread safe source of truth. The ObservableList below
	 * remains a UI mirror and is mutated only on the JavaFX application thread.
	 */
	private final java.util.concurrent.ConcurrentMap<String, ChatMember> activeChatMembersByCallAndCategory =
			new java.util.concurrent.ConcurrentHashMap<>();

	/*
	 * Message table update buffers.
	 *
	 * Do not write directly to lst_globalChatMessageList from worker threads.
	 * Use publishChatMessage(...) instead.
	 *
	 * The actual ObservableList mutation is batched and executed on the JavaFX
	 * application thread. The visible list order remains newest-first.
	 */
	private final Object pendingChatMessagesLock = new Object();
	private final List<ChatMessage> pendingChatMessages = new ArrayList<>();
	private boolean chatMessageFlushScheduled = false;

	/*
	 * Same idea for DXCluster messages.
	 */
	private final Object pendingClusterMessagesLock = new Object();
	private final List<ClusterMessage> pendingClusterMessages = new ArrayList<>();
	private boolean clusterMessageFlushScheduled = false;

	private ObservableList<ChatMember> lst_DBBasedWkdCallSignList = FXCollections.observableArrayList();

//	private HashMap<String, ChatMember> map_ucxLogInfoWorkedCalls = new HashMap<String, ChatMember>(); //Destination of ucx-log worked-messages

	// ******************************************************************************************************************************************


	/**
	 * Executes UI-bound work on the JavaFX application thread.
	 *
	 * Worker threads may call this safely. If the caller is already on the FX
	 * thread, the action is executed immediately to preserve ordering.
	 */
	private void runOnFxThread(Runnable action) {
		if (action == null) {
			return;
		}

		if (Platform.isFxApplicationThread()) {
			action.run();
		} else {
			Platform.runLater(action);
		}
	}

	/**
	 * Builds the unique identity key for one active ON4KST login.
	 *
	 * <p>The complete callsign must be preserved here. Callsigns such as
	 * {@code 9A0BB-2}, {@code 9A0BB-70} and {@code 9A0BB-144} represent different
	 * active chat sessions and must therefore remain separate objects, even when
	 * they are logged into the same chat category.</p>
	 *
	 * <p>The normalized base callsign ({@code callSignRaw}) is deliberately not
	 * used for this key. It remains the common station key for worked, band and
	 * NOT-QRV information.</p>
	 */
	private String buildActiveChatMemberKey(String callSign, ChatCategory category) {

		if (callSign == null) {
			return null;
		}

		String fullCallSign = callSign.trim().toUpperCase(Locale.ROOT);

		if (fullCallSign.isBlank()) {
			return null;
		}

		int categoryNumber =
				category == null ? -1 : category.getCategoryNumber();

		return fullCallSign + "|" + categoryNumber;
	}

	private String buildActiveChatMemberKey(ChatMember member) {

		if (member == null) {
			return null;
		}

		return buildActiveChatMemberKey(
				member.getCallSign(),
				member.getChatCategory()
		);
	}

	/**
	 * Adds or replaces an active chat member in the worker-thread model and mirrors
	 * that change to the JavaFX list. This is the only supported path for ON4KST
	 * user-enter events.
	 */
	public void addOrUpdateActiveChatMember(ChatMember member) {
		String key = buildActiveChatMemberKey(member);
		if (key == null) {
			return;
		}

		activeChatMembersByCallAndCategory.put(key, member);

		runOnFxThread(() -> {
			int existingIndex = findChatMemberIndexInUiListByKey(key);
			if (existingIndex >= 0) {
				lst_chatMemberList.set(existingIndex, member);
			} else {
				lst_chatMemberList.add(member);
			}
		});
	}

	/**
	 * Removes an active chat member from the worker-thread model and from the UI
	 * mirror. Removal from the ObservableList is always performed on the FX thread
	 * because the list is bound to FilteredList/SortedList/TableView.
	 */
	public boolean removeActiveChatMember(ChatMember member) {
		String key = buildActiveChatMemberKey(member);
		if (key == null) {
			return false;
		}

		ChatMember removedMember = activeChatMembersByCallAndCategory.remove(key);
		runOnFxThread(() -> lst_chatMemberList.removeIf(currentMember -> key.equals(buildActiveChatMemberKey(currentMember))));
		return removedMember != null;
	}

	/**
	 * Clears the active-member model and the JavaFX UI mirror. Use this instead of
	 * getLst_chatMemberList().clear() from timers or worker threads.
	 */
	public void clearActiveChatMembers() {
		activeChatMembersByCallAndCategory.clear();
		runOnFxThread(() -> lst_chatMemberList.clear());
	}

	/**
	 * Resolves a member from the thread-safe active model. This avoids reading the
	 * TableView backing list from MessageBusManagementThread.
	 */
	public ChatMember findActiveChatMember(ChatMember lookForThis) {
		String key = buildActiveChatMemberKey(lookForThis);
		return key == null ? null : activeChatMembersByCallAndCategory.get(key);
	}

	public ChatMember findActiveChatMember(String callSign, ChatCategory category) {
		String key = buildActiveChatMemberKey(callSign, category);
		return key == null ? null : activeChatMembersByCallAndCategory.get(key);
	}

	public int getActiveChatMemberCount() {
		return activeChatMembersByCallAndCategory.size();
	}

	/**
	 * Returns all active category variants of a callsign. This is used when a QRG or
	 * band hint should be propagated from one category instance to the same station
	 * in another category.
	 */
	public List<ChatMember> findActiveChatMembersByRawCall(String callSignRaw) {
		String normalizedCallsign = ChatMember.normalizeCallSignToBaseCallSign(callSignRaw);
		if (normalizedCallsign == null || normalizedCallsign.isBlank()) {
			return Collections.emptyList();
		}

		String normalizedKeyPart = normalizedCallsign.trim().toUpperCase(Locale.ROOT);
		List<ChatMember> matchingMembers = new ArrayList<>();

		for (ChatMember member : activeChatMembersByCallAndCategory.values()) {
			if (member == null) {
				continue;
			}

			String memberCall = member.getCallSignRaw() != null ? member.getCallSignRaw() : member.getCallSign();
			String normalizedMemberCall = ChatMember.normalizeCallSignToBaseCallSign(memberCall);
			if (normalizedMemberCall != null && normalizedMemberCall.equalsIgnoreCase(normalizedKeyPart)) {
				matchingMembers.add(member);
			}
		}

		return matchingMembers;
	}

	/**
	 * Copies the band-specific NOT-QRV state to every active category variant of the
	 * same base callsign. The database already uses callSignRaw as its key; applying
	 * the state to the runtime model immediately prevents category-dependent B+,
	 * filter, map and score results before the next database refresh.
	 *
	 * @param sourceMember member whose current NOT-QRV checkboxes are authoritative
	 */
	public void propagateNotQrvStateToActiveMembers(ChatMember sourceMember) {
		if (sourceMember == null) {
			return;
		}

		String rawCall = sourceMember.getCallSignRaw() != null
				? sourceMember.getCallSignRaw()
				: sourceMember.getCallSign();

		List<ChatMember> variants = findActiveChatMembersByRawCall(rawCall);
		if (variants.isEmpty()) {
			variants = List.of(sourceMember);
		}

		for (ChatMember target : variants) {
			if (target == null) {
				continue;
			}

			target.setQrv50(sourceMember.isQrv50());
			target.setQrv70(sourceMember.isQrv70());
			target.setQrv144(sourceMember.isQrv144());
			target.setQrv432(sourceMember.isQrv432());
			target.setQrv1240(sourceMember.isQrv1240());
			target.setQrv2300(sourceMember.isQrv2300());
			target.setQrv3400(sourceMember.isQrv3400());
			target.setQrv5600(sourceMember.isQrv5600());
			target.setQrv10G(sourceMember.isQrv10G());
		}

		if (scoreService != null) {
			scoreService.requestRecompute("NOT-QRV state changed");
		}

		fireUserListUpdate("NOT-QRV state propagated to callsign variants");
	}

	/**
	 * Updates locator and derived direction values in the active model. The UI list
	 * contains the same member object, but the user-list refresh is still triggered
	 * on the JavaFX thread so TableView/map derived displays can repaint safely.
	 */
	public boolean updateActiveChatMemberLocator(ChatMember lookupMember, String newLocator) {
		ChatMember activeMember = findActiveChatMember(lookupMember);
		if (activeMember == null) {
			return false;
		}

		activeMember.setQra(newLocator);
		activeMember.setQrb(new Location().getDistanceKmByTwoLocatorStrings(chatPreferences.getStn_loginLocatorMainCat(), newLocator));
		activeMember.setQTFdirection(new Location(chatPreferences.getStn_loginLocatorMainCat()).getBearing(new Location(newLocator)));
		fireUserListUpdate("Locator changed");
		return true;
	}

	public boolean updateActiveChatMemberState(ChatMember lookupMember, int newState) {
		ChatMember activeMember = findActiveChatMember(lookupMember);
		if (activeMember == null) {
			return false;
		}

		activeMember.setState(newState);
		fireUserListUpdate("User state changed");
		return true;
	}

	/**
	 * Applies a UM3-style user-info update only to an already active chat member.
	 *
	 * ON4KST may send UM3 messages for stations that are not logged into the
	 * current channel, for example when a user changes locator/profile data on the
	 * website. Such stations must not be added to the visible chat member list,
	 * because they cannot be addressed in the channel. Their full information will
	 * be delivered again with UA0/UA5/UA2 when they actually join.
	 *
	 * @return true if an active member was found and updated, false if the UM3
	 *         information was intentionally ignored.
	 */
	public boolean updateActiveChatMemberInfoIfPresent(ChatMember updatedMember) {
		String key = buildActiveChatMemberKey(updatedMember);
		if (key == null) {
			return false;
		}

		ChatMember activeMember = activeChatMembersByCallAndCategory.get(key);
		if (activeMember == null) {
			return false;
		}

		activeMember.setName(updatedMember.getName());
		activeMember.setQra(updatedMember.getQra());
		activeMember.setState(updatedMember.getState());
		activeMember.setLastActivity(updatedMember.getLastActivity());
		activeMember.setActivityTimeLastInEpoch(updatedMember.getActivityTimeLastInEpoch());
		activeMember.setQrb(updatedMember.getQrb());
		activeMember.setQTFdirection(updatedMember.getQTFdirection());
		fireUserListUpdate("User info updated");
		return true;
	}

	/**
	 * Backward-compatible wrapper for older call sites. Despite the historic name,
	 * this method no longer adds unknown UM3 users to the active member model.
	 */
	public boolean updateOrAddActiveChatMemberInfo(ChatMember updatedMember) {
		return updateActiveChatMemberInfoIfPresent(updatedMember);
	}

	/**
	 * Stores a newly detected QRG/band on all active category variants of a station.
	 * The old StringProperty is still set for compatibility with existing TableView
	 * columns and DXCluster code.
	 */
	public void applyDetectedFrequencyToActiveMembers(ChatMember sender, Band detectedBand, double detectedFrequencyMhz) {
		if (sender == null || detectedBand == null || detectedFrequencyMhz <= 0) {
			return;
		}

		String displayFrequency = String.valueOf(detectedFrequencyMhz);
		String rawCall = sender.getCallSignRaw() != null ? sender.getCallSignRaw() : sender.getCallSign();

		for (ChatMember member : findActiveChatMembersByRawCall(rawCall)) {
			if (member == null) {
				continue;
			}
			member.addKnownFrequency(detectedBand, detectedFrequencyMhz);
			member.setFrequency(new SimpleStringProperty(displayFrequency));
		}

		// Also cover dummy senders that are not in the active model, e.g. [n/a] senders.
		sender.addKnownFrequency(detectedBand, detectedFrequencyMhz);
		sender.setFrequency(new SimpleStringProperty(displayFrequency));
		fireUserListUpdate("Frequency detected");
	}

	private int findChatMemberIndexInUiListByKey(String key) {
		if (key == null) {
			return -1;
		}

		for (int i = 0; i < lst_chatMemberList.size(); i++) {
			if (key.equals(buildActiveChatMemberKey(lst_chatMemberList.get(i)))) {
				return i;
			}
		}
		return -1;
	}




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


	/**
	 * Notifies the UI that station-list derived values have changed.
	 *
	 * <p>This method may be called from parser threads, UDP listener threads and
	 * reachability worker threads. Therefore the listener is always invoked on the
	 * JavaFX application thread.</p>
	 *
	 * @param reason short debug reason for the UI log
	 */
	public void fireUserListUpdate(String reason) {
		if (statusListener == null) {
			return;
		}

		if (Platform.isFxApplicationThread()) {
			statusListener.onUserListUpdated(reason);
		} else {
			Platform.runLater(() -> statusListener.onUserListUpdated(reason));
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

	/**
	 * Adds a chat message to the UI message store.
	 *
	 * Important:
	 * - This method may be called from worker threads.
	 * - The ObservableList is modified only on the JavaFX application thread.
	 * - The backing list remains newest-first.
	 * - Old messages are trimmed to avoid unlimited memory growth.
	 */
	public void publishChatMessage(ChatMessage message) {
		if (message == null) {
			return;
		}

		synchronized (pendingChatMessagesLock) {
			pendingChatMessages.add(message);

			if (chatMessageFlushScheduled) {
				return;
			}

			chatMessageFlushScheduled = true;
		}

		Platform.runLater(this::flushPendingChatMessagesToUi);
	}

	private void flushPendingChatMessagesToUi() {
		List<ChatMessage> batch;

		synchronized (pendingChatMessagesLock) {
			batch = new ArrayList<>(pendingChatMessages);
			pendingChatMessages.clear();
			chatMessageFlushScheduled = false;
		}

		if (batch.isEmpty()) {
			return;
		}

		/*
		 * pendingChatMessages is collected in arrival order:
		 * old -> new
		 *
		 * lst_globalChatMessageList must stay newest-first:
		 * new -> old
		 */
		Collections.reverse(batch);

		lst_globalChatMessageList.addAll(0, batch);

		trimGlobalChatMessageListIfNeeded();
	}

	private void trimGlobalChatMessageListIfNeeded() {
		int maxSize = ApplicationConstants.CHAT_MESSAGE_STORE_MAX_SIZE;
		int trimToSize = ApplicationConstants.CHAT_MESSAGE_STORE_TRIM_TO_SIZE;

		if (maxSize <= 0 || trimToSize <= 0 || trimToSize >= maxSize) {
			return;
		}

		int currentSize = lst_globalChatMessageList.size();

		if (currentSize <= maxSize) {
			return;
		}

		/*
		 * List order is newest-first.
		 * Therefore old messages are at the end of the list.
		 */
		lst_globalChatMessageList.remove(trimToSize, currentSize);
	}

	/**
	 * Adds a DXCluster message to the UI cluster message store.
	 *
	 * Same policy as for chat messages:
	 * - batched UI update
	 * - JavaFX thread only for ObservableList mutation
	 * - newest-first visible order
	 * - bounded list size
	 */
	public void publishClusterMessage(ClusterMessage message) {
		if (message == null) {
			return;
		}

		synchronized (pendingClusterMessagesLock) {
			pendingClusterMessages.add(message);

			if (clusterMessageFlushScheduled) {
				return;
			}

			clusterMessageFlushScheduled = true;
		}

		Platform.runLater(this::flushPendingClusterMessagesToUi);
	}

	private void flushPendingClusterMessagesToUi() {
		List<ClusterMessage> batch;

		synchronized (pendingClusterMessagesLock) {
			batch = new ArrayList<>(pendingClusterMessages);
			pendingClusterMessages.clear();
			clusterMessageFlushScheduled = false;
		}

		if (batch.isEmpty()) {
			return;
		}

		Collections.reverse(batch);

		lst_clusterMemberList.addAll(0, batch);

		trimClusterMessageListIfNeeded();
	}

	private void trimClusterMessageListIfNeeded() {
		int maxSize = ApplicationConstants.CLUSTER_MESSAGE_STORE_MAX_SIZE;
		int trimToSize = ApplicationConstants.CLUSTER_MESSAGE_STORE_TRIM_TO_SIZE;

		if (maxSize <= 0 || trimToSize <= 0 || trimToSize >= maxSize) {
			return;
		}

		int currentSize = lst_clusterMemberList.size();

		if (currentSize <= maxSize) {
			return;
		}

		lst_clusterMemberList.remove(trimToSize, currentSize);
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
		lstNotify_QSOSniffer_sniffedCallSignList =
				chatPreferences
						.getLstNotify_QSOSniffer_sniffedCallSignList();

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
		reachabilityService = new ReachabilityService(this);
		rebuildWorkedGrossFieldCacheFromDatabase();

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
//            if ((lstNotify_QSOSniffer_sniffedCallSignList.contains(senderCall) ||
//                    lstNotify_QSOSniffer_sniffedCallSignList.contains(receiverCall)) &&
//                    (!receiverCall.equals(this.getChatPreferences().getStn_loginCallSignRaw()))) {
//
//                msgText = ("Sniffed: " + "(" + senderCall + " > ") + receiverCall +") " + msgText;
//                chatMessage.setMessageText(msgText);
//                return true;
//            }

			if (isSniffedMessage(chatMessage)) {
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

//        lstNotify_QSOSniffer_sniffedCallSignList.add("DF0GEB");


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

	public synchronized void startDxClusterServerIfEnabled() {
		if (!chatPreferences.isNotify_dxClusterServerEnabled()
				|| dxClusterServer != null) {
			return;
		}

		dxClusterServer = new DXClusterThreadPooledServer(
				chatPreferences.getNotify_dxclusterServerPort(),
				this,
				this
		);

		Thread serverThread = new Thread(dxClusterServer);
		serverThread.setName("DXCluster-thread-pooled-server");
		serverThread.setDaemon(true);
		serverThread.start();
	}

	public synchronized void stopDxClusterServer() {
		DXClusterThreadPooledServer serverToStop = dxClusterServer;
		dxClusterServer = null;

		if (serverToStop != null) {
			serverToStop.stop();
		}
	}

	public synchronized void restartDxClusterServerIfEnabled() {
		stopDxClusterServer();
		startDxClusterServerIfEnabled();
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
	 * Starts the shared beacon timer with the interval currently stored in the
	 * preferences.
	 *
	 * <p>Both chat categories deliberately use the same timer. The task checks the
	 * individual enable flag and text for each category every time it runs.</p>
	 *
	 * @param initialDelayMillis delay before the next beacon run
	 */
	private synchronized void scheduleBeaconTimer(long initialDelayMillis) {
		stopBeaconTimer();

		int configuredInterval =
				chatPreferences.getBcn_beaconIntervalInMinutesMainCat();
		int effectiveInterval =
				Math.max(MIN_BEACON_INTERVAL_MINUTES, configuredInterval);

		/*
		 * Keep both legacy XML values synchronized. The second value remains in the
		 * configuration for backward compatibility, but no longer represents an
		 * independent timer.
		 */
		chatPreferences.setBcn_beaconIntervalInMinutesMainCat(effectiveInterval);
		chatPreferences.setBcn_beaconIntervalInMinutesSecondCat(effectiveInterval);

		long intervalMillis = TimeUnit.MINUTES.toMillis(effectiveInterval);
		long safeInitialDelay = Math.max(0L, initialDelayMillis);

		beaconTimer = new Timer("BeaconTimer");
		beaconTimer.schedule(
				new BeaconTask(this, this),
				safeInitialDelay,
				intervalMillis
		);

		System.out.println("[ChatController, Info]: Shared beacon timer scheduled every "
				+ effectiveInterval + " minute(s).");
	}

	/**
	 * Applies a changed beacon interval while the chat connection is running.
	 *
	 * <p>The countdown starts again with the new interval. No beacon is sent
	 * immediately merely because the setting was changed.</p>
	 */
	public synchronized void restartBeaconTimer() {
		if (!isConnectedAndLoggedIn()) {
			return;
		}

		long intervalMillis = TimeUnit.MINUTES.toMillis(
				Math.max(
						MIN_BEACON_INTERVAL_MINUTES,
						chatPreferences.getBcn_beaconIntervalInMinutesMainCat()
				)
		);
		scheduleBeaconTimer(intervalMillis);
	}

	/**
	 * Stops the shared beacon timer if it is currently running.
	 */
	private synchronized void stopBeaconTimer() {
		if (beaconTimer == null) {
			return;
		}

		beaconTimer.cancel();
		beaconTimer.purge();
		beaconTimer = null;
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
			socket = new Socket(
					chatPreferences.getStn_on4kstServersDns(),
					chatPreferences.getStn_on4kstServersPort()
			);//socket for the on4kst chat server

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

			airScoutUDPReaderThread = new ReadUDPbyAirScoutMessageThread(
					chatPreferences.getAirScout_asCommunicationPort(),
					this,
					this
			);
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
			startDxClusterServerIfEnabled();


			this.setConnectedAndLoggedIn(true);

			/**
			 * The CQ-beacon-Task will be executed every time but checks for itself whether
			 * CQ messages are enabled or not
			 */
			scheduleBeaconTimer(INITIAL_BEACON_DELAY_MILLIS);

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
							chatController.clearActiveChatMembers();

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


	/**
	 * Returns the background reachability service used by the station table.
	 *
	 * @return reachability service
	 */
	public ReachabilityService getReachabilityService() {
		return reachabilityService;
	}

	/**
	 * Returns the runtime gross-field cache used by the new-locator filter.
	 *
	 * @return worked gross-field cache
	 */
	public WorkedGrossFieldCache getWorkedGrossFieldCache() {
		return workedGrossFieldCache;
	}

	/**
	 * Rebuilds the gross-field cache from persistent SQLite data and enriches it with
	 * legacy ChatMember worked/qra rows where possible.
	 */
	public void rebuildWorkedGrossFieldCacheFromDatabase() {
		if (dbHandler == null) {
			return;
		}

		try {
			workedGrossFieldCache.rebuildFromDatabaseSnapshot(dbHandler.fetchWorkedGrossFieldsFromDB());
			workedGrossFieldCache.addWorkedBandsFromStoredChatMembers(dbHandler.fetchChatMemberWkdDataFromDB().values());
		} catch (Exception exception) {
			System.out.println("[ChatController, warning]: could not rebuild worked gross-field cache: "
					+ exception.getMessage());
		}
	}

	/**
	 * Persists and caches one worked gross field after a logger QSO packet.
	 *
	 * @param band worked band
	 * @param locator6 six-character Maidenhead locator
	 * @param workedCall worked station
	 * @param source logger/source name
	 */
	public void registerWorkedGrossField(Band band, String locator6, ChatMember workedCall, String source) {
		if (band == null || locator6 == null) {
			return;
		}

		String normalizedLocator6 = WorkedGrossFieldCache.extractLocator6(locator6);
		if (normalizedLocator6 == null) {
			return;
		}

		String callSignRaw = workedCall == null ? null : workedCall.getCallSignRaw();

		try {
			dbHandler.upsertWorkedGrossField(band, normalizedLocator6, callSignRaw, source);
		} catch (Exception exception) {
			System.out.println("[ChatController, warning]: could not persist worked gross field: "
					+ exception.getMessage());
		}

		workedGrossFieldCache.addWorked(band, normalizedLocator6);
		fireUserListUpdate("Worked gross field updated");
	}

	/**
	 * Returns true when the member's four-character grid square has already been
	 * worked on any band.
	 *
	 * <p>This method intentionally ignores the currently enabled own bands. The grid
	 * status in the user list is meant to be a simple and robust any-band indicator.</p>
	 *
	 * @param member member to inspect
	 * @return true if the gross grid square was worked on any band
	 */
	public boolean isGridSquareWorkedAny(ChatMember member) {
		if (member == null || member.getQra() == null) {
			return false;
		}

		return workedGrossFieldCache.isGrossFieldWorkedAny(member.getQra());
	}

	/**
	 * Returns true when the member's four-character grid square has not been worked
	 * on any band yet.
	 *
	 * <p>This is the predicate behind the optional "Only new grids" filter. It no
	 * longer depends on active station band settings.</p>
	 *
	 * @param member member to inspect
	 * @return true if the gross grid square is still new
	 */
	public boolean isNewGridSquare(ChatMember member) {
		if (member == null || member.getQra() == null) {
			return false;
		}

		return !workedGrossFieldCache.isGrossFieldWorkedAny(member.getQra());
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
						+ "|" + chatPreferences.getLoginChatCategoryMain().getCategoryNumber() + "|praktiKST v" + ApplicationConstants.APPLICATION_CURRENT_VERSION
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

		rebuildWorkedGrossFieldCacheFromDatabase();

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
			activeChatMember.setWorked50(storedChatMemberState.isWorked50());
			activeChatMember.setWorked70(storedChatMemberState.isWorked70());
			activeChatMember.setQrv144(storedChatMemberState.isQrv144());
			activeChatMember.setQrv432(storedChatMemberState.isQrv432());
			activeChatMember.setQrv1240(storedChatMemberState.isQrv1240());
			activeChatMember.setQrv2300(storedChatMemberState.isQrv2300());
			activeChatMember.setQrv3400(storedChatMemberState.isQrv3400());
			activeChatMember.setQrv5600(storedChatMemberState.isQrv5600());
			activeChatMember.setQrv10G(storedChatMemberState.isQrv10G());
			activeChatMember.setQrv50(storedChatMemberState.isQrv50());
			activeChatMember.setQrv70(storedChatMemberState.isQrv70());
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

	/**
	 * decides if a message in the in-queue is directed to me or if its directed to another station and sniffed
	 * @param chatMessage
	 * @return
	 */
	public boolean isSniffedMessage(ChatMessage chatMessage) {
		if (chatMessage == null || chatMessage.getSender() == null || chatMessage.getReceiver() == null) {
			return false;
		}

		String senderCall = chatMessage.getSender().getCallSign();
		String receiverCall = chatMessage.getReceiver().getCallSign();

		if (senderCall == null || receiverCall == null) {
			return false;
		}

		if (lstNotify_QSOSniffer_sniffedCallSignList == null || lstNotify_QSOSniffer_sniffedCallSignList.isEmpty()) {
			return false;
		}

		boolean observedCall =
				lstNotify_QSOSniffer_sniffedCallSignList.contains(senderCall)
						|| lstNotify_QSOSniffer_sniffedCallSignList.contains(receiverCall);

		if (!observedCall) {
			return false;
		}

		String myCall = getChatPreferences() != null ? getChatPreferences().getStn_loginCallSign() : null;
		String myRawCall = getChatPreferences() != null ? getChatPreferences().getStn_loginCallSignRaw() : null;

		/*
		 * Sniffed messages should appear in the private table only if they are not
		 * already direct messages to my own callsign.
		 */
		return !receiverCall.equals(myCall) && !receiverCall.equals(myRawCall);
	}

	/**
	 * changes the chatmessage if it had been a sniffed one and not directed to me. Only for marking.
	 * @param chatMessage
	 * @return
	 */
	public String formatChatMessageTextForDisplay(ChatMessage chatMessage) {
		if (chatMessage == null) {
			return "";
		}

		String msgText = chatMessage.getMessageText();

		if (msgText == null) {
			msgText = "";
		}

		if (!isSniffedMessage(chatMessage)) {
			return msgText;
		}

		String senderCall = chatMessage.getSender() != null ? chatMessage.getSender().getCallSign() : "";
		String receiverCall = chatMessage.getReceiver() != null ? chatMessage.getReceiver().getCallSign() : "";

		return "Sniffed: (" + senderCall + " > " + receiverCall + ") " + msgText;
	}
}