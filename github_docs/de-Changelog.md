# Changelog

> 🇬🇧 [English version](en-Changelog) | 🇩🇪 Du liest gerade die deutsche Version

Versionsverlauf von KST4Contest / PraktiKST.

---

letzter Changelog bitte aus GitHub entnehmen. Der bisherige Changelog

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
- Lebensdauer für den Worked-Status (automatisches Zurücksetzen)
- Filterung des „Cluster & QSO der anderen"-Fensters auf eigenes QTF
- Weitere Topografie-basierte Berechnungen für die Richtungswarnung
