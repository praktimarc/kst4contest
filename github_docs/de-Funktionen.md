# Funktionen

> 🇬🇧 [English version](en-Features) | 🇩🇪 Du liest gerade die deutsche Version

Dieses Kapitel beschreibt die wichtigsten Funktionen von KST4Contest, ihre Herleitung und die Grenzen der daraus gewonnenen Informationen.

---
## Richtungsgelegenheiten aus gerichteten Nachrichten

Im ON4KST-Chat ist sichtbar, welche Station eine Nachricht an welche andere Station richtet. Eine tatsächliche Antennenrichtung wird dabei nicht übertragen. Für den Contestbetrieb lässt sich aus einer solchen Nachricht trotzdem eine brauchbare Annahme ableiten: Wer einen Sked anfragt, beantwortet oder vorbereitet, richtet seine Antenne normalerweise zumindest ungefähr auf die angesprochene Station.

KST4Contest wertet deshalb gerichtete Nachrichten zwischen zwei anderen Stationen aus. Die Nachricht muss nicht ausdrücklich als Sked gekennzeichnet sein. Entscheidend sind der Absender, der Empfänger und deren Locator.

### Wie wird die Richtung hergeleitet?

Angenommen, Station A schreibt eine gerichtete Nachricht an Station B:

1. KST4Contest berechnet die Richtung von Station A zu Station B.
2. Diese Richtung wird als wahrscheinliche Antennenrichtung von Station A verwendet.
3. Anschließend wird die Richtung von Station A zur eigenen Station berechnet.
4. Die Winkeldifferenz wird mit der Hälfte des konfigurierten Antennen-Öffnungswinkels verglichen.
5. Zusätzlich muss Station A innerhalb des konfigurierten maximalen QRB liegen.

Ein eingetragener Öffnungswinkel von `70°` ergibt damit einen angenommenen Korridor von jeweils `35°` links und rechts der Richtung von Station A zu Station B.

| Beispiel | Ergebnis |
|---|---|
| Richtung A → B: `120°`, Richtung A → eigene Station: `145°` | Winkeldifferenz `25°`: Richtungsgelegenheit erkannt |
| Richtung A → B: `120°`, Richtung A → eigene Station: `165°` | Winkeldifferenz `45°`: außerhalb des angenommenen Korridors |
| Locator von A oder B fehlt | Keine Richtungsberechnung möglich |
| A liegt außerhalb des maximalen QRB | Keine Richtungsgelegenheit |

### Was wird in der Benutzerliste angezeigt?

Wird eine Richtungsgelegenheit erkannt, erscheint das Rufzeichen des Absenders in der Benutzerliste grün und fett. Im Evening-Modus wird dafür ein helleres Grün verwendet. Der Empfänger der Nachricht wird nicht allein deshalb markiert; eine Antwort in Gegenrichtung wird als eigene Nachricht und damit als neuer Fall berechnet.

![Erkannte Richtungsgelegenheit in der Benutzerliste](direction_opportunity_highlight.png)

Im Bild sendete DF0GEB eine gerichtete Nachricht an DN9APW und bekam eine Antwort. KST4Contest erkannte die Richtungsgelegenheit und markierte DN9APW in der Benutzerliste.
Zur Verdeutlichung ist die MAP eingeblendet. Ich stehe als Empfänger zwischen beiden Stationen und bekomme deswegen die Warnung.

Die Markierung bleibt fünf Minuten ab der letzten passenden Nachricht sichtbar. Eine weitere passende Nachricht derselben Station beginnt diesen Zeitraum erneut. Sendet die Station vorher eine gerichtete Nachricht, deren Richtung die Bedingungen nicht erfüllt, wird die Markierung unmittelbar entfernt.

Ist die einfache Soundausgabe aktiviert, gibt KST4Contest beim erstmaligen Erkennen der Richtungsgelegenheit zusätzlich einen kurzen Hinweis aus. Solange die Station bereits markiert ist, wird derselbe Hinweis nicht mit jeder weiteren passenden Nachricht wiederholt.

### Was bedeutet die Markierung – und was nicht?

Die Berechnung ist eine geometrische Herleitung. Sie beweist nicht, dass Station A ihre Antenne tatsächlich auf Station B ausgerichtet hat. Ebenso wenig berücksichtigt sie Gelände, aktuelle Ausbreitungsbedingungen, die reale Antennencharakteristik der fremden Station oder deren Rotatorposition.

ON4KST liefert keinen individuellen Öffnungswinkel für die fremde Station. KST4Contest verwendet deshalb den für die eigene Antenne konfigurierten Wert auch als Näherung für Station A. Ein zu großer Wert erzeugt entsprechend mehr mögliche Richtungsgelegenheiten, ein zu kleiner Wert kann brauchbare Situationen übersehen.

Im Klartext: Die grüne Markierung ist ein begründeter Hinweis auf eine mögliche Gelegenheit. Sie ist weder eine Ausbreitungsvorhersage noch eine Garantie für ein QSO.

Konfiguration:

- [Antennen-Öffnungswinkel](de-Konfiguration#antennen-öffnungswinkel-antenna-beamwidth)
- [Standard-Maximum-QRB](de-Konfiguration#standard-maximum-qrb)

---

## Weitergabe als DX-Cluster-Spot

Seit Version 1.23 kann KST4Contest eine erkannte Richtungsgelegenheit an den DX-Cluster-Client eines Logprogramms weitergeben. Dafür muss der lokale DX-Cluster-Server aktiviert und für den Absender eine verwertbare Frequenz bekannt sein.

Die Frequenz kann bereits aus einer früheren Nachricht stammen oder erstmals in der aktuell auslösenden Nachricht stehen. In beiden Fällen steht sie der Spot-Prüfung zur Verfügung. KST4Contest überträgt damit nicht jede im Chat gefundene QRG, sondern nur Frequenzen, die mit einer geometrisch passenden gerichteten Nachricht zusammenfallen.

Die Fünf-Minuten-Markierung und der DX-Cluster-Spot beruhen auf derselben Richtungsberechnung, haben aber einen unterschiedlichen Lebenszyklus: Die Markierung bleibt vorübergehend in der Benutzerliste sichtbar. Der Spot wird unmittelbar beim Verarbeiten der passenden Nachricht erzeugt.

Einrichtung, Frequenzbehandlung und Grenzen: [Integrierter DX-Cluster-Server](de-DX-Cluster-Server).

---

## QRG-Erkennung

Im ON4KST-Chat werden Frequenzen selten einheitlich geschrieben. Eine Station nennt beispielsweise zuerst `432.088`, später nur noch `.100` und in einer weiteren Nachricht `qrg 120`. Für einen Menschen ist der Zusammenhang meistens klar. Ein Programm muss dagegen unterscheiden, ob `120` eine Frequenz, eine Zeitangabe, eine Entfernung oder etwas völlig anderes bedeutet.

KST4Contest wertet deshalb den Text jeder öffentlichen und gerichteten Chat-Nachricht aus. Eine erkannte QRG wird dem Absender zugeordnet und in der **QRG-Spalte** der Benutzerliste angezeigt. Die Spalte enthält die zuletzt erkannte Frequenz und stellt mindestens drei Nachkommastellen dar. Ein intern als `144.21` gespeicherter Wert erscheint damit als `144.210`.

### Welche Angaben werden erkannt?

| Schreibweise | Beispiel | Verarbeitung |
|---|---|---|
| Vollständige Frequenz | `144.210`, `432,088`, `10368.100` | Das Band ergibt sich direkt aus der Frequenz. |
| Relative Frequenz mit Punkt oder Komma | `.210`, `,088` | Das Band wird aus dem Stationskontext oder dem konfigurierten Fallback ergänzt. |
| Dreistellige Frequenz mit Textkontext | `qrg 210`, `freq is 210`, `on 210`, `210 MHz` | Die Zahl wird als relative Frequenz behandelt. |
| Dreistellige Zahl ohne Frequenzkontext | `210`, `599`, `144` | Die Zahl wird absichtlich nicht als QRG übernommen. |

Die letzte Einschränkung verhindert plausible, aber falsche Ergebnisse. Mit einem Fallback von `144 MHz` ließe sich ein Signalrapport `599` technisch problemlos zu `144.599 MHz` zusammensetzen. Das Ergebnis wäre formal gültig und fachlich trotzdem Unsinn.

### Wie wird das Band einer relativen QRG bestimmt?

KST4Contest verwendet folgende Reihenfolge:

1. Wurde für denselben Absender innerhalb der letzten 30 Minuten bereits eine passende vollständige Frequenz erkannt, verwendet KST4Contest deren Band.
2. Sind mehrere aktuelle Bänder bekannt, wird der zuletzt aktualisierte plausible Bandkontext verwendet.
3. Fehlt ein geeigneter Stationskontext, verwendet KST4Contest das unter **Fallback band for relative QRG detection** ausgewählte Band.

Beispiel: Das globale Fallback steht auf `144 MHz`. Eine Station nennt zunächst `432.088` und schreibt wenige Minuten später `.100`. KST4Contest ergänzt nicht das globale Fallback, sondern den aktuelleren Stationskontext. Das Ergebnis ist `432.100 MHz`. Schreibt eine andere Station ohne vorherige Bandinformation `.100`, wird daraus `144.100 MHz`.

Das Fallback-Band ist damit tatsächlich nur der letzte Ausweg. Es wird aus den von KST4Contest unterstützten Bandwerten ausgewählt und wirkt auf die gesamte QRG-Erkennung – nicht nur auf den integrierten DX-Cluster.

### Wofür wird die erkannte QRG verwendet?

Die zuletzt erkannte Frequenz erscheint in der Benutzerliste. Der zugehörige Bandkontext kann außerdem in weitere Funktionen einfließen, beispielsweise in:

- die Erkennung aktiver Bänder einer Station,
- den Chatmember-Score und die Prioritätslisten,
- Band-Upgrade-Hinweise nach einem Logeintrag,
- die Frequenzwahl bei Skeds,
- einen DX-Cluster-Spot aus einer erkannten Richtungsgelegenheit.

Steht die QRG erstmals in der Nachricht, die zugleich eine Richtungsgelegenheit auslöst, wird sie vor der Richtungs- und Spotprüfung verarbeitet. Der daraus erzeugte Spot kann deshalb bereits die Frequenz dieser Nachricht verwenden.

Die Erkennung bleibt eine Textauswertung. KST4Contest kann nicht beweisen, dass die Station noch auf der genannten Frequenz arbeitet oder ob sich eine mehrdeutige Angabe auf einen anderen Zusammenhang bezieht. Genau deshalb werden nackte dreistellige Zahlen ohne Frequenzkontext nicht mehr übernommen.

Konfiguration und unterstützte Fallback-Bänder: [Fallback-Band für relative QRG-Erkennung](de-Konfiguration#fallback-band-für-relative-qrg-erkennung).

Verwendung in der Bandmap eines Logprogramms: [Integrierter DX-Cluster-Server](de-DX-Cluster-Server).

---
## Gearbeitete Rufzeichen, neue Bänder und neue Großfelder

KST4Contest unterscheidet drei Informationen, die im Contest ähnlich aussehen können, aber unterschiedliche Fragen beantworten:

1. Wurde dieses Rufzeichen bereits gearbeitet?
2. Wurde dieses Rufzeichen auf einem bestimmten Band gearbeitet?
3. Wurde das vierstellige Maidenhead-Großfeld bereits gearbeitet – möglicherweise mit einer anderen Station?

Diese Trennung ist notwendig. Ein bereits gearbeitetes Rufzeichen kann auf einem anderen Band weiterhin interessant sein. Umgekehrt kann eine noch nicht gearbeitete Station in einem Großfeld liegen, das bereits im Log steht.

### Worked-Informationen aus dem Log

Die [Log-Synchronisation](de-Log-Synchronisation) übernimmt neue QSOs aus dem Logprogramm. Welche Informationen dabei zur Verfügung stehen, hängt von der verwendeten Schnittstelle ab:

- Der dateibasierte Simplelogfile-Interpreter erkennt nur das Rufzeichen. Er kann deshalb lediglich den globalen Worked-Status setzen.
- Die QSO-UDP-Schnittstellen und der Win-Test-Netzwerk-Listener können zusätzlich das Band übernehmen.
- Enthält das Logpaket einen gültigen Locator, speichert KST4Contest außerdem das gearbeitete vierstellige Großfeld für dieses Band.

Fehlt eine Information im Logpaket, wird sie nicht geraten. Ein QSO ohne Locator erzeugt deshalb keinen Großfeld-Eintrag; ein Simplelogfile-Treffer erzeugt keine bandbezogene Worked-Markierung.

### Bedeutung der Bandspalten

Unter der gemeinsamen Spalte **worked** erscheinen nur die Bänder, die unter **Station → my station uses …** aktiviert wurden. Die Zellen verwenden bewusst kurze Kennzeichen:

| Anzeige | Bedeutung |
|---|---|
| `X` | Das Rufzeichen wurde auf diesem Band gearbeitet. |
| `a` | Die Station bietet dieses Band an, das Band ist noch nicht gearbeitet und das Rufzeichen wurde bisher auf keinem Band gearbeitet. |
| `B+` | Die Station bietet dieses Band an und das Band ist noch nicht gearbeitet. Das Rufzeichen wurde bereits auf einem anderen Band gearbeitet. Ist die getrennte `a`-Anzeige deaktiviert, wird auch ein vollständig neues Rufzeichen als `B+` dargestellt. |
| `o` | Das vierstellige Großfeld der Station wurde auf diesem Band bereits gearbeitet – unabhängig vom Rufzeichen. |
| leer | Für dieses Band liegt keine passende Information vor. Das ist nicht gleichbedeutend mit „nicht QRV“. |

Das `o` ist eine unabhängige Zusatzinformation und kann deshalb mit den anderen Kennzeichen kombiniert werden. Möglich sind beispielsweise `Xo`, `ao` oder `B+o`. Ein einzelnes `o` bedeutet: Das Großfeld wurde auf diesem Band bereits gearbeitet, für das angezeigte Rufzeichen liegt aber weder eine Worked-Markierung noch eine aktuelle Bandmöglichkeit vor.

![Bandbezogener Worked-Status und Worked-Großfelder](worked_band_status.png)

### Wie entsteht eine Bandmöglichkeit?

KST4Contest zeigt `a` oder `B+` nur an, wenn sich eine noch offene gemeinsame Bandmöglichkeit herleiten lässt. Dafür werden folgende Informationen zusammengeführt:

1. die in den Stationseinstellungen aktivierten eigenen Bänder,
2. höchstens 30 Minuten alte QRG-Erkennungen der Gegenstation,
3. eindeutige Bandangaben im Namensfeld der Gegenstation,
4. die pro Band gespeicherten Worked-Markierungen und
5. manuell gesetzte NOT-QRV-Markierungen.

Aktive Chat-Einträge mit demselben normalisierten Rufzeichen werden gemeinsam ausgewertet. Das ist insbesondere bei mehreren Chat-Kategorien oder unterschiedlichen sichtbaren Rufzeichenvarianten wichtig. Eine eindeutige Bandangabe im Namensfeld bleibt dabei so lange nutzbar, wie der betreffende Chat-Eintrag aktiv ist; eine aus einer Nachricht erkannte QRG läuft nach 30 Minuten aus.

Anschließend werden nur die Bänder berücksichtigt, die an der eigenen Station aktiviert, für die Gegenstation bekannt und noch nicht gearbeitet sind. Ein manuelles NOT-QRV-Tag übersteuert die automatisch erkannten Hinweise. Die Chat-Kategorie allein reicht dagegen nicht als Nachweis, dass eine einzelne Station auf einem bestimmten Band QRV ist.

Die globale Worked-Markierung entscheidet nicht darüber, ob eine Bandmöglichkeit besteht. Sie unterscheidet in der Darstellung lediglich zwischen `a` und `B+`. Die eigentliche Bandprüfung arbeitet mit den bandbezogenen Worked-Informationen.

### Bedeutung von `wkdany`

Die Unterspalte **wkdany** fasst den globalen Rufzeichen- und Großfeldstatus zusammen:

| Anzeige | Bedeutung |
|---|---|
| leer | Weder das Rufzeichen noch das vierstellige Großfeld wurden gearbeitet. |
| `x` | Das Rufzeichen wurde auf mindestens einem Band gearbeitet. |
| `o` | Das vierstellige Großfeld wurde auf mindestens einem Band gearbeitet. |
| `xo` | Rufzeichen und Großfeld wurden bereits gearbeitet. |

`wkdany` ist eine bandunabhängige Übersicht. Das kleine `x` darf daher nicht mit dem großen `X` in einer Bandspalte verwechselt werden. Der globale Status wird für die Anzeige und den globalen **wkd**-Filter verwendet, nicht als Ersatz für bandbezogene Worked-Informationen.

### NOT-QRV-Markierungen

Teilt eine Station mit, dass sie auf einem bestimmten Band nicht QRV ist, kann dies im **Further Info**-Bereich der ausgewählten Station markiert werden:

1. Station in der Benutzerliste auswählen.
2. Im Bereich **Not QRV** das betreffende Band aktivieren.
3. **tag not qrv all** nur verwenden, wenn die Station auf keinem der unterstützten Bänder angefragt werden soll.

Angezeigt werden die einzelnen NOT-QRV-Schalter der Bänder, die für die eigene Station aktiviert sind. **tag not qrv all** setzt dagegen alle unterstützten Bänder, auch wenn einzelne davon momentan nicht in der Benutzeroberfläche eingeblendet sind. Die Markierung wird bandbezogen unter dem normalisierten Rufzeichen gespeichert und auf dessen aktive Chat-Varianten übertragen.

![Bandbezogene NOT-QRV-Markierungen im Further-Info-Bereich](not_qrv_controls.png)

NOT-QRV ist eine manuelle Korrektur und hat deshalb Vorrang vor automatisch erkannten QRGs und Bandangaben im Namensfeld. Das betreffende Band wird nicht mehr als `a` oder `B+` angeboten, vom **New bands**-Filter nicht als Gelegenheit gewertet und von den zugehörigen Bandfiltern ausgeblendet.

Im Klartext: Ein erkannter Hinweis bedeutet „wahrscheinlich auf diesem Band aktiv“. Ein manuelles NOT-QRV-Tag bedeutet „für unsere weitere Auswahl nicht auf diesem Band anfragen“. Diese Entscheidung soll nicht durch die nächste erkannte Zahl wieder aufgehoben werden.

### Speicherung und Lebensdauer

Worked-, NOT-QRV- und Großfeldinformationen werden in der internen SQLite-Datenbank gespeichert und beim nächsten Start wieder geladen. Die Einträge laufen nach drei Tagen automatisch ab. Ein Reset vor jedem Contest ist deshalb normalerweise nicht erforderlich.

Ein manueller Reset unter **Workedstn database** entfernt sämtliche Worked-Markierungen, NOT-QRV-Tags und gespeicherten Worked-Großfelder. Die bekannten Rufzeichenzeilen bleiben dabei in der Datenbank erhalten. Einzelheiten: [Worked Station Database Settings](de-Konfiguration#worked-station-database-settings-gearbeitete-stationen-datenbank).


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

## Filter für Worked-Status, neue Bänder und neue Großfelder

Die Filter oberhalb der Benutzerliste greifen auf dieselben Informationen zurück wie die Worked-Spalten:

- **wkd** blendet Rufzeichen aus, die auf mindestens einem Band gearbeitet wurden.
- Die einzelnen Band-Schaltflächen blenden Stationen aus, wenn das Rufzeichen auf diesem Band bereits gearbeitet oder für dieses Band manuell als NOT QRV markiert wurde.
- **New bands** zeigt nur Stationen, für die mindestens ein eigenes aktiviertes, noch nicht gearbeitetes Band bekannt ist. Berücksichtigt werden aktuelle QRG-Erkennungen und Bandangaben im Namensfeld; NOT-QRV hat Vorrang.
- **Only new grids** zeigt nur Stationen, deren vierstelliges Großfeld auf noch keinem Band gearbeitet wurde. Stationen ohne auswertbaren Locator erfüllen den Filter nicht.
- **Grid color** verändert die Liste nicht. Ist die Funktion aktiv, wird das QRA-Feld eines bereits gearbeiteten Großfelds dezent dunkler dargestellt. Neue Großfelder behalten die normale Tabellenfarbe.

Mehrere aktivierte Filter werden gemeinsam angewendet. Eine Station bleibt nur sichtbar, wenn sie alle gewählten Bedingungen erfüllt. Die Filter reagieren unmittelbar auf neue Logeinträge und geänderte NOT-QRV-Markierungen.

Bedienung und Aufbau der Filterleiste: [Benutzeroberfläche – Filter](de-Benutzeroberflaeche#filter).

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

## Automatische Antworten auf Privatnachrichten (ab v1.25)

Nicht jede im ON4KST-Chat eingeloggte Station nimmt am gerade laufenden Contest teil. Trotzdem werden Sked-Anfragen während größerer Contests teilweise unkoordiniert und in großer Zahl an erreichbare Rufzeichen verteilt. Ohne automatische Antwort müssten diese Stationen immer wieder von Hand erklären, dass sie nicht mitfunken oder keine Skeds fahren.

KST4Contest kann darauf mit einem vorher festgelegten Text reagieren. Die eingehende Privatnachricht bleibt dabei sichtbar; sie wird weder blockiert noch verworfen. Davon getrennt lässt sich eine QRG-Antwort aktivieren, die auf typische Fragen wie `qrg?`, `freq?` oder `pse qrg` reagiert.

Bei zwei gleichzeitig geöffneten Chat-Kategorien bleibt der Zusammenhang erhalten: Die Antwort wird in der Kategorie der eingegangenen Nachricht gesendet. Eine QRG-Anfrage erhält außerdem nur die QRG dieser Kategorie und nicht eine Liste aller konfigurierten Frequenzen.

Automatische Antworten benötigen Grenzen. KST4Contest versieht sie daher mit `[KST4C Automsg]`, ignoriert entsprechend gekennzeichnete Nachrichten bei der allgemeinen und QRG-bezogenen Antwort und begrenzt weitere Antworten an dieselbe Station in derselben Kategorie auf eine Nachricht innerhalb von zwei Minuten. Der Schutz gilt gemeinsam für beide Antwortarten.

Im Klartext: Die Funktion verhindert keine Massenanfragen. Sie verhindert aber, dass der Empfänger jede davon einzeln mit derselben Absage beantworten muss. Sie soll keine Unterhaltung simulieren und erst recht keine endlose Diskussion mit einem zweiten automatischen Client beginnen.

Konfiguration, erkannte QRG-Anfragen und genaue Kategorienzuordnung: [Konfiguration – Messagehandling Settings](de-Konfiguration#messagehandling-settings-ab-v125).

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

## Sked-Erinnerungen mit ALERT (ab v1.40)

Für jeden Chatmember kann ein Sked-Erinnerungsdienst mit automatischen Nachrichten aktiviert werden. Konfigurierbare Intervallmuster:

- **2+1 Minuten**: Nachrichten bei 2 min und 1 min vor dem Sked.
- **5+2+1 Minuten**: Nachrichten bei 5, 2 und 1 min vor dem Sked.
- **10+5+2+1 Minuten**: Nachrichten bei 10, 5, 2 und 1 min vor dem Sked.

Zusätzlich zu den Nachrichten an die Gegenstation gibt es eine **akustische und optische Benachrichtigung** für den eigenen Operator, sodass kein Sked vergessen wird.

Aktivierung: FurtherInfo-Panel der entsprechenden Station.

---

## QSO-Monitoring (ab v1.31)

Für ausgewählte Rufzeichen kann KST4Contest gerichtete Nachrichten zusätzlich in der PM-Tabelle anzeigen. Dabei werden sowohl Nachrichten berücksichtigt, die das überwachte Rufzeichen sendet, als auch Nachrichten, die an dieses Rufzeichen gerichtet sind.

Die Nachricht bleibt gleichzeitig in ihrer ursprünglichen Tabelle erhalten und wird im PM-Fenster mit Absender und Empfänger als überwachte Kommunikation gekennzeichnet.

Konfiguration: [Konfiguration – Sniffer-Einstellungen](de-Konfiguration#sniffer-einstellungen-ab-v131)

---

## Win-Test-Integration (ab v1.31, vollständig ab v1.40)

KST4Contest unterstützt [Win-Test](https://www.win-test.com/) vollständig als Logprogramm:

- **Log-Synchronisation**: Gearbeitete Stationen werden automatisch aus Win-Test übernommen und in der Benutzerliste markiert.
- **Frequenz-Auswertung**: Die aktuelle TRX-Frequenz wird aus Win-Test-UDP-Paketen ausgewertet und befüllt die `MYQRG`-Variable.
- **Sked-Übergabe (SKED Push via UDP)**: Vereinbarte Skeds aus KST4Contest können direkt an Win-Test übertragen werden, sodass das Rufzeichen der Gegenstation im Win-Test-Sked-Fenster erscheint.

Details zur Konfiguration: [Konfiguration – Win-Test-Netzwerk-Listener](de-Konfiguration#win-test-netzwerk-listener)

---

## PSTRotator-Interface (ab v1.31, vollständig ab v1.40)

KST4Contest kann die Antennenrichtung direkt über **PSTRotator** steuern. Wenn in der Benutzerliste eine Station ausgewählt wird, kann der Rotator automatisch auf den QTF zur ausgewählten Station gedreht werden.

Konfiguration: [Konfiguration – PSTRotator-Einstellungen](de-Konfiguration#pstrotator-einstellungen-ab-v131)

---

## Band-Upgrade-Hinweis nach einem Logeintrag

Meldet UCXLog oder Win-Test einen neuen Logeintrag mit Bandinformation, prüft KST4Contest, ob die gearbeitete Station noch ein weiteres gemeinsames Band anbietet.

Die Herleitung verwendet dieselben Regeln wie `a`, `B+` und der Filter **New bands**: aktivierte eigene Bänder, aktuelle QRG-Erkennungen, Bandangaben im Namensfeld, bandbezogene Worked-Markierungen und NOT-QRV-Tags. Ausgewertet werden die aktiven Chat-Varianten desselben normalisierten Rufzeichens.

Bleibt mindestens ein gemeinsames, noch nicht gearbeitetes Band übrig, erscheint für ungefähr zwölf Sekunden ein blinkender Hinweis mit Rufzeichen und den betreffenden Bändern, beispielsweise `BAND+ DL0ABC 432, 1296`. Der Tooltip zeigt zusätzlich die bei der Entscheidung berücksichtigten aktivierten, gearbeiteten und als NOT QRV markierten Bänder. Ist die allgemeine Soundausgabe eingeschaltet, wird außerdem ein kurzer Hinweiston abgespielt.

Der einfache Simplelogfile-Interpreter kann diesen Hinweis nicht zuverlässig auslösen, weil er keine Bandinformation für das gerade geloggte QSO liefert.

Konfiguration: [Band-Upgrade-Hinweis nach einem Logeintrag](de-Konfiguration#band-upgrade-hinweis-nach-einem-logeintrag).

Worked-, NOT-QRV- und Großfelddaten laufen nach drei Tagen automatisch ab. Einzelheiten und manueller Reset: [Worked Station Database Settings](de-Konfiguration#worked-station-database-settings-gearbeitete-stationen-datenbank).

---
## Prioritätsscore und Prioritätsliste (ab v1.40)

### Warum wird überhaupt ein Score benötigt?

Eine klassische Chat-Benutzerliste zeigt zunächst nur, welche Stationen gerade eingeloggt sind. Im Contestbetrieb reicht diese Information nicht aus. Der Operator muss zusätzlich abschätzen, welche Station noch nicht gearbeitet wurde, auf welchem Band ein QSO möglich sein könnte, wohin die Antenne zeigt, ob ein passendes Flugzeug verfügbar ist und ob ein vereinbarter Sked unmittelbar bevorsteht.

Bei einer kurzen Liste lässt sich das noch im Kopf erledigen. Mit zunehmender Contestdauer, mehreren Bändern und zwei gleichzeitig verwendeten Chat-Kategorien wird daraus jedoch eine ständig wiederholte Entscheidung.

KST4Contest führt die bereits vorhandenen Informationen deshalb in einem Prioritätsscore zusammen. Der Score beantwortet nicht die Frage, ob ein QSO sicher möglich ist. Er hilft bei der praktisch wichtigeren Frage:

> Welche der aktuell sichtbaren Stationen sollte ich mir als Nächstes ansehen?

### Wann wird eine Station ausgeschlossen?

Vor der eigentlichen Gewichtung prüft KST4Contest, ob überhaupt eine bekannte Bandmöglichkeit besteht. Dafür werden alle aktiven Chat-Einträge desselben normalisierten Basisrufzeichens gemeinsam ausgewertet.

Berücksichtigt werden:

1. die in den Stationseinstellungen aktivierten eigenen Bänder,
2. höchstens 30 Minuten alte QRG-Erkennungen der Gegenstation,
3. eindeutige Bandangaben im Namensfeld ihrer aktiven Chat-Einträge,
4. die pro Band gespeicherten Worked-Markierungen und
5. manuell gesetzte NOT-QRV-Tags.

NOT QRV hat dabei Vorrang vor automatisch erkannten Frequenzen oder Bandangaben.

Sind Bänder der Gegenstation bekannt, aber keines davon ist lokal aktiviert und noch verfügbar, erhält die Station einen Score von `0`. Dasselbe gilt, wenn alle gemeinsam möglichen Bänder bereits gearbeitet wurden.

Fehlen dagegen sämtliche Bandinformationen, wird die Station nicht allein deshalb ausgeschlossen. Eine unbekannte Bandmöglichkeit ist nicht dasselbe wie eine nachweislich unmögliche Bandmöglichkeit. Erst wenn alle eigenen aktivierten Bänder für die Station manuell als NOT QRV markiert wurden, ist auch in diesem Fall keine aktuelle Contestmöglichkeit mehr vorhanden.

Stationen mit einem Score von `0` bleiben in der Benutzerliste sichtbar, erscheinen aber nicht in der Prioritätsliste.

### Welche Informationen erhöhen oder verringern den Score?

Der Score entsteht aus mehreren voneinander unabhängigen Hinweisen. Ein einzelnes Kriterium entscheidet daher normalerweise nicht über den endgültigen Listenplatz.

| Faktor | Wirkung auf die Priorisierung |
|---|---|
| Worked-Status | Ein noch auf keinem unterstützten Band gearbeitetes Rufzeichen erhält eine höhere Ausgangspriorität. Bereits gearbeitete Stationen werden niedriger bewertet, bleiben bei offenen Bandmöglichkeiten aber Kandidaten. |
| Verfügbare Bänder | Mehrere gemeinsam nutzbare und noch nicht gearbeitete Bänder erhöhen die Priorität. Optional kann für Band-Upgrades ein zusätzlicher Boost aktiviert werden. |
| Entfernung | Entfernungen unter 200 km werden niedriger gewichtet. Der Bereich zwischen 200 km und dem konfigurierten maximalen QRB wird bevorzugt. Stationen jenseits des maximalen QRB werden deutlich herabgestuft. Fehlt der QRB, entfällt dieser Faktor. |
| Antennenrichtung | Liegt der QTF zur Gegenstation innerhalb der Hälfte des konfigurierten Antennen-Öffnungswinkels um den aktuellen eigenen QTF, steigt der Score. Je näher die Richtungen zusammenliegen, desto stärker wirkt der Hinweis. |
| AirScout | Mindestens ein aktuell erreichbares Flugzeug erhöht den Score. Eine erwartete AP-Gelegenheit in null, einer oder zwei Minuten wird zusätzlich zeitlich gewichtet. |
| Aktuelle Chat-Aktivität | Eine Nachricht innerhalb der letzten Minute wirkt stärker als eine Nachricht innerhalb der letzten drei Minuten. Mehrere eingehende Zeilen innerhalb des Aktivitätsfensters erhöhen den Score zusätzlich. |
| Positive Signale | Erkannte Angaben wie `QRV`, `READY`, `RGR`, `OK`, `TNX` oder vergleichbare konfigurierte Textmuster werden für einige Minuten als positiver Hinweis berücksichtigt. |
| Antwortverhalten | Reagiert eine Station nach einer eigenen `/cq`-Nachricht schnell mit einer weiteren sichtbaren Chat-Zeile, wirkt sich die gemittelte Reaktionszeit positiv aus. Bleibt eine solche Zeile aus, entsteht nach dem konfigurierten Timeout eine negative Bewertung. |
| Skeds | Ein eingetragener Sked erhöht die Priorität zunächst leicht. Innerhalb der letzten 15 Minuten vor dem Termin steigt der Einfluss kontinuierlich an. Zwischen drei Minuten vor und einer Minute nach dem Termin erhält der Sked eine sehr hohe Priorität. |
| Fehlgeschlagener Versuch | **Sked fail** reduziert den Score der Station stark, bis die Markierung mit **Reset fail** zurückgesetzt oder KST4Contest neu gestartet wird. |

Das standardmäßige Aktivitätsfenster für die Anzahl eingehender Nachrichten beträgt 180 Sekunden. Eine aktuelle Nachricht innerhalb der letzten 60 Sekunden wird noch einmal gesondert bewertet. Der standardmäßige No-Reply-Timeout beträgt 13 Minuten.

Beim Antwortverhalten kann KST4Contest nicht sicher feststellen, ob eine nachfolgende öffentliche oder private Nachricht tatsächlich die Antwort auf die eigene Anfrage war. Jede anschließend empfangene Zeile derselben Station beendet deshalb den laufenden Antwortzeitversuch. Der Wert ist eine praktische Näherung und keine statistisch belastbare Antwortquote.

### Was bedeutet ein eingetragener Sked?

Ein Sked ist eine zeitlich vereinbarte Arbeitsaufgabe. Deshalb übersteuert ein unmittelbar bevorstehender Sked die meisten normalen Aktivitäts- und Entfernungshinweise. Ohne diese Gewichtung könnte eine gerade sehr aktive Station einen vereinbarten Termin aus der Prioritätsliste verdrängen.

Der starke Sked-Boost ist absichtlich auf den Zeitraum von drei Minuten vor bis eine Minute nach dem eingetragenen Termin begrenzt. Ein weiter in der Zukunft liegender Sked bleibt sichtbar, soll den laufenden Betrieb aber noch nicht dominieren.

Die Bewertung wird für das normalisierte Basisrufzeichen vorgenommen. Ein Sked für eine aktive Variante wie `9A0BB-23` beeinflusst daher den gemeinsamen Score der zu `9A0BB` gehörenden Chat-Einträge.

### Wie werden mehrere SSIDs und Chat-Kategorien behandelt?

Aktive Rufzeichen wie `9A0BB-2`, `9A0BB-70`, `9A0BB-23` und `9A0BB-13` bleiben getrennte Chatmember. Dadurch können Nachrichten weiterhin an das vollständige Rufzeichen und die richtige Chat-Kategorie adressiert werden.

Worked-, Band-, NOT-QRV- und Score-Informationen beziehen sich dagegen auf das gemeinsame Basisrufzeichen `9A0BB`. Der Score wird deshalb einmal berechnet und auf alle aktiven Varianten übertragen. Die Benutzerliste kann mehrere getrennte Zeilen mit demselben Score enthalten; in der Prioritätsliste erscheint das Basisrufzeichen nur einmal.

Als konkretes Nachrichtenziel verwendet KST4Contest den zuletzt passenden aktiven Login in der zuletzt verwendeten Chat-Kategorie. Beim Anklicken eines Kandidaten wird anschließend das vollständige Rufzeichen einschließlich Suffix und Kategorie ausgewählt.

### Aktualisierung und Anzeige

Änderungen durch neue Nachrichten, AirScout-Daten, Skeds, Worked-Informationen oder manuelle NOT-QRV- und Sked-fail-Markierungen fordern unmittelbar eine Neuberechnung an. Zusätzlich wird der Score regelmäßig im Hintergrund aktualisiert, weil Aktivitäts-, AP- und Sked-Informationen auch ohne neues Ereignis altern.

Eine kurze Verzögerung von einigen Sekunden zwischen einem Ereignis und der sichtbaren neuen Reihenfolge ist daher normal.

Die Benutzeroberfläche zeigt den Score an drei Stellen:

- als numerisch sortierbare Spalte **Score** in der Benutzerliste,
- für die ausgewählte Station im Bereich **Further Info** und
- als kompakte Liste der beiden derzeit höchstbewerteten Kandidaten mit einem zusätzlichen Fenster für bis zu 15 Kandidaten.

Bedienung: [Prioritätsliste in der Benutzeroberfläche](de-Benutzeroberflaeche#prioritätsliste).

### Was sagt der Score nicht aus?

Der Zahlenwert ist weder eine Erfolgswahrscheinlichkeit noch eine Signalprognose. Ein doppelt so hoher Score bedeutet nicht, dass ein QSO doppelt so wahrscheinlich ist.

Die Berechnung kennt unter anderem nicht:

- die tatsächliche Antennenrichtung der Gegenstation,
- deren momentane Betriebssituation,
- lokale Störungen,
- kurzfristige Ausbreitungsänderungen,
- Geländeabschattungen außerhalb der jeweils angebundenen Funktionen oder
- die Frage, ob eine im Chat aktive Station tatsächlich gerade am Funkgerät sitzt.

Auch bekannte Eingabedaten können veraltet oder missverständlich sein. Eine erkannte Frequenz belegt beispielsweise nur, dass diese QRG kürzlich im Zusammenhang mit der Station aufgetreten ist.

Im Klartext: Der Score ersetzt nicht die Entscheidung des Operators. Er sorgt dafür, dass die dafür bereits vorhandenen Informationen nicht bei jedem Kandidaten erneut im Kopf zusammengesucht werden müssen.

Zugehörige Einstellungen:

- [Aktivierte Bänder](de-Konfiguration#aktivierte-bänder)
- [Antennen-Öffnungswinkel](de-Konfiguration#antennen-öffnungswinkel-antenna-beamwidth)
- [Standard-Maximum-QRB](de-Konfiguration#standard-maximum-qrb)
- [AirScout-Einstellungen](de-Konfiguration#airscout-einstellungen)
- [Band-Upgrade-Hinweis und Priority Boost](de-Konfiguration#band-upgrade-hinweis-nach-einem-logeintrag)

---

## AP-Timeline (ab v1.40)

Eine visuelle Zeitleiste zeigt für jeden möglichen AP-Ankunftsminuten-Slot bis zu 4 hochbewertete Stationen, die per Aircraft Scatter erreichbar wären. Priorisierungskriterien:

- Bevorzugt werden APs mit dem **höchsten Reflexionspotenzial** (nicht unbedingt die schnellste Ankunft).
- Stationen, auf die die eigene Antenne nicht zeigt, werden **transparent** dargestellt.

So kann der Contest-Operator auf einem Blick sehen, welche Stationen wann und über welche Flugzeuge erreichbar sein werden.

---

## Intervall-Beacon

KST4Contest kann wiederkehrende CQ-Nachrichten in den öffentlichen Chat senden. Beide Chat-Kategorien verwenden ein gemeinsames Intervall, besitzen aber jeweils einen eigenen Aktivierungsschalter und Nachrichtentext. Globale Variablen wie `MYQRG`, `SECONDQRG` oder `MYLOCATOR` werden unmittelbar vor jeder Aussendung aktualisiert.

Der Beacon ist für längeres CQ-Rufen auf einer festen Frequenz gedacht. Beim Absuchen oder häufigen Wechseln der QRG sollte er ausgeschaltet werden, damit keine inzwischen falsche Frequenz verbreitet wird. Details: [Konfiguration – Beacon Settings](de-Konfiguration#beacon-settings-automatischer-beacon).
---

## Simplelogfile

Dateibasierte Log-Auswertung per Regex. Details: [Log-Synchronisation](Log-Synchronisation#methode-1-universal-file-based-callsign-interpreter-simplelogfile).

---

## Cluster & QSO der anderen

Ein separates Fenster zeigt den QSO-Fluss zwischen anderen Stationen. Besonders interessant in ruhigeren Nacht-Stunden während des Contests, wenn weniger Verkehr herrscht.

Dieses Fenster kann miniaturisiert werden, wenn es nicht benötigt wird. Zukünftig geplant: Filterung auf Stationen im ausgewählten QTF.

---

## Stationskarte (ab v1.41)

Eine interaktive OpenStreetMap-Karte zeigt die geografische Position aller aktiven Chatmember.

**Funktionen:**

- Stationsmarker mit Rufzeichen-Labels, farblich nach Aktivität und Sked-Status
- **Antennen-Kegel** für die eigene Station
- **Verbindungslinie** zur aktuell ausgewählten Station
- **Maidenhead-Raster** (QRA-Locator-Gitter als Overlay)
- **Wegprofil-Diagramm**: Geländehöhen-Querschnitt zwischen eigener und ausgewählter Station, inklusive Fresnel-Zonen-Analyse und Horizonterkennung
- Mehrere Terrainquellen: **Copernicus GLO-30** (hochauflösendes DEM), **Open-Meteo API**, synthetischer Fallback und **Offline-DEM-Import** für den Betrieb ohne Internetverbindung
- Aircraft-Scatter-Weganalyse verknüpft mit den Geländedaten

Die Karte funktioniert in gepackten Umgebungen (AppImage, Flatpak) ohne Zugriff auf externe CDNs: Die Kartenkacheln werden über einen lokalen Tile-Proxy abgerufen, die Leaflet.js-Bibliothek ist in der Anwendung eingebettet.

---

## Optimierte Nachrichtenverarbeitung / 30.000-Nachrichten-Limit (ab v1.41)

Die internen Chat- und Nachrichtentabellen sind auf **30.000 Einträge** begrenzt. Ältere Nachrichten werden automatisch verworfen, sobald das Limit erreicht wird. Damit bleiben Speicherverbrauch und Darstellungsperformance auch bei mehrtägigen Contest-Betrieb stabil.

---

## Bildschirmgerechte Fenstergröße (ab v1.41)

Beim Programmstart berechnet KST4Contest eine bildschirmgerechte Startgröße für das Hauptfenster:

- Die gespeicherte Fenstergröße aus der letzten Session wird verwendet – aber **niemals größer als der aktuelle Bildschirm**.
- Wenn KST4Contest zuletzt auf einem größeren Monitor betrieben wurde, wird das Fenster automatisch auf die aktuelle Anzeige verkleinert.
- Das UI-Layout ist **kompakter und reaktionsfähiger auf kleineren Bildschirmen**.

Damit werden unbrauchbare, abgeschnittene Fenster beim Wechsel zwischen Geräten oder Monitoren verhindert.
