package kst4contest.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import kst4contest.model.AirPlane;
import kst4contest.model.AirPlaneReflectionInfo;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatPreferences;

/**
 * Resolves variables used in operator messages, shortcuts, snippets and
 * beacons.
 *
 * <p>Global variables only depend on the local station configuration and can
 * therefore be used in every message context. Station variables additionally
 * require a selected remote station. Keeping both groups in one resolver
 * prevents the send field and the beacon tasks from implementing different
 * replacement rules.</p>
 */
public final class MessageVariableResolver {

    private static final int SHORT_VALUE_LENGTH = 7;

    private final ChatPreferences chatPreferences;

    /**
     * Creates a resolver backed by the live application preferences.
     *
     * @param chatPreferences preferences that provide current station values
     */
    public MessageVariableResolver(ChatPreferences chatPreferences) {
        this.chatPreferences = Objects.requireNonNull(chatPreferences, "chatPreferences");
    }

    /**
     * Resolves variables which do not require a selected remote station.
     *
     * <p>The replacement is literal rather than regular-expression based.
     * Callsigns, locators and frequencies are data, not regular expressions.</p>
     *
     * @param template text that may contain variables
     * @return text with all available global variables resolved, or {@code null}
     *         when the supplied template is {@code null}
     */
    public String resolveGlobalVariables(String template) {
        if (template == null) {
            return null;
        }

        String primaryFrequency = valueOrEmpty(chatPreferences.getMYQRGFirstCat().getValue());
        String secondaryFrequency = valueOrEmpty(chatPreferences.getMYQRGSecondCat().getValue());
        String ownLocator = valueOrEmpty(chatPreferences.getStn_loginLocatorMainCat());

        String resolvedText = template;
        resolvedText = resolvedText.replace("MYQRGSHORT", abbreviate(primaryFrequency));
        resolvedText = resolvedText.replace("MYQRG", primaryFrequency);
        resolvedText = resolvedText.replace("SECONDQRG", secondaryFrequency);
        resolvedText = resolvedText.replace("MYLOCATORSHORT", abbreviateLocator(ownLocator));
        resolvedText = resolvedText.replace("MYLOCATOR", ownLocator);
        resolvedText = resolvedText.replace("MYCALL", valueOrEmpty(chatPreferences.getStn_loginCallSign()));
        resolvedText = resolvedText.replace("MYQTF", formatHeading(chatPreferences.getActualQTF().getValue().doubleValue()));
        return resolvedText;
    }

    /**
     * Resolves global variables and variables derived from a selected station.
     *
     * <p>If no station is selected, station-specific placeholders remain visible.
     * This is intentional: silently removing {@code QRZNAME}, {@code FIRSTAP} or
     * {@code SECONDAP} could create a plausible-looking but incomplete message.</p>
     *
     * @param template text that may contain variables
     * @param selectedStation currently selected remote station, may be {@code null}
     * @return resolved message text
     */
    public String resolveForSelectedStation(String template, ChatMember selectedStation) {
        String resolvedText = resolveGlobalVariables(template);

        if (resolvedText == null || selectedStation == null) {
            return resolvedText;
        }

        resolvedText = resolvedText.replace("QRZNAME", resolveStationName(selectedStation));
        resolvedText = resolvedText.replace("FIRSTAP", resolveFirstAirPlane(selectedStation));
        resolvedText = resolvedText.replace("SECONDAP", resolveSecondAirPlane(selectedStation));
        return resolvedText;
    }

    private String resolveStationName(ChatMember selectedStation) {
        String stationName = valueOrEmpty(selectedStation.getName()).trim();

        if (!stationName.isEmpty()) {
            return stationName;
        }

        return valueOrEmpty(selectedStation.getCallSign());
    }

    private String resolveFirstAirPlane(ChatMember selectedStation) {
        List<AirPlane> risingAirPlanes = getRisingAirPlanes(selectedStation);

        if (risingAirPlanes.isEmpty()) {
            return "no ap available";
        }

        AirPlane firstAirPlane = risingAirPlanes.get(0);
        return "a " + firstAirPlane.getPotencialDescriptionAsWord()
                + " in " + firstAirPlane.getArrivingDurationMinutes() + " min";
    }

    private String resolveSecondAirPlane(ChatMember selectedStation) {
        List<AirPlane> risingAirPlanes = getRisingAirPlanes(selectedStation);

        if (risingAirPlanes.size() < 2) {
            return "";
        }

        AirPlane secondAirPlane = risingAirPlanes.get(1);
        return "Next " + secondAirPlane.getPotencialDescriptionAsWord()
                + " in " + secondAirPlane.getArrivingDurationMinutes() + " min";
    }

    private List<AirPlane> getRisingAirPlanes(ChatMember selectedStation) {
        AirPlaneReflectionInfo reflectionInfo = selectedStation.getAirPlaneReflectInfo();

        if (reflectionInfo == null || reflectionInfo.getRisingAirplanes() == null) {
            return List.of();
        }

        return reflectionInfo.getRisingAirplanes();
    }

    private String abbreviate(String value) {
        return value.substring(0, Math.min(value.length(), SHORT_VALUE_LENGTH));
    }

    private String abbreviateLocator(String locator) {
        return locator.substring(0, Math.min(locator.length(), 4));
    }

    private String formatHeading(double headingDegrees) {
        if (!Double.isFinite(headingDegrees)) {
            return "";
        }

        return BigDecimal.valueOf(headingDegrees).stripTrailingZeros().toPlainString();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}