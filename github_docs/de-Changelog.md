# Changelog

> 🇬🇧 [English version](en-Changelog) | 🇩🇪 Du liest gerade die deutsche Version

Versionsverlauf von KST4Contest / PraktiKST.

---

letzter Changelog bitte aus GitHub entnehmen. Der bisherige Changelog

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
