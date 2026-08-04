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
| Callsign | Rufzeichen der Station |
| Name | Name beziehungsweise Zusatzinformationen aus dem Chat-Namensfeld |
| QRA | Maidenhead-Locator |
| QRB | Entfernung in km |
| QTF | Richtung in Grad |
| QRG | Zuletzt aus einer Chat-Nachricht erkannte Frequenz |
| Tropo | Ergebnis der bandbezogenen Tropo- beziehungsweise Streckenbewertung |
| Score | Aktueller Prioritätswert |
| Act | Minuten seit der letzten Aktivität |
| AP | AirScout-Flugzeugdaten, sofern aktiviert |
| worked | Bandbezogener Worked-, Bandmöglichkeits- und Großfeldstatus sowie `wkdany` |
| NOT QRV @ | Bänder, auf denen die Station manuell als nicht QRV markiert wurde |
| Category | Chat-Kategorie des Eintrags |

Die QRG-Spalte zeigt die zuletzt für eine Station erkannte Frequenz. Fehlende Nullen werden für die Anzeige ergänzt, sodass beispielsweise `144.21` als `144.210` erscheint. Erkennt KST4Contest nacheinander Frequenzen auf mehreren Bändern, zeigt die Spalte den letzten Treffer; die internen Bandinformationen können trotzdem mehrere aktuelle Bänder der Station enthalten.

Relative Angaben werden zunächst mit einem höchstens 30 Minuten alten Bandkontext desselben Absenders kombiniert. Nur wenn dieser fehlt, verwendet KST4Contest das globale Fallback-Band. Erkennungsregeln, Beispiele und Grenzen: [QRG-Erkennung](de-Funktionen#qrg-erkennung).

### Worked-, Band- und Großfeldstatus

Die Unterspalten unter **worked** sind kompakt, weil bei mehreren aktivierten Bändern kaum Platz für ausgeschriebene Zustände bleibt. `X` kennzeichnet ein auf diesem Band gearbeitetes Rufzeichen. `a` und `B+` weisen auf ein angebotenes, noch nicht gearbeitetes Band hin. Ein angehängtes `o` bedeutet, dass das vierstellige Großfeld auf diesem Band bereits gearbeitet wurde.

Die Unterspalte **wkdany** ist bandunabhängig: `x` steht für ein bereits gearbeitetes Rufzeichen, `o` für ein auf irgendeinem Band gearbeitetes Großfeld und `xo` für beides.

Jede Statuszelle besitzt einen Tooltip mit der Legende und dem für die betreffende Station ermittelten Zustand. Die vollständige Herleitung einschließlich NOT-QRV-Vorrang: [Gearbeitete Rufzeichen, neue Bänder und neue Großfelder](de-Funktionen#gearbeitete-rufzeichen-neue-bänder-und-neue-großfelder).

![Bandbezogener Worked-Status und Worked-Großfelder](worked_band_status.png)

**Sortierung**: Klick auf Spaltenköpfe. QRB-Sortierung arbeitet numerisch (ab v1.22 korrigiert).


Ein grün und fett dargestelltes Rufzeichen kennzeichnet eine aus einer gerichteten Nachricht hergeleitete Richtungsgelegenheit. Die Markierung bezieht sich auf den Absender der Nachricht und bleibt höchstens fünf Minuten sichtbar. Herleitung und Grenzen: [Richtungsgelegenheiten aus gerichteten Nachrichten](de-Funktionen#richtungsgelegenheiten-aus-gerichteten-nachrichten).
### Sendfeld

Texteingabe für ausgehende Nachrichten. Nach Klick auf ein Rufzeichen in der Benutzerliste erhält das Sendfeld automatisch den Fokus – sofort tippen ohne Doppelklick (ab v1.22).

### MYQRG-Feld

Rechts neben dem Sendbutton. Zeigt die aktuelle eigene QRG an, kann auch manuell eingetragen werden.

### MYQTF-Feld *(für v1.3)*

Eingabefeld für die aktuelle Antennenrichtung. Wird für die geplante `MYQTF`-Variable verwendet.

---

## Nachrichtentabellen

KST4Contest zeigt Nachrichtentexte bewusst einzeilig an. So bleiben auch bei hohem Chat-Aufkommen viele Einträge gleichzeitig sichtbar. Der Nachteil liegt auf der Hand: Bei einer schmalen **Message**-Spalte passt nicht jede Nachricht vollständig in die Zeile.

Ist ein Nachrichtentext breiter als die sichtbare Zelle, zeigt KST4Contest den vollständigen Inhalt als Tooltip an. Dazu die Maus kurz über die betreffende **Message**-Zelle halten. Passt der Text vollständig in die Spalte, wird kein zusätzlicher Volltext-Tooltip eingeblendet.

Webadressen mit `http://`, `https://` oder dem Präfix `www.` werden innerhalb des Nachrichtentextes als Links dargestellt. Ein Klick öffnet die Adresse im Standardbrowser des Betriebssystems. Andere Protokolle werden nicht als Link behandelt.

![Abgeschnittener Nachrichtentext mit Volltext-Tooltip und Link](message_tooltip_and_link.png)

Damit muss der Divider nicht allein deshalb verschoben werden, um eine einzelne längere Nachricht zu lesen. Für einen dauerhaft breiteren Nachrichtenbereich kann er selbstverständlich weiterhin angepasst werden.

---

## Filter

Die Filterleiste befindet sich oberhalb der Chatmember-Tabelle. Sie ist in mehrere logisch zusammengehörige Bereiche gegliedert:

- **Show only QTF** begrenzt die Liste auf eine gewählte Antennenrichtung.
- **Show only QRB [km] <=** setzt eine maximale Entfernung.
- **Find** sucht nach einem Rufzeichen.
- **wkd** blendet Rufzeichen aus, die bereits auf mindestens einem Band gearbeitet wurden.
- Die einzelnen Band-Schaltflächen blenden eine Station aus, wenn sie auf dem betreffenden Band bereits gearbeitet oder dort als NOT QRV markiert wurde. Angezeigt werden nur die für die eigene Station aktivierten Bänder.
- **Only new grids** zeigt ausschließlich Stationen aus vierstelligen Großfeldern, die auf noch keinem Band gearbeitet wurden.
- **Grid color** ist kein Filter. Die Funktion markiert das QRA-Feld bereits gearbeiteter Großfelder, ohne Stationen auszublenden.
- **New bands** zeigt Stationen mit mindestens einer erkannten, an der eigenen Station aktivierten und noch nicht gearbeiteten Bandmöglichkeit. NOT-QRV-Markierungen haben Vorrang.
- **Reachability**, **Tropo >=0dB** und **AS next 5m** schränken die Liste anhand der gewählten Strecken- beziehungsweise AirScout-Bedingungen ein.

Die Filterleiste besitzt keine feste Breite. QTF sowie die Worked- und Reachability-Filter nutzen zunächst den gesamten Platz ihrer jeweiligen Zeile. Wird der horizontale Divider nach rechts verschoben und die Chatmember-Ansicht dadurch schmaler, wechseln die Controls erst dann in die nächste Zeile, wenn ihre tatsächlich benötigte Breite nicht mehr zur Verfügung steht.

![Umgebrochene Filterleiste bei schmaler Chatmember-Ansicht](filter_bar_wrapped.png)

Im Klartext: Die Filter bestimmen weiterhin den Inhalt der Tabelle, aber nicht mehr die Mindestbreite der gesamten rechten Programmseite. In der normalen Ansicht bleibt die Leiste kompakt. Erst bei einer tatsächlich schmalen Ansicht benötigt sie mehr Höhe. Der Divider kann anschließend wieder nach links verschoben werden; die Controls ordnen sich unmittelbar neu an.

---
---

## Stationsinfo-Panel (Further Info)

Rechts unten werden die Nachrichten der ausgewählten Station zusammengeführt. Dazu gehören öffentliche Nachrichten, Privatnachrichten an die eigene Station und – soweit im Chat sichtbar – Privatnachrichten an andere Stationen.

Der im Panel gewählte Filter bestimmt, welche dieser Nachrichten angezeigt werden. Unter **Settings → GUI** lässt sich festlegen, welcher Filter beim Öffnen einer Stationsinformation vorausgewählt ist:

- alle Nachrichten,
- Privatnachrichten an die eigene Station,
- Privatnachrichten an andere Stationen oder
- öffentliche Nachrichten.

Die Einstellung verändert nur die Darstellung im Stationsinfo-Panel. Nachrichten werden dadurch weder verworfen noch aus den übrigen Nachrichtentabellen entfernt. Der Filter kann im Panel jederzeit für die aktuell betrachtete Station gewechselt werden.

Im unteren Bereich können für die ausgewählte Station bandbezogene **Not QRV**-Markierungen gesetzt werden. Sichtbar sind die Bänder, die in den Stationseinstellungen für die eigene Station aktiviert wurden. **tag not qrv all** setzt beziehungsweise entfernt die Markierung für alle unterstützten Bänder gemeinsam, einschließlich momentan nicht eingeblendeter Bänder.

Die Änderung wirkt sofort auf die Spalte **NOT QRV @**, die Bandmöglichkeiten und die zugehörigen Filter. Sie wird in der internen Datenbank gespeichert und nach einem Neustart wiederhergestellt.

![Bandbezogene NOT-QRV-Markierungen im Further-Info-Bereich](not_qrv_controls.png)

Hier können auch **Sked-Erinnerungen / Wecker** für beide Skkedpartner aktiviert werden.

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
