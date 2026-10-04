package kst4contest.view.compose

import androidx.compose.runtime.mutableStateListOf

/**
 * The divider positions of one splitter, as fractions of its length.
 *
 * Compose Multiplatform brings no SplitPane, so this is ours. The positions are a
 * setting, not decoration: an operator arranges the main window once for their screen
 * and expects it back at the next contest.
 *
 * @param paneCount how many panes the splitter holds; there is one divider less
 * @param stored the remembered positions, used only when they fit and make sense
 * @param minPanePx the smallest a pane may be dragged to, so nothing vanishes by accident
 * @param save called on release with the positions to remember — never during a drag,
 *        because the JavaFX version wrote the profile to disk on every pixel of one
 */
class SplitterState(
    val paneCount: Int,
    stored: DoubleArray,
    private val minPanePx: Float = DEFAULT_MIN_PANE_PX,
    private val save: (DoubleArray) -> Unit,
) {

    private val dividerCount = (paneCount - 1).coerceAtLeast(0)

    private val livePositions = mutableStateListOf<Float>().apply {
        addAll(sanitize(stored, dividerCount))
    }

    /** What the draw reads. Compose state, so a drag reaches the screen. */
    val positions: List<Float> get() = livePositions

    private var savedPositions: List<Float> = livePositions.toList()

    /**
     * Moves one divider, held off its neighbours and off the minimum pane size.
     *
     * @param to the wanted position as a fraction of the whole
     * @param totalPx the splitter's length, needed to turn the minimum into a fraction
     */
    fun drag(index: Int, to: Float, totalPx: Float) {
        if (index !in 0 until dividerCount) return

        val minFraction = if (totalPx > 0f) minPanePx / totalPx else 0f
        val lowerBound = (if (index == 0) 0f else livePositions[index - 1]) + minFraction
        val upperBound = (if (index == dividerCount - 1) 1f else livePositions[index + 1]) - minFraction

        // A window too narrow for the minimums would give an empty range; splitting the
        // remaining room is better than laying out nothing.
        livePositions[index] = if (lowerBound > upperBound) {
            (lowerBound + upperBound) / 2f
        } else {
            to.coerceIn(lowerBound, upperBound)
        }
    }

    /** Remembers the positions, but only if the drag actually moved something. */
    fun dragFinished() {
        val current = livePositions.toList()
        if (current == savedPositions) return
        savedPositions = current
        save(DoubleArray(current.size) { current[it].toDouble() })
    }

    /** The pane lengths for a given splitter length; they always add up to the whole. */
    fun paneSizes(totalPx: Float): List<Float> {
        if (paneCount <= 1) return listOf(totalPx)
        val edges = buildList {
            add(0f)
            livePositions.forEach { add(it * totalPx) }
            add(totalPx)
        }
        return (0 until paneCount).map { edges[it + 1] - edges[it] }
    }

    companion object {

        /** Enough to still see a table header and grab the divider again. */
        const val DEFAULT_MIN_PANE_PX = 24f

        /**
         * How much length the panes may share, once the divider handles have taken theirs.
         *
         * The panes and the handles are laid out as siblings, so distributing the whole
         * length among the panes overflows the splitter by one handle width per divider —
         * 20 dp for the five-pane message column, enough to push the bottom pane off the
         * edge. Never negative: a splitter smaller than its own handles hands out nothing.
         */
        fun availableFor(totalPx: Float, dividerPx: Float, paneCount: Int): Float {
            val dividerCount = (paneCount - 1).coerceAtLeast(0)
            return (totalPx - dividerPx * dividerCount).coerceAtLeast(0f)
        }

        /**
         * The remembered positions are used only when there are exactly as many as there
         * are dividers, each inside the pane and in ascending order. Anything else is a
         * profile from an older layout, and an even split beats a window whose panes have
         * no size.
         */
        internal fun sanitize(stored: DoubleArray, dividerCount: Int): List<Float> {
            val evenSplit = (1..dividerCount).map { it.toFloat() / (dividerCount + 1) }
            if (stored.size != dividerCount) return evenSplit
            if (stored.any { !it.isFinite() || it <= 0.0 || it >= 1.0 }) return evenSplit
            if (stored.toList() != stored.sorted()) return evenSplit
            return stored.map { it.toFloat() }
        }
    }
}
