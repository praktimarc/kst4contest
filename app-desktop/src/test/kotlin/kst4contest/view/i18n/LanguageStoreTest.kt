package kst4contest.view.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * The store, and the two ways out of it.
 *
 * Built over two lambdas rather than over ChatPreferences, so none of this touches the
 * operator's home directory: that constructor copies resources into it.
 */
class LanguageStoreTest {

    private var stored: String = SYSTEM_LANGUAGE

    private fun store(systemDefault: Locale = Locale.UK) = LanguageStore(
        storedLanguage = { stored },
        storeLanguage = { stored = it },
        systemDefault = systemDefault,
    )

    @Test
    fun theStoredLanguageIsInForceFromTheStart() {
        stored = "de"

        assertEquals("de", store().language)
    }

    @Test
    fun anEmptySettingFollowsTheSystem() {
        stored = SYSTEM_LANGUAGE

        assertEquals("de", store(Locale.GERMANY).language)
    }

    @Test
    fun choosingALanguageWritesItThroughAndChangesTheTexts() {
        val store = store()
        val englishSave = store.strings.settingsSave

        store.use("de")

        assertEquals("de", store.language)
        assertEquals("de", stored)
        assertNotEquals(englishSave, store.strings.settingsSave)
        assertEquals("Einstellungen speichern", store.strings.settingsSave)
    }

    @Test
    fun theStoredChoiceIsReadableAndIsNotTheResolvedCode() {
        /*
         * The picker shows what was chosen, not what it resolved to: an operator who picked
         * "system language" must keep seeing that and not "EN". One source of truth, so a
         * caller never has to decide whether to ask the store or the preference.
         */
        stored = SYSTEM_LANGUAGE
        val store = store(Locale.UK)

        assertEquals(SYSTEM_LANGUAGE, store.stored)
        assertEquals("en", store.language)

        store.use("de")

        assertEquals("de", store.stored)
        assertEquals("de", store.language)
    }

    @Test
    fun aLanguageThisBuildDoesNotCarryIsRefusedRatherThanStored() {
        /*
         * The picker cannot offer one, but a hand-edited preferences file can name one, and
         * so can a build that dropped a language. Storing it would persist a dead setting.
         */
        val store = store()

        store.use("klingon")

        assertEquals("en", store.language)
        assertEquals(SYSTEM_LANGUAGE, stored, "a language with no texts was stored")
    }

    @Test
    fun choosingTheSystemLanguageIsStoredAsTheSystemLanguage() {
        // Not as the code it resolves to: the operator asked to follow the system, and a
        // system they later change must still be followed.
        stored = "de"
        val store = store(Locale.UK)

        store.use(SYSTEM_LANGUAGE)

        assertEquals("en", store.language)
        assertEquals(SYSTEM_LANGUAGE, stored)
    }

    @Test
    fun aKeyOnlyTheBaseHasComesBackInTheBaseLanguage() {
        /*
         * The fallback the whole contribution workflow depends on, and it cannot be tested
         * through German: German is a shipped language and the boundary test holds it
         * complete, so no key is ever missing from it. Built from maps instead, and read
         * through real generated members so the test goes the same way a window does.
         */
        val base = mapOf(
            "settings.save" to "Save settings",
            "menu.file" to "File",
        )
        val partial = mapOf("menu.file" to "Datei")

        val strings = Strings { key -> partial[key] ?: base[key] ?: key }

        assertEquals("Save settings", strings.settingsSave, "the base text did not come through")
        assertEquals("Datei", strings.menuFile, "the translation did not win")
    }

    @Test
    fun theJavaBridgeSeesWhatTheStoreSees() {
        val store = store()

        store.use("de")

        assertEquals(store.strings.settingsSave, CurrentStrings.get().settingsSave)
    }

    @Test
    fun aPlaceholderTextIsFormattedInTheChosenLanguagesOwnWordOrder() {
        /*
         * The reason MessageFormat is here rather than concatenation: in German the argument
         * comes first, which no amount of "Connect to " + x can produce.
         */
        val store = store()

        assertEquals("Connect to 144 MHz", store.strings.connectionConnectTo("144 MHz"))

        store.use("de")

        assertEquals("Mit 144 MHz verbinden", store.strings.connectionConnectTo("144 MHz"))
    }
}
