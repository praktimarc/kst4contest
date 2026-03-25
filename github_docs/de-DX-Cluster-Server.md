# Integrierter DX-Cluster-Server

> 🇬🇧 [English version](en-DX-Cluster-Server) | 🇩🇪 Du liest gerade die deutsche Version

Ab **Version 1.23** enthält KST4Contest einen integrierten DX-Cluster-Server. Dieser sendet Spots direkt an das Logprogramm, wenn eine Richtungs-Warnung ausgelöst wird.

*(Idee von OM0AAO, Viliam Petrik – danke!)*

---

## Wozu dient der integrierte DX-Cluster-Server?

Wenn KST4Contest erkennt, dass eine Station aus der eigenen Richtung ein Sked anfragt und gleichzeitig eine QRG bekannt ist, wird **automatisch ein DX-Cluster-Spot generiert** und an den Cluster-Client des Logprogramms gesendet.

Das Logprogramm zeigt den Spot in der Bandkarte an. Ein Klick auf den Spot stellt Frequenz und Mode des Transceivers direkt ein – ohne manuelles Eintippen.

---

## Einrichtung

### In KST4Contest

In den Preferences → **DX-Cluster-Server-Einstellungen**:

1. **Port** des internen Servers eintragen (z. B. 7300 oder 8000 – muss mit dem Logprogramm übereinstimmen).
2. **Spotter-Rufzeichen** eintragen – **unbedingt ein anderes Rufzeichen als das Contest-Rufzeichen verwenden!**
   - Grund: Logprogramme filtern Spots, die vom eigenen Rufzeichen stammen, als „gearbeitet" heraus. Wenn der Spotter dasselbe Rufzeichen hat, werden die Spots nicht angezeigt.
3. **Angenommene MHz** eintragen: Bei Frequenzangaben wie „.205" im Chat muss KST4Contest entscheiden, ob 144.205, 432.205 oder 1296.205 gemeint ist. Bei Einband-Contests einfach die entsprechende Bandmitte eintragen. Vollständige Frequenzangaben wie „144.205" oder „1296.338" im Chat werden immer korrekt erkannt.

### In UCXLog

- Verbindung zu einem DX-Cluster-Server konfigurieren:
  - Host: `127.0.0.1` (oder IP des KST4Contest-Computers)
  - Port: Wie in KST4Contest konfiguriert
  - Passwort: kann leer bleiben
- Über die Schaltfläche **„Send a test message to your log"** kann die Verbindung getestet werden.

### In N1MM+

Ähnliche Einstellungen:
- Host: `127.0.0.1` (oder IP des KST4Contest-Computers)
- Port: Wie in KST4Contest konfiguriert

---

## Funktionsweise

Ein Spot wird generiert, wenn **beide** Bedingungen erfüllt sind:

1. Eine **Richtungs-Warnung** wurde ausgelöst (Station macht ein Sked in die eigene Richtung).
2. **QRG der Station ist bekannt** (aus dem Chat ausgelesen oder manuell eingetragen).

Der generierte Spot enthält:
- Rufzeichen der Station
- Frequenz
- Spotterzeit

Das Logprogramm kann den Spot dann in der Bandkarte anzeigen und den TRX per Mausklick auf die Frequenz abstimmen.

---

## Multi-Computer-Setup

Wenn KST4Contest auf einem separaten Computer läuft (nicht auf dem Log-Computer):

- Host im Logprogramm: IP des KST4Contest-Computers (nicht `127.0.0.1`)
- Entspricht der Konfiguration der QSO-UDP-Broadcast-Pakete (siehe [Log-Synchronisation](de-Log-Synchronisation))

---

## Getestete Logprogramme

- **UCXLog** ✓
- **N1MM+** ✓

Weitere Testergebnisse sind willkommen – bitte per E-Mail an DO5AMF melden.
