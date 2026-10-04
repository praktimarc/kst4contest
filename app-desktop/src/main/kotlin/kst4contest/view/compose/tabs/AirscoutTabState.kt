package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences

/**
 * The Airscout tab as a typed facade over ChatPreferences.
 *
 * Every property writes through immediately and there is deliberately no apply(), the
 * way the JavaFX controls behaved.
 *
 * Three properties are guarded before the write, and the guard is not decoration. The
 * ChatPreferences setters do not keep the previous value on bad input, they substitute
 * the factory default: an empty identifier becomes "AS"/"KST", a port outside 1..65535
 * becomes 9872, a band value that is not a positive number becomes "1440000". The
 * JavaFX window never let that happen — it validated on focus loss and put the stored
 * value back into the field. Writing through on every keystroke without the same guard
 * would silently reset an operator's configuration while he is typing.
 *
 * The accepted ranges are the JavaFX ones, unchanged: AirScout ports and the band
 * value are protocol values and neither the defaults nor the bounds are touched.
 */
class AirscoutTabState(private val prefs: ChatPreferences) {

    var airScout_asUDPListenerEnabled: Boolean
        get() = prefs.isAirScout_asUDPListenerEnabled()
        set(value) { prefs.setAirScout_asUDPListenerEnabled(value) }

    /** Logical identifier of the target AirScout server; invalid input is ignored. */
    var airScout_asServerNameString: String
        get() = prefs.getAirScout_asServerNameString()
        set(value) {
            if (isValidIdentifier(value)) {
                prefs.setAirScout_asServerNameString(value.trim())
            }
        }

    /** Identifier of this KST4Contest instance; invalid input is ignored. */
    var airScout_asClientNameString: String
        get() = prefs.getAirScout_asClientNameString()
        set(value) {
            if (isValidIdentifier(value)) {
                prefs.setAirScout_asClientNameString(value.trim())
            }
        }

    /**
     * AirScout UDP port, default 9872. Must match the AirScout network settings, and a
     * change takes effect after a reconnect — the JavaFX tab said so and nothing about
     * that changes here.
     */
    var airScout_asCommunicationPort: Int
        get() = prefs.getAirScout_asCommunicationPort()
        set(value) {
            if (isValidPort(value)) {
                prefs.setAirScout_asCommunicationPort(value)
            }
        }

    /**
     * Pick the AirScout frequency per station instead of forcing one value. Uses the
     * station's current QRG first, then station-name and chat-category evidence.
     */
    var airScout_autoBandSelectionEnabled: Boolean
        get() = prefs.isAirScout_autoBandSelectionEnabled()
        set(value) { prefs.setAirScout_autoBandSelectionEnabled(value) }

    /**
     * The forced AirScout band value, a protocol number such as 1440000 for 144 MHz.
     * Stored as the protocol spells it, which is what Long.toString of the parsed
     * value produced in the JavaFX field.
     */
    var airScout_asBandString: String
        get() = prefs.getAirScout_asBandString()
        set(value) {
            parseBandValue(value)?.let { prefs.setAirScout_asBandString(it) }
        }

    /**
     * False while the frequency is selected per station: the JavaFX band field was
     * disabled then, because a forced value is the fallback for exactly that case.
     */
    val bandValueEditable: Boolean
        get() = !airScout_autoBandSelectionEnabled

    companion object {

        /**
         * AirScout encloses the identifiers in quotation marks, so an empty identifier,
         * a quotation mark or a line break would produce an invalid protocol message.
         */
        @JvmStatic
        fun isValidIdentifier(candidate: String?): Boolean {
            val normalized = candidate?.trim() ?: return false
            return normalized.isNotEmpty() &&
                !normalized.contains("\"") &&
                !normalized.contains("\r") &&
                !normalized.contains("\n")
        }

        /** The range the JavaFX field accepted. Not narrowed. */
        @JvmStatic
        fun isValidPort(candidate: Int): Boolean = candidate in 1..65535

        /** The normalized band value, or null when it is not a positive number. */
        @JvmStatic
        fun parseBandValue(candidate: String?): String? {
            val parsed = candidate?.trim()?.toLongOrNull() ?: return null
            return if (parsed > 0) parsed.toString() else null
        }
    }
}
