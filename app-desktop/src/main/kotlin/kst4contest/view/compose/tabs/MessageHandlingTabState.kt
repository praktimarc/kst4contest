package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import kst4contest.view.compose.SettingsDraft
import kst4contest.view.compose.setting

/**
 * State of the message-handling tab.
 *
 * The JavaFX window offered one checkbox and one text field for the automatic
 * answer and wrote both chat categories from each of them
 * (Kst4ContestApplication:12004 and :12039). Exposing one property per pair keeps
 * that: a tab that wrote only the main category would leave the second chat on its
 * previous value, which no part of the window ever displayed.
 *
 * Edits are staged in the window's [SettingsDraft] and take effect when the operator
 * applies them.
 */
class MessageHandlingTabState(
    private val prefs: ChatPreferences,
    private val draft: SettingsDraft,
    private val applyDebugModeToFile: (Boolean) -> Unit = {},
) {

    /** Drives both category flags, as the single JavaFX checkbox did. */
    var autoAnswerEnabled: Boolean by draft.setting(
        { prefs.isMessageHandling_autoAnswerEnabled() },
    ) { enabled ->
        prefs.setMessageHandling_autoAnswerEnabled(enabled)
        prefs.setMessageHandling_autoAnswerEnabledSecondCat(enabled)
    }

    /** Drives both category texts, as the single JavaFX text field did. */
    var autoAnswerText: String by draft.setting(
        { prefs.getMessageHandling_autoAnswerTextMainCat() },
    ) { text ->
        prefs.setMessageHandling_autoAnswerTextMainCat(text)
        prefs.setMessageHandling_autoAnswerTextSecondCat(text)
    }

    /** Independent of the automatic answer; its own checkbox in the JavaFX window. */
    var autoAnswerToQRGRequestEnabled: Boolean by draft.setting(
        { prefs.isMessageHandling_autoAnswerToQRGRequestEnabled() },
    ) { prefs.setMessageHandling_autoAnswerToQRGRequestEnabled(it) }

    /**
     * Records the whole session to file. Beyond the preference it opens or closes the
     * error-log handler and the history recorder, which only the application can sequence;
     * [applyDebugModeToFile] carries that out in the order the JavaFX checkbox kept.
     */
    var debugModeToFileEnabled: Boolean by draft.setting(
        { prefs.isMessageHandling_debugModeToFileEnabled() },
        effect = { applyDebugModeToFile(prefs.isMessageHandling_debugModeToFileEnabled()) },
    ) { prefs.setMessageHandling_debugModeToFileEnabled(it) }
}
