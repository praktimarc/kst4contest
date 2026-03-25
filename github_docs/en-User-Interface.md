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
| Call | Station's callsign |
| Name | Name from the chat name field |
| Loc | Maidenhead locator |
| QRB | Distance in km |
| QTF | Direction in degrees |
| QRG | Automatically detected frequency |
| AP | AirScout aircraft data (when active) |
| Band colours | Worked / NOT-QRV status per band |

**Sorting**: Click column headers. QRB sorting is numerical (corrected in v1.22).

### Send Field

Text input for outgoing messages. After clicking a callsign in the user list, the send field automatically receives focus – start typing immediately without double-clicking (from v1.22).

### MYQRG Field

To the right of the send button. Shows the current own QRG, can also be entered manually.

### MYQTF Field *(for v1.3)*

Input field for the current antenna direction. Used for the planned `MYQTF` variable.

---

## Filters

The filter bar (from v1.21 as a flowpane for small screens):

- **Show only QTF**: Activate direction filter (N/NE/E/… buttons or degree input)
- **Show only QRB [km] <=**: Activate distance filter (toggle button)
- **Hide Worked [Band]**: Hide worked stations per band (one toggle per band)
- **Hide NOT-QRV [Band]**: Hide NOT-QRV-tagged stations per band

---

## Station Info Panel (Further Info)

Bottom right: Shows all messages of a selected station (CQ messages and PMs in one panel). A message filter can be pre-configured via the default filter in the Preferences.

**Sked reminders** can also be activated here.

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
- **Right-click in the user list**: Opens the snippet menu and further actions (QRZ.com profile, set NOT-QRV tags).
- **Enter from anywhere**: When text is in the send field, Enter sends directly – even if the focus is elsewhere.
- **Stop the beacon**: Switch off the beacon while scanning frequencies to avoid flooding the chat with messages.
