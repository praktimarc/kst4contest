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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.view.i18n.LocalStrings

/**
 * One tab of the settings window: a title and the content to show.
 *
 * Tabs are handed in rather than listed here so each one can be written and tested on
 * its own, and so the window does not need to know what any of them contains.
 */
class SettingsTab(
    /**
     * Read during composition, not stored as text, so a language change retitles the strip
     * of an already open window. The tabs themselves are built once, outside any
     * composition; without this the titles would only follow on the next time the window
     * was opened.
     */
    val title: () -> String,
    /**
     * Called when the operator selects this tab, before its content is shown. The
     * worked-stations tab used it to re-read the database, which it has to: its list is
     * a snapshot taken when the window was built.
     */
    val onSelected: (() -> Unit)? = null,
    val content: @Composable () -> Unit,
) {

    /** For a title that genuinely never changes -- the tests, and anything not translated. */
    constructor(
        title: String,
        onSelected: (() -> Unit)? = null,
        content: @Composable () -> Unit,
    ) : this({ title }, onSelected, content)
}

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
 * A request to close the settings window, from outside its composition: the title bar's X
 * and the Windows menu. The window decides what to do with it -- close, or ask about the
 * edits not applied yet -- the next time it draws, which keeps the request itself free of
 * any wait on the event thread.
 */
class SettingsCloseRequest {

    /** Counts the requests, so the window can tell a new one from one already handled. */
    var count by mutableStateOf(0)
        private set

    fun request() {
        count++
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
 * The controls stage their edits in a [SettingsDraft]; the button bar applies it. Closing
 * the window while edits are waiting asks whether to apply or discard them. A close from
 * outside -- a profile switch, the client shutting down -- discards them: the preferences
 * they belong to are about to be replaced or dropped.
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

    /** The draft and the close request of the window that is open, if one is. */
    @Volatile
    private var openWith: Pair<SettingsDraft, SettingsCloseRequest>? = null

    /**
     * Closes the window and waits for it to be gone; see ComposeWindowHost.close. Edits not
     * applied yet are discarded. For a close the operator did not ask for -- a profile switch,
     * the shutdown; the operator's own close goes through [requestClose].
     */
    @JvmStatic
    fun close() = host.close()

    /**
     * Closes the window the way its X does: at once when nothing waits to be applied,
     * otherwise after asking. The window is raised first when there is something to ask, so
     * the question is not hidden behind the main window.
     */
    @JvmStatic
    fun requestClose() {
        val (draft, request) = openWith ?: return
        if (!host.isOpen) return
        if (draft.hasPendingChanges) host.raise()
        request.request()
    }

    /**
     * @param draft the edits of this opening of the window; the tabs and the buttons were
     *        built with the same one
     * @param buttons the bottom button row; it receives the action that closes the
     *        window, because only the window itself can end its Compose application
     */
    @JvmStatic
    fun show(
        tabs: List<SettingsTab>,
        notices: SettingsNotices,
        draft: SettingsDraft,
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
        widthDp: Float,
        heightDp: Float,
        onResized: (Float, Float) -> Unit,
        buttons: @Composable (close: () -> Unit) -> Unit,
    ) {
        if (host.isOpen) {
            // Raises the open window; the new draft belongs to no window and is dropped.
            host.raise()
            return
        }

        val closeRequest = SettingsCloseRequest()
        openWith = draft to closeRequest

        host.show(
            title = { kst4contest.view.i18n.CurrentStrings.get().settingsWindowTitle },
            darkMode = darkMode,
            baseFontSizeSp = baseFontSizeSp,
            paletteStore = paletteStore,
            languageStore = languageStore,
            widthDp = widthDp,
            heightDp = heightDp,
            onResized = onResized,
            onCloseRequest = closeRequest::request,
        ) { close ->
            SettingsContent(tabs, notices, buttons, close, draft, closeRequest)
        }
    }
}

/**
 * @param draft the edits waiting to be applied. Provided to the tabs, so a field still holding
 *        a typed entry can be committed into it before an apply.
 * @param closeRequest the operator's requests to close the window, from its X or the menu
 */
@Composable
internal fun SettingsContent(
    tabs: List<SettingsTab>,
    notices: SettingsNotices,
    buttons: @Composable (close: () -> Unit) -> Unit,
    close: () -> Unit,
    draft: SettingsDraft = SettingsDraft(),
    closeRequest: SettingsCloseRequest = SettingsCloseRequest(),
) {
    var selected by remember { mutableStateOf(0) }
    var askingAboutPending by remember { mutableStateOf(false) }

    /*
     * A close request is handled here, where the close action and the notices are. Fields
     * still holding a typed entry are committed first, so an entry typed just before the X
     * counts as pending. One the field refuses leaves the window open with the reason shown.
     */
    val requests = closeRequest.count
    LaunchedEffect(requests) {
        if (requests == 0) return@LaunchedEffect

        val before = notices.current
        draft.commitTypedEntries()
        val now = notices.current
        when {
            now is SettingsNotice.Problem && now !== before -> Unit
            draft.hasPendingChanges -> askingAboutPending = true
            else -> close()
        }
    }

    CompositionLocalProvider(LocalSettingsDraft provides draft) {
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
    }

    if (askingAboutPending) {
        PendingChangesDialog(
            onApply = {
                askingAboutPending = false
                draft.apply()
                close()
            },
            onDiscard = {
                askingAboutPending = false
                draft.discard()
                close()
            },
            onBack = { askingAboutPending = false },
        )
    }

    notices.current?.let { notice ->
        AlertDialog(
            onDismissRequest = notices::dismiss,
            title = {
                Text(
                    when (notice) {
                        is SettingsNotice.Info -> kst4contest.view.i18n.LocalStrings.current.settingsNoticeInfo
                        is SettingsNotice.Problem -> kst4contest.view.i18n.LocalStrings.current.settingsNoticeProblem
                    }
                )
            },
            text = { Text(notice.message) },
            confirmButton = { TextButton(onClick = notices::dismiss) { Text(kst4contest.view.i18n.LocalStrings.current.dialogOk) } },
        )
    }
}

/**
 * Asks what becomes of the edits not applied yet when the operator closes the window: apply
 * them, discard them, or go back to the window. Dismissing it goes back -- nothing is lost
 * by a stray click beside it.
 */
@Composable
internal fun PendingChangesDialog(onApply: () -> Unit, onDiscard: () -> Unit, onBack: () -> Unit) {
    val strings = LocalStrings.current

    AlertDialog(
        onDismissRequest = onBack,
        title = { Text(strings.settingsPendingTitle) },
        text = { Text(strings.settingsPendingQuestion) },
        confirmButton = { TextButton(onClick = onApply) { Text(strings.settingsPendingApply) } },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) { Text(strings.settingsPendingDiscard) }
                TextButton(onClick = onBack) { Text(strings.settingsPendingBack) }
            }
        },
    )
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
internal fun SettingsTabStrip(
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
                        tab.title(),
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
