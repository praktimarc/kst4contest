package kst4contest.view.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * One tab of the settings window: a title and the content to show.
 *
 * Tabs are handed in rather than listed here so each one can be written and tested on
 * its own, and so the window does not need to know what any of them contains.
 */
class SettingsTab(
    val title: String,
    /**
     * Called when the operator selects this tab, before its content is shown. The
     * worked-stations tab used it to re-read the database, which it has to: its list is
     * a snapshot taken when the window was built.
     */
    val onSelected: (() -> Unit)? = null,
    val content: @Composable () -> Unit,
)

/** Something the window has to tell the operator. */
sealed interface SettingsNotice {

    val message: String

    /** It happened; this is what the operator should know about it. */
    data class Info(override val message: String) : SettingsNotice

    /** It did not happen, and this is why. */
    data class Problem(override val message: String) : SettingsNotice
}

/**
 * Carries a notice from a tab or a button to the one dialog that shows it.
 *
 * The JavaFX window opened an Alert per field and per button. One reporter keeps that
 * behaviour without every tab having to carry a dialog, and because the window opens
 * at most once there is at most one of these in flight.
 */
class SettingsNotices {

    var current: SettingsNotice? by mutableStateOf(null)
        private set

    fun info(message: String) {
        current = SettingsNotice.Info(message)
    }

    fun problem(message: String) {
        current = SettingsNotice.Problem(message)
    }

    fun dismiss() {
        current = null
    }
}

/**
 * The settings window.
 *
 * Unlike the operator profile picker this window does **not** block: the operator keeps
 * working while it is open, exactly as the JavaFX window behaved. It is also opened at
 * most once — a second instance would let two windows write the same preferences
 * against each other.
 *
 * Saving does not collect values. Every control writes into ChatPreferences as it
 * changes; the button bar only persists, which is what the JavaFX "Save settings"
 * button did.
 */
object SettingsWindow {

    /**
     * Opening, closing, the single instance and the design switch all live in
     * ComposeWindowHost; this object adds only what is specific to the settings
     * window. The shared part was written here first and is now on its third window,
     * and its close path is exactly where the profile switch broke.
     */
    private val host = ComposeWindowHost("settings-window")

    /** Whether the window is currently open, so a toggle can tell. */
    @JvmStatic
    val isOpen: Boolean
        get() = host.isOpen

    /** Follows a design switch made in the main window. */
    @JvmStatic
    fun applyDarkMode(darkMode: Boolean) = host.applyDarkMode(darkMode)

    /** Closes the window and waits for it to be gone; see ComposeWindowHost.close. */
    @JvmStatic
    fun close() = host.close()

    /**
     * @param buttons the bottom button row; it receives the action that closes the
     *        window, because only the window itself can end its Compose application
     */
    @JvmStatic
    fun show(
        tabs: List<SettingsTab>,
        notices: SettingsNotices,
        darkMode: Boolean,
        baseFontSizeSp: Float,
        widthDp: Float,
        heightDp: Float,
        onResized: (Float, Float) -> Unit,
        buttons: @Composable (close: () -> Unit) -> Unit,
    ) {
        host.show(
            title = "Change Client Settings",
            darkMode = darkMode,
            baseFontSizeSp = baseFontSizeSp,
            widthDp = widthDp,
            heightDp = heightDp,
            onResized = onResized,
        ) { close ->
            SettingsContent(tabs, notices, buttons, close)
        }
    }
}

@Composable
private fun SettingsContent(
    tabs: List<SettingsTab>,
    notices: SettingsNotices,
    buttons: @Composable (close: () -> Unit) -> Unit,
    close: () -> Unit,
) {
    var selected by remember { mutableStateOf(0) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsTabStrip(
                tabs = tabs,
                selectedIndex = selected,
                onSelect = { index ->
                    selected = index
                    tabs[index].onSelected?.invoke()
                },
            )

            /*
             * One scroll region for the whole window, with a scrollbar beside it. The
             * tabs used to scroll each for themselves, and none of them could show a
             * bar: a tab does not know the window it sits in, so content simply ended
             * mid-form with nothing to say that more was below.
             */
            val scroll = rememberScrollState()

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(end = Density.SCROLLBAR_GUTTER),
                ) {
                    tabs.getOrNull(selected)?.content?.invoke()
                }

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(scroll),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    style = LocalScrollbarStyle.current.copy(
                        /* Muted on purpose: a bar in the accent colour shouts. */
                        unhoverColor = MaterialTheme.colorScheme.outline,
                        hoverColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }

            buttons(close)
        }
    }

    notices.current?.let { notice ->
        AlertDialog(
            onDismissRequest = notices::dismiss,
            title = {
                Text(
                    when (notice) {
                        is SettingsNotice.Info -> "Info"
                        is SettingsNotice.Problem -> "Settings"
                    }
                )
            },
            text = { Text(notice.message) },
            confirmButton = { TextButton(onClick = notices::dismiss) { Text("OK") } },
        )
    }
}

/**
 * The row of tab buttons.
 *
 * Material's ScrollableTabRow is not usable here: it gives every tab a minimum width
 * of 90dp and a height of 48dp, both private constants. Eleven tabs then need over
 * 990dp before a single letter is measured, so the row overflowed and cut the last
 * title in half.
 *
 * Here a tab is as wide as its own title plus a small padding, which is how the
 * JavaFX TabPane sized them, and the row wraps to a second line when it truly does
 * not fit. Wrapping rather than clipping is deliberate: half a word is the worst of
 * the available outcomes, and a scrolling strip hides that tabs exist at all.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsTabStrip(
    tabs: List<SettingsTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex

                val underline = MaterialTheme.colorScheme.primary

                /*
                 * One box per tab, and the underline is painted inside it rather than
                 * stacked below it. A HorizontalDivider is fillMaxWidth by default, so
                 * putting one under each tab made every tab demand the full row width —
                 * and the row then wrapped after every single tab.
                 */
                Box(
                    modifier = Modifier
                        .heightIn(min = Density.TAB_MIN_HEIGHT)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable { onSelect(index) }
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
                        .padding(horizontal = Density.TAB_HORIZONTAL_PADDING, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        tab.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }

        HorizontalDivider(thickness = Density.HAIRLINE)
    }
}
