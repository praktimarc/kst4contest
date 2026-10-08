package kst4contest.view.i18n

import kst4contest.model.ChatPreferences
import java.util.Locale

/**
 * Builds the [LanguageStore] of the active profile.
 *
 * The store itself takes lambdas so it can be tested without a profile on disk; this is where
 * they are tied to the real preferences. It exists for the same reason `PaletteStoreFactory`
 * does: a Kotlin `(String) -> Unit` is not a void lambda seen from Java, so assembling them
 * here keeps the Java call one line and the lambda types out of it.
 */
object LanguageStoreFactory {

    /**
     * Creates the store for one profile.
     *
     * The language is written through to [prefs] at once, which is what switches the open
     * windows. The XML is written by the existing "save settings" path, exactly like every
     * other setting.
     *
     * @param prefs the active profile's preferences
     * @param systemDefault the system locale, used when the preference is empty
     * @return a store for the interface language
     */
    @JvmStatic
    @JvmOverloads
    fun create(
        prefs: ChatPreferences,
        systemDefault: Locale = Locale.getDefault(),
    ): LanguageStore = LanguageStore(
        storedLanguage = { prefs.guiOptions_language },
        storeLanguage = { language -> prefs.guiOptions_language = language },
        systemDefault = systemDefault,
    )
}
