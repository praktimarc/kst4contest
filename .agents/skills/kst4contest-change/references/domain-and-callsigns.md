# Domain, callsigns and bands

## Chat-member identity

Full callsign variants can be distinct chat identities.

Examples:

```text
DN9APW
DN9APW-2
DN9APW-70
```

Do not globally strip suffixes when identifying chat members.

Category is also part of identity. A practical key is conceptually equivalent to:

```text
FULL_CALLSIGN|CATEGORY
```

Do not allow messages from one category to attach to a same-looking member in another category.

## Base-call operations

Some features intentionally operate on the base callsign.

Confirmed examples:

### Worked state

Worked status is shared across suffix variants of the same base call.

If the base station has been worked on the relevant basis, variants such as `CALL-2`, `CALL-70`, `CALL-144`, `CALL-432` should not become independent worked identities merely because of the suffix.

### Monitoring

Monitoring a station entered as `DN9APW-2` or `DN9APW-70` should monitor the base call `DN9APW`.

This is intentional: a user monitoring another station's skeds should not have to create one monitor entry per SSID.

Do not extend base-call matching to unrelated features without approval.

## Suffix semantics

Do not assume a suffix always means a band or category.

Historical examples have shown the same base calls with different suffix conventions in different chat categories.

Therefore:

- preserve exact full-call identity where needed;
- normalize only for explicitly approved base-call features;
- never infer missing band/category semantics from the suffix alone.

## Categories

KST4Contest's central VHF/UHF usage focuses on ON4KST categories 2 and 3.

However, other categories can occur.

Rules:

- unsupported/uninteresting categories must be ignored or handled safely;
- they must not produce index, switch or null errors;
- do not let their values contaminate category-2/category-3 band logic.

## Band availability

Band activity can be derived from several signals including name/text parsing and explicit/manual information.

Confirmed invariant:

`NOT-QRV` overrides positive availability indications.

Known-active-band and `B+` handling should use one consistent interpretation across the program.

Do not implement separate slightly different parsers in multiple UI/features if a shared existing mechanism is available.

## Current QRG

Features that depend on propagation/band/frequency should use the current relevant QRG or the approved band calculation.

Never silently reintroduce a universal hardcoded 144 MHz fallback.

When a frequency is ambiguous and no approved fallback exists, ask rather than guessing.
