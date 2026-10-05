package kst4contest.controller;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The frames on the wire do not change with the interface language.
 *
 * A translated frame breaks the connection, and nothing about the user interface would show
 * it. This checks the output rather than the translation files, because the output is what the
 * server reads -- and because a boundary that is only documented is a boundary that holds
 * until the next person pulls a string into a bundle in good faith.
 *
 * Turkish is in the list deliberately: it is the locale under which "I".toLowerCase() is not
 * "i", the classic trap for any protocol code that case-folds without Locale.ROOT.
 */
class On4KstProtocolLocaleTest {

    private static final Locale[] LOCALES = {
            Locale.GERMANY,
            Locale.UK,
            Locale.forLanguageTag("tr"),
    };

    private <T> T underLocale(final Locale locale, final Supplier<T> body) {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(locale);
            return body.get();
        } finally {
            Locale.setDefault(previous);
        }
    }

    private void assertLocaleIndependent(final String what, final Supplier<String> frame) {
        String reference = underLocale(Locale.UK, frame);

        for (Locale locale : LOCALES) {
            assertEquals(reference, underLocale(locale, frame), what + " under " + locale);
        }
    }

    @Test
    void theLoginFrameIsLocaleIndependent() {
        assertLocaleIndependent(
                "login",
                () -> On4KstProtocol.login("DN9APW", "secret", 1, "KST4Contest", 0L));
    }

    @Test
    void theSettingsDoneFrameIsLocaleIndependent() {
        assertLocaleIndependent("settingsDone", () -> On4KstProtocol.settingsDone(1));
    }

    @Test
    void theAddChatFrameIsLocaleIndependent() {
        assertLocaleIndependent("addChat", () -> On4KstProtocol.addChat(1, 0L));
    }

    @Test
    void theLivenessFramesAreLocaleIndependent() {
        assertLocaleIndependent("clientLivenessProbe", On4KstProtocol::clientLivenessProbe);
        assertLocaleIndependent(
                "serverLivenessProbeResponse",
                On4KstProtocol::serverLivenessProbeResponse);
    }

    @Test
    void theFramesReallyAreTheOpcodesThisTestClaimsToGuard() {
        /*
         * Without this, the four tests above would still pass if the frames became empty
         * strings: identical under every locale, and identically useless.
         */
        assertEquals(
                "LOGINC|",
                On4KstProtocol.login("DN9APW", "secret", 1, "KST4Contest", 0L).substring(0, 7));
        assertEquals("SDONE|", On4KstProtocol.settingsDone(1).substring(0, 6));
        assertEquals("ACHAT|", On4KstProtocol.addChat(1, 0L).substring(0, 6));
    }
}
