package kst4contest.test;

import kst4contest.view.map.MapHtmlResources;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapHtmlResourcesContractTest {

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
}
