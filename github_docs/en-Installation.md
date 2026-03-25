# Installation

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-Installation)

## Prerequisites

### Java

KST4Contest is a Java application. A current **Java Runtime Environment (JRE)** is required. The recommended version is Java 17 or higher.

### ON4KST Account

To use the chat client, you need a registered account with the ON4KST chat service:

- Register at: http://www.on4kst.info/chat/register.php

### Behavioural Etiquette

The official language in the ON4KST Chat is **English**. Please use English even when communicating with stations from your own country. Common HAM abbreviations (agn, dir, pse, rrr, tnx, 73 …) are widely used and understood.

### Sending Personal Messages

To send a private message to another station, always use this format:

```
/CQ CALLSIGN message text
```

Example: `/CQ DL5ASG pse sked 144.205?`

During contest operation (5–6 messages per second in the public channel), public messages directed at a specific callsign are easily missed. KST4Contest also catches such messages if they are accidentally posted publicly (see [Features – PM Catching](Features#pm-catching)).

---

## Download

The latest version can be downloaded as a ZIP file:

**https://do5amf.funkerportal.de/**

The filename follows the pattern `kst4Contest_v<version>.zip`.

---

## Installation

1. Download the ZIP file.
2. Unzip into a folder of your choice.
3. Run `praktiKST.exe` (Windows) or the corresponding start script.

Settings are stored at `%USERPROFILE%\.praktikst\preferences.xml` (Windows).

---

## Updating

KST4Contest includes an **automatic update notification service**: when a new version is available, a window will appear at startup showing:
- A notification that a new version is available
- A changelog
- The download link for the latest package

### Update Process

Currently the only way to update is:

1. Delete the old folder.
2. Unzip the new package.

Your settings file (`preferences.xml`) is preserved since it is stored in your user folder, not the program folder.

---

## Known Issues at Startup

### Norton 360

Norton 360 flags `praktiKST.exe` as dangerous (false positive). You need to add an exception:

1. Open Norton 360.
2. Security → History → Find the relevant event.
3. Select "Restore & Add Exception".

*(Reported by PE0WGA, Franz van Velzen – thank you!)*
