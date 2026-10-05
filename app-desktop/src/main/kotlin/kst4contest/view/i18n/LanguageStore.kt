package kst4contest.view.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

/**
 * The language in force, held as Compose state.
 *
 * **This is what makes a language change reach every open window** -- the same arrangement the
 * palette uses, and for the same reason: in Etappe 8 a remembered value meant a change
 * reached nothing until something else happened to recompose.
 *
 * Two ways out, one truth: [strings] for Compose, which recomposes on the state read, and
 * [CurrentStrings] for the Java call sites, which pick the new language up on their next call
 * without needing any machinery of their own.
 *
 * @param storedLanguage the preference, possibly empty for "follow the system"
 * @param storeLanguage writes the preference back
 * @param systemDefault injected so tests need not touch the JVM's global default
 */
class LanguageStore(
    storedLanguage: () -> String?,
    private val storeLanguage: (String) -> Unit,
    private val systemDefault: Locale = Locale.getDefault(),
) {

    private var chosen by mutableStateOf(storedLanguage().orEmpty())
    private var current by mutableStateOf(languageFor(storedLanguage(), systemDefault))

    init {
        CurrentStrings.set(stringsFor(current))
    }

    /** The language code in force, after the system default and the fallback are applied. */
    val language: String get() = current

    /**
     * What the operator actually chose, which is what the picker shows.
     *
     * Not the same as [language]: [SYSTEM_LANGUAGE] stays itself here and resolves there. The
     * picker has to show "system language" and not the code it happens to mean today, and
     * having both on the store means a caller never has to decide whether to ask the store or
     * the preference -- which is two sources of truth for one value.
     */
    val stored: String get() = chosen

    /**
     * The texts in force.
     *
     * Reads the state, so a composable calling this recomposes when the language changes.
     * That state read is the whole mechanism and must not be cached.
     */
    val strings: Strings get() = stringsFor(current)

    /**
     * Switches the language.
     *
     * A language this build has no texts for is refused rather than stored: persisting a dead
     * setting would leave the operator with English and no way to see why.
     *
     * [SYSTEM_LANGUAGE] is stored as itself and not as the code it resolves to, so an
     * operator who later changes their system is still followed.
     *
     * @param language a language code, or [SYSTEM_LANGUAGE]
     */
    fun use(language: String) {
        val asked = language.trim().lowercase(Locale.ROOT)
        val resolved = languageFor(language, systemDefault)

        if (asked != SYSTEM_LANGUAGE && resolved != asked) {
            return
        }

        current = resolved
        chosen = language
        storeLanguage(language)
        CurrentStrings.set(stringsFor(resolved))
    }

    private fun stringsFor(language: String): Strings {
        val texts = Translations.BY_LANGUAGE[language].orEmpty()
        val base = Translations.BY_LANGUAGE.getValue(Translations.BASE_LANGUAGE)

        /*
         * Chosen first, base second. A key the translation leaves out falls through, which is
         * what lets a partial contributed language ship at all.
         */
        return Strings { key -> texts[key] ?: base[key] ?: key }
    }
}

/**
 * The texts the Java call sites read.
 *
 * A volatile field rather than a parameter on a few hundred signatures: those call sites are
 * AWT dialogs and menu builders that run once per interaction, so reading the current value
 * on the next call is both correct and all they need.
 */
object CurrentStrings {

    @Volatile
    private var strings: Strings = baseStrings()

    /** The texts in force. */
    @JvmStatic
    fun get(): Strings = strings

    internal fun set(value: Strings) {
        strings = value
    }
}

/**
 * The texts for the windows below.
 *
 * The default is the base language rather than null: a window composed before a profile
 * exists -- the operator profile picker -- still has to be able to draw.
 */
val LocalStrings = staticCompositionLocalOf { baseStrings() }

/** The base language's texts, with the key itself for anything the base somehow lacks. */
private fun baseStrings(): Strings {
    val base = Translations.BY_LANGUAGE.getValue(Translations.BASE_LANGUAGE)
    return Strings { key -> base[key] ?: key }
}
