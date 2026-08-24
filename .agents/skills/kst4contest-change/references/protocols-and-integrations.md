# Protocols and external integrations

This file records stable rules plus historical context. For exact current wire formats, ports and frequency strings, inspect the current code and authoritative upstream documentation.

## ON4KST

KST4Contest depends on long-lived server communication where malformed commands or framing can lead to disconnects.

Rules:

- preserve exact protocol framing;
- treat CR/LF changes as protocol changes, not formatting cleanup;
- do not append extra line terminators without verification;
- detect actual socket/server disconnects reliably;
- initial connection failure must not create an uncontrolled infinite loop;
- reconnect logic must tolerate unstable Internet access;
- the UI should make disconnected state clearly visible where implemented.

If Windows-specific behaviour is suspected, do not assume Win10/Win11 line-ending semantics explain it without reproducing or tracing the bytes.

## UCXLog / DXLog UDP XML

`contactreplace` must be handled equivalently to `contactinfo` for whole-log broadcasts where applicable.

Historical raw-packet XML start detection included:

```text
<?xml
<contactinfo
<contactreplace
<RadioInfo
```

DOM handling also included a fallback to `contactreplace`.

Before changing this path, inspect the current parser because the code may have been refactored since this behaviour was introduced.

Preferred layering:

```text
UDP receiver -> parser -> DTO -> service/domain/DB -> controller -> UI
```

## Win-Test

Historical integration uses UDP port 8721 for Win-Test information.

Do not hardcode this fact into unrelated logic. Verify current configuration before changing listener setup or band mapping.

Changes involving 50/70 MHz, worked state or frequency mapping must be consistent with other logging inputs.

## AirScout

KST4Contest integrates with AirScout path/airplane-scatter information.

Stable principles:

- propagation/path queries must reflect the current relevant frequency/band;
- do not fall back to 144 MHz merely because older code did;
- unsupported chat categories must fail safely;
- frequency-string formatting is an external API contract and must be checked, not guessed.

Historical work included a temporary 430 MHz approximation for ambiguous higher-band handling. Treat that as historical context, not a permanent invariant. Inspect the current implementation before using or changing it.

## PSTRotator

The integration has evolved.

Historical project notes mention more than one control approach, and recent work included UDP control/feedback behaviour around a configurable control port and feedback on the next port, including SPID movement retry logic.

Therefore:

- inspect the current implementation before assuming TCP vs UDP;
- inspect current settings/defaults;
- do not copy an old port/transport assumption into new code;
- preserve asynchronous JavaFX-safe handling;
- preserve any verified retry sequence only if it still exists in current code/tests.

If current code and historical notes conflict, ask Marc after showing the conflict.

## DXCluster

DXCluster is integrated into the contest workflow.

Preserve:
- copyable/usable cluster lines;
- correct sender/receiver locator semantics;
- safe handling of missing locator data.

A historical bug copied sender and receiver locators as equal; do not reintroduce that behaviour.

## Protocol-wide error handling

External data is not trusted to be complete.

Rules:

- validate before dereferencing;
- unknown categories/bands/tags should degrade safely;
- malformed packets must not kill long-running receiver/management threads;
- logging should make the rejected input diagnosable without flooding normal operation.
