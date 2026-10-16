package kst4contest.view.compose

import java.awt.Point
import java.awt.Rectangle
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The size and position the host fitted a window to are not the operator's: the first
 * matching report is swallowed, and nothing after it.
 */
class FitRecordTest {

    private val fitted = Rectangle(1920, 0, 640, 480)

    @Test
    fun `nothing counts as fitted before a fit`() {
        val fit = FitRecord()

        assertFalse(fit.takeSize(640, 480))
        assertFalse(fit.takePosition(Point(1920, 0)))
    }

    @Test
    fun `the fit echoing back is recognised despite rounding and a nudge`() {
        val fit = FitRecord()
        fit.record(fitted)

        assertTrue(fit.takeSize(641, 479))
        assertTrue(fit.takePosition(Point(1920 + 10, 0 + 28)))
    }

    @Test
    fun `a fit is consumed once, so a later identical resize or move is the operator's`() {
        val fit = FitRecord()
        fit.record(fitted)
        fit.takeSize(640, 480)
        fit.takePosition(Point(1920, 0))

        assertFalse(fit.takeSize(640, 480))
        assertFalse(fit.takePosition(Point(1920, 0)))
    }

    @Test
    fun `an operator report in between clears the record`() {
        val fit = FitRecord()
        fit.record(fitted)

        assertFalse(fit.takeSize(900, 700))
        assertFalse(fit.takePosition(Point(100, 100)))
        assertFalse(fit.takeSize(640, 480))
    }
}
