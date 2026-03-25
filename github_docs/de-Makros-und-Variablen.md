# Makros und Variablen

> 🇬🇧 [English version](en-Macros-and-Variables) | 🇩🇪 Du liest gerade die deutsche Version

KST4Contest bietet ein flexibles System aus Text-Snippets, Shortcuts und eingebauten Variablen, die den Chat-Workflow im Contest erheblich beschleunigen.

---

## Überblick

| Typ | Aufruf | Zweck |
|---|---|---|
| **Shortcuts** | Button in der Toolbar | Schneller Text-Insert ins Sendfeld |
| **Snippets** | Rechtsklick / Ctrl+1..0 | Text-Bausteine, optionaler PM-Versand |
| **Variablen** | In allen Text-Feldern verwendbar | Dynamische Werte (QRG, Locator, AP-Daten) |

---

## Shortcuts (Schnellzugriff-Schaltflächen)

Konfigurierbar in den Preferences → **Shortcut Settings**.

- Jeder konfigurierte Text erzeugt **einen Button** in der Benutzeroberfläche.
- Ein Klick fügt den Text in das **Sendfeld** ein.
- **Alle Variablen** können in Shortcuts verwendet werden und werden beim Einfügen sofort aufgelöst.
- Auch längere Texte möglich.

**Tipp**: Häufig verwendete Abkürzungen wie „pse", „rrr", „tnx", „73" als Shortcuts anlegen.

---

## Snippets (Text-Bausteine)

Konfigurierbar in den Preferences → **Snippet Settings**.

### Aufruf

- **Rechtsklick** auf ein Rufzeichen in der Benutzerliste
- **Rechtsklick** in der CQ-Nachrichtentabelle
- **Rechtsklick** in der PM-Nachrichtentabelle
- **Tastaturkürzel**: `Ctrl+1` bis `Ctrl+0` für die ersten 10 Snippets

### Verhalten mit ausgewähltem Rufzeichen

Wenn in der Benutzerliste ein Rufzeichen ausgewählt ist, wird der Snippet als **Privatnachricht** adressiert:

```
/CQ RUFZEICHEN <Snippet-Text>
```

Anschließend kann mit **Enter** direkt gesendet werden – auch wenn das Sendfeld nicht den Fokus hat.

### Hardware-Makro-Tastatur

*(Idee von IU3OAR, Gianluca Costantino)*

Die Tastenkombinationen `Ctrl+1` bis `Ctrl+0` können auf einer programmierbaren Makro-Tastatur belegt werden. Ein weiterer Tastendruck (auf eine „Enter"-Taste) sendet den Text sofort. Im Contest-Betrieb spart das erheblich Zeit.

### Vordefinierte Standard-Snippets

Beim ersten Start werden einige Snippets vorbelegt, z. B.:

- `Hi OM, try sked?`
- `I am calling cq ur dir, pse lsn to me at MYQRG`
- `pse ur qrg?`
- `rrr, I move to your qrg nw, pse ant dir me`

Diese können in den Preferences angepasst oder gelöscht werden.

---

## Variablen

Variablen werden in geschriebenen Texten (Snippets, Shortcuts, Beacon, Sendfeld) durch ihre aktuellen Werte ersetzt. Einfach den Variablennamen **großgeschrieben** in den Text einfügen.

### MYQRG

Wird durch die aktuelle Transceiverfrequenz ersetzt.

- Quelle: TRX-Sync via UDP vom Logprogramm (wenn aktiviert)
- Fallback: Manuell eingetragener Wert im MYQRG-Textfeld rechts neben dem Sendbutton
- Format: `144.388.03`

**Beispiel**: `calling cq at MYQRG` → `calling cq at 144.388.03`

### MYQRGSHORT

Wie MYQRG, aber nur die ersten 7 Zeichen.

- Format: `144.388`

**Beispiel**: `qrg: MYQRGSHORT` → `qrg: 144.388`

### MYLOCATOR

Wird durch den eigenen Maidenhead-Locator (6-stellig) ersetzt.

- Format: `JO51IJ`

**Beispiel**: `my loc: MYLOCATOR` → `my loc: JO51IJ`

### MYLOCATORSHORT

Wie MYLOCATOR, aber nur die ersten 4 Zeichen.

- Format: `JO51`

**Beispiel**: `loc: MYLOCATORSHORT` → `loc: JO51`

### QRZNAME

Wird durch den **Namen** der aktuell ausgewählten Station aus dem Chat-Namenfeld ersetzt.

**Beispiel**: `Hi QRZNAME, sked?` → `Hi Gianluca, sked?`

### FIRSTAP

Wird durch Daten des ersten reflektierbaren Flugzeugs zur ausgewählten Station ersetzt (sofern vorhanden).

- Bedingung: AirScout ist aktiv und ein Flugzeug ist verfügbar.
- Format-Beispiel: `a very big AP in 1 min`

**Beispiel**: `AP info: FIRSTAP` → `AP info: a very big AP in 1 min`

### SECONDAP

Wie FIRSTAP, aber für das zweite verfügbare Flugzeug.

- Format-Beispiel: `Next big AP in 9 min`

**Beispiel**: `also: SECONDAP` → `also: Next big AP in 9 min`

### MYQTF *(geplant für v1.3)*

Wird durch die aktuelle Antennenrichtung in Worten ersetzt (z. B. `north`, `north east`, `east`, …).

- Quelle: Winkelwert im MYQTF-Eingabefeld (rechts neben dem MYQRG-Feld)

---

## Variablen im Beacon

Alle Variablen können auch im **automatischen Beacon** (Intervall-Nachrichten) verwendet werden. Empfohlene Beacon-Konfiguration:

```
calling cq at MYQRG, loc MYLOCATOR, GL all!
```

Da KST4Contest QRG-Daten automatisch aus Chat-Nachrichten ausliest: Wenn andere Stationen ebenfalls KST4Contest nutzen, sehen sie die eigene QRG sofort in der QRG-Spalte der Benutzerliste.

---

## Beispiel-Workflow mit Makros im Contest

1. Station in der Benutzerliste auswählen → Rufzeichen ist nun vorausgewählt.
2. `Ctrl+1` drücken → Snippet „Hi OM, try sked?" wird als PM adressiert.
3. Enter drücken → Nachricht wird gesendet.
4. Station antwortet mit Frequenz → QRG-Spalte wird automatisch befüllt.
5. `Ctrl+2` → Snippet „I am calling cq ur dir, pse lsn to me at 144.388" (MYQRG aufgelöst).
6. Enter → Gesendet.

Ohne manuelle Tipparbeit, ohne Fehler, ohne Unterbrechung des CQ-Rufens.
