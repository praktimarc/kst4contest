---
title: AP and Sked Timeline
icon: ⏱️
category: Contest Awareness
since: "1.40"
summary: Show upcoming aircraft-scatter candidates and scheduled contacts together on a 30-minute timeline.
description: The timeline relates AirScout opportunities, priority candidates, antenna direction and internal skeds to their expected time.
tagsList:
  - timeline
  - AP windows
  - aircraft scatter
  - sked
related:
  - airscout
  - priority-score
  - sked-reminder
---

## The useful station may only be useful for a minute

Aircraft-scatter opportunities are time-dependent. A candidate which matters in two minutes may be irrelevant now, while an agreed sked must remain visible even when no aircraft is currently available.

The timeline places both kinds of event into the same 30-minute view.

Events further in the future appear on the right. As their time approaches, they move left towards the present.

![AP candidates and skeds in the timeline](/manual/assets/sked_timeline.png)

## AP candidates and scheduled contacts remain distinct

AP candidates appear in the upper lanes. Up to four selected candidates can be shown for each arrival minute. Their colours represent the reflection potential reported by AirScout:

- magenta from 95%,
- red from 75%,
- yellow from 50%, and
- blue below 50%.

Skeds appear as diamonds in the lower lane. Their labels use the complete selected KST callsign so that band-specific or otherwise suffixed logins remain identifiable.

> Complete KST callsigns in sked labels are included in Nightly / v1.42.

## Antenna direction remains visible

A marker becomes more transparent when its QTF is clearly outside the current antenna direction. The callsign remains readable. Targets near the centre of the configured antenna beam receive an additional visual highlight.

Clicking an AP candidate selects the corresponding active chat member, including the callsign suffix and chat category.

## A timing aid, not a contact forecast

The timeline shows when an event is expected to matter. It does not guarantee that the frequency is clear, that the remote station is ready or that the calculated aircraft path will produce a workable signal.

AirScout data can change. So can the actual operating situation.

[Read the complete timeline behaviour in the manual.](/manual/en/features/#ap-and-sked-timeline)