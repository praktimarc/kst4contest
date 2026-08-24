# Automated messaging, beacons and skeds

## General

Automated replies/beacons must be conservative because they interact with the live ON4KST service.

## Established safety behaviour

Historical confirmed rules include:

- beacon/autoanswer scheduling shares controlled timing rather than spawning uncontrolled independent timers;
- minimum interval has been tightened to avoid spam;
- automated text length is bounded;
- invalid or incomplete replies are rejected before transmission;
- a cooldown is not consumed unless a complete valid reply enters the TX queue;
- QRG/frequency requests take precedence where the current implementation defines that;
- automated-message markers are ignored to prevent response loops.

Recent logic used a two-minute cooldown keyed by complete callsign plus chat category and ignored the project's own automated-message marker.

Treat exact marker strings and timing constants as current-code facts to verify, not values to recreate from memory.

## Monitoring

Station monitoring is intentionally base-call-wide:

Entering a variant such as:

```text
DN9APW-2
DN9APW-70
```

monitors:

```text
DN9APW
```

This reduces manual configuration for sked monitoring.

Keep this separate from chat-member identity, which may require full suffix + category.
