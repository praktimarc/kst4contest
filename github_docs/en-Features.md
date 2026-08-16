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

> Configuration: [Configuration – Antenna Beamwidth](en-Configuration#antenna-beamwidth)

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

- [Active Bands](en-Configuration#active-bands)
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

KST4Contest can send recurring CQ messages to the public chat. The beacon is intended for longer periods of calling CQ on a fixed frequency: it publishes the local QRG regularly without requiring the operator to enter the same text again.

Both chat categories use one shared interval, but each category has its own enable setting and message template. The second beacon is only sent while the second chat is enabled and connected.

Global variables such as `MYQRG`, `SECONDQRG`, `MYLOCATOR` and `MYQTF` are resolved immediately before every transmission. A QRG updated by the logging software can therefore appear in the next beacon.

Before transmission, KST4Contest validates the completely resolved message. Empty messages, line breaks, the protocol separator `|` and messages exceeding 120 characters are not sent.

Disable the beacon while searching the band or changing QRG frequently. An automatically published frequency is useful only while somebody is actually listening and calling there.

Configuration, timer behaviour and available variables: [Configuration – Beacon Settings](en-Configuration#beacon-settings).

---


## Simplelogfile

File-based log evaluation using regex. Details: [Log Synchronisation](en-Log-Sync#method-1-universal-file-based-callsign-interpreter-simplelogfile).
---

## Global Message Views

Most message tables in KST4Contest are deliberately tied either to the local station or to the station currently selected in the user list. Some message streams must, however, remain visible independently of that selection.

KST4Contest therefore provides three global message tabs below the main user list:

| Tab | Content |
|---|---|
| **Public messages** | Public chat messages, including CQ calls and beacon messages |
| **DXCluster messages** | DX cluster messages delivered by the ON4KST server |
| **QSO of the other** | Directed chat messages between chat logins other than the local station |

**Public messages** is selected by default. Changing the selected station does not affect any of these three views.

![Global message tabs below the main user list](global_message_tabs.png)

### DXCluster messages

The DX cluster table shows cluster messages received through the ON4KST connection. Depending on the information contained in the source message, the table displays:

- the time,
- the reporting station and its locator,
- the reported station and its locator,
- the QRG,
- the message text, and
- the global Worked state of the reported station.

An empty locator or another empty field does not necessarily indicate a processing error. The corresponding information may simply be absent from the source message.

This view must not be confused with the [built-in DX Cluster server](en-DX-Cluster-Server). The built-in server sends derived direction spots to connected logging software. The **DXCluster messages** tab displays cluster traffic received from ON4KST.

### QSO of the other

The **QSO of the other** table displays directed chat messages for which neither the sender nor the receiver is the local station. Messages addressed to `ALL` are not included.

The table contains the following columns:

| Column | Meaning |
|---|---|
| **Time** | Time of the chat message |
| **Call TX** | Complete callsign of the sender |
| **Last QRG TX** | Most recently detected QRG assigned to the sender |
| **wkd TX?** | Global Worked state of the sender |
| **Call RX** | Complete callsign of the receiver |
| **Last QRG RX** | Most recently detected QRG assigned to the receiver |
| **wkd RX?** | Global Worked state of the receiver |
| **Message** | Message text |
| **Category** | Chat category in which the message was received |

The QRG columns are not a historical record of the frequency used for the displayed message. They show the latest QRG currently known for the respective chat member. The value may originate from another message and may change when a newer QRG is detected.

The two Worked columns show the global callsign state. They do not indicate whether the station has already been worked on the QRG or band shown next to it.

The expression “QSO of the other” is used as a compact user-interface label. A directed chat message does not prove that an actual radio QSO has taken place. It may equally be a sked request, a frequency exchange or another private message between two chat logins.

### Separate monitor window

The DX cluster and QSO-of-the-other tables are additionally available in a separate monitor window. It places the DX cluster table above the directed messages between other stations.

![Separate monitor window for DX cluster traffic and directed messages between other stations](cluster_qso_monitor.png)

The tabs and the monitor window use the same underlying message stores. Opening the separate window does not create another connection, receive the messages a second time or maintain an independent history.

The window can be hidden or restored through:

**Windows → Hide cluster / stranger QSOs**

or:

**Windows → Show cluster / stranger QSOs**

The additional window is useful when these message streams should remain visible on a second monitor or while another part of the main window is being used. During periods with heavy chat traffic, the global tabs are usually more compact.

When a table cell cannot display its complete message, moving the mouse over the cell shows the full text in a tooltip. Web links beginning with `http://`, `https://` or `www.` can be opened in the system browser.

---

## Station Map and Path Analysis (from v1.41)

The station map shows the geographical relationship between the local station and the chat members which are currently relevant in the main window. It is not a second, independent user list: filters applied to the chat-member table also determine which stations are passed to the map.

![Station map with path analysis](station_map_path_analysis.png)

### Stations and markers

A station can be displayed only if a usable six-character Maidenhead locator is available. Chat entries without a sufficiently precise locator remain in the user list but cannot be positioned reliably on the map.

Active chat variants belonging to the same normalised base callsign are combined into one map marker. This avoids several markers being placed at exactly the same position when, for example, a station is logged in with separate suffixes for different bands. The marker information includes the currently derived bands and, where applicable, open `B+` opportunities.

Marker colours provide a compact status indication:

| Colour | Meaning |
|---|---|
| Blue | Normal station marker |
| Yellow | The callsign has already been worked on at least one band |
| Green | The station is inside the current antenna sector and is relevant as a directional candidate |
| Orange | Currently selected station |

The selected state has the highest display priority, followed by the directional warning and Worked state. A selected station therefore remains orange even if it also meets one of the other conditions.

At lower zoom levels, nearby markers are combined into screen-based clusters. This is a display function and does not merge the underlying chat members. Selected stations and important directional candidates remain individually visible where possible.

Clicking a station marker selects the corresponding active chat member in the main window. KST4Contest scrolls to the entry in the user list, updates the **Further Info** panel and prepares the complete visible callsign as the message target. The chat suffix and category therefore remain relevant even though several variants may share one map marker.

### Antenna sector, connection line and locator grid

The map displays the local station together with the currently configured antenna direction, beamwidth and maximum QRB. These values form the visible antenna sector.

Selecting a remote station adds a connection line between both locations. The Maidenhead overlay provides a geographical reference without requiring the operator to translate every locator mentally.

The map does not know the actual radiation pattern, side lobes or elevation angle of the antenna. The displayed sector is therefore a geometrical representation of the configured horizontal beamwidth, not a complete antenna model.

### Terrain profile

For the selected path, KST4Contest requests terrain elevations from the Open-Meteo elevation service. The active provider uses Copernicus GLO-90 data and requests no more than 100 evenly distributed elevation coordinates for one path.

The terrain resolution and the sampling distance are not the same thing. On a long path, the distance between two requested points can be considerably larger than the nominal resolution of the elevation model. Small terrain features may therefore remain undetected.

The profile combines:

- terrain elevation,
- the geometrical line between both antennas,
- Earth-curvature correction using an effective Earth-radius factor of `k = 4/3`,
- the radio and terrain horizons,
- the first Fresnel zone,
- minimum Fresnel clearance,
- detected Fresnel-zone intrusion, and
- a rough knife-edge diffraction estimate for relevant obstructions.

The configured **Own antenna height AGL** is added to the terrain elevation at the local station. For the remote station, KST4Contest currently assumes an antenna height of 10 metres above the local terrain.

Moving the mouse over the path profile marks the corresponding position on the map. This makes it easier to identify which hill or terrain section causes a reported obstruction.

### Frequency selection

Fresnel clearance and link-budget results depend on frequency. KST4Contest therefore attempts to derive a usable analysis frequency from recent QRG or band information associated with the selected station.

The value displayed as **Frequency** in the analysis panel is the frequency actually used for the calculation. Check it before interpreting the result. A frequency which merely belongs to a possible band is still only an approximation if the station is expected to operate elsewhere.

This matters particularly on the microwave bands. The Fresnel zone becomes smaller as frequency increases, while free-space path loss and feeder loss increase. A calculation performed for the wrong band may therefore look plausible while describing a different radio path.

### Link budget and propagation assessment

The link-budget estimate uses:

- the configured local and remote transmit powers,
- the configured antenna gains,
- estimated feeder losses,
- free-space path loss, and
- a rough additional loss derived from the terrain obstruction.

Antenna gains must be entered in dBi. Values specified in dBd must first be converted.

The calculation produces an estimated received power and a bidirectional SSB margin. The result is also made available to the Reachability calculation used by the **Tropo** column and the corresponding filter in the main window.

The map and the table use the same `ReachabilityService` and calculation cache. A result calculated for the map can therefore also become available to the user list without repeating the complete request.

KST4Contest deliberately does not request an online terrain profile for every visible chat member whenever the list changes. That would create unnecessary API traffic and make normal chat processing dependent on a large number of external requests. Select the required station on the map or use **Calc selected** when a current calculation is needed.

### Compact view

The lower analysis panel can be hidden with **Hide path analysis** and restored with **Show path analysis**. Its visibility is stored in the preferences and restored at the next start.

The divider between the map and the analysis panel can be moved to allocate more space to either section. Hiding the analysis panel does not discard the selected station or close the map.

Operation of the map window is described under [Station Map](en-User-Interface#station-map).

Configuration of antenna height, power and gain is described under [Path Analysis and Link Budget](en-Configuration#path-analysis-and-link-budget).

### Limits of the result

The path analysis is an engineering estimate. Among other things, it does not know:

- the actual antenna height and station setup of the remote operator,
- vegetation, buildings and other clutter which is not represented in the elevation data,
- the current refractivity profile of the atmosphere,
- ducting, scattering or reflection conditions,
- local interference or receiver performance, or
- whether a detected QRG is still in use.

The **Mechanisms** indication lists propagation mechanisms which may be consistent with the calculated geometry. It does not prove that one of them is currently available.

Aircraft Scatter information is not currently coupled to the terrain-profile calculation. AirScout data and the path analysis may both describe the same remote station, but they remain separate assessments.

OpenStreetMap tiles and the active elevation provider require an Internet connection. Leaflet and the map application itself are bundled locally, and tile requests pass through a local proxy, but this proxy is not a permanent offline map store.

In plain terms: the analysis helps to identify plausible paths, obvious obstructions and incorrect assumptions. It does not replace propagation experience or a real signal.

--- 

## Bounded Message Stores (from v1.41)

During a long contest, KST4Contest may receive tens of thousands of chat and DX cluster messages. If these lists were allowed to grow without limit for the complete runtime, memory consumption would not be the only problem. Filtering, sorting and updating the tables built on top of them would also become increasingly expensive.

KST4Contest therefore uses two separate bounded message stores:

| Message store | Clean-up starts above | Size after clean-up |
|---|---:|---:|
| Chat messages | 30,000 entries | 25,000 entries |
| DX cluster messages | 10,000 entries | 8,000 entries |

New messages are inserted at the beginning of the respective list. When the upper limit is exceeded, KST4Contest removes the oldest entries from the end until the specified target size is reached.

### Why are there two thresholds?

The store is not reduced to its maximum size again after every single incoming message. After a clean-up, the chat-message store has room for another 5,000 entries and the DX cluster store for another 2,000.

KST4Contest therefore removes old entries in batches instead of modifying the end of the list again for every subsequent message. The clean-up runs much less frequently as a result.

### Which tables share a store?

The following views are filtered representations of the same global chat-message list:

- **Public messages**,
- the private-message table,
- the messages in the **Further Info** panel, and
- **QSO of the other**.

These tables do not each retain another 30,000 messages. When an old chat message is removed from the shared store, it disappears from all views based on that store at the same time.

The **DXCluster messages** tab and the DX cluster table in the separate monitor window likewise use the same cluster-message store. Opening the additional window neither creates a second message connection nor duplicates the received messages.

The chat and DX cluster stores are independent of each other. Heavy public-chat traffic therefore does not reduce the capacity available for DX cluster messages, and vice versa.

### No permanent history

Both message stores exist in memory only. They are written neither to the internal Worked database nor to another local message file.

After restarting KST4Contest, the tables begin with empty lists and are rebuilt exclusively from newly received messages. These views are working tools for the current session, not a permanent chat archive.

---

## Screen-Aware Main Window Sizing (from v1.41)

KST4Contest stores the most recently used size of the main window. This is useful as long as the application is started on a comparable display the next time. If it was previously used on a larger monitor, however, the stored size may extend beyond the visible area of a smaller screen.

KST4Contest therefore checks the stored size against the usable area of the primary screen during startup.

### How is the startup size determined?

If the stored values are valid, KST4Contest initially uses the last saved height and width. If no usable values are available, the following default size is used:

- 1,234 pixels wide and
- 768 pixels high.

KST4Contest does not use the complete screen resolution as the available area. It uses the visual bounds reported by JavaFX for the primary screen. Taskbars, docks and similar operating-system areas are already excluded from these bounds.

An additional safety margin of 40 pixels is subtracted. If the stored width or height exceeds the remaining space, only the affected value is reduced.

After the user interface has been built with this content size, KST4Contest checks the complete native operating-system window, including its title bar and borders. The window is reduced or moved into the visible area again if necessary.

This catches two different cases:

1. The stored content area is larger than the current screen.
2. The content area fits, but the complete native window still extends beyond the visible area because of its borders or position.

### What happens to the layout?

The complete interface is not scaled proportionally. Instead, the main window receives less space and the UI areas designed for this situation react to the available width.

The filter bar remains compact at normal window sizes. Its controls wrap into additional rows only when their actual required width no longer fits. The dividers can still be used to distribute the available space between the message and station areas.

### Limits of the automatic correction

The check always uses the **primary screen**. It does not restore the previous position on a particular secondary monitor.

The automatic size restriction currently applies to the main window only. The settings window, the separate cluster and QSO monitor window and other auxiliary windows continue to use their stored sizes without the same additional check against the primary screen.

In plain terms: the protection mainly prevents the central main window from becoming unusable after moving to a smaller display. It is not a complete window-position manager for a changing multi-monitor setup.