---
title: Directional Opportunities
icon: 📐
category: Contest Workflow
since: "1.1"
summary: Recognise when a station in a directed ON4KST exchange may be pointing towards the local station and highlight the short-lived opportunity.
description: KST4Contest derives a possible antenna direction from directed ON4KST messages and marks matching senders without requiring the local DX Cluster server.
tagsList:
  - ON4KST
  - directional opportunity
  - antenna direction
  - beamwidth
  - QRB
  - contest workflow
related:
  - band-recognition
  - dx-cluster
  - rotator-control
---

## Why watch messages between other stations?

A directed ON4KST message shows who is talking to whom. It does not include an antenna direction. During a contest, however, a station requesting, answering or preparing a sked will often point at least approximately towards the station being addressed.

That creates a brief opportunity for stations near the same direction. KST4Contest has recognised this situation since version 1.1. The detection and user-list highlight work independently of the local DX Cluster server.

## How is the opportunity derived?

Assume that station A sends a directed message to station B. KST4Contest:

1. calculates the direction from A to B and treats it as the likely antenna direction of A;
2. calculates the direction from A to the local station;
3. compares the angular difference with half of the configured antenna beamwidth; and
4. checks that A is inside the configured maximum QRB.

If the local station lies inside this assumed antenna corridor, A becomes a directional opportunity. A reply from B is a new case: the likely direction is then calculated from B back towards A.

The configured beamwidth is the complete angle. With `70°`, KST4Contest therefore assumes a corridor of `35°` on either side of the direction between the two stations.

## What does the operator see?

The sender's callsign appears green and bold in the user list for five minutes. A later matching message restarts this period. If the station sends another directed message which no longer matches the geometry, the mark is removed immediately.

![Detected directional opportunity in the user list](/manual/assets/direction_opportunity_highlight.png)

When simple sound notifications are enabled, the first detection also produces a short acoustic indication. Further matching messages do not repeat the sound while the station is already marked.

## What practical value does it have?

The indication draws attention to a station at the moment when its antenna may already cover the local direction. It can be enough to notice a callsign which would otherwise remain one line among many in the chat.

DM5M reports that around 35–40% of the directional opportunities used at that station have resulted in a QSO. This is station-specific operating experience, not a general success rate. Location, antenna patterns, propagation, contest activity and operating practice can produce very different results elsewhere.

## Optional use in the logger bandmap

Since version 1.23, KST4Contest can forward a detected opportunity and a known QRG through its local DX Cluster server. This is an optional second step. The direction calculation, green user-list mark and sound do not depend on a DX-Cluster connection.

The local DX Cluster output reuses the result; it does not create the directional opportunity. If the server is disabled or no suitable QRG is known, the visual indication still works normally.

## What does the indication not prove?

ON4KST does not transmit the actual antenna direction or beamwidth of the remote station. KST4Contest therefore uses the locally configured antenna beamwidth as a practical approximation.

The calculation also does not know the real antenna pattern, rotator position, terrain or current propagation. A matching corridor is not a propagation forecast and does not guarantee a QSO.

In practical terms: the green callsign identifies a geometrically plausible moment to pay attention. Whether calling makes sense remains an operator decision.

[Read the complete derivation and numerical examples in the manual.](/manual/en/features/#directional-opportunities-from-directed-messages)

[Open the antenna beamwidth setting.](/manual/en/configuration/#antenna-beamwidth)

[Open the maximum QRB setting.](/manual/en/configuration/#default-maximum-qrb)

[Read how the optional local DX Cluster output works.](/features/dx-cluster/)
