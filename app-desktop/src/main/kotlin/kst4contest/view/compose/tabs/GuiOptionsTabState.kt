package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import kst4contest.view.compose.SettingsDraft
import kst4contest.view.compose.setting
import kst4contest.view.i18n.LanguageStore

/**
 * The GUI tab as a typed facade over ChatPreferences.
 *
 * Edits are staged in the window's [SettingsDraft] and take effect when the operator
 * applies them. The language is the exception: it switches at once, because the switch is
 * itself what the operator wants to see.
 */
class GuiOptionsTabState(
    private val prefs: ChatPreferences,
    private val draft: SettingsDraft,
    private val languageStore: LanguageStore,
) {

    var gUI_darkModeActiveByDefault: Boolean by draft.setting({ prefs.isGUI_darkModeActiveByDefault() }) { prefs.setGUI_darkModeActiveByDefault(it) }

    var guiOptions_defaultFilterNothing: Boolean by draft.setting({ prefs.isGuiOptions_defaultFilterNothing() }) { prefs.setGuiOptions_defaultFilterNothing(it) }

    var guiOptions_defaultFilterPmToMe: Boolean by draft.setting({ prefs.isGuiOptions_defaultFilterPmToMe() }) { prefs.setGuiOptions_defaultFilterPmToMe(it) }

    var guiOptions_defaultFilterPmToOther: Boolean by draft.setting({ prefs.isGuiOptions_defaultFilterPmToOther() }) { prefs.setGuiOptions_defaultFilterPmToOther(it) }

    var guiOptions_defaultFilterPublicMsgs: Boolean by draft.setting({ prefs.isGuiOptions_defaultFilterPublicMsgs() }) { prefs.setGuiOptions_defaultFilterPublicMsgs(it) }

    var guiOptions_showFreshCallHintInBandColumns: Boolean by draft.setting({ prefs.isGuiOptions_showFreshCallHintInBandColumns() }) { prefs.setGuiOptions_showFreshCallHintInBandColumns(it) }

    /**
     * The interface language, as the picker offers it: a language code, or the empty string
     * for the system language.
     *
     * Read and written through the store, both ways. The store writes the preference itself;
     * reading the preference here instead would give one value two sources, and they disagree
     * the moment a caller hands the state a store wired to anything else.
     */
    var language: String
        get() = languageStore.stored
        set(value) { languageStore.use(value) }

    var guiOptions_rememberWindowPositions: Boolean by draft.setting({ prefs.isGuiOptions_rememberWindowPositions() }) { prefs.setGuiOptions_rememberWindowPositions(it) }

    var guiOptions_showGrossFieldWorkedHintInBandColumns: Boolean by draft.setting({ prefs.isGuiOptions_showGrossFieldWorkedHintInBandColumns() }) { prefs.setGuiOptions_showGrossFieldWorkedHintInBandColumns(it) }

}
