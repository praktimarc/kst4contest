package kst4contest.view.compose.tabs

import kst4contest.view.i18n.LocalStrings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import kst4contest.model.ChatMember
import androidx.compose.ui.Modifier
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kst4contest.view.compose.Form

/**
 * The Workedstn database tab: refresh, and a confirmed reset of the contest flags.
 *
 * The warning text is the one the JavaFX dialog carried, including the note that a
 * reset before every contest is normally unnecessary — these data expire on their own.
 */
@Composable
fun WorkedDatabaseTab(state: WorkedDatabaseTabState) {
    val strings = LocalStrings.current

    var revision by remember { mutableStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        @Suppress("UNUSED_EXPRESSION") revision

        Form.section(strings.workedSection) {
            Text(
                strings.workedExpiryHint
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Form.button(strings.workedRefresh) { state.refresh(); revision++ }
                Form.button(strings.workedReset) {
                    state.requestReset(); revision++
                }
            }
            state.lastAffectedLines?.let { Text(strings.workedLastResetAffected(it)) }

            WorkedStationsTable(state.workedStations, revision)
        }
    }

    if (state.confirmationPending) {
        AlertDialog(
            onDismissRequest = { state.cancelReset(); revision++ },
            title = { Text(strings.workedResetDialogTitle) },
            text = {
                Text(
                    strings.workedResetDialogBody
                )
            },
            confirmButton = {
                TextButton(onClick = { state.confirmReset(); revision++ }) { Text(strings.workedResetConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { state.cancelReset(); revision++ }) { Text(strings.dialogCancel) }
            },
        )
    }

    if (state.resetFailed) {
        AlertDialog(
            onDismissRequest = { state.dismissError(); revision++ },
            title = { Text(strings.workedResetDialogTitle) },
            text = {
                Text(
                    strings.workedResetFailed
                )
            },
            confirmButton = {
                TextButton(onClick = { state.dismissError(); revision++ }) { Text(strings.dialogOk) }
            },
        )
    }
}

/**
 * The worked-stations table: one row per callsign in the database, a cross for any
 * band worked and one column per band.
 *
 * Read-only, as the JavaFX table was. The band columns are those the JavaFX table had,
 * in its order and under its labels — 23, 13, 9, 6 and 3 are centimetre labels for
 * 1240, 2300, 3400, 5600 and 10 GHz, which is what a microwave operator reads.
 */
@Composable
private fun WorkedStationsTable(rows: List<ChatMember>, revision: Int) {
    val strings = LocalStrings.current

    @Suppress("UNUSED_EXPRESSION") revision

    val bands: List<Pair<String, (ChatMember) -> Boolean>> = listOf(
        "wkd" to { m: ChatMember -> m.isWorked },
        "50" to { m: ChatMember -> m.isWorked50 },
        "70" to { m: ChatMember -> m.isWorked70 },
        "144" to { m: ChatMember -> m.isWorked144 },
        "432" to { m: ChatMember -> m.isWorked432 },
        "23" to { m: ChatMember -> m.isWorked1240 },
        "13" to { m: ChatMember -> m.isWorked2300 },
        "9" to { m: ChatMember -> m.isWorked3400 },
        "6" to { m: ChatMember -> m.isWorked5600 },
        "3" to { m: ChatMember -> m.isWorked10G },
    )

    if (rows.isEmpty()) {
        Text(strings.workedEmpty)
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(strings.stationCallsign, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
            bands.forEach { (label, _) ->
                Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
        }

        LazyColumn(modifier = Modifier.height(260.dp).fillMaxWidth()) {
            /*
             * Keyed by the raw callsign: the database holds one row per callsign, and a
             * positional key would reshuffle every row when the list is re-read.
             */
            items(rows, key = { it.callSignRaw ?: it.callSign ?: "" }) { member ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(member.callSign ?: "", modifier = Modifier.weight(2f))
                    bands.forEach { (_, isWorked) ->
                        Text(
                            if (isWorked(member)) "X" else "",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
