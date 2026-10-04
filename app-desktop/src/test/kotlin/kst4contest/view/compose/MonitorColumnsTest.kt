package kst4contest.view.compose

import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import kst4contest.model.ClusterMessage
import kst4contest.observe.SimpleValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow

/**
 * The columns of the two monitor tables.
 *
 * Two things are worth pinning. The ids are part of the operator's stored layout, so
 * renaming one silently loses their column widths. And every value has to survive a
 * half-filled message: these tables are fed straight from the wire, and a
 * NullPointerException in a live table during a contest is the worst possible moment.
 */
class MonitorColumnsTest {

    @Test
    fun `the DX cluster columns keep the ids the stored layout uses`() {
        assertEquals(
            listOf("time", "call-tx", "locator-tx", "call-rx", "locator-rx", "qrg", "message", "worked"),
            MonitorColumns.dxCluster().map { it.id },
        )
    }

    @Test
    fun `the DX cluster columns keep their headings`() {
        assertEquals(
            listOf("Time", "Call tx", "LOC tx", "Call rx", "LOC rx", "QRG", "Message", "wkd"),
            MonitorColumns.dxCluster().map { it.title },
        )
    }

    @Test
    fun `a DX cluster row without sender or receiver reads as empty, not as a crash`() {
        val bare = ClusterMessage()

        MonitorColumns.dxCluster().forEach { column ->
            assertDoesNotThrow({ column.value(bare) }, "column ${column.id}")
        }
    }

    @Test
    fun `the worked column shows a cross, as the JavaFX table did`() {
        val worked = ClusterMessage().apply { isReceiverWkd = true }
        val notWorked = ClusterMessage()

        val column = MonitorColumns.dxCluster().first { it.id == "worked" }

        assertEquals("X", column.value(worked))
        assertEquals("", column.value(notWorked))
    }

    @Test
    fun `the QSO monitor keeps the nine columns the JavaFX table actually showed`() {
        // Ten were built there and nine added; the "Name" column was never shown.
        assertEquals(
            listOf("time", "call-tx", "last-qrg-tx", "worked-tx", "call-rx", "last-qrg-rx",
                "worked-rx", "message", "category"),
            MonitorColumns.qsoOfTheOther().map { it.id },
        )
    }

    @Test
    fun `a QSO monitor row without sender or receiver reads as empty, not as a crash`() {
        val bare = ChatMessage()

        MonitorColumns.qsoOfTheOther().forEach { column ->
            assertDoesNotThrow({ column.value(bare) }, "column ${column.id}")
        }
    }

    @Test
    fun `the time column shows a clock time, not the epoch off the wire`() {
        // The server sends epoch seconds; the JavaFX cell ran them through
        // Utils4KST.time_convertEpochToReadable, which is H:mm:ss in Etc/UTC.
        val spot = ClusterMessage().apply { timeGenerated = "1759000000" }

        val column = MonitorColumns.dxCluster().first { it.id == "time" }

        assertEquals("19:06:40", column.value(spot))
    }

    @Test
    fun `an unreadable time is shown as it came rather than throwing`() {
        val spot = ClusterMessage().apply { timeGenerated = "not a number" }

        val column = MonitorColumns.dxCluster().first { it.id == "time" }

        assertEquals("not a number", column.value(spot))
    }

    @Test
    fun `the QSO time column is formatted the same way`() {
        val message = ChatMessage().apply { messageGeneratedTime = "1759000000" }

        val column = MonitorColumns.qsoOfTheOther().first { it.id == "time" }

        assertEquals("19:06:40", column.value(message))
    }

    @Test
    fun `the cluster QRG is padded the way the main window pads it`() {
        val spot = ClusterMessage().apply {
            receiver = ChatMember().apply { frequency = SimpleValue("144.3") }
        }

        val column = MonitorColumns.dxCluster().first { it.id == "qrg" }

        assertEquals("144.300", column.value(spot),
            "the same column in the main window pads to three places; two tables of "
                + "the same data must not disagree")
    }

    @Test
    fun `the category comes from the sender, as it did in the JavaFX table`() {
        val sender = ChatMember().apply { chatCategory = ChatCategory(2) }
        val message = ChatMessage().apply { this.sender = sender }

        val column = MonitorColumns.qsoOfTheOther().first { it.id == "category" }

        assertEquals(
            sender.chatCategory.getChatCategoryName(sender.chatCategory.categoryNumber),
            column.value(message),
            "the old cell read getSender().getChatCategory(); the message carries a "
                + "field of the same name that is not always the same value",
        )
    }
}
