package kst4contest.view.map;

/**
 * User/configuration values for the path link-budget estimate.
 *
 * <p>All antenna gains are dBi. If the user knows dBd values, add 2.15 dB
 * before entering them as dBi. Example: 12 dBd = 14.15 dBi.</p>
 */
public record PathLinkBudgetSettings(
        double ownTxPowerWatts,
        double ownAntennaGainDbi,
        double targetTxPowerWatts,
        double targetAntennaGainDbi,
        double vhfFeederLossPerStationDb,
        double feederLossIncreaseDbPer200MHz,
        double maxEstimatedFeederLossPerStationDb,
        double requiredSsbSignalDbm,
        double requiredCwSignalDbm,
        double contestMarginDb
) {

    public static PathLinkBudgetSettings defaults() {
        return new PathLinkBudgetSettings(
                750.0,
                8.0,
                100.0,
                8.0,
                2.0,
                2.0,
                20.0,
                -126.0,
                -132.0,
                6.0
        );
    }

    public PathLinkBudgetSettings {
        ownTxPowerWatts = sanitizePositive(ownTxPowerWatts, 750.0);
        ownAntennaGainDbi = sanitizeFinite(ownAntennaGainDbi, 8.0);

        targetTxPowerWatts = sanitizePositive(targetTxPowerWatts, 100.0);
        targetAntennaGainDbi = sanitizeFinite(targetAntennaGainDbi, 8.0);

        vhfFeederLossPerStationDb = sanitizeNonNegative(vhfFeederLossPerStationDb, 2.0);
        feederLossIncreaseDbPer200MHz = sanitizeNonNegative(feederLossIncreaseDbPer200MHz, 2.0);
        maxEstimatedFeederLossPerStationDb = sanitizeNonNegative(maxEstimatedFeederLossPerStationDb, 20.0);

        requiredSsbSignalDbm = sanitizeFinite(requiredSsbSignalDbm, -126.0);
        requiredCwSignalDbm = sanitizeFinite(requiredCwSignalDbm, -132.0);
        contestMarginDb = sanitizeNonNegative(contestMarginDb, 6.0);
    }

    private static double sanitizePositive(double value, double fallback) {
        return Double.isFinite(value) && value > 0.0 ? value : fallback;
    }

    private static double sanitizeNonNegative(double value, double fallback) {
        return Double.isFinite(value) && value >= 0.0 ? value : fallback;
    }

    private static double sanitizeFinite(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }
}