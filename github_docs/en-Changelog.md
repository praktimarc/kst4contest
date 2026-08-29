# Changelog

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-Changelog)

Version history of KST4Contest / PraktiKST.

Published Stable versions and their application packages are available under [GitHub Releases](https://github.com/praktimarc/kst4contest/releases). This page also lists changes from the current development version where they have already been implemented and tested.

---

## v1.42.0 (2026-08-22)

**Shared band context, session-based ON4KST connection and signed macOS packages**

v1.42 brings several previously separate calculations together. Band information, Worked status, NOT-QRV marks, callsign suffixes and frequencies are now used more consistently by the user list, station map, priority calculation and external interfaces.

### Added

- **Visible ON4KST connection state:** A compact `LINK` indicator in the main window displays the actual state of the ON4KST connection. Green means fully authenticated and synchronised, yellow indicates connection setup or synchronisation, and red indicates a lost connection, an error or the delay before the next connection attempt.

- **Shared band-opportunity calculation:** A central `BandOpportunityResolver` evaluates recent QRGs, band designators in station names, active callsign variants, Worked information and NOT-QRV marks. The user list, **New bands**, band-upgrade hint, Priority Score, station map and automatic band selection now use the same basis.

- **Extended band status display:** The band columns now distinguish:
  - `X` for worked on this band,
  - `a` for an offered and unworked band of a completely new callsign,
  - `B+` for an offered and unworked band of a callsign already worked elsewhere, and
  - `o` for a locator square already worked on this band.

  The codes can be combined, for example as `ao` or `B+o`. The additional `a` and `o` indicators can be hidden separately in the GUI settings.

- **50 and 70 MHz support:** Both bands are available in station settings, Worked and NOT-QRV handling, the user list, filters, the internal database, UCXLog processing and the Win-Test listener. Existing databases are extended with the required columns.

- **Global message tabs:** Public messages, ON4KST DX cluster messages and directed messages between other stations can be displayed directly in the main window. The existing separate monitor window remains available and uses the same underlying message stores.

- **Manual QTF input:** The current antenna direction can be changed directly in KST4Contest when PSTRotator is not being used.

- **`MYQTF` message variable:** The current antenna direction can be inserted as a numeric angle in degrees into shortcuts, snippets and other supported message texts.

- **Filter reset:** A dedicated reset button reliably removes the active user-list filter predicates.

- **Map clustering:** Stations close to each other are grouped at lower zoom levels. The selected station and relevant direction opportunities remain individually visible.

- **Hideable path analysis:** The terrain profile and analysis section of the station map can be hidden completely. The selected state is stored and restored at the next application start.

### Changed

- **Session-based ON4KST connection lifecycle:** Each socket, reader, writer, message bus and queue now belongs to an explicitly identified connection session. Delayed threads from an obsolete connection can therefore no longer process data or close its replacement. `ONLINE` is reported only after the login has been accepted and all requested user lists have been received. Connection setup, login and synchronisation use bounded timeouts, while heartbeats, missing inbound traffic, EOF and read or write failures trigger controlled reconnect attempts with backoff where appropriate.

- **Validated ON4KST protocol commands:** Outgoing frames are built centrally and checked for valid categories, locators and prohibited frame delimiters. Because ON4KST maintains one locator per TCP session, the main locator is used for both chat categories and a conflicting secondary configuration is logged instead of sending contradictory commands to the server.

- **More precise QRG recognition:** Complete and relative frequency references continue to be recognised. Bare three-digit numbers are treated as QRGs only when a frequency context is available. Signal reports, band designators and unrelated numbers therefore produce fewer false frequencies.

- **Station-specific frequency context:** For relative QRGs, KST4Contest first uses a band context for the same station which is no more than 30 minutes old. The globally configured fallback band is used only when this context is unavailable.

- **Fallback band dropdown:** The global fallback can only be selected from supported band values. It applies to the complete QRG parser, not merely to DX cluster spots.

- **Consistent QRG formatting:** Frequencies are displayed with at least three decimal places in the user list and message tables.

- **Band-aware AirScout and path calculations:** KST4Contest derives a realistic frequency for each station from its current QRG and known band information. AirScout receives canonical band values. The temporary 430 MHz fallback has been replaced with 432 MHz.

- **Shared propagation-frequency selection:** AirScout, **Calc selected** and the station-map path analysis use the same `PropagationFrequencyResolver`. A band selected explicitly in the Reachability dropdown is respected for manual calculations.

- **Separate callsign variants:** Active chat members are distinguished by their complete callsign, including suffix, and by chat category. `DN9APW`, `DN9APW-2` and similar logins therefore remain separate message targets.

- **Shared base-callsign information:** Worked flags, NOT-QRV information and Priority Scores continue to be evaluated across variants of the same base callsign. Separate message targets no longer result in contradictory Worked information.

- **Corrected Priority Score eligibility:** Stations without a common available band, or with an overriding NOT-QRV mark, are no longer offered as priority candidates. An open band at a station already worked elsewhere can receive a separate Priority Boost.

- **Extended sked creation:** The band is selected from those enabled locally. `SSB` or `CW` is selected explicitly for the Win-Test handover instead of attempting to infer the mode unreliably from the band.

- **More precise Win-Test sked handover:** The QRG must belong to the selected band. Visible KST suffixes are removed for the logging target while portable callsign components are preserved. `ADDSKED` timestamps are generated correctly. A failed handover does not remove the internal KST4Contest sked.

- **Exact sked targets:** The timeline and automatic reminders use the complete visible KST callsign. A sked for `DN9APW-2` is not accidentally sent to another variant of the same base callsign.

- **Reworked beacon and automatic replies:** Both chat categories use one shared timer while retaining separate enable switches and message texts. The minimum permitted interval is one minute and message texts are limited to 120 characters. The stored beacon state is restored from the configuration at startup.

- **Central variable resolution:** Message variables used by beacons, shortcuts, snippets and other generated text are processed by one shared resolver.

- **Improved message tables:** Truncated messages show their complete text in a tooltip. Recognised web addresses can be opened in the system browser.

- **More compact filter bar:** Filters retain their compact arrangement at normal widths and wrap only when the available space is genuinely insufficient. This allows the centre divider to be moved further to the right.

- **DXLog full-log import:** In addition to `contactinfo`, the UCXLog-compatible UDP listener processes `contactreplace`. This allows a complete log broadcast by DXLog.net to be imported.

- **Defined Simplelogfile behaviour:** The selected text file is evaluated once per minute using a fixed callsign pattern. Matches set the global Worked status for all active variants of the base callsign but are not persisted in SQLite. A missing file is created, and read or creation errors do not terminate the periodic task. A database reset does not change the file, so callsigns contained in it are marked as worked again during the next evaluation.

- **Guarded automatic QRG updates:** `MYQRG` is updated only by an enabled interface which actually supplies valid `RadioInfo` or Win-Test `STATUS` packets. An enabled source which provides no data does not remove the need for a functional check or manual QRG maintenance.

- **Improved version comparison:** Versions are compared semantically so that patch releases and Nightly versions are not misclassified by conversion to a floating-point number.

### Fixed

- **Reliable initial user list:** Invalid or incomplete `UA0` member records are rejected and logged individually without preventing alphabetically following members from being processed. Valid entries are staged per category and published as one complete snapshot when the first corresponding `UE` end marker is received.

- **User list disappearing after login:** ON4KST may send additional `UE` frames for the same category after name, state or other live updates. Repeated end markers are now detected and ignored so that an already populated user list cannot be replaced by an empty snapshot.

- **Failed initial connection and lost sockets:** An unavailable server during startup no longer sends KST4Contest into an endless or busy-wait loop. The user interface remains responsive and further attempts use bounded reconnect backoff. Sockets closed by the server, or connections without inbound traffic for an excessive period, are also detected reliably.

- **Message-bus diagnostics:** Correctly processed ON4KST frames are no longer reported additionally as `Critical, detected unhandled Chatmessage`. Only genuinely unknown frames reach the fallback diagnostic branch.

- **Password in diagnostic output:** The ON4KST password is no longer written in plain text to the console or error log during connection setup.

- **Long-running station-selection failure:** Chat members managed by the message thread have been decoupled from the JavaFX view. Simultaneous data and table updates therefore no longer cause broken selection models or concurrent-modification problems after longer runtimes.

- **No phantom chat members from UM3:** Historical or additional server messages no longer create user-list entries for stations which are not actually logged into the chat.

- **Messages to callsigns with suffixes:** Several active variants of the same base callsign no longer overwrite each other. This resolves [Issue #73](https://github.com/praktimarc/kst4contest/issues/73).

- **DX cluster locators:** The reporting and reported station no longer receive the same locator accidentally. This resolves [Issue #48](https://github.com/praktimarc/kst4contest/issues/48).

- **Worked state in “QSO of the other”:** The Worked columns again use the correct transmitting and receiving station.

- **Missing SECONDAP data:** A missing second aircraft-scatter opportunity no longer produces an invalid text selection and JavaFX exception when the field is edited.

- **Historical callsigns:** Highlighting or selecting a callsign which is no longer present in the active user list no longer causes an exception.

- **Win-Test sked time and callsign handling:** Timestamps, band-to-QRG assignment, KST suffixes and portable callsigns are handled correctly.

- **Filter reset:** All filter predicates are actually removed, and the visible button state again matches the effective filter state.

### Documentation and packaging

- The German and English manuals have been systematically checked against the source code, extended and supplied with current screenshots. The documentation now describes not only the controls, but also how information is derived and where the limits of a result lie.

- The AUR provides `kst4contest-bin`, `kst4contest` and `kst4contest-git` for Arch Linux.

- The download page distinguishes between Stable, Beta and Nightly and offers only packages which actually belong to the corresponding channel.

- Nightly packages are built automatically from the current `main` branch. Stable and Beta releases use predictable asset names for the supported platforms.

- **Signed and notarized macOS packages:** The DMG files for Apple Silicon and Intel are signed with an Apple Developer ID and notarized by Apple, with the notarization ticket stapled into the DMG. The first launch now works by double-clicking, without the previous detour through **Open** in the context menu, and the check also succeeds without an internet connection. This applies to Nightly, Beta and Stable packages alike. The Windows packages remain unsigned.

- **Correct bundle identifier and version on macOS:** The application now identifies itself as `de.x08.KST4Contest` rather than `kst4contest.view`, and carries its actual version number in the bundle. Previously every release reported version `1.0` in Finder's **Get Info** panel. Existing settings are unaffected, because KST4Contest stores its data in `~/.praktiKST/` rather than keying it to the bundle identifier.

### Known limitations

- The active terrain provider uses Open-Meteo with Copernicus GLO-90 data and no more than 100 elevation samples per path.

- The atmospheric K factor is currently fixed at `4/3`.

- An antenna height of 10 metres above ground is assumed for the remote station.

- Aircraft-scatter information from AirScout and the terrain analysis in the station map remain separate assessments.

- More detailed configuration of station height, frequency and K factor is being tracked in [Issue #74](https://github.com/praktimarc/kst4contest/issues/74).

The published version is available as [Release v1.42.0](https://github.com/praktimarc/kst4contest/releases/tag/v1.42.0).

---

## v1.41.1 (2026-07-08)

**Hotfix for text input and focus handling**

### Fixed

- The message input field was unexpectedly cleared after some time or during particular UI updates.
- Filtering or selecting a station could move the input focus back to the send field unintentionally.

The corrected version is available as [Release v1.41.1](https://github.com/praktimarc/kst4contest/releases/tag/v1.41.1).

---
## v1.41.0 (2026-07-01)

**Station map, bounded message stores and screen-aware main window**

### Added

- **Station map:** An interactive OpenStreetMap-based map displays active chat members for which a usable locator is available.

- **Antenna sector and connection line:** The map shows the current local QTF, configured antenna beamwidth, maximum QRB and the path to the selected station.

- **Maidenhead grid:** A locator grid adapted to the current zoom level provides geographical orientation.

- **Terrain profile:** An elevation profile for a selected station can be requested from the Open-Meteo Elevation API. The active source uses Copernicus GLO-90 data and no more than 100 evenly distributed samples.

- **Geometrical path analysis:** The calculation includes line of sight, Earth curvature using `k = 4/3`, radio and terrain horizons, the first Fresnel zone and a rough obstruction estimate.

- **Local map proxy:** Leaflet is bundled with the application. Map tiles are loaded through a local proxy so that no external JavaScript library is required at runtime. An Internet connection is still required for OpenStreetMap tiles and online elevation data.

### Changed

- **Bounded message stores:** The global chat-message list is reduced from more than 30,000 entries to 25,000. The separate DX cluster store is reduced from more than 10,000 entries to 8,000.

- **Screen-aware startup size:** The main window is checked against the visible area of the primary screen at startup and reduced or repositioned when necessary.

- **More compact user interface:** Several UI sections were adapted for smaller displays and movable dividers.

### Scope of the map function

The station map and AirScout may refer to the same remote station, but their calculations remain separate. Aircraft used by AirScout are not included in the terrain profile.

Classes for Copernicus GLO-30, offline DEM imports and additional terrain providers existed in the source tree but were not part of the active calculation chain in v1.41. The elevation source actually used was Open-Meteo based on Copernicus GLO-90.

---


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

- ~~Lifetime for worked status (automatic reset)~~ ✅ **Implemented in v1.40** (3-day lifetime, no manual reset needed anymore)
- Filtering the "Cluster & QSO of others" window to own QTF
- Further topography-based calculations for direction warnings
