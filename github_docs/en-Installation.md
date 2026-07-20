# Installation

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-Installation)

KST4Contest is distributed as a ready-to-run application package for Windows, Linux and macOS. The official release packages do not require a separate Java installation: the required Java runtime is already included.

In practical terms, you need a supported operating system, an internet connection and an ON4KST account. That sounds manageable—and usually it is.

## Requirements

### ON4KST account

KST4Contest is an independent client for the ON4KST chat, but it does not replace the corresponding user account.

If you do not have an account yet, you can create one on the official ON4KST website:

- [ON4KST login and registration](https://www.on4kst.org/chat/login.php)

ON4KST specifies English as the common language for the chat. This also applies when both stations happen to share another language. During contests, the usual amateur radio abbreviations such as `pse`, `agn`, `qrg`, `dir`, `rrr`, `tnx` and `73` are widely used.

Using the chat and sending personal messages are covered in the user interface chapter. For the installation, the only thing that matters at this point is that the account works.

### Screen size

A usable screen area of approximately **1200 × 720 pixels** or more is recommended.

KST4Contest automatically adapts the main window to the available area of the primary screen. The application can therefore also be started on smaller displays. Less space is still less space, however: not all tables, filters and additional information can then be displayed at a useful size at the same time.

### Java

The ready-made packages from the [GitHub Releases](https://github.com/praktimarc/kst4contest/releases/latest) do not require a separate Java installation. The required runtime is distributed with KST4Contest.

A Java development environment is only required if you want to build KST4Contest from source. For the AUR packages `kst4contest` and `kst4contest-git`, Java 21 and Maven are handled as build dependencies by the package manager.

---

## Stable, Beta or Nightly?

KST4Contest is distributed through three development channels:

| Channel | Intended use | Classification |
|---|---|---|
| **Stable** | Normal contest operation | Published version intended for regular use |
| **Beta** | Focused testing before a Stable release | Pre-release version with a largely defined feature set |
| **Nightly** | Early testing of new features | Current development state from the `main` branch |

The **Stable version** is recommended for normal operation.

Beta and Nightly builds may contain features that are not yet available in the Stable release. They may also contain unfinished workflows, changed settings or new defects. That is not an unusual failure of the release process; it is the purpose of a development channel.

If a function in this manual is explicitly marked as **Nightly**, it is not necessarily part of the current Stable release yet.

---

## Download

The current Stable version is available here:

- [Latest KST4Contest release](https://github.com/praktimarc/kst4contest/releases/latest)
- [All published releases](https://github.com/praktimarc/kst4contest/releases)

The release page contains the application packages as well as the English and German PDF manuals.

### Which package do I need?

| Operating system | Package | Typical filename |
|---|---|---|
| Windows x64 | ZIP package | `praktiKST-v<version>-windows-x64.zip` |
| Linux x86_64 | AppImage | `KST4Contest-v<version>-linux-x86_64.AppImage` |
| Debian/Ubuntu amd64 | DEB package | `KST4Contest-v<version>-debian-amd64.deb` |
| Fedora/RPM x86_64 | RPM package | `KST4Contest-v<version>-fedora-x86_64.rpm` |
| Arch Linux x86_64 | Arch package | `KST4Contest-v<version>-archlinux-x86_64.pkg.tar.zst` |
| Linux with Flatpak | Flatpak reference | `de.x08.KST4Contest.flatpakref` |
| macOS Apple Silicon | DMG for ARM64 | `KST4Contest-v<version>-macos-arm64.dmg` |
| macOS Intel | DMG for x86_64 | `KST4Contest-v<version>-macos-x86_64.dmg` |

Only download application packages from the official GitHub Releases, the KST4Contest Flatpak repository or the linked AUR packages. Files from other sources may have been built differently, may be outdated or may have been modified.

---

## Installing on Windows

KST4Contest is distributed as a ZIP package for Windows. A conventional installer is not required.

1. Download `praktiKST-v<version>-windows-x64.zip` from the latest release.
2. Extract the ZIP file completely into a dedicated folder.
3. Open the extracted folder.
4. Start `praktiKST.exe`.

Do not start the application directly from the compressed ZIP file. KST4Contest consists of several files and a bundled runtime. Windows can only use this structure reliably after the archive has been extracted completely.

The application settings are not stored in the extracted program directory. They are stored in your user profile:

```text
%USERPROFILE%\.praktiKST\preferences.xml
```

This allows a new application version to be extracted into a different directory without losing the existing settings.

---

## Installing on Linux

Several Linux package formats are available. The appropriate choice depends less on KST4Contest itself than on your distribution and the way you prefer to manage updates.

| Installation method | Useful when … |
|---|---|
| **Flatpak** | updates should be managed centrally and Flatpak is already in use |
| **AppImage** | KST4Contest should run as a portable file without package installation |
| **DEB/RPM** | the distribution’s native package manager should be used |
| **Arch package/AUR** | Arch Linux, Manjaro or EndeavourOS is being used |

### Flatpak

For most Linux users, Flatpak is the simplest way to keep KST4Contest installed and manageable through the system’s update tools. The KST4Contest repository is GPG-signed and contains the Stable, Beta and Nightly channels.

#### Installing Stable from the release file

Download `de.x08.KST4Contest.flatpakref` from the latest release and open it with the software manager provided by your desktop environment.

Alternatively, install it from a terminal:

```bash
flatpak install ./de.x08.KST4Contest.flatpakref
```

#### Adding the KST4Contest repository

The repository only needs to be added once:

```bash
flatpak remote-add --if-not-exists kst4contest \
https://praktimarc.github.io/kst4contest/kst4contest.flatpakrepo
```

You can then install the Stable version:

```bash
flatpak install kst4contest de.x08.KST4Contest//stable
```

#### Installing Beta

```bash
flatpak install kst4contest de.x08.KST4Contest//beta
```

#### Installing Nightly

```bash
flatpak install kst4contest de.x08.KST4Contest//nightly
```

All three KST4Contest channels use the same application ID. Installing multiple KST4Contest channels in parallel is therefore not supported.

For example, to switch from Stable to Nightly:

```bash
flatpak uninstall de.x08.KST4Contest//stable
flatpak install kst4contest de.x08.KST4Contest//nightly
```

This does not automatically remove the personal settings stored in `~/.praktiKST`.

Installed Flatpak applications can be updated with:

```bash
flatpak update de.x08.KST4Contest
```

Depending on the desktop environment, the graphical software manager may also display or automatically install available Flatpak updates. The `flatpak update` command itself performs an update; it does not promise to turn up and run by itself one day.

### AppImage

The AppImage does not require a conventional installation.

1. Download `KST4Contest-v<version>-linux-x86_64.AppImage`.
2. Open a terminal in the download directory.
3. Make the file executable:

```bash
chmod +x KST4Contest-v<version>-linux-x86_64.AppImage
```

4. Start KST4Contest:

```bash
./KST4Contest-v<version>-linux-x86_64.AppImage
```

The AppImage can then be moved to another location, such as `~/Applications`.

### Debian and Ubuntu

Install the DEB package with:

```bash
sudo apt install ./KST4Contest-v<version>-debian-amd64.deb
```

Alternatively, open the file with a graphical package manager.

### Fedora and compatible RPM systems

Install the RPM package with:

```bash
sudo dnf install ./KST4Contest-v<version>-fedora-x86_64.rpm
```

### Arch Linux: installing the release package

The Arch package downloaded from the GitHub Release can be installed directly:

```bash
sudo pacman -U KST4Contest-v<version>-archlinux-x86_64.pkg.tar.zst
```

### Arch Linux: installing from the AUR

Three variants are available in the AUR:

| Package | Content |
|---|---|
| [`kst4contest-bin`](https://aur.archlinux.org/packages/kst4contest-bin) | Pre-built package from the current Stable release |
| [`kst4contest`](https://aur.archlinux.org/packages/kst4contest) | Stable release built locally from source |
| [`kst4contest-git`](https://aur.archlinux.org/packages/kst4contest-git) | Current development state from the `main` branch |

For most users, `kst4contest-bin` is the straightforward option:

```bash
yay -S kst4contest-bin
```

To build the Stable release from source:

```bash
yay -S kst4contest
```

To build the current development state:

```bash
yay -S kst4contest-git
```

All three packages provide the same application and are therefore defined as conflicting with one another. Install only one variant at a time.

AUR updates are included when the selected AUR helper checks for package updates, for example:

```bash
yay -Syu
```

They do not happen automatically merely because the package came from the AUR.

---

## Installing on macOS

> **Best-effort support:** The macOS packages are built together with the other releases, but they are not tested to the same extent as the Windows and Linux versions. Feedback is welcome; fully tested support for every macOS version and hardware variant cannot currently be guaranteed.

Apple Silicon Macs require the package marked `arm64`. Intel Macs require the package marked `x86_64`.

1. Download the appropriate DMG file.
2. Open the DMG file.
3. Drag `KST4Contest.app` into the **Applications** folder.
4. Start KST4Contest from the Applications folder or Launchpad.

The application is not currently notarized by Apple. macOS may therefore block the first launch.

If the application came from the official GitHub Release:

1. Open the Applications folder in Finder.
2. Right-click or Control-click `KST4Contest.app`.
3. Select **Open**.
4. Confirm the launch in the dialog that appears.

Alternatively, macOS may provide an **Open Anyway** button under **System Settings → Privacy & Security**.

---

## Where are the settings stored?

KST4Contest stores its settings and other local working files in the user’s home directory. The application directory and the data directory are separate.

| Operating system | Data directory | Settings file |
|---|---|---|
| Windows | `%USERPROFILE%\.praktiKST\` | `%USERPROFILE%\.praktiKST\preferences.xml` |
| Linux | `~/.praktiKST/` | `~/.praktiKST/preferences.xml` |
| macOS | `~/.praktiKST/` | `~/.praktiKST/preferences.xml` |

Note the spelling `.praktiKST` with an uppercase `KST`. Linux and macOS distinguish between uppercase and lowercase letters. `.praktikst` would simply be a different directory.

An application update does not remove this directory. Even so, creating a backup before a major version change or extensive configuration work is sensible.

---

## Updates

When KST4Contest starts, it checks whether a newer Stable release is available. If it finds one, it displays an information window containing:

- the installed version,
- the latest Stable version,
- a short overview of the main changes,
- the changelog,
- known issues,
- a link to the corresponding GitHub Release page.

![KST4Contest update notification](update_window.png)

The update checker does not install anything. It reports the new version and opens the platform-neutral release page. From there, you must select the appropriate package for Windows, Linux or macOS.

### Updating Windows

1. Close KST4Contest.
2. Download the new Windows ZIP package.
3. Extract it into a new or empty directory.
4. Start the new version.
5. Check that the existing settings have been loaded.
6. Remove the old application directory only after that.

The settings are preserved because they are stored under `%USERPROFILE%\.praktiKST`, not in the application directory.

### Updating an AppImage

1. Download the new AppImage.
2. Make it executable.
3. Start the new file.
4. Remove the previous AppImage only after the new version works.

### Updating Debian and Ubuntu

```bash
sudo apt install ./KST4Contest-v<version>-debian-amd64.deb
```

### Updating Fedora

```bash
sudo dnf upgrade ./KST4Contest-v<version>-fedora-x86_64.rpm
```

### Updating the Arch package

```bash
sudo pacman -U KST4Contest-v<version>-archlinux-x86_64.pkg.tar.zst
```

### Updating an AUR package

Update the installed package through the selected AUR helper, for example:

```bash
yay -Syu
```

### Updating Flatpak

```bash
flatpak update de.x08.KST4Contest
```

### Updating macOS

1. Download the new DMG file for the appropriate architecture.
2. Close KST4Contest.
3. Open the DMG file.
4. Replace `KST4Contest.app` in the Applications folder.
5. Start the new version and check the existing settings.

The configuration under `~/.praktiKST` is not affected.

---

## Problems during the first launch

### Windows reports an unknown application

KST4Contest is not currently signed with a commercial Windows code-signing certificate. Windows or an additional security product may therefore warn about an unknown or rarely downloaded application.

Check the following first:

- Did the file come from the [official KST4Contest release](https://github.com/praktimarc/kst4contest/releases/latest)?
- Does the filename match the published release?
- Was the file downloaded completely?
- Does the security product report a specific detection name or only a general reputation warning?

A warning alone is neither reliable proof of malware nor automatically a false positive. If the origin of the file is unclear, do not run it or restore it from quarantine.

Some users have reported quarantine messages from Norton 360 in particular. If the warning can be reproduced, please create a [GitHub issue](https://github.com/praktimarc/kst4contest/issues) and include:

- the KST4Contest version,
- the complete filename,
- the security product and its version,
- the reported detection name,
- a screenshot of the warning, if possible.

### The AppImage does not start

Check the executable permission first:

```bash
chmod +x KST4Contest-v<version>-linux-x86_64.AppImage
```

Then start the file from a terminal. Error messages shown there are usually more useful than a double-click that simply produces no visible result.

### macOS blocks the application

Use the **Open** function described under [Installing on macOS](#installing-on-macos). Before doing so, verify that the DMG file came from the official GitHub Release.

### The problem remains

First check whether the problem has already been reported under [GitHub Issues](https://github.com/praktimarc/kst4contest/issues). If it has not, create a new issue containing:

- the operating system and version,
- the KST4Contest version,
- the installation method,
- the exact error message,
- the steps required to reproduce the problem.

“It does not work” usually describes the situation accurately, but it is of limited value during diagnosis.