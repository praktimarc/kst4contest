package kst4contest.view.compose

import kst4contest.model.ChatMember
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * The 22 columns of the station list.
 *
 * The ids are the operator's stored layout — renaming one loses their column widths
 * without a word — and these rows come from the network, so every value has to survive
 * a half-filled station.
 *
 * "wkdany" sits at the END of the worked group, not the start. Pinned to the JavaFX
 * table, which adds the band sub-columns under `isStn_bandActiveXX()` and only then
 * `workedCol.getColumns().add(wkdAny_subcol)` (`Kst4ContestApplication.java:2496`).
 *
 * A band cell's text comes from the `formatBand` the caller supplies, because the
 * JavaFX cell factory distinguishes worked from new from unavailable; the production
 * caller passes `formatBandCellStatus` (`:10460`).
 */
class StationColumnsTest {

    private fun columns() = StationColumns.all(
        tropoOf = { "" },
        priorityScoreOf = { 0.0 },
        formatBand = { _, _, worked -> if (worked) "X" else "" },
    )

    @Test
    fun `the ids are the ones the stored layout uses`() {
        assertEquals(
            listOf(
                "callsign", "name", "qra", "qrb", "qtf", "qrg", "airscout", "tropo",
                "score", "activity", "band-50", "band-70", "band-144", "band-432",
                "band-1296", "band-2320", "band-3400", "band-5760", "band-10g",
                "worked-any", "not-qrv", "category",
            ),
            columns().map { it.id },
        )
    }

    @Test
    fun `the headings are the ones the operator reads`() {
        assertEquals(
            listOf(
                "Callsign", "Name", "QRA", "QRB", "QTF", "QRG", "AP [minutes / pot%]",
                "Tropo", "Score", "Act", "50", "70", "144", "432", "23",
                "13", "9", "6", "3", "wkdany", "NOT QRV @", "Category",
            ),
            columns().map { it.title() },
        )
    }

    @Test
    fun `the worked columns are grouped under one heading`() {
        // Without the group, ten columns headed "50 70 144 432 23 13 9 6 3 wkdany"
        // say nothing about what they mean. The JavaFX table put them under "worked",
        // and added wkdany last.
        val grouped = columns().filter { it.group == "worked" }.map { it.id }

        assertEquals(
            listOf("band-50", "band-70", "band-144", "band-432", "band-1296",
                "band-2320", "band-3400", "band-5760", "band-10g", "worked-any"),
            grouped,
        )
    }

    @Test
    fun `columns outside that group carry no group`() {
        assertNull(columns().first { it.id == "callsign" }.group)
        assertNull(columns().first { it.id == "category" }.group)
    }

    @Test
    fun `a half-filled station reads as empty, not as a crash`() {
        val bare = ChatMember()

        columns().forEach { column ->
            assertDoesNotThrow({ column.value(bare) }, "column ${column.id}")
        }
    }

    @Test
    fun `a band column shows a cross only when that band was worked`() {
        val worked144 = ChatMember().apply { isWorked144 = true }

        val band144 = columns().first { it.id == "band-144" }
        val band432 = columns().first { it.id == "band-432" }

        assertEquals("X", band144.value(worked144))
        assertEquals("", band432.value(worked144))
    }

    @Test
    fun `the NOT QRV column names the bands the station is not on`() {
        /*
         * Every QRV flag starts true — a station is assumed to be on every band until
         * it says otherwise — so a fresh row leaves this column empty and it fills as
         * bands are marked off. Listing the QRV bands instead would put a full column
         * on almost every row and mean the opposite of its heading.
         */
        val member = ChatMember().apply {
            isQrv432 = false
            isQrv1240 = false
        }

        val notQrv = columns().first { it.id == "not-qrv" }.value(member)

        assertTrue(notQrv.contains("70cm"), "432 was marked off")
        assertTrue(notQrv.contains("23cm"), "1240 too")
        assertTrue(!notQrv.contains("2m"), "144 was never marked off, so it is not listed")
    }

    @Test
    fun `a station on every band has an empty NOT QRV column`() {
        assertEquals("", columns().first { it.id == "not-qrv" }.value(ChatMember()))
    }

    @Test
    fun `tropo and score come from outside the station`() {
        val columns = StationColumns.all(
            tropoOf = { "12 dB" },
            priorityScoreOf = { 250.0 },
            formatBand = { _, _, _ -> "" },
        )
        val member = ChatMember()

        assertEquals("12 dB", columns.first { it.id == "tropo" }.value(member),
            "reachability is computed by the controller, not carried by the row")
        assertEquals("250", columns.first { it.id == "score" }.value(member))
    }
}
