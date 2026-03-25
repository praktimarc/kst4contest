# Log-Synchronisation

> 🇬🇧 [English version](en-Log-Sync) | 🇩🇪 Du liest gerade die deutsche Version

KST4Contest markiert gearbeitete Stationen automatisch in der Chat-Benutzerliste. Dafür gibt es zwei grundlegende Methoden:

---

## Methode 1: Universal File Based Callsign Interpreter (Simplelogfile)

KST4Contest liest eine Log-Datei und sucht mittels regulärem Ausdruck nach Rufzeichen-Mustern. Dabei werden auch binäre Logdateien unterstützt – unlesbarer Binärinhalt wird einfach ignoriert.

**Vorteil**: Funktioniert mit nahezu jedem Logprogramm, das eine Datei schreibt.
**Nachteil**: Keine Bandinformation möglich – es wird nur „gearbeitet" markiert, nicht auf welchem Band.

Pfad der Log-Datei in den Preferences eintragen. Die Datei wird nur gelesen, nie verändert (read-only).

> **Tipp**: Die Simplelogfile-Funktion kann auch genutzt werden, um Stationen zu markieren, die definitiv nicht erreichbar sind (z. B. eigene Notizen). Das wird in einer späteren Version durch ein besseres Tagging-System ersetzt.

---

## Methode 2: Netzwerk-Listener (UDP-Broadcast) – Empfohlen

Das Logprogramm sendet beim Speichern eines QSOs ein UDP-Paket an die Broadcast-Adresse des Heimnetzwerks. KST4Contest empfängt dieses Paket und markiert die Station inklusive **Bandinformation** in der internen SQLite-Datenbank.

> **Wichtig**: KST4Contest muss **parallel zum Logprogramm laufen**. QSOs, die während einer Abwesenheit von KST4Contest geloggt werden, werden nicht erfasst – außer bei QARTest (kann das komplette Log senden).

**Standard UDP-Port**: 12060 (entspricht dem Standard der meisten Logprogramme)

---

## Unterstützte Logprogramme

### UCXLog (DL7UCX)

UCXLog sendet QSO-UDP-Pakete und Transceiver-Frequenzpakete.

**Einstellungen in UCXLog:**
- UDP-Broadcast aktivieren
- IP-Adresse des KST4Contest-Computers eintragen (bei lokalem Betrieb: `127.0.0.1`)
- Port: 12060 (Standard)

Grün markierte Felder in den UCXLog-Einstellungen beachten: IP und Port müssen eingetragen werden.

Hinweis für Multi-Setup (2 Computer, 2 Radios, eine KST4Contest-Instanz): Beide Logprogramme müssen die QSO-Pakete an die IP des KST4Contest-Computers senden. Dann ist mindestens eine IP nicht `127.0.0.1`.

### QARTest (IK3QAR)

**Besonderheit**: QARTest kann das **vollständige Log** an KST4Contest senden (Schaltfläche „Invia log completo" in den QARTest-Einstellungen). Damit werden auch QSOs erfasst, die vor dem Start von KST4Contest geloggt wurden.

**Einstellungen in QARTest:**
- UDP-Broadcast und IP/Port wie UCXLog konfigurieren
- „Invia log completo" für den vollständigen Log-Upload verwenden

*(„Buona funzionalità caro IK3QAR!" – DO5AMF)*

### N1MM+

**Einstellungen in N1MM+:**

In N1MM+ unter `Config → Configure Ports, Mode Control, Winkey, etc. → Broadcast Data`:
- `Radio Info` aktivieren (für TRX-Sync/QRG)
- `Contact Info` aktivieren (für QSO-Sync)
- IP: `127.0.0.1` (oder IP des KST4Contest-Computers)
- Port: 12060

Für den integrierten DX-Cluster-Server: N1MM+ als DX-Cluster-Client konfigurieren (Server: `127.0.0.1`, Port wie in KST4Contest eingestellt).

### DXLog.net

**Einstellungen in DXLog.net:**
- UDP-Broadcast aktivieren
- IP des KST4Contest-Computers eintragen (grün markierte Felder)
- Port: 12060

### WinTest

WinTest wird ebenfalls unterstützt. KST4Contest empfängt WinTest-UDP-Pakete über einen dedizierten Listener. Die Konfiguration erfolgt analog zu den anderen Programmen.

---

## TRX-Frequenz-Synchronisation

Neben der QSO-Synchronisation übertragen UCXLog und andere Programme auch die **aktuelle Transceiverfrequenz** via UDP. KST4Contest verarbeitet diese Information und stellt sie als Variable `MYQRG` bereit.

**Ergebnis**: Die eigene QRG muss im Chat nie mehr manuell eingegeben werden – ein Klick auf den MYQRG-Button oder die Verwendung der Variable im Beacon genügt.

> **Hinweis für Multi-Setup**: Bei zwei Logprogrammen an zwei Computern sollte nur **eines** die Frequenzpakete senden. KST4Contest kann nicht zwischen den Quellen unterscheiden und verarbeitet alle eingehenden Pakete.

---

## Multi-Setup: 2 Radios, 2 Computer

Für DM5M-typische Setups (2 Radios, 2 Computer, eine KST4Contest-Instanz oder zwei separate):

**Variante A – Eine gemeinsame KST4Contest-Instanz:**
- Beide Logprogramme senden QSO-Pakete an die IP des KST4Contest-Computers
- Nur ein Logprogramm sendet Frequenzpakete (empfohlen: das VHF-Logprogramm)

**Variante B – Zwei separate KST4Contest-Instanzen (empfohlen):**
- Jedes Logprogramm kommuniziert mit seiner eigenen KST4Contest-Instanz via `127.0.0.1`
- Zwei separate Chat-Logins
- Bessere Trennung und weniger Konflikte

---

## Interne Datenbank

KST4Contest speichert die Worked-Information in einer internen **SQLite-Datenbank**. Diese ist von der Logprogramm-Datenbank unabhängig und wird nur über den UDP-Broadcast befüllt.

Vor jedem neuen Contest: Datenbank zurücksetzen! → [Konfiguration – Worked Station Database Settings](Konfiguration#worked-station-database-settings)
