# Configuration

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-Konfiguration)

After the first start, the **settings window** opens – this is the central starting point for all configuration. It is recommended to keep the settings window open during operation (e.g. to quickly toggle the beacon on and off).

> **Important**: Always click **"Save Settings"** after any change! Settings are stored in `~/.praktikst/preferences.xml`. From v1.21 onwards, window sizes and divider positions are also saved when you click Save.

---

## Station Settings

### Callsign and Locator

Enter your callsign and Maidenhead locator (6 characters, e.g. `JN49IJ`). These values are used for distance and direction calculations.

### Active Bands

Use the **"my station uses band"** checkboxes to select which bands you are active on. Only selected bands will show buttons and table rows in the user interface. A restart is required after changing these settings.

### Antenna Beamwidth

Enter a realistic value for your antenna's beamwidth (in degrees). This value is used for the [Sked Direction Highlighting](Features#sked-direction-highlighting). A test value of 50° has proven useful; DM5M uses Quads with 69°.

> **Do not** enter fantasy values – the direction calculations will become meaningless.

### Default Maximum QRB

Maximum distance (in km) for which direction warnings should be triggered. A realistic value for DM5M is 900 km. Stations beyond this distance are ignored for highlighting purposes.

---

## Log Sync Settings

Two methods are available for automatically marking worked stations. Details: [Log Synchronisation](en-Log-Sync).

### Universal File Based Callsign Interpreter (Simplelogfile)

Interprets any log file using regex to find callsign patterns. No band information available. Suitable as a fallback or for unsupported logging programs.

### Network Listener for Logger's QSO UDP Broadcast

**Recommended method.** KST4Contest listens for UDP packets sent by the logging software when saving a QSO. Stations are marked including band information. UDP port: default **12060**.

---

## TRX Sync Settings

Receives the current transceiver frequency from the logging software via UDP. Makes the `MYQRG` variable available automatically. Useful for:

- Quickly inserting your own QRG into chat messages.
- Automatic CQ beacon with current frequency.

> **Note for multi-setup**: When running two logging programs on two computers but only one KST4Contest instance, only one logging program should send frequency packets. KST4Contest cannot distinguish between sources.

---

## AirScout Settings

Configuration of the interface to AirScout for aircraft scatter detection. Details: [AirScout Integration](en-AirScout-Integration).

---

## Notification Settings

Three notification types are available:

1. **Simple sounds**: TADA sound for incoming messages, tick for sked direction detection, etc.
2. **CW announcement**: The callsign of a station sending a private message is output as a CW signal.
3. **Phonetic announcement**: The callsign is spoken phonetically.

---

## Shortcut Settings

Configure quick-access buttons that appear directly in the main window. Clicking a button inserts the configured text into the send field. All [variables](Macros-and-Variables#variables) can be used.

---

## Snippet Settings

Text snippets are accessible via:

- **Right-click** on a callsign in the user list
- **Right-click** in the CQ message table
- **Right-click** in the PM message table
- **Keyboard shortcuts**: `Ctrl+1` to `Ctrl+0` for the first 10 snippets

If a callsign is selected in the user list, the snippet is addressed as a direct message:
`/CQ CALLSIGN <snippet text>`

---

## Beacon Settings

Configure an automatic interval message in the public chat channel. Recommended: use the `MYQRG` variable in the text so the current frequency is always up to date. Interval and text are freely configurable.

> **Tip**: Enable the beacon while calling CQ and quickly disable it in the settings window when not calling.

---

## Messagehandling Settings (from v1.25)

New settings section with the following options:

- **Auto-reply to all incoming messages**: Configurable automatic reply to private messages.
- **Auto-reply with CQ QRG**: When someone asks for your frequency, KST4Contest automatically replies with the `MYQRG` variable content.
- **Default filter for the userinfo window**: Pre-configured message filter for the station info panel *(for Gianluca :-) )*.

---

## Worked Station Database Settings

Reset the internal worked database before each contest! Contains:

- Worked status for all stations (per band)
- NOT-QRV tags (since v1.2)

Use the **"Reinitialize"** button below the table. A planned feature is an automatic expiry time for the worked status.

---

## Dark Mode (from v1.26)

Toggle via the menu: **Window → Use Dark Mode**. Colours can be individually customised via CSS.

---

## Saving Settings

Click **"Save Settings"** after **every** change! Without saving, all changes are lost on next start.

- Storage location: `~/.praktikst/preferences.xml`
- From v1.21: Window sizes and divider positions are also saved.
- If you encounter problems: delete the configuration file → KST4Contest creates a new one with default values.
