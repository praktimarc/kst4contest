package kst4contest.view.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.model.ChatMessage
import kst4contest.model.ClusterMessage

/**
 * The cluster and QSO monitor.
 *
 * Two live tables, one above the other. This is the first window in which the table
 * carries data that changes while the operator watches, which is why the row identity
 * rules in DataTableState matter here and not in the settings window: in Etappe 2 the
 * visible defects all came from that coupling, not from the table itself.
 */
object MonitorWindow {

    private val host = ComposeWindowHost("monitor-window")

    @JvmStatic
    val isOpen: Boolean
        get() = host.isOpen

    @JvmStatic
    fun applyDarkMode(darkMode: Boolean) = host.applyDarkMode(darkMode)

    @JvmStatic
    fun close() = host.close()

    /**
     * @param clusterTable fed from `getLst_clusterMemberList`
     * @param qsoTable fed from `toOtherMessages` over the global chat message list
     */
    @JvmStatic
    fun show(
        clusterTable: DataTableState<ClusterMessage>,
        qsoTable: DataTableState<ChatMessage>,
        darkMode: Boolean,
        baseFontSizeSp: Float,
        /**
         * The palette of the active profile, handed to the theme. Null draws the shipped
         * palette.
         */
        paletteStore: PaletteStore? = null,
        widthDp: Float,
        heightDp: Float,
        onResized: (Float, Float) -> Unit,
    ) {
        host.show(
            title = "Cluster & QSO of the other",
            darkMode = darkMode,
            baseFontSizeSp = baseFontSizeSp,
            paletteStore = paletteStore,
            widthDp = widthDp,
            heightDp = heightDp,
            onResized = onResized,
        ) { _ ->
            MonitorContent(clusterTable, qsoTable)
        }
    }
}

@Composable
private fun MonitorContent(
    clusterTable: DataTableState<ClusterMessage>,
    qsoTable: DataTableState<ChatMessage>,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            Text("DX cluster", style = MaterialTheme.typography.titleSmall)

            /*
             * Half each, fixed. The JavaFX window used a SplitPane whose divider the
             * operator could drag, and that position was stored; reproducing the
             * gesture belongs with the station list of Etappe 5, where a split pane is
             * unavoidable. Until then an even split is honest rather than a broken
             * handle.
             */
            DataTableView(clusterTable, modifier = Modifier.weight(1f).fillMaxWidth())

            HorizontalDivider(thickness = Density.HAIRLINE)

            Text(
                "QSO of the other",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp),
            )

            DataTableView(qsoTable, modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
}
