# Installation

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-Installation)

## Prerequisites

An resolution of 1200px by 720px is recommended

### ON4KST Account

To use the chat, a registered account with the ON4KST chat service is required:

- Register at: http://www.on4kst.info/chat/register.php

### Chat Etiquette

The official language in the ON4KST Chat is **English**. Please use English even when communicating with stations from your own country. Common HAM abbreviations (agn, dir, pse, rrr, tnx, 73 …) are widely used and understood.

### Personal Messages

To send a private message to another station, always use the following format:

```
/CQ CALLSIGN message text
```

Example: `/CQ DL5ASG pse sked 144.205?`

During heavy chat traffic (5–6 messages per second in a contest), public messages directed at a specific callsign are easily missed. However, KST4Contest also catches such messages if they are accidentally posted publicly (see [Features – PM Catching](Features#catching-personal-messages)).

---

## Download

### Windows

The latest version can be downloaded as a ZIP file:

**https://github.com/praktimarc/kst4contest/releases/latest**

The filename has the format `praktiKST-v<version_number>-windows-x64.zip`.

### Linux

Multiple package formats are available from the releases page:

**https://github.com/praktimarc/kst4contest/releases/latest**

| Format | Filename | Suitable for |
|---|---|---|
| AppImage | `KST4Contest-v<version>-linux-x86_64.AppImage` | All distributions |
| Debian package | `KST4Contest-v<version>-debian-amd64.deb` | Debian, Ubuntu, Linux Mint, … |
| RPM package | `KST4Contest-v<version>-fedora-x86_64.rpm` | Fedora, RHEL, openSUSE, … |
| Arch package | `KST4Contest-v<version>-archlinux-x86_64.pkg.tar.zst` | Arch Linux, Manjaro, … |
| Flatpak | `de.x08.KST4Contest.flatpakref` | All distributions with Flatpak |

> **Recommended for Linux:** The Flatpak installation is the easiest way to stay up to date — `flatpak update` handles all future updates automatically. The repository is GPG-signed for security.

### macOS

> ⚠️ **Best-Effort Support:** macOS builds are provided as a convenience but are **not fully tested**. We build and release macOS binaries with every release, but we cannot test every scenario on macOS. If you encounter issues, please report them – we will do our best to address them, but cannot guarantee the same level of support as for Windows and Linux.

The latest version can be downloaded as a DMG disk image (available for both Apple Silicon and Intel Macs):

**https://github.com/praktimarc/kst4contest/releases/latest**

The filename has the format `KST4Contest-v<version_number>-macos-<arch>.dmg`, where `<arch>` is `arm64` (Apple Silicon) or `x86_64` (Intel).


---

## Installation

### Windows

1. Download the ZIP file.
2. Unzip the ZIP file into a folder of your choice.
3. Run `praktiKST.exe`.

Settings are stored at `%USERPROFILE%\.praktikst\preferences.xml`.

### Linux

Settings are always stored at `~/.praktikst/preferences.xml`.

#### AppImage

1. Download the AppImage.
2. Make it executable: `chmod +x KST4Contest-v<version>-linux-x86_64.AppImage`
3. Run it.

#### Debian / Ubuntu

```bash
sudo apt install ./KST4Contest-v<version>-debian-amd64.deb
```

Or double-click the `.deb` file in your file manager.

#### Fedora / RHEL

```bash
sudo dnf install ./KST4Contest-v<version>-fedora-x86_64.rpm
```

#### Arch Linux

```bash
sudo pacman -U KST4Contest-v<version>-archlinux-x86_64.pkg.tar.zst
```

#### Flatpak

Download `de.x08.KST4Contest.flatpakref` from the [latest release](https://github.com/praktimarc/kst4contest/releases/latest) and open it, or run:
```bash
flatpak install de.x08.KST4Contest.flatpakref
```

Or add the remote manually:
```bash
flatpak remote-add kst4contest https://praktimarc.github.io/kst4contest/
flatpak install kst4contest de.x08.KST4Contest
```

### macOS
1. Download the DMG file for your architecture (Apple Silicon or Intel).
2. Open the DMG file.
3. Drag `KST4Contest.app` into your **Applications** folder.
4. On first launch, macOS may show a warning because the app is not notarised. To open it:
   - Right-click (or Control-click) on `KST4Contest.app` in Finder and choose **Open**.
   - Alternatively, go to **System Settings → Privacy & Security** and click **Open Anyway**.
5. Run KST4Contest from your Applications folder or Launchpad.

Settings are stored at `~/.praktikst/preferences.xml`.

---

## Updating

KST4Contest includes an **automatic update notification service**: as soon as a new version is available, a window will appear at startup with:
- information that a new version is available,
- a changelog,
- the download link for the new version.

![Example Update Window](update_window.png)

### Update Process

#### Windows

Currently, there is only one way to update:

1. Delete the old folder.
2. Unzip the new ZIP file.

The settings file (`preferences.xml`) is preserved because it is stored in the user folder, not the program folder.

#### Linux

- **AppImage**: Download the new AppImage, make it executable (`chmod +x`), optionally delete the old one.
- **Debian/Ubuntu**: `sudo apt install ./KST4Contest-v<version>-debian-amd64.deb`
- **Fedora/RHEL**: `sudo dnf upgrade ./KST4Contest-v<version>-fedora-x86_64.rpm`
- **Arch Linux**: `sudo pacman -U KST4Contest-v<version>-archlinux-x86_64.pkg.tar.zst`
- **Flatpak (repository)**: `flatpak update` – updates all Flatpak apps including KST4Contest.

#### macOS

1. Download the new DMG file.
2. Open the DMG.
3. Drag the new `KST4Contest.app` into your **Applications** folder, replacing the old version.


---

## Known Issues at Startup

### Norton 360

Norton 360 classifies `praktiKST.exe` as dangerous (false positive). An exception must be created for the file:

1. Open Norton 360.
2. Security → History → Find the corresponding event.
3. Select "Restore & Add Exception".

*(Reported by PE0WGA, Franz van Velzen – thank you!)*
