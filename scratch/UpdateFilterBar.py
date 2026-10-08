import sys

content = """package kst4contest.view.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The filter bar above the station list.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StationFilterBar(
    filters: StationFilterState,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.OutlinedTextField(
                    value = filters.searchText,
                    onValueChange = { 
                        filters.searchText = it
                        onChanged()
                    },
                    label = { Text("User filter", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.width(150.dp).heightIn(min = Density.BUTTON_MIN_HEIGHT),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                androidx.compose.material3.Button(
                    onClick = {
                        filters.reset()
                        onChanged()
                    },
                    shape = Density.BUTTON_SHAPE,
                    contentPadding = Density.BUTTON_CONTENT_PADDING,
                    modifier = Modifier.heightIn(min = Density.BUTTON_MIN_HEIGHT)
                ) {
                    Text("Reset filters", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP)) {
            FilterToggle("Only new grids", filters.onlyNewGrids) {
                filters.onlyNewGrids = it
                onChanged()
            }
            FilterToggle("Tropo >=0dB", filters.tropoReachable) {
                filters.tropoReachable = it
                onChanged()
            }
            FilterToggle("New bands", filters.newBands) {
                filters.newBands = it
                onChanged()
            }
            FilterToggle("AS next 5m", filters.airScoutNext5Min) {
                filters.airScoutNext5Min = it
                onChanged()
            }
            FilterToggle("Inactive stations", filters.hideInactive) {
                filters.hideInactive = it
                onChanged()
            }
            /* Not a filter. It colours the locator cell and hides nothing. */
            FilterToggle("Grid color", filters.gridColouring) { filters.gridColouring = it }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hide worked:", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(90.dp))

            FilterToggle("wkd", filters.hideWorkedAny) {
                filters.hideWorkedAny = it
                onChanged()
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP)) {
                WorkedBandFilter.entries.forEach { band ->
                    FilterToggle(band.label, filters.isHideWorkedOn(band)) {
                        filters.setHideWorkedOn(band, it)
                        onChanged()
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Show only QTF:", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(90.dp))

            FilterToggle("on", filters.qtfEnabled) {
                filters.qtfEnabled = it
                onChanged()
            }
            
            androidx.compose.material3.OutlinedTextField(
                value = if (filters.qtfDegrees.rem(1) == 0.0) filters.qtfDegrees.toInt().toString() else filters.qtfDegrees.toString(),
                onValueChange = { 
                    val parsed = it.toDoubleOrNull()
                    if (parsed != null) {
                        filters.qtfDegrees = parsed
                        filters.bearing = null
                        if (filters.qtfEnabled) {
                            onChanged()
                        }
                    } else if (it.isEmpty()) {
                        filters.qtfDegrees = 0.0
                        filters.bearing = null
                        if (filters.qtfEnabled) {
                            onChanged()
                        }
                    }
                },
                modifier = Modifier.width(60.dp).padding(start = 6.dp).heightIn(min = Density.BUTTON_MIN_HEIGHT),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
                modifier = Modifier.padding(start = 6.dp),
            ) {
                Bearing.entries.forEach { bearing ->
                    FilterToggle(bearing.name, filters.bearing == bearing) {
                        filters.selectBearing(bearing)
                        if (filters.qtfEnabled) {
                            onChanged()
                        }
                    }
                }
            }
        }
    }
}

/** A toggle in the filter bar: pressed means on, and it says so by its colour. */
@Composable
private fun FilterToggle(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val colours = if (on) {
        androidx.compose.material3.ButtonDefaults.buttonColors()
    } else {
        androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
    }

    androidx.compose.material3.Button(
        onClick = { onChange(!on) },
        colors = colours,
        shape = Density.BUTTON_SHAPE,
        contentPadding = Density.BUTTON_CONTENT_PADDING,
        border = if (on) null else androidx.compose.foundation.BorderStroke(
            Density.HAIRLINE,
            MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.heightIn(min = Density.BUTTON_MIN_HEIGHT),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}
