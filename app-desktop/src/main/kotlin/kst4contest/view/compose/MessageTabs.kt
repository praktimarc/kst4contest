package kst4contest.view.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kst4contest.model.ChatMessage
import kst4contest.model.ClusterMessage

/**
 * The three tabs below the station list.
 *
 * Two of them are the monitor window's tables under their own layout ids — the same
 * columns, separate stored widths, which is what PROJECT_CONTEXT requires. Only the
 * public message tab is its own.
 */
@Composable
fun MessageTabs(
    publicMessages: DataTableState<ChatMessage>,
    clusterMessages: DataTableState<ClusterMessage>,
    qsoOfTheOther: DataTableState<ChatMessage>,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(0) }

    val titles = listOf("Public messages", "DXCluster messages", "QSO of the other")

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
            titles.forEachIndexed { index, title ->
                val isSelected = index == selected
                val underline = MaterialTheme.colorScheme.primary

                Box(
                    modifier = Modifier
                        .heightIn(min = Density.TAB_MIN_HEIGHT)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent
                        )
                        .clickable { selected = index }
                        .drawBehind {
                            if (isSelected) {
                                val thickness = 2.dp.toPx()
                                drawRect(
                                    color = underline,
                                    topLeft = Offset(0f, size.height - thickness),
                                    size = Size(size.width, thickness),
                                )
                            }
                        }
                        .padding(horizontal = Density.TAB_HORIZONTAL_PADDING, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
            }
        }

        HorizontalDivider(thickness = Density.HAIRLINE)

        Box(modifier = Modifier.fillMaxSize()) {
            when (selected) {
                0 -> DataTableView(publicMessages, modifier = Modifier.fillMaxSize())
                1 -> DataTableView(clusterMessages, modifier = Modifier.fillMaxSize())
                else -> DataTableView(qsoOfTheOther, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
