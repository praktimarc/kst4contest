package kst4contest.view.compose

import kst4contest.controller.Utils4KST

/**
 * How the message tables show what arrives off the wire.
 *
 * Shared rather than repeated: the same value appears in the monitor window and in the
 * tabs below the station list, and two tables of the same data disagreeing about how
 * they show it is worse than either format on its own.
 */
internal object MessageFormats {

    /**
     * The epoch seconds off the wire as a clock time, the way the JavaFX cells showed
     * them. An unreadable value is shown as it came: an operator seeing the raw text
     * learns more than one seeing an empty cell.
     */
    fun clockTime(epochFromServer: String?): String {
        val raw = epochFromServer?.trim().orEmpty()

        if (raw.isEmpty()) {
            return ""
        }

        return runCatching { Utils4KST().time_convertEpochToReadable(raw) }.getOrDefault(raw)
    }

    /**
     * Three decimal places, as `formatQrgForUi` pads them in the main window. The same
     * column appears in both windows, and two tables of the same data must not
     * disagree about how they show it.
     */
    fun paddedQrg(raw: String): String {
        val text = raw.trim().replace(',', '.')

        if (text.isEmpty() || text.toDoubleOrNull() == null) {
            return text
        }

        val dot = text.indexOf('.')

        if (dot < 0) {
            return text + ".000"
        }

        val decimals = text.length - dot - 1

        return if (decimals >= 3) text else text + "0".repeat(3 - decimals)
    }

}
