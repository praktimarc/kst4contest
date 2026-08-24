# Review checklist

## Scope

- Is every changed file necessary?
- Did unrelated formatting or refactoring slip in?
- Were user-authored local changes preserved?

## Architecture/threading

- Is canonical state owned outside JavaFX controls/lists?
- Does worker code avoid `ObservableList` access?
- Are UI mutations routed to the FX thread?
- Are parser/I/O/domain/UI responsibilities clearer or at least not more coupled?

## Domain

- Full callsign + category identity preserved?
- Base-call matching restricted to worked/monitoring or another explicitly approved feature?
- Unknown category/band values safe?
- NOT-QRV precedence preserved?
- Missing QRB/QTF remains unavailable rather than zero?

## Protocols

- ON4KST framing unchanged unless approved?
- CR/LF exact?
- UCX `contactreplace` compatibility preserved?
- Frequency strings verified rather than guessed?
- PSTRotator/AirScout transport/API assumptions checked against current code?
- Malformed input contained?

## UI

- Selection/focus/zoom/sorting preserved?
- `/cq callsign` prefill behaviour preserved where relevant?
- Main send-category fallback preserved where relevant?
- map WebView workaround preserved?
- null values displayed safely?

## Tests/build

- Focused regression test added or updated?
- Test summary checked?
- Ignored failures explicitly reported?
- PMD/SpotBugs output considered?
- packaging/module-list checks considered if modules changed?

## Documentation

- DE and EN manual both checked?
- website/README/changelog checked if user-visible?
- screenshot impact reported?
- writing style applied?
- no undocumented implementation or documented-but-unimplemented behaviour?

## Git/release

- No version bump unless requested?
- No generated release/update-feed changes by accident?
- No staging/commit/push without explicit authorization?
