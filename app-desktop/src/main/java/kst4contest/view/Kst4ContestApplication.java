package kst4contest.view;
import kst4contest.view.compose.SettingsHost;
import kst4contest.view.compose.SettingsNotices;
import kst4contest.view.compose.SettingsTabsKt;
import kst4contest.view.compose.UpdateWindow;
import kst4contest.view.compose.UpdateWindowState;
import kst4contest.view.compose.ColumnWidthStore;
import kst4contest.view.compose.DataTableState;
import kst4contest.view.compose.MonitorColumns;
import kst4contest.view.compose.MonitorWindow;
import kst4contest.view.compose.RowKeys;
import kst4contest.view.compose.BlinkingNotice;
import kst4contest.view.compose.ThreadStatusButtons;
import kst4contest.view.compose.StationColumns;
import kst4contest.view.compose.StationFilterState;
import kst4contest.view.compose.DirectedMessageColumns;
import kst4contest.view.compose.PublicMessageColumns;
import kst4contest.view.compose.SelectedMessageFilter;
import kst4contest.view.compose.SelectedStationMessageColumns;
import kst4contest.view.compose.SelectedStationState;
import kst4contest.view.compose.TopPriorityState;
import kst4contest.view.compose.TimelineState;
import kst4contest.view.compose.ChatInputState;
import kst4contest.view.compose.MainMenuState;
import kst4contest.view.compose.MainWindowState;
import kst4contest.view.compose.MainWindowSurroundings;
import kst4contest.view.compose.MainWindowHost;
import kst4contest.view.compose.MainWindowFrame;
import kst4contest.view.compose.MainWindowSize;
import kst4contest.view.compose.SplitterState;
import kst4contest.view.compose.SettingsWindow;
import kst4contest.controller.ScoreService;
import kst4contest.view.compose.SettingsWindow;
import kst4contest.controller.On4KstConnectionState;

import javafx.scene.image.Image;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.event.Event;
import kst4contest.view.TimelineView; // The new class we created
import kst4contest.model.ContestSked; // The new model
import javafx.scene.control.TableRow; // For the priority coloring

import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import javafx.util.StringConverter;
import kst4contest.ApplicationConstants;
import kst4contest.view.compose.OperatorProfilePickerWindow;
import kst4contest.observe.MutableValue;
import kst4contest.observe.ObservableRoster;
import kst4contest.observe.SimpleValue;
import kst4contest.observe.UiDispatcher;
import kst4contest.controller.ChatController;
import kst4contest.controller.MessageVariableResolver;
import kst4contest.controller.StatusUpdateListener;
import kst4contest.controller.Utils4KST;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import kst4contest.locatorUtils.DirectionUtils;
import kst4contest.model.*;

import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.stage.Screen;

import kst4contest.logic.BandOpportunityResolver;
import kst4contest.utils.ApplicationFileLogging;
import kst4contest.utils.ApplicationFileUtils;
import kst4contest.utils.PlatformUtils;
import kst4contest.controller.ActiveOperatorProfile;
import kst4contest.controller.OperatorProfileStore;
import kst4contest.controller.OperatorProfileManagementService;
import kst4contest.controller.OperatorProfilePaths;
import kst4contest.model.OperatorProfile;
import kst4contest.model.OperatorProfileSelection;
import kst4contest.view.map.OfflineDemImportService;
import kst4contest.controller.WorkedGrossFieldCache;


public class Kst4ContestApplication extends Application implements StatusUpdateListener, SettingsHost  {
	
//	private static final Kst4ContestApplication dbcontroller = new DBController();
	// Null means Auto: use the lowest session band, or category fallback.

	// Enables optional color highlighting of the QRA/grid cell.
// The status text itself remains visible independently of this flag.

	private static final Logger LOGGER = Logger.getLogger(
			Kst4ContestApplication.class.getName());
	private static ApplicationFileLogging fileLogging;
	private static final String SIMPLE_LOG_MANUAL_URL =
			"https://kst4contest.hamradioonline.de/manual/en/log-sync/"
					+ "#method-1-universal-file-based-callsign-interpreter-simplelogfile";

	private boolean gridSquareHighlightEnabled = false;


	/**
	 * True for a very short moment when the operator explicitly interacts with the
	 * ChatMember table by mouse or keyboard.
	 *
	 * This flag is the distinction between:
	 * - real operator selection: may overwrite the send field with "/cq CALL "
	 * - artificial refresh/selection event: must not overwrite operator text
	 */
	private boolean operatorInitiatedChatMemberSelectionChange = false;


	private PauseTransition userListRefreshCoalescer;
	private String pendingUserListUpdateReason = "";

	/**
	 * Remembers the last combined ChatMember table predicate that was applied.
	 *
	 * This avoids repeatedly assigning the same predicate to the FilteredList during
	 * periodic refreshes. Reassigning the predicate without a real filter change can
	 * invalidate the TableView and trigger artificial selection events.
	 */
	private Predicate<ChatMember> lastAppliedChatMemberFilterPredicate = null;


	private Band selectedReachabilityBandOverride = null;

			private LayoutAutosave layoutAutosave;

	private final Button btnConnectionStateIndicator = new Button("LINK");
	private final Tooltip tipConnectionStateIndicator = new Tooltip();
	private On4KstConnectionState lastDisplayedConnectionState;
	private String lastDisplayedConnectionDetail = "";

	/** Main window menu bar; its menus are shared with the secondary windows on macOS. */
	private MenuBar mainScreenMenuBar;
	/** macOS only: connection state shown as menu title in the system menu bar. */
	private final Menu menuConnectionStateMacOs = new Menu("LINK");
	private final MenuItem menuItemConnectionStateDetailMacOs = new MenuItem();
	/** Scenes whose stylesheet follows the light/dark design switch. */
	private final List<Scene> themedScenes = new ArrayList<>();

	private final Button btnBandUpgradeIndicator = new Button("BAND+");
	private final Tooltip tipBandUpgradeIndicator = new Tooltip();
	private Timeline bandUpgradeBlinkTimeline;

	private final Button btnSkedWarnIndicator = new Button("SKED");
	private final Tooltip tipSkedWarnIndicator = new Tooltip();
	private Timeline skedWarnBlinkTimeline;

	// Timeline: show at most N priority markers per minute bucket (minute 0/1 often has many planes)
	private static final int TIMELINE_PRIORITY_MARKERS_PER_MINUTE = 2;

	// Timeline: show 2 more in other directions
	private static final int TIMELINE_BEAM_MARKERS_PER_MINUTE = 2;


	// Keep in sync with TimelineView PREVIEW_TIME_MS (currently 30 minutes = 30L * 60L * 1000L)
	private static final long TIMELINE_PREVIEW_TIME_MS = 30L * 60L * 1000L;

	/**
	 * Reused show-all predicate for the station table.
	 *
	 * Using the same instance avoids unnecessary FilteredList invalidations when no
	 * station filters are active. Such invalidations can make the TableView selection
	 * model fire again although the operator did not select another station.
	 */
	private static final Predicate<ChatMember> SHOW_ALL_CHAT_MEMBER_PREDICATE = chatMember -> true;

	//recoloring of the chatmembers list is turned on and off here
	private static final boolean ENABLE_PRIORITY_SCORE_ROW_COLORING = false;

	private TimelineView timelineView; //timeline view above the sendtext-field

    private final Map<String, Button> statusButtons = new HashMap<>(); //there we will place some flickering

	public static final String STYLE_DEFAULTCSSDAY_FILE = "KST4ContestDefaultDay.css";
	public static final String STYLE_DEFAULTCSSDAY_RESOURCE = "/KST4ContestDefaultDay.css";

	public static final String STYLE_DEFAULTCSSEVENING_FILE = "KST4ContestDefaultEvening.css";
	public static final String STYLE_DEFAULTCSSEVENING_RESOURCE = "/KST4ContestDefaultEvening.css";

	String chatState;
	/** Hands worker-thread results over to the JavaFX application thread. */
	private final UiDispatcher uiDispatcher = new JavaFxUiDispatcher();

	/**
	 * Runs work on the JavaFX thread. Package-private so the Compose bridge classes can hand
	 * their work to the same dispatcher rather than bringing a second one.
	 */
	void runOnUi(final Runnable work) {
		uiDispatcher.runOnUi(work);
	}

	/**
	 * The profile switch, reachable from the Compose menu.
	 *
	 * <p>A thin forwarder rather than a promotion of the private method: the switch is about
	 * to change its nature in task 14, and a second public entry point would be a second
	 * thing to change then.</p>
	 */
	void showOperatorProfileSwitchDialogFromCompose() {
		showOperatorProfileSwitchDialog();
	}

	/** Forwarders so the Compose send line runs the very same checks the send button does. */
	boolean isMessageAddressedToOwnCallsignFromCompose(final String messageText) {
		return isMessageAddressedToOwnCallsign(messageText);
	}

	ChatCategory resolveOutgoingChatCategoryFromCompose(
			final String outgoingText,
			final ChatMember selectedMember) {
		return resolveOutgoingChatCategory(outgoingText, selectedMember);
	}

	/**
	 * Follows the own QRG in the text field. Replaces the former
	 * {@code textProperty().bind(...)}: a MutableValue is not a JavaFX
	 * property, so the field is updated through a listener instead. Attaching
	 * twice is harmless; detaching releases the listener.
	 */
	private java.util.function.Consumer<String> ownQrgFollower;

	private void attachOwnQrgFollower() {
		detachOwnQrgFollower();
		MutableValue<String> ownQrg = chatcontroller.getChatPreferences().getMYQRGFirstCat();
		ownQrgFollower = value -> uiDispatcher.runOnUi(() -> txt_ownqrgMainCategory.setText(value));
		ownQrg.addListener(ownQrgFollower);
		txt_ownqrgMainCategory.setText(ownQrg.get());
	}

	private void detachOwnQrgFollower() {
		if (ownQrgFollower != null) {
			chatcontroller.getChatPreferences().getMYQRGFirstCat().removeListener(ownQrgFollower);
			ownQrgFollower = null;
		}
	}

	/** Live mirrors of core rosters, handed to TableViews. Released on shutdown. */
	private final java.util.List<FxRosterBinding<?>> rosterBindings = new java.util.ArrayList<>();

	private <T> javafx.collections.ObservableList<T> mirrorOf(ObservableRoster<T> roster) {
		FxRosterBinding<T> binding = FxRosterBinding.mirror(roster, uiDispatcher);
		rosterBindings.add(binding);
		return binding.list();
	}

	/**
	 * Registers a mirror whose content is computed rather than copied. Used for the
	 * station list and the message tabs, which used to be FilteredList/SortedList
	 * views on a core collection.
	 */
	private <T> FxRosterBinding<T> derivedBinding(
			java.util.function.Supplier<java.util.List<T>> derivation,
			ObservableRoster<?>... sources) {
		FxRosterBinding<T> binding = FxRosterBinding.derived(uiDispatcher, derivation, sources);
		rosterBindings.add(binding);
		return binding;
	}

	/** The visible station list, replacing the former SortedList on the TableView. */
	private FxRosterBinding<ChatMember> chatMemberListBinding;

	/**
	 * The message view of the selected-station info window. Its filter is switched
	 * by the radio buttons of that window, so the binding has to be refreshed
	 * explicitly. The other message views keep the filter they were given in the
	 * controller and follow their source rosters on their own.
	 */
	private FxRosterBinding<ChatMessage> selectedCallSignInfoMessageBinding;

	/**
	 * Sets the message filter of the selected-station info window and shows the
	 * result immediately, the way {@code FilteredList.setPredicate} did.
	 */
	private void applySelectedCallSignInfoFilter(Predicate<ChatMessage> filter) {
		chatcontroller.setSelectedCallSignInfoFilter(filter);
		if (selectedCallSignInfoMessageBinding != null) {
			selectedCallSignInfoMessageBinding.refresh();
			Platform.runLater(() -> {
				if (composeMainWindowState != null) {
					uiDispatcher.runOnUi(() -> composeMainWindowState.getSelectedStationMessages().replaceRows(
							new java.util.ArrayList<>(selectedCallSignInfoMessageBinding.list())
					));
				}
			});
		}
	}

	/** Follows the PSTRotator QTF in the text field; replaces a JavaFX string binding. */
	private java.util.function.Consumer<Double> qtfFollower;

	private void attachQtfFollower() {
		detachQtfFollower();
		MutableValue<Double> qtf = chatcontroller.getChatPreferences().getActualQTF();
		qtfFollower = value -> uiDispatcher.runOnUi(
				() -> txt_myQTF.setText(value == null ? "" : Double.toString(value)));
		qtf.addListener(qtfFollower);
		txt_myQTF.setText(qtf.get() == null ? "" : Double.toString(qtf.get()));
	}

	private void detachQtfFollower() {
		if (qtfFollower != null) {
			chatcontroller.getChatPreferences().getActualQTF().removeListener(qtfFollower);
			qtfFollower = null;
		}
	}

	/**
	 * Follows the priority score of the selected station in a label. Replaces a
	 * JavaFX string binding: the score is no longer a JavaFX property. The listener
	 * is owned by the label, which is discarded together with the info pane.
	 */
	private void attachPriorityScoreFollower(Label label) {
		/*
		 * Fully qualified on purpose: javafx.beans.value.ObservableValue is imported
		 * in this class, so the core type cannot be imported under the same name.
		 */
		kst4contest.observe.ObservableValue<Double> score =
				chatcontroller.getScoreService().selectedCallPriorityScore();
		java.util.function.Consumer<Double> follower =
				value -> uiDispatcher.runOnUi(() -> label.setText(formatPriorityScore(value)));
		score.addListener(follower);
		label.setText(formatPriorityScore(score.get()));
		label.sceneProperty().addListener((observable, oldScene, newScene) -> {
			if (newScene == null) {
				score.removeListener(follower);
			}
		});
	}

	private static String formatPriorityScore(Double value) {
		if (value == null || Double.isNaN(value)) {
			return "Priority score: -";
		}
		return String.format(java.util.Locale.US, "Priority score: %.0f", value);
	}

	ChatController chatcontroller;
	public ChatController getChatController() { return chatcontroller; }
	MessageVariableResolver messageVariableResolver;


	Button MYQRGButton; // TODO: clean code? Got the myqrg button out of the factory method to modify
						// the text later
	Button MYCALLSetQRGButton;

	Timer timer_buildWindowTitle;
//	Timer timer_chatMemberTableSortTimer; // need that because javafx bug, it´s the only way to actualize the table...
	Timer timer_updatePrivatemessageTable; // same here
	VBox selectedCallSignFurtherInfoPane = new VBox();

	ToggleButton[] btnQtfButtonsAvl = new ToggleButton[8];

	private void ensureStationMapSupportInitialized() {
		// Nothing to initialize for Compose map here
	}

	private void toggleStationMapWindow() {
		if (composeMainWindowState != null) {
			kst4contest.view.compose.map.StationMapWindow.INSTANCE.toggle(composeMainWindowState);
		}
	}

	private void showSelectedCallsignOnMap() {
		if (composeMainWindowState != null) {
			if (!kst4contest.view.compose.map.StationMapWindow.INSTANCE.isShowing()) {
				kst4contest.view.compose.map.StationMapWindow.INSTANCE.toggle(composeMainWindowState);
			}
		}
	}

	private void refreshStationMapIfVisible() {
		// Compose map state is reactive to main window state
	}

	/**
	 * Resolves a reasonable initial directory for the DEM tile file chooser.
	 *
	 * <p>Preference order:
	 * <ol>
	 *     <li>configured DEM root directory if it already exists</li>
	 *     <li>its parent directory if that exists</li>
	 *     <li>user home directory</li>
	 * </ol>
	 *
	 * @param configuredDemRootDirectory current DEM root directory text
	 * @return usable initial directory or null
	 */
	private File resolveInitialDirectoryForDemImport(String configuredDemRootDirectory) {
		if (configuredDemRootDirectory != null && !configuredDemRootDirectory.isBlank()) {
			File configuredDirectory = new File(configuredDemRootDirectory.trim());

			if (configuredDirectory.isDirectory()) {
				return configuredDirectory;
			}

			File parentDirectory = configuredDirectory.getParentFile();
			if (parentDirectory != null && parentDirectory.isDirectory()) {
				return parentDirectory;
			}
		}

		File userHomeDirectory = new File(System.getProperty("user.home"));
		return userHomeDirectory.isDirectory() ? userHomeDirectory : null;
	}

	/**
	 * helper DTO for planes and arriving time in minutes. Maybe
	 */
	private static final class NextApInfo {
		final AirPlane plane;
		final int arrivingMinutes;

		private NextApInfo(AirPlane plane, int arrivingMinutes) {
			this.plane = plane;
			this.arrivingMinutes = arrivingMinutes;
		}
	}

	/**
	 * Helper DTO for timeline building
	 */
	private static final class TimelineCandidateTmp {
		final kst4contest.controller.ScoreService.TopCandidate top;
		final ChatMember representativeMember;
		final NextApInfo nextAp;

		TimelineCandidateTmp(kst4contest.controller.ScoreService.TopCandidate top, ChatMember representativeMember, NextApInfo nextAp) {
			this.top = top;
			this.representativeMember = representativeMember;
			this.nextAp = nextAp;
		}
	}


	public static void showUserInputErrorWindow (String message) {

		Alert a = new Alert(AlertType.INFORMATION);

		a.setTitle("You entered something strange");
		a.setHeaderText("Value not accepted");
		a.setContentText(message);
		a.show();

	}

	/**
	 * Method to draw an arrow with the head pointing to a callsigns maidenhead locator direction
	 * @param deg
	 * @return
	 */
	public static Node createArrow(double deg) {
		// Convert degrees to radians
		double rad = Math.toRadians(90-180 - deg);

		// Length of the arrow line
		double arrowLength = 6;

		// Coordinates of the arrow tip
		double tipX = arrowLength * Math.cos(rad);
		double tipY = arrowLength * Math.sin(rad);

		// Draw the arrow line
		Line arrowLine = new Line(0, 0, tipX, -tipY);
		arrowLine.setStroke(Color.LIGHTGREEN);

		// Calculate coordinates for the arrowhead
		double arrowheadAngle = Math.toRadians(20); // Angle of arrowhead
		double arrowheadLength = 15; // Length of arrowhead
		double arrowheadX1 = tipX + arrowheadLength * Math.cos(rad - arrowheadAngle);
		double arrowheadY1 = tipY + arrowheadLength * Math.sin(rad - arrowheadAngle);
		double arrowheadX2 = tipX + arrowheadLength * Math.cos(rad + arrowheadAngle);
		double arrowheadY2 = tipY + arrowheadLength * Math.sin(rad + arrowheadAngle);

		// Draw the arrowhead
		Polygon arrowhead = new Polygon(
				0, 0,  // tip
				arrowheadX1, -arrowheadY1, // left corner
				arrowheadX2, -arrowheadY2 // right corner
		);
		arrowhead.setFill(Color.GREEN);

		// Return the arrow element (line + polygon)
		return new javafx.scene.Group(arrowLine, arrowhead);
	}

    /**
     * Gets thread notifications and makes new statusbuttons at the top
     *
     * @param sourceName
     */
    private void updateStatusButton(String sourceName, ThreadStateMessage threadStateMessage) {
        Button button = statusButtons.computeIfAbsent(sourceName, name -> {
            Button b = new Button(threadStateMessage.getThreadNickName());
            b.getStyleClass().removeIf(cls -> cls.startsWith("btn-showstate"));

            b.getStyleClass().add("btn-showstate-enabled");
             b.setTooltip(new Tooltip(threadStateMessage.getRunningInformation()));

            this.flwpne_StatusBar.getChildren().add(b); // BorderPane oder HBox o. ä.
            return b;
        });


        button.setText(sourceName + ": " + threadStateMessage.getRunningInformationTextDescription());

		if (composeMainWindowState != null) {
			composeMainWindowState.getThreadButtons().update(sourceName, threadStateMessage);
		}

        button.getTooltip().setText(threadStateMessage.getRunningInformation());
        button.getStyleClass().removeIf(cls -> cls.startsWith("btn-showstate"));
        button.getStyleClass().add("btn-showstate-enabled-furtherInfo");
//        button.setStyle("-fx-text-fill: red;");


        PauseTransition pause = new PauseTransition(Duration.seconds(0.2));
        pause.setOnFinished(e -> {
            button.getStyleClass().removeIf(cls -> cls.startsWith("btn-showstate"));
            button.getStyleClass().add("btn-showstate-enabled-default");
//                button.setStyle("-fx-text-fill: blue;");
        });
        pause.play();

    }

	/**
	 * Helps the view to format the RX Bands for a callsign, using the chatmembers frequencies detected MAP
	 * @param callSignRaw
	 * @param maxAgeMs
	 * @return
	 */
	private String formatDetectedRxBandsForCallsignRaw(String callSignRaw, long maxAgeMs) {

		if (callSignRaw == null) return "Bands: -";

		List<ChatMember> variants = chatcontroller.findActiveChatMembersByRawCall(callSignRaw);
		BandOpportunityResolver.Resolution bandResolution =
				BandOpportunityResolver.resolve(variants, System.currentTimeMillis(), maxAgeMs);
		EnumSet<Band> availableBands = bandResolution.getAvailableBands();

		if (availableBands.isEmpty()) {
			return "Bands: -";
		}

		Map<Band, ChatMember.ActiveFrequencyInfo> newestPerBand =
				new EnumMap<>(Band.class);

		for (ChatMember member : variants) {
			if (member == null || member.getKnownActiveBands() == null) continue;

			for (Map.Entry<Band, ChatMember.ActiveFrequencyInfo> entry
					: member.getKnownActiveBands().entrySet()) {

				Band band = entry.getKey();
				ChatMember.ActiveFrequencyInfo info = entry.getValue();
				if (band == null || info == null || !availableBands.contains(band)) continue;

				long ageMs = System.currentTimeMillis() - info.timestampEpoch;
				if (ageMs < 0L || (maxAgeMs > 0 && ageMs > maxAgeMs)) continue;

				ChatMember.ActiveFrequencyInfo existing = newestPerBand.get(band);
				if (existing == null || info.timestampEpoch > existing.timestampEpoch) {
					newestPerBand.put(band, info);
				}
			}
		}

		StringBuilder result = new StringBuilder("Bands: ").append(
				availableBands.stream()
						.sorted()
						.map(this::bandToHumanLabel)
						.collect(java.util.stream.Collectors.joining(", "))
		);

		if (newestPerBand.isEmpty()) {
			return result.toString();
		}

		result.append(" | QRGs: ");
		boolean first = true;

		for (Band band : Band.values()) {
			ChatMember.ActiveFrequencyInfo info = newestPerBand.get(band);
			if (info == null) continue;

			if (!first) result.append(" | ");
			first = false;

			long ageMinutes = (System.currentTimeMillis() - info.timestampEpoch) / 60_000L;
			result.append(bandToHumanLabel(band))
					.append(" ")
					.append(String.format(Locale.US, "%.3f", info.frequency))
					.append(" MHz (")
					.append(ageMinutes)
					.append(" min ago)");
		}

		return result.toString();
	}

	/**
	 * Checks whether a station has known activity on the given band and that band is
	 * one of my own currently enabled bands, i.e. a new-band opportunity worth flagging
	 * with a star in that band's table cell.
	 *
	 * @param chatMember station row
	 * @param band       band to check
	 * @return true if the band cell should show a star
	 */
	private boolean isBandOfferForMainView(ChatMember chatMember, Band band) {
		if (chatMember == null || band == null || chatcontroller == null) {
			return false;
		}

		List<ChatMember> variants =
				chatcontroller.findActiveChatMembersByRawCall(chatMember.getCallSignRaw());
		if (variants.isEmpty()) {
			variants = List.of(chatMember);
		}

		BandOpportunityResolver.Resolution resolution =
				BandOpportunityResolver.resolve(variants, System.currentTimeMillis());
		EnumSet<Band> enabledBands =
				BandOpportunityResolver.getEnabledStationBands(chatcontroller.getChatPreferences());

		return resolution.getUnworkedEnabledBands(enabledBands).contains(band);
	}

	/**
	 * Checks whether the station's four-character grid square has already been worked
	 * specifically on the given band, regardless of which call was worked there. Mirrors
	 * the "o" semantics of the wkdAny column, just scoped to one band instead of any band.
	 * Can be turned off via
	 * {@link ChatPreferences#isGuiOptions_showGrossFieldWorkedHintInBandColumns()}.
	 *
	 * @param chatMember station row
	 * @param band       band to check
	 * @return true if the cell should show the "o" worked-grid-square hint
	 */
	private boolean isGrossFieldWorkedForBand(ChatMember chatMember, Band band) {
		if (chatMember == null || band == null || chatcontroller == null) {
			return false;
		}

		if (!chatcontroller.getChatPreferences().isGuiOptions_showGrossFieldWorkedHintInBandColumns()) {
			return false;
		}

		String qra = chatMember.getQra();
		if (qra == null || qra.isBlank()) {
			return false;
		}

		return chatcontroller.getWorkedGrossFieldCache().isGrossFieldWorked(band, qra);
	}

	/**
	 * Resolves the compact per-band status shown in the 144/432/23/13/9/6/3 table
	 * columns.
	 *
	 * <p>{@code X}/{@code a}/{@code B+}/(empty) describe the band-opportunity part:
	 * {@code X} if worked on this exact band, otherwise {@code a}/{@code B+} if the band
	 * is offered, enabled and unworked ({@code a} when the call has not been worked on
	 * any band yet, {@code B+} when it has), otherwise empty.</p>
	 *
	 * <p>{@code o} is an independent overlay, appended to whichever of the above applies,
	 * exactly like the {@code x}/{@code o}/{@code xo} combination in the wkdAny column:
	 * it marks that this band's grid square has already been worked, by any station.
	 * So e.g. {@code "a"}, {@code "ao"}, {@code "B+"}, {@code "B+o"} or just {@code "o"}
	 * can all appear.</p>
	 *
	 * @param chatMember     station row
	 * @param band           band this column represents
	 * @param workedThisBand true if the per-band worked flag for this exact band is set
	 * @return compact status text for the cell
	 */
	private String formatBandCellStatus(ChatMember chatMember, Band band, boolean workedThisBand) {
		String opportunity;

		boolean showFreshCallHint = chatcontroller != null
				&& chatcontroller.getChatPreferences().isGuiOptions_showFreshCallHintInBandColumns();

		if (workedThisBand) {
			opportunity = "X";
		} else if (isBandOfferForMainView(chatMember, band)) {
			opportunity = showFreshCallHint && chatMember != null && !chatMember.isWorked() ? "a" : "B+";
		} else {
			opportunity = "";
		}

		return isGrossFieldWorkedForBand(chatMember, band) ? opportunity + "o" : opportunity;
	}

	/**
	 * Builds the tooltip shown on a band-status cell: a fixed legend plus this row's
	 * resolved status for the given band.
	 */
	private String buildBandCellStatusTooltipText(ChatMember chatMember, Band band, String status) {
		StringBuilder tooltip = new StringBuilder("Band status:\n")
				.append("X = worked on this band\n")
				.append("B+ = band available, not worked on this band yet (call already worked on another band)\n")
				.append("a = band available, call not worked on any band yet (can be turned off in GUI settings; falls back to B+)\n")
				.append("o = grid square already worked on this band, by any station (can be turned off in GUI settings)\n")
				.append("(empty) = no information for this band\n")
				.append("o can combine with the others, e.g. \"ao\" or \"B+o\"");

		if (chatMember != null && band != null) {
			tooltip.append("\n\nThis station (").append(bandToHumanLabel(band)).append("):\n")
					.append("Status: ").append(status == null || status.isBlank() ? "-" : status);
		}

		return tooltip.toString();
	}

	/**
	 * Creates a shared cell factory for one band-status column, attaching the
	 * {@link #buildBandCellStatusTooltipText(ChatMember, Band, String)} tooltip.
	 */
	private Callback<TableColumn<ChatMember, String>, TableCell<ChatMember, String>> createBandStatusCellFactory(Band band) {
		return column -> new TruncatedTextTableCell<ChatMember>(
				java.util.function.Function.identity(),
				(member, status) -> buildBandCellStatusTooltipText(member, band, status)
		) {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);

				if (empty) {
					setStyle("");
					return;
				}
				setAlignment(Pos.CENTER);
				setStyle("-fx-font-weight: bold;");
			}
		};
	}

	private String bandToHumanLabel(kst4contest.model.Band b) {
		// Human-friendly labels for VHF/UHF/microwave contesting
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
			case B_24G -> "24G";
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


	/**
	 * This method generates a BoderPane which shows some additional information about a callsign which had been
	 * selected either: <br/>
	 * - at the userlist <br/>
	 * - at the CQ message list (senders callsign)<br/>
	 * - at the PM message list (senders callsign)<br/><br/>
	 * Method gets its information source out of the original chatmember object of the userlist, not a copy
	 *
	 * @param selectedCallSignInfoStageChatMember
	 * @return
	 */
	private BorderPane generateFurtherInfoAbtSelectedCallsignBP(ChatMember selectedCallSignInfoStageChatMember) {


		selectedCallSignInfoBorderPane = new BorderPane();

		SplitPane selectedCallSignSplitPane = new SplitPane();
		selectedCallSignSplitPane.setOrientation(Orientation.VERTICAL);
		selectedCallSignSplitPane.setDividerPositions(chatcontroller.getChatPreferences().getGUIselectedCallSignSplitPane_dividerposition());


		TableView<ChatMessage> initFurtherInfoAbtCallsignMSGTable = initFurtherInfoAbtCallsignMSGTable();

		Label selectedCallSignInfoLblQTFInfo = new Label("QTF:" + selectedCallSignInfoStageChatMember.getQTFdirection() + " deg");

		Label selectedCallSignInfoLblQRBInfo = new Label("QRB: " + selectedCallSignInfoStageChatMember.getQrb() + " km");



		GridPane selectedCallSignDownerSiteGridPane = new GridPane();
		selectedCallSignDownerSiteGridPane.setHgap(10);
		selectedCallSignDownerSiteGridPane.setVgap(2);
		selectedCallSignDownerSiteGridPane.add(selectedCallSignInfoLblQTFInfo, 0,0,1,1);
		selectedCallSignDownerSiteGridPane.add(selectedCallSignInfoLblQRBInfo, 0,1,1,1);
		selectedCallSignDownerSiteGridPane.add(new Label("Last activity: " + new Utils4KST().time_convertEpochToReadable(selectedCallSignInfoStageChatMember.getActivityTimeLastInEpoch()+"")), 0,2,1,1);
		selectedCallSignDownerSiteGridPane.add(new Label(("(" + Utils4KST.time_getSecondsBetweenEpochAndNow(selectedCallSignInfoStageChatMember.getActivityTimeLastInEpoch()+"") /60%60) +" min ago)"), 0,3,1,1);

		// Show detected RX bands based on frequency recognition in chat history.
		// Default: last 30 minutes (same horizon as Smart Parser history usage)
		Label lblDetectedRxBands = new Label(
				formatDetectedRxBandsForCallsignRaw(selectedCallSignInfoStageChatMember.getCallSignRaw(), 30L * 60L * 1000L)
			);
		lblDetectedRxBands.setWrapText(true);
		lblDetectedRxBands.setTooltip(new Tooltip(
				"Bands are derived from recent QRG detections and the station name. "
						+ "Manual NOT-QRV tags override automatic hints."
		));
		selectedCallSignDownerSiteGridPane.add(lblDetectedRxBands, 0, 4, 1, 1);


		Label selectedCallSignInfoLblPriorityScore = new Label();
		attachPriorityScoreFollower(selectedCallSignInfoLblPriorityScore);
//		selectedCallSignDownerSiteGridPane.add(selectedCallSignInfoLblPriorityScore, 0,5,1,1);

		Button btnSkedFail = new Button("Sked fail");
		btnSkedFail.setTooltip(new Tooltip("Marks the path as failed (permanent until reset). Strongly reduces priority score."));
		btnSkedFail.setOnAction(e -> {
			ChatMember sel = chatcontroller.getScoreService().selectedChatMember().get();
			if (sel == null) return;
			chatcontroller.getStationMetricsService().markManualSkedFail(sel.getCallSignRaw());
			chatcontroller.getScoreService().requestRecompute("manual-sked-fail");
		});

		Button btnSkedFailReset = new Button("Reset fail");
		btnSkedFailReset.setTooltip(new Tooltip("Resets the manual sked-fail flag for this station."));
		btnSkedFailReset.setOnAction(e -> {
			ChatMember sel = chatcontroller.getScoreService().selectedChatMember().get();
			if (sel == null) return;
			chatcontroller.getStationMetricsService().resetManualSkedFail(sel.getCallSignRaw());
			chatcontroller.getScoreService().requestRecompute("manual-sked-fail-reset");
		});

		HBox priorityRow = new HBox(8, selectedCallSignInfoLblPriorityScore, btnSkedFail, btnSkedFailReset);
		priorityRow.setAlignment(Pos.CENTER_LEFT);

		selectedCallSignDownerSiteGridPane.add(priorityRow, 0, 5, 1, 1);

		ChoiceBox<Integer> cbSkedMinutes = new ChoiceBox<>(
				FXCollections.observableArrayList(
						2, 3, 4, 5, 6, 7, 8, 9, 10,
						11, 12, 13, 14, 15, 20
				)
		);
		cbSkedMinutes.getSelectionModel().select(Integer.valueOf(5));

		EnumSet<Band> enabledSkedBands =
				BandOpportunityResolver.getEnabledStationBands(
						chatcontroller.getChatPreferences()
				);

		ChoiceBox<Band> cbSkedBand = new ChoiceBox<>(
				FXCollections.observableArrayList(enabledSkedBands)
		);

		cbSkedBand.setConverter(new StringConverter<>() {
			@Override
			public String toString(Band band) {
				return band == null ? "" : bandToHumanLabel(band);
			}

			@Override
			public Band fromString(String value) {
				return null;
			}
		});

		cbSkedBand.setPrefWidth(75);
		cbSkedBand.setTooltip(new Tooltip(
				"Band for this sked. The initial value is derived from recent "
						+ "QRG information or the station name. Only bands enabled "
						+ "for your own station are offered."
		));

		Band defaultSkedBand =
				resolveDefaultSkedBand(
						selectedCallSignInfoStageChatMember,
						enabledSkedBands
				);

		if (defaultSkedBand != null) {
			cbSkedBand.setValue(defaultSkedBand);
		}

		ChoiceBox<String> cbSkedMode = new ChoiceBox<>(
				FXCollections.observableArrayList("SSB", "CW")
		);

		String configuredSkedMode =
				chatcontroller.getChatPreferences()
						.getLogsynch_wintestSkedMode();

		if (configuredSkedMode == null
				|| (!"SSB".equalsIgnoreCase(configuredSkedMode)
				&& !"CW".equalsIgnoreCase(configuredSkedMode))) {

			configuredSkedMode = "SSB";
		}

		cbSkedMode.setValue(
				configuredSkedMode.trim().toUpperCase(Locale.ROOT)
		);

		cbSkedMode.setTooltip(new Tooltip(
				"Mode transferred to Win-Test with the ADDSKED packet"
		));

		cbSkedMode.setOnAction(e ->
				chatcontroller.getChatPreferences()
						.setLogsynch_wintestSkedMode(
								cbSkedMode.getValue()
						)
		);

		ChoiceBox<String> cbReminderOffsets = new ChoiceBox<>(
				FXCollections.observableArrayList(
						"2+1",
						"5+2+1",
						"10+5+2+1"
				)
		);
		cbReminderOffsets.getSelectionModel().select("2+1");

		CheckBox chkPmReminders = new CheckBox("Remind-PM in ");

		Button btnCreateSked = new Button("Create sked");
		btnCreateSked.setTooltip(new Tooltip(
				"Creates a sked entry and boosts priority during the approach."
		));

		btnCreateSked.setOnAction(e -> {
			ChatMember selectedMember =
					chatcontroller.getScoreService()
							.selectedChatMember()
							.get();

			if (selectedMember == null) {
				return;
			}

			Band selectedBand = cbSkedBand.getValue();
			if (selectedBand == null) {
				showUserInputErrorWindow(
						"No sked band is available. Enable at least one band "
								+ "under \"My station uses ...\" before creating a sked."
				);
				return;
			}

			if (cbSkedMode.getValue() != null) {
				chatcontroller.getChatPreferences()
						.setLogsynch_wintestSkedMode(cbSkedMode.getValue());
			}

			int minutes =
					cbSkedMinutes.getValue() == null
							? 5
							: cbSkedMinutes.getValue();

			long skedTime =
					System.currentTimeMillis() + minutes * 60_000L;

			double azimuth =
					selectedMember.getQTFdirection() != null
							? selectedMember.getQTFdirection()
							: 0.0;

			ContestSked sked = new ContestSked(
					selectedMember.getCallSignRaw(),
					selectedMember.getCallSign(),
					selectedMember.getChatCategory(),
					azimuth,
					skedTime,
					selectedBand
			);

			chatcontroller.addSked(sked);
			chatcontroller.getScoreService()
					.requestRecompute("sked-created");

			if (chkPmReminders.isSelected()) {
				List<Integer> offsets =
						parseMinuteOffsets(cbReminderOffsets.getValue());

				chatcontroller.getSkedReminderService().armReminders(
						sked.getTargetChatCallsign(),
						sked.getTargetChatCategory(),
						skedTime,
						offsets
				);
			}
		});

		HBox skedTimeGroup = new HBox(
				4,
				new Label("Sked in"),
				cbSkedMinutes,
				new Label("min")
		);
		skedTimeGroup.setAlignment(Pos.CENTER_LEFT);

		HBox skedBandGroup = new HBox(
				4,
				new Label("Band"),
				cbSkedBand
		);
		skedBandGroup.setAlignment(Pos.CENTER_LEFT);

		HBox skedModeGroup = new HBox(
				4,
				new Label("Mode"),
				cbSkedMode
		);
		skedModeGroup.setAlignment(Pos.CENTER_LEFT);

		HBox skedReminderGroup = new HBox(
				4,
				chkPmReminders,
				cbReminderOffsets
		);
		skedReminderGroup.setAlignment(Pos.CENTER_LEFT);

		FlowPane skedRow = new FlowPane(
				Orientation.HORIZONTAL,
				10,
				5
		);
		skedRow.setAlignment(Pos.CENTER_LEFT);
		skedRow.getChildren().addAll(
				skedTimeGroup,
				skedBandGroup,
				skedModeGroup,
				btnCreateSked,
				skedReminderGroup
		);

		selectedCallSignDownerSiteGridPane.add(
				skedRow,
				0,
				6,
				2,
				1
		);
		GridPane.setHgrow(skedRow, Priority.ALWAYS);



		Label selectedCallSignChatCategoryLabelDesc = new Label(selectedCallSignInfoStageChatMember.getCallSign() + " in chatcategory: " + selectedCallSignInfoStageChatMember.getChatCategory().getChatCategoryName(selectedCallSignInfoStageChatMember.getChatCategory().getCategoryNumber()));

		selectedCallSignChatCategoryLabelDesc.getStyleClass().clear();
		selectedCallSignChatCategoryLabelDesc.getStyleClass().add("label");
		selectedCallSignChatCategoryLabelDesc.getStyleClass().add("label-callSignChatCatDescriptor");

		selectedCallSignChatCategoryLabelDesc.setAlignment(Pos.CENTER);


		selectedCallSignDownerSiteGridPane.add(selectedCallSignChatCategoryLabelDesc, 1,4,1,3);
//		GridPane.setHalignment(selectedCallSignDownerSiteGridPane, HPos.CENTER);

        // * users qrv info setting will follow here

		CheckBox furtherInfoPnl_chkbx_notQRV50 = new CheckBox("tag not qrv 50");
		furtherInfoPnl_chkbx_notQRV50.setSelected(!selectedCallSignInfoStageChatMember.isQrv50());
		furtherInfoPnl_chkbx_notQRV50.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv50(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv50(false);
				}

				try {

					chatcontroller.propagateNotQrvStateToActiveMembers(selectedCallSignInfoStageChatMember);
					chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);
					GuiUtils.triggerGUIFilteredChatMemberListChange(chatcontroller);
				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		CheckBox furtherInfoPnl_chkbx_notQRV70 = new CheckBox("tag not qrv 70");
		furtherInfoPnl_chkbx_notQRV70.setSelected(!selectedCallSignInfoStageChatMember.isQrv70());
		furtherInfoPnl_chkbx_notQRV70.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv70(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv70(false);
				}

				try {

					chatcontroller.propagateNotQrvStateToActiveMembers(selectedCallSignInfoStageChatMember);
					chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);
					GuiUtils.triggerGUIFilteredChatMemberListChange(chatcontroller);
				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		CheckBox furtherInfoPnl_chkbx_notQRV144 = new CheckBox("tag not qrv 144");
		furtherInfoPnl_chkbx_notQRV144.setSelected(!selectedCallSignInfoStageChatMember.isQrv144());
		furtherInfoPnl_chkbx_notQRV144.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv144(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv144(false);
				}
				try {

				chatcontroller.propagateNotQrvStateToActiveMembers(selectedCallSignInfoStageChatMember);
				chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);

				GuiUtils.triggerGUIFilteredChatMemberListChange(chatcontroller);

				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		CheckBox furtherInfoPnl_chkbx_notQRV432 = new CheckBox("tag not qrv 432");
		furtherInfoPnl_chkbx_notQRV432.setSelected(!selectedCallSignInfoStageChatMember.isQrv432());
		furtherInfoPnl_chkbx_notQRV432.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv432(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv432(false);
				}

				try {

					chatcontroller.propagateNotQrvStateToActiveMembers(selectedCallSignInfoStageChatMember);
					chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);
					GuiUtils.triggerGUIFilteredChatMemberListChange(chatcontroller);
				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		CheckBox furtherInfoPnl_chkbx_notQRV23 = new CheckBox("tag not qrv 23cm");
		furtherInfoPnl_chkbx_notQRV23.setSelected(!selectedCallSignInfoStageChatMember.isQrv1240());
		furtherInfoPnl_chkbx_notQRV23.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv1240(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv1240(false);
				}

				try {

					chatcontroller.propagateNotQrvStateToActiveMembers(selectedCallSignInfoStageChatMember);
					chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);
					GuiUtils.triggerGUIFilteredChatMemberListChange(chatcontroller);
				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		CheckBox furtherInfoPnl_chkbx_notQRV13 = new CheckBox("tag not qrv 13cm");
		furtherInfoPnl_chkbx_notQRV13.setSelected(!selectedCallSignInfoStageChatMember.isQrv2300());
		furtherInfoPnl_chkbx_notQRV13.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv2300(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv2300(false);
				}

				try {

					chatcontroller.propagateNotQrvStateToActiveMembers(selectedCallSignInfoStageChatMember);
					chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);
					GuiUtils.triggerGUIFilteredChatMemberListChange(chatcontroller);
				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		CheckBox furtherInfoPnl_chkbx_notQRV9 = new CheckBox("tag not qrv 9cm");
		furtherInfoPnl_chkbx_notQRV9.setSelected(!selectedCallSignInfoStageChatMember.isQrv3400());
		furtherInfoPnl_chkbx_notQRV9.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv3400(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv3400(false);
				}

				try {

					chatcontroller.propagateNotQrvStateToActiveMembers(selectedCallSignInfoStageChatMember);
					chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);
					GuiUtils.triggerGUIFilteredChatMemberListChange(chatcontroller);
				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		CheckBox furtherInfoPnl_chkbx_notQRV6 = new CheckBox("tag not qrv 6cm");
		furtherInfoPnl_chkbx_notQRV6.setSelected(!selectedCallSignInfoStageChatMember.isQrv5600());
		furtherInfoPnl_chkbx_notQRV6.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv5600(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv5600(false);

				}

				try {

					chatcontroller.propagateNotQrvStateToActiveMembers(selectedCallSignInfoStageChatMember);
					chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);
					GuiUtils.triggerGUIFilteredChatMemberListChange(chatcontroller);
				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		CheckBox furtherInfoPnl_chkbx_notQRV3 = new CheckBox("tag not qrv 3cm");
		furtherInfoPnl_chkbx_notQRV3.setSelected(!selectedCallSignInfoStageChatMember.isQrv10G());
		furtherInfoPnl_chkbx_notQRV3.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv10G(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv10G(false);
				}

				try {

					chatcontroller.propagateNotQrvStateToActiveMembers(selectedCallSignInfoStageChatMember);
					chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);
					GuiUtils.triggerGUIFilteredChatMemberListChange(chatcontroller);
				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		CheckBox furtherInfoPnl_chkbx_notQRVall = new CheckBox("tag not qrv all");
		furtherInfoPnl_chkbx_notQRVall.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv50(true);
					selectedCallSignInfoStageChatMember.setQrv70(true);
					selectedCallSignInfoStageChatMember.setQrv144(true);
					selectedCallSignInfoStageChatMember.setQrv432(true);
					selectedCallSignInfoStageChatMember.setQrv1240(true);
					selectedCallSignInfoStageChatMember.setQrv2300(true);
					selectedCallSignInfoStageChatMember.setQrv3400(true);
					selectedCallSignInfoStageChatMember.setQrv5600(true);
					selectedCallSignInfoStageChatMember.setQrv10G(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv50(false);
					selectedCallSignInfoStageChatMember.setQrv70(false);
					selectedCallSignInfoStageChatMember.setQrv144(false);
					selectedCallSignInfoStageChatMember.setQrv432(false);
					selectedCallSignInfoStageChatMember.setQrv1240(false);
					selectedCallSignInfoStageChatMember.setQrv2300(false);
					selectedCallSignInfoStageChatMember.setQrv3400(false);
					selectedCallSignInfoStageChatMember.setQrv5600(false);
					selectedCallSignInfoStageChatMember.setQrv10G(false);
				}

				try {

					chatcontroller.propagateNotQrvStateToActiveMembers(selectedCallSignInfoStageChatMember);
					chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);
					GuiUtils.triggerGUIFilteredChatMemberListChange(chatcontroller);
				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		selectedCallSignDownerSiteGridPane.add(furtherInfoPnl_chkbx_notQRV144, 2,0,1,1);
		selectedCallSignDownerSiteGridPane.add(furtherInfoPnl_chkbx_notQRV432, 2,1,1,1);
		selectedCallSignDownerSiteGridPane.add(furtherInfoPnl_chkbx_notQRV23, 2,2,1,1);
		selectedCallSignDownerSiteGridPane.add(furtherInfoPnl_chkbx_notQRV13, 2,3,1,1);
		selectedCallSignDownerSiteGridPane.add(furtherInfoPnl_chkbx_notQRV9, 3,0,1,1);
		selectedCallSignDownerSiteGridPane.add(furtherInfoPnl_chkbx_notQRV6, 3,1,1,1);
		selectedCallSignDownerSiteGridPane.add(furtherInfoPnl_chkbx_notQRV3, 3,2,1,1);
		selectedCallSignDownerSiteGridPane.add(furtherInfoPnl_chkbx_notQRVall, 3,3,1,1);

		if (!chatcontroller.getChatPreferences().isStn_bandActive50()) {
			furtherInfoPnl_chkbx_notQRV50.setVisible(false);
		}
		if (!chatcontroller.getChatPreferences().isStn_bandActive70()) {
			furtherInfoPnl_chkbx_notQRV70.setVisible(false);
		}
		if (!chatcontroller.getChatPreferences().isStn_bandActive144()) {
			furtherInfoPnl_chkbx_notQRV144.setVisible(false);
		}
		if (!chatcontroller.getChatPreferences().isStn_bandActive432()) {
			furtherInfoPnl_chkbx_notQRV432.setVisible(false);
		}

		if (!chatcontroller.getChatPreferences().isStn_bandActive1240()) {
			furtherInfoPnl_chkbx_notQRV23.setVisible(false);
		}
		if (!chatcontroller.getChatPreferences().isStn_bandActive2300()) {
			furtherInfoPnl_chkbx_notQRV13.setVisible(false);
		}
		if (!chatcontroller.getChatPreferences().isStn_bandActive3400()) {
			furtherInfoPnl_chkbx_notQRV9.setVisible(false);
		}
		if (!chatcontroller.getChatPreferences().isStn_bandActive5600()) {
			furtherInfoPnl_chkbx_notQRV6.setVisible(false);
		}
		if (!chatcontroller.getChatPreferences().isStn_bandActive10G()) {
			furtherInfoPnl_chkbx_notQRV3.setVisible(false);
		}




		/**
		 * users qrv info setting ending
		 */

		Button selectedCallSignShowAsPathBtn = new Button("Show path in AS");
		selectedCallSignShowAsPathBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				chatcontroller.airScout_SendAsShowPathPacket(selectedCallSignInfoStageChatMember);
			}
		});
		selectedCallSignShowAsPathBtn.setGraphic(createArrow(selectedCallSignInfoStageChatMember.getQTFdirection()));

		Button selectedCallSignShowOnMapBtn = new Button("\uD83D\uDDFA Show on map");
		selectedCallSignShowOnMapBtn.setTooltip(new Tooltip("Show selected station on map"));

		selectedCallSignShowOnMapBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				showSelectedCallsignOnMap();
			}
		});
		selectedCallSignShowOnMapBtn.setTooltip(new Tooltip("Show the selected station on the map"));


        Button selectedCallSignTurnAntBtn = new Button("Turn ant1 to " + selectedCallSignInfoStageChatMember.getCallSignRaw());
        selectedCallSignTurnAntBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                chatcontroller.rotateTo(selectedCallSignInfoStageChatMember.getQTFdirection());

                //TODO: Hier muss was hin
            }
        });
        selectedCallSignTurnAntBtn.setGraphic(createArrow(selectedCallSignInfoStageChatMember.getQTFdirection()));

		Button selectedCallSignShowQRZprofile = new Button("Lookup on qrz.com");
		selectedCallSignShowQRZprofile.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				getHostServices().showDocument("https://www.qrz.com/db/" + selectedCallSignInfoStageChatMember.getCallSign());
			}
		});

		Button selectedCallSignShowQRZCqprofile = new Button("Lookup on qrzcq.com");
		selectedCallSignShowQRZCqprofile.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				getHostServices().showDocument("https://www.qrzcq.com/call/" + selectedCallSignInfoStageChatMember.getCallSign());
			}
		});

//		selectedCallSignDownerSiteGridPane.add(selectedCallSignShowAsPathBtn, 1,0,1,1);

		HBox selectedCallSignPathAndMapButtons = new HBox(10, selectedCallSignShowAsPathBtn, selectedCallSignShowOnMapBtn);
		selectedCallSignDownerSiteGridPane.add(selectedCallSignPathAndMapButtons, 1,0,1,1);

        selectedCallSignDownerSiteGridPane.add(selectedCallSignTurnAntBtn, 1,1,1,1);

		selectedCallSignDownerSiteGridPane.add(selectedCallSignShowQRZprofile, 1,2,1,1);
		selectedCallSignDownerSiteGridPane.add(selectedCallSignShowQRZCqprofile, 1,3,1,1);


		/*
		 * The old GridPane layout above is intentionally left in place because it
		 * creates all controls, bindings and event handlers in the established order.
		 * From here on we only change the presentation: clear the temporary GridPane
		 * and reassemble the same controls in a compact VBox/FlowPane layout.
		 */
		selectedCallSignDownerSiteGridPane.getChildren().clear();

		VBox selectedCallSignCompactControlsPane = initSelectedCallSignCompactControlsPane(
				selectedCallSignInfoStageChatMember,
				selectedCallSignChatCategoryLabelDesc,
				selectedCallSignInfoLblQTFInfo,
				selectedCallSignInfoLblQRBInfo,
				lblDetectedRxBands,
				priorityRow,
				skedRow,
				selectedCallSignPathAndMapButtons,
				selectedCallSignTurnAntBtn,
				selectedCallSignShowQRZprofile,
				selectedCallSignShowQRZCqprofile,
				furtherInfoPnl_chkbx_notQRV50,
				furtherInfoPnl_chkbx_notQRV70,
				furtherInfoPnl_chkbx_notQRV144,
				furtherInfoPnl_chkbx_notQRV432,
				furtherInfoPnl_chkbx_notQRV23,
				furtherInfoPnl_chkbx_notQRV13,
				furtherInfoPnl_chkbx_notQRV9,
				furtherInfoPnl_chkbx_notQRV6,
				furtherInfoPnl_chkbx_notQRV3,
				furtherInfoPnl_chkbx_notQRVall
		);

		selectedCallSignSplitPane.getItems().add(initFurtherInfoAbtCallsignMSGTable);
		selectedCallSignSplitPane.getItems().add(selectedCallSignCompactControlsPane);



//		selectedCallSignSplitPane.getItems().add(initFurtherInfoAbtCallsignMSGTable);
//		selectedCallSignSplitPane.getItems().add(selectedCallSignDownerSiteGridPane);

		//first initialize how much divider positions we need...
//		chatcontroller.getChatPreferences().setGUIselectedCallSignSplitPane_dividerposition(new double[selectedCallSignSplitPane.getDividers().size()]);
		/**
		 * Then add change listeners to the dividers to save their state
		 */
		for (SplitPane.Divider divider : selectedCallSignSplitPane.getDividers()) {
			divider.positionProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observableValue, Number oldDividerPos, Number newDividerPosition) {
//					System.out.println("<<<<<<<<<<<<<<<<<<< devider " + selectedCallSignSplitPane.getDividers().indexOf(divider)  + " position change, new position: " + newDividerPosition + " // size dev: " +  selectedCallSignSplitPane.getDividers().size());
					chatcontroller.getChatPreferences().getGUIselectedCallSignSplitPane_dividerposition()[selectedCallSignSplitPane.getDividers().indexOf(divider)] = newDividerPosition.doubleValue();
					requestLayoutSave();
				}
			});

		}


		selectedCallSignInfoBorderPane.setCenter(selectedCallSignSplitPane);

		HBox selectedCallSignInfoBottomControlsBox = new HBox();
		selectedCallSignInfoBottomControlsBox.setSpacing(10);
//		selectedCallSignInfoBottomControlsBox.getChildren().add(new CheckBox("Always on top"));

		ToggleGroup selectedCallSignInfoFilterMessagesRadioGrp = new ToggleGroup();
		RadioButton selectedCallSignFilterToMeMsgRB = new RadioButton("pm to me ");
//		selectedCallSignFilterToMeMsgRB.setSelected(true);
		selectedCallSignFilterToMeMsgRB.setToggleGroup(selectedCallSignInfoFilterMessagesRadioGrp);
		RadioButton selectedCallSignFilterMsgToOtherRB = new RadioButton("pm to other");
		selectedCallSignFilterMsgToOtherRB.setToggleGroup(selectedCallSignInfoFilterMessagesRadioGrp);
		RadioButton selectedCallSignFilterMsgpublic = new RadioButton("public msgs");
		selectedCallSignFilterMsgpublic.setToggleGroup(selectedCallSignInfoFilterMessagesRadioGrp);
		RadioButton selectedCallSignNoFilterRB = new RadioButton("nothing");
		selectedCallSignNoFilterRB.setToggleGroup(selectedCallSignInfoFilterMessagesRadioGrp);


		selectedCallSignInfoFilterMessagesRadioGrp.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			@Override
			public void changed(ObservableValue<? extends Toggle> observableValue, Toggle toggle, Toggle t1) {

				RadioButton radioButton = (RadioButton) selectedCallSignInfoFilterMessagesRadioGrp.getSelectedToggle();

				if (radioButton.equals(selectedCallSignFilterToMeMsgRB)) {
					applySelectedCallSignInfoFilter(new Predicate<ChatMessage>() {
						@Override
						public boolean test(ChatMessage chatMessage) {

							try {

                                //message is directed to all and I am not mentioned
                                if (chatMessage.getReceiver().getCallSign().equals("ALL") && !(chatMessage.getMessageText().toLowerCase().contains(chatcontroller.getChatPreferences().getStn_loginCallSign().toLowerCase()))) {
                                    return false;
                                }

								if (((chatMessage.getReceiver().getCallSign().equals(chatcontroller.getChatPreferences().getStn_loginCallSign())) || (chatMessage.getSender().getCallSign().equals(chatcontroller.getChatPreferences().getStn_loginCallSign()))
								) && ((chatMessage.getReceiver().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) || (chatMessage.getSender().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())))) {
									return true;
								}

								else return false; //TODO old version before 1.26
//								if (((chatMessage.getReceiver().getCallSign().equals(chatcontroller.getChatPreferences().getLoginCallSign())) || (chatMessage.getSender().getCallSign().equals(chatcontroller.getChatPreferences().getLoginCallSign()))
//								) && (chatMessage.getReceiver() == (selectedCallSignInfoStageChatMember) || (chatMessage.getSender() == (selectedCallSignInfoStageChatMember)))) {
//									return true;
//								}

//								else return false;
							} catch (Exception exception) {
								System.out.println("KST4ContestApp <<<catched error>>> " + exception.getMessage());
							}

							return true;
						}
					});

//					System.out.println(t1 + " filter to me was selected <<<<<<<<<<<<<<<<<<<");
				} else if (radioButton.equals(selectedCallSignFilterMsgToOtherRB)) {

					applySelectedCallSignInfoFilter(new Predicate<ChatMessage>() {
						@Override
						public boolean test(ChatMessage chatMessage) {

							try {

								if ((chatMessage.getSender().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) && (!chatMessage.getReceiver().getCallSign().equals("ALL")) && (!chatMessage.getReceiver().getCallSign().equals(chatcontroller.getChatPreferences().getStn_loginCallSign()))) {
									return true;
								} else if ((chatMessage.getReceiver().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) && (!chatMessage.getReceiver().getCallSign().equals("ALL")) && (!chatMessage.getReceiver().getCallSign().equals(chatcontroller.getChatPreferences().getStn_loginCallSign()))) {
									return true;
								} else return false;
							} catch (NullPointerException SenderNull) {
//								System.out.println("KST4ContestApp, <<<catched error>>>: Sender/receiver of the message is unknown, categorizing is impossible: " + SenderNull.getMessage());

								return false;
							}

						}
					});

					System.out.println(t1 + " filter to other was selected <<<<<<<<<<<<<<<<<<<");
				} else if (radioButton.equals(selectedCallSignFilterMsgpublic)) {

					applySelectedCallSignInfoFilter(new Predicate<ChatMessage>() {
						@Override
						public boolean test(ChatMessage chatMessage) {

							try {

								if ((chatMessage.getSender().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) && (chatMessage.getReceiver().getCallSign().equals("ALL"))) {
									return true;
								}
									else return false;

							} catch (NullPointerException SenderNull) {
								System.out.println("KST4ContestApp, <<<catched error>>>: Sender of the message is unknown, categorizing is impossible");

								return false;
							}

						}
					});


					System.out.println(t1 + " filter to public was selected <<<<<<<<<<<<<<<<<<<");
				} else {
					System.out.println(t1 + " no filter was selected <<<<<<<<<<<<<<<<<<<");
					applySelectedCallSignInfoFilter(new Predicate<ChatMessage>() {
						@Override
						public boolean test(ChatMessage chatMessage) {

							try {

								if ((chatMessage.getSender().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) ||
										chatMessage.getReceiver().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) {
									return true;
								}

								else return false;
							} catch (NullPointerException SenderNull) {
								System.out.println("KST4ContestApp, <<<catched error>>>: Sender/receiver of the message is unknown, categorizing is impossible");

								return false;
							}
						}
					});
				}
			}
		});

		selectedCallSignInfoBottomControlsBox.getChildren().add(new Label("Messages of " + selectedCallSignInfoStageChatMember.getCallSign() + " -> Filter:  "));
		selectedCallSignInfoBottomControlsBox.getChildren().add(selectedCallSignNoFilterRB);
		selectedCallSignInfoBottomControlsBox.getChildren().add(selectedCallSignFilterToMeMsgRB);
		selectedCallSignInfoBottomControlsBox.getChildren().add(selectedCallSignFilterMsgToOtherRB);
		selectedCallSignInfoBottomControlsBox.getChildren().add(selectedCallSignFilterMsgpublic);

//		selectedCallSignInfoBottomControlsBox.getChildren().add(new CheckBox("Filter messages to me"));
//		selectedCallSignInfoBottomControlsBox.getChildren().add(new CheckBox("Filter messages to Other"));
		selectedCallSignInfoBorderPane.setTop(selectedCallSignInfoBottomControlsBox);

		applySelectedCallSignInfoFilter(new Predicate<ChatMessage>() {
			/**
			 * This is the filter "nothing" option. It will get all communication of a callsign to all directions
			 *
			 * @param chatMessage the input argument
			 * @return
			 */
			@Override
			public boolean test(ChatMessage chatMessage) {

				try {
					if ((chatMessage.getSender().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) ||
							chatMessage.getReceiver().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) {
						return true;
					} else return false;

				} catch (Exception exception) {
//					System.out.println("KST4ContestApplication <<<catched ERROR>>>>: cant get sender infos due to sender is not known yet" + exception.getMessage());
				 return false;
				}
			}
		});

//		selectedCallSignNoFilterRB.setSelected(true);
		selectedCallSignNoFilterRB.setSelected(chatcontroller.getChatPreferences().isGuiOptions_defaultFilterNothing()); //default options reading
		selectedCallSignFilterMsgpublic.setSelected(chatcontroller.getChatPreferences().isGuiOptions_defaultFilterPublicMsgs()); //default options reading
		selectedCallSignFilterToMeMsgRB.setSelected(chatcontroller.getChatPreferences().isGuiOptions_defaultFilterPmToMe()); //default options reading
		selectedCallSignFilterMsgToOtherRB.setSelected(chatcontroller.getChatPreferences().isGuiOptions_defaultFilterPmToOther()); //default options reading

		return selectedCallSignInfoBorderPane;

	}


	/**
	 * Applies the currently selected station filters to the ChatMember FilteredList.
	 *
	 * This replaces the old predicate-property binding / forced refresh mechanics.
	 * The important point is that the TableView should only be invalidated when the
	 * effective filter really changed. Otherwise JavaFX may emit selection events
	 * during normal periodic updates although the operator did not click another
	 * station.
	 *
	 * <p>Regardless of whether the invalidation below was really necessary,
	 * re-evaluating the FilteredList predicate can make the TableView emit a
	 * selection-changed event for the still-selected station (e.g. because the
	 * FilteredList/SortedList rebuild produces a new ChatMember instance for the
	 * same callsign). This happens synchronously for every caller of this method,
	 * including the callsign search field's textProperty listener on every single
	 * keystroke. Without a guard, that spurious event reaches the table-selection
	 * listener and re-prepares/re-focuses the send text field, yanking keyboard
	 * focus away from wherever the operator currently is. Guard it the same way
	 * focusChatMemberAndPrepareCq guards its own programmatic selection.</p>
	 */
	private void applyChatMemberFilterPredicates() {
		if (chatcontroller == null
				|| chatMemberListBinding == null
				|| chatcontroller.getLst_chatMemberListFilterPredicates() == null) {
			return;
		}

		Predicate<ChatMember> combinedPredicate;

		if (chatcontroller.getLst_chatMemberListFilterPredicates().snapshot().isEmpty()) {
			combinedPredicate = SHOW_ALL_CHAT_MEMBER_PREDICATE;
		} else {
			combinedPredicate = chatcontroller.getLst_chatMemberListFilterPredicates()
					.snapshot()
					.stream()
					.reduce(SHOW_ALL_CHAT_MEMBER_PREDICATE, Predicate::and);
		}

		/*
		 * Do not reset the same show-all predicate over and over again.
		 *
		 * The periodic priority/user-list refresh calls this method regularly. If the
		 * visible station list is rebuilt without real filter changes, the TableView
		 * selection model may emit another selection event and the send-text field can
		 * be prepared again.
		 */
		if (combinedPredicate == lastAppliedChatMemberFilterPredicate) {
			return;
		}

		lastAppliedChatMemberFilterPredicate = combinedPredicate;

		boolean previousProgrammaticFlag = programmaticChatMemberSelectionChange;
		programmaticChatMemberSelectionChange = true;
		try {
			chatMemberListBinding.refresh();
		} finally {
			programmaticChatMemberSelectionChange = previousProgrammaticFlag;
		}
	}


	/**
	 * Helper method for furtherinfoPane
	 * @param s
	 * @return
	 */
	private static List<Integer> parseMinuteOffsets(String s) {
		if (s == null || s.isBlank()) return List.of();
		String[] parts = s.split("\\+");
		List<Integer> out = new ArrayList<>();
		for (String p : parts) {
			try {
				out.add(Integer.parseInt(p.trim()));
			} catch (Exception ignore) {}
		}
		return out;
	}


	private TableView<ChatMember> initChatMemberTable() {

		TableView<ChatMember> tbl_chatMemberTable = new TableView<ChatMember>();
		tbl_chatMemberTable.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {

				//we need to overdrive the Enter pressed as it should (in the whole scene) send the text!
				if (event.getCode() == KeyCode.ENTER) {

					event.consume();
					sendButton.fire();
				}

			}
		});

		TableColumn<ChatMember, String> callSignCol =
				new TableColumn<ChatMember, String>("Callsign");

		callSignCol.setCellValueFactory(cellDataFeatures -> {
			ChatMember member = cellDataFeatures.getValue();

			if (member == null || member.getCallSign() == null) {
				return new SimpleStringProperty("");
			}

			String displayedCallsign = member.getState() == 1
					? "(" + member.getCallSign() + ")"
					: member.getCallSign();

			return new SimpleStringProperty(displayedCallsign);
		});

		callSignCol.setCellFactory(column -> new TruncatedTextTableCell<ChatMember>() {

			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);

				/*
				 * JavaFX reuses TableCell instances while the user scrolls, sorts or
				 * filters the table. Every visual state must therefore be reset before
				 * the cell is populated with another ChatMember.
				 */
				getStyleClass().remove("table-cell-inAngleAndRange");
				setGraphic(null);

				if (empty || item == null) {
					setText(null);
					setStyle("");
					return;
				}

				setText(item);

				ChatMember chatMember = getTableRow() == null
						? null
						: getTableRow().getItem();

				if (chatMember == null) {
					setStyle("");
					return;
				}

				boolean useBoldFont = chatMember.getState() == 2
						|| chatMember.getState() == 3;

				if (chatMember.isInAngleAndRange()) {
					getStyleClass().add("table-cell-inAngleAndRange");
					useBoldFont = true;
				}

				setStyle(useBoldFont
						? "-fx-font-weight: bold;"
						: "-fx-font-weight: normal;");
			}
		});

		callSignCol.setSortType(TableColumn.SortType.ASCENDING);
		tbl_chatMemberTable.getSortOrder().add(callSignCol);

		TableColumn<ChatMember, String> nameCol = new TableColumn<ChatMember, String>("Name");
		nameCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty name = new SimpleStringProperty();

				name.setValue(cellDataFeatures.getValue().getName());

				return name;
			}
		});

		TableColumn<ChatMember, String> qraCol = new TableColumn<ChatMember, String>("QRA");
		qraCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty qra = new SimpleStringProperty();

				qra.setValue(cellDataFeatures.getValue().getQra());

				return qra;
			}
		});

		/**
		 * Optionally highlights already worked grid squares in the QRA column.
		 *
		 * <p>The default table design is preserved as much as possible:
		 * <ul>
		 *     <li>If grid coloring is disabled, the cell uses the standard JavaFX style.</li>
		 *     <li>If the grid is not worked yet, the cell also uses the standard style.</li>
		 *     <li>Only already worked grids get a subtle background color.</li>
		 *     <li>Selected rows keep the normal table selection style.</li>
		 * </ul>
		 *
		 * <p>No font weight is changed here. The compact worked/grid status such as
		 * {@code xo} is emphasized only in the worked-any column.</p>
		 */
		qraCol.setCellFactory(column -> new TruncatedTextTableCell<ChatMember>(
				java.util.function.Function.identity(),
				(member, value) -> {
					String grossField = WorkedGrossFieldCache.extractGrossField(value);
					boolean gridWorked = member != null
							&& chatcontroller != null
							&& chatcontroller.isGridSquareWorkedAny(member);
					return "Grid status: "
							+ (grossField == null ? "unknown" : grossField)
							+ "\nGrid worked any: "
							+ (gridWorked ? "yes" : "no");
				}
		) {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);

				if (empty || item == null) {
					setStyle("");
					return;
				}

				ChatMember member = getTableRow() == null ? null : getTableRow().getItem();
				boolean gridWorked = member != null
						&& chatcontroller != null
						&& chatcontroller.isGridSquareWorkedAny(member);

				/*
				 * Important:
				 * Do not style the cell unless the Grid color button is active AND the
				 * grid has already been worked. This keeps normal/new grids visually
				 * identical to the standard table design.
				 */
				if (!gridSquareHighlightEnabled || !gridWorked) {
					setStyle("");
					return;
				}

				/*
				 * Preserve normal selection appearance. A selected row should look like a
				 * selected row, not like a custom-colored QRA cell.
				 */
				if (isSelected() || (getTableRow() != null && getTableRow().isSelected())) {
					setStyle("");
					return;
				}

				/*
				 * Already worked grid:
				 * Use a subtle background derived from the current theme. No bold text.
				 */
				setStyle("-fx-background-color: derive(-fx-control-inner-background, -18%);");
			}
		});

		TableColumn<ChatMember, String> qrBCol = new TableColumn<ChatMember, String>("QRB");
		qrBCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty qrb = new SimpleStringProperty();

				qrb.setValue((cellDataFeatures.getValue().getQrb()+""));

				if (qrb.getValue().contains(".")) {
					 qrb.setValue(qrb.getValue().substring(0,qrb.getValue().indexOf(".")));
				}

				return qrb;
			}
		});

		qrBCol.setComparator(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {

				int distance1 = Integer.parseInt(o1);
				int distance2 = Integer.parseInt(o2);

                return Integer.compare(distance1, distance2);
            }
		});

		TableColumn<ChatMember, String> qtfCol = new TableColumn<ChatMember, String>("QTF");
		qtfCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty qra = new SimpleStringProperty();

				qra.setValue(cellDataFeatures.getValue().getQTFdirection()+"°");

				return qra;
			}
		});

		qtfCol.setComparator(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {

				double doubleDegreesObj1 = Double.parseDouble(o1.split("°")[0]); //filter the "°"
				double doubleDegreesObj2 = Double.parseDouble(o2.split("°")[0]); //filter the "°"

				if (doubleDegreesObj1 < doubleDegreesObj2) {
					return -1;
				} else if (doubleDegreesObj1 == doubleDegreesObj2) {
					return 0;
				} else if (doubleDegreesObj1 > doubleDegreesObj2) {
					return 1;
				}

				return 0;//should never happen!
			}
		});

		qtfCol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(15));

		TableColumn<ChatMember, String> qrgCol = new TableColumn<ChatMember, String>("QRG");
		qrgCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {

				return new SimpleStringProperty(cellDataFeatures.getValue().getFrequency().get());
			}

		});
		applyQrgUiFormatting(qrgCol); //insert zero until qrg string looks pretty


		TableColumn<ChatMember, String> airScoutCol = new TableColumn<ChatMember, String>("AP [minutes / pot%]");
		airScoutCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty airPlaneInfo = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getAirPlaneReflectInfo().getRisingAirplanes() == null) {
					airPlaneInfo.setValue("nil");
				}

				else if (cellDataFeatures.getValue().getAirPlaneReflectInfo().getRisingAirplanes().size() <= 0) {
					airPlaneInfo.setValue("nil");
				} else {
					String apInfoText = ""
							+ cellDataFeatures.getValue().getAirPlaneReflectInfo().getRisingAirplanes().get(0)
									.getArrivingDurationMinutes()
							+ " (" + cellDataFeatures.getValue().getAirPlaneReflectInfo().getRisingAirplanes().get(0)
									.getPotential()
							+ "%)";
//					
//					
					if (cellDataFeatures.getValue().getAirPlaneReflectInfo().getRisingAirplanes().size() > 1) {
						apInfoText += " / "
								+ cellDataFeatures.getValue().getAirPlaneReflectInfo().getRisingAirplanes().get(1)
										.getArrivingDurationMinutes()
								+ " (" + cellDataFeatures.getValue().getAirPlaneReflectInfo().getRisingAirplanes()
										.get(1).getPotential()
								+ "%)";
					}

					airPlaneInfo.setValue(apInfoText);
				}

				return airPlaneInfo;
			}
		});
		/**
		 * HIGH EXPERIMENTAL::::::::
		 */
		airScoutCol.setCellFactory(new Callback<TableColumn<ChatMember, String>, TableCell<ChatMember, String>>() {
			public TableCell call(TableColumn param) {
				return new TruncatedTextTableCell<ChatMember>() {

					@Override
					public void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);

						if (!isEmpty()) {

							if (item.contains("nil")) {
								this.getStyleClass().clear(); //clear css reference, then recoloring
								this.getStyleClass().add("table-cell"); //set old reference
							}
							// Get fancy and change color based on data
							if (item.contains("100%")) {
//
								this.getStyleClass().add("table-cell-100PercentAP");

							} else if (item.contains("75%") && !item.contains("100%")) {
								this.getStyleClass().add("table-cell-75PercentAP");

							} else if (item.contains("50%") && ((!item.contains("100%")) || (!item.contains("75%")))) {
								this.getStyleClass().add("table-cell-50PercentAP");
							}

						}
					}
				};
			}
		});
		/**
		 * END HIGH EXPERIMENTAL::::::::
		 */


		TableColumn<ChatMember, String> tropoCol = new TableColumn<ChatMember, String>("Tropo");
		tropoCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				ChatMember member = cellDataFeatures.getValue();
				Band selectedBand = resolveReachabilityBandForUi(member);
				chatcontroller.getReachabilityService().ensureTropoMarginCalculated(member, selectedBand);
				return new SimpleStringProperty(member.formatTropoSsbMarginForBand(selectedBand));
			}
		});
		tropoCol.setComparator((left, right) -> Double.compare(parseTableDouble(left), parseTableDouble(right)));
		tropoCol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(13));

		TableColumn<ChatMember, String> priorityScoreCol = new TableColumn<ChatMember, String>("Score");
		priorityScoreCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				return new SimpleStringProperty(formatPriorityScore(cellDataFeatures.getValue()));
			}
		});
		priorityScoreCol.setComparator((left, right) -> Double.compare(parseTableDouble(left), parseTableDouble(right)));
		priorityScoreCol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(18));

		TableColumn<ChatMember, String> lastActCol = new TableColumn<ChatMember, String>("Act");
		lastActCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty lastActEpoch = new SimpleStringProperty();

//				lastActEpoch.setValue(cellDataFeatures.getValue().getActivityTimeLastInEpoch()+"");

				lastActEpoch.setValue((Utils4KST.time_getSecondsBetweenEpochAndNow(cellDataFeatures.getValue().getActivityTimeLastInEpoch()+"") /60%60) +"");

				return lastActEpoch;
			}
		});
		lastActCol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(32));

/**
 * section of worked flag in chatmember table
 */

		TableColumn<ChatMember, String> workedCol = new TableColumn<ChatMember, String>("worked");
		workedCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});

		TableColumn<ChatMember, String> wkdAny_subcol = new TableColumn<ChatMember, String>("wkdany");
		wkdAny_subcol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
						return new SimpleStringProperty(formatWorkedAnyGridStatus(cellDataFeatures.getValue()));
					}
				});
		wkdAny_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(14));

		/**
		 * Shows the compact worked/grid status and explains it by tooltip.
		 */
		wkdAny_subcol.setCellFactory(column -> new TruncatedTextTableCell<ChatMember>(
				java.util.function.Function.identity(),
				(member, value) -> buildWorkedAnyGridStatusTooltip(member)
		) {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);

				if (empty) {
					setStyle("");
					return;
				}
				setAlignment(Pos.CENTER);
				setStyle("-fx-font-weight: bold;");
			}
		});


		TableColumn<ChatMember, String> sixMCol_subcol = new TableColumn<ChatMember, String>("50");
		sixMCol_subcol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
						return new SimpleStringProperty(formatBandCellStatus(
								cellDataFeatures.getValue(), Band.B_50, cellDataFeatures.getValue().isWorked50()
						));
					}
				});
		sixMCol_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(28));
		sixMCol_subcol.setCellFactory(createBandStatusCellFactory(Band.B_50));

		TableColumn<ChatMember, String> fourMCol_subcol = new TableColumn<ChatMember, String>("70");
		fourMCol_subcol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
						return new SimpleStringProperty(formatBandCellStatus(
								cellDataFeatures.getValue(), Band.B_70, cellDataFeatures.getValue().isWorked70()
						));
					}
				});
		fourMCol_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(28));
		fourMCol_subcol.setCellFactory(createBandStatusCellFactory(Band.B_70));

		TableColumn<ChatMember, String> vhfCol_subcol = new TableColumn<ChatMember, String>("144");
		vhfCol_subcol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
						return new SimpleStringProperty(formatBandCellStatus(
								cellDataFeatures.getValue(), Band.B_144, cellDataFeatures.getValue().isWorked144()
						));
					}
				});
		vhfCol_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(28));
		vhfCol_subcol.setCellFactory(createBandStatusCellFactory(Band.B_144));

		TableColumn<ChatMember, String> uhfCol_subcol = new TableColumn<ChatMember, String>("432");
		uhfCol_subcol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
						return new SimpleStringProperty(formatBandCellStatus(
								cellDataFeatures.getValue(), Band.B_432, cellDataFeatures.getValue().isWorked432()
						));
					}
				});

		uhfCol_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(28));
		uhfCol_subcol.setCellFactory(createBandStatusCellFactory(Band.B_432));

		TableColumn<ChatMember, String> shf23_subcol = new TableColumn<ChatMember, String>("23");
		shf23_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				return new SimpleStringProperty(formatBandCellStatus(
						cellDataFeatures.getValue(), Band.B_1296, cellDataFeatures.getValue().isWorked1240()
				));
			}
		});
		shf23_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(30));
		shf23_subcol.setCellFactory(createBandStatusCellFactory(Band.B_1296));

		TableColumn<ChatMember, String> shf13_subcol = new TableColumn<ChatMember, String>("13");
		shf13_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				return new SimpleStringProperty(formatBandCellStatus(
						cellDataFeatures.getValue(), Band.B_2320, cellDataFeatures.getValue().isWorked2300()
				));
			}
		});
		shf13_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(30));
		shf13_subcol.setCellFactory(createBandStatusCellFactory(Band.B_2320));

		TableColumn<ChatMember, String> shf9_subcol = new TableColumn<ChatMember, String>("9");
		shf9_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				return new SimpleStringProperty(formatBandCellStatus(
						cellDataFeatures.getValue(), Band.B_3400, cellDataFeatures.getValue().isWorked3400()
				));
			}
		});
		shf9_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(32));
		shf9_subcol.setCellFactory(createBandStatusCellFactory(Band.B_3400));

		TableColumn<ChatMember, String> shf6_subcol = new TableColumn<ChatMember, String>("6");
		shf6_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				return new SimpleStringProperty(formatBandCellStatus(
						cellDataFeatures.getValue(), Band.B_5760, cellDataFeatures.getValue().isWorked5600()
				));
			}
		});
		shf6_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(32));
		shf6_subcol.setCellFactory(createBandStatusCellFactory(Band.B_5760));

		TableColumn<ChatMember, String> shf3_subcol = new TableColumn<ChatMember, String>("3");
		shf3_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				return new SimpleStringProperty(formatBandCellStatus(
						cellDataFeatures.getValue(), Band.B_10G, cellDataFeatures.getValue().isWorked10G()
				));
			}
		});

		shf3_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(32));
		shf3_subcol.setCellFactory(createBandStatusCellFactory(Band.B_10G));

		/**
		 * section of NOT-QRV flag in chatmember table
		 */

		TableColumn<ChatMember, String> notQRVCol = new TableColumn<ChatMember, String>("NOT QRV @");
		notQRVCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				wkd.setValue("");

				if (!cellDataFeatures.getValue().isQrv50()) {
					wkd.setValue(wkd.getValue() + "6m ");
				} else {
					wkd.setValue(wkd.getValue().replace("6m ",""));
				}

				if (!cellDataFeatures.getValue().isQrv70()) {
					wkd.setValue(wkd.getValue() + "4m ");
				} else {
					wkd.setValue(wkd.getValue().replace("4m ",""));
				}

				if (!cellDataFeatures.getValue().isQrv144()) {
					wkd.setValue(wkd.getValue() + "144 ");
				} else {
					wkd.setValue(wkd.getValue().replace("144 ",""));
				}

				if (!cellDataFeatures.getValue().isQrv432()) {
					wkd.setValue(wkd.getValue() + "70 ");
				} else {
					wkd.setValue(wkd.getValue().replace("70 ",""));
				}

				if (!cellDataFeatures.getValue().isQrv1240()) {
					wkd.setValue(wkd.getValue() + "SHF23 ");
				} else {
					wkd.setValue(wkd.getValue().replace("SHFcm ",""));
				}

				if (!cellDataFeatures.getValue().isQrv2300()) {
					wkd.setValue(wkd.getValue() + "SHF13 ");
				} else {
					wkd.setValue(wkd.getValue().replace("SHF13 ",""));
				}

				if (!cellDataFeatures.getValue().isQrv3400()) {
					wkd.setValue(wkd.getValue() + "SHF9 ");
				} else {
					wkd.setValue(wkd.getValue().replace("SHF9 ",""));
				}

				if (!cellDataFeatures.getValue().isQrv5600()) {
					wkd.setValue(wkd.getValue() + "SHF6 ");
				} else {
					wkd.setValue(wkd.getValue().replace("SHF6 ",""));
				}

				if (!cellDataFeatures.getValue().isQrv10G()) {
					wkd.setValue(wkd.getValue() + "SHF3 ");
				} else {
					wkd.setValue(wkd.getValue().replace("SHF3 ",""));
				}


				return wkd;
			}
		});


		/**
		 * section of NOT-QRV flag in chatmember table
		 */

		TableColumn<ChatMember, String> chatCategoryCol = new TableColumn<ChatMember, String>("Category");
		chatCategoryCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				StringProperty category = new SimpleStringProperty();

//				category.setValue(cellDataFeatures.getValue().getChatCategory().getCategoryNumber() + "");
				category.setValue(cellDataFeatures.getValue().getChatCategory().getChatCategoryName(cellDataFeatures.getValue().getChatCategory().getCategoryNumber()));

				return category;

			}
		});


		/**
		 * add now only cols which affects the used band of my station
		 */

		if (chatcontroller.getChatPreferences().isStn_bandActive50()) {
			workedCol.getColumns().add(sixMCol_subcol);
		}
		if (chatcontroller.getChatPreferences().isStn_bandActive70()) {
			workedCol.getColumns().add(fourMCol_subcol);
		}
		if (chatcontroller.getChatPreferences().isStn_bandActive144()) {
			workedCol.getColumns().add(vhfCol_subcol);
		}
		if (chatcontroller.getChatPreferences().isStn_bandActive432()) {
			workedCol.getColumns().add(uhfCol_subcol);
		}

		if (chatcontroller.getChatPreferences().isStn_bandActive1240()) {
			workedCol.getColumns().add(shf23_subcol);
		}
		if (chatcontroller.getChatPreferences().isStn_bandActive2300()) {
			workedCol.getColumns().add(shf13_subcol);
		}
		if (chatcontroller.getChatPreferences().isStn_bandActive3400()) {
			workedCol.getColumns().add(shf9_subcol);
		}
		if (chatcontroller.getChatPreferences().isStn_bandActive5600()) {
			workedCol.getColumns().add(shf6_subcol);
		}
		if (chatcontroller.getChatPreferences().isStn_bandActive10G()) {
			workedCol.getColumns().add(shf3_subcol);
		}

		/**
		 * The worked any col makes sense in all cases
		 */
			workedCol.getColumns().add(wkdAny_subcol);




//		tbl_chatMemberTable.getColumns().addAll(callSignCol, nameCol, qraCol, qrBCol, qtfCol, qrgCol, lastActCol, airScoutCol, workedCol, notQRVCol, chatCategoryCol);

		tbl_chatMemberTable.getColumns().addAll(callSignCol, nameCol, qraCol, qrBCol, qtfCol, qrgCol, tropoCol, priorityScoreCol, lastActCol, airScoutCol, workedCol, notQRVCol, chatCategoryCol);


//		tbl_chatMemberTable.setItems(chatcontroller.getLst_chatMemberListFiltered());

		/*
		 * The station list is derived in core now. The TableView column headers feed
		 * the comparator that the SortedList used to receive through a binding.
		 */
		chatcontroller.setChatMemberOrder(tbl_chatMemberTable.getComparator());
		chatMemberListBinding = derivedBinding(
				chatcontroller::visibleChatMembers,
				chatcontroller.getLst_chatMemberList(),
				chatcontroller.getLst_chatMemberListFilterPredicates());
		tbl_chatMemberTable.setItems(chatMemberListBinding.list());
		tbl_chatMemberTable.comparatorProperty().addListener((observable, oldComparator, newComparator) -> {
			chatcontroller.setChatMemberOrder(newComparator);
			chatMemberListBinding.refresh();
		});
		/*
		 * The default sort policy would sort the mirror in place. A SortedList used to
		 * absorb that, so the visible order was always derived from the unsorted
		 * source. Keep it that way: the derived list is already ordered, and sorting
		 * an already sorted mirror again could reorder equally ranked stations.
		 */
		tbl_chatMemberTable.setSortPolicy(table -> {
			chatcontroller.setChatMemberOrder(table.getComparator());
			chatMemberListBinding.refresh();
			return true;
		});
//		chatcontroller.getLst_chatMemberList().addListener(new ListChangeListener<ChatMember>() {
////		ObservableStringValue chatState = new SimpleStringProperty();
//		
//			@Override
//			public void onChanged(javafx.collections.ListChangeListener.Change<? extends ChatMember> pChange) {
////				while (pChange.next()) {
////					System.out.println("List changed");
//					
//					//TODO: Das kann man ggf anders machen
//					
//					String chatState = chatcontroller.getChatPreferences().getProgramVersion() + " / "
//							+ "Connected to: " + chatcontroller.getChatPreferences().getLoginChatCategory() + " / "
//							+ chatcontroller.getLst_chatMemberList().size() + " users online.";
//					chatcontroller.getChatPreferences().setChatState(chatState);
//					
////					chatcontroller.getChatPreferences().setChatState(chatcontroller.getChatPreferences().getProgramVersion() + " / "
////							+ "Connected to: " + chatcontroller.getChatPreferences().getLoginChatCategory() + " / "
////							+ chatcontroller.getLst_chatMemberList().size() + " users online.");
////					primaryStage.setTitle("asdf");
////					primaryStage.setTitle(chatcontroller.getChatPreferences().getProgramVersion() + " / "
////							+ "Connected to: " + chatcontroller.getChatPreferences().getLoginChatCategory() + " / "
////							+ chatcontroller.getLst_chatMemberList().size() + " users online.");
////				}
//			}
//		});

		tbl_chatMemberTable.getSortOrder().add(callSignCol);

//		initializeCommunicationOverMyHeadVizalizationStage(new ChatMember());

		/**
		 * timer_chatMemberTableSortTimer -->
		 * This part fixes a javafx bug. The update of the Chatmember fields is (for any
		 * reason) not visible in the ui. Its neccessarry to (now no more sort!) but refresh
		 * the table in intervals to keep the table up to date.
		 */

//		timer_chatMemberTableSortTimer = new Timer();
//		timer_chatMemberTableSortTimer.scheduleAtFixedRate(new TimerTask() {
//
//			public void run() {
//				Thread.currentThread().setName("chatMemberTableSortTimer");
//
//				Platform.runLater(() -> {
//
//					try {
//
////						tbl_chatMemberTable.sort();
//
//					} catch (Exception e) {
//						System.out.println("[Main.java, Warning:] Table sorting (actualizing) failed this time.");
//					}
//
//
//					tbl_chatMemberTable.refresh();
//
////					tbl_chatMemberTable.
//
//				});
//			}
//		}, new Date(), 5000);

		tbl_chatMemberTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		applyTruncatedTextCells(
				nameCol, qrBCol, qtfCol, tropoCol, priorityScoreCol,
				lastActCol, notQRVCol, chatCategoryCol
		);
		TableLayoutManager.install(
				tbl_chatMemberTable,
				"chat-members",
				chatcontroller.getChatPreferences(),
				layoutAutosave,
				TableLayoutManager.column("callsign", callSignCol),
				TableLayoutManager.column("name", nameCol).maximumInitialWidth(220),
				TableLayoutManager.column("qra", qraCol),
				TableLayoutManager.column("qrb", qrBCol),
				TableLayoutManager.column("qtf", qtfCol),
				TableLayoutManager.column("qrg", qrgCol),
				TableLayoutManager.column("tropo", tropoCol),
				TableLayoutManager.column("score", priorityScoreCol),
				TableLayoutManager.column("activity", lastActCol),
				TableLayoutManager.column("airscout", airScoutCol).maximumInitialWidth(190),
				TableLayoutManager.column("worked-any", wkdAny_subcol),
				TableLayoutManager.column("band-50", sixMCol_subcol),
				TableLayoutManager.column("band-70", fourMCol_subcol),
				TableLayoutManager.column("band-144", vhfCol_subcol),
				TableLayoutManager.column("band-432", uhfCol_subcol),
				TableLayoutManager.column("band-1296", shf23_subcol),
				TableLayoutManager.column("band-2320", shf13_subcol),
				TableLayoutManager.column("band-3400", shf9_subcol),
				TableLayoutManager.column("band-5760", shf6_subcol),
				TableLayoutManager.column("band-10g", shf3_subcol),
				TableLayoutManager.column("not-qrv", notQRVCol).maximumInitialWidth(180),
				TableLayoutManager.column("category", chatCategoryCol)
		);


		/**
		 * expoerimental, new since 1.40: priorities
		 */

		tbl_chatMemberTable.setRowFactory(tv -> new TableRow<ChatMember>() {
			@Override
			protected void updateItem(ChatMember item, boolean empty) {
				super.updateItem(item, empty);

				if (!ENABLE_PRIORITY_SCORE_ROW_COLORING) {

					setStyle(""); // Reset style for empty rows
				} else {
					double score = item.getCurrentPriorityScore(); // Ensure ChatMember has this getter!

					// Color Logic:
					// > 1000 = NUCLEAR (Imminent Sked) -> Blinking Red (simulated here with solid red)
					// > 200  = High Prio (AirScout / Good Sked) -> Orange
					// > 100  = Medium Prio (Unworked / New Multi) -> Light Yellow
					// <= 0   = Low Prio / Not Reachable -> Greyed out text

					// Note: Styles need to be adjusted if Dark Mode is active!

					boolean isDark = chatcontroller.getChatPreferences().isGUI_darkModeActive();

					if (score > 1000) {
						// Critical Alert
						setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white; -fx-font-weight: bold;");
					} else if (score >= 200) {
						// High Priority
						setStyle("-fx-background-color: " + (isDark ? "#cc6600" : "#ffcc00") + "; -fx-text-fill: black;");
					} else if (score >= 100) {
						// Medium Priority
						setStyle("-fx-background-color: " + (isDark ? "#888800" : "#ffffcc") + "; -fx-text-fill: black;");
					} else if (score <= 0) {
						// Penalty / Not Reachable
						setStyle("-fx-text-fill: " + (isDark ? "#666666" : "#aaaaaa") + ";");
					} else {
						// Standard Reset
						setStyle("");
					}
				}
			}
		});

		return tbl_chatMemberTable;
	}

	private ChatMember resolveChatMemberForCallRawAndCategory(String callRaw, ChatCategory preferredCategory) {

		if (callRaw == null) return null;

		// 1) Prefer exact (callRaw + category)
		// One snapshot for both passes: the roster is consistent inside it.
		List<ChatMember> currentMembers = chatcontroller.getLst_chatMemberList().snapshot();

		for (ChatMember m : currentMembers) {
			if (m == null) continue;
			if (m.getCallSignRaw() == null) continue;
			if (!m.getCallSignRaw().equalsIgnoreCase(callRaw)) continue;

			if (preferredCategory != null && preferredCategory.equals(m.getChatCategory())) {
				return m;
			}
		}

		// 2) Fallback: any variant with same callsignRaw
		for (ChatMember m : currentMembers) {
			if (m == null) continue;
			if (m.getCallSignRaw() == null) continue;
			if (m.getCallSignRaw().equalsIgnoreCase(callRaw)) return m;
		}

		return null;
	}

	/**
	 * Focuses a ChatMember and prepares the send field.
	 *
	 * This overload is used by explicit operator actions such as timeline clicks or
	 * context actions. In those cases it is acceptable to overwrite the previous
	 * automatic /cq template, because the operator intentionally chose a station.
	 */
	private void focusChatMemberAndPrepareCq(ChatMember member) {
		focusChatMemberAndPrepareCq(member, true);
	}

	/**
	 * Focuses a ChatMember and optionally prepares the /cq command.
	 *
	 * The method selects the member in the table if possible, but guards that
	 * programmatic selection with programmaticChatMemberSelectionChange. This prevents
	 * the table selection listener from running the same logic a second time.
	 *
	 * @param member ChatMember that should become the active station
	 * @param forcePrepareCq true if /cq should be prepared even if the logical
	 *                       selection did not change
	 */
	private void focusChatMemberAndPrepareCq(ChatMember member, boolean forcePrepareCq) {
		if (member == null) {
			return;
		}

		ChatMember previousSelectedMember = selectedCallSignInfoStageChatMember;

		/*
		 * Selecting the row programmatically is useful for visual feedback, but the
		 * table-selection listener must not prepare /cq a second time.
		 */
		try {
			if (tbl_chatMember != null
					&& tbl_chatMember.getItems() != null
					&& tbl_chatMember.getItems().contains(member)) {
				programmaticChatMemberSelectionChange = true;
				tbl_chatMember.getSelectionModel().select(member);
				tbl_chatMember.scrollTo(member);
			}
		} catch (Exception ignored) {
			// Ignore: table may not be ready yet or member may currently be filtered out.
		} finally {
			programmaticChatMemberSelectionChange = false;
		}

		handleChatMemberSelectionChanged(member, forcePrepareCq);
	}

	/**
	 * Central handler for a selected ChatMember.
	 *
	 * Selection changes can be emitted by JavaFX even when the user did not click a
	 * new station, for example after FilteredList/SortedList invalidations during
	 * periodic score updates.
	 *
	 * Therefore the send text is only auto-prepared when:
	 * - the logical station really changed, or
	 * - the caller explicitly forces preparation.
	 *
	 * Even then, the send field is protected by prepareCqTextForSelectedChatMember()
	 * so manually typed operator text is not destroyed.
	 */
	private void handleChatMemberSelectionChanged(ChatMember selectedMember, boolean forcePrepareCq) {
		if (selectedMember == null) {
			return;
		}

		/*
		 * Whether the info pane already shows this station. Compared against the
		 * member the pane was last built for, not against the selection model's
		 * previous value: a station entering or leaving the chat replaces the whole
		 * table mirror, which makes the model emit [null, sameMember]. Its previous
		 * value is then null and looks like a change, while the operator never left
		 * the station.
		 *
		 * Rebuilding the pane on such an event resets the message filter radio
		 * buttons to their configured defaults, which takes the operator's view away
		 * while they are still reading it. On a busy server that happens every few
		 * seconds.
		 */
		boolean infoPaneAlreadyShowsMember =
				isSameLogicalChatMember(selectedCallSignInfoStageChatMember, selectedMember);

		selectedCallSignInfoStageChatMember = selectedMember;

		if (chatcontroller != null && chatcontroller.getScoreService() != null) {
			chatcontroller.getScoreService().setSelectedChatMember(selectedMember);
		}

		if (!infoPaneAlreadyShowsMember) {
			try {
				selectedCallSignFurtherInfoPane.getChildren().setAll(generateFurtherInfoAbtSelectedCallsignBP(selectedMember));
			} catch (Exception exception) {
				System.out.println("KST4CApp: ERROR, selected member disappeared: " + exception.getMessage());
			}
		}

		/*
		 * prepareCqTextForSelectedChatMember() ends in requestFocus() on the send
		 * field. It must therefore only run when the station really changed, or when
		 * the operator selected a row themselves: the [null, sameMember] burst a
		 * mirror replacement produces would otherwise pull the keyboard focus away
		 * from whatever field the operator is typing in, every time a station enters
		 * or leaves the chat.
		 */
		if (forcePrepareCq || !infoPaneAlreadyShowsMember) {
			prepareCqTextForSelectedChatMember(selectedMember, forcePrepareCq);
		}
	}

	/**
	 * Prepares the send field with "/cq CALL " for the selected ChatMember.
	 *
	 * The ChatMember's category is stored together with the prepared text. This is
	 * required because a later table refresh may change the selected row while the
	 * operator is still editing the message. The send handler can then still send the
	 * message in the category of the callsign shown in the text field.
	 */
	private void prepareCqTextForSelectedChatMember(ChatMember member, boolean forceOverwrite) {
		if (member == null) {
			return;
		}

		prepareCqTextForCallsign(member.getCallSign(), member.getChatCategory(), forceOverwrite);
	}

	/**
	 * Convenience overload when only a callsign is known.
	 *
	 * No category is attached in this case. The send handler will later try to
	 * resolve the category from the visible /cq callsign and the active user list.
	 */
	private void prepareCqTextForCallsign(String callSign, boolean forceOverwrite) {
		prepareCqTextForCallsign(callSign, null, forceOverwrite);
	}

	/**
	 * Prepares "/cq CALL " in the send field without destroying operator input.
	 *
	 * The field may only be overwritten when:
	 * - forceOverwrite is true,
	 * - the field is empty, or
	 * - the field still contains the previous automatically generated /cq template.
	 *
	 * If the operator already changed the text, this method leaves the field exactly
	 * as it is.
	 */
	private void prepareCqTextForCallsign(String callSign, ChatCategory preparedCategory, boolean forceOverwrite) {
		if (callSign == null || callSign.isBlank() || txt_chatMessageUserInput == null) {
			return;
		}

		String preparedText = "/cq " + callSign.trim() + " ";

		if (!forceOverwrite && !canOverwriteSendTextWithAutoPreparedText()) {
			return;
		}

		txt_chatMessageUserInput.setText(preparedText);

		/*
		 * Remember the generated text and its target. This connects the visible /cq
		 * command to the correct chat category even if the selected table row changes
		 * later due to refresh/filter activity.
		 */
		lastAutoPreparedSendText = preparedText;
		lastAutoPreparedCqTargetCallsign = normalizeCallsignForCategoryResolution(callSign);
		lastAutoPreparedCqTargetCategory = normalizeToActiveChatCategory(preparedCategory);

		/*
		 * Deferred on purpose. When the operator clicks a table row, this method runs
		 * from inside the selection listener, which fires during the mouse event. The
		 * TableView claims the focus afterwards as part of its own default handling,
		 * so an immediate requestFocus() here is silently undone. Platform.runLater
		 * on purpose and not uiDispatcher: the dispatcher runs the task inline when
		 * the caller is already on the UI thread, which is exactly the case here and
		 * would defer nothing.
		 */
		Platform.runLater(() -> {
			txt_chatMessageUserInput.requestFocus();
			txt_chatMessageUserInput.selectEnd();
		});
	}

	/**
	 * Checks whether an automatic /cq preparation is allowed to overwrite the input.
	 *
	 * It is safe to overwrite:
	 * - an empty field
	 * - the exact text that was previously created automatically
	 *
	 * It is not safe to overwrite anything else, because that means the operator has
	 * started typing or editing a message.
	 */
	private boolean canOverwriteSendTextWithAutoPreparedText() {
		String currentText = txt_chatMessageUserInput == null ? null : txt_chatMessageUserInput.getText();

		if (currentText == null || currentText.isBlank()) {
			return true;
		}

		return currentText.equals(lastAutoPreparedSendText);
	}

	/**
	 * Compares two ChatMember objects by their active chat identity.
	 *
	 * <p>JavaFX may replace item instances during list refreshes, so object
	 * identity alone is not sufficient. The complete callsign and the chat
	 * category identify one active ON4KST login.</p>
	 *
	 * <p>The raw/base callsign must not be used here. Otherwise callsigns such as
	 * {@code 9A0BB-2} and {@code 9A0BB-70} would still be treated as the same
	 * selection inside one chat category.</p>
	 */
	private boolean isSameLogicalChatMember(ChatMember a, ChatMember b) {

		if (a == b) {
			return true;
		}

		if (a == null || b == null) {
			return false;
		}

		String aCallSign = a.getCallSign();
		String bCallSign = b.getCallSign();

		if (aCallSign == null
				|| bCallSign == null
				|| !aCallSign.equalsIgnoreCase(bCallSign)) {
			return false;
		}

		ChatCategory aCategory = a.getChatCategory();
		ChatCategory bCategory = b.getChatCategory();

		if (aCategory == bCategory) {
			return true;
		}

		if (aCategory == null || bCategory == null) {
			return false;
		}

		return aCategory.getCategoryNumber()
				== bCategory.getCategoryNumber();
	}

	/**
	 * Resolves the chat category for an outgoing message.
	 *
	 * Important: if the send field contains an explicit "/cq CALL ...", the target
	 * callsign in the text is authoritative. This prevents a later table refresh or
	 * selection event from sending an already typed message in the category of a
	 * different selected ChatMember.
	 */
	private ChatCategory resolveOutgoingChatCategory(String outgoingText, ChatMember selectedMember) {
		ChatCategory mainCategory = chatcontroller != null ? chatcontroller.getChatCategoryMain() : null;
		ChatCategory selectedMemberCategory = getActiveChatCategoryForMember(selectedMember);
		String cqTargetCallsign = extractCqTargetCallsign(outgoingText);

		if (cqTargetCallsign != null) {
			String normalizedTarget = normalizeCallsignForCategoryResolution(cqTargetCallsign);

			/*
			 * If this text was auto-prepared from a concrete ChatMember, keep that
			 * category attached to the text even if the table selection changes later.
			 */
			if (normalizedTarget.equals(lastAutoPreparedCqTargetCallsign)
					&& lastAutoPreparedCqTargetCategory != null) {
				return lastAutoPreparedCqTargetCategory;
			}

			/*
			 * If the currently selected member is the same station as the /cq target,
			 * its category is safe to use.
			 */
			if (selectedMember != null
					&& selectedMemberCategory != null
					&& chatMemberMatchesCallsign(selectedMember, normalizedTarget)) {
				return selectedMemberCategory;
			}

			/*
			 * Otherwise resolve the /cq target from the user list. This also protects
			 * manually typed /cq texts when a different station is selected.
			 */
			ChatCategory uniqueCategoryForTarget = findUniqueActiveCategoryForCallsign(normalizedTarget);
			if (uniqueCategoryForTarget != null) {
				return uniqueCategoryForTarget;
			}

			System.out.println("KST4CApp: WARNING, could not resolve chat category for explicit /cq target "
					+ cqTargetCallsign + "; using main chat category as safe fallback.");
			return mainCategory;
		}

		if (selectedMemberCategory != null) {
			return selectedMemberCategory;
		}

		return mainCategory;
	}

	/**
	 * Returns an active ChatCategory object for the member category.
	 *
	 * This avoids sending with stale category instances and maps the member category
	 * to the controller's current main/second category objects.
	 */
	private ChatCategory getActiveChatCategoryForMember(ChatMember member) {
		if (member == null) {
			return null;
		}
		return normalizeToActiveChatCategory(member.getChatCategory());
	}

	/**
	 * Maps any category object to the currently active main/second chat category.
	 *
	 * This is safer than directly reusing a category object from a ChatMember, because
	 * the controller owns the actual active category instances used for sending.
	 */
	private ChatCategory normalizeToActiveChatCategory(ChatCategory category) {
		if (category == null || chatcontroller == null) {
			return null;
		}

		String categoryNumber = category.getCategoryNumber() + "";

		if (chatcontroller.getChatCategoryMain() != null
				&& categoryNumber.equals(chatcontroller.getChatCategoryMain().getCategoryNumber() + "")) {
			return chatcontroller.getChatCategoryMain();
		}

		if (chatcontroller.getChatCategorySecondChat() != null
				&& categoryNumber.equals(chatcontroller.getChatCategorySecondChat().getCategoryNumber() + "")) {
			return chatcontroller.getChatCategorySecondChat();
		}

		return null;
	}

	/**
	 * Extracts the target callsign from a directed ON4KST command such as:
	 *
	 * /cq DL1ABC hello
	 *
	 * Returns null for normal/general messages that do not start with "/cq ".
	 */
	private String extractCqTargetCallsign(String outgoingText) {
		if (outgoingText == null) {
			return null;
		}

		String trimmedText = outgoingText.trim();
		if (trimmedText.length() < 5 || !trimmedText.toLowerCase(Locale.ROOT).startsWith("/cq ")) {
			return null;
		}

		String afterCommand = trimmedText.substring(4).trim();
		if (afterCommand.isBlank()) {
			return null;
		}

		String[] parts = afterCommand.split("\\s+", 2);
		return parts.length > 0 && !parts[0].isBlank() ? parts[0].trim() : null;
	}

	/**
	 * Searches the active user list for one unique category for a callsign.
	 *
	 * If the same callsign is present in both active chat channels, the result is not
	 * unique and null is returned. In that case the send handler falls back to the
	 * main category and logs a warning.
	 */
	private ChatCategory findUniqueActiveCategoryForCallsign(String normalizedCallsign) {
		if (normalizedCallsign == null || normalizedCallsign.isBlank()
				|| chatcontroller == null || chatcontroller.getLst_chatMemberList() == null) {
			return null;
		}

		ChatCategory foundCategory = null;

		for (ChatMember chatMember : chatcontroller.getLst_chatMemberList().snapshot()) {
			if (chatMember == null || !chatMemberMatchesCallsign(chatMember, normalizedCallsign)) {
				continue;
			}

			ChatCategory activeCategory = getActiveChatCategoryForMember(chatMember);
			if (activeCategory == null) {
				continue;
			}

			if (foundCategory == null) {
				foundCategory = activeCategory;
			} else if (foundCategory.getCategoryNumber() != activeCategory.getCategoryNumber()) {
				// Same callsign is present in multiple active chat channels; not unique.
				return null;
			}
		}

		return foundCategory;
	}

	/**
	 * Checks whether a ChatMember represents the given normalized callsign.
	 *
	 * Both getCallSign() and getCallSignRaw() are checked because different parts of
	 * the program use either the decorated/display callsign or the raw callsign.
	 */
	private boolean chatMemberMatchesCallsign(ChatMember chatMember, String normalizedCallsign) {
		if (chatMember == null || normalizedCallsign == null || normalizedCallsign.isBlank()) {
			return false;
		}

		String callSign = normalizeCallsignForCategoryResolution(chatMember.getCallSign());
		String callSignRaw = normalizeCallsignForCategoryResolution(chatMember.getCallSignRaw());

		return normalizedCallsign.equals(callSign) || normalizedCallsign.equals(callSignRaw);
	}

	/**
	 * Normalizes callsigns for safe comparisons.
	 */
	private String normalizeCallsignForCategoryResolution(String callsign) {
		return callsign == null ? "" : callsign.trim().toUpperCase(Locale.ROOT);
	}

//	private void focusChatMemberAndPrepareCq(ChatMember member) {
//		if (member == null) return;
//
//		// Try selecting in table if it is visible (nice UX), but do not depend on it
//		try {
//			if (tbl_chatMember != null && tbl_chatMember.getItems() != null && tbl_chatMember.getItems().contains(member)) {
//				tbl_chatMember.getSelectionModel().select(member);
//				tbl_chatMember.scrollTo(member);
//			}
//		} catch (Exception ignored) {
//			// ignore: table not ready or filtered
//		}
//
//		// Force selection effects regardless of filters/selection state
//		selectedCallSignInfoStageChatMember = member;
//		chatcontroller.getScoreService().setSelectedChatMember(member);
//
//		selectedCallSignFurtherInfoPane.getChildren().setAll(generateFurtherInfoAbtSelectedCallsignBP(member));
//
//		txt_chatMessageUserInput.clear();
//		txt_chatMessageUserInput.setText("/cq " + member.getCallSign() + " ");
//		txt_chatMessageUserInput.requestFocus();
//		txt_chatMessageUserInput.selectEnd();
//	}



	/**
	 * Initializes the right click contextmenu for the chatmember-table, sets the
	 * clickhandler for the contextmenu out of a string array (each menuitam will be
	 * created out of exact one array-entry). These are initialized by the
	 * chatpreferences object out of the config-xml
	 *
	 * @return
	 */
	private ContextMenu initChatMemberTableContextMenu(ObservableList<String> contextMenuEntries) { // new mechanic

		ContextMenu chatMemberContextMenu = new ContextMenu();

		for (Iterator iterator = contextMenuEntries.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			final MenuItem menuItem = new MenuItem(string);
			menuItem.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					appendResolvedMessageText(menuItem.getText());
				}
			});

			chatMemberContextMenu.getItems().add(menuItem);

		}

//		MenuItem macro1 = new MenuItem("Pse Sked?");
//		macro1.setOnAction(new EventHandler<ActionEvent>() {
//	         public void handle(ActionEvent event) {
//	        	 txt_chatMessageUserInput.setText(txt_chatMessageUserInput.getText() + macro1.getText());
//	          }
//	       });
//		MenuItem macro10 = new MenuItem("Pse qrg 2m?");
//		MenuItem macro20 = new MenuItem("Pse Call at ");
//		MenuItem macro30 = new MenuItem("In qso nw, pse qrx, I will meep you");
//		MenuItem macro40 = new MenuItem("Pse qrg 70cm?");
//		MenuItem macro50 = new MenuItem("pse qrg 23cm?");
//		MenuItem macro60 = new MenuItem("____________________________________");
//		MenuItem macro70 = new MenuItem("Watch QSO history");
//
//		chatMemberContextMenu.getItems().add(macro1);
//		chatMemberContextMenu.getItems().add(macro10);
//		chatMemberContextMenu.getItems().add(macro20);
//		chatMemberContextMenu.getItems().add(macro30);
//		chatMemberContextMenu.getItems().add(macro40);
//		chatMemberContextMenu.getItems().add(macro50);
//		chatMemberContextMenu.getItems().add(macro60);
//		chatMemberContextMenu.getItems().add(macro70);

		return chatMemberContextMenu;

	}

	private TableView<ChatMessage> initFurtherInfoAbtCallsignMSGTable() {

		TableView<ChatMessage> tbl_furtherInfoAbtCallsignMSGTable = new TableView<ChatMessage>();
//		tbl_furtherInfoAbtCallsignMSGTable.setTooltip(new Tooltip("Messages of selected station are shown here"));

		TableColumn<ChatMessage, String> timeCol = new TableColumn<ChatMessage, String>("Time");
		timeCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty time = new SimpleStringProperty();

				time.setValue(new Utils4KST()
						.time_convertEpochToReadable(cellDataFeatures.getValue().getMessageGeneratedTime()));

				return time;
			}
		});

		TableColumn<ChatMessage, String> callSignTRCVCol = new TableColumn<ChatMessage, String>("Call TX");
		callSignTRCVCol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
						SimpleStringProperty callSign = new SimpleStringProperty();

						if (cellDataFeatures.getValue().getSender() != null) {

							callSign.setValue(cellDataFeatures.getValue().getSender().getCallSign());
						} else {

							callSign.setValue("");// TODO: Prevents a bug of not setting all values as a default
						}
						return callSign;
					}
				});

		TableColumn<ChatMessage, String> callSignRCVRCol = new TableColumn<ChatMessage, String>("Call RX");
		callSignRCVRCol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
						SimpleStringProperty callTX = new SimpleStringProperty();

						if (cellDataFeatures.getValue().getReceiver().getCallSign() != null) {

							callTX.setValue(cellDataFeatures.getValue().getReceiver().getCallSign());
						} else {

							callTX.setValue("");// TODO: Prevents a bug of not setting all values as a default
						}
						return callTX;
					}
				});

//		TableColumn<ChatMessage, String> nameCol = new TableColumn<ChatMessage, String>("Name");
//		nameCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {
//
//			@Override
//			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
//				SimpleStringProperty name = new SimpleStringProperty();
//
//				if (cellDataFeatures.getValue().getSender() != null) {
//
//					name.setValue(cellDataFeatures.getValue().getSender().getName());
//				} else {
//
//					name.setValue("");// TODO: Prevents a bug of not setting all values as a default
//				}
//				return name;
//			}
//		});

		TableColumn<ChatMessage, String> qrgTXerCol = new TableColumn<ChatMessage, String>("Last QRG TX");
		qrgTXerCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				StringProperty qrg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

//					qrg.setValue(cellDataFeatures.getValue().getSender().getFrequency());
					qrg.setValue(cellDataFeatures.getValue().getSender().getFrequency().get());
				} else {

					qrg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return qrg;
			}
		});

		TableColumn<ChatMessage, String> qrgRXerCol = new TableColumn<ChatMessage, String>("Last QRG RX");
		qrgRXerCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				StringProperty qrg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getReceiver() != null) {

//					qrg.setValue(cellDataFeatures.getValue().getReceiver().getFrequency());
					qrg.setValue(cellDataFeatures.getValue().getReceiver().getFrequency().get());

				} else {

					qrg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return qrg;
			}
		});

		TableColumn<ChatMessage, String> msgCol = new TableColumn<ChatMessage, String>("Message");
		msgCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty msg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getMessageText() != null) {

					msg.setValue(cellDataFeatures.getValue().getMessageText());
				} else {

					msg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return msg;
			}
		});
		msgCol.prefWidthProperty().bind(tbl_furtherInfoAbtCallsignMSGTable.widthProperty().divide(2));

		msgCol.setCellFactory(column ->
				new MessageTextTableCell<>(getHostServices()::showDocument)
		);

		TableColumn<ChatMessage, String> workedRXCol = new TableColumn<ChatMessage, String>("wkd RX?");
		workedRXCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getReceiver().isWorked()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});

		TableColumn<ChatMessage, String> workedTXCol = new TableColumn<ChatMessage, String>("wkd TX?");
		workedRXCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender().isWorked()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});

		tbl_furtherInfoAbtCallsignMSGTable.getColumns().addAll(timeCol, callSignTRCVCol, callSignRCVRCol,
				  msgCol);

		/*
		 * A new info window replaces the old one, and with it its table. Release the
		 * previous mirror instead of leaving it listening on the message roster.
		 */
		if (selectedCallSignInfoMessageBinding != null) {
			selectedCallSignInfoMessageBinding.dispose();
			rosterBindings.remove(selectedCallSignInfoMessageBinding);
		}
		selectedCallSignInfoMessageBinding = derivedBinding(
				chatcontroller::selectedCallSignInfoMessages,
				chatcontroller.getLst_globalChatMessageList());
		selectedCallSignInfoMessageBinding.list().addListener((javafx.collections.ListChangeListener<ChatMessage>) c -> {
			if (composeMainWindowState != null) {
				uiDispatcher.runOnUi(() -> composeMainWindowState.getSelectedStationMessages().replaceRows(
						new java.util.ArrayList<>(selectedCallSignInfoMessageBinding.list())
				));
			}
		});
		tbl_furtherInfoAbtCallsignMSGTable.setItems(selectedCallSignInfoMessageBinding.list());
		applyTruncatedTextCells(
				timeCol, callSignTRCVCol, callSignRCVRCol, qrgTXerCol,
				qrgRXerCol, workedRXCol, workedTXCol
		);
		TableLayoutManager.install(
				tbl_furtherInfoAbtCallsignMSGTable,
				"selected-station-messages",
				chatcontroller.getChatPreferences(),
				layoutAutosave,
				TableLayoutManager.column("time", timeCol),
				TableLayoutManager.column("call-tx", callSignTRCVCol),
				TableLayoutManager.column("call-rx", callSignRCVRCol),
				TableLayoutManager.column("last-qrg-tx", qrgTXerCol),
				TableLayoutManager.column("last-qrg-rx", qrgRXerCol),
				TableLayoutManager.column("message", msgCol).flexible(360),
				TableLayoutManager.column("worked-rx", workedRXCol),
				TableLayoutManager.column("worked-tx", workedTXCol)
		);

		return tbl_furtherInfoAbtCallsignMSGTable;
	}





	/**
	 * Reassembles the selected-station controls into a compact, balanced layout.
	 *
	 * The selected-station message table stays immediately visible above this pane.
	 * This pane only contains the summary and controls below it:
	 * - compact station summary
	 * - action buttons such as Show path / Show on map / Turn antenna / lookups
	 * - score and sked controls
	 * - not-QRV tags
	 *
	 * Existing controls are passed in instead of recreated so all existing event
	 * handlers, bindings and comments in generateFurtherInfoAbtSelectedCallsignBP(...)
	 * remain intact.
	 */
	private VBox initSelectedCallSignCompactControlsPane(
			ChatMember selectedCallSignInfoStageChatMember,
			Label selectedCallSignChatCategoryLabelDesc,
			Label selectedCallSignInfoLblQTFInfo,
			Label selectedCallSignInfoLblQRBInfo,
			Label lblDetectedRxBands,
			HBox priorityRow,
			FlowPane skedRow,
			HBox selectedCallSignPathAndMapButtons,
			Button selectedCallSignTurnAntBtn,
			Button selectedCallSignShowQRZprofile,
			Button selectedCallSignShowQRZCqprofile,
			CheckBox furtherInfoPnl_chkbx_notQRV50,
			CheckBox furtherInfoPnl_chkbx_notQRV70,
			CheckBox furtherInfoPnl_chkbx_notQRV144,
			CheckBox furtherInfoPnl_chkbx_notQRV432,
			CheckBox furtherInfoPnl_chkbx_notQRV23,
			CheckBox furtherInfoPnl_chkbx_notQRV13,
			CheckBox furtherInfoPnl_chkbx_notQRV9,
			CheckBox furtherInfoPnl_chkbx_notQRV6,
			CheckBox furtherInfoPnl_chkbx_notQRV3,
			CheckBox furtherInfoPnl_chkbx_notQRVall
	) {

		for (CheckBox cb : Arrays.asList(
				furtherInfoPnl_chkbx_notQRV50,
				furtherInfoPnl_chkbx_notQRV70,
				furtherInfoPnl_chkbx_notQRV144,
				furtherInfoPnl_chkbx_notQRV432,
				furtherInfoPnl_chkbx_notQRV23,
				furtherInfoPnl_chkbx_notQRV13,
				furtherInfoPnl_chkbx_notQRV9,
				furtherInfoPnl_chkbx_notQRV6,
				furtherInfoPnl_chkbx_notQRV3,
				furtherInfoPnl_chkbx_notQRVall
		)) {
			// Hidden band controls should not reserve space in the compact FlowPane.
			cb.managedProperty().bind(cb.visibleProperty());
		}

		FlowPane stationSummaryFlow = new FlowPane();
		stationSummaryFlow.setHgap(8);
		stationSummaryFlow.setVgap(2);
		stationSummaryFlow.setAlignment(Pos.CENTER_LEFT);
		stationSummaryFlow.setStyle("-fx-padding: 3; -fx-border-color: lightgrey; -fx-border-width: 1;");

		stationSummaryFlow.getChildren().addAll(
				selectedCallSignChatCategoryLabelDesc,
				new Label("|"),
				selectedCallSignInfoLblQTFInfo,
				selectedCallSignInfoLblQRBInfo,
				new Label("Last activity: " + new Utils4KST().time_convertEpochToReadable(
						selectedCallSignInfoStageChatMember.getActivityTimeLastInEpoch() + "")),
				new Label("("
						+ Utils4KST.time_getSecondsBetweenEpochAndNow(
						selectedCallSignInfoStageChatMember.getActivityTimeLastInEpoch() + "") / 60 % 60
						+ " min ago)"),
				lblDetectedRxBands
		);

		FlowPane actionFlow = new FlowPane();
		actionFlow.setHgap(5);
		actionFlow.setVgap(3);
		actionFlow.setAlignment(Pos.CENTER_LEFT);
		actionFlow.setStyle("-fx-padding: 3; -fx-border-color: lightgrey; -fx-border-width: 1;");
		actionFlow.getChildren().addAll(
				selectedCallSignPathAndMapButtons,
				selectedCallSignTurnAntBtn,
				selectedCallSignShowQRZprofile,
				selectedCallSignShowQRZCqprofile
		);

		FlowPane scoreAndSkedFlow = new FlowPane();
		scoreAndSkedFlow.setHgap(5);
		scoreAndSkedFlow.setVgap(3);
		scoreAndSkedFlow.setAlignment(Pos.CENTER_LEFT);
		scoreAndSkedFlow.setStyle("-fx-padding: 3; -fx-border-color: lightgrey; -fx-border-width: 1;");
		scoreAndSkedFlow.getChildren().addAll(priorityRow, skedRow);

		FlowPane notQrvFlow = new FlowPane();
		notQrvFlow.setHgap(5);
		notQrvFlow.setVgap(2);
		notQrvFlow.setAlignment(Pos.CENTER_LEFT);
		notQrvFlow.setStyle("-fx-padding: 3; -fx-border-color: lightgrey; -fx-border-width: 1;");
		notQrvFlow.getChildren().addAll(
				new Label("Not QRV:"),
				furtherInfoPnl_chkbx_notQRV50,
				furtherInfoPnl_chkbx_notQRV70,
				furtherInfoPnl_chkbx_notQRV144,
				furtherInfoPnl_chkbx_notQRV432,
				furtherInfoPnl_chkbx_notQRV23,
				furtherInfoPnl_chkbx_notQRV13,
				furtherInfoPnl_chkbx_notQRV9,
				furtherInfoPnl_chkbx_notQRV6,
				furtherInfoPnl_chkbx_notQRV3,
				furtherInfoPnl_chkbx_notQRVall
		);

		VBox selectedCallSignCompactControlsPane = new VBox(4);
		selectedCallSignCompactControlsPane.setStyle("-fx-padding: 3;");
		selectedCallSignCompactControlsPane.getChildren().addAll(
				stationSummaryFlow,
				actionFlow,
				scoreAndSkedFlow,
				notQrvFlow
		);

		return selectedCallSignCompactControlsPane;
	}

	/**
	 * Builds the lower global-message area.
	 *
	 * This TabPane is intentionally independent from the selected ChatMember.
	 * It contains global message streams only:
	 * - public/CQ messages
	 * - DXCluster messages
	 * - messages between other stations ("QSO of the other")
	 *
	 * The already initialized public-message table is passed in so its existing
	 * context menu and selection handling remain unchanged.
	 *
	 * DXCluster and QSO-of-the-other get their own TableViews on the same backing
	 * lists. This is important because the existing separate "Cluster & QSO of the other"
	 * window can keep using its own TableViews at the same time.
	 */
	private TabPane initBottomGlobalMessageTabPane(TableView<ChatMessage> tbl_generalMessageTable) {

		TabPane bottomMessageTabs = new TabPane();
		bottomMessageTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		bottomMessageTabs.setTabMinHeight(22);
		bottomMessageTabs.setTabMaxHeight(24);

		Tab publicMessagesTab = new Tab("Public messages");
		publicMessagesTab.setTooltip(new Tooltip("Public/CQ messages from the chat."));
		publicMessagesTab.setContent(tbl_generalMessageTable);

		Tab dxClusterMessagesTab = new Tab("DXCluster messages");
		dxClusterMessagesTab.setTooltip(new Tooltip("DXCluster spots."));
		dxClusterMessagesTab.setContent(initDXClusterTable("dx-cluster-main"));

		Tab qsoOfTheOtherTab = new Tab("QSO of the other");
		qsoOfTheOtherTab.setTooltip(new Tooltip("Messages between other stations. This view is not tied to the selected ChatMember."));
		qsoOfTheOtherTab.setContent(initChatToOtherMSGTable("qso-other-main"));

		bottomMessageTabs.getTabs().addAll(publicMessagesTab, dxClusterMessagesTab, qsoOfTheOtherTab);
		bottomMessageTabs.getSelectionModel().select(publicMessagesTab);

		return bottomMessageTabs;
	}

	/**
	 * initializes the tableview in which the cq- and beacon-texts are shown
	 * 
	 * @return
	 */
	private TableView initChatGeneralMSGTable() {

		TableView<ChatMessage> tbl_generalMSGTable = new TableView<ChatMessage>();
//		tbl_generalMSGTable.setTooltip(new Tooltip("General messages are shown here (handle it like CQ messages)"));

		TableColumn<ChatMessage, String> timeCol = new TableColumn<ChatMessage, String>("Time");
		timeCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty time = new SimpleStringProperty();

				time.setValue(new Utils4KST()
						.time_convertEpochToReadable(cellDataFeatures.getValue().getMessageGeneratedTime()));

				return time;
			}
		});

		TableColumn<ChatMessage, String> callSignCol = new TableColumn<ChatMessage, String>("Callsign");
		callSignCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty callSign = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

					callSign.setValue(cellDataFeatures.getValue().getSender().getCallSign());
				} else {

					callSign.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return callSign;
			}
		});

		TableColumn<ChatMessage, String> nameCol = new TableColumn<ChatMessage, String>("Name");
		nameCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty name = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

					name.setValue(cellDataFeatures.getValue().getSender().getName());
				} else {

					name.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return name;
			}
		});

		TableColumn<ChatMessage, String> qrgCol = new TableColumn<ChatMessage, String>("Last QRG");
		qrgCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {


			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				StringProperty qrg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

//					qrg.setValue(cellDataFeatures.getValue().getSender().getFrequency());
					qrg.setValue(cellDataFeatures.getValue().getSender().getFrequency().get());
				} else {

					qrg.setValue("");
				}
				return qrg;
			}
		});

		applyQrgUiFormatting(qrgCol); //fills ending 0 to format the qrgs pretty


		TableColumn<ChatMessage, String> msgCol = new TableColumn<ChatMessage, String>("Message");
		msgCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty msg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getMessageText() != null) {

					msg.setValue(cellDataFeatures.getValue().getMessageText());
				} else {

					msg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return msg;
			}
		});
		msgCol.prefWidthProperty().bind(tbl_generalMSGTable.widthProperty().divide(2));

		msgCol.setCellFactory(column ->
				new MessageTextTableCell<>(
						getHostServices()::showDocument,
						messageText -> {
							String ownCallsign = chatcontroller
									.getChatPreferences()
									.getStn_loginCallSign();

							return messageText != null
									&& ownCallsign != null
									&& !ownCallsign.isBlank()
									&& messageText
									.toUpperCase(Locale.ROOT)
									.contains(ownCallsign.toUpperCase(Locale.ROOT));
						},
						"defaultText-column",
						"messageToMe-column"
				)
		);

		TableColumn<ChatMessage, String> categoryCol = new TableColumn<ChatMessage, String>("Category");
		categoryCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				StringProperty category = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

					category.setValue(cellDataFeatures.getValue().getSender().getChatCategory().getChatCategoryName(cellDataFeatures.getValue().getSender().getChatCategory().getCategoryNumber()));
				} else {

					category.setValue("UNKNOWN! Report BUG!"); //TODO: Better bugtracking should follow
				}
				return category;
			}
		});

		tbl_generalMSGTable.getColumns().addAll(timeCol, callSignCol, nameCol, msgCol, qrgCol, categoryCol);

		tbl_generalMSGTable.setItems(this.<ChatMessage>derivedBinding(
				chatcontroller::toAllMessages,
				chatcontroller.getLst_globalChatMessageList()).list());
		applyTruncatedTextCells(timeCol, callSignCol, nameCol, categoryCol);
		TableLayoutManager.install(
				tbl_generalMSGTable,
				"public-messages",
				chatcontroller.getChatPreferences(),
				layoutAutosave,
				TableLayoutManager.column("time", timeCol),
				TableLayoutManager.column("callsign", callSignCol),
				TableLayoutManager.column("name", nameCol).maximumInitialWidth(220),
				TableLayoutManager.column("message", msgCol).flexible(360),
				TableLayoutManager.column("last-qrg", qrgCol),
				TableLayoutManager.column("category", categoryCol)
		);

		tbl_generalMSGTable.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {

				//we need to overdrive the Enter pressed as it should (in the whole scene) send the text!
				if (event.getCode() == KeyCode.ENTER) {

					event.consume();
					sendButton.fire();
				}

			}
		});

		return tbl_generalMSGTable;
	}

	private TableView<ChatMessage> initChatprivateMSGTable() {

		TableView<ChatMessage> tbl_privateMSGTable = new TableView<ChatMessage>();
//		tbl_privateMSGTable.setTooltip(new Tooltip("Private messages to you are shown here"));

		TableColumn<ChatMessage, String> timeCol = new TableColumn<ChatMessage, String>("Time");


		timeCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty time = new SimpleStringProperty();

				time.setValue(new Utils4KST()
						.time_convertEpochToReadable(cellDataFeatures.getValue().getMessageGeneratedTime()));

				//TODO: Farbe soll rein
				return time;
			}
		});

		TableColumn<ChatMessage, String> callSignCol = new TableColumn<ChatMessage, String>("Callsign");
		callSignCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty callSign = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

					callSign.setValue(cellDataFeatures.getValue().getSender().getCallSign());
				} else {

					callSign.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return callSign;
			}
		});

//		callSignCol.setCellFactory(new Callback<TableColumn<ChatMessage, String>, TableCell<ChatMessage, String>>() {
//			public TableCell call(TableColumn param) {
//				return new TableCell<ChatMessage, String>() {
//
//					@Override
//					public void updateItem(String item, boolean empty) {
//						super.updateItem(item, empty);
//						if (!isEmpty()) {
////							this.setTextFill(Color.BLACK);
//							// Get fancy and change color based on data
//							if (item.contains(chatcontroller.getChatPreferences().getStn_loginCallSign())) {
//								this.setTextFill(Color.GREEN);
//
//							}
//							setText(item);
//						}
//					}
//				};
//			}
//		});

		TableColumn<ChatMessage, String> nameCol = new TableColumn<ChatMessage, String>("Name");
		nameCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty name = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

					name.setValue(cellDataFeatures.getValue().getSender().getName());
				} else {

					name.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return name;
			}
		});

		TableColumn<ChatMessage, String> qraCol = new TableColumn<ChatMessage, String>("QRA");
		qraCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty qra = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

					qra.setValue(cellDataFeatures.getValue().getSender().getQra());
				} else {

					qra.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return qra;
			}
		});

		TableColumn<ChatMessage, String> qrgCol = new TableColumn<ChatMessage, String>("Last known QRG");
		qrgCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				StringProperty qrg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

//					qrg.setValue(cellDataFeatures.getValue().getSender().getFrequency());
					qrg.setValue(cellDataFeatures.getValue().getSender().getFrequency().get());
				} else {

					qrg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return qrg;
			}
		});
		applyQrgUiFormatting(qrgCol); //fills ending 0 to format the qrgs pretty


//		TableColumn<ChatMessage, String> msgCol = new TableColumn<ChatMessage, String>("Message");
//		msgCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {
//
//			@Override
//			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
//				SimpleStringProperty msg = new SimpleStringProperty();
//
//				if (cellDataFeatures.getValue().getMessageText() != null) {
//
//					msg.setValue(cellDataFeatures.getValue().getMessageText());
//				} else {
//
//					msg.setValue("");// TODO: Prevents a bug of not setting all values as a default
//				}
//				return msg;
//			}
//		});

		TableColumn<ChatMessage, String> msgCol = new TableColumn<ChatMessage, String>("Message");
		msgCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty msg = new SimpleStringProperty();

				if (cellDataFeatures.getValue() != null) {
					msg.setValue(chatcontroller.formatChatMessageTextForDisplay(cellDataFeatures.getValue()));
				} else {
					msg.setValue("");
				}

				return msg;
			}
		});
		msgCol.prefWidthProperty().bind(tbl_privateMSGTable.widthProperty().divide(2.5));

		msgCol.setCellFactory(column ->
				new MessageTextTableCell<>(getHostServices()::showDocument)
		);

		TableColumn<ChatMessage, String> airScoutCol = new TableColumn<ChatMessage, String>("AP [minutes / pot%]");
		airScoutCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {
			
			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty airPlaneInfo = new SimpleStringProperty();

				try {
					if (cellDataFeatures.getValue().getSender().getAirPlaneReflectInfo().getRisingAirplanes() == null) {
						airPlaneInfo.setValue("nil");
					}

					else if (cellDataFeatures.getValue().getSender().getAirPlaneReflectInfo().getRisingAirplanes().size() <= 0) {
						airPlaneInfo.setValue("nil");
					} else {
						String apInfoText = ""
								+ cellDataFeatures.getValue().getSender().getAirPlaneReflectInfo().getRisingAirplanes().get(0)
								.getArrivingDurationMinutes()
								+ " (" + cellDataFeatures.getValue().getSender().getAirPlaneReflectInfo().getRisingAirplanes().get(0)
								.getPotential()
								+ "%)";
//
//
						if (cellDataFeatures.getValue().getSender().getAirPlaneReflectInfo().getRisingAirplanes().size() > 1) {
							apInfoText += " / "
									+ cellDataFeatures.getValue().getSender().getAirPlaneReflectInfo().getRisingAirplanes().get(1)
									.getArrivingDurationMinutes()
									+ " (" + cellDataFeatures.getValue().getSender().getAirPlaneReflectInfo().getRisingAirplanes()
									.get(1).getPotential()
									+ "%)";
						}

						airPlaneInfo.setValue(apInfoText);
					}
				} catch (NullPointerException thereIsNoApReflectionInfo) {
					//e.g. in case of mycall it´s not possible to set!
				}

				return airPlaneInfo;

			}
		});
		/**
		 * HIGH EXPERIMENTAL::::::::
		 */
		airScoutCol.setCellFactory(new Callback<TableColumn<ChatMessage, String>, TableCell<ChatMessage, String>>() {
			public TableCell call(TableColumn param) {
				return new TruncatedTextTableCell<ChatMessage>() {

					@Override
					public void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						if (!isEmpty()) {
							this.setTextFill(Color.BLACK);
							// Get fancy and change color based on data

							try {
								if (item.contains("100%")) {
									this.setTextFill(Color.BLUEVIOLET);
								} else if (item.contains("75%") && !item.contains("100%")) {
									this.setTextFill(Color.RED);
								} else if (item.contains("50%") && ((!item.contains("100%")) || (!item.contains("75%")))) {
									this.setTextFill(Color.ORANGE);
								}

							} catch (Exception exc) {

							}

						}
					}
				};
			}
		});
		/**
		 * END HIGH EXPERIMENTAL::::::::
		 */

		TableColumn<ChatMessage, String> qrbCol = new TableColumn<ChatMessage, String>("QRB");
		qrbCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty qrb = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null && !cellDataFeatures.getValue().getSender().getCallSign().equals(chatcontroller.getChatPreferences().getStn_loginCallSign())) {
					//do not calc for your own callsign as this will be NaN

					if (!cellDataFeatures.getValue().getSender().getCallSign().equals(chatcontroller.getChatPreferences().getStn_loginCallSign())) {

						try {
							qrb.setValue(cellDataFeatures.getValue().getSender().getQrb().intValue() +" km (" + cellDataFeatures.getValue().getSender().getQTFdirection().intValue() + ")°"); //make int for less space
						} catch (Exception nullOrFormatExc) {
							System.out.println("KST4ContestApp: <<<catched error>>>: qrb was faulty" + nullOrFormatExc.getMessage() + " / " + nullOrFormatExc.getStackTrace());
						}
					}

				} else {

					qrb.setValue("");//Prevents a bug of not setting all values as a default
				}
				return qrb;
			}
		});

		TableColumn<ChatMessage, String> categoryCol = new TableColumn<ChatMessage, String>("Category");
		categoryCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty category = new SimpleStringProperty();

				try {
					category.setValue(cellDataFeatures.getValue().getChatCategory().getChatCategoryName(cellDataFeatures.getValue().getChatCategory().getCategoryNumber()));

				} catch (Exception nullpointerExcForServerMessages) {

				}

				return category;
			}
		});

		tbl_privateMSGTable.getColumns().addAll(timeCol, callSignCol, nameCol, qraCol, qrbCol, msgCol, qrgCol, airScoutCol, categoryCol);

		/*
		 * The monitoring list is a second source here: adding a monitored callsign
		 * changes which messages this tab shows, without a new message arriving.
		 */
		tbl_privateMSGTable.setItems(this.<ChatMessage>derivedBinding(
				chatcontroller::toMeMessages,
				chatcontroller.getLst_globalChatMessageList(),
				chatcontroller.getLstNotify_QSOSniffer_sniffedCallSignList()).list());
		applyTruncatedTextCells(timeCol, callSignCol, nameCol, qraCol, qrbCol, categoryCol);
		TableLayoutManager.install(
				tbl_privateMSGTable,
				"private-messages",
				chatcontroller.getChatPreferences(),
				layoutAutosave,
				TableLayoutManager.column("time", timeCol),
				TableLayoutManager.column("callsign", callSignCol),
				TableLayoutManager.column("name", nameCol).maximumInitialWidth(220),
				TableLayoutManager.column("qra", qraCol),
				TableLayoutManager.column("qrb", qrbCol),
				TableLayoutManager.column("message", msgCol).flexible(360),
				TableLayoutManager.column("last-qrg", qrgCol),
				TableLayoutManager.column("airscout", airScoutCol).maximumInitialWidth(190),
				TableLayoutManager.column("category", categoryCol)
		);

		tbl_privateMSGTable.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {

				//we need to overdrive the Enter pressed as it should (in the whole scene) send the text!
				if (event.getCode() == KeyCode.ENTER) {

					event.consume();
					sendButton.fire();
				}

			}
		});

		// Color new private messages and restore the normal row style after five minutes.
		tbl_privateMSGTable.setRowFactory(tv -> new TableRow<ChatMessage>() {
			@Override
			protected void updateItem(
					final ChatMessage item,
					final boolean empty
			) {
				super.updateItem(item, empty);

				getStyleClass().removeAll(
						PrivateMessageRowStyleResolver.knownStyleClasses()
				);

				if (empty || item == null || item.getSender() == null) {
					return;
				}

				final String ownCallsign = chatcontroller
						.getChatPreferences()
						.getStn_loginCallSign();

				final boolean ownMessage = Objects.equals(
						item.getSender().getCallSign(),
						ownCallsign
				);

				final String styleClass;

				if (ownMessage) {
					styleClass = PrivateMessageRowStyleResolver
							.resolveStyleClass(true, 0);
				} else {
					try {
						final long ageSeconds = new Utils4KST()
								.time_generateCurrentEpochTime()
								- Long.parseLong(
										item.getMessageGeneratedTime()
								);

						styleClass = PrivateMessageRowStyleResolver
								.resolveStyleClass(false, ageSeconds);
					} catch (NumberFormatException exception) {
						return;
					}
				}

				if (styleClass != null) {
					getStyleClass().add(styleClass);
				}

			}
		});

		return tbl_privateMSGTable;
	}

	private TableView<ClusterMessage> initDXClusterTable(String layoutId) {

		TableView<ClusterMessage> tbl_DXCTable = new TableView<ClusterMessage>();
//		tbl_DXCTable.setTooltip(new Tooltip("Cluster Messages are shown here"));

		TableColumn<ClusterMessage, String> timeCol = new TableColumn<ClusterMessage, String>("Time");
		timeCol.setCellValueFactory(new Callback<CellDataFeatures<ClusterMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ClusterMessage, String> cellDataFeatures) {
				SimpleStringProperty time = new SimpleStringProperty();

				time.setValue(
						new Utils4KST().time_convertEpochToReadable(cellDataFeatures.getValue().getTimeGenerated()));

				return time;
			}
		});

		TableColumn<ClusterMessage, String> callSignCol = new TableColumn<ClusterMessage, String>("Call tx");
		callSignCol
				.setCellValueFactory(new Callback<CellDataFeatures<ClusterMessage, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ClusterMessage, String> cellDataFeatures) {
						SimpleStringProperty callSign = new SimpleStringProperty();

						if (cellDataFeatures.getValue().getSender() != null) {

							callSign.setValue(cellDataFeatures.getValue().getSender().getCallSign());
						} else {

							callSign.setValue("");// TODO: Prevents a bug of not setting all values as a default
						}
						return callSign;
					}
				});

		TableColumn<ClusterMessage, String> locTXCol = new TableColumn<ClusterMessage, String>("LOC tx");
		locTXCol.setCellValueFactory(new Callback<CellDataFeatures<ClusterMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ClusterMessage, String> cellDataFeatures) {
				SimpleStringProperty locTX = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

					locTX.setValue(cellDataFeatures.getValue().getSender().getQra());
				} else {

					locTX.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return locTX;
			}
		});

		TableColumn<ClusterMessage, String> callSignRXCol = new TableColumn<ClusterMessage, String>("Call rx");
		callSignRXCol
				.setCellValueFactory(new Callback<CellDataFeatures<ClusterMessage, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ClusterMessage, String> cellDataFeatures) {
						SimpleStringProperty callSignRX = new SimpleStringProperty();

						if (cellDataFeatures.getValue().getReceiver() != null) {

							callSignRX.setValue(cellDataFeatures.getValue().getReceiver().getCallSign());
						} else {

							callSignRX.setValue("");// TODO: Prevents a bug of not setting all values as a default
						}
						return callSignRX;
					}
				});

		TableColumn<ClusterMessage, String> locRXCol = new TableColumn<ClusterMessage, String>("LOC rx");
		locRXCol.setCellValueFactory(new Callback<CellDataFeatures<ClusterMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ClusterMessage, String> cellDataFeatures) {
				SimpleStringProperty locRX = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getReceiver() != null) {

					locRX.setValue(cellDataFeatures.getValue().getReceiver().getQra());
				} else {

					locRX.setValue("");
				}
				return locRX;
			}
		});

		TableColumn<ClusterMessage, String> qrgCol = new TableColumn<ClusterMessage, String>("QRG");
		qrgCol.setCellValueFactory(new Callback<CellDataFeatures<ClusterMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ClusterMessage, String> cellDataFeatures) {
				StringProperty qrg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getReceiver() != null) {

//					qrg.setValue(cellDataFeatures.getValue().getReceiver().getFrequency());
					qrg.setValue(cellDataFeatures.getValue().getReceiver().getFrequency().get());
				} else {

					qrg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return qrg;
			}
		});
		applyQrgUiFormatting(qrgCol); //fills ending 0 to format the qrgs pretty


		TableColumn<ClusterMessage, String> msgCol = new TableColumn<ClusterMessage, String>("Message");
		msgCol.setCellValueFactory(new Callback<CellDataFeatures<ClusterMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ClusterMessage, String> cellDataFeatures) {
				SimpleStringProperty msg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getMessageInhibited() != null) {

					msg.setValue(cellDataFeatures.getValue().getMessageInhibited());
				} else {

					msg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return msg;
			}
		});

		msgCol.setCellFactory(column ->
				new MessageTextTableCell<>(getHostServices()::showDocument)
		);

		TableColumn<ClusterMessage, String> workedCol = new TableColumn<ClusterMessage, String>("wkd");
		workedCol
				.setCellValueFactory(new Callback<CellDataFeatures<ClusterMessage, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ClusterMessage, String> cellDataFeatures) {
						SimpleStringProperty wkd = new SimpleStringProperty();

						wkd.setValue(cellDataFeatures.getValue().isReceiverWkd() + "");

						if (cellDataFeatures.getValue().isReceiverWkd()) {
							wkd.setValue("X");
						} else {
							wkd.setValue("");
						}

						return wkd;
					}
				});

		tbl_DXCTable.getColumns().addAll(timeCol, callSignCol, locTXCol, callSignRXCol, locRXCol, qrgCol, msgCol,
				workedCol);

		ObservableList<ClusterMessage> clusterMSGList = mirrorOf(chatcontroller.getLst_clusterMemberList());
		tbl_DXCTable.setItems(clusterMSGList);
		applyTruncatedTextCells(
				timeCol, callSignCol, locTXCol, callSignRXCol,
				locRXCol, workedCol
		);
		TableLayoutManager.install(
				tbl_DXCTable,
				layoutId,
				chatcontroller.getChatPreferences(),
				layoutAutosave,
				TableLayoutManager.column("time", timeCol),
				TableLayoutManager.column("call-tx", callSignCol),
				TableLayoutManager.column("locator-tx", locTXCol),
				TableLayoutManager.column("call-rx", callSignRXCol),
				TableLayoutManager.column("locator-rx", locRXCol),
				TableLayoutManager.column("qrg", qrgCol),
				TableLayoutManager.column("message", msgCol).flexible(360),
				TableLayoutManager.column("worked", workedCol)
		);

		return tbl_DXCTable;
	}

	private TableView<ChatMessage> initChatToOtherMSGTable(String layoutId) {

		TableView<ChatMessage> tbl_toOtherMSGTable = new TableView<ChatMessage>();
//		tbl_toOtherMSGTable.setTooltip(new Tooltip("Messages between other member are shown here"));

		TableColumn<ChatMessage, String> timeCol = new TableColumn<ChatMessage, String>("Time");
		timeCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty time = new SimpleStringProperty();

				time.setValue(new Utils4KST()
						.time_convertEpochToReadable(cellDataFeatures.getValue().getMessageGeneratedTime()));

				return time;
			}
		});

		TableColumn<ChatMessage, String> callSignTRCVCol = new TableColumn<ChatMessage, String>("Call TX");
		callSignTRCVCol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
						SimpleStringProperty callSign = new SimpleStringProperty();

						if (cellDataFeatures.getValue().getSender() != null) {

							callSign.setValue(cellDataFeatures.getValue().getSender().getCallSign());
						} else {

							callSign.setValue("");// TODO: Prevents a bug of not setting all values as a default
						}
						return callSign;
					}
				});

		TableColumn<ChatMessage, String> callSignRCVRCol = new TableColumn<ChatMessage, String>("Call RX");
		callSignRCVRCol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
						SimpleStringProperty callTX = new SimpleStringProperty();

						if (cellDataFeatures.getValue().getReceiver().getCallSign() != null) {

							callTX.setValue(cellDataFeatures.getValue().getReceiver().getCallSign());
						} else {

							callTX.setValue("");// TODO: Prevents a bug of not setting all values as a default
						}
						return callTX;
					}
				});

//		TableColumn<ChatMessage, String> nameCol = new TableColumn<ChatMessage, String>("Name");
//		nameCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {
//
//			@Override
//			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
//				SimpleStringProperty name = new SimpleStringProperty();
//
//				if (cellDataFeatures.getValue().getSender() != null) {
//
//					name.setValue(cellDataFeatures.getValue().getSender().getName());
//				} else {
//
//					name.setValue("");// TODO: Prevents a bug of not setting all values as a default
//				}
//				return name;
//			}
//		});

		TableColumn<ChatMessage, String> qrgTXerCol = new TableColumn<ChatMessage, String>("Last QRG TX");
		qrgTXerCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				StringProperty qrg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

//					qrg.setValue(cellDataFeatures.getValue().getSender().getFrequency());
					qrg.setValue(cellDataFeatures.getValue().getSender().getFrequency().get());
				} else {

					qrg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return qrg;
			}
		});

		TableColumn<ChatMessage, String> qrgRXerCol = new TableColumn<ChatMessage, String>("Last QRG RX");
		qrgRXerCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				StringProperty qrg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getReceiver() != null) {

//					qrg.setValue(cellDataFeatures.getValue().getReceiver().getFrequency());
					qrg.setValue(cellDataFeatures.getValue().getReceiver().getFrequency().get());
					
				} else {

					qrg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return qrg;
			}
		});

		TableColumn<ChatMessage, String> msgCol = new TableColumn<ChatMessage, String>("Message");
		msgCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty msg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getMessageText() != null) {

					msg.setValue(cellDataFeatures.getValue().getMessageText());
				} else {

					msg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return msg;
			}
		});
		msgCol.prefWidthProperty().bind(tbl_toOtherMSGTable.widthProperty().divide(2));

		msgCol.setCellFactory(column ->
				new MessageTextTableCell<>(getHostServices()::showDocument)
		);


		TableColumn<ChatMessage, String> workedRXCol = new TableColumn<ChatMessage, String>("wkd RX?");
		workedRXCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getReceiver().isWorked()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});

		TableColumn<ChatMessage, String> workedTXCol = new TableColumn<ChatMessage, String>("wkd TX?");
		workedTXCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender().isWorked()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});

		TableColumn<ChatMessage, String> categoryCol = new TableColumn<ChatMessage, String>("Category");
		categoryCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				StringProperty category = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

//					qrg.setValue(cellDataFeatures.getValue().getSender().getFrequency());
					category.setValue(cellDataFeatures.getValue().getSender().getChatCategory().getChatCategoryName(cellDataFeatures.getValue().getSender().getChatCategory().getCategoryNumber()));

				} else {

					category.setValue("UNKNOWN! Report BUG!"); //TODO: Better bugtracking should follow
				}
				return category;
			}
		});


		tbl_toOtherMSGTable.getColumns().addAll(timeCol, callSignTRCVCol, qrgTXerCol, workedTXCol, callSignRCVRCol,
				qrgRXerCol, workedRXCol, msgCol, categoryCol);

		tbl_toOtherMSGTable.setItems(this.<ChatMessage>derivedBinding(
				chatcontroller::toOtherMessages,
				chatcontroller.getLst_globalChatMessageList()).list());
		applyTruncatedTextCells(
				timeCol, callSignTRCVCol, qrgTXerCol, workedTXCol,
				callSignRCVRCol, qrgRXerCol, workedRXCol, categoryCol
		);
		TableLayoutManager.install(
				tbl_toOtherMSGTable,
				layoutId,
				chatcontroller.getChatPreferences(),
				layoutAutosave,
				TableLayoutManager.column("time", timeCol),
				TableLayoutManager.column("call-tx", callSignTRCVCol),
				TableLayoutManager.column("last-qrg-tx", qrgTXerCol),
				TableLayoutManager.column("worked-tx", workedTXCol),
				TableLayoutManager.column("call-rx", callSignRCVRCol),
				TableLayoutManager.column("last-qrg-rx", qrgRXerCol),
				TableLayoutManager.column("worked-rx", workedRXCol),
				TableLayoutManager.column("message", msgCol).flexible(360),
				TableLayoutManager.column("category", categoryCol)
		);

		return tbl_toOtherMSGTable;
	}

	private TableView<String> initShortcutTable() {

		TableView<String> tbl_txtShorts = new TableView<String>();
		tbl_txtShorts.setTooltip(new Tooltip("Personalize your shortcut-buttons here"));

		TableColumn<String, String> ShortCol = new TableColumn<String, String>("Shortcut-Buttontext");
		ShortCol.setCellValueFactory(new Callback<CellDataFeatures<String, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<String, String> cellDataFeatures) {
				SimpleStringProperty shortCT = new SimpleStringProperty();
				shortCT.setValue(cellDataFeatures.getValue());
				return shortCT;
			}
		});
		ShortCol.setCellFactory(TextFieldTableCell.forTableColumn());

		ShortCol.setOnEditCommit(new EventHandler<CellEditEvent<String, String>>() {
			@Override
			public void handle(CellEditEvent<String, String> t) {

				String newValue = t.getNewValue();
				if (newValue == null || newValue.isBlank()) {
					t.getTableView().getItems().remove(t.getTablePosition().getRow());
				} else {
					t.getTableView().getItems().set(t.getTablePosition().getRow(), newValue);
				}

				refreshShortcutButtons();
			}
		});

		tbl_txtShorts.getColumns().addAll(ShortCol);

		tbl_txtShorts.setEditable(true);
		tbl_txtShorts.setItems(mirrorOf(chatcontroller.getChatPreferences().getLst_txtShortCutBtnList()));

		return tbl_txtShorts;
	} // TODO: Textsnippets table


//	private BorderPane initTopPriorityListPane(TableView<ChatMember> tbl_chatMember, TextField txt_chatMessageUserInput) {
//
//		BorderPane pane = new BorderPane();
//		pane.setStyle("-fx-padding: 3;");
//
//		Label header = new Label("Top priority candidates");
//		header.getStyleClass().add("label");
//
//		ListView<kst4contest.controller.ScoreService.TopCandidate> listView = new ListView<>();
//		listView.setItems(mirrorOf(chatcontroller.getScoreService().topCandidates()));
//
//		listView.setCellFactory(lv -> new ListCell<>() {
//			@Override
//			protected void updateItem(kst4contest.controller.ScoreService.TopCandidate item, boolean empty) {
//				super.updateItem(item, empty);
//				if (empty || item == null) {
//					setText(null);
//					return;
//				}
//				// Keep it compact; score is mainly evaluated in FurtherInfo
//				setText(item.getDisplayCallSign() + "  |  score " + String.format(java.util.Locale.US, "%.0f", item.getScore()));
//			}
//		});
//
//		listView.setOnMouseClicked(evt -> {
//			if (evt.getClickCount() < 1) return;
//			kst4contest.controller.ScoreService.TopCandidate c = listView.getSelectionModel().getSelectedItem();
//			if (c == null) return;
//
//			ChatMember resolved = resolveChatMemberForTopCandidate(c);
//			if (resolved == null) return;
//
//			// Try to select in table (reuses existing selection logic)
//			if (tbl_chatMember.getItems().contains(resolved)) {
//				tbl_chatMember.getSelectionModel().select(resolved);
//				tbl_chatMember.scrollTo(resolved);
//			} else {
//				// Fallback: if filtered out, still show FurtherInfo + prepare /cq
//				selectedCallSignInfoStageChatMember = resolved;
//				chatcontroller.getScoreService().setSelectedChatMember(selectedCallSignInfoStageChatMember);
//
//				selectedCallSignFurtherInfoPane.getChildren().setAll(generateFurtherInfoAbtSelectedCallsignBP(resolved));
//				txt_chatMessageUserInput.clear();
//				txt_chatMessageUserInput.setText("/cq " + resolved.getCallSign() + " ");
//				txt_chatMessageUserInput.requestFocus();
//				txt_chatMessageUserInput.selectEnd();
//
//				// Keep ScoreService selection in sync
//				chatcontroller.getScoreService().setSelectedChatMember(resolved);
//			}
//		});
//
//		pane.setTop(header);
//		pane.setCenter(listView);
//		return pane;
//	}

	/**
	 * Builds the compact priority candidate area for the right side of the main UI.
	 *
	 * The former implementation showed the complete priority list permanently.
	 * That used a lot of vertical space although normally only the first one or two
	 * candidates are relevant during live operation.
	 *
	 * New behaviour:
	 * - show only Top 1 and Top 2 directly in the main window
	 * - click Top 1/Top 2 to select that candidate immediately
	 * - use "more" to open the full priority list in a separate window
	 */
	private BorderPane initTopPriorityListPane(TableView<ChatMember> tbl_chatMember, TextField txt_chatMessageUserInput) {

		BorderPane pane = new BorderPane();
		pane.setStyle("-fx-padding: 3; -fx-border-color: lightgrey; -fx-border-width: 1;");

		Label header = new Label("Priority:");
		header.setMinWidth(48);

		Button top1Button = new Button("1 -");
		Button top2Button = new Button("2 -");
		Button moreButton = new Button("more");

		top1Button.setMaxWidth(Double.MAX_VALUE);
		top2Button.setMaxWidth(Double.MAX_VALUE);

		HBox.setHgrow(top1Button, Priority.ALWAYS);
		HBox.setHgrow(top2Button, Priority.ALWAYS);

		HBox row = new HBox(4, header, top1Button, top2Button, moreButton);
		row.setAlignment(Pos.CENTER_LEFT);

		Runnable refreshButtons = () -> {
			List<kst4contest.controller.ScoreService.TopCandidate> items =
					chatcontroller.getScoreService().topCandidates().snapshot();

			updateTopCandidateButton(top1Button, items, 0);
			updateTopCandidateButton(top2Button, items, 1);
		};

		chatcontroller.getScoreService().topCandidates().addListener(candidates -> refreshButtons.run());

		refreshButtons.run();

		top1Button.setOnAction(e -> selectTopCandidateAt(0, tbl_chatMember, txt_chatMessageUserInput));
		top2Button.setOnAction(e -> selectTopCandidateAt(1, tbl_chatMember, txt_chatMessageUserInput));
		moreButton.setOnAction(e -> showTopPriorityCandidatesWindow(tbl_chatMember, txt_chatMessageUserInput));

		pane.setCenter(row);
		pane.setMinHeight(38);
		pane.setPrefHeight(42);
		pane.setMaxHeight(58);
		SplitPane.setResizableWithParent(pane, Boolean.FALSE);

		return pane;
	}

	/**
	 * Updates one of the compact priority buttons from the current TopCandidate list.
	 * Disabled buttons indicate that not enough candidates are currently available.
	 */
	private void updateTopCandidateButton(
			Button button,
			List<kst4contest.controller.ScoreService.TopCandidate> items,
			int index
	) {
		if (items == null || items.size() <= index || items.get(index) == null) {
			button.setText((index + 1) + " -");
			button.setDisable(true);
			button.setTooltip(null);
			return;
		}

		kst4contest.controller.ScoreService.TopCandidate candidate = items.get(index);

		button.setText(String.format(
				java.util.Locale.US,
				"%d %s %.0f",
				index + 1,
				candidate.getDisplayCallSign(),
				candidate.getScore()
		));
		button.setDisable(false);
		button.setTooltip(new Tooltip("Select " + candidate.getDisplayCallSign() + " from priority candidates"));
	}

	/**
	 * Selects the TopCandidate at the given index, if available.
	 */
	private void selectTopCandidateAt(
			int index,
			TableView<ChatMember> tbl_chatMember,
			TextField txt_chatMessageUserInput
	) {
		List<kst4contest.controller.ScoreService.TopCandidate> items =
				chatcontroller.getScoreService().topCandidates().snapshot();

		if (items == null || items.size() <= index) {
			return;
		}

		selectTopCandidate(items.get(index), tbl_chatMember, txt_chatMessageUserInput);
	}

	/**
	 * Resolves and selects a priority candidate.
	 *
	 * <p>The common selection helper is used deliberately. It updates Further Info,
	 * prepares the directed message and selects the corresponding table row when
	 * the row is currently visible. A candidate hidden by an active table filter
	 * remains usable without changing or resetting that filter.</p>
	 */
	private void selectTopCandidate(
			kst4contest.controller.ScoreService.TopCandidate candidate,
			TableView<ChatMember> tbl_chatMember,
			TextField txt_chatMessageUserInput
	) {
		if (candidate == null) {
			return;
		}

		ChatMember resolved = resolveChatMemberForTopCandidate(candidate);

		if (resolved == null) {
			return;
		}

		/*
		 * This is an explicit operator action and therefore follows the same path
		 * as a timeline click. The exact callsign and category stored in the
		 * TopCandidate remain authoritative.
		 *
		 * focusChatMemberAndPrepareCq() also handles candidates which are currently
		 * hidden by a FilteredList. In that case the table row cannot be selected,
		 * but Further Info, ScoreService selection and the prepared /cq message are
		 * still updated.
		 */
		focusChatMemberAndPrepareCq(resolved);
	}

	/**
	 * Opens the complete priority candidate list in a separate window.
	 *
	 * This keeps the main UI compact while still making the full list available when
	 * the operator wants to inspect more than the first two candidates.
	 */
	private void showTopPriorityCandidatesWindow(
			TableView<ChatMember> tbl_chatMember,
			TextField txt_chatMessageUserInput
	) {
		Stage stage = new Stage();
		GuiUtils.applyApplicationIcon(stage);

		stage.setTitle("Top priority candidates");

		ListView<kst4contest.controller.ScoreService.TopCandidate> listView = new ListView<>();
		listView.setItems(mirrorOf(chatcontroller.getScoreService().topCandidates()));

		listView.setCellFactory(lv -> new ListCell<>() {
			@Override
			protected void updateItem(kst4contest.controller.ScoreService.TopCandidate item, boolean empty) {
				super.updateItem(item, empty);

				if (empty || item == null) {
					setText(null);
					return;
				}

				setText(item.getDisplayCallSign()
						+ "  |  score "
						+ String.format(java.util.Locale.US, "%.0f", item.getScore()));
			}
		});

		listView.setOnMouseClicked(evt -> {
			if (evt.getClickCount() < 2) {
				return;
			}

			kst4contest.controller.ScoreService.TopCandidate selected =
					listView.getSelectionModel().getSelectedItem();

			if (selected == null) {
				return;
			}

			selectTopCandidate(selected, tbl_chatMember, txt_chatMessageUserInput);
			stage.close();
		});

		BorderPane root = new BorderPane();
		root.setStyle("-fx-padding: 5;");
		root.setCenter(listView);
		root.setBottom(new Label("Double-click a candidate to select it."));

		Scene candidatesScene = new Scene(root, 360, 500);
		registerThemedScene(candidatesScene);
		stage.setOnHidden(event -> themedScenes.remove(candidatesScene));
		installSharedSystemMenuBar(candidatesScene);
		stage.setScene(candidatesScene);
		stage.show();
	}


	private ChatMember resolveChatMemberForTopCandidate(
			kst4contest.controller.ScoreService.TopCandidate c
	) {

		if (c == null
				|| c.getDisplayCallSign() == null
				|| c.getPreferredChatCategory() == null) {
			return null;
		}

		/*
		 * Resolve the concrete active login by full callsign and category. Falling
		 * back to callSignRaw could select a different suffix in the same category.
		 */
		return chatcontroller.findActiveChatMember(
				c.getDisplayCallSign(),
				c.getPreferredChatCategory()
		);
	}


	private void updateTimelineVisuals() {
		if (timelineView == null || chatcontroller == null) return;

		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(this::updateTimelineVisuals);
			return;
		}

		List<ContestSked> skedsSnapshot = new ArrayList<>(chatcontroller.getActiveSkeds().snapshot());
		List<TimelineView.CandidateEvent> candidates = buildTimelinePriorityCandidateEvents();

		timelineView.updateVisuals(skedsSnapshot, candidates);

		if (composeMainWindowState != null) {
			List<kst4contest.view.compose.TimelineCandidate> composeCandidates = new ArrayList<>();
			for (TimelineView.CandidateEvent c : candidates) {
				composeCandidates.add(new kst4contest.view.compose.TimelineCandidate(
						c.getCallSignRaw(),
						c.getDisplayCallSign(),
						c.getPreferredChatCategory(),
						c.getTimeUntilMs(),
						c.getMinuteBucket(),
						c.getLaneIndex(),
						c.getTargetAzimuth(),
						c.getScore(),
						c.getOpportunityPotentialPercent(),
						c.getTooltipText()
				));
			}
			uiDispatcher.runOnUi(() -> {
				composeMainWindowState.getTimeline().replaceSkeds(skedsSnapshot);
				composeMainWindowState.getTimeline().replaceCandidates(composeCandidates);
				composeMainWindowState.getTimeline().setAntennaAzimuth(chatcontroller.getChatPreferences().getActualQTF().get());
				composeMainWindowState.getTimeline().setBeamWidth(chatcontroller.getChatPreferences().getStn_antennaBeamWidthDeg());
			});
		}
	}

	/**
	 * Build candidate markers for the timeline:
	 * - Use ScoreService TopCandidates (already sorted)
	 * - Resolve representative ChatMember (preferred category if possible)
	 * - Use "next airplane arriving minute" as time basis
	 * - Bucket by minute, keep top 1-2 per minute (config above)
	 */
	private List<TimelineView.CandidateEvent> buildTimelinePriorityCandidateEvents() {

		if (chatcontroller.getScoreService() == null) return Collections.emptyList();

		long now = System.currentTimeMillis();

		// Snapshot to avoid concurrent modifications (TopCandidates list is FX observable)
		List<kst4contest.controller.ScoreService.TopCandidate> topSnapshot =
				new ArrayList<>(chatcontroller.getScoreService().topCandidates().snapshot());

		Map<Integer, List<TimelineCandidateTmp>> byMinute = new HashMap<>();

		for (kst4contest.controller.ScoreService.TopCandidate c : topSnapshot) {

			ChatMember representative = resolveChatMemberForTopCandidate(c);
			if (representative == null) continue;

			AirPlaneReflectionInfo apInfo = representative.getAirPlaneReflectInfo();

			// choose airplane by (highest potential) then (shortest time) within preview window
			int maxMinutes = (int) (TIMELINE_PREVIEW_TIME_MS / 60_000L);
			NextApInfo selectedAp = findBestAirplane(apInfo, maxMinutes);
			if (selectedAp == null) continue;

			long timeUntilMs = selectedAp.arrivingMinutes * 60_000L;
			if (timeUntilMs < 0 || timeUntilMs > TIMELINE_PREVIEW_TIME_MS) continue;

			int minuteBucket = selectedAp.arrivingMinutes;

			byMinute.computeIfAbsent(minuteBucket, k -> new ArrayList<>())
					.add(new TimelineCandidateTmp(c, representative, selectedAp));
		}

		List<TimelineView.CandidateEvent> out = new ArrayList<>();

		for (Map.Entry<Integer, List<TimelineCandidateTmp>> e : byMinute.entrySet()) {

			int minuteBucket = e.getKey();
			List<TimelineCandidateTmp> bucket = e.getValue();

			// Highest score first
			bucket.sort((a, b) -> Double.compare(b.top.getScore(), a.top.getScore()));


			// 1) Pick top-N in beam -> lanes 0..1
			List<TimelineCandidateTmp> inBeam = new ArrayList<>();
			for (TimelineCandidateTmp tmp : bucket) {
				if (chatcontroller.isChatMemberInMyBeam(tmp.representativeMember)) {
					inBeam.add(tmp);
				}
			}

			// Avoid duplicates between beam and global selection
			Set<String> used = new HashSet<>();

			int beamTake = Math.min(TIMELINE_BEAM_MARKERS_PER_MINUTE, inBeam.size());
			for (int lane = 0; lane < beamTake; lane++) {

				TimelineCandidateTmp tmp = inBeam.get(lane);
				used.add(tmp.top.getCallSignRaw());

				double az = (tmp.representativeMember.getQTFdirection() != null) ? tmp.representativeMember.getQTFdirection() : 0.0;
				long timeUntilMs = minuteBucket * 60_000L;

				String tooltip = buildTimelineCandidateTooltip(tmp, minuteBucket);
				int potential = (tmp.nextAp != null && tmp.nextAp.plane != null) ? tmp.nextAp.plane.getPotential() : 0;

				out.add(new TimelineView.CandidateEvent(
						tmp.top.getCallSignRaw(),
						tmp.top.getDisplayCallSign(),
						tmp.top.getPreferredChatCategory(),
						timeUntilMs,
						minuteBucket,
						lane, // lanes 0..1 = in-beam
						az,
						tmp.top.getScore(),
						potential,
						tooltip
				));
			}

			// 2) Pick top-N global distinct -> lanes 2..3
			int globalAdded = 0;
			for (TimelineCandidateTmp tmp : bucket) {

				if (globalAdded >= TIMELINE_PRIORITY_MARKERS_PER_MINUTE) break;
				if (used.contains(tmp.top.getCallSignRaw())) continue;

				double az = (tmp.representativeMember.getQTFdirection() != null) ? tmp.representativeMember.getQTFdirection() : 0.0;
				long timeUntilMs = minuteBucket * 60_000L;

				String tooltip = buildTimelineCandidateTooltip(tmp, minuteBucket);
				int potential = (tmp.nextAp != null && tmp.nextAp.plane != null) ? tmp.nextAp.plane.getPotential() : 0;

				int laneIndex = TIMELINE_BEAM_MARKERS_PER_MINUTE + globalAdded; // lanes 2..3

				out.add(new TimelineView.CandidateEvent(
						tmp.top.getCallSignRaw(),
						tmp.top.getDisplayCallSign(),
						tmp.top.getPreferredChatCategory(),
						timeUntilMs,
						minuteBucket,
						laneIndex,
						az,
						tmp.top.getScore(),
						potential,
						tooltip
				));

				globalAdded++;
			}
		}

		out.sort(Comparator
				.comparingInt(TimelineView.CandidateEvent::getMinuteBucket)
				.thenComparingInt(TimelineView.CandidateEvent::getLaneIndex));

		return out;
	}

	private String buildTimelineCandidateTooltip(TimelineCandidateTmp tmp, int minuteBucket) {
		AirPlane p = tmp.nextAp.plane;

		String planeStr = "-";
		if (p != null) {
			planeStr = p.getApCallSign()
					+ " | " + p.getPotencialDescriptionAsWord()
					+ " | pot " + p.getPotential()
					+ " | dist " + p.getDistanceKm() + " km";
		}

		NextApInfo earliest = findEarliestAirplane(
				tmp.representativeMember.getAirPlaneReflectInfo(),
				(int) (TIMELINE_PREVIEW_TIME_MS / 60_000L)
		);

		String earliestStr = "";
		if (earliest != null && earliest.plane != null) {
			// only show if it differs from the selected/best plane minute
			if (earliest.arrivingMinutes != minuteBucket) {
				earliestStr = "\nearliest AP: +" + earliest.arrivingMinutes
						+ " min (pot " + earliest.plane.getPotential() + "%)";
			}
		}

		return tmp.top.getDisplayCallSign()
				+ "\nscore: " + String.format(Locale.US, "%.0f", tmp.top.getScore())
				+ "\nbest AP: +" + minuteBucket + " min"
				+ "\nplane: " + planeStr
				+ earliestStr;
	}

	/**
	 * Select the airplane that should drive timeline/sked decisions.
	 *
	 * Rule: prefer highest potential; if tied, prefer shortest arriving time.
	 * Only considers planes within [0..maxMinutes] to avoid dropping stations completely.
	 */
	private NextApInfo findBestAirplane(AirPlaneReflectionInfo apInfo, int maxMinutes) {
		if (apInfo == null) return null;
		if (apInfo.getRisingAirplanes() == null) return null;

		AirPlane best = null;
		int bestMin = Integer.MAX_VALUE;
		int bestPot = Integer.MIN_VALUE;

		for (AirPlane p : apInfo.getRisingAirplanes()) {
			if (p == null) continue;

			int m = p.getArrivingDurationMinutes();
			if (m < 0 || m > maxMinutes) continue;

			int pot = p.getPotential();

			// primary: potential DESC, secondary: time ASC
			if (best == null || pot > bestPot || (pot == bestPot && m < bestMin)) {
				best = p;
				bestPot = pot;
				bestMin = m;
			}
		}

		if (best == null) return null;
		return new NextApInfo(best, bestMin);
	}

	/**
	 * Select the earliest airplane (used for additional tooltip info).
	 * Rule: prefer shortest arriving time; if tied, prefer higher potential.
	 */
	private NextApInfo findEarliestAirplane(AirPlaneReflectionInfo apInfo, int maxMinutes) {
		if (apInfo == null) return null;
		if (apInfo.getRisingAirplanes() == null) return null;

		AirPlane best = null;
		int bestMin = Integer.MAX_VALUE;

		for (AirPlane p : apInfo.getRisingAirplanes()) {
			if (p == null) continue;

			int m = p.getArrivingDurationMinutes();
			if (m < 0 || m > maxMinutes) continue;

			if (m < bestMin) {
				bestMin = m;
				best = p;
			} else if (m == bestMin && best != null && p.getPotential() > best.getPotential()) {
				best = p;
			}
		}

		if (best == null || bestMin == Integer.MAX_VALUE) return null;
		return new NextApInfo(best, bestMin);
	}


	private TableView<String> initNotifyAtCallSignTable() {

		TableView<String> table = new TableView<>();

		table.setTooltip(new Tooltip(
				"Messages sent by or addressed to a monitored "
						+ "callsign are also shown in the PM table."
		));

		TableColumn<String, String> callSignColumn =
				new TableColumn<>("Monitored callsign");

		callSignColumn.setCellValueFactory(
				cellData ->
						new SimpleStringProperty(
								cellData.getValue()
						)
		);

		callSignColumn.setCellFactory(
				TextFieldTableCell.forTableColumn()
		);

		callSignColumn.setOnEditCommit(event -> {
			int row = event.getTablePosition().getRow();

			String enteredCallSign =
					event.getNewValue() == null
							? ""
							: event.getNewValue()
							  .trim()
							  .toUpperCase(Locale.ROOT);

			if (enteredCallSign.isBlank()) {
				event.getTableView()
						.getItems()
						.remove(row);
				return;
			}

			/*
			 * QSO monitoring intentionally works with the base callsign.
			 * Entering DN9APW, DN9APW-2 or DN9APW-70 therefore produces
			 * the same monitoring entry: DN9APW.
			 */
			String monitoredBaseCall =
					ChatMember.normalizeCallSignToBaseCallSign(
							enteredCallSign
					);

			if (!GuiUtils.isCallSignSyntax(monitoredBaseCall)) {
				alertWindowEvent(
						"Please enter a valid callsign."
				);
				event.getTableView().refresh();
				return;
			}

			boolean duplicate = false;

			for (int i = 0;
			     i < event.getTableView().getItems().size();
			     i++) {

				if (i == row) {
					continue;
				}

				String existing =
						event.getTableView()
								.getItems()
								.get(i);

				if (existing != null
						&& existing.equalsIgnoreCase(
						monitoredBaseCall
				)) {
					duplicate = true;
					break;
				}
			}

			if (duplicate) {
				alertWindowEvent(
						"This base callsign is already "
								+ "in the monitoring list."
				);
				event.getTableView().refresh();
				return;
			}

			event.getTableView()
					.getItems()
					.set(row, monitoredBaseCall);
		});

		table.getColumns().add(callSignColumn);
		table.setEditable(true);

		return table;
	}

	private TableView<String> initTextSnippetsTable() {

		TableView<String> tbl_txtSnips = new TableView<String>();
		tbl_txtSnips.setTooltip(new Tooltip("Personalize your textsnippets here"));


		TableColumn<String, String> snipCol = new TableColumn<String, String>("Snippet");
		snipCol.setCellValueFactory(new Callback<CellDataFeatures<String, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<String, String> cellDataFeatures) {
				SimpleStringProperty snippet = new SimpleStringProperty();
				snippet.setValue(cellDataFeatures.getValue());
				return snippet;
			}
		});
		snipCol.setCellFactory(TextFieldTableCell.forTableColumn());
		// TODO: https://www.youtube.com/watch?v=M_kp20qrtLw = tutorial dafuer

//		snipCol.setOnEditCommit(e->e.getTableView().getItems().get(e.getTablePosition().getRow()).replace(".*", e.getNewValue()));

		snipCol.setOnEditCommit(new EventHandler<CellEditEvent<String, String>>() {
			@Override
			public void handle(CellEditEvent<String, String> t) {

				String newValue = t.getNewValue();
				if (newValue == null || newValue.isBlank()) {
					t.getTableView().getItems().remove(t.getTablePosition().getRow());
				} else {
					t.getTableView().getItems().set(t.getTablePosition().getRow(), newValue);
				}

				refreshTextSnippetContextMenus();
			}
		});

		tbl_txtSnips.getColumns().addAll(snipCol);

		tbl_txtSnips.setEditable(true);
//		tbl_txtSnips.set

//		ObservableList<String> lst_textSnipList = );
		tbl_txtSnips.setItems(mirrorOf(chatcontroller.getChatPreferences().getLst_txtSnipList()));
//		tbl_txtSnips.bind

		return tbl_txtSnips;
	}

	/**
	 * Rebuilds the shortcut button row after a shortcut was edited or moved.
	 */
	private void refreshShortcutButtons() {
		if (flwPane_textSnippets == null) {
			return;
		}

		flwPane_textSnippets.getChildren().setAll(
				buttonFactory(mirrorOf(chatcontroller.getChatPreferences().getLst_txtShortCutBtnList()))
		);
	}

	/**
	 * Rebuilds every context menu which exposes the configured text snippets.
	 */
	private void refreshTextSnippetContextMenus() {
		ObservableList<String> snippets = mirrorOf(chatcontroller.getChatPreferences().getLst_txtSnipList());
		chatMessageContextMenu = initChatMemberTableContextMenu(snippets);
		chatMemberContextMenu = initChatMemberTableContextMenu(snippets);
	}

	/**
	 * Moves the selected table entry by one position.
	 *
	 * @param tableView table that owns the ordered list
	 * @param offset {@code -1} for up or {@code 1} for down
	 * @return {@code true} if an item was moved
	 */
	private boolean moveSelectedTableEntry(TableView<String> tableView, int offset) {
		int currentIndex = tableView.getSelectionModel().getSelectedIndex();
		int targetIndex = currentIndex + offset;

		if (currentIndex < 0 || targetIndex < 0 || targetIndex >= tableView.getItems().size()) {
			return false;
		}

		String selectedEntry = tableView.getItems().remove(currentIndex);
		tableView.getItems().add(targetIndex, selectedEntry);
		tableView.getSelectionModel().clearAndSelect(targetIndex);
		tableView.scrollTo(targetIndex);
		return true;
	}


	/**
	 * Validates and stores one beacon template.
	 *
	 * <p>The template is checked against the same protocol and length rules which
	 * are applied by the timer before transmission. A variable-only template may
	 * temporarily resolve to an empty value and can still be stored; the timer will
	 * not send it until a usable value is available.</p>
	 *
	 * @param textField field containing the configured beacon template
	 * @param mainCategory {@code true} for the main category, {@code false} for
	 *                     the optional second category
	 */
	private void applyBeaconTextSetting(
			TextField textField,
			boolean mainCategory
	) {
		String configuredText =
				textField.getText() == null
						? ""
						: textField.getText();

		try {
			chatcontroller.validateBeaconTemplate(configuredText);

			if (mainCategory) {
				chatcontroller.getChatPreferences()
						.setBcn_beaconTextMainCat(configuredText);
			} else {
				chatcontroller.getChatPreferences()
						.setBcn_beaconTextSecondCat(configuredText);
			}
		} catch (IllegalArgumentException exception) {
			String previousText =
					mainCategory
							? chatcontroller.getChatPreferences()
							  .getBcn_beaconTextMainCat()
							: chatcontroller.getChatPreferences()
							  .getBcn_beaconTextSecondCat();

			textField.setText(previousText);

			alertWindowEvent(
					"The beacon message is invalid: "
							+ exception.getMessage()
			);
		}
	}

	/**
	 * Validates and applies the shared interval used by both beacon categories.
	 *
	 * <p>Only whole minutes are accepted. If the application is connected, changing
	 * the value restarts the shared timer and begins a new countdown with the
	 * selected interval.</p>
	 *
	 * @param intervalField field containing the interval in minutes
	 */
	private void applySharedBeaconInterval(TextField intervalField) {
		String enteredValue =
				intervalField.getText() == null ? "" : intervalField.getText().trim();

		try {
			int intervalMinutes = Integer.parseInt(enteredValue);

			if (intervalMinutes < ChatController.MIN_BEACON_INTERVAL_MINUTES) {
				throw new NumberFormatException("Beacon interval below minimum");
			}

			chatcontroller.getChatPreferences()
					.setBcn_beaconIntervalInMinutesMainCat(intervalMinutes);

			/*
			 * Preserve the legacy second-category XML value, but keep it synchronized
			 * with the one shared interval.
			 */
			chatcontroller.getChatPreferences()
					.setBcn_beaconIntervalInMinutesSecondCat(intervalMinutes);

			intervalField.setText(Integer.toString(intervalMinutes));
			chatcontroller.restartBeaconTimer();

			System.out.println("[Main.java, Info]: Shared beacon interval set to "
					+ intervalMinutes + " minute(s).");
		} catch (NumberFormatException exception) {
			int currentInterval = Math.max(
					ChatController.MIN_BEACON_INTERVAL_MINUTES,
					chatcontroller.getChatPreferences()
							.getBcn_beaconIntervalInMinutesMainCat()
			);

			intervalField.setText(Integer.toString(currentInterval));
			alertWindowEvent(
					"Enter a whole beacon interval of at least "
							+ ChatController.MIN_BEACON_INTERVAL_MINUTES
							+ " minute."
			);
		}
	}


	private TableView<ChatMember> initWkdStnTable() {

		TableView<ChatMember> tbl_chatMemberWkdDBTable = new TableView<ChatMember>();
		tbl_chatMemberWkdDBTable.setTooltip(new Tooltip("worked info DB"));

		TableColumn<ChatMember, String> callSignCol = new TableColumn<ChatMember, String>("Callsign");
		callSignCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty callsgn = new SimpleStringProperty();

				callsgn.setValue(cellDataFeatures.getValue().getCallSign());

				return callsgn;
			}
		});

		TableColumn<ChatMember, String> workedCol = new TableColumn<ChatMember, String>("worked");
		workedCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});

		TableColumn<ChatMember, String> wkdAny_subcol = new TableColumn<ChatMember, String>("wkd");
		wkdAny_subcol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
						SimpleStringProperty wkd = new SimpleStringProperty();

						if (cellDataFeatures.getValue().isWorked()) {
							wkd.setValue("X");
						} else {
							wkd.setValue("");
						}

						return wkd;
					}
				});
		wkdAny_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(28));

		TableColumn<ChatMember, String> sixMCol_subcol = new TableColumn<ChatMember, String>("50");
		sixMCol_subcol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
						SimpleStringProperty wkd = new SimpleStringProperty();

						if (cellDataFeatures.getValue().isWorked50()) {
							wkd.setValue("X");
						} else {
							wkd.setValue("");
						}

						return wkd;
					}
				});
		sixMCol_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(28));

		TableColumn<ChatMember, String> fourMCol_subcol = new TableColumn<ChatMember, String>("70");
		fourMCol_subcol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
						SimpleStringProperty wkd = new SimpleStringProperty();

						if (cellDataFeatures.getValue().isWorked70()) {
							wkd.setValue("X");
						} else {
							wkd.setValue("");
						}

						return wkd;
					}
				});
		fourMCol_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(28));

		TableColumn<ChatMember, String> vhfCol_subcol = new TableColumn<ChatMember, String>("144");
		vhfCol_subcol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
						SimpleStringProperty wkd = new SimpleStringProperty();

						if (cellDataFeatures.getValue().isWorked144()) {
							wkd.setValue("X");
						} else {
							wkd.setValue("");
						}

						return wkd;
					}
				});
		vhfCol_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(28));

		TableColumn<ChatMember, String> uhfCol_subcol = new TableColumn<ChatMember, String>("432");
		uhfCol_subcol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
						SimpleStringProperty wkd = new SimpleStringProperty();

						if (cellDataFeatures.getValue().isWorked432()) {
							wkd.setValue("X");
						} else {
							wkd.setValue("");
						}

						return wkd;
					}
				});

		uhfCol_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(28));

		TableColumn<ChatMember, String> shf23_subcol = new TableColumn<ChatMember, String>("23");
		shf23_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked1240()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});
		shf23_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(30));

		TableColumn<ChatMember, String> shf13_subcol = new TableColumn<ChatMember, String>("13");
		shf13_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked2300()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});
		shf13_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(30));

		TableColumn<ChatMember, String> shf9_subcol = new TableColumn<ChatMember, String>("9");
		shf9_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked3400()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});
		shf9_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(32));

		TableColumn<ChatMember, String> shf6_subcol = new TableColumn<ChatMember, String>("6");
		shf6_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked5600()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});
		shf6_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(32));

		TableColumn<ChatMember, String> shf3_subcol = new TableColumn<ChatMember, String>("3");
		shf3_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked10G()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});

		shf3_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(32));

		workedCol.getColumns().addAll(wkdAny_subcol, sixMCol_subcol, fourMCol_subcol, vhfCol_subcol, uhfCol_subcol, shf23_subcol, shf13_subcol,
				shf9_subcol, shf6_subcol, shf3_subcol); // TODO: automatize enabling to users bandChoice

		tbl_chatMemberWkdDBTable.getColumns().addAll(callSignCol, workedCol);

		tbl_chatMemberWkdDBTable.setItems(mirrorOf(chatcontroller.getLst_DBBasedWkdCallSignList()));
		applyTruncatedTextCells(
				callSignCol, wkdAny_subcol, sixMCol_subcol, fourMCol_subcol,
				vhfCol_subcol, uhfCol_subcol, shf23_subcol, shf13_subcol,
				shf9_subcol, shf6_subcol, shf3_subcol
		);
		TableLayoutManager.install(
				tbl_chatMemberWkdDBTable,
				"worked-database",
				chatcontroller.getChatPreferences(),
				layoutAutosave,
				TableLayoutManager.column("callsign", callSignCol),
				TableLayoutManager.column("worked-any", wkdAny_subcol),
				TableLayoutManager.column("band-50", sixMCol_subcol),
				TableLayoutManager.column("band-70", fourMCol_subcol),
				TableLayoutManager.column("band-144", vhfCol_subcol),
				TableLayoutManager.column("band-432", uhfCol_subcol),
				TableLayoutManager.column("band-1296", shf23_subcol),
				TableLayoutManager.column("band-2320", shf13_subcol),
				TableLayoutManager.column("band-3400", shf9_subcol),
				TableLayoutManager.column("band-5760", shf6_subcol),
				TableLayoutManager.column("band-10g", shf3_subcol)
		);

		// TODO: https://www.youtube.com/watch?v=M_kp20qrtLw = tutorial dafuer

//		snipCol.setOnEditCommit(e->e.getTableView().getItems().get(e.getTablePosition().getRow()).replace(".*", e.getNewValue()));

//		callSignCol.setOnEditCommit(new EventHandler<CellEditEvent<String, String>>() {
//			@Override
//			public void handle(CellEditEvent<String, String> t) {
//
//				String newValue = t.getNewValue();
//				t.getTableView().getItems().set(t.getTablePosition().getRow(), newValue);
//				
//				if (newValue == "") { //delete lines which had been cleared
//					t.getTableView().getItems().remove(t.getTablePosition().getRow());
//				}
//
//				chatMessageContextMenu = initChatMemberTableContextMenu(chatcontroller.getChatPreferences().getLst_txtSnipList()); // TODO: thats not
//																									// clean, there had
//																									// to be a listener
//																									// triggered update
//																									// method
//				chatMemberContextMenu = initChatMemberTableContextMenu(chatcontroller.getChatPreferences().getLst_txtSnipList());
//
//			}
//		});

//		tbl_chatMemberWkdDBTable.getColumns().addAll(callSignCol);

		tbl_chatMemberWkdDBTable.setEditable(true);
//		tbl_txtSnips.set

//		ObservableList<String> lst_textSnipList = );
//		tbl_wkdStn.setItems(mirrorOf(chatcontroller.getChatPreferences().getLst_txtSnipList()));
//		tbl_txtSnips.bind

		return tbl_chatMemberWkdDBTable;
	}

	private MenuBar initMenuBar() {

		Menu fileMenu = new Menu("File");

		// build "Connect to <configured chat>" label from saved preferences
		ChatCategory mainCat = chatcontroller.getChatPreferences().getLoginChatCategoryMain();
		String connectLabel = "Connect to " + mainCat.getChatCategoryName(mainCat.getCategoryNumber());
		if (chatcontroller.getChatPreferences().isLoginToSecondChatEnabled()) {
			ChatCategory secCat = chatcontroller.getChatPreferences().getLoginChatCategorySecond();
			if (secCat != null) {
				connectLabel += " & " + secCat.getChatCategoryName(secCat.getCategoryNumber());
			}
		}
		menuItemFileConnect = new MenuItem(connectLabel);
		menuItemFileConnect.setDisable(false);

		if (chatcontroller.isConnectedAndLoggedIn() || chatcontroller.isConnectedAndNOTLoggedIn()) {
			menuItemFileConnect.setDisable(true);
		}

		menuItemFileConnect.setOnAction(event -> {
			System.out.println("[Info] File menu: Connect clicked, using saved preferences");

			String call = chatcontroller.getChatPreferences().getStn_loginCallSign();
			String pass = chatcontroller.getChatPreferences().getStn_loginPassword();

			if (call == null || call.isBlank() || pass == null || pass.isBlank()) {
				Alert alert = new Alert(Alert.AlertType.WARNING);
				alert.setTitle("Cannot connect");
				alert.setHeaderText("Login credentials missing");
				alert.setContentText("Please configure your callsign and password in Settings first.");
				alert.show();
				return;
			}

			try {
				chatcontroller.execute();

				menuItemFileConnect.setDisable(true);
				menuItemFileDisconnect.setDisable(false);
				menuItemOptionsAwayBack.setDisable(false);
				menuItemOptionsSetFrequencyAsName.setDisable(false);

//				chatcontroller.setConnectedAndLoggedIn(true);
//				chatcontroller.setDisconnected(false);

			} catch (InterruptedException | IOException e) {
				LOGGER.log(java.util.logging.Level.SEVERE, "Exception", e);
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Connection failed");
				alert.setContentText("Could not connect: " + e.getMessage());
				alert.show();
			}
		});

		menuItemFileDisconnect = new MenuItem("Disconnect");
		menuItemFileDisconnect.setDisable(true);

		if (chatcontroller.isConnectedAndLoggedIn() || chatcontroller.isConnectedAndNOTLoggedIn()) {
			menuItemFileDisconnect.setDisable(false);
		} if (chatcontroller.isDisconnected()) {
			menuItemFileDisconnect.setDisable(true);
		}
		menuItemFileDisconnect.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				chatcontroller.disconnect(ApplicationConstants.DISCSTRING_DISCONNECTONLY);
				menuItemFileDisconnect.setDisable(true);
				menuItemFileConnect.setDisable(false);
			}
		});


		MenuItem menuItemFileSwitchProfile = new MenuItem("Switch operator profile...");
		menuItemFileSwitchProfile.setOnAction(event -> showOperatorProfileSwitchDialog());

		menuItemFileExit = new MenuItem("Exit + disconnect");
		menuItemFileExit.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				closeWindowEvent(null);
			}
		});

		// add menu items to menu
		fileMenu.getItems().add(menuItemFileConnect);
		fileMenu.getItems().add(menuItemFileDisconnect);
		fileMenu.getItems().add(menuItemFileSwitchProfile);
		fileMenu.getItems().add(menuItemFileExit);

		Menu optionsMenu = new Menu("Options");
		menuItemOptionsSetFrequencyAsName = new MenuItem("Set QRG as name in Chat (main category)");
		menuItemOptionsSetFrequencyAsName.setDisable(true);
		menuItemOptionsSetFrequencyAsName.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				ChatMessage sendMe = new ChatMessage();
				sendMe.setMessageDirectedToServer(false);
				sendMe.setMessageText("/SETNAME " + chatcontroller.getChatPreferences().getMYQRGFirstCat().get());

				chatcontroller.getMessageTXBus().add(sendMe);

			}
		});


		menuItemOptionsAwayBack = new MenuItem("Show me as away in chat");


		menuItemOptionsShow = new MenuItem("Show options");

		menuItemOptionsAwayBack.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				ChatMessage sendMe = new ChatMessage();
				sendMe.setMessageDirectedToServer(false);

				if (chatcontroller.getChatPreferences().isStn_loginAFKState()) {

					menuItemOptionsAwayBack.setText("Show me as AWAY FROM chat!");
					chatcontroller.getChatPreferences().setStn_loginAFKState(false);
					sendMe.setMessageText("/BACK");

				} else {

					menuItemOptionsAwayBack.setText("Show me as ACTIVE in chat!");
					chatcontroller.getChatPreferences().setStn_loginAFKState(true);
					sendMe.setMessageText("/AWAY");
				}

				chatcontroller.getMessageTXBus().add(sendMe);

			}
		});
		menuItemOptionsShow.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				openSettingsWindow();
			}
		});

		optionsMenu.getItems().addAll(menuItemOptionsSetFrequencyAsName, menuItemOptionsAwayBack, menuItemOptionsShow);

		Menu macroMenu = new Menu("Macros");

		MenuItem macro1 = new MenuItem("Pse Sked?");
		MenuItem macro10 = new MenuItem("Pse qrg 2m?");
		MenuItem macro20 = new MenuItem("Pse Call at ");
		MenuItem macro30 = new MenuItem("In qso nw, pse qrx, I will meep you");
		MenuItem macro40 = new MenuItem("Pse qrg 70cm?");
		MenuItem macro50 = new MenuItem("pse qrg 23cm?");

		macroMenu.getItems().addAll(macro1, macro10, macro20, macro30, macro40, macro50);

		Menu windowMenu = new Menu("Windows");
		menuItemWindowCluster = new MenuItem("Hide cluster / stranger QSOs");
		menuItemWindowCluster.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				if (MonitorWindow.isOpen()) {
					MonitorWindow.close();
					releaseMonitorListeners();
				} else {
					openMonitorWindow();
				}
			}
		});

		/*
		 * Opens instead of toggling, and keeps one label: the settings window is a
		 * Compose window which the operator can also close from its own frame, so a
		 * label that tracked the state here would go stale without being told.
		 */
		menuItemWindowHideOptions = new MenuItem("hide options");
		menuItemWindowHideOptions.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				if (SettingsWindow.isOpen()) {
					SettingsWindow.close();
				} else {
					openSettingsWindow();
				}
			}
		});

		menuItemWindowDarkDesign = new MenuItem("Use dark mode design");
		menuItemWindowDarkDesign.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				// quick switch for this session only, the startup design is set per profile in the GUI options
				applyTheme(true);
			}
		});

		menuItemWindowDefaultDesign = new MenuItem("Use default mode design");
		menuItemWindowDefaultDesign.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				// quick switch for this session only, the startup design is set per profile in the GUI options
				applyTheme(false);
			}
		});


		menuItemWindowStationMap = new MenuItem("Show / hide station map");
		menuItemWindowStationMap.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				toggleStationMapWindow();
			}
		});

//		windowMenu.getItems().addAll(menuItemWindowCluster, menuItemWindowHideOptions, menuItemWindowDarkDesign, menuItemWindowDefaultDesign, menuItemWindowStationMap);

		/*
		 * The labels are refreshed when the menu opens, not tracked as the windows come
		 * and go. These are Compose windows and the operator can close one from its own
		 * frame without telling the menu; asking at the moment the menu is read is the
		 * only answer that cannot go stale.
		 */
		windowMenu.setOnShowing(event -> {
			menuItemWindowCluster.setText(MonitorWindow.isOpen()
					? "Hide cluster / stranger QSOs"
					: "Show cluster / stranger QSOs");
			menuItemWindowHideOptions.setText(SettingsWindow.isOpen() ? "hide options" : "show options");
		});

		windowMenu.getItems().addAll(
				menuItemWindowCluster,
				menuItemWindowHideOptions,
				new SeparatorMenuItem(),
				menuItemWindowStationMap,
				new SeparatorMenuItem(),
				menuItemWindowDarkDesign,
				menuItemWindowDefaultDesign
		);

		Menu helpMenu = new Menu("Info");

		MenuItem help1 = new MenuItem("No help here.");
		menuItemInfoDonate = new MenuItem("Donate for kst4Contest development via PayPal");
		MenuItem help3 = new MenuItem("_______________________");
		help3.setDisable(true);
		menuItemInfoHomepage = new MenuItem("Visit DARC X08-Homepage");
		MenuItem menuItmDonateON4KST = new MenuItem("Donate for ON4KST Chatservers with PayPal to on4kst@skynet.be");
		menuItemInfoDonateOv3t = new MenuItem("Donate for OV3T´s plane feed service");
//		help5.setDisable(true);
		menuItemInfoContact = new MenuItem("Contact the author using default mail app");
		menuItemInfoNewsgroup = new MenuItem("Join kst4Contest newsgroup");
//		MenuItem help9 = new MenuItem("Download the changelog / roadmap");

		// Changelog
		// https://e.pcloud.link/publink/show?code=XZwAoWZIap9DYqDlhhwncqAxLbU6STOh2PV

		menuItemInfoDonate.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				getHostServices().showDocument("https://ko-fi.com/praktimarc");

			}
		});

		menuItemInfoHomepage.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				getHostServices().showDocument("http://www.x08.de");

			}
		});

		menuItemInfoContact.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				getHostServices().showDocument("mailto:praktimarc+kst4contest@gmail.com");

			}
		});

		menuItemInfoNewsgroup.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				getHostServices().showDocument("https://groups.google.com/g/kst4contest/about");

			}
		});

//		menuItmDonateON4KST.setOnAction(new EventHandler<ActionEvent>() {
//			public void handle(ActionEvent event) {
//
//				getHostServices().showDocument("https://www.paypal.com");
//
//
//			}
//		});

		menuItemInfoDonateOv3t.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				getHostServices().showDocument("https://www.paypal.me/ov3t");


			}
		});

//		help9.setOnAction(new EventHandler<ActionEvent>() {
//			public void handle(ActionEvent event) {
//
//				getHostServices()
//						.showDocument("https://e.pcloud.link/publink/show?code=XZwAoWZIap9DYqDlhhwncqAxLbU6STOh2PV");
//
//			}
//		});

		menuItemInfoAbout = new MenuItem("About...");
		menuItemInfoAbout.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				Alert a = new Alert(AlertType.INFORMATION);

				a.setTitle("About kst4contest");
				a.setHeaderText("kst4Contest " + ApplicationConstants.APPLICATION_CURRENT_VERSION
						+ ": ON4KST Chatclient by DO5AMF and DN9APW");
				a.setContentText(chatcontroller.getChatPreferences().getProgramVersion());
				a.show();
			}
		});

		helpMenu.getItems().addAll(menuItemInfoDonate, help3, menuItemInfoHomepage, menuItemInfoDonateOv3t, menuItmDonateON4KST, menuItemInfoContact, menuItemInfoNewsgroup, menuItemInfoAbout);

		MenuBar menubar = new MenuBar();
		menubar.getMenus().addAll(fileMenu, optionsMenu, windowMenu, helpMenu); // macromenu deleted

		// On macOS the menu belongs into the system menu bar at the top of the screen.
		// The node stays in the status bar pane but is not rendered there.
		// The connection state indicator moves into the menu bar as a read-only menu.
		if (PlatformUtils.isMacOs()) {
			menuItemConnectionStateDetailMacOs.setDisable(true);
			menuConnectionStateMacOs.getItems().setAll(menuItemConnectionStateDetailMacOs);
			menubar.getMenus().add(menuConnectionStateMacOs);
			menubar.setUseSystemMenuBar(true);
		}

		return menubar;
	}

	/**
	 * On macOS, attaches an invisible system menu bar to a secondary window, so the
	 * main menus stay visible in the screen menu bar while that window has the focus.
	 * The menus are shared with the main window instead of copied, so enabled state and
	 * labels stay consistent everywhere. Does nothing on other platforms.
	 */
	private void installSharedSystemMenuBar(Scene scene) {
		if (!PlatformUtils.isMacOs() || mainScreenMenuBar == null || scene == null) {
			return;
		}

		Pane rootPane;
		if (scene.getRoot() instanceof Pane pane) {
			rootPane = pane;
		} else {
			// Controls like SplitPane do not accept extra children, so wrap them.
			rootPane = new BorderPane(scene.getRoot());
			scene.setRoot(rootPane);
		}

		MenuBar sharedMenuBar = new MenuBar();
		sharedMenuBar.setUseSystemMenuBar(true);
		sharedMenuBar.setManaged(false);
		Bindings.bindContent(sharedMenuBar.getMenus(), mainScreenMenuBar.getMenus());
		rootPane.getChildren().add(sharedMenuBar);
	}

	/**
	 * Registers a scene whose stylesheet follows the light/dark design switch and
	 * applies the currently active design to it.
	 */
	private void registerThemedScene(Scene scene) {
		if (scene == null) {
			return;
		}
		themedScenes.add(scene);
		applyThemeStylesheet(scene, chatcontroller.getChatPreferences().isGUI_darkModeActive());
	}

	/**
	 * Switches all registered windows and the station map to the dark or light design
	 * for the running session. The per-profile startup design is not changed here.
	 */
	private void applyTheme(boolean darkMode) {
		LOGGER.info("Switching GUI design to " + (darkMode ? "dark" : "light") + " mode");

		chatcontroller.getChatPreferences().setGUI_darkModeActive(darkMode);
		setUserAgentStylesheet(null);

		for (Scene scene : themedScenes) {
			applyThemeStylesheet(scene, darkMode);
		}

		// The Compose settings window is not a Scene, so it is told separately.
		MainWindowHost.applyDarkMode(darkMode);
		SettingsWindow.applyDarkMode(darkMode);
		UpdateWindow.applyDarkMode(darkMode);
		MonitorWindow.applyDarkMode(darkMode);
		kst4contest.view.compose.map.StationMapWindow.applyDarkMode(darkMode);


	}

	private static void applyThemeStylesheet(Scene scene, boolean darkMode) {
		scene.getStylesheets().setAll(darkMode
				? ApplicationConstants.STYLECSSFILE_DEFAULT_EVENING
				: ApplicationConstants.STYLECSSFILE_DEFAULT_DAYLIGHT);
	}

	/**
	 * Title of the macOS connection state menu, e.g. "🟢 LINK: Connected".
	 */
	static String macOsConnectionStateMenuTitle(On4KstConnectionState state) {
		On4KstConnectionState effectiveState = state == null
				? On4KstConnectionState.DISCONNECTED : state;
		return switch (effectiveState) {
			case ONLINE -> "🟢 LINK: Connected";
			case CONNECTING, WAITING_FOR_LOGIN_PROMPT, AUTHENTICATING,
			     SYNCING_MAIN_CHAT, SYNCING_SECOND_CHAT -> "🟡 LINK: Connecting…";
			case STOPPING -> "🟡 LINK: Disconnecting…";
			case RECONNECT_WAIT -> "🔴 LINK: Reconnecting…";
			case DISCONNECTED -> "🔴 LINK: Disconnected";
		};
	}


	/*****************************************************
	 * Sked warning Initializing and functional section
	 ****************************************************/

	/**
	 * Initializes the button for the Sked Warning (its an non clickable Info button)
	 */
	private void initSkedWarnIndicatorButton() {
		btnSkedWarnIndicator.setVisible(false);
		btnSkedWarnIndicator.managedProperty().bind(btnSkedWarnIndicator.visibleProperty());

		// "no click function" - it is just an indicator
		btnSkedWarnIndicator.setMouseTransparent(true);
		btnSkedWarnIndicator.setFocusTraversable(false);

		btnSkedWarnIndicator.setStyle(
				"-fx-background-color: rgba(255,0,255,0.85);" +
						"-fx-text-fill: black;" +
						"-fx-font-weight: bold;" +
						"-fx-padding: 2 8 2 8;" +
						"-fx-background-radius: 6;"
		);

		btnSkedWarnIndicator.setTooltip(tipSkedWarnIndicator);
	}

	private void initConnectionStateIndicatorButton() {
		LOGGER.fine("Initializing compact ON4KST connection-state indicator");

		btnConnectionStateIndicator.setMouseTransparent(true);
		btnConnectionStateIndicator.setFocusTraversable(false);
		btnConnectionStateIndicator.setMnemonicParsing(false);
		btnConnectionStateIndicator.setMinSize(44, 22);
		btnConnectionStateIndicator.setPrefSize(44, 22);
		btnConnectionStateIndicator.setMaxSize(44, 22);
		btnConnectionStateIndicator.setTooltip(tipConnectionStateIndicator);
		btnConnectionStateIndicator.setAccessibleText(
				"ON4KST connection state");
		FlowPane.setMargin(
				btnConnectionStateIndicator, new Insets(2, 4, 2, 4));

		updateConnectionStateIndicator(
				chatcontroller.getOn4KstConnectionState(),
				"No ON4KST connection");
	}

	private void updateConnectionStateIndicator(
			On4KstConnectionState state,
			String detail
	) {
		if (!Platform.isFxApplicationThread()) {
			LOGGER.warning(
					"Connection indicator update arrived outside the JavaFX thread; "
							+ "rescheduling it safely");
			Platform.runLater(() -> updateConnectionStateIndicator(state, detail));
			return;
		}

		On4KstConnectionState effectiveState = state == null
				? On4KstConnectionState.DISCONNECTED : state;
		String stateDetail = detail == null || detail.isBlank()
				? effectiveState.name() : detail;
				
		if (composeMainWindowState != null) {
			composeMainWindowState.getSurroundings().setConnectionState(effectiveState);
			composeMainWindowState.getSurroundings().setConnectionDetail(stateDetail);
		}

		logConnectionIndicatorTransition(effectiveState, stateDetail);

		tipConnectionStateIndicator.setText(
				"ON4KST link: " + effectiveState.name() + "\n" + stateDetail);

		/*
		 * The Compose window's badge and menu follow the same update, so the two windows can
		 * never disagree about the link while they sit side by side.
		 */
		if (composeMainWindowState != null) {
			composeMainWindowState.getSurroundings().setConnectionState(effectiveState);
			composeMainWindowState.getSurroundings().setConnectionDetail(stateDetail);
		}
		btnConnectionStateIndicator.setAccessibleHelp(stateDetail);
		menuConnectionStateMacOs.setText(macOsConnectionStateMenuTitle(effectiveState));
		menuItemConnectionStateDetailMacOs.setText(stateDetail);

		String commonStyle =
				"-fx-font-size: 10px;"
						+ "-fx-font-weight: bold;"
						+ "-fx-padding: 1 5 1 5;"
						+ "-fx-background-radius: 6;"
						+ "-fx-border-radius: 6;"
						+ "-fx-border-width: 2;";

		switch (effectiveState) {
			case ONLINE -> {
				btnConnectionStateIndicator.setText("LINK");
				btnConnectionStateIndicator.setStyle(
						commonStyle
								+ "-fx-background-color: #238636;"
								+ "-fx-border-color: #56d364;"
								+ "-fx-text-fill: white;"
								+ "-fx-effect: dropshadow(three-pass-box, "
								+ "rgba(35,134,54,0.55), 5, 0.25, 0, 0);");
			}

			case CONNECTING, WAITING_FOR_LOGIN_PROMPT, AUTHENTICATING,
			     SYNCING_MAIN_CHAT, SYNCING_SECOND_CHAT, STOPPING -> {
				btnConnectionStateIndicator.setText("LINK…");
				btnConnectionStateIndicator.setStyle(
						commonStyle
								+ "-fx-background-color: #ffb300;"
								+ "-fx-border-color: #ffe082;"
								+ "-fx-text-fill: #1b1b1b;"
								+ "-fx-effect: dropshadow(three-pass-box, "
								+ "rgba(255,179,0,0.65), 6, 0.3, 0, 0);");
			}

			case DISCONNECTED, RECONNECT_WAIT -> {
				btnConnectionStateIndicator.setText("LINK!");
				btnConnectionStateIndicator.setStyle(
						commonStyle
								+ "-fx-background-color: #d50000;"
								+ "-fx-border-color: #ff6b6b;"
								+ "-fx-text-fill: white;"
								+ "-fx-effect: dropshadow(three-pass-box, "
								+ "rgba(255,0,0,0.95), 10, 0.55, 0, 0);");
			}
		}
	}

	private void logConnectionIndicatorTransition(
			On4KstConnectionState newState,
			String newDetail
	) {
		boolean stateChanged = newState != lastDisplayedConnectionState;
		boolean detailChanged = !Objects.equals(
				newDetail, lastDisplayedConnectionDetail);
		if (!stateChanged && !detailChanged) {
			return;
		}

		On4KstConnectionState previousState = lastDisplayedConnectionState;
		Level logLevel = switch (newState) {
			case DISCONNECTED, RECONNECT_WAIT -> Level.WARNING;
			default -> Level.INFO;
		};

		LOGGER.log(logLevel,
				"ON4KST connection indicator: {0} -> {1}; detail: {2}",
				new Object[] {
						previousState == null ? "UNINITIALIZED" : previousState,
						newState,
						newDetail
				});

		lastDisplayedConnectionState = newState;
		lastDisplayedConnectionDetail = newDetail;
	}


	private void showBlinkingSkedWarnIndicator(String text) {
		// short text for the button; full text in tooltip
		String shown = text;
		if (shown.length() > 38) shown = shown.substring(0, 35) + "...";

		btnSkedWarnIndicator.setText(shown);
		tipSkedWarnIndicator.setText(text);

		btnSkedWarnIndicator.setVisible(true);
		btnSkedWarnIndicator.setOpacity(1.0);

		if (composeMainWindowState != null) {
			uiDispatcher.runOnUi(() -> composeMainWindowState.getSkedNotice().show(text, text));
		}

		if (skedWarnBlinkTimeline != null) {
			skedWarnBlinkTimeline.stop();
		}

		skedWarnBlinkTimeline = new Timeline(
				new KeyFrame(Duration.ZERO, e -> btnSkedWarnIndicator.setOpacity(1.0)),
				new KeyFrame(Duration.millis(250), e -> btnSkedWarnIndicator.setOpacity(0.25)),
				new KeyFrame(Duration.millis(500), e -> btnSkedWarnIndicator.setOpacity(1.0))
		);
		skedWarnBlinkTimeline.setCycleCount(24); // 12 = 6 seconds
		skedWarnBlinkTimeline.setOnFinished(e -> hideSkedWarnIndicator());
		skedWarnBlinkTimeline.playFromStart();
	}

	private void hideSkedWarnIndicator() {
		btnSkedWarnIndicator.setOpacity(1.0);
		btnSkedWarnIndicator.setVisible(false);

		if (composeMainWindowState != null) {
			uiDispatcher.runOnUi(() -> composeMainWindowState.getSkedNotice().hide());
		}
	}


	/*****************************************************
	 * Band-Upgrade warning (after log entry) section
	 ****************************************************/

	/**
	 * Initializes the button for the Band-Upgrade Hint.
	 * Non-clickable; it blinks and shows the reason (call + remaining bands).
	 */
	private void initBandUpgradeIndicatorButton() {
		btnBandUpgradeIndicator.setVisible(false);
		btnBandUpgradeIndicator.managedProperty().bind(btnBandUpgradeIndicator.visibleProperty());

		btnBandUpgradeIndicator.setMouseTransparent(true);
		btnBandUpgradeIndicator.setFocusTraversable(false);

		btnBandUpgradeIndicator.setStyle(
				"-fx-background-color: rgba(255,255,0,0.85);" +
						"-fx-text-fill: black;" +
						"-fx-font-weight: bold;" +
						"-fx-padding: 2 8 2 8;" +
						"-fx-background-radius: 6;"
		);

		btnBandUpgradeIndicator.setTooltip(tipBandUpgradeIndicator);
	}

	private void maybeShowBandUpgradeIndicator(String key, ThreadStateMessage msg) {
		if (msg == null) return;

		String nick = msg.getThreadNickName() == null ? "" : msg.getThreadNickName().toLowerCase(Locale.ROOT);
		String k = key == null ? "" : key.toLowerCase(Locale.ROOT);

		boolean isBandUpgrade = k.contains("bandupgrade") || nick.contains("bandupgrade");
		if (!isBandUpgrade) return;

		String buttonText = msg.getRunningInformationTextDescription();
		if (buttonText == null || buttonText.isBlank()) buttonText = "BAND+";

		String tooltip = msg.getRunningInformation();
		if (tooltip == null || tooltip.isBlank()) tooltip = buttonText;

		final String finalButtonText = buttonText;
		final String finalTooltip = tooltip;

		Platform.runLater(() -> showBlinkingBandUpgradeIndicator(finalButtonText, finalTooltip));
	}

	private void showBlinkingBandUpgradeIndicator(String buttonText, String tooltipText) {

		// short text for the button; full text in tooltip
		String shown = buttonText;
		if (shown.length() > 38) shown = shown.substring(0, 35) + "...";

		btnBandUpgradeIndicator.setText(shown);
		tipBandUpgradeIndicator.setText(tooltipText);

		btnBandUpgradeIndicator.setVisible(true);
		btnBandUpgradeIndicator.setOpacity(1.0);

		if (composeMainWindowState != null) {
			uiDispatcher.runOnUi(() -> composeMainWindowState.getBandUpgradeNotice().show(buttonText, tooltipText));
		}

		if (bandUpgradeBlinkTimeline != null) {
			bandUpgradeBlinkTimeline.stop();
		}

		bandUpgradeBlinkTimeline = new Timeline(
				new KeyFrame(Duration.ZERO, e -> btnBandUpgradeIndicator.setOpacity(1.0)),
				new KeyFrame(Duration.millis(250), e -> btnBandUpgradeIndicator.setOpacity(0.25)),
				new KeyFrame(Duration.millis(500), e -> btnBandUpgradeIndicator.setOpacity(1.0))
		);
		bandUpgradeBlinkTimeline.setCycleCount(24); // ~12 seconds
		bandUpgradeBlinkTimeline.setOnFinished(e -> hideBandUpgradeIndicator());
		bandUpgradeBlinkTimeline.playFromStart();
	}

	private void hideBandUpgradeIndicator() {
		btnBandUpgradeIndicator.setOpacity(1.0);
		btnBandUpgradeIndicator.setVisible(false);

		if (composeMainWindowState != null) {
			uiDispatcher.runOnUi(() -> composeMainWindowState.getBandUpgradeNotice().hide());
		}
	}

/**
 * End Band-Upgrade section
 */


	/**
	 * End Sked warning section
	 */


//	SimpleStringProperty messageBusOfChatCtrl = messageBus;
	Scene scn_ChatwindowMainScene;

	MenuItem menuItemFileConnect;
	MenuItem menuItemFileDisconnect;
	MenuItem menuItemOptionsAwayBack;

	MenuItem menuItemOptionsSetFrequencyAsName;

	/*
	 * Held so the Compose menu bar can fire them. Firing the very same MenuItem is what
	 * guarantees the two bars do literally the same thing while they run side by side —
	 * a second implementation would be a second thing to compare. When the JavaFX window
	 * goes, these handlers move into methods and the fields go with the window.
	 */
	MenuItem menuItemFileExit;
	MenuItem menuItemOptionsShow;
	MenuItem menuItemWindowCluster;
	MenuItem menuItemWindowHideOptions;
	MenuItem menuItemWindowDarkDesign;
	MenuItem menuItemWindowDefaultDesign;
	MenuItem menuItemWindowStationMap;
	MenuItem menuItemInfoDonate;
	MenuItem menuItemInfoDonateOv3t;
	MenuItem menuItemInfoHomepage;
	MenuItem menuItemInfoNewsgroup;
	MenuItem menuItemInfoContact;
	MenuItem menuItemInfoAbout;
	TextField txt_chatMessageUserInput = new TextField();
	Button sendButton;
	TextField txt_ownqrgMainCategory = new TextField();
	TextField txt_ownqrgSecondCategory = new TextField();
	TextField txt_myQTF = new TextField();

	/**
	 * Stores the last automatically prepared text in the send input field.
	 *
	 * This is used to distinguish between:
	 * - text that KST4Contest created automatically, e.g. "/cq DL1ABC "
	 * - text that the operator has already edited manually
	 *
	 * Only automatically prepared text may be overwritten by a later automatic
	 * preparation. Manually typed text must never be destroyed by a table refresh.
	 */
	private String lastAutoPreparedSendText = "";

	/**
	 * Callsign behind the last automatically prepared /cq command.
	 *
	 * This is important for category resolution while sending. If the visible text is
	 * "/cq DL1ABC ..." and a later table refresh changes the selected ChatMember, the
	 * outgoing message must still be sent in the category that belonged to DL1ABC.
	 */
	private String lastAutoPreparedCqTargetCallsign = "";

	/**
	 * Chat category that belonged to the last automatically prepared /cq target.
	 *
	 * The send handler uses this as the strongest category hint when the current
	 * input still targets the same callsign.
	 */
	private ChatCategory lastAutoPreparedCqTargetCategory = null;

	/**
	 * Guard flag for programmatic table selections.
	 *
	 * Some UI actions intentionally select a row in the ChatMember table for visual
	 * feedback. Those programmatic selections must not run the normal user-selection
	 * handler again, otherwise /cq preparation can happen twice or at the wrong time.
	 */
	private boolean programmaticChatMemberSelectionChange = false;

	ContextMenu chatMessageContextMenu; // public due need to update it on modify
	ContextMenu chatMemberContextMenu;// public due need to update it on modify
//	FlowPane chatMemberTableFilterQTFAndQRBHbox;
	HBox chatMemberTableFilterQTFAndQRBHbox;

    TableView<ChatMember> tbl_chatMember = new TableView<ChatMember>();

	FlowPane flwPane_textSnippets;
    FlowPane flwpne_StatusBar;

	/**
	 * True once this runtime released its resources. Shutdown must stay idempotent
	 * because it is reached both through the JavaFX stop() callback and explicitly.
	 */
	private boolean runtimeShutdownDone;

	/**
	 * The primary stage of this runtime, remembered so shutdown can close it.
	 */
	private Stage ownPrimaryStage;

//	Stage stage_selectedCallSignInfoStage;
	ChatMember selectedCallSignInfoStageChatMember;
	BorderPane selectedCallSignInfoBorderPane;


    Stage notify_setSnifferEntitiesStage;






	/**
	 * Generates buttons out of pre made Strings, one button per given string in the
	 * buttontext-array. Buttonclick will add the buttontext + " " to the
	 * send-Textfield <br/>
	 * <br/>
	 * 
	 * <b>ATTENTION: MYQRG-Button adds myqrg-textfield-string. <br/>
	 * For identification of the button in the dom and make it functional, the
	 * init-value have to be "MYQRG"! </b>
	 * 
	 *
	 * @return
	 */
	private Node[] buttonFactory(ObservableList<String> shortcuts) {

		Button[] txMessageButtons = new Button[shortcuts.size()];

		for (int i = 0; i < shortcuts.size(); i++) {

			txMessageButtons[i] = new Button(shortcuts.get(i));

			if (shortcuts.get(i).equals("MYQRG")) {
				txMessageButtons[i].setTooltip(new Tooltip("MYQRG"));
//
				txMessageButtons[i].getStyleClass().clear();
				txMessageButtons[i].getStyleClass().add("button");
				txMessageButtons[i].getStyleClass().add("buttonMyQrg1");

				MYQRGButton = txMessageButtons[i];

			}

			if (shortcuts.get(i).equals("SECONDQRG")) {
				txMessageButtons[i].setTooltip(new Tooltip("SECONDQRG"));
//
				txMessageButtons[i].getStyleClass().clear();
				txMessageButtons[i].getStyleClass().add("button");
				txMessageButtons[i].getStyleClass().add("buttonMyQrg1");

				txMessageButtons[i].setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent actionEvent) {
						txt_chatMessageUserInput
								.setText(txt_chatMessageUserInput.getText() + txt_ownqrgSecondCategory.getText() + " ");
						System.out.println("2nd click");
					}
				});
//				MYQRGButton = txMessageButtons[i];

			}

			if (shortcuts.get(i).equals("/SETNAME MYQRG")) {
//				txMessageButtons[i].setTooltip(new Tooltip("Set your qrg as name in Chat"));
//				txMessageButtons[i]
//						.setStyle("-fx-background-color:\r\n" + "        linear-gradient(#c8fac0, #c8fac0),\r\n"
//								+ "        radial-gradient(center 50% -40%, radius 200%, #c8ee36 45%, #c0c800 50%);\r\n"
//								+ "    -fx-background-radius: 6, 5;\r\n" + "    -fx-background-insets: 0, 1;\r\n"
//								+ "    -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.4) , 5, 0.0 , 0 , 1 );\r\n"
//								+ "    -fx-text-fill: #395306");
				txMessageButtons[i].getStyleClass().clear();
				txMessageButtons[i].getStyleClass().add("button");
				txMessageButtons[i].getStyleClass().add("buttonMyQrg1");

				MYCALLSetQRGButton = txMessageButtons[i];
			}

			txMessageButtons[i].setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent arg0) {

					if (((Button) arg0.getSource()).getText().equals("MYQRG")) {
						((Button) arg0.getSource()).setTooltip(new Tooltip("MYQRG"));

					}

					if (((Button) arg0.getSource()).getTooltip() != null) {

						if (((Button) arg0.getSource()).getText().equals("MYQRG")
								|| ((Button) arg0.getSource()).getTooltip().getText().equals("MYQRG")) {

							((Button) arg0.getSource()).setTooltip(new Tooltip("MYQRG"));

							if (((Button) arg0.getSource()).getTooltip().getText().equals("MYQRG")) {
								txt_chatMessageUserInput
										.setText(txt_chatMessageUserInput.getText() + txt_ownqrgMainCategory.getText() + " ");
							}
						}

						if (((Button) arg0.getSource()).getText().equals("SECONDQRG")
								|| ((Button) arg0.getSource()).getTooltip().getText().equals("SECONDQRG")) {

							((Button) arg0.getSource()).setTooltip(new Tooltip("SECONDQRG"));

							if (((Button) arg0.getSource()).getTooltip().getText().equals("SECONDQRG")) {
								txt_chatMessageUserInput
										.setText(txt_chatMessageUserInput.getText() + txt_ownqrgSecondCategory.getText() + " ");
							}
						}
					} else {

//						System.out.println("Button clicked " + ((Button) arg0.getSource()).getText());
					appendResolvedMessageText(
							((Button) arg0.getSource()).getText() + " "
					);

				}

				}
			});
		}

		return txMessageButtons;
	}

	/**
	 * Resolves the operator profile this runtime works with.
	 *
	 * <p>Only executed on the very first launch. A profile switch sets the profile before
	 * building the new runtime, so the resolution is skipped there.</p>
	 *
	 * <p>An installation with no or exactly one profile is resolved without asking
	 * anything, which keeps the single operator startup exactly as it was.</p>
	 *
	 * @return true if the application may continue starting up
	 */
	private boolean resolveOperatorProfileIfRequired() {

		if (ActiveOperatorProfile.isInitialized()) {
			return true;
		}

		OperatorProfileBootstrap bootstrap = new OperatorProfileBootstrap();
		OperatorProfileSelection resolvedProfile = bootstrap.resolveAtStartup(
				new OperatorProfileStore(),
				CommandLineOptions.remembered(),
				OperatorProfilePickerWindow::showAndSelect);

		if (bootstrap.getStartupWarning() != null) {
			Alert startupWarning = new Alert(AlertType.WARNING);
			startupWarning.setTitle("Operator profile");
			startupWarning.setHeaderText("The requested operator profile was not found.");
			startupWarning.setContentText(bootstrap.getStartupWarning());
			startupWarning.showAndWait();
		}

		if (resolvedProfile == null) {
			Platform.exit();
			System.exit(0);
			return false;
		}

		ActiveOperatorProfile.set(resolvedProfile);
		return true;
	}

	/**
	 * Returns the window title suffix naming the active operator profile.
	 *
	 * <p>Empty for the historic single profile installation, so nothing changes visually
	 * for operators who never create a second profile.</p>
	 *
	 * @return the suffix to append to a window title, never null
	 */
	private String buildOperatorProfileTitleSuffix() {

		OperatorProfileSelection activeProfile = ActiveOperatorProfile.get();

		if (activeProfile == null || activeProfile.getProfile().isRootProfile()) {
			return "";
		}

		return " - " + activeProfile.getProfile().getDisplayName();
	}

	/**
	 * Lets the operator pick another profile and rebuilds the runtime for it.
	 *
	 * <p>Offers to create a second profile when only one exists, because the menu entry
	 * is the discoverable place to find the feature at all.</p>
	 */
	private void showOperatorProfileSwitchDialog() {

		OperatorProfileStore profileStore = new OperatorProfileStore();
		List<OperatorProfile> selectableProfiles = profileStore.loadProfiles();

		if (selectableProfiles.size() < 2) {
			Alert noProfilesYet = new Alert(AlertType.INFORMATION);
			noProfilesYet.setTitle("Operator profiles");
			noProfilesYet.setHeaderText("Only one operator profile is configured.");
			noProfilesYet.setContentText(
					"Additional profiles are created in the settings window on the "
							+ "\"Profiles\" tab. Each profile keeps its own settings and layout, "
							+ "and can either share the station worked database or use its own.");
			noProfilesYet.showAndWait();
			return;
		}

		OperatorProfileSelection activeProfile = ActiveOperatorProfile.get();
		String activeProfileId = activeProfile == null ? null : activeProfile.getProfile().getProfileId();

		Optional<OperatorProfile> chosenProfile =
				OperatorProfilePickerWindow.showAndSelect(selectableProfiles, activeProfileId);

		if (chosenProfile.isEmpty()) {
			return;
		}

		if (chosenProfile.get().getProfileId().equalsIgnoreCase(activeProfileId)) {
			return;
		}

		requestOperatorProfileSwitch(chosenProfile.get());
	}

	/**
	 * Confirms and performs a switch to another operator profile.
	 *
	 * <p>Shared by the File menu and the profile settings tab, so both ask the same
	 * question before giving up the running session.</p>
	 *
	 * @param targetProfile profile to activate
	 */
	private void requestOperatorProfileSwitch(OperatorProfile targetProfile) {

		if (targetProfile == null || !confirmOperatorProfileSwitch(targetProfile)) {
			return;
		}

		ApplicationRuntimeLauncher.switchProfile(targetProfile);
	}

	/**
	 * Asks whether the running session may be given up for a profile switch.
	 *
	 * @param targetProfile profile the operator selected
	 * @return true if the switch may proceed
	 */
	private boolean confirmOperatorProfileSwitch(OperatorProfile targetProfile) {

		Alert confirmation = new Alert(AlertType.CONFIRMATION);
		confirmation.setTitle("Switch operator profile");
		confirmation.setHeaderText("Switch to \"" + targetProfile.getDisplayName() + "\"?");
		confirmation.setContentText(
				"The ON4KST connection is closed and all windows are rebuilt with the "
						+ "settings and layout of the selected profile.\n\n"
						+ "Unsaved settings of the current profile are lost. Window sizes, "
						+ "divider and column widths are saved automatically.");

		ButtonType switchButton = new ButtonType("Switch profile", ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
		confirmation.getButtonTypes().setAll(switchButton, cancelButton);

		/*
		 * Raised above everything else on purpose. The request comes from the Compose
		 * settings window, which is not a JavaFX window and therefore not this dialog's
		 * owner; without this the confirmation opens behind it and the switch looks as
		 * if nothing happened at all.
		 */
		Window confirmationWindow = confirmation.getDialogPane().getScene().getWindow();
		if (confirmationWindow instanceof Stage confirmationStage) {
			confirmationStage.setAlwaysOnTop(true);
			confirmationStage.toFront();
		}

		return confirmation.showAndWait().orElse(cancelButton) == switchButton;
	}

	@Override
	public void stop() {
		shutdownRuntime();
		System.exit(0);
	}

	/**
	 * Releases every resource this runtime owns, without terminating the process.
	 *
	 * <p>Separated from {@link #stop()} so the same teardown can be reused when the
	 * operator switches to another profile and a fresh runtime is built afterwards.
	 * The method is idempotent and tolerates a runtime that never connected, because a
	 * switch may happen before the first login.</p>
	 */
	public void shutdownRuntime() {

		if (runtimeShutdownDone) {
			return;
		}

		runtimeShutdownDone = true;

		LOGGER.info("Application is shutting down and closing all resources");

		if (layoutAutosave != null) {
			// Flush before cancelling, otherwise a pending debounced write would either
			// be lost or land after a profile switch.
			layoutAutosave.flushPending();
			layoutAutosave.cancelPending();
		}

		cancelViewTimer(timer_buildWindowTitle);
		timer_buildWindowTitle = null;

//		timer_chatMemberTableSortTimer.purge();
//		timer_chatMemberTableSortTimer.cancel();

		cancelViewTimer(timer_updatePrivatemessageTable);
		timer_updatePrivatemessageTable = null;

		stopAnimation(userListRefreshCoalescer);
		userListRefreshCoalescer = null;
		stopAnimation(skedWarnBlinkTimeline);
		skedWarnBlinkTimeline = null;
		stopAnimation(bandUpgradeBlinkTimeline);
		bandUpgradeBlinkTimeline = null;



		/*
		 * Every mirror holds a listener inside a core roster. Without releasing them
		 * a discarded runtime stays reachable across an operator profile switch.
		 */
		rosterBindings.forEach(FxRosterBinding::dispose);
		rosterBindings.clear();
		chatMemberListBinding = null;
		selectedCallSignInfoMessageBinding = null;

		kst4contest.view.compose.map.StationMapWindow.INSTANCE.hide();

		closeOwnedStages();

		try {
			if (chatcontroller != null) {
				chatcontroller.disconnect(ApplicationConstants.DISCSTRING_DISCONNECT_AND_CLOSE);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Exception during disconnect", e);
		}
		chatcontroller.getMessageHistoryRecorder().close();
		closeFileLogging();
	}

	/**
	 * Cancels a timer created during user interface construction.
	 *
	 * @param timerToCancel timer to cancel, may be null when startup did not get that far
	 */
	private static void cancelViewTimer(Timer timerToCancel) {

		if (timerToCancel == null) {
			return;
		}

		timerToCancel.purge();
		timerToCancel.cancel();
	}

	/**
	 * Stops a JavaFX animation if it exists.
	 *
	 * @param animationToStop animation to stop, may be null
	 */
	private static void stopAnimation(Animation animationToStop) {

		if (animationToStop != null) {
			animationToStop.stop();
		}
	}

	/**
	 * Closes every window this runtime opened, so no stale window survives a profile
	 * switch. The map window is closed by its own dispose method.
	 */
	private void closeOwnedStages() {

		/*
		 * The settings window is a Compose window, not a Stage, so it has to be closed
		 * by name. It carries the Connect button, so a copy left over from the previous
		 * profile would block the new one from opening its own.
		 */
		SettingsWindow.close();
		UpdateWindow.close();
		MonitorWindow.close();
		releaseMonitorListeners();
		// Etappe 5c: Compose Fenster bleibt beim Profilwechsel offen
		// MainWindowHost.close();
		releaseComposeMainWindow();

		for (Stage ownedStage : new Stage[] {
				ownPrimaryStage }) {

			if (ownedStage != null) {
				try {
					ownedStage.close();
				} catch (Exception e) {
					System.out.println("[Main.java, Warning:] Could not close a window: " + e.getMessage());
				}
			}
		}

		ownPrimaryStage = null;
	}

	private void requestLayoutSave() {
		if (layoutAutosave != null) {
			layoutAutosave.requestSave();
		}
	}

	private Queue<Media> musicList = new LinkedList<Media>();
	private MediaPlayer mediaPlayer ;




	private void playCWLauncher(String playThisChars) {

		char[] playThisInCW = playThisChars.toUpperCase().toCharArray();

		for (char letterToPlay: playThisInCW){
			switch (letterToPlay){
				case 'A':
					musicList.add(new Media(new File ("LTTRA.mp3").toURI().toString()));
					break;
				case 'B':
					musicList.add(new Media(new File ("LTTRB.mp3").toURI().toString()));
					break;
				case 'C':
					musicList.add(new Media(new File ("LTTRC.mp3").toURI().toString()));
					break;
				case 'D':
					musicList.add(new Media(new File ("LTTRD.mp3").toURI().toString()));
					break;
				case 'E':
					musicList.add(new Media(new File ("LTTRE.mp3").toURI().toString()));
					break;
				case 'F':
					musicList.add(new Media(new File ("LTTRF.mp3").toURI().toString()));
					break;
				case 'G':
					musicList.add(new Media(new File ("LTTRG.mp3").toURI().toString()));
					break;
				case 'H':
					musicList.add(new Media(new File ("LTTRH.mp3").toURI().toString()));
					break;
				case 'I':
					musicList.add(new Media(new File ("LTTRI.mp3").toURI().toString()));
					break;
				case 'J':
					musicList.add(new Media(new File ("LTTRJ.mp3").toURI().toString()));
					break;
				case 'K':
					musicList.add(new Media(new File ("LTTRK.mp3").toURI().toString()));
					break;
				case 'L':
					musicList.add(new Media(new File ("LTTRL.mp3").toURI().toString()));
					break;
				case 'M':
					musicList.add(new Media(new File ("LTTRM.mp3").toURI().toString()));
					break;
				case 'N':
					musicList.add(new Media(new File ("LTTRN.mp3").toURI().toString()));
					break;
				case 'O':
					musicList.add(new Media(new File ("LTTRO.mp3").toURI().toString()));
					break;
				case 'P':
					musicList.add(new Media(new File ("LTTRP.mp3").toURI().toString()));
					break;
				case 'Q':
					musicList.add(new Media(new File ("LTTRQ.mp3").toURI().toString()));
					break;
				case 'R':
					musicList.add(new Media(new File ("LTTRR.mp3").toURI().toString()));
					break;
				case 'S':
					musicList.add(new Media(new File ("LTTRS.mp3").toURI().toString()));
					break;
				case 'T':
					musicList.add(new Media(new File ("LTTRT.mp3").toURI().toString()));
					break;
				case 'U':
					musicList.add(new Media(new File ("LTTRU.mp3").toURI().toString()));
					break;
				case 'V':
					musicList.add(new Media(new File ("LTTRV.mp3").toURI().toString()));
					break;
				case 'W':
					musicList.add(new Media(new File ("LTTRW.mp3").toURI().toString()));
					break;
				case 'X':
					musicList.add(new Media(new File ("LTTRX.mp3").toURI().toString()));
					break;
				case 'Y':
					musicList.add(new Media(new File ("LTTRY.mp3").toURI().toString()));
					break;
				case 'Z':
					musicList.add(new Media(new File ("LTTRZ.mp3").toURI().toString()));
					break;
				case '1':
					musicList.add(new Media(new File ("LTTR1.mp3").toURI().toString()));
					break;
				case '2':
					musicList.add(new Media(new File ("LTTR2.mp3").toURI().toString()));
					break;
				case '3':
					musicList.add(new Media(new File ("LTTR3.mp3").toURI().toString()));
					break;
				case '4':
					musicList.add(new Media(new File ("LTTR4.mp3").toURI().toString()));
					break;
				case '5':
					musicList.add(new Media(new File ("LTTR5.mp3").toURI().toString()));
					break;
				case '6':
					musicList.add(new Media(new File ("LTTR6.mp3").toURI().toString()));
					break;
				case '7':
					musicList.add(new Media(new File ("LTTR7.mp3").toURI().toString()));
					break;
				case '8':
					musicList.add(new Media(new File ("LTTR8.mp3").toURI().toString()));
					break;
				case '9':
					musicList.add(new Media(new File ("LTTR9.mp3").toURI().toString()));
					break;
				case '0':
					musicList.add(new Media(new File ("LTTR0.mp3").toURI().toString()));
					break;
				case '/':
					musicList.add(new Media(new File ("LTTRSTROKE.mp3").toURI().toString()));
					break;
				case ' ':
					musicList.add(new Media(new File ("LTTRSPACE.mp3").toURI().toString()));
					break;
				default:
					System.out.println("[KST4ContestApp, warning, letter not defined:] cwLetters = " + Arrays.toString(playThisInCW));
			}
		}
		playMusic();
//		mediaPlayer.dispose();

	}

	/**
	 * Plays a voice file for each char in the string (only EN alphabetic and numbers) except some specials: <br/><br/>
	 *
	 * 	case '!': BELL<br/>
	 *  case '?': YOUGOTMAIL<br/>
	 * 	case '#': HELLO<br/>
	 * 	case '*': 73 bye<br/>
	 * 	case '$': STROKEPORTABLE<br/>
	 * @param playThisChars
	 */
	private void playVoiceLauncher(String playThisChars) {

		char[] playThisInCW = playThisChars.toUpperCase().toCharArray();

		for (char letterToPlay: playThisInCW){
			switch (letterToPlay){
				case '!':
					musicList.add(new Media(new File ("VOICEBELL.mp3").toURI().toString()));
					break;
				case '?':
					musicList.add(new Media(new File ("VOICEYOUGOTMAIL.mp3").toURI().toString()));
					break;
				case '#':
					musicList.add(new Media(new File ("VOICEHELLO.mp3").toURI().toString()));
					break;
				case '*':
					musicList.add(new Media(new File ("VOICE73.mp3").toURI().toString()));
					break;
				case '$':
					musicList.add(new Media(new File ("VOICESTROKEPORTABLE.mp3").toURI().toString()));
					break;
				case 'A':
					musicList.add(new Media(new File ("VOICEA.mp3").toURI().toString()));
					break;
				case 'B':
					musicList.add(new Media(new File ("VOICEB.mp3").toURI().toString()));
					break;
				case 'C':
					musicList.add(new Media(new File ("VOICEC.mp3").toURI().toString()));
					break;
				case 'D':
					musicList.add(new Media(new File ("VOICED.mp3").toURI().toString()));
					break;
				case 'E':
					musicList.add(new Media(new File ("VOICEE.mp3").toURI().toString()));
					break;
				case 'F':
					musicList.add(new Media(new File ("VOICEF.mp3").toURI().toString()));
					break;
				case 'G':
					musicList.add(new Media(new File ("VOICEG.mp3").toURI().toString()));
					break;
				case 'H':
					musicList.add(new Media(new File ("VOICEH.mp3").toURI().toString()));
					break;
				case 'I':
					musicList.add(new Media(new File ("VOICEI.mp3").toURI().toString()));
					break;
				case 'J':
					musicList.add(new Media(new File ("VOICEJ.mp3").toURI().toString()));
					break;
				case 'K':
					musicList.add(new Media(new File ("VOICEK.mp3").toURI().toString()));
					break;
				case 'L':
					musicList.add(new Media(new File ("VOICEL.mp3").toURI().toString()));
					break;
				case 'M':
					musicList.add(new Media(new File ("VOICEM.mp3").toURI().toString()));
					break;
				case 'N':
					musicList.add(new Media(new File ("VOICEN.mp3").toURI().toString()));
					break;
				case 'O':
					musicList.add(new Media(new File ("VOICEO.mp3").toURI().toString()));
					break;
				case 'P':
					musicList.add(new Media(new File ("VOICEP.mp3").toURI().toString()));
					break;
				case 'Q':
					musicList.add(new Media(new File ("VOICEQ.mp3").toURI().toString()));
					break;
				case 'R':
					musicList.add(new Media(new File ("VOICER.mp3").toURI().toString()));
					break;
				case 'S':
					musicList.add(new Media(new File ("VOICES.mp3").toURI().toString()));
					break;
				case 'T':
					musicList.add(new Media(new File ("VOICET.mp3").toURI().toString()));
					break;
				case 'U':
					musicList.add(new Media(new File ("VOICEU.mp3").toURI().toString()));
					break;
				case 'V':
					musicList.add(new Media(new File ("VOICEV.mp3").toURI().toString()));
					break;
				case 'W':
					musicList.add(new Media(new File ("VOICEW.mp3").toURI().toString()));
					break;
				case 'X':
					musicList.add(new Media(new File ("VOICEX.mp3").toURI().toString()));
					break;
				case 'Y':
					musicList.add(new Media(new File ("VOICEY.mp3").toURI().toString()));
					break;
				case 'Z':
					musicList.add(new Media(new File ("VOICEZ.mp3").toURI().toString()));
					break;
				case '1':
					musicList.add(new Media(new File ("VOICE1.mp3").toURI().toString()));
					break;
				case '2':
					musicList.add(new Media(new File ("VOICE2.mp3").toURI().toString()));
					break;
				case '3':
					musicList.add(new Media(new File ("VOICE3.mp3").toURI().toString()));
					break;
				case '4':
					musicList.add(new Media(new File ("VOICE4.mp3").toURI().toString()));
					break;
				case '5':
					musicList.add(new Media(new File ("VOICE5.mp3").toURI().toString()));
					break;
				case '6':
					musicList.add(new Media(new File ("VOICE6.mp3").toURI().toString()));
					break;
				case '7':
					musicList.add(new Media(new File ("VOICE7.mp3").toURI().toString()));
					break;
				case '8':
					musicList.add(new Media(new File ("VOICE8.mp3").toURI().toString()));
					break;
				case '9':
					musicList.add(new Media(new File ("VOICE9.mp3").toURI().toString()));
					break;
				case '0':
					musicList.add(new Media(new File ("VOICE0.mp3").toURI().toString()));
					break;
				case '/':
					musicList.add(new Media(new File ("VOICESTROKE.mp3").toURI().toString()));
					break;
//				case ' ':
//					musicList.add(new Media(new File ("VOICESPACE.mp3").toURI().toString()));
//					break;
				default:
					System.out.println("[KST4ContestApp, warning, letter not defined:] cwLetters = " + Arrays.toString(playThisInCW));
			}
		}
		playMusic();
//		mediaPlayer.dispose();

	}

//	protected static final int SAMPLE_RATE = 16 * 1024;




	private void playMusic() {


//		System.out.println("Kst4ContestApplication.playMusic");
		if(musicList.peek() == null)
		{
			return;
		}
		mediaPlayer = new MediaPlayer(musicList.poll());
		mediaPlayer.setRate(1.0);

		mediaPlayer.setOnReady(() -> {
			mediaPlayer.play();
			mediaPlayer.setOnEndOfMedia(() -> {
//				mediaPlayer.dispose();
				playMusic();
				if (musicList.isEmpty()) {
//					mediaPlayer.dispose();
				}
			});
		});

	}


	/**
	 * Calculates a screen-aware startup size for the main chat window.
	 *
	 * Preferences store the main scene size as:
	 * - index 0 = height
	 * - index 1 = width
	 *
	 * The stored size is used unchanged as long as it fits into the currently
	 * available primary screen area. If the application was last used on a larger
	 * monitor and is now started on a smaller screen, the size is reduced so the
	 * main window remains usable immediately after startup.
	 *
	 * Screen.getVisualBounds() is used instead of Screen.getBounds() because the
	 * visual bounds exclude task bars, docks and similar OS UI areas.
	 *
	 * The returned array keeps the same H/W order as ChatPreferences:
	 * - index 0 = corrected height
	 * - index 1 = corrected width
	 */
	private double[] getScreenAwareMainSceneSizeHW(double[] storedSceneSizeHW) {

		final double fallbackHeight = 768.0;
		final double fallbackWidth = 1234.0;

		/*
		 * Leave a little room for the native window decoration and screen edges.
		 * JavaFX Scene size does not include the full native Stage decoration.
		 */
		final double screenMargin = 40.0;

		double storedHeight = fallbackHeight;
		double storedWidth = fallbackWidth;

		if (storedSceneSizeHW != null && storedSceneSizeHW.length >= 2) {
			if (Double.isFinite(storedSceneSizeHW[0]) && storedSceneSizeHW[0] > 0) {
				storedHeight = storedSceneSizeHW[0];
			}

			if (Double.isFinite(storedSceneSizeHW[1]) && storedSceneSizeHW[1] > 0) {
				storedWidth = storedSceneSizeHW[1];
			}
		}

		Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();

		double availableWidth = Math.max(1.0, visualBounds.getWidth() - screenMargin);
		double availableHeight = Math.max(1.0, visualBounds.getHeight() - screenMargin);

		double correctedWidth = Math.min(storedWidth, availableWidth);
		double correctedHeight = Math.min(storedHeight, availableHeight);

		if (correctedWidth != storedWidth || correctedHeight != storedHeight) {
			System.out.println("[Main.java, Info]: Main window startup size reduced to fit current screen. "
					+ "storedWidth=" + storedWidth
					+ ", storedHeight=" + storedHeight
					+ ", availableWidth=" + availableWidth
					+ ", availableHeight=" + availableHeight
					+ ", correctedWidth=" + correctedWidth
					+ ", correctedHeight=" + correctedHeight);
		}

		return new double[] {
				correctedHeight,
				correctedWidth
		};
	}

	@Override
	public void init() {

		Parameters applicationParameters = getParameters();

		CommandLineOptions.remember(CommandLineOptions.parse(
				applicationParameters == null ? null : applicationParameters.getRaw()));
	}

	@Override
	public void start(Stage primaryStage) throws InterruptedException, IOException, URISyntaxException {

		if (!resolveOperatorProfileIfRequired()) {
			return;
		}

		ownPrimaryStage = primaryStage;

		/*
		 * A profile switch closes every window of the old runtime before the new one
		 * exists. With the JavaFX default that would end the process, so the application
		 * takes over the exit decision and closing the main window is handled explicitly.
		 */
		Platform.setImplicitExit(false);
		primaryStage.setOnCloseRequest(closeRequest -> {
			closeRequest.consume();
			ApplicationRuntimeLauncher.exitApplication();
		});
		ApplicationRuntimeLauncher.setCurrent(this);

		GuiUtils.applyApplicationIcon(primaryStage);

		VBox pnl_inputAndSendButtons = new VBox(); //gets the sendtext field, send button and the timeline
		timelineView = new TimelineView();
		timelineView.prefWidthProperty().bind(pnl_inputAndSendButtons.widthProperty());
		timelineView.setMinHeight(80); //min height
		timelineView.setPrefHeight(80);
		timelineView.setStyle("-fx-background-color: #333333; -fx-border-color: red;"); //TODO:Debug!
		pnl_inputAndSendButtons.getChildren().add(timelineView);

		timelineView.setSkedTooltipExtraTextProvider(this::buildSkedHoverInfo);

		/**
		 * if user changing width
		 */
		timelineView.widthProperty().addListener((obs, oldV, newV) -> {
			if (newV.doubleValue() > 10) {
				updateTimelineVisuals();
			}
		});

		/**
		 * if user changing height
		 */
		timelineView.heightProperty().addListener((obs, oldV, newV) -> {
			if (newV.doubleValue() > 10) {
				updateTimelineVisuals();
			}
		});


		ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, STYLE_DEFAULTCSSDAY_RESOURCE, STYLE_DEFAULTCSSDAY_FILE);
		ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, STYLE_DEFAULTCSSEVENING_RESOURCE, STYLE_DEFAULTCSSEVENING_FILE);
		ChatMember ownChatMemberObject = new ChatMember();
		OperatorProfileSelection activeOperatorProfile = ActiveOperatorProfile.get();

		// instantiate the Chatcontroller with the user object and the files of the active profile
		chatcontroller = new ChatController(
				ownChatMemberObject,
				this,
				activeOperatorProfile.getPreferencesRelativeFileName(),
				activeOperatorProfile.getWorkedDatabaseRelativeFileName(),
				activeOperatorProfile.isSeedWorkedDatabaseFromResource(),
				uiDispatcher);
		setDebugFileLoggingEnabled(chatcontroller.getChatPreferences()
				.isMessageHandling_debugModeToFileEnabled());
		layoutAutosave = new LayoutAutosave(chatcontroller.getChatPreferences());
		// Every profile starts with its own configured design; the menu quick switch only lasts for the session.
		chatcontroller.getChatPreferences().setGUI_darkModeActive(
				chatcontroller.getChatPreferences().isGUI_darkModeActiveByDefault());
		messageVariableResolver = new MessageVariableResolver(chatcontroller.getChatPreferences());
		chatcontroller.setStatusListener(this); //callback interface for updating Thread events in visual

		// 1. Timeline an die Sked-Liste binden
		chatcontroller.getActiveSkeds().addListener(skeds -> updateTimelineVisuals());

		// 1. bind table to the sked list
		chatcontroller.getScoreService().uiPulse().addListener(newVal -> {
			updateTimelineVisuals();
		});

		// Keep TimelineView antenna azimuth in sync with preferences (rotator / QTF)
//		chatcontroller.getChatPreferences().getActualQTF().addListener(newV -> {
//			timelineView.setCurrentAntennaAzimuth(newV.doubleValue());
//			timelineView.updateVisuals(chatcontroller.getActiveSkeds());
//		});

		// initial value
		timelineView.setCurrentAntennaAzimuth(chatcontroller.getChatPreferences().getActualQTF().get());
		timelineView.setBeamWidthDeg(chatcontroller.getChatPreferences().getStn_antennaBeamWidthDeg());

		// Update visuals when rotor direction changes
		chatcontroller.getChatPreferences().getActualQTF().addListener(newV -> {
			timelineView.setCurrentAntennaAzimuth(newV.doubleValue());
			updateTimelineVisuals();
		});

		try {

			txt_ownqrgMainCategory.getStyleClass().clear();
			txt_ownqrgMainCategory.getStyleClass().add("text-input");
			txt_ownqrgMainCategory.getStyleClass().add("text-input-MYQRG1");

			txt_ownqrgMainCategory.focusedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
						Boolean newPropertyValue) {
					if (newPropertyValue) {
						// Do nothing until field loses focus, user will enter his frequency
					} else {
						System.out.println(
								"[Main.java, Info]: Set the frequency1 property by hand to: " + txt_ownqrgMainCategory.getText());
						chatcontroller.getChatPreferences().getMYQRGFirstCat().set(txt_ownqrgMainCategory.getText());
					}
				}
			});

			txt_ownqrgSecondCategory.getStyleClass().clear();
			txt_ownqrgSecondCategory.getStyleClass().add("text-input");
			txt_ownqrgSecondCategory.getStyleClass().add("text-input-MYQRG1");

			txt_ownqrgSecondCategory.focusedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
									Boolean newPropertyValue) {
					if (newPropertyValue) {
						// Do nothing until field loses focus, user will enter his frequency
					} else {
						System.out.println(
								"[Main.java, Info]: Set the frequency2 property by hand to: " + txt_ownqrgSecondCategory.getText());
//			            chatcontroller.getChatPreferences().setMYQRG(txt_ownqrgSecondCategory.getText());
						chatcontroller.getChatPreferences().getMYQRGSecondCat().set(txt_ownqrgSecondCategory.getText());
					}
				}
			});


			txt_myQTF.getStyleClass().clear();
			txt_myQTF.getStyleClass().add("text-input");
			txt_myQTF.getStyleClass().add("text-input-MYQRG1");

			// Rotator sync on/off determines whether this field is bound (read-only, driven
			// by PSTRotator) or free for manual entry, mirroring the MYQRG sync behaviour above.
			if (chatcontroller.getChatPreferences().isStn_pstRotatorEnabled()) {
				attachQtfFollower();
				txt_myQTF.setTooltip(new Tooltip("This is your current QTF, read out at PSTRotator"));
				txt_myQTF.setFocusTraversable(false);
			} else {
				txt_myQTF.setText(Double.toString(chatcontroller.getChatPreferences().getActualQTF().get()));
				txt_myQTF.setTooltip(new Tooltip("Enter your antenna heading (QTF) by hand - no rotator sync active"));
				txt_myQTF.setFocusTraversable(true);
			}

			txt_myQTF.focusedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
						Boolean newPropertyValue) {
					if (newPropertyValue) {
//			            System.out.println("Textfield on focus");
						// Do nothing until field loses focus, user will enter his frequency
					} else {
						if (txt_myQTF.textProperty().isBound()) {
							return; // rotator sync active, field is driven by PSTRotator
						}
						try {
						System.out.println(
								"[Main.java, Info]: Set the MYQTF property by hand to: " + txt_myQTF.getText());
						chatcontroller.getChatPreferences().getActualQTF().set(Double.parseDouble(txt_myQTF.getText()));}
						catch (Exception exception) {
							System.out.println("bullshit entered in myqtf");
							txt_myQTF.setText("0.0");
						}
					}
				}
			});

			txt_myQTF.setPrefSize(40, 0);
//			txt_ownqrg.setMinSize(40, 0);
			txt_myQTF.setAlignment(Pos.BASELINE_RIGHT);

			SplitPane mainWindowLeftSplitPane = new SplitPane();
			mainWindowLeftSplitPane.setOrientation(Orientation.HORIZONTAL);

			BorderPane bPaneChatWindow = new BorderPane();

			/*
			 * Restore the main window size from preferences, but never start larger than
			 * the currently available screen area. This avoids unusable oversized windows
			 * when KST4Contest was last used on a larger monitor.
			 */
			double[] screenAwareMainSceneSizeHW = getScreenAwareMainSceneSizeHW(
					chatcontroller.getChatPreferences().getGUIscn_ChatwindowMainSceneSizeHW()
			);

			scn_ChatwindowMainScene = new Scene(
					bPaneChatWindow,
					screenAwareMainSceneSizeHW[1],
					screenAwareMainSceneSizeHW[0]
			);

			registerThemedScene(scn_ChatwindowMainScene);



			//add listeners for size changes to restore after startup
			scn_ChatwindowMainScene.widthProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observableValue, Number number, Number newWidthValue) {
					chatcontroller.getChatPreferences().getGUIscn_ChatwindowMainSceneSizeHW()[1] = newWidthValue.doubleValue();
					requestLayoutSave();
				}
			});

			scn_ChatwindowMainScene.heightProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observableValue, Number number, Number newHeightValue) {
					chatcontroller.getChatPreferences().getGUIscn_ChatwindowMainSceneSizeHW()[0] = newHeightValue.doubleValue();
					requestLayoutSave();
				}
			});

			scn_ChatwindowMainScene.setOnKeyPressed(keyEvent -> {
				if (keyEvent.getCode() == KeyCode.ENTER) {
					sendButton.fire();
					keyEvent.consume();
					return;
				}

				if (keyEvent.getCode() == KeyCode.ESCAPE) {
					txt_chatMessageUserInput.clear();
					keyEvent.consume();
					return;
				}

				int snippetIndex = resolveSnippetIndex(keyEvent);
				if (snippetIndex < 0) {
					return;
				}

				insertTextSnippet(snippetIndex);
				keyEvent.consume();
			});



			mainScreenMenuBar = initMenuBar();
            flwpne_StatusBar = new FlowPane();

            flwpne_StatusBar.getChildren().add(mainScreenMenuBar);
			bPaneChatWindow.setTop(flwpne_StatusBar);

			initConnectionStateIndicatorButton();
			// On macOS the connection state is shown in the system menu bar instead.
			if (!PlatformUtils.isMacOs()) {
				flwpne_StatusBar.getChildren().add(btnConnectionStateIndicator);
			}

			initSkedWarnIndicatorButton();

			chatcontroller.lastUiReminderEvent().addListener(
					reminderEvent -> {
						if (reminderEvent == null) {
							return;
						}

						String text = "REMINDER: "
								+ reminderEvent.getCallSignRaw()
								+ "  T-"
								+ reminderEvent.getMinutesBefore()
								+ "m";

						Platform.runLater(
								() -> showBlinkingSkedWarnIndicator(text)
						);
					}
			);

			flwpne_StatusBar.getChildren().add(btnSkedWarnIndicator);


			initBandUpgradeIndicatorButton();
			flwpne_StatusBar.getChildren().add(btnBandUpgradeIndicator);



			SplitPane messageSectionSplitpane = new SplitPane();
			messageSectionSplitpane.setOrientation(Orientation.VERTICAL);

			HBox textInputFlowPane = new HBox();

//			FlowPane textInputFlowPane = new FlowPane();

			sendButton = new Button("TX");
			sendButton.setMinSize(20, 0);
//			sendButton.setOnAction(new EventHandler<ActionEvent>() {
//				@Override
//				public void handle(ActionEvent event) {
//
//					ChatMessage sendMe = new ChatMessage();
//
//					/*
//					 * Resolve the selected station safely.
//					 *
//					 * Normal case:
//					 * - selectedCallSignInfoStageChatMember is set by clicking/selecting a station.
//					 *
//					 * Fallbacks:
//					 * - current table selection
//					 * - ScoreService selected member
//					 *
//					 * If no station is selected at all, the message is sent to the main category.
//					 */
//					ChatMember effectiveSelectedMember = selectedCallSignInfoStageChatMember;
//
//					if (effectiveSelectedMember == null
//							&& tbl_chatMember != null
//							&& tbl_chatMember.getSelectionModel() != null) {
//						effectiveSelectedMember = tbl_chatMember.getSelectionModel().getSelectedItem();
//					}
//
//					if (effectiveSelectedMember == null
//							&& chatcontroller != null
//							&& chatcontroller.getScoreService() != null) {
//						effectiveSelectedMember = chatcontroller.getScoreService().getSelectedChatMember();
//					}
//
//					/*
//					 * Default decision:
//					 * If no station is selected, use the main category.
//					 */
//					ChatCategory sendMeInThisCat = chatcontroller.getChatCategoryMain();
//
//					/*
//					 * If a station is selected and its category matches one of our active chat
//					 * categories, send in that category. This keeps the old behaviour, but avoids
//					 * NullPointerExceptions when no station has been selected yet.
//					 */
//					if (effectiveSelectedMember != null && effectiveSelectedMember.getChatCategory() != null) {
//
//						String categoryNumber = effectiveSelectedMember.getChatCategory().getCategoryNumber() + "";
//
//						if (chatcontroller.getChatCategoryMain() != null
//								&& categoryNumber.equals(chatcontroller.getChatCategoryMain().getCategoryNumber() + "")) {
//
//							sendMeInThisCat = chatcontroller.getChatCategoryMain();
//
//						} else if (chatcontroller.getChatCategorySecondChat() != null
//								&& categoryNumber.equals(chatcontroller.getChatCategorySecondChat().getCategoryNumber() + "")) {
//
//							sendMeInThisCat = chatcontroller.getChatCategorySecondChat();
//
//						} else {
//							sendMeInThisCat = chatcontroller.getChatCategoryMain(); // Chatcategory default decision
//						}
//					}
//
//					String selectedMemberDebugText;
//					if (effectiveSelectedMember == null) {
//						selectedMemberDebugText = "none, using main category";
//					} else {
//						selectedMemberDebugText = effectiveSelectedMember.getCallSignRaw()
//								+ " / "
//								+ effectiveSelectedMember.getChatCategory();
//					}
//
//					System.out.println("<<<<<<<<<<<<<<<<<<<<< detected Category for sending message is "
//							+ sendMeInThisCat
//							+ " // selected member: "
//							+ selectedMemberDebugText
//							+ " evt "
//							+ event.isConsumed());
//
//					sendMe.setChatCategory(sendMeInThisCat); // new in 1.26, answer in channel of the selected member if available
//					sendMe.setMessageText(txt_chatMessageUserInput.getText());
//
//					// If operator sends "/cq CALL ..." => arm pending ping metrics for reply-time / no-reply tracking
//					chatcontroller.getStationMetricsService().tryRecordOutboundCq(sendMe.getMessageText(), System.currentTimeMillis());
//					chatcontroller.getScoreService().requestRecompute("outbound-tx");
//
//					sendMe.setMessageDirectedToServer(false);
//
//					chatcontroller.getMessageTXBus().add(sendMe); // move the message to the tx queue
//
//					txt_chatMessageUserInput.clear();
//				}
//			});

			sendButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {

					ChatMessage sendMe = new ChatMessage();

					/*
					 * Resolve the selected station safely.
					 *
					 * Normal case:
					 * - selectedCallSignInfoStageChatMember is set by clicking/selecting a station.
					 *
					 * Fallbacks:
					 * - current table selection
					 * - ScoreService selected member
					 *
					 * If no station is selected at all, the message is sent to the main category.
					 */
					ChatMember effectiveSelectedMember = selectedCallSignInfoStageChatMember;

					if (effectiveSelectedMember == null
							&& tbl_chatMember != null
							&& tbl_chatMember.getSelectionModel() != null) {
						effectiveSelectedMember = tbl_chatMember.getSelectionModel().getSelectedItem();
					}

					/*
					 * Resolve variables only after the key or mouse event that edited the
					 * TextField has finished. This also supports variables entered manually
					 * instead of through a shortcut or snippet.
					 */
					String resolvedOutgoingText =
							messageVariableResolver.resolveForSelectedStation(
									txt_chatMessageUserInput.getText(),
									effectiveSelectedMember
							);

					/*
					 * A variable may legitimately resolve to an empty string. In that case
					 * there is no message to send.
					 */
					if (resolvedOutgoingText == null || resolvedOutgoingText.isBlank()) {
						txt_chatMessageUserInput.clear();
						return;
					}

					/*
					 * Keep the existing protection against private messages to the local
					 * callsign, but perform the check at the controlled send boundary.
					 */
					if (isMessageAddressedToOwnCallsign(resolvedOutgoingText)) {
						txt_chatMessageUserInput.clear();
						return;
					}


					if (effectiveSelectedMember == null
							&& chatcontroller != null
							&& chatcontroller.getScoreService() != null) {
						effectiveSelectedMember = chatcontroller.getScoreService().getSelectedChatMember();
					}

					/*
					 * Category decision:
					 *
					 * If the outgoing text starts with "/cq CALL ...", the target callsign inside
					 * the text is authoritative. The message category is therefore resolved from
					 * that target callsign, not blindly from the current table selection.
					 *
					 * This is important because the ChatMember table can emit artificial selection
					 * events during periodic refreshes. Such events must not move an already typed
					 * /cq message into another channel.
					 */
					ChatCategory sendMeInThisCat = resolveOutgoingChatCategory(
							resolvedOutgoingText,
							effectiveSelectedMember
					);

					/*
					 * Final safety fallback:
					 * If the category could not be resolved for any reason, use the main category.
					 */
					if (sendMeInThisCat == null) {
						sendMeInThisCat = chatcontroller.getChatCategoryMain();
					}

					/*
					 * Debug output:
					 * Helps verify whether the selected station and the final send category match
					 * the expected behavior during tests.
					 */
					String selectedMemberDebugText;
					if (effectiveSelectedMember == null) {
						selectedMemberDebugText = "none, using main category";
					} else {
						selectedMemberDebugText = effectiveSelectedMember.getCallSignRaw()
								+ " / "
								+ effectiveSelectedMember.getChatCategory();
					}

					System.out.println("<<<<<<<<<<<<<<<<<<<<< detected Category for sending message is "
							+ sendMeInThisCat
							+ " // selected member: "
							+ selectedMemberDebugText
							+ " evt "
							+ event.isConsumed());

					/*
					 * Build outgoing chat message.
					 *
					 * The category has already been resolved above. It may be:
					 * - the category of the /cq target callsign,
					 * - the category of the currently selected member,
					 * - or the main category as fallback.
					 */
					sendMe.setChatCategory(sendMeInThisCat);
					sendMe.setMessageText(resolvedOutgoingText);

					/*
					 * If operator sends "/cq CALL ..." then update the station metrics.
					 *
					 * This keeps reply-time / no-reply tracking working after the category fix.
					 */
					chatcontroller.getStationMetricsService()
							.tryRecordOutboundCq(sendMe.getMessageText(), System.currentTimeMillis());

					chatcontroller.getScoreService().requestRecompute("outbound-tx");

					sendMe.setMessageDirectedToServer(false);

					/*
					 * Move the message to the TX queue.
					 *
					 * Actual sending is still handled by the existing TX mechanism.
					 */
					chatcontroller.getMessageTXBus().add(sendMe);

					/*
					 * Clear the input field only after the message has been queued.
					 *
					 * This is an intentional clear after sending, not the old problematic
					 * selection-listener overwrite.
					 */
					txt_chatMessageUserInput.clear();
				}
			});

//			sendButton.setMnemonicParsing(true);

			Button btn_clear = new Button("clear");
			btn_clear.setMinSize(20, 0);
			btn_clear.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
//			    	System.out.println("clear clicked: " + event.toString());
					txt_chatMessageUserInput.clear();

				}
			});

//			TextField txt_chatMessageUserInput
//			txt_chatMessageUserInput.setPrefWidth("80%");
			txt_chatMessageUserInput.setFocusTraversable(false);
			txt_chatMessageUserInput.setPrefSize(500, 0);
			txt_chatMessageUserInput.setText("");
			txt_chatMessageUserInput.setTooltip(new Tooltip("Textmessage to Chat"));
			txt_chatMessageUserInput.setOnKeyPressed(new EventHandler<KeyEvent>() {

				@Override
				public void handle(KeyEvent event) {
					if (event.getCode().equals(KeyCode.ENTER)) {
//			        	System.out.println("Enter pressed");

						event.consume();
						sendButton.fire();
					}
				}
			});


			final Separator sepVert1 = new Separator();
			sepVert1.setOrientation(Orientation.VERTICAL);
			sepVert1.setValignment(VPos.CENTER);
//	        sepVert1.setPrefHeight(80);
			sepVert1.setPrefWidth(30);

			txt_ownqrgMainCategory.setText("MYQRG");
			txt_ownqrgMainCategory.setPrefSize(70, 0);
			txt_ownqrgMainCategory.setAlignment(Pos.BASELINE_LEFT);
			txt_ownqrgMainCategory.setFocusTraversable(false);

//			txt_ownqrgSecondCategory.setText("SECONDQRG");
			txt_ownqrgSecondCategory.setText(chatcontroller.getChatPreferences().getMYQRGSecondCat().get());
			txt_ownqrgSecondCategory.setPrefSize(70, 0);
			txt_ownqrgSecondCategory.setAlignment(Pos.BASELINE_CENTER);
			txt_ownqrgSecondCategory.setFocusTraversable(false);
			txt_ownqrgSecondCategory.setTooltip(new Tooltip("Enter frequency for second chat-category here by hand! <fixme>"));

			primaryStage.setTitle(chatcontroller.getChatPreferences().getChatState() + buildOperatorProfileTitleSuffix());

			timer_buildWindowTitle = new Timer();
			timer_buildWindowTitle.scheduleAtFixedRate(new TimerTask() {
				public void run() {

					Thread.currentThread().setName("buildWindowTitleTimer");

					Platform.runLater(() -> {

						String chatState = "";
						if (chatcontroller.isConnectedAndLoggedIn()) {

							chatState = "Connected to: " + chatcontroller.getChatPreferences().getLoginChatCategoryMain();

							if (chatcontroller.getChatPreferences().isLoginToSecondChatEnabled()) {
								chatState += " and " + chatcontroller.getChatPreferences().getLoginChatCategorySecond();
							}

							chatState += " "
									+ " as " + chatcontroller.getChatPreferences().getStn_loginCallSign() + " ("
									+ chatcontroller.getChatPreferences().getStn_loginNameMainCat() + ")" + " in "
									+ chatcontroller.getChatPreferences().getStn_loginLocatorMainCat() + " ("
									+ chatcontroller.getLst_chatMemberList().size() + " users online, "
									+ chatcontroller.visibleChatMemberCount() + " shown), "
									+ (chatcontroller.getLst_globalChatMessageList().size())
									+ " messages total.";
							chatcontroller.getChatPreferences().setChatState(chatState);
						}

						else {
							On4KstConnectionState connectionState =
									chatcontroller.getOn4KstConnectionState();

							chatState = switch (connectionState) {
								case RECONNECT_WAIT ->
										"CONNECTION LOST – reconnect scheduled";
								case CONNECTING ->
										"Connecting to ON4KST…";
								case WAITING_FOR_LOGIN_PROMPT, AUTHENTICATING ->
										"Connected – authenticating with ON4KST…";
								case SYNCING_MAIN_CHAT, SYNCING_SECOND_CHAT ->
										"Connected – synchronizing ON4KST chat data…";
								default ->
										"DISCONNECTED!";
							};

							chatcontroller.getChatPreferences().setChatState(chatState);
						}

						if (chatcontroller.isDisconnected()) {
							chatState = "DISCONNECTED!";
							chatcontroller.getChatPreferences().setChatState(chatState);
						}

						primaryStage.setTitle(chatcontroller.getChatPreferences().getChatState() + buildOperatorProfileTitleSuffix());

//						System.out.println(chatcontroller.getChatPreferences().getChatState());
					});
				}
			}, new Date(), 5000);



			textInputFlowPane.setSpacing(6);
			textInputFlowPane.setAlignment(Pos.CENTER_LEFT);

			textInputFlowPane.getChildren().addAll(txt_chatMessageUserInput, sendButton, btn_clear, sepVert1,
					txt_ownqrgMainCategory, txt_ownqrgSecondCategory, txt_myQTF);


			flwPane_textSnippets = new FlowPane();

			flwPane_textSnippets.getChildren()
					.addAll(buttonFactory(mirrorOf(this.chatcontroller.getChatPreferences().getLst_txtShortCutBtnList())));

			TableView<ChatMessage> privateMessageTable = initChatprivateMSGTable();

			chatMessageContextMenu = initChatMemberTableContextMenu(
					mirrorOf(this.chatcontroller.getChatPreferences().getLst_txtSnipList())); // new mechanic

			privateMessageTable.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent t) {
					if (t.getButton() == MouseButton.SECONDARY) {
						chatMessageContextMenu.show(primaryStage, t.getScreenX(), t.getScreenY());

					}
				}
			});

			TableViewSelectionModel<ChatMessage> privateChatselectionModelChatMessage = privateMessageTable
					.getSelectionModel();
			privateChatselectionModelChatMessage.setSelectionMode(SelectionMode.SINGLE);

			ObservableList<ChatMessage> selectedChatMessageList = privateChatselectionModelChatMessage
					.getSelectedItems();
			privateChatselectionModelChatMessage.selectedItemProperty().addListener(
					(observableValue, oldSelectedMessage, newSelectedMessage) -> {
						if (newSelectedMessage == null) {
							return;
						}

						/*
						 * Special case:
						 * If the selected private message was sent by ourselves, the sender is our
						 * own callsign. Replying to ourselves would be wrong, so the receiver is
						 * extracted from the original message text and used as /cq target.
						 */
						if (newSelectedMessage.getSender().getCallSign().equals(chatcontroller.getChatPreferences().getStn_loginCallSign())) {
							System.out.println("////////////////////////////// rx in orginal message: "
									+ newSelectedMessage.getReceiver().getCallSign());

							String receiverCallsign = newSelectedMessage.getMessageText()
									.substring(2, (newSelectedMessage.getMessageText().indexOf(")")));

							System.out.println("privChat selected ChatMember: was own object...! rx was: " + receiverCallsign);

							prepareCqTextForCallsign(receiverCallsign, newSelectedMessage.getChatCategory(), false);

						} else {
							prepareCqTextForSelectedChatMember(newSelectedMessage.getSender(), false);
							focusChatMemberAndPrepareCq(newSelectedMessage.getSender(), false);

							try {
								selectedCallSignFurtherInfoPane.getChildren().clear();
								selectedCallSignInfoStageChatMember = newSelectedMessage.getSender();
								chatcontroller.getScoreService().setSelectedChatMember(selectedCallSignInfoStageChatMember);
								selectedCallSignFurtherInfoPane.getChildren()
										.add(generateFurtherInfoAbtSelectedCallsignBP(selectedCallSignInfoStageChatMember));
								txt_chatMessageUserInput.requestFocus();
								txt_chatMessageUserInput.selectEnd();
							} catch (Exception exception) {
								System.out.println("KST4CApp, <<<catched error>>>>: message sender is not in the userlist any more!");
							}

							System.out.println("privChat selected ChatMember: " + newSelectedMessage.getSender());
						}
					}
			);

//			selectedChatMessageList.addListener(new ListChangeListener<ChatMessage>() {
//				@Override
//				public void onChanged(Change<? extends ChatMessage> selectedChatMemberPrivateChat) {
//					if (privateChatselectionModelChatMessage.getSelectedItems().isEmpty()) {
//						// do nothing, that was a deselection-event!
//					} else {
//
//						/**
//						 * We need a special trick here. Since the private message list is a messagelist only for my own callsign, it´s not useful to show a sender and receiver.
//						 * But if you choose a line with a message which you sent do another station, the default mechanism will type "/cq MYOWNCALL" to the textfield and if you are sleepy,
//						 * you wouldnt remark that you sent a message to yourself. Thatswhy the rx-callsign (in brackets) will be extracted out of your sended message and added to the sendmessage-field.
//						 * Thats what happening in line with //here1
//						 * Your own sent texts will look like this:
//						 *
//						 * (>ON4KST) Hi team! Nice to meet you
//						 *
//						 */
//
//						if (selectedChatMemberPrivateChat.getList().get(0).getSender().getCallSign().equals(chatcontroller.getChatPreferences().getStn_loginCallSign()) ) {
//							//selected message of own callsign ... now filter the foreign callsign and fill it in after /cq
//							System.out.println("////////////////////////////// rx in orginal message: " + selectedChatMemberPrivateChat.getList().get(0).getReceiver().getCallSign());
//							System.out.println("privChat selected ChatMember: was own object...!" + "rx was: " + selectedChatMemberPrivateChat.getList().get(0).getMessageText().substring(2,(selectedChatMemberPrivateChat.getList().get(0).getMessageText().indexOf(")"))));
//
//							txt_chatMessageUserInput.clear();
//							txt_chatMessageUserInput.setText("/cq "
//									+ selectedChatMemberPrivateChat.getList().get(0).getMessageText().substring(2,(selectedChatMemberPrivateChat.getList().get(0).getMessageText().indexOf(")"))) + " "); //here1
//							txt_chatMessageUserInput.requestFocus();
//							txt_chatMessageUserInput.selectEnd();
//
//							//own messages end here
//
//						} else {
//
//
//							txt_chatMessageUserInput.clear();
//							txt_chatMessageUserInput.setText("/cq "
//									+ selectedChatMemberPrivateChat.getList().get(0).getSender().getCallSign() + " ");
//							txt_chatMessageUserInput.requestFocus();
//							txt_chatMessageUserInput.selectEnd();
//
//
//							focusChatMemberAndPrepareCq(selectedChatMemberPrivateChat.getList().get(0).getSender());
//
//
//							try {
//								selectedCallSignFurtherInfoPane.getChildren().clear();
//								selectedCallSignInfoStageChatMember = selectedChatMemberPrivateChat.getList().get(0).getSender();
//								chatcontroller.getScoreService().setSelectedChatMember(selectedCallSignInfoStageChatMember);
//
//								chatcontroller.getScoreService().setSelectedChatMember(selectedCallSignInfoStageChatMember); //important after selection change
//								selectedCallSignFurtherInfoPane.getChildren().add(generateFurtherInfoAbtSelectedCallsignBP(selectedCallSignInfoStageChatMember));
//								txt_chatMessageUserInput.requestFocus();
//								txt_chatMessageUserInput.selectEnd();
//							} catch (Exception exception) {
//								System.out.println("KST4CApp, <<<catched error>>>>: message sender is not in the userlist any more!");
//							}
//
//							System.out.println("privChat selected ChatMember: "
//									+ selectedChatMemberPrivateChat.getList().get(0).getSender());
//							// selectedChatMemberList.clear();
////						selectionModelChatMember.clearSelection(0);
//						}
//					}
//				}
//			});

			timer_updatePrivatemessageTable = new Timer();
			timer_updatePrivatemessageTable.scheduleAtFixedRate(new TimerTask() {

				public void run() {
					Thread.currentThread().setName("UpdatePrivateMessageTableTimer");
					Platform.runLater(() -> {

						privateMessageTable.refresh();

					});
				}
			}, new Date(), 5000);


			TableView<ChatMessage> tbl_generalMessageTable = new TableView<ChatMessage>();
			tbl_generalMessageTable = initChatGeneralMSGTable();

			tbl_generalMessageTable.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent t) {
					if (t.getButton() == MouseButton.SECONDARY) {
						chatMemberContextMenu.show(primaryStage, t.getScreenX(), t.getScreenY());
					}
				}
			});

			TableViewSelectionModel<ChatMessage> generalChatselectionModelChatMessage = tbl_generalMessageTable
					.getSelectionModel();
			generalChatselectionModelChatMessage.setSelectionMode(SelectionMode.SINGLE);

			generalChatselectionModelChatMessage.selectedItemProperty().addListener(
					(observableValue, oldSelectedMessage, newSelectedMessage) -> {
						if (newSelectedMessage == null) {
							return;
						}

						prepareCqTextForSelectedChatMember(newSelectedMessage.getSender(), false);

						System.out.println("cq chat selected ChatMember: " + newSelectedMessage.getSender());

						try {
							focusChatMemberAndPrepareCq(newSelectedMessage.getSender(), false);
						} catch (Exception exception) {
							System.out.println("KST4CApp, <<<catched error>>>>: message sender is not in the userlist any more!");
						}

						try {
							selectedCallSignFurtherInfoPane.getChildren().clear();
							selectedCallSignInfoStageChatMember = newSelectedMessage.getSender();
							chatcontroller.getScoreService().setSelectedChatMember(selectedCallSignInfoStageChatMember);
							selectedCallSignFurtherInfoPane.getChildren()
									.add(generateFurtherInfoAbtSelectedCallsignBP(selectedCallSignInfoStageChatMember));
							txt_chatMessageUserInput.requestFocus();
							txt_chatMessageUserInput.selectEnd();
						} catch (Exception exception) {
							System.out.println("KST4CApp, <<<catched error>>>>: message sender is not in the userlist any more!");
						}
					}
			);

//			ObservableList<ChatMessage> selectedChatMessageListGeneralChat = generalChatselectionModelChatMessage
//					.getSelectedItems();
//			selectedChatMessageListGeneralChat.addListener(new ListChangeListener<ChatMessage>() {
//				@Override
//				public void onChanged(Change<? extends ChatMessage> selectedChatMemberGeneralChat) {
//					if (generalChatselectionModelChatMessage.getSelectedItems().isEmpty()) {
//						// do nothing, that was a deselection-event!
//					} else {
//
//						txt_chatMessageUserInput.clear();
//						txt_chatMessageUserInput.setText("/cq "
//								+ selectedChatMemberGeneralChat.getList().get(0).getSender().getCallSign() + " ");
//						txt_chatMessageUserInput.requestFocus();
//						txt_chatMessageUserInput.selectEnd();
//						System.out.println("cq chat selected ChatMember: "
//								+ selectedChatMemberGeneralChat.getList().get(0).getSender());
//
//
//						try {
//							//scroll the chatmembers table to the entry - try because of sender could be null
//							focusChatMemberAndPrepareCq(selectedChatMemberGeneralChat.getList().get(0).getSender());
//
//						} catch (Exception exception) {
//							System.out.println("KST4CApp, <<<catched error>>>>: message sender is not in the userlist any more!");
//						}
//
//						try {
//							selectedCallSignFurtherInfoPane.getChildren().clear();
//							selectedCallSignInfoStageChatMember = selectedChatMemberGeneralChat.getList().get(0).getSender();
//							chatcontroller.getScoreService().setSelectedChatMember(selectedCallSignInfoStageChatMember);
//
//							chatcontroller.getScoreService().setSelectedChatMember(selectedCallSignInfoStageChatMember); //important after selection change
//							selectedCallSignFurtherInfoPane.getChildren().add(generateFurtherInfoAbtSelectedCallsignBP(selectedCallSignInfoStageChatMember));
//							txt_chatMessageUserInput.requestFocus();
//							txt_chatMessageUserInput.selectEnd();
//						} catch (Exception exception) {
//							System.out.println("KST4CApp, <<<catched error>>>>: message sender is not in the userlist any more!");
//						}
//						// selectedChatMemberList.clear();
////						selectionModelChatMember.clearSelection(0);
//					}
//				}
//			});



//			messageSectionSplitpane.getItems().addAll(privateMessageTable, flwPane_textSnippets,pnl_inputAndSendButtons, textInputFlowPane,
//					tbl_generalMessageTable);

			TabPane bottomGlobalMessageTabPane = initBottomGlobalMessageTabPane(tbl_generalMessageTable);

			messageSectionSplitpane.getItems().addAll(
					privateMessageTable,
					flwPane_textSnippets,
					pnl_inputAndSendButtons,
					textInputFlowPane,
					bottomGlobalMessageTabPane
			);

			messageSectionSplitpane.setDividerPositions(chatcontroller.getChatPreferences().getGUImessageSectionSplitpane_dividerposition());

			//first initialize how much divider positions we need...
//			chatcontroller.getChatPreferences().setGUImessageSectionSplitpane_dividerposition(chatcontroller.getChatPreferences().getGUImessageSectionSplitpane_dividerposition());
			/**
			 * Then add change listeners to the dividers to save their state
			 */
			for (SplitPane.Divider divider : messageSectionSplitpane.getDividers()) {
				divider.positionProperty().addListener(new ChangeListener<Number>() {
					@Override
					public void changed(ObservableValue<? extends Number> observableValue, Number oldDividerPos, Number newDividerPosition) {
						System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<< devider>>>>>> " + messageSectionSplitpane.getDividers().indexOf(divider)  + " position change, new position: " + newDividerPosition + " // size dev: " +  messageSectionSplitpane.getDividers().size());
						chatcontroller.getChatPreferences().getGUImessageSectionSplitpane_dividerposition()[messageSectionSplitpane.getDividers().indexOf(divider)] = newDividerPosition.doubleValue();
						requestLayoutSave();
					}
				});

			}

			//Changed to add contextmenu to cq message table
//			messageSectionSplitpane.getItems().addAll(privateMessageTable, flwPane_textSnippets, textInputFlowPane,
//					initChatGeneralMSGTable());

			bPaneChatWindow.setCenter(mainWindowLeftSplitPane);


			tbl_chatMember = initChatMemberTable();

			/*
			 * Detect real operator interaction with the ChatMember table.
			 *
			 * The selection listener itself cannot reliably know whether a selection change
			 * came from the user or from a periodic table refresh. Mouse and keyboard events
			 * happen before the selection model changes, so they are used to mark the next
			 * selection event as intentional.
			 */
			tbl_chatMember.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
				markOperatorChatMemberSelectionIntent();
			});

			tbl_chatMember.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
				switch (event.getCode()) {
					case UP:
					case DOWN:
					case PAGE_UP:
					case PAGE_DOWN:
					case HOME:
					case END:
					case ENTER:
					case SPACE:
						markOperatorChatMemberSelectionIntent();
						break;
					default:
						break;
				}
			});

			timelineView.setOnCandidateClicked(ev -> {
				if (ev == null) return;

				ChatMember resolved = resolveChatMemberForCallRawAndCategory(ev.getCallSignRaw(), ev.getPreferredChatCategory());
				if (resolved == null) return;

				// Always prepare /cq + FurtherInfo (even if filtered out in table)
				focusChatMemberAndPrepareCq(resolved);
			});


			TableViewSelectionModel<ChatMember> selectionModelChatMember = tbl_chatMember.getSelectionModel();
			selectionModelChatMember.setSelectionMode(SelectionMode.SINGLE);

			tbl_chatMember.autosize();

//			tbl_chatMember.getda

			selectionModelChatMember.selectedItemProperty().addListener(
					(observableValue, oldSelectedMember, newSelectedMember) -> {
						if (newSelectedMember == null || programmaticChatMemberSelectionChange) {
							return;
						}

						/*
						 * If the operator really selected a ChatMember by mouse or keyboard, keep
						 * the old KST4Contest behavior: always prepare "/cq CALL ", even if the
						 * send field already contains text.
						 *
						 * If this selection event came from a periodic refresh, filter update,
						 * score update or table rebuild, do not force overwriting. In that case
						 * prepareCqTextForSelectedChatMember() may only update an empty or still
						 * automatically prepared field.
						 */
						boolean forcePrepareCqBecauseOperatorSelected =
								operatorInitiatedChatMemberSelectionChange;

						try {
							handleChatMemberSelectionChanged(
									newSelectedMember,
									forcePrepareCqBecauseOperatorSelected
							);
						} finally {
							operatorInitiatedChatMemberSelectionChange = false;
						}
					}
			);

//			ObservableList<ChatMember> selectedChatMemberList = selectionModelChatMember.getSelectedItems();
//			selectedChatMemberList.addListener(new ListChangeListener<ChatMember>() {
//				@Override
//				public void onChanged(Change<? extends ChatMember> selectedChatMember) {
//					try{
//
//						if (selectionModelChatMember.getSelectedItems().isEmpty()) {
//							// do nothing, that was a deselection-event!
//						} else {
//
//
//
//							selectedCallSignInfoStageChatMember = selectionModelChatMember.getSelectedItems().get(0); //TODO: temp test 1.26: get selected chatmember out of ist
//							chatcontroller.getScoreService().setSelectedChatMember(selectedCallSignInfoStageChatMember); //important after selection cchange
//
////							selectedCallSignInfoStageChatMember = chatcontroller.getLst_chatMemberList()
////									.get(chatcontroller.checkListForChatMemberIndexByCallSign(
////											selectedChatMember.getList().get(0)));
//
//							try {
//								selectedCallSignFurtherInfoPane.getChildren().clear();
//							} catch (Exception exception) {
//								System.out.println("KST4CApp: ERROR: " + exception.getMessage() );
//							}
//
//							try {
//
//								selectedCallSignFurtherInfoPane.getChildren().add(generateFurtherInfoAbtSelectedCallsignBP(selectedCallSignInfoStageChatMember));
//							} catch (Exception exception) {
//							System.out.println("KST4CApp: ERROR, selected member disappeared: " + exception.getStackTrace() );
//							exception.printStackTrace();
//						}
//
//							txt_chatMessageUserInput.clear();
//							txt_chatMessageUserInput
//									.setText("/cq " + selectedChatMember.getList().get(0).getCallSign() + " ");
//							txt_chatMessageUserInput.requestFocus();
//							txt_chatMessageUserInput.selectEnd();
////							System.out.println(
////									"##################selected ChatMember: " + selectedChatMember.getList().get(0));
//							// selectedChatMemberList.clear();
//	//						selectionModelChatMember.clearSelection(0);
//						}
//					} catch (Exception exception) {
//						exception.printStackTrace();
//						selectedCallSignFurtherInfoPane.getChildren().clear();
//						txt_chatMessageUserInput.clear();
//						System.out.println("KST4ContestApp <<<catched ERROR>>>, selected user left chat!");
//					}
//				}
//			});

			// TODO: Take together contextmenu and macromenu, generate together

			// Creates the Contextmenu for right clicks to the chatmember-list
			// TODO: If the old selection is identical with the new selection, /CQ station
			// will not be written by the contextmenu clicklistener. Have to improve that
			// some time
//			ContextMenu chatMemberContextMenu = initChatMemberTableContextMenu(
//					this.chatcontroller.getChatPreferences().getTextSnippets()); old mechanic

			chatMemberContextMenu = initChatMemberTableContextMenu(
					mirrorOf(this.chatcontroller.getChatPreferences().getLst_txtSnipList()));

			tbl_chatMember.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent t) {
					if (t.getButton() == MouseButton.SECONDARY) {
						chatMemberContextMenu.show(primaryStage, t.getScreenX(), t.getScreenY());

					}
				}
			});

			SplitPane mainWindowRightSplitPane = new SplitPane();
			mainWindowRightSplitPane.setOrientation(Orientation.VERTICAL);
//			mainWindowRightSplitPane.setDividerPositions(chatcontroller.getChatPreferences().getGUImainWindowRightSplitPane_dividerposition());

			BorderPane chatMemberTableBorderPane = new BorderPane();
			chatMemberTableBorderPane.setCenter(tbl_chatMember);

			chatMemberTableBorderPane.setMinWidth(0);
			tbl_chatMember.setMinWidth(0);

			chatMemberTableFilterQTFAndQRBHbox = new HBox(10);
			chatMemberTableFilterQTFAndQRBHbox.setAlignment(Pos.CENTER_LEFT);
			chatMemberTableFilterQTFAndQRBHbox.setMinWidth(0);

//			chatMemberTableFilterQTFAndQRBHbox.set

			VBox chatMemberTableFilterVBoxForAllFilters= new VBox();

			Button btnResetChatMemberFilters = new Button("Reset filters");
			btnResetChatMemberFilters.getStyleClass().clear();
			btnResetChatMemberFilters.getStyleClass().addAll(
					"button",
					"buttonMyQrg1"
			);
			btnResetChatMemberFilters.setTooltip(
					new Tooltip("Disable all station-list filters and show every user")
			);

			chatMemberTableFilterVBoxForAllFilters.setSpacing(1);

			chatMemberTableFilterVBoxForAllFilters.setMinWidth(0);

			chatMemberTableFilterVBoxForAllFilters.setStyle("-fx-padding: 1;" +
					"-fx-border-style: solid inside;" +
					"-fx-border-width: 1;" +
					"-fx-border-insets: 1;" +
					"-fx-border-radius: 1;" +
					"-fx-border-color: lightgreen;");


			HBox chatMemberTableFilterQRBHBox = new HBox(2);
			chatMemberTableFilterQRBHBox.setAlignment(Pos.CENTER_LEFT);

			TextField chatMemberTableFilterMaxQrbTF = new TextField(chatcontroller.getChatPreferences().getStn_maxQRBDefault() + "");
			chatMemberTableFilterMaxQrbTF.setFocusTraversable(false);


			ToggleButton btnTglNewLocator = new ToggleButton("Only new grids");
			Predicate<ChatMember> newLocatorPredicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {
					return chatcontroller.isNewGridSquare(chatMember);
				}
			};

			btnTglNewLocator.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglNewLocator.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(newLocatorPredicate);
					} else {
						chatcontroller.removeChatMemberListFilterPredicate(newLocatorPredicate);
					}
				}
			});
			btnTglNewLocator.setTooltip(new Tooltip("Show only stations whose 4-character grid square has not been worked on any band yet"));


			ToggleButton btnTglGridColor = new ToggleButton("Grid color");

			/**
			 * Enables optional QRA-cell coloring for grid status.
			 *
			 * <p>This does not filter the list. It only colors the locator field:
			 * worked grid = darker, new grid = brighter.</p>
			 */
			btnTglGridColor.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					gridSquareHighlightEnabled = btnTglGridColor.isSelected();
					tbl_chatMember.refresh();
				}
			});

			btnTglGridColor.setTooltip(new Tooltip("Color the QRA field by grid status without filtering the station list"));

			ToggleButton btnTglReachableTropo = new ToggleButton("Tropo >=0dB");
			Predicate<ChatMember> reachableTropoPredicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {
					return isReachableViaTropoFilterMatch(chatMember);
				}
			};
			btnTglReachableTropo.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglReachableTropo.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(reachableTropoPredicate);
					} else {
						chatcontroller.removeChatMemberListFilterPredicate(reachableTropoPredicate);
					}
				}
			});
			btnTglReachableTropo.setTooltip(new Tooltip("Show stations with non-negative SSB tropo margin. Pending/failed calculations stay visible."));

			ToggleButton btnTglNewBands = new ToggleButton("New bands");
			Predicate<ChatMember> newBandsPredicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {
					return isNewBandOpportunity(chatMember);
				}
			};
			btnTglNewBands.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglNewBands.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(newBandsPredicate);
					} else {
						chatcontroller.removeChatMemberListFilterPredicate(newBandsPredicate);
					}
				}
			});
			btnTglNewBands.setTooltip(new Tooltip(
					"Show stations offering at least one enabled, unworked band. "
							+ "Recent QRG detections and station names are evaluated; "
							+ "NOT-QRV tags override them."
			));

			ToggleButton btnTglAsNext5Min = new ToggleButton("AS next 5m");
			Predicate<ChatMember> asNext5MinPredicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {
					return hasAsWindowInNextMinutes(chatMember, 5);
				}
			};
			btnTglAsNext5Min.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglAsNext5Min.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(asNext5MinPredicate);
					} else {
						chatcontroller.removeChatMemberListFilterPredicate(asNext5MinPredicate);
					}
				}
			});
			btnTglAsNext5Min.setTooltip(new Tooltip("Show stations with any AirScout window now or within the next 5 minutes"));

			ToggleButton tglBtnQRBEnable = new ToggleButton("Show only QRB [km] <= ");
			tglBtnQRBEnable.selectedProperty().addListener(new ChangeListener<Boolean>() {
				Predicate<ChatMember> maxQrbPredicate = new Predicate<ChatMember>() {
					@Override
					public boolean test(ChatMember chatMember) {
//						if (chatMember.getQrb() < Double.parseDouble(chatMemberTableFilterMaxQrbTF.getText())) {
//							return true;
//						} else return false;

						try {
							String maxQrbText = chatMemberTableFilterMaxQrbTF.getText();
							if (chatMember == null || maxQrbText == null || maxQrbText.isBlank()) {
								return true;
							}
							return chatMember.getQrb() <= Double.parseDouble(maxQrbText);
						} catch (Exception exception) {
							return true;
						}
					}
				};
				@Override
				public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
					if (tglBtnQRBEnable.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(maxQrbPredicate);
					} else chatcontroller.removeChatMemberListFilterPredicate(maxQrbPredicate);
				}
			});

			chatMemberTableFilterQRBHBox.getChildren().add(tglBtnQRBEnable);
//			chatMemberTableFilterMaxQrbTF.textProperty().addListener(new ChangeListener<String>() {
//				@Override
//				public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
//					if (!newValue.matches("\\d*")) {
//						chatMemberTableFilterMaxQrbTF.setText(newValue.replaceAll("[^\\d]", ""));
//					}
//				}
//			});

			chatMemberTableFilterMaxQrbTF.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
					String safeValue = newValue == null ? "" : newValue.replaceAll("[^\\d]", "");
					if (newValue != null && !newValue.equals(safeValue)) {
						chatMemberTableFilterMaxQrbTF.setText(safeValue);
						return;
					}

					if (tglBtnQRBEnable.isSelected()) {
						lastAppliedChatMemberFilterPredicate = null;
						applyChatMemberFilterPredicates();
					}
				}
			});

			chatMemberTableFilterMaxQrbTF.setPrefSize(50,0);

			chatMemberTableFilterQRBHBox.getChildren().add(chatMemberTableFilterMaxQrbTF);
			chatMemberTableFilterQRBHBox.setStyle("-fx-padding: 1;" +
					"-fx-border-style: solid inside;" +
					"-fx-border-width: 1;" +
					"-fx-border-insets: 1;" +
					"-fx-border-radius: 1;" +
					"-fx-border-color: lightgrey;");

			chatMemberTableFilterQTFAndQRBHbox.getChildren().addAll(
					btnResetChatMemberFilters,
					chatMemberTableFilterQRBHBox
			);


			FlowPane chatMemberTableFilterQTFHBox = new FlowPane(
					Orientation.HORIZONTAL,
					2,
					2
			);
			chatMemberTableFilterQTFHBox.setAlignment(Pos.CENTER_LEFT);
			chatMemberTableFilterQTFHBox.setRowValignment(VPos.CENTER);
			chatMemberTableFilterQTFHBox.setMinWidth(0);

			HBox.setHgrow(
					chatMemberTableFilterQTFHBox,
					Priority.ALWAYS
			);


			CheckBox chatMemberTableFilterQtfEnableChkbx = new CheckBox("Show only QTF:");
			TextField chatMemberTableFilterQtfTF = new TextField(chatcontroller.getChatPreferences().getStn_qtfDefault()+"");
			chatMemberTableFilterQtfTF.setFocusTraversable(false);
//			chatMemberTableFilterQtfTF.textProperty().addListener(new ChangeListener<String>() {
//				@Override
//				public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
//					if (newValue.equals("")) {
//						chatMemberTableFilterQtfTF.setText("0");
//					}
//					if (!newValue.matches("\\d*")) {
//						chatMemberTableFilterQtfTF.setText(newValue.replaceAll("[^\\d]", ""));
//					}
//					System.out.println("new default QTF: " + newValue);
//					chatMemberTableFilterQtfEnableChkbx.setSelected(false);
//					chatMemberTableFilterQtfEnableChkbx.setSelected(true);
//				}
//			});

			chatMemberTableFilterQtfTF.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
					String safeValue = newValue == null || newValue.isBlank()
							? "0"
							: newValue.replaceAll("[^\\d]", "");

					if (newValue == null || !newValue.equals(safeValue)) {
						chatMemberTableFilterQtfTF.setText(safeValue);
						return;
					}

					System.out.println("new default QTF: " + safeValue);

					if (chatMemberTableFilterQtfEnableChkbx.isSelected()) {
						lastAppliedChatMemberFilterPredicate = null;
						applyChatMemberFilterPredicates();
					}
				}
			});

			chatMemberTableFilterQtfEnableChkbx.selectedProperty().addListener(new ChangeListener<Boolean>() {

				Predicate<ChatMember> qtfCheckPredicate = new Predicate<ChatMember>() {
					@Override
					public boolean test(ChatMember chatMember) {

//						System.out.println(chatMemberTableFilterQtfTF.getText() + " stn have " + chatMember.getQTFdirection());

						return DirectionUtils.isAngleInRange(chatMember.getQTFdirection(),Double.parseDouble(chatMemberTableFilterQtfTF.getText()), chatcontroller.getChatPreferences().getStn_antennaBeamWidthDeg());

					}
				};
				@Override
				public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
					if (chatMemberTableFilterQtfEnableChkbx.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(qtfCheckPredicate);
					} else {
						chatcontroller.removeChatMemberListFilterPredicate(qtfCheckPredicate);
//						uiHelper_recolorQtfDirectionButtonsExceptThisOne(new Button("justADummy"));
					}
				}
			});
			chatMemberTableFilterQTFHBox.getChildren().add(chatMemberTableFilterQtfEnableChkbx);




			chatMemberTableFilterQtfTF.setPrefSize(50,0);
//			chatMemberTableFilterQTFHBox.getChildren().add(chatMemberTableFilterQtfTF);
			chatMemberTableFilterQTFHBox.setStyle("-fx-padding: 1;" +
					"-fx-border-style: solid inside;" +
					"-fx-border-width: 1;" +
					"-fx-border-insets: 1;" +
					"-fx-border-radius: 1;" +
					"-fx-border-color: lightgrey;");



            ToggleGroup tglGrpQTF = new ToggleGroup(); //Tooglegroup for the qtf filter options

            tglGrpQTF.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
                @Override
                public void changed(ObservableValue<? extends Toggle> observableValue, Toggle toggle, Toggle t1) {
                    if (t1 == null) {
                        chatMemberTableFilterQtfEnableChkbx.setSelected(false);
                    } else {
                        chatMemberTableFilterQtfEnableChkbx.setSelected(true);
                    }
                }
            });

//			Button qtfNorth = new Button("N");
            ToggleButton qtfNorth = new ToggleButton("N");
            qtfNorth.setToggleGroup(tglGrpQTF);
			btnQtfButtonsAvl[0] = qtfNorth;

			qtfNorth.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					chatMemberTableFilterQtfTF.textProperty().set("0");
//					uiHelper_recolorQtfDirectionButtonsExceptThisOne(qtfNorth);
				}

			});

            ToggleButton qtfNorthEast = new ToggleButton("NE");
            qtfNorthEast.setToggleGroup(tglGrpQTF);
			btnQtfButtonsAvl[1] = qtfNorthEast;
			qtfNorthEast.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					chatMemberTableFilterQtfTF.textProperty().set("45");
//					uiHelper_recolorQtfDirectionButtonsExceptThisOne(qtfNorthEast);
				}
			});

            ToggleButton qtfEast = new ToggleButton("E");
            qtfEast.setToggleGroup(tglGrpQTF);
			btnQtfButtonsAvl[2] = qtfEast;
			qtfEast.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					chatMemberTableFilterQtfTF.textProperty().set("90");
//					uiHelper_recolorQtfDirectionButtonsExceptThisOne(qtfEast);
				}
			});

            ToggleButton qtfSouthEast = new ToggleButton("SE");
            qtfSouthEast.setToggleGroup(tglGrpQTF);
			btnQtfButtonsAvl[3] = qtfSouthEast;
			qtfSouthEast.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					chatMemberTableFilterQtfTF.textProperty().set("135");
//					uiHelper_recolorQtfDirectionButtonsExceptThisOne(qtfSouthEast);
				}
			});

            ToggleButton qtfSouth = new ToggleButton("S");
            qtfSouth.setToggleGroup(tglGrpQTF);
			btnQtfButtonsAvl[4] = qtfSouth;
			qtfSouth.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					chatMemberTableFilterQtfTF.textProperty().set("180");
//					uiHelper_recolorQtfDirectionButtonsExceptThisOne(qtfSouth);
				}
			});

            ToggleButton qtfSouthWest = new ToggleButton("SW");
            qtfSouthWest.setToggleGroup(tglGrpQTF);
			btnQtfButtonsAvl[5] = qtfSouthWest;
			qtfSouthWest.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					chatMemberTableFilterQtfTF.textProperty().set("225");
//					uiHelper_recolorQtfDirectionButtonsExceptThisOne(qtfSouthWest);
				}
			});
            ToggleButton qtfWest = new ToggleButton("W");
            qtfWest.setToggleGroup(tglGrpQTF);
			btnQtfButtonsAvl[6] = qtfWest;
			qtfWest.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					chatMemberTableFilterQtfTF.textProperty().set("270");
//					uiHelper_recolorQtfDirectionButtonsExceptThisOne(qtfWest);
				}

			});
            ToggleButton qtfNorthWest = new ToggleButton("NW");
            qtfNorthWest.setToggleGroup(tglGrpQTF);
			btnQtfButtonsAvl[7] = qtfNorthWest;
            qtfNorthWest.setToggleGroup(tglGrpQTF);
			qtfNorthWest.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					chatMemberTableFilterQtfTF.textProperty().set("315");
//					uiHelper_recolorQtfDirectionButtonsExceptThisOne(qtfNorthWest);
				}
			});


//			chatMemberTableFilterQTFHBox.setSpacing(5);
			Label lblQtfHalfBeamwidth = new Label("± BW/2");
			lblQtfHalfBeamwidth.setTooltip(new Tooltip(
					"The QTF filter uses half of the configured total antenna beamwidth "
							+ "on each side of the selected direction."));

			chatMemberTableFilterQTFHBox.getChildren().addAll(
					chatMemberTableFilterQtfTF,
					lblQtfHalfBeamwidth,
					qtfNorth,
					qtfNorthEast,
					qtfEast,
					qtfSouthEast,
					qtfSouth,
					qtfSouthWest,
					qtfWest,
					qtfNorthWest);

			chatMemberTableFilterQTFAndQRBHbox.getChildren().add(chatMemberTableFilterQTFHBox);

			chatMemberTableFilterVBoxForAllFilters.getChildren().add(chatMemberTableFilterQTFAndQRBHbox);

			HBox chatMemberTableFilterTextFieldBox = new HBox();
			chatMemberTableFilterTextFieldBox.setAlignment(Pos.CENTER_LEFT);
			chatMemberTableFilterTextFieldBox.setStyle("-fx-padding: 1;" +
					"-fx-border-style: solid inside;" +
					"-fx-border-width: 1;" +
					"-fx-border-insets: 1;" +
					"-fx-border-radius: 1;" +
					"-fx-border-color: lightgrey;");


//			chatcontroller.getLst_chatMemberListFiltered().predicateProperty().bind(Bindings.createObjectBinding(() -> chatcontroller.getLst_chatMemberListFilterPredicates().stream().reduce(x -> true, Predicate::and), chatcontroller.getLst_chatMemberListFilterPredicates()));

			applyChatMemberFilterPredicates();

			chatcontroller.getLst_chatMemberListFilterPredicates().addListener(
					predicates -> applyChatMemberFilterPredicates()
			);

			TextField chatMemberTableFilterTextField = new TextField();
			chatMemberTableFilterTextField.setPromptText("Find...");

			chatMemberTableFilterTextField.setFocusTraversable(false);

			chatMemberTableFilterTextField.textProperty().addListener(new ChangeListener<String>() {

				Predicate<ChatMember> searchTextPredicate = new Predicate<ChatMember>() {
					@Override
					public boolean test(ChatMember chatMember) {
						String filterText = chatMemberTableFilterTextField.getText();
						if (filterText == null || filterText.isBlank()) {
							return true;
						}

						String callSign = chatMember == null || chatMember.getCallSign() == null
								? ""
								: chatMember.getCallSign();

						return callSign.toUpperCase(Locale.ROOT).contains(filterText.trim().toUpperCase(Locale.ROOT));
					}
				};

				@Override
				public void changed(ObservableValue<? extends String> observableValue, String oldText, String newText) {
					boolean filterActive = newText != null && !newText.isBlank();
					boolean predicatePresent = chatcontroller.getLst_chatMemberListFilterPredicates().snapshot().contains(searchTextPredicate);

					if (filterActive && !predicatePresent) {
						chatcontroller.addChatMemberListFilterPredicate(searchTextPredicate);
					} else if (!filterActive && predicatePresent) {
						chatcontroller.removeChatMemberListFilterPredicate(searchTextPredicate);
					} else {
						/*
						 * The predicate object captures the TextField. When the text changes from
						 * e.g. "D" to "DL", the predicate list itself does not change, so the
						 * filter must be re-applied explicitly.
						 */
						lastAppliedChatMemberFilterPredicate = null;
						applyChatMemberFilterPredicates();
					}
				}
			});


			FlowPane chatMemberTableFilterWorkedBandFiltersHbx = new FlowPane(
					Orientation.HORIZONTAL,
					2,
					3
			);

			HBox chatMemberTableReachabilityBox = new HBox(4);
			chatMemberTableReachabilityBox.setAlignment(Pos.CENTER_LEFT);
			chatMemberTableReachabilityBox.setMinWidth(0);

			chatMemberTableReachabilityBox.setStyle(
					"-fx-padding: 1;" +
							"-fx-border-style: solid inside;" +
							"-fx-border-width: 1;" +
							"-fx-border-insets: 1;" +
							"-fx-border-radius: 1;" +
							"-fx-border-color: lightgrey;"
			);

			chatMemberTableFilterWorkedBandFiltersHbx.setAlignment(Pos.CENTER_LEFT);
			chatMemberTableFilterWorkedBandFiltersHbx.setRowValignment(VPos.CENTER);
			chatMemberTableFilterWorkedBandFiltersHbx.setMinWidth(0);


			Button btnCalculateSelectedTropo = new Button("Calc selected");

			/**
			 * Calculates full terrain/path reachability only for the currently selected
			 * station. This is intentionally operator-triggered to avoid API-limit problems.
			 */
			btnCalculateSelectedTropo.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					ChatMember selectedMember = tbl_chatMember.getSelectionModel().getSelectedItem();

					if (selectedMember == null && chatcontroller.getScoreService() != null) {
						selectedMember = chatcontroller.getScoreService().getSelectedChatMember();
					}

					if (selectedMember == null) {
						return;
					}

					Band selectedBand = resolveReachabilityBandForUi(selectedMember);
					chatcontroller.getReachabilityService().calculateSelectedStationOnDemand(selectedMember, selectedBand);

					/*
					 * If the map is already initialized, make it request the same operator-selected
					 * band. ReachabilityService deduplicates the identical calculation key, so this
					 * attaches the map callback without causing a second terrain API request.
					 */

				}
			});

			btnCalculateSelectedTropo.setTooltip(new Tooltip("Calculate full Tropo/path analysis for the selected station only"));


			ComboBox<String> cmbReachabilityBand = new ComboBox<>();
			cmbReachabilityBand.getItems().add("Auto");
			for (Band band : chatcontroller.getReachabilityService().getEnabledStationBands()) {
				cmbReachabilityBand.getItems().add(band.getDisplayLabel());
			}
			cmbReachabilityBand.getSelectionModel().select("Auto");
			cmbReachabilityBand.setTooltip(new Tooltip(
					"Reachability band for Tropo column/filter. Auto uses the current "
							+ "QRG, station-name hints and the supported chat category."
			));
			cmbReachabilityBand.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					selectedReachabilityBandOverride = parseReachabilityBandSelection(cmbReachabilityBand.getValue());

					// Changing the display band must not start batch terrain analysis.
					// Existing cached values are shown; new values are calculated on map click
					// or via the explicit "Calc selected" button.
					chatcontroller.fireUserListUpdate("Reachability band changed");
				}
			});

			chatMemberTableReachabilityBox.getChildren().addAll(
					new Label("Reachability:"),
					cmbReachabilityBand,
					btnCalculateSelectedTropo
			);

			/**
			 * In order to work the filters needs the proper band settings, which should be worked
			 */
			if (chatcontroller.getReachabilityService().getEnabledStationBands().isEmpty()) {
				Label bandSetupWarning = new Label("Please enable at least one active station band for New Locator/New Band filters.");
				bandSetupWarning.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
				bandSetupWarning.setWrapText(true);
				bandSetupWarning.setMinWidth(0);
				bandSetupWarning.setMaxWidth(320);
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(bandSetupWarning);
			}

			ToggleButton btnTglwkd = new ToggleButton("wkd");

			Predicate<ChatMember> wkdPredicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(wkdPredicate);
					} else {
						chatcontroller.removeChatMemberListFilterPredicate(wkdPredicate);
					}
				}
			});

			ToggleButton btnTglwkd50 = new ToggleButton("50");

			Predicate<ChatMember> wkd50Predicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked50() || !chatMember.isQrv50()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd50.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd50.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(wkd50Predicate);
					} else {
						chatcontroller.removeChatMemberListFilterPredicate(wkd50Predicate);
					}
				}
			});

			ToggleButton btnTglwkd70 = new ToggleButton("70");

			Predicate<ChatMember> wkd70Predicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked70() || !chatMember.isQrv70()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd70.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd70.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(wkd70Predicate);
					} else {
						chatcontroller.removeChatMemberListFilterPredicate(wkd70Predicate);
					}
				}
			});

			ToggleButton btnTglwkd144 = new ToggleButton("144");

			Predicate<ChatMember> wkd144Predicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked144() || !chatMember.isQrv144()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd144.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd144.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(wkd144Predicate);
					} else {
						chatcontroller.removeChatMemberListFilterPredicate(wkd144Predicate);
					}
				}
			});
//			btnTglwkd144.setVisible(chatcontroller.getChatPreferences().isStn_bandActive144());

			ToggleButton btnTglwkd432 = new ToggleButton("432");

			Predicate<ChatMember> wkd432Predicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked432() || !chatMember.isQrv432()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd432.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd432.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(wkd432Predicate);
					} else {
						chatcontroller.removeChatMemberListFilterPredicate(wkd432Predicate);
					}
				}
			});
//			btnTglwkd432.setVisible(chatcontroller.getChatPreferences().isStn_bandActive432());


			ToggleButton btnTglwkd23 = new ToggleButton("23");

			Predicate<ChatMember> wkd23Predicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked1240() || !chatMember.isQrv1240()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd23.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd23.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(wkd23Predicate);
					} else {
						chatcontroller.removeChatMemberListFilterPredicate(wkd23Predicate);
					}
				}
			});

			ToggleButton btnTglwkd13 = new ToggleButton("13");

			Predicate<ChatMember> wkd13Predicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked2300() || !chatMember.isQrv2300()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd13.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd13.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(wkd13Predicate);
					} else {
						chatcontroller.removeChatMemberListFilterPredicate(wkd13Predicate);
					}
				}
			});

			ToggleButton btnTglwkd9 = new ToggleButton("9");

			Predicate<ChatMember> wkd9Predicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked3400() || !chatMember.isQrv3400()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd9.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd9.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(wkd9Predicate);
					} else {
						chatcontroller.removeChatMemberListFilterPredicate(wkd9Predicate);
					}
				}
			});


			ToggleButton btnTglwkd6 = new ToggleButton("6");

			Predicate<ChatMember> wkd6Predicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked5600() || !chatMember.isQrv5600()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd6.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd6.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(wkd6Predicate);
					} else {
						chatcontroller.removeChatMemberListFilterPredicate(wkd6Predicate);
					}
				}
			});


			ToggleButton btnTglwkd3 = new ToggleButton("3");

			Predicate<ChatMember> wkd3Predicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked10G() || !chatMember.isQrv10G()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd3.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd3.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(wkd3Predicate);
					} else {
						chatcontroller.removeChatMemberListFilterPredicate(wkd3Predicate);
					}
				}
			});

			ToggleButton btnTglInactive = new ToggleButton("Inactive stations");

			Predicate<ChatMember> inactivePredicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {
					long inactiveMinutes =
							Utils4KST.time_getSecondsBetweenEpochAndNow(
									chatMember.getActivityTimeLastInEpoch() + ""
							) / 60L;

					return inactiveMinutes <= 20L;
				}
			};
			btnTglInactive.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglInactive.isSelected()) {
						chatcontroller.addChatMemberListFilterPredicate(inactivePredicate);
					} else {
						chatcontroller.removeChatMemberListFilterPredicate(inactivePredicate);
					}
				}
			});

			btnTglInactive.setTooltip(new Tooltip("Hide inactive stations"));

			chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(new Label("Hide worked:\nHide un-QRV: "));
			chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd);

			/**
			 * add only filter buttons at the callsigntable which affects used bands
			 */
			if (chatcontroller.getChatPreferences().isStn_bandActive50()) {
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd50);
			}
			if (chatcontroller.getChatPreferences().isStn_bandActive70()) {
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd70);
			}
			if (chatcontroller.getChatPreferences().isStn_bandActive144()) {
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd144);
			}
			if (chatcontroller.getChatPreferences().isStn_bandActive432()) {
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd432);
			}

			if (chatcontroller.getChatPreferences().isStn_bandActive1240()) {
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd23);
			}
			if (chatcontroller.getChatPreferences().isStn_bandActive2300()) {
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd13);
			}
			if (chatcontroller.getChatPreferences().isStn_bandActive3400()) {
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd9);
			}
			if (chatcontroller.getChatPreferences().isStn_bandActive5600()) {
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd6);

			}
			if (chatcontroller.getChatPreferences().isStn_bandActive10G()) {
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd3);

			}

			chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglInactive);
//			chatMemberTableFilterWorkedBandFiltersHbx.setAlignment(Pos.CENTER_LEFT);
//			chatMemberTableFilterWorkedBandFiltersHbx.setSpacing(5);
			chatMemberTableFilterWorkedBandFiltersHbx.setStyle("-fx-padding: 1;" +
					"-fx-border-style: solid inside;" +
					"-fx-border-width: 1;" +
					"-fx-border-insets: 1;" +
					"-fx-border-radius: 1;" +
					"-fx-border-color: lightgrey;");


			chatMemberTableFilterWorkedBandFiltersHbx.getChildren().addAll(
					btnTglNewLocator,
					btnTglGridColor,
					btnTglReachableTropo,
					btnTglNewBands,
					btnTglAsNext5Min
			);

			btnResetChatMemberFilters.setOnAction(event -> {

				/*
				 * Reset the visible state of every station-list filter.
				 *
				 * Grid coloring and the selected reachability band are intentionally
				 * preserved because they are display/calculation settings, not filters.
				 */
				List.of(
						btnTglNewLocator,
						btnTglReachableTropo,
						btnTglNewBands,
						btnTglAsNext5Min,
						tglBtnQRBEnable,
						btnTglwkd,
						btnTglwkd50,
						btnTglwkd70,
						btnTglwkd144,
						btnTglwkd432,
						btnTglwkd23,
						btnTglwkd13,
						btnTglwkd9,
						btnTglwkd6,
						btnTglwkd3,
						btnTglInactive
				).forEach(toggleButton -> toggleButton.setSelected(false));

				tglGrpQTF.selectToggle(null);
				chatMemberTableFilterQtfEnableChkbx.setSelected(false);
				chatMemberTableFilterTextField.clear();

				/*
				 * Programmatically changing a ToggleButton does not invoke its action
				 * handler. Clear the predicate list explicitly so no stale predicate
				 * can remain active.
				 */
				chatcontroller.clearChatMemberListFilterPredicates();
			});

			chatMemberTableFilterTextFieldBox.getChildren().addAll(chatMemberTableFilterTextField);


			HBox chatMemberTableFilterTextFieldAndWorkedBandsHbx = new HBox(2);
			chatMemberTableFilterTextFieldAndWorkedBandsHbx.setAlignment(Pos.CENTER_LEFT);
			chatMemberTableFilterTextFieldAndWorkedBandsHbx.setMinWidth(0);

			HBox.setHgrow(
					chatMemberTableFilterWorkedBandFiltersHbx,
					Priority.ALWAYS
			);

			chatMemberTableFilterTextFieldAndWorkedBandsHbx.getChildren().addAll(
					chatMemberTableFilterTextFieldBox,
					chatMemberTableReachabilityBox,
					chatMemberTableFilterWorkedBandFiltersHbx
			);

			chatMemberTableFilterVBoxForAllFilters.getChildren().add(chatMemberTableFilterTextFieldAndWorkedBandsHbx);

//			Tooltip filterPanelTooltip = new Tooltip("Set the station-visible-filters here");
//			Tooltip.install(chatMemberTableFilterVBoxForAllFilters,filterPanelTooltip);

			Tooltip filterTextBoxTooltip = new Tooltip("Free text search");
			Tooltip.install(chatMemberTableFilterTextField,filterTextBoxTooltip);

			chatMemberTableBorderPane.setTop(chatMemberTableFilterVBoxForAllFilters);


			mainWindowRightSplitPane.getItems().add(chatMemberTableBorderPane);

			BorderPane topPriorityListPane = initTopPriorityListPane(tbl_chatMember, txt_chatMessageUserInput);
			mainWindowRightSplitPane.getItems().add(topPriorityListPane);//adds priority list panel


			mainWindowLeftSplitPane.getItems().addAll(messageSectionSplitpane, mainWindowRightSplitPane);
			mainWindowLeftSplitPane.setDividerPositions(chatcontroller.getChatPreferences().getGUImainWindowLeftSplitPane_dividerposition());

			//first initialize how much divider positions we need...
//			chatcontroller.getChatPreferences().setGUImainWindowLeftSplitPane_dividerposition(new double[mainWindowLeftSplitPane.getDividers().size()]);
//			chatcontroller.getChatPreferences().getGUImainWindowLeftSplitPane_dividerposition()[0] = 0.2;
			/**
			 * here will follow the Splitpane divider listener to save the user made UI changes, should been made at the very end of all splitpane operations
			 */
			for (SplitPane.Divider divider : mainWindowLeftSplitPane.getDividers()) {
				divider.positionProperty().addListener(new ChangeListener<Number>() {
					@Override
					public void changed(ObservableValue<? extends Number> observableValue, Number oldDividerPos, Number newDividerPosition) {
						System.out.println("<<<<<<<<<<<<<<<<<<< mainWindowLeftSplitPanedevider " + mainWindowLeftSplitPane.getDividers().indexOf(divider)  + " position change, new position: " + newDividerPosition + " // size dev: " +  mainWindowLeftSplitPane.getDividers().size());
						chatcontroller.getChatPreferences().getGUImainWindowLeftSplitPane_dividerposition()[mainWindowLeftSplitPane.getDividers().indexOf(divider)] = newDividerPosition.doubleValue();
						requestLayoutSave();
					}
				});

			}


			mainWindowRightSplitPane.getItems().add(selectedCallSignFurtherInfoPane);

			// Ensure the stored divider array matches the current UI layout (2 dividers = 3 items).
			chatcontroller.getChatPreferences().ensureMainWindowRightSplitPaneDividerPositions(mainWindowRightSplitPane.getDividers().size());

			// Apply persisted divider positions AFTER all items exist.
			mainWindowRightSplitPane.setDividerPositions(chatcontroller.getChatPreferences().getGUImainWindowRightSplitPane_dividerposition());

			//first initialize how much divider positions we need...
//			chatcontroller.getChatPreferences().setGUImainWindowRightSplitPane_dividerposition(new double[mainWindowRightSplitPane.getDividers().size()]);

			/**
			 * here will follow the Splitpane divider listener to save the user made UI changes, should been made at the very end of all splitpane operations
			 */

			for (SplitPane.Divider divider : mainWindowRightSplitPane.getDividers()) {
				divider.positionProperty().addListener(new ChangeListener<Number>() {
					@Override
					public void changed(ObservableValue<? extends Number> observableValue, Number oldDividerPos, Number newDividerPosition) {
						System.out.println("<<<<<<<<<<<<<<<<<<<>>>>>> devider mainwindowRIGHTsplitpane " + mainWindowRightSplitPane.getDividers().indexOf(divider)  + " position change, new position: " + newDividerPosition + " // size dev: " +  mainWindowRightSplitPane.getDividers().size());
//						chatcontroller.getChatPreferences().getGUImainWindowRightSplitPane_dividerposition()[mainWindowRightSplitPane.getDividers().indexOf(divider)] = newDividerPosition.doubleValue();

						int dividerIndex = mainWindowRightSplitPane.getDividers().indexOf(divider);
						double[] storedPositions = chatcontroller.getChatPreferences().getGUImainWindowRightSplitPane_dividerposition();

						if (dividerIndex >= 0 && dividerIndex < storedPositions.length) {
							storedPositions[dividerIndex] = newDividerPosition.doubleValue();
							requestLayoutSave();
						} else {
							// Avoid crashes if preferences are older than the current UI layout.
							System.out.println("WARN: cannot store mainWindowRightSplitPane divider position: index="
									+ dividerIndex + ", storedLen=" + storedPositions.length + ", dividerCount="
									+ mainWindowRightSplitPane.getDividers().size());
						}
					}
				});

			}

			// Etappe 5c: JavaFX Hauptfenster wird nicht mehr angezeigt
			// primaryStage.setScene(scn_ChatwindowMainScene);

			/*
			 * Safety net after the Scene has been attached to the Stage.
			 * Some platforms add native window decoration after setScene(...), so this
			 * second check prevents the Stage from extending beyond the visible screen.
			 */
			// ensureStageFitsPrimaryScreen(primaryStage);

			// primaryStage.show();

		} catch (Exception e) {
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception", e);
		}

		/**
		 * Window selected callsign information
		 * Works with a ChatMember variable, initialized by a selected-listener of the Chatmemberlist
		 */



		/**
		 * end Window selected callsign information
		 */

		/**
		 * Window Cluster & qso of the other
		 *
		 * Compose now; see openMonitorWindow and kst4contest.view.compose.MonitorWindow.
		 */
		openMonitorWindow();

		/*
		 * The Compose main window, beside this one, only on --compose-main-window. It is here
		 * rather than earlier because it reads the tables and the preferences this method has
		 * just finished building.
		 */
		openComposeMainWindowIfRequested();

		/**
		 * Window updates
		 *
		 * Compose now; see openUpdateWindowIfAvailable and
		 * kst4contest.view.compose.UpdateWindow.
		 */
		openUpdateWindowIfAvailable();

		/*****************************************************************************
		 *
		 * Settings window
		 *
		 * Compose now; see openSettingsWindow and kst4contest.view.compose. It is opened
		 * here because it carries the Connect button and is therefore the way into the
		 * chat, which is what the JavaFX window was shown for at startup too.
		 *
		 ****************************************************************************/
		openSettingsWindow();



		//initialize the timeline
		Platform.runLater(this::updateTimelineVisuals);


	}

	/**
	 * This is a helping class for providing information for the TimeLineView to give full information about the
	 * Chatmember objects on which the Sked object is referring to.
	 * @param sked
	 * @return
	 */
	private String buildSkedHoverInfo(ContestSked sked) {
		if (sked == null || sked.getTargetCallsign() == null) return "";

		String callRaw = sked.getTargetCallsign().trim().toUpperCase();

		ChatMember member = null;
		for (ChatMember cm : chatcontroller.getLst_chatMemberList().snapshot()) {
			if (cm == null || cm.getCallSignRaw() == null) continue;
			if (callRaw.equals(cm.getCallSignRaw().trim().toUpperCase())) {
				member = cm;
				break;
			}
		}
		if (member == null) return "";

		AirPlaneReflectionInfo ap = member.getAirPlaneReflectInfo();
		if (ap == null) return "";

		StringBuilder sb = new StringBuilder();
		sb.append("AP reachable: ").append(ap.getAirPlanesReachableCntr());

		if (ap.getRisingAirplanes() != null && !ap.getRisingAirplanes().isEmpty()) {
			AirPlane a0 = ap.getRisingAirplanes().get(0);
			sb.append("\nNext: ")
					.append(a0.getArrivingDurationMinutes())
					.append(" min (")
					.append(a0.getPotential())
					.append("%)");

			if (ap.getRisingAirplanes().size() > 1) {
				AirPlane a1 = ap.getRisingAirplanes().get(1);
				sb.append(" / ")
						.append(a1.getArrivingDurationMinutes())
						.append(" min (")
						.append(a1.getPotential())
						.append("%)");
			}
		}
		return sb.toString();
	}


	/**
	 * Formats the global worked/grid status for the worked-any column.
	 *
	 * <p>Status characters follow the user feedback:
	 * <ul>
	 *     <li>empty = call not worked, grid not worked</li>
	 *     <li>x = call worked</li>
	 *     <li>o = grid worked, call not worked</li>
	 *     <li>xo = call and grid worked</li>
	 * </ul>
	 *
	 * <p>The call status is based on {@link ChatMember#isWorked()}, i.e. worked-any.
	 * The grid status is based on the four-character grid square worked on any band.</p>
	 *
	 * @param member station row
	 * @return compact status text
	 */
	private String formatWorkedAnyGridStatus(ChatMember member) {
		if (member == null) {
			return "";
		}

		boolean callWorked = member.isWorked();
		boolean gridWorked = chatcontroller != null && chatcontroller.isGridSquareWorkedAny(member);

		if (callWorked && gridWorked) {
			return "xo";
		}

		if (callWorked) {
			return "x";
		}

		if (gridWorked) {
			return "o";
		}

		return "";
	}

	/**
	 * Builds a tooltip for the worked-any/grid-status cell.
	 *
	 * @param member station row
	 * @return tooltip text
	 */
	private String buildWorkedAnyGridStatusTooltip(ChatMember member) {
		if (member == null) {
			return "empty = call not worked, grid not worked\n"
					+ "x = call worked\n"
					+ "o = grid worked\n"
					+ "xo = call and grid worked";
		}

		String grossField = WorkedGrossFieldCache.extractGrossField(member.getQra());
		String gridText = grossField == null ? "unknown grid" : "grid " + grossField;

		return "Worked status:\n"
				+ "empty = call not worked, grid not worked\n"
				+ "x = call worked\n"
				+ "o = grid worked\n"
				+ "xo = call and grid worked\n\n"
				+ "This station:\n"
				+ "Call worked: " + (member.isWorked() ? "yes" : "no") + "\n"
				+ "Grid worked: " + (chatcontroller != null && chatcontroller.isGridSquareWorkedAny(member) ? "yes" : "no")
				+ " (" + gridText + ")";
	}

//	/**
//	 *
//	 * resets the style of the not selected direction buttons
//	 *
//	 * REPLACED BY CSS USAGE
//	 * @deprecated
//	 * @param exceptThisButton
//	 * @return
//	 */
//	public boolean uiHelper_recolorQtfDirectionButtonsExceptThisOne(Button exceptThisButton) {
//
////		Button[] qtfButtons = new Button[8];
//
//		for (int i = 0; i < btnQtfButtonsAvl.length; i++) {
//
////			if (!btnQtfButtonsAvl[i].equals(exceptThisButton)) {
////				btnQtfButtonsAvl[i].setStyle("");
////			} else {
////				btnQtfButtonsAvl[i].setStyle("-fx-background-color:\n" +
////						"        linear-gradient(#f0ff35, #a9ff00),\n" +
////						"        radial-gradient(center 50% -40%, radius 200%, #b8ee36 45%, #80c800 50%);\n" +
////						"    -fx-background-radius: 6, 5;\n" +
////						"    -fx-background-insets: 0, 1;\n" +
////						"    -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.4) , 5, 0.0 , 0 , 1 );\n" +
////						"    -fx-text-fill: #395306;"); //Todo fancy button style
////			}
//
//		}
//
//		return true;
//
//	}

	/**
	 * 
	 * @param width,           left and right of the label
	 * @param labelofSeperator Info text
	 * @return
	 */
	public HBox generateLabeledSeparator(int width, String labelofSeperator) {

		HBox labeledSeparator = new HBox();
		Label lblInfo = new Label(labelofSeperator);
		Separator leftSeparator = new Separator();
		leftSeparator.setPrefWidth(width);
		Separator rightSeparator = new Separator();
		rightSeparator.setPrefWidth(width);
		labeledSeparator.getChildren().add(leftSeparator);
		labeledSeparator.getChildren().add(lblInfo);
		labeledSeparator.getChildren().add(rightSeparator);
		labeledSeparator.setAlignment(Pos.CENTER);

		return labeledSeparator;
	}

	/**
	 * Handles the close action of the Chatwindow
	 * 
	 * @param event
	 */
	private void closeWindowEvent(WindowEvent event) {
		System.out.println("Window close request ...");

//        if(storageModel.dataSetChanged()) {  // if the dataset has changed, alert the user with a popup
		Alert alert = new Alert(Alert.AlertType.WARNING);
		alert.getButtonTypes().remove(ButtonType.OK);
		alert.getButtonTypes().add(ButtonType.CANCEL);
		alert.getButtonTypes().add(ButtonType.YES);
		alert.setTitle("Quit application");
		alert.setContentText(String.format("Do you want to disconnect from the Chat?"));
//            alert.initOwner(primaryStage.getOwner());
		Optional<ButtonType> res = alert.showAndWait();

		if (res.isPresent()) {
			if (res.get().equals(ButtonType.CANCEL)) {
//				event.consume();
			} else {
				System.out.println("closewindowevent: exiting the application");

				// Routed through the launcher so the runtime that is actually live
				// releases its resources. After a profile switch that is no longer the
				// instance JavaFX would call stop() on.
				ApplicationRuntimeLauncher.exitApplication();
			}
		}
//        }
	}

	/**
	 * Informs a user about a warning, shows given String in simple alertwindow
	 *
	 */
	public static void alertWindowEvent(String warning) {
		System.out.println("Alert due to ... " + warning);

//        if(storageModel.dataSetChanged()) {  // if the dataset has changed, alert the user with a popup
		Alert alert = new Alert(Alert.AlertType.WARNING);
//		alert.getButtonTypes().remove(ButtonType.OK);
//		alert.getButtonTypes().add(ButtonType.CANCEL);
//		alert.getButtonTypes().add(ButtonType.YES);
		alert.setTitle("WARNING");
		alert.setContentText(String.format(warning));

        alert.show();

	}

//    public void updateStatusButtons() {
//        //TODO: Hier muss noch was hin
////        get
//    }


	public static void main(String[] args) {
		setupFileLogging();
		launch(args);
	}

	private static void setupFileLogging() {
		if (fileLogging != null) {
			return;
		}
		try {
			Path logFile = Path.of(ApplicationFileUtils.getFilePath(
					ApplicationConstants.APPLICATION_NAME,
					"kst4contest-errors.log"));
			// Start conservatively until the persisted preference has been loaded.
			fileLogging = new ApplicationFileLogging(logFile, false);
		} catch (IOException e) {
			System.err.println("Could not set up file logging: " + e.getMessage());
		}
	}

	private static void setDebugFileLoggingEnabled(boolean enabled) {
		if (fileLogging != null) {
			fileLogging.setDebugEnabled(enabled);
		}
	}

	private static void closeFileLogging() {
		if (fileLogging != null) {
			fileLogging.close();
			fileLogging = null;
		}
	}

    @Override
	public void onThreadStatusChanged(
			String key,
		    ThreadStateMessage threadStateMessage
	) {
		if (threadStateMessage == null) {
			LOGGER.log(Level.WARNING,
					"Ignoring empty thread-status update from source {0}",
					key == null ? "UNKNOWN" : key);
			return;
		}
		if (key == null || key.isBlank()) {
			LOGGER.log(Level.WARNING,
					"Ignoring thread-status update without a source key. Detail: {0}",
					threadStateMessage.getRunningInformation());
			return;
		}

		if ("ON4KST".equalsIgnoreCase(key)) {
			String detail = threadStateMessage.getRunningInformation();
			Platform.runLater(() -> updateConnectionStateIndicator(
					chatcontroller.getOn4KstConnectionState(), detail));
			return;
		}

		Platform.runLater(() -> updateStatusButton(key, threadStateMessage));
		maybeShowBandUpgradeIndicator(key, threadStateMessage);
	}


	public void onConnectionStateChanged(
			On4KstConnectionState state,
			String detail
	) {
		On4KstConnectionState effectiveState = state;
		if (effectiveState == null) {
			LOGGER.log(Level.WARNING,
					"Received ON4KST connection callback without a state; "
							+ "treating it as disconnected. Detail: {0}",
					detail);
			effectiveState = On4KstConnectionState.DISCONNECTED;
		}

		On4KstConnectionState stateToDisplay = effectiveState;
		Platform.runLater(() -> {
			boolean active = stateToDisplay.isConnectionAttemptActive();
			boolean online = stateToDisplay.isOnline();
			updateConnectionStateIndicator(stateToDisplay, detail);

			if (menuItemFileConnect != null) {
				menuItemFileConnect.setDisable(active);
			}
			if (menuItemFileDisconnect != null) {
				menuItemFileDisconnect.setDisable(!active);
			}
			if (menuItemOptionsAwayBack != null) {
				menuItemOptionsAwayBack.setDisable(!online);
			}
			if (menuItemOptionsSetFrequencyAsName != null) {
				menuItemOptionsSetFrequencyAsName.setDisable(!online);
			}
			if (sendButton != null) {
				sendButton.setDisable(!online);
			}
			if (txt_chatMessageUserInput != null) {
				txt_chatMessageUserInput.setDisable(!online);
			}
		});
	}

	@Override
	public void onSimpleLogFileCreated(Path filePath) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Simplelogfile created");
		alert.setHeaderText("The selected Simplelogfile did not exist and has been created");

		Label explanation = new Label(
				"File: " + filePath + "\n\n"
						+ "First check whether you need the Simplelogfile integration. If your logging "
						+ "application provides a supported network interface, use that interface for "
						+ "band and locator information.\n\n"
						+ "If you use Simplelogfile, configure your logging application to write its live log to this file. "
						+ "Then log a test QSO and verify that the callsign is marked as worked "
						+ "in KST4Contest within one minute.\n\n"
						+ "Before each contest, verify that the logging application writes the current "
						+ "contest log to this exact file. KST4Contest does not reset Simplelogfile-derived "
						+ "Worked marks automatically when a new contest starts.");
		explanation.setWrapText(true);

		Hyperlink manualLink = new Hyperlink("Open the Simplelogfile manual");
		manualLink.setOnAction(event -> getHostServices().showDocument(SIMPLE_LOG_MANUAL_URL));

		VBox content = new VBox(10, explanation, manualLink);
		content.setPrefWidth(560);
		alert.getDialogPane().setContent(content);
		alert.show();
	}


	/**
	 * Forces the station FilteredList to evaluate all active predicates again.
	 *
	 * <p>Several filters depend on mutable ChatMember fields, for example worked
	 * flags, AirScout windows or calculated Tropo values. Updating such fields does
	 * not necessarily create a JavaFX list-change event. Therefore we explicitly
	 * re-apply the combined predicate instead of adding/removing a dummy predicate.
	 * The dummy-predicate trick can corrupt SortedList mapping in JavaFX.</p>
	 *
	 * <p>applyChatMemberFilterPredicates() itself guards against the resulting
	 * spurious TableView selection events, so no extra guard is needed here.</p>
	 */
	private void forceChatMemberFilterRefresh() {
		updateComposeFilters();
		applyChatMemberFilterPredicates();
	}

	private void updateComposeFilters() {
		if (composeMainWindowState == null) return;
		kst4contest.view.compose.StationFilterState state = composeMainWindowState.getStationFilter();

		String text = state.getSearchText().trim();
		boolean hasText = !text.isEmpty();

		java.util.function.Predicate<ChatMember> unifiedPredicate = member -> {
			if (hasText) {
				String cs = member.getCallSign();
				if (cs == null || !cs.toUpperCase(java.util.Locale.ROOT).contains(text.toUpperCase(java.util.Locale.ROOT))) {
					return false;
				}
			}

			if (state.isOn(kst4contest.view.compose.StationFilter.ONLY_NEW_GRIDS) && !chatcontroller.isNewGridSquare(member)) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.TROPO_REACHABLE) && !isReachableViaTropoFilterMatch(member)) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.NEW_BANDS) && !isNewBandOpportunity(member)) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.AIRSCOUT_NEXT_5_MIN) && !hasAsWindowInNextMinutes(member, 5)) return false;

			if (state.isOn(kst4contest.view.compose.StationFilter.MAX_QRB)) {
				Double qrb = member.getQrb();
				if (qrb == null || qrb > state.getMaxQrbKm()) return false;
			}

			if (state.isOn(kst4contest.view.compose.StationFilter.QTF)) {
				Double qtf = member.getQTFdirection();
				if (qtf == null || !kst4contest.locatorUtils.DirectionUtils.isAngleInRange(qtf, state.getQtfDegrees(), chatcontroller.getChatPreferences().getStn_antennaBeamWidthDeg())) return false;
			}

			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_INACTIVE)) {
				long inactiveMinutes = kst4contest.controller.Utils4KST.time_getSecondsBetweenEpochAndNow(member.getActivityTimeLastInEpoch() + "") / 60;
				if (inactiveMinutes > 20L) return false;
			}

			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_ANY) && member.isWorked()) return false;

			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_50) && (member.isWorked50() || !member.isQrv50())) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_70) && (member.isWorked70() || !member.isQrv70())) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_144) && (member.isWorked144() || !member.isQrv144())) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_432) && (member.isWorked432() || !member.isQrv432())) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_23) && (member.isWorked1240() || !member.isQrv1240())) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_13) && (member.isWorked2300() || !member.isQrv2300())) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_9) && (member.isWorked3400() || !member.isQrv3400())) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_6) && (member.isWorked5600() || !member.isQrv5600())) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_3) && (member.isWorked10G() || !member.isQrv10G())) return false;

			return true;
		};

		composeMainWindowState.getStations().setRowFilter(unifiedPredicate::test);

		chatcontroller.clearChatMemberListFilterPredicates();
		chatcontroller.addChatMemberListFilterPredicate(unifiedPredicate);
	}

	@Override
	public void onUserListUpdated() {
		scheduleUserListRefresh(null);
	}

	@Override
	public void onUserListUpdated(final String reason) {
		scheduleUserListRefresh(reason);
	}

	private void scheduleUserListRefresh(final String diagnosticReason) {
		Platform.runLater(() -> {
			if (diagnosticReason != null && !diagnosticReason.isBlank()) {
				pendingUserListUpdateReason = diagnosticReason;
			}

			if (userListRefreshCoalescer == null) {
				userListRefreshCoalescer = new PauseTransition(Duration.millis(300));
				userListRefreshCoalescer.setOnFinished(event -> {
					final String completedDiagnosticReason = pendingUserListUpdateReason;
					pendingUserListUpdateReason = "";

					forceChatMemberFilterRefresh();

					if (tbl_chatMember != null) {
						tbl_chatMember.refresh();
					}
					if (composeMainWindowState != null) {
						composeMainWindowState.getStations().forceRedraw();
					}

					refreshStationMapIfVisible();

					if (!completedDiagnosticReason.isEmpty()) {
						System.out.println("KST4Capp, UI Update Trigger: "
								+ completedDiagnosticReason);
					}
				});
			}

			userListRefreshCoalescer.playFromStart();
		});
	}


//	public class MaidenheadLocatorMapPane extends Pane {
//
//		private static final double MAP_WIDTH = 800;
//		private static final double MAP_HEIGHT = 600;
//		private static final double CIRCLE_RADIUS = 5;
//		private static final double TEXT_OFFSET_X = 10;
//		private static final double TEXT_OFFSET_Y = -10;
//
//		public MaidenheadLocatorMapPane() {
//			setPrefSize(MAP_WIDTH, MAP_HEIGHT);
//		}
//
//		public void addLocator(String locator, Color color) {
//			double[] coords = locatorToCoordinates(locator);
//			Circle circle = new Circle(coords[0], coords[1], CIRCLE_RADIUS, color);
//			Text text = new Text(coords[0] + TEXT_OFFSET_X, coords[1] + TEXT_OFFSET_Y, locator);
//			getChildren().addAll(circle, text);
//		}
//
//		public void connectLocators(String locator1, String locator2) {
//			double[] coords1 = locatorToCoordinates(locator1);
//			double[] coords2 = locatorToCoordinates(locator2);
//			Line line = new Line(coords1[0], coords1[1], coords2[0], coords2[1]);
//			getChildren().add(line);
//
//			// Calculate distance between locators
//			double distance = calculateDistance(coords1, coords2);
//
//			// Calculate direction in degrees from locator1 to locator2
//			double direction = calculateDirection(coords1, coords2);
//
//			// Format distance to display only two decimal places
//			DecimalFormat df = new DecimalFormat("#.##");
//
//			// Create text for displaying distance and direction
//			Text distanceText = new Text((coords1[0] + coords2[0]) / 2, (coords1[1] + coords2[1]) / 2, "Distance: " + df.format(distance) + " km");
//			Text directionText = new Text((coords1[0] + coords2[0]) / 2, (coords1[1] + coords2[1]) / 2 + 20, "Direction: " + df.format(direction) + "°");
//			getChildren().addAll(distanceText, directionText);
//		}
//
//		// Helper method to convert Maidenhead locator string to coordinates
//		private double[] locatorToCoordinates(String locator) {
//			double lon = (locator.charAt(0) - 'A') * 20 - 180;
//			double lat = (locator.charAt(1) - 'A') * 10 - 90;
//			lon += (locator.charAt(2) - '0') * 2;
//			lat += (locator.charAt(3) - '0');
//			lon += (locator.charAt(4) - 'A') * 5.0 / 60;
//			lat += (locator.charAt(5) - 'A') * 2.5 / 60;
//
//			// Convert coordinates to map coordinates
//			double x = (lon + 180) / 360 * MAP_WIDTH;
//			double y = MAP_HEIGHT - (lat + 90) / 180 * MAP_HEIGHT;
//			return new double[]{x, y};
//		}
//
//		// Helper method to calculate distance between two coordinates (in km)
//		private double calculateDistance(double[] coords1, double[] coords2) {
//			double lon1 = Math.toRadians(coords1[0]);
//			double lat1 = Math.toRadians(coords1[1]);
//			double lon2 = Math.toRadians(coords2[0]);
//			double lat2 = Math.toRadians(coords2[1]);
//
//			double dlon = lon2 - lon1;
//			double dlat = lat2 - lat1;
//
//			double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
//			double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//
//			// Earth radius in km
//			double radius = 6371;
//
//			return radius * c;
//		}
//
//		// Helper method to calculate direction in degrees from coords1 to coords2
//		private double calculateDirection(double[] coords1, double[] coords2) {
//			double lon1 = Math.toRadians(coords1[0]);
//			double lat1 = Math.toRadians(coords1[1]);
//			double lon2 = Math.toRadians(coords2[0]);
//			double lat2 = Math.toRadians(coords2[1]);
//
//			double dLon = lon2 - lon1;
//
//			double y = Math.sin(dLon) * Math.cos(lat2);
//			double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
//
//			double direction = Math.atan2(y, x);
//			direction = Math.toDegrees(direction);
//			direction = (direction + 360) % 360;
//
//			return direction;
//		}
//	}


	/**
	 * Returns the best currently selected ChatMember for actions that depend on a
	 * selected station.
	 *
	 * <p>The normal source is selectedCallSignInfoStageChatMember. As a fallback,
	 * the current table selection and the ScoreService selection are checked. If
	 * nothing is selected yet, null is returned. This is valid and must be handled
	 * by the caller.</p>
	 *
	 * @return selected ChatMember or null if no station is selected
	 */
	private ChatMember getEffectiveSelectedChatMember() {
		if (selectedCallSignInfoStageChatMember != null) {
			return selectedCallSignInfoStageChatMember;
		}

		if (tbl_chatMember != null
				&& tbl_chatMember.getSelectionModel() != null
				&& tbl_chatMember.getSelectionModel().getSelectedItem() != null) {
			return tbl_chatMember.getSelectionModel().getSelectedItem();
		}

		if (chatcontroller != null
				&& chatcontroller.getScoreService() != null
				&& chatcontroller.getScoreService().getSelectedChatMember() != null) {
			return chatcontroller.getScoreService().getSelectedChatMember();
		}

		return null;
	}


	/**
	 * Maps Ctrl+1 through Ctrl+0 to snippet list indices 0 through 9.
	 *
	 * @param keyEvent keyboard event from the main scene
	 * @return snippet index or {@code -1} if the event is not a snippet shortcut
	 */
	private int resolveSnippetIndex(KeyEvent keyEvent) {
		if (!keyEvent.isControlDown()) {
			return -1;
		}

		return switch (keyEvent.getCode()) {
			case DIGIT1 -> 0;
			case DIGIT2 -> 1;
			case DIGIT3 -> 2;
			case DIGIT4 -> 3;
			case DIGIT5 -> 4;
			case DIGIT6 -> 5;
			case DIGIT7 -> 6;
			case DIGIT8 -> 7;
			case DIGIT9 -> 8;
			case DIGIT0 -> 9;
			default -> -1;
		};
	}

	/**
	 * Resolves variables in a shortcut or snippet and appends the result to the
	 * message field.
	 *
	 * <p>The replacement is performed by the action that inserts the text. It is
	 * deliberately not performed by a text-property listener because changing a
	 * JavaFX TextField while the same key event is still being processed can leave
	 * invalid selection bounds behind.</p>
	 *
	 * @param template shortcut or snippet text to append
	 */
	private void appendResolvedMessageText(String template) {
		String resolvedText = messageVariableResolver.resolveForSelectedStation(
				template,
				getEffectiveSelectedChatMember()
		);

		if (resolvedText == null || resolvedText.isBlank()) {
			return;
		}

		String currentText = txt_chatMessageUserInput.getText();
		txt_chatMessageUserInput.setText(
				(currentText == null ? "" : currentText) + resolvedText
		);
		txt_chatMessageUserInput.requestFocus();
		txt_chatMessageUserInput.selectEnd();
	}

	/**
	 * Checks whether an outgoing private message targets the local callsign.
	 *
	 * @param messageText outgoing message text
	 * @return {@code true} if the /cq target is the local station
	 */
	private boolean isMessageAddressedToOwnCallsign(String messageText) {
		String targetCallsign = extractCqTargetCallsign(messageText);

		if (targetCallsign == null
				|| chatcontroller == null
				|| chatcontroller.getChatPreferences() == null) {
			return false;
		}

		String ownCallsign = chatcontroller.getChatPreferences().getStn_loginCallSign();
		return ownCallsign != null && targetCallsign.equalsIgnoreCase(ownCallsign.trim());
	}

	/**
	 * Inserts one configured snippet as a private message to the selected station.
	 *
	 * <p>Missing selections and unassigned snippet positions are valid operator
	 * states. They are checked explicitly and no longer handled through a generic
	 * exception.</p>
	 *
	 * @param snippetIndex zero-based index in the configured snippet list
	 */
	private void insertTextSnippet(int snippetIndex) {
		ChatMember selectedStation = getEffectiveSelectedChatMember();
		ObservableList<String> snippets = mirrorOf(chatcontroller.getChatPreferences().getLst_txtSnipList());

		if (selectedStation == null || selectedStation.getCallSign() == null) {
			System.out.println("[Main.java, Info]: Text snippet ignored because no station is selected.");
			return;
		}

		if (snippetIndex < 0 || snippetIndex >= snippets.size()) {
			System.out.println("[Main.java, Info]: No text snippet is configured for index " + snippetIndex + ".");
			return;
		}

		String preparedMessage = "/cq " + selectedStation.getCallSign() + " " + snippets.get(snippetIndex);
		txt_chatMessageUserInput.setText(
				messageVariableResolver.resolveForSelectedStation(preparedMessage, selectedStation)
		);
		txt_chatMessageUserInput.requestFocus();
		txt_chatMessageUserInput.selectEnd();
	}


	/**
	 * Resolves the chat category for an outgoing operator message.
	 *
	 * <p>If a station is selected, the message is sent in the category of that
	 * station. If no station is selected yet, the message is sent in the main chat
	 * category. This prevents NullPointerExceptions when the operator writes to the
	 * chat immediately after joining.</p>
	 *
	 * @param selectedMember selected station, may be null
	 * @return category for the outgoing message, normally main or second category
	 */
	private ChatCategory resolveOutgoingChatCategory(ChatMember selectedMember) {
		ChatCategory mainCategory = chatcontroller.getChatCategoryMain();
		ChatCategory secondCategory = chatcontroller.getChatCategorySecondChat();

		if (selectedMember == null || selectedMember.getChatCategory() == null) {
			return mainCategory;
		}

		String categoryNumber = selectedMember.getChatCategory().getCategoryNumber() + "";

		if (mainCategory != null
				&& categoryNumber.equals(mainCategory.getCategoryNumber() + "")) {
			return mainCategory;
		}

		if (secondCategory != null
				&& categoryNumber.equals(secondCategory.getCategoryNumber() + "")) {
			return secondCategory;
		}

		return mainCategory;
	}


	/**
	 * helper method, in cases of QRG ending qith 0 as double values, it will fill the ending 0 to the qrg string
	 * @param raw
	 * @return
	 */
	private static String formatQrgForUi(String raw) {

		if (raw == null) return "";
		String s = raw.trim();
		if (s.isEmpty()) return "";

		// Einheitlich Punkt als Dezimaltrenner
		s = s.replace(',', '.');

		// Wenn es nicht numerisch ist: einfach anzeigen wie es ist
		try {
			Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return s;
		}

		int dot = s.indexOf('.');
		if (dot < 0) {
			// keine Nachkommastellen -> .000 ergänzen
			return s + ".000";
		}

		String dec = s.substring(dot + 1);
		if (dec.length() >= 3) {
			// schon >= 3 Nachkommastellen -> unverändert lassen
			return s;
		}

		StringBuilder sb = new StringBuilder(s);
		while (dec.length() < 3) {
			sb.append('0');
			dec += "0";
		}
		return sb.toString();
	}

	private static <T> void applyQrgUiFormatting(TableColumn<T, String> col) {
		col.setCellFactory(tc -> new TruncatedTextTableCell<>(Kst4ContestApplication::formatQrgForUi));
	}

	@SafeVarargs
	private static <T> void applyTruncatedTextCells(TableColumn<T, String>... columns) {
		for (TableColumn<T, String> column : columns) {
			column.setCellFactory(ignored -> new TruncatedTextTableCell<>());
		}
	}

	/**
	 * Interface for the chatcontroller to update the timeline
	 *
	 * @return
	 */
	public TimelineView getTimelineView() {
		return timelineView;
	}

	// NEW: Helper to force-refresh the table (triggering row color update)
	public void refreshChatMemberTable() {
		if (tbl_chatMember != null) {
			tbl_chatMember.refresh();
		}
	}

	/*************************************************************************************
	 * Settings window (Compose)
	 *
	 * The window, its eleven tabs and the rules of every field live in Kotlin under
	 * kst4contest.view.compose. What stays here is the work only this class can do:
	 * rebuilding JavaFX controls, and starting or dropping the ON4KST session.
	 ************************************************************************************/

	/**
	 * There is no configurable base font size; the JavaFX settings window used the
	 * platform default. This is the same value the operator profile picker uses, so both
	 * Compose windows read like the JavaFX ones next to them.
	 */
	private static final float SETTINGS_WINDOW_FONT_SIZE_SP = 12f;

	private final SettingsNotices settingsNotices = new SettingsNotices();

	/**
	 * The monitor window's roster subscriptions. Held so shutdownRuntime can release
	 * them: a discarded runtime that still feeds a closed window keeps itself alive.
	 */
	private java.util.function.Consumer<java.util.List<ClusterMessage>> monitorClusterListener;

	private java.util.function.Consumer<java.util.List<ChatMessage>> monitorChatListener;

	/** Releases the monitor window's roster subscriptions. */
	private void releaseMonitorListeners() {

		if (monitorClusterListener != null) {
			chatcontroller.getLst_clusterMemberList().removeListener(monitorClusterListener);
			monitorClusterListener = null;
		}

		if (monitorChatListener != null) {
			chatcontroller.getLst_globalChatMessageList().removeListener(monitorChatListener);
			monitorChatListener = null;
		}
	}

	/*****************************************************
	 * The Compose main window, opened beside the JavaFX one
	 ****************************************************/

	/** Held so the feeds can be released; a discarded runtime that still feeds a window lives on. */
	private MainWindowState composeMainWindowState;
	private java.util.function.Consumer<java.util.List<ChatMember>> composeMemberListener;
	private java.util.function.Consumer<java.util.List<ChatMessage>> composeChatListener;
	private java.util.function.Consumer<java.util.List<ClusterMessage>> composeClusterListener;

	/**
	 * Opens the Compose main window beside the JavaFX one.
	 *
	 * <p>Only on --compose-main-window, and never a second time. The two run side by side so
	 * the operator can compare them against a live session, which is the only way the
	 * differences of the last three Etappen came to light.</p>
	 */
	private void openComposeMainWindowIfRequested() {

		// Etappe 5c: Compose Hauptfenster wird immer gestartet
		// if (!CommandLineOptions.remembered().isComposeMainWindowRequested()) {
		// 	return;
		// }

		// if (MainWindowHost.isOpen()) {
		// 	return;
		// }

		ColumnWidthStore widths = new ColumnWidthStore() {
			@Override
			public Double width(String tableId, String columnId) {
				java.util.OptionalDouble stored =
						chatcontroller.getChatPreferences().getTableColumnWidth(tableId, columnId);
				return stored.isPresent() ? stored.getAsDouble() : null;
			}

			@Override
			public void setWidth(String tableId, String columnId, double width) {
				chatcontroller.getChatPreferences().setTableColumnWidth(tableId, columnId, width);
			}

			@Override
			public void requestSave() {
				requestLayoutSave();
			}
		};

		/*
		 * Own table ids, deliberately. The Compose window's columns are the operator's to
		 * arrange separately while both windows are on screen; sharing the ids would let a
		 * drag in one silently rearrange the other.
		 */
		DataTableState<ChatMember> stations = new DataTableState<>(
				StationColumns.INSTANCE.all(
						member -> {
							Band band = resolveReachabilityBandForUi(member);
							chatcontroller.getReachabilityService()
									.ensureTropoMarginCalculated(member, band);
							return member.formatTropoSsbMarginForBand(band);
						},
						member -> parseTableDouble(formatPriorityScore(member)),
						(member, band, worked) -> formatBandCellStatus(member, band, worked),
						BandOpportunityResolver.getEnabledStationBands(chatcontroller.getChatPreferences())),
				RowKeys.INSTANCE.byValue(ChatMember::getCallSign),
				"compose-stations",
				widths);

		DataTableState<ChatMessage> directed = new DataTableState<>(
				DirectedMessageColumns.INSTANCE.all(
						() -> chatcontroller.getChatPreferences().getStn_loginCallSign(),
						chatcontroller::formatChatMessageTextForDisplay),
				RowKeys.INSTANCE.byReference(),
				"compose-directed-messages",
				widths);

		DataTableState<ChatMessage> publicMessages = new DataTableState<>(
				PublicMessageColumns.INSTANCE.all(),
				RowKeys.INSTANCE.byReference(),
				"compose-public-messages",
				widths);

		DataTableState<ClusterMessage> cluster = new DataTableState<>(
				MonitorColumns.INSTANCE.dxCluster(),
				RowKeys.INSTANCE.byReference(),
				"compose-dx-cluster",
				widths);

		DataTableState<ChatMessage> qsoOfTheOther = new DataTableState<>(
				MonitorColumns.INSTANCE.qsoOfTheOther(),
				RowKeys.INSTANCE.byReference(),
				"compose-qso-other",
				widths);

		DataTableState<ChatMessage> selectedStationMessages = new DataTableState<>(
				SelectedStationMessageColumns.INSTANCE.all(),
				RowKeys.INSTANCE.byReference(),
				"compose-selected-station-messages",
				widths);

		MainMenuState menu = new MainMenuState();
		MainWindowSurroundings surroundings = new MainWindowSurroundings(menu);

		SelectedStationState selectedStation = new SelectedStationState(
				member -> {
					chatcontroller.propagateNotQrvStateToActiveMembers(member);
					return kotlin.Unit.INSTANCE;
				},
				this::createSkedFromCompose,
				member -> kotlin.Unit.INSTANCE,
				member -> kotlin.Unit.INSTANCE,
				filter -> {
					if (filter == null) {
						return kotlin.Unit.INSTANCE;
					}
					Predicate<ChatMessage> predicate;
					switch (filter) {
						case PM_TO_ME:
							predicate = chatMessage -> {
								try {
									if (chatMessage.getReceiver().getCallSign().equals("ALL") && !(chatMessage.getMessageText().toLowerCase().contains(chatcontroller.getChatPreferences().getStn_loginCallSign().toLowerCase()))) {
										return false;
									}
									return ((chatMessage.getReceiver().getCallSign().equals(chatcontroller.getChatPreferences().getStn_loginCallSign())) || (chatMessage.getSender().getCallSign().equals(chatcontroller.getChatPreferences().getStn_loginCallSign()))) &&
											((chatMessage.getReceiver().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) || (chatMessage.getSender().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())));
								} catch (Exception e) { return false; }
							};
							break;
						case PM_TO_OTHER:
							predicate = chatMessage -> {
								try {
									return (chatMessage.getSender().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign()) && !chatMessage.getReceiver().getCallSign().equals("ALL") && !chatMessage.getReceiver().getCallSign().equals(chatcontroller.getChatPreferences().getStn_loginCallSign())) ||
											(chatMessage.getReceiver().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign()) && !chatMessage.getReceiver().getCallSign().equals("ALL") && !chatMessage.getReceiver().getCallSign().equals(chatcontroller.getChatPreferences().getStn_loginCallSign()));
								} catch (Exception e) { return false; }
							};
							break;
						case PUBLIC:
							predicate = chatMessage -> {
								try {
									return chatMessage.getSender().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign()) && chatMessage.getReceiver().getCallSign().equals("ALL");
								} catch (Exception e) { return false; }
							};
							break;
						case NOTHING:
						default:
							predicate = chatMessage -> {
								try {
									return chatMessage.getSender().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign()) || chatMessage.getReceiver().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign());
								} catch (Exception e) { return false; }
							};
							break;
					}
					applySelectedCallSignInfoFilter(predicate);
					return kotlin.Unit.INSTANCE;
				},
				member -> {
					if (member == null || member.getCallSignRaw() == null) {
						return "";
					}
					return formatDetectedRxBandsForCallsignRaw(member.getCallSignRaw(), 30L * 60L * 1000L);
				},
				member -> {
					if (member == null) return null;
					java.util.EnumSet<Band> enabledBands = kst4contest.logic.BandOpportunityResolver.getEnabledStationBands(chatcontroller.getChatPreferences());
					return resolveDefaultSkedBand(member, enabledBands);
				});

		if (chatcontroller.getChatPreferences().isGuiOptions_defaultFilterPmToMe()) {
			selectedStation.setMessageFilter(SelectedMessageFilter.PM_TO_ME);
		} else if (chatcontroller.getChatPreferences().isGuiOptions_defaultFilterPmToOther()) {
			selectedStation.setMessageFilter(SelectedMessageFilter.PM_TO_OTHER);
		} else {
			selectedStation.setMessageFilter(SelectedMessageFilter.NOTHING);
		}

		TopPriorityState topPriority = new TopPriorityState(
				this::composeTopStations,
				member -> selectStationInCompose(selectedStation, stations, member));

		ChatInputState chatInput = new ChatInputState(new ComposeChatInputActions(this));

		composeMainWindowState = new MainWindowState(
				BandOpportunityResolver.getEnabledStationBands(chatcontroller.getChatPreferences()),
				chatcontroller.getChatPreferences(),
				stations,
				new StationFilterState(),
				directed,
				publicMessages,
				cluster,
				qsoOfTheOther,
				selectedStationMessages,
				selectedStation,
				topPriority,
				new TimelineState(),
				chatInput,
				menu,
				surroundings,
				new BlinkingNotice(),
				new BlinkingNotice(),
				new ThreadStatusButtons(),
				new SplitterState(
						2,
						chatcontroller.getChatPreferences().getGUImainWindowLeftSplitPane_dividerposition(),
						SplitterState.DEFAULT_MIN_PANE_PX,
						positions -> {
							chatcontroller.getChatPreferences()
									.setGUImainWindowLeftSplitPane_dividerposition(positions);
							requestLayoutSave();
							return kotlin.Unit.INSTANCE;
						}),
				new SplitterState(
						MainWindowState.MESSAGE_PANE_COUNT,
						chatcontroller.getChatPreferences().getGUImessageSectionSplitpane_dividerposition(),
						SplitterState.DEFAULT_MIN_PANE_PX,
						positions -> {
							chatcontroller.getChatPreferences()
									.setGUImessageSectionSplitpane_dividerposition(positions);
							requestLayoutSave();
							return kotlin.Unit.INSTANCE;
						}),
				new SplitterState(
						MainWindowState.RIGHT_PANE_COUNT,
						chatcontroller.getChatPreferences().getGUImainWindowRightSplitPane_dividerposition(),
						SplitterState.DEFAULT_MIN_PANE_PX,
						positions -> {
							chatcontroller.getChatPreferences()
									.setGUImainWindowRightSplitPane_dividerposition(positions);
							requestLayoutSave();
							return kotlin.Unit.INSTANCE;
						}));

		menu.setConnectLabel(buildComposeConnectLabel());

		/*
		 * Subscribed first and seeded afterwards, so a message arriving in between is not
		 * missed — the order FxRosterBinding uses for the same reason.
		 */
		composeMemberListener = rows -> uiDispatcher.runOnUi(() -> stations.replaceRows(rows));
		chatcontroller.getLst_chatMemberList().addListener(composeMemberListener);

		composeChatListener = rows -> uiDispatcher.runOnUi(() -> {
			directed.replaceRows(chatcontroller.toMeMessages());
			publicMessages.replaceRows(rows);
			qsoOfTheOther.replaceRows(chatcontroller.toOtherMessages());
		});
		chatcontroller.getLst_globalChatMessageList().addListener(composeChatListener);

		composeClusterListener = rows -> uiDispatcher.runOnUi(() -> cluster.replaceRows(rows));
		chatcontroller.getLst_clusterMemberList().addListener(composeClusterListener);

		if (chatcontroller.getScoreService() != null) {
			chatcontroller.getScoreService().topCandidates().addListener(candidates -> uiDispatcher.runOnUi(() -> topPriority.refresh()));
		}
		uiDispatcher.runOnUi(() -> topPriority.refresh());

		stations.replaceRows(chatcontroller.getLst_chatMemberList().snapshot());
		directed.replaceRows(chatcontroller.toMeMessages());
		publicMessages.replaceRows(chatcontroller.getLst_globalChatMessageList().snapshot());
		cluster.replaceRows(chatcontroller.getLst_clusterMemberList().snapshot());
		qsoOfTheOther.replaceRows(chatcontroller.toOtherMessages());
		if (selectedCallSignInfoMessageBinding != null) {
			selectedStationMessages.replaceRows(new java.util.ArrayList<>(selectedCallSignInfoMessageBinding.list()));
		}

		double[] storedSize = chatcontroller.getChatPreferences().getGUIscn_ChatwindowMainSceneSizeHW();
		javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
		MainWindowSize size = MainWindowFrame.INSTANCE.startupSize(
				storedSize, screen.getHeight(), screen.getWidth());

		MainWindowHost.show(
				composeMainWindowState,
				new ComposeMenuActions(this),
				MainWindowFrame.INSTANCE.title(
						"KST4Contest (Compose)", buildOperatorProfileTitleSuffixForCompose()),
				chatcontroller.getChatPreferences().isGUI_darkModeActive(),
				SETTINGS_WINDOW_FONT_SIZE_SP,
				(float) size.getWidthDp(),
				(float) size.getHeightDp(),
				(width, height) -> {
					/*
					 * Not written to the profile while both windows live: the JavaFX window
					 * owns that setting, and two windows writing one size would leave the
					 * operator's arrangement to whichever was resized last.
					 */
					return kotlin.Unit.INSTANCE;
				},
				() -> composeSkedBands(selectedStation.getSelected()),
				address -> runOnUi(() -> getHostServices().showDocument(address)),
				() -> {
					runOnUi(() -> showSelectedCallsignOnMap());
					return kotlin.Unit.INSTANCE;
				},
				() -> {
					if (selectedCallSignInfoStageChatMember != null) {
						chatcontroller.airScout_SendAsShowPathPacket(selectedCallSignInfoStageChatMember);
					}
					return kotlin.Unit.INSTANCE;
				},
				member -> selectStationInCompose(selectedStation, stations, member),
				message -> onDirectedMessageClickedInCompose(selectedStation, stations, composeMainWindowState.getChatInput(), message),
				this::isOwnChatMessage,
				this::chatMessageAgeSeconds,
				() -> {
					runOnUi(() -> {
						updateComposeFilters();
					});
					return kotlin.Unit.INSTANCE;
				},
				() -> {
					runOnUi(() -> showTopPriorityCandidatesWindow(tbl_chatMember, txt_chatMessageUserInput));
					return kotlin.Unit.INSTANCE;
				},
				candidate -> {
					if (candidate == null) return kotlin.Unit.INSTANCE;
					ChatMember resolved = resolveChatMemberForCallRawAndCategory(
							candidate.getCallSignRaw(), candidate.getPreferredChatCategory());
					if (resolved != null) {
						selectStationInCompose(selectedStation, stations, resolved);
						composeMainWindowState.getChatInput().prepareCq(resolved.getCallSign(), false);
						
						// Sync with JavaFX state just in case
						prepareCqTextForSelectedChatMember(resolved, false);
						focusChatMemberAndPrepareCq(resolved, false);
					}
					return kotlin.Unit.INSTANCE;
				});

		updateComposeSurroundings();
	}

	/**
	 * Arranges a sked from the Compose panel, exactly as the JavaFX sked button does.
	 *
	 * @return whether it was arranged; a station without a bearing still gets one, at 0°, as
	 *         the original did
	 */
	private boolean createSkedFromCompose(
			final ChatMember selectedMember,
			final int minutes,
			final Band band,
			final String mode) {

		if (selectedMember == null || band == null) {
			return false;
		}

		long skedTime = System.currentTimeMillis() + minutes * 60_000L;
		double azimuth =
				selectedMember.getQTFdirection() != null ? selectedMember.getQTFdirection() : 0.0;

		ContestSked sked = new ContestSked(
				selectedMember.getCallSignRaw(),
				selectedMember.getCallSign(),
				selectedMember.getChatCategory(),
				azimuth,
				skedTime,
				band);

		chatcontroller.addSked(sked);
		chatcontroller.getScoreService().requestRecompute("sked-created");

		// Arm PM reminders, exactly as the JavaFX Create sked handler does
		if (composeMainWindowState != null) {
			SelectedStationState ssState = composeMainWindowState.getSelectedStation();
			if (ssState.getRemindPm()) {
				List<Integer> offsets = parseMinuteOffsets(ssState.getReminderOffsets());
				chatcontroller.getSkedReminderService().armReminders(
						sked.getTargetChatCallsign(),
						sked.getTargetChatCategory(),
						skedTime,
						offsets
				);
			}
		}

		return true;
	}

	/** The Connect item's label, built exactly as initMenuBar builds it. */
	private String buildComposeConnectLabel() {
		ChatCategory mainCat = chatcontroller.getChatPreferences().getLoginChatCategoryMain();
		if (mainCat == null) {
			return "Connect";
		}

		String label = "Connect to " + mainCat.getChatCategoryName(mainCat.getCategoryNumber());
		if (chatcontroller.getChatPreferences().isLoginToSecondChatEnabled()) {
			ChatCategory secondCat = chatcontroller.getChatPreferences().getLoginChatCategorySecond();
			if (secondCat != null) {
				label += " & " + secondCat.getChatCategoryName(secondCat.getCategoryNumber());
			}
		}
		return label;
	}

	/** The profile name for the title, or null for the root profile. */
	private String buildOperatorProfileTitleSuffixForCompose() {
		OperatorProfileSelection activeProfile = ActiveOperatorProfile.get();
		if (activeProfile == null || activeProfile.getProfile().isRootProfile()) {
			return null;
		}
		return activeProfile.getProfile().getDisplayName();
	}

	/** The bands a sked can be arranged on for this station: enabled here, available there. */
	private java.util.List<Band> composeSkedBands(ChatMember selectedMember) {
		if (selectedMember == null) {
			return java.util.List.of();
		}

		java.util.List<ChatMember> variants =
				chatcontroller.findActiveChatMembersByRawCall(selectedMember.getCallSignRaw());
		if (variants.isEmpty()) {
			variants = java.util.List.of(selectedMember);
		}

		BandOpportunityResolver.Resolution resolution =
				BandOpportunityResolver.resolve(variants, System.currentTimeMillis());
		EnumSet<Band> enabledBands =
				BandOpportunityResolver.getEnabledStationBands(chatcontroller.getChatPreferences());

		EnumSet<Band> available = EnumSet.copyOf(resolution.getAvailableBands());
		available.retainAll(enabledBands);
		return new ArrayList<>(available);
	}

	/** The score service's ranking, as the Compose priority bar wants it. */
	private java.util.List<kotlin.Pair<ChatMember, Double>> composeTopStations() {
		java.util.List<kotlin.Pair<ChatMember, Double>> out = new ArrayList<>();

		if (chatcontroller.getScoreService() == null) {
			return out;
		}

		for (ScoreService.TopCandidate candidate
				: chatcontroller.getScoreService().topCandidates().snapshot()) {

			/*
			 * A candidate names a callsign, not a station. The first active variant is the one
			 * the JavaFX buttons resolved to as well; a callsign with no variant left the chat
			 * between the ranking and this draw.
			 */
			java.util.List<ChatMember> variants =
					chatcontroller.findActiveChatMembersByRawCall(candidate.getCallSignRaw());
			if (!variants.isEmpty()) {
				out.add(new kotlin.Pair<>(variants.get(0), candidate.getScore()));
			}
		}
		return out;
	}

	/**
	 * Selects a station in the Compose window: the panel, the table and the score service, the
	 * three places the JavaFX selection listener kept in step.
	 */
	private kotlin.Unit selectStationInCompose(
			SelectedStationState panel,
			DataTableState<ChatMember> table,
			ChatMember member) {

		panel.select(member);
		table.select(member);
		
		selectedCallSignInfoStageChatMember = member;
		if (selectedCallSignInfoMessageBinding != null) {
			selectedCallSignInfoMessageBinding.refresh();
			Platform.runLater(() -> {
				if (composeMainWindowState != null) {
					uiDispatcher.runOnUi(() -> composeMainWindowState.getSelectedStationMessages().replaceRows(
							new java.util.ArrayList<>(selectedCallSignInfoMessageBinding.list())
					));
				}
			});
		}

		if (member != null && chatcontroller.getScoreService() != null) {
			chatcontroller.getScoreService().setSelectedChatMember(member);
			
			if (composeMainWindowState != null && composeMainWindowState.getChatInput() != null) {
				composeMainWindowState.getChatInput().prepareCq(member.getCallSign(), false);
			}
			
			// Also sync JavaFX side
			runOnUi(() -> {
				programmaticChatMemberSelectionChange = true;
				try {
					focusChatMemberAndPrepareCq(member, false);
				} finally {
					programmaticChatMemberSelectionChange = false;
				}
			});
		}
		return kotlin.Unit.INSTANCE;
	}

	private kotlin.Unit onDirectedMessageClickedInCompose(
			SelectedStationState panel,
			DataTableState<ChatMember> stationsTable,
			ChatInputState chatInput,
			ChatMessage message) {
		
		if (message == null || message.getSender() == null) {
			return kotlin.Unit.INSTANCE;
		}

		if (message.getSender().getCallSign().equals(chatcontroller.getChatPreferences().getStn_loginCallSign())) {
			String receiverCallsign = message.getMessageText().substring(2, message.getMessageText().indexOf(")"));
			chatInput.prepareCq(receiverCallsign, false);
			
			// Sync with JavaFX state just in case
			prepareCqTextForCallsign(receiverCallsign, message.getChatCategory(), false);
		} else {
			selectStationInCompose(panel, stationsTable, message.getSender());
			
			chatInput.prepareCq(message.getSender().getCallSign(), false);
			
			// Sync with JavaFX state just in case
			prepareCqTextForSelectedChatMember(message.getSender(), false);
			focusChatMemberAndPrepareCq(message.getSender(), false);
		}

		return kotlin.Unit.INSTANCE;
	}

	/** Whether a message came from this station; decides its row colour. */
	private boolean isOwnChatMessage(ChatMessage message) {
		if (message == null || message.getSender() == null) {
			return false;
		}

		String ownCall = chatcontroller.getChatPreferences().getStn_loginCallSign();
		return ownCall != null && ownCall.equals(message.getSender().getCallSign());
	}

	/** How long ago a message arrived, for the fading highlight. */
	private long chatMessageAgeSeconds(ChatMessage message) {
		if (message == null) {
			return Long.MAX_VALUE;
		}

		try {
			long generated = Long.parseLong(message.getMessageGeneratedTime());
			return (System.currentTimeMillis() / 1000L) - generated;
		} catch (RuntimeException notATimestamp) {
			return Long.MAX_VALUE;
		}
	}

	/** Pushes the link state and the window flags into the Compose window. */
	private void updateComposeSurroundings() {
		if (composeMainWindowState == null) {
			return;
		}

		MainWindowSurroundings surroundings = composeMainWindowState.getSurroundings();
		surroundings.setConnectionState(chatcontroller.getOn4KstConnectionState());
		surroundings.setSettingsWindowOpen(SettingsWindow.isOpen());
		surroundings.setMonitorWindowOpen(MonitorWindow.isOpen());
		composeMainWindowState.getMenu().setAwayFromChat(
				chatcontroller.getChatPreferences().isStn_loginAFKState());
	}

	/** Releases the Compose window's feeds; a discarded runtime that still feeds one lives on. */
	private void releaseComposeMainWindow() {
		if (composeMemberListener != null) {
			chatcontroller.getLst_chatMemberList().removeListener(composeMemberListener);
			composeMemberListener = null;
		}
		if (composeChatListener != null) {
			chatcontroller.getLst_globalChatMessageList().removeListener(composeChatListener);
			composeChatListener = null;
		}
		if (composeClusterListener != null) {
			chatcontroller.getLst_clusterMemberList().removeListener(composeClusterListener);
			composeClusterListener = null;
		}
		if (composeMainWindowState != null) {
			composeMainWindowState.release();
			composeMainWindowState = null;
		}
	}

	/**
	 * Opens the cluster and QSO monitor.
	 *
	 * Both tables follow their roster: the listener replaces the rows, and
	 * DataTableState keeps the selection on the row the operator picked rather than on
	 * the position it happened to occupy. The two subscriptions are held in fields and
	 * released by releaseMonitorListeners, because a discarded runtime that still feeds
	 * a closed window keeps itself alive.
	 */
	private void openMonitorWindow() {

		/*
		 * Guarded before anything is built. Without this a second click registers two
		 * more roster listeners and then hands them to a window that refuses to open,
		 * so every inbound message would re-run toOtherMessages once more over up to
		 * thirty thousand stored messages — work whose result is thrown away, on the
		 * thread that draws.
		 */
		if (MonitorWindow.isOpen()) {
			return;
		}

		ColumnWidthStore widths = new ColumnWidthStore() {
			@Override
			public Double width(String tableId, String columnId) {
				java.util.OptionalDouble stored =
						chatcontroller.getChatPreferences().getTableColumnWidth(tableId, columnId);
				return stored.isPresent() ? stored.getAsDouble() : null;
			}

			@Override
			public void setWidth(String tableId, String columnId, double width) {
				chatcontroller.getChatPreferences().setTableColumnWidth(tableId, columnId, width);
			}

			@Override
			public void requestSave() {
				requestLayoutSave();
			}
		};

		DataTableState<ClusterMessage> clusterTable = new DataTableState<>(
				MonitorColumns.INSTANCE.dxCluster(),
				RowKeys.INSTANCE.byReference(),
				"dx-cluster-monitor",
				widths);

		DataTableState<ChatMessage> qsoTable = new DataTableState<>(
				MonitorColumns.INSTANCE.qsoOfTheOther(),
				RowKeys.INSTANCE.byReference(),
				"qso-other-monitor",
				widths);

		/*
		 * Subscribed first and seeded afterwards, so a message arriving in between is
		 * not missed — the order FxRosterBinding uses for the same reason.
		 */
		monitorClusterListener = rows -> uiDispatcher.runOnUi(() -> clusterTable.replaceRows(rows));
		chatcontroller.getLst_clusterMemberList().addListener(monitorClusterListener);

		/*
		 * The QSO table shows a filtered view, so the whole filter is re-run rather
		 * than the delivered list being used: toOtherMessages is what decides which
		 * messages belong to other operators.
		 */
		monitorChatListener =
				rows -> uiDispatcher.runOnUi(() -> qsoTable.replaceRows(chatcontroller.toOtherMessages()));
		chatcontroller.getLst_globalChatMessageList().addListener(monitorChatListener);

		clusterTable.replaceRows(chatcontroller.getLst_clusterMemberList().snapshot());
		qsoTable.replaceRows(chatcontroller.toOtherMessages());

		MonitorWindow.show(
				clusterTable,
				qsoTable,
				chatcontroller.getChatPreferences().isGUI_darkModeActive(),
				SETTINGS_WINDOW_FONT_SIZE_SP,
				(float) chatcontroller.getChatPreferences().getGUIclusterAndQSOMonStage_SceneSizeHW()[0],
				(float) chatcontroller.getChatPreferences().getGUIclusterAndQSOMonStage_SceneSizeHW()[1],
				(width, height) -> {
					chatcontroller.getChatPreferences().getGUIclusterAndQSOMonStage_SceneSizeHW()[0] = width;
					chatcontroller.getChatPreferences().getGUIclusterAndQSOMonStage_SceneSizeHW()[1] = height;
					requestLayoutSave();
					return kotlin.Unit.INSTANCE;
				});
	}

	/**
	 * Opens the update window when the server carries a newer release.
	 *
	 * The decision itself lives in UpdateWindowState and is covered by tests; what
	 * stays here is reaching the system browser, which is the host application's
	 * business.
	 */
	private void openUpdateWindowIfAvailable() {

		try {
			UpdateWindowState updateState = new UpdateWindowState(
					chatcontroller.getUpdateInformation(),
					ApplicationConstants.APPLICATION_CURRENT_VERSION,
					ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER);

			if (!updateState.getUpdateAvailable()) {
				return;
			}

			UpdateWindow.show(
					updateState,
					chatcontroller.getChatPreferences().isGUI_darkModeActive(),
					SETTINGS_WINDOW_FONT_SIZE_SP,
					(float) chatcontroller.getChatPreferences().getGUIstage_updateStage_SceneSizeHW()[0],
					(float) chatcontroller.getChatPreferences().getGUIstage_updateStage_SceneSizeHW()[1],
					address -> getHostServices().showDocument(address),
					(width, height) -> {
						chatcontroller.getChatPreferences().getGUIstage_updateStage_SceneSizeHW()[0] = width;
						chatcontroller.getChatPreferences().getGUIstage_updateStage_SceneSizeHW()[1] = height;
						requestLayoutSave();
						return kotlin.Unit.INSTANCE;
					});
		} catch (Exception updateProblem) {
			// The client must start even when the update service is unreachable.
			System.out.println("[KST4ContestApp, ERROR]: Problem on Updateservice! "
					+ updateProblem.getMessage());
			updateProblem.printStackTrace();
		}
	}

	/**
	 * Opens the settings window, or does nothing when it is already open.
	 *
	 * <p>This window carries the Connect button and is therefore the way into the chat,
	 * which is why it is opened once at startup as the JavaFX window was.</p>
	 */
	private void openSettingsWindow() {

		SettingsWindow.show(
				SettingsTabsKt.buildSettingsTabs(chatcontroller, this, settingsNotices),
				settingsNotices,
				chatcontroller.getChatPreferences().isGUI_darkModeActive(),
				SETTINGS_WINDOW_FONT_SIZE_SP,
				(float) chatcontroller.getChatPreferences().getGUIsettingsStageSceneSizeHW()[0],
				(float) chatcontroller.getChatPreferences().getGUIsettingsStageSceneSizeHW()[1],
				(width, height) -> {
					chatcontroller.getChatPreferences().getGUIsettingsStageSceneSizeHW()[0] = width;
					chatcontroller.getChatPreferences().getGUIsettingsStageSceneSizeHW()[1] = height;
					requestLayoutSave();
					return kotlin.Unit.INSTANCE;
				},
				SettingsTabsKt.settingsButtons(chatcontroller, this, settingsNotices));
	}

	@Override
	public void refreshShortcutButtonsFromSettings() {
		uiDispatcher.runOnUi(this::refreshShortcutButtons);
	}

	@Override
	public void refreshTextSnippetContextMenusFromSettings() {
		uiDispatcher.runOnUi(this::refreshTextSnippetContextMenus);
	}

	@Override
	public String detectWintestBroadcastAddress() {

		try {
			return detectPreferredWintestBroadcastAddress();
		} catch (Exception detectionProblem) {
			System.out.println("[Main.java, Warning]: Could not auto-detect broadcast: "
					+ detectionProblem.getMessage());
			return null;
		}
	}

	@Override
	public void applyOwnQrgFollower(boolean enabled) {

		uiDispatcher.runOnUi(() -> {
			if (enabled) {
				attachOwnQrgFollower();
			} else {
				detachOwnQrgFollower();
			}
		});
	}

	/**
	 * The worked-stations rows are a snapshot the tab takes from the roster itself, so
	 * there is nothing for this class to redraw. The JavaFX table needed an explicit
	 * refresh because a TableView does not notice that the objects inside it changed.
	 */
	@Override
	public void refreshWorkedStationsView() {
		// nothing to do: the Compose tab re-reads the roster
	}

	@Override
	public void switchToOperatorProfile(OperatorProfile profile) {
		uiDispatcher.runOnUi(() -> requestOperatorProfileSwitch(profile));
	}

	/**
	 * Starts the ON4KST session. The credentials are already in the preferences: every
	 * field of the Station tab writes through as it is edited, so there is nothing to
	 * collect here first.
	 */
	@Override
	public String connectFromSettings() {

		try {
			chatcontroller.execute();
			return null;
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			return "The connection was interrupted: " + interrupted.getMessage();
		} catch (IOException connectionProblem) {
			return "The ON4KST server could not be reached: " + connectionProblem.getMessage();
		}
	}

	@Override
	public void disconnectOnlyFromSettings() {
		chatcontroller.disconnect(ApplicationConstants.DISCSTRING_DISCONNECTONLY);
	}

	@Override
	public void disconnectAndCloseChatFromSettings() {
		uiDispatcher.runOnUi(() -> closeWindowEvent(null));
	}

	@Override
	public String savePreferencesFromSettings() {

		if (!chatcontroller.getChatPreferences().writePreferencesToXmlFile()) {
			return null;
		}

		if (layoutAutosave != null) {
			layoutAutosave.cancelPending();
		}

		return chatcontroller.getChatPreferences().getStoreAndRestorePreferencesFileName();
	}

	private String detectPreferredWintestBroadcastAddress() {
		String internetRouteBroadcast = detectInternetRouteBroadcastAddress();
		if (internetRouteBroadcast != null && !internetRouteBroadcast.isBlank()) {
			return internetRouteBroadcast;
		}
		return detectFirstUsableBroadcastAddress();
	}

	private String detectInternetRouteBroadcastAddress() {
		java.net.DatagramSocket routeProbe = null;
		try {
			routeProbe = new java.net.DatagramSocket();
			routeProbe.connect(java.net.InetAddress.getByName("8.8.8.8"), 53);

			java.net.InetAddress localAddress = routeProbe.getLocalAddress();
			if (localAddress == null || localAddress.isAnyLocalAddress() || localAddress.isLoopbackAddress()) {
				return null;
			}

			java.net.NetworkInterface networkInterface = java.net.NetworkInterface.getByInetAddress(localAddress);
			if (networkInterface == null || !networkInterface.isUp()) {
				return null;
			}

			for (java.net.InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
				if (!(interfaceAddress.getAddress() instanceof java.net.Inet4Address)) {
					continue;
				}
				if (!localAddress.equals(interfaceAddress.getAddress())) {
					continue;
				}
				if (interfaceAddress.getBroadcast() != null) {
					return interfaceAddress.getBroadcast().getHostAddress();
				}
			}

			for (java.net.InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
				if (interfaceAddress.getBroadcast() != null && interfaceAddress.getAddress() instanceof java.net.Inet4Address) {
					return interfaceAddress.getBroadcast().getHostAddress();
				}
			}
		} catch (Exception ignored) {
			// Fallback to generic detection if internet-route probing fails
		} finally {
			if (routeProbe != null && !routeProbe.isClosed()) {
				routeProbe.close();
			}
		}
		return null;
	}

	private String detectFirstUsableBroadcastAddress() {
		try {
			for (java.net.NetworkInterface networkInterface : java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())) {
				if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual() || networkInterface.isPointToPoint()) {
					continue;
				}
				for (java.net.InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
					if (interfaceAddress.getBroadcast() != null && interfaceAddress.getAddress() instanceof java.net.Inet4Address) {
						return interfaceAddress.getBroadcast().getHostAddress();
					}
				}
			}
		} catch (Exception ignored) {
			// Keep configured value if no interface can be detected
		}
		return null;
	}


	/**
	 * Checks whether a value can safely be used as an AirScout routing
	 * identifier.
	 *
	 * Spaces are allowed because AirScout encloses the identifier in quotation
	 * marks. Quotation marks and line breaks would break the protocol message.
	 *
	 * @param identifier entered server or client identifier
	 * @return true if the identifier can be used
	 */
	private boolean isValidAirScoutIdentifier(String identifier) {
		if (identifier == null) {
			return false;
		}

		String normalizedIdentifier = identifier.trim();

		return !normalizedIdentifier.isEmpty()
				&& !normalizedIdentifier.contains("\"")
				&& !normalizedIdentifier.contains("\r")
				&& !normalizedIdentifier.contains("\n");
	}

	/**
	 * Helper for creating station double preferences textfields
	 * @param initialValue
	 * @param tooltipText
	 * @param valueConsumer
	 * @return
	 */
	private TextField createDoublePreferenceTextField(double initialValue,
	                                                  String tooltipText,
	                                                  java.util.function.DoubleConsumer valueConsumer) {
		TextField textField = new TextField(String.valueOf(initialValue));
		textField.setFocusTraversable(false);
		textField.setTooltip(new Tooltip(tooltipText));

		textField.focusedProperty().addListener((observable, oldValue, focused) -> {
			if (!focused) {
				try {
					String normalizedText = textField.getText().trim().replace(",", ".");
					double parsedValue = Double.parseDouble(normalizedText);
					valueConsumer.accept(parsedValue);
					textField.setText(String.valueOf(parsedValue));
					refreshStationMapIfVisible();
				} catch (NumberFormatException exception) {
					textField.setText(String.valueOf(initialValue));
				}
			}
		});

		textField.setOnAction(event -> {
			try {
				String normalizedText = textField.getText().trim().replace(",", ".");
				double parsedValue = Double.parseDouble(normalizedText);
				valueConsumer.accept(parsedValue);
				textField.setText(String.valueOf(parsedValue));
				refreshStationMapIfVisible();
			} catch (NumberFormatException exception) {
				textField.setText(String.valueOf(initialValue));
			}
		});

		return textField;
	}

	/**
	 * Ensures that the native Stage itself fits into the primary screen's visible
	 * area.
	 *
	 * This is a second safety net in addition to getScreenAwareMainSceneSizeHW(...).
	 * The first method corrects the JavaFX Scene size. This method corrects the
	 * actual Stage position and size, including platform-specific window behaviour.
	 */
	private void ensureStageFitsPrimaryScreen(Stage stage) {
		if (stage == null) {
			return;
		}

		Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();

		double correctedX = stage.getX();
		double correctedY = stage.getY();
		double correctedWidth = stage.getWidth();
		double correctedHeight = stage.getHeight();

		if (!Double.isFinite(correctedWidth) || correctedWidth <= 0) {
			correctedWidth = visualBounds.getWidth();
		}

		if (!Double.isFinite(correctedHeight) || correctedHeight <= 0) {
			correctedHeight = visualBounds.getHeight();
		}

		correctedWidth = Math.min(correctedWidth, visualBounds.getWidth());
		correctedHeight = Math.min(correctedHeight, visualBounds.getHeight());

		if (!Double.isFinite(correctedX)) {
			correctedX = visualBounds.getMinX();
		}

		if (!Double.isFinite(correctedY)) {
			correctedY = visualBounds.getMinY();
		}

		if (correctedX < visualBounds.getMinX()) {
			correctedX = visualBounds.getMinX();
		}

		if (correctedY < visualBounds.getMinY()) {
			correctedY = visualBounds.getMinY();
		}

		if (correctedX + correctedWidth > visualBounds.getMaxX()) {
			correctedX = visualBounds.getMaxX() - correctedWidth;
		}

		if (correctedY + correctedHeight > visualBounds.getMaxY()) {
			correctedY = visualBounds.getMaxY() - correctedHeight;
		}

		stage.setX(correctedX);
		stage.setY(correctedY);
		stage.setWidth(correctedWidth);
		stage.setHeight(correctedHeight);
	}

	/**
	 * Resolves the currently selected UI reachability band. Null override means Auto.
	 *
	 * @param member station row
	 * @return manually selected band or automatic band
	 */
	private Band resolveReachabilityBandForUi(ChatMember member) {
		if (selectedReachabilityBandOverride != null) {
			return selectedReachabilityBandOverride;
		}
		return chatcontroller.getReachabilityService().resolveAutoBand(member);
	}

	/**
	 * Parses the reachability ComboBox selection.
	 *
	 * @param selectedLabel selected UI text
	 * @return selected band, or null for Auto
	 */
	private Band parseReachabilityBandSelection(String selectedLabel) {
		if (selectedLabel == null || selectedLabel.isBlank() || "Auto".equalsIgnoreCase(selectedLabel)) {
			return null;
		}

		for (Band band : Band.values()) {
			if (selectedLabel.equalsIgnoreCase(band.getDisplayLabel())) {
				return band;
			}
		}
		return null;
	}

	/**
	 * Formats the projected priority score for the station table.
	 *
	 * @param member station row
	 * @return score text
	 */
	private String formatPriorityScore(ChatMember member) {
		if (member == null || !Double.isFinite(member.getCurrentPriorityScore())) {
			return "-";
		}
		return String.format(Locale.US, "%.0f", member.getCurrentPriorityScore());
	}

	/**
	 * Parses a numeric table value for custom score sorting.
	 *
	 * @param value table text
	 * @return parsed value or negative infinity for missing values
	 */
	private double parseTableDouble(String value) {
		if (value == null || value.isBlank() || value.equals("-") || value.startsWith("- @")) {
			return Double.NEGATIVE_INFINITY;
		}

		String normalized = value.replace(",", ".").replace("+", "").trim();
		int firstSpace = normalized.indexOf(' ');
		if (firstSpace > 0) {
			normalized = normalized.substring(0, firstSpace);
		}

		try {
			return Double.parseDouble(normalized);
		} catch (NumberFormatException ignored) {
			return Double.NEGATIVE_INFINITY;
		}
	}

	/**
	 * Predicate helper for the tropo filter. Pending or failed calculations remain
	 * visible as requested; only finite negative margins are hidden.
	 *
	 * @param member station row
	 * @return true if station should remain visible under the tropo filter
	 */
	private boolean isReachableViaTropoFilterMatch(ChatMember member) {
		if (member == null || chatcontroller.getReachabilityService() == null) {
			return true;
		}

		Band selectedBand = resolveReachabilityBandForUi(member);
		chatcontroller.getReachabilityService().ensureTropoMarginCalculated(member, selectedBand);

		OptionalDouble marginDb = member.getTropoSsbMarginDb(selectedBand);
		if (marginDb.isEmpty() || !Double.isFinite(marginDb.getAsDouble())) {
			return true;
		}

		return marginDb.getAsDouble() >= 0.0;
	}

	/**
	 * Marks that the next ChatMember selection event is caused by direct operator
	 * interaction.
	 *
	 * JavaFX selection events do not reliably tell us whether they were caused by
	 * the user or by a table refresh. Therefore we set this flag on mouse/key input
	 * before the selection model fires.
	 *
	 * Platform.runLater() resets the flag after the current JavaFX event cycle, so a
	 * later periodic refresh cannot accidentally reuse this operator intent.
	 */
	private void markOperatorChatMemberSelectionIntent() {
		operatorInitiatedChatMemberSelectionChange = true;

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				operatorInitiatedChatMemberSelectionChange = false;
			}
		});
	}

	/**
	 * Returns true when a station has any AirScout window now or within maxMinutes.
	 *
	 * @param member station row
	 * @param maxMinutes look-ahead window
	 * @return true if any AS window is available
	 */
	private boolean hasAsWindowInNextMinutes(ChatMember member, int maxMinutes) {
		if (member == null || member.getAirPlaneReflectInfo() == null
				|| member.getAirPlaneReflectInfo().getRisingAirplanes() == null) {
			return false;
		}

		return member.getAirPlaneReflectInfo().getRisingAirplanes().stream()
				.filter(Objects::nonNull)
				.anyMatch(airPlane -> airPlane.getArrivingDurationMinutes() >= 0
						&& airPlane.getArrivingDurationMinutes() <= maxMinutes);
	}

	/**
	 * Returns true when the station is known to be QRV on at least one enabled own band
	 * that has not been worked yet.
	 *
	 * @param member station row
	 * @return true if there is a new-band opportunity
	 */
	private boolean isNewBandOpportunity(ChatMember member) {
		if (member == null || chatcontroller == null) {
			return false;
		}

		List<ChatMember> variants =
				chatcontroller.findActiveChatMembersByRawCall(member.getCallSignRaw());
		if (variants.isEmpty()) {
			variants = List.of(member);
		}

		BandOpportunityResolver.Resolution resolution =
				BandOpportunityResolver.resolve(variants, System.currentTimeMillis());
		EnumSet<Band> enabledBands =
				BandOpportunityResolver.getEnabledStationBands(chatcontroller.getChatPreferences());

		return !resolution.getUnworkedEnabledBands(enabledBands).isEmpty();
	}

}

/**
 * This cell type is used to declare buttons which can be placed in the tableview
 *
 * // source: https://stackoverflow.com/questions/76248808/how-do-i-add-a-button-into-a-jfx-tableview
 */
class ActionButtonTableCell<S, T> extends TableCell<S, T> {
	private final ToggleButton actionButton;

	public ActionButtonTableCell(String label, Consumer<S> function) {
		this.getStyleClass().add("action-button-table-cell");
		this.actionButton = new ToggleButton(label);
		this.actionButton.setOnAction(e -> function.accept(getCurrentItem()));
		this.actionButton.setMaxWidth(Double.MAX_VALUE);
	}
	public S getCurrentItem() {
		// No need for a cast here:
		System.out.println("<<<<<<<<<<<<<<<<<<<<TV Actionbutton pressed");
		return getTableView().getItems().get(getIndex());
	}

	public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(String label, Consumer<S> function) {
		return param -> new ActionButtonTableCell<>(label, function);
	}
	@Override
	public void updateItem(T item, boolean empty) {
		super.updateItem(item, empty);
		if (empty) {
			setGraphic(null);
		} else {
			setGraphic(actionButton);
		}
	}


}



/**
 * This cell type is used to declare buttons which can be placed in the tableview
 *
 * // source: https://stackoverflow.com/questions/76248808/how-do-i-add-a-button-into-a-jfx-tableview
 */
class CheckBoxTableCell<S, T> extends TableCell<S, T> {
	private final CheckBox actionCheckBox;

	public CheckBoxTableCell(String label, Consumer<S> function) {
		this.getStyleClass().add("action-button-table-cell");
		this.actionCheckBox = new CheckBox(label);
		this.actionCheckBox.setOnAction(e -> function.accept(getCurrentItem()));
//		this.actionCheckBox.setMaxWidth(Double.MAX_VALUE);
	}
	public S getCurrentItem() {
		// No need for a cast here:
		System.out.println("<<<<<<<<<<<<<<<<<<<<TV Actionbutton pressed");
		return getTableView().getItems().get(getIndex());
	}

	public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(String label, Consumer<S> function) {
		return param -> new CheckBoxTableCell<>(label, function);
	}

	@Override
	public void updateItem(T item, boolean empty) {
		super.updateItem(item, empty);
		if (empty) {
			setGraphic(null);
		} else {
			setGraphic(actionCheckBox);
		}
	}



}
