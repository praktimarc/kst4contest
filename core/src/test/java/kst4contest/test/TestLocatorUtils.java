package kst4contest.test;

import kst4contest.locatorUtils.Angle;
import kst4contest.locatorUtils.Latitude;
import kst4contest.locatorUtils.Location;
import kst4contest.locatorUtils.Longitude;

import java.text.DecimalFormat;

public class TestLocatorUtils {

    public static void main(String[] args) {
        Location location = new Location("JN49FL");
        Location location2 = new Location("JO51IJ");

//        System.out.println(location.getDistanceKm(location2));
//        System.out.println(Location.getBearing(location, location2));


        System.out.println((new Location().getDistanceKmByTwoLocatorStrings("JN49FL", "kn02fx") + ""));
//        System.out.println((new Location().getBearing());
//        int test =  888.08;
//        System.out.println(test);

//        String test = new Location().getDistanceKmByTwoLocatorStrings("JN49FL", "Jo51ij") + "";
//
//        DecimalFormat df = new DecimalFormat("#.##");
//
//        System.out.println(df.format(Double.parseDouble(test)));

//        Angle angle = new Angle();
//        Latitude lat = new Latitude();
//        Longitude lon = new Longitude();
//
//        location.setLatitude(lat);
//        location.setLongitude(lon);



    }
}
