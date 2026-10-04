package kst4contest.view.compose.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import kst4contest.view.compose.Density
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kst4contest.model.OperatorProfile
import kst4contest.view.compose.Form

/**
 * The profiles tab.
 *
 * Every action that cannot be undone asks first, and the question names what will be
 * removed — the JavaFX pane did that for the deletion and it is the one action here
 * that destroys data.
 */
@Composable
fun ProfilesTab(state: ProfilesTabState) {
    var notice by remember { mutableStateOf<ProfileNotice?>(null) }
    var dialog by remember { mutableStateOf<ProfileDialog?>(null) }

    fun act(action: () -> ProfileNotice?) {
        notice = action()
    }

    Column(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Form.labelledRow("Active profile:") { Text(state.activeProfileName) }
            Form.labelledRow("Settings file:") { Text(state.preferencesPath) }
            Form.labelledRow("Worked stations:") { Text(state.workedDatabasePath) }
        }

        ProfileList(state, state::select)

        Row(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP)) {
            Form.button("New profile...") { dialog = ProfileDialog.Create }
            Form.button("Duplicate...") {
                dialog = ProfileDialog.Duplicate(state.duplicateNameSuggestion())
            }
            Form.button("Rename...") {
                state.selected?.let { dialog = ProfileDialog.Rename(it.displayName) }
            }
            Form.button("Delete...") {
                val profile = state.selected
                if (profile == null) {
                    act { state.deleteSelected() }
                } else {
                    dialog = ProfileDialog.ConfirmDelete(profile, state.describeDeletion(profile))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP)) {
            Form.button("Change worked stations...") {
                val profile = state.selected
                if (profile == null || !state.canChangeWorkedDataMode(profile)) {
                    act { state.changeWorkedDataModeOfSelected(false) }
                } else {
                    dialog = ProfileDialog.WorkedData(profile, profile.isSharedWorkedDatabase)
                }
            }

            Form.button("Switch to selected profile...") { act { state.activateSelected() } }
        }

        Text(
            "Each profile has its own settings and window layout. A profile can either share " +
                "the common station worked stations, which is what a multi operator station " +
                "wants, or keep its own. Duplicating a profile copies everything except " +
                "callsign and password, and never copies worked stations.",
            style = MaterialTheme.typography.bodySmall,
        )
    }

    dialog?.let { open ->
        ProfileDialogs(
            dialog = open,
            onDismiss = { dialog = null },
            onCreate = { name, shared -> dialog = null; act { state.createProfile(name, shared) } },
            onDuplicate = { name -> dialog = null; act { state.duplicateSelected(name) } },
            onRename = { name -> dialog = null; act { state.renameSelected(name) } },
            onDelete = { dialog = null; act { state.deleteSelected() } },
            onWorkedData = { shared -> dialog = null; act { state.changeWorkedDataModeOfSelected(shared) } },
        )
    }

    notice?.let { shown ->
        AlertDialog(
            onDismissRequest = { notice = null },
            title = { Text("Operator profiles") },
            text = { Text(shown.message) },
            confirmButton = { TextButton(onClick = { notice = null }) { Text("OK") } },
        )
    }
}

/**
 * The profile table. It observes ProfilesTabState directly: the selection and the list
 * are Compose state, so a row redraws when the selection moves off it.
 */
@Composable
private fun ProfileList(state: ProfilesTabState, onSelect: (OperatorProfile?) -> Unit) {
    if (state.profiles.isEmpty()) {
        Text("No operator profile configured.")
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            HeaderCell("Profile", 0.4f)
            HeaderCell("Worked stations", 0.4f)
            HeaderCell("Last used", 0.2f)
        }

        LazyColumn(modifier = Modifier.height(200.dp).fillMaxWidth()) {
            items(state.profiles, key = { it.profileId }) { profile ->
                val isSelected = profile == state.selected

                Column {
                    /*
                     * The selected row carries a full frame in the accent colour on top
                     * of the tint. Unlike the editable lists there is no field border
                     * here to compete with, so the frame is the clearest marker.
                     */
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        Density.SELECTION_BORDER,
                                        MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onSelect(profile) }
                            .heightIn(min = Density.ROW_MIN_HEIGHT)
                            .padding(horizontal = 4.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            profile.displayName + if (state.isActive(profile)) "  (in use)" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(0.4f),
                        )
                        Text(
                            ProfilesTabState.describeWorkedDataMode(profile),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(0.4f),
                        )
                        Text(
                            ProfilesTabState.describeLastUsed(profile),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(0.2f),
                        )
                    }

                    /* A hairline between rows, so the list reads as a table. */
                    if (!isSelected) {
                        HorizontalDivider(thickness = Density.HAIRLINE)
                    }
                }
            }
        }
    }
}

/** Extension on RowScope because only that scope provides the weight modifier. */
@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    Text(text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(weight))
}

/** The dialogs the profile actions need, so each action asks before it acts. */
private sealed interface ProfileDialog {
    data object Create : ProfileDialog
    data class Duplicate(val suggestion: String) : ProfileDialog
    data class Rename(val current: String) : ProfileDialog
    data class ConfirmDelete(val profile: OperatorProfile, val consequences: String) : ProfileDialog
    data class WorkedData(val profile: OperatorProfile, val shared: Boolean) : ProfileDialog
}

@Composable
private fun ProfileDialogs(
    dialog: ProfileDialog,
    onDismiss: () -> Unit,
    onCreate: (String, Boolean) -> Unit,
    onDuplicate: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onWorkedData: (Boolean) -> Unit,
) {
    when (dialog) {
        ProfileDialog.Create -> {
            var name by remember { mutableStateOf("") }
            var shared by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("New operator profile") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Name and worked stations of the new profile")
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            singleLine = true,
                            label = { Text("Profile name, for example DN9APW") },
                        )
                        WorkedDataChoice(shared) { shared = it }
                    }
                },
                confirmButton = { TextButton(onClick = { onCreate(name, shared) }) { Text("Create profile") } },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
            )
        }

        is ProfileDialog.Duplicate -> {
            var name by remember { mutableStateOf(dialog.suggestion) }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Duplicate operator profile") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Name of the new profile")
                        Text(
                            "Everything is copied except callsign and password. Worked stations " +
                                "are never copied.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            singleLine = true,
                        )
                    }
                },
                confirmButton = { TextButton(onClick = { onDuplicate(name) }) { Text("OK") } },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
            )
        }

        is ProfileDialog.Rename -> {
            var name by remember { mutableStateOf(dialog.current) }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Rename operator profile") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("New name of the profile")
                        Text(
                            "Files and folders of the profile are not touched.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            singleLine = true,
                        )
                    }
                },
                confirmButton = { TextButton(onClick = { onRename(name) }) { Text("OK") } },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
            )
        }

        is ProfileDialog.ConfirmDelete -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete operator profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Delete the profile \"" + dialog.profile.displayName + "\"?")
                    Text(dialog.consequences)
                }
            },
            confirmButton = { TextButton(onClick = onDelete) { Text("Delete profile") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        )

        is ProfileDialog.WorkedData -> {
            var shared by remember { mutableStateOf(dialog.shared) }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Worked stations") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Worked stations of \"" + dialog.profile.displayName + "\"")
                        WorkedDataChoice(shared) { shared = it }
                        Text(
                            "Switching does not move any data. Worked stations already collected " +
                                "under the other setting stay where they are.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                confirmButton = { TextButton(onClick = { onWorkedData(shared) }) { Text("Apply") } },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
            )
        }
    }
}

/** The two mutually exclusive worked-stations options, as the radio buttons were. */
@Composable
private fun WorkedDataChoice(shared: Boolean, onChange: (Boolean) -> Unit) {
    Column {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = !shared,
                    onClick = { onChange(false) },
                    modifier = Modifier.size(Density.CHECK_SIZE),
                )
                Text(
                    "Own worked stations for this profile",
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = shared,
                    onClick = { onChange(true) },
                    modifier = Modifier.size(Density.CHECK_SIZE),
                )
                Text(
                    "Share the common station worked stations (multi operator station)",
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}
