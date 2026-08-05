# Log Synchronisation

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-Log-Synchronisation)

KST4Contest imports worked stations from the logging application and derives the global Worked status, per-band Worked marks and – where a locator is available – worked grid squares. Three input paths are available: the file-based Simplelogfile interpreter, the general QSO UDP listener and the dedicated Win-Test network listener.

---

![Log Sync Settings Window](client_settings_window_logsync.png)

## Method 1: Universal File Based Callsign Interpreter (Simplelogfile)

KST4Contest reads a log file and searches it for callsigns using a configurable regular expression. The file is read only and is never modified. Binary log files can also be used; content which cannot be interpreted as text is skipped.

The advantage is broad compatibility: no dedicated network interface is required from the logging application.

The limitation is equally clear. A callsign match alone provides neither a reliable band nor a locator. The Simplelogfile interpreter can therefore set only the global Worked status. It does not create a per-band `X`, a worked-grid record or a reliable basis for the band-upgrade hint after a log entry.

Configure the log-file path and regular expression in the **Log sync** tab. Use one of the network interfaces where possible if band-specific information is required.

---

# Method 2: Network Listener for QSO UDP Packets – Recommended

UCXLog, QARTest, N1MM+ and DXLog.net can transmit a UDP packet when a QSO is saved. KST4Contest receives these packets on port `12060` by default and imports the callsign together with any band and locator information they contain.

If a band is available, the callsign is marked as worked on that band. If the packet also contains a valid locator, KST4Contest stores its four-character grid square for that band. Missing information is not inferred from unrelated fields.

KST4Contest must be running when the packet is transmitted. Some logging applications can, however, resend an existing log: QARTest provides **Invia log completo**, while DXLog.net sends `contactreplace` packets when broadcasting the complete log. KST4Contest processes both mechanisms.

**Default port:** `12060`

---

## Supported Logging Software

### UCXLog (DL7UCX)

![UCXLog Configuration](ucxlog_logsync.png)

UCXLog sends QSO UDP packets and transceiver frequency packets.

**Settings in UCXLog:**
- Enable UDP broadcast
- Enter the IP address of the KST4Contest computer (for local operation: `127.0.0.1`)
- Port: 12060 (default)

Note the green-highlighted fields in the UCXLog settings: IP and port must be filled in.

Note for multi-setup (2 computers, 2 radios, one KST4Contest instance): Both logging programs must send QSO packets to the IP of the KST4Contest computer. In this case, at least one IP is not `127.0.0.1`.

### QARTest (IK3QAR)

![QARTest Configuration](qartest_logsync.png)

**Special feature**: QARTest can send the **complete log** to KST4Contest (button "Invia log completo" in the QARTest settings). This means QSOs logged before KST4Contest was started are also captured.

**Settings in QARTest:**
- Configure UDP broadcast and IP/port as with UCXLog
- Use "Invia log completo" for a full log upload

*(„Buona funzionalità caro IK3QAR!" – DO5AMF)*

### N1MM+

**Settings in N1MM+:**

In N1MM+ under `Config → Configure Ports, Mode Control, Winkey, etc. → Broadcast Data`:
- Enable `Radio Info` (for TRX sync / QRG)
- Enable `Contact Info` (for QSO sync)
- IP: `127.0.0.1` (or IP of the KST4Contest computer)
- Port: 12060

For the built-in DX cluster server: configure N1MM+ as a DX cluster client (server: `127.0.0.1`, port as set in KST4Contest).

### DXLog.net

![DXLog.net Configuration](dxlog_net_logsync.png)

**Settings in DXLog.net:**
- Enable UDP broadcast
- Enter the IP of the KST4Contest computer (green-highlighted fields)
- Port: 12060

When broadcasting the complete logbook, DXLog.net uses `contactreplace` instead of `contactinfo`. KST4Contest processes both packet types. Older QSOs can therefore be imported by starting a complete-log broadcast while KST4Contest is running.

### Win-Test

Win-Test is supported with a dedicated UDP network listener that understands the native Win-Test network protocol.

For a new QSO, KST4Contest imports the callsign and resolves the native Win-Test band ID. This includes 50 and 70 MHz. If the packet contains a valid locator, the worked grid square is also stored for the detected band.

**Advantages of Win-Test Integration:**
- **Per-band Worked data:** New QSOs set the Worked mark for the band reported by Win-Test and update the grid-square status where a locator is available.
- Automatic QSO synchronization to mark worked stations.
- **Sked Handover (ADDSKED):** Using the "Create sked" button in the station info panel not only creates a sked in KST4Contest but also *sends it directly via UDP to the Win-Test network as an ADDSKED packet* – automatically, as soon as the listener is active. No separate toggle is needed.
- You can choose between "AUTO", "SSB", or "CW" sked modes.
- **Automatic QRG resolution for SKEDs:** KST4Contest selects the sked frequency intelligently:
  1. If the other station mentioned their QRG in a recent chat message, that frequency is used.
  2. Otherwise, your own current QRG is used (from Win-Test STATUS or manual entry).

**Settings in the "Log Synchronisation" tab:**
- Enable `Receive Win-Test network based UDP log messages`.
- `UDP-Port for Win-Test listener` (default: 9871).
- `KST station name in Win-Test network (src of SKED packets)`: Defines the station name KST4Contest uses in the WT network (e.g. "KST").
- `Win-Test network broadcast address`: Usually detected automatically; required to send sked packets to the network.

**Settings in the "TRX Synchronisation" tab:**
- `Win-Test STATUS QRG Sync`: When enabled, KST4Contest takes the current transceiver frequency from the Win-Test STATUS packet and uses it as your own QRG (MYQRG).
- `Use pass frequency from Win-Test STATUS`: Instead of the main TRX frequency, the pass frequency contained in the STATUS packet is used as MYQRG (useful for multi-op setups that operate with a dedicated pass QRG).
- `Win-Test station name filter`: If a name is entered here (e.g. "STN1"), KST4Contest only processes packets from that specific Win-Test instance. Leave empty to accept all.

**Settings in Win-Test:**
- The network in Win-Test must be active.
- Win-Test must be configured to send/receive its broadcasts on the corresponding port (default 9871).

---

## TRX Frequency Synchronisation

In addition to QSO synchronisation, UCXLog and other programs also transmit the **current transceiver frequency** via UDP. KST4Contest processes this information and makes it available as the `MYQRG` variable.

![Frequency Buttons](qrg_buttons.png)

**Result**: Your own QRG never needs to be typed manually in the chat – clicking the MYQRG button or using the variable in the beacon is sufficient.

**Sources for your own QRG (MYQRG):**
- UCXLog, N1MM+, DXLog.net, QARTest via UDP port 12060
- Win-Test STATUS packet (optional, configurable in the "TRX Synchronisation" tab under "Win-Test STATUS QRG Sync")
- Manual entry in the QRG field

> **Note for multi-setup**: With two logging programs on two computers, only **one** should send frequency packets. KST4Contest cannot distinguish between sources and processes all incoming packets.

---

## Multi-Setup: 2 Radios, 2 Computers

For DM5M-style setups (2 radios, 2 computers, one KST4Contest instance or two separate):

**Option A – One shared KST4Contest instance:**
- Both logging programs send QSO packets to the IP of the KST4Contest computer
- Only one logging program sends frequency packets (recommended: the VHF logging program)

**Option B – Two separate KST4Contest instances (recommended):**
- Each logging program communicates with its own KST4Contest instance via `127.0.0.1`
- Two separate chat logins
- Better separation and fewer conflicts

---

## Internal Database

KST4Contest stores Worked, NOT-QRV and worked-grid information in its own SQLite database. This database is independent of the logging application's database.

The input sources provide different levels of detail:

| Source | Global callsign status | Per-band status | Grid square |
|---|---:|---:|---:|
| Simplelogfile | yes | no | no |
| QSO UDP listener | yes | yes, if included in the packet | yes, if both band and locator are available |
| Win-Test network listener | yes | yes | yes, if a locator is available |

The information is restored when KST4Contest starts and updated during operation when new log entries arrive. It expires automatically after three days, so a reset before every contest is normally unnecessary.

A complete manual reset removes Worked marks, NOT-QRV marks and worked grid squares together. See [Worked Station Database Settings](en-Configuration#worked-station-database-settings) for details.