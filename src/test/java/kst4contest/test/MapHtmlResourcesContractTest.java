package kst4contest.test;

import kst4contest.view.map.MapHtmlResources;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapHtmlResourcesContractTest {

    @Test
    void moveEndUpdatesViewportWithoutRebuildingStationMarkers() {
        String html = MapHtmlResources.createStationMapHtml(12345);

        String handler = eventHandler(html, "moveend");

        assertTrue(handler.contains("notifyViewport();"));
        assertFalse(handler.contains("renderStationMarkers();"));
    }

    @Test
    void zoomEndStillRebuildsStationMarkersBeforeUpdatingViewport() {
        String html = MapHtmlResources.createStationMapHtml(12345);

        String handler = eventHandler(html, "zoomend");
        int markerRender = handler.indexOf("renderStationMarkers();");
        int viewportNotification = handler.indexOf("notifyViewport();");

        assertTrue(markerRender >= 0);
        assertTrue(viewportNotification > markerRender);
    }

    @Test
    void stationDataChangesStillRebuildStationMarkers() {
        String html = MapHtmlResources.createStationMapHtml(12345);

        int setterStart = html.indexOf("function setStations(stationsJson)");
        int nextFunction = html.indexOf("function setBeam(beamJson)", setterStart);
        String setterBody = html.substring(setterStart, nextFunction);

        assertTrue(setterBody.contains("stationData = JSON.parse(stationsJson);"));
        assertTrue(setterBody.contains("renderStationMarkers();"));
    }

    @Test
    void viewportNotificationForwardsBoundsAndZoomToJava() {
        String html = MapHtmlResources.createStationMapHtml(12345);

        String functionBody = functionBody(html, "function notifyViewport()", "function gridLineColor()");

        assertTrue(functionBody.contains("const bounds = map.getBounds();"));
        assertTrue(functionBody.contains("const zoom = map.getZoom();"));
        assertTrue(functionBody.contains("window.javaMapBridge.onViewportChanged("));
        assertTrue(functionBody.contains("bounds.getSouth(),"));
        assertTrue(functionBody.contains("bounds.getWest(),"));
        assertTrue(functionBody.contains("bounds.getNorth(),"));
        assertTrue(functionBody.contains("bounds.getEast(),"));
    }

    @Test
    void stationHitTestingKeepsRawCallsignSelectionContract() {
        String html = MapHtmlResources.createStationMapHtml(12345);

        String functionBody = functionBody(html, "function inspectPoint(x, y)", "function setHome(lat, lon, zoom)");

        assertTrue(functionBody.contains("el.closest('.station-marker-root')"));
        assertTrue(functionBody.contains("stationRoot.getAttribute('data-callsignraw')"));
        assertTrue(functionBody.contains("return 'station|' + callSignRaw"));
    }

    @Test
    void stationClusteringCanBeToggledWithoutReplacingStationData() {
        String html = MapHtmlResources.createStationMapHtml(12345);

        assertTrue(html.contains("let stationClusteringEnabled = true;"));
        assertTrue(html.contains("if (!stationClusteringEnabled"));
        assertTrue(html.contains("|| Number(map.getZoom()) >= KST_CLUSTER_DISABLE_ZOOM)"));
        assertTrue(html.contains("function setStationClusteringEnabled(enabled)"));

        int setterStart = html.indexOf("function setStationClusteringEnabled(enabled)");
        int setterEnd = html.indexOf('}', setterStart);
        String setterBody = html.substring(setterStart, setterEnd);
        int stateUpdate = setterBody.indexOf("stationClusteringEnabled = Boolean(enabled);");
        int markerRender = setterBody.indexOf("renderStationMarkers();");

        assertTrue(stateUpdate >= 0);
        assertTrue(markerRender > stateUpdate);
        assertFalse(setterBody.contains("stationData ="));
        assertTrue(html.contains("setStationClusteringEnabled: setStationClusteringEnabled"));
    }

    private static String eventHandler(String html, String eventName) {
        String signature = "map.on('" + eventName + "', function () {";
        int handlerStart = html.indexOf(signature);
        int handlerEnd = html.indexOf("});", handlerStart);

        assertTrue(handlerStart >= 0);
        assertTrue(handlerEnd > handlerStart);
        return html.substring(handlerStart, handlerEnd);
    }

    private static String functionBody(String html, String signature, String nextSignature) {
        int functionStart = html.indexOf(signature);
        int functionEnd = html.indexOf(nextSignature, functionStart);

        assertTrue(functionStart >= 0);
        assertTrue(functionEnd > functionStart);
        return html.substring(functionStart, functionEnd);
    }
}
