package kst4contest.view.map;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The one line of text above the map.
 *
 * It is the only place the operator is told how many stations are on the map at all,
 * whether a filter is hiding some of them, and what is known about the one they picked.
 * The Compose port showed "Selected: F5JMI" and nothing else.
 *
 * Pinned against updateStatusLabel() in StationMapView.java, which is what an operator
 * has been reading since release 1.4x.
 */
class StationMapStatusTextTest {

    private MapCallsignRawSnapshot snapshot(String call,
                                            String locator,
                                            String bandSummary,
                                            boolean offersSelectedBand,
                                            double qrbKm,
                                            double qtfDeg,
                                            Map<String, String> frequencies) {
        return new MapCallsignRawSnapshot(
                call, call, locator, 51.0, 10.0, bandSummary, frequencies,
                offersSelectedBand, false, false, true, qrbKm, qtfDeg, 0, 0L
        );
    }

    @Test
    void without_a_selection_it_reports_only_the_count() {
        assertEquals(
                "Showing 49 visible stations",
                StationMapStatusText.build(49, false, null)
        );
    }

    @Test
    void a_filtered_view_says_so_because_the_count_alone_would_mislead() {
        assertEquals(
                "Showing 12 visible stations | filtered view active",
                StationMapStatusText.build(12, true, null)
        );
    }

    @Test
    void a_selected_station_adds_its_callsign_locator_distance_and_bearing() {
        MapCallsignRawSnapshot selected =
                snapshot("PA3EKM", "JO33GE", "", false, 316.4, 251.0, Map.of());

        assertEquals(
                "Showing 50 visible stations | Selected: PA3EKM | JO33GE | 316 km / 251° | Bands: - | QRG: -",
                StationMapStatusText.build(50, false, selected)
        );
    }

    @Test
    void the_band_summary_is_marked_when_the_station_offers_the_selected_band() {
        MapCallsignRawSnapshot selected =
                snapshot("DG1BHA", "JO43", "144, 432, 1296", true, 100.0, 90.0, Map.of());

        assertEquals(
                "Showing 1 visible stations | Selected: DG1BHA | JO43 | 100 km / 90° | Bands: 144, 432, 1296 B+ | QRG: -",
                StationMapStatusText.build(1, false, selected)
        );
    }

    /** The detail text is multi-line; the status bar is one line. */
    @Test
    void known_frequencies_are_folded_onto_one_line() {
        Map<String, String> frequencies = new LinkedHashMap<>();
        frequencies.put("144", "144.300");
        frequencies.put("432", "432.200");

        MapCallsignRawSnapshot selected =
                snapshot("DK2EA", "JO50UF", "144", false, 79.0, 125.0, frequencies);

        String status = StationMapStatusText.build(3, false, selected);

        assertEquals(
                "Showing 3 visible stations | Selected: DK2EA | JO50UF | 79 km / 125° | Bands: 144"
                        + " | QRG: 144: 144.300 432: 432.200",
                status
        );
        assertFalse(status.contains("\n"), "the status bar is a single line");
    }

    @Test
    void a_station_without_a_locator_is_not_given_an_empty_field() {
        MapCallsignRawSnapshot selected =
                snapshot("DL1ABC", "", "144", false, 50.0, 10.0, Map.of());

        assertEquals(
                "Showing 2 visible stations | Selected: DL1ABC | 50 km / 10° | Bands: 144 | QRG: -",
                StationMapStatusText.build(2, false, selected)
        );
    }

    /**
     * QRB and QTF can be absent. Absent is not zero, and "NaN km" is not a reading an
     * operator should be shown.
     */
    @Test
    void an_unknown_distance_or_bearing_is_shown_as_unavailable() {
        MapCallsignRawSnapshot selected =
                snapshot("DL1ABC", "JO50", "144", false, Double.NaN, Double.NaN, Map.of());

        assertEquals(
                "Showing 2 visible stations | Selected: DL1ABC | JO50 | - / - | Bands: 144 | QRG: -",
                StationMapStatusText.build(2, false, selected)
        );
    }

    @Test
    void the_count_is_the_number_of_markers_the_map_was_given() {
        List<MapCallsignRawSnapshot> markers = List.of(
                snapshot("A", "JO50", "", false, 1.0, 1.0, Map.of()),
                snapshot("B", "JO50", "", false, 1.0, 1.0, Map.of())
        );

        assertEquals(
                "Showing 2 visible stations",
                StationMapStatusText.build(markers.size(), false, null)
        );
    }

    /**
     * "Known to be unknown" is information. The JavaFX line appended " | QRG: -"
     * because "-" is not blank, and dropping the segment instead leaves the operator
     * unable to tell a frequency that is not known from one that fell off the end of an
     * ellipsis.
     */
    @Test
    void a_station_with_no_known_frequency_still_gets_a_qrg_field() {
        MapCallsignRawSnapshot selected =
                snapshot("DL1ABC", "JO50", "144", false, 50.0, 10.0, Map.of());

        assertEquals(
                "Showing 2 visible stations | Selected: DL1ABC | JO50 | 50 km / 10° | Bands: 144 | QRG: -",
                StationMapStatusText.build(2, false, selected)
        );
    }
}
