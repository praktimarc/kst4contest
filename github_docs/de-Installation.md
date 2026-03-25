# Installation

> 🇬🇧 [English version](en-Installation) | 🇩🇪 Du liest gerade die deutsche Version

## Voraussetzungen

### Java

KST4Contest ist eine Java-Anwendung. Es wird eine aktuelle **Java Runtime Environment (JRE)** benötigt. Die empfohlene Version ist Java 17 oder höher.

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

Die aktuelle Version kann als ZIP-Datei heruntergeladen werden:

**https://do5amf.funkerportal.de/**

Der Dateiname hat das Format `kst4Contest_v<Versionsnummer>.zip`.

---

## Installation

1. ZIP-Datei herunterladen.
2. ZIP-Datei in einen gewünschten Ordner entpacken.
3. `praktiKST.exe` (Windows) bzw. das entsprechende Start-Skript ausführen.

Die Einstellungen werden unter `%USERPROFILE%\.praktikst\preferences.xml` (Windows) gespeichert.

---

## Update

KST4Contest enthält einen **automatischen Update-Hinweis-Dienst**: Sobald eine neue Version verfügbar ist, erscheint beim Start ein Fenster mit:
- der Information, dass eine neue Version vorliegt,
- einem Changelog,
- dem Download-Link zur neuen Version.

### Update-Prozess

Derzeit gibt es nur einen Weg zum Aktualisieren:

1. Den alten Ordner löschen.
2. Das neue ZIP entpacken.

Die Einstellungsdatei (`preferences.xml`) bleibt erhalten, da sie im Benutzerordner gespeichert ist – nicht im Programmordner.

---

## Bekannte Probleme beim Start

### Norton 360

Norton 360 stuft `praktiKST.exe` als gefährlich ein (Fehlalarm). Es muss eine Ausnahme für die Datei eingerichtet werden:

1. Norton 360 öffnen.
2. Sicherheit → Verlauf → Das entsprechende Ereignis suchen.
3. „Wiederherstellen & Ausnahme hinzufügen" wählen.

*(Gemeldet von PE0WGA, Franz van Velzen – danke!)*
