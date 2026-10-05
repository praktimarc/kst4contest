package kst4contest.view.compose

import kst4contest.model.AirPlane
import kst4contest.model.AirPlaneReflectionInfo
import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The messages addressed to this station. Its own pane above the send line, not one of
 * the tabs — the operator must be able to see a private call without switching away from
 * whatever tab they are reading.
 */
class DirectedMessageColumnsTest {

    private val columns = DirectedMessageColumns.all { "formatted: ${it.messageText}" }

    /** The stored column widths are keyed by these ids; renaming one loses a setting. */
    @Test
    fun `the column ids are the ones the widths are stored under`() {
        assertEquals(
            listOf("time", "callsign", "name", "qra", "qrb", "message", "last-qrg", "airscout", "category"),
            columns.map { it.id },
        )
    }

    /** The order operators read left to right, character for character from JavaFX. */
    @Test
    fun `the titles are the ones on screen today`() {
        assertEquals(
            listOf("Time", "Callsign", "Name", "QRA", "QRB", "Message", "Last known QRG", "AP [minutes / pot%]", "Category"),
            columns.map { it.title() },
        )
    }

    @Test
    fun `a message from a known station fills the sender columns`() {
        val message = message(sender = member("DL0ABC", name = "Hans", qra = "JO40AA"))

        assertEquals("DL0ABC", cell("callsign", message))
        assertEquals("Hans", cell("name", message))
        assertEquals("JO40AA", cell("qra", message))
    }

    /**
     * A message whose sender is not in the user list any more still has to render. The
     * JavaFX cells all guarded for it, and a station leaving the chat mid contest is
     * normal, not exceptional.
     */
    @Test
    fun `a message without a sender still renders every column`() {
        val message = message(sender = null)
        columns.forEach { column ->
            val value = column.value(message)
            assertTrue(value.isNotEmpty() || value.isEmpty(), "${column.id} threw")
        }
        assertEquals("", cell("callsign", message))
        assertEquals("", cell("qrb", message))
    }

    @Test
    fun `the message text comes from the formatter the controller provides`() {
        val message = message(sender = member("DL0ABC"), text = "pse 23cm")
        assertEquals("formatted: pse 23cm", cell("message", message))
    }

    /** The odd degree sign outside the bracket is JavaFX's; operators read it that way. */
    @Test
    fun `the qrb cell carries the distance and the bearing`() {
        val sender = member("DL0ABC").apply {
            qrb = 342.7
            setQTFdirection(88.4)
        }
        assertEquals("342 km (88)°", cell("qrb", message(sender = sender)))
    }

    /** Own messages get no distance to themselves. */
    @Test
    fun `a message from the own station has no qrb`() {
        val sender = member("DN9APW").apply { qrb = 0.0 }
        val cols = DirectedMessageColumns.all(ownCallSign = { "DN9APW" }) { "x" }
        val cell = cols.single { it.id == "qrb" }.value(message(sender = sender))
        assertEquals("", cell)
    }

    @Test
    fun `the airscout cell is the shared one`() {
        val sender = member("DL0ABC").apply {
            airPlaneReflectInfo = AirPlaneReflectionInfo().apply {
                risingAirplanes = mutableListOf(
                    AirPlane().apply { arrivingDurationMinutes = 7; potential = 85 },
                )
            }
        }
        assertEquals("7 (85%)", cell("airscout", message(sender = sender)))
    }

    @Test
    fun `the category names the chat the message arrived in`() {
        val message = message(sender = member("DL0ABC")).apply { chatCategory = ChatCategory(2) }
        assertTrue(cell("category", message).isNotBlank())
    }

    private fun cell(id: String, message: ChatMessage): String =
        columns.single { it.id == id }.value(message)

    private fun message(sender: ChatMember?, text: String = "hello"): ChatMessage =
        ChatMessage().apply {
            this.sender = sender
            messageText = text
            chatCategory = ChatCategory(2)
        }

    private fun member(call: String, name: String = "", qra: String = ""): ChatMember =
        ChatMember().apply {
            setCallSign(call)
            this.name = name
            this.qra = qra
        }
}
