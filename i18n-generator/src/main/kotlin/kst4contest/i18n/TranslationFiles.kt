package kst4contest.i18n

import java.io.File
import java.util.Properties

/** One language's texts. */
data class Translation(val language: String, val entries: Map<String, String>)

/** What the generator refuses. The message is read by a contributor, so it names the file. */
class TranslationException(message: String) : RuntimeException(message)

/**
 * Reads one translation file.
 *
 * `Properties` does the parsing -- separators, comments, escapes, continuation lines -- and is
 * battle-tested at it. What it does *not* do is complain about a key that appears twice: it
 * keeps the last one silently. In a file of several hundred lines that is an easy mistake and
 * an invisible loss, so the keys are counted separately before the values are taken.
 *
 * @throws TranslationException when the file cannot be read or names a key twice
 */
fun readTranslation(file: File): Translation {

    if (!file.isFile) {
        throw TranslationException("${file.name}: no such translation file (${file.path})")
    }

    val duplicates = duplicateKeysIn(file)

    if (duplicates.isNotEmpty()) {
        throw TranslationException(
            "${file.name}: these keys appear more than once, so one translation would be "
                    + "lost without a word: ${duplicates.sorted().joinToString(", ")}"
        )
    }

    val properties = Properties()
    runCatching { file.reader(Charsets.UTF_8).use(properties::load) }
        .onFailure { throw TranslationException("${file.name}: cannot be read -- ${it.message}") }

    val language = languageOf(file.name)
        ?: throw TranslationException("${file.name}: the file name does not name a language")

    return Translation(
        language = language,
        entries = properties.stringPropertyNames().associateWith { properties.getProperty(it) },
    )
}

/**
 * The language a translation file name names, or null when it names none.
 *
 * `strings_de.properties` is German, `strings_pt_BR.properties` is Brazilian Portuguese, and
 * `strings.properties` is not a translation file at all.
 */
fun languageOf(fileName: String): String? =
    FILE_NAME.matchEntire(fileName)?.groupValues?.get(1)

/**
 * Keys that appear more than once, found by reading the lines rather than the parsed map --
 * which is the only place the information still exists.
 *
 * Deliberately does not understand continuation lines (a trailing backslash): a continuation
 * whose text contains a separator would be counted as a key. That is acceptable here because
 * these texts are one line each, and the alternative is reimplementing `Properties`' parser
 * to find a mistake `Properties` itself does not report.
 */
private fun duplicateKeysIn(file: File): Set<String> {
    val seen = mutableSetOf<String>()
    val twice = mutableSetOf<String>()

    file.forEachLine(Charsets.UTF_8) { raw ->
        val line = raw.trim()

        if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
            return@forEachLine
        }

        val separator = line.indexOfFirst { it == '=' || it == ':' }

        if (separator <= 0) {
            return@forEachLine
        }

        val key = line.take(separator).trim()

        if (!seen.add(key)) {
            twice += key
        }
    }

    return twice
}

private val FILE_NAME = Regex("""strings_([A-Za-z]{2}(?:_[A-Za-z]{2})?)\.properties""")
