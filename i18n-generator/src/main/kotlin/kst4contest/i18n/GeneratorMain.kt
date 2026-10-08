package kst4contest.i18n

import java.io.File

/**
 * What generation reported.
 *
 * @param warnings incomplete translations -- allowed, and the reason a partial contribution
 *        can be submitted at all
 * @param coverage one line per language, so a pull request can be judged at a glance
 */
data class GenerationResult(val warnings: List<String>, val coverage: List<String>)

/**
 * English, because the source text lives in the code and AGENTS.md requires it there.
 *
 * Deliberately a second declaration: the generated `Translations.BASE_LANGUAGE` says the same
 * thing on the application's side. They cannot be shared -- this module is build tooling and
 * is not on the application's classpath -- and the generated source is written from this
 * constant, so a drift would show up in `GenerationTest`.
 */
const val BASE_LANGUAGE: String = "en"

/**
 * Reads every translation file in a directory and writes the generated Kotlin.
 *
 * Incompleteness is allowed, incorrectness is not. A missing key warns and falls back to the
 * base at runtime; an orphan key, a placeholder mismatch, a duplicate key, an unreadable file
 * or two keys collapsing to one identifier all throw, which fails the build with this
 * message.
 *
 * Anything in the directory that is not a translation file -- a README for contributors, say
 * -- is ignored rather than refused: documentation must not break the build.
 *
 * @param inputDirectory holds `strings_<code>.properties`
 * @param outputDirectory receives the generated source and any worksheets
 * @return the warnings and the coverage lines
 * @throws TranslationException on anything a contributor has to fix
 */
fun generate(inputDirectory: File, outputDirectory: File): GenerationResult {

    val translations = (inputDirectory.listFiles() ?: emptyArray())
        .filter { languageOf(it.name) != null }
        .sortedBy { it.name }
        .map(::readTranslation)

    val base = translations.firstOrNull { it.language == BASE_LANGUAGE }
        ?: throw TranslationException(
            "strings_$BASE_LANGUAGE.properties is missing from ${inputDirectory.path}; the base "
                    + "language is where every key and every argument count comes from"
        )

    refuseIllegalIdentifiers(base)
    refuseIdentifierCollisions(base)

    val others = translations.filter { it.language != BASE_LANGUAGE }
    val warnings = mutableListOf<String>()

    for (translation in others) {
        refuseOrphans(base, translation)
        refusePlaceholderMismatches(base, translation)

        val missing = missingKeys(base.entries.keys, translation.entries.keys)

        if (missing.isNotEmpty()) {
            warnings += "strings_${translation.language}.properties is missing "
                .plus("${missing.size} of ${base.entries.size} keys; they fall back to ")
                .plus("$BASE_LANGUAGE: ${missing.sorted().take(10).joinToString(", ")}")

            writeWorksheet(outputDirectory, translation.language, base, missing)
        }
    }

    val target = File(outputDirectory, "kst4contest/view/i18n/Strings.kt")
    target.parentFile.mkdirs()
    target.writeText(stringsSource(base, others), Charsets.UTF_8)

    return GenerationResult(
        warnings = warnings,
        coverage = translations.map { "${it.language}: ${it.entries.size}/${base.entries.size}" },
    )
}

private fun refuseIllegalIdentifiers(base: Translation) {
    val illegal = illegalIdentifierKeys(base.entries.keys)

    if (illegal.isEmpty()) {
        return
    }

    throw TranslationException(
        "strings_$BASE_LANGUAGE.properties has keys that do not form a usable name -- a key is "
                + "letters and digits with . _ - between words, nothing else. A key carrying "
                + "anything more would be written into the generated source as code: "
                + illegal.sorted().joinToString(", ")
    )
}

private fun refuseIdentifierCollisions(base: Translation) {
    val collisions = identifierCollisions(base.entries.keys)

    if (collisions.isEmpty()) {
        return
    }

    throw TranslationException(
        "these keys would become the same Kotlin name: " + collisions.entries.joinToString("; ") {
            "${it.key} <- ${it.value.sorted().joinToString(", ")}"
        }
    )
}

private fun refuseOrphans(base: Translation, translation: Translation) {
    val orphans = orphanKeys(base.entries.keys, translation.entries.keys)

    if (orphans.isEmpty()) {
        return
    }

    throw TranslationException(
        "strings_${translation.language}.properties names keys the base does not, so nothing "
                + "would ever read them -- a typo? " + orphans.sorted().joinToString(", ")
    )
}

private fun refusePlaceholderMismatches(base: Translation, translation: Translation) {
    val mismatched = translation.entries.keys.filter { key ->
        argumentCount(translation.entries.getValue(key)) != argumentCount(base.entries.getValue(key))
    }

    if (mismatched.isEmpty()) {
        return
    }

    throw TranslationException(
        "strings_${translation.language}.properties uses different placeholders than the base "
                + "for: " + mismatched.sorted().joinToString(", ")
                + ". A placeholder the translation leaves out drops its data silently."
    )
}

/**
 * Writes the keys a translation still lacks, with the base text as the value.
 *
 * A worksheet, not a build output: a contributor copies its lines into their own file and
 * translates the right-hand side. Without it they would have to diff two files of several
 * hundred lines to find out what is left.
 *
 * Only written when something is missing -- an empty worksheet is a file somebody has to open
 * to learn there is nothing in it.
 */
private fun writeWorksheet(
    outputDirectory: File,
    language: String,
    base: Translation,
    missing: Set<String>,
) {
    val file = File(outputDirectory, "todo/strings_$language.todo.properties")
    file.parentFile.mkdirs()

    val header = "# ${missing.size} of ${base.entries.size} keys are not yet translated into "
        .plus("$language. Copy these lines into strings_$language.properties and translate the ")
        .plus("values. Partial is fine: anything left out falls back to $BASE_LANGUAGE.")

    val lines = missing.sorted().joinToString("\n") { key ->
        "$key=${base.entries.getValue(key)}"
    }

    file.writeText("$header\n\n$lines\n", Charsets.UTF_8)
}

/** Called by Gradle: input directory, output directory. */
object GeneratorMain {

    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "usage: GeneratorMain <input dir> <output dir>" }

        val result = generate(File(args[0]), File(args[1]))

        result.warnings.forEach { println("[i18n, warning] $it") }
        println("[i18n] coverage -- " + result.coverage.joinToString(", "))
    }
}
