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
import androidx.compose.ui.awt.ComposeDialog
import kst4contest.model.OperatorProfile
import java.awt.Dialog
import java.awt.Dimension
import java.awt.EventQueue
import java.util.Optional
import java.util.concurrent.atomic.AtomicReference
import javax.swing.WindowConstants

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

    private val DIALOG_SIZE = Dimension(380, 300)

    /**
     * Opens the picker and blocks until the operator decides.
     *
     * A modal [ComposeDialog] and NOT the former dedicated thread plus `CountDownLatch`.
     * That construction deadlocked once the UiDispatcher moved to the AWT event thread:
     * `ComposeMenuActions.switchOperatorProfile` delivers the File menu's profile switch
     * there, and skiko's SwingDispatcher posts *every* continuation of `application { }`
     * — composition, the button callbacks, even the return that would release the latch —
     * to that same thread, with no `isDispatchNeeded` shortcut. A latch does not pump
     * events, so the window never appeared and the client froze with no timeout anywhere
     * in the path. A modal AWT dialog runs a nested event pump instead: it blocks its
     * caller *and* keeps drawing, which is what `Stage.showAndWait()` did.
     *
     * Works from any thread. The startup path calls this from `main` before any window
     * exists; the File menu calls it from the event thread. Either way the dialog itself is
     * built and shown ON the event thread — see [openOnEventThread].
     */
    @JvmStatic
    fun showAndSelect(
        selectableProfiles: List<OperatorProfile>,
        preselectedProfileId: String?,
    ): Optional<OperatorProfile> {
        val state = OperatorProfilePickerState(selectableProfiles, preselectedProfileId)
        val chosen = AtomicReference<OperatorProfile?>(null)

        openOnEventThread { openPicker(state, chosen) }

        return Optional.ofNullable(chosen.get())
    }

    private fun openPicker(
        state: OperatorProfilePickerState,
        chosen: AtomicReference<OperatorProfile?>,
    ) {
        val dialog = ComposeDialog(owner = null, modalityType = Dialog.ModalityType.APPLICATION_MODAL)
        dialog.title = "Select operator profile"
        dialog.size = DIALOG_SIZE
        dialog.setLocationRelativeTo(null)
        /* Disposed and not hidden: a hidden dialog keeps its window and Skia layer alive. */
        dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        /*
         * Raised above everything else. At startup there is nothing to hide behind, but
         * the File menu opens this over the main window, which is not its owner.
         */
        dialog.isAlwaysOnTop = true

        dialog.setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = DIALOG_FONT_SIZE_SP) {
                PickerContent(
                    state = state,
                    onStart = { chosen.set(state.selected); dialog.dispose() },
                    /*
                     * Quit and the window X agree: no choice. Every caller reads an empty
                     * Optional as "the operator does not want to continue", which at
                     * startup ends the process.
                     */
                    onQuit = { chosen.set(null); dialog.dispose() },
                )
            }
        }

        dialog.isVisible = true
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
