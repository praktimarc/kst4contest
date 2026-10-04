package kst4contest.view.compose.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kst4contest.model.Band
import kst4contest.view.ApplicationRuntimeLauncher
import kst4contest.view.compose.ComposeWindowHost
import kst4contest.view.compose.MainWindowState
import kst4contest.view.map.MapCallsignRawSnapshotBuilder
import kst4contest.view.map.PathAnalysisResult
import kst4contest.view.map.StationMapStatusText
import kst4contest.view.map.TileFetcher
import java.util.EnumSet

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
object StationMapWindow {

    private val host = ComposeWindowHost("Map")
    var isDarkThemeState = androidx.compose.runtime.mutableStateOf(false)

    fun show(
        mainWindowState: MainWindowState,
        darkMode: Boolean,
        baseFontSizeSp: Float,
        widthDp: Float,
        heightDp: Float,
        onResized: (Float, Float) -> Unit,
    ) {
        isDarkThemeState.value = darkMode
        host.show(
            title = "Station Map",
            darkMode = darkMode,
            baseFontSizeSp = baseFontSizeSp,
            widthDp = widthDp,
            heightDp = heightDp,
            onResized = onResized,
        ) { _ ->
            val window = this.window
            androidx.compose.runtime.LaunchedEffect(window) { window?.minimumSize = java.awt.Dimension(900, 600) }
            val tileFetcher = remember { TileFetcher() }

            /*
             * Released when the window closes. The fetcher owns a thread pool, and a
             * live pool thread is a GC root: without this, every open of this window
             * leaves its whole tile cache reachable for the rest of the session.
             */
            androidx.compose.runtime.DisposableEffect(tileFetcher) {
                onDispose { tileFetcher.close() }
            }

            /*
             * The fetcher is plain Java state, so nothing about it recomposes on its
             * own. Polled rather than wired through a callback: this drives one line of
             * text, and a callback from a background fetch thread into Compose state is
             * more machinery than the question deserves.
             */
            var tilesUnavailable by remember { mutableStateOf(false) }
            LaunchedEffect(tileFetcher) {
                while (true) {
                    kotlinx.coroutines.delay(1_000)
                    tilesUnavailable = tileFetcher.isTileSourceUnavailable()
                }
            }
            val prefs = mainWindowState.prefs
            
            var groupingEnabled by remember { mutableStateOf(prefs.isGUIstationMapClusteringEnabled) }
            var pathAnalysisVisible by remember { mutableStateOf(prefs.isGUIstationMapPathAnalysisVisible) }
            
            val selectedMember = mainWindowState.selectedStation.selected
            
            val ownLocator6 by remember { androidx.compose.runtime.mutableStateOf(mainWindowState.prefs.stn_loginLocatorMainCat) }
            val antennaAzimuthDeg = mainWindowState.antennaQtf.value
            val antennaBeamWidthDeg = mainWindowState.prefs.stn_antennaBeamWidthDeg
            val maxQrbKm = mainWindowState.prefs.stn_maxQRBDefault

            val markers by remember(mainWindowState.stations.rows, mainWindowState.stations.contentRevision, selectedMember, mainWindowState.activeBands) {
                derivedStateOf {
                    val builder = MapCallsignRawSnapshotBuilder()
                    val activeBands = mainWindowState.activeBands
                    
                    val bandSet = EnumSet.noneOf(Band::class.java)
                    if (activeBands.isNotEmpty()) {
                        bandSet.addAll(activeBands)
                    }

                    builder.buildSnapshots(
                        mainWindowState.stations.rows,
                        selectedMember,
                        bandSet
                    )
                }
            }

            var pathAnalysisResult by remember { mutableStateOf<PathAnalysisResult?>(null) }
            val mapState = remember { MapState() }

            LaunchedEffect(selectedMember) {
                if (selectedMember == null) {
                    pathAnalysisResult = null
                    return@LaunchedEffect
                }
                
                val chatController = ApplicationRuntimeLauncher.getCurrent()?.chatController
                if (chatController != null) {
                    val selectedSnapshot = markers.find { it.callSignRaw() == selectedMember.callSignRaw }
                    
                    if (selectedSnapshot != null && selectedSnapshot.hasUsablePosition()) {
                        /*
                         * Pan to the station and leave the zoom alone. The Leaflet map
                         * did exactly this (focusCallsignRaw) and touched the zoom only
                         * when the station was hidden inside a bubble, and then only far
                         * enough to break the bubble open. An earlier Compose version
                         * recomputed a zoom from a bounding box on every selection and
                         * clamped it into 4..12, which threw an operator working at zoom
                         * 15 out to about 7 every time they picked the next station.
                         * AGENTS.md names zoom first among the things not to change as
                         * an incidental side effect.
                         */
                        mapState.centerLon = selectedSnapshot.longitudeDeg()
                        mapState.centerLat = selectedSnapshot.latitudeDeg()
                        mapState.zoom = kst4contest.view.map.StationMapClusterer
                            .zoomToRevealStations(mapState.zoom.toDouble(), groupingEnabled)
                            .toFloat()
                    }

                    pathAnalysisResult = PathAnalysisResult.loading(
                        prefs.stn_loginLocatorMainCat,
                        selectedSnapshot?.locator6() ?: "",
                        selectedSnapshot?.callSignRaw() ?: ""
                    )
                    
                    val overrideBand: kst4contest.model.Band? = null
                    
                    chatController.reachabilityService.requestPathAnalysisForMap(
                        selectedMember,
                        selectedSnapshot,
                        overrideBand
                    ) { result ->
                        pathAnalysisResult = result
                    }
                }
            }

            androidx.compose.material3.Surface(
                color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxSize()
            ) {
            Column(Modifier.fillMaxSize()) {
                /*
                 * The status line takes the room, so the buttons sit at the right edge
                 * the way the JavaFX header had them (HBox.setHgrow on the label). The
                 * controls are the shared ones: a raw Material button paints itself in
                 * the theme's primary colour, which in the evening sheet is a bright
                 * green, and the map window was the only one wearing it.
                 */
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val statusText = StationMapStatusText.build(
                        markers.size,
                        mainWindowState.stationFilter.activeFilters.isNotEmpty() ||
                            mainWindowState.stationFilter.searchText.isNotBlank(),
                        markers.firstOrNull { it.callSignRaw() == selectedMember?.callSignRaw },
                    )

                    /*
                     * The tail of this line carries the bands and the frequency, and on a
                     * narrow window that is exactly what the ellipsis eats. The JavaFX
                     * header put the same text into a tooltip for that reason
                     * (statusTooltip, StationMapView.updateStatusLabel).
                     */
                    androidx.compose.foundation.TooltipArea(
                        tooltip = { kst4contest.view.compose.TooltipCard(statusText) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = statusText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (selectedMember != null) {
                        kst4contest.view.compose.Form.button("Trigger cluster spot") {
                            ApplicationRuntimeLauncher.getCurrent()?.chatController
                                ?.dxClusterServer?.broadcastSingleDXClusterEntryToLoggers(selectedMember)
                        }
                    }

                    kst4contest.view.compose.Form.button("Reset view") {
                        mainWindowState.selectedStation.select(null)
                        val home = runCatching {
                            kst4contest.locatorUtils.Location(ownLocator6)
                        }.getOrNull()
                        mapState.centerLon = home?.longitude?.toDegrees() ?: 10.0
                        mapState.centerLat = home?.latitude?.toDegrees() ?: 51.0
                        /* The zoom is deliberately left alone; see AGENTS.md, UI behaviour. */
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        kst4contest.view.compose.CompactCheckbox(groupingEnabled) {
                            groupingEnabled = it
                            prefs.isGUIstationMapClusteringEnabled = it
                        }
                        Text("Group nearby stations", modifier = Modifier.padding(start = 4.dp))
                    }

                    if (!pathAnalysisVisible) {
                        Text(
                            text = "Path analysis is hidden.",
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.alpha(0.85f)
                        )
                    }

                    kst4contest.view.compose.Form.button(
                        if (pathAnalysisVisible) "Hide path analysis" else "Show path analysis"
                    ) {
                        pathAnalysisVisible = !pathAnalysisVisible
                        prefs.isGUIstationMapPathAnalysisVisible = pathAnalysisVisible
                    }
                }

                // Main Layout
                Column(Modifier.weight(1f).fillMaxWidth()) {
                val mapSplitterState = androidx.compose.runtime.remember {
                    kst4contest.view.compose.SplitterState(
                        paneCount = 2,
                        stored = doubleArrayOf(0.65),
                        save = { }
                    )
                }

                val leftPane: @Composable () -> Unit = {
                    Column(Modifier.fillMaxHeight()) {
                        val isDarkTheme = isDarkThemeState.value
                        androidx.compose.foundation.layout.Box(Modifier.weight(1f).fillMaxWidth()) {
                        ComposeStationMap(
                            markers = markers,
                            tileFetcher = tileFetcher,
                            mapState = mapState,
                            groupingEnabled = groupingEnabled,
                            selectedMarkerCallsign = selectedMember?.callSignRaw,
                            ownLocator6 = ownLocator6,
                            antennaAzimuthDeg = antennaAzimuthDeg ?: 0.0,
                            antennaBeamWidthDeg = antennaBeamWidthDeg,
                            maxQrbKm = maxQrbKm,
                            darkMode = isDarkTheme,
                            onMarkerClicked = { snapshot ->
                                val chatController = ApplicationRuntimeLauncher.getCurrent()?.chatController
                                val member = chatController?.lst_chatMemberList?.snapshot()?.find { it.callSignRaw == snapshot.callSignRaw() }
                                if (member != null) {
                                    mainWindowState.selectedStation.select(member)
                                    
                                    // Mirror old JavaFX handleMapCallsignSelection logic: prefill /cq
                                    mainWindowState.chatInput.prepareCq(member.callSign ?: "", forceOverwrite = true)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        MapOverlayControls(
                            darkMode = isDarkTheme,
                            tilesUnavailable = tilesUnavailable,
                            onZoomIn = { mapState.zoom = (mapState.zoom + 1f).coerceAtMost(MAX_MAP_ZOOM) },
                            onZoomOut = { mapState.zoom = (mapState.zoom - 1f).coerceAtLeast(MIN_MAP_ZOOM) },
                        )
                        }

                        if (pathAnalysisVisible) {
                            val points = pathAnalysisResult?.profilePoints() ?: emptyList()
                            val isDarkTheme = isDarkThemeState.value
                            PathProfileChart(
                                profilePoints = points,
                                totalDistanceKm = pathAnalysisResult?.distanceKm() ?: Double.NaN,
                                darkMode = isDarkTheme,
                                homeAntennaHeightMeters = pathAnalysisResult?.homeAntennaHeightMeters() ?: Double.NaN,
                                targetAntennaHeightMeters = pathAnalysisResult?.targetAntennaHeightMeters() ?: Double.NaN,
                                analysisFrequencyMHz = pathAnalysisResult?.analysisFrequencyMHz() ?: Double.NaN,
                                effectiveEarthRadiusFactor = pathAnalysisResult?.effectiveEarthRadiusFactor() ?: kst4contest.view.map.PathGeometryUtils.DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR,
                                horizonSummary = pathAnalysisResult?.horizonSummary() ?: kst4contest.view.map.PathHorizonSummary.empty(),
                                obstructionSummary = pathAnalysisResult?.obstructionSummary() ?: kst4contest.view.map.PathObstructionSummary.empty(),
                                modifier = Modifier.fillMaxWidth().height(210.dp)
                            )
                        }
                    }
                }

                val rightPane: @Composable () -> Unit = {
                    val scrollState = rememberScrollState()
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxHeight()) {
                        PathAnalysisDetails(
                            result = pathAnalysisResult,
                            darkMode = isDarkThemeState.value,
                            modifier = Modifier.fillMaxHeight()
                                .padding(horizontal = 4.dp)
                                .padding(end = 12.dp)
                                .verticalScroll(scrollState),
                        )
                        androidx.compose.foundation.VerticalScrollbar(
                            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd).fillMaxHeight(),
                            adapter = androidx.compose.foundation.rememberScrollbarAdapter(scrollState)
                        )
                    }
                }

                if (pathAnalysisVisible) {
                    kst4contest.view.compose.SplitterPane(
                        state = mapSplitterState,
                        orientation = kst4contest.view.compose.SplitterOrientation.HORIZONTAL,
                        panes = listOf(leftPane, rightPane),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize()) {
                        leftPane()
                    }
                }
                } // main layout column
            } // window column
            } // Surface
        } // host.show content
    }

    @JvmStatic
    fun applyDarkMode(darkMode: Boolean) {
        isDarkThemeState.value = darkMode
        host.applyDarkMode(darkMode)
    }

    fun hide() {
        host.close()
    }
    
    fun isShowing(): Boolean = host.isOpen
    
    fun toggle(mainWindowState: MainWindowState) {
        if (isShowing()) {
            hide()
        } else {
            show(
                mainWindowState = mainWindowState,
                darkMode = mainWindowState.prefs.isGUI_darkModeActive,
                baseFontSizeSp = 12f,
                widthDp = 1000f,
                heightDp = 700f,
                onResized = { w, h -> }
            )
        }
    }
}
