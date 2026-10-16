package kst4contest.view.compose

import java.awt.Rectangle
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** A size the host fitted a window to is not the operator's and is never written back. */
class FitRecordTest {

    @Test
    fun `only the fitted size counts as fitted`() {
        val fit = FitRecord()
        assertFalse(fit.isFittedSize(640, 480))

        fit.bounds = Rectangle(1920, 0, 640, 480)

        assertTrue(fit.isFittedSize(640, 480))
        assertFalse(fit.isFittedSize(800, 480))
    }
}
