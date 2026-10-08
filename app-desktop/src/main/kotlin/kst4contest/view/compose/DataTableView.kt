package kst4contest.view.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Surface
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import java.awt.Cursor
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A multi-column table.
 *
 * The rules — row identity, selection across a refresh, stable sorting, stored widths
 * — live in [DataTableState] and are tested there. What is here is only the drawing
 * and the gestures, because nothing that draws is covered by a test in this release.
 *
 * Rows are keyed by the state's own row key, never by their position: the lists this
 * table serves are live, and a positional key reshuffles every row whenever something
 * arrives at the top.
 *
 * @param rowStyle colours a whole row, as the station list does from its priority
 *        score. Not used by the monitor tables, and deliberately still present: the
 *        interface is cut for the station list of Etappe 5.
 */
/*
 * TooltipArea is experimental in Compose foundation. The opt-in is deliberate: it is
 * the supported way to reach the hover text the JavaFX cells had, and losing that
 * loses the only way to read a value that does not fit its column.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> DataTableView(
    state: DataTableState<T>,
    modifier: Modifier = Modifier,
    rowStyle: ((T) -> CellAccent?)? = null,
    onRowClick: ((T) -> Unit)? = null,
) {
    val density = LocalDensity.current

    val horizontal = rememberScrollState()
    val vertical = rememberLazyListState()

    /*
     * These lists are newest-first, and a LazyColumn anchors to the item it was
     * showing — so every arrival at the top pushes the view down and the operator
     * drifts away from the newest line without touching anything.
     *
     * The rule: stay at the top while the operator is at the top, and leave them alone
     * once they have scrolled down to read something. Yanking a reader back to the top
     * on every inbound message would be worse than drifting.
     */
    var stickToTop by remember { mutableStateOf(true) }

    LaunchedEffect(vertical) {
        snapshotFlow { vertical.isScrollInProgress }.collect { scrolling ->
            /* Only a scroll the operator made ends; a prepend does not scroll. */
            if (!scrolling) {
                stickToTop = vertical.firstVisibleItemIndex == 0 &&
                    vertical.firstVisibleItemScrollOffset == 0
            }
        }
    }

    val newestKey = state.visibleRows.firstOrNull()?.let { state.keyOf(it) }

    LaunchedEffect(newestKey) {
        if (stickToTop && newestKey != null) {
            vertical.scrollToItem(0)
        }
    }

    /*
     * Both directions scroll. The columns of these two tables need about 890dp and
     * 1000dp, and the stored window width starts at 700: without this the last columns
     * are simply unreachable, and so are the handles that would narrow them. The JavaFX
     * tables had a horizontal scrollbar.
     */
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val slack = unusedWidth(state, maxWidth)
    val lastColumnId = state.orderedColumns.lastOrNull()?.id

    Column(modifier = Modifier.fillMaxSize()) {
        /*
         * A second header row above the first, spanning the columns that share a
         * group. The station list needs it: ten columns headed "wkdany 50 70 144 432
         * 23 13 9 6 3" say nothing without the word "worked" over them.
         */
        val groups = state.orderedColumns.map { it.group }

        if (groups.any { it != null }) {
            Row(
                modifier = Modifier
                    .horizontalScroll(horizontal)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .heightIn(min = Density.ROW_MIN_HEIGHT),
            ) {
                var index = 0

                while (index < state.orderedColumns.size) {
                    val group = groups[index]
                    var span = 1

                    while (index + span < groups.size && groups[index + span] == group && group != null) {
                        span++
                    }

                    val width = (index until index + span).sumOf { position ->
                        val column = state.orderedColumns[position]
                        val stored = state.widthOf(column.id)
                        val own = stored ?: (DEFAULT_COLUMN_WIDTH * column.weight).value.toDouble()
                        own + Density.COLUMN_HANDLE_WIDTH.value
                    }

                    Box(
                        modifier = Modifier.width(width.dp).padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (group != null) {
                            Text(
                                group,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                    }

                    index += span
                }
            }

            HorizontalDivider(thickness = Density.HAIRLINE)
        }

        /*
         * IntrinsicSize.Min, so the fillMaxHeight of the cells and the grab handles
         * resolves against the header's own content instead of against the whole
         * table. Without it the header took the entire height and the handles became
         * full-length bars down the window.
         */
        Row(
            modifier = Modifier
                .horizontalScroll(horizontal)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .height(IntrinsicSize.Min)
                .heightIn(min = Density.ROW_MIN_HEIGHT),
        ) {
            state.orderedColumns.forEachIndexed { index, column ->
                val sorted = state.sortedColumnId == column.id

                /*
                 * Click to sort, drag sideways to reorder — the two gestures the JavaFX
                 * header had. The drag is measured in whole column steps rather than
                 * pixels, so a column moves once per column crossed instead of
                 * flickering between two places.
                 */
                var dragged by remember(column.id) { mutableStateOf(0f) }

                Box(
                    modifier = Modifier
                        .columnWidth(state, column, if (column.id == lastColumnId) slack else 0.dp)
                        .fillMaxHeight()
                        .clickable { state.toggleSort(column.id) }
                        .pointerInput(column.id, index) {
                            detectHorizontalDragGestures(
                                onDragEnd = { dragged = 0f },
                                onDragCancel = { dragged = 0f },
                            ) { _, dragAmount ->
                                dragged += dragAmount
                                val step = with(density) { DEFAULT_COLUMN_WIDTH.toPx() } / 2f

                                if (dragged > step) {
                                    state.moveColumn(column.id, index + 1)
                                    dragged = 0f
                                } else if (dragged < -step) {
                                    state.moveColumn(column.id, index - 1)
                                    dragged = 0f
                                }
                            }
                        }
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.MOVE_CURSOR)))
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        column.title() + if (sorted) (if (state.sortAscending) " ▲" else " ▼") else "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }

                /*
                 * The grab handle between two headers. Two pixels wide on purpose: the
                 * JavaFX table put the same gesture on the column border, and operators
                 * resize these tables constantly during a contest.
                 */
                /*
                 * A hairline to look at, a wider area to grab. The JavaFX column border
                 * was a thin line; a 4dp bar in the outline colour reads as a wall
                 * between the columns.
                 */
                Box(
                    modifier = Modifier
                        .width(Density.COLUMN_HANDLE_WIDTH)
                        .fillMaxHeight()
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                        .pointerInput(column.id) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                /*
                                 * The drag is in pixels, the stored width is in dp, and
                                 * the stored value survives a restart. Mixing the two
                                 * doubles a column on its first touch on a HiDPI screen
                                 * and writes that into preferences.xml.
                                 */
                                val deltaDp = with(density) { dragAmount.toDp().value }
                                val current = state.widthOf(column.id)
                                    ?: (DEFAULT_COLUMN_WIDTH * column.weight).value.toDouble()

                                state.resizeColumn(column.id, current + deltaDp)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(Density.HAIRLINE)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline),
                    )
                }
            }
        }

        HorizontalDivider(thickness = Density.HAIRLINE)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        LazyColumn(
            state = vertical,
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontal)
                .padding(end = Density.SCROLLBAR_GUTTER),
        ) {
            itemsIndexed(state.visibleRows, key = { _, row -> state.keyOf(row) }) { index, row ->
                val selected = state.isSelected(row)
                val accent = rowStyle?.invoke(row)
                val isEven = index % 2 == 0
                val _forceRedraw = state.contentRevision

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(
                                when {
                                    selected -> MaterialTheme.colorScheme.secondaryContainer
                                    accent?.background != null -> accent.background
                                    isEven -> Color.Transparent
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .clickable {
                                state.select(row)
                                onRowClick?.invoke(row)
                            }
                            .heightIn(min = Density.ROW_MIN_HEIGHT),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        state.orderedColumns.forEach { column ->
                            val value = column.value(row)
                            val cell = column.accent?.invoke(row, value)

                            Box(
                                modifier = Modifier
                                    .columnWidth(state, column, if (column.id == lastColumnId) slack else 0.dp)
                                    .fillMaxHeight()
                                    .background(cell?.background ?: Color.Transparent)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                TooltipArea(
                                    tooltip = {
                                        val hint = column.tooltip?.invoke(row, value) ?: value

                                        if (hint.isNotBlank()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                tonalElevation = 2.dp,
                                            ) {
                                                Text(
                                                    hint,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(4.dp),
                                                )
                                            }
                                        }
                                    },
                                ) {
                                    Text(
                                        value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = cell?.foreground
                                            ?: accent?.foreground
                                            ?: MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(Density.COLUMN_HANDLE_WIDTH)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(Density.HAIRLINE)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = Density.HAIRLINE,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(vertical),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                style = LocalScrollbarStyle.current.copy(
                    unhoverColor = MaterialTheme.colorScheme.outline,
                    hoverColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }

        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(horizontal),
            modifier = Modifier.fillMaxWidth(),
            style = LocalScrollbarStyle.current.copy(
                unhoverColor = MaterialTheme.colorScheme.outline,
                hoverColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
    }
    }

}


/**
 * A stored width when the operator has sized this column, otherwise the column's own
 * weight. The stored value wins because it is the operator's decision.
 *
 * @param extra added to the last column so the table fills the window instead of
 *        leaving a band of empty background to the right. The JavaFX table did the
 *        same through `flexibleInitialWidth`.
 */
@Composable
private fun <T> Modifier.columnWidth(
    state: DataTableState<T>,
    column: DataColumn<T>,
    extra: Dp = 0.dp,
): Modifier {
    val stored = state.widthOf(column.id)
    val own = if (stored != null) stored.dp else DEFAULT_COLUMN_WIDTH * column.weight

    return this.width(own + extra)
}

/**
 * How much is left over once every column has its width. Zero when the columns are
 * already wider than the window, in which case the table scrolls.
 */
@Composable
private fun <T> unusedWidth(state: DataTableState<T>, available: Dp): Dp {
    val used = state.orderedColumns.sumOf { column ->
        val stored = state.widthOf(column.id)
        val own = if (stored != null) stored else (DEFAULT_COLUMN_WIDTH * column.weight).value.toDouble()
        own + Density.COLUMN_HANDLE_WIDTH.value
    }

    return (available.value - used).coerceAtLeast(0.0).dp
}

private val DEFAULT_COLUMN_WIDTH = 85.dp
