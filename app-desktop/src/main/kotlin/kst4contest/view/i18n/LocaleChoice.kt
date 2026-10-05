package kst4contest.view.i18n

import java.util.Locale

/** The stored value that means "whatever the system is set to". */
const val SYSTEM_LANGUAGE: String = ""

/**
 * The language to use.
 *
 * An explicit setting wins; an empty one follows the system; anything this build has no texts
 * for falls back to the base language. A pure function so it can be tested without touching
 * the JVM's default locale, which is global state and leaks into whatever runs next.
 *
 * @param stored the value from the preferences, possibly hand-edited or from another build
 * @param systemDefault usually `Locale.getDefault()`
 * @return a language code [Translations.BY_LANGUAGE] has an entry for
 */
fun languageFor(stored: String?, systemDefault: Locale): String {
    val wanted = stored?.trim()?.lowercase(Locale.ROOT).orEmpty()

    val candidate = if (wanted == SYSTEM_LANGUAGE) {
        systemDefault.language.lowercase(Locale.ROOT)
    } else {
        wanted
    }

    return if (Translations.BY_LANGUAGE.containsKey(candidate)) {
        candidate
    } else {
        Translations.BASE_LANGUAGE
    }
}
