package kst4contest.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The one decision AWT forces that JavaFX did not.
 *
 * <p>{@code HostServices.showDocument} took web and mail addresses alike.
 * {@code Desktop.browse("mailto:...")} silently opens nothing on Linux, so the scheme has
 * to be read before the call is chosen.</p>
 */
class ExternalDocumentsTest {

    @Test
    void aMailtoAddressGoesToTheMailClient() {
        assertTrue(ExternalDocuments.isMailAddress("mailto:praktimarc+kst4contest@gmail.com"));
    }

    @Test
    void theSchemeIsReadWithoutRegardToCase() {
        assertTrue(ExternalDocuments.isMailAddress("MAILTO:someone@example.org"));
    }

    @Test
    void webAddressesGoToTheBrowser() {
        assertFalse(ExternalDocuments.isMailAddress("https://ko-fi.com/praktimarc"));
        assertFalse(ExternalDocuments.isMailAddress("http://www.x08.de"));
    }

    @Test
    void anAddressThatMentionsMailtoLaterIsStillAWebAddress() {
        // Would be a mail client launch for a perfectly ordinary link otherwise.
        assertFalse(ExternalDocuments.isMailAddress("https://example.org/?to=mailto:x@y.z"));
    }

    @Test
    void nothingIsNotAMailAddress() {
        assertFalse(ExternalDocuments.isMailAddress(null));
        assertFalse(ExternalDocuments.isMailAddress("   "));
    }
}
