# Features

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-Funktionen)

Overview of all main features of KST4Contest.

---

## Sked Direction Highlighting

One of the core features: when a station makes a sked request **towards your direction**, it is highlighted **green and bold** in the user list.

### How does it work?

The calculation is based on the following logic:

- When station A sends a sked request to station B, it is assumed that A is pointing its antenna towards B.
- If the resulting direction from A to your own station is within half the beamwidth of your own antenna, A is highlighted.

**Example** (beamwidth 69°, half-angle 34.5°):

| Situation | Result for DO5AMF in JN49 |
|---|---|
| Sked from F5FEN → DM5M | ✅ Highlighted (F5FEN points towards DM5M, close to JN49) |
| Sked from DM5M → F5FEN | ✅ Highlighted (DM5M replies towards F5FEN) |
| F1DBN is uninvolved | ❌ No highlighting |
| DO5AMF/P (different location) | ❌ No highlighting for sked reply |

The calculation does not include topographic path calculations – this is a deliberate simplification. It may be added in a future version.

> Configuration: [Configuration – Antenna Beamwidth](Configuration#antenna-beamwidth)

---

## Sked Direction Spots (Built-in DX Cluster)

From **v1.23**: Direction warnings are forwarded as DX cluster spots to the logging software when a QRG is known. Details: [DX Cluster Server](en-DX-Cluster-Server).

---

## QRG Detection (QRG Reading)

KST4Contest processes every line of text flowing through the channel and automatically extracts **frequency references**. These are displayed in the user list in the **QRG column**.

Recognised formats: `144.205`, `432.088`, `.205` (with configured band assumption), etc.

**Benefit**: Without asking, you can directly look up a station's calling frequency and decide whether a contact is possible.

---

## Worked Callsigns, New Bands and New Grid Squares

KST4Contest distinguishes between three pieces of information which may look similar during a contest but answer different questions:

1. Has this callsign been worked before?
2. Has this callsign been worked on a particular band?
3. Has the four-character Maidenhead grid square already been worked, possibly with a different station?

This distinction matters. A callsign already worked on one band may still be useful on another. Conversely, a new callsign may be located in a grid square which is already in the log.

### Worked information from the log

[Log Synchronisation](en-Log-Sync) imports new QSOs from the logging application. The amount of information available depends on the interface being used:

- The file-based Simplelogfile interpreter detects callsigns only. It can therefore set only the global Worked status.
- The QSO UDP interfaces and the Win-Test network listener can also provide the band.
- If the log packet contains a valid locator, KST4Contest additionally stores the worked four-character grid square for that band.

Missing information is not guessed. A QSO without a locator does not create a worked-grid record, and a Simplelogfile match does not create a band-specific Worked mark.

### Meaning of the band columns

The shared **worked** column contains only the bands enabled under **Station → My station uses …**. Its cells deliberately use short status codes because a full description would leave very little room for the actual user list.

| Display | Meaning |
|---|---|
| `X` | The callsign has been worked on this band. |
| `a` | The station offers this band, the band has not been worked yet, and the callsign has not been worked on any band. |
| `B+` | The station offers this band and it has not been worked yet. The callsign has already been worked on another band. If the separate `a` display is disabled, a completely new callsign is also shown as `B+`. |
| `o` | The station's four-character grid square has already been worked on this band, regardless of callsign. |
| empty | No matching information is available for this band. This does not mean that the station is not QRV. |

The `o` is an independent overlay and can therefore be combined with the other codes. Examples include `Xo`, `ao` and `B+o`. A single `o` means that the grid square has been worked on this band, while the displayed callsign has neither a Worked mark nor a current band opportunity.

![Band-specific Worked status and worked grid squares](worked_band_status.png)

### How is a band opportunity derived?

KST4Contest displays `a` or `B+` only if it can derive an open band opportunity. The calculation combines:

1. the bands enabled for the local station,
2. QRGs detected for the remote station during the previous 30 minutes,
3. explicit band designators in the remote station's name field,
4. stored per-band Worked marks, and
5. manually assigned NOT-QRV marks.

Active chat entries with the same normalised callsign are evaluated together. This is particularly relevant when the station appears in several chat categories or with different visible callsign variants. An explicit band designator in the name field remains useful while the corresponding chat entry is active. A band derived from a detected QRG expires after 30 minutes.

The remaining set contains only bands which are enabled locally, known for the remote station and not yet worked. A manual NOT-QRV mark overrides automatically detected evidence. The chat category alone is not sufficient evidence that an individual station is QRV on a particular band.

The global Worked mark does not decide whether a band opportunity exists. It merely selects `a` or `B+` for the display. The actual opportunity calculation uses per-band Worked information.

### Meaning of `wkdany`

The **wkdany** subcolumn combines the global callsign and grid-square status:

| Display | Meaning |
|---|---|
| empty | Neither the callsign nor the four-character grid square has been worked. |
| `x` | The callsign has been worked on at least one band. |
| `o` | The four-character grid square has been worked on at least one band. |
| `xo` | Both the callsign and the grid square have been worked. |

`wkdany` is deliberately band-independent. The lower-case `x` must therefore not be confused with the upper-case `X` in a band column. The global status is used for the overview and the global **wkd** filter; it is not a substitute for per-band Worked information.

### NOT-QRV marks

If a station reports that it is not QRV on a particular band, mark this in the selected station's **Further Info** panel:

1. Select the station in the user list.
2. Enable the relevant band under **Not QRV**.
3. Use **tag not qrv all** only if the station should not be requested on any supported band.

The individual NOT-QRV controls are shown for the bands enabled at the local station. **tag not qrv all**, however, marks every supported band, including bands which are not currently visible in the user interface. The state is stored per band under the normalised callsign and propagated to its active chat variants.

![Per-band NOT-QRV marks in the Further Info panel](not_qrv_controls.png)

NOT-QRV is a manual correction and therefore takes precedence over detected QRGs and band designators in the name field. The affected band is no longer offered as `a` or `B+`, is not counted as an opportunity by the **New bands** filter and is excluded by the corresponding band filter.

In plain terms: an automatically detected hint means "probably active on this band". A manual NOT-QRV mark means "do not request this station on this band". The next detected number must not silently reverse that decision.

### Storage and lifetime

Worked, NOT-QRV and worked-grid information is stored in the internal SQLite database and restored on the next start. Entries expire automatically after three days, so a reset before every contest is normally unnecessary.

A manual reset under **Workedstn database** removes all Worked marks, NOT-QRV marks and stored worked grid squares. The known callsign rows remain in the database. See [Worked Station Database Settings](en-Configuration#worked-station-database-settings) for details.

## Direction Filter

Shows only stations in the user list that are located in a specific direction. Toggle using the N / NE / E / SE / S / SW / W / NW buttons or by entering degrees manually.

Useful: While calling CQ in a specific direction, only show stations in that direction.

---

## Distance Filter

Hide stations beyond a maximum distance. The **"Show only QRB [km] <="** button is a toggle.

---

## Filters for Worked Status, New Bands and New Grid Squares

The filters above the user list use the same information as the Worked columns:

- **wkd** hides callsigns which have been worked on at least one band.
- The individual band buttons hide a station if the callsign has already been worked on that band or has manually been marked NOT QRV there.
- **New bands** shows only stations for which at least one locally enabled and unworked band is known. It evaluates recent QRG detections and band designators in the name field; NOT-QRV takes precedence.
- **Only new grids** shows only stations whose four-character grid square has not been worked on any band. Stations without a valid locator do not pass this filter.
- **Grid color** does not filter the list. When enabled, it gives the QRA cell of an already worked grid square a slightly darker background. New grid squares retain the normal table colour.

Several active filters are applied together. A station remains visible only if it satisfies every selected condition. The filters react immediately to new log entries and changed NOT-QRV marks.

Operation and layout of the filter bar: [User Interface – Filters](en-User-Interface#filters).

---

## Coloured PM Rows (from v1.25)

New private messages appear in **red**. The colour fades every 30 seconds from yellow to white – like a rainbow fade. This makes it immediately clear how recent a message is.

*(Idea by IU3OAR, Gianluca Costantino – thank you!)*

---

## PM Catching

Some users accidentally post direct messages publicly, e.g.:

```
(DM5M) pse ur qrg
```

KST4Contest detects such messages that contain your own callsign and automatically sorts them into the **private messages table**. No messages are missed this way.

---

## Multi-Channel Login (from v1.26)

Simultaneous login to **two chat categories** (e.g. 144 MHz and 432 MHz). Both chats are monitored in parallel.

---

## Dark Mode (from v1.26)

Toggle via: **Window → Use Dark Mode**

For individual colour adjustments: edit the CSS file (path in the program settings).

---

## Opposite Station Multi-Callsign Login Tagging (from v1.26)

Support for stations that are active in the chat with multiple callsigns simultaneously (e.g. expedition setups).

---

## QRZ.com and QRZ-CQ Profile Buttons (from v1.24)

For selected stations in the user list, there are direct buttons to open the **QRZ.com profile** and the **QRZ-CQ profile** in the browser.

---

## Sked Reminders with ALERT (from v1.40)

A sked reminder service with automatic messages can be activated for each chat member. Configurable interval patterns:

- **2+1 minutes**: Messages at 2 min and 1 min before the sked.
- **5+2+1 minutes**: Messages at 5, 2 and 1 min before the sked.
- **10+5+2+1 minutes**: Messages at 10, 5, 2 and 1 min before the sked.

In addition to the automated messages to the remote station, there is an **acoustic and visual notification** for your own operator so no sked is ever missed.

Activate from the FurtherInfo panel of the corresponding station.

---

## QSO Sniffer (from v1.31)

The QSO sniffer monitors the chat for messages from a configurable callsign list and automatically forwards them to the **PM window**. This prevents relevant messages from being lost in the general chat traffic.

Configuration: [Configuration – Sniffer Settings](en-Configuration#sniffer-settings-from-v131)

---

## Win-Test Integration (from v1.31, fully configurable from v1.40)

KST4Contest fully supports [Win-Test](https://www.win-test.com/) as a logging programme:

- **Log synchronisation**: Worked stations are automatically retrieved from Win-Test and marked in the user list.
- **Frequency parsing**: The current TRX frequency is read from Win-Test UDP packets and populates the `MYQRG` variable.
- **Sked handover (SKED push via UDP)**: Agreed skeds from KST4Contest can be pushed directly to Win-Test, so the remote callsign appears in Win-Test's sked window.

Details: [Configuration – Win-Test Network Listener](en-Configuration#win-test-network-listener)

---

## PSTRotator Interface (from v1.31, fully configurable from v1.40)

KST4Contest can control antenna direction directly via **PSTRotator**. When a station is selected in the user list, the rotator can automatically be turned to the QTF of the selected station.

Configuration: [Configuration – PSTRotator Settings](en-Configuration#pstrotator-settings-from-v131)

---

## Band Upgrade Hint after a Log Entry

When UCXLog or Win-Test reports a new log entry with band information, KST4Contest checks whether the worked station still offers another common band.

The calculation follows the same rules as `a`, `B+` and the **New bands** filter: locally enabled bands, recent QRG detections, band designators in the name field, per-band Worked marks and NOT-QRV marks. Active chat variants of the same normalised callsign are evaluated together.

If at least one common and unworked band remains, a blinking hint appears for approximately twelve seconds. It includes the callsign and the remaining bands, for example `BAND+ DL0ABC 432, 1296`. Its tooltip also lists the enabled, worked and NOT-QRV bands used for the decision. If general notification sounds are enabled, KST4Contest also plays a short sound.

The Simplelogfile interpreter cannot trigger this hint reliably because it provides no band information for the QSO which has just been logged.

Configuration: [Band Upgrade Hint after a Log Entry](en-Configuration#band-upgrade-hint-after-a-log-entry).

Worked, NOT-QRV and worked-grid data expire automatically after three days. See [Worked Station Database Settings](en-Configuration#worked-station-database-settings) for the lifetime and manual reset behaviour.

---

## Chatmember Score System / Priority List (from v1.40)

KST4Contest automatically calculates a **priority score** for each active chat member. The score is derived from:

- Antenna direction of the remote station (is it pointing towards me?)
- QRB (distance)
- Activity time and message count
- Active bands and frequencies
- AP availability (AirScout)
- Sked direction (degrees)
- Sked success rate and skedfail markings

The top candidates are highlighted in a dedicated priority list, helping you not to miss the most important contacts during contest stress.

Stations with a failed sked can be marked using the **Skedfail button** in the FurtherInfo panel – this temporarily lowers their score.

---

## AP Timeline (from v1.40)

A visual timeline shows up to 4 highly-scored stations per minute slot that should be workable via aircraft scatter. Prioritisation criteria:

- **Highest reflection potential** is preferred (not necessarily the fastest arrival).
- Stations towards which your antenna is not pointing are shown **transparently**.

This gives the contest operator a quick overview of which stations will be reachable via which aircraft and at what time.

---

## Interval Beacon

Automatic CQ messages in the public channel at a configurable interval. Recommended: use the `MYQRG` variable so the current frequency is always accurate. Details: [Configuration – Beacon Settings](Configuration#beacon-settings).

---

## Simplelogfile

File-based log evaluation using regex. Details: [Log Synchronisation](Log-Sync#method-1-universal-file-based-callsign-interpreter-simplelogfile).

---

## Cluster & QSO of Others

A separate window showing the QSO flow between other stations. Particularly interesting during quieter night-time hours of a contest. This window can be minimised when not needed. Future plan: filtering to stations in your selected QTF.

---

## Station Map (from v1.41)

An interactive OpenStreetMap-based map showing the geographic position of all active chat members.

**Features:**

- Station markers with callsign labels, coloured by activity and sked state
- Antenna **beam cone** visualisation for the own station
- **Connection line** to the currently selected station
- **Maidenhead grid** overlay (QRA locator grid)
- **Path profile chart**: Terrain elevation cross-section between own station and the selected station, including Fresnel zone analysis and obstruction/horizon detection
- Multiple terrain data sources: **Copernicus GLO-30** (high-resolution DEM), **Open-Meteo API**, synthetic fallback, and **offline DEM import** for air-gapped use
- Aircraft scatter path analysis integrated with the terrain data

The map works in packaged environments (AppImage, Flatpak) without internet access to external CDNs: map tiles are fetched via a local tile proxy, and the Leaflet.js library is bundled inside the application.

---

## Optimised Message Handling / 30,000 Message Limit (from v1.41)

The internal chat and message tables are capped at **30,000 entries**. Older messages are automatically discarded when the limit is reached. This keeps memory usage and rendering performance stable during multi-day contest operations.

---

## Screen-Aware Window Sizing (from v1.41)

On startup, KST4Contest calculates a screen-aware size for the main window:

- The stored window size from the previous session is used – but **never larger than the current screen**.
- If KST4Contest was last used on a larger monitor, the window is automatically scaled down to fit the current display without clipping.
- The UI layout is more **compact and responsive on smaller screens**, showing the same information in less space.

This prevents unusable oversized windows when switching between machines or monitors.
