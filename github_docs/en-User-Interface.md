# User Interface

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-Benutzeroberflaeche)

## Connecting to the Chat

1. Select a **chat category** in the settings window (e.g. 144 MHz VHF, 432 MHz UHF, …).
2. Click the **Connect** button.
3. Wait for the connection to be established.

> Disconnecting and reconnecting is only possible via the settings window. It is therefore recommended to keep the settings window open.

---

## Main Window Overview

The main window consists of several areas:

### PM Window (top left)

Shows all received **private messages** as well as intercepted public messages containing your own callsign. New messages appear in **red** and fade every 30 seconds from yellow to white.

### User List (Chat Members)

The central table of all currently active chat users. Columns (depending on configuration):

| Column | Content |
|---|---|
| Callsign | Station callsign |
| Name | Name and additional information from the chat name field |
| QRA | Maidenhead locator |
| QRB | Distance in km |
| QTF | Direction in degrees |
| QRG | Most recent frequency detected in a chat message |
| Tropo | Result of the band-specific tropo or path assessment |
| Score | Current, numerically sortable priority score of the normalised base callsign |
| Act | Minutes since the most recent activity |
| AP | AirScout aircraft data, when enabled |
| worked | Per-band Worked, band-opportunity and grid-square status, plus `wkdany` |
| NOT QRV @ | Bands on which the station has manually been marked not QRV |
| Category | Chat category of this entry |

### Worked, band and grid-square status

The subcolumns under **worked** use compact codes because several enabled bands leave little room for full descriptions. `X` marks a callsign worked on that band. `a` and `B+` identify an offered band which has not yet been worked. An appended `o` means that the four-character grid square has already been worked on this band.

The **wkdany** subcolumn is band-independent: `x` means that the callsign has been worked, `o` means that the grid square has been worked on any band, and `xo` means both.

Each status cell has a tooltip containing the legend and the state derived for that station. For the complete calculation, including NOT-QRV precedence, see [Worked Callsigns, New Bands and New Grid Squares](en-Features#worked-callsigns-new-bands-and-new-grid-squares).

![Band-specific Worked status and worked grid squares](worked_band_status.png)

**Sorting**: Click column headers. QRB sorting is numerical (corrected in v1.22).

### Send Field

Text input for outgoing messages. After clicking a callsign in the user list, the send field automatically receives focus – start typing immediately without double-clicking (from v1.22).

### MYQRG Field

To the right of the send button. Shows the current own QRG, can also be entered manually.

### MYQTF Field *(for v1.3)*

Input field for the current antenna direction. Used for the planned `MYQTF` variable.

---

## Filters

The filter bar is located above the chat-member table and groups related controls:

- **Show only QTF** limits the list to a selected antenna direction.
- **Show only QRB [km] <=** sets a maximum distance.
- **Find** searches for a callsign.
- **wkd** hides callsigns which have already been worked on at least one band.
- The individual band buttons hide a station if it has already been worked on that band or has been marked NOT QRV there. Only bands enabled for the local station are shown.
- **Only new grids** shows only stations in four-character grid squares which have not been worked on any band.
- **Grid color** is not a filter. It marks the QRA cell of an already worked grid square without hiding stations.
- **New bands** shows stations with at least one detected, locally enabled and unworked band opportunity. NOT-QRV marks take precedence.
- **Reachability**, **Tropo >=0dB** and **AS next 5m** limit the list according to the selected path or AirScout criteria.

The filter bar has no fixed width. QTF, Worked and Reachability controls initially use the available space in their respective rows. When the horizontal divider is moved to the right and the chat-member area becomes narrower, controls wrap only when their actual required width no longer fits.

![Wrapped filter bar in a narrow chat-member view](filter_bar_wrapped.png)

In plain terms: the filters determine the table contents, but no longer enforce the minimum width of the entire right-hand side. The bar remains compact in the normal layout and uses additional height only when the view becomes genuinely narrow. Moving the divider back to the left immediately returns the controls to the available rows.

---

## Station Info Panel (Further Info)

The lower-right panel combines the messages associated with the selected station. This includes public messages, private messages to the local station and, where visible in the chat, private messages addressed to other stations.

The selected filter controls which of these messages are displayed. Under **Settings → GUI**, the default filter can be set to:

- all messages,
- private messages to the local station,
- private messages to other stations, or
- public messages.

This setting changes the Further Info display only. Messages are neither discarded nor removed from the other message tables, and the filter can be changed at any time for the currently selected station.

The lower part of the panel contains per-band **Not QRV** marks for the selected station. Individual controls are shown for the bands enabled in the local station settings. **tag not qrv all** sets or removes the mark for every supported band, including bands which are not currently visible.

The change immediately affects the **NOT QRV @** column, band opportunities and the corresponding filters. It is stored in the internal database and restored after a restart.

![Per-band NOT-QRV marks in the Further Info panel](not_qrv_controls.png)

The current **Priority score** of the selected station is displayed in the same section.

**Sked fail** marks an unsuccessful attempt and strongly reduces the score of the normalised base callsign. **Reset fail** removes the mark. It applies to all active suffix and category variants of the station and remains active for the current program session.

The controls underneath are used to create a sked:

| Control | Meaning |
|---|---|
| **Sked in** | Time remaining until the sked |
| **Band** | Agreed band selected from the locally enabled bands |
| **Mode** | `SSB` or `CW` for a possible Win-Test handover |
| **Create sked** | Create the internal sked |
| **Remind-PM in** | Enable automatic reminder PMs |
| **2+1**, **5+2+1**, **10+5+2+1** | Times at which reminder PMs are sent before the sked |

![Sked controls in the Further Info section](sked_controls.png)

The proposed band is derived from recent QRG and name information for the station. It can be changed explicitly before creating the sked. The mode selection only affects the Win-Test handover; the internal sked and reminder PMs work independently.

**Create sked** always creates the appointment inside KST4Contest first. If the Win-Test network listener is active, KST4Contest then attempts an additional handover to Win-Test. If no QRG matching the selected band can be found or Win-Test cannot be reached, the internal sked, its priority contribution and any scheduled reminders remain intact.

The complete derivation and limitations are described under [Skeds and Sked Reminders](en-Features#skeds-and-sked-reminders).

---

## Priority List

The compact priority bar is located on the right-hand side between the user list and the Further Info section. It displays the two currently highest-ranked candidates directly in the main window:

```text
Priority:  1 CALLSIGN SCORE  2 CALLSIGN SCORE  more
```

Clicking either candidate selects the corresponding active chat member. The complete callsign, including its suffix and chat category, is used.

The **more** button opens a separate window containing up to 15 candidates. The list is sorted by descending score. Double-clicking an entry selects the candidate and closes the window.

![Priority Score, compact candidate list and Further Info controls](priority_score_overview.png)

Stations with a score of `0` are not included in the priority list. They remain visible in the user list so that the reason for their exclusion can be examined and, for example, an incorrect NOT-QRV mark can be changed.

The score is calculated for the normalised base callsign. Several active variants such as `9A0BB-2` and `9A0BB-70` may therefore display the same value in the user list. They nevertheless remain separate message targets.

New messages, AirScout data, skeds and status changes request a new calculation. A periodic refresh also runs in the background. A briefly outdated order is therefore not an error.

Calculation and limitations: [Priority Score and Priority List](en-Features#priority-score-and-priority-list-from-v140).

---

## Station Map

The station map is opened or closed through:

**Windows → Show / hide station map**

The window uses the chat members currently visible in the filtered user list. Changing the QRB, QTF, Worked, band or Reachability filters can therefore also change the stations shown on the map.

A station can additionally be opened directly from the **Further Info** panel using **Show on map**. This selects the station on the map and requests the associated path analysis.

Stations with the same normalised base callsign and position are combined into one marker. At lower zoom levels, nearby markers may additionally be displayed as clusters. These are display groups only; the individual chat logins remain separate message targets inside KST4Contest.

Clicking a station marker:

1. selects the corresponding chat member,
2. scrolls the main user list to that entry,
3. updates the **Further Info** panel, and
4. prepares the complete visible callsign as the message target.

The map details for the selected station include its locator, QRB, QTF, detected bands and available band opportunities. **Trigger cluster spot** sends a spot through the built-in local DX Cluster server so that connected logging software can receive the selected station and QRG.

The path-analysis section shows the terrain profile and the calculated route between both stations. Depending on the available data, it includes:

- the analysis frequency,
- line-of-sight and horizon information,
- Fresnel-zone clearance,
- detected obstructions,
- an estimated link budget,
- received power and SSB margin, and
- a short assessment of the path.

Moving the mouse over the terrain profile highlights the corresponding geographical position on the map.

The analysis can be hidden using **Hide path analysis** when more space is required for the map. The compact state displays **Path analysis is hidden.** together with the **Show path analysis** button.

![Station map with hidden path analysis](station_map_compact.png)

The selected station and map contents remain available while the analysis panel is hidden. The setting is stored and restored at the next start.

Calculation method and limitations: [Station Map and Path Analysis](en-Features#station-map-and-path-analysis-from-v141).

---

## Global Message Tabs and Monitor Window

Three global message tabs are located below the main user list. Unlike the **Further Info** panel, their contents do not depend on the station currently selected.

| Tab | Displayed messages |
|---|---|
| **Public messages** | All public chat messages, including CQ calls and beacons |
| **DXCluster messages** | DX cluster messages received from the ON4KST server |
| **QSO of the other** | Directed messages between chat logins other than the local station |

The **Public messages** tab is selected by default.

![Global message tabs below the main user list](global_message_tabs.png)

The **DXCluster messages** table contains the time, reporting and reported stations, locators, QRG, message text and global Worked state where these values are available in the received message.

The **QSO of the other** table contains:

- the complete sender and receiver callsigns,
- the latest QRG currently known for each station,
- the global Worked state of each station,
- the message text, and
- the chat category.

The displayed QRG is not necessarily the frequency on which the stations intend to make a contact. It is the latest QRG currently associated with the respective chat member. The Worked state is global and not specific to the displayed QRG or band.

A directed chat message in this table does not prove that a radio QSO has taken place. The table also contains sked requests, frequency exchanges and other directed messages between third-party chat logins.

### Separate monitor window

The DX cluster and QSO-of-the-other tables can also be displayed together in a separate window.

![Separate monitor window for DX cluster traffic and directed messages between other stations](cluster_qso_monitor.png)

The separate window and the tabs use the same underlying messages. Hiding the window does not stop message processing or remove messages from the tabs.

Use **Windows → Hide cluster / stranger QSOs** to hide the window and **Windows → Show cluster / stranger QSOs** to restore it.

If a message is too long for its table cell, moving the mouse over the cell displays the complete text in a tooltip. Links beginning with `http://`, `https://` or `www.` can be opened in the system browser.

---

## Menu

### Windows

- **Hide cluster / stranger QSOs** hides the separate monitor window for DX cluster messages and directed messages between other stations.
- **Show cluster / stranger QSOs** restores the monitor window.
- **hide options** hides the settings window.
- **show options** restores the settings window.
- **Use dark mode design** activates the dark colour scheme.
- **Use default mode design** restores the default colour scheme.
- **Show / hide station map** opens or closes the separate station-map and path-analysis window.

---

## Window Sizes and Dividers

From **v1.21**, clicking **"Save Settings"** also saves window sizes and divider positions of all panels in the configuration file, which are restored on the next start.

If you encounter display problems: delete the configuration file → KST4Contest creates new default values.

---

## Operating Tips

- **Keep the settings window open**: Quick access to enable/disable the beacon.
- **Right-click in the user list**: Opens the snippet menu and other context actions.
- **Mark a station NOT QRV**: Select the station and use the per-band controls in the **Further Info** panel.
- **Enter from anywhere**: When text is in the send field, Enter sends directly – even if the focus is elsewhere.
- **Stop the beacon**: Switch off the beacon while scanning frequencies to avoid flooding the chat with messages.
