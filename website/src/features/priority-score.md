---
title: Priority Score System
icon: 🎯
category: Contest Workflow
since: "1.40"
summary: Calculate one priority per base callsign, exclude known unusable band combinations and order the remaining active stations by band, distance, direction, activity, AirScout, reply and sked context.
description: KST4Contest combines information which would otherwise have to be evaluated separately and uses the resulting score to identify stations worth examining next.
tagsList:
  - ON4KST
  - Priority Score
  - priority candidates
  - band opportunity
  - VHF
  - UHF
  - SHF
  - contest workflow
related:
  - qrg-detection
  - band-recognition
  - timeline
  - airscout
  - sked-reminder
  - dual-chat
---

## What question does the score answer?

An ON4KST user list initially shows which stations are logged in. During a contest, that is only the beginning of the decision.

For every possible contact, the operator would otherwise have to combine questions such as:

- Has the station already been worked?
- Is another common band still available?
- Does the current antenna direction fit?
- Is the station active in the chat?
- Is an aircraft-scatter opportunity approaching?
- Has the station replied quickly before?
- Is there an agreed sked?

KST4Contest combines this existing context into one relative Priority Score.

The score answers:

> Which active station should I examine next?

It does not answer whether a QSO will succeed.

## Eligibility comes before weighting

Before adding activity, AirScout or sked points, KST4Contest checks whether a usable band opportunity is known.

All active callsign and category variants of the same base callsign are evaluated together.

The band calculation uses:

1. the bands enabled under **My station uses ...**;
2. QRGs detected for the remote station during the previous 30 minutes;
3. explicit band designators in the name fields of its active chat entries;
4. stored per-band Worked marks; and
5. manually assigned NOT-QRV marks.

A manual NOT-QRV mark takes precedence over an automatically detected QRG or band designator.

If bands are known for the remote station but none of them is both enabled locally and still available, the station receives a score of `0`.

The same applies when every known common band has already been worked.

## Unknown is not the same as impossible

A station is not excluded merely because no band information has been detected.

Missing information means that KST4Contest cannot currently prove which band is available. It does not prove that no common band exists.

Such a station remains eligible unless every locally enabled band has explicitly been marked NOT QRV.

Passing this eligibility check does not guarantee a place in the priority list. It only allows the normal weighting to continue. If the final calculation still results in `0`, the station remains outside the priority list.

This distinction is important. A band incompatibility is a hard reason for exclusion. A low final score is the result of the complete operating context.

## How is the score calculated?

The current calculation starts with a base value of `100`.

The following contributions are then applied in order.

### Worked and band information

| Condition | Current effect |
|---|---:|
| No supported band has been worked | `+200` |
| At least one supported band has already been worked | `−150` |
| Each additional compatible offered band after the first | `+80` |
| Optional band-upgrade Priority Boost | `+180` |

The additional band count is based on offered bands which are enabled locally and not marked NOT QRV. Worked bands remain part of that general multi-band context, while at least one unworked common band is required for a known band-upgrade opportunity.

The optional Priority Boost applies only when:

- the station has already been worked on at least one band;
- another common and unworked band remains; and
- **Priority boost for band-upgrade cases** is enabled.

Without this option, an open band makes the station eligible but does not by itself guarantee a positive final score.

## Distance

When a valid QRB is available, the subtotal calculated so far is multiplied according to the distance:

| Distance | Multiplier |
|---|---:|
| Below `200 km` | `× 0.7` |
| Between `200 km` and the configured maximum QRB | `× 1.15` |
| Beyond the configured maximum QRB | `× 0.3` |

This does not mean that short or very distant contacts are impossible. It reflects the intended contest workflow: stations inside the configured working range, but not already in the very short-distance range, receive more attention.

If no QRB is available, the distance multiplier is omitted.

## AirScout information

When AirScout reports at least one reachable aircraft, the station receives `+200`.

The arrival time of the next aircraft adds:

| Expected arrival | Additional score |
|---|---:|
| `0 minutes` | `+120` |
| `1 minute` | `+60` |
| `2 minutes` | `+30` |

The AirScout potential percentage is used elsewhere for display and timeline selection. It is not converted directly into the Priority Score.

A displayed aircraft therefore indicates a time-dependent opportunity. It is not a success probability.

## Antenna direction

If the QTF to the remote station lies inside half of the configured antenna beamwidth around the current local QTF, the station receives between `+80` and `+200`.

The value rises towards the centre of the antenna direction:

- approximately `+80` at the edge of the configured beam;
- up to `+200` when both directions match.

The calculation uses the local antenna direction. It does not know the actual antenna direction of the remote station.

## Current chat activity

Recent incoming messages raise the priority:

| Activity | Score |
|---|---:|
| Last incoming line less than 60 seconds ago | `+120` |
| Last incoming line less than three minutes ago | `+60` |

Several incoming lines inside the configured momentum window add:

| Lines inside the window | Score |
|---|---:|
| At least 2 | `+60` |
| At least 4 | `+110` |
| At least 6 | `+160` |

The default momentum window is 180 seconds.

This factor identifies stations which are currently active in the chat. It does not prove that they are available for a contact with the local station.

## Positive text signals

Configured expressions such as `QRV`, `READY`, `RGR`, `OK`, `TNX` or `LSN` are treated as positive activity hints.

A detected expression remains effective for five minutes and adds `+120`.

The detector uses case-insensitive literal substring matching. It does not interpret sentence meaning or negation. A line containing `not QRV` still contains the configured substring `QRV`.

Positive text signals must therefore remain a comparatively weak hint inside the larger calculation. They are useful for recognising activity, but they are not a natural-language interpretation of the message.

## Reply behaviour

When the operator sends:

```text
/cq CALLSIGN ...
```

KST4Contest starts measuring the response time for the corresponding base callsign.

A smoothed average response time adds:

| Average response time | Score |
|---|---:|
| Below one minute | `+80` |
| Below three minutes | `+40` |

Any subsequently received public or private line from the same base callsign ends the pending measurement.

KST4Contest cannot determine whether that line was really an answer to the local operator. The value is therefore an operational approximation, not a statistically reliable reply rate.

If no line is received before the configured timeout, a No-Reply strike is added. The default timeout is 13 minutes.

Each strike divides the current subtotal by:

```text
1 + (number of strikes × 0.6)
```

No-Reply strikes accumulate during the current program run.

## Scheduled contacts

A stored sked is an operating commitment and receives a deliberately strong time-dependent contribution.

| Time relative to the sked | Effect |
|---|---:|
| More than 15 minutes before | `+40` |
| Final 15 minutes | Increasing contribution |
| Three minutes before until one minute after | `+5000` |

The strong contribution around the scheduled time is intentional. An agreed contact should not disappear from the list merely because another station happens to be very active in the chat.

The sked contribution belongs to the normalised base callsign. A sked created for `9A0BB-23` therefore also raises the common score shown for other active `9A0BB` variants.

The actual reminder destination remains the complete selected callsign in its original chat category.

## Manual Sked fail

**Sked fail** represents an explicit operator assessment that the attempted contact or path has failed.

The complete final score, including an imminent sked contribution, is multiplied by `0.15`.

Applying this penalty at the end is important. Otherwise the later `+5000` sked contribution would almost completely cancel the failure mark and leave the station at the top of the list.

The mark:

- applies to the normalised base callsign;
- affects all active suffix and category variants;
- remains active for the current program run; and
- can be removed with **Reset fail**.

The sked itself remains stored. Only its effect on the final operating priority is overruled.

## One station, several active logins

Calls such as:

```text
9A0BB-2
9A0BB-70
9A0BB-23
9A0BB-13
```

remain separate message targets.

For scoring, they are grouped through the base callsign:

```text
9A0BB
```

KST4Contest calculates one score for that base callsign and projects it to every active variant.

This prevents one physical station from occupying several positions in the priority list merely because it uses several suffixes or chat categories.

The individual rows remain separate because outgoing messages still require the complete callsign and the correct category.

## Which login is shown in the priority list?

KST4Contest selects one active login as the representative of the base callsign.

The selection order is:

1. a variant in the chat category from which the most recent inbound activity was received;
2. the most recently active variant inside that category;
3. otherwise, the most recently active variant across all categories.

The resulting Priority Candidate retains:

- the normalised base callsign;
- the complete displayed callsign; and
- the preferred chat category.

Selecting the candidate therefore leads back to an actual active login rather than an abstract database key.

## Display and operation

![Priority Score, compact candidate list and Further Info controls](/manual/assets/priority_score_overview.png)

The score appears in three places:

- the numerically sortable **Score** column in the user list;
- the **Further Info** section of the selected station; and
- the compact priority bar between the user list and Further Info.

The compact bar shows the two highest-ranked candidates.

The **more** button opens a separate list containing up to 15 candidates. Double-click a candidate to select it.

Only finite scores above `0` are included in the priority list. Stations with a score of `0` remain visible in the normal user list, where Worked, band and NOT-QRV information can still be examined.

The priority list is calculated from the active station model and is independent of the current table filters. A candidate may therefore appear in the priority list while its row is hidden by **New bands**, QRB, QTF or another user-list filter.

Selecting such a candidate still updates Further Info and prepares the directed message. The active filter remains unchanged.

> Correct band eligibility, base-callsign grouping, separate suffix routing, the final Sked-fail override and selection of filtered candidates are included from v1.42 onwards.

## When is the score updated?

New calculations are requested after relevant events such as:

- incoming chat messages;
- AirScout updates;
- newly created skeds;
- log and Worked updates;
- changes to NOT-QRV marks;
- **Sked fail** and **Reset fail**; and
- outgoing `/cq` messages.

The background scheduler checks the score every three seconds. Time-dependent information is also recalculated periodically when no new event arrives.

A short delay between an event and the visible new order is therefore normal. The calculation is deliberately coalesced so that several closely spaced events do not create a separate JavaFX update for every station.

## What does the score not tell you?

The Priority Score is not:

- a probability of completing the QSO;
- a signal-strength estimate;
- a propagation forecast;
- a confirmed antenna direction of the remote station; or
- proof that the station is currently sitting at the radio.

A score of `800` is not twice as promising as a score of `400`. The values establish an order inside the current calculation; they do not form a physical scale.

Input data may also be incomplete or outdated:

- a detected QRG can be up to 30 minutes old;
- a name field may describe several bands without stating the current one;
- a chat line may be unrelated to the local station;
- an AirScout result depends on its external aircraft data and configuration; and
- an agreed sked may no longer reflect the actual situation.

In practical terms: the score keeps the available information from having to be reconstructed mentally for every station. The decision still belongs to the operator.

[Read the complete Priority Score description in the manual.](/manual/en/features/#priority-score-and-priority-list-from-v140)

[Open the related station and scoring settings.](/manual/en/configuration/#band-upgrade-hint-after-a-log-entry)

[Read how active suffix variants remain separate message targets.](/features/dual-chat/)

[Read how Priority Candidates are used on the timeline.](/features/timeline/)
