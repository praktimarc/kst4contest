---
title: Priority Score System
icon: 🎯
category: Contest Workflow
since: "1.40"
summary: Rank active base callsigns using known band, direction, activity, AirScout, reply and sked information.
description: KST4Contest excludes stations with no known remaining band opportunity and orders the other active candidates using the context already available to the client.
tagsList:
  - ON4KST
  - VHF
  - UHF
  - SHF
  - contest
  - priority candidates
related:
  - timeline
  - airscout
  - sked-reminder
---

## The problem behind the score

An ON4KST user list shows who is logged in. During a contest, the more useful question is which station should be examined next.

Answering that question manually means repeatedly combining Worked status, possible bands, distance, antenna direction, recent activity, AP opportunities and scheduled contacts. This remains manageable with a short list. It becomes less reliable after several hours of contest operation, especially when multiple bands and chat categories are involved.

## How candidates are selected

KST4Contest first checks whether a known common and unworked band opportunity remains. Locally enabled bands, recent QRG detections, band designators in station names, Worked information and manual NOT-QRV marks are evaluated together.

A known incompatibility or an already completed set of possible bands produces a score of `0` and removes the station from the priority list. Missing band information alone does not. Unknown is not the same as impossible.

The remaining stations are ranked using several independent hints, including:

- distance and the configured maximum QRB,
- the current antenna direction and beamwidth,
- recent chat activity and reaction behaviour,
- available AirScout aircraft,
- open band opportunities, and
- the timing of scheduled contacts.

An imminent sked receives a deliberately strong boost. Failed attempts and unanswered calls reduce the score.

## One station, several chat logins

Suffixes and chat categories remain separate message targets. Worked, band, NOT-QRV and score information is shared through the normalised base callsign.

This prevents a station using several band-specific logins from occupying several positions in the priority list while still allowing KST4Contest to address the correct complete callsign in the correct category.

![Priority Score, compact candidate list and Further Info controls](/manual/assets/priority_score_overview.png)

## A decision aid, not a propagation forecast

The score is a relative operating priority. It is not a success probability, a signal estimate or a guarantee that the remote station is ready for a QSO.

Its value depends on the information available to KST4Contest, and that information may be incomplete, outdated or ambiguous. The final decision remains with the operator.

[Read the complete calculation and its limitations in the manual.](/manual/en/features/#priority-score-and-priority-list-from-v140)