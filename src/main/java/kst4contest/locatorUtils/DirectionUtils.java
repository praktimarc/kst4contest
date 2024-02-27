package kst4contest.locatorUtils;

public class DirectionUtils {

    /**
     * Tests, if the angle (from me to) other station is in the range of the
     * angle (qtf) in degrees where my antenna points to.
     *
     * @param toForeignAngle [degrees]
     * @param mySelectedQTFAngle [degrees]
     * @param antennaBeamwidth [degrees]
     * @return
     */
    public static boolean isAngleInRange(double toForeignAngle,
                                         double mySelectedQTFAngle, double antennaBeamwidth) {

        double beamwidth = antennaBeamwidth / 2; // half left, half right

        double startAngle = mySelectedQTFAngle - beamwidth;
        double endAngle = mySelectedQTFAngle + beamwidth;

        // Normalize angles to be between 0 and 360 degrees
        toForeignAngle = normalizeAngle(toForeignAngle);
        startAngle = normalizeAngle(startAngle);
        endAngle = normalizeAngle(endAngle);

        // Check if the range wraps around 360 degrees
        if (startAngle <= endAngle) {
            return toForeignAngle >= startAngle && toForeignAngle <= endAngle;
        } else {
            // Range wraps around 360 degrees, so check if angle is within the
            // range or outside the range
            return toForeignAngle >= startAngle || toForeignAngle <= endAngle;
        }
    }

    private static double normalizeAngle(double angle) {
        if (angle < 0) {
            angle += 360;
        }
        if (angle >= 360) {
            angle -= 360;
        }
        return angle;
    }




}
