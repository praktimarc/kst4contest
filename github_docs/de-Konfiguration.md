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

## Server-Einstellungen (ab v1.31)

Der Chat-Server-DNS und -Port sind in den Preferences konfigurierbar:

- **Server-DNS**: Standard `www.on4kst.org` (ab v1.31 geändert von `www.on4kst.info`).
- **Port**: Standardport des ON4KST-Servers.

Eine Änderung ist nur notwendig, wenn der Server umzieht oder ein alternativer Endpunkt genutzt wird.

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

![Benachrichtigungen, DX-Cluster-Ausgabe und QSO-Monitoring](client_settings_window_notification.png)

Im Reiter **Notification** werden nicht nur akustische Hinweise konfiguriert. Hier befinden sich auch die Einstellungen für den lokalen DX-Cluster-Server, den Band-Upgrade-Hinweis und das QSO-Monitoring.

### Akustische Hinweise

Die drei Audiofunktionen arbeiten unabhängig voneinander:

- **Play notification sounds …** aktiviert kurze Hinweistöne für neue Privatnachrichten, erkannte Richtungsgelegenheiten, Sked-Erinnerungen und Band-Upgrade-Hinweise.
- **Spell the sender's callsign in CW …** gibt das Rufzeichen des Absenders einer neuen Privatnachricht als CW-Signal aus.
- **Speak the sender's callsign phonetically …** spricht das Rufzeichen des Absenders phonetisch aus.

CW- und Sprachausgabe können gleichzeitig aktiviert werden. Das ist technisch möglich, im Contest aber nicht zwingend hilfreich. In der Praxis sollte nur die Ausgabe eingeschaltet werden, die im eigenen Stationsbetrieb tatsächlich wahrgenommen werden kann, ohne den Operator dauerhaft zu beschäftigen.

### Local DX Cluster output

KST4Contest kann erkannte Richtungsgelegenheiten als DX-Cluster-Spots an ein Logprogramm weitergeben. Der praktische Nutzen liegt auf der Hand: Eine im Chat erkannte Frequenz erscheint direkt in der Bandmap des Logprogramms und muss nicht erst von Hand übertragen werden.

Die Checkbox **Enable the local DX Cluster server …** startet beziehungsweise beendet den lokalen TCP-Server. Bei einer laufenden Chat-Verbindung wird die Änderung sofort wirksam.

Folgende Einstellungen sind erforderlich:

- **TCP port**: Port, auf dem KST4Contest Verbindungen von DX-Cluster-Clients annimmt. Der Standardwert ist `8000`. Wird der Port während einer laufenden Verbindung geändert, startet KST4Contest den Server auf dem neuen Port neu. Der Logger muss sich anschließend ebenfalls mit dem neuen Port verbinden.
- **Fallback band in MHz**: Bandpräfix für relative Frequenzangaben. Aus `205` oder `.205` wird bei einem Fallback-Band von `144` die Frequenz `144.205 MHz`. Vollständige Angaben wie `432.205` oder `1296.338` benötigen diesen Fallback nicht.
- **Spotter callsign**: Rufzeichen, das im erzeugten DX-Cluster-Spot als Spotter erscheint. Hier sollte ein anderes Rufzeichen als das im Contest verwendete Stationsrufzeichen eingetragen werden. Einige Logprogramme filtern Spots des eigenen Rufzeichens oder behandeln sie anders als fremde Spots.
- **Send test spot**: Sendet einen Testspot für `DL0TEST` auf `.300` des eingestellten Fallback-Bandes. Der Test funktioniert nur, wenn KST4Contest mit dem Chat verbunden, der lokale DX-Cluster-Server aktiviert und mindestens ein DX-Cluster-Client verbunden ist.

KST4Contest erzeugt nicht bei jeder im Chat gefundenen Frequenz automatisch einen Spot. Ein Spot entsteht nur dann, wenn eine gerichtete Nachricht zwischen zwei Stationen auf eine für die eigene Station interessante Antennenrichtung schließen lässt und für den Absender eine nutzbare Frequenz bekannt ist.

Die vollständige Herleitung und die Einrichtung des Logprogramms sind im Kapitel [Integrierter DX-Cluster-Server](de-DX-Cluster-Server) beschrieben.

### Band-Upgrade-Hinweis nach einem Logeintrag

Nach einem über UCXLog oder Win-Test empfangenen Logeintrag kann KST4Contest prüfen, ob die gerade gearbeitete Station auf einem weiteren gemeinsamen, aber noch nicht gearbeiteten Band aktiv ist.

Dafür werden drei Informationen miteinander verglichen:

1. die in den Stationseinstellungen aktivierten eigenen Bänder,
2. die innerhalb der letzten 30 Minuten erkannten Bänder der Gegenstation,
3. die bereits pro Band gespeicherten Worked-Markierungen.

Bleibt danach mindestens ein gemeinsames, noch nicht gearbeitetes Band übrig, erscheint im Hauptfenster ein blinkender **BAND+**-Hinweis. Ist die allgemeine Soundausgabe aktiviert, wird zusätzlich ein Hinweiston abgespielt.

Die beiden Optionen haben unterschiedliche Aufgaben:

- **Blink + sound …** aktiviert den eigentlichen Band-Upgrade-Hinweis.
- **Priority boost …** erhöht zusätzlich die Priorität entsprechender Stationen, damit sie in den Kandidatenlisten besser sichtbar bleiben. Der Boost garantiert keinen bestimmten Listenplatz; er ist nur ein zusätzlicher Faktor innerhalb der gesamten Prioritätsberechnung.

Der Hinweis setzt eine Log-Synchronisation mit Bandinformation voraus. Der einfache dateibasierte Callsign-Interpreter kann nur Rufzeichen erkennen und liefert deshalb keine ausreichende Grundlage für diese Prüfung.

### Sniffer-Einstellungen (ab v1.31)

Das QSO-Monitoring ist für Stationen gedacht, deren Kommunikation man gezielt verfolgen möchte. Das kann beispielsweise eine seltene Station, eine DXpedition oder eine andere Station des eigenen Contest-Teams sein.

Für jedes eingetragene Rufzeichen zeigt KST4Contest Nachrichten zusätzlich in der PM-Tabelle an, wenn das Rufzeichen entweder Absender oder Empfänger der Nachricht ist. Die ursprüngliche Nachricht wird dabei nicht aus ihrer normalen Tabelle entfernt.

Überwachte Nachrichten werden in der PM-Tabelle eindeutig gekennzeichnet:

```text
Sniffed: (SENDER > RECEIVER) Nachrichtentext
```

So wird sichtbar, dass die Nachricht nicht an die eigene Station gerichtet war.

Rufzeichen werden folgendermaßen verwaltet:

1. Mit **Add monitored callsign** ein neues Rufzeichen hinzufügen.
2. Ein vorhandenes Rufzeichen per Doppelklick bearbeiten und die Änderung mit `Enter` übernehmen.
3. Zum Entfernen den Inhalt einer Tabellenzelle löschen und mit `Enter` bestätigen.

Doppelte oder syntaktisch ungültige Rufzeichen werden nicht übernommen. Die Liste wird mit **Save Settings** in der `preferences.xml` gespeichert und beim nächsten Programmstart wiederhergestellt.

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

![Beacon-Einstellungen](client_settings_window_beacon.png)

Ein Beacon sendet in regelmäßigen Abständen eine öffentliche CQ-Nachricht. Er ist für Betriebssituationen gedacht, in denen die eigene Station über längere Zeit auf einer festen Frequenz ruft. Andere Stationen erhalten dadurch eine aktuelle QRG-Information, ohne dass der Operator denselben Text wiederholt von Hand in den Chat schreiben muss.

KST4Contest verwendet einen gemeinsamen Timer für beide Chat-Kategorien. Aktivierung und Nachrichtentext werden trotzdem getrennt konfiguriert:

- **Enable CQ beacon** aktiviert den Beacon der betreffenden Kategorie.
- **Beacon message** enthält den öffentlichen Nachrichtentext dieser Kategorie.
- **Shared beacon interval** legt das gemeinsame Intervall für beide Kategorien fest.

Sind beide Beacons aktiviert, werden sie beim selben Timer-Lauf nacheinander in ihren jeweiligen Kategorien gesendet. Der zweite Beacon wird nur berücksichtigt, wenn auch der zweite Chat aktiviert und verbunden ist.

### Intervall und Timer-Verhalten

Das Intervall wird in ganzen Minuten angegeben. Der kleinste zulässige Wert ist eine Minute.

Nach dem Aufbau der Chat-Verbindung prüft KST4Contest die Beacons erstmals nach ungefähr zehn Sekunden. Anschließend gilt das eingestellte Intervall. Wird der Wert während einer laufenden Verbindung geändert, beginnt der Countdown mit dem neuen Intervall erneut. Die Änderung selbst löst keine sofortige Nachricht aus.

### Nachrichtentext und Variablen

Ein Beacon darf nach der Variablenauflösung höchstens 120 Zeichen enthalten. KST4Contest prüft deshalb nicht nur das eingetragene Template, sondern den tatsächlich zu sendenden Text.

Im Beacon können alle [globalen Variablen](de-Makros-und-Variablen#variablen-im-beacon) verwendet werden, beispielsweise:

```text
calling cq at MYQRG, ant MYQTF deg, loc MYLOCATOR
```

Die Variablen werden bei jedem Timer-Lauf neu aufgelöst. Ändert die Logsoftware zwischenzeitlich die in `MYQRG` gespeicherte Frequenz, verwendet bereits der nächste Beacon den neuen Wert.

Stationsbezogene Variablen wie `QRZNAME`, `FIRSTAP` oder `SECONDAP` benötigen dagegen eine ausgewählte Gegenstation. Da ein öffentlicher Beacon keine Gegenstation adressiert, werden diese Variablen im Beacon nicht aufgelöst.

### Wann sollte der Beacon ausgeschaltet werden?

Der Beacon ist nur dann hilfreich, wenn seine QRG-Angabe zum tatsächlichen Betrieb passt. Bleibt er beim Absuchen oder häufigen Wechseln von Frequenzen aktiviert, können andere Stationen auf einer inzwischen falschen Frequenz nach der eigenen Station suchen.

Im Klartext: Solange auf einer festen QRG CQ gerufen wird, spart der Beacon Arbeit. Beim „Schleichen“ über das Band sollte er ausgeschaltet werden.

Änderungen wirken während der laufenden Verbindung. Damit Aktivierung, Texte und Intervall auch nach dem nächsten Programmstart erhalten bleiben, anschließend **Save Settings** verwenden.
---

## Messagehandling Settings (ab v1.25)

Neuer Einstellungsbereich mit folgenden Optionen:

- **Auto-Antwort auf alle eingehenden Nachrichten**: Automatische Antwort auf Privatnachrichten konfigurierbar.
- **Auto-Antwort mit eigener CQ-QRG**: Wenn jemand nach der eigenen QRG fragt, antwortet KST4Contest automatisch mit dem Inhalt der `MYQRG`-Variable.
- **Standard-Filter für das Userinfo-Fenster**: Voreingestellter Nachrichtenfilter für das Stationsinfo-Fenster konfigurierbar *(für Gianluca :-) )*.

---

## Win-Test-Netzwerk-Listener (ab v1.31)

Dedizierter Empfänger für Win-Test-spezifische UDP-Pakete. Ermöglicht:

- **Log-Synchronisation**: Gearbeitete Stationen werden aus Win-Test übernommen und in der Benutzerliste markiert.
- **Frequenz-Auswertung**: Die aktuelle TRX-Frequenz aus Win-Test befüllt die `MYQRG`-Variable.
- **Sked-Übergabe (SKED Push)**: Skeds aus KST4Contest werden via UDP direkt an Win-Test übergeben. Der UDP-Broadcast-Standardport von Win-Test (9871) wird verwendet.

Einstellungen:
- **Aktivieren/Deaktivieren**: Checkbox in den Preferences (ab v1.40).
- **Port**: Konfigurierbarer UDP-Port für den Win-Test-Listener.
- **Sked-UDP-Adresse und Port**: Zieladresse und Port für die SKED-Übergabe an Win-Test.

> **Hinweis**: Der Win-Test-Listener ist ein **zusätzlicher** Listener – der Standard-QSO-UDP-Broadcast-Listener auf Port 12060 bleibt davon unabhängig.

---

## PSTRotator-Einstellungen (ab v1.31)

KST4Contest kann die Antennenrichtung über PSTRotator steuern.

Einstellungen:
- **Aktivieren/Deaktivieren**: Checkbox in den Preferences (ab v1.40).
- **IP-Adresse**: IP-Adresse des PSTRotator-Rechners (Standard: `127.0.0.1` bei Betrieb auf demselben PC).
- **Port**: Kommunikationsport von PSTRotator.

> **Hinweis**: Nach einem Klick auf den Richtungs-Button wartet KST4Contest kurz auf die Rotatorantwort. Bei langsamen Rotoren (z. B. SPID) kann es zu einer kleinen Verzögerung kommen.

---

## Worked Station Database Settings (Gearbeitete-Stationen-Datenbank)

Die interne Worked-Datenbank enthält:

- Worked-Status aller Stationen (pro Band)
- NOT-QRV-Tags (seit v1.2)

**Ab v1.40**: Einträge haben eine automatische Lebensdauer von **3 Tagen** – ein manuelles Zurücksetzen vor jedem Contest ist nicht mehr zwingend notwendig. Für ein vollständiges Reset kann trotzdem die Schaltfläche **„Reinitialize"** verwendet werden.

---

## Dark Mode (ab v1.26)

Umschaltbar über das Menü: **Window → Use Dark Mode**. Die Farben können über CSS individuell angepasst werden.

---

## Einstellungen speichern

Nach **jeder** Änderung **„Save Settings"** klicken! Ohne Speichern gehen alle Änderungen beim nächsten Start verloren.

- Speicherort: unter Linux `~/.praktikst/preferences.xml` und unter Windows `%USERPROFILE%\.praktikst\preferences.xml` (bzw. `C:\Users\<Benutzername>\.praktikst\preferences.xml`)
- Ab v1.21: Fenstergrößen und Divider-Positionen werden ebenfalls gespeichert.
- Bei Problemen: Konfigurationsdatei löschen → KST4Contest erstellt eine neue mit Standardwerten.
