# Known edge cases and regression patterns

This file is a regression-awareness list. Reproduce/inspect current code before deciding a historical bug still exists.

## Incomplete ChatMember

Known failure pattern:

```text
Cannot invoke "java.lang.Double.intValue()" because
ChatMember.getQrb() is null
```

Lesson:

- QRB can be absent;
- UI/calculation code must not blindly unbox/convert;
- unavailable is not zero.

Apply the same reasoning to QTF and other server/fallback-derived fields.

## Callsign suffix collisions

A historical issue caused messages/identity problems between:

```text
DN9APW
DN9APW-2
```

The corrective model is not "strip all suffixes".

Instead:

- keep full-call chat identities distinct;
- include category;
- use base call only for explicitly base-call-wide features such as worked state/monitoring.

## Unsupported chat categories

The ON4KST ecosystem can expose categories outside the two central ones.

A parser/switch/filter must not throw because the category is irrelevant to KST4Contest.

Safe ignore/fallback beats fake band assignment.

## AirScout higher-band queries

A historical observation showed aircraft visible in AirScout while API results for a 432 MHz case were empty.

Potential causes included frequency-string formatting.

Lesson:

- verify the exact upstream contract;
- compare request produced by KST4Contest with a known-working request;
- do not "fix" by guessing a string format or falling back to an unrelated band.

## Second airplane-scatter result / missing aircraft

A known UI failure involved missing/partial airplane-scatter data and a `TextInputControl` range error (`start must be <= end`).

Lesson:

- empty/partial AP results must be validated before text-range highlighting/selection;
- second-result paths need the same null/range checks as primary results.

## Historic/unknown user message

A user/message record can refer to a callsign not present in the current member list.

Do not require current login membership to render or classify historic messages.

## UM3-style handling

Historical message handling included cases that should be ignored safely if the user is not in the chat/member state.

Lesson:

- external message types must tolerate missing member references.

## CR/LF and disconnect suspicion

Do not treat line endings as harmless text formatting in socket code.

When diagnosing a disconnect:

- inspect transmitted bytes;
- inspect server response/EOF;
- compare Windows versions only after proving the application sends different bytes;
- avoid duplicated LF/CRLF terminators.

## Filter reset

A reset button that clears control values but leaves predicates active is not a valid reset.

Verify final predicate composition, not only UI state.

## Map render flicker

Leaflet/WebView render fragmentation under Java 21 was mitigated by:

```text
window.L_DISABLE_3D = true
```

before Leaflet load.

Do not remove as "obsolete CSS cleanup" without a visual regression check.

## Network start/reconnect loop

Initial connection failure must not spin indefinitely or block controlled recovery.

Connection state must be based on actual I/O lifecycle rather than only `Socket.isConnected()`-style historical state.
