package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import kst4contest.view.i18n.LanguageStore

/**
 * The GUI tab as a typed facade over ChatPreferences.
 *
 * Writes through immediately and has no apply(), like every settings tab: the
 * JavaFX controls wrote as they changed and "Save settings" only persisted.
 */
class GuiOptionsTabState(
    private val prefs: ChatPreferences,
    private val languageStore: LanguageStore,
) {

    var gUI_darkModeActiveByDefault: Boolean
        get() = prefs.isGUI_darkModeActiveByDefault()
        set(value) { prefs.setGUI_darkModeActiveByDefault(value) }

    var guiOptions_defaultFilterNothing: Boolean
        get() = prefs.isGuiOptions_defaultFilterNothing()
        set(value) { prefs.setGuiOptions_defaultFilterNothing(value) }

    var guiOptions_defaultFilterPmToMe: Boolean
        get() = prefs.isGuiOptions_defaultFilterPmToMe()
        set(value) { prefs.setGuiOptions_defaultFilterPmToMe(value) }

    var guiOptions_defaultFilterPmToOther: Boolean
        get() = prefs.isGuiOptions_defaultFilterPmToOther()
        set(value) { prefs.setGuiOptions_defaultFilterPmToOther(value) }

    var guiOptions_defaultFilterPublicMsgs: Boolean
        get() = prefs.isGuiOptions_defaultFilterPublicMsgs()
        set(value) { prefs.setGuiOptions_defaultFilterPublicMsgs(value) }

    var guiOptions_showFreshCallHintInBandColumns: Boolean
        get() = prefs.isGuiOptions_showFreshCallHintInBandColumns()
        set(value) { prefs.setGuiOptions_showFreshCallHintInBandColumns(value) }

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

    var guiOptions_showGrossFieldWorkedHintInBandColumns: Boolean
        get() = prefs.isGuiOptions_showGrossFieldWorkedHintInBandColumns()
        set(value) { prefs.setGuiOptions_showGrossFieldWorkedHintInBandColumns(value) }

}
