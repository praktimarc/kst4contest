package kst4contest.view.compose

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.model.PaletteRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A colour change reaches a composed window without anything else happening.
 *
 * This is the one mechanism the stage's acceptance criterion hangs on, and it used not to
 * work: `Kst4ContestTheme` took its palette from `remember(darkMode)`, so a change reached
 * nothing until somebody toggled day/evening. A state-level test of the store cannot see
 * that fault -- the store was always right, the theme never asked it again.
 *
 * Headless: no window is opened, only a composition.
 */
class ThemeReadsPaletteStoreTest {

    private var stored = ""

    private fun store() = PaletteStore(
        shippedOf = { JavaFxStylesheet.read("/KST4ContestDefaultDay.css") },
        fileRolesOf = { emptyMap() },
        storedOverridesOf = { stored },
        storeOverrides = { _, encoded -> stored = encoded },
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aColourSetAfterTheFirstDrawReachesTheComposition() = runComposeUiTest {
        val store = store()
        val seen = mutableListOf<Color>()

        setContent {
            CompositionLocalProvider(LocalPaletteStore provides store) {
                Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                    // Reading the palette is what a control does; record what it was handed.
                    seen += LocalJavaFxPalette.current.base
                }
            }
        }

        waitForIdle()
        val firstDraw = seen.last()

        store.set(darkMode = false, role = PaletteRole.SURFACE, colour = "#FF0000")
        waitForIdle()

        assertEquals(
            Color(0xFFFF0000),
            seen.last(),
            "the composition still holds the palette from before the change, which is the "
                    + "remember(darkMode) fault this store exists to fix",
        )
        assertEquals(store.shipped(false).base, firstDraw, "the first draw was not the shipped base")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun withoutAStoreTheShippedPaletteIsStillDrawn() {
        /*
         * Not caution: the operator profile picker composes before a profile, and therefore
         * before a store, exists. It must still have colours.
         */
        runComposeUiTest {
            var seen: Color? = null

            setContent {
                Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                    seen = LocalJavaFxPalette.current.base
                }
            }

            waitForIdle()

            assertEquals(JavaFxStylesheet.read("/KST4ContestDefaultDay.css").base, seen)
        }
    }
}
