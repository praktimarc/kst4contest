# Changelog

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-Changelog)

Version history of KST4Contest / PraktiKST.

---

For the latest changelog, please refer to GitHub. The previous changelog is below.

## v1.40 (2026-02-16)
**Major Feature Release: Score System, AP Timeline, Win-Test, PSTRotator**

**New:**
- **Chatmember Score System**: Every chat member is automatically scored based on antenna direction, activity time, message count, active bands, frequencies, sked direction (degrees), and other factors. Top candidates are highlighted in a dedicated list.
- **AP Timeline**: For each minute of possible aircraft arrival, up to 4 highly-scored stations are shown that should be workable. Aircraft with the highest potential are preferred over the fastest arrival. Chat members whose antenna is not pointing towards you are shown transparently.
- **Win-Test Support** (Beta since v1.31, now fully configurable): Log synchronisation, frequency parsing and **sked handover via UDP** fully integrated. Can be enabled/disabled in Preferences.
- **PSTRotator Interface** (Beta since v1.31, now fully configurable): Rotator position updates directly from KST4Contest. Can be enabled/disabled in Preferences.
- **QSO Sniffer**: Messages from configurable callsign lists are automatically forwarded to the PM window.
- **Band Alert for logged stations**: When a station is logged, a hint appears if that station has another active band that you are also QRV on.
- **Sked Reminder ALERT**: A sked alarm with automatic messages in configurable intervals (2+1 / 5+2+1 / 10+5+2+1 minutes before the sked) can be set up for each chat member, plus acoustic and visual notification.
- **Load chat history on startup**: Chat server history is loaded on connect to immediately see active members and recent messages.
- **Skedfail button**: In the FurtherInfo panel, a sked failure can be marked for a chat member, which lowers their priority score.

**Changed:**
- AP notes added to internal DX cluster spots.
- Chat member table scrolling follows the current message selection automatically.
- Generic auto-reply and QRG auto-reply now fire a maximum of once every 45 seconds per callsign (prevents spam and message ping-pong).
- New saveable settings: ServerDNS/Port, PSTRotator interface, Win-Test interface, callsign sniffer, Dark Mode on by default.
- Date column removed from chat table (time only – saves space).

**Fixed:**
- User list now automatically sorted on every new member sign-on.
- Posonpill messages now terminate exactly one client instance (no longer affects all instances or wtKST).
- wtKST: crash on KST4Contest disconnection fixed.
- Multiple issues with callsign suffixes like `/p`, `-2`, etc. fixed throughout.
- `QTFDefault` was not saved correctly → fixed.
- AirScout watchlist (ASWATCHLIST) was not being updated → fixed.
- Dark Mode: QRG fields not displayed at full size → fixed.
- Version number display corrected.

---

## v1.31 (2025-12-13)
**Win-Test + PSTRotator Beta, QSO Sniffer, DNS Hotfix**

**New:**
- **Win-Test support** (Beta, not yet deactivatable): Log synchronisation and frequency parsing.
- **PSTRotator support** (Beta, not yet deactivatable).
- **QSO Sniffer**: Messages from configurable callsigns are forwarded to the PM window.

**Changed:**
- **DNS server changed**: From `www.on4kst.info` to `www.on4kst.org` (hotfix). The DNS server is now configurable in Preferences.

**Fixed:**
- Endless loop in error case freezes the client → fixed.

---

## v1.266 (2025-10-03)
**AirScout Fix for Callsigns with Suffix**

**Fixed:**
- AirScout interface did not work when the login callsign contained a suffix (e.g. `9A1W-2`). AirScout cannot handle this format – only the base callsign without suffix is now passed to AirScout.

*(Bug reported and tested by 9A2HM / Kreso – many thanks!)*

---

## v1.265 (2025-09-28)
**Direction Buttons Stay Coloured When Active**

**Fixed:**
- Direction buttons (N / NE / E etc.) now keep their highlight colour when activated, making the active state immediately visible.

---

## v1.264 (2025-08-02)
**Simplelogfile: Improved Callsign Recognition**

**Fixed:**
- Callsigns like `S53CC`, `S51A`, etc. were not being marked as worked in the SimpleLogFile interpreter → recognition pattern improved.

*(Bug reported by Boris, S53CC – thank you!)*

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
- ~~Lifetime for worked status (automatic reset)~~ ✅ **Implemented in v1.40** (3-day lifetime, no manual reset needed anymore)
- Filtering the "Cluster & QSO of others" window to own QTF
- Further topography-based calculations for direction warnings
