# Konfiguration

> 🇬🇧 [English version](en-Configuration) | 🇩🇪 Du liest gerade die deutsche Version

Nach dem ersten Start öffnet sich das **Einstellungsfenster** – dieses ist der zentrale Ausgangspunkt für alle Konfigurationen. Es empfiehlt sich, das Einstellungsfenster während des Betriebs geöffnet zu lassen (z. B. um den Beacon schnell ein- und auszuschalten).

> **Wichtig**: Nach jeder Änderung unbedingt **„Save Settings"** klicken! Die Einstellungen werden unter Linux in `~/.praktikst/preferences.xml` und unter Windows in `%USERPROFILE%\.praktikst\preferences.xml` (bzw. `C:\Users\<Benutzername>\.praktikst\preferences.xml`) gespeichert. Ab v1.21 werden auch Fenstergrößen und Divider-Positionen beim Speichern gesichert.

---

## Station Settings (Stationseinstellungen)

![Stationseinstellungen](client_settings_window_station.png)

### Login und Chat-Kategorien

Hier werden die Zugangsdaten für den ON4KST-Chat eingetragen (Rufzeichen und Passwort). 
Zudem wird die **primäre Chat-Kategorie** (z. B. IARU Region 1 VHF/Microwave) ausgewählt.

Mit der Option für einen **zweiten Chat** (Multi-Channel-Login) kann man sich gleichzeitig in eine weitere Kategorie (z. B. UHF/SHF) einloggen. Beide Chats werden dann parallel überwacht. Hier kann optional auch ein abweichender Login-Name für den zweiten Chat vergeben werden (nützlich für Opposite Station Multi-Callsign Logging).

### Rufzeichen und Locator

Eigenes Rufzeichen und Maidenhead-Locator (6-stellig, z. B. `JN49IJ`) eintragen. Diese Werte werden für Distanz- und Richtungsberechnungen benötigt.

### Aktivierte Bänder

Über die **„my station uses band"**-Checkboxen werden die aktiven Bänder ausgewählt. Nur für ausgewählte Bänder erscheinen Schaltflächen und Tabellenzeilen in der Benutzeroberfläche. Nach Änderungen muss die Software neu gestartet werden.

### Antennen-Öffnungswinkel (Antenna Beamwidth)

Einen realistischen Wert für den Öffnungswinkel der eigenen Antenne eintragen (in Grad). Dieser Wert wird für die [Sked-Richtungs-Hervorhebung](Funktionen#sked-richtungs-hervorhebung) verwendet. Ein Testwert von 50° hat sich bewährt; DM5M nutzt Quads mit 69°.

> **Keinesfalls** Fantasy-Werte eintragen – die Richtungsberechnungen werden sonst unbrauchbar.

### Standard-Maximum-QRB

Maximale Entfernung (in km), für die Richtungs-Warnungen ausgelöst werden sollen. Realistischer Wert für DM5M: 900 km. Stationen, die weiter entfernt sind, werden für Highlighting-Zwecke ignoriert.

---

## Log-Sync-Einstellungen

Drei Methoden stehen zur Verfügung, um gearbeitete Stationen automatisch zu markieren. Details: [Log-Synchronisation](de-Log-Synchronisation).

### Universal File Based Callsign Interpreter (Simplelogfile)

Interpretiert beliebige Log-Dateien per Regex nach Rufzeichen-Mustern. Keine Bandinformation möglich. Geeignet als Fallback oder für nicht direkt unterstützte Logprogramme.

### Netzwerk-Listener für QSO-UDP-Broadcast

**Empfohlene Methode.** KST4Contest hört auf UDP-Pakete, die das Logprogramm beim Speichern eines QSOs an die Broadcast-Adresse sendet. Die Stationen werden mit Bandinformation markiert. UDP-Port: Standard **12060**. (Wird z. B. von UCXLog, N1MM+, QARTest, DXLog.net genutzt).

### Win-Test Network-Listener (Zusätzlicher UDP-Listener)

Dedizierter Netzwerk-Erkenner für Win-Test. KST4Contest empfängt und verarbeitet Win-Test-spezifische UDP-Pakete (inkl. Sked-Übergabe) auf dem dafür konfigurierten Port.

---

## TRX-Sync-Einstellungen

Empfängt die aktuelle Frequenz des Transceivers vom Logprogramm via UDP. Ermöglicht die automatische Befüllung der Variable `MYQRG`. Nützlich für:

- Schnelles Einfügen der eigenen QRG in Chat-Nachrichten.
- Automatische CQ-Baken mit aktueller Frequenz.

> **Hinweis für Multi-Setup**: Wenn zwei Logprogramme an zwei Computern betrieben werden, aber nur eine KST4Contest-Instanz, darf nur ein Logprogramm die Frequenzpakete senden. KST4Contest kann nicht zwischen den Quellen unterscheiden.

---

## AirScout-Einstellungen

Konfiguration der Schnittstelle zu AirScout für die Flugzeug-Scatter-Erkennung. Details: [AirScout-Integration](de-AirScout-Integration).

---

## Notification Settings (Benachrichtigungen)

Drei Benachrichtigungstypen stehen zur Wahl:

1. **Einfache Sounds**: TADA-Sound für eingehende Nachrichten, Tick für Sked-Richtungserkennung usw.
2. **CW-Ansage**: Das Rufzeichen einer Station, die eine Privatnachricht sendet, wird als CW-Signal ausgegeben.
3. **Phonetische Ansage**: Das Rufzeichen wird phonetisch ausgesprochen.

---

## Shortcut Settings (Schnellzugriff-Schaltflächen)

Konfiguration von Schnellzugriff-Schaltflächen, die direkt im Hauptfenster erscheinen. Ein Klick auf eine Schaltfläche fügt den konfigurierten Text in das Sendfeld ein. Alle [Variablen](Makros-und-Variablen#variablen) können verwendet werden.

---

## Snippet Settings (Text-Snippets)

Text-Snippets sind über folgende Wege abrufbar:

- **Rechtsklick** auf ein Rufzeichen in der Benutzerliste
- **Rechtsklick** in der CQ-Nachrichtentabelle
- **Rechtsklick** in der PM-Nachrichtentabelle
- **Tastenkombinationen**: `Ctrl+1` bis `Ctrl+0` für die ersten 10 Snippets

Wenn in der Benutzerliste ein Rufzeichen ausgewählt ist, wird der Snippet als Direktnachricht adressiert:
`/CQ RUFZEICHEN <Snippet-Text>`

---

## Beacon Settings (Automatischer Beacon)

Konfiguration eines automatischen Intervall-Beacons im öffentlichen Chat-Kanal. Empfohlen: Variable `MYQRG` im Text verwenden, damit die aktuelle Frequenz immer aktuell ist. Intervall und Text sind frei konfigurierbar.

> **Tipp**: Beacon beim CQ-Rufen aktivieren und im Einstellungsfenster schnell deaktivieren, wenn kein CQ gerufen wird.

---

## Messagehandling Settings (ab v1.25)

Neuer Einstellungsbereich mit folgenden Optionen:

- **Auto-Antwort auf alle eingehenden Nachrichten**: Automatische Antwort auf Privatnachrichten konfigurierbar.
- **Auto-Antwort mit eigener CQ-QRG**: Wenn jemand nach der eigenen QRG fragt, antwortet KST4Contest automatisch mit dem Inhalt der `MYQRG`-Variable.
- **Standard-Filter für das Userinfo-Fenster**: Voreingestellter Nachrichtenfilter für das Stationsinfo-Fenster konfigurierbar *(für Gianluca :-) )*.

---

## Worked Station Database Settings (Gearbeitete-Stationen-Datenbank)

Vor jedem Contest die interne Worked-Datenbank zurücksetzen! Enthält:

- Worked-Status aller Stationen (pro Band)
- NOT-QRV-Tags (seit v1.2)

Schaltfläche **„Reinitialize"** unter der Tabelle verwenden. Eine geplante Funktion ist eine automatische Ablaufzeit für den Worked-Status.

---

## Dark Mode (ab v1.26)

Umschaltbar über das Menü: **Window → Use Dark Mode**. Die Farben können über CSS individuell angepasst werden.

---

## Einstellungen speichern

Nach **jeder** Änderung **„Save Settings"** klicken! Ohne Speichern gehen alle Änderungen beim nächsten Start verloren.

- Speicherort: unter Linux `~/.praktikst/preferences.xml` und unter Windows `%USERPROFILE%\.praktikst\preferences.xml` (bzw. `C:\Users\<Benutzername>\.praktikst\preferences.xml`)
- Ab v1.21: Fenstergrößen und Divider-Positionen werden ebenfalls gespeichert.
- Bei Problemen: Konfigurationsdatei löschen → KST4Contest erstellt eine neue mit Standardwerten.
