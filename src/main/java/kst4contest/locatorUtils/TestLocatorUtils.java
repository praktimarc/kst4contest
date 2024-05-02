package kst4contest.locatorUtils;

public class TestLocatorUtils {

    public static void main(String[] args) {


//        isInAngle(myLocation, location1, location2);
        System.out.println(isInAngleAndRange("JN49FL", "jo43xm", "jo30sa", 900, 50));
        System.out.println(isInAngleAndRange("JN49FL", "jo51ij", "jn39oc", 900, 50));
        System.out.println(isInAngleAndRange("JN49FL", "jn39oc", "jo51ij", 1100, 50));
    }

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
        System.out.println("skedTX -> skedRX deg: " + bearingOfSekdSenderToSkedReceiver);

        double bearingOfSekdSenderToMe = skedSenderLocation.getBearing(myLocation);
        System.out.println("skedTX -> me deg: " + bearingOfSekdSenderToMe);

        if (DirectionUtils.isAngleInRange(bearingOfSekdSenderToSkedReceiver, bearingOfSekdSenderToMe, hisAntennaBeamWidth)) {
            //may I should get "/2" because of 50% of the 3dB opening angle if txer is directed to sender exactly
            return true;
        } else return false;
    }


}
