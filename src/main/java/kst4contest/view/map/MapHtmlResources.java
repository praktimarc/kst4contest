package kst4contest.view.map;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * HTML host for the JavaFX WebView map.
 *
 * This version keeps DOM-based station markers, but exposes helper APIs so the
 * JavaFX WebView can decide interactions directly:
 * - inspectPoint(x,y) returns what is under the cursor
 * - zoomIn()/zoomOut() are callable from Java
 * - grid / beam / connection use non-interactive panes
 * - JavaScript errors are forwarded to Java through javaMapBridge
 * - setTheme(light|dark) aligns the map with the JavaFX application theme
 *
 * Important:
 * This version intentionally uses integer Leaflet zoom levels again.
 * Fractional zoom in JavaFX WebView caused unreliable marker positioning.
 */
public final class MapHtmlResources {

    private MapHtmlResources() {
    }

    private static String readRequiredResource(String resourcePath) {
        try (InputStream inputStream = MapHtmlResources.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing map resource: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read map resource: " + resourcePath, exception);
        }
    }

    public static String createStationMapHtml(int tileProxyPort) {
        String leafletCss = readRequiredResource("/web/leaflet/leaflet.css");
        String leafletJs = readRequiredResource("/web/leaflet/leaflet.js");

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>KST4Contest Station Map</title>
                    <style>
                """ + leafletCss + """
                    </style>
                    <style>
                        :root {
                            --map-background: #ede9df;
                            --station-label-bg: rgba(248, 248, 248, 0.95);
                            --station-label-color: #1c1c1c;
                            --station-label-border: rgba(0, 0, 0, 0.20);
                            --grid-label-bg: rgba(255, 255, 255, 0.18);
                            --grid-label-color: rgba(20, 20, 20, 0.38);
                            --control-bg: rgba(255, 255, 255, 0.96);
                            --control-fg: #242424;
                            --control-border: #b7b7b7;
                            --attribution-bg: rgba(255, 255, 255, 0.88);
                            --attribution-fg: #2d2d2d;
                            --attribution-link: #145fa3;
                        }

                        body.kst-theme-dark {
                            --map-background: #23282d;
                            --station-label-bg: rgba(36, 40, 45, 0.96);
                            --station-label-color: #f1f3f5;
                            --station-label-border: rgba(255, 255, 255, 0.18);
                            --grid-label-bg: rgba(34, 38, 43, 0.20);
                            --grid-label-color: rgba(235, 240, 245, 0.42);
                            --control-bg: rgba(55, 62, 67, 0.96);
                            --control-fg: #e2e6ea;
                            --control-border: #556068;
                            --attribution-bg: rgba(34, 38, 43, 0.86);
                            --attribution-fg: #d2d8dd;
                            --attribution-link: #88c7ff;
                        }

                        html, body, #map {
                            width: 100%;
                            height: 100%;
                            margin: 0;
                            padding: 0;
                            overflow: hidden;
                            background: var(--map-background);
                            font-family: Arial, sans-serif;
                        }

                        .leaflet-container {
                            background: var(--map-background);
                        }

                        body.kst-theme-dark .leaflet-tile-pane {
                            opacity: 0.82;
                        }

                        .leaflet-bar {
                            border: 1px solid var(--control-border);
                            box-shadow: 0 1px 4px rgba(0, 0, 0, 0.28);
                        }

                        .leaflet-bar a,
                        .leaflet-bar a:hover {
                            background: var(--control-bg);
                            color: var(--control-fg);
                            border-bottom: 1px solid var(--control-border);
                        }

                        .leaflet-control-attribution {
                            background: var(--attribution-bg);
                            color: var(--attribution-fg);
                        }

                        .leaflet-control-attribution a {
                            color: var(--attribution-link);
                        }

                        .station-marker-wrapper {
                            background: transparent;
                            border: none;
                            box-shadow: none;
                        }

                        .station-marker-root {
                            position: relative;
                            width: 1px;
                            height: 1px;
                            pointer-events: auto;
                            cursor: pointer;
                            user-select: none;
                        }

                        .station-dot {
                            position: absolute;
                            left: -6px;
                            top: -6px;
                            width: 12px;
                            height: 12px;
                            border-radius: 50%;
                            background: #1d1d1d;
                            box-sizing: border-box;
                            border: 2px solid #4da6ff;
                            box-shadow: 0 0 0 1px rgba(0,0,0,0.25);
                        }

                        .station-dot.worked {
                            border-color: #ffd24d;
                        }

                        .station-dot.warning {
                            border-color: #00ff66;
                        }

                        .station-dot.selected {
                            left: -8px;
                            top: -8px;
                            width: 16px;
                            height: 16px;
                            border-width: 3px;
                            border-color: #ff9900;
                        }

                        .station-label {
                            position: absolute;
                            left: 10px;
                            top: -22px;
                            display: inline-block;
                            background: var(--station-label-bg);
                            color: var(--station-label-color);
                            border: 1px solid var(--station-label-border);
                            border-radius: 5px;
                            padding: 2px 5px;
                            font-size: 12px;
                            font-weight: 700;
                            line-height: 1.1;
                            white-space: nowrap;
                            box-shadow: 0 1px 4px rgba(0, 0, 0, 0.35);
                        }

                        .station-label.warning {
                            color: #00ff66;
                            border-color: rgba(0, 255, 102, 0.75);
                            font-weight: 800;
                        }

                        .station-cluster-wrapper {
                            background: transparent;
                            border: none;
                            box-shadow: none;
                        }

                        .station-cluster-root {
                            position: relative;
                            width: 1px;
                            height: 1px;
                            pointer-events: auto;
                            cursor: pointer;
                            user-select: none;
                        }

                        /*
                         * Cluster design aligned with normal KST4Contest station markers:
                         * - dark center like station-dot
                         * - blue border for normal clustered stations
                         * - yellow border when the cluster contains worked stations
                         * - orange hover border similar to selected station state
                         */
                        .station-cluster-bubble {
                            position: absolute;
                            left: -16px;
                            top: -16px;
                            min-width: 32px;
                            height: 32px;
                            padding: 0 7px;
                            border-radius: 18px;
                            background: #1d1d1d;
                            color: #f4f7fa;
                            border: 2px solid #4da6ff;
                            box-shadow:
                                0 0 0 1px rgba(0, 0, 0, 0.35),
                                0 2px 7px rgba(0, 0, 0, 0.42);
                            box-sizing: border-box;
                            font-size: 13px;
                            font-weight: 800;
                            line-height: 28px;
                            text-align: center;
                            white-space: nowrap;
                        }

                        .station-cluster-bubble.medium {
                            left: -18px;
                            top: -18px;
                            min-width: 36px;
                            height: 36px;
                            border-radius: 20px;
                            font-size: 14px;
                            line-height: 32px;
                        }

                        .station-cluster-bubble.large {
                            left: -21px;
                            top: -21px;
                            min-width: 42px;
                            height: 42px;
                            border-radius: 23px;
                            font-size: 15px;
                            line-height: 38px;
                        }

                        .station-cluster-bubble.worked {
                            border-color: #ffd24d;
                        }

                        .station-cluster-bubble.warning {
                            border-color: #00ff66;
                            color: #00ff66;
                        }

                        .station-cluster-root:hover .station-cluster-bubble {
                            border-color: #ff9900;
                            box-shadow:
                                0 0 0 2px rgba(255, 153, 0, 0.28),
                                0 2px 9px rgba(0, 0, 0, 0.48);
                        }

                        body.kst-theme-dark .station-cluster-bubble {
                            background: #202428;
                            color: #f1f3f5;
                            border-color: #4da6ff;
                            box-shadow:
                                0 0 0 1px rgba(255, 255, 255, 0.12),
                                0 2px 8px rgba(0, 0, 0, 0.58);
                        }

                        body.kst-theme-dark .station-cluster-bubble.worked {
                            border-color: #ffd24d;
                        }

                        body.kst-theme-dark .station-cluster-bubble.warning {
                            border-color: #00ff66;
                            color: #00ff66;
                        }

                        body.kst-theme-dark .station-cluster-root:hover .station-cluster-bubble {
                            border-color: #ff9900;
                            box-shadow:
                                0 0 0 2px rgba(255, 153, 0, 0.32),
                                0 2px 10px rgba(0, 0, 0, 0.65);
                        }

                        .maidenhead-grid-label-wrapper {
                            background: transparent;
                            border: none;
                            box-shadow: none;
                            pointer-events: none;
                        }

                        .maidenhead-grid-label-wrapper .maidenhead-grid-label {
                            transform: translate(-50%, -50%);
                        }

                        .maidenhead-grid-label {
                            display: inline-block;
                            background: var(--grid-label-bg);
                            #color: var(--grid-label-color);
                            color: #63067a;
                            border-radius: 3px;
                            padding: 0 3px;
                            font-weight: 600;
                            text-shadow: 0 0 2px rgba(0, 0, 0, 0.18);
                            white-space: nowrap;
                            user-select: none;
                            pointer-events: none;
                        }
                        
                    </style>
                </head>
                <body class="kst-theme-light">
                <div id="map"></div>

                <script>
                    // JavaFX 21 WebView can render Leaflet's CSS translate3d tile positioning incorrectly.
                    // Disable Leaflet 3D transforms before leaflet.js is loaded to keep tile rendering stable.
                    window.L_DISABLE_3D = true;
                </script>
                <script>
                """ + leafletJs + """
                </script>
                <script>window._kstTileProxyPort=__TILE_PROXY_PORT__;</script>

                <script>
                    window.kstMapApi = (function () {
                        let map;
                        let stationLayer;
                        let gridLayer;
                        let beamLayer;
                        let connectionLayer;
                        let profileHoverMarker;

                        /*
                         * Currently visible individual station markers.
                         *
                         * A callsign is present here only when it is rendered individually.
                         * If the station is inside a cluster, stationsByCallsignRaw is used
                         * for focus/zoom operations.
                         */
                        let markersByCallsignRaw = {};

                        /*
                         * All station data as received from Java.
                         */
                        let stationData = [];
                        let stationsByCallsignRaw = {};

                        let clustersById = {};
                        let clusterSequence = 0;

                        let activeTheme = 'light';
                        let invalidateNotifyTimer = 0;

                        /*
                         * Integer zoom only.
                         *
                         * Fractional zoom is intentionally disabled because JavaFX WebView
                         * and Leaflet marker positioning were unreliable at intermediate
                         * zoom levels.
                         */
                        const KST_ZOOM_STEP = 1;
                        const KST_MIN_ZOOM = 3;
                        const KST_MAX_ZOOM = 18;

                        /*
                         * Enables verbose JavaScript map logging.
                         *
                         * Keep this false for normal operation. jsError() still reports
                         * real errors.
                         */
                        const KST_MAP_DEBUG = false;

                        /*
                         * Cluster settings.
                         *
                         * With integer zoom, clustering is disabled at zoom >= 8.
                         * At zoom 7 only very close stations are grouped.
                         */
                        const KST_CLUSTER_DISABLE_ZOOM = 8;

                        /*
                         * Do not cluster pairs immediately. Two nearby stations are still
                         * readable and should remain individually clickable.
                         */
                        const KST_CLUSTER_MIN_STATIONS = 3;

                        /*
                         * Cluster cell sizes in screen pixels.
                         *
                         * Smaller cells make clustering less aggressive. Stations must be
                         * closer together on screen before they are grouped.
                         */
                        const KST_CLUSTER_CELL_SIZE_HIGH_ZOOM = 55;
                        const KST_CLUSTER_CELL_SIZE_MEDIUM_ZOOM = 70;
                        const KST_CLUSTER_CELL_SIZE_LOW_ZOOM = 95;
                        const KST_CLUSTER_CELL_SIZE_VERY_LOW_ZOOM = 125;

                        function jsLog(message) {
                            if (!KST_MAP_DEBUG) {
                                return;
                            }

                            try {
                                if (window.javaMapBridge) {
                                    window.javaMapBridge.onJsLog(String(message));
                                }
                            } catch (e) {
                                console.log('[KST map fallback log]', message, e);
                            }
                        }

                        function jsError(message) {
                            try {
                                if (window.javaMapBridge) {
                                    window.javaMapBridge.onJsError(String(message));
                                }
                            } catch (e) {
                                console.error('[KST map fallback error]', message);
                            }
                        }

                        function escapeHtml(value) {
                            if (value === null || value === undefined) {
                                return '';
                            }

                            return String(value)
                                .replace(/&/g, '&amp;')
                                .replace(/</g, '&lt;')
                                .replace(/>/g, '&gt;')
                                .replace(/"/g, '&quot;')
                                .replace(/'/g, '&#39;');
                        }

                        function notifyMapReady() {
                            try {
                                if (window.javaMapBridge) {
                                    window.javaMapBridge.onMapReady();
                                }
                            } catch (e) {
                                jsError('notifyMapReady failed: ' + e);
                            }
                        }

                        function notifyViewport() {
                            if (!map) {
                                return;
                            }

                            try {
                                if (!window.javaMapBridge) {
                                    jsError('notifyViewport skipped: no javaMapBridge');
                                    return;
                                }

                                const bounds = map.getBounds();
                                const zoom = map.getZoom();

                                jsLog('notifyViewport south=' + bounds.getSouth()
                                        + ' west=' + bounds.getWest()
                                        + ' north=' + bounds.getNorth()
                                        + ' east=' + bounds.getEast()
                                        + ' zoom=' + zoom);

                                window.javaMapBridge.onViewportChanged(
                                    bounds.getSouth(),
                                    bounds.getWest(),
                                    bounds.getNorth(),
                                    bounds.getEast(),
                                    zoom
                                );
                            } catch (e) {
                                jsError('notifyViewport failed: ' + e);
                            }
                        }

                        function gridLineColor() {
                            return activeTheme === 'dark' ? '#e1e7ec' : '#46586c';
                        }

                        function gridLineOpacity() {
                            return activeTheme === 'dark' ? 0.48 : 0.56;
                        }

                        function connectionColor() {
                            return activeTheme === 'dark' ? '#2fd7ff' : '#00a4cf';
                        }

                        function applyThemeClass() {
                            document.body.classList.remove('kst-theme-light', 'kst-theme-dark');
                            document.body.classList.add(activeTheme === 'dark' ? 'kst-theme-dark' : 'kst-theme-light');
                        }

                        function setTheme(themeName) {
                            activeTheme = themeName === 'dark' ? 'dark' : 'light';
                            applyThemeClass();
                            jsLog('setTheme ' + activeTheme);
                        }

                        function buildStationMarkerHtml(station) {
                            let dotClasses = 'station-dot';
                            if (station.selected) {
                                dotClasses += ' selected';
                            } else if (station.warningToMyDirection) {
                                dotClasses += ' warning';
                            } else if (station.worked) {
                                dotClasses += ' worked';
                            }

                            let labelClasses = 'station-label';
                            if (station.warningToMyDirection) {
                                labelClasses += ' warning';
                            }

                            return '<div class="station-marker-root" data-callsignraw="' + escapeHtml(station.callSignRaw) + '">' 
                                + '<div class="' + dotClasses + '"></div>'
                                + '<div class="' + labelClasses + '">' + escapeHtml(station.markerLabel) + '</div>'
                                + '</div>';
                        }

                        function getStationCallsignKey(station) {
                            if (!station || station.callSignRaw === null || station.callSignRaw === undefined) {
                                return '';
                            }

                            return String(station.callSignRaw);
                        }

                        function isStationPositionValid(station) {
                            if (!station) {
                                return false;
                            }

                            return isFinite(station.latitudeDeg) && isFinite(station.longitudeDeg);
                        }

                        /**
                         * Returns true for stations that must not be hidden inside a cluster.
                         *
                         * These stations remain individually visible even at low zoom levels
                         * because they are operationally important during contest operation.
                         */
                        function shouldRenderStationIndividually(station) {
                            if (!station) {
                                return false;
                            }

                            if (station.selected) {
                                return true;
                            }

                            if (station.warningToMyDirection) {
                                return true;
                            }

                            return false;
                        }

                        /**
                         * Returns the cluster grid size in screen pixels for the current zoom level.
                         *
                         * The cluster calculation is screen-based, not locator-based. This makes
                         * the decision match the real visual problem: too many labels in the same
                         * screen area.
                         */
                        function getClusterCellSizePx() {
                            if (!map) {
                                return KST_CLUSTER_CELL_SIZE_MEDIUM_ZOOM;
                            }

                            const zoom = Number(map.getZoom());

                            if (zoom >= 7) {
                                return KST_CLUSTER_CELL_SIZE_HIGH_ZOOM;
                            }

                            if (zoom >= 6) {
                                return KST_CLUSTER_CELL_SIZE_MEDIUM_ZOOM;
                            }

                            if (zoom >= 5) {
                                return KST_CLUSTER_CELL_SIZE_LOW_ZOOM;
                            }

                            return KST_CLUSTER_CELL_SIZE_VERY_LOW_ZOOM;
                        }

                        function buildClusterMarkerHtml(clusterId, clusterStations) {
                            const count = clusterStations ? clusterStations.length : 0;

                            let bubbleClasses = 'station-cluster-bubble';

                            if (count >= 20) {
                                bubbleClasses += ' large';
                            } else if (count >= 8) {
                                bubbleClasses += ' medium';
                            }

                            const containsWorkedStation = clusterStations
                                && clusterStations.some(station => station && station.worked);

                            const containsWarningStation = clusterStations
                                && clusterStations.some(station => station && station.warningToMyDirection);

                            if (containsWarningStation) {
                                bubbleClasses += ' warning';
                            } else if (containsWorkedStation) {
                                bubbleClasses += ' worked';
                            }

                            return '<div class="station-cluster-root" data-cluster-id="' + escapeHtml(clusterId) + '">'
                                + '<div class="' + bubbleClasses + '">' + count + '</div>'
                                + '</div>';
                        }

                        function buildClusterTooltipHtml(clusterStations) {
                            if (!clusterStations || clusterStations.length === 0) {
                                return '';
                            }

                            const callsigns = clusterStations
                                .map(station => station.markerLabel || station.callSignRaw || station.callSign || '')
                                .filter(value => value !== null && value !== undefined && String(value).trim() !== '')
                                .map(value => String(value).trim())
                                .sort();

                            const maxPreviewCount = 20;
                            const preview = callsigns
                                .slice(0, maxPreviewCount)
                                .map(value => escapeHtml(value))
                                .join('<br>');

                            const remainingCount = Math.max(0, callsigns.length - maxPreviewCount);

                            let html = '<b>' + clusterStations.length + ' stations</b>';

                            if (preview) {
                                html += '<br>' + preview;
                            }

                            if (remainingCount > 0) {
                                html += '<br>+' + remainingCount + ' more';
                            }

                            html += '<br><i>Click to zoom in</i>';

                            return html;
                        }

                        function addStationMarker(station) {
                            if (!stationLayer || !isStationPositionValid(station)) {
                                return;
                            }

                            const marker = L.marker(
                                [station.latitudeDeg, station.longitudeDeg],
                                {
                                    interactive: true,
                                    keyboard: false,
                                    icon: L.divIcon({
                                        className: 'station-marker-wrapper',
                                        html: buildStationMarkerHtml(station),
                                        iconSize: [1, 1],
                                        iconAnchor: [0, 0]
                                    })
                                }
                            );

                            marker.addTo(stationLayer);

                            const callSignKey = getStationCallsignKey(station);
                            if (callSignKey) {
                                markersByCallsignRaw[callSignKey] = marker;
                            }
                        }

                        function addClusterMarker(clusterStations) {
                            if (!stationLayer || !clusterStations || clusterStations.length === 0) {
                                return;
                            }

                            let latSum = 0.0;
                            let lonSum = 0.0;
                            let validCount = 0;

                            clusterStations.forEach(station => {
                                if (isStationPositionValid(station)) {
                                    latSum += Number(station.latitudeDeg);
                                    lonSum += Number(station.longitudeDeg);
                                    validCount++;
                                }
                            });

                            if (validCount === 0) {
                                return;
                            }

                            const centerLat = latSum / validCount;
                            const centerLon = lonSum / validCount;

                            const clusterId = 'cluster-' + (++clusterSequence);
                            clustersById[clusterId] = clusterStations;

                            const clusterMarker = L.marker(
                                [centerLat, centerLon],
                                {
                                    interactive: true,
                                    keyboard: false,
                                    icon: L.divIcon({
                                        className: 'station-cluster-wrapper',
                                        html: buildClusterMarkerHtml(clusterId, clusterStations),
                                        iconSize: [1, 1],
                                        iconAnchor: [0, 0]
                                    })
                                }
                            );

                            clusterMarker.on('click', function () {
                                zoomToCluster(clusterStations);
                            });

                            clusterMarker.bindTooltip(buildClusterTooltipHtml(clusterStations), {
                                direction: 'top',
                                sticky: true,
                                opacity: 0.95
                            });

                            clusterMarker.addTo(stationLayer);
                        }

                        function renderAllStationsIndividually() {
                            stationData.forEach(station => {
                                addStationMarker(station);
                            });
                        }

                        /**
                         * Renders stations with simple screen-grid clustering.
                         *
                         * Important stations such as the currently selected station and warning
                         * stations are rendered individually before clustering. They remain
                         * visible even at low zoom levels.
                         */
                        function renderClusteredStations() {
                            const clusterCellSizePx = getClusterCellSizePx();
                            const buckets = {};

                            stationData.forEach(station => {
                                if (!isStationPositionValid(station)) {
                                    return;
                                }

                                if (shouldRenderStationIndividually(station)) {
                                    addStationMarker(station);
                                    return;
                                }

                                const point = map.latLngToContainerPoint([station.latitudeDeg, station.longitudeDeg]);
                                const cellX = Math.floor(point.x / clusterCellSizePx);
                                const cellY = Math.floor(point.y / clusterCellSizePx);
                                const key = cellX + ':' + cellY;

                                if (!buckets[key]) {
                                    buckets[key] = [];
                                }

                                buckets[key].push(station);
                            });

                            Object.keys(buckets).forEach(key => {
                                const bucketStations = buckets[key];

                                if (bucketStations.length >= KST_CLUSTER_MIN_STATIONS) {
                                    addClusterMarker(bucketStations);
                                } else {
                                    bucketStations.forEach(station => addStationMarker(station));
                                }
                            });
                        }

                        /**
                         * Re-renders station markers for the current zoom and viewport.
                         *
                         * This is called when stations arrive from Java and after zoom/move
                         * events, because screen-grid clusters depend on the current viewport.
                         */
                        function renderStationMarkers() {
                            if (!map || !stationLayer) {
                                return;
                            }

                            stationLayer.clearLayers();
                            markersByCallsignRaw = {};
                            clustersById = {};
                            clusterSequence = 0;

                            if (!stationData || stationData.length === 0) {
                                return;
                            }

                            if (Number(map.getZoom()) >= KST_CLUSTER_DISABLE_ZOOM) {
                                renderAllStationsIndividually();
                            } else {
                                renderClusteredStations();
                            }
                        }

                        /**
                         * Zooms into a cluster.
                         *
                         * A cluster click does not select one station. It moves the map towards
                         * the cluster center and increases zoom until individual stations become
                         * visible.
                         */
                        function zoomToCluster(clusterStations) {
                            if (!map || !clusterStations || clusterStations.length === 0) {
                                return;
                            }

                            const latLngs = [];

                            clusterStations.forEach(station => {
                                if (isStationPositionValid(station)) {
                                    latLngs.push(L.latLng(station.latitudeDeg, station.longitudeDeg));
                                }
                            });

                            if (latLngs.length === 0) {
                                return;
                            }

                            const bounds = L.latLngBounds(latLngs);
                            const currentZoom = Number(map.getZoom());

                            const targetZoom = clampZoomToLeafletLimits(
                                Math.min(KST_CLUSTER_DISABLE_ZOOM, currentZoom + 1)
                            );

                            map.setView(bounds.getCenter(), targetZoom, {
                                animate: false
                            });

                            notifyViewport();
                        }

                        function init() {
                            if (map) {
                                return true;
                            }

                            if (typeof L === 'undefined') {
                                jsError('Leaflet is not loaded. Station map cannot initialize.');
                                return false;
                            }

                            applyThemeClass();

                            map = L.map('map', {
                                zoomControl: true,
                                minZoom: KST_MIN_ZOOM,
                                maxZoom: KST_MAX_ZOOM,

                                /*
                                 * Integer zoom only.
                                 * This keeps station positions stable in JavaFX WebView.
                                 */
                                zoomSnap: 1,
                                zoomDelta: 1,
                                wheelPxPerZoomLevel: 120,

                                zoomAnimation: false,
                                fadeAnimation: false,
                                markerZoomAnimation: false,
                                inertia: false
                            }).setView([51.0, 10.0], 6);

                            jsLog('Leaflet map initialized');

                            const tileLayer = L.tileLayer(
                                'http://127.0.0.1:' + window._kstTileProxyPort + '/tiles/{s}/{z}/{x}/{y}.png',
                                {
                                    minZoom: KST_MIN_ZOOM,
                                    maxZoom: KST_MAX_ZOOM,
                                    updateWhenZooming: false,
                                    keepBuffer: 4,
                                    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                                }
                            );

                            tileLayer.on('tileerror', function (event) {
                                const source = event && event.tile ? event.tile.src : 'unknown';
                                jsError('OSM tile load failed: ' + source);
                            });

                            tileLayer.addTo(map);

                            map.createPane('beamPane');
                            map.getPane('beamPane').style.zIndex = 410;
                            map.getPane('beamPane').style.pointerEvents = 'none';

                            map.createPane('gridPane');
                            map.getPane('gridPane').style.zIndex = 420;
                            map.getPane('gridPane').style.pointerEvents = 'none';

                            map.createPane('gridLabelPane');
                            map.getPane('gridLabelPane').style.zIndex = 430;
                            map.getPane('gridLabelPane').style.pointerEvents = 'none';

                            map.createPane('connectionPane');
                            map.getPane('connectionPane').style.zIndex = 440;
                            map.getPane('connectionPane').style.pointerEvents = 'none';

                            stationLayer = L.layerGroup().addTo(map);
                            gridLayer = L.layerGroup().addTo(map);
                            beamLayer = L.layerGroup().addTo(map);
                            connectionLayer = L.layerGroup().addTo(map);

                            map.on('zoomend', function () {
                                renderStationMarkers();
                                notifyViewport();
                            });

                            map.on('moveend', function () {
                                renderStationMarkers();
                                notifyViewport();
                            });

                            notifyMapReady();
                            notifyViewport();
                            return true;
                        }

                        function invalidateSize() {
                            if (!map) {
                                return;
                            }

                            const mapElement = document.getElementById('map');
                            const domWidth = mapElement ? mapElement.clientWidth : -1;
                            const domHeight = mapElement ? mapElement.clientHeight : -1;

                            jsLog('invalidateSize dom=' + domWidth + 'x' + domHeight
                                    + ' leafletBefore=' + map.getSize().x + 'x' + map.getSize().y);

                            window.requestAnimationFrame(function () {
                                map.invalidateSize({
                                    animate: false,
                                    pan: false,
                                    debounceMoveend: true
                                });

                                window.setTimeout(function () {
                                    map.invalidateSize({
                                        animate: false,
                                        pan: false,
                                        debounceMoveend: true
                                    });

                                    jsLog('invalidateSize leafletAfter=' + map.getSize().x + 'x' + map.getSize().y);

                                    if (invalidateNotifyTimer) {
                                        window.clearTimeout(invalidateNotifyTimer);
                                    }

                                    invalidateNotifyTimer = window.setTimeout(function () {
                                        notifyViewport();
                                    }, 120);
                                }, 80);
                            });
                        }

                        function resize() {
                            invalidateSize();
                        }

                        /**
                         * Keeps the zoom value inside the explicit KST zoom limits.
                         */
                        function clampZoomToLeafletLimits(zoom) {
                            return Math.max(KST_MIN_ZOOM, Math.min(KST_MAX_ZOOM, zoom));
                        }

                        /**
                         * Changes the zoom in stable integer steps.
                         */
                        function changeZoomByKstStep(delta) {
                            if (!map) {
                                return;
                            }

                            const currentZoom = Number(map.getZoom());
                            const nextZoom = clampZoomToLeafletLimits(currentZoom + delta);

                            if (nextZoom === currentZoom) {
                                return;
                            }

                            map.setZoom(nextZoom, {
                                animate: false
                            });
                        }

                        function zoomIn() {
                            changeZoomByKstStep(KST_ZOOM_STEP);
                        }

                        function zoomOut() {
                            changeZoomByKstStep(-KST_ZOOM_STEP);
                        }

                        function getViewportState() {
                            if (!map) {
                                return '';
                            }

                            const bounds = map.getBounds();
                            const zoom = map.getZoom();

                            return [
                                bounds.getSouth(),
                                bounds.getWest(),
                                bounds.getNorth(),
                                bounds.getEast(),
                                zoom
                            ].join('|');
                        }

                        function inspectPoint(x, y) {
                            try {
                                const el = document.elementFromPoint(x, y);
                                if (!el) {
                                    return 'none||||';
                                }

                                const stationRoot = el.closest('.station-marker-root');
                                if (stationRoot) {
                                    const callSignRaw = stationRoot.getAttribute('data-callsignraw') || '';
                                    return 'station|' + callSignRaw + '|' + el.tagName + '|' + (el.className || '') + '|' + (el.textContent || '').trim();
                                }

                                const clusterRoot = el.closest('.station-cluster-root');
                                if (clusterRoot) {
                                    const clusterId = clusterRoot.getAttribute('data-cluster-id') || '';
                                    return 'cluster|' + clusterId + '|' + el.tagName + '|' + (el.className || '') + '|' + (el.textContent || '').trim();
                                }

                                const zoomInButton = el.closest('.leaflet-control-zoom-in');
                                if (zoomInButton) {
                                    return 'zoomIn||' + el.tagName + '|' + (el.className || '') + '|' + (el.textContent || '').trim();
                                }

                                const zoomOutButton = el.closest('.leaflet-control-zoom-out');
                                if (zoomOutButton) {
                                    return 'zoomOut||' + el.tagName + '|' + (el.className || '') + '|' + (el.textContent || '').trim();
                                }

                                return 'none||' + el.tagName + '|' + (el.className || '') + '|' + (el.textContent || '').trim();
                            } catch (e) {
                                return 'error||ERROR|' + e + '|';
                            }
                        }

                        function setHome(lat, lon, zoom) {
                            if (!init()) {
                                return;
                            }

                            map.setView([lat, lon], clampZoomToLeafletLimits(Math.round(Number(zoom))));
                        }

                        function setStations(stationsJson) {
                            if (!init()) {
                                return;
                            }

                            stationData = JSON.parse(stationsJson);
                            stationsByCallsignRaw = {};

                            stationData.forEach(station => {
                                const callSignKey = getStationCallsignKey(station);
                                if (callSignKey) {
                                    stationsByCallsignRaw[callSignKey] = station;
                                }
                            });

                            renderStationMarkers();
                        }

                        function setBeam(beamJson) {
                            if (!init()) {
                                return;
                            }
                            beamLayer.clearLayers();

                            if (!beamJson || beamJson === 'null') {
                                return;
                            }

                            const points = JSON.parse(beamJson);
                            if (!points || points.length < 3) {
                                return;
                            }

                            const latLngs = points.map(point => [point.lat, point.lon]);

                            L.polygon(latLngs, {
                                pane: 'beamPane',
                                color: '#ff4d4d',
                                weight: 2,
                                fillColor: '#ff4d4d',
                                fillOpacity: 0.12,
                                interactive: false
                            }).addTo(beamLayer);
                        }

                        function setConnection(connectionJson) {
                            if (!init()) {
                                return;
                            }
                            connectionLayer.clearLayers();

                            if (!connectionJson || connectionJson === 'null') {
                                return;
                            }

                            const points = JSON.parse(connectionJson);
                            if (!points || points.length !== 2) {
                                return;
                            }

                            L.polyline(points.map(point => [point.lat, point.lon]), {
                                pane: 'connectionPane',
                                color: connectionColor(),
                                weight: 2,
                                dashArray: '6,6',
                                opacity: 0.85,
                                interactive: false
                            }).addTo(connectionLayer);
                        }
                        
                        function setProfileHoverPoint(point) {
                            if (!init()) {
                                return;
                            }
                
                            if (profileHoverMarker) {
                                map.removeLayer(profileHoverMarker);
                                profileHoverMarker = null;
                            }
                
                            if (!point || !isFinite(point.lat) || !isFinite(point.lon)) {
                                return;
                            }
                
                            profileHoverMarker = L.circleMarker([point.lat, point.lon], {
                                radius: 6,
                                color: '#ffcc00',
                                weight: 2,
                                fillColor: '#ffcc00',
                                fillOpacity: 0.85,
                                interactive: false
                            }).addTo(map);
                
                            if (point.label) {
                                profileHoverMarker.bindTooltip(point.label, {
                                    permanent: false,
                                    direction: 'top'
                                }).openTooltip();
                            }
                        }

                        function setGrid(gridJson) {
                            if (!init()) {
                                return;
                            }
                            gridLayer.clearLayers();

                            const cells = JSON.parse(gridJson);

                            cells.forEach(cell => {
                                const rectangle = L.rectangle(
                                    [
                                        [cell.southLat, cell.westLon],
                                        [cell.northLat, cell.eastLon]
                                    ],
                                    {
                                        pane: 'gridPane',
                                        color: gridLineColor(),
                                        opacity: gridLineOpacity(),
                                        weight: 1.4,
                                        fillOpacity: 0.0,
                                        interactive: false
                                    }
                                );

                                rectangle.addTo(gridLayer);

                                if (cell.showLabel) {
                                    const centerLat = (cell.southLat + cell.northLat) / 2.0;
                                    const centerLon = (cell.westLon + cell.eastLon) / 2.0;
                                    const labelFontPx = cell.labelFontPx || 12;

                                    L.marker([centerLat, centerLon], {
                                        pane: 'gridLabelPane',
                                        interactive: false,
                                        keyboard: false,
                                        icon: L.divIcon({
                                            className: 'maidenhead-grid-label-wrapper',
                                            html: '<div class="maidenhead-grid-label" style="font-size:' + labelFontPx + 'px;">' + escapeHtml(cell.locatorLabel) + '</div>',
                                            iconSize: [1, 1],
                                            iconAnchor: [0, 0]
                                        })
                                    }).addTo(gridLayer);
                                }
                            });
                        }

                        function focusCallsignRaw(callSignRaw) {
                            if (!map || !callSignRaw) {
                                return;
                            }

                            const marker = markersByCallsignRaw[callSignRaw];
                            if (marker) {
                                map.panTo(marker.getLatLng(), {
                                    animate: false
                                });

                                notifyViewport();
                                return;
                            }

                            /*
                             * If the station is currently hidden inside a cluster, there is no
                             * individual marker. Use the raw station data and zoom in far enough
                             * so clustering is disabled and the station becomes visible.
                             */
                            const station = stationsByCallsignRaw[callSignRaw];
                            if (station && isStationPositionValid(station)) {
                                const focusZoom = clampZoomToLeafletLimits(
                                    Math.max(Number(map.getZoom()), KST_CLUSTER_DISABLE_ZOOM)
                                );

                                map.setView([station.latitudeDeg, station.longitudeDeg], focusZoom, {
                                    animate: false
                                });

                                notifyViewport();
                            }
                        }
                         
                        return {
                            init: init,
                            invalidateSize: invalidateSize,
                            resize: resize,
                            zoomIn: zoomIn,
                            zoomOut: zoomOut,
                            inspectPoint: inspectPoint,
                            getViewportState: getViewportState,
                            setHome: setHome,
                            setStations: setStations,
                            setBeam: setBeam,
                            setConnection: setConnection,
                            setProfileHoverPoint: setProfileHoverPoint,
                            setGrid: setGrid,
                            focusCallsignRaw: focusCallsignRaw,
                            setTheme: setTheme
                        };
                    })();
                </script>
                </body>
                </html>
                """.replace("__TILE_PROXY_PORT__", String.valueOf(tileProxyPort));
    }
}