# Configuration

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-Konfiguration)

After the first start, the **settings window** opens – this is the central starting point for all configuration. It is recommended to keep the settings window open during operation (e.g. to quickly toggle the beacon on and off).

> **Important**: Always click **"Save Settings"** after any change! Settings are stored in `~/.praktikst/preferences.xml` on Linux and in `%USERPROFILE%\.praktikst\preferences.xml` (or `C:\Users\<Username>\.praktikst\preferences.xml`) on Windows. From v1.21 onwards, window sizes and divider positions are also saved when you click Save.

---

## Station Settings

![Station Settings](client_settings_window_station.png)

### Login and Chat Categories

Enter your ON4KST chat credentials here (callsign and password).
Also, select the **primary chat category** (e.g., IARU Region 1 VHF/Microwave).

With the option for a **second chat** (Multi-Channel Login), you can log in to another category simultaneously (e.g., UHF/SHF). Both chats will then be monitored in parallel. You can optionally specify a different login name for the second chat (useful for Opposite Station Multi-Callsign Logging).

### Callsign and Locator

Enter your own callsign and Maidenhead locator (6 characters, e.g., `JN49IJ`). These values are needed for distance and direction calculations.

### Active Bands

The **My station uses …** checkboxes define the bands available in the current local station setup. Supported choices are 50 MHz, 70 MHz, 144 MHz, 432 MHz, 1296 MHz, 2320 MHz, 3400 MHz, 5760 MHz and 10 GHz.

This selection controls more than the visible band columns. It is also used for:

- the per-band Worked and NOT-QRV filters,
- the NOT-QRV controls visible in the **Further Info** panel,
- the `a` and `B+` band-opportunity calculation,
- the **New bands** filter,
- the band-upgrade hint after a log entry, and
- band-specific priority and Reachability functions.

After a change, click **Save Settings** and restart KST4Contest. Band columns and several related controls are created while the user interface is being built and are therefore not added or removed completely during the current session.

### Antenna Beamwidth

Enter a realistic value for your antenna's beamwidth (in degrees). This value is used for the [Sked Direction Highlighting](Features#sked-direction-highlighting). A test value of 50° has proven effective; DM5M uses quads with 69°.

> **Do not** enter fantasy values – the direction calculations will become useless.

### Default Maximum QRB

Maximum distance (in km) for which direction warnings should be triggered. A realistic value for DM5M is 900 km. Stations farther away are ignored for highlighting purposes.

---

## Server Settings (from v1.31)

The chat server DNS and port are configurable in the Preferences:

- **Server DNS**: Default `www.on4kst.org` (changed from `www.on4kst.info` in v1.31 hotfix).
- **Port**: Default port of the ON4KST server.

A change is only needed if the server moves or an alternative endpoint is used.

---

## Log Sync Settings

Three methods are available for automatically marking worked stations. Details: [Log Synchronisation](en-Log-Sync).

### Universal File Based Callsign Interpreter (Simplelogfile)

Interprets any log file using regex for callsign patterns. No band information is available. Suitable as a fallback or for log programs that are not directly supported.

### Network Listener for Logger's QSO UDP Broadcast

**Recommended method.** KST4Contest listens for UDP packets sent by the logging software to the broadcast address when a QSO is saved. Stations are marked with band information. UDP port: default **12060**. (Used by UCXLog, N1MM+, QARTest, DXLog.net, etc.).

### Win-Test Network Listener (Additional UDP Listener)

A dedicated network listener for Win-Test. KST4Contest receives and processes Win-Test-specific UDP packets (including sked handovers) on the configured port.

---

## TRX Sync Settings

Receives the current transceiver frequency from the logging software via UDP. This enables the automatic population of the `MYQRG` variable. Useful for:

- Quickly inserting your own QRG into chat messages.
- Automatic CQ beacon with current frequency.

> **Note for multi-setup**: When running two logging programs on two computers but only one KST4Contest instance, only one logging program should send frequency packets. KST4Contest cannot distinguish between sources.

---

## AirScout Settings

Configuration of the interface to AirScout for aircraft scatter detection. Details: [AirScout Integration](en-AirScout-Integration).

---

## Notification Settings

![Notifications, DX cluster output and QSO monitoring](client_settings_window_notification.png)

Three notification types are available:

1. **Simple sounds**: TADA sound for incoming messages, tick for sked direction detection, etc.
2. **CW announcement**: The callsign of a station sending a private message is output as a CW signal.
3. **Phonetic announcement**: The callsign is pronounced phonetically.

### Fallback Band for Relative QRG Detection

The **Fallback band for relative QRG detection** dropdown selects the band used when a relative QRG cannot be assigned to a recent station-specific band context.

Only band prefixes supported by the frequency parser are available:

```text
50 MHz
70 MHz
144 MHz
432 MHz
1296 MHz
2320 MHz
3400 MHz
5760 MHz
10368 MHz (10G)
24048 MHz (24G)
```

The dropdown is neither a filter nor an override for complete frequencies. `432.088` is recognised as a frequency in the 432 MHz band regardless of the selection. The fallback is needed for relative values such as `.205`, `,205` or `qrg 205`.

Before using the fallback, KST4Contest checks the sender's recent band context. If a complete frequency has been detected for the same station during the previous 30 minutes, that band takes precedence. A fallback setting of `144 MHz` therefore still turns `.100` into `432.100 MHz` if the station mentioned `432.088` shortly before.

Although the setting is located in the Notification tab, it affects the general QRG parser. It therefore influences the QRG column, detected active bands, priority calculations, band-upgrade hints and other functions which use a known station frequency – not only DX cluster spots.

### Band Upgrade Hint after a Log Entry

After receiving a log entry from UCXLog or Win-Test, KST4Contest can check whether the station which has just been worked still offers another common and unworked band.

The check uses the same derivation as the `a` and `B+` display:

1. the bands enabled in the local station settings,
2. QRGs detected for the remote station during the previous 30 minutes,
3. explicit band designators in the name fields of its active chat entries,
4. stored per-band Worked marks, and
5. manually assigned NOT-QRV marks.

Active chat variants of the same normalised callsign are evaluated together. NOT-QRV takes precedence over an automatically detected QRG or band designator.

If at least one common and unworked band remains, the main window displays a blinking **BAND+** hint for approximately twelve seconds. The callsign and remaining bands are included in the button text; the tooltip contains the complete derivation. If general notification sounds are enabled, KST4Contest also plays a short sound.

The two options serve different purposes:

- **Blink + sound …** enables the hint after a matching log entry.
- **Priority boost …** additionally raises the score of stations which have already been worked on at least one band but still offer another common and unworked band.

The Priority Boost is only one factor in the complete calculation. Distance, antenna direction, recent activity, AirScout data, skeds and negative hints may still change the final order. Enabling the option therefore guarantees neither a particular score nor a particular position in the priority list.

The other score weights currently have no separate user-interface controls. Several existing settings nevertheless provide input data for the calculation, particularly the [enabled bands](#enabled-bands), [antenna beamwidth](#antenna-beamwidth), [default maximum QRB](#default-maximum-qrb) and [AirScout settings](#airscout-settings).

The complete calculation is described under [Priority Score and Priority List](en-Features#priority-score-and-priority-list-from-v140).


The hint requires a log-synchronisation source which provides band information. The file-based callsign interpreter sees callsigns only and cannot reliably identify the band of the QSO which has just been logged.

Further explanation: [Band Upgrade Hint after a Log Entry](en-Features#band-upgrade-hint-after-a-log-entry).

---

## Shortcut Settings

Configuration of quick-access buttons that appear directly in the main window. Clicking a button inserts the configured text into the send field. All [variables](Macros-and-Variables#variables) can be used.

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

Configuration of an automatic interval beacon in the public chat channel. Recommended: use the `MYQRG` variable in the text so the current frequency is always up to date. Interval and text are freely configurable.

> **Tip**: Enable the beacon when calling CQ and quickly disable it in the settings window when not calling.

---

## Messagehandling Settings (from v1.25)

New settings section with the following options:

- **Auto-reply to all incoming messages**: Configurable automatic reply to private messages.
- **Auto-reply with own CQ QRG**: When someone asks for your QRG, KST4Contest automatically replies with the content of the `MYQRG` variable.
- **Default filter for the userinfo window**: Pre-configured message filter for the station info window *(for Gianluca :-) )*.

---

## Win-Test Network Listener (from v1.31)

A dedicated listener for Win-Test-specific UDP packets. Enables:

- **Log synchronisation**: Worked stations are retrieved from Win-Test and marked in the user list.
- **Frequency parsing**: The current TRX frequency from Win-Test populates the `MYQRG` variable.
- **Sked handover (SKED push)**: Skeds from KST4Contest are passed directly to Win-Test via UDP. Win-Test's default UDP broadcast port (9871) is used.

Settings:
- **Enable/Disable**: Checkbox in Preferences (from v1.40).
- **Port**: Configurable UDP port for the Win-Test listener.
- **Sked UDP address and port**: Target address and port for SKED handover to Win-Test.

> **Note**: The Win-Test listener is an **additional** listener – the standard QSO UDP broadcast listener on port 12060 remains independent.

---

## PSTRotator Settings (from v1.31)

KST4Contest can control antenna direction via PSTRotator.

Settings:
- **Enable/Disable**: Checkbox in Preferences (from v1.40).
- **IP address**: IP address of the PSTRotator computer (default: `127.0.0.1` when running on the same PC).
- **Port**: Communication port of PSTRotator.

> **Note**: After clicking a direction button, KST4Contest waits briefly for the rotator response. With slow rotors (e.g. SPID) there may be a small delay.

---

## Sniffer Settings (from v1.31)

The QSO sniffer filters chat messages from configurable callsigns and forwards them to the PM window.

Settings:
- **Callsign list**: Comma-separated list of callsigns whose messages are always forwarded to the PM window.

Use case: Keep track of important stations (e.g. DX expeditions or trusted contest allies) without constantly monitoring the main chat.

---

## GUI Settings: Band-Column Hints

Two optional additions to the band columns can be enabled or disabled in the **GUI** tab:

- **Show "o" in band columns …** displays an `o` if the four-character grid square has already been worked on the relevant band. Disabling the option does not delete any database records; it only hides the additional indicator in the band columns. `wkdany` is unaffected.
- **Show "a" in band columns …** distinguishes a completely new callsign from a band opportunity involving a callsign already worked elsewhere. When disabled, both cases are displayed as `B+`. The band-opportunity calculation itself remains unchanged.

Changes are reflected in the current user interface immediately. Click **Save Settings** afterwards if they should persist after the next start.

![GUI settings for band-column hints](client_settings_window_gui.png)

---

## Worked Station Database Settings

The internal SQLite database stores contest-related state independently of the logging application's database:

- the global Worked status of a callsign,
- Worked status per band,
- manually assigned NOT-QRV marks per band, and
- worked four-character grid squares per band.

The normalised callsign, without visible chat brackets or category formatting, is used as the key. This allows active variants of the same callsign to be evaluated consistently.

Worked and NOT-QRV information expires automatically three days after its most recent change. Stored grid squares expire three days after the corresponding log entry. A manual reset before every contest is therefore normally unnecessary.

The **Reset worked, NOT-QRV and grid data...** button removes every Worked mark, NOT-QRV mark and stored worked grid square. A confirmation dialog is displayed first. Known callsign rows remain in the database; only the contest-related state is reset.

A reset is useful when you deliberately want to start with an empty contest state or have imported test data. It is not intended as a daily maintenance step.

Display and derivation: [Worked Callsigns, New Bands and New Grid Squares](en-Features#worked-callsigns-new-bands-and-new-grid-squares).

---

## Dark Mode (from v1.26)

Toggle via the menu: **Window → Use Dark Mode**. The colors can be individually customized via CSS.

---

## Saving Settings

Click **"Save Settings"** after **every** change! Without saving, all changes will be lost on the next start.

- Storage location: `~/.praktikst/preferences.xml` on Linux and `%USERPROFILE%\.praktikst\preferences.xml` (or `C:\Users\<Username>\.praktikst\preferences.xml`) on Windows
- From v1.21: Window sizes and divider positions are also saved.
- If you encounter problems: delete the configuration file → KST4Contest will create a new one with default values.
