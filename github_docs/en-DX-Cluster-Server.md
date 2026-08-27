# Built-in DX Cluster Server

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-DX-Cluster-Server)

Since version 1.23, KST4Contest has included a local DX Cluster server. It forwards detected directional opportunities and their frequencies to the DX Cluster client of a logging programme.

The idea came from OM0AAO, Viliam Petrik. Thank you!

---

## Why Use a Local DX Cluster Server?

Finding an interesting frequency in the chat is only the first step. During a contest, that information needs to reach the place where it can be used immediately: the logging programme and its bandmap.

KST4Contest therefore combines two pieces of information it already has:

1. A directed chat message can indicate the approximate direction in which the sending station may be pointing its antenna.
2. A frequency for that station may be known from the same message or an earlier one.

When both pieces fit, KST4Contest creates a local DX Cluster spot. The logger can display it in its bandmap and, depending on its own configuration, tune the transceiver to the frequency when the spot is clicked.

In practical terms, the operator does not have to find the information in the chat, read it, remember it and enter it again in the logger. These small interruptions consume a surprising amount of attention during a contest.

---

## Automatic Spots from Directional Opportunities

Assume that station A sends a directed message to station B. KST4Contest uses the direction from A to B as an approximation of the current antenna direction of station A. It then checks whether the local station lies inside the assumed antenna corridor as seen from A.

Two directions are compared:

- the direction from station A to station B;
- the direction from station A to the local station.

The **Antenna Beamwidth** configured under the Station settings is the complete angle. Half of that value is applied on either side of the direction A → B. A setting of `70°` therefore produces a corridor of `±35°`.

ON4KST does not supply antenna data for the remote station. KST4Contest therefore also uses the locally configured beamwidth as an approximation for station A. This is not a measurement of the station's actual antenna direction. It is a deliberately simple geometrical assumption.

An automatic DX Cluster spot is created only when all of the following conditions are met:

1. A directed message between two other stations has been detected.
2. Valid locators are known for the sender and receiver.
3. The sender is within the configured **Default Maximum QRB** from the local station.
4. The local station lies inside the assumed antenna corridor as seen from the sender.
5. A usable frequency is known for the sender or detected in the current message.
6. The local DX Cluster server is enabled.

When these conditions are met, the spot is created while the message is processed. The green directional highlight shown in parallel remains visible for five minutes and may be extended or removed by later messages.

The calculation does not consider terrain, current propagation or the station's actual operating intention. It identifies a plausible opportunity. The full derivation and a numerical example are available under [Directional Opportunities from Directed Messages](en-Features#directional-opportunities-from-directed-messages).

---

## Manual Spot for the Selected Map Station

A spot can also be triggered deliberately. Select a station on the station map and use **Trigger cluster spot** in the detail panel.

This manual action does not require a previously detected directed message. The maximum QRB and antenna beamwidth also do not decide whether the spot is sent. It requires:

- the local DX Cluster server to be enabled;
- at least one connected DX Cluster client; and
- a usable QRG for the selected map station.

This lets the operator send an already selected station to the bandmap even when the conditions for an automatic directional spot are not present. Map operation is described under [Station Map](en-User-Interface#station-map).

Both automatic and manual spots are sent only to clients connected to KST4Contest. They are not forwarded to a public Internet cluster.

---

## Which Frequency Is Used?

A DX Cluster spot needs an unambiguous frequency. KST4Contest uses the same QRG detection as the user list and the other band-related functions.

Complete frequencies determine their band directly:

```text
144.205
432,088
1296.338
10368.100
```

Relative values contain only the frequency part within a band:

```text
.205
,205
qrg 205
freq is 205
on 205
205 MHz
```

A bare three-digit number such as `205` is not evaluated without frequency-related context. The same applies to `599`, `144` or text such as `worked 210 stations`. This prevents signal reports, band names or QSO totals from being stored as plausible-looking frequencies and later sent to the logger.

For a relative QRG, KST4Contest determines the band in this order:

1. It checks whether a suitable band context has been detected for the same sender during the previous 30 minutes.
2. If several current bands are known, it uses the most recently updated plausible context.
3. Only when no suitable station context exists does it use the band selected under **Fallback band for relative QRG detection**.

Example:

```text
Global fallback: 144 MHz
Most recent complete QRG for the station: 432.088 MHz
New chat value from the same station: .100
Detected QRG: 432.100 MHz
DX Cluster frequency: 432100.0 kHz
```

Without the current 432 MHz context, the same value would use the global fallback and become `144.100 MHz`.

QRG detection runs before the direction and spot checks. If a station mentions its frequency for the first time in the directed message which also triggers a directional opportunity, the resulting spot can already contain that frequency. A newly detected QRG replaces an older value for the station.

The fallback band is a global QRG-detection setting. Its effect is not limited to the DX Cluster server. Configuration, supported bands and related behaviour are described under [Fallback Band for Relative QRG Detection](en-Configuration#fallback-band-for-relative-qrg-detection).

---

## Setting Up KST4Contest

Open the **Notification** tab in Preferences.

![Notification settings and local DX Cluster server](client_settings_window_notification.png)

Configure the following:

1. Enable **Enable the local DX Cluster server …**.
2. Enter a free **TCP port**. The default is `8000`.
3. Select the appropriate band under **Fallback band for relative QRG detection**.
4. Enter a **Spotter callsign**.

The spotter callsign should preferably differ from the contest callsign. Some loggers filter spots which appear to originate from the local station or handle them differently. Using the same callsign is not prohibited by KST4Contest, but it may make a correctly generated spot invisible in the bandmap.

Changes to the enabled state and TCP port take effect immediately while KST4Contest is connected to the chat. Changing the port disconnects existing DX Cluster clients; the logger must reconnect to the new port.

Use **Save Settings** to store the settings permanently in `preferences.xml`.

---

## Setting Up the Logging Programme

Configure the logging programme as a DX Cluster client connected to KST4Contest.

| Setting | KST4Contest and logger on the same computer | Logger on another computer |
|---|---|---|
| Host | `127.0.0.1` | IP address of the KST4Contest computer |
| Port | TCP port configured in KST4Contest | TCP port configured in KST4Contest |
| Login | Any callsign, if the logger requires one | Any callsign, if the logger requires one |
| Password | Not required | Not required |

KST4Contest does not use the login sent by the logger for authentication. The connection is intended for the local computer or a trusted station network.

If the logger runs on another computer, its TCP connection must be allowed through the local firewall on the KST4Contest computer. Do not expose the port directly to the Internet without additional protection.

Several DX Cluster clients can be connected at the same time. Every generated spot is sent to all clients which are currently connected.

---

## Testing the Connection

The **Send test spot** button creates the following test entry:

```text
Spotted callsign: DO5AMF
Comment: Testing DXC-Spot: Congrats, you donated $100!
Frequency: .300 on the configured fallback band
```

With `144 MHz` selected as the fallback band, the spot therefore appears at approximately `144.300 MHz`.

The comment is a deliberately retained Easter egg. It only makes the test spot easy to recognise in the logger. No donation or other external action is triggered.

Three conditions must be met before running the test:

1. KST4Contest is connected to the ON4KST chat.
2. The local DX Cluster server is enabled.
3. The logging programme's DX Cluster client is connected to KST4Contest.

If no client is connected, KST4Contest displays a corresponding message. A successful test therefore confirms that at least one connected client received the generated spot.

---

## Content of a Generated Spot

A spot contains:

- the configured spotter callsign;
- the normalised frequency;
- the complete visible callsign of the detected or selected station;
- the locator; and
- the current UTC time.

For automatically generated directional spots, KST4Contest can add up to two current AirScout entries to the comment. Missing AirScout data does not prevent the spot from being sent. A spot triggered manually from the station map uses the selected station's locator without this optional addition.

An automatic comment with AirScout information may look like this:

```text
JN49GL , AP: 1min, 100%; 4min, 75%
```

---

## If No Spot Appears

### The Test Spot Does Not Reach the Logger

Check:

- Is KST4Contest connected to the chat?
- Is the local DX Cluster server enabled?
- Does the logger use the same TCP port?
- Does the logger use `127.0.0.1` when both programmes run locally?
- Is a firewall blocking the connection?
- Is the DX Cluster window or bandmap enabled in the logger?

### The Test Works, but Automatic Spots Are Missing

The TCP connection is then working in principle. At least one condition for the relevant chat situation was probably not met:

- no directed message between two other stations;
- missing locator;
- sender outside the maximum QRB;
- direction outside the configured beamwidth;
- no detected frequency.

KST4Contest deliberately does not send every frequency it finds to the logger. Otherwise, a feature intended to reduce distraction would quickly become a local spot generator with rather too much enthusiasm.

### A Manually Triggered Spot Is Missing

Check that a station is selected on the map and that it has a usable QRG. The local server must be enabled and at least one client must be connected. Directional-message geometry, maximum QRB and beamwidth are not prerequisites for the manual action.

### The Spot Appears on the Wrong Band

First check which frequencies were detected for the station during the previous 30 minutes. For a relative value, this station context takes priority over the global fallback.

If no current station context exists, check **Fallback band for relative QRG detection**. The fallback is used only when the band cannot be determined from a complete frequency or the sender's current context.

### The Logger Hides the Spot

Try a spotter callsign which differs from the contest callsign. Depending on the logger, spots from the local callsign may be filtered or handled specially. KST4Contest itself does not require the two callsigns to differ.

---

## Tested Logging Programmes

The interface has been used with:

- UCXLog
- N1MM+
- Minos

Other loggers may work if they support a normal TCP connection to a DX Cluster server and accept conventional `DX de ...` spot lines.

Related reference pages:

- [User Interface](en-User-Interface)
- [Features](en-Features)
- [Configuration](en-Configuration)
