# Settings and data context

Inspect `Config`/settings classes and current UI before using these names; this list records important settings/concepts encountered during prior work.

## Band / station settings

Important concepts have included:

- `MYQRGFirstCat`;
- `MYQRGSecondCat`;
- manual station band information;
- current/actual QTF;
- known-active bands;
- selected/current QRG.

Band-dependent features must use the correct category/station context.

## Antenna / path settings

Important concepts have included:

- `actualQTF`;
- `antennaBeamWidthDeg`;
- maximum QRB;
- AirScout/path-analysis settings.

Missing QRB/QTF must remain unknown, not numeric zero.

## UI settings

Persisted UI behaviour has included:

- dark mode;
- map/path-analysis visibility;
- filters/reachability controls;
- column visibility;
- divider/layout state where implemented.

Do not reset persisted user choices as an incidental effect of a feature change.

## Logging / worked persistence

Worked information is persisted and updated through multiple input paths.

Before changing one path, compare semantics across:

- DB load on startup;
- simple/manual log integration where present;
- UCXLog/DXLog;
- Win-Test;
- other current logging inputs.

The goal is one worked-state interpretation regardless of source.

## Chat/message automation

Configuration has included:

- beacon/autoanswer enablement;
- beacon defaults;
- message limits/timers;
- category-specific communication.

Do not duplicate timers or create per-feature scheduling that bypasses the shared safety model.

## Connection state

Connection-state UI must reflect actual ON4KST connection lifecycle.

Any new state enum/property should have a clear owner and thread boundary.

## Persistence rule

Do not change persisted keys/schema/semantics simply to make new code easier.

If a schema/key migration is required:

1. explain current and new format;
2. describe backward compatibility;
3. ask for approval before implementing.
