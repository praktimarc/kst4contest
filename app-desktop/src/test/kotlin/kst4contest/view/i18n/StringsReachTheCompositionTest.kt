package kst4contest.view.i18n

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * A language change reaches a composed window without anything else happening.
 *
 * The one mechanism the acceptance criterion hangs on, and the one a state-level test cannot
 * see: the store is always right, the question is whether the composition asks it again.
 * Etappe 8 shipped exactly this fault for the palette, and found it with a test shaped like
 * this one.
 *
 * Headless: no window is opened, only a composition.
 */
class StringsReachTheCompositionTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aLanguageChosenAfterTheFirstDrawReachesTheComposition() = runComposeUiTest {
        var stored = SYSTEM_LANGUAGE
        val store = LanguageStore(
            storedLanguage = { stored },
            storeLanguage = { stored = it },
            systemDefault = Locale.UK,
        )
        val seen = mutableListOf<String>()

        setContent {
            /*
             * store.strings is read INSIDE the composition, which is what makes the state
             * read belong to it. Reading it outside and passing the result in would hand the
             * composition a value that never changes again -- the remember(darkMode) fault.
             */
            CompositionLocalProvider(LocalStrings provides store.strings) {
                seen += LocalStrings.current.settingsSave
            }
        }

        waitForIdle()
        val firstDraw = seen.last()

        store.use("de")
        waitForIdle()

        assertEquals("Save settings", firstDraw, "the first draw was not the base language")
        assertEquals(
            "Einstellungen speichern",
            seen.last(),
            "the composition still holds the texts from before the change",
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun withoutAStoreTheBaseLanguageIsStillDrawn() {
        /*
         * Not caution: the operator profile picker composes before a profile, and therefore
         * before a store, exists. It must still have texts.
         */
        runComposeUiTest {
            var seen: String? = null

            setContent { seen = LocalStrings.current.settingsSave }

            waitForIdle()

            assertEquals("Save settings", seen)
        }
    }
}
