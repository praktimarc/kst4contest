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

## Worked Marking

Worked stations are visually marked in the user list – per band. Based on [Log Synchronisation](en-Log-Sync) via UDP or Simplelogfile.

Reset the database before each contest: [Configuration – Worked Station Database Settings](Configuration#worked-station-database-settings).

---

## NOT-QRV Tags (from v1.2)

When a station indicates it is not QRV on a specific band, this can be manually marked:

1. Select the station in the user list.
2. Right-click → Set NOT-QRV for the appropriate band.

These tags are stored in the internal database and persist after a KST4Contest restart. Can be reset via the settings.

**Benefit**: Prevents repeated sked requests on bands where the station is not active – saves time for both sides.

---

## Direction Filter

Shows only stations in the user list that are located in a specific direction. Toggle using the N / NE / E / SE / S / SW / W / NW buttons or by entering degrees manually.

Useful: While calling CQ in a specific direction, only show stations in that direction.

---

## Distance Filter

Hide stations beyond a maximum distance. The **"Show only QRB [km] <="** button is a toggle.

---

## Worked and NOT-QRV Filter

Toggle buttons (one per band) to hide already-worked stations and/or NOT-QRV-tagged stations. The filter takes effect **immediately** without manually reactivating (live since v1.22).

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

## Sked Reminders with ALERT (from v1.40)

A sked reminder service with automatic messages can be activated for each chat member. Configurable interval patterns:

- **2+1 minutes**: Messages at 2 min and 1 min before the sked.
- **5+2+1 minutes**: Messages at 5, 2 and 1 min before the sked.
- **10+5+2+1 minutes**: Messages at 10, 5, 2 and 1 min before the sked.

In addition to the automated messages to the remote station, there is an **acoustic and visual notification** for your own operator so no sked is ever missed.

Activate from the FurtherInfo panel of the corresponding station.

---

## QSO Sniffer (from v1.31)

The QSO sniffer monitors the chat for messages from a configurable callsign list and automatically forwards them to the **PM window**. This prevents relevant messages from being lost in the general chat traffic.

Configuration: [Configuration – Sniffer Settings](en-Configuration#sniffer-settings-from-v131)

---

## Win-Test Integration (from v1.31, fully configurable from v1.40)

KST4Contest fully supports [Win-Test](https://www.win-test.com/) as a logging programme:

- **Log synchronisation**: Worked stations are automatically retrieved from Win-Test and marked in the user list.
- **Frequency parsing**: The current TRX frequency is read from Win-Test UDP packets and populates the `MYQRG` variable.
- **Sked handover (SKED push via UDP)**: Agreed skeds from KST4Contest can be pushed directly to Win-Test, so the remote callsign appears in Win-Test's sked window.

Details: [Configuration – Win-Test Network Listener](en-Configuration#win-test-network-listener)

---

## PSTRotator Interface (from v1.31, fully configurable from v1.40)

KST4Contest can control antenna direction directly via **PSTRotator**. When a station is selected in the user list, the rotator can automatically be turned to the QTF of the selected station.

Configuration: [Configuration – PSTRotator Settings](en-Configuration#pstrotator-settings-from-v131)

---

## Band Alert for New QSOs (from v1.40)

When a station is logged, KST4Contest automatically checks whether that station has shown any other active bands in the chat that you are also QRV on. If so, a **hint alert** appears so no multi-band opportunity is missed.

---

## Worked Tag Lifetime (from v1.40)

Worked stations are automatically removed from the database after **3 days**. Manually resetting the worked database before each contest is therefore no longer strictly necessary – the database keeps itself up to date.

---

## Chatmember Score System / Priority List (from v1.40)

KST4Contest automatically calculates a **priority score** for each active chat member. The score is derived from:

- Antenna direction of the remote station (is it pointing towards me?)
- QRB (distance)
- Activity time and message count
- Active bands and frequencies
- AP availability (AirScout)
- Sked direction (degrees)
- Sked success rate and skedfail markings

The top candidates are highlighted in a dedicated priority list, helping you not to miss the most important contacts during contest stress.

Stations with a failed sked can be marked using the **Skedfail button** in the FurtherInfo panel – this temporarily lowers their score.

---

## AP Timeline (from v1.40)

A visual timeline shows up to 4 highly-scored stations per minute slot that should be workable via aircraft scatter. Prioritisation criteria:

- **Highest reflection potential** is preferred (not necessarily the fastest arrival).
- Stations towards which your antenna is not pointing are shown **transparently**.

This gives the contest operator a quick overview of which stations will be reachable via which aircraft and at what time.

---

## Interval Beacon

Automatic CQ messages in the public channel at a configurable interval. Recommended: use the `MYQRG` variable so the current frequency is always accurate. Details: [Configuration – Beacon Settings](Configuration#beacon-settings).

---

## Simplelogfile

File-based log evaluation using regex. Details: [Log Synchronisation](Log-Sync#method-1-universal-file-based-callsign-interpreter-simplelogfile).

---

## Cluster & QSO of Others

A separate window showing the QSO flow between other stations. Particularly interesting during quieter night-time hours of a contest. This window can be minimised when not needed. Future plan: filtering to stations in your selected QTF.
