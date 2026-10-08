package kst4contest.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

/**
 * Generation end to end: what breaks the build and what only warns.
 *
 * The distinction is the whole arrangement. A translation that is 80 per cent finished has to
 * be submittable, or nobody outside this project can contribute one; a translation that is
 * wrong must not ship.
 */
class GenerationTest {

    private fun directories(): Pair<File, File> {
        val input = Files.createTempDirectory("kst4contest-i18n-in").toFile()
        val output = Files.createTempDirectory("kst4contest-i18n-out").toFile()
        return input to output
    }

    private fun write(directory: File, name: String, content: String) {
        File(directory, name).writeText(content, Charsets.UTF_8)
    }

    private fun generated(output: File): File =
        File(output, "kst4contest/view/i18n/Strings.kt")

    @Test
    fun aCompleteSetGeneratesAndWarnsAboutNothing() {
        val (input, output) = directories()
        write(input, "strings_en.properties", "a=one\n")
        write(input, "strings_de.properties", "a=eins\n")

        val result = generate(input, output)

        assertTrue(result.warnings.isEmpty(), "${result.warnings}")
        assertTrue(generated(output).isFile, "no source written")
    }

    @Test
    fun aMissingKeyOnlyWarnsAndStillGenerates() {
        val (input, output) = directories()
        write(input, "strings_en.properties", "a=one\nb=two\n")
        write(input, "strings_de.properties", "a=eins\n")

        val result = generate(input, output)

        assertEquals(1, result.warnings.size, "${result.warnings}")
        assertTrue(result.warnings.single().contains("b"), result.warnings.single())
        assertTrue(generated(output).isFile)
    }

    @Test
    fun anOrphanKeyBreaksTheBuildAndNamesItself() {
        val (input, output) = directories()
        write(input, "strings_en.properties", "a=one\n")
        write(input, "strings_de.properties", "a=eins\ntypo=zwei\n")

        val failure = assertThrows(TranslationException::class.java) { generate(input, output) }

        assertTrue(failure.message!!.contains("typo"), failure.message)
        assertTrue(failure.message!!.contains("strings_de"), failure.message)
    }

    @Test
    fun aPlaceholderMismatchBreaksTheBuild() {
        /*
         * The one that would otherwise swallow data in silence: the base passes two
         * arguments, the translation prints one, and MessageFormat says nothing.
         */
        val (input, output) = directories()
        write(input, "strings_en.properties", "a={0} of {1}\n")
        write(input, "strings_de.properties", "a={0}\n")

        val failure = assertThrows(TranslationException::class.java) { generate(input, output) }

        assertTrue(failure.message!!.contains("a"), failure.message)
    }

    @Test
    fun anIdentifierCollisionBreaksTheBuild() {
        val (input, output) = directories()
        write(input, "strings_en.properties", "gui.startupDesign=x\ngui.startup_design=y\n")

        val failure = assertThrows(TranslationException::class.java) { generate(input, output) }

        assertTrue(failure.message!!.contains("guiStartupDesign"), failure.message)
    }

    @Test
    fun aKeyCarryingKotlinSyntaxBreaksTheBuildRatherThanInjectingCode() {
        /*
         * The key, not the value, becomes the member name, and only . _ - are stripped from
         * it. A \uXXXX escape lets a contributed .properties file carry a key that, dropped
         * into `val <name>: String get() = ...`, closes the template and opens a property
         * initializer -- Kotlin that runs when Strings is built in every client. The base
         * file is the vector: only base keys become members. This must fail the build the way
         * an orphan or a collision does, not ship.
         *
         * The escapes below decode to the key `inject: Int = run{ pwn() }`.
         */
        val (input, output) = directories()
        write(
            input,
            "strings_en.properties",
            "ok=fine\n" +
                "inject\\u003a\\u0020Int\\u0020\\u003d\\u0020run\\u007b\\u0020pwn()\\u0020\\u007d=value\n",
        )

        val failure = assertThrows(TranslationException::class.java) { generate(input, output) }

        assertTrue(failure.message!!.contains("inject"), failure.message)
        assertFalse(generated(output).exists(), "source was written despite an unusable key")
    }

    @Test
    fun aMissingBaseFileBreaksTheBuild() {
        val (input, output) = directories()
        write(input, "strings_de.properties", "a=eins\n")

        assertThrows(TranslationException::class.java) { generate(input, output) }
    }

    @Test
    fun aThirdLanguageIsGeneratedToo() {
        // Prüfschwerpunkt 6: a contributed language must not need any wiring.
        val (input, output) = directories()
        write(input, "strings_en.properties", "a=one\nb=two\n")
        write(input, "strings_de.properties", "a=eins\nb=zwei\n")
        write(input, "strings_fr.properties", "a=un\n")

        val result = generate(input, output)

        assertTrue(generated(output).readText().contains("\"a\" to \"un\""))
        assertTrue(result.coverage.any { it.startsWith("fr:") && it.contains("1/2") }, "${result.coverage}")
    }

    @Test
    fun theCoverageLineNamesEveryLanguage() {
        val (input, output) = directories()
        write(input, "strings_en.properties", "a=one\nb=two\n")
        write(input, "strings_de.properties", "a=eins\nb=zwei\n")

        val result = generate(input, output)

        assertTrue(result.coverage.any { it.startsWith("de:") && it.contains("2/2") }, "${result.coverage}")
    }

    @Test
    fun anIncompleteTranslationGetsAWorksheetWithTheEnglishTextToTranslate() {
        val (input, output) = directories()
        write(input, "strings_en.properties", "a=one\nb=two\n")
        write(input, "strings_de.properties", "a=eins\n")

        generate(input, output)

        val worksheet = File(output, "todo/strings_de.todo.properties")
        assertTrue(worksheet.isFile, "no worksheet was written")

        val content = worksheet.readText()
        assertTrue(content.contains("b=two"), content)
        assertFalse(content.lines().any { it.startsWith("a=") }, "an already translated key was listed")
    }

    @Test
    fun aCompleteTranslationGetsNoWorksheet() {
        // An empty worksheet is a file somebody has to open to learn there is nothing to do.
        val (input, output) = directories()
        write(input, "strings_en.properties", "a=one\n")
        write(input, "strings_de.properties", "a=eins\n")

        generate(input, output)

        assertFalse(File(output, "todo/strings_de.todo.properties").exists())
    }

    @Test
    fun aFileThatIsNotATranslationIsIgnoredRatherThanRefused() {
        /*
         * The input directory may hold a README telling a contributor what to do. Refusing it
         * would make the documentation break the build.
         */
        val (input, output) = directories()
        write(input, "strings_en.properties", "a=one\n")
        write(input, "README.md", "# how to contribute a language\n")

        val result = generate(input, output)

        assertTrue(result.warnings.isEmpty(), "${result.warnings}")
        assertTrue(generated(output).isFile)
    }
}
