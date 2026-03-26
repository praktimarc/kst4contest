# Log Synchronisation

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-Log-Synchronisation)

KST4Contest automatically marks worked stations in the chat user list. Two basic methods are available:

---
![Log Sync Settings Window](client_settings_window_logsync.png)

## Method 1: Universal File Based Callsign Interpreter (Simplelogfile)

KST4Contest reads a log file and searches for callsign patterns using a regular expression. Binary log files are also supported – unreadable binary content is simply ignored.

**Advantage**: Works with almost any logging program that writes a file.
**Disadvantage**: No band information available – stations are only marked as "worked", not on which band.

Enter the path to the log file in the Preferences. The file is only read, never modified (read-only).

> **Tip**: The Simplelogfile function can also be used to mark stations that are definitely unreachable (e.g. personal notes). This will be replaced in a later version by a better tagging system.

---

## Method 2: Network Listener (UDP Broadcast) – Recommended

When saving a QSO, the logging software sends a UDP packet to the broadcast address of the home network. KST4Contest receives this packet and marks the station including **band information** in its internal SQLite database.

> **Important**: KST4Contest must be **running in parallel with the logging software**. QSOs logged while KST4Contest is not running will not be captured – except with QARTest (which can send the complete log).

**Default UDP port**: 12060 (matches the default of most logging programs)

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

### Win-Test

Win-Test is supported with a dedicated UDP network listener that understands the native Win-Test network protocol.

**Advantages of Win-Test Integration:**
- Automatic QSO synchronization to mark worked stations.
- **Sked Handover (ADDSKED):** Using the "Create sked" button in the station info panel not only creates a sked in KST4Contest but also *sends it directly via UDP to the Win-Test network as an ADDSKED packet*.
- You can choose between "AUTO", "SSB", or "CW" sked modes.

**Required Settings in KST4Contest:**
- `UDP-Port for Win-Test listener` (Default: 9871).
- Enable `Receive Win-Test network based UDP log messages`.
- Enable `Win-Test sked transmission (push via ADDSKED to Win-Test network)`.
- `KST station name in Win-Test network (src of SKED packets)`: Defines the station name KST4Contest uses in the WT network (e.g., "KST").
- `Win-Test station name filter`: If a name is entered here (e.g., "STN1"), only QSOs from that specific Win-Test instance will be processed. Leave empty to accept all.
- `Win-Test network broadcast address`: Is usually detected automatically and is required to send sked packets to the network.

**Settings in Win-Test:**
- The network in Win-Test must be active.
- Win-Test must be configured to send/receive its broadcasts on the corresponding port (default 9871).

---

## TRX Frequency Synchronisation

In addition to QSO synchronisation, UCXLog and other programs also transmit the **current transceiver frequency** via UDP. KST4Contest processes this information and makes it available as the `MYQRG` variable.

![Frequency Buttons](qrg_buttons.png)

**Result**: Your own QRG never needs to be typed manually in the chat – clicking the MYQRG button or using the variable in the beacon is sufficient.

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

KST4Contest stores worked information in an internal **SQLite database**. This is independent of the logging program's database and is only populated via the UDP broadcast.

Before each new contest: reset the database! → [Configuration – Worked Station Database Settings](Configuration#worked-station-database-settings)
