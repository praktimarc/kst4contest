package kst4contest.i18n

/**
 * The rules that decide whether a set of translations is usable.
 *
 * Pure functions over key sets and texts, with no file and no Gradle in sight, so the part
 * that can be quietly wrong is the part that is tested.
 */

/** Keys the base has and a translation does not. Allowed: they fall back to the base. */
fun missingKeys(base: Set<String>, other: Set<String>): Set<String> = base - other

/** Keys a translation has and the base does not. Not allowed: a contributor's typo. */
fun orphanKeys(base: Set<String>, other: Set<String>): Set<String> = other - base

/**
 * How many arguments a text needs.
 *
 * The highest placeholder index plus one, and deliberately not the number of occurrences:
 * `MessageFormat` addresses arguments by index, so `{1} {1}` needs two and `{0} {0}` needs
 * one. Counting occurrences makes the first throw and the second swallow an argument.
 *
 * A brace followed by anything but a digit is not a placeholder -- braces occur in real
 * interface text.
 */
fun argumentCount(text: String): Int {
    val indices = PLACEHOLDER.findAll(text).map { it.groupValues[1].toInt() }
    return (indices.maxOrNull() ?: -1) + 1
}

/**
 * The Kotlin identifier for a key.
 *
 * `settings.save` becomes `settingsSave`. Dots, underscores and dashes all separate words,
 * because a contributor will use whichever they are used to and the generated name must not
 * depend on which.
 */
fun identifierFor(key: String): String {
    val words = key.split('.', '_', '-').filter { it.isNotEmpty() }

    val joined = words.mapIndexed { index, word ->
        if (index == 0) {
            word.replaceFirstChar { it.lowercaseChar() }
        } else {
            word.replaceFirstChar { it.uppercaseChar() }
        }
    }.joinToString("")

    /* A key may start with a digit; a Kotlin identifier may not. */
    return if (joined.firstOrNull()?.isDigit() == true) "key$joined" else joined
}

/**
 * Keys that would produce the same identifier, grouped by that identifier.
 *
 * Generating both would emit Kotlin that does not compile, and the compiler's complaint names
 * the generated file -- which tells a contributor nothing about the line they wrote.
 */
fun identifierCollisions(keys: Collection<String>): Map<String, List<String>> =
    keys.groupBy { identifierFor(it) }.filterValues { it.size > 1 }

/**
 * Keys whose identifier would not be a legal Kotlin name.
 *
 * [identifierFor] strips only `.`, `_` and `-`; every other character a `Properties` file can
 * carry in a key -- a space, a brace, a quote, a `=`, a newline decoded from a `\uXXXX` escape
 * -- passes through into identifier position unchanged. Emitted there it does not merely fail
 * to compile: the member is written as `val <identifier>: String get() = lookup(...)`, so a
 * key that reads `x: String get() = "" ... init { ... } ... val y` breaks out of the template
 * and leaves a property initializer or an init block of the contributor's choosing, which
 * runs when `Strings` is instantiated in the application. The value is already escaped before
 * it becomes a literal; the identifier was not checked at all. So a generated name that is
 * not letters-then-letters-and-digits is refused before any source is written.
 */
fun illegalIdentifierKeys(keys: Collection<String>): List<String> =
    keys.filterNot { IDENTIFIER.matches(identifierFor(it)) }

/** A letter, then letters and digits: the plain Kotlin identifier this generator emits. */
private val IDENTIFIER = Regex("[A-Za-z][A-Za-z0-9]*")

/** `{0}`, `{12}`, `{0,number}` -- a brace, digits, then either a brace or a comma. */
private val PLACEHOLDER = Regex("""\{(\d+)\s*[,}]""")
