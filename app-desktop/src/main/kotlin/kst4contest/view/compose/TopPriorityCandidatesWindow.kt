package kst4contest.view.compose

import kst4contest.view.i18n.LocalStrings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.controller.ScoreService
import java.util.Locale
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * One line of the candidate list.
 *
 * Kept as a function rather than inlined into the row so the wording stays under a
 * test: the JavaFX cell spelled it `callsign + "  |  score " + %.0f` for two releases,
 * and an operator scanning the list reads the shape before the content.
 */
internal fun candidateLabel(candidate: ScoreService.TopCandidate): String =
    candidate.displayCallSign +
        "  |  score " +
        String.format(Locale.US, "%.0f", candidate.score)

/**
 * The full priority list, opened by the "more" button beside the two priority buttons.
 *
 * It exists so the main window can stay compact while the whole ranking is still
 * reachable. Its JavaFX predecessor was the only Stage this application constructed
 * itself, which is why it is ported before anything is deleted: no JavaFX dependency
 * can be dropped while it stands.
 */
object TopPriorityCandidatesWindow {

    private val host = ComposeWindowHost("TopPriorityCandidates")

    @JvmStatic
    val isOpen: Boolean get() = host.isOpen

    @JvmStatic
    fun applyDarkMode(darkMode: Boolean) = host.applyDarkMode(darkMode)

    @JvmStatic
    fun hide() = host.close()

    /**
     * `Supplier` and `Consumer` rather than Kotlin function types: the only caller is
     * Java, and a Java lambda cannot produce Kotlin's `Unit`, so the Kotlin shape would
     * force `return kotlin.Unit.INSTANCE` into the call site for nothing.
     *
     * @param candidates read afresh while the window is open; the ranking changes as
     *        scores do, and the JavaFX list was bound to the roster for that reason
     * @param onPicked called with the chosen candidate, after which the window closes
     */
    @JvmStatic
    fun show(
        candidates: Supplier<List<ScoreService.TopCandidate>>,
        onPicked: Consumer<ScoreService.TopCandidate>,
        darkMode: Boolean,
        baseFontSizeSp: Float,
        /**
         * The palette of the active profile, handed to the theme. Null draws the shipped
         * palette.
         */
        paletteStore: PaletteStore? = null,
        /**
         * The interface language of the active profile, handed to the window. Null draws the
         * base language.
         */
        languageStore: kst4contest.view.i18n.LanguageStore? = null,
    ) {
        host.show(
            title = { kst4contest.view.i18n.CurrentStrings.get().candidatesTitle },
            darkMode = darkMode,
            baseFontSizeSp = baseFontSizeSp,
            paletteStore = paletteStore,
            languageStore = languageStore,
            widthDp = 360f,
            heightDp = 500f,
        ) { close ->
            var ranking by remember { mutableStateOf(candidates.get()) }

            /*
             * Polled rather than pushed. The ranking lives in the score service, which
             * is not Compose state, and this window is open for seconds at a time while
             * an operator reads it — wiring a callback in and out for that is more
             * machinery than the question deserves.
             */
            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(POLL_INTERVAL_MS)
                    ranking = candidates.get()
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(Modifier.fillMaxSize().padding(5.dp)) {
                    CandidateList(
                        ranking = ranking,
                        onPicked = { picked ->
                            onPicked.accept(picked)
                            close()
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text(
                        LocalStrings.current.candidatesDoubleClick,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    /** The list on its own, so a test can render it without opening a window. */
    @Composable
    internal fun CandidateListForTest(
        ranking: List<ScoreService.TopCandidate>,
        onPicked: (ScoreService.TopCandidate) -> Unit,
    ) = CandidateList(ranking, onPicked)

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun CandidateList(
        ranking: List<ScoreService.TopCandidate>,
        onPicked: (ScoreService.TopCandidate) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        if (ranking.isEmpty()) {
            Text(LocalStrings.current.candidatesNone, modifier = modifier.padding(4.dp))
            return
        }

        LazyColumn(modifier) {
            items(ranking) { candidate ->
                Text(
                    text = candidateLabel(candidate),
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { },
                            onDoubleClick = { onPicked(candidate) },
                        )
                        .padding(vertical = 3.dp, horizontal = 4.dp),
                )
            }
        }
    }

    /** Fast enough that a changing ranking is not stale, slow enough to cost nothing. */
    private const val POLL_INTERVAL_MS = 1_000L
}
