package kst4contest.view.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StationFilterBar(
    filters: StationFilterState,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // ROW 1: Reset, QRB, QTF
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Group 1: Reset & QRB
            Row(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP), verticalAlignment = Alignment.CenterVertically) {
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

                FilterToggle("Show only QRB [km] <=", filters.maxQrbEnabled) {
                    filters.maxQrbEnabled = it
                    onChanged()
                }
                
                OutlinedTextField(
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
                    modifier = Modifier.width(60.dp).heightIn(min = Density.BUTTON_MIN_HEIGHT),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }

            // Group 2: QTF
            Row(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP), verticalAlignment = Alignment.CenterVertically) {
                FilterToggle("Show only QTF:", filters.qtfEnabled) {
                    filters.qtfEnabled = it
                    onChanged()
                }
                
                OutlinedTextField(
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
                    modifier = Modifier.width(55.dp).heightIn(min = Density.BUTTON_MIN_HEIGHT),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                
                Text("± BW/2", style = MaterialTheme.typography.bodySmall)

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

        // ROW 2: Find, Hide worked, Misc
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Find
            OutlinedTextField(
                value = filters.searchText,
                onValueChange = { 
                    filters.searchText = it
                    onChanged()
                },
                placeholder = { Text("Find...", style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.width(120.dp).heightIn(min = Density.BUTTON_MIN_HEIGHT),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )

            // Hide worked
            Row(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP), verticalAlignment = Alignment.CenterVertically) {
                Text("Hide worked:", style = MaterialTheme.typography.bodySmall)
                FilterToggle("wkd", filters.hideWorkedAny) {
                    filters.hideWorkedAny = it
                    onChanged()
                }
                WorkedBandFilter.entries.forEach { band ->
                    FilterToggle(band.label, filters.isHideWorkedOn(band)) {
                        filters.setHideWorkedOn(band, it)
                        onChanged()
                    }
                }
            }

            // Others
            Row(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP), verticalAlignment = Alignment.CenterVertically) {
                FilterToggle("Inactive stations", filters.hideInactive) {
                    filters.hideInactive = it
                    onChanged()
                }
                FilterToggle("Only new grids", filters.onlyNewGrids) {
                    filters.onlyNewGrids = it
                    onChanged()
                }
            }
        }

        // ROW 3: More misc
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterToggle("Grid color", filters.gridColouring) { filters.gridColouring = it }
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
        }
    }
}

@Composable
private fun FilterToggle(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val colours = if (on) {
        androidx.compose.material3.ButtonDefaults.buttonColors()
    } else {
        androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
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
