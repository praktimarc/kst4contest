package kst4contest.view.compose

import kst4contest.model.ChatMember
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * The two priority buttons beside the station list.
 *
 * They are a shortcut to the station the score says to work next, so what they name
 * and what they do when pressed is the whole of their value.
 *
 * `refresh()` is what fills them, and the score service is what calls it
 * (`Kst4ContestApplication` wires it to `topCandidates()` and calls it once at
 * startup). Reading the ranking on demand instead would never reach the screen: the
 * ranking is not Compose state, so a button derived from it could not ask for a
 * redraw when the score changed.
 */
class TopPriorityStateTest {

    private fun member(call: String) = ChatMember().apply { callSign = call }

    @Test
    fun `the buttons name the highest scoring stations and their score`() {
        val pa6i = member("PA6I")
        val dk7se = member("DK7SE")
        val state = TopPriorityState(
            topStations = { listOf(pa6i to 968.0, dk7se to 895.0) },
            onSelect = { },
        )

        state.refresh()

        assertEquals(listOf("1 PA6I 968", "2 DK7SE 895"), state.entries.map { it.label })
    }

    @Test
    fun `only the first two are offered`() {
        val state = TopPriorityState(
            topStations = { (1..5).map { member("DL${it}ABC") to it.toDouble() } },
            onSelect = { },
        )

        state.refresh()

        assertEquals(2, state.entries.size, "the rest are behind the more button")
    }

    @Test
    fun `fewer than two stations gives fewer buttons`() {
        val state = TopPriorityState(topStations = { listOf(member("DN9APW") to 10.0) }, onSelect = { })

        state.refresh()

        assertEquals(1, state.entries.size)
    }

    @Test
    fun `with no stations there is nothing to press`() {
        val state = TopPriorityState(topStations = { emptyList() }, onSelect = { })

        state.refresh()

        assertTrue(state.entries.isEmpty())
    }

    @Test
    fun `pressing one selects that station`() {
        val pa6i = member("PA6I")
        var selected: ChatMember? = null
        val state = TopPriorityState(topStations = { listOf(pa6i to 968.0) }, onSelect = { selected = it })
        state.refresh()

        state.entries.first().select()

        assertSame(pa6i, selected)
    }

    @Test
    fun `a station without a callsign is skipped rather than shown blank`() {
        val state = TopPriorityState(
            topStations = { listOf(ChatMember() to 500.0, member("DN9APW") to 400.0) },
            onSelect = { },
        )

        state.refresh()

        assertEquals(listOf("1 DN9APW 400"), state.entries.map { it.label })
    }

    /**
     * Nothing is shown until the score service has spoken.
     *
     * Deliberate: the ranking lives outside Compose, so the buttons are pushed from the
     * score service rather than pulled during composition. An earlier version derived
     * them on read, which looked simpler and could never have updated the screen.
     */
    @Test
    fun `the buttons stay empty until the score service fills them`() {
        val state = TopPriorityState(topStations = { listOf(member("PA6I") to 968.0) }, onSelect = { })

        assertTrue(state.entries.isEmpty(), "nothing should appear before the first refresh")

        state.refresh()

        assertEquals(listOf("1 PA6I 968"), state.entries.map { it.label })
    }

    @Test
    fun `a later refresh replaces what the buttons said`() {
        var ranking = listOf(member("PA6I") to 968.0)
        val state = TopPriorityState(topStations = { ranking }, onSelect = { })
        state.refresh()

        ranking = listOf(member("DK7SE") to 1200.0)
        state.refresh()

        assertEquals(listOf("1 DK7SE 1200"), state.entries.map { it.label })
    }
}
