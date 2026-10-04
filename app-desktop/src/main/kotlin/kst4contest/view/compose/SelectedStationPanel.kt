package kst4contest.view.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kst4contest.model.Band
import java.util.function.Consumer

/**
 * The panel below the station list, for whichever station is selected.
 *
 * Its rules live in [SelectedStationState] and are tested there; what is here is the
 * drawing. With nothing selected it says so rather than showing dead controls — the
 * JavaFX pane disabled them, which looks the same as broken.
 *
 * @param openInBrowser opens a lookup address; reaching the browser is the host
 *        application's business
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedStationPanel(
    state: SelectedStationState,
    messages: DataTableState<kst4contest.model.ChatMessage>,
    /** The bands this station is set up for; BandOpportunityResolver decides them. */
    skedBands: List<Band>,
    /** My own station's enabled bands; used for the "Tag not QRV" checkboxes. */
    activeBands: Set<Band>,
    openInBrowser: Consumer<String>,
    onShowOnMap: () -> Unit,
    onShowPathInAirScout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val member = state.selected

    Column(
        modifier = modifier.fillMaxWidth().padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (member == null) {
            Text(
                "No station selected.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            return@Column
        }

        Text(
            member.callSign.orEmpty() + "  " + member.qra.orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            val category = member.chatCategory?.let { it.getChatCategoryName(it.categoryNumber) }
            if (category != null) {
                Text(category, style = MaterialTheme.typography.bodySmall)
            }
            if (member.getQTFdirection() != null) {
                Text("QTF: ${member.getQTFdirection()}°", style = MaterialTheme.typography.bodySmall)
            }
            if (member.qrb != null) {
                Text("QRB: ${member.qrb} km", style = MaterialTheme.typography.bodySmall)
            }

            val lastActSec = member.activityTimeLastInEpoch
            if (lastActSec > 0L) {
                val actStr = MessageFormats.clockTime(lastActSec.toString())
                val minAgo = kst4contest.controller.Utils4KST.time_getSecondsBetweenEpochAndNow(lastActSec.toString()) / 60
                Text("Last activity: $actStr ($minAgo min ago)", style = MaterialTheme.typography.bodySmall)
            }

            val detectedBands = state.detectedBands()
            if (detectedBands.isNotBlank()) {
                Text(detectedBands, style = MaterialTheme.typography.bodySmall)
            }
        }

        Form.section("Show messages") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP)) {
                SelectedMessageFilter.entries.forEach { filter ->
                    RadioChoice(
                        label = filter.label,
                        selected = state.messageFilter == filter,
                        onSelect = { state.messageFilter = filter },
                    )
                }
            }
        }

        DataTableView(
            state = messages,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )

        Form.section("Tag not QRV") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP)) {
                val activeNotQrvBands = NotQrvBand.entries.filter { activeBands.contains(it.band) }
                
                activeNotQrvBands.forEach { band ->
                    Form.check(band.label, state.isTaggedNotQrv(band)) {
                        state.tagNotQrv(band, it)
                    }
                }

                Form.check("all", activeNotQrvBands.all { state.isTaggedNotQrv(it) }) {
                    state.tagNotQrvAll(it)
                }
            }
        }

        Form.section("Sked") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(Density.FIELD_GAP)
            ) {
                Form.inlineChoice(
                    label = "Sked in (min)",
                    items = SelectedStationState.SKED_MINUTES,
                    selected = state.skedMinutes,
                    describe = { it.toString() },
                    onSelect = { state.skedMinutes = it },
                )
                Form.inlineChoice(
                    label = "Band",
                    items = skedBands,
                    selected = state.skedBand,
                    describe = { it.prefix + " MHz" },
                    onSelect = { state.skedBand = it },
                )
                Form.inlineChoice(
                    label = "Mode",
                    items = SelectedStationState.SKED_MODES,
                    selected = state.skedMode,
                    describe = { it },
                    onSelect = { state.skedMode = it },
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Form.check("Remind-PM in", state.remindPm) { state.remindPm = it }
                Form.inlineChoice(
                    label = "Reminder offsets",
                    items = SelectedStationState.REMINDER_OFFSETS,
                    selected = state.reminderOffsets,
                    describe = { it },
                    enabled = state.remindPm,
                    onSelect = { state.reminderOffsets = it },
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP)) {
                Form.button("Create sked", enabled = state.canCreateSked) { state.createSked() }
                Form.button("Sked fail", enabled = state.canCreateSked) { state.markSkedFail() }
                Form.button("Reset fail", enabled = state.canCreateSked) { state.resetSkedFail() }
            }
        }

        Form.section("Look up and show") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP)) {
                Form.button("Show path in AS", onClick = onShowPathInAirScout)
                Form.button("🗺 Show on map", onClick = onShowOnMap)
                Form.button("Lookup on qrz.com", enabled = state.canLookUp) {
                    openInBrowser.accept(state.qrzComUrl())
                }
                Form.button("Lookup on qrzcq.com", enabled = state.canLookUp) {
                    openInBrowser.accept(state.qrzCqUrl())
                }
            }
        }


    }
}

/**
 * The two priority buttons beside the station list: a shortcut to the station the
 * score says to work next.
 */
@Composable
fun TopPriorityBar(
    state: TopPriorityState,
    onShowMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Priority:", style = MaterialTheme.typography.bodySmall)

        state.entries.forEach { entry ->
            Form.button(entry.label, onClick = entry.select)
        }

        Form.button("more", onClick = onShowMore)
    }
}


/**
 * One of a set of mutually exclusive choices.
 *
 * Material's RadioButton carries the same 48dp minimum that made every field too tall on
 * the desktop, so the touch target is released the way the other compact controls do it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadioChoice(label: String, selected: Boolean, onSelect: () -> Unit) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Row(
            modifier = Modifier
                .selectable(selected = selected, onClick = onSelect)
                .padding(end = Density.BUTTON_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect,
                modifier = Modifier.size(Density.CHECK_SIZE),
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
