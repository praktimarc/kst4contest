package kst4contest.view.compose

import androidx.compose.ui.graphics.Color
import kst4contest.model.PaletteOverrides
import kst4contest.model.PaletteRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The store, and the three ways back.
 *
 * Built over four lambdas rather than over ChatPreferences, so none of this touches the
 * operator's home directory: that constructor copies resources into it.
 */
class PaletteStoreTest {

    private val stored = mutableMapOf(false to "", true to "")
    private var fileRoles: Map<PaletteRole, Color> = emptyMap()

    private fun store() = PaletteStore(
        shippedOf = { dark ->
            JavaFxStylesheet.read(
                if (dark) "/KST4ContestDefaultEvening.css" else "/KST4ContestDefaultDay.css"
            )
        },
        fileRolesOf = { fileRoles },
        storedOverridesOf = { dark -> stored.getValue(dark) },
        storeOverrides = { dark, encoded -> stored[dark] = encoded },
    )

    @Test
    fun theImmutableLayersAreReadOnceAndNotOnEveryResolve() {
        /*
         * resolved() is called from inside Kst4ContestTheme, so it runs on every recomposition
         * of every window. Reading the classpath and the profile's stylesheet there means two
         * CSS parses and a disk read per recomposition, on the UI thread -- and this project's
         * standing lesson is that a green build says nothing about what the UI thread is doing.
         *
         * The override state is the live part; the two layers under it are immutable for the
         * run, which is also what both manuals promise ("changes to the file take effect after
         * a restart").
         */
        var shippedReads = 0
        var fileReads = 0

        val store = PaletteStore(
            shippedOf = { dark ->
                shippedReads++
                JavaFxStylesheet.read(
                    if (dark) "/KST4ContestDefaultEvening.css" else "/KST4ContestDefaultDay.css"
                )
            },
            fileRolesOf = { fileReads++; emptyMap() },
            storedOverridesOf = { "" },
            storeOverrides = { _, _ -> },
        )

        repeat(20) { store.resolved(false) }
        store.set(false, PaletteRole.ACCENT, "#FF0000")
        repeat(20) { store.resolved(false) }

        assertEquals(1, shippedReads, "the shipped sheet was parsed more than once")
        assertEquals(1, fileReads, "the profile stylesheet was read more than once")
    }

    @Test
    fun eachDesignReadsItsOwnLayersOnceWhenItIsFirstNeeded() {
        var reads = 0
        val store = PaletteStore(
            shippedOf = { dark ->
                reads++
                JavaFxStylesheet.read(
                    if (dark) "/KST4ContestDefaultEvening.css" else "/KST4ContestDefaultDay.css"
                )
            },
            fileRolesOf = { emptyMap() },
            storedOverridesOf = { "" },
            storeOverrides = { _, _ -> },
        )

        repeat(5) { store.resolved(false) }
        assertEquals(1, reads, "the daylight sheet was read more than once")

        repeat(5) { store.resolved(true) }
        assertEquals(2, reads, "the evening sheet was not read, or was read more than once")
    }

    @Test
    fun aChangeIsVisibleInTheResolvedPaletteAndInTheStorage() {
        val store = store()

        store.set(darkMode = false, role = PaletteRole.ACCENT, colour = "#FF0000")

        assertEquals(Color(0xFFFF0000), store.resolved(false).palette.accent)
        assertEquals(
            PaletteSource.CHANGED,
            store.resolved(false).roles[PaletteRole.ACCENT]!!.source,
        )
        assertEquals("accent=#FF0000", stored.getValue(false))
    }

    @Test
    fun anUnparsableColourLeavesTheRoleAsItWas() {
        // The operator typed something that is not a colour. Nothing may turn black.
        val store = store()
        val before = store.resolved(false).palette.accent

        store.set(false, PaletteRole.ACCENT, "rot")

        assertEquals(before, store.resolved(false).palette.accent)
        assertEquals("", stored.getValue(false))
    }

    @Test
    fun theTwoDesignsDoNotDragEachOtherAlong() {
        val store = store()

        store.set(darkMode = true, role = PaletteRole.ACCENT, colour = "#FF0000")

        assertEquals(
            PaletteSource.CHANGED,
            store.resolved(true).roles[PaletteRole.ACCENT]!!.source,
        )
        assertEquals(
            PaletteSource.SHIPPED,
            store.resolved(false).roles[PaletteRole.ACCENT]!!.source,
        )
    }

    @Test
    fun resetToShippedBeatsAFileAsWell() {
        /*
         * The acceptance criterion of this stage: "exactly the shipped colours". With a file
         * in the middle, clearing the changes would not get there -- the file would.
         */
        fileRoles = mapOf(PaletteRole.ACCENT to Color(0xFF00FF00))
        val store = store()
        val shippedAccent = store.shipped(false).accent

        store.resetToShipped(false)

        assertEquals(shippedAccent, store.resolved(false).palette.accent)
        assertEquals(
            PaletteSource.CHANGED,
            store.resolved(false).roles[PaletteRole.ACCENT]!!.source,
        )
        assertEquals(6, PaletteOverrides.parse(stored.getValue(false)).size)
    }

    @Test
    fun resetToShippedReproducesEveryShippedRoleExactly() {
        /*
         * The acceptance criterion says "exactly the shipped colours -- not something that
         * resembles them", and the path goes through hex: resetToShipped writes each role as
         * a six-digit string and reads it back. That is only lossless because Compose packs an
         * sRGB Color at 8 bits per channel, so every channel is already an exact k/255. Pinned
         * for all six roles and both designs, because the day the packing changes, nothing
         * else would notice.
         */
        for (dark in listOf(false, true)) {
            val store = store()
            val shipped = store.shipped(dark)

            store.resetToShipped(dark)
            val after = store.resolved(dark).palette

            assertEquals(shipped.base, after.base, "surface, dark=$dark")
            assertEquals(shipped.windowBackground, after.windowBackground, "window, dark=$dark")
            assertEquals(shipped.controlInnerBackground, after.controlInnerBackground, "field, dark=$dark")
            assertEquals(shipped.labelTextFill, after.labelTextFill, "text, dark=$dark")
            assertEquals(shipped.accent, after.accent, "accent, dark=$dark")
            assertEquals(shipped.separatorLine, after.separatorLine, "separator, dark=$dark")
            assertEquals(shipped.textAccent, after.textAccent, "text accent, dark=$dark")
        }
    }

    @Test
    fun resetToShippedAlsoWinsOverAFileThatRestatesTheShippedSheet() {
        /*
         * The root profile's real situation: its stylesheet is the shipped sheet, written by
         * copyResourceIfRequired. Resetting there must land on the shipped palette and not on
         * something the file nudged.
         */
        val evening = JavaFxStylesheet.read("/KST4ContestDefaultEvening.css")
        fileRoles = JavaFxStylesheet.declaredRoles(
            JavaFxStylesheet::class.java.getResource("/KST4ContestDefaultEvening.css")!!.readText()
        )
        val store = store()

        store.resetToShipped(true)

        assertEquals(evening.accent, store.resolved(true).palette.accent)
        assertEquals(evening.textAccent, store.resolved(true).palette.textAccent)
        assertEquals(evening.labelTextFill, store.resolved(true).palette.labelTextFill)
    }

    @Test
    fun discardingChangesLetsTheFileApplyAgain() {
        fileRoles = mapOf(PaletteRole.ACCENT to Color(0xFF00FF00))
        val store = store()
        store.set(false, PaletteRole.ACCENT, "#FF0000")

        store.discardChanges(false)

        assertEquals(Color(0xFF00FF00), store.resolved(false).palette.accent)
        assertEquals(PaletteSource.FILE, store.resolved(false).roles[PaletteRole.ACCENT]!!.source)
        assertTrue(stored.getValue(false).isEmpty())
    }

    @Test
    fun theSnapshotRestoresWhatWasInForceWhenItWasTaken() {
        val store = store()
        store.set(false, PaletteRole.ACCENT, "#FF0000")

        store.takeSnapshot()
        store.set(false, PaletteRole.ACCENT, "#0000FF")
        store.set(false, PaletteRole.TEXT, "#00FF00")
        store.restoreSnapshot()

        // Including the change that was already there: the snapshot is not "shipped".
        assertEquals(Color(0xFFFF0000), store.resolved(false).palette.accent)
        assertEquals(PaletteSource.SHIPPED, store.resolved(false).roles[PaletteRole.TEXT]!!.source)
    }

    @Test
    fun theSnapshotRestoresBothDesignsAndNotOnlyTheOneInForce() {
        /*
         * takeSnapshot captures both, and the button says "back to how it was". An operator
         * who changed a colour, switched day/evening from the main window's menu, changed
         * another and then asked to go back means both -- undoing half would leave a state
         * they never chose.
         */
        val store = store()
        store.takeSnapshot()

        store.set(false, PaletteRole.ACCENT, "#FF0000")
        store.set(true, PaletteRole.ACCENT, "#00FF00")
        store.restoreSnapshot()

        assertEquals(
            PaletteSource.SHIPPED,
            store.resolved(false).roles[PaletteRole.ACCENT]!!.source,
            "the daylight change survived",
        )
        assertEquals(
            PaletteSource.SHIPPED,
            store.resolved(true).roles[PaletteRole.ACCENT]!!.source,
            "the evening change survived",
        )
    }

    @Test
    fun restoringWithoutASnapshotChangesNothing() {
        val store = store()
        store.set(false, PaletteRole.ACCENT, "#FF0000")

        store.restoreSnapshot()

        assertEquals(Color(0xFFFF0000), store.resolved(false).palette.accent)
    }

    @Test
    fun theLifelineReadsTheShippedSheetAndNotTheResolvedPalette() {
        /*
         * The rescue button is drawn in these colours. If it ever read the resolved palette,
         * it would vanish in exactly the palette it exists to undo -- and nobody would find
         * out until they needed it.
         */
        val store = store()
        val shippedBase = store.shipped(false).base

        PaletteRole.values().forEach { store.set(false, it, "#000000") }

        assertEquals(shippedBase, store.shipped(false).base)
        assertEquals(Color.Black, store.resolved(false).palette.base)
    }
}
