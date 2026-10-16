package kst4contest.controller;

import java.util.Optional;

/**
 * Decides which radio a received report belongs to. Pure, so the rule is tested without
 * a network connection; the receivers ask it and write to the returned slot.
 *
 * <p>Without SO2R every report goes to radio 1, as before SO2R existed. An empty result
 * means the report is dropped.</p>
 */
public final class RadioSlotRouter {

    private RadioSlotRouter() {
    }

    /**
     * @param radioNr the RadioInfo {@code RadioNr} text, null when the packet has none
     */
    public static Optional<RadioSlot> forRadioInfo(boolean so2rEnabled, String radioNr) {
        if (!so2rEnabled || radioNr == null || radioNr.isBlank()) {
            return Optional.of(RadioSlot.FIRST);
        }
        return switch (radioNr.trim()) {
            case "1" -> Optional.of(RadioSlot.FIRST);
            case "2" -> Optional.of(RadioSlot.SECOND);
            default -> Optional.empty();
        };
    }

    /**
     * @param nameRadio1 the existing station filter; empty accepts any station for radio 1
     * @param nameRadio2 the station of radio 2; empty means radio 2 receives nothing
     */
    public static Optional<RadioSlot> forWinTestStation(
            boolean so2rEnabled, String stationName, String nameRadio1, String nameRadio2) {
        String station = stationName == null ? "" : stationName;
        boolean radio1Accepts = nameRadio1 == null || nameRadio1.isBlank()
                || station.equalsIgnoreCase(nameRadio1);

        if (so2rEnabled && nameRadio2 != null && !nameRadio2.isBlank()
                && station.equalsIgnoreCase(nameRadio2)) {
            return Optional.of(RadioSlot.SECOND);
        }
        return radio1Accepts ? Optional.of(RadioSlot.FIRST) : Optional.empty();
    }
}
