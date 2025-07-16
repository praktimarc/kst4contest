package kst4contest.test;

import javafx.beans.property.SimpleStringProperty;
import kst4contest.controller.Utils4KST;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternMatcherTest {

    /**
     * Tests if pattern matches with the given String.
     *
     * @param testString
     * @param regExPattern
     * @return true if match, else false
     */

    private static boolean testPattern(String testString, String regExPattern) {

        Pattern pattern = Pattern.compile(regExPattern);
        Matcher matcher = pattern.matcher(testString);

        return matcher.find();
    }

    /**
     * Normalizes a chatmembers frequency-string for cluster usage<br/>
     * <b>returns a frequency String in KHz like = "144300" or "144300.0" to match DXC protocol needs</b>
     *
     * @param optionalPrefix: if there is a value like ".300", it have to be decided, wich ".300": 144.300, 432.300, 1296.300 .... prefix means for example "144."
     */
    private static String normalizeFrequencyString(String qrgString, String optionalPrefix) {

//        final String PTRN_QRG_CAT2 = "(([0-9]{3,4}[\\.|,| ]?[0-9]{3})([\\.|,][\\d]{1,2})?)|(([a-zA-Z][0-4]{1}[\\d]{2}\\b)([\\.|,][\\d]{1,2}\\b)?)|((\\b[0-4]{1}[\\d]{2}\\b)([\\.|,][\\d]{1,2}\\b)?)";

        final String PTRN_QRG_CAT2_wholeQRGMHz4Digits = "(([0-9]{4}[\\.|,| ]?[0-9]{3})([\\.|,][\\d]{1,2})?)"; //1296.300.3 etc
        final String PTRN_QRG_CAT2_wholeQRGMHz3Digits = "(([0-9]{3}[\\.|,| ]?[0-9]{3})([\\.][\\d]{1,2})?)"; //144.300.3 etc
        final String PTRN_QRG_CAT2_QRGwithoutPrefix = "((\\b[0-4]{1}[\\d]{2}\\b)([\\.|,][\\d]{1,2}\\b)?)"; //144.300.3 etc
        String predefinedPrefixInMHz = optionalPrefix;

        String stringAggregation = "";

        if (testPattern(qrgString, PTRN_QRG_CAT2_wholeQRGMHz4Digits)) {
            System.out.print("yep: ");
            stringAggregation = qrgString;

            stringAggregation = stringAggregation.replace(".","");
            stringAggregation = stringAggregation.replace(",","");
            if (stringAggregation.length() == 8) {
                String stringAggregationNew = stringAggregation.substring(0, stringAggregation.length()-1) + "." + stringAggregation.substring(stringAggregation.length()-1, stringAggregation.length());
                stringAggregation = stringAggregationNew;
            } else if (stringAggregation.length() == 9) {
                String stringAggregationNew = stringAggregation.substring(0, stringAggregation.length()-2) + "." + stringAggregation.substring(stringAggregation.length()-2, stringAggregation.length());
                stringAggregation = stringAggregationNew;
            }

        } else

            if (testPattern(qrgString, PTRN_QRG_CAT2_wholeQRGMHz3Digits)) {
                System.out.print("yep: ");
                stringAggregation = qrgString;

                stringAggregation = stringAggregation.replace(".","");
                stringAggregation = stringAggregation.replace(",","");
                if (stringAggregation.length() == 7) {
                    String stringAggregationNew = stringAggregation.substring(0, stringAggregation.length()-1) + "." + stringAggregation.substring(stringAggregation.length()-1, stringAggregation.length());
                    stringAggregation = stringAggregationNew;
                } else if (stringAggregation.length() == 8) {
                    String stringAggregationNew = stringAggregation.substring(0, stringAggregation.length()-2) + "." + stringAggregation.substring(stringAggregation.length()-2, stringAggregation.length());
                    stringAggregation = stringAggregationNew;
                }
        }
            else

            if (testPattern(qrgString, PTRN_QRG_CAT2_QRGwithoutPrefix)) { //case ".050 or .300 or something like that"
                System.out.print("yep: ");
                stringAggregation = qrgString;

                stringAggregation = stringAggregation.replace(".", "");
                stringAggregation = stringAggregation.replace(",", "");
                if (stringAggregation.length() == 3) { // like 050 or 300
                    String stringAggregationNew = optionalPrefix + stringAggregation;
                    stringAggregation = stringAggregationNew;
                    return stringAggregation;

                } else if (stringAggregation.length() == 4) { //like 050.2 --> 0502

                    stringAggregation = optionalPrefix + stringAggregation;
                    String stringAggregationNew = stringAggregation.substring(0, stringAggregation.length() - 1) + "." + stringAggregation.substring(stringAggregation.length() - 1, stringAggregation.length());
                    stringAggregation = stringAggregationNew;
                    return stringAggregation;

                } else if (stringAggregation.length() == 5) { //like 050.20 --> 05020

                    stringAggregation = optionalPrefix + stringAggregation;
                    String stringAggregationNew = stringAggregation.substring(0, stringAggregation.length() - 2) + "." + stringAggregation.substring(stringAggregation.length() - 2, stringAggregation.length());
                    stringAggregation = stringAggregationNew;
                    return stringAggregation;
                }
            }

        return qrgString;
    }

    public static void main(String[] args) {

        int i = 0;

        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("144.775", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("144.300.2", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("144,300.2", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("144300.2", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("144300,2", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("144.300", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("144.300.20", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("300", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString(".300", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString(".300.2", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString(".300.20", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("1296.300", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("1296,300", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("1296.300.2", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("1296.300.20", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("1296,300,2", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("1296,300,20", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("1296.300,2", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("1296,300.2", new SimpleStringProperty("144")));
        System.out.println(i++ + ": " + Utils4KST.normalizeFrequencyString("q305", new SimpleStringProperty("144")));
    }
}
