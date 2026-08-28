---
title: Dual Chat Categories
icon: 💬
category: ON4KST Chat
since: "1.26"
summary: Monitor two ON4KST categories in one interface while preserving the complete callsign and category required for correct message routing.
description: KST4Contest uses one authenticated ON4KST TCP session for two chat categories, keeps their message contexts separate and shares station-related Worked, band and priority information through the normalised base callsign.
tagsList:
  - ON4KST
  - dual chat
  - multi-channel login
  - callsign suffix
  - VHF
  - UHF
  - SHF
  - microwave contest
related:
  - private-message-handling
  - trx-qrg-synchronisation
  - band-recognition
  - priority-score
  - log-sync
  - airscout
---

## Why use two chat categories?

VHF, UHF and microwave activity is not always concentrated in one ON4KST category. An operator may need to follow one category for the lower bands and another for microwave operation.

Opening two unrelated chat clients would show both message streams, but every comparison between them would remain manual. Worked status, band information, active stations and sked context would still be distributed across separate windows.

KST4Contest therefore signs in once and adds a second chat category through ON4KST Single Sign-on when required. Both categories use the same TCP session and are processed in one operating context.

![Primary and secondary chat settings](/manual/assets/client_settings_window_station.png)

## One connection, two category contexts

KST4Contest authenticates the ON4KST TCP session with one local login callsign and one password. The session uses one common locator for both categories; the locator is not an authentication credential. The primary category is part of the initial login. If a second category is enabled, the client adds it to the same session through Single Sign-on. It does not open another TCP connection or perform a second local login.

The primary and secondary categories still retain their own:

- ON4KST chat category;
- visible category-specific **Name in Chat** field;
- public message stream;
- message context and destination category for outgoing messages;
- local QRG;
- beacon enable setting and message template; and
- active chat-member entries.

The **Name in Chat 2** field configures only the visible name field for the second category. It is neither a second login callsign nor a message destination. Both categories continue to use the one local login callsign and the common locator of the TCP session.

One connection therefore does not mean one combined message stream. Messages must still be sent in the category in which the intended destination is active.

## What identifies an active chat member?

For remote chat participants, an active member is identified by:

1. the complete visible callsign variant, including its suffix; and
2. the ON4KST chat category.

Both parts are required.

Consider the following active entries:

| Complete visible callsign variant | Chat category | Active entity |
|---|---:|---|
| `9A0BB-2` | 1 | separate |
| `9A0BB-70` | 1 | separate |
| `9A0BB-23` | 2 | separate |
| `9A0BB-13` | 2 | separate |

All four entries belong to the same base callsign, but they are four different chat-member identities. Joining, updating or leaving the chat affects only the corresponding complete callsign in the corresponding category.

This is particularly important for `9A0BB-2` and `9A0BB-70`: because both use the same category, the category alone cannot distinguish them.

> Correct separation of several suffix variants within the same category is included from v1.42 onwards and fixes [Issue #73](https://github.com/praktimarc/kst4contest/issues/73).

## How are messages routed?

When a row is selected, KST4Contest retains both the complete callsign and its category. A private message is addressed to that complete callsign and sent in the corresponding category within the shared session. Incoming private messages for the local station are addressed to the local login callsign, not to a category-specific **Name in Chat** field.

Selecting `9A0BB-70` in category 1 therefore creates a message for `9A0BB-70` in category 1. It is not silently reduced to `9A0BB`, and it is not sent through category 2 merely because another variant is active there.

The same distinction is used for:

- incoming private messages;
- outgoing message echoes;
- public messages addressed to the local station;
- station selection from the user list;
- sked reminder messages; and
- leaving or updating an active chat entry.

The different variants do not have to be able to send messages to each other. That is not the purpose of the function. They must remain correctly reachable by other stations and receive the messages addressed to their respective chat-member identity.

## What is shared through the base callsign?

Some information describes a remote radio station rather than one temporary chat-member identity. This information is aggregated through the normalised base callsign.

For the examples above, the common base callsign is `9A0BB`.

The shared station context includes:

- global Worked status;
- Worked status per band;
- manually assigned NOT-QRV marks;
- known band opportunities derived from the active variants;
- the Priority Score; and
- stored contest state in the internal database.

A QSO with `9A0BB-70` therefore also marks the corresponding base callsign as worked for the other active suffix variants. It would be misleading to present `9A0BB-2` as a completely new station immediately afterwards.

Per-band information is still retained. Working the station on 70 MHz does not mark it as worked on 23 cm. The common base callsign links the variants; the band remains a separate dimension.

## Why are band hints combined?

A remote station may use complete visible callsign variants to describe the available bands:

- `9A0BB-2`
- `9A0BB-70`
- `9A0BB-23`
- `9A0BB-13`

KST4Contest can evaluate these active variants together when deriving possible bands. Recent QRG information and band designators in the name fields can contribute as well.

This allows the user list, the `a` and `B+` indicators, the **New bands** filter and the automatic propagation-frequency selection to use information which may be distributed across the two chat categories.

A manual NOT-QRV mark still takes precedence. An automatically detected suffix or name is evidence of possible activity, not permission to ignore an explicit correction.

## Why is the Priority Score not duplicated?

The Priority Score represents the operating priority of the remote station, not the number of chat-member identities it happens to use.

KST4Contest therefore calculates the score for the normalised base callsign and projects it to the active variants. Several suffixes do not create several independent priority candidates merely by being logged in more than once.

The individual rows remain selectable because the correct message destination still matters.

In plain terms: one station should not occupy half the priority list, but the operator must still be able to contact the correct chat-member identity.

## Categories are context, not proof of a band

A chat category describes the general operating area of that chat. It does not prove that every station in the category is currently QRV on every associated band.

KST4Contest therefore uses stronger station-specific information where available:

1. a recently detected QRG;
2. an explicit band designator in the station name or suffix;
3. the locally enabled bands;
4. stored Worked and NOT-QRV information; and
5. only then a supported category fallback where the function requires one.

Categories unrelated to the supported VHF, UHF, microwave or EME workflows are ignored by propagation functions which cannot derive a meaningful result from them. They must not cause a fallback to an arbitrary band.

## What remains separate?

Sharing the station context does not merge everything associated with the base callsign.

The following remain tied to the individual remote chat member or message:

- complete message destination;
- chat category;
- public message stream;
- visible callsign suffix;
- join and leave state;
- the specific row selected by the operator; and
- message history associated with that chat member and category.

This distinction is the basis of the dual-chat implementation: operational information may be shared where it describes the same remote radio station, while communication remains attached to the complete ON4KST callsign and category.

## Practical limitations

The shared base-callsign model assumes that complete visible callsign variants separated by `-` belong to the same underlying amateur-radio station. That is appropriate for variants such as `-2`, `-70`, `-144` or `-432`.

KST4Contest cannot determine whether two operators behind those chat entries are using one station, several independently operated stations or a distributed contest setup. Worked and band information is therefore shared at callsign level, while the operator remains responsible for selecting the correct chat destination.

[Read how both chat categories are configured.](/manual/en/configuration/#login-and-chat-categories)

[Read how Worked and band information is derived.](/manual/en/features/#worked-callsigns-new-bands-and-new-grid-squares)

[Read how suffixed callsigns are handled in skeds.](/features/sked-reminder/)
