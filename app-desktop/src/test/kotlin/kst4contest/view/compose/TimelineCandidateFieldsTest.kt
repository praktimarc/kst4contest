package kst4contest.view.compose

import kst4contest.model.ChatCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Pins the field order of the timeline candidate.
 *
 * The JavaFX builder produced its own DTO and copied it across field by field; the builder
 * now constructs this type directly. Three of the ten values are plain numbers of the same
 * kind — minuteBucket, laneIndex and opportunityPotentialPercent are all Int — so a swapped
 * pair would compile, run, and quietly draw the right station in the wrong lane at the wrong
 * minute. Nothing else checks that.
 */
class TimelineCandidateFieldsTest {

    @Test
    fun theTenValuesKeepTheirPositions() {
        val category = ChatCategory(ChatCategory.VUHF)

        val candidate = TimelineCandidate(
            "DL1ABC", "DL1ABC/P", category, 90_000L, 3, 1, 215.0, 42.5, 77, "tooltip",
        )

        assertEquals("DL1ABC", candidate.callSignRaw)
        assertEquals("DL1ABC/P", candidate.displayCallSign)
        assertEquals(category, candidate.preferredChatCategory)
        assertEquals(90_000L, candidate.timeUntilMs)
        assertEquals(3, candidate.minuteBucket)
        assertEquals(1, candidate.laneIndex)
        assertEquals(215.0, candidate.targetAzimuth)
        assertEquals(42.5, candidate.score)
        assertEquals(77, candidate.opportunityPotentialPercent)
        assertEquals("tooltip", candidate.tooltipText)
    }

    @Test
    fun theSortTheStripReliesOnIsByMinuteThenLane() {
        val category = ChatCategory(ChatCategory.VUHF)
        fun candidate(minute: Int, lane: Int) = TimelineCandidate(
            "DL1ABC", "DL1ABC", category, 0L, minute, lane, 0.0, 0.0, 0, null,
        )

        val sorted = listOf(candidate(2, 0), candidate(1, 1), candidate(1, 0))
            .sortedWith(compareBy({ it.minuteBucket }, { it.laneIndex }))

        // The builder sorts before handing the list over; the strip draws in that order.
        assertEquals(listOf(1 to 0, 1 to 1, 2 to 0), sorted.map { it.minuteBucket to it.laneIndex })
    }
}
