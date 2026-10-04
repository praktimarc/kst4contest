package kst4contest.view.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.awt.Cursor

/** Which way the panes are stacked. */
enum class SplitterOrientation { HORIZONTAL, VERTICAL }

/**
 * A splitter with draggable dividers, because Compose Multiplatform has none.
 *
 * The dividers look like the column handles of the tables — a hairline to see, a wider
 * area to grab — so the two things an operator can drag in this window feel the same.
 *
 * @param panes one composable per pane; there is one divider less than there are panes.
 *        Must be as many as [SplitterState.paneCount] — a mismatch lays out silently
 *        wrong rather than failing, which is what MainWindowSplitterDefaultsTest guards
 *        for the three splitters of the main window.
 */
@Composable
fun SplitterPane(
    state: SplitterState,
    orientation: SplitterOrientation,
    panes: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val handlePx = with(density) { Density.COLUMN_HANDLE_WIDTH.toPx() }
        val fullPx = with(density) {
            if (orientation == SplitterOrientation.HORIZONTAL) maxWidth.toPx() else maxHeight.toPx()
        }

        /*
         * The handles are siblings of the panes, so what the panes share is what is left
         * after them. The same span drives the drag, which keeps the divider under the
         * cursor rather than drifting from it by a handle width per divider.
         */
        val totalPx = SplitterState.availableFor(fullPx, handlePx, state.paneCount)
        val sizes = state.paneSizes(totalPx)

        val body: @Composable () -> Unit = {
            panes.forEachIndexed { index, pane ->
                val sizeDp = with(density) { sizes.getOrElse(index) { 0f }.toDp() }

                Box(
                    if (orientation == SplitterOrientation.HORIZONTAL) {
                        Modifier.width(sizeDp).fillMaxHeight()
                    } else {
                        Modifier.height(sizeDp).fillMaxWidth()
                    },
                ) {
                    pane()
                }

                if (index < panes.size - 1) {
                    SplitterHandle(
                        state = state,
                        index = index,
                        orientation = orientation,
                        totalPx = totalPx,
                    )
                }
            }
        }

        if (orientation == SplitterOrientation.HORIZONTAL) {
            Row(Modifier.fillMaxWidth().fillMaxHeight()) { body() }
        } else {
            Column(Modifier.fillMaxWidth().fillMaxHeight()) { body() }
        }
    }
}

/**
 * One divider. The drag is converted to a fraction of the whole before it reaches the
 * state, which is what keeps the stored value independent of the screen it was set on.
 */
@Composable
private fun SplitterHandle(
    state: SplitterState,
    index: Int,
    orientation: SplitterOrientation,
    totalPx: Float,
) {
    val horizontal = orientation == SplitterOrientation.HORIZONTAL
    val cursor = if (horizontal) Cursor.E_RESIZE_CURSOR else Cursor.N_RESIZE_CURSOR

    Box(
        modifier = Modifier
            .then(
                if (horizontal) Modifier.width(Density.COLUMN_HANDLE_WIDTH).fillMaxHeight()
                else Modifier.height(Density.COLUMN_HANDLE_WIDTH).fillMaxWidth(),
            )
            .pointerHoverIcon(PointerIcon(Cursor(cursor)))
            .pointerInput(index, totalPx) {
                detectDragGestures(
                    onDragEnd = { state.dragFinished() },
                    onDragCancel = { state.dragFinished() },
                ) { _, dragAmount ->
                    if (totalPx <= 0f) return@detectDragGestures
                    val delta = if (horizontal) dragAmount.x else dragAmount.y
                    val current = state.positions.getOrElse(index) { 0f }
                    state.drag(index, current + delta / totalPx, totalPx)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (horizontal) Modifier.width(Density.HAIRLINE).fillMaxHeight()
                    else Modifier.height(Density.HAIRLINE).fillMaxWidth(),
                )
                .background(MaterialTheme.colorScheme.outline),
        )
    }
}
