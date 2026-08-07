---
title: Log Synchronisation
icon: 🔄
category: Logger Integration
since: "1.31"
summary: Import callsign, band, locator and QRG information from supported contest loggers at the level provided by each interface.
description: KST4Contest connects chat activity with current log state through file-based evaluation, general QSO UDP packets and the native Win-Test network protocol.
tagsList:
  - Win-Test
  - UCXLog
  - N1MM+
  - QARTest
  - DXLog.net
  - contest logger
related:
  - priority-score
  - dual-chat
  - sked-reminder
---

## Why connect the logger?

The chat may still show a station as an interesting candidate after the QSO has already been logged. Without synchronisation, Worked filters, band columns and priority calculations would continue to use an outdated contest state.

KST4Contest therefore imports the information provided by the logging application and applies it to the active chat entries of the corresponding base callsign.

## Three interfaces with different levels of detail

The available information depends on the interface:

| Interface | Callsign | Band | Locator |
|---|---:|---:|---:|
| Simplelogfile interpreter | yes | no | no |
| General QSO UDP listener | yes | when included | when included |
| Win-Test network listener | yes | yes | when included |

The Simplelogfile interpreter is broadly compatible but can only identify worked callsigns. A callsign match in a file does not provide enough information to infer a reliable band or grid square.

The general UDP listener processes packets from UCXLog, N1MM+, QARTest and DXLog.net. Where the packet contains band and locator data, KST4Contest also updates the per-band Worked state and worked grid square.

![Log synchronisation settings](/manual/assets/client_settings_window_logsync.png)

## Native Win-Test integration

Win-Test uses a separate listener for its native network protocol. KST4Contest resolves the Win-Test band ID, including 50 and 70 MHz, and stores the resulting Worked information in the same internal database.

STATUS packets can also update the local QRG. In multi-operator networks, a station-name filter prevents STATUS packets from another operating position from replacing the frequency of the intended radio.

Win-Test can additionally receive skeds created in KST4Contest. The handover only takes place when a QRG matching the selected band can be determined. No fixed fallback frequency is inserted merely to make the packet technically valid.

> The band-aware sked handover and explicit `SSB`/`CW` selection are included in Nightly / v1.42.

## Stored state and limitations

Worked, NOT-QRV and worked-grid information is stored in the internal SQLite database and restored after a restart. Contest-related records expire automatically after three days.

KST4Contest can only use the fields supplied by the selected interface. Missing band or locator data is not reconstructed from guesswork. This makes the result less complete in some cases, but also avoids turning an incomplete log packet into incorrect Worked information.

[Read the complete log synchronisation setup in the manual.](/manual/en/log-sync/)