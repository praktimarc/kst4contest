# Log-Synchronisation

> 🇬🇧 [English version](en-Log-Sync) | 🇩🇪 Du liest gerade die deutsche Version

KST4Contest übernimmt gearbeitete Stationen aus dem Logprogramm und stellt daraus den globalen Worked-Status, bandbezogene Worked-Markierungen und – sofern ein Locator übertragen wurde – gearbeitete Großfelder bereit. Dafür gibt es drei Wege: den dateibasierten Simplelogfile-Interpreter, den allgemeinen QSO-UDP-Listener und den eigenen Win-Test-Netzwerk-Listener.

---

![Log-Synchronisation Einstellungsfenster](client_settings_window_logsync.png)

## Methode 1: Universal File Based Callsign Interpreter (Simplelogfile)

KST4Contest liest die ausgewählte Textdatei einmal pro Minute und sucht darin mit einem fest eingebauten regulären Ausdruck nach Rufzeichen. Jedes gefundene Rufzeichen wird auf sein Basisrufzeichen normalisiert. Der globale Worked-Status gilt dadurch für alle derzeit aktiven Chat-Varianten dieses Rufzeichens.

Der Vorteil liegt in der breiten Kompatibilität: Die Funktion benötigt keine besondere Netzwerkschnittstelle des Logprogramms.

Die Grenze ist ebenso eindeutig: Aus einem reinen Rufzeichentreffer lassen sich weder Band noch Locator zuverlässig ableiten. Der Simplelogfile-Interpreter kann deshalb nur den globalen Worked-Status setzen. Er erzeugt keine bandbezogene `X`-Markierung, kein Worked-Großfeld und keine belastbare Grundlage für den Band-Upgrade-Hinweis nach einem Logeintrag.

Den Pfad der Textdatei im Reiter **Log sync** auswählen. Fehlt die Datei, legt KST4Contest sie an und zeigt einmalig einen nicht blockierenden Hinweis mit dem Pfad und den nächsten Prüfschritten an. Lese- oder Erstellungsfehler werden protokolliert; die minütliche Auswertung läuft beim nächsten Termin weiter. Für bandbezogene Auswertungen sollte nach Möglichkeit eine der Netzwerkschnittstellen verwendet werden.

Der aus dem Simplelogfile abgeleitete Worked-Status wird nicht in der internen SQLite-Datenbank gespeichert. Die ausgewählte Datei ist die dauerhafte Quelle und wird auch nach einem Neustart erneut eingelesen. Der Interpreter setzt nur positive Worked-Markierungen; er entfernt während der laufenden Programmsitzung keine bereits gesetzte Markierung und führt beim Wechsel zu einem neuen Contest keinen automatischen Reset durch. Auch ein manueller Datenbank-Reset ändert oder leert das Simplelogfile nicht. Darin enthaltene Rufzeichen werden bei der nächsten Auswertung innerhalb einer Minute erneut als gearbeitet markiert. Vor jedem Contest sollte deshalb geprüft werden, ob das Logprogramm genau die aktuelle Contestdatei beschreibt.

---

## Methode 2: Netzwerk-Listener für QSO-UDP-Pakete – empfohlen

UCXLog, QARTest, N1MM+ und DXLog.net können beim Speichern eines QSOs ein UDP-Paket senden. KST4Contest empfängt diese Pakete standardmäßig auf Port `12060` und übernimmt das Rufzeichen sowie die enthaltenen Band- und Locatorinformationen.

Liegt eine Bandinformation vor, wird das Rufzeichen für dieses Band als gearbeitet markiert. Enthält das Paket zusätzlich einen gültigen Locator, speichert KST4Contest dessen vierstelliges Großfeld für das betreffende Band. Fehlende Informationen werden nicht aus anderen Feldern geraten.

KST4Contest muss zum Zeitpunkt der Übertragung laufen. Einige Logprogramme können jedoch das vorhandene Log erneut senden: QARTest bietet dafür **Invia log completo**; DXLog.net sendet beim Broadcast des vollständigen Logs `contactreplace`-Pakete, die KST4Contest ebenfalls verarbeitet.

**Standardport:** `12060`

---

## Unterstützte Logprogramme

### UCXLog (DL7UCX)

![UCXLog Konfiguration](ucxlog_logsync.png)

UCXLog sendet QSO-UDP-Pakete und Transceiver-Frequenzpakete.

**Einstellungen in UCXLog:**
- UDP-Broadcast aktivieren
- IP-Adresse des KST4Contest-Computers eintragen (bei lokalem Betrieb: `127.0.0.1`)
- Port: 12060 (Standard)

Grün markierte Felder in den UCXLog-Einstellungen beachten: IP und Port müssen eingetragen werden.

Hinweis für Multi-Setup (2 Computer, 2 Radios, eine KST4Contest-Instanz): Beide Logprogramme müssen die QSO-Pakete an die IP des KST4Contest-Computers senden. Dann ist mindestens eine IP nicht `127.0.0.1`.

### QARTest (IK3QAR)

![QARTest Konfiguration](qartest_logsync.png)

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

![DXLog.net Konfiguration](dxlog_net_logsync.png)

**Einstellungen in DXLog.net:**
- UDP-Broadcast aktivieren
- IP des KST4Contest-Computers eintragen (grün markierte Felder)
- Port: 12060

Beim Broadcast des vollständigen Logbuchs verwendet DXLog.net `contactreplace` anstelle von `contactinfo`. KST4Contest verarbeitet beide Pakettypen. Damit können auch ältere QSOs übernommen werden, wenn der vollständige Broadcast ausgelöst wird, während KST4Contest läuft.

### Win-Test

Win-Test wird über einen eigenen UDP-Listener für das native Win-Test-Netzwerkprotokoll angebunden. Dieser Listener ist vom allgemeinen QSO-UDP-Listener auf Port `12060` unabhängig.

#### QSO- und Worked-Synchronisation

Bei einem neuen QSO übernimmt KST4Contest:

- das geloggte Rufzeichen,
- die native Win-Test-Band-ID und
- einen gültigen Locator, sofern er im Paket enthalten ist.

Die Band-IDs für 50 und 70 MHz werden ebenso verarbeitet wie die VHF-, UHF- und SHF-Bänder. Das Rufzeichen wird global und auf dem erkannten Band als gearbeitet markiert. Liegt zusätzlich ein Locator vor, wird dessen vierstelliges Großfeld für dieses Band gespeichert.

Die Daten werden in derselben internen Datenbank abgelegt wie Worked-Informationen aus den übrigen QSO-UDP-Schnittstellen und nach einem Neustart wiederhergestellt.

#### Bereits geloggte QSOs nachladen

Win-Test sendet jedes neue QSO als Broadcast. QSOs, die vor dem Start von KST4Contest geloggt wurden, sind darin nicht enthalten. KST4Contest fordert diese QSOs deshalb selbst an, sobald der Win-Test-Netzwerk-Listener eine Win-Test-Station im Netzwerk erkennt.

Der Abgleich benötigt keine eigene Einstellung und keinen Bedienschritt:

- Win-Test meldet mit `IHAVE`, welche QSO-Nummern welches Logs es führt.
- KST4Contest fordert die fehlenden Bereiche mit `NEEDQSO` an, höchstens 50 QSOs pro Anfrage.
- Win-Test beantwortet die Anfrage mit gewöhnlichen `ADDQSO`-Paketen. Sie werden genauso ausgewertet wie ein live geloggtes QSO.

Bereits gearbeitete Stationen erscheinen dadurch auch dann als gearbeitet, wenn KST4Contest erst während des Contests gestartet wird. Der Abgleich bleibt anschließend aktiv und holt auch einzelne Pakete nach, die im laufenden Betrieb verloren gegangen sind. Bereits bekannte QSOs werden erkannt und nicht erneut gespeichert.

Sind mehrere Win-Test-Stationen im Netzwerk aktiv, wird jedes Log abgeglichen. Damit sind die bandbezogenen Worked-Markierungen aller Bandstationen vollständig. Der Stationsnamensfilter wirkt weiterhin nur auf die QRG-Synchronisation und schränkt den Logabgleich nicht ein.

Meldet eine erkannte Station kein auswertbares `IHAVE`, etwa bei einer älteren Win-Test-Version, fordert KST4Contest die QSOs blockweise ab QSO-Nummer 1 an, bis ein Block unbeantwortet bleibt.

Voraussetzung ist ein aktiviertes Win-Test-Netzwerk. Ist das Win-Test-Netzwerk oder der Listener in KST4Contest deaktiviert, findet kein Abgleich statt.

#### Skeds an Win-Test übergeben

Mit **Create sked** wird zunächst ein interner KST4Contest-Sked angelegt. Ist der Win-Test-Netzwerk-Listener aktiviert, versucht KST4Contest anschließend automatisch, den Sked als `ADDSKED` an das Win-Test-Netzwerk zu übertragen.

Die QRG wird in folgender Reihenfolge bestimmt:

1. KST4Contest sucht die neueste, höchstens 30 Minuten alte QRG der Gegenstation auf dem ausdrücklich ausgewählten Band. Dabei werden aktive Varianten desselben Basisrufzeichens gemeinsam ausgewertet.
2. Fehlt eine solche QRG, wird die eigene QRG der Chat-Kategorie geprüft, in der der Sked angelegt wurde. Sie wird nur verwendet, wenn sie sich auswerten lässt und tatsächlich zum ausgewählten Band gehört.
3. Kann auf keinem dieser Wege eine passende QRG ermittelt werden, wird kein `ADDSKED` gesendet.

Eine feste Ersatzfrequenz wie `144.300` wird bewusst nicht verwendet. Eine technisch erfolgreiche Übergabe mit falschem Band oder falscher QRG wäre im Contestbetrieb schlechter als eine sichtbar ausgelassene Übergabe.

Der interne Sked bleibt in jedem Fall erhalten. Das gilt auch bei einer ungültigen Broadcast-Adresse, einem Netzwerkfehler oder einem nicht erreichbaren Win-Test-Client.

#### Behandlung von KST-Rufzeichensuffixen

KST-Suffixe kennzeichnen häufig den verwendeten Chat-Login oder ein Band. Sie gehören nicht in jedem Fall zum Logrufzeichen. Für Win-Test entfernt KST4Contest deshalb einen mit `-` abgetrennten KST-Suffix, erhält aber portable und internationale Rufzeichenbestandteile:

| Rufzeichen im KST-Chat | Übergabe an Win-Test |
|---|---|
| `DN9APW-2` | `DN9APW` |
| `9A0BB-70` | `9A0BB` |
| `EA5/G8MBI/P-70` | `EA5/G8MBI/P` |
| `DN9APW-2/P` | `DN9APW/P` |

Innerhalb von KST4Contest bleibt das vollständige Rufzeichen erhalten. Timeline, Reminder-PMs und Chat-Kategorie beziehen sich weiterhin auf den konkret ausgewählten Login.

#### Mode, Zeitpunkt und Notizen

Der Mode wird beim Anlegen des Skeds ausdrücklich als `SSB` oder `CW` gewählt. Eine automatische Ableitung aus der QRG findet nicht statt, weil eine begrenzte Liste angenommener Bandsegmente nicht alle unterstützten VHF-, UHF- und SHF-Bänder zuverlässig abbilden kann.

KST4Contest überträgt den tatsächlichen Sked-Zeitpunkt ohne einen zusätzlichen Minutenversatz. Die Notizen enthalten – soweit bekannt – Locator und QTF sowie den Hinweis, dass der Sked über KST4Contest angelegt wurde.

Für die Übergabe sendet KST4Contest die Win-Test-Pakete `LOCKSKED`, `ADDSKED` und `UNLOCKSKED`.

![Von KST4Contest an Win-Test übergebener Sked](wintest_sked_handover.png)

#### Einstellungen

Im Reiter **Log sync**:

- `Receive Win-Test network based UDP log messages`
- `UDP-Port for Win-Test listener`, standardmäßig `9871`
- `KST station name in Win-Test network (src of SKED packets)`
- `Win-Test network broadcast address`

Im Reiter **TRX sync**:

- `Win-Test STATUS QRG Sync`
- `Use pass frequency from Win-Test STATUS`
- `Win-Test station name filter`

Das Win-Test-Netzwerk muss in Win-Test aktiviert sein. Der Stationsname sollte die sendende KST4Contest-Instanz innerhalb des Win-Test-Netzwerks eindeutig erkennen lassen.

Die Broadcast-Adresse ermittelt KST4Contest selbst: Aus der Absenderadresse der empfangenen Win-Test-Pakete wird das passende lokale Netzwerk bestimmt und dessen Broadcast-Adresse verwendet. Die eingetragene Adresse dient als Rückfallebene, wenn kein lokales Netzwerk zur Win-Test-Station passt, etwa wenn Win-Test hinter einem Router liegt.

Das ist wichtig, weil Win-Test ausschließlich auf Broadcasts reagiert und eine Adresse in einem nicht vorhandenen Netzwerk keinen Fehler auslöst: Das Paket wird ohne Meldung weggeroutet. Eine veraltete Eintragung, etwa aus einem anderen Netzwerk, machte dadurch früher sowohl die Sked-Übergabe als auch den Logabgleich wirkungslos.

Ausführliche Beschreibung der Einstellungen: [Win-Test-Netzwerk-Listener](de-Konfiguration#win-test-netzwerk-listener-ab-v131)


## TRX-Frequenz-Synchronisation

Neben der QSO-Synchronisation übertragen UCXLog und andere Programme auch die **aktuelle Transceiverfrequenz** via UDP. KST4Contest verarbeitet diese Information und stellt sie als Variable `MYQRG` bereit.

![FrequenzButtons](qrg_buttons.png)

**Ergebnis**: Eine aktivierte Schnittstelle aktualisiert `MYQRG`, sobald sie tatsächlich gültige Frequenzpakete liefert. Das Aktivieren allein erzeugt noch keine QRG. Kommen keine passenden Pakete an, muss die Schnittstelle geprüft oder die QRG nach dem Deaktivieren beider automatischen Quellen manuell gepflegt werden.

**Quellen für die eigene QRG (MYQRG):**
- UCXLog, N1MM+, DXLog.net, QARTest via UDP-Port 12060
- Win-Test STATUS-Paket (optional, konfigurierbar im Reiter „TRX-Synchronisation" unter „Win-Test STATUS QRG Sync")
- Manuelle Eingabe im QRG-Feld

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

KST4Contest speichert Worked-, NOT-QRV- und Großfeldinformationen aus den Netzwerkschnittstellen sowie manuelle Markierungen in einer eigenen SQLite-Datenbank. Sie ist von der Datenbank des Logprogramms unabhängig. Simplelogfile-Treffer sind davon ausgenommen und werden bei jeder Programmsitzung aus der ausgewählten Datei neu abgeleitet.

Die Datenquellen liefern unterschiedlich genaue Informationen:

| Quelle | Rufzeichen global | Bandbezogen | Großfeld |
|---|---:|---:|---:|
| Simplelogfile | ja | nein | nein |
| QSO-UDP-Listener | ja | ja, wenn im Paket enthalten | ja, wenn Band und Locator enthalten sind |
| Win-Test-Netzwerk-Listener | ja | ja | ja, wenn ein Locator enthalten ist |

Die in SQLite gespeicherten Daten werden beim Programmstart wieder geladen und bei neuen Logeinträgen während des Betriebs aktualisiert. Sie laufen nach drei Tagen automatisch ab. Ein Reset vor jedem Contest ist daher normalerweise nicht erforderlich. Für den Simplelogfile-Interpreter gilt diese Lebensdauer nicht: Seine Datei bleibt die dauerhafte Quelle und wird durch einen Datenbank-Reset weder geändert noch geleert. Enthaltene Rufzeichen setzt die nächste periodische Auswertung erneut auf den globalen Worked-Status.

Ein vollständiger manueller Reset entfernt Worked-Markierungen, NOT-QRV-Tags und Worked-Großfelder gemeinsam. Weitere Einzelheiten: [Worked Station Database Settings](de-Konfiguration#worked-station-database-settings-gearbeitete-stationen-datenbank).
