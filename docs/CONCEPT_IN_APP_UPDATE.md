# Konzept: In-App-Update für KST4Contest

> Status: Konzept, noch nicht umgesetzt. Zielrelease **1.50**. Alle Zeilenanker beziehen sich auf `nextMajorRelease/version1_50` (Version 1.50.0) und wurden gegen den Branch verifiziert.

## Ziel

Stable-Updates werden nach Zustimmung des Nutzers heruntergeladen. KST4Contest beendet sich danach kontrolliert und startet den plattformspezifischen Aktualisierungsvorgang. Updates dürfen den Programmstart nicht blockieren und während eines Contests nicht erzwungen werden.

## Ausgangslage

Der vorhandene Mechanismus erfüllt das nicht:

- `UpdateChecker.downloadLatestVersionInfoXML()` (`src/main/java/kst4contest/controller/UpdateChecker.java:54-71`) benutzt `new URL(...).openStream()` + `Files.copy(..., REPLACE_EXISTING)` **direkt auf die Zieldatei** — ohne Timeouts, ohne HTTP-Statusprüfung, ohne temporäre Datei. Ein Fehlschlag beschädigt zusätzlich die lokale Feed-Kopie.
- Der Aufruf steht **synchron im Konstruktor** von `ChatController` (`ChatController.java:2937-2941`); `ChatController` wird in `Kst4ContestApplication.start(Stage)` erzeugt (`Kst4ContestApplication.java:6971`, `start` ab `:6914`). Der Netzwerkaufruf läuft damit unbegrenzt **auf dem JavaFX-Application-Thread während des Starts** — genau der Punkt „darf den Programmstart nicht blockieren".
- `parseUpdateXMLFile()` (`UpdateChecker.java:73-236`) liest Kindelemente **positionsabhängig** über einen Zähler in `String[7]` bzw. `String[3]` (`:144-145`, `:182-183`). Ein achtes Kindelement in `<changeLog>` erzeugt eine `ArrayIndexOutOfBoundsException` und killt den gesamten Check.
- Die Anzeige ist ein eigenes `Stage` mit `setAlwaysOnTop(true)` (`Kst4ContestApplication.java:9162`), inline in `start()` aufgebaut (`:9152-9294`) und beim Start automatisch geöffnet (`:9280`). Es gibt **keinen Menüpunkt** zum erneuten Prüfen. Ein Catch-All bei `:9289-9292` schaltet die Funktion stillschweigend ab, sobald der Feed fehlt oder nicht passt.
- Toter Code: `main()` (`:27-34`) und ein Testdatenblock (`:208-234`).

## Bestätigte Festlegungen

| Punkt | Entscheidung |
|---|---|
| Contest-Schutz | Hinweis wird unterdrückt, solange eine ON4KST-Verbindung besteht |
| Update-Starter | Kein eigenes Starter-Programm; Installer/Paketverwaltung wird direkt gestartet, danach beendet sich KST4Contest |
| Flatpak | Nur Hinweis mit Update-Befehl, kein Download, kein Artefakt |
| AppImage | Kontrollierter Dateiaustausch, kein `AppImageUpdate` |
| Kanal | Ausschließlich Stable |
| Feed-Auslieferung | Bleibt wie bisher: der Webserver zieht das Repo alle 5 Minuten per Git und baut die Website selbst |
| Zuschnitt | Drei Stufen |
| Signatur | SignPath Foundation, als **optionaler additiver** CI-Schritt; unsigniert bleibt lauffähig |
| Windows-ZIP | Bleibt als portable Alternative |

## Was unverändert bleibt

1. **Alte Clients (≤ 1.44) am erweiterten Feed.** `UpdateChecker` sucht per `getElementsByTagName` nach `latestVersion`, `versionNumber`, `semanticVersion`, `adminMessage`, `majorChanges`, `latestVersionPathOnWebserver`, `changeLog`, `bug`. Das ist eine **Descendant-Suche** — deshalb kommt der neue Block als *Geschwister* neben `<latestVersion>` und benutzt ausschließlich neue Elementnamen.
2. **`<changeLog>` behält exakt seine 7 Kinder** (`changedVersionNumber, date, description, added, changed, fixed, removed`) in unveränderter Reihenfolge, `<bug>` seine Struktur. `<needUpdateSinceLastVersion>`, `<roadmap>`, `<bugsReported>` bleiben unangetastet.
3. **`~/.praktiKST/`** — nur ein neues Unterverzeichnis `updates/`. `kst4ContestVersionInfo.xml` behält Name und Ort, damit ein Downgrade auf 1.44 funktioniert.
4. **Windows-ZIP als Portable** — die App verändert ihr eigenes Verzeichnis nie und führt für diesen Typ keinen Installer aus.
5. **Kein Eingriff** in Flatpak-, AUR-, DEB-, RPM- oder Arch-Installationen; **kein `sudo`/`pkexec`** aus der Anwendung heraus.
6. **Offlinebetrieb** — ein fehlgeschlagener Check ist stumm (nur `java.util.logging`), der Programmstart ist davon vollständig entkoppelt.
7. **Legacy-Vergleich** — `APPLICATION_CURRENTVERSIONNUMBER` und `<versionNumber>` bleiben als Fallback; `VersionUtils.compareStableVersions` wird wiederverwendet und nicht verändert.
8. **Bei unbekannter Installationsart** wird ausschließlich die Downloadseite geöffnet.

---

## Stufe 1 — Feed, Paketkennzeichnung, Prüfdienst, Hinweis-UI

Kein Download, kein Installationsstart.

### 1a. Installationsart zur Buildzeit kennzeichnen

Es existiert heute **keinerlei** Build-Metadatum: kein `--java-options`, keine `.properties` unter `src`, kein Resource-Filtering in `pom.xml`, keine Manifest-Einträge.

**Gewählter Mechanismus:** jpackage-Flag `--java-options -Dkst4contest.packageType=<typ>`.

| Alternative | Warum nicht |
|---|---|
| Gefilterte `.properties`-Resource | **Alle Pakettypen entstehen aus demselben Maven-Artefakt**; die Unterscheidung fällt erst im jpackage-Aufruf. Lokale Builds und Tests bekämen einen falschen Marker. |
| Manifest-Eintrag | Gleiches JAR-Problem, mehr Bewegungsteile. |
| Verzeichnis-/Prozessheuristik | Laut Zielbild ausdrücklich ausgeschlossen; bei ZIP-Portable ohnehin frei verschiebbar. |
| Reines Env-Sniffing | Erkennt Flatpak und AppImage, aber nicht deb/rpm/arch/zip/msi. |

Vorteile: keine Auswirkung auf `mvn`/Tests/lokale Entwicklung (Property fehlt → `UNKNOWN` → sicherer Fallback); der Wert landet in `lib/app/<Name>.cfg` des app-image und wird dadurch vom AUR-`-bin`-Repack (`packaging/aur/kst4contest-bin/PKGBUILD` macht nur `cp -a usr`) und vom Flatpak-Build (bindet das app-image als `type: dir`-Source ein) **automatisch mitkopiert**.

Werte: `windows-zip`, `windows-msi`, `macos-dmg`, `linux-appimage`, `flatpak`, `linux-deb`, `linux-rpm`, `linux-arch`.

**15 Aufrufstellen:**

| Datei | Zeilen | Wert |
|---|---|---|
| `.github/workflows/tagged-release.yml` | 53 / 104 / 184 / 238 / 292 / 403 | `windows-zip` / `linux-appimage` / `linux-deb` / `linux-rpm` / `linux-arch` / `flatpak` |
| `.github/workflows/nightly-artifacts.yml` | 69 / 127 / 214 / 275 / 338 / 458 | dieselbe Reihenfolge |
| `packaging/macos/build-signed-dmg.sh` | 72 | `macos-dmg` |
| `packaging/aur/kst4contest/PKGBUILD` | 44 | `linux-arch` |
| `packaging/aur/kst4contest-git/PKGBUILD` | 37 | `linux-arch` |

`packaging/aur/kst4contest-bin/PKGBUILD` erbt korrekt — **keine Änderung**.

PowerShell-Syntax (tagged-release:53, nightly:69) braucht einfache Anführungszeichen, sonst frisst PowerShell das `-D`:
`--java-options '-Dkst4contest.packageType=windows-zip'`

**Auflösung zur Laufzeit** (`RuntimePackageTypeResolver`), bewusst konservativ:

1. `System.getProperty("kst4contest.packageType")`; unbekannter Wert → weiter.
2. `/.flatpak-info` vorhanden **oder** `FLATPAK_ID` gesetzt → `FLATPAK`. Diese Prüfung **überschreibt** die Property — eine Fehlklassifikation richtet ausgerechnet im Flatpak den größten Schaden an (Download in eine Sandbox, die den Host nie aktualisieren kann).
3. `APPIMAGE` gesetzt → `LINUX_APPIMAGE`.
4. sonst `UNKNOWN`.

### 1b. Feed erweitern

Der Feed wird **von der Website** erzeugt (`website/src/_data/versionInfo.js`, ausgeliefert über `website/src/version-info.njk`), gespeist aus der GitHub-Releases-API — nicht aus den Build-Jobs. Der Webserver zieht das Repository alle 5 Minuten per Git und baut selbst; eine geänderte `versionInfo.js` wird dadurch automatisch wirksam, **ohne** Upload-Schritt in den Workflows.

**Platzierung: neues Top-Level-Element als Geschwister von `<latestVersion>`.** Ein Kindelement *innerhalb* `<latestVersion>` wäre riskant, weil der alte Parser dort per Descendant-Suche liest; als Geschwister ist jede Kollision strukturell ausgeschlossen.

```xml
<updatePackages formatVersion="1" semanticVersion="1.45.0">
    <updatePackage>
        <packageType>linux-deb</packageType>
        <operatingSystem>linux</operatingSystem>
        <architecture>x86_64</architecture>
        <fileName>KST4Contest-v1.45.0-debian-amd64.deb</fileName>
        <downloadUrl>https://github.com/praktimarc/kst4contest/releases/download/v1.45.0/…</downloadUrl>
        <fileSizeBytes>142335488</fileSizeBytes>
        <sha256>9f2c…</sha256>                      <!-- 64 hex, klein -->
        <signatureState>unsigned</signatureState>    <!-- unsigned | authenticode | apple-notarized | gpg-repo -->
        <releaseNotesUrl>…/releases/tag/v1.45.0</releaseNotesUrl>
        <installHint>package-manager</installHint>   <!-- run | package-manager | portable | hint-only -->
    </updatePackage>
    <updatePackage>
        <packageType>flatpak</packageType>
        <operatingSystem>linux</operatingSystem>
        <architecture>x86_64</architecture>
        <signatureState>gpg-repo</signatureState>
        <releaseNotesUrl>…/releases/tag/v1.45.0</releaseNotesUrl>
        <installHint>hint-only</installHint>
        <hintCommand>flatpak update de.x08.KST4Contest</hintCommand>
        <!-- bewusst ohne downloadUrl/sha256 -->
    </updatePackage>
</updatePackages>
```

**Datenherkunft in `versionInfo.js`:**

- `fileName`, `downloadUrl`, `fileSizeBytes` stehen **bereits in der vorhandenen `/releases`-Antwort** (`release.assets[]` mit `name`, `browser_download_url`, `size`) — kein zusätzlicher API-Aufruf. Aktuell werden die Assets weggeworfen.
- `packageType`/`operatingSystem`/`architecture` über einen deterministischen Klassifikator auf die Assetnamen, die die Workflows fest vergeben (`tagged-release.yml:783-791`): `-windows-x64.zip`, `-linux-x86_64.AppImage`, `-debian-amd64.deb`, `-fedora-x86_64.rpm`, `-archlinux-*.pkg.tar.zst`, `-macos-*.dmg`. `.flatpakref` erzeugt keinen Download, sondern den synthetischen Hint-Eintrag. Manuals und der Feed selbst werden übersprungen.
- **`sha256` muss der Release-Workflow erzeugen.** Im Job `release-tag` liegen alle Artefakte bereits unter `release-assets/*`, bevor `ncipollo/release-action` läuft. Dort nach den `download-artifact`-Schritten und **vor** „Create tagged release":
  ```yaml
  - name: Compute release asset checksums
    run: |
      cd release-assets
      find . -type f ! -name SHA256SUMS.txt -print0 \
        | xargs -0 sha256sum | sed 's#\./##' > SHA256SUMS.txt
  ```
  und `release-assets/SHA256SUMS.txt` in die `artifacts:`-Liste (`:783-791`) aufnehmen. Die bestehende Reihenfolge (Release zuerst, dann Website-Build, `:771-825`) bleibt korrekt — die Summen liegen zum Zeitpunkt des Website-Builds bereits am Release. `versionInfo.js` lädt die Datei **nur für das neueste Stable-Release** und baut daraus eine `filename → sha256`-Map; das ist ein Download-URL-Abruf und belastet das API-Ratelimit nicht.
- `signatureState` aus einer statischen Tabelle je packageType (`macos-dmg → apple-notarized`, `flatpak → gpg-repo`, Rest `unsigned`). In Stufe 3 wird `windows-msi` nur dann auf `authenticode` gesetzt, wenn ein signiertes Asset tatsächlich existiert — der Feed darf nicht lügen, wenn der SignPath-Schritt übersprungen wurde.

**Robustheit:** Weil der Server unabhängig vom Release-Workflow baut, kann er einen Feed erzeugen, bevor `SHA256SUMS.txt` am Release hängt. Fehlt `<sha256>`, gilt das Artefakt als **nicht automatisch ladbar** — der Client öffnet nur die Downloadseite. Ein Artefakt ohne Prüfsumme wird nie geladen und nie gestartet. Die bestehende `try/catch`-Klammer in `versionInfo.js` bricht den Website-Build bei einem unvollständigen Feed ab; die neue Sektion darf **nicht** so scharf sein — bei Problemen wird `<updatePackages>` weggelassen, der Rest des Feeds bleibt gültig.

**Mitzuziehen:**
- `website/scripts/validate-version-info.js` — bekannter `packageType`; `sha256` matcht `^[0-9a-f]{64}$`; `downloadUrl` beginnt mit `https://github.com/praktimarc/kst4contest/releases/download/`; `fileSizeBytes` numerisch > 0. **Zusätzlich neu und wichtig:** `<changeLog>` muss weiterhin exakt die 7 Kinder in der Altreihenfolge haben — das nagelt die Alt-Client-Kompatibilität fest.
- `website/test/version-info.test.js` — Klassifikator, SHA256SUMS-Parsing, „Release ohne Assets".

**Warnung, nicht Teil dieser Umsetzung:** `.github/workflows/test-publish-version-xml.yml` (ungetrackt, nur `workflow_dispatch`) erzeugt mit einem eigenen Python-Generator einen konkurrierenden Feed **ohne** `<semanticVersion>` und lädt ihn per WebDAV hoch. Würde er ausgelöst, fielen alle Clients auf den kaputten Double-Vergleich (`1.41.1` → `1.411`) zurück.

### 1c. Prüfdienst neu bauen

Neues Package `kst4contest.service.update` (konsistent zum vorhandenen `kst4contest/service/path/`).

| Datei | Verantwortung |
|---|---|
| `model/PackageType.java` | Enum + `feedId()`, `fromFeedId()` (null/unbekannt → `UNKNOWN`, wirft nie), `updateStrategy()` |
| `model/UpdatePackageInfo.java` | Transport-DTO, **explizite Klasse, kein Record** (AGENTS.md) |
| `model/UpdateCheckResult.java` | Ergebnis-DTO inkl. `resultSource` (`NETWORK`/`CACHE`/`NONE`) |
| `service/update/UpdateFeedParser.java` | **Reiner Parser**, kein IO, kein Netz |
| `service/update/UpdateFeedClient.java` | **Nur HTTP**, injizierbarer `HttpClient` + Timeouts |
| `service/update/UpdateCheckService.java` | Orchestrierung, injizierbare Abhängigkeiten + `Clock` |
| `service/update/RuntimePackageTypeResolver.java` | Installationsart, Test-Konstruktor für Property/Env/Pfad |

Muster: `OpenMeteoTerrainProfileProvider.java:76-88` (injizierbarer Client, konfigurierbare Timeouts), `TerrainPackageDownloader.java:107-145` (Temp-Datei → Statusprüfung → `Files.move`).

Verhalten:

- Aufruf **asynchron im Daemon-Thread, erst nach `primaryStage.show()`** — nie vorher. `ChatController.java:2937-2941` entfällt.
- Ergebnis über eine `ObjectProperty<UpdateCheckResult>` im `ChatController`, gesetzt via `Platform.runLater`. Muster: `lastUiReminderEvent` (`ChatController.java:4236-4243`). Der Worker fasst **keine** `ObservableList` an.
- Versionsvergleich über `VersionUtils.compareStableVersions`. **Achtung:** `parseVersion` wirft bei leerem oder nicht-numerischem Input (`IllegalArgumentException`/`NumberFormatException`) — der Dienst muss das fangen und auf den Double-Pfad zurückfallen. Heute ist das in der View (`:9266-9278`) nicht abgesichert.
- Ausschließlich Stable. Fehlende Verbindung erzeugt **keine** Meldung, nur einen Logeintrag über `java.util.logging` statt `System.out`. Höchstens eine Meldung je Sitzung. Bei Netzfehler Rückfall auf die zuvor gecachte Datei.

**Positionales Parsing entschärfen:** Primärpfad namensbasiert (`readChildText(element, "changedVersionNumber")` …), Ergebnis weiterhin als `String[7]` in derselben Reihenfolge, damit der TreeView-Aufbau im Update-Fenster unverändert bleibt. Kompatibilitätspfad für uralte Feeds behält den positionalen Walk, aber mit `if (counter >= entry.length) break;`. Präfixe (`"Date: "`, `"Desc: "`) exakt erhalten. Bei `<bug>` ist `aChangeLogEntry[2]` heute uninitialisiert → explizit `""`.

`UpdateChecker.java` wird durch den Dienst ersetzt.

### 1d. Hinweis-UI

Wiederverwendet wird das etablierte Muster `initSkedWarnIndicatorButton()` (`Kst4ContestApplication.java:5790-5807`) und `initBandUpgradeIndicatorButton()` (`:5965`): versteckt per Default, `managedProperty().bind(visibleProperty())`, eingehängt in `flwpne_StatusBar` (Aufbau `:7158`, `setTop` `:7161`, Indicator-Buttons `:7163`, `:7166`, `:7189`).

Abweichung: **nicht** `setMouseTransparent(true)` — der Button ist klickbar und öffnet ein `ContextMenu`.

- Text: `KST4Contest 1.x.y is available`
- Aktionen: Änderungen anzeigen · Update herunterladen (Stufe 1: deaktiviert bzw. „Release-Seite öffnen") · für diese Sitzung schließen
- **Contest-Regel:** `visible = updateAvailable && !dismissedForSession && Verbindungszustand != ONLINE`. `onConnectionStateChanged(...)` (`:12566`) ruft zusätzlich `refreshUpdateHintVisibility()` im vorhandenen `Platform.runLater`-Block — der Hinweis verschwindet beim Login in den Chat und kommt beim Trennen zurück.
- FX-Thread-Guard-Idiom aus `:5833` übernehmen.
- **Neuer Menüpunkt** im Info-Menü (Info-Menü, `helpMenu.getItems().addAll(...)` `:5774`): „Check for updates…" und „Show update details…". Damit existiert erstmals ein manueller Re-Check.
- Das bestehende `stage_updateStage` wird aus `start()` in eine eigene, **lazy** aufgerufene Methode herausgezogen; gespeicherte Fenstergröße (`getGUIstage_updateStage_SceneSizeHW`, `:9253-9260`) bleibt. Das Auto-`show()` (`:9280`) entfällt, `setAlwaysOnTop(true)` (`:9162`) wird entfernt — ein Always-on-Top-Fenster über einem laufenden Contest ist genau das, was das Ziel verbietet.

### 1e. Versionsdrift absichern

`pom.xml:9` und `ApplicationConstants.APPLICATION_CURRENT_VERSION` werden **von Hand gepflegt, ohne jede Prüfung**. Für einen Mechanismus, der genau diese Zeichenkette vergleicht, ist das die riskanteste Stelle. Analog zu `packaging/AddModules.java --verify-pom` (an `validate` gebunden, `pom.xml:219-240`) einen `--verify-version`-Wächter ergänzen. Kein Auto-Bump.

`docs/PROJECT_CONTEXT.md:198` beschreibt zusätzlich die Kodierung von `APPLICATION_CURRENTVERSIONNUMBER` (`1.43.1` → `1.431`) — beim Bump mitführen, solange das Legacy-Feld existiert. Auf 1.50 stehen die Werte in `ApplicationConstants.java:23` und `:30`.

### 1f. `--app-version` nachziehen

Außer im macOS-Skript (`build-signed-dmg.sh:75`) setzt **kein** jpackage-Aufruf `--app-version`. DEB und RPM tragen dadurch intern die Vorgabeversion `1.0`; ein `apt install ./KST4Contest-v1.45.0.deb` wäre für dpkg **kein Upgrade**. Vor Stufe 3 zwingend, sinnvollerweise überall — dieselbe `sed`-Ableitung wie `build-signed-dmg.sh:59-65`. Nebenwirkung: bestehende DEB/RPM-Installationen springen einmalig von `1.0` auf die echte Version.

---

## Stufe 2 — Gesicherter Download

Erst nach „Update herunterladen".

| Datei | Verantwortung |
|---|---|
| `service/update/UpdateDownloadPolicy.java` | Reine Validierung, kein IO |
| `service/update/UpdateDownloadService.java` | Download + Verifikation |
| `service/update/UpdateStorage.java` | `~/.praktiKST/updates/`, Aufräumen verwaister Dateien |
| `model/UpdateDownloadResult.java` | Ergebnis-DTO |

**Ablauf** (Muster `TerrainPackageDownloader.java:93-165`):

1. Download nach `<fileName>.download`;
2. Prüfung von Dateigröße **und** SHA-256 (`computeSha256`, `:175-187`, 8-KiB-Puffer + `HexFormat`);
3. `Files.move` erst nach erfolgreicher Prüfung;
4. bei Abbruch oder falscher Prüfsumme temporäre Datei löschen, verständliche Meldung;
5. **niemals** ein ungeprüftes Artefakt starten.

Empfehlung: `BodyHandlers.ofInputStream()` + `DigestInputStream` statt `ofFile` — ein Durchlauf statt zwei, und ein Fortschrittsbalken ist bei 150-MB-Dateien Pflicht. Short-circuit, wenn die Zieldatei bereits mit passendem Hash existiert.

**Sicherheitsschranke — nicht optional:** SHA-256 aus dem Feed beweist nur, dass die Datei zum Feed passt. Da der Feed über einen selbst gebauten Host ausgeliefert wird, ist eine **Host-Allowlist** (`github.com`, `objects.githubusercontent.com`) die eigentliche Schutzschicht: selbst ein manipulierter Feed kann keine beliebige URL unterschieben. `UpdateDownloadPolicy` prüft zusätzlich: Schema `https`; `sha256` matcht `^[0-9a-fA-F]{64}$`; `fileName` ohne `/`, `\`, `..`; `fileSizeBytes` > 0 und < 1 GiB; ausreichend Plattenplatz; `packageType` erlaubt Download.

Stufe 2 führt **nichts** aus. Nach Erfolg: „Jetzt beenden und aktualisieren" / „Später installieren". Flatpak erhält gar keinen Download-Pfad, sondern den Hinweistext aus `<hintCommand>`.

---

## Stufe 3 — Plattformausführung, Windows-Installer, Signatur

### 3a. Windows-Installer

`--type msi` mit **dauerhaft konstanter** `--win-upgrade-uuid`, dazu `--win-menu --win-shortcut --win-dir-chooser`. `choco install wixtoolset` steht bereits in beiden Workflows (`tagged-release.yml:33-35`, `nightly-artifacts.yml:49-51`) und wird bisher **nicht genutzt** — die Voraussetzung ist vorhanden. Der ZIP-Build bleibt bestehen.

Die GUID muss dokumentiert werden (z. B. `packaging/windows/README.md`) — ändert sie sich, sind MSI-Upgrades dauerhaft kaputt. Asset in die `artifacts:`-Liste und in den SHA256SUMS-Schritt aufnehmen.

### 3b. Ausführung je Installationsart

| Installation | Verhalten |
|---|---|
| `windows-msi` | `msiexec /i "<datei>" /qb` detached, danach beenden |
| `windows-zip` | Keine Ausführung. Installer laden, Ordner öffnen, Benutzerdaten bleiben. **Hinweis nötig**, dass der alte ZIP-Ordner bestehen bleibt und künftig der Installer-Eintrag zu starten ist — sonst startet der Nutzer weiter die alte `praktiKST.exe` und sieht nie wieder ein Update |
| `macos-dmg` | `open "<dmg>"`, danach beenden; der Nutzer zieht selbst. Ein laufendes `.app` kann sich ohne separates Updater-Binary nicht ersetzen |
| `linux-appimage` | Neues AppImage neben `$APPIMAGE` ablegen, `chmod +x`, Ordner öffnen, beenden. Kein In-Place-Overwrite einer laufenden Datei |
| `flatpak` | Nur Verweis auf `flatpak update de.x08.KST4Contest`; keine Datei im Paket anfassen |
| `linux-deb/rpm/arch` | `xdg-open "<datei>"` → GNOME Software / Discover / gdebi übernehmen inkl. Rechteabfrage. **Bewusst kein `pkexec`/`sudo`** aus der Anwendung heraus |
| AUR | Verweis auf den vorhandenen AUR-Helper |
| `UNKNOWN` | Ausschließlich Downloadseite öffnen |

Kein eigener Update-Starter. Der kontrollierte Ausstieg: Verbindung prüfen und ggf. bestätigen lassen → ON4KST sauber trennen → Layout/Preferences flushen → DB schließen → Installer starten → `Platform.exit()` + `System.exit(0)`. Kein `Runtime.halt`. Der vorhandene Weg über `Kst4ContestApplication.stop()` (`disconnect("CLOSEALL")` + `System.exit(0)`) bleibt die Basis.

### 3c. SignPath

Eigener Step im MSI-Job, gegated über das Vorhandensein des Secrets. Fehlt es (bis zur Foundation-Freigabe), läuft die Pipeline unverändert durch und liefert ein unsigniertes MSI. Signiertes Ergebnis unter eigenem Namen hochladen; `versionInfo.js` setzt `signatureState` nur, wenn dieses Asset existiert.

**Vorbedingung für die Abhängigkeitsprüfung:** `module-info.java:14-15` deklariert `requires org.junit.jupiter.api;` und `requires org.mockito;`, weil Testklassen unter `src/main/java/` liegen (u. a. `controller/On4KstProtocolTest.java`, `controller/On4KstSocketThreadTest.java`, `controller/ReadUDPByWintestThreadTest.java`, `test/MockKstServer.java`, `view/map/mapTest.java`). `pom.xml` deklariert `junit-jupiter-api` und `mockito-core` deshalb mit `<scope>compile</scope>`, und JUnit, Mockito, ByteBuddy und Objenesis landen im ausgelieferten Runtime-Image — im Flatpak in `lib/app/` nachgewiesen. Ausgerechnet ByteBuddy/Mockito (Bytecode-Manipulation, Agent) in einem signierten Produktionsbinary ist für so eine Prüfung ungünstig. Als **eigener, abgegrenzter Commit vor dem Antrag** bereinigen: Klassen nach `src/test/java/`, `requires` entfernen, Scopes zurück auf `test`.

---

## Tests

Alle neuen Tests unter `src/test/java/kst4contest/service/update/` bzw. `.../model/`, Fixtures unter `src/test/resources/update/`. **Nichts Neues unter `src/main/java`** — die dort liegenden Testklassen sind Altlast, keine Vorlage.

| Testklasse | Assertions |
|---|---|
| `UpdateFeedParserTest` | Legacy-Feed 1.44 ohne `<updatePackages>` parst korrekt; erweiterter Feed, Reihenfolge irrelevant; `<changeLog>` mit 8 Kindern → **keine** `ArrayIndexOutOfBoundsException`; kaputtes XML → leeres Ergebnis statt Exception; XXE-Payload wird nicht expandiert; `sha256` in Großbuchstaben wird normalisiert |
| `UpdateFeedClientTest` | 200 → Zieldatei ersetzt; 404/500 → Temp gelöscht und **vorhandene Zieldatei unverändert** (die konkrete Regression gegenüber `UpdateChecker:54-71`); Timeout → `success=false`, kein Throw; leerer Body verworfen |
| `UpdateCheckServiceTest` | 1.44.0/1.44.0 → kein Update; /1.45.0 → Update; /1.4.4 → kein Update; /1.44.1 → Update; kaputte `semanticVersion` → Double-Fallback statt Exception; Netz down + Cache → `CACHE`; ohne Cache → `NONE`; Paketauswahl je Typ; `UNKNOWN` → kein Paket; injizierte `Clock` für den Prüfrhythmus |
| `RuntimePackageTypeResolverTest` | Property gewinnt; unbekannter Wert → `UNKNOWN`; `/.flatpak-info` → `FLATPAK` **auch gegen** widersprechende Property; `APPIMAGE` → AppImage; alles `null` → kein NPE |
| `UpdateDownloadPolicyTest` | `http://` abgelehnt; fremder Host abgelehnt; fehlende/zu kurze/nicht-hex `sha256` abgelehnt; `fileName` mit `../` abgelehnt; Größe ≤ 0 abgelehnt; `FLATPAK`/`UNKNOWN` abgelehnt |
| `UpdateDownloadServiceTest` | Ohne Netz über `com.sun.net.httpserver.HttpServer` auf Port 0 (JDK-only, keine neue Dependency): Hash-Mismatch → Temp gelöscht, Ziel **nicht** angelegt; Match → Bytes identisch; Non-2xx → Temp gelöscht; bereits gültige Datei → kein HTTP-Call; Progress monoton bis `totalBytes`; Abbruch → Temp gelöscht |
| `UpdateLauncher*Test` (Stufe 3) | Injizierter `ProcessStarter` fängt die Kommandozeile: MSI → `[msiexec, /i, <abs>, /qb]`; deb/rpm/arch → `[xdg-open, <abs>]`; dmg → `[open, <abs>]`; **kein Kommando enthält `sudo` oder `pkexec`** (explizite Assertion); `FLATPAK`/`UNKNOWN`/`WINDOWS_ZIP` → `ProcessStarter` nie aufgerufen |
| `website/test/version-info.test.js` | Klassifikator für alle Namensmuster; SHA256SUMS-Parsing inkl. Doppelspace; Release ohne Assets → Feed bleibt valide; erzeugter Feed hat weiterhin exakt 7 `<changeLog>`-Kinder |

Versionsvergleich: bestehende `VersionUtils`-Abdeckung um nicht-numerische Eingaben ergänzen.

---

## Dokumentation

Nach jeder Stufe eine gezielte Einordnung, kein Voll-Audit (`AGENTS.md`):

- `github_docs/de-Installation.md` und `github_docs/en-Installation.md` — Windows-Installer, Verhältnis zum ZIP, Update-Weg je Paketformat, Flatpak-Hinweis. Beide Sprachversionen semantisch gleichziehen.
- Neuer Abschnitt zur Updatefunktion (Hinweis, Zustimmung, Contest-Regel, Verhalten je Installationsart) — DE und EN.
- `docs/PROJECT_CONTEXT.md` — Abschnitt „Website / Deployment Relationship" (`:192-200`) um Feed-Format, Paketkennzeichnung, Host-Allowlist und die Entscheidung gegen ein Updater-Binary ergänzen.
- `website/src/features/` prüfen, ob die Funktion dort dargestellt werden soll.
- Screenshots des Hauptfensters veralten durch den Hinweisbereich — als veraltet markieren, nicht erfinden.

## Verifikation

1. `./mvnw -B test` — **Surefire-Berichte einzeln lesen**, nicht am Exit-Code messen (`AGENTS.md`: Testfehler brechen den Build nicht ab). Hinweis: auf Host-JDK 26 schlagen 8 Mockito-Tests umgebungsbedingt fehl; mit Temurin 21 prüfen.
2. `cd website && npm test && npm run build && npm run validate:version-info` gegen einen erweiterten Beispiel-Feed.
3. **Abwärtskompatibilität:** erweiterten Feed gegen den **alten** `UpdateChecker` laufen lassen und bestätigen, dass `<latestVersion>` und `<changeLog>` unverändert gelesen werden.
4. **Startverhalten:** Anwendung mit nicht erreichbarer Feed-URL starten — die UI muss ohne Verzögerung und ohne Meldung hochkommen.
5. **Contest-Regel:** mit ON4KST verbunden erscheint kein Hinweis; nach dem Trennen erscheint er.
6. **Paketkennzeichnung:** je Artefakt einmal starten und die erkannte Installationsart prüfen. Der Flatpak lässt sich lokal testen — die Sandbox ist netzwerkseitig transparent (verifiziert).
7. **Stufe 2:** Download mit absichtlich falscher Prüfsumme — es darf keine Zieldatei entstehen und nichts gestartet werden.

## Offene Punkte

1. **Zweite Feed-Quelle?** Wenn der serverseitige Build hinterherhinkt, sehen Clients einen veralteten Feed. Soll der Client hilfsweise `https://github.com/praktimarc/kst4contest/releases/latest/download/kst4ContestVersionInfo.xml` verwenden?
2. **`test-publish-version-xml.yml`** bleibt vorerst unverändert, ist aber scharf (Feed ohne `<semanticVersion>`). Bei der späteren Umstellung auf GitHub Actions zusammenführen oder entfernen.
3. **`--app-version` überall setzen** ändert die Paketversion bestehender DEB/RPM-Installationen einmalig von `1.0` auf die echte. Akzeptabel?
4. **`UpdateChecker.java`** löschen oder als deprecated Fassade behalten? (`module-info.java` exportiert `kst4contest.controller`.)
5. **MSI vs. ZIP** — soll MSI der empfohlene Windows-Weg werden? Beide Installationen können parallel existieren; ein MSI-Upgrade findet eine ZIP-Installation nicht.
6. **Opt-out-Preference?** Soll `ChatPreferences` ein „Check for updates on start" bekommen? Der Abruf verrät IP und Version an den Feed-Host.
7. **Prüfrhythmus** — einmal pro Start, oder höchstens alle 24 h über einen Zeitstempel in `~/.praktiKST/`?
8. **Beta-Nutzer** — `1.46.0-beta` gegen Stable `1.45.0` ergibt „kein Update", weil `compareStableVersions` das Suffix abschneidet. Gewollt?
9. **Signaturzustand** darf nur automatisch aus dem Build stammen, sonst driftet er.
10. **Vertrauensmodell** — soll der Feed mittelfristig selbst signiert werden (Detached-Signatur + eingebauter Public Key)?
11. **ZIP-Migration** — der Hinweistext für den Wechsel ZIP → Installer muss formuliert und in beiden Sprachversionen dokumentiert werden.
