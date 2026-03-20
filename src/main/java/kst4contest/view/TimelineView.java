package kst4contest.view;

import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.scene.Node;
import javafx.scene.Group;
import javafx.scene.text.Font;
import kst4contest.model.ContestSked;
import kst4contest.model.ChatCategory;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A custom UI Component that visualizes future events (Skeds/AP).
 * It changes opacity based on the current antenna direction.
 *
 * Extended:
 * - Can also render "priority candidates" (ScoreService top list) with time base = next AirScout airplane minute.
 * - Clicking a candidate triggers a callback (selection + /cq preparation happens in Kst4ContestApplication).
 */
public class TimelineView extends Pane {

    private double currentAntennaAzimuth = 0;
    private double beamWidth = 50.0; // TODO: from Prefs (later)
    private final long PREVIEW_TIME_MS = 30 * 60 * 1000; // 30 Minutes Preview
    double margin = 30; // enough space for the callsign label

    private Function<ContestSked, String> skedTooltipExtraTextProvider; //used for further info in sked tooltip


    private Consumer<CandidateEvent> onCandidateClicked;

    public TimelineView() {
            this.setPrefHeight(40);
            this.setStyle("-fx-background-color: #2b2b2b;");
            // weitere init defaults, falls du welche hattest
        }

        /**
         * Backward compatibility: if some code still calls the old ctor.
         * Potential/tooltip are not view-wide properties; they belong to markers/events.
         */
        @Deprecated
        public TimelineView(int opportunityPotentialPercent, String tooltipText) {
            this(); // ignore args on purpose
        }

//
//    public int getOpportunityPotentialPercent() {
//        return opportunityPotentialPercent;
//    }

    public void setCurrentAntennaAzimuth(double az) {
        this.currentAntennaAzimuth = az;
    }

    public long getPreviewTimeMs() {
        return PREVIEW_TIME_MS;
    }

    public void setOnCandidateClicked(Consumer<CandidateEvent> handler) {
        this.onCandidateClicked = handler;
    }

    /** Backward compatible call (Skeds only) */
    public void updateVisuals(List<ContestSked> skeds) {
        updateVisuals(skeds, Collections.emptyList());
    }

    /**
     * Redraws the timeline based on the list of active skeds AND priority candidates.
     */
    public void updateVisuals(List<ContestSked> skeds, List<CandidateEvent> candidates) {
        this.getChildren().clear();

        double width = this.getWidth();
        if (width <= 5) {
            // Layout not ready yet; will be updated by caller later (uiPulse/list change)
            return;
        }

        // Draw Axis
        double axisY = 30;
        Line axis = new Line(0, axisY, width, axisY);
        axis.setStroke(Color.GRAY);
        this.getChildren().add(axis);

        long now = System.currentTimeMillis();

        // 1) Draw Priority Candidates (upper lanes)
        for (CandidateEvent ev : candidates) {
            long timeDiff = ev.getTimeUntilMs();
            if (timeDiff < 0 || timeDiff > PREVIEW_TIME_MS) continue;

            double percent = (double) timeDiff / PREVIEW_TIME_MS;
            double xPos = percent * width;
            xPos = Math.max(margin, Math.min(this.getWidth() - margin, xPos)); //starting point of the diagram


            Node marker = createCandidateMarker(ev);

            applyAntennaEffect(marker, ev.getTargetAzimuth());

            // Upper lanes so they don't overlap skeds
            double laneBaseY = 2;
            double laneOffsetY = 14.0 * ev.getLaneIndex();

            marker.setLayoutX(xPos);
            marker.setLayoutY(laneBaseY + laneOffsetY);

            this.getChildren().add(marker);
        }

        // 2) Draw Skeds (lower lane)
        for (ContestSked sked : skeds) {
            long timeDiff = sked.getSkedTimeEpoch() - now;

            // Only draw if within the next 30 mins
            if (timeDiff >= 0 && timeDiff <= PREVIEW_TIME_MS) {

                double percent = (double) timeDiff / PREVIEW_TIME_MS;
                double xPos = percent * width;
                xPos = clamp(xPos, 10, width - 10);

                Node marker = createSkedMarker(sked);

                applyAntennaEffect(marker, sked.getTargetAzimuth());

                marker.setLayoutX(xPos);
                marker.setLayoutY(axisY - 18); // below candidate lanes, near axis
                this.getChildren().add(marker);
            }
        }
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Logic:
     * If Antenna is ON TARGET -> Bright & Glowing.
     * If Antenna is OFF TARGET -> Transparent (Ghost).
     */
    private void applyAntennaEffect(Node marker, double targetAz) {

        // invalid azimuth -> keep readable
        if (!Double.isFinite(targetAz) || targetAz < 0) {
            return;
        }

        double delta = Math.abs(currentAntennaAzimuth - targetAz);
        if (delta > 180) delta = 360 - delta;

        final boolean onTarget = delta <= (beamWidth / 2.0);
        final boolean inBeam = delta <= beamWidth;

        // Rule: only fade when we are clearly NOT pointing there
        final double iconOpacity = inBeam ? 1.0 : 0.30;

        if (marker instanceof Group g) {
            // Never fade the whole group -> text stays readable
            g.setOpacity(1.0);

            for (Node child : g.getChildren()) {
                if (child instanceof Label) {
                    child.setOpacity(1.0);
                } else {
                    child.setOpacity(iconOpacity);
                }
            }

            // Add glow only if well centered (optional)
            if (onTarget) {
                g.setEffect(new DropShadow(10, Color.LIMEGREEN));
                g.setScaleX(1.10);
                g.setScaleY(1.10);
            } else {
                g.setEffect(null);
                g.setScaleX(1.0);
                g.setScaleY(1.0);
            }
            return;
        }

        // fallback
        marker.setOpacity(iconOpacity);
        marker.setEffect(onTarget ? new DropShadow(10, Color.LIMEGREEN) : null);
    }

    public void setSkedTooltipExtraTextProvider(Function<ContestSked, String> provider) {
        this.skedTooltipExtraTextProvider = provider;
    }


    /** Existing marker for Skeds (diamond + label) */
    private Node createSkedMarker(ContestSked sked) {
        Polygon diamond = new Polygon(0.0, 0.0, 6.0, 6.0, 0.0, 12.0, -6.0, 6.0);

        diamond.setFill(colorForPotential(sked.getOpportunityPotentialPercent()));

        String baseToolTipFallBack = sked.getTargetCallsign() + " (" + sked.getBand() + ")\nAz: " + sked.getTargetAzimuth();

        if (skedTooltipExtraTextProvider != null) {
            String extra = skedTooltipExtraTextProvider.apply(sked);
            if (extra != null && !extra.isBlank()) {
                baseToolTipFallBack += "\n" + extra;
            }
        }

        Tooltip t = new Tooltip(baseToolTipFallBack);
        Tooltip.install(diamond, t);

        Label lbl = new Label("SKED: " + sked.getTargetCallsign());
//        lbl.setFont(new Font(9));
//        lbl.setTextFill(Color.WHITE);
        lbl.setLayoutY(14);
        lbl.setLayoutX(-10);

        lbl.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-color: rgba(0,0,0,0.65);" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 1 4 1 4;"
        );
        lbl.setEffect(new DropShadow(2, Color.BLACK));


        return new Group(diamond, lbl);
    }

    /**
     * Give me a color for a given potencial of a AP reflection
     *
     * @param p AS potencial
     * @return
     */
    private Color colorForPotential(int p) {
        if (p >= 95) return Color.MAGENTA; // ~100%
        if (p >= 75) return Color.RED;
        if (p >= 50) return Color.YELLOW;
        return Color.DEEPSKYBLUE; // low potential
    }

    /** New marker for Priority Candidates (triangle + label) */
    private Node createCandidateMarker(CandidateEvent ev) {

        // Color derived from airplane potential (not urgency)
        Color markerColor = colorForPotential(ev.getOpportunityPotentialPercent());

        // small triangle marker (points downwards)
        Polygon tri = new Polygon(-6.0, 0.0, 6.0, 0.0, 0.0, 10.0);
        tri.setFill(markerColor);

        // Optional: small dot behind triangle (makes it easier to see)
        Circle dot = new Circle(4, markerColor);
        dot.setLayoutY(5); // center behind triangle

        Label lbl = new Label(ev.getDisplayCallSign());
        lbl.setFont(new Font(9));
        lbl.setTextFill(Color.WHITE);
        lbl.setLayoutY(10);
        lbl.setLayoutX(-12);

        lbl.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-color: rgba(0,0,0,0.65);" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 1 4 1 4;"
        );
        lbl.setEffect(new DropShadow(8, Color.BLACK));

        // IMPORTANT: include dot + triangle + label in the Group
        Group g = new Group(dot, tri, lbl);

        if (ev.getTooltipText() != null && !ev.getTooltipText().isBlank()) {
            Tooltip.install(g, new Tooltip(ev.getTooltipText()));
        }

        g.setOnMouseClicked(e -> {
            if (onCandidateClicked != null) {
                onCandidateClicked.accept(ev);
            }
        });

        return g;
    }



    /**
     * Data object rendered by the Timeline ("priority candidate").
     * Created in Kst4ContestApplication from ScoreService TopCandidates + AirScout next-AP minute.
     */
    public static class CandidateEvent {
        private final String callSignRaw;
        private final String displayCallSign;
        private final ChatCategory preferredChatCategory;

        private final long timeUntilMs;
        private final int minuteBucket;
        private final int laneIndex;

        private final double targetAzimuth;
        private final double score;

        private final String tooltipText;

        private final int opportunityPotentialPercent;


        public CandidateEvent(
                String callSignRaw,
                String displayCallSign,
                ChatCategory preferredChatCategory,
                long timeUntilMs,
                int minuteBucket,
                int laneIndex,
                double targetAzimuth,
                double score,
                int opportunityPotentialPercent,
                String tooltipText
        ) {
            this.callSignRaw = callSignRaw;
            this.displayCallSign = displayCallSign;
            this.preferredChatCategory = preferredChatCategory;
            this.timeUntilMs = timeUntilMs;
            this.minuteBucket = minuteBucket;
            this.laneIndex = laneIndex;
            this.targetAzimuth = targetAzimuth;
            this.score = score;
            this.opportunityPotentialPercent = opportunityPotentialPercent;
            this.tooltipText = tooltipText;
        }

        public String getCallSignRaw() { return callSignRaw; }
        public String getDisplayCallSign() { return displayCallSign; }
        public ChatCategory getPreferredChatCategory() { return preferredChatCategory; }

        public long getTimeUntilMs() { return timeUntilMs; }
        public int getMinuteBucket() { return minuteBucket; }
        public int getLaneIndex() { return laneIndex; }

        public double getTargetAzimuth() { return targetAzimuth; }
        public double getScore() { return score; }

        public String getTooltipText() { return tooltipText; }
        public int getOpportunityPotentialPercent() { return opportunityPotentialPercent; }

    }

    public void setBeamWidthDeg(double beamWidthDeg) {
        if (beamWidthDeg > 0 && Double.isFinite(beamWidthDeg)) {
            this.beamWidth = beamWidthDeg;
        }
    }

}
