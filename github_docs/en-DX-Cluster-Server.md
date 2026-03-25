# Built-in DX Cluster Server

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-DX-Cluster-Server)

From **version 1.23**, KST4Contest includes a built-in DX cluster server. It sends spots directly to the logging software whenever a direction warning is triggered.

*(Idea by OM0AAO, Viliam Petrik – thank you!)*

---

## What is the Built-in DX Cluster Server For?

When KST4Contest detects that a station is requesting a sked from your direction and a QRG is known, it **automatically generates a DX cluster spot** and feeds it directly to the logging software's cluster client / band map.

The logging software then displays the spot in the band map. Clicking the spot sets the transceiver's frequency and mode directly – without any manual typing.

---

## Setup

### In KST4Contest

In Preferences → **DX Cluster Server Settings**:

1. Enter the **port** of the internal server (e.g. 7300 or 8000 – must match the logging software).
2. Enter a **spotter callsign** – **this must be a different callsign than your contest callsign!**
   - Reason: Logging programs filter spots from your own callsign as "already worked". If the spotter uses the same callsign, the spots will not be displayed.
3. Enter the **assumed MHz**: For frequency references like ".205" in the chat, KST4Contest needs to decide whether 144.205, 432.205 or 1296.205 is meant. For single-band contests, simply enter the corresponding band centre. Full frequency references like "144.205" or "1296.338" in the chat are always correctly identified.

### In UCXLog

- Configure a DX cluster server connection:
  - Host: `127.0.0.1` (or IP of the KST4Contest computer)
  - Port: As configured in KST4Contest
  - Password: can be left empty
- Use the **"Send a test message to your log"** button to test the connection.

### In N1MM+

Similar settings:
- Host: `127.0.0.1` (or IP of the KST4Contest computer)
- Port: As configured in KST4Contest

---

## How It Works

A spot is generated when **both** conditions are met:

1. A **direction warning** has been triggered (station is making a sked in your direction).
2. The **station's QRG is known** (read from the chat or manually entered).

The generated spot contains:
- Station's callsign
- Frequency
- Spot time

The logging software can then display the spot in the band map and tune the TRX to that frequency with a mouse click.

---

## Multi-Computer Setup

If KST4Contest runs on a separate computer (not the logging computer):

- Host in the logging software: IP of the KST4Contest computer (not `127.0.0.1`)
- Same configuration as for the QSO UDP broadcast packets (see [Log Synchronisation](en-Log-Sync))

---

## Tested Logging Software

- **UCXLog** ✓
- **N1MM+** ✓

Further test reports are welcome – please send by email to DO5AMF.
