package kst4contest.view.compose.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.view.compose.Form
import kst4contest.view.i18n.Translations
import kst4contest.view.i18n.SYSTEM_LANGUAGE
import kst4contest.view.i18n.LocalStrings

/**
 * The GUI tab: startup design, the startup message filters and the band-column hints.
 *
 * The four startup filters stay four independent checkboxes. The JavaFX tab offered
 * them that way, and turning them into a choice of one would take a combination away
 * that an operator may have configured.
 */
@Composable
fun GuiOptionsTab(state: GuiOptionsTabState) {
    val strings = LocalStrings.current

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Form.section(strings.guiStartupDesign) {
            Form.check("Start in the evening design", state.gUI_darkModeActiveByDefault) {
                state.gUI_darkModeActiveByDefault = it
            }
        }

        Form.flowingSection(strings.guiStartupFilters) {
            Form.check("Public messages", state.guiOptions_defaultFilterPublicMsgs) {
                state.guiOptions_defaultFilterPublicMsgs = it
            }
            Form.check("Private messages to me", state.guiOptions_defaultFilterPmToMe) {
                state.guiOptions_defaultFilterPmToMe = it
            }
            Form.check("Private messages to others", state.guiOptions_defaultFilterPmToOther) {
                state.guiOptions_defaultFilterPmToOther = it
            }
            Form.check("No filter", state.guiOptions_defaultFilterNothing) {
                state.guiOptions_defaultFilterNothing = it
            }
        }

        Form.flowingSection(strings.guiBandColumnHints) {
            Form.check("Mark a callsign seen for the first time", state.guiOptions_showFreshCallHintInBandColumns) {
                state.guiOptions_showFreshCallHintInBandColumns = it
            }
            Form.check("Mark a worked large grid field", state.guiOptions_showGrossFieldWorkedHintInBandColumns) {
                state.guiOptions_showGrossFieldWorkedHintInBandColumns = it
            }
        }

        /*
         * Appended last so no established section position moves, the same reason the colours
         * tab was appended after Profiles. The label and the "system" entry come from the
         * texts, so the picker is itself in the language it switches.
         */
        Form.section(strings.guiLanguage) {
            Form.choice(
                label = strings.guiLanguage,
                items = listOf(SYSTEM_LANGUAGE) + Translations.BY_LANGUAGE.keys.sorted(),
                selected = state.language,
                describe = { code ->
                    /*
                     * The code in capitals rather than a name for the language: a display name
                     * per language would be another key every contributor has to maintain, and
                     * DE/EN is unambiguous for two. A longer list is where a name earns itself.
                     */
                    if (code == SYSTEM_LANGUAGE) strings.guiLanguageSystem else code.uppercase()
                },
                onSelect = { state.language = it },
            )
        }

    }
}
