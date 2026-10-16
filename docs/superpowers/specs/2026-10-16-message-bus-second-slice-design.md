# Entwurf — MessageBusManagementThread, zweite Scheibe (#105)

Freigegeben vom Maintainer am 2026-10-16 (Gespräch). Baut auf der ersten Scheibe auf
(PR #123: Frequenz- und Auto-Antwort-Charakterisierung, reine Entscheidungsfunktionen für
Auto-Antwort, Frequenzerkennung, Richtungs-Chance und DX-Cluster-Spot).

## Warum

`processRXMessage23001` behandelt Chatnachrichten zweimal: im CH-Zweig (Live-Chat, rund
350 Zeilen) und im CR-Zweig (Verlauf beim Anmelden, rund 130 Zeilen). Der CR-Zweig ist eine
nachgeführte Kopie des CH-Zweigs. Jede Korrektur muss zweimal gemacht werden, und was im
CH-Zweig passiert — Töne, Befehle, Auto-Antwort, Tick, DX-Cluster-Spot — ist nur über einen
laufenden Thread mit gemockten Mitspielern erreichbar.

## Ziel

Ein gemeinsamer Ablauf für CH und CR, dessen Entscheidungen als reine Funktionen ohne Thread
testbar sind. **Verhalten bleibt unverändert**, auch dort, wo es eigenartig ist.

## Die vier Unterschiede, die erhalten bleiben

| | CH (live) | CR (Verlauf) |
|---|---|---|
| Frequenzerkennung | vor dem Zuordnen und Veröffentlichen | nach dem Veröffentlichen und dem Diagnose-Log |
| an mich | veröffentlichen, Töne (P, CW, Stimme), `//BELL`, `//VER`-Antwort, Auto-Antwort | nur veröffentlichen |
| zwischen anderen | Winkel-Flag, Tick, DX-Cluster-Spot, Diagnose-Logs | nur Winkel-Flag |
| SERVER-Absender | mit leerer `AirPlaneReflectionInfo` | ohne |

Gleich in beiden: öffentliche Nachricht (Empfängerfeld `0`) wird mit dem „alle“-Empfänger
veröffentlicht; eigene Nachricht an andere bekommt `(>CALL)` vorangestellt; ein
`NullPointerException` beim Zuordnen wird geloggt und verschluckt.

## Bausteine

1. **Charakterisierungstests für CR** durch den echten Thread, wie die erste Scheibe es für
   CH getan hat. Sie halten die vier Unterschiede fest, bevor irgendetwas verschoben wird.
2. **`InboundChatFrame`** — reines Record aus den Feldern eines CH/CR-Frames: Kategorie-Nummer,
   Zeit, Absender-Call, Absender-Name, Text, Empfängerfeld. Fabrikmethode aus `String[]`.
3. **`ChatRoute classifyRoute(String receiverField, String senderCall, String receiverCall,
   String ownCall)`** → `PUBLIC | TO_ME | FROM_ME | BETWEEN_OTHERS`. Rein. Erhält die bekannte
   Eigenart: „an mich“ vergleicht mit Groß-/Kleinschreibung, „von mir“ ohne.
4. **`ChatMessageEffects`** — Entscheidungsobjekt. Eine reine Funktion
   `decide(frame, route, live, context)` liefert, was zu tun ist: Töne, `//BELL`,
   `//VER`-Antwort, Auto-Antwort-Text, Tick, Winkel-Flag, Spot-Entscheidung. Der Kontext
   trägt die Präferenzwerte und den bisherigen Winkel-Zustand des Absenders, keine Objekte mit
   Seiteneffekten. Die vorhandenen reinen Funktionen der ersten Scheibe werden genutzt, nicht
   dupliziert.
5. **Ein Ausführer** wendet die Effekte an (Töne abspielen, in die TX-Queue legen,
   Cooldown setzen, Spot senden, veröffentlichen) — in der heutigen Reihenfolge.
6. **`processChatFrame(String[] fields, boolean live)`** ersetzt CH- und CR-Zweig. `live`
   steuert die vier Unterschiede oben, sonst nichts.

## Bewusst nicht

- Die Eigenarten werden nicht korrigiert; sie werden als Kandidaten für eigene Issues gelistet
  (Groß-/Kleinschreibung bei „an mich“, Reihenfolge der Frequenzerkennung in CR, SERVER ohne
  AirScout-Info in CR).
- Sniffer, Erinnerungen und Skeds im `ChatController` sind ein eigenes Thema.
- Andere Frame-Typen (Benutzerliste, LOC, DX-Cluster, UE …) bleiben, wie sie sind.

## Tests

- CR-Charakterisierung durch den Thread (Schritt 1); die vorhandenen CH-Tests der ersten
  Scheibe bleiben unverändert grün — sie sichern das Zusammenlegen ab.
- `InboundChatFrame`, `classifyRoute` (alle vier Routen, beide Schreibweisen-Eigenarten),
  `decide` (je Route live und Verlauf, mit und ohne Töne/Befehle/Auto-Antwort/Spot).
- Voller Build grün; Testzahlen aus den XML-Berichten.
