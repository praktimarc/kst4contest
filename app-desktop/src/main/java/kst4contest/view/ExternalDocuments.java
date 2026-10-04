package kst4contest.view;

import java.awt.Desktop;
import java.net.URI;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Opens an address in whatever the desktop uses for it.
 *
 * <p>Replaces {@code Application.getHostServices().showDocument(...)}, which went away
 * with {@code extends Application}. JavaFX treated web and mail addresses alike; AWT does
 * not, so the scheme is decided here: {@code Desktop.mail} for mail, {@code Desktop.browse}
 * for everything else. A {@code browse("mailto:...")} silently opens nothing on Linux.</p>
 */
public final class ExternalDocuments {

    private static final Logger LOGGER = Logger.getLogger(ExternalDocuments.class.getName());

    private static final String MAIL_SCHEME = "mailto:";

    private ExternalDocuments() {
        // Utility class.
    }

    /**
     * Whether the address names a mail recipient rather than a document.
     *
     * @param address the address to classify, may be null
     * @return true when the address carries the mailto scheme
     */
    public static boolean isMailAddress(final String address) {

        if (address == null) {
            return false;
        }

        return address.trim().toLowerCase(Locale.ROOT).startsWith(MAIL_SCHEME);
    }

    /**
     * Opens the address, or logs why it could not be opened.
     *
     * <p>A failure here is never worth terminating anything: the operator asked for a web
     * page, not for a state change.</p>
     *
     * @param address an http/https address or a mailto address, may be null
     */
    public static void open(final String address) {

        if (address == null || address.isBlank()) {
            return;
        }

        try {
            URI target = URI.create(address.trim());

            if (!Desktop.isDesktopSupported()) {
                LOGGER.log(Level.WARNING, "No desktop integration available for {0}", address);
                return;
            }

            Desktop desktop = Desktop.getDesktop();

            if (isMailAddress(address)) {
                desktop.mail(target);
            } else {
                desktop.browse(target);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not open " + address, e);
        }
    }
}
