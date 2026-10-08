package kst4contest.view.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * A new private message is highlighted and the highlight fades over five minutes, so a call
 * that arrived while the operator was looking elsewhere is still findable — noticing one is
 * what that pane is for. Own messages get their own colour instead.
 *
 * The steps and the colours are PrivateMessageRowStyleResolver's and the stylesheets', which
 * both designs share.
 */
class DirectedMessageRowStyleTest {

    @Test
    fun `a message just arrived is brightest`() {
        val accent = DirectedMessageRowStyle.accentFor(ownMessage = false, ageSeconds = 0)
        assertEquals(DirectedMessageRowStyle.FRESH, accent?.background)
    }

    /** Six steps, each dimmer than the last, so age is readable without reading the clock. */
    @Test
    fun `the highlight fades step by step`() {
        val ages = listOf(30L, 60L, 90L, 120L, 180L, 300L)
        val colours = ages.map { DirectedMessageRowStyle.accentFor(false, it)?.background }

        assertEquals(ages.size, colours.distinct().size, "two steps share a colour")
        colours.forEach { assertNotEquals(null, it) }
    }

    /** The step boundaries are inclusive, as the resolver's `<=` is. */
    @Test
    fun `a boundary age belongs to the brighter step`() {
        assertEquals(
            DirectedMessageRowStyle.accentFor(false, 30),
            DirectedMessageRowStyle.accentFor(false, 29),
        )
        assertNotEquals(
            DirectedMessageRowStyle.accentFor(false, 30),
            DirectedMessageRowStyle.accentFor(false, 31),
        )
    }

    /** After five minutes it is no longer news and the row goes back to normal. */
    @Test
    fun `an old message is not highlighted at all`() {
        assertNull(DirectedMessageRowStyle.accentFor(false, 301))
        assertNull(DirectedMessageRowStyle.accentFor(false, 60 * 60))
    }

    /** Own messages are blue at any age: they are context, not news. */
    @Test
    fun `an own message keeps its own colour however old`() {
        assertEquals(DirectedMessageRowStyle.OWN, DirectedMessageRowStyle.accentFor(true, 0)?.background)
        assertEquals(DirectedMessageRowStyle.OWN, DirectedMessageRowStyle.accentFor(true, 9999)?.background)
    }

    /** The resolver in the JavaFX view decides the steps; this must not drift from it. */
    @Test
    fun `the steps are the resolver's own`() {
        listOf(0L, 30L, 60L, 90L, 120L, 180L, 300L).forEach { age ->
            val styleClass = kst4contest.view.PrivateMessageRowStyleResolver
                .resolveStyleClass(false, age)
            val accent = DirectedMessageRowStyle.accentFor(false, age)
            assertEquals(
                styleClass != null,
                accent != null,
                "age $age: the resolver says $styleClass but the accent says $accent",
            )
        }
    }
}
