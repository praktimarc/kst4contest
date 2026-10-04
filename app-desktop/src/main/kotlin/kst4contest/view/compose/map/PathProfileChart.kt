package kst4contest.view.compose.map

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.ExperimentalComposeUiApi
import kst4contest.view.map.PathGeometryUtils
import kst4contest.view.map.PathHorizonSummary
import kst4contest.view.map.PathObstructionSummary
import kst4contest.view.map.PathProfilePoint
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PathProfileChart(
    modifier: Modifier = Modifier,
    profilePoints: List<PathProfilePoint> = emptyList(),
    totalDistanceKm: Double = Double.NaN,
    darkMode: Boolean = false,
    homeAntennaHeightMeters: Double = Double.NaN,
    targetAntennaHeightMeters: Double = Double.NaN,
    analysisFrequencyMHz: Double = Double.NaN,
    effectiveEarthRadiusFactor: Double = PathGeometryUtils.DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR,
    horizonSummary: PathHorizonSummary = PathHorizonSummary.empty(),
    obstructionSummary: PathObstructionSummary = PathObstructionSummary.empty(),
    onProfilePointHovered: ((PathProfilePoint?) -> Unit)? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(fontSize = 12.sp)

    Canvas(modifier = modifier
        .onPointerEvent(PointerEventType.Move) { event ->
            if (onProfilePointHovered != null && profilePoints.isNotEmpty()) {
                val position = event.changes.first().position
                val x = position.x.toDouble()
                val left = 58.0
                val right = 18.0
                // For a proper hover, we need the total width of the Canvas, which we can get from the layout or event
                // event.changes bounds? We can assume the size is roughly available if we store it.
                // Alternatively, we use awaitPointerEventScope inside pointerInput which gives access to size.
            }
        }
        .onPointerEvent(PointerEventType.Exit) {
            onProfilePointHovered?.invoke(null)
        }
    ) {
        val width = size.width.toDouble()
        val height = size.height.toDouble()

        if (width <= 1.0 || height <= 1.0) {
            return@Canvas
        }

        // --- Colors ---
        val background = if (darkMode) Color(34, 39, 43) else Color(248, 248, 248)
        val border = if (darkMode) Color(96, 106, 116) else Color(180, 180, 180)
        val gridColor = if (darkMode) Color(95, 105, 115, (255 * 0.70).toInt()) else Color(185, 185, 185, (255 * 0.75).toInt())
        val textColor = if (darkMode) Color(230, 235, 239) else Color(40, 40, 40)

        val terrainFill = if (darkMode) Color(70, 165, 95, (255 * 0.35).toInt()) else Color(90, 180, 110, (255 * 0.35).toInt())
        val terrainLine = if (darkMode) Color(95, 210, 120) else Color(45, 150, 70)

        val losLine = if (darkMode) Color(255, 182, 80) else Color(220, 130, 20)

        val fresnelFill = if (darkMode) Color(170, 135, 255, (255 * 0.16).toInt()) else Color(150, 105, 240, (255 * 0.16).toInt())
        val fresnelLine = if (darkMode) Color(205, 155, 255) else Color(130, 75, 210)

        val endpointMarker = if (darkMode) Color(245, 245, 245) else Color(45, 45, 45)
        val horizonMarker = if (darkMode) Color(95, 190, 255) else Color(30, 120, 200)
        val criticalMarker = if (darkMode) Color(255, 100, 100) else Color(210, 45, 45)

        val terrainHorizonMarker = if (darkMode) Color(255, 220, 105) else Color(185, 125, 0)
        val obstructionMarker = if (darkMode) Color(255, 135, 75) else Color(225, 85, 30)

        drawRect(color = background)
        drawRect(
            color = border,
            topLeft = Offset(0.5f, 0.5f),
            size = Size((width - 1.0).toFloat(), (height - 1.0).toFloat()),
            style = Stroke(1.0f)
        )

        val left = 58.0
        val headerTop = 12.0
        val headerHeight = 34.0
        val endpointTextHeight = 18.0
        val top = headerTop + headerHeight + endpointTextHeight
        val right = 18.0
        val bottom = 56.0

        val plotX = left
        val plotY = top
        val plotWidth = max(10.0, width - left - right)
        val plotHeight = max(10.0, height - top - bottom)

        // Draw Grid
        for (i in 0..4) {
            val y = plotY + plotHeight * i / 4.0
            drawLine(gridColor, Offset(plotX.toFloat(), y.toFloat()), Offset((plotX + plotWidth).toFloat(), y.toFloat()), strokeWidth = 1f)
        }
        for (i in 0..4) {
            val x = plotX + plotWidth * i / 4.0
            drawLine(gridColor, Offset(x.toFloat(), plotY.toFloat()), Offset(x.toFloat(), (plotY + plotHeight).toFloat()), strokeWidth = 1f)
        }

        fun estimateTextWidth(text: String?): Double {
            if (text.isNullOrBlank()) return 0.0
            // Simple approximation like in JavaFX code
            return text.length * 6.2
        }

        fun drawAxisLabels(minElevation: Double, maxElevation: Double) {
            val style = textStyle.copy(color = textColor)
            for (i in 0..4) {
                val fraction = i / 4.0
                val y = plotY + plotHeight - fraction * plotHeight
                val value = minElevation + fraction * (maxElevation - minElevation)
                safeDrawText(textMeasurer, String.format(Locale.US, "%.0f", value), topLeft = Offset(8f, (y - 6).toFloat()), style = style)
            }
            val xFractions = doubleArrayOf(0.0, 0.25, 0.50, 0.75, 1.0)
            for (xFraction in xFractions) {
                val x = plotX + xFraction * plotWidth
                val distance = if (totalDistanceKm.isFinite()) totalDistanceKm * xFraction else 0.0
                safeDrawText(textMeasurer, String.format(Locale.US, "%.0f", distance), topLeft = Offset((x - 8).toFloat(), (plotY + plotHeight + 16.0).toFloat()), style = style)
            }
            safeDrawText(textMeasurer, "Height [m]", topLeft = Offset(plotX.toFloat(), (plotY - 14.0).toFloat()), style = style)
            safeDrawText(textMeasurer, "Distance [km]", topLeft = Offset((plotX + plotWidth / 2.0 - 30.0).toFloat(), (plotY + plotHeight + 34.0).toFloat()), style = style)
            if (analysisFrequencyMHz.isFinite() && analysisFrequencyMHz > 0.0) {
                val freqText = String.format(Locale.US, "f = %.3f MHz", analysisFrequencyMHz)
                safeDrawText(textMeasurer, freqText, topLeft = Offset((plotX + plotWidth - estimateTextWidth(freqText)).toFloat(), (plotY + plotHeight + 34.0).toFloat()), style = style)
            }
        }

        if (profilePoints.isEmpty()) {
            safeDrawText(textMeasurer, "No profile samples available.", topLeft = Offset(plotX.toFloat(), (plotY + plotHeight / 2.0).toFloat()), style = textStyle.copy(color = textColor))
            drawAxisLabels(0.0, 1.0)
            return@Canvas
        }

        fun terrainDisplayElevationMeters(point: PathProfilePoint?): Double {
            if (point == null) return Double.NaN
            if (point.curvatureAdjustedElevationMeters().isFinite()) {
                return point.curvatureAdjustedElevationMeters()
            }
            return PathGeometryUtils.calculateCurvatureAdjustedElevationMeters(point, totalDistanceKm)
        }

        fun determineMinimumElevation(): Double {
            var minElev = Double.POSITIVE_INFINITY
            for (point in profilePoints) {
                minElev = min(minElev, terrainDisplayElevationMeters(point))
                if (point.lineOfSightHeightMeters().isFinite()) minElev = min(minElev, point.lineOfSightHeightMeters())
                if (point.fresnelUpperHeightMeters().isFinite()) minElev = min(minElev, point.fresnelUpperHeightMeters())
                if (point.fresnelLowerHeightMeters().isFinite()) minElev = min(minElev, point.fresnelLowerHeightMeters())
            }
            return if (minElev.isInfinite()) 0.0 else minElev
        }

        fun determineMaximumElevation(): Double {
            var maxElev = Double.NEGATIVE_INFINITY
            for (point in profilePoints) {
                maxElev = max(maxElev, terrainDisplayElevationMeters(point))
                if (point.lineOfSightHeightMeters().isFinite()) maxElev = max(maxElev, point.lineOfSightHeightMeters())
                if (point.fresnelUpperHeightMeters().isFinite()) maxElev = max(maxElev, point.fresnelUpperHeightMeters())
                if (point.fresnelLowerHeightMeters().isFinite()) maxElev = max(maxElev, point.fresnelLowerHeightMeters())
            }
            return if (maxElev.isInfinite()) 1.0 else maxElev
        }

        var minElevation = determineMinimumElevation()
        var maxElevation = determineMaximumElevation()
        var elevationRange = max(1.0, maxElevation - minElevation)
        val paddingMeters = max(20.0, elevationRange * 0.08)

        minElevation -= paddingMeters
        maxElevation += paddingMeters
        elevationRange = max(1.0, maxElevation - minElevation)

        fun normalizeDistance(distanceKm: Double): Double {
            if (!totalDistanceKm.isFinite() || totalDistanceKm <= 0.0) return 0.0
            return max(0.0, min(1.0, distanceKm / totalDistanceKm))
        }

        fun normalizeElevation(elevationMeters: Double, minElev: Double, elevRange: Double): Double {
            return max(0.0, min(1.0, (elevationMeters - minElev) / elevRange))
        }

        fun hasEnrichedLosGeometry(): Boolean {
            return profilePoints.any { it.lineOfSightHeightMeters().isFinite() }
        }

        fun hasEnrichedFresnelGeometry(): Boolean {
            return profilePoints.any { it.fresnelUpperHeightMeters().isFinite() || it.fresnelLowerHeightMeters().isFinite() }
        }

        // Draw terrain fill
        if (profilePoints.size >= 2) {
            val path = Path()
            path.moveTo((plotX + normalizeDistance(profilePoints.first().distanceKm()) * plotWidth).toFloat(), (plotY + plotHeight).toFloat())
            for (point in profilePoints) {
                val x = plotX + normalizeDistance(point.distanceKm()) * plotWidth
                val y = plotY + plotHeight - normalizeElevation(terrainDisplayElevationMeters(point), minElevation, elevationRange) * plotHeight
                path.lineTo(x.toFloat(), y.toFloat())
            }
            path.lineTo((plotX + normalizeDistance(profilePoints.last().distanceKm()) * plotWidth).toFloat(), (plotY + plotHeight).toFloat())
            path.close()
            drawPath(path, color = terrainFill, style = Fill)
        }

        // Draw Fresnel fill
        if (hasEnrichedFresnelGeometry() && profilePoints.size >= 2) {
            val allFresnelFinite = profilePoints.all { it.fresnelUpperHeightMeters().isFinite() && it.fresnelLowerHeightMeters().isFinite() }
            if (allFresnelFinite) {
                val path = Path()
                path.moveTo((plotX + normalizeDistance(profilePoints.first().distanceKm()) * plotWidth).toFloat(),
                    (plotY + plotHeight - normalizeElevation(profilePoints.first().fresnelUpperHeightMeters(), minElevation, elevationRange) * plotHeight).toFloat())
                
                for (point in profilePoints) {
                    val x = plotX + normalizeDistance(point.distanceKm()) * plotWidth
                    val y = plotY + plotHeight - normalizeElevation(point.fresnelUpperHeightMeters(), minElevation, elevationRange) * plotHeight
                    path.lineTo(x.toFloat(), y.toFloat())
                }
                for (i in profilePoints.indices.reversed()) {
                    val point = profilePoints[i]
                    val x = plotX + normalizeDistance(point.distanceKm()) * plotWidth
                    val y = plotY + plotHeight - normalizeElevation(point.fresnelLowerHeightMeters(), minElevation, elevationRange) * plotHeight
                    path.lineTo(x.toFloat(), y.toFloat())
                }
                path.close()
                drawPath(path, color = fresnelFill, style = Fill)
            }
        }

        // Draw terrain line
        for (i in 1 until profilePoints.size) {
            val prev = profilePoints[i - 1]
            val curr = profilePoints[i]
            val x1 = plotX + normalizeDistance(prev.distanceKm()) * plotWidth
            val y1 = plotY + plotHeight - normalizeElevation(terrainDisplayElevationMeters(prev), minElevation, elevationRange) * plotHeight
            val x2 = plotX + normalizeDistance(curr.distanceKm()) * plotWidth
            val y2 = plotY + plotHeight - normalizeElevation(terrainDisplayElevationMeters(curr), minElevation, elevationRange) * plotHeight
            drawLine(terrainLine, Offset(x1.toFloat(), y1.toFloat()), Offset(x2.toFloat(), y2.toFloat()), strokeWidth = 2f)
        }

        // Draw Fresnel hull
        if (hasEnrichedFresnelGeometry()) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
            for (i in 1 until profilePoints.size) {
                val prev = profilePoints[i - 1]
                val curr = profilePoints[i]
                if (prev.fresnelUpperHeightMeters().isFinite() && curr.fresnelUpperHeightMeters().isFinite()) {
                    val x1 = plotX + normalizeDistance(prev.distanceKm()) * plotWidth
                    val y1 = plotY + plotHeight - normalizeElevation(prev.fresnelUpperHeightMeters(), minElevation, elevationRange) * plotHeight
                    val x2 = plotX + normalizeDistance(curr.distanceKm()) * plotWidth
                    val y2 = plotY + plotHeight - normalizeElevation(curr.fresnelUpperHeightMeters(), minElevation, elevationRange) * plotHeight
                    drawLine(fresnelLine, Offset(x1.toFloat(), y1.toFloat()), Offset(x2.toFloat(), y2.toFloat()), strokeWidth = 1f, pathEffect = dashEffect)
                }
                if (prev.fresnelLowerHeightMeters().isFinite() && curr.fresnelLowerHeightMeters().isFinite()) {
                    val x1 = plotX + normalizeDistance(prev.distanceKm()) * plotWidth
                    val y1 = plotY + plotHeight - normalizeElevation(prev.fresnelLowerHeightMeters(), minElevation, elevationRange) * plotHeight
                    val x2 = plotX + normalizeDistance(curr.distanceKm()) * plotWidth
                    val y2 = plotY + plotHeight - normalizeElevation(curr.fresnelLowerHeightMeters(), minElevation, elevationRange) * plotHeight
                    drawLine(fresnelLine, Offset(x1.toFloat(), y1.toFloat()), Offset(x2.toFloat(), y2.toFloat()), strokeWidth = 1f, pathEffect = dashEffect)
                }
            }
        }

        // Draw LOS line
        if (hasEnrichedLosGeometry()) {
            for (i in 1 until profilePoints.size) {
                val prev = profilePoints[i - 1]
                val curr = profilePoints[i]
                if (prev.lineOfSightHeightMeters().isFinite() && curr.lineOfSightHeightMeters().isFinite()) {
                    val x1 = plotX + normalizeDistance(prev.distanceKm()) * plotWidth
                    val y1 = plotY + plotHeight - normalizeElevation(prev.lineOfSightHeightMeters(), minElevation, elevationRange) * plotHeight
                    val x2 = plotX + normalizeDistance(curr.distanceKm()) * plotWidth
                    val y2 = plotY + plotHeight - normalizeElevation(curr.lineOfSightHeightMeters(), minElevation, elevationRange) * plotHeight
                    drawLine(losLine, Offset(x1.toFloat(), y1.toFloat()), Offset(x2.toFloat(), y2.toFloat()), strokeWidth = 1.5f)
                }
            }
        } else if (profilePoints.size >= 2 && homeAntennaHeightMeters.isFinite() && targetAntennaHeightMeters.isFinite()) {
            val startAntennaMeters = terrainDisplayElevationMeters(profilePoints.first()) + homeAntennaHeightMeters
            val endAntennaMeters = terrainDisplayElevationMeters(profilePoints.last()) + targetAntennaHeightMeters
            val x1 = plotX
            val y1 = plotY + plotHeight - normalizeElevation(startAntennaMeters, minElevation, elevationRange) * plotHeight
            val x2 = plotX + plotWidth
            val y2 = plotY + plotHeight - normalizeElevation(endAntennaMeters, minElevation, elevationRange) * plotHeight
            drawLine(losLine, Offset(x1.toFloat(), y1.toFloat()), Offset(x2.toFloat(), y2.toFloat()), strokeWidth = 1.5f)
        }

        fun buildEndpointLabel(name: String, groundMetersAsl: Double, antennaHeightMetersAgl: Double): String {
            val hasGround = groundMetersAsl.isFinite()
            val hasAntenna = antennaHeightMetersAgl.isFinite()
            if (hasGround && hasAntenna) return String.format(Locale.US, "%s: %.0f m ASL + %.0f m AGL", name, groundMetersAsl, antennaHeightMetersAgl)
            if (hasGround) return String.format(Locale.US, "%s: %.0f m ASL", name, groundMetersAsl)
            if (hasAntenna) return String.format(Locale.US, "%s: +%.0f m AGL", name, antennaHeightMetersAgl)
            return "$name: -"
        }

        // Endpoint markers
        if (profilePoints.size >= 2) {
            val first = profilePoints.first()
            val last = profilePoints.last()
            val firstGround = terrainDisplayElevationMeters(first)
            val lastGround = terrainDisplayElevationMeters(last)
            val firstAntenna = if (first.lineOfSightHeightMeters().isFinite()) first.lineOfSightHeightMeters() else firstGround + homeAntennaHeightMeters
            val lastAntenna = if (last.lineOfSightHeightMeters().isFinite()) last.lineOfSightHeightMeters() else lastGround + targetAntennaHeightMeters

            val x1 = plotX
            val y1Ground = plotY + plotHeight - normalizeElevation(firstGround, minElevation, elevationRange) * plotHeight
            val y1Antenna = plotY + plotHeight - normalizeElevation(firstAntenna, minElevation, elevationRange) * plotHeight

            val x2 = plotX + plotWidth
            val y2Ground = plotY + plotHeight - normalizeElevation(lastGround, minElevation, elevationRange) * plotHeight
            val y2Antenna = plotY + plotHeight - normalizeElevation(lastAntenna, minElevation, elevationRange) * plotHeight

            drawLine(endpointMarker, Offset(x1.toFloat(), y1Ground.toFloat()), Offset(x1.toFloat(), y1Antenna.toFloat()), strokeWidth = 1.2f)
            drawCircle(endpointMarker, radius = 3f, center = Offset(x1.toFloat(), y1Antenna.toFloat()))

            drawLine(endpointMarker, Offset(x2.toFloat(), y2Ground.toFloat()), Offset(x2.toFloat(), y2Antenna.toFloat()), strokeWidth = 1.2f)
            drawCircle(endpointMarker, radius = 3f, center = Offset(x2.toFloat(), y2Antenna.toFloat()))

            val homeLabel = buildEndpointLabel("Home", first.elevationMeters(), homeAntennaHeightMeters)
            val dxLabel = buildEndpointLabel("DX", last.elevationMeters(), targetAntennaHeightMeters)

            val headerLabelY = plotY - 20.0 // Adjusted for Compose text drawing top-left

            safeDrawText(textMeasurer, homeLabel, topLeft = Offset((plotX + 4.0).toFloat(), headerLabelY.toFloat()), style = textStyle.copy(color = textColor))

            val dxLabelWidth = estimateTextWidth(dxLabel)
            safeDrawText(textMeasurer, dxLabel, topLeft = Offset(max(plotX + plotWidth - dxLabelWidth - 4.0, plotX + plotWidth * 0.45).toFloat(), headerLabelY.toFloat()), style = textStyle.copy(color = textColor))
        }

        // Find Point By Sample Index
        fun findPointBySampleIndex(sampleIndex: Int): PathProfilePoint? {
            if (sampleIndex < 0) return null
            return profilePoints.find { it.sampleIndex() == sampleIndex }
        }

        // Radio Horizon Markers
        if (totalDistanceKm.isFinite() && totalDistanceKm > 0.0) {
            val homeHorizonKm = PathGeometryUtils.calculateRadioHorizonDistanceKm(homeAntennaHeightMeters, effectiveEarthRadiusFactor)
            val targetHorizonKm = PathGeometryUtils.calculateRadioHorizonDistanceKm(targetAntennaHeightMeters, effectiveEarthRadiusFactor)

            fun drawVerticalHorizonMarker(distanceKm: Double, label: String) {
                val x = plotX + normalizeDistance(distanceKm) * plotWidth
                drawLine(horizonMarker, Offset(x.toFloat(), plotY.toFloat()), Offset(x.toFloat(), (plotY + plotHeight).toFloat()), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                val fullLabel = String.format(Locale.US, "%s %.1f km", label, distanceKm)
                val labelWidth = estimateTextWidth(fullLabel)
                var labelX = x + 5.0
                if (labelX + labelWidth > plotX + plotWidth) {
                    labelX = max(plotX, x - labelWidth - 5.0)
                }
                safeDrawText(textMeasurer, fullLabel, topLeft = Offset(labelX.toFloat(), (plotY + plotHeight - 18.0).toFloat()), style = textStyle.copy(color = textColor))
            }

            if (homeHorizonKm.isFinite() && homeHorizonKm > 0.0 && homeHorizonKm < totalDistanceKm) {
                drawVerticalHorizonMarker(homeHorizonKm, "Home radio horizon")
            }
            val targetMarkerDistanceKm = totalDistanceKm - targetHorizonKm
            if (targetMarkerDistanceKm.isFinite() && targetMarkerDistanceKm > 0.0 && targetMarkerDistanceKm < totalDistanceKm) {
                drawVerticalHorizonMarker(targetMarkerDistanceKm, "DX radio horizon")
            }
        }

        // Terrain Horizon Markers
        if (horizonSummary != null && profilePoints.isNotEmpty()) {
            fun drawTerrainHorizonMarker(sampleIndex: Int, label: String) {
                val point = findPointBySampleIndex(sampleIndex) ?: return
                val x = plotX + normalizeDistance(point.distanceKm()) * plotWidth
                val y = plotY + plotHeight - normalizeElevation(terrainDisplayElevationMeters(point), minElevation, elevationRange) * plotHeight

                val path = Path()
                path.moveTo(x.toFloat(), (y - 8.0).toFloat())
                path.lineTo((x - 5.0).toFloat(), (y + 2.0).toFloat())
                path.lineTo((x + 5.0).toFloat(), (y + 2.0).toFloat())
                path.close()
                drawPath(path, color = terrainHorizonMarker, style = Fill)

                drawLine(terrainHorizonMarker, Offset(x.toFloat(), y.toFloat()), Offset(x.toFloat(), (plotY + plotHeight).toFloat()), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 5f)))

                val fullLabel = String.format(Locale.US, "%s %.1f km", label, point.distanceKm())
                val labelWidth = estimateTextWidth(fullLabel)
                var labelX = x + 7.0
                if (labelX + labelWidth > plotX + plotWidth) {
                    labelX = max(plotX, x - labelWidth - 7.0)
                }
                var labelY = min(plotY + plotHeight - 22.0, y + 18.0)
                labelY = max(plotY + 20.0, labelY)

                safeDrawText(textMeasurer, fullLabel, topLeft = Offset(labelX.toFloat(), (labelY - 10.0).toFloat()), style = textStyle.copy(color = textColor))
            }

            if (horizonSummary.hasHomeTerrainHorizon()) {
                drawTerrainHorizonMarker(horizonSummary.homeTerrainHorizonSampleIndex(), "Home terrain horizon")
            }
            if (horizonSummary.hasTargetTerrainHorizon()) {
                drawTerrainHorizonMarker(horizonSummary.targetTerrainHorizonSampleIndex(), "DX terrain horizon")
            }
        }

        // Obstruction Marker
        if (obstructionSummary != null && obstructionSummary.hasDominantLosObstruction()) {
            val point = findPointBySampleIndex(obstructionSummary.dominantObstructionSampleIndex())
            if (point != null) {
                val x = plotX + normalizeDistance(point.distanceKm()) * plotWidth
                val y = plotY + plotHeight - normalizeElevation(terrainDisplayElevationMeters(point), minElevation, elevationRange) * plotHeight

                val path = Path()
                path.moveTo(x.toFloat(), (y - 7.0).toFloat())
                path.lineTo((x - 5.0).toFloat(), y.toFloat())
                path.lineTo(x.toFloat(), (y + 7.0).toFloat())
                path.lineTo((x + 5.0).toFloat(), y.toFloat())
                path.close()
                drawPath(path, color = obstructionMarker, style = Fill)

                drawLine(obstructionMarker, Offset(x.toFloat(), y.toFloat()), Offset(x.toFloat(), (plotY + plotHeight).toFloat()), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 4f)))

                val label = String.format(Locale.US, "Diffraction candidate %.1f km, KE ≈ %.1f dB", obstructionSummary.dominantObstructionPathDistanceKm(), obstructionSummary.estimatedKnifeEdgeLossDb())
                val labelWidth = estimateTextWidth(label)
                var labelX = x + 8.0
                if (labelX + labelWidth > plotX + plotWidth) {
                    labelX = max(plotX, x - labelWidth - 8.0)
                }
                var labelY = min(plotY + plotHeight - 34.0, y + 30.0)
                labelY = max(plotY + 34.0, labelY)

                safeDrawText(textMeasurer, label, topLeft = Offset(labelX.toFloat(), (labelY - 10.0).toFloat()), style = textStyle.copy(color = textColor))
            }
        }

        // Critical Marker
        fun findCriticalPoint(): PathProfilePoint? {
            var worstFresnelPoint: PathProfilePoint? = null
            var worstFresnelIntrusion = 0.0
            var worstLosPoint: PathProfilePoint? = null
            var minimumLosClearance = Double.POSITIVE_INFINITY

            for (point in profilePoints) {
                if (point.fresnelIntrusionMeters().isFinite() && point.fresnelIntrusionMeters() > worstFresnelIntrusion) {
                    worstFresnelIntrusion = point.fresnelIntrusionMeters()
                    worstFresnelPoint = point
                }
                if (point.lineOfSightClearanceMeters().isFinite() && point.lineOfSightClearanceMeters() < minimumLosClearance) {
                    minimumLosClearance = point.lineOfSightClearanceMeters()
                    worstLosPoint = point
                }
            }
            if (worstFresnelPoint != null) return worstFresnelPoint
            if (worstLosPoint != null && worstLosPoint.isLineOfSightBlocked) return worstLosPoint
            return null
        }

        val criticalPoint = findCriticalPoint()
        if (criticalPoint != null) {
            val x = plotX + normalizeDistance(criticalPoint.distanceKm()) * plotWidth
            val y = plotY + plotHeight - normalizeElevation(terrainDisplayElevationMeters(criticalPoint), minElevation, elevationRange) * plotHeight

            drawCircle(criticalMarker, radius = 4f, center = Offset(x.toFloat(), y.toFloat()))
            drawLine(criticalMarker, Offset(x.toFloat(), y.toFloat()), Offset(x.toFloat(), (plotY + plotHeight).toFloat()), strokeWidth = 1f)

            val label = if (criticalPoint.hasFresnelIntrusion()) {
                String.format(Locale.US, "Critical point: Fresnel intrusion %.1f m @ %.1f km", criticalPoint.fresnelIntrusionMeters(), criticalPoint.distanceKm())
            } else {
                String.format(Locale.US, "Critical point: LOS clearance %.1f m @ %.1f km", criticalPoint.lineOfSightClearanceMeters(), criticalPoint.distanceKm())
            }

            val labelWidth = estimateTextWidth(label)
            var labelX = x + 8.0
            if (labelX + labelWidth > plotX + plotWidth) {
                labelX = max(plotX, x - labelWidth - 8.0)
            }
            var labelY = max(plotY + 22.0, y - 10.0)
            if (labelY < plotY + 18.0) {
                labelY = plotY + 18.0
            }

            safeDrawText(textMeasurer, label, topLeft = Offset(labelX.toFloat(), (labelY - 10.0).toFloat()), style = textStyle.copy(color = textColor))
        }

        // Draw axis labels
        drawAxisLabels(minElevation, maxElevation)

        // Draw legend
        var legX = plotX
        val legY = headerTop + 12.0
        val textY = legY - 10.0 // adjust for compose text drawing top-left vs baseline

        safeDrawText(textMeasurer, "Legend:", topLeft = Offset(legX.toFloat(), textY.toFloat()), style = textStyle.copy(color = textColor))
        legX += 48.0

        fun drawHorizontalLegendLineItem(lineColor: Color, dashed: Boolean, lx: Double, ly: Double, text: String): Double {
            val pe = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6f, 4f)) else null
            drawLine(lineColor, Offset(lx.toFloat(), (ly - 4.0).toFloat()), Offset((lx + 14.0).toFloat(), (ly - 4.0).toFloat()), strokeWidth = 1.6f, pathEffect = pe)
            safeDrawText(textMeasurer, text, topLeft = Offset((lx + 18.0).toFloat(), (ly - 10.0).toFloat()), style = textStyle.copy(color = textColor))
            return lx + 18.0 + estimateTextWidth(text) + 18.0
        }

        fun drawHorizontalLegendTriangleItem(markerColor: Color, lx: Double, ly: Double, text: String): Double {
            val path = Path()
            path.moveTo((lx + 4.0).toFloat(), (ly - 10.0).toFloat())
            path.lineTo(lx.toFloat(), (ly - 2.0).toFloat())
            path.lineTo((lx + 8.0).toFloat(), (ly - 2.0).toFloat())
            path.close()
            drawPath(path, color = markerColor, style = Fill)
            safeDrawText(textMeasurer, text, topLeft = Offset((lx + 14.0).toFloat(), (ly - 10.0).toFloat()), style = textStyle.copy(color = textColor))
            return lx + 14.0 + estimateTextWidth(text) + 18.0
        }

        fun drawHorizontalLegendDiamondItem(markerColor: Color, lx: Double, ly: Double, text: String): Double {
            val cx = lx + 4.0
            val cy = ly - 6.0
            val path = Path()
            path.moveTo(cx.toFloat(), (cy - 4.0).toFloat())
            path.lineTo((cx - 4.0).toFloat(), cy.toFloat())
            path.lineTo(cx.toFloat(), (cy + 4.0).toFloat())
            path.lineTo((cx + 4.0).toFloat(), cy.toFloat())
            path.close()
            drawPath(path, color = markerColor, style = Fill)
            safeDrawText(textMeasurer, text, topLeft = Offset((lx + 14.0).toFloat(), (ly - 10.0).toFloat()), style = textStyle.copy(color = textColor))
            return lx + 14.0 + estimateTextWidth(text) + 18.0
        }

        fun drawHorizontalLegendDotItem(markerColor: Color, lx: Double, ly: Double, text: String): Double {
            drawCircle(markerColor, radius = 4f, center = Offset((lx + 4.0).toFloat(), (ly - 4.0).toFloat()))
            safeDrawText(textMeasurer, text, topLeft = Offset((lx + 14.0).toFloat(), (ly - 10.0).toFloat()), style = textStyle.copy(color = textColor))
            return lx + 14.0 + estimateTextWidth(text) + 18.0
        }

        legX = drawHorizontalLegendLineItem(terrainLine, false, legX, legY, "Terrain")
        legX = drawHorizontalLegendLineItem(losLine, false, legX, legY, "LOS")
        legX = drawHorizontalLegendLineItem(fresnelLine, true, legX, legY, "Fresnel")
        legX = drawHorizontalLegendLineItem(horizonMarker, true, legX, legY, "Radio hor.")
        legX = drawHorizontalLegendTriangleItem(terrainHorizonMarker, legX, legY, "Terr. hor.")
        legX = drawHorizontalLegendDiamondItem(obstructionMarker, legX, legY, "Diffraction")
        drawHorizontalLegendDotItem(criticalMarker, legX, legY, "Critical")
    }
}


fun androidx.compose.ui.graphics.drawscope.DrawScope.safeDrawText(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    topLeft: androidx.compose.ui.geometry.Offset,
    style: androidx.compose.ui.text.TextStyle
) {
    if (topLeft.x.isNaN() || topLeft.y.isNaN() || topLeft.x.isInfinite() || topLeft.y.isInfinite()) return
    this.drawText(
        textMeasurer = textMeasurer,
        text = text,
        topLeft = topLeft,
        style = style,
        size = androidx.compose.ui.geometry.Size(10000f, 10000f)
    )
}
