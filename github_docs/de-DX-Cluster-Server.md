# Integrierter DX-Cluster-Server

> 🇬🇧 [English version](en-DX-Cluster-Server) | 🇩🇪 Du liest gerade die deutsche Version

Seit Version 1.23 enthält KST4Contest einen lokalen DX-Cluster-Server. Er übergibt erkannte Richtungsgelegenheiten und die dazugehörige Frequenz an den DX-Cluster-Client eines Logprogramms.

Die Idee dazu stammt von OM0AAO, Viliam Petrik. Vielen Dank!

---

## Warum überhaupt ein eigener DX-Cluster-Server?

Eine interessante Frequenz im Chat zu erkennen ist nur der erste Schritt. Im Contest muss diese Information dort ankommen, wo sie unmittelbar verwendet werden kann: im Logprogramm und dessen Bandmap.

KST4Contest verbindet deshalb zwei bereits vorhandene Informationen:

1. Aus einer gerichteten Chat-Nachricht lässt sich abschätzen, in welche Richtung die sendende Station ihre Antenne wahrscheinlich ausgerichtet hat.
2. Aus derselben oder einer vorherigen Nachricht kann eine Frequenz der Station bekannt sein.

Treffen beide Informationen zusammen, erzeugt KST4Contest einen lokalen DX-Cluster-Spot. Der Logger kann diesen Spot in seiner Bandmap anzeigen und – abhängig von seiner eigenen Konfiguration – den Transceiver nach einem Klick auf die entsprechende Frequenz einstellen.

Im Klartext: Die Information muss nicht erst im Chat gefunden, gelesen, gemerkt und anschließend erneut in den Logger eingetragen werden. Genau diese kleinen Unterbrechungen kosten im Contest überraschend viel Aufmerksamkeit.

---

## Wie wird eine Richtungsgelegenheit hergeleitet?

Angenommen, Station A schreibt eine gerichtete Nachricht an Station B. KST4Contest verwendet die Richtung von A zu B als Näherung für die aktuelle Antennenrichtung von Station A. Anschließend wird geprüft, ob die eigene Station aus Sicht von A innerhalb des angenommenen Antennenkorridors liegt.

Dafür werden zwei Richtungen verglichen:

- die Richtung von Station A zu Station B,
- die Richtung von Station A zur eigenen Station.

Der in den Station Settings konfigurierte Antennen-Öffnungswinkel ist der vollständige Winkel. Für die Prüfung wird jeweils die Hälfte links und rechts der Richtung A → B angesetzt. Bei `70°` sind das somit `±35°`.

Da ON4KST keine Antennendaten der fremden Station überträgt, verwendet KST4Contest den Öffnungswinkel der eigenen Antenne zugleich als Näherung für Station A. Das ist keine Messung der tatsächlichen Antennenrichtung, sondern eine bewusst einfache geometrische Annahme.

Ein DX-Cluster-Spot wird nur erzeugt, wenn alle folgenden Bedingungen erfüllt sind:

1. Eine gerichtete Nachricht wurde zwischen zwei anderen Stationen erkannt.
2. Für Absender und Empfänger sind gültige Locator bekannt.
3. Der Absender liegt innerhalb des konfigurierten maximalen QRB zur eigenen Station.
4. Die eigene Station liegt aus Sicht des Absenders innerhalb des angenommenen Antennenkorridors.
5. Für den Absender ist eine verwertbare Frequenz bekannt oder in der aktuellen Nachricht erkannt worden.
6. Der lokale DX-Cluster-Server ist aktiviert.

Treffen die Bedingungen zu, wird der Spot unmittelbar beim Verarbeiten der Nachricht erzeugt. Die parallel angezeigte grüne Richtungsmarkierung bleibt dagegen fünf Minuten sichtbar und kann durch spätere Nachrichten verlängert oder vorzeitig entfernt werden.

Das Verfahren berücksichtigt weder Gelände noch aktuelle Ausbreitungsbedingungen und beweist keine tatsächliche Antennenstellung. Es erkennt eine plausible Gelegenheit. Die ausführliche Herleitung und ein Zahlenbeispiel stehen unter [Richtungsgelegenheiten aus gerichteten Nachrichten](de-Funktionen#richtungsgelegenheiten-aus-gerichteten-nachrichten).

---

## Welche Frequenz wird verwendet?

Ein DX-Cluster-Spot benötigt eine eindeutige Frequenz. KST4Contest verwendet dafür dieselbe QRG-Erkennung wie die Benutzerliste und die übrigen bandbezogenen Funktionen.

Vollständige Frequenzen bestimmen ihr Band direkt:

```text
144.205
432,088
1296.338
10368.100
```

Relative Angaben enthalten dagegen nur den Frequenzanteil innerhalb eines Bandes:

```text
.205
,205
qrg 205
freq is 205
on 205
205 MHz
```

Eine nackte dreistellige Zahl wie `205` wird ohne Frequenzkontext nicht ausgewertet. Dasselbe gilt für `599`, `144` oder Formulierungen wie `worked 210 stations`. So verhindert KST4Contest, dass Signalrapporte, Bandnamen oder Zählwerte als scheinbar plausible QRG gespeichert und später an den Logger weitergegeben werden.

Bei einer relativen QRG wird das Band in dieser Reihenfolge bestimmt:

1. KST4Contest prüft, ob für denselben Absender innerhalb der letzten 30 Minuten bereits ein passender Bandkontext bekannt wurde.
2. Sind mehrere aktuelle Bänder bekannt, wird der zuletzt aktualisierte plausible Kontext verwendet.
3. Erst wenn kein geeigneter Stationskontext vorhanden ist, verwendet KST4Contest das unter **Fallback band for relative QRG detection** ausgewählte Band.

Beispiel:

```text
Globales Fallback: 144 MHz
Letzte vollständige QRG der Station: 432.088 MHz
Neue Chat-Angabe derselben Station: .100
Erkannte QRG: 432.100 MHz
DX-Cluster-Frequenz: 432100.0 kHz
```

Ohne den aktuellen 432-MHz-Kontext würde dieselbe Angabe mit dem globalen Fallback zu `144.100 MHz` ergänzt.

Die QRG-Erkennung läuft vor der Richtungs- und Spotprüfung. Nennt eine Station ihre Frequenz erstmals in der gerichteten Nachricht, die zugleich eine Richtungsgelegenheit auslöst, kann bereits der daraus erzeugte Spot diese Frequenz enthalten. War zuvor eine andere QRG der Station bekannt, wird sie durch die neu erkannte Angabe aktualisiert.

Das Fallback-Band ist eine globale Einstellung der QRG-Erkennung. Seine Wirkung ist nicht auf den DX-Cluster beschränkt. Konfiguration, unterstützte Bänder und weitere Folgen sind unter [Fallback-Band für relative QRG-Erkennung](de-Konfiguration#fallback-band-für-relative-qrg-erkennung) beschrieben.

---

## Einrichtung in KST4Contest

Öffne den Reiter **Notification** in den Preferences.

![Benachrichtigungen und lokaler DX-Cluster-Server](client_settings_window_notification.png)

Konfiguriere anschließend:

1. **Enable the local DX Cluster server …** aktivieren.
2. Einen freien **TCP port** eintragen. Standard ist `8000`.
3. Unter **Fallback band for relative QRG detection** das passende Band auswählen.
4. Ein **Spotter callsign** festlegen.

Für das Spotter-Rufzeichen sollte nach Möglichkeit ein anderes Rufzeichen als das Contest-Rufzeichen verwendet werden. Einige Logger filtern Spots, die scheinbar von der eigenen Station stammen. Das Ergebnis wäre technisch korrekt erzeugt, aber in der Bandmap trotzdem unsichtbar – eine besonders unproduktive Art von Erfolg.

Änderungen am Aktivierungsstatus und am TCP-Port werden während einer laufenden Chat-Verbindung sofort angewendet. Bei einem Portwechsel werden vorhandene DX-Cluster-Verbindungen getrennt und müssen vom Logger neu aufgebaut werden.

Die Einstellungen werden erst mit **Save Settings** dauerhaft in der `preferences.xml` gespeichert.

---

## Einrichtung im Logprogramm

Das Logprogramm wird als DX-Cluster-Client mit KST4Contest verbunden.

| Einstellung | KST4Contest und Logger auf demselben Computer | Logger auf einem anderen Computer |
|---|---|---|
| Host | `127.0.0.1` | IP-Adresse des KST4Contest-Computers |
| Port | In KST4Contest konfigurierter TCP-Port | In KST4Contest konfigurierter TCP-Port |
| Login | Beliebiges Rufzeichen, falls der Logger eines verlangt | Beliebiges Rufzeichen, falls der Logger eines verlangt |
| Passwort | Nicht erforderlich | Nicht erforderlich |

KST4Contest wertet den vom Logger gesendeten Login nicht zur Authentifizierung aus. Die Verbindung ist für ein lokales oder vertrauenswürdiges Stationsnetz vorgesehen.

Wenn der Logger auf einem anderen Computer läuft, muss dessen Verbindung durch die lokale Firewall des KST4Contest-Computers zugelassen werden. Der Port sollte nicht ohne weiteren Schutz aus dem Internet erreichbar sein.

Mehrere DX-Cluster-Clients können gleichzeitig verbunden werden. Ein erzeugter Spot wird an alle aktuell verbundenen Clients gesendet.

---

## Verbindung testen

Die Schaltfläche **Send test spot** erzeugt einen neutralen Testeintrag:

```text
Spotted callsign: DO5AMF
Comment: KST4CONTEST TEST
Frequency: .300 des konfigurierten Fallback-Bandes
```

Bei einem Fallback-Band von `144` erscheint der Spot daher auf ungefähr `144.300 MHz`.

Vor dem Test müssen drei Bedingungen erfüllt sein:

1. KST4Contest ist mit dem ON4KST-Chat verbunden.
2. Der lokale DX-Cluster-Server ist aktiviert.
3. Der DX-Cluster-Client des Logprogramms ist mit KST4Contest verbunden.

Fehlt die Client-Verbindung, zeigt KST4Contest eine entsprechende Meldung an. Ein erfolgreich ausgeführter Test bedeutet damit tatsächlich, dass mindestens ein Client den Spot erhalten hat.

---

## Inhalt eines erzeugten Spots

Ein Spot enthält:

- das konfigurierte Spotter-Rufzeichen,
- die normalisierte Frequenz,
- das Rufzeichen der erkannten Station,
- den Locator,
- Flugzeug-Scatter-Informationen, falls vorhanden,
- die aktuelle UTC-Zeit.

Wenn für die Station aktuelle Aircraft-Scatter-Informationen vorliegen, kann KST4Contest diese als zusätzliche AP-Information in den Kommentar des Spots aufnehmen.

---

## Wenn kein Spot erscheint

### Der Testspot kommt nicht im Logger an

Prüfe:

- Ist KST4Contest mit dem Chat verbunden?
- Ist der lokale DX-Cluster-Server aktiviert?
- Verwendet der Logger denselben TCP-Port?
- Verwendet der Logger bei lokalem Betrieb `127.0.0.1`?
- Blockiert eine Firewall die Verbindung?
- Ist im Logger das DX-Cluster-Fenster beziehungsweise die Bandmap aktiviert?

### Testspot funktioniert, aber reale Spots fehlen

Dann funktioniert die Verbindung grundsätzlich. Für die betreffende Chat-Situation war wahrscheinlich mindestens eine fachliche Bedingung nicht erfüllt:

- kein gerichteter Nachrichtenaustausch,
- fehlender Locator,
- Station außerhalb des maximalen QRB,
- Richtung außerhalb des konfigurierten Öffnungswinkels,
- keine erkannte Frequenz.

KST4Contest sendet absichtlich nicht jede gefundene Frequenz an den Logger. Andernfalls würde aus einer Arbeitserleichterung sehr schnell eine lokale Spot-Schleuder.

### Der Spot erscheint auf dem falschen Band

Prüfe zuerst, welche Frequenzen für die betreffende Station innerhalb der letzten 30 Minuten erkannt wurden. Bei einer relativen Angabe hat dieser Stationskontext Vorrang vor dem globalen Fallback.

Ist kein aktueller Stationskontext vorhanden, prüfe die Auswahl unter **Fallback band for relative QRG detection**. Das Fallback wird nur benötigt, wenn sich das Band weder aus einer vollständigen Frequenz noch aus dem aktuellen Kontext des Absenders ergibt.

### Der Spot wird vom Logger ausgeblendet

Verwende ein Spotter-Rufzeichen, das nicht mit dem eigenen Contest-Rufzeichen identisch ist. Abhängig vom Logger können eigene Spots gefiltert oder besonders behandelt werden.

---

## Getestete Logprogramme

Die Schnittstelle wurde mit folgenden Logprogrammen verwendet:

- UCXLog
- N1MM+

Weitere Logger können funktionieren, wenn sie eine normale TCP-Verbindung zu einem DX-Cluster-Server unterstützen.
