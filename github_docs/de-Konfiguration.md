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

Über die Checkboxen **My station uses …** wird festgelegt, auf welchen Bändern die eigene Station im aktuellen Setup arbeiten kann. Unterstützt werden 50 MHz, 70 MHz, 144 MHz, 432 MHz, 1296 MHz, 2320 MHz, 3400 MHz, 5760 MHz und 10 GHz.

Die Auswahl steuert nicht nur die sichtbaren Bandspalten. Sie wird außerdem verwendet für:

- die bandbezogenen Worked- und NOT-QRV-Filter,
- die im **Further Info**-Bereich sichtbaren NOT-QRV-Schalter,
- die Herleitung von `a`- und `B+`-Bandmöglichkeiten,
- den Filter **New bands**,
- den Band-Upgrade-Hinweis nach einem Logeintrag und
- bandbezogene Prioritäts- und Reachability-Funktionen.

Nach einer Änderung **Save Settings** verwenden und KST4Contest neu starten. Die Bandspalten und mehrere zugehörige Bedienelemente werden beim Aufbau der Benutzeroberfläche erzeugt und deshalb nicht vollständig in der laufenden Sitzung ergänzt oder entfernt.

### Antennen-Öffnungswinkel (Antenna Beamwidth)

Trage den vollständigen horizontalen Öffnungswinkel der eigenen Antenne in Grad ein. KST4Contest verwendet jeweils die Hälfte dieses Werts links und rechts der gewählten beziehungsweise hergeleiteten Antennenrichtung. Ein eingetragener Wert von `70°` entspricht daher einem Korridor von `±35°`.

Der Wert wird an mehreren Stellen verwendet:

- für den QTF-Filter der Benutzerliste,
- für die Darstellung des eigenen Antennenkorridors,
- als angenommener Öffnungswinkel einer fremden Station bei der [Herleitung von Richtungsgelegenheiten](de-Funktionen#richtungsgelegenheiten-aus-gerichteten-nachrichten).

Der letzte Punkt ist bewusst eine Näherung. ON4KST überträgt weder die verwendete Antenne noch deren Öffnungswinkel. KST4Contest verwendet deshalb den eigenen Wert als praktikable Annahme für die Gegenstation.

Wähle einen realistischen Wert. Ein zu großer Öffnungswinkel erzeugt viele geometrische Treffer, die praktisch kaum noch eine Aussage haben. Ein zu kleiner Wert kann dagegen brauchbare Richtungsgelegenheiten ausblenden.

### Standard-Maximum-QRB

Trage die maximale Entfernung in Kilometern ein, innerhalb der KST4Contest Richtungsgelegenheiten berücksichtigen soll. Maßgeblich ist die Entfernung zwischen der eigenen Station und dem Absender der gerichteten Nachricht – nicht die Entfernung zwischen Absender und Empfänger.

Liegt der Absender weiter entfernt, wird die Situation auch dann nicht hervorgehoben und nicht als Richtungsgelegenheit an den lokalen DX-Cluster-Server weitergegeben, wenn der berechnete Winkel passen würde.

Der Wert sollte zum eigenen Stationsaufbau und zum vorgesehenen Contestbetrieb passen. Ein unnötig großer Bereich erzeugt Hinweise für Stationen, die praktisch nicht mehr zum Arbeitsbereich gehören; ein zu kleiner Bereich blendet mögliche Kandidaten bereits vor der Richtungsbewertung aus.

---

### Streckenanalyse und Link-Budget

Die Streckenanalyse der Stationskarte verwendet einige Angaben aus der Stationskonfiguration. Diese Werte beschreiben das eigene Stationssetup und – soweit keine individuellen Daten der Gegenstation vorliegen – ein angenommenes Setup der Gegenstation.

Folgende Einstellungen werden berücksichtigt:

- **Own antenna height AGL [m]** gibt die Höhe der eigenen Antenne über dem lokalen Gelände an. Die Angabe bezieht sich auf *Above Ground Level* und nicht auf die Höhe über dem Meeresspiegel. KST4Contest addiert diesen Wert zur Geländehöhe am eigenen Standort.
- **Own TX power [W]** gibt die verwendete Sendeleistung der eigenen Station in Watt an.
- **Own ant. gain [dBi]** gibt den Antennengewinn der eigenen Station in dBi an.
- **DX OM TX power [W]** gibt die für die Gegenstation angenommene Sendeleistung in Watt an.
- **DX OM ant. gain [dBi]** gibt den für die Gegenstation angenommenen Antennengewinn in dBi an.

Für die Antennenhöhe der Gegenstation verwendet KST4Contest derzeit einen festen Wert von 10 m über dem lokalen Gelände. Die Leistungs- und Antennendaten der Gegenstation sind globale Annahmen. Sie ersetzen keine individuell bekannten Stationsdaten, ermöglichen aber eine einheitliche Abschätzung, wenn keine genaueren Informationen vorliegen.

Die Antennengewinne müssen in dBi angegeben werden. Falls ein Wert in dBd vorliegt, kann er näherungsweise wie folgt umgerechnet werden:

`dBi = dBd + 2.15`

Auch die aktuelle Antennenrichtung, die konfigurierte Strahlbreite, das maximale QRB und die für die eigene Station aktivierten Bänder beeinflussen die Darstellung oder Auswertung auf der Stationskarte. Die aktuelle QTF und die Strahlbreite bestimmen beispielsweise den eingezeichneten Antennensektor und die Hervorhebung von Stationen innerhalb dieses Bereichs.

Für die Berücksichtigung der Erdkrümmung verwendet die Streckenanalyse einen festen Faktor von `k = 4/3` für den effektiven Erdradius. Das ist eine übliche Näherung für eine durchschnittliche troposphärische Refraktion. Tatsächliche Ausbreitungsbedingungen können davon deutlich abweichen.

Das Link-Budget berücksichtigt unter anderem:

- die Entfernung zwischen beiden Stationen,
- die verwendete Frequenz,
- die konfigurierte Sendeleistung,
- die Antennengewinne,
- geschätzte Speiseleitungsverluste,
- den Freiraumverlust sowie
- eine grobe Zusatzdämpfung durch Hindernisse im Streckenprofil.

Die daraus berechnete Empfangsleistung und SSB- beziehungsweise CW-Marge sind technische Abschätzungen. Sie sollen dabei helfen, mögliche Verbindungen einzuordnen. Sie sind keine vollständige Feldstärkeprognose und können insbesondere aktuelle Wetterbedingungen, lokale Abschattungen, Mehrwegeausbreitung oder andere nicht bekannte Stationsparameter nicht vollständig berücksichtigen.

---

### Streckenanalyse und Link-Budget

Die Stationskarte verwendet mehrere Werte aus dem Reiter **Station**, um das Geländeprofil und das Link-Budget zur ausgewählten Gegenstation zu berechnen.

| Einstellung | Verwendung |
|---|---|
| **Own antenna height AGL** | Höhe der eigenen Antenne über dem lokalen Gelände in Metern |
| **Own TX power W** | Eigene Sendeleistung in Watt |
| **Own ant. gain dBi** | Gewinn der eigenen Antenne in dBi |
| **DX OM TX power W** | Angenommene Sendeleistung der Gegenstation in Watt |
| **DX OM ant. gain dBi** | Angenommener Antennengewinn der Gegenstation in dBi |

**AGL** bedeutet „above ground level“. Trage hier nicht die Höhe über dem Meeresspiegel ein. Die Geländehöhe am eigenen Standort stammt bereits aus dem abgerufenen Höhenprofil; die konfigurierte Antennenhöhe wird zu diesem Wert addiert.

Für die Antennenhöhe der Gegenstation verwendet KST4Contest derzeit einen festen Standardwert von 10 Metern über Grund. Eine stationsbezogene Antennenhöhe wird im ON4KST-Chat nicht übertragen.

Antennengewinne müssen in `dBi` eingetragen werden. Liegt ein Wert in `dBd` vor, gilt:

```text
dBi = dBd + 2,15 dB



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

### Fallback-Band für relative QRG-Erkennung

Das Dropdown **Fallback band for relative QRG detection** legt fest, welches Band KST4Contest verwendet, wenn eine relative QRG keinem aktuellen Stationskontext zugeordnet werden kann.

Zur Auswahl stehen ausschließlich die vom Frequenzparser unterstützten Bandpräfixe:

```text
50 MHz
70 MHz
144 MHz
432 MHz
1296 MHz
2320 MHz
3400 MHz
5760 MHz
10368 MHz (10G)
24048 MHz (24G)
```

Das Dropdown ist kein Filter und keine Vorgabe für vollständig angegebene Frequenzen. `432.088` wird unabhängig von der Auswahl als Frequenz im 432-MHz-Band erkannt. Benötigt wird das Fallback bei relativen Angaben wie `.205`, `,205` oder `qrg 205`.

Bevor KST4Contest auf das Fallback zurückgreift, prüft es den Bandkontext des Absenders. Wurde für dieselbe Station innerhalb der letzten 30 Minuten bereits eine passende vollständige Frequenz erkannt, hat dieses Band Vorrang. Ein Fallback von `144 MHz` macht aus `.100` daher `432.100 MHz`, wenn die Station kurz zuvor beispielsweise `432.088` genannt hat.

Die Einstellung befindet sich im Notification-Bereich, wirkt aber auf die gesamte QRG-Erkennung. Damit beeinflusst sie nicht nur mögliche DX-Cluster-Spots, sondern auch die QRG-Spalte, erkannte aktive Bänder, Priorisierung, Band-Upgrade-Hinweise und Funktionen, die eine bekannte Stationsfrequenz verwenden.

Mehr zur Erkennungslogik und zu absichtlich ignorierten Zahlen: [QRG-Erkennung](de-Funktionen#qrg-erkennung).

### Local DX Cluster output

KST4Contest kann erkannte Richtungsgelegenheiten als DX-Cluster-Spots an ein Logprogramm weitergeben. Eine im Chat erkannte Frequenz erscheint dadurch direkt in der Bandmap des Logprogramms und muss nicht erst von Hand übertragen werden.

Die Checkbox **Enable the local DX Cluster server …** startet beziehungsweise beendet den lokalen TCP-Server. Bei einer laufenden Chat-Verbindung wird die Änderung sofort wirksam.

Folgende Einstellungen und Schaltflächen gehören zur lokalen DX-Cluster-Ausgabe:

- **TCP port**: Port, auf dem KST4Contest Verbindungen von DX-Cluster-Clients annimmt. Der Standardwert ist `8000`. Wird der Port während einer laufenden Verbindung geändert, startet KST4Contest den Server auf dem neuen Port neu. Der Logger muss sich anschließend ebenfalls mit dem neuen Port verbinden.
- **Fallback band for relative QRG detection**: Das oben beschriebene globale Fallback-Band. Der Testspot verwendet `.300` dieses Bandes. Reale Spots verwenden dagegen die für den jeweiligen Absender erkannte QRG.
- **Spotter callsign**: Rufzeichen, das im erzeugten DX-Cluster-Spot als Spotter erscheint. Hier sollte ein anderes Rufzeichen als das im Contest verwendete Stationsrufzeichen eingetragen werden. Einige Logprogramme filtern Spots des eigenen Rufzeichens oder behandeln sie anders als fremde Spots.
- **Send test spot**: Sendet einen Testspot für `DL0TEST` auf `.300` des ausgewählten Fallback-Bandes. Der Test funktioniert nur, wenn KST4Contest mit dem Chat verbunden, der lokale DX-Cluster-Server aktiviert und mindestens ein DX-Cluster-Client verbunden ist.

KST4Contest erzeugt nicht bei jeder im Chat gefundenen Frequenz automatisch einen Spot. Ein Spot entsteht nur dann, wenn eine gerichtete Nachricht zwischen zwei Stationen auf eine für die eigene Station interessante Antennenrichtung schließen lässt und für den Absender eine nutzbare Frequenz bekannt ist.

Die vollständige Herleitung und die Einrichtung des Logprogramms sind im Kapitel [Integrierter DX-Cluster-Server](de-DX-Cluster-Server) beschrieben.
### Band-Upgrade-Hinweis nach einem Logeintrag

Nach einem über UCXLog oder Win-Test empfangenen Logeintrag kann KST4Contest prüfen, ob die gerade gearbeitete Station noch ein weiteres gemeinsames, aber bisher nicht gearbeitetes Band anbietet.

Die Prüfung verwendet dieselbe Bandherleitung wie die `a`- und `B+`-Anzeige:

1. die in den Stationseinstellungen aktivierten eigenen Bänder,
2. höchstens 30 Minuten alte QRG-Erkennungen der Gegenstation,
3. eindeutige Bandangaben im Namensfeld ihrer aktiven Chat-Einträge,
4. die pro Band gespeicherten Worked-Markierungen und
5. manuell gesetzte NOT-QRV-Tags.

Aktive Chat-Varianten desselben normalisierten Rufzeichens werden gemeinsam ausgewertet. NOT-QRV hat Vorrang vor einer automatisch erkannten QRG oder Bandangabe.

Bleibt mindestens ein gemeinsames, noch nicht gearbeitetes Band übrig, erscheint im Hauptfenster für ungefähr zwölf Sekunden ein blinkender **BAND+**-Hinweis mit Rufzeichen und den noch offenen Bändern. Der Tooltip zeigt die vollständige Herleitung. Ist die allgemeine Soundausgabe aktiviert, wird zusätzlich ein kurzer Hinweiston abgespielt.

Die beiden Optionen haben unterschiedliche Aufgaben:

- **Blink + sound …** aktiviert den Hinweis nach einem passenden Logeintrag.
- **Priority boost …** erhöht zusätzlich den Score von Stationen, die bereits auf mindestens einem Band gearbeitet wurden, aber noch ein weiteres gemeinsames und nicht gearbeitetes Band anbieten.

Der Priority Boost ist nur ein Faktor innerhalb der gesamten Berechnung. Entfernung, Antennenrichtung, aktuelle Aktivität, AirScout-Daten, Skeds und negative Hinweise können den endgültigen Listenplatz weiterhin verändern. Die aktivierte Option garantiert deshalb weder einen bestimmten Score noch einen bestimmten Platz in der Prioritätsliste.

Die übrigen Score-Gewichte besitzen derzeit keine eigenen Bedienelemente. Mehrere vorhandene Einstellungen liefern jedoch Eingangsdaten für die Berechnung, insbesondere die [aktivierten Bänder](#aktivierte-bänder), der [Antennen-Öffnungswinkel](#antennen-öffnungswinkel-antenna-beamwidth), der [Standard-Maximum-QRB](#standard-maximum-qrb) und die [AirScout-Einstellungen](#airscout-einstellungen).

Die vollständige Herleitung ist unter [Prioritätsscore und Prioritätsliste](de-Funktionen#prioritätsscore-und-prioritätsliste-ab-v140) beschrieben.

Der Hinweis setzt eine Log-Synchronisation mit Bandinformation voraus. Der einfache dateibasierte Callsign-Interpreter erkennt lediglich Rufzeichen und liefert deshalb keine sichere Information über das Band des gerade geloggten QSOs.

Weitere Hintergründe: [Band-Upgrade-Hinweis nach einem Logeintrag](de-Funktionen#band-upgrade-hinweis-nach-einem-logeintrag).

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

![Automatische Antworten](client_settings_window_messagehandling.png)

Die wichtigste Anwendung der allgemeinen automatischen Antwort betrifft Stationen, die zwar im ON4KST-Chat eingeloggt sind, den laufenden Contest aber nicht mitfunken. Gerade während größerer Contests werden Sked-Anfragen teilweise unkoordiniert und in großer Zahl an eingeloggte Stationen verteilt, ohne vorher zu prüfen, ob sie überhaupt teilnehmen. Die Empfänger müssten sonst immer wieder dieselbe Absage schreiben.

KST4Contest kann darauf mit einem vorher festgelegten Text reagieren. Davon getrennt steht eine gezielte QRG-Auskunft zur Verfügung. Beide Funktionen können unabhängig voneinander aktiviert werden.

### Allgemeine automatische Antwort

**Enable automatic reply to all private messages** beantwortet eingehende Privatnachrichten mit dem Text im Feld rechts daneben. Es gibt einen gemeinsamen Text für beide Chat-Kategorien. Die beim Eingeben verwendete Groß- und Kleinschreibung bleibt erhalten.

Eine zweckmäßige Nachricht ist beispielsweise:

```text
Sri, I am not taking part in this contest. No skeds.
```

Die eingegangene Privatnachricht bleibt sichtbar. Die Funktion blockiert oder verwirft keine Anfrage, sondern erspart lediglich die wiederholte manuelle Antwort.

Die Antwort wird in derselben Chat-Kategorie gesendet, in der die Privatnachricht eingegangen ist. Das ist bei einem parallelen Login in zwei Kategorien entscheidend: Eine Nachricht aus dem Microwave-Chat darf nicht versehentlich im VHF/UHF-Chat beantwortet werden.

### Automatische QRG-Antwort

**Enable automatic QRG replies** reagiert auf typische QRG-Anfragen. Die Erkennung unterscheidet nicht zwischen Groß- und Kleinschreibung und sucht nach folgenden Textbestandteilen:

```text
ur qrg?
your qrg?
qrg?
freq?
pse qrg
```

Die Antwort enthält nur die QRG der Kategorie, in der die Anfrage eingegangen ist:

| Eingegangene Privatnachricht | Verwendete QRG |
|---|---|
| Hauptkategorie | aktuelle QRG der Hauptkategorie |
| zweite Chat-Kategorie | aktuelle QRG der zweiten Kategorie |

Die Werte stammen aus denselben QRG-Feldern, die auch von `MYQRG` und `SECONDQRG` verwendet werden. Die Haupt-QRG kann manuell eingetragen oder durch die [TRX-Synchronisation](#trx-sync-einstellungen) aktualisiert werden. Für die zweite Kategorie wird der dort konfigurierte beziehungsweise manuell eingetragene Wert verwendet.

Sind die allgemeine und die QRG-bezogene Antwort gleichzeitig aktiviert, hat die QRG-Antwort Vorrang. Eine erkannte QRG-Anfrage erzeugt daher nicht zusätzlich den allgemeinen Antworttext.

### Schutz vor wiederholten Antworten

Jede automatisch erzeugte Nachricht trägt das feste Präfix:

```text
[KST4C Automsg]
```

Die allgemeine und die QRG-bezogene Antwort reagieren nicht auf Nachrichten, die dieses Präfix bereits enthalten. Dadurch beantworten sich zwei entsprechend arbeitende Clients nicht gegenseitig in einer Schleife.

Zusätzlich gilt eine gemeinsame Sperrzeit von zwei Minuten für beide Antwortarten. Die Sperre wird getrennt je Rufzeichen und Chat-Kategorie geführt. Hat eine Station gerade in der Hauptkategorie eine automatische Antwort erhalten, kann sie deshalb weiterhin eine Antwort in der zweiten Kategorie erhalten. Weitere Nachrichten derselben Station in derselben Kategorie lösen während der folgenden zwei Minuten dagegen keine neue automatische Antwort aus.

Die Sperrzeit beginnt nur, wenn KST4Contest tatsächlich eine Antwort sendet.

> **Hinweis**: Der Antworttext sollte den tatsächlichen Status eindeutig benennen. Wer den Contest nur beobachtet und keine Skeds fahren möchte, sollte genau das mitteilen. Eine vage Nachricht erzeugt im Zweifel nur die nächste Rückfrage – und damit exakt die Arbeit, welche die Funktion vermeiden soll.

Änderungen wirken während der laufenden Verbindung. Damit Aktivierung und Text nach dem nächsten Programmstart erhalten bleiben, anschließend **Save Settings** verwenden.

Weitere Hintergründe: [Automatische Antworten auf Privatnachrichten](de-Funktionen#automatische-antworten-auf-privatnachrichten-ab-v125).

---

## Win-Test-Netzwerk-Listener (ab v1.31)

Der Win-Test-Netzwerk-Listener verarbeitet das native Win-Test-UDP-Protokoll. Er ist vom allgemeinen QSO-UDP-Listener auf Port `12060` unabhängig und übernimmt drei Aufgaben:

- QSOs einschließlich Band- und Locatorinformation auswerten,
- STATUS-Pakete für die eigene QRG verarbeiten und
- Skeds an das Win-Test-Netzwerk übergeben.

### Einstellungen unter Log sync

| Einstellung | Funktion |
|---|---|
| **Receive Win-Test network based UDP log messages** | Aktiviert den Win-Test-Netzwerk-Listener. Bei aktiviertem Listener wird nach **Create sked** auch die Sked-Übergabe versucht. |
| **UDP-Port for Win-Test listener** | Port des Win-Test-Netzwerks. Standard ist `9871`. Der Port wird auch für die Sked-Übergabe verwendet. |
| **KST station name in Win-Test network (src of SKED packets)** | Stationsname, unter dem KST4Contest die Sked-Pakete sendet. In einem Netzwerk mit mehreren Clients sollte ein eindeutiger Name verwendet werden. |
| **Win-Test network broadcast address** | Zieladresse für ausgehende Win-Test-Netzwerkpakete. Bei lokalem Netzwerkbetrieb muss hier eine vom Win-Test-Rechner erreichbare Broadcast-Adresse eingetragen sein. |

Die Broadcast-Adresse ist konfigurierbar, weil `255.255.255.255` nicht in jedem Stationsnetz und nicht über jede Netzwerkschnittstelle zuverlässig weitergeleitet wird. Bei mehreren Rechnern kann stattdessen die zum Stationsnetz gehörende gerichtete Broadcast-Adresse erforderlich sein.

### Einstellungen unter TRX sync

| Einstellung | Funktion |
|---|---|
| **Win-Test STATUS QRG Sync** | Übernimmt die aktuelle Frequenz aus Win-Test-STATUS-Paketen als eigene QRG. |
| **Use pass frequency from Win-Test STATUS** | Verwendet die übertragene Pass-Frequenz anstelle der normalen TRX-QRG. |
| **Win-Test station name filter** | Verarbeitet nur STATUS-Pakete der angegebenen Win-Test-Station. Ein leeres Feld akzeptiert alle Stationsnamen. |

Der Stationsfilter ist insbesondere bei mehreren Win-Test-Clients sinnvoll. Ohne Filter kann die zuletzt eingegangene STATUS-Meldung eines anderen Arbeitsplatzes die eigene QRG in KST4Contest überschreiben.

### Sked-Übergabe

Für die Sked-Übergabe gibt es keinen davon getrennten internen Sked-Modus. Ist der Listener aktiviert, versucht **Create sked** zusätzlich zur internen Anlage die Übertragung an Win-Test.

KST4Contest sendet nur dann ein `ADDSKED`-Paket, wenn eine QRG ermittelt wurde, die zum ausdrücklich ausgewählten Band gehört. Kann keine passende QRG gefunden werden, bleibt der interne Sked bestehen und die Win-Test-Übergabe wird ausgelassen.

Die Auswahl `SSB` oder `CW` erfolgt direkt im Further-Info-Bereich beim Anlegen des Skeds. Eine automatische Mode-Ableitung wird nicht verwendet.

Nach Änderungen **Save Settings** verwenden, damit Port, Stationsname, Broadcast-Adresse und TRX-Optionen beim nächsten Programmstart wiederhergestellt werden.

Datenbehandlung und QRG-Auswahl: [Log-Synchronisation – Win-Test](de-Log-Synchronisation#win-test)


## PSTRotator-Einstellungen (ab v1.31)

KST4Contest kann die Antennenrichtung über PSTRotator steuern.

Einstellungen:
- **Aktivieren/Deaktivieren**: Checkbox in den Preferences (ab v1.40).
- **IP-Adresse**: IP-Adresse des PSTRotator-Rechners (Standard: `127.0.0.1` bei Betrieb auf demselben PC).
- **Port**: Kommunikationsport von PSTRotator.

> **Hinweis**: Nach einem Klick auf den Richtungs-Button wartet KST4Contest kurz auf die Rotatorantwort. Bei langsamen Rotoren (z. B. SPID) kann es zu einer kleinen Verzögerung kommen.

---

## GUI Settings: Hinweise in den Bandspalten

Im Reiter **GUI** lassen sich zwei Zusatzinformationen der Bandspalten ein- oder ausblenden:

- **Show "o" in band columns …** zeigt ein `o`, wenn das vierstellige Großfeld auf dem betreffenden Band bereits gearbeitet wurde. Das Abschalten entfernt keine Daten aus der Datenbank; nur die zusätzliche Anzeige in den Bandspalten wird ausgeblendet. `wkdany` bleibt davon unberührt.
- **Show "a" in band columns …** unterscheidet ein vollständig neues Rufzeichen von einer Bandmöglichkeit mit einem bereits auf einem anderen Band gearbeiteten Rufzeichen. Ist die Option ausgeschaltet, werden beide Fälle als `B+` dargestellt. Die Bandherleitung selbst ändert sich dadurch nicht.

Änderungen werden in der laufenden Benutzeroberfläche unmittelbar sichtbar. Damit sie nach dem nächsten Programmstart erhalten bleiben, anschließend **Save Settings** verwenden.

![GUI-Einstellungen für die Hinweise in den Bandspalten](client_settings_window_gui.png)

---

## Worked Station Database Settings (Gearbeitete-Stationen-Datenbank)

Die interne SQLite-Datenbank speichert die contestbezogenen Zustände unabhängig von der Datenbank des Logprogramms:

- globaler Worked-Status eines Rufzeichens,
- Worked-Status pro Band,
- manuell gesetzte NOT-QRV-Tags pro Band und
- gearbeitete vierstellige Großfelder pro Band.

Als Schlüssel wird das normalisierte Rufzeichen ohne sichtbare Chat-Klammern oder Kategorieformatierung verwendet. Dadurch können aktive Varianten desselben Rufzeichens konsistent ausgewertet werden.

Worked- und NOT-QRV-Informationen laufen drei Tage nach ihrer letzten Änderung automatisch ab. Gespeicherte Großfelder laufen drei Tage nach dem zugehörigen Logeintrag ab. Ein manuelles Zurücksetzen vor jedem Contest ist deshalb normalerweise nicht erforderlich.

Die Schaltfläche **Reset worked, NOT-QRV and grid data...** entfernt sämtliche Worked-Markierungen, NOT-QRV-Tags und gespeicherten Großfelder. Vor dem Reset erscheint eine Sicherheitsabfrage. Die bekannten Rufzeichenzeilen bleiben erhalten; zurückgesetzt werden nur die contestbezogenen Zustände.

Ein Reset ist sinnvoll, wenn bewusst mit einem leeren Conteststand begonnen werden soll oder Testdaten eingelesen wurden. Als tägliche Wartungsmaßnahme ist er nicht vorgesehen.

Anzeige und Herleitung: [Gearbeitete Rufzeichen, neue Bänder und neue Großfelder](de-Funktionen#gearbeitete-rufzeichen-neue-bänder-und-neue-großfelder).

---

## Dark Mode (ab v1.26)

Umschaltbar über das Menü: **Window → Use Dark Mode**. Die Farben können über CSS individuell angepasst werden.

---

## Einstellungen speichern

Nach **jeder** Änderung **„Save Settings"** klicken! Ohne Speichern gehen alle Änderungen beim nächsten Start verloren.

- Speicherort: unter Linux `~/.praktikst/preferences.xml` und unter Windows `%USERPROFILE%\.praktikst\preferences.xml` (bzw. `C:\Users\<Benutzername>\.praktikst\preferences.xml`)
- Ab v1.21: Fenstergrößen und Divider-Positionen werden ebenfalls gespeichert.
- Bei Problemen: Konfigurationsdatei löschen → KST4Contest erstellt eine neue mit Standardwerten.
