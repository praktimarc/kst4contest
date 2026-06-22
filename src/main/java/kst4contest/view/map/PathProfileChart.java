package kst4contest.view.map;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Locale;

import java.util.function.Consumer;
import javafx.scene.input.MouseEvent;

/**
 * JavaFX canvas chart for previewing the current path profile.
 *
 * <p>The chart is intentionally explicit and self-explanatory:
 * <ul>
 *     <li>terrain profile</li>
 *     <li>direct line of sight</li>
 *     <li>1st Fresnel zone</li>
 *     <li>endpoint ground / antenna information</li>
 *     <li>critical point marker</li>
 *     <li>axes and legend</li>
 * </ul>
 *
 * <p>Important: this chart visualizes geometric/topographic path evaluation.
 * It does not claim that a blocked LOS automatically means "no connection".</p>
 */
public final class PathProfileChart extends Canvas {


    private Consumer<PathProfilePoint> onProfilePointHovered;

    private PathObstructionSummary obstructionSummary = PathObstructionSummary.empty();
    private List<PathProfilePoint> profilePoints = List.of();
    private double totalDistanceKm = Double.NaN;
    private boolean darkMode;

    private double homeAntennaHeightMeters = Double.NaN;
    private double targetAntennaHeightMeters = Double.NaN;
    private double analysisFrequencyMHz = Double.NaN;
    private double effectiveEarthRadiusFactor = PathGeometryUtils.DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR;
    private PathHorizonSummary horizonSummary = PathHorizonSummary.empty();


    public PathProfileChart() {
        setWidth(420);
        setHeight(280);

        widthProperty().addListener((obs, oldValue, newValue) -> redraw());
        heightProperty().addListener((obs, oldValue, newValue) -> redraw());

        setOnMouseMoved(this::handleMouseMoved);
        setOnMouseExited(event -> {
            if (onProfilePointHovered != null) {
                onProfilePointHovered.accept(null);
            }
        });
    }

    public void setRadioPath(double homeAntennaHeightMeters,
                             double targetAntennaHeightMeters,
                             double analysisFrequencyMHz) {
        setRadioPath(
                homeAntennaHeightMeters,
                targetAntennaHeightMeters,
                analysisFrequencyMHz,
                PathGeometryUtils.DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR
        );
    }

    public void setRadioPath(double homeAntennaHeightMeters,
                             double targetAntennaHeightMeters,
                             double analysisFrequencyMHz,
                             double effectiveEarthRadiusFactor) {
        this.homeAntennaHeightMeters = homeAntennaHeightMeters;
        this.targetAntennaHeightMeters = targetAntennaHeightMeters;
        this.analysisFrequencyMHz = analysisFrequencyMHz;
        this.effectiveEarthRadiusFactor =
                PathGeometryUtils.sanitizeEffectiveEarthRadiusFactor(effectiveEarthRadiusFactor);
        redraw();
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        redraw();
    }

    public void setProfile(List<PathProfilePoint> profilePoints, double totalDistanceKm) {
        this.profilePoints = profilePoints == null ? List.of() : List.copyOf(profilePoints);
        this.totalDistanceKm = totalDistanceKm;
        redraw();
    }

    private void redraw() {
        double width = getWidth();
        double height = getHeight();

        if (width <= 1.0 || height <= 1.0) {
            return;
        }

        GraphicsContext gc = getGraphicsContext2D();

        Color background = darkMode ? Color.rgb(34, 39, 43) : Color.rgb(248, 248, 248);
        Color border = darkMode ? Color.rgb(96, 106, 116) : Color.rgb(180, 180, 180);
        Color grid = darkMode ? Color.rgb(95, 105, 115, 0.70) : Color.rgb(185, 185, 185, 0.75);
        Color text = darkMode ? Color.rgb(230, 235, 239) : Color.rgb(40, 40, 40);

        Color terrainFill = darkMode ? Color.rgb(70, 165, 95, 0.35) : Color.rgb(90, 180, 110, 0.35);
        Color terrainLine = darkMode ? Color.rgb(95, 210, 120) : Color.rgb(45, 150, 70);

        Color losLine = darkMode ? Color.rgb(255, 182, 80) : Color.rgb(220, 130, 20);

        Color fresnelFill = darkMode ? Color.rgb(170, 135, 255, 0.16) : Color.rgb(150, 105, 240, 0.16);
        Color fresnelLine = darkMode ? Color.rgb(205, 155, 255) : Color.rgb(130, 75, 210);

        Color endpointMarker = darkMode ? Color.rgb(245, 245, 245) : Color.rgb(45, 45, 45);
        Color horizonMarker = darkMode ? Color.rgb(95, 190, 255) : Color.rgb(30, 120, 200);
        Color criticalMarker = darkMode ? Color.rgb(255, 100, 100) : Color.rgb(210, 45, 45);

        Color terrainHorizonMarker = darkMode ? Color.rgb(255, 220, 105) : Color.rgb(185, 125, 0);
        Color obstructionMarker = darkMode ? Color.rgb(255, 135, 75) : Color.rgb(225, 85, 30);

        gc.setFill(background);
        gc.fillRect(0, 0, width, height);

        gc.setStroke(border);
        gc.strokeRect(0.5, 0.5, width - 1.0, height - 1.0);

        double left = 58.0;
        double headerTop = 12.0;
        double headerHeight = 34.0;
        double endpointTextHeight = 18.0;
        double top = headerTop + headerHeight + endpointTextHeight;
        double right = 18.0;
        double bottom = 56.0;

        double plotX = left;
        double plotY = top;
        double plotWidth = Math.max(10.0, width - left - right);
        double plotHeight = Math.max(10.0, height - top - bottom);

        drawGrid(gc, grid, plotX, plotY, plotWidth, plotHeight);

        if (profilePoints.isEmpty()) {
            gc.setFill(text);
            gc.fillText("No profile samples available.", plotX, plotY + plotHeight / 2.0);
            drawAxisLabels(gc, text, plotX, plotY, plotWidth, plotHeight, 0.0, 1.0);
            return;
        }

        double minElevation = determineMinimumElevation();
        double maxElevation = determineMaximumElevation();
        double elevationRange = Math.max(1.0, maxElevation - minElevation);
        double paddingMeters = Math.max(20.0, elevationRange * 0.08);

        minElevation -= paddingMeters;
        maxElevation += paddingMeters;
        elevationRange = Math.max(1.0, maxElevation - minElevation);

        drawTerrainFill(gc, terrainFill, plotX, plotY, plotWidth, plotHeight, minElevation, elevationRange);
        drawFresnelFill(gc, fresnelFill, plotX, plotY, plotWidth, plotHeight, minElevation, elevationRange);

        drawTerrain(gc, terrainLine, plotX, plotY, plotWidth, plotHeight, minElevation, elevationRange);
        drawFresnelHull(gc, fresnelLine, plotX, plotY, plotWidth, plotHeight, minElevation, elevationRange);
        drawLosLine(gc, losLine, plotX, plotY, plotWidth, plotHeight, minElevation, elevationRange);

        drawEndpointMarkers(gc, endpointMarker, text, plotX, plotY, plotWidth, plotHeight, minElevation, elevationRange);
        drawRadioHorizonMarkers(gc, horizonMarker, text, plotX, plotY, plotWidth, plotHeight);
        drawTerrainHorizonMarkers(gc, terrainHorizonMarker, text, plotX, plotY, plotWidth, plotHeight, minElevation, elevationRange);
        drawObstructionMarker(gc, obstructionMarker, text, plotX, plotY, plotWidth, plotHeight, minElevation, elevationRange);
        drawCriticalMarker(gc, criticalMarker, text, plotX, plotY, plotWidth, plotHeight, minElevation, elevationRange);

        drawAxisLabels(gc, text, plotX, plotY, plotWidth, plotHeight, minElevation, maxElevation);
        drawLegend(gc, text, terrainLine, losLine, fresnelLine, horizonMarker, terrainHorizonMarker, obstructionMarker, criticalMarker, plotX, headerTop + 12.0, plotWidth);
    }

    private void drawGrid(GraphicsContext gc,
                          Color gridColor,
                          double plotX,
                          double plotY,
                          double plotWidth,
                          double plotHeight) {

        gc.setStroke(gridColor);
        gc.setLineWidth(1.0);
        gc.setLineDashes(null);

        for (int i = 0; i <= 4; i++) {
            double y = plotY + plotHeight * i / 4.0;
            gc.strokeLine(plotX, y, plotX + plotWidth, y);
        }

        for (int i = 0; i <= 4; i++) {
            double x = plotX + plotWidth * i / 4.0;
            gc.strokeLine(x, plotY, x, plotY + plotHeight);
        }
    }

    private double determineMinimumElevation() {
        double minElevation = Double.POSITIVE_INFINITY;

        for (PathProfilePoint point : profilePoints) {
            minElevation = Math.min(minElevation, terrainDisplayElevationMeters(point));

            if (Double.isFinite(point.lineOfSightHeightMeters())) {
                minElevation = Math.min(minElevation, point.lineOfSightHeightMeters());
            }

            if (Double.isFinite(point.fresnelUpperHeightMeters())) {
                minElevation = Math.min(minElevation, point.fresnelUpperHeightMeters());
            }

            if (Double.isFinite(point.fresnelLowerHeightMeters())) {
                minElevation = Math.min(minElevation, point.fresnelLowerHeightMeters());
            }
        }

        return Double.isInfinite(minElevation) ? 0.0 : minElevation;
    }

    private double determineMaximumElevation() {
        double maxElevation = Double.NEGATIVE_INFINITY;

        for (PathProfilePoint point : profilePoints) {
            maxElevation = Math.max(maxElevation, terrainDisplayElevationMeters(point));

            if (Double.isFinite(point.lineOfSightHeightMeters())) {
                maxElevation = Math.max(maxElevation, point.lineOfSightHeightMeters());
            }

            if (Double.isFinite(point.fresnelUpperHeightMeters())) {
                maxElevation = Math.max(maxElevation, point.fresnelUpperHeightMeters());
            }

            if (Double.isFinite(point.fresnelLowerHeightMeters())) {
                maxElevation = Math.max(maxElevation, point.fresnelLowerHeightMeters());
            }
        }

        return Double.isInfinite(maxElevation) ? 1.0 : maxElevation;
    }

    private void drawTerrainFill(GraphicsContext gc,
                                 Color fillColor,
                                 double plotX,
                                 double plotY,
                                 double plotWidth,
                                 double plotHeight,
                                 double minElevation,
                                 double elevationRange) {

        if (profilePoints.size() < 2) {
            return;
        }

        int pointCount = profilePoints.size() + 2;
        double[] xs = new double[pointCount];
        double[] ys = new double[pointCount];

        xs[0] = plotX + normalizeDistance(profilePoints.get(0).distanceKm()) * plotWidth;
        ys[0] = plotY + plotHeight;

        for (int i = 0; i < profilePoints.size(); i++) {
            PathProfilePoint point = profilePoints.get(i);
            xs[i + 1] = plotX + normalizeDistance(point.distanceKm()) * plotWidth;
            ys[i + 1] = plotY + plotHeight
                    - normalizeElevation(terrainDisplayElevationMeters(point), minElevation, elevationRange) * plotHeight;
        }

        xs[pointCount - 1] = plotX + normalizeDistance(profilePoints.get(profilePoints.size() - 1).distanceKm()) * plotWidth;
        ys[pointCount - 1] = plotY + plotHeight;

        gc.setFill(fillColor);
        gc.fillPolygon(xs, ys, pointCount);
    }

    private void drawTerrain(GraphicsContext gc,
                             Color terrainLine,
                             double plotX,
                             double plotY,
                             double plotWidth,
                             double plotHeight,
                             double minElevation,
                             double elevationRange) {

        gc.setStroke(terrainLine);
        gc.setLineWidth(2.0);
        gc.setLineDashes(null);

        for (int i = 1; i < profilePoints.size(); i++) {
            PathProfilePoint previous = profilePoints.get(i - 1);
            PathProfilePoint current = profilePoints.get(i);

            double x1 = plotX + normalizeDistance(previous.distanceKm()) * plotWidth;
            double y1 = plotY + plotHeight
                    - normalizeElevation(terrainDisplayElevationMeters(previous), minElevation, elevationRange) * plotHeight;

            double x2 = plotX + normalizeDistance(current.distanceKm()) * plotWidth;
            double y2 = plotY + plotHeight
                    - normalizeElevation(terrainDisplayElevationMeters(current), minElevation, elevationRange) * plotHeight;

            gc.strokeLine(x1, y1, x2, y2);
        }
    }

    private void drawLosLine(GraphicsContext gc,
                             Color losLine,
                             double plotX,
                             double plotY,
                             double plotWidth,
                             double plotHeight,
                             double minElevation,
                             double elevationRange) {

        gc.setStroke(losLine);
        gc.setLineWidth(1.5);
        gc.setLineDashes(null);

        if (hasEnrichedLosGeometry()) {
            for (int i = 1; i < profilePoints.size(); i++) {
                PathProfilePoint previous = profilePoints.get(i - 1);
                PathProfilePoint current = profilePoints.get(i);

                if (!Double.isFinite(previous.lineOfSightHeightMeters())
                        || !Double.isFinite(current.lineOfSightHeightMeters())) {
                    continue;
                }

                double x1 = plotX + normalizeDistance(previous.distanceKm()) * plotWidth;
                double y1 = plotY + plotHeight
                        - normalizeElevation(previous.lineOfSightHeightMeters(), minElevation, elevationRange) * plotHeight;

                double x2 = plotX + normalizeDistance(current.distanceKm()) * plotWidth;
                double y2 = plotY + plotHeight
                        - normalizeElevation(current.lineOfSightHeightMeters(), minElevation, elevationRange) * plotHeight;

                gc.strokeLine(x1, y1, x2, y2);
            }
            return;
        }

        if (profilePoints.size() < 2
                || !Double.isFinite(homeAntennaHeightMeters)
                || !Double.isFinite(targetAntennaHeightMeters)) {
            return;
        }

        double startAntennaMeters = terrainDisplayElevationMeters(profilePoints.get(0)) + homeAntennaHeightMeters;
        double endAntennaMeters = terrainDisplayElevationMeters(profilePoints.get(profilePoints.size() - 1))
                + targetAntennaHeightMeters;

        double x1 = plotX;
        double y1 = plotY + plotHeight - normalizeElevation(startAntennaMeters, minElevation, elevationRange) * plotHeight;

        double x2 = plotX + plotWidth;
        double y2 = plotY + plotHeight - normalizeElevation(endAntennaMeters, minElevation, elevationRange) * plotHeight;

        gc.strokeLine(x1, y1, x2, y2);
    }

    private void drawFresnelFill(GraphicsContext gc,
                                 Color fillColor,
                                 double plotX,
                                 double plotY,
                                 double plotWidth,
                                 double plotHeight,
                                 double minElevation,
                                 double elevationRange) {

        if (!hasEnrichedFresnelGeometry() || profilePoints.size() < 2) {
            return;
        }

        for (PathProfilePoint point : profilePoints) {
            if (!Double.isFinite(point.fresnelUpperHeightMeters())
                    || !Double.isFinite(point.fresnelLowerHeightMeters())) {
                return;
            }
        }

        int n = profilePoints.size();
        double[] xs = new double[n * 2];
        double[] ys = new double[n * 2];

        for (int i = 0; i < n; i++) {
            PathProfilePoint point = profilePoints.get(i);
            xs[i] = plotX + normalizeDistance(point.distanceKm()) * plotWidth;
            ys[i] = plotY + plotHeight
                    - normalizeElevation(point.fresnelUpperHeightMeters(), minElevation, elevationRange) * plotHeight;
        }

        for (int i = 0; i < n; i++) {
            PathProfilePoint point = profilePoints.get(n - 1 - i);
            xs[n + i] = plotX + normalizeDistance(point.distanceKm()) * plotWidth;
            ys[n + i] = plotY + plotHeight
                    - normalizeElevation(point.fresnelLowerHeightMeters(), minElevation, elevationRange) * plotHeight;
        }

        gc.setFill(fillColor);
        gc.fillPolygon(xs, ys, xs.length);
    }

    private void drawFresnelHull(GraphicsContext gc,
                                 Color fresnelLine,
                                 double plotX,
                                 double plotY,
                                 double plotWidth,
                                 double plotHeight,
                                 double minElevation,
                                 double elevationRange) {

        if (!hasEnrichedFresnelGeometry()) {
            return;
        }

        gc.setStroke(fresnelLine);
        gc.setLineWidth(1.0);
        gc.setLineDashes(6.0, 4.0);

        for (int i = 1; i < profilePoints.size(); i++) {
            PathProfilePoint previous = profilePoints.get(i - 1);
            PathProfilePoint current = profilePoints.get(i);

            if (Double.isFinite(previous.fresnelUpperHeightMeters())
                    && Double.isFinite(current.fresnelUpperHeightMeters())) {

                double x1 = plotX + normalizeDistance(previous.distanceKm()) * plotWidth;
                double y1 = plotY + plotHeight
                        - normalizeElevation(previous.fresnelUpperHeightMeters(), minElevation, elevationRange) * plotHeight;

                double x2 = plotX + normalizeDistance(current.distanceKm()) * plotWidth;
                double y2 = plotY + plotHeight
                        - normalizeElevation(current.fresnelUpperHeightMeters(), minElevation, elevationRange) * plotHeight;

                gc.strokeLine(x1, y1, x2, y2);
            }

            if (Double.isFinite(previous.fresnelLowerHeightMeters())
                    && Double.isFinite(current.fresnelLowerHeightMeters())) {

                double x1 = plotX + normalizeDistance(previous.distanceKm()) * plotWidth;
                double y1 = plotY + plotHeight
                        - normalizeElevation(previous.fresnelLowerHeightMeters(), minElevation, elevationRange) * plotHeight;

                double x2 = plotX + normalizeDistance(current.distanceKm()) * plotWidth;
                double y2 = plotY + plotHeight
                        - normalizeElevation(current.fresnelLowerHeightMeters(), minElevation, elevationRange) * plotHeight;

                gc.strokeLine(x1, y1, x2, y2);
            }
        }

        gc.setLineDashes(null);
    }

    private void drawEndpointMarkers(GraphicsContext gc,
                                     Color markerColor,
                                     Color textColor,
                                     double plotX,
                                     double plotY,
                                     double plotWidth,
                                     double plotHeight,
                                     double minElevation,
                                     double elevationRange) {

        if (profilePoints.size() < 2) {
            return;
        }

        PathProfilePoint first = profilePoints.get(0);
        PathProfilePoint last = profilePoints.get(profilePoints.size() - 1);

        double firstGround = terrainDisplayElevationMeters(first);
        double lastGround = terrainDisplayElevationMeters(last);

        double firstAntenna = Double.isFinite(first.lineOfSightHeightMeters())
                ? first.lineOfSightHeightMeters()
                : firstGround + homeAntennaHeightMeters;

        double lastAntenna = Double.isFinite(last.lineOfSightHeightMeters())
                ? last.lineOfSightHeightMeters()
                : lastGround + targetAntennaHeightMeters;

        double x1 = plotX;
        double y1Ground = plotY + plotHeight - normalizeElevation(firstGround, minElevation, elevationRange) * plotHeight;
        double y1Antenna = plotY + plotHeight - normalizeElevation(firstAntenna, minElevation, elevationRange) * plotHeight;

        double x2 = plotX + plotWidth;
        double y2Ground = plotY + plotHeight - normalizeElevation(lastGround, minElevation, elevationRange) * plotHeight;
        double y2Antenna = plotY + plotHeight - normalizeElevation(lastAntenna, minElevation, elevationRange) * plotHeight;

        gc.setStroke(markerColor);
        gc.setFill(markerColor);
        gc.setLineWidth(1.2);

        gc.strokeLine(x1, y1Ground, x1, y1Antenna);
        gc.fillOval(x1 - 3.0, y1Antenna - 3.0, 6.0, 6.0);

        gc.strokeLine(x2, y2Ground, x2, y2Antenna);
        gc.fillOval(x2 - 3.0, y2Antenna - 3.0, 6.0, 6.0);

        gc.setFill(textColor);

        String homeLabel = buildEndpointLabel("Home", first.elevationMeters(), homeAntennaHeightMeters);
        String dxLabel = buildEndpointLabel("DX", last.elevationMeters(), targetAntennaHeightMeters);

        double headerLabelY = plotY - 10.0;

        gc.fillText(homeLabel, plotX + 4.0, headerLabelY);

        double dxLabelWidth = estimateTextWidth(dxLabel);
        gc.fillText(
                dxLabel,
                Math.max(plotX + plotWidth - dxLabelWidth - 4.0, plotX + plotWidth * 0.45),
                headerLabelY
        );
    }

    private String buildEndpointLabel(String name, double groundMetersAsl, double antennaHeightMetersAgl) {
        boolean hasGround = Double.isFinite(groundMetersAsl);
        boolean hasAntenna = Double.isFinite(antennaHeightMetersAgl);

        if (hasGround && hasAntenna) {
            return String.format(Locale.US, "%s: %.0f m ASL + %.0f m AGL", name, groundMetersAsl, antennaHeightMetersAgl);
        }

        if (hasGround) {
            return String.format(Locale.US, "%s: %.0f m ASL", name, groundMetersAsl);
        }

        if (hasAntenna) {
            return String.format(Locale.US, "%s: +%.0f m AGL", name, antennaHeightMetersAgl);
        }

        return name + ": -";
    }

    private void drawObstructionMarker(GraphicsContext gc,
                                       Color markerColor,
                                       Color textColor,
                                       double plotX,
                                       double plotY,
                                       double plotWidth,
                                       double plotHeight,
                                       double minElevation,
                                       double elevationRange) {

        if (obstructionSummary == null || !obstructionSummary.hasDominantLosObstruction()) {
            return;
        }

        PathProfilePoint point = findPointBySampleIndex(obstructionSummary.dominantObstructionSampleIndex());
        if (point == null) {
            return;
        }

        double x = plotX + normalizeDistance(point.distanceKm()) * plotWidth;
        double y = plotY + plotHeight
                - normalizeElevation(terrainDisplayElevationMeters(point), minElevation, elevationRange) * plotHeight;

        gc.setFill(markerColor);

        double[] diamondX = {x, x - 5.0, x, x + 5.0};
        double[] diamondY = {y - 7.0, y, y + 7.0, y};
        gc.fillPolygon(diamondX, diamondY, 4);

        gc.setStroke(markerColor);
        gc.setLineWidth(1.0);
        gc.setLineDashes(2.0, 4.0);
        gc.strokeLine(x, y, x, plotY + plotHeight);
        gc.setLineDashes(null);

        String label = String.format(
                Locale.US,
                "Diffraction candidate %.1f km, KE ≈ %.1f dB",
                obstructionSummary.dominantObstructionPathDistanceKm(),
                obstructionSummary.estimatedKnifeEdgeLossDb()
        );

        double labelWidth = estimateTextWidth(label);
        double labelX = x + 8.0;

        if (labelX + labelWidth > plotX + plotWidth) {
            labelX = Math.max(plotX, x - labelWidth - 8.0);
        }

        double labelY = Math.min(plotY + plotHeight - 34.0, y + 30.0);
        labelY = Math.max(plotY + 34.0, labelY);

        gc.setFill(textColor);
        gc.fillText(label, labelX, labelY);
    }

    private double drawHorizontalLegendDiamondItem(GraphicsContext gc,
                                                   Color textColor,
                                                   Color markerColor,
                                                   double x,
                                                   double y,
                                                   String text) {

        gc.setFill(markerColor);

        double centerX = x + 4.0;
        double centerY = y - 6.0;

        double[] diamondX = {centerX, centerX - 4.0, centerX, centerX + 4.0};
        double[] diamondY = {centerY - 4.0, centerY, centerY + 4.0, centerY};
        gc.fillPolygon(diamondX, diamondY, 4);

        gc.setFill(textColor);
        gc.fillText(text, x + 14.0, y);

        return x + 14.0 + estimateTextWidth(text) + 18.0;
    }

    private void drawCriticalMarker(GraphicsContext gc,
                                    Color markerColor,
                                    Color textColor,
                                    double plotX,
                                    double plotY,
                                    double plotWidth,
                                    double plotHeight,
                                    double minElevation,
                                    double elevationRange) {

        PathProfilePoint criticalPoint = findCriticalPoint();
        if (criticalPoint == null) {
            return;
        }

        double x = plotX + normalizeDistance(criticalPoint.distanceKm()) * plotWidth;
        double y = plotY + plotHeight
                - normalizeElevation(terrainDisplayElevationMeters(criticalPoint), minElevation, elevationRange) * plotHeight;

        gc.setFill(markerColor);
        gc.fillOval(x - 4.0, y - 4.0, 8.0, 8.0);

        gc.setStroke(markerColor);
        gc.setLineWidth(1.0);
        gc.strokeLine(x, y, x, plotY + plotHeight);

        gc.setFill(textColor);

        String label;
        if (criticalPoint.hasFresnelIntrusion()) {
            label = String.format(
                    Locale.US,
                    "Critical point: Fresnel intrusion %.1f m @ %.1f km",
                    criticalPoint.fresnelIntrusionMeters(),
                    criticalPoint.distanceKm()
            );
        } else {
            label = String.format(
                    Locale.US,
                    "Critical point: LOS clearance %.1f m @ %.1f km",
                    criticalPoint.lineOfSightClearanceMeters(),
                    criticalPoint.distanceKm()
            );
        }

        double labelWidth = estimateTextWidth(label);
        double labelX = x + 8.0;
        if (labelX + labelWidth > plotX + plotWidth) {
            labelX = Math.max(plotX, x - labelWidth - 8.0);
        }

        double labelY = Math.max(plotY + 22.0, y - 10.0);

        if (labelY < plotY + 18.0) {
            labelY = plotY + 18.0;
        }

        gc.fillText(label, labelX, labelY);
    }

    private PathProfilePoint findCriticalPoint() {
        PathProfilePoint worstFresnelPoint = null;
        double worstFresnelIntrusion = 0.0;

        PathProfilePoint worstLosPoint = null;
        double minimumLosClearance = Double.POSITIVE_INFINITY;

        for (PathProfilePoint point : profilePoints) {
            if (Double.isFinite(point.fresnelIntrusionMeters())
                    && point.fresnelIntrusionMeters() > worstFresnelIntrusion) {
                worstFresnelIntrusion = point.fresnelIntrusionMeters();
                worstFresnelPoint = point;
            }

            if (Double.isFinite(point.lineOfSightClearanceMeters())
                    && point.lineOfSightClearanceMeters() < minimumLosClearance) {
                minimumLosClearance = point.lineOfSightClearanceMeters();
                worstLosPoint = point;
            }
        }

        if (worstFresnelPoint != null) {
            return worstFresnelPoint;
        }

        if (worstLosPoint != null && worstLosPoint.isLineOfSightBlocked()) {
            return worstLosPoint;
        }

        return null;
    }

    private void drawTerrainHorizonMarkers(GraphicsContext gc,
                                           Color markerColor,
                                           Color textColor,
                                           double plotX,
                                           double plotY,
                                           double plotWidth,
                                           double plotHeight,
                                           double minElevation,
                                           double elevationRange) {

        if (horizonSummary == null || profilePoints.isEmpty()) {
            return;
        }

        if (horizonSummary.hasHomeTerrainHorizon()) {
            drawTerrainHorizonMarker(
                    gc,
                    markerColor,
                    textColor,
                    plotX,
                    plotY,
                    plotWidth,
                    plotHeight,
                    minElevation,
                    elevationRange,
                    horizonSummary.homeTerrainHorizonSampleIndex(),
                    "Home terrain horizon"
            );
        }

        if (horizonSummary.hasTargetTerrainHorizon()) {
            drawTerrainHorizonMarker(
                    gc,
                    markerColor,
                    textColor,
                    plotX,
                    plotY,
                    plotWidth,
                    plotHeight,
                    minElevation,
                    elevationRange,
                    horizonSummary.targetTerrainHorizonSampleIndex(),
                    "DX terrain horizon"
            );
        }
    }

    private void drawTerrainHorizonMarker(GraphicsContext gc,
                                          Color markerColor,
                                          Color textColor,
                                          double plotX,
                                          double plotY,
                                          double plotWidth,
                                          double plotHeight,
                                          double minElevation,
                                          double elevationRange,
                                          int sampleIndex,
                                          String label) {

        PathProfilePoint point = findPointBySampleIndex(sampleIndex);
        if (point == null) {
            return;
        }

        double x = plotX + normalizeDistance(point.distanceKm()) * plotWidth;
        double y = plotY + plotHeight
                - normalizeElevation(terrainDisplayElevationMeters(point), minElevation, elevationRange) * plotHeight;

        gc.setFill(markerColor);

        double[] triangleX = {x, x - 5.0, x + 5.0};
        double[] triangleY = {y - 8.0, y + 2.0, y + 2.0};
        gc.fillPolygon(triangleX, triangleY, 3);

        gc.setStroke(markerColor);
        gc.setLineWidth(1.0);
        gc.setLineDashes(3.0, 5.0);
        gc.strokeLine(x, y, x, plotY + plotHeight);
        gc.setLineDashes(null);

        String fullLabel = String.format(
                Locale.US,
                "%s %.1f km",
                label,
                point.distanceKm()
        );

        double labelWidth = estimateTextWidth(fullLabel);
        double labelX = x + 7.0;

        if (labelX + labelWidth > plotX + plotWidth) {
            labelX = Math.max(plotX, x - labelWidth - 7.0);
        }

        double labelY = Math.min(plotY + plotHeight - 22.0, y + 18.0);
        labelY = Math.max(plotY + 20.0, labelY);

        gc.setFill(textColor);
        gc.fillText(fullLabel, labelX, labelY);
    }

    private PathProfilePoint findPointBySampleIndex(int sampleIndex) {
        if (sampleIndex < 0) {
            return null;
        }

        for (PathProfilePoint point : profilePoints) {
            if (point.sampleIndex() == sampleIndex) {
                return point;
            }
        }

        return null;
    }


    private void drawAxisLabels(GraphicsContext gc,
                                Color textColor,
                                double plotX,
                                double plotY,
                                double plotWidth,
                                double plotHeight,
                                double minElevation,
                                double maxElevation) {

        gc.setFill(textColor);

        for (int i = 0; i <= 4; i++) {
            double fraction = (double) i / 4.0;
            double y = plotY + plotHeight - fraction * plotHeight;
            double value = minElevation + fraction * (maxElevation - minElevation);

            gc.fillText(String.format(Locale.US, "%.0f", value), 8.0, y + 4.0);
        }

        double[] xFractions = {0.0, 0.25, 0.50, 0.75, 1.0};
        for (double xFraction : xFractions) {
            double x = plotX + xFraction * plotWidth;
            double distance = Double.isFinite(totalDistanceKm) ? totalDistanceKm * xFraction : 0.0;
            gc.fillText(String.format(Locale.US, "%.0f", distance), x - 8.0, plotY + plotHeight + 16.0);
        }

        gc.fillText("Height [m]", plotX, plotY - 4.0);
        gc.fillText("Distance [km]", plotX + plotWidth / 2.0 - 30.0, plotY + plotHeight + 34.0);

        if (Double.isFinite(analysisFrequencyMHz) && analysisFrequencyMHz > 0.0) {
            String freqText = String.format(Locale.US, "f = %.3f MHz", analysisFrequencyMHz);
            gc.fillText(freqText, plotX + plotWidth - estimateTextWidth(freqText), plotY + plotHeight + 34.0);
        }
    }

    private void drawLegend(GraphicsContext gc,
                            Color textColor,
                            Color terrainLine,
                            Color losLine,
                            Color fresnelLine,
                            Color horizonMarker,
                            Color terrainHorizonMarker,
                            Color obstructionMarker,
                            Color criticalMarker,
                            double plotX,
                            double legendBaselineY,
                            double plotWidth) {

        double x = plotX;
        double y = legendBaselineY;

        gc.setFill(textColor);
        gc.fillText("Legend:", x, y);
        x += 48.0;

        x = drawHorizontalLegendLineItem(gc, textColor, terrainLine, false, x, y, "Terrain");
        x = drawHorizontalLegendLineItem(gc, textColor, losLine, false, x, y, "LOS");
        x = drawHorizontalLegendLineItem(gc, textColor, fresnelLine, true, x, y, "Fresnel");
        x = drawHorizontalLegendLineItem(gc, textColor, horizonMarker, true, x, y, "Radio hor.");
        x = drawHorizontalLegendTriangleItem(gc, textColor, terrainHorizonMarker, x, y, "Terr. hor.");
        x = drawHorizontalLegendDiamondItem(gc, textColor, obstructionMarker, x, y, "Diffraction");

        drawHorizontalLegendDotItem(gc, textColor, criticalMarker, x, y, "Critical");
    }


    private double drawHorizontalLegendTriangleItem(GraphicsContext gc,
                                                    Color textColor,
                                                    Color markerColor,
                                                    double x,
                                                    double y,
                                                    String text) {

        gc.setFill(markerColor);

        double[] triangleX = {x + 4.0, x, x + 8.0};
        double[] triangleY = {y - 10.0, y - 2.0, y - 2.0};
        gc.fillPolygon(triangleX, triangleY, 3);

        gc.setFill(textColor);
        gc.fillText(text, x + 14.0, y);

        return x + 14.0 + estimateTextWidth(text) + 18.0;
    }

    private double drawHorizontalLegendLineItem(GraphicsContext gc,
                                                Color textColor,
                                                Color lineColor,
                                                boolean dashed,
                                                double x,
                                                double y,
                                                String text) {

        gc.setStroke(lineColor);
        gc.setLineWidth(1.6);
        gc.setLineDashes(dashed ? new double[]{6.0, 4.0} : null);
        gc.strokeLine(x, y - 4.0, x + 14.0, y - 4.0);
        gc.setLineDashes(null);

        gc.setFill(textColor);
        gc.fillText(text, x + 18.0, y);

        return x + 18.0 + estimateTextWidth(text) + 18.0;
    }

    private double drawHorizontalLegendDotItem(GraphicsContext gc,
                                               Color textColor,
                                               Color dotColor,
                                               double x,
                                               double y,
                                               String text) {

        gc.setFill(dotColor);
        gc.fillOval(x, y - 8.0, 8.0, 8.0);

        gc.setFill(textColor);
        gc.fillText(text, x + 14.0, y);

        return x + 14.0 + estimateTextWidth(text) + 18.0;
    }

    private void drawLegendLine(GraphicsContext gc,
                                Color color,
                                boolean dashed,
                                double x,
                                double y,
                                String text) {

        gc.setStroke(color);
        gc.setLineWidth(1.6);
        gc.setLineDashes(dashed ? new double[]{6.0, 4.0} : null);
        gc.strokeLine(x, y - 4.0, x + 10.0, y - 4.0);
        gc.setLineDashes(null);

        gc.setFill(darkMode ? Color.rgb(230, 235, 239) : Color.rgb(40, 40, 40));
        gc.fillText(text, x + 14.0, y);
    }

    private boolean hasEnrichedLosGeometry() {
        return profilePoints.stream().anyMatch(point -> Double.isFinite(point.lineOfSightHeightMeters()));
    }

    private boolean hasEnrichedFresnelGeometry() {
        return profilePoints.stream().anyMatch(point ->
                Double.isFinite(point.fresnelUpperHeightMeters())
                        || Double.isFinite(point.fresnelLowerHeightMeters()));
    }

    private double terrainDisplayElevationMeters(PathProfilePoint point) {
        if (point == null) {
            return Double.NaN;
        }

        if (Double.isFinite(point.curvatureAdjustedElevationMeters())) {
            return point.curvatureAdjustedElevationMeters();
        }

        return PathGeometryUtils.calculateCurvatureAdjustedElevationMeters(point, totalDistanceKm);
    }

    private double normalizeDistance(double distanceKm) {
        if (!Double.isFinite(totalDistanceKm) || totalDistanceKm <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, distanceKm / totalDistanceKm));
    }

    private double normalizeElevation(double elevationMeters, double minElevation, double elevationRange) {
        return Math.max(0.0, Math.min(1.0, (elevationMeters - minElevation) / elevationRange));
    }

    private double estimateTextWidth(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }
        return text.length() * 6.2;
    }

    private void drawRadioHorizonMarkers(GraphicsContext gc,
                                         Color markerColor,
                                         Color textColor,
                                         double plotX,
                                         double plotY,
                                         double plotWidth,
                                         double plotHeight) {

        if (!Double.isFinite(totalDistanceKm) || totalDistanceKm <= 0.0) {
            return;
        }

        double homeHorizonKm = PathGeometryUtils.calculateRadioHorizonDistanceKm(
                homeAntennaHeightMeters,
                effectiveEarthRadiusFactor
        );

        double targetHorizonKm = PathGeometryUtils.calculateRadioHorizonDistanceKm(
                targetAntennaHeightMeters,
                effectiveEarthRadiusFactor
        );

        if (Double.isFinite(homeHorizonKm)
                && homeHorizonKm > 0.0
                && homeHorizonKm < totalDistanceKm) {

            drawVerticalHorizonMarker(
                    gc,
                    markerColor,
                    textColor,
                    plotX,
                    plotY,
                    plotWidth,
                    plotHeight,
                    homeHorizonKm,
                    "Home radio horizon"
            );
        }

        double targetMarkerDistanceKm = totalDistanceKm - targetHorizonKm;

        if (Double.isFinite(targetMarkerDistanceKm)
                && targetMarkerDistanceKm > 0.0
                && targetMarkerDistanceKm < totalDistanceKm) {

            drawVerticalHorizonMarker(
                    gc,
                    markerColor,
                    textColor,
                    plotX,
                    plotY,
                    plotWidth,
                    plotHeight,
                    targetMarkerDistanceKm,
                    "DX radio horizon"
            );
        }
    }

    private void drawVerticalHorizonMarker(GraphicsContext gc,
                                           Color markerColor,
                                           Color textColor,
                                           double plotX,
                                           double plotY,
                                           double plotWidth,
                                           double plotHeight,
                                           double distanceKm,
                                           String label) {

        double x = plotX + normalizeDistance(distanceKm) * plotWidth;

        gc.setStroke(markerColor);
        gc.setLineWidth(1.0);
        gc.setLineDashes(4.0, 4.0);
        gc.strokeLine(x, plotY, x, plotY + plotHeight);
        gc.setLineDashes(null);

        gc.setFill(textColor);

        String fullLabel = String.format(
                Locale.US,
                "%s %.1f km",
                label,
                distanceKm
        );

        double labelWidth = estimateTextWidth(fullLabel);
        double labelX = x + 5.0;

        if (labelX + labelWidth > plotX + plotWidth) {
            labelX = Math.max(plotX, x - labelWidth - 5.0);
        }

        gc.fillText(fullLabel, labelX, plotY + plotHeight - 8.0);
    }

    public void setHorizonSummary(PathHorizonSummary horizonSummary) {
        this.horizonSummary = horizonSummary == null ? PathHorizonSummary.empty() : horizonSummary;
        redraw();
    }

    public void setObstructionSummary(PathObstructionSummary obstructionSummary) {
        this.obstructionSummary = obstructionSummary == null
                ? PathObstructionSummary.empty()
                : obstructionSummary;
        redraw();
    }

    public void setOnProfilePointHovered(Consumer<PathProfilePoint> onProfilePointHovered) {
        this.onProfilePointHovered = onProfilePointHovered;
    }

    private void handleMouseMoved(MouseEvent event) {
        if (onProfilePointHovered == null || profilePoints.isEmpty()) {
            return;
        }

        double width = getWidth();

        double left = 58.0;
        double right = 18.0;
        double plotWidth = Math.max(10.0, width - left - right);

        double x = event.getX();

        if (x < left || x > left + plotWidth) {
            onProfilePointHovered.accept(null);
            return;
        }

        double normalizedDistance = Math.max(0.0, Math.min(1.0, (x - left) / plotWidth));
        double targetDistanceKm = Double.isFinite(totalDistanceKm)
                ? normalizedDistance * totalDistanceKm
                : Double.NaN;

        PathProfilePoint nearestPoint = findNearestProfilePoint(targetDistanceKm);
        onProfilePointHovered.accept(nearestPoint);
    }

    private PathProfilePoint findNearestProfilePoint(double targetDistanceKm) {
        if (!Double.isFinite(targetDistanceKm) || profilePoints.isEmpty()) {
            return null;
        }

        PathProfilePoint nearestPoint = null;
        double bestDistanceDelta = Double.POSITIVE_INFINITY;

        for (PathProfilePoint point : profilePoints) {
            double delta = Math.abs(point.distanceKm() - targetDistanceKm);

            if (delta < bestDistanceDelta) {
                bestDistanceDelta = delta;
                nearestPoint = point;
            }
        }

        return nearestPoint;
    }
}