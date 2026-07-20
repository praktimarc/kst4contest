# Installation

> 🇬🇧 [English version](en-Installation) | 🇩🇪 Du liest gerade die deutsche Version

KST4Contest wird für Windows, Linux und macOS als fertiges Programmpaket bereitgestellt. Für die offiziellen Release-Pakete muss Java nicht separat installiert werden: Die benötigte Java-Laufzeitumgebung ist bereits enthalten.

Benötigt werden damit im Wesentlichen ein unterstütztes Betriebssystem, eine Internetverbindung und ein ON4KST-Account. Das klingt zunächst überschaubar – und ist es normalerweise auch.

## Voraussetzungen

### ON4KST-Account

KST4Contest ist ein eigenständiger Client für den ON4KST-Chat, ersetzt aber nicht den dazugehörigen Benutzeraccount.

Falls noch kein Account vorhanden ist, kann er über die offizielle ON4KST-Seite angelegt werden:

- [ON4KST-Anmeldung und Registrierung](https://www.on4kst.org/chat/login.php)

ON4KST gibt Englisch als gemeinsame Sprache im Chat vor. Das gilt auch dann, wenn beide beteiligten Stationen dieselbe Muttersprache sprechen. Im Contestbetrieb werden häufig die üblichen Amateurfunk-Abkürzungen wie `pse`, `agn`, `qrg`, `dir`, `rrr`, `tnx` oder `73` verwendet.

Die eigentliche Bedienung des Chats und das Senden persönlicher Nachrichten werden im Kapitel zur Benutzeroberfläche beschrieben. Für die Installation ist zunächst nur wichtig, dass der Account funktioniert.

### Bildschirmgröße

Eine nutzbare Bildschirmfläche von ungefähr **1200 × 720 Pixeln** oder mehr wird empfohlen.

KST4Contest passt das Hauptfenster automatisch an die verfügbare Fläche des primären Bildschirms an. Das Programm kann deshalb auch auf kleineren Bildschirmen gestartet werden. Weniger Platz bleibt allerdings weniger Platz: In diesem Fall können nicht alle Tabellen, Filter und Zusatzinformationen gleichzeitig in sinnvoller Größe dargestellt werden.

### Java

Für die fertigen Pakete aus den [GitHub Releases](https://github.com/praktimarc/kst4contest/releases/latest) ist keine separate Java-Installation erforderlich. Die benötigte Laufzeitumgebung wird zusammen mit KST4Contest ausgeliefert.

Eine Java-Entwicklungsumgebung wird nur benötigt, wenn KST4Contest selbst aus dem Quellcode gebaut werden soll. Bei den AUR-Paketen `kst4contest` und `kst4contest-git` werden Java 21 und Maven als Build-Abhängigkeiten durch den Paketmanager berücksichtigt.

---

## Stable, Beta oder Nightly?

KST4Contest wird in drei Entwicklungsständen bereitgestellt:

| Kanal | Geeignet für | Einordnung |
|---|---|---|
| **Stable** | Normaler Contestbetrieb | Veröffentlichte und für den regulären Einsatz vorgesehene Version |
| **Beta** | Gezielte Tests vor einem Stable-Release | Vorabversion mit weitgehend festgelegtem Funktionsumfang |
| **Nightly** | Frühe Tests neuer Funktionen | Aktueller Entwicklungsstand aus dem `main`-Branch |

Für den normalen Einsatz wird die **Stable-Version** empfohlen.

Beta- und Nightly-Versionen können Funktionen enthalten, die im Stable-Release noch nicht verfügbar sind. Sie können aber ebenso unfertige Bedienabläufe, geänderte Einstellungen oder neue Fehler enthalten. Das ist kein ungewöhnlicher Defekt im Veröffentlichungsverfahren, sondern der Zweck eines Entwicklungskanals.

Wenn in diesem Manual eine Funktion ausdrücklich mit **Nightly** gekennzeichnet ist, gehört sie noch nicht zwingend zum aktuellen Stable-Release.

---

## Download

Die aktuelle Stable-Version ist hier verfügbar:

- [Aktuelles KST4Contest-Release](https://github.com/praktimarc/kst4contest/releases/latest)
- [Alle veröffentlichten Versionen](https://github.com/praktimarc/kst4contest/releases)

Die Release-Seite enthält die Programmpakete sowie die deutschen und englischen PDF-Handbücher.

### Welches Paket wird benötigt?

| Betriebssystem | Paket | Typischer Dateiname |
|---|---|---|
| Windows x64 | ZIP-Paket | `praktiKST-v<Version>-windows-x64.zip` |
| Linux x86_64 | AppImage | `KST4Contest-v<Version>-linux-x86_64.AppImage` |
| Debian/Ubuntu amd64 | DEB-Paket | `KST4Contest-v<Version>-debian-amd64.deb` |
| Fedora/RPM x86_64 | RPM-Paket | `KST4Contest-v<Version>-fedora-x86_64.rpm` |
| Arch Linux x86_64 | Arch-Paket | `KST4Contest-v<Version>-archlinux-x86_64.pkg.tar.zst` |
| Linux mit Flatpak | Flatpak-Referenz | `de.x08.KST4Contest.flatpakref` |
| macOS Apple Silicon | DMG für ARM64 | `KST4Contest-v<Version>-macos-arm64.dmg` |
| macOS Intel | DMG für x86_64 | `KST4Contest-v<Version>-macos-x86_64.dmg` |

Lade Programmpakete nur aus den offiziellen GitHub Releases, dem KST4Contest-Flatpak-Repository oder den verlinkten AUR-Paketen herunter. Dateien aus anderen Quellen können anders gebaut, veraltet oder verändert worden sein.

---

## Installation unter Windows

KST4Contest wird unter Windows als ZIP-Paket bereitgestellt. Ein klassischer Installer ist nicht erforderlich.

1. Lade `praktiKST-v<Version>-windows-x64.zip` aus dem aktuellen Release herunter.
2. Entpacke die ZIP-Datei vollständig in einen eigenen Ordner.
3. Öffne den entpackten Ordner.
4. Starte `praktiKST.exe`.

Starte das Programm nicht direkt aus der noch komprimierten ZIP-Datei. KST4Contest besteht aus mehreren Dateien und einer mitgelieferten Laufzeitumgebung. Windows kann diese Struktur nur zuverlässig verwenden, wenn das Archiv vorher vollständig entpackt wurde.

Die Programmeinstellungen befinden sich nicht im entpackten Programmordner, sondern im Benutzerverzeichnis:

```text
%USERPROFILE%\.praktiKST\preferences.xml
```

Dadurch können neue Programmversionen in einen anderen Ordner entpackt werden, ohne dass die vorhandenen Einstellungen verloren gehen.

---

## Installation unter Linux

Unter Linux stehen mehrere Paketformate zur Verfügung. Welches davon sinnvoll ist, hängt weniger von KST4Contest als von der verwendeten Distribution und der gewünschten Update-Methode ab.

| Installationsart | Sinnvoll, wenn … |
|---|---|
| **Flatpak** | Updates zentral verwaltet werden sollen und Flatpak bereits verwendet wird |
| **AppImage** | KST4Contest ohne Installation als einzelne portable Datei gestartet werden soll |
| **DEB/RPM** | die Paketverwaltung der Distribution verwendet werden soll |
| **Arch-Paket/AUR** | Arch Linux, Manjaro oder EndeavourOS eingesetzt wird |

### Flatpak

Flatpak ist für die meisten Linux-Anwender der einfachste Weg, KST4Contest installiert und aktualisierbar zu halten. Das KST4Contest-Repository ist GPG-signiert und enthält die Kanäle Stable, Beta und Nightly.

#### Stable über die Release-Datei installieren

Lade `de.x08.KST4Contest.flatpakref` aus dem aktuellen Release herunter und öffne die Datei mit der Softwareverwaltung der verwendeten Desktop-Umgebung.

Alternativ kann sie im Terminal installiert werden:

```bash
flatpak install ./de.x08.KST4Contest.flatpakref
```

#### KST4Contest-Repository hinzufügen

Das Repository muss nur einmal hinzugefügt werden:

```bash
flatpak remote-add --if-not-exists kst4contest \
https://praktimarc.github.io/kst4contest/kst4contest.flatpakrepo
```

Anschließend kann die Stable-Version installiert werden:

```bash
flatpak install kst4contest de.x08.KST4Contest//stable
```

#### Beta installieren

```bash
flatpak install kst4contest de.x08.KST4Contest//beta
```

#### Nightly installieren

```bash
flatpak install kst4contest de.x08.KST4Contest//nightly
```

KST4Contest verwendet für alle drei Kanäle dieselbe App-ID. Eine parallele Installation mehrerer KST4Contest-Kanäle ist deshalb nicht vorgesehen.

Zum Wechsel von Stable auf Nightly beispielsweise:

```bash
flatpak uninstall de.x08.KST4Contest//stable
flatpak install kst4contest de.x08.KST4Contest//nightly
```

Die persönlichen Einstellungen im Verzeichnis `~/.praktiKST` werden dabei nicht automatisch gelöscht.

Installierte Flatpak-Anwendungen können mit folgendem Befehl aktualisiert werden:

```bash
flatpak update de.x08.KST4Contest
```

Je nach Desktop-Umgebung kann die grafische Softwareverwaltung verfügbare Flatpak-Updates ebenfalls anzeigen oder automatisch installieren. Der Befehl `flatpak update` selbst führt das Update aus; er verspricht nicht, irgendwann von allein vorbeizukommen.

### AppImage

Das AppImage benötigt keine klassische Installation.

1. Lade `KST4Contest-v<Version>-linux-x86_64.AppImage` herunter.
2. Öffne ein Terminal im Download-Verzeichnis.
3. Mache die Datei ausführbar:

```bash
chmod +x KST4Contest-v<Version>-linux-x86_64.AppImage
```

4. Starte KST4Contest:

```bash
./KST4Contest-v<Version>-linux-x86_64.AppImage
```

Das AppImage kann anschließend an einen anderen Ort verschoben werden, beispielsweise nach `~/Applications`.

### Debian und Ubuntu

Installiere das DEB-Paket mit:

```bash
sudo apt install ./KST4Contest-v<Version>-debian-amd64.deb
```

Alternativ kann die Datei in einer grafischen Paketverwaltung geöffnet werden.

### Fedora und kompatible RPM-Systeme

Installiere das RPM-Paket mit:

```bash
sudo dnf install ./KST4Contest-v<Version>-fedora-x86_64.rpm
```

### Arch Linux: fertiges Release-Paket

Das aus dem GitHub Release heruntergeladene Arch-Paket kann direkt installiert werden:

```bash
sudo pacman -U KST4Contest-v<Version>-archlinux-x86_64.pkg.tar.zst
```

### Arch Linux: Installation über den AUR

Im AUR stehen drei Varianten bereit:

| Paket | Inhalt |
|---|---|
| [`kst4contest-bin`](https://aur.archlinux.org/packages/kst4contest-bin) | Vorgefertigtes Paket des aktuellen Stable-Releases |
| [`kst4contest`](https://aur.archlinux.org/packages/kst4contest) | Stable-Release, das lokal aus dem Quellcode gebaut wird |
| [`kst4contest-git`](https://aur.archlinux.org/packages/kst4contest-git) | Aktueller Entwicklungsstand aus dem `main`-Branch |

Für die meisten Anwender ist `kst4contest-bin` die naheliegende Variante:

```bash
yay -S kst4contest-bin
```

Stable aus dem Quellcode bauen:

```bash
yay -S kst4contest
```

Aktuellen Entwicklungsstand bauen:

```bash
yay -S kst4contest-git
```

Die drei Pakete stellen dieselbe Anwendung bereit und sind deshalb als gegenseitige Konflikte definiert. Installiere nur eine Variante gleichzeitig.

AUR-Updates werden berücksichtigt, wenn der verwendete AUR-Helper nach Paketaktualisierungen sucht, beispielsweise mit:

```bash
yay -Syu
```

Sie erfolgen nicht allein deshalb automatisch, weil das Paket aus dem AUR stammt.

---

## Installation unter macOS

> **Best-Effort-Support:** Die macOS-Pakete werden zusammen mit den übrigen Releases gebaut, aber nicht in demselben Umfang getestet wie die Windows- und Linux-Versionen. Rückmeldungen sind willkommen; eine vollständig geprüfte Unterstützung aller macOS-Versionen und Hardwarevarianten kann derzeit jedoch nicht zugesagt werden.

Für Apple-Silicon-Macs wird das Paket mit `arm64` benötigt. Für Intel-Macs ist das Paket mit `x86_64` vorgesehen.

1. Lade die passende DMG-Datei herunter.
2. Öffne die DMG-Datei.
3. Ziehe `KST4Contest.app` in den Ordner **Programme**.
4. Starte KST4Contest aus dem Programme-Ordner oder über das Launchpad.

Die Anwendung ist derzeit nicht von Apple notarisiert. macOS kann den ersten Start deshalb blockieren.

Falls die Anwendung aus dem offiziellen GitHub Release stammt:

1. Öffne den Programme-Ordner im Finder.
2. Klicke mit der rechten Maustaste oder mit gedrückter Ctrl-Taste auf `KST4Contest.app`.
3. Wähle **Öffnen**.
4. Bestätige den Start im angezeigten Dialog.

Alternativ kann macOS unter **Systemeinstellungen → Datenschutz & Sicherheit** die Schaltfläche **Trotzdem öffnen** anbieten.

---

## Wo werden die Einstellungen gespeichert?

KST4Contest speichert seine Einstellungen und weitere lokale Arbeitsdateien im Benutzerverzeichnis. Der Programmordner und das Datenverzeichnis sind voneinander getrennt.

| Betriebssystem | Datenverzeichnis | Einstellungsdatei |
|---|---|---|
| Windows | `%USERPROFILE%\.praktiKST\` | `%USERPROFILE%\.praktiKST\preferences.xml` |
| Linux | `~/.praktiKST/` | `~/.praktiKST/preferences.xml` |
| macOS | `~/.praktiKST/` | `~/.praktiKST/preferences.xml` |

Beachte die Schreibweise `.praktiKST` mit großem `KST`. Unter Linux und macOS wird zwischen Groß- und Kleinschreibung unterschieden. `.praktikst` wäre dort schlicht ein anderes Verzeichnis.

Ein Programmupdate entfernt dieses Verzeichnis nicht. Trotzdem ist es sinnvoll, vor größeren Versionswechseln oder umfangreichen Änderungen an der Konfiguration eine Sicherung davon anzulegen.

---

## Updates

KST4Contest prüft beim Start, ob ein neueres Stable-Release verfügbar ist. Wenn eine neuere Version gefunden wird, erscheint ein Informationsfenster mit:

- der installierten Version,
- der aktuellen Stable-Version,
- einer kurzen Übersicht wesentlicher Änderungen,
- dem Changelog,
- bekannten Problemen,
- einem Link zur passenden GitHub-Release-Seite.

![Update-Hinweis von KST4Contest](update_window.png)

Der Update-Checker installiert nichts selbst. Er informiert über die neue Version und öffnet die plattformneutrale Release-Seite. Dort muss das passende Paket für Windows, Linux oder macOS ausgewählt werden.

### Windows aktualisieren

1. Beende KST4Contest.
2. Lade das neue Windows-ZIP herunter.
3. Entpacke es in einen neuen oder leeren Ordner.
4. Starte die neue Version.
5. Prüfe, ob die bisherigen Einstellungen geladen wurden.
6. Entferne den alten Programmordner erst danach.

Die Einstellungen bleiben erhalten, weil sie unter `%USERPROFILE%\.praktiKST` und nicht im Programmordner gespeichert werden.

### AppImage aktualisieren

1. Lade das neue AppImage herunter.
2. Mache es ausführbar.
3. Starte die neue Datei.
4. Entferne das bisherige AppImage erst, wenn die neue Version funktioniert.

### Debian und Ubuntu aktualisieren

```bash
sudo apt install ./KST4Contest-v<Version>-debian-amd64.deb
```

### Fedora aktualisieren

```bash
sudo dnf upgrade ./KST4Contest-v<Version>-fedora-x86_64.rpm
```

### Arch-Paket aktualisieren

```bash
sudo pacman -U KST4Contest-v<Version>-archlinux-x86_64.pkg.tar.zst
```

### AUR-Paket aktualisieren

Aktualisiere das installierte Paket über den verwendeten AUR-Helper, beispielsweise:

```bash
yay -Syu
```

### Flatpak aktualisieren

```bash
flatpak update de.x08.KST4Contest
```

### macOS aktualisieren

1. Lade die neue DMG-Datei für die vorhandene Architektur herunter.
2. Beende KST4Contest.
3. Öffne die DMG-Datei.
4. Ersetze `KST4Contest.app` im Programme-Ordner.
5. Starte die neue Version und prüfe die vorhandenen Einstellungen.

Die Konfiguration unter `~/.praktiKST` bleibt davon unberührt.

---

## Probleme beim ersten Start

### Windows meldet eine unbekannte Anwendung

KST4Contest ist derzeit nicht mit einem kommerziellen Windows-Code-Signing-Zertifikat signiert. Windows oder ein zusätzlich installiertes Sicherheitsprodukt kann deshalb vor einer unbekannten oder selten heruntergeladenen Anwendung warnen.

Prüfe in diesem Fall zuerst:

- Stammt die Datei aus dem [offiziellen KST4Contest-Release](https://github.com/praktimarc/kst4contest/releases/latest)?
- Passt der Dateiname zum veröffentlichten Release?
- Wurde die Datei vollständig heruntergeladen?
- Nennt das Sicherheitsprogramm einen konkreten Erkennungsnamen oder nur eine allgemeine Reputationswarnung?

Ein Warnhinweis ist allein weder ein sicherer Beweis für Schadsoftware noch automatisch ein Fehlalarm. Wenn die Herkunft der Datei unklar ist, sollte sie nicht gestartet oder aus der Quarantäne wiederhergestellt werden.

Einige Anwender haben insbesondere Quarantäne-Meldungen von Norton 360 gemeldet. Wenn sich eine Meldung reproduzieren lässt, erstelle bitte einen [GitHub-Issue](https://github.com/praktimarc/kst4contest/issues) und nenne:

- die KST4Contest-Version,
- den vollständigen Dateinamen,
- das verwendete Sicherheitsprodukt und dessen Version,
- den angezeigten Erkennungsnamen,
- möglichst einen Screenshot der Meldung.

### Das AppImage startet nicht

Prüfe zuerst das Ausführungsrecht:

```bash
chmod +x KST4Contest-v<Version>-linux-x86_64.AppImage
```

Starte die Datei anschließend aus einem Terminal. Fehlermeldungen sind dort meist aussagekräftiger als ein Doppelklick, der lediglich nichts Sichtbares tut.

### macOS blockiert die Anwendung

Verwende die unter [Installation unter macOS](#installation-unter-macos) beschriebene Funktion **Öffnen** im Kontextmenü. Prüfe vorher, ob die DMG-Datei aus dem offiziellen GitHub Release stammt.

### Das Problem bleibt bestehen

Prüfe zunächst, ob das Problem bereits unter [GitHub Issues](https://github.com/praktimarc/kst4contest/issues) beschrieben wurde. Falls nicht, erstelle einen neuen Issue mit:

- Betriebssystem und Version,
- verwendeter KST4Contest-Version,
- Installationsart,
- genauer Fehlermeldung,
- den Schritten, mit denen sich das Problem reproduzieren lässt.

„Geht nicht“ beschreibt den Zustand meistens korrekt, hilft bei der Fehlersuche aber nur begrenzt.