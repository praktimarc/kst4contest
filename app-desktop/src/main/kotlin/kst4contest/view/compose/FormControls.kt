package kst4contest.view.compose

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Form controls shared by the settings tabs.
 *
 * Numeric fields keep the operator's text while it is being typed and only hand a
 * parsed value upwards when it parses. Rejecting keystrokes instead would make a
 * field impossible to clear, and replacing an unparsable entry with a default would
 * silently change a setting — PROJECT_CONTEXT is explicit that an absent or malformed
 * value must not become a fabricated zero.
 *
 * Every control here is built to desktop density; see [Density] for why and for the
 * measurements.
 */
object Form {

    @Composable
    fun text(label: String, value: String?, enabled: Boolean = true, onChange: (String) -> Unit) {
        val draft = rememberDraft(value ?: "")

        labelled(label) {
            CompactTextField(
                value = draft.value,
                onValueChange = { typed ->
                    draft.value = typed
                    onChange(typed)
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    @Composable
    fun password(label: String, value: String?, enabled: Boolean = true, onChange: (String) -> Unit) {
        val draft = rememberDraft(value ?: "")

        labelled(label) {
            CompactTextField(
                value = draft.value,
                onValueChange = { typed ->
                    draft.value = typed
                    onChange(typed)
                },
                enabled = enabled,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    /**
     * A text field that is validated when it is committed, not while it is typed.
     *
     * This is the Compose counterpart of the JavaFX fields which hung their rule on
     * `focusedProperty` — the DX cluster port, the spotter callsign, the beacon
     * templates and the beacon interval. Committing on every keystroke would refuse
     * every prefix of a valid entry, so the draft lives here until the operator leaves
     * the field or presses Enter.
     *
     * @param stored the value currently stored; the draft falls back to it when an
     *        entry is refused, which is what those fields did with setText
     * @param commit returns null when the value was accepted, otherwise the reason
     * @param onRefused shown to the operator; the JavaFX fields opened an alert
     */
    @Composable
    fun committed(
        label: String,
        stored: String,
        commit: (String) -> String?,
        onRefused: (String) -> Unit,
    ) {
        var draft by remember(stored) { mutableStateOf(stored) }

        fun commitDraft() {
            val refusal = commit(draft)
            if (refusal != null) {
                onRefused(refusal)
                draft = stored
            }
        }

        labelled(label) {
            CompactTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focus -> if (!focus.isFocused) commitDraft() }
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                            commitDraft()
                            true
                        } else {
                            false
                        }
                    },
            )
        }
    }

    /**
     * Picks one item from a fixed list, the counterpart of the JavaFX ChoiceBox.
     *
     * A null selection is possible and shown as such: the second chat category has no
     * value until the operator picks one, and inventing one would log the station into
     * a chat it never chose.
     */
    @Composable
    fun <T> choice(
        label: String,
        items: List<T>,
        selected: T?,
        describe: (T) -> String,
        enabled: Boolean = true,
        onSelect: (T) -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }
        val draft = rememberDraft(selected)

        labelled(label) {
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = enabled,
                    shape = Density.BUTTON_SHAPE,
                    contentPadding = Density.FIELD_CONTENT_PADDING,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Density.FIELD_MIN_HEIGHT),
                ) {
                    Text(
                        draft.value?.let(describe) ?: "not selected",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(describe(item), style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                expanded = false
                                draft.value = item
                                onSelect(item)
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun <T> inlineChoice(
        label: String,
        items: List<T>,
        selected: T?,
        describe: (T) -> String,
        enabled: Boolean = true,
        onSelect: (T) -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }
        val draft = rememberDraft(selected)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = enabled,
                    shape = Density.BUTTON_SHAPE,
                    contentPadding = Density.FIELD_CONTENT_PADDING,
                    modifier = Modifier.heightIn(min = Density.FIELD_MIN_HEIGHT),
                ) {
                    Text(
                        draft.value?.let(describe) ?: "not selected",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(describe(item), style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                expanded = false
                                draft.value = item
                                onSelect(item)
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun check(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
        val draft = rememberDraft(value)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = Density.ROW_MIN_HEIGHT),
        ) {
            CompactCheckbox(draft.value) { ticked ->
                draft.value = ticked
                onChange(ticked)
            }
            Text(label, modifier = Modifier.padding(start = 6.dp))
        }
    }

    @Composable
    fun int(label: String, value: Int, onChange: (Int) -> Unit) {
        var draft by remember(value) { mutableStateOf(value.toString()) }

        labelled(label) {
            CompactTextField(
                value = draft,
                onValueChange = { typed ->
                    draft = typed
                    typed.trim().toIntOrNull()?.let(onChange)
                },
                isError = draft.trim().toIntOrNull() == null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    @Composable
    fun decimal(label: String, value: Double, onChange: (Double) -> Unit) {
        var draft by remember(value) { mutableStateOf(value.toString()) }

        labelled(label) {
            CompactTextField(
                value = draft,
                onValueChange = { typed ->
                    draft = typed
                    typed.trim().replace(',', '.').toDoubleOrNull()?.let(onChange)
                },
                isError = draft.trim().replace(',', '.').toDoubleOrNull() == null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    /**
     * A titled section.
     *
     * The JavaFX window drew `generateLabeledSeparator`: a rule, the label, another
     * rule. Here the label leads and one rule runs to the right edge, which sits
     * better above the left-aligned label column than a centred title does. Either
     * way it is a visible division, which a bare line of text was not.
     */
    @Composable
    fun section(title: String, content: @Composable () -> Unit) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(Density.FIELD_GAP),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                HorizontalDivider(modifier = Modifier.weight(1f).padding(start = 8.dp))
            }
            content()
        }
    }

    /**
     * A section whose controls flow across the width instead of stacking.
     *
     * For short, like-for-like options — the nine band checkboxes, the startup filters.
     * Stacked they cost nine rows of height for nine words; flowing they fill the width
     * the window actually has and re-flow when it changes.
     */
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun flowingSection(title: String, content: @Composable FlowRowScope.() -> Unit) {
        section(title) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }

    /**
     * A labelled row holding a control this object does not provide, so a tab can add
     * one without losing the shared label column.
     */
    @Composable
    fun labelledRow(label: String, field: @Composable () -> Unit) {
        labelled(label, field)
    }

    /**
     * The button of the settings windows: square-ish corners and desktop padding.
     *
     * Material's own Button is a 40dp pill, which is the single strongest reason the
     * window looked like a phone application.
     */
    /**
     * The button of the settings windows.
     *
     * Material's own Button is a 40dp pill; this one is square-ish and at desktop
     * height. It also carries the two states the JavaFX stylesheets give every button:
     * a gradient while the pointer is over it and a red border while it is held down.
     */
    @Composable
    fun button(text: String, enabled: Boolean = true, onClick: () -> Unit) {
        val palette = LocalJavaFxPalette.current
        val interactions = remember { MutableInteractionSource() }
        val hovered by interactions.collectIsHoveredAsState()
        val pressed by interactions.collectIsPressedAsState()

        val hoverGradient = palette.buttonHoverGradient

        CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides 0.dp
        ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = Density.BUTTON_SHAPE,
            contentPadding = Density.BUTTON_CONTENT_PADDING,
            interactionSource = interactions,
            border = if (pressed) {
                BorderStroke(Density.SELECTION_BORDER, palette.buttonPressedBorder)
            } else {
                null
            },
            colors = if (hovered && enabled && hoverGradient.isNotEmpty()) {
                /*
                 * Compose paints a solid container, so the gradient is applied as the
                 * background below and the container itself is cleared. The text turns
                 * dark because the gradient the sheets use is a bright lime.
                 */
                ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black,
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            modifier = Modifier
                .heightIn(min = Density.BUTTON_MIN_HEIGHT)
                .then(
                    if (hovered && enabled && hoverGradient.size >= 2) {
                        Modifier.background(
                            brush = Brush.verticalGradient(hoverGradient),
                            shape = Density.BUTTON_SHAPE,
                        )
                    } else {
                        Modifier
                    }
                ),
        ) {
            /*
             * One line, always. A label left to wrap turns a squeezed button into a tall
             * bar instead of simply being too narrow, and that bar then pushes the rest
             * of the window out of view.
             */
            Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        }
    }

    /**
     * Holds what the operator sees while they change it.
     *
     * The settings controls are bound to plain ChatPreferences values, and those are not
     * Compose state: writing one triggers no recomposition, so a field bound straight to
     * the stored value keeps painting the value from the last composition. Typing then
     * writes through correctly and shows nothing, a checkbox never appears to tick, and a
     * picker never appears to change — which is exactly how it behaved.
     *
     * Keyed on the stored value, so an external change still reaches the control, and so
     * a setter that normalises its input (a trimmed space, an upper-cased callsign) puts
     * the normalised value back. This is the pattern GuardedTextField already used for
     * the external-interface fields.
     */
    @Composable
    private fun <T> rememberDraft(stored: T): MutableState<T> =
        remember(stored) { mutableStateOf(stored) }

    @Composable
    private fun labelled(label: String, field: @Composable () -> Unit) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = Density.ROW_MIN_HEIGHT),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(Density.LABEL_COLUMN_WIDTH),
            )
            field()
        }
    }
}

/**
 * A text field at desktop density.
 *
 * Built from BasicTextField and the outlined decoration box rather than from
 * OutlinedTextField, because that composable enforces a 56dp minimum height which
 * cannot be passed a smaller one. The decoration box takes the content padding, and
 * that is what buys the height back.
 *
 * The decoration box and its container are marked experimental in Material 3. The
 * opt-in is deliberate and there is no alternative: it is the only supported way to
 * build a text field below the enforced minimum height. Should the signature change,
 * the two calls below are the whole surface to fix, and the tests around the tab states
 * stay unaffected because none of them render.
 *
 * @param borderColor overrides the outline, so a caller can mark a selected row
 * @param borderThickness overrides the outline width for the same reason
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    borderColor: Color? = null,
    borderThickness: Dp? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    /*
     * The container is the JavaFX -fx-control-inner-background, which both sheets set
     * apart from the window background. Material leaves an outlined field transparent,
     * which made every field vanish into the surface.
     */
    val colors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = borderColor ?: MaterialTheme.colorScheme.outline,
        focusedBorderColor = borderColor ?: MaterialTheme.colorScheme.primary,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    )

    /*
     * The colour has to be carried in the text style: BasicTextField has no theme of
     * its own, so without this the text would stay black in the evening design.
     */
    val onField = MaterialTheme.colorScheme.surfaceVariant.let { field ->
        val brightness = 0.3f * field.red + 0.59f * field.green + 0.11f * field.blue
        if (brightness > 0.5f) Color.Black else Color.White
    }
    val textColor = if (enabled) onField else onField.copy(alpha = 0.38f)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        modifier = modifier.heightIn(min = Density.FIELD_MIN_HEIGHT),
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = enabled,
            singleLine = true,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            isError = isError,
            colors = colors,
            contentPadding = Density.FIELD_CONTENT_PADDING,
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors,
                    shape = Density.BUTTON_SHAPE,
                    focusedBorderThickness = borderThickness ?: 2.dp,
                    unfocusedBorderThickness = borderThickness ?: 1.dp,
                )
            },
        )
    }
}

/**
 * A checkbox without the 48dp touch target Material reserves around it. On a desktop
 * form that target is dead space between every two rows.
 */
@Composable
internal fun CompactCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(Density.CHECK_SIZE),
        )
    }
}
