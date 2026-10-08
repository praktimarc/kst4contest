package kst4contest.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Reading a translation file.
 *
 * The one rule with teeth: `java.util.Properties` keeps the last of two identical keys
 * without a word. A contributor who translates a key twice -- easy in a file of several
 * hundred lines -- would otherwise lose one of their translations and never find out.
 */
class TranslationFilesTest {

    private fun createTempDirectory(): File =
        java.nio.file.Files.createTempDirectory("kst4contest-i18n").toFile()

    private fun fileWith(name: String, content: String): File {
        val file = File(createTempDirectory(), name)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    @Test
    fun aPlainFileIsReadKeyByKey() {
        val file = fileWith("strings_en.properties", "settings.save=Save settings\nmenu.file=File\n")

        val translation = readTranslation(file)

        assertEquals("en", translation.language)
        assertEquals("Save settings", translation.entries["settings.save"])
        assertEquals("File", translation.entries["menu.file"])
    }

    @Test
    fun umlautsSurviveBecauseTheFileIsUtf8() {
        // Java 9 and later read .properties as UTF-8, which is why no escapes are needed.
        val file = fileWith("strings_de.properties", "settings.save=Einstellungen speichern\nx=Grüße\n")

        assertEquals("Grüße", readTranslation(file).entries["x"])
    }

    @Test
    fun aDuplicateKeyIsRefusedRatherThanSilentlyWinning() {
        /*
         * Properties would keep the second and say nothing. For a contributor that means one
         * of their two translations vanishes with a green build.
         */
        val file = fileWith("strings_de.properties", "a=erste\nb=zwei\na=zweite\n")

        val failure = assertThrows(TranslationException::class.java) { readTranslation(file) }

        assertTrue(failure.message!!.contains("a"), failure.message)
        assertTrue(failure.message!!.contains("strings_de.properties"), failure.message)
    }

    @Test
    fun aDuplicateKeyIsStillFoundWhenTheLinesAreNotAdjacentOrAreSpaced() {
        val file = fileWith("strings_de.properties", "a = eins\nb=zwei\n a=drei\n")

        assertThrows(TranslationException::class.java) { readTranslation(file) }
    }

    @Test
    fun commentsAndBlankLinesAreNotKeys() {
        val file = fileWith("strings_en.properties", "# a comment\n! another\n\na=one\n")

        assertEquals(mapOf("a" to "one"), readTranslation(file).entries)
    }

    @Test
    fun aValueMayContainAnEqualsSign() {
        // Only the first separator splits; the rest is text. Operators do write "a=b".
        val file = fileWith("strings_en.properties", "formula=a=b\n")

        assertEquals("a=b", readTranslation(file).entries["formula"])
    }

    @Test
    fun aMissingFileIsRefusedWithItsName() {
        val absent = File("/nonexistent/kst4contest/strings_de.properties")

        val failure = assertThrows(TranslationException::class.java) { readTranslation(absent) }

        assertTrue(failure.message!!.contains("strings_de.properties"), failure.message)
    }

    @Test
    fun theLanguageComesFromTheFileName() {
        assertEquals("en", languageOf("strings_en.properties"))
        assertEquals("de", languageOf("strings_de.properties"))
        assertEquals("pt_BR", languageOf("strings_pt_BR.properties"))
    }

    @Test
    fun aFileNameThatIsNotATranslationNamesNoLanguage() {
        assertNull(languageOf("strings.properties"))
        assertNull(languageOf("gradle.properties"))
        assertNull(languageOf("strings_de.txt"))
    }
}
