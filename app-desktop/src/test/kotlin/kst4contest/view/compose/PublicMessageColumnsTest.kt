package kst4contest.view.compose

import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import kst4contest.observe.SimpleValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * The public message tab, the third of the three below the station list.
 *
 * The other two reuse the monitor tables of Etappe 4 under their own layout ids; this
 * one is its own. Its ids are the operator's stored widths, and its rows come off the
 * wire half-filled.
 */
class PublicMessageColumnsTest {

    @Test
    fun `the ids are the ones the stored layout uses`() {
        assertEquals(
            listOf("time", "callsign", "name", "last-qrg", "message", "category"),
            PublicMessageColumns.all().map { it.id },
        )
    }

    @Test
    fun `the headings are the ones the operator reads`() {
        assertEquals(
            listOf("Time", "Callsign", "Name", "Last QRG", "Message", "Category"),
            PublicMessageColumns.all().map { it.title() },
        )
    }

    @Test
    fun `a message without a sender reads as empty, not as a crash`() {
        val bare = ChatMessage()

        PublicMessageColumns.all().forEach { column ->
            assertDoesNotThrow({ column.value(bare) }, "column ${column.id}")
        }
    }

    @Test
    fun `the time is a clock time, as it is in every other table`() {
        val message = ChatMessage().apply { messageGeneratedTime = "1759000000" }

        assertEquals("19:06:40", PublicMessageColumns.all().first { it.id == "time" }.value(message))
    }

    @Test
    fun `callsign, name and QRG come from the sender`() {
        val message = ChatMessage().apply {
            sender = ChatMember().apply {
                callSign = "DN9APW"
                name = "Philipp"
                frequency = SimpleValue("144.3")
            }
            messageText = "CQ contest"
        }

        val columns = PublicMessageColumns.all()

        assertEquals("DN9APW", columns.first { it.id == "callsign" }.value(message))
        assertEquals("Philipp", columns.first { it.id == "name" }.value(message))
        assertEquals("144.300", columns.first { it.id == "last-qrg" }.value(message),
            "padded like every other QRG column")
        assertEquals("CQ contest", columns.first { it.id == "message" }.value(message))
    }
}
