---
title: Private Message Handling
icon: ✉️
category: ON4KST Chat
since: "1.1"
summary: Catch public mentions of the configured login callsign in the PM table and use six green age levels to keep recent messages visible without changing their routing.
description: KST4Contest combines PM Catching, age-based row highlighting and reply preparation so that direct messages and relevant public mentions remain practical to handle during a busy contest.
tagsList:
  - ON4KST
  - private messages
  - PM Catching
  - message age
  - callsign mention
  - contest workflow
related:
  - automatic-replies
  - qso-monitoring
  - global-message-views
  - dual-chat
---

## Why catch more than direct PMs?

Not every message intended for one station is sent as a directed ON4KST message. An operator may accidentally post something like this publicly:

```text
(DM5M) pse ur qrg
```

Without an additional view, that line remains among CQ calls, beacons and the rest of the public chat. KST4Contest therefore also shows a public message in the PM table when its text contains the configured local login callsign.

PM Catching has existed since version 1.1. A slightly flippant description is **“gossip detection”**. It is a nickname, not the formal name of the function.

## How is a mention recognised?

The check searches the message text for the complete configured login callsign without distinguishing upper- and lower-case letters. A login of `DM5M` therefore matches both:

```text
(DM5M) pse ur qrg
dm5m are you qrv?
```

This is a text search, not a linguistic interpretation. A spelling error or shortened callsign does not match. Conversely, a sentence which merely mentions the callsign can appear in the PM table even when no reply was expected.

In plain terms: PM Catching finds visible callsign text. It cannot know what the author meant.

## The original message remains unchanged

Catching adds the message to the PM view. It does not turn the public line into a private message, change its receiver, rewrite its text or move it to another chat category.

The same principle applies to [QSO Monitoring](/features/qso-monitoring/): a monitored message may be shown additionally in the PM table, but its original routing remains intact. The two mechanisms use different criteria:

- PM Catching looks for the local login callsign in the message text.
- QSO Monitoring checks whether a configured station is actually the sender or receiver.

## Selecting a row prepares the reply

Selecting an incoming PM-table row prepares a `/cq` reply to its sender. If the selected row contains an outgoing message from the local station, KST4Contest instead restores the original receiver as the reply target.

The selection prepares the target and input context. It does not send a message automatically.

## Six green age levels

Age-based row highlighting is included from version 1.25 onwards. New non-local rows in the PM table pass through six green stages:

| Message age | Display |
|---|---|
| up to and including 30 seconds | first green level |
| 31 to 60 seconds | second green level |
| 61 to 90 seconds | third green level |
| 91 to 120 seconds | fourth green level |
| 121 to 180 seconds | fifth green level |
| 181 to 300 seconds | sixth green level |
| from 301 seconds | normal table colour |

The table refreshes the age display every five seconds. A colour boundary may therefore become visible during the next refresh rather than at the exact second.

After five minutes, the row returns to the normal table colour. Messages sent by the local station retain their separate highlight and do not use the green age scale.

## What produces a PM notification sound?

PM audio is reserved for messages actually directed to the local login. A public message shown through PM Catching does not trigger the simple PM sound, CW callsign output or phonetic callsign output.

Messages added through QSO Monitoring likewise remain silent. Their appearance in the PM table is a visual aid, not evidence that the local station received a new private message.

In practical terms: the PM table brings the relevant lines together, the age colours show what is fresh, and selecting a row prepares the likely reply target. The operator still decides whether the message was really intended for the station and whether it needs an answer.

[Read the PM Catching recognition and its limits in the manual.](/manual/en/features/#pm-catching-from-v11)

[Read the exact six age levels.](/manual/en/features/#coloured-pm-rows-from-v125)

[Open the PM-window controls and selection behaviour.](/manual/en/user-interface/#pm-window-top-left)

[Read which incoming messages produce audio notifications.](/manual/en/configuration/#notification-settings)
