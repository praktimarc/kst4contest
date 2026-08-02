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

## Band-Alert bei neuen QSOs (ab v1.40)

Wenn eine Station geloggt wird, prüft KST4Contest automatisch, ob diese Station im Chat weitere aktive Bänder angezeigt hat, auf denen man selbst ebenfalls QRV ist. Falls ja, erscheint ein **Hinweis-Alert**, damit keine Multi-Band-Möglichkeit übersehen wird.

---

## Worked-Tag-Lebensdauer (ab v1.40)

Gearbeitete Stationen werden nach **3 Tagen** automatisch aus der Datenbank entfernt. Ein manuelles Zurücksetzen der Worked-Datenbank vor jedem Contest ist damit nicht mehr zwingend notwendig – die Datenbank hält sich selbst aktuell.

---

## Chatmember Score-System / Prioritätsliste (ab v1.40)

KST4Contest berechnet automatisch eine **Prioritätsbewertung** für jeden aktiven Chatmember. Der Score setzt sich zusammen aus:

- Antennenrichtung der Gegenstation (zeigt sie auf mich?)
- QRB (Entfernung)
- Aktivitätszeit und Nachrichtenanzahl
- Aktive Bänder und Frequenzen
- AP-Verfügbarkeit (AirScout)
- Sked-Richtung
- Sked-Erfolgsrate und Skedfail-Markierungen

Die Top-Kandidaten werden in einer eigenen Prioritätsliste hervorgehoben und helfen, im Contest-Stress die wichtigsten Stationen nicht zu übersehen.

Stationen, bei denen ein Sked gescheitert ist, können über den **Skedfail-Button** im FurtherInfo-Panel markiert werden – das senkt ihren Score vorübergehend.

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
