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
  - trx-qrg-synchronisation
  - station-filters
  - band-recognition
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

The Simplelogfile interpreter reads the selected text file once per minute using a fixed callsign pattern. Matches are normalised to the base callsign and set the global Worked status for all active variants. A callsign match does not provide enough information to infer a reliable band or grid square.

If the file does not exist, KST4Contest creates it and first asks you to check whether Simplelogfile is needed or whether the logging application provides a supported network interface. The notice then displays the path together with the initial setup and contest checks. The file itself is the durable source. Its Worked marks are not stored in SQLite, are not removed automatically during the running session and are not reset automatically for a new contest.

The general UDP listener processes packets from UCXLog, N1MM+, QARTest and DXLog.net. Where the packet contains band and locator data, KST4Contest also updates the per-band Worked state and worked grid square.

![Log synchronisation settings](/manual/assets/client_settings_window_logsync.png)

## Native Win-Test integration

Win-Test uses a separate listener for its native network protocol. KST4Contest resolves the Win-Test band ID, including 50 and 70 MHz, and stores the resulting Worked information in the same internal database.

QSOs logged before KST4Contest was started are recovered from Win-Test as well. As soon as a Win-Test station is detected, the missing part of its log is requested over the native protocol, so stations already worked are marked as worked even when the client joins the contest late. The recovery needs no setting, keeps running to close gaps caused by lost packets, and covers every log in a multi-station network.

STATUS packets can also update the local QRG when QRG synchronisation is enabled and valid packets actually arrive. Enabling the source alone does not supply a frequency. In multi-operator networks, a station-name filter prevents STATUS packets from another operating position from replacing the frequency of the intended radio.

Win-Test can additionally receive skeds created in KST4Contest. The handover only takes place when a QRG matching the selected band can be determined. No fixed fallback frequency is inserted merely to make the packet technically valid.

> The band-aware sked handover and explicit `SSB`/`CW` selection are included from v1.42 onwards.

## Stored state and limitations

Worked, NOT-QRV and worked-grid information received through the network interfaces, together with manual marks, is stored in the internal SQLite database and restored after a restart. Contest-related records expire automatically after three days.

Simplelogfile marks follow a different data flow: they remain runtime state derived from the selected file. The file is read again after a restart and has no automatic contest reset. A database reset does not change or empty it. Callsigns contained in the file are marked as worked again during the next evaluation within one minute.

KST4Contest can only use the fields supplied by the selected interface. Missing band or locator data is not reconstructed from guesswork. This makes the result less complete in some cases, but also avoids turning an incomplete log packet into incorrect Worked information.

[Read the complete log synchronisation setup in the manual.](/manual/en/log-sync/)
