package kst4contest.view.feed

import kst4contest.model.ContestSked
import kst4contest.observe.UiDispatcher
import kst4contest.view.compose.TimelineCandidate
import kst4contest.view.compose.TimelineState

/**
 * Fills the timeline above the send field.
 *
 * A class of its own because of how it was found. The feed used to sit inside
 * `updateTimelineVisuals()`, behind a guard on a JavaFX field that the never-shown
 * window construction created; deleting that construction would have emptied the
 * Compose timeline without a compile error and with a green suite. A feed that can be
 * constructed on its own cannot hide inside something else's lifetime.
 *
 * Knows nothing about JavaFX. Delivery goes through the [UiDispatcher] because the
 * controller's listeners fire on network threads.
 */
class TimelineFeed(
    private val target: TimelineState,
    private val dispatcher: UiDispatcher,
) {

    /**
     * Replaces everything the timeline shows, in one delivery.
     *
     * One delivery and not four: four would let the operator see a frame with new skeds
     * against an old antenna heading.
     *
     * The lists are copied before they are handed over. The caller's are snapshots of
     * roster state that keeps changing, and the timeline must not follow them.
     */
    fun push(
        skeds: List<ContestSked>,
        candidates: List<TimelineCandidate>,
        antennaAzimuthDeg: Double,
        beamWidthDeg: Double,
    ) {
        val skedsCopy = skeds.toList()
        val candidatesCopy = candidates.toList()

        dispatcher.runOnUi {
            target.replaceSkeds(skedsCopy)
            target.replaceCandidates(candidatesCopy)
            target.antennaAzimuth = antennaAzimuthDeg
            target.setBeamWidth(beamWidthDeg)
        }
    }
}
