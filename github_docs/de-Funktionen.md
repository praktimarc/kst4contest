# Funktionen

> 🇬🇧 [English version](en-Features) | 🇩🇪 Du liest gerade die deutsche Version

Übersicht aller Hauptfunktionen von KST4Contest.

---

## Sked-Richtungs-Hervorhebung

Eine der Kernfunktionen: Wenn eine Station ein Sked in die **eigene Richtung** sendet, wird sie in der Benutzerliste **grün und fett** hervorgehoben.

### Wie funktioniert das?

Die Berechnung basiert auf folgender Logik:

- Wenn Station A eine Sked-Anfrage an Station B sendet, wird angenommen, dass A ihre Antenne auf B ausrichtet.
- Wenn die daraus resultierende Richtung von A zur eigenen Station innerhalb des halben Öffnungswinkels der eigenen Antenne liegt, wird A hervorgehoben.

**Beispiel** (Öffnungswinkel 69°, Halbwinkel 34,5°):

| Situation | Ergebnis für DO5AMF in JN49 |
|---|---|
| Sked von F5FEN → DM5M | ✅ Hervorhebung (F5FEN zeigt Richtung DM5M, das liegt nahe JN49) |
| Sked von DM5M → F5FEN | ✅ Hervorhebung (DM5M antwortet in Richtung F5FEN) |
| F1DBN ist unbeteiligt | ❌ Keine Hervorhebung |
| DO5AMF/P (anderer Standort) | ❌ Keine Hervorhebung für Sked-Antwort |

Die Berechnung berücksichtigt keine topografischen Wegberechnungen – das ist eine bewusste Vereinfachung. Möglicherweise wird das in einer späteren Version ergänzt.

> Konfiguration: [Konfiguration – Antennen-Öffnungswinkel](Konfiguration#antennen-öffnungswinkel-antenna-beamwidth)

---

## Sked-Richtungs-Spots (Integrierter DX-Cluster)

Ab **v1.23**: Richtungs-Warnungen werden als DX-Cluster-Spots an das Logprogramm weitergeleitet, wenn eine QRG bekannt ist. Details: [DX-Cluster-Server](de-DX-Cluster-Server).

---

## QRG-Erkennung (QRG Reading)

KST4Contest verarbeitet jede Chat-Nachricht und extrahiert automatisch **Frequenzangaben**. Diese werden in der Benutzerliste in der **QRG-Spalte** angezeigt.

Erkannte Formate: `144.205`, `432.088`, `.205` (mit konfigurierter Bandannahme), etc.

**Nutzen**: Ohne nachzufragen kann man direkt auf die QRG einer Station schauen und entscheiden, ob eine Verbindung möglich ist.

---

## Worked-Markierung

Gearbeitete Stationen werden in der Benutzerliste visuell markiert – pro Band. Grundlage ist die [Log-Synchronisation](de-Log-Synchronisation) via UDP oder Simplelogfile.

Vor jedem Contest die Datenbank zurücksetzen: [Konfiguration – Worked Station Database Settings](Konfiguration#worked-station-database-settings).

---

## NOT-QRV-Tags (ab v1.2)

Wenn eine Station mitteilt, dass sie auf einem bestimmten Band nicht QRV ist, kann dies manuell markiert werden:

1. Station in der Benutzerliste auswählen.
2. Rechtsklick → NOT-QRV für das entsprechende Band setzen.

Diese Tags werden in der internen Datenbank gespeichert und bleiben nach einem Neustart von KST4Contest erhalten. Zurücksetzen über die Einstellungen möglich.

**Nutzen**: Verhindert wiederholte Sked-Anfragen auf Bändern, auf denen die Station nicht QRV ist – schont sowohl die eigenen Nerven als auch die der Gegenstation.

---

## Richtungsfilter (Direction Filter)

Zeigt in der Benutzerliste nur Stationen an, die sich in einer bestimmten Richtung befinden. Aktivierbar über die Buttons N / NE / E / SE / S / SW / W / NW oder durch manuelle Eingabe von Grad.

Sinnvoll: Während man CQ in eine bestimmte Richtung ruft, nur Stationen in dieser Richtung anzeigen.

---

## Entfernungsfilter (Distance Filter)

Stationen jenseits einer maximalen Entfernung ausblenden. Schaltfläche **„Show only QRB [km] <="** ist ein Toggle-Button.

---

## Worked- und NOT-QRV-Filter

Toggle-Buttons (einer pro Band) zum Ausblenden bereits gearbeiteter Stationen und/oder NOT-QRV-markierter Stationen. Der Filter wirkt **sofort** ohne manuelles Neu-Aktivieren (ab v1.22 live).

---

## Farbige PM-Zeilen (ab v1.25)

Neue Privatnachrichten erscheinen in **Rot**. Die Farbe wechselt alle 30 Sekunden über Gelb bis Weiß – wie ein Regenbogen-Fade. So ist auf einen Blick erkennbar, wie aktuell eine Nachricht ist.

*(Idee von IU3OAR, Gianluca Costantino – danke!)*

---

## PM-Abfang (Catching Personal Messages)

Manche Nutzer senden Direktnachrichten versehentlich öffentlich, z. B.:

```
(DM5M) pse ur qrg
```

KST4Contest erkennt solche Nachrichten, die das eigene Rufzeichen enthalten, und sortiert sie automatisch in die **Privatnachrichten-Tabelle** ein. So gehen keine Nachrichten verloren.

---

## Multi-Channel-Login (ab v1.26)

Gleichzeitiger Login in **zwei Chat-Kategorien** (z. B. 144 MHz und 432 MHz). Beide Chats werden parallel überwacht.

---

## Dark Mode (ab v1.26)

Aktivierbar über: **Window → Use Dark Mode**

Für individuelle Farbanpassungen: CSS-Datei bearbeiten (Pfad in den Programmunterlagen).

---

## Opposite Station Multi-Callsign Login-Tagging (ab v1.26)

Unterstützung für Stationen, die mit mehreren Rufzeichen gleichzeitig im Chat aktiv sind (z. B. Expedition-Setups).

---

## QRZ.com und QRZ-CQ Profil-Buttons (ab v1.24)

Für ausgewählte Stationen in der Benutzerliste gibt es direkte Buttons, um das **QRZ.com-Profil** und das **QRZ-CQ-Profil** im Browser zu öffnen.

---

## Sked-Erinnerungen (Sked Reminder Service)

Für vereinbarte Skeds können automatische Erinnerungs-PMs konfiguriert werden, die X Minuten vor dem vereinbarten Zeitpunkt gesendet werden. Die Erinnerungen werden aus dem FurtherInfo-Panel heraus aktiviert.

---

## Prioritätsliste / Score-Service

KST4Contest berechnet automatisch eine **Prioritätsliste** der interessantesten Gesprächspartner, basierend auf:

- Richtungserkennung
- QRB (Entfernung)
- AP-Verfügbarkeit (AirScout)
- Worked-Status

Die Top-Kandidaten werden in einer eigenen Liste angezeigt und helfen, im Contest-Stress die wichtigsten Stationen nicht zu übersehen.

---

## Intervall-Beacon

Automatische CQ-Meldungen im öffentlichen Kanal in konfigurierbarem Intervall. Empfohlene Verwendung mit der Variable `MYQRG` für aktuelle Frequenzangabe. Details: [Konfiguration – Beacon Settings](Konfiguration#beacon-settings-automatischer-beacon).

---

## Simplelogfile

Dateibasierte Log-Auswertung per Regex. Details: [Log-Synchronisation](Log-Synchronisation#methode-1-universal-file-based-callsign-interpreter-simplelogfile).

---

## Cluster & QSO der anderen

Ein separates Fenster zeigt den QSO-Fluss zwischen anderen Stationen. Besonders interessant in ruhigeren Nacht-Stunden während des Contests, wenn weniger Verkehr herrscht.

Dieses Fenster kann miniaturisiert werden, wenn es nicht benötigt wird. Zukünftig geplant: Filterung auf Stationen im ausgewählten QTF.
