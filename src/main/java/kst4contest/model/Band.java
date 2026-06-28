package kst4contest.model;

/**
 * Represents Amateur Radio Bands and their physical limits.
 * Used for plausibility checks in the Smart Parser.
 */
public enum Band {
    B_144(144.000, 146.000, "144"),
    B_432(432.000, 434.000, "432"),
    B_1296(1296.000, 1298.000, "1296"),
    B_2320(2320.000, 2322.000, "2320"),
    B_3400(3400.000, 3410.000, "3400"),
    B_5760(5760.000, 5762.000, "5760"),
    B_10G(10368.000, 10370.000, "10368"),
    B_24G(24048.000, 24050.000, "24048");
    // more space for future usage

    private final double minFreq;
    private final double maxFreq;
    private final String prefix; // Default prefix for "short value" parsing (e.g., .210)

    Band(double min, double max, String prefix) {
        this.minFreq = min;
        this.maxFreq = max;
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns the lower edge used as practical analysis frequency when only the band
     * is known. This keeps the batch reachability calculation deterministic.
     *
     * @return frequency in MHz
     */
    public double getDefaultAnalysisFrequencyMHz() {
        return minFreq;
    }

    /**
     * Returns a compact label for table display and filter controls.
     *
     * @return human readable band label
     */
    public String getDisplayLabel() {
        switch (this) {
            case B_144: return "144";
            case B_432: return "432";
            case B_1296: return "1296";
            case B_2320: return "2320";
            case B_3400: return "3400";
            case B_5760: return "5760";
            case B_10G: return "10G";
            case B_24G: return "24G";
            default: return prefix;
        }
    }

    /**
     * Checks if a specific frequency falls within this band's limits.
     */
    public boolean isPlausible(double freq) {
        return freq >= minFreq && freq <= maxFreq;
    }

    /**
     * Helper to find the matching Band enum for a given frequency.
     * Returns null if no band matches.
     */
    public static Band fromFrequency(double freq) {
        for (Band b : values()) {
            if (b.isPlausible(freq)) return b;
        }
        return null;
    }
}