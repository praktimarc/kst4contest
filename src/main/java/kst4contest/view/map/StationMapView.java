package kst4contest.view.map;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import kst4contest.view.GuiUtils;
import netscape.javascript.JSObject;
import kst4contest.ApplicationConstants;
import kst4contest.locatorUtils.Location;
import kst4contest.model.ChatPreferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.ColumnConstraints;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;

/**
 * Standalone station map window.
 *
 * Responsibilities:
 * - hosts the JavaFX WebView
 * - renders stations, beam, connection line and Maidenhead grid
 * - shows detail information for the selected callsign
 * - exposes click callbacks to the application bridge
 * - routes critical click/zoom interactions directly from JavaFX when the DOM listeners are unreliable
 */
public final class StationMapView {

    /**
     * Enables verbose map debug output.
     *
     * Keep this false for normal operation because scroll and viewport logs are very
     * noisy during map interaction.
     */
    private static final boolean MAP_DEBUG_LOGGING = false;

    private static final double MINIMUM_HEIGHT_WITH_PATH_ANALYSIS = 650.0;
    private static final double MINIMUM_HEIGHT_WITHOUT_PATH_ANALYSIS = 420.0;

    private final PathProfileChart detailPathProfileChart = new PathProfileChart();
    private final Label detailPathModeValue = new Label("-");

    private final ChatPreferences chatPreferences;

    private final Stage stage = new Stage();
    private final WebView webView = new WebView();
    private final WebEngine webEngine = webView.getEngine();

    /**
     * Keep a strong Java reference to the bridge object.
     *
     * JavaFX WebView/JSObject does not guarantee that a Java object passed through
     * window.setMember(...) remains strongly reachable from the Java side. Newer
     * JavaFX/WebKit/GC combinations can otherwise lose callbacks after a while.
     */
    private final JavaMapBridge javaMapBridge = new JavaMapBridge();


    private TileProxyServer tileProxyServer;

    private Scene scene;
    private BorderPane rootPane;
    private SplitPane mainSplitPane;
    private VBox detailPane;

    private final Label statusLabel = new Label("Station map not initialized yet.");

    private final Label pathAnalysisHiddenHintLabel = new Label("Path analysis is hidden.");
    private final Button pathAnalysisVisibilityButton = new Button();
    private final Tooltip pathAnalysisVisibilityTooltip = new Tooltip();

    private final Button resetViewButton = new Button("Reset view");
    private final Tooltip statusTooltip = new Tooltip();

    private Runnable onResetView;

    private double lastDetailDividerPosition = 0.65;



    private final Button triggerClusterSpotButton = new Button("Trigger cluster spot");

    private final Label detailPathFromLocatorValue = new Label("-");
    private final Label detailPathToLocatorValue = new Label("-");
    private final Label detailPathDistanceValue = new Label("-");
    private final Label detailPathBearingValue = new Label("-");
    private final Label detailPathEndpointsValue = new Label("-");
    private final Label detailPathStatusValue = new Label("No station selected.");

    private String homeLocator6 = "";


    private Consumer<String> onCallsignRawSelected;
    private Consumer<String> onTriggerClusterSpot;

    private boolean mapReady;
    private boolean homeViewInitialized;

    private List<MapCallsignRawSnapshot> lastSnapshots = List.of();
    private MapCallsignRawSnapshot lastSelectedSnapshot;
    private boolean filteredViewActive;
    /**
     * Last JSON payloads sent into Leaflet. Used to avoid clearing/recreating
     * layers every score/table refresh. Rebuilding DOM markers every few seconds
     * is visible as flicker in JavaFX 21 WebView.
     */
    private String lastRenderedStationsJson = "";
    private String lastRenderedBeamJson = "";
    private String lastRenderedConnectionJson = "";
    private String lastRenderedGridJson = "";

    /**
     * Prevent periodic refreshes from panning the map back to the selected station.
     * Explicit calls to focusCallsignRaw(...) still pan immediately.
     */
    private String lastAutoFocusedCallsignRaw = "";


    private double homeLatitudeDeg = Double.NaN;
    private double homeLongitudeDeg = Double.NaN;
    private double antennaAzimuthDeg;
    private double beamWidthDeg;
    private double maxQrbKm;

    private double viewportSouthLat = Double.NaN;
    private double viewportWestLon = Double.NaN;
    private double viewportNorthLat = Double.NaN;
    private double viewportEastLon = Double.NaN;
    private int viewportZoom = 6;

    private String detailCallsignRaw;

    private PathAnalysisResult lastPathAnalysisResult = PathAnalysisResult.waitingForSelection("");

    private VBox mapAndProfilePane;
    private VBox profileSection;
    private VBox pathAnalysisSection;
    private ScrollPane detailScrollPane;



    private final Label detailPathLosValue = new Label("-");
    private final Label detailPathWorstClearanceValue = new Label("-");

    private final Label detailPathSamplesValue = new Label("-");

    private final Label detailPathFrequencyValue = new Label("-");
    private final Label detailPathRefractionValue = new Label("-");
    private final Label detailPathHorizonValue = new Label("-");
    private final Label detailPathTerrainHorizonValue = new Label("-");
    private final Label detailPathFresnelValue = new Label("-");
    private final Label detailPathWorstFresnelValue = new Label("-");

    private final Label detailPathObstructionValue = new Label("-");

    private final Label detailPathAssessmentValue = new Label("-");
    private final Label detailPathMechanismsValue = new Label("-");

    private final Label detailPathLinkBudgetValue = new Label("-");
    private final Label detailPathRxPowerValue = new Label("-");
    private final Label detailPathCwHintValue = new Label("-");


    public StationMapView(ChatPreferences chatPreferences) {
        this.chatPreferences = Objects.requireNonNull(chatPreferences, "chatPreferences");
        GuiUtils.applyApplicationIcon(stage);

        try {
            tileProxyServer = new TileProxyServer();
        } catch (IOException e) {
            System.err.println("[StationMap] tile proxy failed to start: " + e.getMessage());
        }
        initializeUi();
        initializeWebView();
    }

    public void setPathAnalysisResult(PathAnalysisResult pathAnalysisResult) {
        this.lastPathAnalysisResult = pathAnalysisResult == null
                ? PathAnalysisResult.waitingForSelection(homeLocator6)
                : pathAnalysisResult;

        updatePathAnalysisPanel(this.lastPathAnalysisResult);

        boolean loading = "Loading".equalsIgnoreCase(this.lastPathAnalysisResult.analysisMode());
        detailPathStatusValue.setStyle(loading ? "-fx-font-style: italic;" : "");
    }

    public void setOnCallsignRawSelected(Consumer<String> onCallsignRawSelected) {
        this.onCallsignRawSelected = onCallsignRawSelected;
    }

    public void setOnTriggerClusterSpot(Consumer<String> onTriggerClusterSpot) {
        this.onTriggerClusterSpot = onTriggerClusterSpot;
    }

    public void setOnResetView(Runnable onResetView) {
        this.onResetView = onResetView;
    }


    public void showWindow() {

        applyThemeFromPreferences();

        if (!stage.isShowing()) {
            stage.show();
        }
        stage.toFront();

        Platform.runLater(() -> {
            webView.requestFocus();
            requestMapInvalidateSize();
        });
    }

    public void hideWindow() {
        stage.hide();
    }

    public boolean isShowing() {
        return stage.isShowing();
    }

    public void applyThemeFromPreferences() {
        boolean darkMode = chatPreferences.isGUI_darkModeActive();
        applySceneTheme(darkMode);
        applyMapThemeToWebView(darkMode);
        detailPathProfileChart.setDarkMode(darkMode);
    }

    public void focusCallsignRaw(String callSignRaw) {
        if (!mapReady || callSignRaw == null || callSignRaw.isBlank()) {
            return;
        }

        String normalizedCallsignRaw = callSignRaw.trim().toUpperCase(Locale.ROOT);
        lastAutoFocusedCallsignRaw = normalizedCallsignRaw;

        executeMapScriptSafely(
                "window.kstMapApi.focusCallsignRaw(" + toJsStringLiteral(normalizedCallsignRaw) + ");"
        );
    }

    public void refreshMap(List<MapCallsignRawSnapshot> snapshots,
                           MapCallsignRawSnapshot selectedSnapshot,
                           String ownLocator6,
                           double antennaAzimuthDeg,
                           double beamWidthDeg,
                           double maxQrbKm,
                           boolean filteredViewActive) {

        this.lastSnapshots = snapshots == null ? List.of() : List.copyOf(snapshots);
        this.lastSelectedSnapshot = selectedSnapshot;
        this.antennaAzimuthDeg = antennaAzimuthDeg;
        this.beamWidthDeg = beamWidthDeg;
        this.maxQrbKm = maxQrbKm;
        this.filteredViewActive = filteredViewActive;

        this.homeLocator6 = normalizeLocator6(ownLocator6);
        updateHomeLocationFromOwnLocator(this.homeLocator6);
        updateStatusLabel();
        updateDetailPanel(selectedSnapshot);

        /*
         * The detail pane is useful only while path analysis is enabled and a
         * concrete station is selected. Reset view therefore removes the pane
         * instead of leaving an empty analysis area beside the map.
         */
        updateDetailPanePresence(
                profileSection.isVisible() && selectedSnapshot != null
        );

        if (mapReady) {
            renderAll();
        }
    }

    private void initializeUi() {

        stage.setTitle("Station Map");

        detailPathEndpointsValue.setWrapText(true);
        detailPathEndpointsValue.setMaxWidth(Double.MAX_VALUE);

        detailPathStatusValue.setWrapText(true);
        detailPathStatusValue.setMaxWidth(Double.MAX_VALUE);

        detailPathAssessmentValue.setWrapText(true);
        detailPathAssessmentValue.setMaxWidth(Double.MAX_VALUE);

        detailPathMechanismsValue.setWrapText(true);
        detailPathMechanismsValue.setMaxWidth(Double.MAX_VALUE);

        triggerClusterSpotButton.setDisable(true);
        triggerClusterSpotButton.setVisible(false);
        triggerClusterSpotButton.setManaged(false);
        triggerClusterSpotButton.setMinWidth(Region.USE_PREF_SIZE);

        triggerClusterSpotButton.setOnAction(event -> {
            if (detailCallsignRaw != null && onTriggerClusterSpot != null) {
                onTriggerClusterSpot.accept(detailCallsignRaw);
            }
        });

        resetViewButton.setMinWidth(Region.USE_PREF_SIZE);
        resetViewButton.setOnAction(event -> {
            if (onResetView != null) {
                onResetView.run();
            }
        });

        pathAnalysisVisibilityButton.setMinWidth(Region.USE_PREF_SIZE);
        pathAnalysisVisibilityButton.setTooltip(pathAnalysisVisibilityTooltip);
        pathAnalysisVisibilityButton.setOnAction(event ->
                setPathAnalysisVisible(!profileSection.isVisible(), true));

        pathAnalysisHiddenHintLabel.setMinWidth(Region.USE_PREF_SIZE);
        pathAnalysisHiddenHintLabel.setStyle("-fx-font-style: italic; -fx-opacity: 0.85;");

        webView.setFocusTraversable(true);
        webView.setPickOnBounds(true);

        webView.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> logWebViewMouseEvent("MOUSE_PRESSED", event));
        webView.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> logWebViewMouseEvent("MOUSE_RELEASED", event));
        webView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleWebViewClick(event));

        webView.addEventHandler(ScrollEvent.SCROLL, event -> {

            if (MAP_DEBUG_LOGGING) {
                System.out.println("[StationMap FX] SCROLL x=" + (int) event.getX()
                        + " y=" + (int) event.getY()
                        + " deltaY=" + event.getDeltaY());
            }

            InteractiveTarget target = inspectInteractiveTarget(event.getX(), event.getY());
            if (MAP_DEBUG_LOGGING) {
                System.out.println("[StationMap FX] inspect scroll -> " + target);
            }

            if (event.getDeltaY() > 0) {
                executeMapScriptSafely("window.kstMapApi.zoomIn();");
                requestViewportPullFromJs();
            } else if (event.getDeltaY() < 0) {
                executeMapScriptSafely("window.kstMapApi.zoomOut();");
                requestViewportPullFromJs();
            }

            event.consume();

            event.consume();
        });

        webView.widthProperty().addListener((obs, oldValue, newValue) -> requestMapInvalidateSize());
        webView.heightProperty().addListener((obs, oldValue, newValue) -> requestMapInvalidateSize());

        profileSection = createProfileSection();


        mapAndProfilePane = new VBox(6, webView, profileSection);
        mapAndProfilePane.setPadding(new Insets(0));

// Important: allow the SplitPane to shrink the map side.
        mapAndProfilePane.setMinWidth(0);
        mapAndProfilePane.setMinHeight(0);

        webView.setMinWidth(0);
        webView.setMinHeight(220);
        webView.setPrefHeight(420);
        webView.setMaxWidth(Double.MAX_VALUE);
        webView.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(webView, Priority.ALWAYS);

        profileSection.setMinWidth(0);
        profileSection.setMinHeight(210);
        profileSection.setPrefHeight(260);
        VBox.setVgrow(profileSection, Priority.NEVER);

        detailPathProfileChart.widthProperty().unbind();
        detailPathProfileChart.widthProperty().bind(
                mapAndProfilePane.widthProperty().subtract(20)
        );

        pathAnalysisSection = createPathAnalysisSection();
        detailPane = new VBox(10, pathAnalysisSection);

        detailPane.setPadding(new Insets(10));

        detailPane.setMinWidth(0);
        detailPane.setPrefWidth(340);
        detailPane.setMaxWidth(Double.MAX_VALUE);

        detailScrollPane = new ScrollPane(detailPane);

        detailScrollPane.setFitToWidth(true);
        detailScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        /*
         * Allow the details pane to be reduced far enough to leave more room for the
         * map. At its minimum width, a callsign with up to ten characters remains
         * readable.
         */
        detailScrollPane.setMinWidth(210);
        detailScrollPane.setPrefWidth(350);

        detailScrollPane.setMaxWidth(Double.MAX_VALUE);

        detailScrollPane.setFitToWidth(true);
        detailScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);


        mainSplitPane = new SplitPane(mapAndProfilePane, detailScrollPane);
        mainSplitPane.setDividerPositions(0.65);
        SplitPane.setResizableWithParent(detailScrollPane, true);

        rootPane = new BorderPane();
        rootPane.setTop(createMapHeader());
        rootPane.setCenter(mainSplitPane);

        double[] size = chatPreferences.getGUIstationMapStageSceneSizeHW();

        double initialWidth = resolveInitialStationMapWidth(size);
        double initialHeight = resolveInitialStationMapHeight(size);

        scene = new Scene(rootPane, initialWidth, initialHeight);

        stage.setMinWidth(900);
        stage.setMinHeight(resolveMinimumStationMapHeight(
                chatPreferences.isGUIstationMapPathAnalysisVisible()));

        stage.setScene(scene);
        setPathAnalysisVisible(chatPreferences.isGUIstationMapPathAnalysisVisible(), false);
        applyThemeFromPreferences();

        stage.setScene(scene);
        applyThemeFromPreferences();
        detailPathProfileChart.setOnProfilePointHovered(this::showProfileHoverPointOnMap);


        double[] pos = chatPreferences.getGUIstationMapStagePositionXY();
        if (pos.length >= 2 && !Double.isNaN(pos[0]) && !Double.isNaN(pos[1])) {
            stage.setX(pos[0]);
            stage.setY(pos[1]);
        }

        stage.widthProperty().addListener((obs, oldValue, newValue) ->
                chatPreferences.getGUIstationMapStageSceneSizeHW()[0] = newValue.doubleValue());

        stage.heightProperty().addListener((obs, oldValue, newValue) ->
                chatPreferences.getGUIstationMapStageSceneSizeHW()[1] = newValue.doubleValue());

        stage.xProperty().addListener((obs, oldValue, newValue) ->
                chatPreferences.getGUIstationMapStagePositionXY()[0] = newValue.doubleValue());

        stage.yProperty().addListener((obs, oldValue, newValue) ->
                chatPreferences.getGUIstationMapStagePositionXY()[1] = newValue.doubleValue());

        stage.setOnShown(event -> Platform.runLater(() -> {
            webView.requestFocus();
            requestMapInvalidateSize();
        }));
        detailPathProfileChart.setDarkMode(chatPreferences.isGUI_darkModeActive());

        makePathValueLabel(detailPathModeValue);
        makePathValueLabel(detailPathSamplesValue);
        makePathValueLabel(detailPathFromLocatorValue);
        makePathValueLabel(detailPathToLocatorValue);
        makePathValueLabel(detailPathDistanceValue);
        makePathValueLabel(detailPathBearingValue);
        makePathValueLabel(detailPathEndpointsValue);
        makePathValueLabel(detailPathFrequencyValue);
        makePathValueLabel(detailPathRefractionValue);
        makePathValueLabel(detailPathHorizonValue);
        makePathValueLabel(detailPathTerrainHorizonValue);
        makePathValueLabel(detailPathFresnelValue);
        makePathValueLabel(detailPathWorstFresnelValue);
        makePathValueLabel(detailPathLosValue);
        makePathValueLabel(detailPathWorstClearanceValue);
        makePathValueLabel(detailPathObstructionValue);
        makePathValueLabel(detailPathAssessmentValue);
        makePathValueLabel(detailPathMechanismsValue);
        makePathValueLabel(detailPathLinkBudgetValue);
        makePathValueLabel(detailPathRxPowerValue);
        makePathValueLabel(detailPathCwHintValue);
        makePathValueLabel(detailPathStatusValue);

    }

    /**
     * Creates an always-visible header for the map status and analysis controls.
     *
     * Keeping the control outside the sections that it hides is important: users
     * must always have an obvious way to restore a previously hidden analysis.
     */
    private HBox createMapHeader() {
        statusLabel.setMinWidth(0);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        statusLabel.setTooltip(statusTooltip);

        HBox header = new HBox(
                10,
                statusLabel,
                triggerClusterSpotButton,
                resetViewButton,
                pathAnalysisHiddenHintLabel,
                pathAnalysisVisibilityButton
        );

        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8));

        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        return header;
    }

    /**
     * Shows or hides both parts of the path analysis as one logical feature.
     *
     * Both visible and managed must be changed. A node that is merely invisible
     * would still reserve layout space and the map would not grow into that area.
     * The current analysis result remains attached to the controls and is
     * immediately available again when the user restores the sections.
     *
     * @param visible true to show the profile and detailed analysis
     * @param persist true when the change was explicitly requested by the user
     */
    private void setPathAnalysisVisible(boolean visible, boolean persist) {
        profileSection.setVisible(visible);
        profileSection.setManaged(visible);

        pathAnalysisSection.setVisible(visible);
        pathAnalysisSection.setManaged(visible);

        /*
         * Showing path analysis must not create an empty detail pane when no
         * station is selected. The pane is restored automatically with the next
         * valid station selection.
         */
        updateDetailPanePresence(
                visible && lastSelectedSnapshot != null
        );

        pathAnalysisHiddenHintLabel.setVisible(!visible);
        pathAnalysisHiddenHintLabel.setManaged(!visible);

        pathAnalysisVisibilityButton.setText(
                visible ? "Hide path analysis" : "Show path analysis");
        pathAnalysisVisibilityButton.setAccessibleText(
                visible ? "Hide path analysis" : "Show path analysis");
        pathAnalysisVisibilityButton.setStyle(
                visible ? "" : "-fx-font-weight: bold;");

        pathAnalysisVisibilityTooltip.setText(visible
                ? "Hide the path profile and detailed path analysis. You can show them again at any time."
                : "Show the path profile and detailed path analysis.");

        pathAnalysisVisibilityButton.setAccessibleHelp(
                pathAnalysisVisibilityTooltip.getText());

        if (!visible) {
            /*
             * Do not leave a profile hover marker on the map after its chart was
             * hidden.
             */
            showProfileHoverPointOnMap(null);
        }

        stage.setMinHeight(resolveMinimumStationMapHeight(visible));

        if (persist) {
            chatPreferences.setGUIstationMapPathAnalysisVisible(visible);
        }

        if (rootPane != null) {
            rootPane.requestLayout();
            Platform.runLater(this::requestMapInvalidateSize);
        }
    }

    private double resolveMinimumStationMapHeight(boolean pathAnalysisVisible) {
        return pathAnalysisVisible
                ? MINIMUM_HEIGHT_WITH_PATH_ANALYSIS
                : MINIMUM_HEIGHT_WITHOUT_PATH_ANALYSIS;
    }


    /**
     * ensures that the labels remains visible
     * @param gridPane
     */
    private void configureCompactGrid(GridPane gridPane) {
        gridPane.getColumnConstraints().clear();

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(70);
        labelColumn.setPrefWidth(115);
        labelColumn.setMaxWidth(130);
        labelColumn.setHgrow(Priority.NEVER);

        ColumnConstraints valueColumn = new ColumnConstraints();

        /*
         * Reserve enough space for a callsign with up to ten characters, while still
         * allowing the details pane to become considerably narrower.
         */
        valueColumn.setMinWidth(85);
        valueColumn.setHgrow(Priority.ALWAYS);

        gridPane.getColumnConstraints().addAll(labelColumn, valueColumn);
    }

    private VBox createPathAnalysisSection() {
        GridPane pathGrid = new GridPane();
        pathGrid.setHgap(10);
        pathGrid.setVgap(8);
        configureCompactGrid(pathGrid);

        int row = 0;
        pathGrid.add(new Label("From locator:"), 0, row);
        pathGrid.add(detailPathFromLocatorValue, 1, row++);

        pathGrid.add(new Label("To locator:"), 0, row);
        pathGrid.add(detailPathToLocatorValue, 1, row++);

        Label distanceBearingValue = new Label();
        distanceBearingValue.textProperty().bind(
                detailPathDistanceValue.textProperty()
                        .concat(" / ")
                        .concat(detailPathBearingValue.textProperty())
        );
        distanceBearingValue.setWrapText(true);

        pathGrid.add(new Label("Distance/QTF:"), 0, row);
        pathGrid.add(distanceBearingValue, 1, row++);



        pathGrid.add(new Label("Endpoints:"), 0, row);
        pathGrid.add(detailPathEndpointsValue, 1, row++);


        Label sourceValue = new Label();
        sourceValue.textProperty().bind(
                detailPathModeValue.textProperty()
                        .concat(" / ")
                        .concat(detailPathSamplesValue.textProperty())
                        .concat(" samples")
        );
        sourceValue.setWrapText(true);

        pathGrid.add(new Label("Source:"), 0, row);
        pathGrid.add(sourceValue, 1, row++);

        pathGrid.add(new Label("Frequency:"), 0, row);
        pathGrid.add(detailPathFrequencyValue, 1, row++);

        pathGrid.add(new Label("Refraction:"), 0, row);
        pathGrid.add(detailPathRefractionValue, 1, row++);

        pathGrid.add(new Label("Radio horizon:"), 0, row);
        pathGrid.add(detailPathHorizonValue, 1, row++);

        pathGrid.add(new Label("Terrain horizon:"), 0, row);
        pathGrid.add(detailPathTerrainHorizonValue, 1, row++);

        Label fresnelCombinedValue = new Label();
        fresnelCombinedValue.textProperty().bind(
                detailPathFresnelValue.textProperty()
                        .concat(" / ")
                        .concat(detailPathWorstFresnelValue.textProperty())
        );
        fresnelCombinedValue.setWrapText(true);

        pathGrid.add(new Label("Fresnel:"), 0, row);
        pathGrid.add(fresnelCombinedValue, 1, row++);

        pathGrid.add(new Label("Obstruction:"), 0, row);
        pathGrid.add(detailPathObstructionValue, 1, row++);


        pathGrid.add(new Label("Assessment:"), 0, row);
        pathGrid.add(detailPathAssessmentValue, 1, row++);

        pathGrid.add(new Label("Link budget:"), 0, row);
        pathGrid.add(detailPathLinkBudgetValue, 1, row++);

        pathGrid.add(new Label("RX power:"), 0, row);
        pathGrid.add(detailPathRxPowerValue, 1, row++);

        pathGrid.add(new Label("CW hint:"), 0, row);
        pathGrid.add(detailPathCwHintValue, 1, row++);

        pathGrid.add(new Label("Mechanisms:"), 0, row);
        pathGrid.add(detailPathMechanismsValue, 1, row++);

        Label losClearanceValue = new Label();
        losClearanceValue.textProperty().bind(
                detailPathLosValue.textProperty()
                        .concat(" / worst ")
                        .concat(detailPathWorstClearanceValue.textProperty())
        );
        losClearanceValue.setWrapText(true);

        pathGrid.add(new Label("LOS:"), 0, row);
        pathGrid.add(losClearanceValue, 1, row++);

        pathGrid.add(new Label("Status:"), 0, row);
        pathGrid.add(detailPathStatusValue, 1, row++);

        return new VBox(10,
                new Label("Path / terrain analysis"),
                new Separator(Orientation.HORIZONTAL),
                pathGrid
        );
    }

    private void requestViewportPullFromJs() {
        if (!mapReady) {
            return;
        }

        Platform.runLater(() ->
                Platform.runLater(this::pullViewportFromJsAndRedrawGrid));
    }

    private void pullViewportFromJsAndRedrawGrid() {
        if (!mapReady) {
            return;
        }

        try {
            Object result = webEngine.executeScript("window.kstMapApi.getViewportState();");
            if (result == null) {
                System.err.println("[StationMap FX] getViewportState returned null");
                return;
            }

            String raw = result.toString();
            if (raw.isBlank()) {
                System.err.println("[StationMap FX] getViewportState returned blank");
                return;
            }

            String[] parts = raw.split("\\|");
            if (parts.length != 5) {
                System.err.println("[StationMap FX] getViewportState unexpected format: " + raw);
                return;
            }

            viewportSouthLat = Double.parseDouble(parts[0]);
            viewportWestLon = Double.parseDouble(parts[1]);
            viewportNorthLat = Double.parseDouble(parts[2]);
            viewportEastLon = Double.parseDouble(parts[3]);
            viewportZoom = (int) Math.round(Double.parseDouble(parts[4]));

            System.out.println("[StationMap FX] pulled viewport south=" + viewportSouthLat
                    + " west=" + viewportWestLon
                    + " north=" + viewportNorthLat
                    + " east=" + viewportEastLon
                    + " zoom=" + viewportZoom);

            renderGridIfViewportKnown();
        } catch (Exception exception) {
            System.err.println("[StationMap FX] pullViewportFromJsAndRedrawGrid failed: " + exception.getMessage());
        }
    }

    private void initializeWebView() {
        webView.setContextMenuEnabled(false);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaMapBridge", javaMapBridge);

                executeMapScriptSafely("window.kstMapApi.init();");

                mapReady = true;
                applyMapThemeToWebView(chatPreferences.isGUI_darkModeActive());
                renderAll();
                requestMapInvalidateSize();
            }
        });

        int proxyPort = tileProxyServer != null ? tileProxyServer.getPort() : 0;
        webEngine.loadContent(MapHtmlResources.createStationMapHtml(proxyPort));
    }

    private void requestMapInvalidateSize() {
        if (!mapReady) {
            return;
        }

        Platform.runLater(() -> Platform.runLater(() ->
                executeMapScriptSafely("window.kstMapApi.resize();")
        ));
    }



    private void handleWebViewClick(MouseEvent event) {
        logWebViewMouseEvent("MOUSE_CLICKED", event);

        InteractiveTarget target = inspectInteractiveTarget(event.getX(), event.getY());
        System.out.println("[StationMap FX] inspect click -> " + target);

        switch (target.kind()) {
            case STATION -> {
                if (target.callSignRaw() != null && !target.callSignRaw().isBlank() && onCallsignRawSelected != null) {
                    System.out.println("[StationMap FX] selecting station " + target.callSignRaw());
                    onCallsignRawSelected.accept(target.callSignRaw());
                    event.consume();
                }
            }
            case ZOOM_IN -> {
                System.out.println("[StationMap FX] zoom in click routed by JavaFX");
                executeMapScriptSafely("window.kstMapApi.zoomIn();");
                requestViewportPullFromJs();
                event.consume();
            }
            case ZOOM_OUT -> {
                System.out.println("[StationMap FX] zoom out click routed by JavaFX");
                executeMapScriptSafely("window.kstMapApi.zoomOut();");
                requestViewportPullFromJs();
                event.consume();
            }

            case NONE -> {
                // Let normal WebView processing continue.
            }
        }
    }

    private void logWebViewMouseEvent(String type, MouseEvent event) {
        System.out.println("[StationMap FX] " + type
                + " x=" + (int) event.getX()
                + " y=" + (int) event.getY()
                + " target=" + event.getTarget().getClass().getSimpleName()
                + " button=" + event.getButton());

        probeDomElementAt(event.getX(), event.getY());
    }

    private void probeDomElementAt(double x, double y) {
        if (!mapReady) {
            return;
        }

        String script = String.format(Locale.US, """
                (function() {
                    var el = document.elementFromPoint(%f, %f);
                    if (!el) {
                        return "null";
                    }
                    var cls = "";
                    try {
                        cls = el.className ? el.className.toString() : "";
                    } catch (e) {
                        cls = "[className-error]";
                    }
                    var id = el.id ? el.id.toString() : "";
                    var text = el.textContent ? el.textContent.trim() : "";
                    if (text.length > 80) {
                        text = text.substring(0, 80);
                    }
                    return "tag=" + el.tagName + " class=" + cls + " id=" + id + " text=" + text;
                })();
                """, x, y);

        try {
            Object result = webEngine.executeScript(script);
            System.out.println("[StationMap FX] elementFromPoint -> " + result);
        } catch (Exception exception) {
            System.err.println("[StationMap FX] elementFromPoint failed: " + exception.getMessage());
        }
    }

    private InteractiveTarget inspectInteractiveTarget(double x, double y) {
        if (!mapReady) {
            return InteractiveTarget.none();
        }

        String script = String.format(Locale.US, """
                window.kstMapApi.inspectPoint(%f, %f);
                """, x, y);

        try {
            Object result = webEngine.executeScript(script);
            if (result == null) {
                return InteractiveTarget.none();
            }

            String raw = result.toString();

            String[] parts = raw.split("\\|", 5);

            String kind = parts.length > 0 ? parts[0] : "none";
            String callSignRaw = parts.length > 1 ? parts[1] : "";
            String tag = parts.length > 2 ? parts[2] : "";
            String cssClass = parts.length > 3 ? parts[3] : "";
            String text = parts.length > 4 ? parts[4] : "";

            return switch (kind) {
                case "station" -> new InteractiveTarget(InteractiveKind.STATION, callSignRaw, tag, cssClass, text);
                case "zoomIn" -> new InteractiveTarget(InteractiveKind.ZOOM_IN, "", tag, cssClass, text);
                case "zoomOut" -> new InteractiveTarget(InteractiveKind.ZOOM_OUT, "", tag, cssClass, text);
                default -> new InteractiveTarget(InteractiveKind.NONE, "", tag, cssClass, text);
            };
        } catch (Exception exception) {
            System.err.println("[StationMap FX] inspectInteractiveTarget failed: " + exception.getMessage());
            return InteractiveTarget.none();
        }
    }

    private void updateHomeLocationFromOwnLocator(String ownLocator6) {
        String normalizedLocator = normalizeLocator6(ownLocator6);
        if (normalizedLocator.isBlank()) {
            homeLatitudeDeg = Double.NaN;
            homeLongitudeDeg = Double.NaN;
            return;
        }

        Location homeLocation = new Location(normalizedLocator);
        homeLatitudeDeg = homeLocation.getLatitude().toDegrees();
        homeLongitudeDeg = homeLocation.getLongitude().toDegrees();
    }

    private void updateStatusLabel() {
        StringBuilder text = new StringBuilder();

        text.append("Showing ")
                .append(lastSnapshots.size())
                .append(" visible stations");

        if (filteredViewActive) {
            text.append(" | filtered view active");
        }

        MapCallsignRawSnapshot selectedSnapshot = lastSelectedSnapshot;

        if (selectedSnapshot != null) {
            text.append(" | Selected: ")
                    .append(selectedSnapshot.displayCallSign());

            if (!selectedSnapshot.locator6().isBlank()) {
                text.append(" | ")
                        .append(selectedSnapshot.locator6());
            }

            text.append(" | ")
                    .append(String.format(
                            Locale.US,
                            "%.0f km / %.0f°",
                            selectedSnapshot.qrbKm(),
                            selectedSnapshot.qtfDeg()
                    ));

            String bandText = selectedSnapshot.bandSummary().isBlank()
                    ? "-"
                    : selectedSnapshot.bandSummary();

            if (selectedSnapshot.offersSelectedBand()) {
                bandText += " B+";
            }

            text.append(" | Bands: ")
                    .append(bandText);

            String frequencies = selectedSnapshot.detailFrequencyText();

            if (frequencies != null && !frequencies.isBlank()) {
                frequencies = frequencies
                        .replace('\n', ' ')
                        .replace('\r', ' ')
                        .replaceAll("\\s+", " ")
                        .trim();

                text.append(" | QRG: ")
                        .append(frequencies);
            }
        }

        String statusText = text.toString();

        statusLabel.setText(statusText);
        statusTooltip.setText(statusText);
    }

    private void updateDetailPanel(MapCallsignRawSnapshot selectedSnapshot) {
        if (selectedSnapshot == null) {
            detailCallsignRaw = null;

            triggerClusterSpotButton.setDisable(true);
            triggerClusterSpotButton.setVisible(false);
            triggerClusterSpotButton.setManaged(false);

            clearPathAnalysisPanel();
            return;
        }

        detailCallsignRaw = selectedSnapshot.callSignRaw();

        triggerClusterSpotButton.setDisable(false);
        triggerClusterSpotButton.setVisible(true);
        triggerClusterSpotButton.setManaged(true);

        updatePathAnalysisPanel(lastPathAnalysisResult);
    }

    private void updateDetailPanePresence(boolean visible) {
        if (mainSplitPane == null || detailScrollPane == null) {
            return;
        }

        if (visible) {
            if (!mainSplitPane.getItems().contains(detailScrollPane)) {
                mainSplitPane.getItems().add(detailScrollPane);

                Platform.runLater(() ->
                        mainSplitPane.setDividerPositions(lastDetailDividerPosition)
                );
            }
        } else {
            if (!mainSplitPane.getDividers().isEmpty()) {
                lastDetailDividerPosition =
                        mainSplitPane.getDividers().get(0).getPosition();
            }

            mainSplitPane.getItems().remove(detailScrollPane);
        }
    }

    private void clearPathAnalysisPanel() {
        detailPathFromLocatorValue.setText(homeLocator6.isBlank() ? "-" : homeLocator6);
        detailPathToLocatorValue.setText("-");
        detailPathDistanceValue.setText("-");
        detailPathBearingValue.setText("-");
        detailPathEndpointsValue.setText("-");
        detailPathStatusValue.setText("Select a station to prepare path and terrain analysis.");
        detailPathModeValue.setText("-");
        detailPathSamplesValue.setText("-");
        detailPathLosValue.setText("-");
        detailPathWorstClearanceValue.setText("-");


        detailPathFrequencyValue.setText("-");
        detailPathRefractionValue.setText("-");
        detailPathHorizonValue.setText("-");
        detailPathFresnelValue.setText("-");
        detailPathWorstFresnelValue.setText("-");
        detailPathTerrainHorizonValue.setText("-");
        detailPathObstructionValue.setText("-");
        detailPathAssessmentValue.setText("-");
        detailPathMechanismsValue.setText("-");

        detailPathLinkBudgetValue.setText("-");
        detailPathRxPowerValue.setText("-");
        detailPathCwHintValue.setText("-");

        detailPathProfileChart.setObstructionSummary(PathObstructionSummary.empty());


        detailPathProfileChart.setProfile(List.of(), Double.NaN);
        detailPathProfileChart.setRadioPath(
                Double.NaN,
                Double.NaN,
                Double.NaN
        );
        detailPathProfileChart.setHorizonSummary(PathHorizonSummary.empty());
        applyPropagationAssessmentStyle(PathAnalysisResult.waitingForSelection(homeLocator6));
    }

    private void updatePathAnalysisPanel(PathAnalysisResult result) {

        detailPathModeValue.setText(
                result.analysisMode().isBlank() ? "-" : result.analysisMode()
        );
        detailPathSamplesValue.setText(String.valueOf(result.profilePoints().size()));
        detailPathLosValue.setText(result.losText());
        detailPathWorstClearanceValue.setText(result.worstClearanceText());


        detailPathFrequencyValue.setText(result.analysisFrequencyText());
        detailPathRefractionValue.setText(result.effectiveEarthRadiusText());
        detailPathHorizonValue.setText(result.radioHorizonText());
        detailPathTerrainHorizonValue.setText(result.terrainHorizonText());
        detailPathFresnelValue.setText(result.fresnelText());
        detailPathWorstFresnelValue.setText(result.worstFresnelClearanceText());
        detailPathObstructionValue.setText(result.obstructionText());

        detailPathAssessmentValue.setText(result.propagationAssessmentText());
        detailPathMechanismsValue.setText(result.propagationMechanismsText());
        applyPropagationAssessmentStyle(result);

        detailPathFromLocatorValue.setText(result.fromLocator6().isBlank() ? "-" : result.fromLocator6());
        detailPathToLocatorValue.setText(result.toLocator6().isBlank() ? "-" : result.toLocator6());
        detailPathDistanceValue.setText(result.distanceText());
        detailPathBearingValue.setText(result.bearingText());
        detailPathEndpointsValue.setText(result.endpointSummaryText());
        detailPathStatusValue.setText(result.statusText());
        detailPathProfileChart.setProfile(result.profilePoints(), result.distanceKm());

        detailPathLinkBudgetValue.setText(result.linkBudgetText());
        detailPathRxPowerValue.setText(result.linkBudgetRxPowerText());
        detailPathCwHintValue.setText(result.cwHintText());

        detailPathProfileChart.setRadioPath(
                result.homeAntennaHeightMeters(),
                result.targetAntennaHeightMeters(),
                result.analysisFrequencyMHz()
        );
        detailPathProfileChart.setHorizonSummary(result.horizonSummary());
        detailPathProfileChart.setObstructionSummary(result.obstructionSummary());
    }

    private void renderAll() {
        if (!mapReady) {
            return;
        }

        if (!homeViewInitialized && Double.isFinite(homeLatitudeDeg) && Double.isFinite(homeLongitudeDeg)) {
            executeMapScriptSafely(
                    "window.kstMapApi.setHome(" + formatDouble(homeLatitudeDeg) + ", "
                            + formatDouble(homeLongitudeDeg) + ", 6);"
            );
            homeViewInitialized = true;
        }

        renderStations();
        renderBeam();
        renderConnectionLine();
        renderGridIfViewportKnown();
        focusSelectedStationOnlyWhenSelectionChanged();
    }

    private void focusSelectedStationOnlyWhenSelectionChanged() {
        if (lastSelectedSnapshot == null || lastSelectedSnapshot.callSignRaw() == null) {
            lastAutoFocusedCallsignRaw = "";
            return;
        }

        String selectedCallsignRaw = lastSelectedSnapshot.callSignRaw().trim().toUpperCase(Locale.ROOT);
        if (selectedCallsignRaw.isBlank() || selectedCallsignRaw.equals(lastAutoFocusedCallsignRaw)) {
            return;
        }

        focusCallsignRaw(selectedCallsignRaw);
    }

    private void renderStations() {
        String stationsJson = toStationsJson(lastSnapshots);
        if (stationsJson.equals(lastRenderedStationsJson)) {
            return;
        }

        lastRenderedStationsJson = stationsJson;

        executeMapScriptSafely(
                "window.kstMapApi.setStations(" + toJsStringLiteral(stationsJson) + ");"
        );
    }

    private void renderBeam() {
        String beamJson = "null";

        if (Double.isFinite(homeLatitudeDeg)
                && Double.isFinite(homeLongitudeDeg)
                && beamWidthDeg > 0.0
                && maxQrbKm > 0.0) {

            List<double[]> sectorPoints = buildBeamPolygon(homeLatitudeDeg, homeLongitudeDeg, antennaAzimuthDeg, beamWidthDeg, maxQrbKm);
            beamJson = toPointArrayJson(sectorPoints);
        }

        if (beamJson.equals(lastRenderedBeamJson)) {
            return;
        }
        lastRenderedBeamJson = beamJson;

        executeMapScriptSafely(
                "window.kstMapApi.setBeam(" + toJsStringLiteral(beamJson) + ");"
        );
    }

    private void renderConnectionLine() {
        String connectionJson = "null";

        if (lastSelectedSnapshot != null && lastSelectedSnapshot.hasUsablePosition()
                && Double.isFinite(homeLatitudeDeg) && Double.isFinite(homeLongitudeDeg)) {

            List<double[]> points = List.of(
                    new double[]{homeLatitudeDeg, homeLongitudeDeg},
                    new double[]{lastSelectedSnapshot.latitudeDeg(), lastSelectedSnapshot.longitudeDeg()}
            );
            connectionJson = toPointArrayJson(points);
        }

        if (connectionJson.equals(lastRenderedConnectionJson)) {
            return;
        }
        lastRenderedConnectionJson = connectionJson;

        executeMapScriptSafely(
                "window.kstMapApi.setConnection(" + toJsStringLiteral(connectionJson) + ");"
        );
    }

    private void renderGridIfViewportKnown() {
        if (!Double.isFinite(viewportSouthLat)
                || !Double.isFinite(viewportWestLon)
                || !Double.isFinite(viewportNorthLat)
                || !Double.isFinite(viewportEastLon)) {
            return;
        }

        double viewportWidthPx = Math.max(1.0, webView.getWidth());
        double viewportHeightPx = Math.max(1.0, webView.getHeight());

        MaidenheadGridRenderPlanner.GridRenderPlan renderPlan = MaidenheadGridRenderPlanner.createPlan(
                viewportZoom,
                viewportSouthLat,
                viewportWestLon,
                viewportNorthLat,
                viewportEastLon,
                viewportWidthPx,
                viewportHeightPx
        );

        List<MaidenheadGridUtils.GridCell> visibleGridCells = MaidenheadGridUtils.buildVisibleCells(
                viewportSouthLat,
                viewportWestLon,
                viewportNorthLat,
                viewportEastLon,
                renderPlan.precision()
        );

        System.out.println("[StationMap] renderGridIfViewportKnown zoom=" + viewportZoom
                + " precision=" + renderPlan.precision().locatorLength()
                + " labelStride=" + renderPlan.labelColumnStride() + "x" + renderPlan.labelRowStride()
                + " cellPx=" + String.format(Locale.US, "%.1f/%.1f", renderPlan.estimatedCellWidthPx(), renderPlan.estimatedCellHeightPx())
                + " cells=" + visibleGridCells.size());

        String gridJson = toGridJson(visibleGridCells, renderPlan);
        if (gridJson.equals(lastRenderedGridJson)) {
            return;
        }
        lastRenderedGridJson = gridJson;

        executeMapScriptSafely(
                "window.kstMapApi.setGrid(" + toJsStringLiteral(gridJson) + ");"
        );
    }



    private List<double[]> buildBeamPolygon(double startLatDeg,
                                            double startLonDeg,
                                            double centerAzimuthDeg,
                                            double beamWidthDeg,
                                            double radiusKm) {

        List<double[]> polygon = new ArrayList<>();
        polygon.add(new double[]{startLatDeg, startLonDeg});

        double startAzimuth = normalizeAngle(centerAzimuthDeg - beamWidthDeg / 2.0);

        int segmentCount = Math.max(12, (int) Math.ceil(beamWidthDeg / 4.0));
        double angleStep = beamWidthDeg / segmentCount;

        for (int i = 0; i <= segmentCount; i++) {
            double currentAzimuth = normalizeAngle(startAzimuth + i * angleStep);
            polygon.add(calculateDestinationPoint(startLatDeg, startLonDeg, currentAzimuth, radiusKm));
        }

        polygon.add(new double[]{startLatDeg, startLonDeg});
        return polygon;
    }

    private double[] calculateDestinationPoint(double startLatDeg,
                                               double startLonDeg,
                                               double bearingDeg,
                                               double distanceKm) {

        double earthRadiusKm = 6371.009;

        double angularDistance = distanceKm / earthRadiusKm;
        double bearingRad = Math.toRadians(bearingDeg);
        double startLatRad = Math.toRadians(startLatDeg);
        double startLonRad = Math.toRadians(startLonDeg);

        double destinationLatRad = Math.asin(
                Math.sin(startLatRad) * Math.cos(angularDistance)
                        + Math.cos(startLatRad) * Math.sin(angularDistance) * Math.cos(bearingRad)
        );

        double destinationLonRad = startLonRad + Math.atan2(
                Math.sin(bearingRad) * Math.sin(angularDistance) * Math.cos(startLatRad),
                Math.cos(angularDistance) - Math.sin(startLatRad) * Math.sin(destinationLatRad)
        );

        double destinationLatDeg = Math.toDegrees(destinationLatRad);
        double destinationLonDeg = normalizeLongitude(Math.toDegrees(destinationLonRad));

        return new double[]{destinationLatDeg, destinationLonDeg};
    }

    private double normalizeAngle(double angleDeg) {
        double normalized = angleDeg % 360.0;
        return normalized < 0.0 ? normalized + 360.0 : normalized;
    }

    private double normalizeLongitude(double longitudeDeg) {
        double normalized = longitudeDeg;
        while (normalized < -180.0) {
            normalized += 360.0;
        }
        while (normalized > 180.0) {
            normalized -= 360.0;
        }
        return normalized;
    }

    private String normalizeLocator6(String locator) {
        if (locator == null) {
            return "";
        }

        String normalized = locator.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() >= 6) {
            normalized = normalized.substring(0, 6);
        }

        return normalized.matches("^[A-R]{2}[0-9]{2}[A-X]{2}$") ? normalized : "";
    }

    private void executeMapScriptSafely(String script) {
        try {
            webEngine.executeScript(script);
        } catch (Exception exception) {
            System.err.println("[StationMap] executeScript failed: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    private void applySceneTheme(boolean darkMode) {
        if (scene == null) {
            return;
        }

        scene.getStylesheets().clear();
        scene.getStylesheets().add(darkMode
                ? ApplicationConstants.STYLECSSFILE_DEFAULT_EVENING
                : ApplicationConstants.STYLECSSFILE_DEFAULT_DAYLIGHT);

        if (darkMode) {
            rootPane.setStyle("-fx-background-color: #2b3035;");
            mainSplitPane.setStyle("-fx-background-color: #2b3035;");
            detailPane.setStyle("-fx-background-color: #31373c; -fx-border-color: #4c565c; -fx-border-width: 0 0 0 1;");
            statusLabel.setStyle("-fx-background-color: #373e43; -fx-text-fill: lightgray; -fx-padding: 8 10 8 10; -fx-background-radius: 4;");
//            detailFrequenciesArea.setStyle("-fx-control-inner-background: #444b50; -fx-text-fill: lightgray;");
        } else {
            rootPane.setStyle("-fx-background-color: #f2f2f2;");
            mainSplitPane.setStyle("-fx-background-color: #f2f2f2;");
            detailPane.setStyle("-fx-background-color: #f7f7f7; -fx-border-color: #d0d0d0; -fx-border-width: 0 0 0 1;");
            statusLabel.setStyle("-fx-background-color: #f7f7f7; -fx-text-fill: #333333; -fx-padding: 8 10 8 10; -fx-background-radius: 4;");
//            detailFrequenciesArea.setStyle("");
        }
    }

    private void applyMapThemeToWebView(boolean darkMode) {
        if (!mapReady) {
            return;
        }

        executeMapScriptSafely(
                "window.kstMapApi.setTheme(" + toJsStringLiteral(darkMode ? "dark" : "light") + ");"
        );
    }

    private void applyPropagationAssessmentStyle(PathAnalysisResult result) {
        int severity = result == null ? 0 : result.propagationSeverityLevel();

        String textColor = chatPreferences.isGUI_darkModeActive()
                ? "#f0f0f0"
                : "#202020";

        String backgroundColor;
        String borderColor;

        switch (severity) {
            case 1 -> {
                backgroundColor = chatPreferences.isGUI_darkModeActive() ? "#1f4d2b" : "#d8f3dc";
                borderColor = "#3aa655";
            }
            case 2 -> {
                backgroundColor = chatPreferences.isGUI_darkModeActive() ? "#4a4420" : "#fff3bf";
                borderColor = "#d4a017";
            }
            case 3 -> {
                backgroundColor = chatPreferences.isGUI_darkModeActive() ? "#4d3520" : "#ffe0b2";
                borderColor = "#e69138";
            }
            case 4 -> {
                backgroundColor = chatPreferences.isGUI_darkModeActive() ? "#5a2b20" : "#ffc9a9";
                borderColor = "#d96c2c";
            }
            case 5 -> {
                backgroundColor = chatPreferences.isGUI_darkModeActive() ? "#5a2020" : "#ffcdd2";
                borderColor = "#d63b3b";
            }
            default -> {
                backgroundColor = chatPreferences.isGUI_darkModeActive() ? "#33383e" : "#eeeeee";
                borderColor = chatPreferences.isGUI_darkModeActive() ? "#666f78" : "#cccccc";
            }
        }

        detailPathAssessmentValue.setStyle(
                "-fx-text-fill: " + textColor + ";"
                        + "-fx-background-color: " + backgroundColor + ";"
                        + "-fx-border-color: " + borderColor + ";"
                        + "-fx-border-radius: 4;"
                        + "-fx-background-radius: 4;"
                        + "-fx-padding: 3 6 3 6;"
        );
    }

    private String toStationsJson(List<MapCallsignRawSnapshot> snapshots) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (MapCallsignRawSnapshot snapshot : snapshots) {
            if (snapshot == null || !snapshot.hasUsablePosition()) {
                continue;
            }

            if (!first) {
                json.append(',');
            }
            first = false;

            json.append('{')
                    .append("\"callSignRaw\":").append(toJsonString(snapshot.callSignRaw())).append(',')
                    .append("\"markerLabel\":").append(toJsonString(snapshot.markerLabel())).append(',')
                    .append("\"latitudeDeg\":").append(formatDouble(snapshot.latitudeDeg())).append(',')
                    .append("\"longitudeDeg\":").append(formatDouble(snapshot.longitudeDeg())).append(',')
                    .append("\"warningToMyDirection\":").append(snapshot.warningToMyDirection()).append(',')
                    .append("\"worked\":").append(snapshot.worked()).append(',')
                    .append("\"selected\":").append(snapshot.selected())
                    .append('}');
        }

        json.append(']');
        return json.toString();
    }

    private String toGridJson(List<MaidenheadGridUtils.GridCell> cells,
                              MaidenheadGridRenderPlanner.GridRenderPlan renderPlan) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (MaidenheadGridUtils.GridCell cell : cells) {
            if (!first) {
                json.append(',');
            }
            first = false;

            boolean showLabel = renderPlan.shouldShowLabel(cell);

            json.append('{')
                    .append("\"locatorLabel\":").append(toJsonString(cell.locatorLabel())).append(',')
                    .append("\"southLat\":").append(formatDouble(cell.southLat())).append(',')
                    .append("\"westLon\":").append(formatDouble(cell.westLon())).append(',')
                    .append("\"northLat\":").append(formatDouble(cell.northLat())).append(',')
                    .append("\"eastLon\":").append(formatDouble(cell.eastLon())).append(',')
                    .append("\"showLabel\":").append(showLabel).append(',')
                    .append("\"labelFontPx\":").append(formatDouble(renderPlan.labelFontSizePx()))
                    .append('}');
        }

        json.append(']');
        return json.toString();
    }

    private String toPointArrayJson(List<double[]> points) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (double[] point : points) {
            if (!first) {
                json.append(',');
            }
            first = false;

            json.append('{')
                    .append("\"lat\":").append(formatDouble(point[0])).append(',')
                    .append("\"lon\":").append(formatDouble(point[1]))
                    .append('}');
        }

        json.append(']');
        return json.toString();
    }

    private String formatDouble(double value) {
        return String.format(Locale.US, "%.8f", value);
    }

    private String toJsStringLiteral(String raw) {
        return "'" + escapeForJavaScript(raw) + "'";
    }

    private String toJsonString(String raw) {
        return "\"" + escapeJson(raw) + "\"";
    }

    private String escapeJson(String raw) {
        if (raw == null) {
            return "";
        }

        return raw
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String escapeForJavaScript(String raw) {
        if (raw == null) {
            return "";
        }

        return raw
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    public final class JavaMapBridge {

        public void onMapReady() {
            System.out.println("[StationMap JS] onMapReady");
            mapReady = true;
            renderAll();
            requestMapInvalidateSize();
        }

        public void onViewportChanged(double southLat,
                                      double westLon,
                                      double northLat,
                                      double eastLon,
                                      double zoom) {

            viewportSouthLat = southLat;
            viewportWestLon = westLon;
            viewportNorthLat = northLat;
            viewportEastLon = eastLon;
            viewportZoom = (int) Math.round(zoom);

            System.out.println("[StationMap JS] onViewportChanged zoom=" + viewportZoom);

            if (Platform.isFxApplicationThread()) {
                renderGridIfViewportKnown();
            } else {
                Platform.runLater(StationMapView.this::renderGridIfViewportKnown);
            }
        }

        public void onCallsignRawClicked(String callSignRaw) {
            System.out.println("[StationMap JS] onCallsignRawClicked " + callSignRaw);
            if (onCallsignRawSelected != null) {
                onCallsignRawSelected.accept(callSignRaw);
            }
        }

        public void onJsLog(String message) {
            System.out.println("[StationMap JS] " + message);
        }

        public void onJsError(String message) {
            System.err.println("[StationMap JS ERROR] " + message);
        }
    }

    private enum InteractiveKind {
        NONE,
        STATION,
        ZOOM_IN,
        ZOOM_OUT
    }

    private record InteractiveTarget(
            InteractiveKind kind,
            String callSignRaw,
            String tag,
            String cssClass,
            String text
    ) {
        static InteractiveTarget none() {
            return new InteractiveTarget(InteractiveKind.NONE, "", "", "", "");
        }
    }

    private VBox createProfileSection() {
        Label titleLabel = new Label("Path profile / terrain analysis");
        titleLabel.setStyle("-fx-font-weight: bold;");

        detailPathProfileChart.setHeight(220);
//        detailPathProfileChart.setMinHeight(180);
//        detailPathProfileChart.setPrefHeight(220);

        VBox chartBox = new VBox(4, titleLabel, detailPathProfileChart);
        chartBox.setPadding(new Insets(4, 8, 8, 8));
        chartBox.setMinHeight(200);
        chartBox.setPrefHeight(250);

//        detailPathProfileChart.widthProperty().bind(chartBox.widthProperty().subtract(20));

        VBox.setVgrow(detailPathProfileChart, Priority.ALWAYS);

        return chartBox;
    }

    private void makePathValueLabel(Label label) {
        if (label == null) {
            return;
        }

        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(label, Priority.ALWAYS);
    }

    /**
     * Callback to show the howered profile path points position at the map
     * @param point
     */
    private void showProfileHoverPointOnMap(PathProfilePoint point) {
        if (!mapReady) {
            return;
        }

        if (point == null
                || !Double.isFinite(point.latitudeDeg())
                || !Double.isFinite(point.longitudeDeg())) {
            executeMapScriptSafely("window.kstMapApi.setProfileHoverPoint(null);");
            return;
        }

        String label = String.format(
                Locale.US,
                "%.1f km / %.0f m",
                point.distanceKm(),
                point.elevationMeters()
        );

        executeMapScriptSafely(
                "window.kstMapApi.setProfileHoverPoint({"
                        + "lat:" + formatDouble(point.latitudeDeg()) + ","
                        + "lon:" + formatDouble(point.longitudeDeg()) + ","
                        + "label:" + toJsStringLiteral(label)
                        + "});"
        );
    }

    private double resolveInitialStationMapWidth(double[] storedSize) {
        if (storedSize == null || storedSize.length < 1 || !Double.isFinite(storedSize[0])) {
            return 1024.0;
        }

        // Avoid restoring very large old test sizes after the layout changed.
        if (storedSize[0] < 900.0 || storedSize[0] > 1600.0) {
            return 1024.0;
        }

        return storedSize[0];
    }

    private double resolveInitialStationMapHeight(double[] storedSize) {
        if (storedSize == null || storedSize.length < 2 || !Double.isFinite(storedSize[1])) {
            return 768.0;
        }

        double minimumHeight = resolveMinimumStationMapHeight(
                chatPreferences.isGUIstationMapPathAnalysisVisible());

        // Avoid restoring very large old test sizes after the layout changed.
        if (storedSize[1] < minimumHeight || storedSize[1] > 1100.0) {
            return 768.0;
        }

        return storedSize[1];
    }
}
