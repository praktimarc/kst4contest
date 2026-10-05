# Umsetzungsplan — die Farben pro Operateurprofil anpassbar machen

> **Für ausführende Agenten:** Schritte sind als Kästchen (`- [ ]`) geführt. Nach jeder Aufgabe
> ein Commit und ein `./gradlew clean build` mit Auswertung der Test-XML. Nach den mit
> **GUI-ABNAHME** markierten Aufgaben die Anwendung starten und von Marc bedienen lassen.

**Ziel:** Der Operateur bestimmt sechs Farbrollen pro Entwurf und pro Profil. Die Änderung
wirkt sofort in allen Fenstern, überlebt den Neustart und ist exakt auf die Auslieferung
zurücksetzbar.

**Architektur:** Drei Schichten mit ausdrücklichem Vorrang — Auslieferung, dann eine CSS-Datei
im Profilverzeichnis, dann die Abweichungen aus den Einstellungen. Die Auflösung ist eine reine
Funktion, die zu jeder Rolle auch ihre Herkunft liefert. Ein `PaletteStore` hält das Ergebnis
als Compose-Zustand, damit `Kst4ContestTheme` es liest statt `remember(darkMode)` — das ist der
einzige Grund, warum eine Änderung alle offenen Fenster erreicht.

**Werkzeuge:** Java 21, Kotlin 2.2, Compose Multiplatform 1.8.2, JUnit 5, Gradle Wrapper.

**Spezifikation:** `docs/superpowers/specs/2026-10-04-farben-anpassbar-design.md`.
**Übergeordnet:** `docs/superpowers/specs/2026-09-25-compose-migration-design.md`, Etappe 8.

## Projektweite Vorgaben

- Kommunikation mit Marc auf **Deutsch**; Quelltextkommentare und Javadoc **ausschließlich
  Englisch**. Commit-Nachrichten knappes Englisch.
- `./gradlew clean build`, Java 21. **Nicht aus Exit-Code 0 auf grün schließen** — die XML
  unter `*/build/test-results/test/TEST-*.xml` auswerten. Grundlinie: **868 Tests, 0 Fehler**.
- **Immer mit Pfadangabe committen** (`git commit -- <pfade>`). Dieser Arbeitsbaum trägt
  vorbestehende gestapelte und ungestapelte Änderungen von vor der Sitzung.
- **Signierte Commits.** Schlägt das Signieren mit `Couldn't get agent socket` fehl, ist der
  Bitwarden-SSH-Agent gesperrt — Marc entsperren lassen, **nicht** unsigniert committen.
- Kein Push, kein Merge, kein Tag, kein Release, kein Versions-Bump. **Keine neue Abhängigkeit.**
- `core` bleibt toolkit-frei: **kein** `androidx.compose`-Import in `core/src/main`. Farben
  werden dort als Zeichenketten geführt, nicht als `Color`.
- **`JavaFxStylesheet` bleibt ein Leser.** Kein CSS-Schreiber, nirgends.
- **Die Reiterreihenfolge im Einstellungsfenster ändert sich nicht.** Der neue Reiter wird
  **angehängt**; `SettingsTabs.kt:200`–`:204` begründet ausdrücklich, warum keine Position
  verschoben wird.
- **Nicht anfassen:** die ausgelieferten Vorlagen `KST4ContestDefaultDay.css` und
  `…Evening.css`, `derive`s Rechenweg samt `javafx-derive-reference.txt` und
  `JavaFxStylesheetTest`, `fontSizePx` und die bestehende Schriftgrößeneinstellung.
  Ausdrücklich erlaubt und in Aufgabe 3 verlangt: **ein Satz** in `derive`s Javadoc, der auf
  diese Etappe verweist und mit ihr erledigt ist. Der Rechenweg bleibt unberührt.

## Prüfschwerpunkte

Acht Dinge, die die Spezifikation voraussetzt, aber nicht als Test benennt. Jede Zeile
bekommt ihren Test in der Aufgabe, der der Code gehört. Die ersten drei sind beim Schreiben
dieses Plans am Quelltext **gemessen** worden und keine Vermutung.

1. **Eine Rolle ist ein Selektor und eine Eigenschaft.** Drei der sechs stehen nicht in
   `.root` — Text in `.label`, die Trennlinie in `.separator *.line` —, und
   `-fx-background-color` allein kommt in der Abendvorlage ein Dutzend Mal vor. Ein Leser, der
   nur die Eigenschaft führt, findet die halbe Palette nie und die andere Hälfte am falschen
   Ort. → Aufgabe 1 und 3.
2. **Der gewählte Akzent muss der sein, den der Operateur sieht.** `Theme.kt:97` bildet
   Materials `primary` auf `textAccent` ab und nicht auf `accent`. Folgt `textAccent` der
   Akzentrolle nicht, ist der Regler ein Regler, der nichts tut — durchgeschrieben und nicht
   gezeichnet, genau die Fehlerklasse der Zeichenschicht-Tests. → A4.
3. **Der Kontrastwächter darf nicht über den Auslieferungsstand warnen.** Gemessen: der
   Tagesakzent liegt bei 2,86, der Abendakzent bei 2,26, Text im Abendfeld bei etwa 2,9 — alle
   drei unter dem WCAG-AA-Boden von 4,5. Eine absolute Grenze leuchtet ab dem ersten Start. → A5.
4. **Eine Datei, die nur eine Rolle nennt, darf nur diese Rolle ändern.** `JavaFxStylesheet.read`
   füllt fehlende Rollen mit Modena-Rückfallwerten; als Schicht 2 benutzt, würde eine
   Datei mit einer einzigen Zeile **alle sechs** Rollen überschreiben. → A3.
5. **Eine kaputte Datei darf den Start nicht verhindern.** Unlesbar, kein CSS, halb
   geschrieben — die Rolle fällt auf die Auslieferung zurück und die Anwendung startet. → A3.
6. **Die Rettungsleine darf nicht an der Palette hängen, die sie retten soll.** Der
   Auslieferungs-Knopf muss seine Farben aus Schicht 1 nehmen, geprüft mit einer Palette, in
   der jede Rolle verändert ist. → A9.
7. **Tag und Abend dürfen sich nicht gegenseitig überschreiben.** Zwei Paletten, zwei
   Abweichungsmengen, eine gemeinsame Speicherung — der naheliegende Fehler ist, dass eine
   Änderung am Abend den Tag mitzieht. → A2 und A4.
8. **Eine Farbangabe, die der Operateur falsch eintippt**, darf nicht zu einer schwarzen
   Oberfläche führen. `#GGG`, `rot`, leer, `#1234567` — unparsbare Eingabe lässt die Rolle, wie
   sie war, und sagt es. → A8.

---

## Dateiübersicht

**Neu in `core`** (toolkit-frei, Farben als Zeichenketten):

| Datei | Verantwortung |
|---|---|
`core/src/main/java/kst4contest/model/PaletteRole.java` | die sechs Rollen, mit ihrem CSS-Eigenschaftsnamen |
`core/src/main/java/kst4contest/model/PaletteOverrides.java` | Kodierung einer Abweichungsmenge als eine Zeichenkette, parsen und schreiben |
`core/src/test/java/kst4contest/model/PaletteOverridesTest.java` | die Kodierung, einschließlich Müll als Eingabe |

**Neu in `app-desktop`:**

| Datei | Verantwortung |
|---|---|
`.../compose/PaletteResolution.kt` | die Dreischicht-Auflösung samt Herkunft je Rolle — reine Funktion |
`.../compose/PaletteStore.kt` | das Ergebnis als Compose-Zustand, die drei Rückwege, und `PaletteStoreFactory` |
`.../compose/ContrastRule.kt` | WCAG-Kontrastverhältnis, relativ zur Auslieferung gemessen |
`.../compose/tabs/ColoursTabState.kt` | typisierte Fassade über Einstellungen und Store |
`.../compose/tabs/ColoursTab.kt` | die Oberfläche, mit der Rettungsleine in Auslieferungsfarben |
dazu vier Testdateien | |

**Geändert:** `ChatPreferences.java` (zwei Einstellungen), `JavaFxStylesheet.kt`
(`declaredRoles` ergänzen), `Theme.kt` (liest den Store, `contrastingText` wird `internal`),
`MainMenuBar.kt` (offener Menütitel), `OperatorProfilePaths.java` (Pfad der Profilvorlage),
`SettingsTabs.kt` (Reiter angehängt), `Kst4ContestApplication.java` (Store beim Start bilden).

---

## Aufgabe 1: Die sechs Rollen und ihre Deklaration

**Dateien:**
- Anlegen: `core/src/main/java/kst4contest/model/PaletteRole.java`
- Anlegen: `core/src/test/java/kst4contest/model/PaletteRoleTest.java`

**Schnittstellen:**
- Liefert: `enum PaletteRole` mit `SURFACE, WINDOW_SURFACE, FIELD_INTERIOR, TEXT, ACCENT,
  SEPARATOR`, je mit `selector()`, `cssProperty()` und `key()`.
- Liefert: `PaletteRole.of(String selector, String cssProperty)` → die Rolle oder `null`.

**Eine Rolle ist ein Selektor und eine Eigenschaft, nicht nur eine Eigenschaft.** Gemessen an
den ausgelieferten Vorlagen:

| Rolle | Selektor | Eigenschaft | Schlüssel |
|---|---|---|---|
| `SURFACE` | `.root` | `-fx-base` | `surface` |
| `WINDOW_SURFACE` | `.root` | `-fx-background` | `windowSurface` |
| `FIELD_INTERIOR` | `.root` | `-fx-control-inner-background` | `fieldInterior` |
| `TEXT` | `.label` | `-fx-text-fill` | `text` |
| `ACCENT` | `.root` | `-fx-accent` | `accent` |
| `SEPARATOR` | `.separator *.line` | `-fx-background-color` | `separator` |

Drei der sechs stehen **nicht** in `.root` — `JavaFxStylesheet.read` liest sie aus `.label`
und `.separator *.line` (`JavaFxStylesheet.kt:103` und `:105`). Ein Entwurf, der nur die
Eigenschaft führt, findet Text und Trennlinie nie und findet `-fx-background-color` an einem
Dutzend fremder Stellen. `parseBlocks` schlüsselt auf den wörtlichen Selektor mit normierten
Leerzeichen, deshalb ist `.separator *.line` genau so zu schreiben.

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

```java
package kst4contest.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The six roles the operator may set.
 *
 * <p>A role is a selector and a property, not a property alone: the shipped sheets state
 * three of the six outside `.root`, and `-fx-background-color` on its own appears a dozen
 * times in one sheet. The selector is what tells the separator's line from a scroll bar.</p>
 *
 * <p>All three strings are part of a file format -- two of them the operator's own
 * hand-edited stylesheet, the third the preferences XML. Renaming any of them silently
 * drops a colour somebody chose, which is why they are pinned here rather than inlined.</p>
 */
class PaletteRoleTest {

    @Test
    void thereAreSixRolesAndEachNamesOneDeclaration() {
        assertEquals(6, PaletteRole.values().length);

        Set<String> declarations = new HashSet<>();
        for (PaletteRole role : PaletteRole.values()) {
            assertTrue(role.cssProperty().startsWith("-fx-"), role + " has no CSS property");
            assertTrue(role.selector().startsWith("."), role + " has no selector");
            assertTrue(
                    declarations.add(role.selector() + "|" + role.cssProperty()),
                    role + " names the same declaration as another role");
        }
    }

    @Test
    void theThreeRolesOutsideRootKeepTheirOwnSelectors() {
        /*
         * These three are the reason a role carries a selector at all. The values are the
         * ones JavaFxStylesheet.read already uses, so the two must not drift apart.
         */
        assertEquals(".label", PaletteRole.TEXT.selector());
        assertEquals("-fx-text-fill", PaletteRole.TEXT.cssProperty());

        assertEquals(".separator *.line", PaletteRole.SEPARATOR.selector());
        assertEquals("-fx-background-color", PaletteRole.SEPARATOR.cssProperty());

        assertEquals(".root", PaletteRole.SURFACE.selector());
    }

    @Test
    void theStorageKeysAreDistinctAndStable() {
        // These strings land in the operator's preferences XML. Renaming one drops a colour.
        assertEquals("surface", PaletteRole.SURFACE.key());
        assertEquals("windowSurface", PaletteRole.WINDOW_SURFACE.key());
        assertEquals("fieldInterior", PaletteRole.FIELD_INTERIOR.key());
        assertEquals("text", PaletteRole.TEXT.key());
        assertEquals("accent", PaletteRole.ACCENT.key());
        assertEquals("separator", PaletteRole.SEPARATOR.key());
    }

    @Test
    void aDeclarationResolvesBackToItsRole() {
        assertEquals(PaletteRole.SURFACE, PaletteRole.of(".root", "-fx-base"));
        assertEquals(PaletteRole.TEXT, PaletteRole.of(".label", "-fx-text-fill"));
    }

    @Test
    void theSamePropertyUnderAnotherSelectorIsNotThatRole() {
        /*
         * The whole point of carrying the selector. Both sheets set -fx-text-fill on a
         * dozen selectors and -fx-background-color on more; only one of each is a role.
         */
        assertNull(PaletteRole.of(".button:hover", "-fx-text-fill"));
        assertNull(PaletteRole.of(".scroll-bar", "-fx-background-color"));
    }

    @Test
    void anUnknownDeclarationIsNotARole() {
        assertNull(PaletteRole.of(".root", "-fx-font-size"));
        assertNull(PaletteRole.of("", ""));
        assertNull(PaletteRole.of(null, null));
    }
}
```

- [ ] **Schritt 2: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :core:test --tests '*PaletteRoleTest*'
```

Erwartung: Übersetzungsfehler, `PaletteRole` existiert nicht.

- [ ] **Schritt 3: Die Umsetzung schreiben**

```java
package kst4contest.model;

/**
 * A colour the operator may set, and the stylesheet declaration it corresponds to.
 *
 * <p>In core and therefore free of any toolkit: a role is a name and three strings, and the
 * colour itself travels as text. Turning text into a drawable colour is the user
 * interface's job.</p>
 *
 * <p>A declaration is a selector and a property together. Three of the six roles are stated
 * outside `.root` by the shipped sheets, and `-fx-background-color` alone appears a dozen
 * times in one of them -- on scroll bars, buttons and toggle buttons. Without the selector,
 * half the roles would never be found and the rest would be found in the wrong places.</p>
 *
 * <p>All three strings are part of a file format. The selector and the property are what a
 * hand-edited stylesheet writes; {@link #key()} is what the stored overrides are written
 * under. Renaming any of them silently drops a colour an operator chose.</p>
 */
public enum PaletteRole {

    /** The control surface; the window surface is normally a `derive` of this. */
    SURFACE(".root", "-fx-base", "surface"),

    /** The window's own area. Neither shipped sheet states it, so both derive it. */
    WINDOW_SURFACE(".root", "-fx-background", "windowSurface"),

    /** Inside a text field or a list. */
    FIELD_INTERIOR(".root", "-fx-control-inner-background", "fieldInterior"),

    /** Label text. Stated on `.label` and not on `.root`. */
    TEXT(".label", "-fx-text-fill", "text"),

    ACCENT(".root", "-fx-accent", "accent"),

    /** The separator's line, whose selector is what tells it from a scroll bar. */
    SEPARATOR(".separator *.line", "-fx-background-color", "separator");

    private final String selector;
    private final String cssProperty;
    private final String key;

    PaletteRole(final String selector, final String cssProperty, final String key) {
        this.selector = selector;
        this.cssProperty = cssProperty;
        this.key = key;
    }

    /** The stylesheet selector this role is stated on. */
    public String selector() {
        return selector;
    }

    /** The stylesheet property this role is read from. */
    public String cssProperty() {
        return cssProperty;
    }

    /** The name this role is stored under in the preferences. */
    public String key() {
        return key;
    }

    /**
     * The role a stylesheet declaration belongs to.
     *
     * @param selector a selector from a stylesheet, may be null
     * @param cssProperty a property name from a stylesheet, may be null
     * @return the role, or null for the hundreds of declarations that are not ours
     */
    public static PaletteRole of(final String selector, final String cssProperty) {

        if (selector == null || cssProperty == null) {
            return null;
        }

        for (PaletteRole role : values()) {
            if (role.selector.equals(selector) && role.cssProperty.equals(cssProperty)) {
                return role;
            }
        }

        return null;
    }
}
```

- [ ] **Schritt 4: Tests laufen lassen**

```bash
./gradlew :core:test --tests '*PaletteRoleTest*'
```

Erwartung: 6 Tests grün.

- [ ] **Schritt 5: Committen**

```bash
git add core/src/main/java/kst4contest/model/PaletteRole.java \
        core/src/test/java/kst4contest/model/PaletteRoleTest.java
git commit -m "Name the six palette roles and the two file formats they appear in" -- \
        core/src/main/java/kst4contest/model/PaletteRole.java \
        core/src/test/java/kst4contest/model/PaletteRoleTest.java
```

---

## Aufgabe 2: Die Abweichungen als eine Zeichenkette

Zwölf einzelne XML-Elemente (sechs Rollen × zwei Entwürfe) wären zwölf Schreibblöcke, zwölf
Leseblöcke und zwölf Feldpaare in einer Datei, die schon 3.300 Zeilen hat. Stattdessen **eine**
Zeichenkette je Entwurf, mit einer reinen Funktion davor.

**Dateien:**
- Anlegen: `core/src/main/java/kst4contest/model/PaletteOverrides.java`
- Anlegen: `core/src/test/java/kst4contest/model/PaletteOverridesTest.java`

**Schnittstellen:**
- Liefert: `PaletteOverrides.parse(String stored)` → `Map<PaletteRole, String>` (nur die
  wirklich genannten Rollen; nie `null`), `PaletteOverrides.format(Map<PaletteRole, String>)`
  → `String`, und `PaletteOverrides.isValidColour(String)` → `boolean`.
- Format: `surface=#ECECEC;accent=#0096C9`. Unbekannte Schlüssel und unparsbare Farben werden
  beim Lesen übersprungen.

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

```java
package kst4contest.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How a set of colour overrides survives in the preferences XML.
 *
 * <p>One string per design rather than twelve elements. The parsing is where an operator's
 * stored colours are either kept or quietly lost, and the input is not always ours: a
 * hand-edited XML, a file written by an older version, or a half-finished write.</p>
 */
class PaletteOverridesTest {

    @Test
    void anEmptyStoreMeansNoOverridesRatherThanAFailure() {
        assertTrue(PaletteOverrides.parse(null).isEmpty());
        assertTrue(PaletteOverrides.parse("").isEmpty());
        assertTrue(PaletteOverrides.parse("   ").isEmpty());
    }

    @Test
    void onlyTheRolesActuallyNamedComeBack() {
        Map<PaletteRole, String> parsed = PaletteOverrides.parse("surface=#ECECEC");

        // The whole point of the layering: one named role overrides one role.
        assertEquals(1, parsed.size());
        assertEquals("#ECECEC", parsed.get(PaletteRole.SURFACE));
    }

    @Test
    void aRoundTripKeepsEveryRole() {
        Map<PaletteRole, String> original = new LinkedHashMap<>();
        for (PaletteRole role : PaletteRole.values()) {
            original.put(role, "#10203" + role.ordinal());
        }

        assertEquals(original, PaletteOverrides.parse(PaletteOverrides.format(original)));
    }

    @Test
    void anUnknownKeyIsSkippedAndTheRestSurvives() {
        // An older or newer version may have written a role this one does not know.
        Map<PaletteRole, String> parsed =
                PaletteOverrides.parse("surface=#ECECEC;chrome=#123456;accent=#0096C9");

        assertEquals(2, parsed.size());
        assertEquals("#ECECEC", parsed.get(PaletteRole.SURFACE));
        assertEquals("#0096C9", parsed.get(PaletteRole.ACCENT));
    }

    @Test
    void anUnparsableColourIsSkippedRatherThanStored() {
        /*
         * This is the difference between an operator losing one colour and an operator
         * losing the readability of the whole client: a bad value must not become black.
         */
        Map<PaletteRole, String> parsed =
                PaletteOverrides.parse("surface=rot;accent=#0096C9;text=#GGGGGG;separator=");

        assertEquals(1, parsed.size());
        assertEquals("#0096C9", parsed.get(PaletteRole.ACCENT));
    }

    @Test
    void garbageIsSurvivedWithoutAnException() {
        // Half-written files exist. None of these may throw.
        for (String garbage : new String[] {
                ";;;", "=", "surface", "surface=", "=#ECECEC", "surface==#ECECEC",
                "surface=#ECECEC;", "\n\t", "a=b=c",
        }) {
            PaletteOverrides.parse(garbage);
        }
    }

    @Test
    void whatCountsAsAColour() {
        assertTrue(PaletteOverrides.isValidColour("#ECECEC"));
        assertTrue(PaletteOverrides.isValidColour("#ececec"));
        assertTrue(PaletteOverrides.isValidColour("  #ECECEC  "));

        assertFalse(PaletteOverrides.isValidColour(null));
        assertFalse(PaletteOverrides.isValidColour(""));
        assertFalse(PaletteOverrides.isValidColour("ECECEC"));
        assertFalse(PaletteOverrides.isValidColour("#ECECE"));
        assertFalse(PaletteOverrides.isValidColour("#ECECECE"));
        assertFalse(PaletteOverrides.isValidColour("#GGGGGG"));
        assertFalse(PaletteOverrides.isValidColour("rot"));
    }

    @Test
    void formattingAnEmptyMapGivesAnEmptyString() {
        assertEquals("", PaletteOverrides.format(new LinkedHashMap<>()));
    }
}
```

- [ ] **Schritt 2: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :core:test --tests '*PaletteOverridesTest*'
```

- [ ] **Schritt 3: Die Umsetzung schreiben**

```java
package kst4contest.model;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A set of palette overrides, encoded as one string.
 *
 * <p>One string per design rather than twelve XML elements: the preferences file is written
 * element by element by hand, and twelve more would be twelve write blocks, twelve read
 * blocks and twelve fields in a class that is already three thousand lines long.</p>
 *
 * <p>Reading is deliberately forgiving and never throws. The input is not always this
 * version's own: a hand-edited XML, a file from an older release, or a write interrupted
 * half way. An unknown key or an unparsable colour is skipped and the rest survives --
 * losing one colour is a nuisance, losing the readability of the client is not.</p>
 */
public final class PaletteOverrides {

    private static final Pattern SIX_DIGIT_HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private static final String PAIR_SEPARATOR = ";";
    private static final String KEY_VALUE_SEPARATOR = "=";

    private PaletteOverrides() {
        // Utility class.
    }

    /**
     * Reads a stored set of overrides.
     *
     * @param stored the stored string, may be null or malformed
     * @return the roles actually named with a usable colour, never null
     */
    public static Map<PaletteRole, String> parse(final String stored) {

        Map<PaletteRole, String> overrides = new LinkedHashMap<>();

        if (stored == null || stored.isBlank()) {
            return overrides;
        }

        for (String pair : stored.split(PAIR_SEPARATOR)) {

            int separator = pair.indexOf(KEY_VALUE_SEPARATOR);

            if (separator <= 0) {
                continue;
            }

            String key = pair.substring(0, separator).trim();
            String value = pair.substring(separator + 1).trim();

            if (!isValidColour(value)) {
                continue;
            }

            for (PaletteRole role : PaletteRole.values()) {
                if (role.key().equals(key)) {
                    overrides.put(role, normalise(value));
                    break;
                }
            }
        }

        return overrides;
    }

    /**
     * Writes a set of overrides for storage.
     *
     * @param overrides the roles to store; entries with an unusable colour are left out
     * @return the string to store, empty when there is nothing to store
     */
    public static String format(final Map<PaletteRole, String> overrides) {

        if (overrides == null || overrides.isEmpty()) {
            return "";
        }

        StringBuilder stored = new StringBuilder();

        for (PaletteRole role : PaletteRole.values()) {

            String colour = overrides.get(role);

            if (!isValidColour(colour)) {
                continue;
            }

            if (stored.length() > 0) {
                stored.append(PAIR_SEPARATOR);
            }

            stored.append(role.key()).append(KEY_VALUE_SEPARATOR).append(normalise(colour));
        }

        return stored.toString();
    }

    /**
     * Whether a string is a colour this application can use.
     *
     * <p>Six hexadecimal digits with a leading hash, and nothing else. The stylesheets use
     * named colours and functions too, but those are the shipped sheets' business; what an
     * operator types has one form so that what they typed is what they get.</p>
     *
     * @param colour the text to check, may be null
     * @return true when it names a colour
     */
    public static boolean isValidColour(final String colour) {
        return colour != null && SIX_DIGIT_HEX.matcher(colour.trim()).matches();
    }

    private static String normalise(final String colour) {
        return colour.trim().toUpperCase(Locale.ROOT);
    }
}
```

- [ ] **Schritt 4: Tests laufen lassen**

```bash
./gradlew :core:test --tests '*PaletteOverridesTest*'
```

Erwartung: 8 Tests grün.

- [ ] **Schritt 5: Die zwei Einstellungen in `ChatPreferences`**

Zwei Felder bei den übrigen GUI-Einstellungen, mit Getter und Setter nach dem Muster der
Nachbarn:

```java
	/**
	 * The operator's colour overrides for the daylight design, as PaletteOverrides encodes
	 * them. Per profile, like every other preference: the file this is written to is the
	 * profile's own.
	 */
	private String guiOptions_paletteOverridesDay = "";

	/** The same for the evening design. Two designs, two independent sets. */
	private String guiOptions_paletteOverridesEvening = "";
```

Getter und Setter dazu (`getGuiOptions_paletteOverridesDay`, `setGuiOptions_paletteOverridesDay`, und
dasselbe für `Evening`), ein `null` im Setter wird zu `""`.

In `writePreferencesToXmlFile()` neben den übrigen `guiOptions`-Elementen zwei Elemente
schreiben, mit `upsertDirectChildText` oder im Stil der Nachbarzeilen:
`guiOptions_paletteOverridesDay` und `guiOptions_paletteOverridesEvening`.

Im Lesepfad daneben zwei Lesestellen nach dem Muster der Nachbarn, mit `""` als Rückfall,
wenn das Element fehlt — eine Einstellungsdatei aus einer älteren Version hat es nicht.

**`CONFIG_VERSION` nicht erhöhen**, ohne zu prüfen, was daran hängt:

```bash
grep -rn "CONFIG_VERSION" core/src/main app-desktop/src/main
```

Trägt die Zahl eine Migrationslogik, gehört die Erhöhung dazu; trägt sie nur eine Notiz, nicht.
Die Entscheidung in der Commit-Nachricht festhalten.

- [ ] **Schritt 6: Den Test für die Rundreise durch die Datei schreiben**

In `core/src/test/java/kst4contest/model/ChatPreferencesPaletteTest.java`:

```java
package kst4contest.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The overrides survive a write and a read, and the two designs stay apart.
 *
 * <p>The second part is the one worth pinning: two designs sharing one storage mechanism is
 * exactly the shape in which a change to the evening palette quietly drags the daylight one
 * with it.</p>
 */
class ChatPreferencesPaletteTest {

    @Test
    void theTwoDesignsAreStoredIndependently() {
        ChatPreferences prefs = new ChatPreferences();

        prefs.setGuiOptions_paletteOverridesDay("surface=#FFFFFF");
        prefs.setGuiOptions_paletteOverridesEvening("surface=#000000");

        assertEquals("surface=#FFFFFF", prefs.getGuiOptions_paletteOverridesDay());
        assertEquals("surface=#000000", prefs.getGuiOptions_paletteOverridesEvening());
    }

    @Test
    void anUnsetOverrideReadsAsEmptyAndNotAsNull() {
        // A preferences file from an older release has no such element at all.
        ChatPreferences prefs = new ChatPreferences();

        assertEquals("", prefs.getGuiOptions_paletteOverridesDay());
        assertEquals("", prefs.getGuiOptions_paletteOverridesEvening());
    }

    @Test
    void aNullSetterValueBecomesEmpty() {
        ChatPreferences prefs = new ChatPreferences();

        prefs.setGuiOptions_paletteOverridesDay(null);

        assertEquals("", prefs.getGuiOptions_paletteOverridesDay());
    }
}
```

Eine Rundreise über die echte Datei schreibt in das Benutzerprofil und gehört deshalb
**nicht** hierher — `ChatPreferencesStationMapVisibilityTest` tut das bereits und ist das
Muster, falls Marc es ausdrücklich will.

- [ ] **Schritt 7: Bauen, Testzahl prüfen, committen**

```bash
./gradlew clean build
grep -h "<testsuite " */build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*skipped="\([0-9]*\)".*failures="\([0-9]*\)".*errors="\([0-9]*\)".*/\1 \2 \3 \4/' | awk '{t+=$1; s+=$2; f+=$3; e+=$4} END {print "tests="t" skipped="s" failures="f" errors="e}'
git add core/src/main/java/kst4contest/model/ core/src/test/java/kst4contest/model/
git commit -m "Store palette overrides as one string per design, per profile" -- \
        core/src/main/java/kst4contest/model/ core/src/test/java/kst4contest/model/
```

Erwartung: `failures=0 errors=0`, und `tests` um die neu geschriebenen Tests über der
Grundlinie von 868. **Die Zahl wird abgelesen, nicht aus diesem Plan übernommen** — in diesem
Zweig ist genau diese Rechnung schon zweimal falsch gewesen, während die Messung recht hatte.
Die abgelesene Zahl gehört in die Commit-Nachricht.

---

## Aufgabe 3: Welche Rollen eine Stilvorlage wirklich nennt

**Der Prüfschwerpunkt dieser Etappe.** `JavaFxStylesheet.read` füllt jede fehlende Rolle mit
einem Modena-Rückfallwert (`JavaFxStylesheet.kt:89`–`:112`). Das ist für Schicht 1 richtig und
für Schicht 2 falsch: als Dateischicht benutzt, würde eine Datei mit der einzigen Zeile
`-fx-base: #ECECEC;` **alle sechs** Rollen überschreiben — gegen die Spezifikation, und still.

Wie weit die beiden Fragen auseinanderliegen, zeigt die Tagesvorlage: ihr `.root` enthält
**nur** `-fx-font-size`. `read` liefert daraus eine vollständige Palette, `declaredRoles`
liefert daraus nichts — und beides ist richtig.

**Dateien:**
- Ändern: `app-desktop/src/main/kotlin/kst4contest/view/compose/JavaFxStylesheet.kt`
- Anlegen: `app-desktop/src/test/kotlin/kst4contest/view/compose/StylesheetDeclaredRolesTest.kt`

**Schnittstellen:**
- Liefert: `JavaFxStylesheet.declaredRoles(source: String): Map<PaletteRole, Color>` — nur die
  Rollen, die `source` an ihrem eigenen Selektor tatsächlich nennt.
- Liefert: `JavaFxStylesheet.declaredRolesOfFile(file: java.io.File): Map<PaletteRole, Color>` —
  dasselbe für eine Datei, leer bei fehlender oder unlesbarer Datei, wirft nie.
- `read(resourcePath)` bleibt **unverändert** in Signatur und Verhalten: Schicht 1 braucht eine
  vollständige Palette.
- Dazu nötig: `parseBlocks` und `colorOf` sind heute `private`. Sie bleiben privat —
  `declaredRoles` steht in derselben Datei und benutzt sie von innen.

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

```kotlin
package kst4contest.view.compose

import kst4contest.model.PaletteRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Which roles a stylesheet actually names.
 *
 * Not the same question as `read`, and conflating the two is the trap of this stage: `read`
 * fills a missing role from the Modena defaults, so a file saying only `-fx-base` would
 * silently override all six. A layer needs presence, not a complete palette.
 *
 * How far apart the two questions are is visible in the shipped sheets themselves -- the
 * daylight one states no colour at all, and `read` still returns a full palette for it.
 */
class StylesheetDeclaredRolesTest {

    private fun shipped(name: String): String =
        JavaFxStylesheet::class.java.getResource(name)!!.readText()

    @Test
    fun aSheetNamingOneRoleDeclaresOnlyThatRole() {
        val declared = JavaFxStylesheet.declaredRoles(".root { -fx-base: #ECECEC; }")

        assertEquals(setOf(PaletteRole.SURFACE), declared.keys)
    }

    @Test
    fun aRoleStatedOutsideRootIsFoundAtItsOwnSelector() {
        /*
         * Three of the six live outside .root. A reader that only looked there would never
         * find the text colour or the separator line -- which is half the palette.
         */
        val declared = JavaFxStylesheet.declaredRoles(
            """
            .label { -fx-text-fill: #112233; }
            .separator *.line { -fx-background-color: #445566; }
            """.trimIndent()
        )

        assertEquals(setOf(PaletteRole.TEXT, PaletteRole.SEPARATOR), declared.keys)
    }

    @Test
    fun theSamePropertyUnderAnotherSelectorIsNotARole() {
        // -fx-background-color appears a dozen times in the evening sheet. One is the role.
        val declared = JavaFxStylesheet.declaredRoles(
            ".scroll-bar { -fx-background-color: #445566; } .button:hover { -fx-text-fill: red; }"
        )

        assertTrue(declared.isEmpty(), "found roles where there are none: ${declared.keys}")
    }

    @Test
    fun aSheetNamingNothingDeclaresNothing() {
        assertTrue(JavaFxStylesheet.declaredRoles("").isEmpty())
        assertTrue(JavaFxStylesheet.declaredRoles("/* just a comment */").isEmpty())
        assertTrue(JavaFxStylesheet.declaredRoles(".button { -fx-padding: 2px; }").isEmpty())
    }

    @Test
    fun theShippedDaylightSheetStatesNoColourAtAll() {
        /*
         * Measured, not assumed: its .root carries only -fx-font-size, and every colour in
         * the daylight design comes from JavaFX's Modena defaults by way of `read`. If this
         * ever starts declaring a role, the daylight palette has gained a second source and
         * the resolver's layer 1 needs looking at.
         */
        val declared = JavaFxStylesheet.declaredRoles(shipped("/KST4ContestDefaultDay.css"))

        assertTrue(declared.isEmpty(), "the daylight sheet now declares ${declared.keys}")
    }

    @Test
    fun theShippedEveningSheetStatesFiveOfTheSixRoles() {
        /*
         * It states the surface, the field interior, the accent, the label text and the
         * separator line, and derives the window surface rather than stating it. This pins
         * the selector-and-property mapping against a real file: get a selector wrong and
         * this count drops without any other test noticing.
         */
        val declared = JavaFxStylesheet.declaredRoles(shipped("/KST4ContestDefaultEvening.css"))

        assertEquals(
            setOf(
                PaletteRole.SURFACE,
                PaletteRole.FIELD_INTERIOR,
                PaletteRole.ACCENT,
                PaletteRole.TEXT,
                PaletteRole.SEPARATOR,
            ),
            declared.keys,
        )
    }

    @Test
    fun aMissingFileDeclaresNothingRatherThanThrowing() {
        val absent = File("/nonexistent/kst4contest/does-not-exist.css")

        assertTrue(JavaFxStylesheet.declaredRolesOfFile(absent).isEmpty())
    }

    @Test
    fun anUnreadableOrNonsenseFileDeclaresNothingRatherThanThrowing() {
        /*
         * A client that will not start because of a broken colour file is worse than one in
         * the wrong colours. Half-written files and files that are not CSS at all both exist,
         * and this one is hand-edited by the operator.
         */
        val nonsense = File.createTempFile("kst4contest-palette", ".css")
        try {
            nonsense.writeText("this is not css { -fx-base")
            assertTrue(JavaFxStylesheet.declaredRolesOfFile(nonsense).isEmpty())
        } finally {
            nonsense.delete()
        }

        val directory = File.createTempFile("kst4contest-palette-dir", "").let {
            it.delete()
            it.mkdirs()
            it
        }
        try {
            assertTrue(JavaFxStylesheet.declaredRolesOfFile(directory).isEmpty())
        } finally {
            directory.delete()
        }
    }
}
```

- [ ] **Schritt 2: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :app-desktop:test --tests '*StylesheetDeclaredRolesTest*'
```

Erwartung: Übersetzungsfehler, `declaredRoles` existiert nicht.

- [ ] **Schritt 3: Die beiden Funktionen ergänzen**

In `JavaFxStylesheet`, hinter `read`, unter Wiederverwendung der vorhandenen privaten
`parseBlocks` und `colorOf`:

```kotlin
    /**
     * The roles a stylesheet source actually names.
     *
     * Deliberately not [read]: that one fills a missing role from the Modena defaults, which
     * is right for the shipped sheets and wrong for a layer meant to override one role at a
     * time. A sheet saying only `-fx-base` must declare exactly one role.
     *
     * Each role is looked up at its own selector, because three of the six are stated
     * outside `.root` and one of those properties -- `-fx-background-color` -- appears a
     * dozen times elsewhere in the evening sheet. `.root` stays the scope in which a
     * `-fx-` reference or a `derive(...)` is resolved, exactly as [read] resolves them.
     */
    fun declaredRoles(source: String): Map<PaletteRole, Color> {
        val blocks = parseBlocks(source)
        val root = blocks[".root"].orEmpty()

        return PaletteRole.values().mapNotNull { role ->
            val stated = blocks[role.selector()]?.get(role.cssProperty())
                ?: return@mapNotNull null
            val colour = colorOf(stated, root) ?: return@mapNotNull null
            role to colour
        }.toMap()
    }

    /**
     * The roles a stylesheet file names, or none when it cannot be read.
     *
     * Never throws. A client that will not start because of a broken colour file is worse
     * than a client in the wrong colours, and this file is one the operator edited by hand.
     */
    fun declaredRolesOfFile(file: java.io.File): Map<PaletteRole, Color> =
        runCatching {
            if (!file.isFile) emptyMap() else declaredRoles(file.readText())
        }.getOrDefault(emptyMap())
```

Import ergänzen: `kst4contest.model.PaletteRole`.

`colorOf` liefert bei einem Wert, den es nicht versteht — einem Farbverlauf etwa —, `null`;
die Rolle gilt dann als nicht genannt und fällt auf die darunterliegende Schicht zurück. Das
ist gewollt und wird vom Test `aSheetNamingNothingDeclaresNothing` mitabgedeckt.

- [ ] **Schritt 4: Die `derive`-Genauigkeit bewerten und die Entscheidung festhalten**

`JavaFxStylesheet.derive`s Javadoc nennt diese Etappe ausdrücklich: die Nachbildung weicht bei
**gesättigten** Farben um bis zu drei Stufen je Kanal ab, und das war nur deshalb unerheblich,
weil jedes `derive(...)` in beiden ausgelieferten Vorlagen von `-fx-base` ausgeht — einem Grau.

Eine handgeschriebene Datei kann `-fx-base` auf ein gesättigtes Rot setzen. Dann geht
`-fx-control-inner-background: derive(-fx-base, 35%)` durch die ungenaue Stelle.

Die Festlegung, und sie gehört als Kommentar an `declaredRoles`:

> Eine Abweichung von drei Stufen je Kanal ist in einer Farbe, die der Operateur selbst
> gewählt hat, nicht wahrnehmbar und nicht wichtig. Der Pin bleibt, wie er ist; `derive`
> wird für diese Etappe **nicht** angefasst. Was der Reiter setzt, geht ohnehin nicht durch
> `derive`: die Auflösung nimmt fertige Farben.

Dazu den veralteten Satz im `derive`-Javadoc berichtigen — er sagt, Etappe 8 müsse das
„revisit", und das ist mit dieser Entscheidung erledigt. Stehen lassen würde einen späteren
Leser auf eine Aufgabe schicken, die es nicht mehr gibt.

- [ ] **Schritt 5: Tests laufen lassen, bauen, committen**

```bash
./gradlew :app-desktop:test --tests '*StylesheetDeclaredRolesTest*'
./gradlew clean build
git add app-desktop/src/main/kotlin/kst4contest/view/compose/JavaFxStylesheet.kt \
        app-desktop/src/test/kotlin/kst4contest/view/compose/StylesheetDeclaredRolesTest.kt
git commit -m "Read which roles a stylesheet names, not a whole palette with defaults" -- \
        app-desktop/src/main/kotlin/kst4contest/view/compose/JavaFxStylesheet.kt \
        app-desktop/src/test/kotlin/kst4contest/view/compose/StylesheetDeclaredRolesTest.kt
```

---

## Aufgabe 4: Die Dreischicht-Auflösung

Das Herz der Etappe, und reine Funktion.

**Dateien:**
- Anlegen: `app-desktop/src/main/kotlin/kst4contest/view/compose/PaletteResolution.kt`
- Anlegen: `app-desktop/src/test/kotlin/kst4contest/view/compose/PaletteResolutionTest.kt`

**Schnittstellen:**
- Liefert: `enum class PaletteSource { SHIPPED, FILE, CHANGED }`
- Liefert: `data class ResolvedRole(val colour: Color, val source: PaletteSource)`
- Liefert: `data class ResolvedPalette(val roles: Map<PaletteRole, ResolvedRole>, val palette: JavaFxPalette)`
- Liefert:
  ```kotlin
  fun resolvePalette(
      shipped: JavaFxPalette,
      fromFile: Map<PaletteRole, Color>,
      changed: Map<PaletteRole, Color>,
  ): ResolvedPalette
  ```
- `roles` enthält **immer alle sechs** Rollen; `palette` ist `shipped` mit den aufgelösten
  Rollen eingesetzt und den übrigen Feldern von `shipped` (`buttonHoverGradient`,
  `buttonPressedBorder`, `fontSizePx`).

**`textAccent` muss der Akzentrolle folgen, sonst tut der Akzentregler so gut wie nichts.**
`Theme.kt:97` bildet Materials `primary` auf `textAccent` ab und **nicht** auf `accent`, mit
einer ausdrücklichen Begründung: `-fx-accent` färbt nur JavaFX' Auswahlbalken, während das
Grün aus `.text-field .text` das ist, was der Operateur als Akzent sieht. Setzt er also
`-fx-accent` und `textAccent` bleibt beim Grün der Vorlage, ändert sich fast nichts Sichtbares
— genau der Fehlschlag, den Etappe 3b viermal hatte.

Also: ist `ACCENT` überschrieben, nimmt `textAccent` dieselbe Farbe. Ist es nicht
überschrieben, bleibt `textAccent` der Mittelwert, den die Vorlage ergibt.

**Folge, die mitgemacht werden muss:** `Kst4ContestMenuRow` zeichnet den offenen Menütitel als
`palette.textAccent` auf `palette.accent` (`MainMenuBar.kt:165` und `:169`). Fallen die beiden
zusammen, ist der Titel unsichtbar. Der offene Titel nimmt deshalb eine Farbe, die zum Akzent
kontrastiert — `Theme.kt`s vorhandenes privates `contrastingText` wird dafür `internal`.

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

```kotlin
package kst4contest.view.compose

import androidx.compose.ui.graphics.Color
import kst4contest.model.PaletteRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The three layers and which one wins, role by role.
 *
 * The provenance is not decoration. Two sources that can decide the same colour is a place
 * where nobody can later say why a colour is what it is, and saying so per role is the one
 * thing that makes the arrangement defensible -- so it is pinned as hard as the colours.
 */
class PaletteResolutionTest {

    private val shipped = JavaFxStylesheet.read("/KST4ContestDefaultDay.css")

    private val red = Color(0xFFFF0000)
    private val green = Color(0xFF00FF00)

    @Test
    fun withNothingSetEveryRoleComesFromTheShippedSheet() {
        val resolved = resolvePalette(shipped, emptyMap(), emptyMap())

        assertEquals(PaletteRole.values().toSet(), resolved.roles.keys)
        resolved.roles.forEach { (role, it) ->
            assertEquals(PaletteSource.SHIPPED, it.source, "$role")
        }
        assertEquals(shipped.base, resolved.palette.base)
        assertEquals(shipped.accent, resolved.palette.accent)
    }

    @Test
    fun aFileOverridesTheShippedSheetForTheRolesItNames() {
        val resolved = resolvePalette(shipped, mapOf(PaletteRole.SURFACE to red), emptyMap())

        assertEquals(red, resolved.roles[PaletteRole.SURFACE]!!.colour)
        assertEquals(PaletteSource.FILE, resolved.roles[PaletteRole.SURFACE]!!.source)

        // And only those roles.
        assertEquals(PaletteSource.SHIPPED, resolved.roles[PaletteRole.ACCENT]!!.source)
        assertEquals(shipped.accent, resolved.roles[PaletteRole.ACCENT]!!.colour)
    }

    @Test
    fun aChangeOverridesBothTheFileAndTheShippedSheet() {
        val resolved = resolvePalette(
            shipped,
            mapOf(PaletteRole.SURFACE to red),
            mapOf(PaletteRole.SURFACE to green),
        )

        assertEquals(green, resolved.roles[PaletteRole.SURFACE]!!.colour)
        assertEquals(PaletteSource.CHANGED, resolved.roles[PaletteRole.SURFACE]!!.source)
    }

    @Test
    fun theResolvedRolesReachThePaletteTheWindowsDraw() {
        val resolved = resolvePalette(
            shipped,
            emptyMap(),
            mapOf(
                PaletteRole.SURFACE to red,
                PaletteRole.WINDOW_SURFACE to green,
                PaletteRole.FIELD_INTERIOR to red,
                PaletteRole.TEXT to green,
                PaletteRole.ACCENT to red,
                PaletteRole.SEPARATOR to green,
            ),
        )

        assertEquals(red, resolved.palette.base)
        assertEquals(green, resolved.palette.windowBackground)
        assertEquals(red, resolved.palette.controlInnerBackground)
        assertEquals(green, resolved.palette.labelTextFill)
        assertEquals(red, resolved.palette.accent)
        assertEquals(green, resolved.palette.separatorLine)
    }

    @Test
    fun theDerivedFieldsStayWithTheShippedSheet() {
        // The hover gradient, the pressed border and the font size are not roles the operator
        // sets here; they follow the shipped sheet rather than becoming further decisions.
        val resolved = resolvePalette(shipped, emptyMap(), mapOf(PaletteRole.SURFACE to red))

        assertEquals(shipped.buttonHoverGradient, resolved.palette.buttonHoverGradient)
        assertEquals(shipped.buttonPressedBorder, resolved.palette.buttonPressedBorder)
        assertEquals(shipped.fontSizePx, resolved.palette.fontSizePx)
    }

    @Test
    fun aChosenAccentIsAlsoTheAccentTheOperatorActuallySees() {
        /*
         * Theme.kt maps Material's primary onto textAccent and not onto accent, because
         * -fx-accent only paints JavaFX's selection bar while the green of .text-field .text
         * is what reads as the accent. Leave textAccent on the sheet's green and setting the
         * accent changes almost nothing visible -- a setting written through and not drawn,
         * which is precisely the fault class the drawing-layer tests exist for.
         */
        val resolved = resolvePalette(shipped, emptyMap(), mapOf(PaletteRole.ACCENT to red))

        assertEquals(red, resolved.palette.accent)
        assertEquals(red, resolved.palette.textAccent)
    }

    @Test
    fun anUntouchedAccentLeavesTheSheetsOwnTextAccentAlone() {
        // Without an override the sheet's gradient midpoint stands; it is not the accent.
        val resolved = resolvePalette(shipped, emptyMap(), mapOf(PaletteRole.SURFACE to red))

        assertEquals(shipped.textAccent, resolved.palette.textAccent)
    }

    @Test
    fun theWindowSurfaceIsNotDraggedAlongByTheSurface() {
        /*
         * The reason these are two roles rather than one: the sheets state the window
         * surface separately, and an operator who sets only the control surface must not
         * have the window surface changed under them.
         */
        val resolved = resolvePalette(shipped, emptyMap(), mapOf(PaletteRole.SURFACE to red))

        assertEquals(red, resolved.palette.base)
        assertEquals(shipped.windowBackground, resolved.palette.windowBackground)
    }
}
```

- [ ] **Schritt 2: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :app-desktop:test --tests '*PaletteResolutionTest*'
```

- [ ] **Schritt 3: Die Umsetzung schreiben**

```kotlin
package kst4contest.view.compose

import androidx.compose.ui.graphics.Color
import kst4contest.model.PaletteRole

/** Which of the three layers a colour in force came from. */
enum class PaletteSource { SHIPPED, FILE, CHANGED }

/** One role's colour, and where it came from. */
data class ResolvedRole(val colour: Color, val source: PaletteSource)

/**
 * The palette in force, and the provenance of every role in it.
 *
 * @param roles always all six, so the settings tab can show a source for each
 * @param palette what the windows draw
 */
data class ResolvedPalette(
    val roles: Map<PaletteRole, ResolvedRole>,
    val palette: JavaFxPalette,
)

/**
 * Resolves the three layers, role by role: the later layer wins.
 *
 * The shipped sheet is the floor and always complete. A file and a change each carry only
 * the roles they name — which is why [JavaFxStylesheet.declaredRoles] exists rather than
 * `read`: a file saying only `-fx-base` must override exactly one role.
 *
 * Fields that are not roles — the hover gradient, the pressed border, the font size — stay
 * with the shipped sheet. They are derived values rather than decisions an operator wants to
 * make one by one.
 *
 * The exception is `textAccent`, which follows the accent role: Material's primary is mapped
 * onto it rather than onto `accent`, because `-fx-accent` paints only JavaFX's selection bar
 * while the green of `.text-field .text` is what an operator reads as the accent. Left on
 * the sheet's own value, a chosen accent would change almost nothing visible.
 */
fun resolvePalette(
    shipped: JavaFxPalette,
    fromFile: Map<PaletteRole, Color>,
    changed: Map<PaletteRole, Color>,
): ResolvedPalette {

    val shippedColour = mapOf(
        PaletteRole.SURFACE to shipped.base,
        PaletteRole.WINDOW_SURFACE to shipped.windowBackground,
        PaletteRole.FIELD_INTERIOR to shipped.controlInnerBackground,
        PaletteRole.TEXT to shipped.labelTextFill,
        PaletteRole.ACCENT to shipped.accent,
        PaletteRole.SEPARATOR to shipped.separatorLine,
    )

    val roles = PaletteRole.values().associateWith { role ->
        when {
            changed.containsKey(role) -> ResolvedRole(changed.getValue(role), PaletteSource.CHANGED)
            fromFile.containsKey(role) -> ResolvedRole(fromFile.getValue(role), PaletteSource.FILE)
            else -> ResolvedRole(shippedColour.getValue(role), PaletteSource.SHIPPED)
        }
    }

    fun colourOf(role: PaletteRole) = roles.getValue(role).colour

    val accentIsChosen = roles.getValue(PaletteRole.ACCENT).source != PaletteSource.SHIPPED

    return ResolvedPalette(
        roles = roles,
        palette = shipped.copy(
            base = colourOf(PaletteRole.SURFACE),
            windowBackground = colourOf(PaletteRole.WINDOW_SURFACE),
            controlInnerBackground = colourOf(PaletteRole.FIELD_INTERIOR),
            labelTextFill = colourOf(PaletteRole.TEXT),
            accent = colourOf(PaletteRole.ACCENT),
            textAccent = if (accentIsChosen) colourOf(PaletteRole.ACCENT) else shipped.textAccent,
            separatorLine = colourOf(PaletteRole.SEPARATOR),
        ),
    )
}
```

- [ ] **Schritt 3b: Den offenen Menütitel lesbar halten**

In `Theme.kt` wird `contrastingText` von `private` auf `internal` gesetzt, mit einem Satz
dazu, warum es die Datei verlässt. In `MainMenuBar.kt:165` nimmt der offene Titel dann:

```kotlin
            color = if (open) contrastingText(palette.accent) else palette.labelTextFill,
```

Vorher stand dort `palette.textAccent` — eine Farbe, die jetzt dieselbe sein kann wie der
Hintergrund dahinter. `contrastingText` wählt Schwarz oder Weiß nach derselben
Helligkeitsformel, die JavaFX benutzt, und ist damit von Bauart aus lesbar; geprüft wird es in
der GUI-Abnahme von Aufgabe 9, Punkt 1.

- [ ] **Schritt 4: Tests laufen lassen, bauen, committen**

```bash
./gradlew :app-desktop:test --tests '*PaletteResolutionTest*'
./gradlew clean build
git commit -m "Resolve the three palette layers role by role, with their provenance" -- \
        app-desktop/src/main/kotlin/kst4contest/view/compose/PaletteResolution.kt \
        app-desktop/src/main/kotlin/kst4contest/view/compose/Theme.kt \
        app-desktop/src/main/kotlin/kst4contest/view/compose/MainMenuBar.kt \
        app-desktop/src/test/kotlin/kst4contest/view/compose/PaletteResolutionTest.kt
```

Die Datei muss vorher `git add`ed sein, damit die Pfadangabe sie findet; `git add` mit
denselben vier Pfaden, nicht `git add -A`.

---

## Aufgabe 5: Der Kontrastwächter

> **Eine Lücke der Spezifikation, gemessen gefüllt.** Der Entwurf nennt das
> WCAG-Kontrastverhältnis als Maß, aber **keine** Grenze. Der naheliegende Boden wäre WCAG AA
> mit 4,5. Die ausgelieferten Vorlagen halten diesen Boden selbst nicht
> ein:
>
> | Paar | Tag | Abend |
> |---|---|---|
> | Text auf Fläche | 17,78 | 7,26 |
> | Text im Feld | 21,0 | **≈2,9** |
> | Akzent auf Fläche | **2,86** | **2,26** |
>
> Eine absolute Grenze würde also ab dem ersten Start in beiden Entwürfen warnen, ohne dass
> der Operateur etwas getan hätte — und eine Warnung, die immer leuchtet, bringt ihm bei,
> Warnungen zu übersehen. Die Grenze bis zum Durchkommen der Vorlagen zu senken (auf etwa
> 2,2) macht sie wertlos.
>
> Der Wächter wird deshalb **relativ**: gewarnt wird, wenn ein Paar unter dem Boden liegt
> **und** schlechter ist als derselbe Paarwert der Auslieferung. Damit warnt der
> Auslieferungsstand von Bauart aus nie, eine Verbesserung warnt nie, und berichtet wird
> genau das, was der Entwurf eigentlich meint: „diese Änderung hat das Lesen schwerer
> gemacht, als es war".
>
> Marc hat das mit dem Plan abgenommen. Die beiden Abendwerte sind von Hand gerechnet, der
> Feldwert über eine Annäherung an `derive`; der Test rechnet sie echt.

**Dateien:**
- Anlegen: `app-desktop/src/main/kotlin/kst4contest/view/compose/ContrastRule.kt`
- Anlegen: `app-desktop/src/test/kotlin/kst4contest/view/compose/ContrastRuleTest.kt`

**Schnittstellen:**
- Liefert: `fun contrastRatio(a: Color, b: Color): Double` — WCAG, 1.0 bis 21.0
- Liefert: `data class ContrastWarning(val what: String, val ratio: Double, val shippedRatio: Double)`
- Liefert: `fun contrastWarnings(resolved: JavaFxPalette, shipped: JavaFxPalette): List<ContrastWarning>`
  — die drei Paare, die wirklich übereinanderliegen: Text auf Fläche, Text auf Feldinnerem,
  Akzent auf Fläche.
- Grenze: `CONTRAST_FLOOR = 4.5` (WCAG AA für Fließtext).

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

```kotlin
package kst4contest.view.compose

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * The readability measure, which only warns.
 *
 * A pure function, and therefore the part that can be wrong without anyone noticing: a
 * warning that never fires is indistinguishable from a palette that is fine, and a warning
 * that always fires teaches the operator to ignore warnings. The reference numbers below are
 * the ones WCAG itself states for black and white.
 *
 * The comparison is against the shipped sheet rather than against the bare floor, because
 * the shipped sheets do not meet that floor themselves -- measured: the evening accent sits
 * at 2.26 on its own surface. What is reported is therefore "this change made a pair harder
 * to read than it shipped", which is the question the operator is actually asking.
 */
class ContrastRuleTest {

    private val day = JavaFxStylesheet.read("/KST4ContestDefaultDay.css")
    private val evening = JavaFxStylesheet.read("/KST4ContestDefaultEvening.css")

    private fun assertClose(expected: Double, actual: Double, what: String) {
        assertTrue(abs(expected - actual) < 0.05, "$what: expected ~$expected but was $actual")
    }

    @Test
    fun blackOnWhiteIsTheMaximumOf21() {
        assertClose(21.0, contrastRatio(Color.Black, Color.White), "black on white")
        assertClose(21.0, contrastRatio(Color.White, Color.Black), "white on black")
    }

    @Test
    fun aColourAgainstItselfIsTheMinimumOf1() {
        assertClose(1.0, contrastRatio(Color.Black, Color.Black), "black on black")
        assertClose(1.0, contrastRatio(Color(0xFF8899AA), Color(0xFF8899AA)), "grey on grey")
    }

    @Test
    fun theRatioDoesNotDependOnWhichIsTheBackground() {
        assertEquals(contrastRatio(Color(0xFF123456), Color(0xFFEEDDCC)),
                     contrastRatio(Color(0xFFEEDDCC), Color(0xFF123456)))
    }

    @Test
    fun midGreyOnWhiteIsAroundFourAndAHalf() {
        // #767676 on white is the canonical WCAG AA boundary example: 4.54.
        assertClose(4.54, contrastRatio(Color(0xFF767676), Color.White), "#767676 on white")
    }

    @Test
    fun aShippedSheetNeverWarnsAboutItself() {
        /*
         * The property the whole relative rule exists for. Both sheets have pairs under the
         * floor -- the day accent at 2.86, the evening accent at 2.26 -- so an absolute rule
         * would warn on a client nobody has touched.
         */
        assertTrue(contrastWarnings(day, day).isEmpty(), "the daylight sheet warns about itself")
        assertTrue(contrastWarnings(evening, evening).isEmpty(), "the evening sheet warns about itself")
    }

    @Test
    fun theShippedSheetsReallyDoHavePairsUnderTheFloor() {
        /*
         * Measured here rather than asserted in prose, because it is the premise of the rule
         * above. If a later change to the sheets makes every pair pass, the relative rule
         * becomes equivalent to the absolute one and the deviation can be dropped.
         */
        val accentOnSurface = contrastRatio(day.accent, day.base)

        assertTrue(
            accentOnSurface < CONTRAST_FLOOR,
            "the daylight accent now passes at $accentOnSurface; the relative rule may no "
                    + "longer be needed",
        )
    }

    @Test
    fun blackTextOnABlackSurfaceWarnsAboutEveryPairItAppearsIn() {
        val unreadable = day.copy(
            base = Color.Black,
            controlInnerBackground = Color.Black,
            labelTextFill = Color.Black,
            accent = Color.Black,
        )

        val warnings = contrastWarnings(unreadable, day)

        assertEquals(3, warnings.size, "all three pairs must warn: ${warnings.map { it.what }}")
        warnings.forEach {
            assertTrue(it.ratio < CONTRAST_FLOOR, it.what)
            assertTrue(it.ratio < it.shippedRatio, "${it.what} is not actually worse than shipped")
        }
    }

    @Test
    fun anImprovementDoesNotWarnEvenWhileStillUnderTheFloor() {
        /*
         * The evening accent ships at 2.26. An operator who moves it to 3.5 has made things
         * better and must not be told off for it -- that is how a guard loses its authority.
         */
        val better = evening.copy(accent = Color.White)

        val accentWarnings = contrastWarnings(better, evening).filter { it.what.contains("Accent") }

        assertTrue(accentWarnings.isEmpty(), "an improvement warned: $accentWarnings")
    }

    @Test
    fun aPairThatStaysComfortablyReadableDoesNotWarn() {
        // Worse than shipped but still well over the floor: nothing to say.
        val slightlyWorse = day.copy(labelTextFill = Color(0xFF444444))

        val textWarnings = contrastWarnings(slightlyWorse, day).filter { it.what.contains("Text") }

        assertTrue(textWarnings.isEmpty(), "a readable pair warned: $textWarnings")
    }
}
```

- [ ] **Schritt 2: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :app-desktop:test --tests '*ContrastRuleTest*'
```

- [ ] **Schritt 3: Die Umsetzung schreiben**

```kotlin
package kst4contest.view.compose

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/** WCAG AA for body text. Below this a pair is reported, never refused. */
const val CONTRAST_FLOOR = 4.5

/**
 * One pair that reads badly, named the way the settings tab shows it.
 *
 * @param ratio what the pair measures now
 * @param shippedRatio what the same pair measures in the shipped sheet, so the tab can say
 *        how far the change moved it
 */
data class ContrastWarning(val what: String, val ratio: Double, val shippedRatio: Double)

/**
 * The WCAG contrast ratio of two colours, between 1.0 and 21.0.
 *
 * Relative luminance and not brightness: the two are easy to confuse, and the JavaFX colour
 * derivation in this same package is a standing reminder that guessing a colour formula
 * produces values that look plausible and are wrong.
 */
fun contrastRatio(a: Color, b: Color): Double {
    val lighter = maxOf(relativeLuminance(a), relativeLuminance(b))
    val darker = minOf(relativeLuminance(a), relativeLuminance(b))
    return (lighter + 0.05) / (darker + 0.05)
}

private fun relativeLuminance(colour: Color): Double {
    fun channel(value: Float): Double {
        val v = value.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    return 0.2126 * channel(colour.red) +
            0.7152 * channel(colour.green) +
            0.0722 * channel(colour.blue)
}

/**
 * The pairs that actually sit on top of each other and read worse than they shipped.
 *
 * Two conditions, both required: under [CONTRAST_FLOOR], and worse than the same pair in the
 * shipped sheet. The second is there because the shipped sheets do not meet the floor
 * themselves -- the evening accent measures 2.26 on its own surface -- so an absolute rule
 * would warn about a client nobody has touched, which is how an operator learns to ignore
 * warnings. With this rule the shipped state never warns and an improvement never warns.
 *
 * Only three pairs, deliberately. Every pair of six roles would be fifteen, most of which
 * never meet on screen, and a warning about a pair that cannot occur costs the same
 * attention as a real one.
 */
fun contrastWarnings(resolved: JavaFxPalette, shipped: JavaFxPalette): List<ContrastWarning> {

    fun pairsOf(palette: JavaFxPalette) = listOf(
        "Text on the surface" to (palette.labelTextFill to palette.base),
        "Text inside a field" to (palette.labelTextFill to palette.controlInnerBackground),
        "Accent on the surface" to (palette.accent to palette.base),
    )

    val shippedRatios = pairsOf(shipped).associate { (what, colours) ->
        what to contrastRatio(colours.first, colours.second)
    }

    return pairsOf(resolved).mapNotNull { (what, colours) ->
        val ratio = contrastRatio(colours.first, colours.second)
        val shippedRatio = shippedRatios.getValue(what)

        if (ratio < CONTRAST_FLOOR && ratio < shippedRatio) {
            ContrastWarning(what, ratio, shippedRatio)
        } else {
            null
        }
    }
}
```

- [ ] **Schritt 4: Tests laufen lassen**

```bash
./gradlew :app-desktop:test --tests '*ContrastRuleTest*'
```

Schlägt `aShippedSheetNeverWarnsAboutItself` fehl, ist die relative Bedingung falsch
herum — nicht die Vorlage schuld.

- [ ] **Schritt 5: Bauen, committen**

```bash
./gradlew clean build
git add app-desktop/src/main/kotlin/kst4contest/view/compose/ContrastRule.kt \
        app-desktop/src/test/kotlin/kst4contest/view/compose/ContrastRuleTest.kt
git commit -m "Warn when a change reads worse than the shipped sheet, not below a fixed floor" -- \
        app-desktop/src/main/kotlin/kst4contest/view/compose/ContrastRule.kt \
        app-desktop/src/test/kotlin/kst4contest/view/compose/ContrastRuleTest.kt
```

---

## Aufgabe 6: Der Store, und dass eine Änderung alle Fenster erreicht

**Dateien:**
- Anlegen: `app-desktop/src/main/kotlin/kst4contest/view/compose/PaletteStore.kt`
- Anlegen: `app-desktop/src/test/kotlin/kst4contest/view/compose/PaletteStoreTest.kt`
- Ändern: `app-desktop/src/main/kotlin/kst4contest/view/compose/Theme.kt`

**Schnittstellen:**
- Liefert:
  ```kotlin
  class PaletteStore(
      private val shippedOf: (darkMode: Boolean) -> JavaFxPalette,
      private val fileRolesOf: (darkMode: Boolean) -> Map<PaletteRole, Color>,
      private val storedOverridesOf: (darkMode: Boolean) -> String,
      private val storeOverrides: (darkMode: Boolean, encoded: String) -> Unit,
  )
  ```
  mit `resolved(darkMode: Boolean): ResolvedPalette`, `set(darkMode, role, colour: String)`,
  `resetToShipped(darkMode)`, `discardChanges(darkMode)`, `restoreSnapshot(darkMode)`,
  `takeSnapshot()`, und `shipped(darkMode): JavaFxPalette` für die Rettungsleine.
- Die vier Lambdas statt eines `ChatPreferences`: der Store wird damit ohne Benutzerprofil auf
  der Platte testbar — `ChatPreferences`' Konstruktor kopiert Ressourcen in das Heimatverzeichnis.
- Liefert: `val LocalPaletteStore: ProvidableCompositionLocal<PaletteStore?>` mit `null` als
  Vorgabe, damit ein Fenster ohne Store (Tests, der Profilwähler vor dem Start) weiter die
  ausgelieferte Palette zeichnet.

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

```kotlin
package kst4contest.view.compose

import androidx.compose.ui.graphics.Color
import kst4contest.model.PaletteRole
import kst4contest.model.PaletteOverrides
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The store, and the three ways back.
 *
 * Built over four lambdas rather than over ChatPreferences, so none of this touches the
 * operator's home directory: that constructor copies resources into it.
 */
class PaletteStoreTest {

    private val stored = mutableMapOf(false to "", true to "")
    private var fileRoles: Map<PaletteRole, Color> = emptyMap()

    private fun store() = PaletteStore(
        shippedOf = { dark ->
            JavaFxStylesheet.read(
                if (dark) "/KST4ContestDefaultEvening.css" else "/KST4ContestDefaultDay.css"
            )
        },
        fileRolesOf = { fileRoles },
        storedOverridesOf = { dark -> stored.getValue(dark) },
        storeOverrides = { dark, encoded -> stored[dark] = encoded },
    )

    @Test
    fun aChangeIsVisibleInTheResolvedPaletteAndInTheStorage() {
        val store = store()

        store.set(darkMode = false, role = PaletteRole.ACCENT, colour = "#FF0000")

        assertEquals(Color(0xFFFF0000), store.resolved(false).palette.accent)
        assertEquals(PaletteSource.CHANGED, store.resolved(false).roles[PaletteRole.ACCENT]!!.source)
        assertEquals("accent=#FF0000", stored.getValue(false))
    }

    @Test
    fun anUnparsableColourLeavesTheRoleAsItWas() {
        // The operator typed something that is not a colour. Nothing may turn black.
        val store = store()
        val before = store.resolved(false).palette.accent

        store.set(false, PaletteRole.ACCENT, "rot")

        assertEquals(before, store.resolved(false).palette.accent)
        assertEquals("", stored.getValue(false))
    }

    @Test
    fun theTwoDesignsDoNotDragEachOtherAlong() {
        val store = store()

        store.set(darkMode = true, role = PaletteRole.ACCENT, colour = "#FF0000")

        assertEquals(PaletteSource.CHANGED, store.resolved(true).roles[PaletteRole.ACCENT]!!.source)
        assertEquals(PaletteSource.SHIPPED, store.resolved(false).roles[PaletteRole.ACCENT]!!.source)
    }

    @Test
    fun resetToShippedBeatsAFileAsWell() {
        /*
         * The acceptance criterion of this stage: "exactly the shipped colours". With a file
         * in the middle, clearing the changes would not get there -- the file would.
         */
        fileRoles = mapOf(PaletteRole.ACCENT to Color(0xFF00FF00))
        val store = store()
        val shippedAccent = store.shipped(false).accent

        store.resetToShipped(false)

        assertEquals(shippedAccent, store.resolved(false).palette.accent)
        assertEquals(PaletteSource.CHANGED, store.resolved(false).roles[PaletteRole.ACCENT]!!.source)
        assertEquals(6, PaletteOverrides.parse(stored.getValue(false)).size)
    }

    @Test
    fun discardingChangesLetsTheFileApplyAgain() {
        fileRoles = mapOf(PaletteRole.ACCENT to Color(0xFF00FF00))
        val store = store()
        store.set(false, PaletteRole.ACCENT, "#FF0000")

        store.discardChanges(false)

        assertEquals(Color(0xFF00FF00), store.resolved(false).palette.accent)
        assertEquals(PaletteSource.FILE, store.resolved(false).roles[PaletteRole.ACCENT]!!.source)
        assertTrue(stored.getValue(false).isEmpty())
    }

    @Test
    fun theSnapshotRestoresWhatWasInForceWhenItWasTaken() {
        val store = store()
        store.set(false, PaletteRole.ACCENT, "#FF0000")

        store.takeSnapshot()
        store.set(false, PaletteRole.ACCENT, "#0000FF")
        store.set(false, PaletteRole.TEXT, "#00FF00")
        store.restoreSnapshot(false)

        // Including the change that was already there: the snapshot is not "shipped".
        assertEquals(Color(0xFFFF0000), store.resolved(false).palette.accent)
        assertEquals(PaletteSource.SHIPPED, store.resolved(false).roles[PaletteRole.TEXT]!!.source)
    }

    @Test
    fun theLifelineReadsTheShippedSheetAndNotTheResolvedPalette() {
        /*
         * The rescue button is drawn in these colours. If it ever read the resolved palette,
         * it would vanish in exactly the palette it exists to undo -- and nobody would find
         * out until they needed it.
         */
        val store = store()
        val shippedBase = store.shipped(false).base

        PaletteRole.values().forEach { store.set(false, it, "#000000") }

        assertEquals(shippedBase, store.shipped(false).base)
        assertEquals(Color.Black, store.resolved(false).palette.base)
    }
}
```

- [ ] **Schritt 2: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :app-desktop:test --tests '*PaletteStoreTest*'
```

- [ ] **Schritt 3: Den Store schreiben**

`PaletteStore` hält je Entwurf einen `mutableStateOf<String>` mit der kodierten
Abweichungsmenge, aus den vier Lambdas beim Bau gefüllt. `resolved(darkMode)` liest diesen
Zustand — **das ist der Grund, warum eine Änderung die Fenster erreicht** — und ruft
`resolvePalette(shippedOf(darkMode), fileRolesOf(darkMode), parse(zustand))`.

- `set` prüft mit `PaletteOverrides.isValidColour` und tut bei ungültiger Eingabe **nichts**.
- `resetToShipped` schreibt alle sechs Rollen mit den Werten aus `shippedOf(darkMode)` als
  ausdrückliche Änderung.
- `discardChanges` setzt den Zustand auf `""`.
- `takeSnapshot` merkt die kodierten Zeichenketten beider Entwürfe; `restoreSnapshot` setzt sie
  zurück. Ohne vorherigen `takeSnapshot` ist `restoreSnapshot` eine No-op.
- Jede Änderung ruft `storeOverrides`, damit `ChatPreferences` dieselbe Durchschreibe-Semantik
  hat wie jeder andere Einstellungsreiter — die XML schreibt erst
  `savePreferencesFromSettings`, wie bei allen anderen Einstellungen auch. **Das gehört in die
  Klassen-Javadoc**, weil es die eine Stelle ist, an der „wirkt sofort" und „überlebt den
  Neustart" auseinanderfallen.
- Eine Farbe nach `Color`: `Color(("ff" + hex.removePrefix("#")).toLong(16))`. Die
  Hilfsfunktion dafür ist privat und wird von `PaletteOverrides.isValidColour` vorab
  abgesichert.

- [ ] **Schritt 4: `Theme.kt` auf den Store umhängen**

```kotlin
    val store = LocalPaletteStore.current

    val palette = if (store != null) {
        /*
         * Read from the store's state, not remembered: that read is what makes a palette
         * change recompose every open window. remember(darkMode) kept the palette until
         * someone toggled day/evening, which is why a change used to reach nothing.
         */
        store.resolved(darkMode).palette
    } else {
        remember(darkMode) {
            JavaFxStylesheet.read(if (darkMode) EVENING_STYLESHEET else DAYLIGHT_STYLESHEET)
        }
    }
```

Der `null`-Zweig ist nicht Vorsicht, sondern nötig: der Profilwähler zeichnet, bevor ein Profil
und damit ein Store existiert.

- [ ] **Schritt 5: Tests laufen lassen, bauen, committen**

```bash
./gradlew :app-desktop:test --tests '*PaletteStoreTest*'
./gradlew clean build
git add app-desktop/src/main/kotlin/kst4contest/view/compose/PaletteStore.kt \
        app-desktop/src/main/kotlin/kst4contest/view/compose/Theme.kt \
        app-desktop/src/test/kotlin/kst4contest/view/compose/PaletteStoreTest.kt
git commit -m "Hold the palette as state so a change reaches every open window" -- \
        app-desktop/src/main/kotlin/kst4contest/view/compose/PaletteStore.kt \
        app-desktop/src/main/kotlin/kst4contest/view/compose/Theme.kt \
        app-desktop/src/test/kotlin/kst4contest/view/compose/PaletteStoreTest.kt
```

---

## Aufgabe 7: Den Store im Laufzeitaufbau bilden und bereitstellen

**Dateien:**
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Ändern: `app-desktop/src/main/kotlin/kst4contest/view/compose/ComposeWindowHost.kt`
- Ändern: `core/src/main/java/kst4contest/controller/OperatorProfilePaths.java`
- Ändern: `core/src/test/java/kst4contest/controller/OperatorProfilePathsTest.java`

**Schnittstellen:**
- Liefert: `OperatorProfilePaths.paletteRelativeFileName(OperatorProfile profile, boolean darkMode)`
  → `String`.

**Die Pfadbildung kommt nach `OperatorProfilePaths`, nicht in eine neue Klasse.** Dort stehen
schon `preferencesRelativeFileName` und `workedDatabaseRelativeFileName` mit genau dieser
Form: Wurzelprofil flach, jedes andere Profil unter `profileRelativeDirectory(profile)`. Eine
zweite Stelle, die dieselbe Form nachbaut, ist die Stelle, an der ein Profil später die Farben
eines anderen liest. Die Methode nimmt deshalb auch ein `OperatorProfile` und nicht
`(boolean, String)`, wie ihre beiden Geschwister.

- [ ] **Schritt 1: Den Test für die Pfadbildung schreiben**

Angehängt an das vorhandene `OperatorProfilePathsTest`, weil er dieselbe Form prüft wie die
Tests der beiden Geschwistermethoden:

```java
    @Test
    void theRootProfileKeepsTheHistoricFlatStylesheetNames() {
        OperatorProfile root = OperatorProfilePaths.buildRootProfile("Default");

        assertEquals(
                "KST4ContestDefaultDay.css",
                OperatorProfilePaths.paletteRelativeFileName(root, false));
        assertEquals(
                "KST4ContestDefaultEvening.css",
                OperatorProfilePaths.paletteRelativeFileName(root, true));
    }

    @Test
    void anotherProfileGetsItsStylesheetInItsOwnDirectory() {
        /*
         * The same shape the preferences and the worked database already take. A different
         * shape here would have one profile reading another profile's colours.
         */
        OperatorProfile other = new OperatorProfile();
        other.setProfileId("A");

        assertEquals(
                "profiles/A/KST4ContestDefaultDay.css",
                OperatorProfilePaths.paletteRelativeFileName(other, false));
    }
```

**Den Aufbau eines Nicht-Wurzelprofils aus dem Nachbartest übernehmen**, nicht erfinden: wie
`OperatorProfilePathsTest` ein Profil mit `profileId` baut, steht dort schon, und `isRootProfile()`
hängt daran, ob die Kennung `ROOT_PROFILE_ID` ist.

- [ ] **Schritt 2: Laufen lassen, umsetzen, laufen lassen**

```bash
./gradlew :core:test --tests '*OperatorProfilePathsTest*'
```

Die Methode, neben ihren beiden Geschwistern und nach deren Muster:

```java
    /**
     * Returns the stylesheet file name relative to the application directory.
     *
     * <p>The operator's own stylesheet for one design, which is the middle of the three
     * palette layers. Shaped like the preferences and the worked database: the root profile
     * keeps the historic flat name, every other profile gets it inside its own directory.</p>
     *
     * @param profile profile to resolve
     * @param darkMode true for the evening design, false for the daylight one
     * @return relative stylesheet file name
     */
    public static String paletteRelativeFileName(final OperatorProfile profile, final boolean darkMode) {

        final String fileName = darkMode
                ? ApplicationConstants.STYLECSSFILE_DEFAULT_EVENING
                : ApplicationConstants.STYLECSSFILE_DEFAULT_DAYLIGHT;

        if (profile.isRootProfile()) {
            return fileName;
        }

        return profileRelativeDirectory(profile) + "/" + fileName;
    }
```

**Achtung, eine Namensgleichheit mit Folgen:** die Datei des Wurzelprofils heißt genauso wie
die Kopie, die `Kst4ContestApplication` beim Start ins Anwendungsverzeichnis schreibt
(`STYLE_DEFAULTCSSDAY_FILE`, `Kst4ContestApplication.java:162`). Das ist **gewollt** — genau
diese Kopie soll der Ausweg sein — aber es heißt: wer beim Start die Kopie überschreibt,
überschreibt die Handarbeit des Operateurs. Vor Aufgabe 10 ist deshalb nachzusehen, ob dieser
Startschreibvorgang noch existiert und ob er bedingungslos überschreibt:

```bash
grep -n "STYLE_DEFAULTCSSDAY_FILE\|STYLE_DEFAULTCSSEVENING_FILE" \
    app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Überschreibt er bedingungslos, **anhalten und Marc sagen**: dann ist entweder der
Startschreibvorgang auf „nur wenn nicht vorhanden" zu ändern oder die Dateischicht braucht
einen anderen Namen. Das zu entscheiden ist nicht Teil dieser Aufgabe.

- [ ] **Schritt 3: Den Store in `startRuntime()` bilden**

Nach dem `chatcontroller`, vor dem ersten Fenster. Die vier Lambdas:

```java
		OperatorProfileSelection activeProfile = ActiveOperatorProfile.get();
		paletteStore = PaletteStoreFactory.create(
				chatcontroller.getChatPreferences(),
				activeProfile);
```

`PaletteStoreFactory` ist eine kleine Kotlin-Funktion neben `PaletteStore.kt`, die die vier
Lambdas aus Einstellungen und Profil zusammensetzt — damit bleibt der Java-Aufruf einzeilig und
der Store ohne `ChatPreferences` testbar.

Beim **Profilwechsel** entsteht der Store neu, weil `startRuntime()` auf einer frischen
`Kst4ContestApplication` läuft; nichts weiter zu tun. Das in einem Kommentar festhalten, weil
es sonst wie ein Versäumnis aussieht.

- [ ] **Schritt 4: Den Store an die Fenster geben**

`ComposeWindowHost.show` bekommt einen Parameter `paletteStore: PaletteStore? = null` und legt
ihn um den Inhalt:

```kotlin
                        CompositionLocalProvider(LocalPaletteStore provides paletteStore) {
                            Kst4ContestTheme(
                                darkMode = darkModeState.value,
                                baseFontSizeSp = baseFontSizeSp,
                            ) {
                                content(::exitApplication)
                            }
                        }
```

Dasselbe in `ComposeAlert` und `OperatorProfilePickerWindow` **nicht** — Dialoge bleiben bei
der ausgelieferten Palette, solange niemand das Gegenteil verlangt; der `null`-Zweig aus
Aufgabe 6 deckt sie ab. Das ist eine Festlegung und gehört in einen Kommentar.

Jeder Fenster-Wirt (`MainWindowHost`, `SettingsWindow`, `MonitorWindow`, `UpdateWindow`,
`StationMapWindow`, `TopPriorityCandidatesWindow`) reicht den Store durch. Die Aufrufer in
`Kst4ContestApplication` übergeben `paletteStore`.

- [ ] **Schritt 5: Bauen, Testzahl prüfen, GUI-ABNAHME**

```bash
./gradlew clean build && ./gradlew :app-desktop:run --args="--profile default"
```

Zu prüfen: alle Fenster sehen aus wie vorher. **Diese Scheibe darf nichts verändern** — sie
verlegt nur, woher die Farben kommen. Jeder sichtbare Unterschied ist ein Fehler.

- [ ] **Schritt 6: Committen**

```bash
git add app-desktop/src/main/kotlin/kst4contest/view/compose/ \
        core/src/main/java/kst4contest/controller/OperatorProfilePaths.java \
        core/src/test/java/kst4contest/controller/OperatorProfilePathsTest.java \
        app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
git commit -m "Build the palette store per profile and hand it to every window" -- \
        app-desktop/src/main/kotlin/kst4contest/view/compose/ \
        core/src/main/java/kst4contest/controller/OperatorProfilePaths.java \
        core/src/test/java/kst4contest/controller/OperatorProfilePathsTest.java \
        app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

---

## Aufgabe 8: Der Zustand des Reiters

**Dateien:**
- Anlegen: `app-desktop/src/main/kotlin/kst4contest/view/compose/tabs/ColoursTabState.kt`
- Anlegen: `app-desktop/src/test/kotlin/kst4contest/view/compose/tabs/ColoursTabStateTest.kt`

**Schnittstellen:**
- Liefert: `class ColoursTabState(private val store: PaletteStore, private val darkModeNow: () -> Boolean)`
  mit `roles(): List<RoleRow>`, `set(role, text)`, `warnings(): List<ContrastWarning>`,
  `resetToShipped()`, `discardChanges()`, `restoreSnapshot()`, `exportStylesheet(): String?`,
  und `lastRefusal: String?`.
- Liefert: `data class RoleRow(val role: PaletteRole, val label: String, val hex: String,
  val source: PaletteSource)`.
- `set` mit unparsbarer Eingabe setzt `lastRefusal` auf einen Satz, den der Reiter zeigt, und
  ändert nichts.

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

Deckt mindestens ab: dass `roles()` sechs Zeilen mit Hex und Herkunft liefert; dass `set` mit
`"rot"`, `""`, `"#GGG"` und `"#1234567"` nichts ändert und `lastRefusal` setzt; dass eine gültige
Eingabe `lastRefusal` löscht; dass `roles()` dem aktiven Entwurf folgt (`darkModeNow`); dass
`warnings()` die Warnungen liefert, und zwar als
`contrastWarnings(store.resolved(darkModeNow()).palette, store.shipped(darkModeNow()))` — mit
**beiden** Paletten, weil der Wächter relativ zur Auslieferung misst.

Aufbau wie `PaletteStoreTest`: ein `PaletteStore` über vier Lambdas, kein `ChatPreferences`.

- [ ] **Schritt 2–4: Laufen lassen, umsetzen, laufen lassen**

```bash
./gradlew :app-desktop:test --tests '*ColoursTabStateTest*'
```

Die Beschriftungen der sechs Rollen auf Englisch, wie jeder andere Oberflächentext:
`Surface`, `Window surface`, `Field interior`, `Text`, `Accent`, `Separator line`. Etappe 9
zieht sie später in ein Ressourcenbündel; hier gehören sie an **eine** Stelle, damit das
später ein Handgriff ist.

- [ ] **Schritt 5: Bauen, committen**

---

## Aufgabe 9: Der Reiter — **GUI-ABNAHME**

**Dateien:**
- Anlegen: `app-desktop/src/main/kotlin/kst4contest/view/compose/tabs/ColoursTab.kt`
- Ändern: `app-desktop/src/main/kotlin/kst4contest/view/compose/SettingsTabs.kt`

- [ ] **Schritt 1: Den Reiter bauen**

Je Rolle eine Zeile über `Form.text`: Beschriftung, Hex-Feld, und rechts die Herkunft als
Text (`shipped`, `from file`, `changed`). Darunter die Warnungen des Kontrastwächters, dann die
drei Rückwege und der Ausfuhr-Knopf.

**Die Rettungsleine wird nicht mit `Form.button` gezeichnet**, weil der die aktive Palette
nimmt. Sie bekommt ihre Farben aus `store.shipped(darkModeNow())`:

```kotlin
/**
 * The one control that refuses the operator's palette.
 *
 * Drawn from the shipped sheet, always. An operator who sets text and surface to the same
 * black cannot see any other button -- including the ones that would undo it. That this
 * looks out of place under a customised palette is the point: it is a lifeline, and not
 * fitting in is what makes it findable.
 */
@Composable
private fun ResetToShippedButton(shipped: JavaFxPalette, onClick: () -> Unit) { … }
```

- [ ] **Schritt 2: Den Reiter anhängen**

In `SettingsTabs.kt` **nach** `SettingsTab("Profiles", …)`, mit dem Kommentar, der dort schon
begründet, warum angehängt und nicht einsortiert wird:

```kotlin
        SettingsTab("Colours") { ColoursTab(ColoursTabState(paletteStore, darkModeNow)) },
```

`paletteStore` und `darkModeNow` müssen bis hierher durchgereicht werden; `SettingsTabs` hat
`prefs` und `host` schon, also denselben Weg nehmen.

- [ ] **Schritt 3: Bauen, Testzahl prüfen, GUI-ABNAHME**

```bash
./gradlew clean build && ./gradlew :app-desktop:run --args="--profile default"
```

Zu prüfen, und das ist die eigentliche Abnahme dieser Etappe:

1. Eine Farbe ändern → **alle offenen Fenster** ziehen sofort mit, nicht erst nach einem
   Fensterwechsel.
2. „Zurück zum Stand von vorher" stellt her, was beim Öffnen des Reiters galt.
3. Einstellungen speichern, Anwendung beenden, neu starten → die Farbe ist noch da.
4. **Text und Fläche auf dasselbe Schwarz stellen** → der Auslieferungs-Knopf bleibt lesbar
   und holt alles zurück. Das ist der Test, für den die Rettungsleine existiert.
5. Tag/Abend umschalten → die beiden Paletten sind unabhängig.
6. Zweites Profil → andere Farben, und das erste bleibt unberührt.
7. Eine Farbe mit schlechtem Kontrast → die Warnung erscheint, und speichern geht trotzdem.
   Gegenprobe, die genauso wichtig ist: **im unberührten Zustand steht keine Warnung da.**
8. Unsinn in ein Feld tippen → nichts ändert sich, der Reiter sagt warum.

- [ ] **Schritt 4: Committen**

---

## Aufgabe 10: Die Datei als Ausweg

- [ ] **Schritt 1: Der Ausfuhr-Knopf**

„Write this design's stylesheet into my profile" kopiert die ausgelieferte Vorlage an den Pfad
aus `OperatorProfilePaths.paletteRelativeFileName`, erzeugt das Profilverzeichnis, falls es fehlt, und sagt, wohin geschrieben
wurde. Überschreibt **nicht**, wenn die Datei schon da ist, sondern sagt, dass sie da ist —
sonst verliert ein Operateur seine Handarbeit mit einem Fehlklick.

- [ ] **Schritt 2: Test**

Dass eine vorhandene Datei nicht überschrieben wird, und dass der Pfad der aus Aufgabe 7 ist.
Mit einem temporären Verzeichnis, nicht im Benutzerprofil.

- [ ] **Schritt 3: GUI-ABNAHME**

Datei ausführen lassen, von Hand eine Farbe darin ändern, Anwendung neu starten → die Farbe
gilt, und der Reiter zeigt bei dieser Rolle `from file`. Dann dieselbe Rolle im Reiter ändern →
`changed` gewinnt. Dann „auf Auslieferung" → die ausgelieferte Farbe, trotz Datei.

Danach die Datei absichtlich zerstören (halbe Zeile, Binärmüll) und neu starten → die
Anwendung startet, die Rolle fällt auf die Auslieferung zurück.

- [ ] **Schritt 4: Committen**

---

## Aufgabe 11: Dokumentation

- [ ] **Schritt 1: Handbuch, beide Sprachen**

`github_docs/de-Funktionen.md` und `github_docs/en-Features.md` bekommen einen Abschnitt zum
Farbreiter. Darin **muss** stehen:

- dass eine Änderung sofort gilt und wie man zurückkommt,
- **dass der Auslieferungs-Knopf immer in den ausgelieferten Farben gezeichnet ist und auch
  dann funktioniert, wenn man nichts mehr lesen kann** — das ist die Stelle, an der ein
  Operateur es nachliest, und der Risikoabschnitt der Spezifikation verlangt es ausdrücklich,
  weil ein Tastaturweg nur hilft, wer ihn kennt,
- dass die Farben pro Profil gelten,
- wo die Datei liegt und dass eine Änderung im Reiter sie überstimmt.

Beide Sprachen semantisch gleich, wie `AGENTS.md` verlangt.

- [ ] **Schritt 2: `docs/PROJECT_CONTEXT.md`**

Die Dreischicht-Quelle und ihren Vorrang eintragen — das ist eine dauerhafte
Architekturentscheidung und gehört nach `AGENTS.md` Punkt 6 dorthin.

- [ ] **Schritt 3: Veraltete Aussagen suchen**

```bash
grep -rniE "stylesheet|farbe|colour|color" github_docs/*.md docs/PROJECT_CONTEXT.md | grep -viE "changelog" | head -20
```

Alles, was sagt, die Farben seien fest oder kämen aus dem Installationsverzeichnis, ist jetzt
falsch.

- [ ] **Schritt 4: Committen**

---

## Was unverändert bleibt

- Die ausgelieferten Vorlagen `KST4ContestDefaultDay.css` und `…Evening.css`.
- `JavaFxStylesheet.read(resourcePath)` in Signatur und Verhalten — Schicht 1 braucht eine
  vollständige Palette.
- Der `derive`-Pin, `javafx-derive-reference.txt` und `JavaFxStylesheetTest`.
- `fontSizePx` und die bestehende Schriftgrößeneinstellung.
- Die Reiterreihenfolge im Einstellungsfenster.
- Dialoge (`ComposeAlert`, `OperatorProfilePickerWindow`) bei der ausgelieferten Palette.
- `core` toolkit-frei: Farben dort als Zeichenketten.

## Bekannte Fallstricke

- **`ComposeStationMapDrawingTest` ist flaky** unter paralleler Last. Bei Rot zuerst isoliert
  nachprüfen.
- **PMD/SpotBugs** melden rund 4448 vorbestehende Befunde und brechen den Build nicht.
- **`ChatPreferences`' Konstruktor kopiert Ressourcen in das Heimatverzeichnis.** Deshalb
  bauen `PaletteStoreTest` und `ColoursTabStateTest` über Lambdas und nicht über
  `ChatPreferences`.
- **Der Rückfalltest aus Etappe 7, Klasse 4**, prüft, dass die Farben aus der Stilvorlage
  kommen. Er muss nach dieser Etappe weiter gelten — er liest die Vorlage direkt und ist von
  den Abweichungen unberührt, aber das ist nachzuprüfen, nicht anzunehmen.
- **Eine Zahl aus diesem Plan ist keine Messung.** Die Testzahlen stehen als Erwartung da;
  abgelesen wird aus der XML.
