---
title: Local DX Cluster Server
icon: 📡
category: Logger Integration
since: "1.23"
summary: Forward detected directional opportunities and their known frequencies as local DX Cluster spots to compatible contest loggers.
description: KST4Contest provides a local TCP DX Cluster server which forwards automatic directional opportunities or a manually selected map station to a logger bandmap.
tagsList:
  - DX Cluster
  - bandmap
  - ON4KST
  - QRG detection
  - UCXLog
  - N1MM+
  - Minos
  - contest logger
related:
  - global-message-views
  - qrg-detection
  - station-map
  - directional-opportunities
  - log-sync
  - priority-score
  - dual-chat
---

## What kind of DX Cluster is this?

KST4Contest does not connect to a public DX Cluster and does not import external spots.

Instead, it provides a local TCP server to which a compatible contest logger can connect as a DX-Cluster client. KST4Contest uses that connection to forward selected information already detected in the ON4KST chat.

The purpose is practical: when a station appears to be pointing in the local direction and its QRG is known, the information can appear directly in the logger bandmap. The operator no longer has to read the frequency in the chat, remember it and enter it again in the logger.

That is the entire idea. The function is a bridge between the KST4Contest analysis and the logger, not another source of general DX traffic.

## Automatic and manual spots

An automatic spot is generated only when all of the following conditions are met:

1. A directed message between two other stations has been detected.
2. Valid locators are available for the sender and receiver.
3. The sender is within the configured maximum QRB.
4. The local station lies inside the assumed antenna corridor of the sender.
5. A usable frequency is known for the sender.
6. The local DX Cluster server is enabled.
KST4Contest deliberately does not forward every frequency mentioned in the chat. Otherwise, a function intended to reduce distraction would produce its own local spot flood.

A spot can also be triggered deliberately. Select a station on the station map and use **Trigger cluster spot** in the detail panel. This manual action does not require a directed message, a match with the maximum QRB or a match with the configured antenna beamwidth. It uses the selected station and its known QRG directly.

For either route, at least one DX-Cluster client must be connected to receive the spot. Both automatic and manual spots remain inside the local or trusted station network; KST4Contest does not forward them to a public DX Cluster.

## How is the directional opportunity derived?

Assume that station A sends a directed message to station B. KST4Contest uses the direction from A to B as an approximation of the current antenna direction of station A.

It then compares:

- the direction from A to B; and
- the direction from A to the local station.

The configured antenna beamwidth is treated as the complete angle. Half of the value is applied to either side of the direction from A to B.

For a configured beamwidth of `70°`, the assumed corridor is therefore `±35°`.

If the local station lies inside this corridor, KST4Contest marks the sender as a directional opportunity. When a QRG is also available, the local DX Cluster spot can be generated immediately.

This is a geometric approximation. ON4KST does not transmit the actual antenna direction or beamwidth of the remote station, so KST4Contest uses the locally configured beamwidth as a practical substitute.

Terrain, propagation and the actual operating intention of the sender are not part of this calculation.

## Which frequency is used?

KST4Contest uses the same QRG detection that supplies the frequency column and other band-related functions.

Complete frequencies determine their band directly:

```text
50.200
70.250
144.205
432.088
1296.338
10368.100
24048.100
```

The value is converted into the kHz representation expected by the DX Cluster protocol:

| Chat frequency | DX Cluster frequency |
|---|---:|
| `50.200` | `50200.0` |
| `144.205` | `144205.0` |
| `1296.338` | `1296338.0` |
| `10368.100` | `10368100.0` |
| `24048.100` | `24048100.0` |

An additional sub-kHz group is retained. For example, `144.205.2` becomes `144205.2`.

## Relative QRG information

A relative frequency contains only the part inside a band:

```text
.205
,205
qrg 205
freq is 205
on 205
```

KST4Contest determines the missing band in this order:

1. a recent band context detected for the same station during the previous 30 minutes;
2. the most recently updated plausible context if several bands are known;
3. the configured **Fallback band for relative QRG detection**.

Example:

```text
Configured fallback: 144 MHz
Recent complete QRG of the station: 432.088 MHz
New message from the same station: .100
Result: 432.100 MHz
DX Cluster value: 432100.0 kHz
```

Without the recent station-specific context, the same `.100` would use the configured fallback and become `144.100 MHz`.

A bare three-digit value is accepted only when the surrounding text identifies it as a frequency. This prevents values such as `599`, a bare band name or the number of completed QSOs from becoming plausible-looking spots.

## What does the spot contain?

Every generated spot contains:

- the configured spotter callsign;
- the normalised frequency;
- the complete visible callsign of the detected or selected station;
- the locator of that station;
- the current UTC time.

Automatically generated directional spots can additionally include up to two current AirScout entries. A manually triggered map spot uses the selected station's locator without this optional addition.

The payload is a fixed, DXSpider-compatible 75-character line. The DX callsign begins in column 27, the 30-character comment begins in column 40 and the UTC time begins in column 71. Short comments are padded; longer comments are limited to the available field. A DX callsign longer than twelve characters is rejected rather than silently truncated.

An example comment with AirScout information may look like this:

```text
JO51HK AP 1m/100%;4m/75%
```

AirScout information is optional. A missing AirScout response does not prevent an automatic directional spot from being sent.

> AP-independent spot creation, corrected sender-locator handling and band-generic frequency conversion are included from v1.42 onwards.

## Connecting a logger

Open the **Notification** tab in the KST4Contest settings.

![Notification and local DX Cluster settings](/manual/assets/client_settings_window_notification.png)

Configure:

1. **Enable the local DX Cluster server …**
2. A free TCP port. The default is `8000`.
3. The fallback band used for relative QRG information.
4. A spotter callsign.

The logger is then configured as the DX-Cluster client:

| Setting | Same computer | Separate logger computer |
|---|---|---|
| Host | `127.0.0.1` | IP address of the KST4Contest computer |
| Port | Configured KST4Contest TCP port | Configured KST4Contest TCP port |
| Login | Any callsign if required | Any callsign if required |
| Password | Not required | Not required |

The spotter callsign should preferably differ from the contest callsign. Some loggers suppress or specially handle spots which appear to originate from the local station. A spot can therefore be generated correctly and still remain invisible in the bandmap.

## Several connected clients

The local server can retain several DX-Cluster client connections. A generated spot is sent to every client currently connected.

KST4Contest sends an empty keep-alive line every 30 seconds so idle connections are less likely to be closed unnoticed. Clients which disconnect while a spot is being sent are removed from the active connection list.

Changing the configured port while connected restarts the local server. Existing logger connections must then reconnect to the new port.

## Network boundary

The server does not authenticate the login supplied by a DX-Cluster client. It is intended for the local computer or a trusted station network.

When the logger runs on another computer:

- use the IP address of the KST4Contest computer;
- allow the configured TCP port through the local firewall; and
- do not expose the server directly to the internet.

A missing password is acceptable inside the intended station network. It is not a security concept for a public service.

## Testing the connection

Use **Send test spot** after the logger has connected.

The test uses `DO5AMF` with the comment `DXC test: You donated $100!` and `.300` on the configured fallback band.

A successful test confirms that at least one client received the generated spot. If the test works but real spots do not appear, the TCP connection is probably not the problem. In that case, check the conditions used for the actual directional opportunity:

- Were valid locators available?
- Was the sender within the maximum QRB?
- Was the direction inside the configured beamwidth?
- Was a valid frequency known?
- Did a station-specific band context change the relative QRG?

For a manual spot, check that the station remains selected on the map and has a usable QRG. Maximum QRB, beamwidth and directed-message geometry are not prerequisites for **Trigger cluster spot**.

## What the spot means — and what it does not

The spot means that KST4Contest detected a plausible directional opportunity and knew a frequency for the sender.

It does not prove:

- the actual antenna direction of the station;
- that the station intends to work the local operator;
- that the path is currently open;
- that the frequency is still unchanged; or
- that a QSO will succeed.

The logger may tune the transceiver to the spot frequency, depending on its own configuration. The operator still decides whether calling the station makes sense.

In plain terms: KST4Contest can remove the typing. It cannot remove the judgement.

## Tested loggers

The local DX Cluster interface has been used with:

- UCXLog
- N1MM+
- Minos

Other loggers may work if they can open a normal TCP connection to a DX Cluster server and accept conventional `DX de ...` spot lines.

[Read the complete setup and troubleshooting section in the manual.](/manual/en/dx-cluster-server/)

[Read how directional opportunities are derived.](/manual/en/features/#directional-opportunities-from-directed-messages)

[Read how relative QRG information is configured.](/manual/en/configuration/#fallback-band-for-relative-qrg-detection)
