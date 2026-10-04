package kst4contest.view.compose

import kst4contest.model.UpdateInformation
import kst4contest.utils.VersionUtils

/**
 * One branch of the update window's tree: a heading and the lines beneath it.
 *
 * Both branches of that tree have the same shape. The change log and the bug list
 * arrive as arrays whose first element is the heading and whose remaining elements are
 * the lines, so one type serves both.
 */
data class UpdateSection(val title: String, val entries: List<String>)

/**
 * What the update window shows, and whether it opens at all.
 *
 * Free of any toolkit so the decision can be tested. The decision is the interesting
 * part: an update window that opens when there is no update is a daily annoyance, and
 * one that stays shut when there is an update defeats the whole mechanism.
 *
 * @param currentVersion the running version, `ApplicationConstants.APPLICATION_CURRENT_VERSION`
 * @param currentVersionNumber the running version as a number, for the older
 *        comparison that is still used when the server reports no semantic version
 */
class UpdateWindowState(
    private val updateInformation: UpdateInformation?,
    private val currentVersion: String,
    private val currentVersionNumber: Double,
) {

    /**
     * Whether an update exists.
     *
     * Two comparisons, as in the JavaFX window: a semantic one when the server reports
     * a semantic version, and the older numeric one otherwise. Absent update
     * information means no update — the check runs at startup, and a failed download
     * must not stop the application from coming up.
     */
    val updateAvailable: Boolean
        get() {
            val information = updateInformation ?: return false

            return if (information.hasSemanticVersion()) {
                VersionUtils.compareStableVersions(
                    information.latestSemanticVersionOnServer,
                    currentVersion,
                ) > 0
            } else {
                information.latestVersionNumberOnServer > currentVersionNumber
            }
        }

    /** "KST4Contest 1.50.0", as the window prints it. */
    val installedVersion: String
        get() = DISPLAY_NAME + " " + currentVersion

    val latestVersion: String
        get() = DISPLAY_NAME + " " + (updateInformation?.latestVersionForDisplay ?: "")

    val majorChanges: String
        get() = updateInformation?.majorChanges ?: ""

    val adminMessage: String
        get() = updateInformation?.adminMessage ?: ""

    /** Opened in the system browser by the "Open release page" link. */
    val releasePageUrl: String
        get() = updateInformation?.latestVersionPathOnWebserver ?: ""

    val changeLog: List<UpdateSection>
        get() = sectionsOf(updateInformation?.changeLog)

    val knownBugs: List<UpdateSection>
        get() = sectionsOf(updateInformation?.bugList)

    /**
     * Turns the stored arrays into sections.
     *
     * A row with nothing but a heading yields a section without lines rather than being
     * dropped: the heading is the version, and a version that brought no listed change
     * is still worth showing as released.
     */
    private fun sectionsOf(rows: List<Array<String>>?): List<UpdateSection> =
        rows.orEmpty()
            .filter { it.isNotEmpty() }
            .map { row -> UpdateSection(row.first(), row.drop(1)) }

    companion object {
        /**
         * The window prints "KST4Contest", not `APPLICATION_NAME`. That constant is
         * "praktiKST" and names the application directory, not the product.
         */
        const val DISPLAY_NAME = "KST4Contest"
    }
}
