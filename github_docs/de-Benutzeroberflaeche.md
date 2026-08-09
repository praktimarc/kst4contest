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
| Score | Aktueller, numerisch sortierbarer Prioritätswert des normalisierten Basisrufzeichens |
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

Im selben Bereich wird der aktuelle **Priority score** der ausgewählten Station angezeigt.

Mit **Sked fail** lässt sich ein fehlgeschlagener Versuch markieren. Der Score des normalisierten Basisrufzeichens wird dadurch stark reduziert. **Reset fail** entfernt diese Markierung wieder. Die Markierung gilt für alle aktiven Suffix- und Kategorievarianten der Station und bleibt innerhalb der laufenden Programmsitzung erhalten.

Darunter befinden sich die Bedienelemente zum Anlegen eines Skeds:

| Bedienelement | Bedeutung |
|---|---|
| **Sked in** | Zeit bis zum Sked |
| **Band** | vereinbartes Band aus den eigenen aktivierten Bändern |
| **Mode** | `SSB` oder `CW` für eine mögliche Win-Test-Übergabe |
| **Create sked** | internen Sked anlegen |
| **Remind-PM in** | automatische Reminder-PMs aktivieren |
| **2+1**, **5+2+1**, **10+5+2+1** | Zeitpunkte der Reminder-PMs vor dem Termin |

![Sked-Steuerung im Further-Info-Bereich](sked_controls.png)

Das vorgeschlagene Band wird aus aktuellen QRG- und Namensinformationen der Station hergeleitet. Vor dem Anlegen kann es ausdrücklich geändert werden. Die Mode-Auswahl betrifft nur die Übergabe an Win-Test; der interne Sked und die Reminder-PMs funktionieren unabhängig davon.

**Create sked** legt den Termin immer zuerst in KST4Contest an. Ist der Win-Test-Netzwerk-Listener aktiv, wird anschließend zusätzlich eine Übergabe an Win-Test versucht. Kann keine zum ausgewählten Band passende QRG ermittelt werden oder ist Win-Test nicht erreichbar, bleiben der interne Sked, seine Priorisierung und gegebenenfalls angelegte Reminder erhalten.

Die vollständige Herleitung und die Grenzen der Funktion sind unter [Skeds und Sked-Erinnerungen](de-Funktionen#skeds-und-sked-erinnerungen) beschrieben.

---

## Prioritätsliste

Die kompakte Prioritätsleiste befindet sich rechts zwischen Benutzerliste und Further-Info-Bereich. Sie zeigt die beiden derzeit höchstbewerteten Kandidaten unmittelbar im Hauptfenster:

```text
Priority:  1 RUFZEICHEN SCORE  2 RUFZEICHEN SCORE  more
```

Ein Klick auf einen der beiden Kandidaten wählt den dazugehörigen aktiven Chatmember aus. Dabei werden das vollständige Rufzeichen einschließlich Suffix und die zugehörige Chat-Kategorie verwendet.

Die Schaltfläche **more** öffnet ein separates Fenster mit bis zu 15 Kandidaten. Die Liste ist nach absteigendem Score sortiert. Ein Doppelklick wählt den betreffenden Kandidaten aus und schließt das Fenster.

![Priority Score, kompakte Kandidatenliste und Further-Info-Steuerung](priority_score_overview.png)

Stationen mit einem Score von `0` werden nicht in die Prioritätsliste aufgenommen. In der Benutzerliste bleiben sie sichtbar, sodass der Ausschluss nachvollzogen und beispielsweise durch eine geänderte NOT-QRV-Markierung korrigiert werden kann.

Der Score wird für das normalisierte Basisrufzeichen berechnet. Mehrere aktive Varianten wie `9A0BB-2` und `9A0BB-70` können daher in der Benutzerliste denselben Wert anzeigen. Die Chatmember bleiben trotzdem getrennte Nachrichtenziele.

Neue Nachrichten, AirScout-Daten, Skeds und Statusänderungen lösen eine Neuberechnung aus. Zusätzlich erfolgt eine regelmäßige Aktualisierung im Hintergrund. Eine kurzzeitig noch nicht angepasste Reihenfolge ist deshalb kein Fehler.

Herleitung und Grenzen: [Prioritätsscore und Prioritätsliste](de-Funktionen#prioritätsscore-und-prioritätsliste-ab-v140).

---

## Stationskarte

Die Stationskarte kann auf zwei Wegen geöffnet werden:

- **Windows → Show / hide station map** öffnet oder schließt das Kartenfenster.
- **Show on map** im **Further Info**-Bereich öffnet die Karte und zentriert sie auf die ausgewählte Station.

Die Karte zeigt die Stationen, die auch nach Anwendung der aktuellen Benutzerlistenfilter noch sichtbar sind. Ein Hinweis in der Kopfzeile zeigt an, wenn eine gefilterte Ansicht aktiv ist.

![Stationskarte mit ausgewählter Station und eingeblendeter Streckenanalyse](station_map_path_analysis.png)

Ein einzelner Stationsmarker kann direkt angeklickt werden. KST4Contest übernimmt die Station daraufhin als aktuelle Auswahl, scrollt die Benutzerliste zum passenden Chatmember und aktualisiert den **Further Info**-Bereich.

Marker, die bei der aktuellen Zoomstufe zu dicht beieinanderliegen, werden als Cluster mit einer Stationsanzahl angezeigt. Ein Klick auf einen Cluster vergrößert den betreffenden Kartenausschnitt. Erst ein anschließend sichtbarer einzelner Marker wählt eine konkrete Station aus.

Für die ausgewählte Station erscheinen rechts unter **Selected station**:

- Rufzeichen,
- Locator,
- QRB und QTF,
- erkannte aktive Bänder,
- gegebenenfalls `B+` für eine offene Bandmöglichkeit und
- die zuletzt bekannten QRGs.

**Trigger cluster spot** sendet für die ausgewählte Station einen einzelnen Spot an die mit dem integrierten DX-Cluster-Server verbundenen Logprogramme. Die Schaltfläche setzt deshalb einen aktivierten Cluster-Server und mindestens einen verbundenen Client voraus.

Unterhalb der Karte befindet sich das Höhenprofil. Rechts werden die dazugehörigen Detailwerte angezeigt, unter anderem:

- verwendete Datenquelle und Anzahl der Höhenpunkte,
- Analysefrequenz,
- Erdkrümmungs- beziehungsweise Refraktionsmodell,
- Radio- und Geländehorizont,
- Fresnel-Freiheit,
- erkannte Hindernisse,
- Link-Budget,
- geschätzter Empfangspegel und
- eine zusammenfassende Pfadbewertung.

Mit **Hide path analysis** werden das Profil unterhalb der Karte und die ausführlichen Analysewerte rechts gemeinsam ausgeblendet. Der Kartenbereich erhält dadurch mehr Platz.

![Stationskarte mit ausgeblendeter Pfadanalyse](station_map_compact.png)

Der Hinweis **Path analysis is hidden** bleibt zusammen mit **Show path analysis** sichtbar. Die Funktion kann daher ohne Umweg wieder eingeschaltet werden. Der Zustand wird gespeichert.

Der Divider zwischen Karte und Detailbereich lässt sich horizontal verschieben. Bei schmalem Detailbereich werden längere Angaben umgebrochen; falls die Höhe nicht ausreicht, erscheint dort eine vertikale Scrollleiste.

Ausführliche Herleitung und Grenzen: [Stationskarte und Streckenanalyse](de-Funktionen#stationskarte-und-streckenanalyse-ab-v141)

---

## Globale Nachrichtentabs und Monitorfenster

Der untere Bereich des Hauptfensters enthält drei globale Nachrichtentabs. Ihr Inhalt ist nicht von der aktuell in der Benutzerliste ausgewählten Station abhängig.

| Tab | Inhalt |
|---|---|
| **Public messages** | Öffentliche Chatnachrichten, CQ-Rufe und Beacons |
| **DXCluster messages** | Über ON4KST empfangene DX-Cluster-Meldungen |
| **QSO of the other** | Gerichtete Nachrichten zwischen zwei anderen Stationen |

![Globale Nachrichtentabs im Hauptfenster](global_message_tabs.png)

Im Tab **QSO of the other** werden Absender und Empfänger getrennt dargestellt. Die Spalten **Last QRG TX** und **Last QRG RX** enthalten die zuletzt für beide Stationen bekannten Frequenzen. Sie geben nicht zwingend die QRG der angezeigten Unterhaltung wieder.

**wkd TX?** und **wkd RX?** zeigen den globalen Worked-Status der beiden Basisrufzeichen. Die Angaben sind nicht bandbezogen.

Der Tab **DXCluster messages** zeigt den meldenden und den gemeldeten Teilnehmer, deren Locator, die QRG, den Meldungstext und den globalen Worked-Status der gemeldeten Station. Welche Felder tatsächlich gefüllt sind, hängt von der vom ON4KST-Server übertragenen Meldung ab.

Nachrichtentexte bleiben einzeilig. Ist eine Zelle zu schmal, erscheint der vollständige Inhalt als Tooltip. Webadressen im Meldungstext lassen sich anklicken.

### Separates Monitorfenster

Zusätzlich öffnet KST4Contest das Fenster **Cluster & QSO of the other**. Es zeigt oben die DX-Cluster-Meldungen und darunter die gerichteten Nachrichten zwischen anderen Stationen.

![Separates Cluster- und QSO-Monitorfenster](cluster_qso_monitor.png)

Die Position des vertikalen Dividers sowie die Fenstergröße werden zusammen mit den übrigen UI-Einstellungen gespeichert. Nach einer Änderung **Save Settings** verwenden.

Das Fenster lässt sich über das Menü aus- und wieder einblenden:

```text
Windows → Hide cluster / stranger QSOs
Windows → Show cluster / stranger QSOs
```

Die Tabellen im Hauptfenster und im Monitorfenster greifen auf dieselben Daten zu. Das Ausblenden des Monitorfensters beendet daher weder den Empfang noch die Darstellung in den unteren Tabs.

Herleitung und Grenzen: [Globale Nachrichtenansichten](de-Funktionen#globale-nachrichtenansichten).

---

## Menü

### Windows

- **Hide cluster / stranger QSOs** beziehungsweise **Show cluster / stranger QSOs**: Blendet das zusätzliche Cluster- und QSO-Monitorfenster aus oder wieder ein.
- **hide options** beziehungsweise **show options**: Blendet das Einstellungsfenster aus oder wieder ein.
- **Use dark mode design**: Aktiviert das dunkle Farbschema.
- **Use default mode design**: Aktiviert das normale helle Farbschema.
- **Show / hide station map** öffnet beziehungsweise schließt das separate Fenster mit Stationskarte und Streckenanalyse.

---

## Fenstergrößen und Divider

Beim Klick auf **Save Settings** speichert KST4Contest die Größen der Programmfenster und die Positionen der relevanten Divider in der Konfigurationsdatei. Diese Werte werden beim nächsten Programmstart wiederverwendet.

Das Hauptfenster wird beim Start zusätzlich gegen den sichtbaren Bereich des primären Bildschirms geprüft. Ist die gespeicherte Größe zu groß, verkleinert und verschiebt KST4Contest das Fenster so, dass es wieder erreichbar bleibt. Die genaue Herleitung ist unter [Bildschirmgerechte Größe des Hauptfensters](de-Funktionen#bildschirmgerechte-größe-des-hauptfensters-ab-v141) beschrieben.

Für die übrigen Programmfenster gilt diese zusätzliche Größenbegrenzung derzeit nicht. Wird beispielsweise das separate Monitorfenster nach einem Wechsel auf einen kleineren Bildschirm zu groß dargestellt, muss seine Größe manuell korrigiert und anschließend erneut mit **Save Settings** gespeichert werden.

Bei einer ungünstigen Aufteilung sollten zuerst die Divider an eine brauchbare Position verschoben und die Einstellungen erneut gespeichert werden. Das Löschen der Konfigurationsdatei setzt zwar die UI-Werte zurück, entfernt aber auch die übrigen gespeicherten Programmeinstellungen und sollte deshalb nur verwendet werden, wenn sich die Oberfläche auf anderem Weg nicht mehr herstellen lässt.

---

## Tipps zur Bedienung

- **Einstellungsfenster geöffnet lassen**: Schneller Zugriff auf Beacon-Aktivierung/Deaktivierung.
- **Rechtsklick in der Benutzerliste**: Öffnet das Snippet-Menü und weitere Aktionen (QRZ.com-Profil, NOT-QRV-Tags setzen).
- **Enter aus dem Chat heraus**: Wenn im Sendfeld Text steht, sendet Enter direkt – auch wenn der Fokus woanders liegt.
- **Beacon stoppen**: Beim Scannen von Frequenzen den Beacon ausschalten, damit der Chat nicht mit Meldungen überflutet wird.
