package kst4contest.view.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kst4contest.model.OperatorProfile
import java.util.Optional
import java.util.concurrent.CountDownLatch

/**
 * The text the JavaFX dialog showed below a profile name. Kept as a function so
 * the wording and the rule behind it stay covered by a test: the root profile and
 * a sharing profile both use the installation's shared worked database.
 */
internal fun workedDatabaseDescription(profile: OperatorProfile): String =
    if (profile.isRootProfile || profile.isSharedWorkedDatabase) {
        "shared station worked database"
    } else {
        "own worked database"
    }

/**
 * Selection state of the operator profile picker, free of Compose so the rules
 * are testable without a toolkit.
 */
class OperatorProfilePickerState(
    val profiles: List<OperatorProfile>,
    preselectedProfileId: String?,
) {
    var selected: OperatorProfile? =
        profiles.firstOrNull { it.profileId == preselectedProfileId } ?: profiles.firstOrNull()
        private set

    val canConfirm: Boolean
        get() = selected != null

    fun select(profile: OperatorProfile) {
        if (profiles.any { it.profileId == profile.profileId }) {
            selected = profiles.first { it.profileId == profile.profileId }
        }
    }
}

/**
 * Asks the operator which profile to start with.
 *
 * Replaces the JavaFX dialog of the same name and keeps its contract: blocks
 * until the operator decides, returns the chosen profile, or empty when they
 * want to quit. Kst4ContestApplication uses this as a method reference, so the
 * signature must not drift.
 */
object OperatorProfilePickerWindow {

    /**
     * The font size the JavaFX dialog hardcoded. It runs before any profile is
     * loaded, so there is no configured size to read; using a different one here
     * would make the dialog look unlike the windows that follow it.
     */
    private const val DIALOG_FONT_SIZE_SP = 12f

    @JvmStatic
    fun showAndSelect(
        selectableProfiles: List<OperatorProfile>,
        preselectedProfileId: String?,
    ): Optional<OperatorProfile> {
        val state = OperatorProfilePickerState(selectableProfiles, preselectedProfileId)
        var chosen: OperatorProfile? = null
        val closed = CountDownLatch(1)

        /*
         * application { } blocks until every Compose window closes, so it must not
         * run on the JavaFX application thread. A dedicated thread plus a latch
         * reproduces the blocking behaviour of Stage.showAndWait() that the callers
         * expect. Measured in the coexistence probe: the JavaFX thread keeps
         * running while this window is open.
         */
        Thread({
            /*
             * exitProcessOnExit = false is essential, not a preference: the default
             * true makes application { } call exitProcess when the last Compose
             * window closes. This dialog runs before the main window opens, so the
             * default would end the application the moment the operator picks a
             * profile.
             */
            application(exitProcessOnExit = false) {
                Window(
                    onCloseRequest = {
                        chosen = null
                        exitApplication()
                    },
                    state = rememberWindowState(width = 380.dp, height = 300.dp,
                        position = WindowPosition(Alignment.Center)),
                    title = "Select operator profile",
                ) {
                    Kst4ContestTheme(darkMode = false, baseFontSizeSp = DIALOG_FONT_SIZE_SP) {
                        PickerContent(
                            state = state,
                            onStart = { chosen = state.selected; exitApplication() },
                            onQuit = { chosen = null; exitApplication() },
                        )
                    }
                }
            }
            closed.countDown()
        }, "operator-profile-picker").start()

        closed.await()
        return Optional.ofNullable(chosen)
    }
}

@Composable
private fun PickerContent(
    state: OperatorProfilePickerState,
    onStart: () -> Unit,
    onQuit: () -> Unit,
) {
    var selectedId by mutableStateOf(state.selected?.profileId)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("More than one operator profile is configured.")

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(state.profiles, key = { it.profileId }) { profile ->
                    val isSelected = profile.profileId == selectedId

                    Column {
                        /*
                         * Marked the same way as the profiles tab: a frame in the accent
                         * colour over a tinted row. Colouring only the name left the
                         * operator guessing which profile they were about to start.
                         */
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        Color.Transparent
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
                                .selectable(selected = isSelected, onClick = {
                                    state.select(profile)
                                    selectedId = profile.profileId
                                })
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        ) {
                            Text(profile.displayName)
                            Text(
                                workedDatabaseDescription(profile),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        if (!isSelected) {
                            HorizontalDivider(thickness = Density.HAIRLINE)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP)) {
                Form.button("Start", enabled = state.canConfirm, onClick = onStart)
                Form.button("Quit", onClick = onQuit)
            }
        }
    }
}
