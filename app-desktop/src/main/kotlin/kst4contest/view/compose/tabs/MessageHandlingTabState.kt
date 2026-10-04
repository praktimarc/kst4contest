package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences

/**
 * State of the message-handling tab.
 *
 * The JavaFX window offered one checkbox and one text field for the automatic
 * answer and wrote both chat categories from each of them
 * (Kst4ContestApplication:12004 and :12039). Exposing one property per pair keeps
 * that: a tab that wrote only the main category would leave the second chat on its
 * previous value, which no part of the window ever displayed.
 */
class MessageHandlingTabState(private val prefs: ChatPreferences) {

    /** Drives both category flags, as the single JavaFX checkbox did. */
    var autoAnswerEnabled: Boolean
        get() = prefs.isMessageHandling_autoAnswerEnabled()
        set(value) {
            prefs.setMessageHandling_autoAnswerEnabled(value)
            prefs.setMessageHandling_autoAnswerEnabledSecondCat(value)
        }

    /** Drives both category texts, as the single JavaFX text field did. */
    var autoAnswerText: String
        get() = prefs.getMessageHandling_autoAnswerTextMainCat()
        set(value) {
            prefs.setMessageHandling_autoAnswerTextMainCat(value)
            prefs.setMessageHandling_autoAnswerTextSecondCat(value)
        }

    /** Independent of the automatic answer; its own checkbox in the JavaFX window. */
    var autoAnswerToQRGRequestEnabled: Boolean
        get() = prefs.isMessageHandling_autoAnswerToQRGRequestEnabled()
        set(value) { prefs.setMessageHandling_autoAnswerToQRGRequestEnabled(value) }
}
