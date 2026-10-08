package kst4contest.view.compose.map

import kst4contest.view.i18n.LocalStrings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import kst4contest.view.map.MapViewport

/** The zoom range the tile source serves, as the viewport defines it. */
internal val MIN_MAP_ZOOM = MapViewport.MIN_ZOOM.toFloat()
internal val MAX_MAP_ZOOM = MapViewport.MAX_ZOOM.toFloat()

/**
 * The two things Leaflet drew on top of the map itself.
 *
 * The zoom buttons, because a map without them can only be zoomed by a wheel or a
 * trackpad and neither is a given on a contest laptop. And the OpenStreetMap
 * attribution, which is not decoration: the tile licence requires it to be visible
 * wherever the tiles are.
 */
@Composable
internal fun BoxScope.MapOverlayControls(
    darkMode: Boolean,
    tilesUnavailable: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
    ) {
    val strings = LocalStrings.current
        ZoomButton("+", darkMode, onZoomIn)
        ZoomButton("−", darkMode, onZoomOut)
    }

    if (tilesUnavailable) {
        /*
         * Said out loud. An empty map and a map with no stations on it look the same,
         * and an operator in a contest should not have to guess which one they have.
         */
        Text(
            text = strings.mapTilesUnavailable,
            color = TILE_WARNING_TEXT,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .background(TILE_WARNING_BACKGROUND, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }

    Text(
        text = "© OpenStreetMap contributors",
        fontSize = TextUnit(10f, TextUnitType.Sp),
        color = if (darkMode) ATTRIBUTION_TEXT_DARK else ATTRIBUTION_TEXT_LIGHT,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .background(if (darkMode) ATTRIBUTION_BACKGROUND_DARK else ATTRIBUTION_BACKGROUND_LIGHT)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

@Composable
private fun ZoomButton(label: String, darkMode: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .background(
                if (darkMode) CONTROL_BACKGROUND_DARK else CONTROL_BACKGROUND_LIGHT,
                RoundedCornerShape(3.dp),
            )
            .border(
                1.dp,
                if (darkMode) CONTROL_BORDER_DARK else CONTROL_BORDER_LIGHT,
                RoundedCornerShape(3.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
    val strings = LocalStrings.current
        Text(
            text = label,
            color = if (darkMode) CONTROL_TEXT_DARK else CONTROL_TEXT_LIGHT,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

/* The `--control-*` and `--attribution-*` values of the Leaflet stylesheet. */
private val CONTROL_BACKGROUND_LIGHT = Color(0xFFFFFFFF).copy(alpha = 0.96f)
private val CONTROL_BACKGROUND_DARK = Color(0xFF373E43).copy(alpha = 0.96f)
private val CONTROL_TEXT_LIGHT = Color(0xFF242424)
private val CONTROL_TEXT_DARK = Color(0xFFE2E6EA)
private val CONTROL_BORDER_LIGHT = Color(0xFFB7B7B7)
private val CONTROL_BORDER_DARK = Color(0xFF556068)
private val ATTRIBUTION_BACKGROUND_LIGHT = Color(0xFFFFFFFF).copy(alpha = 0.88f)
private val ATTRIBUTION_BACKGROUND_DARK = Color(0xFF22262B).copy(alpha = 0.86f)
private val ATTRIBUTION_TEXT_LIGHT = Color(0xFF2D2D2D)
private val ATTRIBUTION_TEXT_DARK = Color(0xFFD2D8DD)
private val TILE_WARNING_TEXT = Color(0xFFFFE0B2)
private val TILE_WARNING_BACKGROUND = Color(0xFF5A2B20).copy(alpha = 0.92f)
