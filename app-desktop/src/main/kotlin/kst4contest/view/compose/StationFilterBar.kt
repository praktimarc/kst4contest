package kst4contest.view.compose

import kst4contest.view.i18n.LocalStrings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * The station filter bar above the station table.
 *
 * Laid out to match the JavaFX original: three tight rows of controls separated
 * by thin vertical rules between logical groups.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StationFilterBar(
    filters: StationFilterState,
    activeBands: Set<kst4contest.model.Band>,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        // ROW 1: Reset | QRB | QTF + bearings
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Reset filters
            FilterButton(strings.filterReset, on = false, isAccent = true) {
                filters.reset()
                onChanged()
            }

            VerticalSeparator()

            // QRB checkbox + field
            CompactCheckbox(
                checked = filters.maxQrbEnabled,
                onCheckedChange = {
                    filters.maxQrbEnabled = it
                    onChanged()
                },
            )
            Text(strings.filterShowOnlyQrb, style = MaterialTheme.typography.bodySmall)
            CompactTextField(
                value = if (filters.maxQrbKm.rem(1) == 0.0) filters.maxQrbKm.toInt().toString() else filters.maxQrbKm.toString(),
                onValueChange = {
                    val parsed = it.toDoubleOrNull()
                    if (parsed != null) {
                        filters.maxQrbKm = parsed
                        if (filters.maxQrbEnabled) onChanged()
                    } else if (it.isEmpty()) {
                        filters.maxQrbKm = 0.0
                        if (filters.maxQrbEnabled) onChanged()
                    }
                },
                modifier = Modifier.width(50.dp)
            )

            VerticalSeparator()

            // QTF checkbox + field + bearings
            CompactCheckbox(
                checked = filters.qtfEnabled,
                onCheckedChange = {
                    filters.qtfEnabled = it
                    onChanged()
                },
            )
            Text(strings.filterShowOnlyQtf, style = MaterialTheme.typography.bodySmall)
            CompactTextField(
                value = if (filters.qtfDegrees.rem(1) == 0.0) filters.qtfDegrees.toInt().toString() else filters.qtfDegrees.toString(),
                onValueChange = {
                    val parsed = it.toDoubleOrNull()
                    if (parsed != null) {
                        filters.qtfDegrees = parsed
                        filters.bearing = null
                        if (filters.qtfEnabled) onChanged()
                    } else if (it.isEmpty()) {
                        filters.qtfDegrees = 0.0
                        filters.bearing = null
                        if (filters.qtfEnabled) onChanged()
                    }
                },
                modifier = Modifier.width(50.dp)
            )
            Text("± BW/2", style = MaterialTheme.typography.bodySmall)

            Bearing.entries.forEach { bearing ->
                FilterButton(bearing.name, on = filters.bearing == bearing) {
                    filters.toggleBearing(bearing)
                    onChanged()
                }
            }
        }

        // ROW 2: Find | Hide worked: wkd 50 70 144 432 | Inactive stations | Only new grids
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactTextField(
                value = filters.searchText,
                onValueChange = {
                    filters.searchText = it
                    onChanged()
                },
                placeholder = strings.filterFind,
                modifier = Modifier.width(90.dp),
            )

            VerticalSeparator()

            Text(strings.filterHideWorked, style = MaterialTheme.typography.bodySmall)
            FilterButton("wkdany", on = filters.hideWorkedAny) {
                filters.hideWorkedAny = !filters.hideWorkedAny
                onChanged()
            }
            WorkedBandFilter.entries.forEach { band ->
                if (band.modelBand in activeBands) {
                    FilterButton(band.label, on = filters.isHideWorkedOn(band)) {
                        filters.setHideWorkedOn(band, !filters.isHideWorkedOn(band))
                        onChanged()
                    }
                }
            }

            VerticalSeparator()

            FilterButton(strings.filterInactiveStations, on = filters.hideInactive) {
                filters.hideInactive = !filters.hideInactive
                onChanged()
            }
            FilterButton(strings.filterOnlyNewGrids, on = filters.onlyNewGrids) {
                filters.onlyNewGrids = !filters.onlyNewGrids
                onChanged()
            }
        }

        // ROW 3: Grid color | Tropo | New bands | AS next 5m
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterButton(strings.filterGridColour, on = filters.gridColouring) {
                filters.gridColouring = !filters.gridColouring
            }
            FilterButton(strings.filterTropo, on = filters.tropoReachable) {
                filters.tropoReachable = !filters.tropoReachable
                onChanged()
            }
            FilterButton(strings.filterNewBands, on = filters.newBands) {
                filters.newBands = !filters.newBands
                onChanged()
            }
            FilterButton("AS next 5m", on = filters.airScoutNext5Min) {
                filters.airScoutNext5Min = !filters.airScoutNext5Min
                onChanged()
            }
        }
    }
}

/**
 * A compact toggle button that does NOT use Material 3 Button (which enforces 40dp).
 * Instead it is a plain clickable Box with border and background, matching the tiny
 * JavaFX filter buttons.
 */
@Composable
private fun FilterButton(label: String, on: Boolean, isAccent: Boolean = false, onClick: () -> Unit) {
    val palette = LocalJavaFxPalette.current
    val isDark = palette.windowBackground.luminance() < 0.5f

    val accentBrush = if (isDark) {
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(Color(0xFF008000), Color(0xFF90EE90))
        )
    } else {
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(Color(0xFF00FFFF), Color(0xFFFF99FF))
        )
    }

    val bgModifier = if (isAccent) {
        Modifier.background(accentBrush, Density.BUTTON_SHAPE)
    } else {
        val bg = if (on) MaterialTheme.colorScheme.primary else Color.Transparent
        Modifier.background(bg, Density.BUTTON_SHAPE)
    }

    val fg = if (isAccent) Color(0xFF395306) else if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isAccent) Color.Transparent else if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .heightIn(min = 20.dp)
            .then(if (borderColor != Color.Transparent) Modifier.border(Density.HAIRLINE, borderColor, Density.BUTTON_SHAPE) else Modifier)
            .then(bgModifier)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = fg, maxLines = 1)
    }
}

/** Thin vertical separator matching JavaFX's group dividers. */
@Composable
private fun VerticalSeparator() {
    Box(
        modifier = Modifier
            .width(Density.HAIRLINE)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.outline),
    )
}


@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
        decorationBox = { innerTextField ->
            Box(
                modifier = modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, Density.BUTTON_SHAPE)
                    .border(1.dp, MaterialTheme.colorScheme.outline, Density.BUTTON_SHAPE)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
                innerTextField()
            }
        },
    )
}
