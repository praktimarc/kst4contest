---
title: Sked Reminder
icon: 🔔
category: Sked Management
since: "1.40"
summary: Create a timed contact, raise its priority, show it on the timeline and optionally send reminder PMs before the agreed time.
description: KST4Contest keeps scheduled contacts in the active workflow, increases their priority as the agreed time approaches and can remind both operators.
tagsList:
  - sked
  - ON4KST
  - contest reminder
  - Win-Test
related:
  - priority-score
  - airscout
  - timeline
---

## Why store a sked inside the chat client?

A contact agreed for five or ten minutes later has to compete with incoming messages, logging, antenna changes and other stations asking for attention. Remembering the time is only part of the problem. The station must also become visible again when the appointment approaches.

KST4Contest therefore treats a sked as an active operating task rather than a simple alarm.

## What happens when a sked is created?

Select the station, the remaining time and one of the locally enabled bands. The mode can be set to `SSB` or `CW` for a possible Win-Test handover.

After pressing **Create sked**, KST4Contest:

1. stores the sked internally,
2. raises the station's Priority Score as the scheduled time approaches,
3. adds the contact to the AP and sked timeline, and
4. optionally schedules private reminder messages.

![Sked controls in the Further Info section](/manual/assets/sked_controls.png)

The internal sked does not depend on Win-Test. If no logger is connected or the network handover fails, the sked remains available in KST4Contest.

## Reminding the remote station

Reminder PMs are optional. The available patterns are:

- two and one minute before the sked,
- five, two and one minute before the sked, or
- ten, five, two and one minute before the sked.

Messages are sent to the complete KST callsign in the selected chat category. A band-specific login such as `CALLSIGN-70` therefore remains a separate message target instead of being silently reduced to the base callsign.

The local operator receives a visual **SKED** indication and, if simple notification sounds are enabled, an acoustic reminder.

## Win-Test handover

When the Win-Test network listener is enabled, KST4Contest also attempts to send the sked to Win-Test.

The frequency is not guessed. KST4Contest first looks for a recent QRG of the remote station on the selected band. If none is available, it checks whether the local QRG of the selected chat category belongs to that band. Without a matching frequency, the Win-Test handover is omitted while the internal sked remains intact.

KST-specific suffixes such as `-2`, `-70` or `-144` are removed from the callsign passed to the log. Portable components such as `/P` and `/M` are preserved.

> Band-aware QRG validation, explicit `SSB`/`CW` selection and the corrected handling of KST suffixes are included from v1.42 onwards.

![Sked handed over from KST4Contest to Win-Test](/manual/assets/wintest_sked_handover.png)

## What the reminder cannot guarantee

Skeds and reminder schedules are stored in memory. They must be recreated after restarting KST4Contest.

The proposed band is derived from recent chat and station-name information. This is useful context, not proof that the station is still operating on the same QRG. Check the band, time and mode before creating the sked.

In plain terms: the function makes a scheduled contact considerably harder to overlook. It cannot prevent every missed sked.

[Read the complete sked handling and its limitations in the manual.](/manual/en/features/#skeds-and-sked-reminders)