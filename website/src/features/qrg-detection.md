---
title: Automatic QRG Detection
icon: 📻
category: ON4KST Chat
since: "1.1"
summary: Recognise complete and relative QRG information in chat messages and retain the sender's recent band context before using a global fallback.
description: KST4Contest extracts usable frequencies from public and directed ON4KST messages and makes the result available to band, priority, sked, path and logger workflows.
tagsList:
  - QRG detection
  - frequency
  - relative QRG
  - band context
  - ON4KST
  - contest workflow
related:
  - global-message-views
  - trx-qrg-synchronisation
  - band-recognition
  - station-map
  - priority-score
  - sked-reminder
  - dx-cluster
---

## Why detect a frequency from chat text?

Frequencies in the ON4KST chat are rarely written in one consistent form. One station may send `432.088`, then shorten the next message to `.100`. Another may write `qrg 210` while a bare `210` in a different message means something else entirely.

KST4Contest evaluates both public and directed chat messages so that useful QRG information does not have to be copied manually into every later operating step.

Automatic QRG Detection has existed since version 1.1. The more precise sender-specific context evaluation described below is included from version 1.42 onwards.

## Which forms are recognised?

Complete frequencies provide their band directly. Examples include:

```text
144.210
432,088
144307
10368.100
10368100
```

The separator is optional for a complete frequency. For a digit-only value, KST4Contest treats the final three digits as the kHz part. This turns `144307` in a station name into `144.307 MHz` and `10368100` in a public or directed chat message into `10368.100 MHz`. The result still has to fall within one of the supported band ranges.

Relative forms omit the band and need additional context:

```text
.210
,088
qrg 210
freq is 210
on 210
210 MHz
```

A dot or comma marks a relative frequency explicitly. A three-digit value is accepted only when nearby text identifies it as a frequency.

## Which band is used for a relative QRG?

KST4Contest resolves the missing band in this order:

1. It first looks for a suitable band context detected for the same sender during the previous 30 minutes.
2. If several current bands are known, it uses the most recently updated plausible context.
3. Only when no suitable station context exists does it use the configured **Fallback band for relative QRG detection**.

For example, assume that the global fallback is 144 MHz. A station first sends `432.088` and then `.100` a few minutes later. The recent context belongs to the same sender, so the result is `432.100 MHz`, not `144.100 MHz`.

Another station without suitable recent context would use the global fallback for the same `.100` message.

## Why are bare three-digit numbers ignored?

Values such as:

```text
210
599
144
```

are deliberately not treated as QRGs without recognisable frequency context. Otherwise, a signal report of `599`, a band name or a QSO count could become a formally valid but operationally useless frequency.

Rejecting an ambiguous number is less convenient than guessing correctly. It is considerably more convenient than tuning to a frequency which existed only in the parser's imagination.

## Where is the detected QRG used?

The most recently detected frequency appears in the **QRG** column. The same information can also contribute to:

- [Band Recognition](/features/band-recognition/) and open band opportunities;
- the [Priority Score](/features/priority-score/);
- frequency selection for [skeds](/features/sked-reminder/);
- the selected frequency for [path analysis](/features/station-map/); and
- automatic or manually triggered [DX Cluster spots](/features/dx-cluster/).

If a QRG appears in the same message which creates a directional opportunity, it is processed before the optional spot check. The new frequency can therefore already be used for that spot.

## Text evidence is not current operating proof

The result is derived from message text. KST4Contest cannot prove that the sender is still operating on the detected frequency, that the value was not superseded outside the chat or that an apparently clear statement belongs to the intended operating context.

A detected QRG is useful working information. It is not a live measurement and not a guarantee that calling on that frequency will reach the station.

In practical terms: the function preserves frequency context which would otherwise be easy to lose. The operator still checks whether that context remains plausible before using it.

[Read the complete QRG Detection description in the manual.](/manual/en/features/#qrg-detection)

[Open the fallback-band configuration.](/manual/en/configuration/#fallback-band-for-relative-qrg-detection)
