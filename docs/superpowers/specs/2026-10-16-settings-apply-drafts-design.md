# Entwurf — Einstellungen frei bearbeiten, mit „Übernehmen" anwenden

Freigegeben vom Maintainer am 2026-10-16. Baut auf PR #128 auf (kein Festschreiben beim
Öffnen, gesperrte Anmeldefelder während einer Sitzung).

## Warum

Bis hierher schrieb jedes Steuerelement des Einstellungsfensters beim Ändern sofort in
`ChatPreferences`, und manche lösten dabei sofort eine Wirkung in der laufenden Sitzung aus:
der Bakentimer startete neu, ein UDP-Listener wurde neu gebunden, der lokale
DX-Cluster-Server gestartet oder gestoppt. Das war die Übernahme des JavaFX-Fensters, und es
hat zwei Folgen, die im Contest stören:

- **Jeder Zwischenstand wirkt.** Wer einen Port von 9871 auf 9872 ändert, tippt über 987 und
  98 hinweg; wer zwei zusammengehörige Häkchen setzt, hat zwischendurch einen halben Stand
  aktiv. Ein Fehlgriff wirkt sofort und lässt sich nicht verwerfen.
- **Es gibt kein „doch nicht".** Das Fenster hat keinen Weg zurück. „Übernehmen/Schließen"
  schloss nur, und wer etwas ausprobiert hatte, musste es von Hand zurückdrehen.

Das neue Verhalten ist das, das ein Operateur von einem Einstellungsdialog erwartet:
bearbeiten, dann anwenden oder verwerfen.

## Verhalten

1. Das Öffnen des Fensters beginnt einen Entwurf. Felder, Häkchen und Auswahlen aller Reiter
   schreiben in den Entwurf statt in die Einstellungen; das Fenster zeigt den Entwurfswert.
   Bearbeiten geht auch während einer Sitzung.
2. **Übernehmen/Schließen** wendet alle ausstehenden Änderungen an — Schreibvorgang samt
   Wirkung — und schließt das Fenster.
3. **Einstellungen speichern** wendet zuerst an, dann schreibt es die Datei. Das Fenster
   bleibt offen.
4. Das **X** der Titelleiste (und das Schließen über das Fenster-Menü) fragt bei
   ausstehenden Änderungen: Übernehmen, Verwerfen oder zurück zum Fenster. Ohne ausstehende
   Änderungen schließt es wie bisher.
5. Ungültige Eingaben werden wie bisher am Feld abgewiesen und erreichen den Entwurf nie.
   Steht beim Übernehmen, Speichern oder Verbinden noch ungültiger Text in einem Feld (rot),
   gilt das als Abweisung: nichts wird angewendet, das Fenster bleibt offen, die Meldung nennt
   das Feld. Sonst ginge still der letzte gültige Wert in Kraft, den der Operateur nicht mehr
   sieht — und Verwerfen wäre nicht mehr möglich, weil die übrigen Änderungen schon wirkten.
   Erst wenn kein Feld etwas abweist, wird angewendet.
6. **Verbinden** wendet ebenfalls zuerst an. Sonst verbände der Knopf mit dem gespeicherten
   statt mit dem sichtbaren Serverport oder Zweitchat-Häkchen — der Operateur sieht den einen
   Wert und bekommt den anderen.
7. **Profilwechsel und Programmende** stellen dieselbe Frage, bevor sie weitergehen
   (`SettingsWindow.settleThen`): Übernehmen wendet die Änderungen auf das **bisherige**
   Profil an — das, in dem sie gemacht wurden — und setzt den Wechsel bzw. das Beenden fort;
   Verwerfen setzt ohne sie fort; Zurück bricht Wechsel bzw. Beenden ab. Ohne offene
   Änderungen geht es sofort und wie bisher weiter. Erst der anschließende Abbau
   (`SettingsWindow.close`) und ein JVM-Shutdown-Hook verwerfen ohne Frage.
8. **Scheitert beim Anwenden ein Schreibvorgang oder eine Wirkung**, laufen alle übrigen
   trotzdem; die Fehler stehen danach in einer Problem-Meldung, das Fenster bleibt offen. Eine
   Einstellung, deren Schreibvorgang scheiterte, bleibt im Entwurf, ihre Wirkung läuft nicht.
   Speichern schreibt die Datei, wenn alle Schreibvorgänge gelangen, auch wenn eine Wirkung
   scheiterte — deren Einstellung ist gespeichert und gehört in die Datei.

## Was sofort wirkt, wie bisher

- **Farben** (Reiter Farben) und **Sprache** (Reiter GUI). Beide sind sichtbar, sobald sie
  wirken; ein Entwurf hätte dort nichts zu zeigen.
- **Anmeldefelder** des Reiters Station: Rufzeichen, beide Namen, Locator, Passwort, beide
  Chatkategorien. Sie sind laut #128 während einer Sitzung gesperrt; ohne Sitzung wirken sie
  ohnehin erst beim nächsten Verbinden.
- **Aktionen**: Profil anlegen, duplizieren, umbenennen, löschen, aktivieren; die Aktionen
  der Worked-Datenbank; der Test-Spot des DX-Clusters; die „Choose…"-Dateiauswahl;
  der Export des Stylesheets. Das sind Handlungen, keine Werte.
- **Reparaturen gespeicherter Werte** (`repairStoredSettings`), einmal beim Öffnen des
  Fensters, bevor ein Reiter gebaut wird: eine unlesbare Ausweichband-Vorgabe des
  DX-Clusters wird zu 144, und die Win-Test-Broadcastadresse wird, solange sie noch der
  Platzhalter 255.255.255.255 ist, durch die erkannte Subnetz-Adresse ersetzt. Beides schrieb
  schon das JavaFX-Fenster beim Aufbau, beides ist keine Eingabe des Operateurs und hat
  nichts zum Übernehmen oder Verwerfen — darum direkt in `ChatPreferences`. Früher geschah
  das in den Reiterzuständen, eines davon beim Lesen während der Komposition; Compose liest
  wann es will, und ein Schreiben dort ist leicht zu übersehen. Jetzt gibt es dafür einen
  bekannten Zeitpunkt, und kein Reiterzustand schreibt beim Lesen oder Bauen.

Unverändert bleiben das Format von `preferences.xml` und die Knöpfe der Verbindungsleiste.

## Mechanismus

`SettingsDraft` ist ein Fensterobjekt, das einmal pro Öffnen entsteht und an alle
Reiterzustände geht. Er hält ausstehende Änderungen, eine pro Einstellung, unter einem
Schlüssel:

```kotlin
draft.stage(key, value, live = { prefs.getX() }, write = { prefs.setX(it) },
            effect = ::restartSomething, effectKey = "something")
draft.valueOf(key) { prefs.getX() }   // Entwurfswert oder der gespeicherte
draft.commitTypedEntries()            // getippter Text der Felder → Entwurf, oder Abweisungen
draft.apply()                          // Schreiben, dann Wirkungen; liefert die Fehlschläge
draft.discard()
draft.hasPendingChanges                // Compose-Zustand
```

- **Lesen geht durch den Entwurf.** Die Getter der Reiterzustände lesen
  `draft.valueOf(key) { prefs… }`. Die Ablage ist Compose-Zustand, also zeichnet ein Feld nach
  einer Änderung neu — die alten `revision`-Zähler der Reiter werden dafür nicht mehr gebraucht,
  schaden aber nicht.
- **Zurück auf den gespeicherten Wert heißt: nichts steht aus.** Ein Wert gleich dem
  gespeicherten nimmt den Eintrag heraus. Ein Häkchen hin und zurück lässt keine Frage beim
  Schließen zurück und keinen Neustart beim Übernehmen.
- **`Form.committed`** bleibt wie in #128: Fokusverlust oder Enter rufen `commit`, und das
  schreibt jetzt in den Entwurf. `current` liest über den Reiterzustand den Entwurfs- oder den
  gespeicherten Wert. Damit gelten die drei Zusagen von #128 weiter: kein Festschreiben beim
  Öffnen, kein doppeltes, kein verlorenes.
- **Listen** (Kurzbefehle, Textbausteine, beobachtete Rufzeichen) halten ihren Arbeitsstand
  schon in `EditableListState`; der Entwurf merkt sich nur, dass die Liste geändert ist, und
  `apply` füllt den Roster aus diesem Stand.

## Reihenfolge beim Anwenden

1. **Erst alle Schreibvorgänge**, in der Reihenfolge, in der die Einstellungen zum ersten Mal
   geändert wurden.
2. **Dann die Wirkungen**, in derselben Reihenfolge, **jede Wirkung einmal**. Wirkungen tragen
   einen Schlüssel; teilen sich zwei Einstellungen einen, läuft er einmal.
3. Dann ist der Entwurf leer.

Schreiben vor Wirken ist nicht Geschmack. Win-Test-Listener ein **und** neuer Port: liefe die
Wirkung zwischen den Schreibvorgängen, bände der Listener noch den alten Port. Erst alle
Werte, dann die Wirkungen heißt: jede Wirkung sieht den fertigen Stand.

Einmal pro Schlüssel ist ebenfalls nicht Geschmack. Zwei kurz nacheinander neu gebundene
Sockets auf demselben Port können am noch nicht freigegebenen ersten scheitern. Die
geteilten Schlüssel:

| Schlüssel | Einstellungen | Wirkung (auf dem fertigen Stand) |
|---|---|---|
| Win-Test-Listener | Listener ein/aus, Port | an → Neustart, aus → Stopp |
| DX-Cluster-Server | Server ein/aus, Port | nur angemeldet: an → Neustart, aus → Stopp |
| Eigene-QRG-Folger | UCXLog-QRG, Win-Test-QRG | Folger an, wenn eine der beiden Quellen an ist |
| Bakentimer | Intervall | Neustart |
| Mitschnitt in Datei | Häkchen | Fehlerprotokoll und Recorder öffnen oder schließen |
| Kurzbefehle / Textbausteine | Listen | Knöpfe bzw. Kontextmenüs neu bauen |

Die geteilten Wirkungen gleichen den laufenden Dienst an den fertigen Stand an, statt zu
fragen, welche der beiden Einstellungen sich geändert hat. Das deckt alle Fälle der sofortigen
Änderung ab: Einschalten startet (ein Neustart eines nicht laufenden Dienstes ist ein Start),
Ausschalten stoppt, ein neuer Port bei laufendem Dienst bindet neu. Ein neuer Port bei
abgeschaltetem Dienst ruft einen Stopp auf einem nicht laufenden Dienst; der tut nichts, wie
zuvor die ausgebliebene Wirkung.

Zwischen verschiedenen Schlüsseln gibt es keine Abhängigkeit; ihre Reihenfolge ist die der
ersten Änderung und damit dieselbe, in der die sofortigen Änderungen sie bisher ausgelöst
hätten.

## Fäden

Alles läuft auf dem AWT-Ereignisfaden, wie bisher die sofortigen Änderungen: Knopfdruck,
Dialogwahl, Menüeintrag. Gewartet wird dort nie. `requestClose` und `settleThen` übernehmen
den getippten Text gleich auf dem Ereignisfaden — ein minimiertes Fenster zeichnet nicht, eine
Entscheidung bis zum nächsten Bild könnte ewig warten — und gehen ohne offene Änderungen sofort
weiter. Gibt es etwas zu fragen, heben sie das Fenster nach vorn und setzen nur einen Zustand;
die Frage erscheint beim nächsten Bild, und die Fortsetzung (Wechsel, Beenden) läuft aus dem
Rückruf des Dialogs, über `EventQueue.invokeLater`, nicht im Klick-Handler selbst.

## Prüfung

Pro Einstellung (`DraftedSettingsTableTest`, mit echtem Entwurf): unverändert nach der
Eingabe, geändert nach Übernehmen, unverändert nach Verwerfen. Pro Fenster: Speichern wendet
an und schreibt; X, Profilwechsel und Programmende fragen bei offenen Änderungen; Zurück
bricht ab; ungültiger Text und Fehlschläge halten das Fenster offen; Bearbeiten während einer
Sitzung geht.
