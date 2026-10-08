package kst4contest.view.map;

import java.util.Locale;

/**
 * The one line of text above the station map.
 *
 * It is the only place the operator is told how many stations are on the map, whether a
 * filter is hiding some of them, and what is known about the one they picked. It lives
 * in core rather than beside the window because it is string building with rules, and
 * because the Compose port lost all of it but the callsign — a loss no test could see
 * while it was written inline in the window.
 *
 * The wording and the separators are the ones {@code StationMapView.updateStatusLabel()}
 * has produced since release 1.4x and are not ours to change casually.
 */
public final class StationMapStatusText {

    private static final String SEPARATOR = " | ";
    private static final String UNAVAILABLE = "-";

    private StationMapStatusText() {
    }

    /**
     * @param visibleStationCount how many markers the map was handed
     * @param filteredViewActive whether a filter is hiding stations, without which the
     *        count reads as "that is everyone on the band"
     * @param selected the station the operator picked, or null
     */
    public static String build(int visibleStationCount,
                               boolean filteredViewActive,
                               MapCallsignRawSnapshot selected) {

        StringBuilder text = new StringBuilder();
        text.append("Showing ").append(visibleStationCount).append(" visible stations");

        if (filteredViewActive) {
            text.append(SEPARATOR).append("filtered view active");
        }

        if (selected == null) {
            return text.toString();
        }

        text.append(SEPARATOR).append("Selected: ").append(selected.displayCallSign());

        if (!selected.locator6().isBlank()) {
            text.append(SEPARATOR).append(selected.locator6());
        }

        text.append(SEPARATOR)
                .append(distanceAndBearingText(selected.qrbKm(), selected.qtfDeg()));

        text.append(SEPARATOR).append("Bands: ").append(bands(selected));

        /*
         * "-" is kept rather than dropped. A station whose frequency is not known is
         * different from one whose frequency fell off the end of an ellipsis, and the
         * operator can only tell the two apart if the field is there.
         */
        String frequencies = singleLine(selected.detailFrequencyText());
        if (!frequencies.isBlank()) {
            text.append(SEPARATOR).append("QRG: ").append(frequencies);
        }

        return text.toString();
    }

    /**
     * Distance and bearing can both be absent, and absent is not zero.
     *
     * Public because the JavaFX status label shows the same pair and must not disagree
     * with this one while both windows exist.
     */
    public static String distanceAndBearingText(double qrbKm, double qtfDeg) {
        return formatRounded(qrbKm, " km") + " / " + formatRounded(qtfDeg, "°");
    }

    private static String formatRounded(double value, String unit) {
        if (!Double.isFinite(value)) {
            return UNAVAILABLE;
        }
        return String.format(Locale.US, "%.0f", value) + unit;
    }

    private static String bands(MapCallsignRawSnapshot selected) {
        String bandText = selected.bandSummary().isBlank() ? UNAVAILABLE : selected.bandSummary();
        return selected.offersSelectedBand() ? bandText + " B+" : bandText;
    }

    /** The detail text is written over several lines; the status bar has one. */
    private static String singleLine(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
    }
}
