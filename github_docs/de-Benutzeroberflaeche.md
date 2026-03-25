# Benutzeroberfläche

> 🇬🇧 [English version](en-User-Interface) | 🇩🇪 Du liest gerade die deutsche Version

## Verbinden mit dem Chat

1. Im Einstellungsfenster eine **Chat-Kategorie** auswählen (z. B. 144 MHz VHF, 432 MHz UHF, …).
2. **Connect**-Button klicken.
3. Warten bis die Verbindung aufgebaut ist.

> Trennen und Neu-Verbinden ist nur über das Einstellungsfenster möglich. Es empfiehlt sich daher, das Einstellungsfenster geöffnet zu lassen.

---

## Hauptfenster-Überblick

Das Hauptfenster besteht aus mehreren Bereichen:

### PM-Fenster (oben links)

Zeigt alle empfangenen **Privatnachrichten** sowie abgefangene öffentliche Nachrichten, die das eigene Rufzeichen enthalten. Neue Nachrichten erscheinen in **Rot** und faden alle 30 Sekunden über Gelb bis Weiß ab.

### Benutzerliste (Chat Members)

Die zentrale Tabelle aller aktuell aktiven Chat-Nutzer. Spalten (je nach Konfiguration):

| Spalte | Inhalt |
|---|---|
| Call | Rufzeichen der Station |
| Name | Name aus dem Chat-Namenfeld |
| Loc | Maidenhead-Locator |
| QRB | Entfernung in km |
| QTF | Richtung in Grad |
| QRG | Automatisch erkannte Frequenz |
| AP | AirScout-Flugzeugdaten (wenn aktiv) |
| Band-Farben | Worked/NOT-QRV-Status pro Band |

**Sortierung**: Klick auf Spaltenköpfe. QRB-Sortierung arbeitet numerisch (ab v1.22 korrigiert).

### Sendfeld

Texteingabe für ausgehende Nachrichten. Nach Klick auf ein Rufzeichen in der Benutzerliste erhält das Sendfeld automatisch den Fokus – sofort tippen ohne Doppelklick (ab v1.22).

### MYQRG-Feld

Rechts neben dem Sendbutton. Zeigt die aktuelle eigene QRG an, kann auch manuell eingetragen werden.

### MYQTF-Feld *(für v1.3)*

Eingabefeld für die aktuelle Antennenrichtung. Wird für die geplante `MYQTF`-Variable verwendet.

---

## Filter

Die Filter-Leiste (ab v1.21 als Flowpane für kleine Bildschirme):

- **Show only QTF**: Richtungsfilter aktivieren (Buttons N/NE/E/… oder Grad-Eingabe)
- **Show only QRB [km] <=**: Entfernungsfilter aktivieren (Toggle-Button)
- **Hide Worked [Band]**: Gearbeitete Stationen pro Band ausblenden (je ein Toggle pro Band)
- **Hide NOT-QRV [Band]**: NOT-QRV-markierte Stationen pro Band ausblenden

---

## Stationsinfo-Panel (Further Info)

Rechts unten: Zeigt alle Nachrichten einer ausgewählten Station (CQ-Nachrichten und PMs in einem Panel). Ein Nachrichtenfilter lässt sich über den Standard-Filter in den Preferences vorbelegen.

Hier können auch **Sked-Erinnerungen** aktiviert werden.

---

## Prioritätsliste

Zeigt die vom Score-Service berechneten Top-Kandidaten. Aktualisiert sich automatisch im Hintergrund basierend auf Richtung, Entfernung und AP-Verfügbarkeit.

---

## Cluster & QSO der anderen

Separates Fenster (kann miniaturisiert werden). Zeigt den Kommunikationsfluss zwischen anderen Stationen – interessant in ruhigeren Phasen.

---

## Menü

### Window
- **Use Dark Mode** (ab v1.26): Dunkles Farbschema aktivieren/deaktivieren.

---

## Fenstergrößen und Divider

Ab **v1.21** werden beim Klick auf **„Save Settings"** auch Fenstergrößen und Divider-Positionen aller Panels in der Konfigurationsdatei gespeichert und beim nächsten Start wiederhergestellt.

Bei Problemen mit der Darstellung: Konfigurationsdatei löschen → KST4Contest erstellt neue Standardwerte.

---

## Tipps zur Bedienung

- **Einstellungsfenster geöffnet lassen**: Schneller Zugriff auf Beacon-Aktivierung/Deaktivierung.
- **Rechtsklick in der Benutzerliste**: Öffnet das Snippet-Menü und weitere Aktionen (QRZ.com-Profil, NOT-QRV-Tags setzen).
- **Enter aus dem Chat heraus**: Wenn im Sendfeld Text steht, sendet Enter direkt – auch wenn der Fokus woanders liegt.
- **Beacon stoppen**: Beim Scannen von Frequenzen den Beacon ausschalten, damit der Chat nicht mit Meldungen überflutet wird.
