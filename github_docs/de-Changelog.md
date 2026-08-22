# Changelog

> 🇬🇧 [English version](en-Changelog) | 🇩🇪 Du liest gerade die deutsche Version

Versionsverlauf von KST4Contest / PraktiKST.

Die veröffentlichten Stable-Versionen und ihre Programmpakete stehen unter [GitHub Releases](https://github.com/praktimarc/kst4contest/releases). Zusätzlich enthält diese Seite die Änderungen des aktuellen Entwicklungsstands, soweit sie bereits implementiert und geprüft wurden.

---

## v1.42 – Nightly / in Entwicklung

> Stand dieses Abschnitts: 14. August 2026.  
> v1.42 ist noch kein veröffentlichtes Stable-Release. Bis zur Freigabe können weitere Änderungen hinzukommen.

v1.42 führt mehrere bisher getrennte Auswertungen zusammen. Bandinformationen, Worked-Status, NOT-QRV-Markierungen, Rufzeichensuffixe und Frequenzen werden dadurch konsistenter in der Benutzerliste, der Stationskarte, der Prioritätsberechnung und den externen Schnittstellen verwendet.

### Neu

- **Sichtbarer ON4KST-Verbindungsstatus:** Ein kompakter `LINK`-Indicator im Hauptfenster zeigt den tatsächlichen Zustand der ON4KST-Verbindung an. Grün bedeutet vollständig angemeldet und synchronisiert, Gelb kennzeichnet Verbindungsaufbau und Synchronisation, Rot eine unterbrochene Verbindung, einen Fehler oder die Wartezeit vor dem nächsten Verbindungsversuch.

- **Gemeinsame Herleitung verfügbarer Bänder:** Ein zentraler `BandOpportunityResolver` wertet aktuelle QRGs, Bandangaben im Namensfeld, aktive Rufzeichenvarianten, Worked-Informationen und NOT-QRV-Markierungen gemeinsam aus. Benutzerliste, **New bands**, Band-Upgrade-Hinweis, Priority Score, Stationskarte und automatische Bandauswahl verwenden damit dieselbe Grundlage.

- **Erweiterte Bandanzeige:** Die Bandspalten unterscheiden jetzt:
  - `X` für auf diesem Band gearbeitet,
  - `a` für ein angebotenes, noch nicht gearbeitetes Band einer insgesamt neuen Station,
  - `B+` für ein angebotenes, noch nicht gearbeitetes Band einer bereits auf einem anderen Band gearbeiteten Station und
  - `o` für ein auf diesem Band bereits gearbeitetes Locator-Großfeld.

  Die Anzeigen können kombiniert werden, beispielsweise als `ao` oder `B+o`. Die zusätzlichen Kennzeichnungen `a` und `o` lassen sich in den GUI-Einstellungen separat ausblenden.

- **Unterstützung für 50 und 70 MHz:** Beide Bänder stehen in der Stationskonfiguration, den Worked- und NOT-QRV-Funktionen, der Benutzerliste, den Filtern, der internen Datenbank, der UCXLog-Auswertung und dem Win-Test-Listener zur Verfügung. Bereits gespeicherte Datenbanken werden um die benötigten Spalten ergänzt.

- **Globale Nachrichtentabs:** Öffentliche Nachrichten, ON4KST-DX-Cluster-Meldungen und gerichtete Nachrichten zwischen anderen Stationen können direkt im Hauptfenster angezeigt werden. Das bisherige separate Monitorfenster bleibt zusätzlich verfügbar und verwendet dieselben Nachrichtenspeicher.

- **Manuelle QTF-Eingabe:** Die aktuelle Antennenrichtung kann auch ohne PSTRotator direkt in KST4Contest geändert werden.

- **Filter zurücksetzen:** Ein eigener Reset-Button entfernt die aktiven Filterprädikate der Benutzerliste zuverlässig.

- **Kartencluster:** Räumlich dicht beieinanderliegende Stationen werden bei niedrigen Zoomstufen zusammengefasst. Die ausgewählte Station und relevante Richtungsgelegenheiten bleiben einzeln sichtbar.

- **Ausblendbare Streckenanalyse:** Geländeprofil und Analysebereich der Stationskarte können vollständig ausgeblendet werden. Die Auswahl wird gespeichert und beim nächsten Programmstart wiederhergestellt.

### Geändert

- **Sessionbezogene ON4KST-Verbindungssteuerung:** Socket, Reader, Writer, Messagebus und Warteschlangen gehören jetzt zu einer eindeutig identifizierten Verbindungssession. Veraltete Threads einer abgelösten Verbindung können dadurch keine Daten mehr verarbeiten oder die neue Verbindung schließen. `ONLINE` wird erst nach bestätigtem Login und vollständig empfangenen Benutzerlisten gemeldet. Verbindungsaufbau, Login und Synchronisation besitzen feste Zeitlimits; Heartbeats, ausbleibende Eingangsdaten, EOF sowie Lese- und Schreibfehler werden überwacht und lösen bei Bedarf einen kontrollierten Neuaufbau mit Backoff aus.

- **ON4KST-Protokollbefehle abgesichert:** Ausgehende Befehle werden zentral aufgebaut und auf gültige Kategorien, Locatoren und unerlaubte Frame-Trennzeichen geprüft. Da ON4KST pro TCP-Session nur einen Locator verwaltet, wird für beide Chat-Kategorien der Hauptlocator verwendet und eine abweichende zweite Konfiguration protokolliert, statt widersprüchliche Befehle an den Server zu senden.

- **QRG-Erkennung präzisiert:** Vollständige und relative Frequenzangaben werden weiterhin erkannt. Nackte dreistellige Zahlen gelten nur noch bei erkennbarem Frequenzkontext als QRG. Signalrapporte, Bandangaben und andere Zahlen erzeugen dadurch seltener falsche Frequenzen.

- **Stationsbezogener Frequenzkontext:** Bei relativen QRGs verwendet KST4Contest zuerst einen höchstens 30 Minuten alten Bandkontext derselben Station. Erst wenn dieser fehlt, wird das global konfigurierte Fallback-Band verwendet.

- **Fallback-Band als Dropdown:** Das globale Fallback kann nur noch aus unterstützten Bandwerten ausgewählt werden. Es betrifft die gesamte QRG-Erkennung und nicht nur DX-Cluster-Spots.

- **Einheitliche QRG-Darstellung:** Frequenzen werden in Benutzer- und Nachrichtentabellen mit mindestens drei Nachkommastellen dargestellt.

- **Bandabhängige AirScout- und Streckenberechnung:** KST4Contest leitet für jede Station eine möglichst realistische Frequenz aus der aktuellen QRG und den bekannten Bandinformationen ab. AirScout erhält kanonische Bandwerte. Die frühere Zwischenlösung mit 430 MHz wurde durch 432 MHz ersetzt.

- **Gemeinsame Frequenzherleitung:** AirScout, **Calc selected** und die Pfadanalyse der Stationskarte verwenden denselben `PropagationFrequencyResolver`. Ein im Reachability-Dropdown ausdrücklich gewähltes Band wird bei manuellen Berechnungen berücksichtigt.

- **Rufzeichenvarianten getrennt verarbeitet:** Aktive Chatmember werden durch das vollständige Rufzeichen einschließlich Suffix und die Chat-Kategorie unterschieden. `DN9APW`, `DN9APW-2` oder vergleichbare Logins bleiben dadurch getrennte Nachrichtenziele.

- **Gemeinsame Basisinformationen:** Worked-Flags, NOT-QRV-Informationen und der Priority Score werden weiterhin für Varianten desselben Basisrufzeichens gemeinsam ausgewertet. Getrennte Nachrichtenziele führen damit nicht zu widersprüchlichen Worked-Daten.

- **Priority Score korrigiert:** Stationen ohne gemeinsames verfügbares Band oder mit übersteuernder NOT-QRV-Markierung werden nicht mehr als Prioritätskandidaten angeboten. Bandgelegenheiten bereits gearbeiteter Stationen können einen eigenen Priority Boost erhalten.

- **Sked-Erstellung erweitert:** Das Band wird aus den lokal aktivierten Bändern gewählt. Für die Win-Test-Übergabe wird `SSB` oder `CW` ausdrücklich ausgewählt, statt den Mode unzuverlässig aus dem Band abzuleiten.

- **Win-Test-Sked-Übergabe präzisiert:** Die QRG muss zum gewählten Band passen. Sichtbare KST-Suffixe werden für das Logziel entfernt, portable Bestandteile bleiben erhalten und die Zeitangaben der `ADDSKED`-Pakete werden korrekt erzeugt. Ein Fehler bei der Übergabe entfernt den internen KST4Contest-Sked nicht.

- **Exakte Sked-Ziele:** Timeline und automatische Erinnerungen verwenden das vollständige sichtbare KST-Rufzeichen. Ein Sked für `DN9APW-2` wird nicht versehentlich an eine andere Variante desselben Basisrufzeichens gesendet.

- **Beacon und Autoantwort überarbeitet:** Beide Chat-Kategorien verwenden einen gemeinsamen Timer, behalten aber getrennte Aktivierungsschalter und Texte. Das zulässige Mindestintervall beträgt zwei Minuten; Nachrichtentexte sind auf 120 Zeichen begrenzt. Die gespeicherte Beacon-Aktivierung wird beim Start aus der Konfiguration übernommen.

- **Variablen zentral aufgelöst:** Nachrichtenvariablen für Beacons, Shortcuts, Snippets und andere automatisch erzeugte Texte werden über einen gemeinsamen Resolver verarbeitet.

- **Nachrichtentabellen verbessert:** Abgeschnittene Nachrichtentexte erhalten einen Tooltip mit dem vollständigen Inhalt. Erkannte Webadressen können im Systembrowser geöffnet werden.

- **Kompaktere Filterleiste:** Die Filter bleiben bei normaler Breite in einer kompakten Anordnung und werden erst dann umgebrochen, wenn der tatsächlich verfügbare Platz nicht mehr ausreicht. Der mittlere Divider kann dadurch weiter verschoben werden.

- **DXLog-Gesamtlog übernommen:** Der UCXLog-kompatible UDP-Listener verarbeitet neben `contactinfo` auch `contactreplace`. Dadurch kann ein von DXLog.net als vollständiges Log ausgesendeter Datenbestand eingelesen werden.

- **Versionserkennung verbessert:** Versionsnummern werden semantisch verglichen, damit beispielsweise Patch-Versionen und Nightly-Stände nicht mehr durch eine einfache Fließkommazahl falsch eingeordnet werden.

### Behoben

- **Zuverlässige Benutzerliste beim Login:** Ungültige oder unvollständige `UA0`-Teilnehmerdatensätze werden einzeln verworfen und protokolliert, ohne die Verarbeitung der alphabetisch folgenden Teilnehmer abzubrechen. Die gültigen Einträge werden zunächst pro Kategorie gesammelt und erst mit dem ersten zugehörigen `UE`-Abschlussframe vollständig veröffentlicht.

- **Benutzerliste verschwindet nach dem Login:** ON4KST kann nach Namens-, Status- oder anderen Live-Änderungen weitere `UE`-Frames für dieselbe Kategorie senden. Wiederholte Abschlussframes werden jetzt erkannt und ignoriert, damit eine bereits gefüllte Benutzerliste nicht durch eine leere Momentaufnahme ersetzt wird.

- **Fehlgeschlagener Erstaufbau und Verbindungsverlust:** Wenn beim Programmstart keine Verbindung zum Server hergestellt werden kann, läuft KST4Contest nicht mehr in eine Endlos- oder Busy-Wait-Schleife. Die Oberfläche bleibt bedienbar und weitere Versuche erfolgen mit begrenztem Backoff. Auch ein vom Server geschlossener oder über längere Zeit stummer Socket wird zuverlässig erkannt.

- **Messagebus-Protokollierung:** Bereits korrekt verarbeitete ON4KST-Frames werden nicht mehr zusätzlich als `Critical, detected unhandled Chatmessage` gemeldet. Nur tatsächlich unbekannte Telegramme erreichen noch diesen Logzweig.

- **Passwort im Fehlerlog:** Das ON4KST-Passwort wird beim Verbindungsaufbau nicht mehr im Klartext in die Konsole oder Logdatei geschrieben.

- **Langzeitfehler der Stationsauswahl:** Die vom Message-Thread verwalteten Chatmember wurden von der JavaFX-Ansicht entkoppelt. Gleichzeitige Änderungen der Daten und Tabellenansicht führen dadurch nicht mehr nach längerer Laufzeit zu fehlerhaften Auswahlmodellen oder Concurrent-Modification-Problemen.

- **Keine Phantom-Chatmember durch UM3:** Historische oder zusätzliche Servermeldungen erzeugen keine Benutzerlisteneinträge für Stationen, die nicht tatsächlich im Chat angemeldet sind.

- **Nachrichten an Rufzeichen mit Suffix:** Mehrere gleichzeitig angemeldete Varianten desselben Basisrufzeichens überschreiben sich nicht mehr gegenseitig. Damit wurde [Issue #73](https://github.com/praktimarc/kst4contest/issues/73) behoben.

- **DX-Cluster-Locatoren:** Sender und gemeldete Station erhalten nicht mehr versehentlich denselben Locator. Damit wurde [Issue #48](https://github.com/praktimarc/kst4contest/issues/48) behoben.

- **Worked-Anzeige in „QSO of the other“:** Die Worked-Spalten verwenden wieder die jeweils richtige sendende beziehungsweise empfangende Station.

- **Fehlende SECONDAP-Daten:** Eine nicht vorhandene zweite Aircraft-Scatter-Gelegenheit führt beim Bearbeiten der Anzeige nicht mehr zu einer ungültigen Textauswahl und JavaFX-Exception.

- **Historische Rufzeichen:** Das Hervorheben oder Anklicken eines Rufzeichens, das nicht mehr in der aktuellen Benutzerliste vorhanden ist, läuft nicht mehr in eine Exception.

- **Win-Test-Sked-Zeit und Rufzeichen:** Zeitstempel, Band-QRG-Zuordnung sowie die Behandlung von KST-Suffixen und portablen Rufzeichen wurden korrigiert.

- **Filter-Reset:** Alle Filterprädikate werden tatsächlich entfernt; der sichtbare Zustand des Buttons entspricht wieder dem wirksamen Filterzustand.

### Dokumentation und Auslieferung

- Das deutsche und englische Handbuch wurde systematisch mit dem Quellcode abgeglichen, erweitert und mit aktuellen Screenshots versehen. Die Dokumentation erklärt nicht nur die Bedienelemente, sondern auch Herleitung, Datenquellen und Grenzen der Funktionen.

- Für Arch Linux stehen `kst4contest-bin`, `kst4contest` und `kst4contest-git` im AUR zur Verfügung.

- Die Downloadseite unterscheidet Stable, Beta und Nightly und bietet die jeweils tatsächlich vorhandenen Pakete für Windows, Linux und macOS an.

- Nightly-Pakete werden automatisiert aus dem aktuellen `main`-Branch gebaut. Stable- und Beta-Releases verwenden reproduzierbare Paketnamen für die unterstützten Plattformen.

- **Signierte und notarisierte macOS-Pakete:** Die DMG-Dateien für Apple Silicon und Intel sind mit einer Apple Developer ID signiert und von Apple notarisiert; das Notarisierungsticket ist in der DMG hinterlegt. Der erste Start funktioniert damit per Doppelklick, ohne den bisherigen Umweg über **Öffnen** im Kontextmenü, und die Prüfung gelingt auch ohne Internetverbindung. Betroffen sind Nightly-, Beta- und Stable-Pakete gleichermaßen. Die Windows-Pakete sind weiterhin nicht signiert.

- **Korrekte Bundle-Kennung und Version unter macOS:** Die Anwendung meldet sich jetzt als `de.x08.KST4Contest` statt als `kst4contest.view` und trägt die tatsächliche Versionsnummer im Bundle. Bisher wies jedes Release im Finder unter **Informationen** die Version `1.0` aus. Bestehende Einstellungen sind davon nicht betroffen, da KST4Contest seine Daten in `~/.praktiKST/` ablegt und nicht an der Bundle-Kennung festmacht.

### Bekannte Grenzen

- Die aktive Geländedatenquelle verwendet Open-Meteo mit Copernicus-GLO-90-Daten und höchstens 100 Höhenpunkten pro Strecke.

- Der atmosphärische K-Faktor ist derzeit fest auf `4/3` eingestellt.

- Für die Gegenstation wird eine Antennenhöhe von 10 Metern über Grund angenommen.

- Aircraft-Scatter-Daten aus AirScout und die Geländeanalyse der Stationskarte sind weiterhin getrennte Bewertungen.

- Eine genauere Konfiguration von Stationshöhe, Frequenz und K-Faktor wird in [Issue #74](https://github.com/praktimarc/kst4contest/issues/74) weiterverfolgt.

---

## v1.41.1 (2026-07-08)

**Hotfix für Texteingabe und Fokusverhalten**

### Behoben

- Das Nachrichteneingabefeld wurde nach einiger Zeit beziehungsweise bei bestimmten UI-Aktualisierungen unerwartet geleert.
- Beim Filtern oder Auswählen einer Station wurde der Eingabefokus unbeabsichtigt wieder in das Sendefeld verschoben.

Die korrigierte Version ist als [Release v1.41.1](https://github.com/praktimarc/kst4contest/releases/tag/v1.41.1) verfügbar.

---


## v1.41.0 (2026-07-01)

**Stationskarte, begrenzte Nachrichtenspeicher und bildschirmgerechtes Hauptfenster**

### Neu

- **Stationskarte:** Eine interaktive OpenStreetMap-Karte stellt aktive Chatmember mit brauchbarem Locator geografisch dar.

- **Antennensektor und Verbindungslinie:** Die Karte zeigt den aktuellen eigenen QTF, den konfigurierten Antennen-Öffnungswinkel, das maximale QRB und die Verbindung zur ausgewählten Station.

- **Maidenhead-Raster:** Ein an die Zoomstufe angepasstes Locator-Raster erleichtert die geografische Einordnung.

- **Geländeprofil:** Für ausgewählte Stationen kann ein Höhenprofil über die Open-Meteo Elevation API berechnet werden. Die aktive Datenquelle verwendet Copernicus GLO-90 und höchstens 100 gleichmäßig verteilte Abfragepunkte.

- **Geometrische Streckenanalyse:** Die Auswertung berücksichtigt Sichtlinie, Erdkrümmung mit `k = 4/3`, Radio- und Geländehorizont, erste Fresnel-Zone und eine grobe Hindernisabschätzung.

- **Lokaler Karten-Proxy:** Leaflet wird mit der Anwendung ausgeliefert. Kartenkacheln werden über einen lokalen Proxy geladen, damit keine externe JavaScript-Bibliothek zur Laufzeit nachgeladen werden muss. Für die OpenStreetMap-Kacheln und die Online-Höhendaten ist weiterhin eine Internetverbindung erforderlich.

### Geändert

- **Begrenzte Nachrichtenspeicher:** Die globale Chatnachrichtenliste wird oberhalb von 30.000 Einträgen auf 25.000 verkleinert. Der getrennte DX-Cluster-Speicher wird oberhalb von 10.000 Einträgen auf 8.000 verkleinert.

- **Bildschirmgerechte Startgröße:** Das Hauptfenster wird beim Start gegen den sichtbaren Bereich des primären Bildschirms geprüft und bei Bedarf verkleinert oder verschoben.

- **Kompaktere Benutzeroberfläche:** Mehrere Bereiche wurden für kleinere Bildschirme und verstellbare Divider angepasst.

### Einordnung der Kartenfunktion

Die Stationskarte und AirScout können dieselbe Gegenstation betreffen, führen aber getrennte Berechnungen durch. Aircraft-Scatter-Flugzeuge werden nicht in das Geländeprofil eingerechnet.

Im Quellcode vorhandene Klassen für Copernicus GLO-30, Offline-DEM-Import und weitere Terrain-Provider waren in v1.41 nicht Bestandteil der aktiv verwendeten Berechnungskette. Die tatsächlich verwendete Online-Höhenquelle ist Open-Meteo auf Basis von Copernicus GLO-90.

---

## v1.40 (2026-02-16)
**Großes Feature-Release: Score-System, AP-Timeline, Win-Test, PSTRotator**

**Neu:**
- **Chatmember Score-System**: Jeder Chatmember erhält automatisch eine Prioritätsbewertung anhand von Antennenrichtung, Aktivitätszeit, Nachrichtenanzahl, aktiven Bändern, Frequenzen, Sked-Richtung und anderen Faktoren. Die Top-Kandidaten werden in einer eigenen Liste hervorgehoben.
- **AP-Timeline**: Für jeden möglichen AP-Ankunftsminuten-Slot werden bis zu 4 hochbewertete Stationen angezeigt, die erreichbar wären. Bevorzugt werden APs mit dem höchsten Potenzial, nicht die schnellste Ankunft. Stationen, auf die die eigene Antenne nicht zeigt, werden transparent dargestellt.
- **Win-Test-Unterstützung** (ab v1.31 als Beta, jetzt vollständig konfigurierbar): Log-Synchronisation, Frequenzauswertung und **Sked-Übergabe via UDP** vollständig integriert. In den Preferences aktivier-/deaktivierbar.
- **PSTRotator-Interface** (ab v1.31 als Beta, jetzt vollständig konfigurierbar): Aktualisierung der Rotatorposition direkt aus KST4Contest. In den Preferences aktivier-/deaktivierbar.
- **QSO-Sniffer**: Nachrichten von konfigurierbaren Rufzeichen-Listen werden automatisch in das PM-Fenster weitergeleitet.
- **Band-Alert bei gearbeiteten Stationen**: Wenn eine Station geloggt wird, erscheint ein Hinweis, wenn diese Station ein weiteres Band aktiv hat, auf dem man selbst ebenfalls QRV ist.
- **Sked-Erinnerungs-ALERT**: Pro Chatmember kann ein Sked-Alarm mit automatischen Nachrichten in konfigurierbaren Intervallen (2+1 / 5+2+1 / 10+5+2+1 Minuten vor dem Sked) eingerichtet werden, plus akustische und optische Benachrichtigung.
- **Chat-Historie beim Start laden**: Beim Verbindungsaufbau wird die Serverhistorie geladen, um aktive Chatmember und letzte Nachrichten sofort sichtbar zu machen.
- **Skedfail-Button**: Im FurtherInfo-Panel kann ein Sked-Misserfolg für einen Chatmember markiert werden, was dessen Score senkt.

**Geändert:**
- AP-Notizen in DX-Cluster-Spots integriert.
- Scrolling der Chatmember-Tabelle folgt automatisch der aktuellen Nachrichtenauswahl.
- Generic Auto-Antwort und QRG-Auto-Antwort senden max. einmal pro 45 Sekunden pro Rufzeichen (verhindert Spam-Schleifen).
- Speicherbare Einstellungen erweitert: ServerDNS/Port, PSTRotator-Interface, Win-Test-Interface, Callsign-Sniffer, Dark-Mode-Standard.
- Datum in der Chat-Tabelle entfernt (nur Uhrzeit verbleibt – spart Platz).

**Behoben:**
- Benutzerliste wird jetzt bei jedem Neu-Login automatisch sortiert.
- Posonpill-Nachrichten beenden jetzt nur genau eine Client-Instanz (nicht alle und nicht wtKST).
- wtKST: Absturz bei KST4Contest-Trennung behoben.
- Mehrere Probleme mit Rufzeichen-Suffixen wie `/p`, `-2` etc. behoben.
- `QTFDefault` wurde nicht korrekt gespeichert → behoben.
- AirScout-Watchlist (ASWATCHLIST) wurde nicht korrekt aktualisiert → behoben.
- Dark Mode: QRG-Felder wurden nicht vollständig angezeigt → behoben.
- Versionsnummer-Anzeige korrigiert.

---

## v1.31 (2025-12-13)
**Win-Test + PSTRotator Beta, QSO-Sniffer, DNS-Hotfix**

**Neu:**
- **Win-Test-Unterstützung** (Beta, noch nicht deaktivierbar): Log-Synchronisation und Frequenzauswertung.
- **PSTRotator-Unterstützung** (Beta, noch nicht deaktivierbar).
- **QSO-Sniffer**: Nachrichten von konfigurierbaren Rufzeichen werden ins PM-Fenster weitergeleitet.

**Geändert:**
- **DNS-Server geändert**: Von `www.on4kst.info` auf `www.on4kst.org` (Hotfix). Der DNS-Server ist ab sofort in den Preferences änderbar.

**Behoben:**
- Endlosschleife im Fehlerfall friert den Client ein → behoben.

---

## v1.266 (2025-10-03)
**AirScout-Fix für Rufzeichen mit Suffix**

**Behoben:**
- AirScout-Interface funktionierte nicht, wenn das Login-Rufzeichen einen Suffix enthielt (z. B. `9A1W-2`). AirScout kann mit diesem Format nicht umgehen – es wird jetzt nur noch das Basis-Rufzeichen ohne Suffix an AirScout übergeben.

*(Fehler gemeldet und getestet von 9A2HM / Kreso – herzlichen Dank!)*

---

## v1.265 (2025-09-28)
**Richtungs-Buttons bleiben aktiviert eingefärbt**

**Behoben:**
- Richtungs-Buttons (N / NE / E usw.) behalten jetzt ihre Farbe, wenn sie aktiviert sind, sodass der Aktivierungsstatus auf einen Blick erkennbar ist.

---

## v1.264 (2025-08-02)
**Simplelogfile: Rufzeichen-Erkennung verbessert**

**Behoben:**
- Rufzeichen wie `S53CC`, `S51A` usw. wurden in der SimpleLogFile-Auswertung nicht als gearbeitet markiert → Erkennungsmuster verbessert.

*(Fehler gemeldet von Boris, S53CC – danke!)*

---

## v1.263 (2025-06-08)
**AirScout-Kommunikation und Login-Name**

**Geändert:**
- AirScout-Kommunikation grundlegend überarbeitet: Nur noch Stationen mit QRB < max-QRB werden an AirScout gesendet.
- Abfrage-Intervall von 12 Sekunden auf **60 Sekunden** erhöht.
- Deutlich weniger Berechnungsaufwand und Nachrichtenverkehr → Stabileres AirScout-Tracking.
- Name des AS-Clients und AS-Servers ist jetzt aus den Preferences konfigurierbar (war vorher hartcodiert auf „KST" / „AS").

**Behoben:**
- „Track in AirScout"-Button war sehr träge → durch neue Kommunikationslogik deutlich verbessert.
- Name im Chat ist jetzt speicherbar (Fehler behoben).
- Visuelle Korrekturen vor und nach dem Login.
- Fehler behoben, der von 9A2HM (Kreso) gemeldet wurde.

---

## v1.262 (2025-05-21)
**Freeze-Fix bei vorzeitiger Nachrichtenlieferung**

**Behoben:**
- ON4KST liefert manchmal Nachrichten, bevor der Login abgeschlossen ist. Das verursachte Fehler in der Nachrichtenverarbeitung → jetzt behoben.

---

## v1.26 (2025-05)
**Multi-Channel-Login und Dark Mode**

**Neu:**
- **Dark Mode**: Umschaltbar über `Window → Use Dark Mode`.
- **Multi-Channel-Login**: Gleichzeitiger Login in zwei Chat-Kategorien.
- **Opposite Station Multi-Callsign Login-Tagging**: Unterstützung für Stationen mit mehreren Rufzeichen.

**Geändert:**
- Farbgebungs-Mechanismus überarbeitet: Farben können jetzt über CSS angepasst werden.

**Behoben:**
- Stationsmarkierung komplett überarbeitet und korrekt gestellt.

---

## v1.251 (2025-02)
**Bugfix für UDP-Broadcast-Spot-Info**

**Behoben:**
- Problem beim Lesen von UDP-Broadcast-Spot-Informationen behoben (gemeldet von Steve Clements – danke!).
- Stationsmarkierung (erneut verbessert).

---

## v1.25 (2025-02)
**Wunschliste umgesetzt**

**Neu:**
- **Neuer Einstellungs-Tab: Messagehandling**
  - Auto-Antwort auf eingehende Nachrichten konfigurierbar.
  - Automatische Antwort mit eigener CQ-QRG, wenn jemand danach fragt.
  - Konfigurierbarer Standard-Filter für das Userinfo-Fenster *(für Gianluca :-) )*.
- **Farbige PM-Zeilen**: Neue Privatnachrichten erscheinen rot und faden alle 30 Sekunden über Gelb bis Weiß ab *(Idee von IU3OAR, Gianluca)*.

**Behoben:**
- Stationen mit Suffixen wie „-2" und „-70" wurden nicht als gearbeitet markiert → werden jetzt ignoriert, Station wird korrekt markiert.

---

## v1.24 (2024-11)
**Wunschliste + DX-Cluster-Spots**

**Neu:**
- Button zum Öffnen des **QRZ.com-Profils** der ausgewählten Station.
- Button zum Öffnen des **QRZ-CQ-Profils** der ausgewählten Station.
- **DX-Cluster-Server-Integration**: Richtungs-Warnungen werden als Spots an das Logprogramm gesendet (wenn QRG bekannt).

*(Zusätzlich wurden Farbgebungen der PM-Zeilen hinzugefügt – tnx Gianluca)*

---

## v1.23 (2024-10)
**Integrierter DX-Cluster-Server**

**Neu:**
- KST4Contest enthält jetzt einen **integrierten DX-Cluster-Server**.
- Generiert DX-Cluster-Spots und sendet sie an das Logprogramm, wenn eine Richtungs-Warnung ausgelöst und eine QRG bekannt ist.
- Spotter-Rufzeichen muss sich vom Contest-Rufzeichen unterscheiden (für korrekte Filterung im Logprogramm).

*(Idee von OM0AAO, Viliam Petrik – danke!)*

---

## v1.22 (2024-05)
**Usability-Verbesserungen und AirScout-Button-Fix**

**Neu:**
- Neue Variablen (tnx OM0AAO, Viliam Petrik):
  - `MYLOCATORSHORT`
  - `MYQRGSHORT`
  - `QRZNAME`

**Geändert:**
- Sendfeld-Fokus: Nach Klick auf Rufzeichen in der Benutzerliste erhält das Sendfeld sofort den Fokus – kein Doppelklick notwendig *(tnx Gianluca)*.

**Behoben:**
- Worked-Station-Filter ist jetzt live-aktiv: Gearbeitete Stationen verschwinden sofort nach Aktivierung des Filters *(tnx Gianluca)*.
- QRB-Sortierung war lexikografisch → jetzt numerisch *(tnx Alessandro Murador)*.
- AirScout-„Show Path"-Button: Klick maximiert AirScout und zeigt den Pfad korrekt an.

---

## v1.21 (2024-04)
**Usability-Verbesserungen**

**Geändert:**
- Fenstergrößen und Divider-Positionen werden beim Klick auf „Save Settings" in der Konfigurationsdatei gespeichert und beim Start wiederhergestellt.
- Filter-Bereich als Flowpane → bessere Darstellung auf kleineren Bildschirmen.

---

## v1.2 (2024-04)
**Bandselektion und NOT-QRV-Tags**

**Neu:**
- **Bandselektion**: In den Preferences auswählbar, welche Bänder aktiv sind. Nur für gewählte Bänder erscheinen Buttons und Felder in der UI. Speichern und Neustart erforderlich.
- **NOT-QRV-Tags pro Station und Band**: Stationen können für jedes Band als „nicht QRV" markiert werden. Kombinierbar mit dem Userlist-Filter.
- **QTF-Pfeil**: Der „Show path in AS"-Button zeigt jetzt einen Pfeil mit dem QTF der ausgewählten Station an.

---

## Frühere Versionen

### v1.1
Erste öffentlich veröffentlichte Version. Grundfunktionen:
- Worked-Markierung via Simplelogfile und UDP
- Sked-Richtungs-Hervorhebung
- QRG-Erkennung
- Text-Snippets und Shortcuts
- AirScout-Interface (erste Version)
- Intervall-Beacon
- PM-Abfang für öffentliche Nachrichten mit eigenem Rufzeichen
- Update-Hinweis-Dienst

---

## Geplante Features

- `MYQTF`-Variable (eigene Antennenrichtung als Text)
- ~~Lebensdauer für den Worked-Status (automatisches Zurücksetzen)~~ ✅ **Umgesetzt in v1.40** (3-Tage-Lebensdauer, kein manuelles Zurücksetzen mehr nötig)
- Filterung des „Cluster & QSO der anderen"-Fensters auf eigenes QTF
- Weitere Topografie-basierte Berechnungen für die Richtungswarnung
