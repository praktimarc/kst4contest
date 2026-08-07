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

## Skeds and Sked Reminders

> Available from v1.40; band, callsign and Win-Test handling extended in Nightly / v1.42.

A sked is more than a reminder tied to a particular time. During a contest, it must become visible early enough, move the agreed station up the priority list and – if required – remind the remote station as well.

KST4Contest therefore treats three tasks separately:

1. The sked is stored internally and included in the priority calculation.
2. The scheduled contact appears in the AP and sked timeline.
3. Automatic private reminder messages can optionally be sent before the agreed time.

When the Win-Test network listener is enabled, KST4Contest also attempts to hand the sked over to Win-Test. A failed handover neither removes nor prevents the internal sked.

### Creating a sked

First select the required station in the user list. The sked controls then appear at the bottom of the **Further Info** section.

| Control | Function |
|---|---|
| **Sked in** | Sets the number of minutes until the sked. Available values are 2 through 15 and 20 minutes. |
| **Band** | Selects the sked band. The dropdown contains the local bands enabled under **Station → my station uses …**. |
| **Mode** | Sets the mode passed to Win-Test. Available values are `SSB` and `CW`. This selection does not affect the internal sked or reminder PMs. |
| **Create sked** | Creates the internal sked and, if the Win-Test network listener is enabled, also attempts the Win-Test handover. |
| **Remind-PM in** | Enables automatic private reminder messages before the sked. |
| **2+1**, **5+2+1**, **10+5+2+1** | Selects how many minutes before the sked the reminder PMs are sent. |

![Sked controls in the Further Info section](sked_controls.png)

KST4Contest attempts to preselect a useful band. It checks the following information in this order:

1. a QRG of the selected station which is no more than 30 minutes old and belongs to a locally enabled band,
2. an unambiguous band designator in the station's name field, and
3. the first locally enabled band.

Active callsign variants belonging to the same base callsign are evaluated together when looking for recent band information. A manual NOT-QRV mark is taken into account by the automatic selection. The operator can still select another band explicitly when a different arrangement has been made.

### Effect on the Priority Score

A stored sked raises the score of the normalised base callsign:

| Time relative to the sked | Contribution to the score |
|---|---:|
| more than 15 minutes before the sked | `+40` |
| 15 to 3 minutes before the sked | continuous increase from `+300` towards `+1200` |
| less than 3 minutes before until 1 minute after the sked | `+5000` |
| more than 1 minute after the sked | no remaining sked boost |

The strong weighting immediately around the scheduled time is intentional. An agreed sked should not disappear from the priority list merely because another station is currently very active but has no fixed appointment.

The score is calculated for the base callsign. A sked created for `DN9APW-2` therefore also affects the shared score of other active `DN9APW` variants. The actual message target nevertheless remains `DN9APW-2` in the chat category selected when the sked was created.

The sked is removed from the internal list five minutes after its scheduled time.

### Reminder PMs

Reminder PMs are only scheduled when **Remind-PM in** is enabled. Depending on the selected pattern, KST4Contest sends a private message such as the following two and one minute before the sked:

```text
[KST4C Autoreminder] sked in 2 min
```

The message is sent to the complete visible KST callsign in the chat category in which the sked was created. A sked for `DN9APW-2` is therefore not accidentally sent to `DN9APW`, `DN9APW-70` or a similarly named station in another category.

When a reminder is actually triggered, KST4Contest also displays the visual **SKED** indication. If simple notification sounds are enabled, a short sound is played as well. Merely arming a reminder does not start the blinking indication.

Creating a new set of reminders for the same complete callsign replaces the previously scheduled reminders for that callsign.

### Storage and limitations

Skeds and reminder schedules are kept in memory only. Any skeds which are still required must be recreated after restarting KST4Contest.

The automatic band selection is derived from available chat information. It cannot prove that the station is still operating on the most recently mentioned QRG. Check the band, time and mode before pressing **Create sked**.

Operation: [Station Info Panel](en-User-Interface#station-info-panel-further-info)

Display: [AP and Sked Timeline](#ap-and-sked-timeline)

Win-Test handover: [Log Synchronisation – Win-Test](en-Log-Sync#win-test)


## QSO Sniffer (from v1.31)

The QSO sniffer monitors the chat for messages from a configurable callsign list and automatically forwards them to the **PM window**. This prevents relevant messages from being lost in the general chat traffic.

Configuration: [Configuration – Sniffer Settings](en-Configuration#sniffer-settings-from-v131)

---

## Win-Test Integration

KST4Contest uses a dedicated listener for the native Win-Test network protocol. It provides three separate functions:

- importing new QSOs including band and, where available, locator information,
- reading the current QRG from Win-Test STATUS packets, and
- handing internally created skeds over to the Win-Test network as `ADDSKED` packets.

The sked handover does not replace a missing QRG with a fixed default frequency. KST4Contest only sends a Win-Test sked when it can determine a QRG which belongs to the selected band. The internal sked, timeline and reminder PMs continue to work independently.

A visible KST suffix such as `-2`, `-70` or `-144` is retained inside KST4Contest but removed from the callsign passed to the Win-Test log. Portable components such as `/P`, `/M` and country prefixes are preserved.

Setup and data handling: [Log Synchronisation – Win-Test](en-Log-Sync#win-test)

Settings: [Win-Test Network Listener](en-Configuration#win-test-network-listener-from-v131)


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

## Priority Score and Priority List (from v1.40)

### Why is a score needed at all?

A conventional chat user list initially tells the operator only which stations are logged in. That is not enough during a contest. The operator must also consider which stations have not yet been worked, which bands may still be available, where the antenna is pointing, whether a suitable aircraft is approaching and whether an agreed sked is about to begin.

With a short list, much of this can still be handled mentally. As the contest continues, several bands are used and two chat categories are monitored at the same time, the same decision has to be reconstructed over and over again.

KST4Contest therefore combines the available information into a priority score. The score does not answer whether a QSO will definitely be possible. It supports the more practical question:

> Which of the currently visible stations should I examine next?

### When is a station excluded?

Before applying the weighted factors, KST4Contest checks whether a known band opportunity exists. All active chat entries belonging to the same normalised base callsign are evaluated together.

The calculation uses:

1. the bands enabled in the local station settings,
2. QRGs detected for the remote station during the previous 30 minutes,
3. explicit band designators in the name fields of its active chat entries,
4. stored per-band Worked marks, and
5. manually assigned NOT-QRV marks.

NOT QRV takes precedence over automatically detected frequencies and band designators.

If the remote station’s bands are known but none of them is both enabled locally and still available, the station receives a score of `0`. The same applies when every common band opportunity has already been worked.

A station is not excluded merely because all band information is missing. An unknown band opportunity is not the same as a known incompatibility. In this case, the station is removed from consideration only if all locally enabled bands have been manually marked NOT QRV for that station.

Stations with a score of `0` remain visible in the user list but are not included in the priority list.

### Which information raises or lowers the score?

The score combines several independent hints. One factor will therefore not normally determine the final position on its own.

| Factor | Effect on priority |
|---|---|
| Worked status | A callsign which has not been worked on any supported band receives a higher initial priority. A station which has already been worked is ranked lower but remains a candidate when another band opportunity is available. |
| Available bands | Several common and unworked bands raise the priority. An additional boost for band-upgrade cases can be enabled separately. |
| Distance | Distances below 200 km are weighted lower. The range between 200 km and the configured maximum QRB is preferred. Stations beyond the maximum QRB are reduced substantially. If the QRB is unavailable, this factor is omitted. |
| Antenna direction | The score rises when the QTF to the station lies within half of the configured antenna beamwidth around the current local QTF. The closer both directions are, the stronger the effect. |
| AirScout | At least one currently reachable aircraft raises the score. An expected AP opportunity in zero, one or two minutes receives an additional time-dependent weighting. |
| Recent chat activity | A message received during the previous minute has a stronger effect than one received during the previous three minutes. Several incoming lines within the activity window raise the score further. |
| Positive signals | Detected terms such as `QRV`, `READY`, `RGR`, `OK`, `TNX` or comparable configured text patterns are treated as a positive hint for several minutes. |
| Reply behaviour | If another visible chat line from the station follows an outgoing `/cq` message quickly, the averaged reaction time raises the score. If no such line arrives before the configured timeout, a negative mark is added. |
| Skeds | A scheduled contact initially adds a small amount of priority. During the final 15 minutes, its influence increases continuously. From three minutes before until one minute after the scheduled time, the sked receives very high priority. |
| Failed attempt | **Sked fail** strongly reduces the station’s score until the mark is removed with **Reset fail** or KST4Contest is restarted. |

The default activity window for counting incoming messages is 180 seconds. A message received during the previous 60 seconds is evaluated separately as current activity. The default no-reply timeout is 13 minutes.

For reply behaviour, KST4Contest cannot prove that a later public or private line is actually a reply to the operator’s request. Any subsequent line received from the same station therefore ends the pending response-time measurement. This is a practical approximation, not a statistically reliable response rate.

### What does a scheduled contact mean for the score?

A sked is a time-dependent operating commitment. An imminent sked must therefore take precedence over most normal activity and distance hints. Without this weighting, a station which happens to be very active in the chat could displace an agreed contact from the priority list.

The strongest sked boost is deliberately limited to the period from three minutes before until one minute after the scheduled time. A sked further in the future remains relevant but should not yet dominate current operation.

The score is calculated for the normalised base callsign. A sked entered for an active variant such as `9A0BB-23` therefore affects the common score of the chat entries belonging to `9A0BB`.

### How are multiple suffixes and chat categories handled?

Active callsigns such as `9A0BB-2`, `9A0BB-70`, `9A0BB-23` and `9A0BB-13` remain separate chat members. Messages can therefore still be addressed to the complete callsign in the correct chat category.

Worked, band, NOT-QRV and score information belongs to the common base callsign `9A0BB`. The score is calculated once and projected to all active variants. The user list may consequently contain several separate rows with the same score, while the priority list contains only one entry for the base station.

KST4Contest uses the most recently suitable active login in the last relevant chat category as the concrete message target. Selecting a priority candidate then resolves the complete callsign, including its suffix and chat category.

### Updating and displaying the score

New messages, AirScout data, skeds, Worked information and manual NOT-QRV or Sked-fail changes request a new calculation immediately. The score is also refreshed periodically because activity, AP and sked information changes with time even when no new event is received.

A delay of a few seconds between an event and the visible new order is therefore normal.

The user interface displays the score in three places:

- the numerically sortable **Score** column in the user list,
- the **Further Info** section for the selected station, and
- the compact list of the two highest-ranked candidates, with a separate window containing up to 15 candidates.

Operation: [Priority List in the User Interface](en-User-Interface#priority-list).

### What does the score not tell you?

The numerical value is neither a success probability nor a signal prediction. A score which is twice as high does not mean that the QSO is twice as likely.

Among other things, the calculation does not know:

- the actual antenna direction of the remote station,
- its current operating situation,
- local interference,
- short-term propagation changes,
- terrain obstruction outside the connected assessment functions, or
- whether a station which is active in the chat is currently sitting at the radio.

Known input data may also be outdated or ambiguous. A detected frequency, for example, proves only that the QRG recently appeared in connection with that station.

In practical terms, the score does not replace the operator’s decision. It prevents the information already available to KST4Contest from having to be reconstructed mentally for every candidate.

Related settings:

- [Enabled Bands](en-Configuration#enabled-bands)
- [Antenna Beamwidth](en-Configuration#antenna-beamwidth)
- [Default Maximum QRB](en-Configuration#default-maximum-qrb)
- [AirScout Settings](en-Configuration#airscout-settings)
- [Band Upgrade Hint and Priority Boost](en-Configuration#band-upgrade-hint-after-a-log-entry)

---

## AP and Sked Timeline

The timeline combines upcoming aircraft-scatter opportunities and stored skeds for the next 30 minutes. It therefore answers two different questions in the same place:

- When is an interesting AP opportunity expected?
- Which previously agreed sked is approaching independently of that opportunity?

Events further in the future appear on the right. As time passes, they move left towards the current time.

![AP candidates and skeds in the timeline](sked_timeline.png)

### AP candidates

AP candidates appear in the upper lanes. Up to four selected candidates can be displayed for each aircraft arrival minute. The selection takes the Priority Score and the reflection potential reported by AirScout into account.

The colour of an AP marker represents the reflection potential:

| Colour | Reflection potential |
|---|---:|
| Magenta | at least 95% |
| Red | at least 75% |
| Yellow | at least 50% |
| Blue | below 50% |

The colour is not a QSO probability. It represents the AirScout value for the calculated reflection geometry.

Clicking an AP candidate selects the corresponding active chat member, including its callsign suffix and chat category. A suitable message can then be prepared immediately.

### Skeds

Skeds appear as diamonds in the lower lane. Their labels use the complete KST callsign, for example `SKED: DN9APW-2`. This makes it clear which particular login was selected for the scheduled contact.

A sked tooltip shows at least:

- the complete KST callsign,
- the agreed band, and
- the QTF towards the remote station.

Where suitable AirScout data is available, the tooltip also includes current AP reachability and the next calculated AP opportunity.

### Antenna direction

When the QTF of an event is clearly outside the current antenna direction, its marker becomes more transparent. The label remains readable. A target close to the centre of the configured antenna beam is highlighted.

This visual effect changes neither the sked nor the Priority Score. It is simply a quick way of identifying candidates which fit the current antenna direction.

The timeline is a preview. AirScout data can change, and a stored sked guarantees neither a clear frequency nor an actual propagation path.



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
