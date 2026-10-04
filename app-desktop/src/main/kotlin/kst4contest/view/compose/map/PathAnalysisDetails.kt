package kst4contest.view.compose.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import kst4contest.view.map.PathAnalysisResult

/**
 * The path and terrain analysis column of the station map window.
 *
 * It lives in its own file so that it can be composed in a test. The window around it
 * needs a running application runtime and a chat controller; this column needs nothing
 * but a result, which is what made the layout defect below invisible to the suite for
 * as long as it existed.
 */
@Composable
internal fun PathAnalysisDetails(
    result: PathAnalysisResult?,
    darkMode: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            "Path / terrain analysis",
            fontSize = TextUnit(12f, TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp),
        )
        HorizontalDivider(Modifier.padding(vertical = 2.dp))

        if (result == null) {
            Text(
                "Select a station to prepare path and terrain analysis.",
                fontSize = TextUnit(12f, TextUnitType.Sp),
            )
            return@Column
        }

        DetailRow("From locator:", result.fromLocator6().ifBlank { "-" })
        DetailRow("To locator:", result.toLocator6().ifBlank { "-" })
        DetailRow("Distance/QTF:", "${result.distanceText()} / ${result.bearingText()}")
        DetailRow("Endpoints:", result.endpointSummaryText())
        DetailRow("Source:", "${result.analysisMode().ifBlank { "-" }} / ${result.profilePoints().size} samples")
        DetailRow("Frequency:", result.analysisFrequencyText())
        DetailRow("Refraction:", result.effectiveEarthRadiusText())
        DetailRow("Radio horizon:", result.radioHorizonText())
        DetailRow("Terrain horizon:", result.terrainHorizonText())
        DetailRow("Fresnel:", "${result.fresnelText()} / ${result.worstFresnelClearanceText()}")
        DetailRow("Obstruction:", result.obstructionText())

        val severity = result.propagationSeverityLevel()
        val textColor = if (darkMode) Color(0xFFF0F0F0) else Color(0xFF202020)
        val backgroundColor = when (severity) {
            1 -> if (darkMode) Color(0xFF1F4D2B) else Color(0xFFD8F3DC)
            2 -> if (darkMode) Color(0xFF4A4420) else Color(0xFFFFF3BF)
            3 -> if (darkMode) Color(0xFF4D3520) else Color(0xFFFFE0B2)
            4 -> if (darkMode) Color(0xFF5A2B20) else Color(0xFFFFC9A9)
            5 -> if (darkMode) Color(0xFF5A2020) else Color(0xFFFFCDD2)
            else -> if (darkMode) Color(0xFF33383E) else Color(0xFFEEEEEE)
        }
        val borderColor = when (severity) {
            1 -> Color(0xFF3AA655)
            2 -> Color(0xFFD4A017)
            3 -> Color(0xFFE69138)
            4 -> Color(0xFFD96C2C)
            5 -> Color(0xFFD63B3B)
            else -> if (darkMode) Color(0xFF666F78) else Color(0xFFCCCCCC)
        }

        Row(Modifier.padding(vertical = 2.dp).fillMaxWidth()) {
            Text(
                "Assessment:",
                modifier = Modifier.weight(LABEL_WEIGHT),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = TextUnit(11f, TextUnitType.Sp),
                lineHeight = TextUnit(13f, TextUnitType.Sp),
            )
            Box(modifier = Modifier.weight(VALUE_WEIGHT)) {
                Box(
                    modifier = Modifier
                        .background(backgroundColor, RoundedCornerShape(4.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Text(result.propagationAssessmentText(), color = textColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        DetailRow("Link budget:", result.linkBudgetText())
        DetailRow("RX power:", result.linkBudgetRxPowerText())
        DetailRow("CW hint:", result.cwHintText())
        DetailRow("Mechanisms:", result.propagationMechanismsText(), valueColor = textColor, valueBold = true)
        DetailRow("LOS:", "${result.losText()} / worst ${result.worstClearanceText()}")
        DetailRow("Status:", result.statusText())
    }
}

/**
 * One label and its value.
 *
 * Both columns are weighted, and the assessment row above follows the same two weights
 * rather than a fixed label width. A fixed width does not shrink: at a narrow pane it
 * keeps its dp and the value beside it gets whatever is left, which was 35% of the pane
 * at 200 px and nothing at all at 60 px.
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    valueBold: Boolean = false,
) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.weight(LABEL_WEIGHT),
            fontSize = TextUnit(11f, TextUnitType.Sp),
            lineHeight = TextUnit(13f, TextUnitType.Sp),
        )
        Text(
            text = value,
            modifier = Modifier.weight(VALUE_WEIGHT),
            color = valueColor,
            fontWeight = if (valueBold) FontWeight.Bold else null,
            fontSize = TextUnit(11f, TextUnitType.Sp),
            lineHeight = TextUnit(13f, TextUnitType.Sp),
        )
    }
}

private const val LABEL_WEIGHT = 0.38f
private const val VALUE_WEIGHT = 0.62f
