package kst4contest.model;

/**
 * Represents a scheduled event or an AirScout opportunity in the future.
 * Used for the Timeline View and Priority Calculation.
 *
 * <p>The base callsign remains the grouping key for scoring and worked-state
 * handling. The exact KST login and its chat category are stored separately
 * because reminders and external logger handover refer to the selected
 * ChatMember entity.</p>
 */
public class ContestSked {

    private String targetCallsign;
    private String targetChatCallsign;
    private ChatCategory targetChatCategory;
    private double targetAzimuth;
    private long skedTimeEpoch;
    private Band band;

    // Opportunity potential (0..100). -1 means "unknown".
    int opportunityPotentialPercent = -1;

    // Status flags to prevent spamming alarms.
    private boolean warning3MinSent = false;
    private boolean warningNowSent = false;

    /**
     * Backward-compatible constructor.
     */
    public ContestSked(String call, double azimuth, long time, Band band) {
        this(call, call, null, azimuth, time, band);
    }

    /**
     * Creates a sked for one exact KST login.
     *
     * @param callRaw            base callsign used for scoring and worked states
     * @param chatCallsign       exact KST login, including an optional dash suffix
     * @param chatCategory       category in which the selected login is active
     * @param azimuth            target azimuth
     * @param time               sked time in epoch milliseconds
     * @param band               selected amateur-radio band
     */
    public ContestSked(String callRaw,
                       String chatCallsign,
                       ChatCategory chatCategory,
                       double azimuth,
                       long time,
                       Band band) {

        this.targetCallsign = callRaw;
        this.targetChatCallsign = chatCallsign;
        this.targetChatCategory = chatCategory;
        this.targetAzimuth = azimuth;
        this.skedTimeEpoch = time;
        this.band = band;
    }

    /**
     * Returns the seconds remaining until the event.
     * Negative values mean the event is in the past.
     */
    public long getTimeUntilSkedSeconds() {
        return (skedTimeEpoch - System.currentTimeMillis()) / 1000;
    }

    /**
     * Returns the base callsign used for scoring and worked-state grouping.
     */
    public String getTargetCallsign() {
        return targetCallsign;
    }

    /**
     * Returns the exact KST login selected when the sked was created.
     */
    public String getTargetChatCallsign() {
        if (targetChatCallsign == null || targetChatCallsign.isBlank()) {
            return targetCallsign;
        }
        return targetChatCallsign;
    }

    public ChatCategory getTargetChatCategory() {
        return targetChatCategory;
    }

    public double getTargetAzimuth() {
        return targetAzimuth;
    }

    public long getSkedTimeEpoch() {
        return skedTimeEpoch;
    }

    public Band getBand() {
        return band;
    }

    public boolean isWarning3MinSent() {
        return warning3MinSent;
    }

    public void setWarning3MinSent(boolean warning3MinSent) {
        this.warning3MinSent = warning3MinSent;
    }

    public boolean isWarningNowSent() {
        return warningNowSent;
    }

    public void setWarningNowSent(boolean warningNowSent) {
        this.warningNowSent = warningNowSent;
    }

    public int getOpportunityPotentialPercent() {
        return opportunityPotentialPercent;
    }

    public void setOpportunityPotentialPercent(int opportunityPotentialPercent) {
        this.opportunityPotentialPercent = opportunityPotentialPercent;
    }
}