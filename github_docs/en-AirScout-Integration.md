# AirScout Integration

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-AirScout-Integration)

AirScout (by DL2ALF) calculates aircraft-scatter opportunities from current aircraft positions. KST4Contest receives these results and displays aircraft suitable for the path to each remote station directly in the user list.

> **Aircraft Scatter** enables very long-distance communication on VHF and higher – even for stations with low altitude above sea level or unfavourable topographic conditions.

---

## Downloading AirScout

Download AirScout from:
- http://airscout.eu/index.php/download

---

## Aircraft Data Feeds (ADS-B)

Public aircraft data feeds on the internet are often unreliable and of limited use. A recommended alternative is the dedicated ADS-B feed service provided by **OV3T (Thomas)**:

- https://airscatter.dk/
- https://www.facebook.com/groups/825093981868542

An account is required for this service. Please consider donating to Thomas – the server costs are not free!

---

## Setting Up AirScout

### Step 1: Configure the ADS-B Feed in AirScout

1. Start AirScout.
2. Enter your OV3T feed account details (username, password, URL) in the AirScout settings.

![AirscoutStep1](as_plane_feed_1.png)
![AirscoutStep2](as_plane_feed_2.png)

3. Test the connection.

### Step 2: Enable UDP Communication for KST4Contest

In AirScout, enable the UDP interface:

- Activate the corresponding checkbox in the AirScout settings (only one checkbox needed).
- Do not change the default ports unless there is a specific reason.

### Step 3: KST4Contest Settings

In KST4Contest Preferences → **AirScout Settings**:
- Enable AirScout communication
- Leave IP and port at their default values (unless changed)

![AirscoutStep3](as_plane_feed_3.png){ width=85% }



---

## Communication Between KST4Contest and AirScout (from v1.263)

**Improvement in v1.263**: KST4Contest now only sends stations to AirScout whose QRB (distance) is less than the configured **maximum QRB**. The query interval has been extended from 12 seconds to **60 seconds**.

**Benefits:**
- Significantly less computation load for AirScout
- Significantly less message traffic
- The tracking issue with the "Show Path in AirScout" button is greatly improved
- Less overall CPU usage

Additionally: The name of the KST4Contest client and AirScout server was previously hardcoded (`KST` and `AS`). From v1.263, the names configured in the Preferences are used.

---

## Multiple KST4Contest Instances and AirScout

> **Note**: If multiple KST4Contest instances are running simultaneously and AirScout communication is enabled on both, AirScout will respond **to both instances**.

This is not a problem if:
- Both instances use the same locator, **or**
- Both instances have different login callsigns.

Otherwise, it may result in incorrect AP data.

---

## AP Column in the User List

After setup, an **AP column** appears in the user list. For each station, it shows the arrival time and the AirScout reflection potential of the first two suitable aircraft.

Example display:

| Station | AP Info |
|---|---|
| DF9QX | 0 (100%) / 0 (100%) |
| F5DYD | 14 (50%) / 31 (50%) |

The number before the brackets is the number of minutes remaining until the calculated opportunity. The percentage is the reflection potential reported by AirScout. It is not a QSO probability.

AP information is also available in the **private messages window**.

## Effect on the Priority Score and Timeline

At least one aircraft reported as reachable by AirScout raises the station's Priority Score. An opportunity expected in zero, one or two minutes receives additional time-dependent weighting. AirScout is only one factor alongside Worked status, available bands, QRB, antenna direction, chat activity and skeds.

The AP and sked timeline uses the Priority Score to select interesting stations and places the next suitable aircraft-scatter opportunity on the time axis. The reflection potential also controls how the AP marker is displayed. The timeline remains a preview and does not guarantee a QSO.

---

## AP Variables in Messages

Aircraft data for the selected station can be inserted directly into shortcuts, snippets and other station-specific messages:

- `FIRSTAP` → e.g. `a very big AP in 1 min`
- `SECONDAP` → e.g. `Next big AP in 9 min`

Details: [Macros and Variables](en-Macros-and-Variables#variables)

Because these values require a selected remote station, `FIRSTAP` and `SECONDAP` are not available as global beacon variables.

---

## "Show Path in AirScout" Button

In the user list there is a button with an arrow showing the direction (QTF) to the selected station. Clicking it maximises the external AirScout window and displays the path to the selected remote station together with the calculated aircraft-scatter opportunities. The button does not start a separate terrain or propagation calculation in KST4Contest.
