package kst4contest.view

import kst4contest.controller.OperatorProfilePaths
import kst4contest.view.compose.JavaFxStylesheet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The file the startup copy writes is the file the root profile's palette layer reads.
 *
 * These were two independent literals of the same string in two classes, and they had to stay
 * equal for the root profile's own stylesheet to be read at all -- the copy is written under
 * one name and resolved under the other. A rename on either side would silently restore the
 * defect this stage was commissioned to fix: a file written at every startup that nothing
 * ever reads. The build would stay green and no other test would notice.
 *
 * Both now derive from `ApplicationConstants`, so there is nothing left to drift. This pins
 * that it stays that way.
 */
class StartupStylesheetCopyTest {

    private val rootProfile = OperatorProfilePaths.buildRootProfile("Default")

    @Test
    fun theCopyTargetIsWhatTheRootProfilesPaletteLayerReads() {
        assertEquals(
            OperatorProfilePaths.paletteRelativeFileName(rootProfile, false),
            Kst4ContestApplication.STYLE_DEFAULTCSSDAY_FILE,
        )
        assertEquals(
            OperatorProfilePaths.paletteRelativeFileName(rootProfile, true),
            Kst4ContestApplication.STYLE_DEFAULTCSSEVENING_FILE,
        )
    }

    @Test
    fun theCopySourceIsTheSheetTheThemeReads() {
        // Written from one resource and read from another would be the same defect, inverted.
        assertEquals(
            JavaFxStylesheet.DAYLIGHT_RESOURCE,
            Kst4ContestApplication.STYLE_DEFAULTCSSDAY_RESOURCE,
        )
        assertEquals(
            JavaFxStylesheet.EVENING_RESOURCE,
            Kst4ContestApplication.STYLE_DEFAULTCSSEVENING_RESOURCE,
        )
    }

    @Test
    fun theResourceIsTheFileNameWithALeadingSlash() {
        assertEquals(
            "/" + Kst4ContestApplication.STYLE_DEFAULTCSSDAY_FILE,
            Kst4ContestApplication.STYLE_DEFAULTCSSDAY_RESOURCE,
        )
    }
}
