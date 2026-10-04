package kst4contest.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Lightweight positive-signal detector.
 *
 * Patterns are configured via a single preference string, delimited by ';' or newlines.
 * Examples: "QRV;READY;RX OK;RGR;TNX;TU;HRD"
 */
public final class SignalDetector {

    private static final AtomicReference<String> lastPatterns = new AtomicReference<>("");
    private static final AtomicReference<List<Pattern>> cached = new AtomicReference<>(List.of());

    private SignalDetector() {}

    public static boolean containsPositiveSignal(String messageText, String patternsDelimited) {
        if (messageText == null || messageText.isBlank()) return false;
        List<Pattern> patterns = compileIfChanged(patternsDelimited);

        String txt = messageText.toUpperCase();
        for (Pattern p : patterns) {
            if (p.matcher(txt).find()) return true;
        }
        return false;
    }

    private static List<Pattern> compileIfChanged(String patternsDelimited) {
        String p = patternsDelimited == null ? "" : patternsDelimited.trim();
        String prev = lastPatterns.get();
        if (p.equals(prev)) return cached.get();

        List<Pattern> out = new ArrayList<>();
        for (String token : p.split("[;\\n\\r]+")) {
            String t = token.trim();
            if (t.isEmpty()) continue;

            // plain substring match, but regex-safe
            String regex = Pattern.quote(t.toUpperCase());
            out.add(Pattern.compile(regex));
        }

        lastPatterns.set(p);
        cached.set(List.copyOf(out));
        return out;
    }
}
