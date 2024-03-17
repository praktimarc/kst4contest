package kst4contest.locatorUtils;

public class DirectionUtils {

    /**
     * Checks wheter a sked-sender writes to a sked-receiver and is in my direction due he beams to this receiver
     *
     * @param myLocator
     * @param locatorOfSkedSender
     * @param locatorOfSekdReceiver
     * @param maxRangeKm
     * @param hisAntennaBeamWidth
     * @return
     */
    public static boolean isInAngleAndRange(String myLocator, String locatorOfSkedSender, String locatorOfSekdReceiver, double maxRangeKm, double hisAntennaBeamWidth) {

        Location myLocation = new Location(myLocator);
        Location skedSenderLocation = new Location(locatorOfSkedSender);
        Location skedReceiverLocation = new Location(locatorOfSekdReceiver);

        double distanceFromMeToLocSender = new Location(myLocator).getDistanceKm(new Location(locatorOfSkedSender));

        // Check if distance exceeds my setted maximum range
        if (distanceFromMeToLocSender > maxRangeKm) {
            System.out.println("too far, " + distanceFromMeToLocSender + " km");
            return false;
        }

        //check bearing of sender to receiver

        double bearingOfSekdSenderToSkedReceiver = skedSenderLocation.getBearing(skedReceiverLocation);
        System.out.println("skedTX -> skedTX deg: " + bearingOfSekdSenderToSkedReceiver);

        double bearingOfSekdSenderToMe = skedSenderLocation.getBearing(myLocation);
        System.out.println("skedTX -> me deg: " + bearingOfSekdSenderToMe);

        if (DirectionUtils.isAngleInRange(bearingOfSekdSenderToMe,bearingOfSekdSenderToSkedReceiver, hisAntennaBeamWidth)) {
            //I may should get "/2" because of 50% of the 3dB opening angle if txer is directed to sender exactly
            System.out.println("isinangleandrange!");
            return true;
        } else {
            System.out.println("not in angle and reach");
            return false;
        }
    }


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
