package kst4contest.model;

/**
 * Represents a scheduled event or an AirScout opportunity in the future.
 * Used for the Timeline View and Priority Calculation.
 */
public class ContestSked {

    private String targetCallsign;
    private double targetAzimuth; // Required for Antenna-Visuals
    private long skedTimeEpoch;   // The peak time (e.g., AP)
    private Band band;
    // Opportunity potential (0..100). -1 means "unknown".
    int opportunityPotentialPercent = -1;

    // Status flags to prevent spamming alarms
    private boolean warning3MinSent = false;
    private boolean warningNowSent = false;

    public ContestSked(String call, double azimuth, long time, Band b) {
        this.targetCallsign = call;
        this.targetAzimuth = azimuth;
        this.skedTimeEpoch = time;
        this.band = b;
    }

    /**
     * Returns the seconds remaining until the event.
     * Negative values mean the event is in the past.
     */
    public long getTimeUntilSkedSeconds() {
        return (skedTimeEpoch - System.currentTimeMillis()) / 1000;
    }

    // Getters and Setters...
    public String getTargetCallsign() { return targetCallsign; }
    public double getTargetAzimuth() { return targetAzimuth; }
    public long getSkedTimeEpoch() { return skedTimeEpoch; }
    public Band getBand() { return band; }
    public boolean isWarning3MinSent() { return warning3MinSent; }
    public void setWarning3MinSent(boolean b) { this.warning3MinSent = b; }
    public boolean isWarningNowSent() { return warningNowSent; }
    public void setWarningNowSent(boolean b) { this.warningNowSent = b; }

    public int getOpportunityPotentialPercent() {
        return opportunityPotentialPercent;
    }

    public void setOpportunityPotentialPercent(int opportunityPotentialPercent) {
        this.opportunityPotentialPercent = opportunityPotentialPercent;
    }
}