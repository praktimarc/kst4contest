# KST4Contest Project Context

Last reviewed: 2026-09-03

This file is the durable technical project context for KST4Contest. It is not a user manual and not a replacement for the changelog. Current code, tests and authoritative external specifications remain the source of truth when this document is stale or ambiguous.

## Purpose

KST4Contest is a Java/JavaFX desktop client for ON4KST chat focused on VHF/UHF/microwave contest workflows. It combines chat handling with contest-oriented station prioritisation, sked/timeline workflows and integrations with logging, aircraft-scatter, rotor and DX-cluster tooling.

## Current Architecture

- Java 21 / JavaFX desktop application built with Maven.
- Main code is under `src/main/java/kst4contest/`.
- Responsibilities are separated across controller, service, logic, model, utility and view areas.
- Network/parser/service/controller/UI boundaries should remain explicit.
- Long-running network/message processing must tolerate malformed or incomplete external input without terminating processing threads.
- JavaFX `ObservableList` state is a UI projection, not the canonical worker-thread domain store.

## Important Invariants

### Chat identity

- For remote chat participants, the complete visible callsign plus category forms the chat-member identity.
- Full callsign variants can therefore be distinct chat-member identities.
- Base-call normalization is permitted only for explicitly base-call-wide functions.
- Worked status is shared across suffix variants of the same base call.
- Monitoring a variant such as `DN9APW-2` or `DN9APW-70` intentionally monitors the base call `DN9APW`.
- Suffixes must not be globally interpreted as a band/category/frequency.

### Band and availability semantics

- ON4KST categories 2 and 3 are the main operational categories, but unexpected category values must fail safely.
- `NOT-QRV` overrides positive inferred band-availability hints.
- Unknown/missing frequency, QRB, QTF or similar external data must remain unavailable rather than becoming a fabricated zero/default.
- Features that depend on frequency should use the current/actual QRG according to current implemented rules; do not silently revert to a fixed 144 MHz default.
- Complete digit-only frequencies use their final three digits as the kHz part and are accepted only when the resulting MHz value lies within a supported `Band` range. The same full-frequency parser is used for station names and public or directed chat messages. Relative QRG rules and bare three-digit context handling remain separate.

### JavaFX/threading

Conceptually:

```text
thread-safe canonical domain state
        |
        | projection on JavaFX Application Thread
        v
JavaFX ObservableList / UI state
```

`MessageBusManagementThread` must not directly iterate or mutate UI-bound JavaFX collections. UI-visible changes should cross the controller/UI boundary and run on the JavaFX Application Thread.

## Configuration and Layout Persistence

- The current `preferences.xml` configuration version is 7. Version 6 introduced optional managed leaf-column widths below `guiOptions`, identified by stable table and column IDs. Parent-column widths remain derived from their leaf columns.
- `GUIstationMapClusteringEnabled` is a layout preference below `guiOptions`. It defaults to `true`, is selectively autosaved and controls only screen-based clustering of nearby map markers. Missing or malformed values retain the enabled default for backward compatibility.
- Stored widths take precedence. Without a usable entry, a managed column is sized once when meaningful table data first becomes available. Message and similar free-text columns use a flexible initial width instead of following the longest value.
- Main-window and separate-monitor DXCluster/QSO tables use distinct layout IDs even though they share the underlying message stores.
- Window sizes and positions, relevant divider positions and managed column widths are selectively autosaved after a short debounce. A pending write is flushed during application shutdown.
- Selective layout writes update the XML already on disk, preserve unknown XML nodes and must not persist unconfirmed functional settings from the current UI. **Save Settings** remains the full settings writer and includes the current layout.
- Full and selective writes are synchronized and replace `preferences.xml` atomically. Missing, unknown or malformed width entries do not prevent loading and fall back to initial sizing.
- Older configuration files require no migration. Older KST4Contest versions can ignore the additional elements; a complete rewrite by such a version may discard column widths without invalidating the remaining file.

## External Interfaces

Treat current implementation/tests and authoritative upstream documentation as source of truth before modifying any interface.

Known integration areas include:

- ON4KST chat;
- AirScout;
- UCXLog / DXLog UDP XML (`contactinfo`, `contactreplace`);
- Win-Test UDP;
- PSTRotator TCP;
- DXCluster;
- local SQLite persistence.

CR/LF framing, XML framing, ports/transports, callsign normalization and frequency formatting are protocol behaviour and must not be changed as incidental cleanup.

### Local DX Cluster output

- Local spots use a fixed 75-character, DXSpider-compatible payload line followed by two BEL characters and CRLF.
- The DX callsign begins in column 27 and occupies up to 12 characters. The 30-character comment begins in column 40, and the five-character UTC time begins in column 71.
- Spotter and frequency padding is calculated dynamically so frequencies from 50 MHz through 24 GHz do not shift the following fields.
- Comments are padded or truncated to exactly 30 characters. Automatic AirScout comments retain the locator first and use the compact form `JO51HK AP 1m/100%;4m/75%`.
- A DX callsign longer than 12 characters is rejected and logged rather than truncated.
- Trigger conditions, QRG recognition and normalisation, login, keepalive, multi-client delivery and the local-only trust boundary remain separate from line formatting.

### Logging and Worked-state persistence

- The Simplelogfile interpreter reads the selected text file after connection startup and then once per minute using a fixed built-in callsign pattern.
- Simplelogfile callsigns are normalized to base callsigns and set only the global Worked state for every active variant. They do not create per-band Worked or grid-square state.
- Simplelogfile-derived Worked state is not persisted in SQLite. The selected file is the durable source and is read again in each application session.
- The interpreter only adds positive runtime marks. It does not remove existing marks during the current session and does not reset automatically when a new contest starts. A database reset does not modify the file; callsigns contained in it are marked as worked again during the next periodic evaluation.
- A missing selected file is created. Read, path and creation failures are contained so the periodic timer remains alive; successful creation triggers a one-time, non-blocking UI notice with the exact path and setup/contest checks.
- Network-derived and manually assigned Worked, NOT-QRV and worked-grid state continues to use SQLite with its established lifetime and reset behaviour.
- Each completed initial ON4KST user list loads one SQLite Worked/NOT-QRV snapshot. `ChatController` applies that snapshot by normalized base callsign to every new category and suffix variant before the completed category is published. The same event-driven path runs again after a reconnect; startup synchronization does not depend on a fixed-delay timer.
- Automatic QRG updates require both an enabled source and valid incoming `RadioInfo` or Win-Test `STATUS` data. Merely enabling a source does not provide or validate a current QRG.
- UCXLog-compatible QSO packets and Win-Test `ADDQSO` packets are converted into one validated external-QSO state. Logger-specific numeric, metre and centimetre values and Win-Test band IDs are normalised once; the resolved band is then the sole source for per-band Worked and worked-grid state.
- A missing or unknown logger band sets only the global Worked state. Worked-grid state requires both a recognised project band and a valid locator; no band or locator is inferred. Packets without a usable callsign are discarded without terminating the listener.
- External logger threads do not read or mutate the JavaFX user-list projection. `ChatController` applies global and per-band Worked state to every active variant of the base callsign on the JavaFX Application Thread before evaluating a band-upgrade notice.
- The established Win-Test handling for 24, 47 and 76 GHz remains unchanged. Their Worked flags are retained, while only frequencies represented by the project `Band` model can create worked-grid state.

### Terrain data providers

- The active terrain profile provider is Open-Meteo using Copernicus GLO-90 data.
- `OfflineDemImportService` only prepares a local directory and copies manually selected Copernicus GLO-30 GeoTIFF files into it. Importing files does not activate an offline provider or change the active calculation chain.

### ON4KST session and authentication

- One KST4Contest connection authenticates one ON4KST TCP session with one local login callsign and one password.
- The TCP session uses one common locator for both categories; the locator is not part of authentication.
- The primary category is part of the initial login. A distinct second category is added to the same session through ON4KST Single Sign-on; it must not create a second TCP connection or local login.
- **Name in Chat** is a visible category-specific name field, not a login callsign or message destination. Private messages to the local station are addressed to the local login callsign.
- The visible **Name in Chat** field, message context, QRG and beacon configuration remain category-specific.

### ON4KST session liveness

- After 90 seconds without inbound data, the application keeps the established empty CRLF heartbeat.
- At about 180 seconds of inbound idle time, the TCP session sends one `RDXQ|<main chat id>|` probe. The probe state belongs to the session, so a two-category session still sends only one probe per idle phase.
- Any subsequent inbound server frame confirms the probe. `DXQ` is accepted as the expected internal response and is not published as chat content.
- If no inbound frame arrives by about 210 seconds, the existing reconnect flow remains responsible for replacing the session.
- Probe diagnostics contain the session id, main category, opcode and timing only. They must not include credentials, complete server frames or normal chat messages.

## User Workflow / UI Invariants

- Contest operating speed and low-friction interaction are primary goals.
- Incidental code changes must not unexpectedly change selection, focus, sorting, tab state, map zoom or prefilled text.
- Map reset clears the selected target without changing zoom unless explicitly redesigned.
- **Group nearby stations** re-renders only the existing station-marker layer from JavaScript `stationData`. It must not reload the WebView, tiles or station data, request a new controller snapshot, or change zoom, viewport or selection.
- Base-callsign aggregation into one geographical marker happens before screen-based clustering. Disabling clustering displays each resulting positionable map station individually but never splits active variants of the same normalised base callsign into separate geographical markers.
- Station selection preserves the established `/cq callsign` prefill behaviour.
- Sending without an explicitly selected send category preserves the established Main-category fallback unless explicitly changed.

## Autoanswer / Beacon

- Automated-message loops must be prevented.
- Cooldown/minimum-interval rules must be preserved.
- A reply rejected before a complete valid TX item is queued must not consume cooldown.
- Current implementation/tests define the exact message markers and timer details.

## Build / Verification

- Use the repository Maven wrapper (`.\mvnw.cmd` on Windows).
- The project uses Java 21 / JavaFX 21.x at this context snapshot.
- JUnit 5/Mockito, PMD and SpotBugs are part of the verification environment.
- Build/test configuration has historically allowed some test/static-analysis failures not to fail the process exit code. Always read actual summaries/reports.

## Documentation Surfaces

- German and English manuals under `github_docs/`.
- Repository README.
- Eleventy-based project website under `website/`.
- Changelog/release communication.
- This technical project context under `docs/PROJECT_CONTEXT.md`.

After implementation use targeted documentation-impact checks. Do not run a complete manual audit unless explicitly requested, release preparation is broad, or targeted checks indicate systematic drift.

## Website / Deployment Relationship

The repository contains the KST4Contest website under `website/`, published separately from the desktop application build.

Current website/deployment scripts and update-feed behaviour must be inspected before changes; do not rely on historical assumptions.

## Important Decisions and Workarounds

- Preserve full callsign/category identity while applying base-call normalisation only to specifically defined features.
- Keep canonical worker-thread domain state separate from JavaFX UI projections.
- Preserve the established JavaFX WebView/Leaflet workaround that avoids problematic CSS 3D transforms unless the original rendering/flicker issue has been reproduced and the replacement is validated.
- Deliberate test data, comments and Easter eggs are preserved unless explicitly changed.

## Planned Technical Direction

These are planned directions, not necessarily implemented behaviour:

- improve propagation/path modelling using higher-resolution terrain data, including Copernicus GLO-30;
- increase terrain/path sampling through a dedicated service/API;
- support high-precision station locations (e.g. extended Maidenhead locators or direct GPS coordinates) while preserving compatible standard display;
- improve terrain/Fresnel/diffraction/refraction modelling for VHF/UHF/microwave use;
- evaluate/implement richer tropospheric/scatter models;
- continue integration of aircraft-scatter and propagation data into reachability/contest workflows.

Before implementing planned items, re-check current decisions and obtain a fresh concept approval.

## Known Limitations / Maintenance Notes

- Historical project context is useful but may be stale; current code/tests win.
- External service/API behaviour must be verified against current upstream documentation when uncertain.
- Screenshots in manuals/website may need targeted replacement after visible UI changes; never fabricate them.
- `station_map_path_analysis.png` and `station_map_reset.png` predate the **Group nearby stations** checkbox in the map header. Replace them with current screenshots when suitable source images are available; the website reuses `station_map_path_analysis.png` through `/manual/assets/`.

## Recent Significant Changes

### 2026-08-25 – Durable project context introduced

- Added a persistent technical context layer so future agents/developers can understand architecture, invariants and cross-project dependencies without replaying chat history.
- Documentation maintenance uses targeted impact assessment rather than a full audit after every implementation.

## Related Projects / Integration Points

### KST4Contest website

- Source is maintained inside this repository under `website/`.
- User-facing feature/configuration changes may require a targeted website check.

### hamradioonline.de

- Serves as the broader amateur-radio umbrella site/infrastructure context.
- KST4Contest content/download/manual links and related knowledge content may intersect with the broader site strategy.

### Webserver / hosting infrastructure

- KST4Contest website and other hamradioonline services depend on the hosting environment.
- Operational details should be maintained in a private infrastructure context rather than duplicated into this public project context when sensitive.

### Planned propagation / terrain service

- Intended to provide richer terrain/propagation data (including higher-resolution Copernicus GLO-30-based processing) to KST4Contest and potentially related hamradioonline tooling.
- Interface contracts must be documented on both provider and consumer sides when they become concrete.
