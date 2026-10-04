package kst4contest.view.compose.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.view.compose.Form

/**
 * The GUI tab: startup design, the startup message filters and the band-column hints.
 *
 * The four startup filters stay four independent checkboxes. The JavaFX tab offered
 * them that way, and turning them into a choice of one would take a combination away
 * that an operator may have configured.
 */
@Composable
fun GuiOptionsTab(state: GuiOptionsTabState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Form.section("Startup design") {
            Form.check("Start in the evening design", state.gUI_darkModeActiveByDefault) {
                state.gUI_darkModeActiveByDefault = it
            }
        }

        Form.flowingSection("Message filters active at startup") {
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

        Form.flowingSection("Hints in the band columns") {
            Form.check("Mark a callsign seen for the first time", state.guiOptions_showFreshCallHintInBandColumns) {
                state.guiOptions_showFreshCallHintInBandColumns = it
            }
            Form.check("Mark a worked large grid field", state.guiOptions_showGrossFieldWorkedHintInBandColumns) {
                state.guiOptions_showGrossFieldWorkedHintInBandColumns = it
            }
        }

    }
}
