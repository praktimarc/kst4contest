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
import java.awt.EventQueue
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
 * The question about edits not applied yet, asked when the window closes, the profile is
 * switched or the client exits.
 *
 * The decision is taken in [settle], on the calling event thread, not in the window's next
 * frame: a minimised window may draw no frames, and an exit from the main window would then
 * wait for the settings window to be restored. Only the dialog itself needs a frame, and the
 * window is raised before it is needed. Compose state, so the open window shows the question.
 */
class SettingsQuestion {

    /** What a shown question goes on with. */
    class Asked(
        /** Run after Apply or Discard; null means close the window, which is what the X wants. */
        val then: (() -> Unit)?,
        /**
         * Why Apply cannot be offered: a field holds text it refuses. Discard and Back still
         * work, so the operator is never trapped behind a field he only wants to get rid of.
         */
        val refusal: String?,
    )

    /** The question being shown, or null. */
    var asking: Asked? by mutableStateOf(null)
        private set

    /** Counts the closes asked for with nothing to settle; the window closes on each. */
    var closeRequests by mutableStateOf(0)
        private set

    /**
     * Settles the pending edits of [draft]: runs [nothingPending] at once when nothing waits
     * and no field refuses what it holds, and shows the question otherwise.
     *
     * While a question is shown a further request is ignored. Replacing its continuation would
     * turn an exit the operator is being asked about into a mere close of the window -- or the
     * other way round.
     *
     * @return true when a question is shown now
     */
    fun settle(
        draft: SettingsDraft,
        notices: SettingsNotices,
        then: (() -> Unit)?,
        nothingPending: () -> Unit,
    ): Boolean {
        if (asking != null) return true

        val refusals = draft.commitTypedEntries()
        // A refusal a field reported on its own focus loss is standing as the notice.
        val standing = notices.current as? SettingsNotice.Problem
        val refusal = refusals.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: standing?.message

        if (refusal == null && !draft.hasPendingChanges) {
            nothingPending()
            return false
        }

        // The reason moves into the question, so two dialogs do not stand on top of each other.
        if (standing != null) notices.dismiss()
        asking = Asked(then, refusal)
        return true
    }

    /** The X of the window: close when nothing waits, ask otherwise. */
    fun settleClose(draft: SettingsDraft, notices: SettingsNotices) {
        settle(draft, notices, then = null, nothingPending = { closeRequests++ })
    }

    /** The question was answered; called before acting on the answer. */
    fun answered() {
        asking = null
    }
}

/**
 * Wraps [action] so it runs at most once, however often it is invoked: a continuation of a
 * profile switch or an exit must not run twice because a dialog was clicked twice.
 */
internal fun runOnce(action: Runnable): Runnable {
    val ran = java.util.concurrent.atomic.AtomicBoolean(false)
    return Runnable { if (ran.compareAndSet(false, true)) action.run() }
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
 * the window, switching the operator profile or leaving the client while edits are waiting
 * first asks whether to apply or discard them, or to go back, which cancels the close, the
 * switch or the exit ([requestClose], [settleThen]). Only [close] -- the teardown that
 * follows -- and a JVM shutdown hook drop them without asking.
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

    /** What the open window was built with, if one is open. */
    private class OpenWindow(
        val draft: SettingsDraft,
        val notices: SettingsNotices,
        val question: SettingsQuestion,
    )

    @Volatile
    private var openWith: OpenWindow? = null

    /**
     * Closes the window and waits for it to be gone; see ComposeWindowHost.close. Edits not
     * applied yet are dropped, so this is the teardown after [settleThen] has asked, not the
     * operator's close; that one goes through [requestClose].
     */
    @JvmStatic
    fun close() = host.close()

    /**
     * Closes the window the way its X does: at once when nothing waits to be applied,
     * otherwise after asking.
     */
    @JvmStatic
    fun requestClose() {
        val open = openWith
        if (open == null || !host.isOpen) return
        if (open.question.settle(open.draft, open.notices, then = null, nothingPending = { host.close() })) {
            host.raise()
        }
    }

    /**
     * Runs [action] once the window's pending edits are settled: at once when the window is
     * closed or nothing waits, otherwise after the operator chose Apply or Discard in the
     * window. Back cancels -- [action] never runs. For the profile switch and the exit, so
     * that neither drops edits the operator has not decided about. While a question is
     * already shown the request is ignored, see [SettingsQuestion.settle].
     *
     * Must be called on the event thread, and never waits there. The decision is taken here
     * and now; with something to ask the window is raised -- de-iconified if minimised, so it
     * draws again -- and the question set as Compose state. The action then runs from the
     * dialog's callback, posted to the event queue, so whatever it opens -- the switch
     * confirmation, the window teardown -- does not run inside the click handler. It runs at
     * most once.
     */
    @JvmStatic
    fun settleThen(action: Runnable) {
        val once = runOnce(action)
        val open = openWith
        if (open == null || !host.isOpen) {
            once.run()
            return
        }
        if (open.question.settle(open.draft, open.notices, then = { EventQueue.invokeLater(once) }, nothingPending = once::run)) {
            host.raise()
        }
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

        val question = SettingsQuestion()
        openWith = OpenWindow(draft, notices, question)

        host.show(
            title = { kst4contest.view.i18n.CurrentStrings.get().settingsWindowTitle },
            darkMode = darkMode,
            baseFontSizeSp = baseFontSizeSp,
            paletteStore = paletteStore,
            languageStore = languageStore,
            widthDp = widthDp,
            heightDp = heightDp,
            onResized = onResized,
            // The X is clicked in a window that is showing, so its frames run.
            onCloseRequest = { question.settleClose(draft, notices) },
        ) { close ->
            SettingsContent(tabs, notices, buttons, close, draft, question)
        }
    }
}

/**
 * @param draft the edits waiting to be applied. Provided to the tabs, so a field still holding
 *        a typed entry can be committed into it before an apply.
 * @param question the question about the pending edits, and the closes asked for without one
 */
@Composable
internal fun SettingsContent(
    tabs: List<SettingsTab>,
    notices: SettingsNotices,
    buttons: @Composable (close: () -> Unit) -> Unit,
    close: () -> Unit,
    draft: SettingsDraft = SettingsDraft(),
    question: SettingsQuestion = SettingsQuestion(),
) {
    var selected by remember { mutableStateOf(0) }
    val closes = question.closeRequests
    LaunchedEffect(closes) {
        if (closes > 0) close()
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

    question.asking?.let { asked ->
        val proceed = asked.then ?: close
        PendingChangesDialog(
            refusal = asked.refusal,
            onApply = {
                question.answered()
                // A refusal or a failure stops here, with the reason shown; nothing goes on.
                if (applyChecked(draft, notices)) proceed()
            },
            onDiscard = {
                question.answered()
                draft.discard()
                proceed()
            },
            onBack = { question.answered() },
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
 * Asks what becomes of the edits not applied yet when the operator closes the window,
 * switches the profile or exits: apply them, discard them, or go back to the window.
 * Dismissing it goes back -- nothing is lost by a stray click beside it.
 *
 * @param refusal set when a field holds text it refuses: shown in the dialog, and Apply is
 *        disabled. Discard and Back stay available, so a field the operator only wants to get
 *        rid of never blocks the close, the switch or the exit.
 */
@Composable
internal fun PendingChangesDialog(
    refusal: String?,
    onApply: () -> Unit,
    onDiscard: () -> Unit,
    onBack: () -> Unit,
) {
    val strings = LocalStrings.current

    AlertDialog(
        onDismissRequest = onBack,
        title = { Text(strings.settingsPendingTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.settingsPendingQuestion)
                // Why Apply is not offered; Discard and Back still are.
                refusal?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = onApply, enabled = refusal == null) { Text(strings.settingsPendingApply) }
        },
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
