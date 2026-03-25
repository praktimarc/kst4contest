# Changelog

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-Changelog)

Version history of KST4Contest / PraktiKST.

---

## v1.263 (2025-06-08)
**AirScout Communication and Login Name**

**Changed:**
- AirScout communication fundamentally revised: Only stations with QRB < max-QRB are now sent to AirScout.
- Query interval extended from 12 seconds to **60 seconds**.
- Significantly less computation load and message traffic → more stable AirScout tracking.
- Name of the AS client and AS server is now configurable from the Preferences (was previously hardcoded to "KST" / "AS").

**Fixed:**
- "Track in AirScout" button was very sluggish → greatly improved by new communication logic.
- Name in chat is now saveable (bug fixed).
- Visual corrections before and after login.
- Bug fixed that was reported by 9A2HM (Kreso).

---

## v1.262 (2025-05-21)
**Freeze Fix for Early Message Delivery**

**Fixed:**
- ON4KST sometimes delivers messages before login is complete. This caused errors in the message processing engine → now fixed.

---

## v1.26 (2025-05)
**Multi-Channel Login and Dark Mode**

**New:**
- **Dark Mode**: Toggle via `Window → Use Dark Mode`.
- **Multi-channel login**: Simultaneous login to two chat categories.
- **Opposite station multi-callsign login tagging**: Support for stations with multiple callsigns.

**Changed:**
- Colouring mechanism revised: Colours can now be customised via CSS.

**Fixed:**
- Station tagging completely revised and corrected.

---

## v1.251 (2025-02)
**Bugfix for UDP Broadcast Spot Info**

**Fixed:**
- Problem reading UDP broadcast spot information fixed (reported by Steve Clements – thank you!).
- Station tagging (further improved).

---

## v1.25 (2025-02)
**Wishlist Time**

**New:**
- **New settings tab: Messagehandling**
  - Auto-reply to incoming messages configurable.
  - Automatic reply with own CQ QRG when someone asks for it.
  - Configurable default filter for the userinfo window *(for Gianluca :-) )*.
- **Coloured PM rows**: New private messages appear red and fade every 30 seconds from yellow to white *(idea by IU3OAR, Gianluca)*.

**Fixed:**
- Stations with suffixes like "-2" and "-70" were not being marked as worked → now ignored, station is correctly marked.

---

## v1.24 (2024-11)
**Wishlist + DX Cluster Spots**

**New:**
- Button to open the **QRZ.com profile** of the selected station.
- Button to open the **QRZ-CQ profile** of the selected station.
- **DX Cluster Server integration**: Direction warnings are sent as spots to the logging software (when QRG is known).

*(Coloured PM row feature also added – tnx Gianluca)*

---

## v1.23 (2024-10)
**Built-in DX Cluster Server**

**New:**
- KST4Contest now contains a **built-in DX cluster server**.
- Generates DX cluster spots and sends them to the logging software when a direction warning is triggered and a QRG is known.
- Spotter callsign must differ from the contest callsign (for correct filtering in the logging software).

*(Idea by OM0AAO, Viliam Petrik – thank you!)*

---

## v1.22 (2024-05)
**Usability Improvements and AirScout Button Fix**

**New:**
- New variables (tnx OM0AAO, Viliam Petrik):
  - `MYLOCATORSHORT`
  - `MYQRGSHORT`
  - `QRZNAME`

**Changed:**
- Send field focus: After clicking a callsign in the user list, the send field immediately receives focus – no double-click needed *(tnx Gianluca)*.

**Fixed:**
- Worked-station filter is now live: Worked stations disappear immediately when the filter is activated *(tnx Gianluca)*.
- QRB sorting was lexicographic → now numeric *(tnx Alessandro Murador)*.
- AirScout "Show Path" button: Click now maximises AirScout and correctly shows the path.

---

## v1.21 (2024-04)
**Usability Improvements**

**Changed:**
- Window sizes and divider positions are saved in the configuration file when clicking "Save Settings" and restored on startup.
- Filter section as flowpane → better display on smaller screens.

---

## v1.2 (2024-04)
**Band Selection and NOT-QRV Tags**

**New:**
- **Band selection**: Selectable in Preferences which bands are active. Only buttons and fields for selected bands appear in the UI. Save and restart required.
- **NOT-QRV tags per station and band**: Stations can be marked as "not QRV" for each band. Combinable with the user list filter.
- **QTF arrow**: The "Show path in AS" button now shows an arrow with the QTF of the selected station.

---

## Earlier Versions

### v1.1
First publicly released version. Core features:
- Worked marking via Simplelogfile and UDP
- Sked direction highlighting
- QRG detection
- Text snippets and shortcuts
- AirScout interface (first version)
- Interval beacon
- PM catching for public messages containing your own callsign
- Update notification service

---

## Planned Features

- `MYQTF` variable (own antenna direction as text)
- Lifetime for worked status (automatic reset)
- Filtering the "Cluster & QSO of others" window to own QTF
- Further topography-based calculations for direction warnings
