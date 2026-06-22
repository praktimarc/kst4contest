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
 * - JavaScript logs are forwarded to Java through javaMapBridge
 * - setTheme(light|dark) aligns the map with the JavaFX application theme
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

    public static String createStationMapHtml() {
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
                """ + leafletJs + """
                </script>

                <script>
                
                
                
                    window.kstMapApi = (function () {
                        let map;
                        let stationLayer;
                        let gridLayer;
                        let beamLayer;
                        let connectionLayer;
                        let profileHoverMarker;
                        let markersByCallsignRaw = {};
                        let activeTheme = 'light';
                        let invalidateNotifyTimer = 0;

                        function jsLog(message) {
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
                                zoomControl: true
                            }).setView([51.0, 10.0], 6);

                            jsLog('Leaflet map initialized');

                            const osmTileLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                maxZoom: 19,
                                attribution: '&copy; OpenStreetMap contributors'
                            });

                            osmTileLayer.on('tileload', function (event) {
                                jsLog('OSM tile loaded');
                            });

                            osmTileLayer.on('tileerror', function (event) {
                                const src = event && event.tile ? event.tile.src : 'unknown';
                                jsError('OSM tile load failed: ' + src);
                            });

                            osmTileLayer.addTo(map);

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
                                jsLog('zoomend -> ' + map.getZoom());
                                notifyViewport();
                            });

                            map.on('moveend', function () {
                                jsLog('moveend');
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
                
                             jsLog('invalidateSize');
                
                             map.invalidateSize(false);
                
                             if (invalidateNotifyTimer) {
                                 window.clearTimeout(invalidateNotifyTimer);
                             }
                
                             invalidateNotifyTimer = window.setTimeout(function () {
                                 notifyViewport();
                             }, 120);
                         }

                        function zoomIn() {
                            if (!map) {
                                return;
                            }

                            map.setZoom(map.getZoom() + 1, { animate: false });
                        }

                        function zoomOut() {
                            if (!map) {
                                return;
                            }

                            map.setZoom(map.getZoom() - 1, { animate: false });
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
                            jsLog('setHome lat=' + lat + ' lon=' + lon + ' zoom=' + zoom);
                            map.setView([lat, lon], zoom);
                        }

                        function setStations(stationsJson) {
                            if (!init()) {
                                return;
                            }

                            stationLayer.clearLayers();
                            markersByCallsignRaw = {};

                            const stations = JSON.parse(stationsJson);
                            jsLog('setStations count=' + stations.length);

                            stations.forEach(station => {
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
                                markersByCallsignRaw[station.callSignRaw] = marker;
                            });
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

                            jsLog('setBeam points=' + points.length);

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

                            jsLog('setConnection');

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
                            jsLog('setGrid cells=' + cells.length);

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
                             if (!marker) {
                                 jsLog('focusCallsignRaw skipped for ' + callSignRaw);
                                 return;
                             }
                
                             jsLog('focusCallsignRaw ' + callSignRaw);
                
                             map.panTo(marker.getLatLng(), {
                                 animate: false
                             });
                
                             notifyViewport();
                         }
                         
                         

                        return {
                              init: init,
                              invalidateSize: invalidateSize,
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
                """;
    }
}
