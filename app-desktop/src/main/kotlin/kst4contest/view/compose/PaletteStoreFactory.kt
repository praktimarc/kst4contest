package kst4contest.view.compose

import kst4contest.ApplicationConstants
import kst4contest.controller.OperatorProfilePaths
import kst4contest.model.ChatPreferences
import kst4contest.model.OperatorProfile
import kst4contest.utils.ApplicationFileUtils
import java.io.File

/**
 * Where one profile's own stylesheets live — layer 2 of the palette, one file per design.
 *
 * A value rather than a path derived inside the factory, so the resolution can be tested
 * without writing into the operator's home directory.
 */
data class OperatorProfilePaletteFiles(val dayFile: File, val eveningFile: File) {

    /** The file for a design. */
    fun fileFor(darkMode: Boolean): File = if (darkMode) eveningFile else dayFile

    companion object {

        /**
         * The files of a profile, by the project's own path convention.
         *
         * @param profile the profile whose stylesheets are wanted
         * @return both files, which need not exist
         */
        @JvmStatic
        fun of(profile: OperatorProfile): OperatorProfilePaletteFiles =
            OperatorProfilePaletteFiles(
                dayFile = absoluteFile(OperatorProfilePaths.paletteRelativeFileName(profile, false)),
                eveningFile = absoluteFile(OperatorProfilePaths.paletteRelativeFileName(profile, true)),
            )

        private fun absoluteFile(relativeName: String): File =
            File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, relativeName))
    }
}

/**
 * Builds the [PaletteStore] of the active profile.
 *
 * The store itself takes four lambdas so it can be tested without a profile on disk; this is
 * where those lambdas are tied to the real preferences and the real files, and it is the only
 * place that knows which preference belongs to which design.
 */
object PaletteStoreFactory {

    /**
     * Creates the store for one profile.
     *
     * Changes write through to [prefs] immediately, which is what recolours the open windows.
     * The XML is written by the existing "save settings" path, exactly like every other
     * setting in this application.
     *
     * @param prefs the active profile's preferences
     * @param files the active profile's own stylesheets, which need not exist
     * @return a store for both designs
     */
    @JvmStatic
    fun create(prefs: ChatPreferences, files: OperatorProfilePaletteFiles): PaletteStore =
        PaletteStore(
            shippedOf = { darkMode -> JavaFxStylesheet.shipped(darkMode) },
            fileRolesOf = { darkMode ->
                JavaFxStylesheet.declaredRolesOfFile(files.fileFor(darkMode))
            },
            storedOverridesOf = { darkMode ->
                if (darkMode) {
                    prefs.guiOptions_paletteOverridesEvening
                } else {
                    prefs.guiOptions_paletteOverridesDay
                }
            },
            storeOverrides = { darkMode, encoded ->
                if (darkMode) {
                    prefs.guiOptions_paletteOverridesEvening = encoded
                } else {
                    prefs.guiOptions_paletteOverridesDay = encoded
                }
            },
        )

    /**
     * Creates the store for the active profile, or for the root profile when no profile has
     * been resolved yet.
     *
     * @param prefs the active profile's preferences
     * @param profile the active profile
     * @return a store for both designs
     */
    @JvmStatic
    fun create(prefs: ChatPreferences, profile: OperatorProfile): PaletteStore =
        create(prefs, OperatorProfilePaletteFiles.of(profile))
}
