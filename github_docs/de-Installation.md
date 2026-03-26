# Installation

> 🇬🇧 [English version](en-Installation) | 🇩🇪 Du liest gerade die deutsche Version

## Voraussetzungen

### ON4KST-Account

Um den Chat zu nutzen, ist ein registrierter Account beim ON4KST-Chat-Dienst erforderlich:

- Registrierung unter: http://www.on4kst.info/chat/register.php

### Verhaltensregeln im Chat

Die offizielle Sprache im ON4KST-Chat ist **Englisch**. Auch bei Kommunikation mit Stationen aus dem eigenen Land bitte Englisch verwenden. Übliche HAM-Abkürzungen (agn, dir, pse, rrr, tnx, 73 …) sind gang und gäbe.

### Persönliche Nachrichten

Um eine Privatnachricht an eine andere Station zu senden, immer folgendes Format verwenden:

```
/CQ RUFZEICHEN Nachrichtentext
```

Beispiel: `/CQ DL5ASG pse sked 144.205?`

Bei starkem Chat-Verkehr (5–6 Nachrichten pro Sekunde im Contest) gehen öffentliche Nachrichten, die an ein bestimmtes Rufzeichen gerichtet sind, leicht unter. KST4Contest fängt solche Nachrichten aber auch dann ab, wenn sie fälschlicherweise öffentlich gepostet werden (siehe [Funktionen – PM-Abfang](Funktionen#catching-personal-messages)).

---

## Download

### Windows

Die aktuelle Version kann als ZIP-Datei heruntergeladen werden:

**https://github.com/praktimarc/kst4contest/releases/latest**

Der Dateiname hat das Format `praktiKST-v<Versionsnummer>-windows-x64.zip `.

### Linux

Die aktuelle Version kann als AppImage heruntergeladen werden:

**https://github.com/praktimarc/kst4contest/releases/latest**

Der Dateiname hat das Format `praktiKST-v<Versionsnummer>-linux-x86_64.AppImage`.


---

## Installation

### Windows

1. ZIP-Datei herunterladen.
2. ZIP-Datei in einen gewünschten Ordner entpacken.
3. `praktiKST.exe` ausführen.

Die Einstellungen werden unter `%USERPROFILE%\.praktikst\preferences.xml` gespeichert.

### Linux
1. AppImage herunterladen.
2. AppImage in gewünschten Ordner entpacken.
3. AppImage ausführbar machen (geht im Terminal mit `chmod +x praktiKST-v<Versionsnummer>-linux-x86_64.AppImage`)
4. AppImage ausführen.

Die Einstellungen werden unter `~/.praktikst/preferences.xml` gespeichert.

---

## Update

KST4Contest enthält einen **automatischen Update-Hinweis-Dienst**: Sobald eine neue Version verfügbar ist, erscheint beim Start ein Fenster mit:
- der Information, dass eine neue Version vorliegt,
- einem Changelog,
- dem Download-Link zur neuen Version.

![Beispiel Update Fenster](update_window.png)

### Update-Prozess

#### Windows

Derzeit gibt es nur einen Weg zum Aktualisieren:

1. Den alten Ordner löschen.
2. Das neue ZIP entpacken.

Die Einstellungsdatei (`preferences.xml`) bleibt erhalten, da sie im Benutzerordner gespeichert ist – nicht im Programmordner.

#### Linux

Derzeit folgendermaßen:
1. neues AppImage herunterladen
2. neues AppImage ausführbar makieren
3. (optional) altes AppImage löschen.


---

## Bekannte Probleme beim Start

### Norton 360

Norton 360 stuft `praktiKST.exe` als gefährlich ein (Fehlalarm). Es muss eine Ausnahme für die Datei eingerichtet werden:

1. Norton 360 öffnen.
2. Sicherheit → Verlauf → Das entsprechende Ereignis suchen.
3. „Wiederherstellen & Ausnahme hinzufügen" wählen.

*(Gemeldet von PE0WGA, Franz van Velzen – danke!)*
