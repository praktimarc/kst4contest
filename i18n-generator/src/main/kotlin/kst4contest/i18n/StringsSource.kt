package kst4contest.i18n

/**
 * Turns translations into the Kotlin the application compiles against.
 *
 * Two things come out: a `Strings` class whose members are one per key -- a property for a
 * plain text, a function for a parameterised one -- and a `Translations` object holding the
 * texts. The class is what makes a typo and a wrong argument count compile errors rather than
 * things an operator discovers.
 *
 * @param base the source language; every key and every argument count comes from here
 * @param translations the other languages, each carrying only the keys it actually has
 * @return the complete Kotlin source file
 */
fun stringsSource(base: Translation, translations: List<Translation>): String {

    val members = base.entries.keys.sorted().joinToString("\n\n") { key ->
        memberFor(key, base.entries.getValue(key))
    }

    val tables = (listOf(base) + translations).joinToString("\n\n") { translation ->
        tableFor(translation, base.entries.size)
    }

    val byLanguage = (listOf(base) + translations).joinToString(",\n") { translation ->
        "        ${kotlinLiteral(translation.language)} to ${tableName(translation.language)}"
    }

    return """
        |// Generated from app-desktop/src/main/i18n by the :i18n-generator module.
        |// Do not edit: edit the .properties files and rebuild.
        |package kst4contest.view.i18n
        |
        |import java.text.MessageFormat
        |
        |/**
        | * The interface texts of one language.
        | *
        | * One member per key, so a mistyped key and a wrong argument count are both compile
        | * errors. Generated -- see the module named above.
        | */
        |class Strings(private val lookup: (String) -> String) {
        |
        |$members
        |}
        |
        |/** The texts themselves, and which language has which. */
        |object Translations {
        |
        |$tables
        |
        |    /** Every language this build carries, by its code. */
        |    val BY_LANGUAGE: Map<String, Map<String, String>> = mapOf(
        |$byLanguage,
        |    )
        |
        |    /** The language every lookup falls back to. */
        |    const val BASE_LANGUAGE: String = ${kotlinLiteral(base.language)}
        |}
        |
    """.trimMargin()
}

/**
 * A text as a Kotlin string literal.
 *
 * The `$` is the one that matters and the one a translator cannot be expected to know about:
 * in Kotlin it opens a template, so an unescaped one produces source that either does not
 * compile or means something other than the text. The backslash goes first, or it would
 * escape the escapes added after it.
 */
fun kotlinLiteral(text: String): String {
    val escaped = text
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("$", "\\$")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    return "\"$escaped\""
}

/** A property for a plain text, a function for one with placeholders. */
private fun memberFor(key: String, text: String): String {
    val identifier = identifierFor(key)
    val arity = argumentCount(text)
    val documentation = "    /** ${commentFor(text)} */"

    if (arity == 0) {
        return "$documentation\n    val $identifier: String get() = lookup(${kotlinLiteral(key)})"
    }

    val parameters = (0 until arity).joinToString(", ") { "arg$it: Any" }
    val arguments = (0 until arity).joinToString(", ") { "arg$it" }

    return documentation +
        "\n    fun $identifier($parameters): String =" +
        "\n        MessageFormat.format(lookup(${kotlinLiteral(key)}), $arguments)"
}

private fun tableFor(translation: Translation, baseSize: Int): String {
    val entries = translation.entries.keys.sorted().joinToString(",\n") { key ->
        "        ${kotlinLiteral(key)} to ${kotlinLiteral(translation.entries.getValue(key))}"
    }

    return "    /** ${translation.language}: ${translation.entries.size} of $baseSize. */\n" +
        "    val ${tableName(translation.language)}: Map<String, String> = mapOf(\n$entries,\n    )"
}

/** `pt_BR` becomes `PT_BR`; the code is already identifier-safe. */
private fun tableName(language: String): String = language.uppercase()

/**
 * The source text as a KDoc line, so a call site shows what it says.
 *
 * The comment terminator has to go: a text containing one would end the KDoc early and leave
 * the rest of the text as stray source.
 */
private fun commentFor(text: String): String =
    text.replace("*/", "* /").replace("\n", " ").take(120)
