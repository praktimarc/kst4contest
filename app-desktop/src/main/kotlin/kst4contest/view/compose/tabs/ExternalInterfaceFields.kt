package kst4contest.view.compose.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import kst4contest.view.compose.CompactTextField
import kst4contest.view.compose.Density
import kst4contest.view.compose.Form
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Controls the three externally-facing settings tabs need and the shared form controls
 * do not provide.
 *
 * Both exist because of the JavaFX behaviour they replace, not for decoration:
 *
 *  - [GuardedTextField] keeps the text the operator typed and only hands up values the
 *    caller accepts. The JavaFX fields validated on focus loss and put the stored value
 *    back on bad input. Writing through on every keystroke without that guard would be
 *    worse than the old behaviour, because the ChatPreferences setters for the AirScout
 *    identifiers, the AirScout port and the AirScout band value substitute the factory
 *    default on bad input instead of keeping the previous value. It also reports focus
 *    loss, which is where a port change may rebind a socket.
 *  - [FilePathRow] is the read-only path plus "Choose..." button of the Log synch tab.
 */
@Composable
internal fun GuardedTextField(
    label: String,
    value: String,
    enabled: Boolean = true,
    accepts: (String) -> Boolean = { true },
    onAccepted: (String) -> Unit,
    onCommit: () -> Unit = {},
) {
    /*
     * Keyed on the stored value: the draft is only reset when the stored value really
     * changed. A keystroke that is rejected, or one that round-trips to the same stored
     * value such as a trailing space that is trimmed away, leaves the typed text alone.
     */
    var draft by remember(value) { mutableStateOf(value) }
    var hadFocus by remember { mutableStateOf(false) }

    LabelledRow(label) {
        CompactTextField(
            value = draft,
            onValueChange = { typed ->
                draft = typed
                if (accepts(typed)) {
                    onAccepted(typed)
                }
            },
            enabled = enabled,
            isError = !accepts(draft),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focusState ->
                if (hadFocus && !focusState.isFocused) {
                    onCommit()
                }
                hadFocus = focusState.isFocused
            },
        )
    }
}

/**
 * A stored file path the operator changes through a file chooser only, the way the
 * JavaFX tab did: it showed a Label, never an editable field, so a typed path was never
 * possible there and is not introduced here.
 */
@Composable
internal fun FilePathRow(
    label: String,
    value: String,
    chooserTitle: String,
    onChosen: (String) -> Unit,
) {
    var choosing by remember { mutableStateOf(false) }

    LabelledRow(label) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Form.button("Choose...") { choosing = true }
        }
    }

    if (choosing) {
        FileChooserDialog(chooserTitle) { chosenPath ->
            choosing = false
            if (chosenPath != null) {
                onChosen(chosenPath)
            }
        }
    }
}

/**
 * The AWT file dialog, opened from Compose. The JavaFX chooser started in the user's
 * home directory and a cancelled choice kept the stored path — both are preserved.
 */
@Composable
private fun FileChooserDialog(title: String, onResult: (String?) -> Unit) {
    AwtWindow(
        create = {
            object : FileDialog(null as Frame?, title, LOAD) {
                override fun setVisible(visible: Boolean) {
                    if (visible) {
                        directory = System.getProperty("user.home")
                    }
                    super.setVisible(visible)
                    if (visible) {
                        val chosenName = file
                        onResult(
                            if (chosenName == null) null
                            else File(directory ?: "", chosenName).absolutePath
                        )
                    }
                }
            }
        },
        dispose = FileDialog::dispose,
    )
}

/** The label column width the shared form controls use, so the tabs line up. */
@Composable
private fun LabelledRow(label: String, field: @Composable () -> Unit) {
    /*
     * Delegates rather than repeating the row: the label column width and the row height
     * are defined once, in Form, so this file cannot drift away from the other tabs.
     */
    Form.labelledRow(label, field)
}
