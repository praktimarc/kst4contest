package kst4contest.view.compose

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * The readability measure, which only warns.
 *
 * A pure function, and therefore the part that can be wrong without anyone noticing: a
 * warning that never fires is indistinguishable from a palette that is fine, and a warning
 * that always fires teaches the operator to ignore warnings. The reference numbers below are
 * the ones WCAG itself states for black and white.
 *
 * The comparison is against the shipped sheet rather than against the bare floor, because
 * the shipped sheets do not meet that floor themselves -- measured: the evening accent sits
 * at 2.26 on its own surface. What is reported is therefore "this change made a pair harder
 * to read than it shipped", which is the question the operator is actually asking.
 */
class ContrastRuleTest {

    private val day = JavaFxStylesheet.read("/KST4ContestDefaultDay.css")
    private val evening = JavaFxStylesheet.read("/KST4ContestDefaultEvening.css")

    private fun assertClose(expected: Double, actual: Double, what: String) {
        assertTrue(abs(expected - actual) < 0.05, "$what: expected ~$expected but was $actual")
    }

    @Test
    fun blackOnWhiteIsTheMaximumOf21() {
        assertClose(21.0, contrastRatio(Color.Black, Color.White), "black on white")
        assertClose(21.0, contrastRatio(Color.White, Color.Black), "white on black")
    }

    @Test
    fun aColourAgainstItselfIsTheMinimumOf1() {
        assertClose(1.0, contrastRatio(Color.Black, Color.Black), "black on black")
        assertClose(1.0, contrastRatio(Color(0xFF8899AA), Color(0xFF8899AA)), "grey on grey")
    }

    @Test
    fun theRatioDoesNotDependOnWhichIsTheBackground() {
        assertEquals(
            contrastRatio(Color(0xFF123456), Color(0xFFEEDDCC)),
            contrastRatio(Color(0xFFEEDDCC), Color(0xFF123456)),
        )
    }

    @Test
    fun midGreyOnWhiteIsAroundFourAndAHalf() {
        // #767676 on white is the canonical WCAG AA boundary example: 4.54.
        assertClose(4.54, contrastRatio(Color(0xFF767676), Color.White), "#767676 on white")
    }

    @Test
    fun aShippedSheetNeverWarnsAboutItself() {
        /*
         * The property the whole relative rule exists for. Both sheets have pairs under the
         * floor -- the day accent at 2.86, the evening accent at 2.26 -- so an absolute rule
         * would warn on a client nobody has touched.
         */
        assertTrue(contrastWarnings(day, day).isEmpty(), "the daylight sheet warns about itself")
        assertTrue(
            contrastWarnings(evening, evening).isEmpty(),
            "the evening sheet warns about itself",
        )
    }

    @Test
    fun theShippedSheetsReallyDoHavePairsUnderTheFloor() {
        /*
         * Measured here rather than asserted in prose, because it is the premise of the rule
         * above. If a later change to the sheets makes every pair pass, the relative rule
         * becomes equivalent to the absolute one and the deviation can be dropped.
         */
        val accentOnSurface = contrastRatio(day.accent, day.base)

        assertTrue(
            accentOnSurface < CONTRAST_FLOOR,
            "the daylight accent now passes at $accentOnSurface; the relative rule may no "
                    + "longer be needed",
        )
    }

    @Test
    fun blackTextOnABlackSurfaceWarnsAboutEveryPairItAppearsIn() {
        val unreadable = day.copy(
            base = Color.Black,
            windowBackground = Color.Black,
            controlInnerBackground = Color.Black,
            labelTextFill = Color.Black,
            accent = Color.Black,
            textAccent = Color.Black,
        )

        val warnings = contrastWarnings(unreadable, day)

        assertEquals(4, warnings.size, "every pair must warn: ${warnings.map { it.what }}")
        warnings.forEach {
            assertTrue(it.ratio < CONTRAST_FLOOR, it.what)
            assertTrue(it.ratio < it.shippedRatio, "${it.what} is not actually worse than shipped")
        }
    }

    @Test
    fun blackeningOnlyTheWindowSurfaceWarnsAboutTheTextOnIt() {
        /*
         * The pair that actually carries almost all the text, and the one the guard missed.
         * Theme.kt maps background and surface onto windowBackground and onBackground and
         * onSurface onto labelTextFill, so every label in every window sits on this pair --
         * while `base` reaches the screen only through the menu strip and the selection tint.
         * Checking text against `base` alone let an operator black out every window and be
         * told nothing.
         */
        val blackWindows = day.copy(windowBackground = Color.Black)

        val warnings = contrastWarnings(blackWindows, day)

        assertTrue(
            warnings.any { it.what.contains("window surface") },
            "no warning when every label in every window went black on black: "
                    + "${warnings.map { it.what }}",
        )
    }

    @Test
    fun theAccentIsMeasuredWhereItIsActuallyDrawn() {
        /*
         * Material's primary is textAccent, not accent, and it is drawn as text and as tab
         * underlines on the window surface -- not as `accent` on `base`. Measuring the wrong
         * colour on the wrong ground answers a question nobody is asking.
         */
        val invisibleAccent = day.copy(textAccent = day.windowBackground)

        val warnings = contrastWarnings(invisibleAccent, day)

        assertTrue(
            warnings.any { it.what.contains("Accent") },
            "an accent identical to the surface it is drawn on did not warn: "
                    + "${warnings.map { it.what }}",
        )
    }

    @Test
    fun anImprovementDoesNotWarnEvenWhileStillUnderTheFloor() {
        /*
         * The evening accent ships at 2.26. An operator who moves it to something lighter has
         * made things better and must not be told off for it -- that is how a guard loses its
         * authority.
         */
        val better = evening.copy(accent = Color.White)

        val accentWarnings = contrastWarnings(better, evening).filter { it.what.contains("Accent") }

        assertTrue(accentWarnings.isEmpty(), "an improvement warned: $accentWarnings")
    }

    @Test
    fun aPairThatStaysComfortablyReadableDoesNotWarn() {
        // Worse than shipped but still well over the floor: nothing to say.
        val slightlyWorse = day.copy(labelTextFill = Color(0xFF444444))

        val textWarnings = contrastWarnings(slightlyWorse, day).filter { it.what.contains("Text") }

        assertTrue(textWarnings.isEmpty(), "a readable pair warned: $textWarnings")
    }
}
