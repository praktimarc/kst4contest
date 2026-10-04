package kst4contest.view.map;

import java.util.Locale;

/**
 * Bidirectional link-budget estimate for one path.
 *
 * <p>The estimate is intentionally operator-facing. It combines free-space
 * loss, the current diffraction/obstruction estimate, antenna gain, feeder loss,
 * TX power and required SSB/CW receiver levels.</p>
 */
public record PathLinkBudgetSummary(
        double frequencyMHz,

        double ownTxPowerDbm,
        double targetTxPowerDbm,
        double ownAntennaGainDbi,
        double targetAntennaGainDbi,

        double ownFeederLossDb,
        double targetFeederLossDb,
        double freeSpacePathLossDb,
        double diffractionLossDb,

        double homeToTargetRxPowerDbm,
        double targetToHomeRxPowerDbm,

        double requiredSsbSignalDbm,
        double requiredCwSignalDbm,
        double contestMarginDb,

        double homeToTargetSsbMarginDb,
        double targetToHomeSsbMarginDb,
        double bidirectionalSsbMarginDb,

        double homeToTargetCwMarginDb,
        double targetToHomeCwMarginDb,
        double bidirectionalCwMarginDb
) {

    public static PathLinkBudgetSummary empty() {
        return new PathLinkBudgetSummary(
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN
        );
    }

    public boolean hasUsableBudget() {
        return Double.isFinite(bidirectionalSsbMarginDb);
    }

    public String ssbMarginText() {
        if (!hasUsableBudget()) {
            return "-";
        }

        return String.format(
                Locale.US,
                "SSB QSO margin %+.1f dB | H→DX %+.1f dB | DX→H %+.1f dB",
                bidirectionalSsbMarginDb,
                homeToTargetSsbMarginDb,
                targetToHomeSsbMarginDb
        );
    }

    public String rxPowerText() {
        if (!Double.isFinite(homeToTargetRxPowerDbm)
                || !Double.isFinite(targetToHomeRxPowerDbm)) {
            return "-";
        }

        return String.format(
                Locale.US,
                "H→DX %.1f dBm | DX→H %.1f dBm",
                homeToTargetRxPowerDbm,
                targetToHomeRxPowerDbm
        );
    }

    public String linkBudgetDetailText() {
        if (!hasUsableBudget()) {
            return "-";
        }

        return String.format(
                Locale.US,
                "FSPL %.1f dB, diffraction %.1f dB, feeder H %.1f dB / DX %.1f dB, required SSB %.1f dBm + %.1f dB margin",
                freeSpacePathLossDb,
                diffractionLossDb,
                ownFeederLossDb,
                targetFeederLossDb,
                requiredSsbSignalDbm,
                contestMarginDb
        );
    }

    public String cwHintText() {
        if (!Double.isFinite(bidirectionalCwMarginDb)) {
            return "-";
        }

        if (bidirectionalSsbMarginDb >= 0.0) {
            return String.format(Locale.US, "SSB workable; CW margin %+.1f dB.", bidirectionalCwMarginDb);
        }

        if (bidirectionalCwMarginDb >= 0.0) {
            return String.format(Locale.US, "SSB marginal/weak, but CW may still be workable (%+.1f dB).", bidirectionalCwMarginDb);
        }

        return String.format(Locale.US, "Even CW budget is negative (%+.1f dB).", bidirectionalCwMarginDb);
    }
}