package kst4contest.view.compose

import androidx.compose.runtime.mutableStateOf
import kst4contest.model.ChatMember

/** One priority button: what it says and what pressing it does. */
class TopPriorityEntry(val label: String, val select: () -> Unit)

/**
 * The two priority buttons beside the station list.
 *
 * A shortcut to the station the score says to work next, so what they name and what
 * they do when pressed is the whole of their value. Everything below the first two is
 * behind the "more" button, as it was.
 *
 * @param topStations the ranked stations with their scores, highest first
 * @param onSelect selects a station in the list, which is what pressing a button did
 */
class TopPriorityState(
    private val topStations: () -> List<Pair<ChatMember, Double>>,
    private val onSelect: (ChatMember) -> Unit,
) {

    private val _entries = mutableStateOf<List<TopPriorityEntry>>(emptyList())

    val entries: List<TopPriorityEntry>
        get() = _entries.value

    fun refresh() {
        _entries.value = topStations()
            .filter { (member, _) -> !member.callSign.isNullOrBlank() }
            .take(SHOWN)
            .mapIndexed { index, (member, score) ->
                TopPriorityEntry(
                    label = "${index + 1} ${member.callSign} ${score.toInt()}",
                ) { onSelect(member) }
            }
    }

    private companion object {
        /** Two, as the JavaFX pane had; the rest sit behind "more". */
        const val SHOWN = 2
    }
}
