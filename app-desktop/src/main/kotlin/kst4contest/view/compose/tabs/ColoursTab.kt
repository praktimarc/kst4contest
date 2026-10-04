package kst4contest.view.compose.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.remember
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kst4contest.view.compose.Density
import kst4contest.view.compose.Form
import kst4contest.view.compose.JavaFxPalette
import kst4contest.view.compose.JavaFxStylesheet
import kst4contest.view.compose.PaletteSource

/**
 * The colours of the lifeline, taken from one palette and nothing else.
 *
 * A value type rather than three expressions inside the composable, so the one property that
 * matters — that these come from the shipped sheet — is testable without rendering.
 */
data class LifelineColours(val container: Color, val content: Color, val border: Color)

/**
 * The lifeline's colours from a palette.
 *
 * @param palette must be the shipped palette; passing the resolved one is the fault this
 *        whole arrangement exists to make impossible
 * @return container, content and border
 */
fun lifelineColours(palette: JavaFxPalette): LifelineColours = LifelineColours(
    container = palette.base,
    content = palette.labelTextFill,
    /* A step away from the container, so it reads as a control and not as a patch of colour. */
    border = JavaFxStylesheet.derive(palette.base, -35.0),
)

/**
 * The colours tab: six roles, where each of them comes from, and three ways back.
 *
 * @param state the tab's own state over the palette store
 * @param reportRefusal shows a refused colour, the same channel the notification, shortcut
 *        and beacon tabs use for theirs
 * @param reportOutcome shows something that went right; a success told through the refusal
 *        channel arrives looking like a failure
 */
@Composable
fun ColoursTab(
    state: ColoursTabState,
    reportRefusal: (String) -> Unit,
    reportOutcome: (String) -> Unit,
) {

    /*
     * The palette in force when the tab is opened, which is what "back to how it was" goes
     * to. Remembered on first composition because there is no "not yet applied" state to
     * go back to -- a change is live the moment it is typed.
     */
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        state.rememberCurrentState()
        /*
         * Focus the tab itself, so the shortcuts work the moment it opens rather than only
         * after the operator has clicked something. The one case this is for is the operator
         * who cannot see what to click.
         */
        runCatching { focus.requestFocus() }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        /*
         * onPreviewKeyEvent, so the three ways back are reachable while the focus sits in one
         * of the six hex fields -- which is exactly where it will be when an operator has just
         * made the client unreadable.
         */
        modifier = Modifier
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                ColourShortcuts.handle(event) { action ->
                    when (action) {
                        ColourAction.BACK_TO_PREVIOUS -> state.restoreRemembered()
                        ColourAction.DISCARD_CHANGES -> state.discardChanges()
                        ColourAction.RESET_TO_SHIPPED -> state.resetToShipped()
                    }
                }
            },
    ) {

        Form.section("Colours of this design") {
            state.roles().forEach { row ->
                /*
                 * The provenance sits in the label, beside the field it explains, rather than
                 * in a column of its own: it answers "why is this colour this?", which only
                 * means anything next to the colour.
                 */
                Form.committed(
                    label = "${row.label} (${provenanceOf(row.source)})",
                    stored = row.hex,
                    commit = { typed -> state.set(row.role, typed) },
                    onRefused = reportRefusal,
                )
            }
        }

        val warnings = state.warnings()
        if (warnings.isNotEmpty()) {
            Form.section("Readability") {
                warnings.forEach { warning ->
                    Text(
                        /*
                         * Both numbers, because the message is comparative: the guard reports
                         * that a pair got worse than it shipped, not that it fails a standard.
                         */
                        text = "%s now reads at %.1f to 1, where this design ships %.1f to 1."
                            .format(warning.what, warning.ratio, warning.shippedRatio),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    "The colours are kept either way — a hard combination can be deliberate.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Form.section("Keeping these colours") {
            /*
             * The tab with the most visible immediate effect is also the one where "it looked
             * right, so it must be stored" is the easiest wrong conclusion to draw. Every
             * other settings tab has the same contract; none of them changes four windows
             * under the operator's hands while they decide.
             */
            Text(
                "A colour applies at once, everywhere. It survives a restart only once "
                    + "Save settings has been pressed.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Form.section("Ways back") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Form.button(
                    "Back to how it was (${ColourShortcuts.labelFor(ColourAction.BACK_TO_PREVIOUS)})"
                ) { state.restoreRemembered() }
                Form.button(
                    "Discard my changes (${ColourShortcuts.labelFor(ColourAction.DISCARD_CHANGES)})"
                ) { state.discardChanges() }
            }

            /*
             * Always drawn in the shipped colours, and deliberately out of place under a
             * customised palette: an operator who sets text and surface to the same black
             * cannot see any other button, including the ones that would undo it. Not fitting
             * in is what makes it findable.
             */
            ResetToShippedButton(state.shipped()) { state.resetToShipped() }
        }

        Form.section("Editing the stylesheet by hand") {
            Text(
                if (state.stylesheetExists()) {
                    "This design reads ${state.stylesheetPath()}. A colour set above "
                        .plus("overrides what the file says for that role.")
                } else {
                    "This design has no stylesheet of its own. One can be written to "
                        .plus("${state.stylesheetPath()} as a starting point.")
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Form.button("Write this design's stylesheet into my profile") {
                /*
                 * An existing file is never replaced -- it is the operator's own work, and a
                 * misclick here would be the one loss this tab could cause that cannot be
                 * undone from inside the application.
                 */
                reportExport(state.exportStylesheet(), reportOutcome, reportRefusal)
            }
        }
    }
}

/** Says what the export did, through the channel that matches the outcome. */
private fun reportExport(
    outcome: StylesheetExportOutcome,
    reportOutcome: (String) -> Unit,
    reportRefusal: (String) -> Unit,
) = when (outcome) {
    is StylesheetExportOutcome.Written -> reportOutcome(
        "Written to ${outcome.path}. Edit it and restart to see the result."
    )

    /* Not a failure: the file is the operator's own, and leaving it alone is the point. */
    is StylesheetExportOutcome.AlreadyThere -> reportOutcome(
        "${outcome.path} is already there and was left untouched."
    )

    is StylesheetExportOutcome.Failed -> reportRefusal(
        "Could not write the stylesheet: ${outcome.reason}"
    )
}

/**
 * The rescue control.
 *
 * Takes the shipped palette as a required parameter and reads no composition local, so there
 * is no path by which it could be drawn in the operator's own colours.
 */
@Composable
private fun ResetToShippedButton(shipped: JavaFxPalette, onClick: () -> Unit) {
    val colours = lifelineColours(shipped)

    Button(
        onClick = onClick,
        shape = Density.BUTTON_SHAPE,
        contentPadding = Density.BUTTON_CONTENT_PADDING,
        border = BorderStroke(Density.SELECTION_BORDER, colours.border),
        colors = ButtonDefaults.buttonColors(
            containerColor = colours.container,
            contentColor = colours.content,
        ),
        modifier = Modifier.padding(top = 6.dp),
    ) {
        Text(
            "Reset to the shipped colours "
                + "(${ColourShortcuts.labelFor(ColourAction.RESET_TO_SHIPPED)})",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Why a role is the colour it is — the spec's mandatory answer to "why is this colour this?" */
private fun provenanceOf(source: PaletteSource): String = when (source) {
    PaletteSource.SHIPPED -> "shipped"
    PaletteSource.FILE -> "from file"
    PaletteSource.CHANGED -> "changed"
}
