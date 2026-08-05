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
| Score | Current priority score |
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

Sked reminders for both sked partners can also be enabled here.

---

## Priority List

Shows the top candidates calculated by the Score Service. Updates automatically in the background based on direction, distance and AP availability.

---

## Cluster & QSO of Others

Separate window (can be minimised). Shows the communication flow between other stations – interesting during quieter contest periods.

---

## Menu

### Window
- **Use Dark Mode** (from v1.26): Toggle dark colour scheme on/off.

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
