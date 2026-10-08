package kst4contest.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * No default that goes to another radio amateur changes with the interface language.
 *
 * Over every String field by reflection rather than over a maintained list. The beacon texts
 * and the auto answers are the ones anybody would think of, and they are also in every
 * operator's stored XML, so translating one would alter existing profiles. The fields nobody
 * thinks of are the reason this is reflective -- including the ones added after this test was
 * written.
 */
class ChatPreferencesLocaleTest {

    private Map<String, String> stringFieldsUnder(final Locale locale) throws Exception {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(locale);
            ChatPreferences prefs = new ChatPreferences();
            Map<String, String> values = new LinkedHashMap<>();

            for (Field field : ChatPreferences.class.getDeclaredFields()) {
                if (field.getType() != String.class || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                values.put(field.getName(), (String) field.get(prefs));
            }

            return values;
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void everyStringDefaultIsTheSameUnderGermanAndEnglish() throws Exception {
        assertEquals(stringFieldsUnder(Locale.UK), stringFieldsUnder(Locale.GERMANY));
    }

    @Test
    void everyStringDefaultIsTheSameUnderTurkishToo() throws Exception {
        // The locale where case folding differs; a default built with toUpperCase() would show.
        assertEquals(
                stringFieldsUnder(Locale.UK),
                stringFieldsUnder(Locale.forLanguageTag("tr")));
    }

    @Test
    void thereAreEnoughStringFieldsForThisTestToMeanSomething() {
        /*
         * Without this, the two tests above would pass just as well if reflection found no
         * fields at all -- two empty maps are equal.
         */
        long count = java.util.Arrays.stream(ChatPreferences.class.getDeclaredFields())
                .filter(field -> field.getType() == String.class)
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .count();

        assertTrue(count > 20, "only " + count + " String fields were found by reflection");
    }

    @Test
    void theTextsThatGoToOtherRadioAmateursAreStillEnglish() throws Exception {
        /*
         * Named as well as covered reflectively: these are in the stored XML of every
         * operator, so a change to them is a change to existing profiles. If a later stage
         * moves them, this test is the place that says it was deliberate.
         */
        Map<String, String> values = stringFieldsUnder(Locale.GERMANY);

        assertEquals("Hi, pse call us", values.get("bcn_beaconTextMainCat"));
        assertEquals("Hi, pse call us", values.get("bcn_beaconTextSecondCat"));
        assertTrue(
                values.get("messageHandling_autoAnswerTextMainCat").startsWith("Hi, sry I am not qrv"),
                values.get("messageHandling_autoAnswerTextMainCat"));
        assertTrue(
                values.get("messageHandling_autoAnswerTextSecondCat").startsWith("Hi, sry I am not qrv"),
                values.get("messageHandling_autoAnswerTextSecondCat"));
    }
}
