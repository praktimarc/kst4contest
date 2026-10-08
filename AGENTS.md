# KST4Contest agent instructions

These project instructions extend Marc's global Codex working agreements.

## Project identity

KST4Contest is a Java/Kotlin desktop client for ON4KST chat, with a Compose Multiplatform user interface with contest-oriented workflows and integrations including logging software, AirScout, rotor control, DXCluster and local persistence.

Primary repository areas:

- `core/src/main/java/kst4contest/` and `core/src/test/`
- `app-desktop/src/main/java/kst4contest/` and `app-desktop/src/test/`
- `github_docs/`
- `website/`
- `docs/`
- `packaging/`
- `.github/`
- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/`

Inspect the current tree before assuming an exact class/path still exists.

## Mandatory interaction rule

For every planned code or documentation implementation:

1. inspect first;
2. explain the task understanding in German;
3. identify and ask all relevant implementation questions;
4. wait for answers when needed;
5. present the final concept in German;
6. state what must remain unchanged;
7. request explicit concept approval;
8. wait;
9. implement only after approval.

If an answer is uncertain, do not interpolate it. Check current code/tests/docs/project context first and ask Marc when the uncertainty can affect behaviour.

## Language

- Communicate with Marc in German.
- Write source-code comments and Javadoc exclusively in English.
- Keep log/protocol/API literals in their canonical form.
- Commit messages are concise English when a commit is explicitly requested.
- User-facing DE/EN documentation follows `$praktimarc-writing-style`.

## Java and user-interface architecture

- Preserve or improve separation between network/parsing/service/controller/UI responsibilities.
- Do not solve architecture problems by letting worker/model code directly manipulate user-interface collections.
- Active chat-member domain state is conceptually thread-safe state and lives in an `ObservableRoster` in `core`; the `ObservableList` a `TableView` is bound to is a mirror of it, not the store.
- `MessageBusManagementThread` must not directly read or mutate the row lists the user interface draws from; it works on snapshots.
- Route UI-visible mutations through the controller and the `UiDispatcher` (`AwtUiDispatcher` hands them to the AWT event dispatch thread, where Compose draws). Do not reach for a toolkit thread call such as `Platform.runLater` or `EventQueue.invokeLater` in `core`; it has no toolkit dependency.
- Prefer explicit DTOs over records when introducing transport/parser DTOs in this codebase unless the approved concept says otherwise.
- Handle incomplete external/historical data defensively.
- `qrb`, QTF and related external values can be absent. `null` means unavailable, not zero.
- Unexpected input must not terminate message-processing or UI threads.

## Callsign and category identity

- Preserve full callsign variants as distinct chat-member identities where the server exposes them separately.
- Category is part of chat identity. Do not merge messages across categories.
- Base-call normalization may be used only for explicitly base-call-wide features such as worked status or monitoring rules.
- Worked status is shared across suffix variants of the same base call.
- Monitoring a callsign variant such as `DN9APW-2` or `DN9APW-70` is intended to monitor the base call `DN9APW`, so users do not need to enter every SSID.
- Do not generalize suffix semantics beyond behaviour explicitly established by the current code/specification.

## Bands and availability

- ON4KST categories 2 and 3 are central to the normal VHF/UHF workflow, but other category values can occur and must fail safely.
- Do not let unsupported categories produce exceptions.
- Known-active-band logic and `B+` interpretation must remain consistent across the application.
- Band information parsed from names/text must respect explicit `NOT-QRV` information; NOT-QRV overrides positive availability hints.
- Do not silently fall back to a fixed band/frequency when a required decision is ambiguous unless an approved fallback exists.
- Manual band settings and actual current QRG must remain consistent with features that depend on frequency.

## External protocols and integrations

Before changing ON4KST, AirScout, UCXLog/DXLog, Win-Test, PSTRotator or DXCluster handling:

- inspect the current implementation;
- preserve exact framing and compatibility;
- inspect current tests;
- check authoritative upstream documentation when the protocol detail is uncertain;
- ask Marc if more than one behaviour is plausible.

Specific invariants and historical context are in `$kst4contest-change` references.

Never change CR/LF, XML framing, callsign normalization, frequency formatting or port/transport assumptions casually.

## UI behaviour

- Preserve contest workflow speed and discoverability.
- Do not change zoom, selection, focus, sorting, tab choice or prefilled text as an incidental side effect.
- Map reset behaviour should clear the selected target without changing the zoom unless a new task explicitly changes this.
- New station selection should preserve the established `/cq callsign` prefill behaviour.
- If no send category is selected, preserve the established Main-category fallback unless explicitly changed.
- Null/unknown data must render as unavailable/empty according to current UI conventions, not as fake zero values.

## Map

- The station map is a Compose canvas. It fetches its tiles directly; the former JavaFX WebView, the bundled Leaflet resources, the CSS-3D-transform workaround and the `TileProxyServer` that worked around the WebView's TLS behaviour in AppImage and Flatpak packaging are all gone.
- Map reset clears the selected target without changing the zoom. Offline DEM, terrain packages and the profile cache must keep working unchanged.

## Autoanswer / beacon safety

- Prevent automated-message loops.
- Respect the established minimum interval/cooldown logic.
- Do not consume a cooldown for a reply that is rejected before a complete valid TX item is queued.
- Preserve priority of frequency/QRG requests where established.
- Cooldown identity must not accidentally collapse unrelated callsign/category identities.
- Treat the current implementation/tests as the source of truth for exact message markers and timer details.

## Build and verification

Use the Gradle wrapper.

Windows:

```text
.\gradlew.bat ...
```

Read the current `gradle.properties` and `gradle/libs.versions.toml` before relying on version numbers.

At this context snapshot the project uses Java 21, Kotlin 2.2 and Compose Multiplatform 1.8.x, with JUnit 5/Mockito, PMD and SpotBugs integrated. JavaFX is gone; there is no `org.openjfx` dependency left.

Important: the build configuration has historically allowed test failures to be ignored, and static-analysis findings do not fail the build. Therefore:

- inspect the Gradle test summary;
- inspect the test-results XML under `*/build/test-results/` when needed;
- do not infer "all tests passed" from exit code 0;
- report PMD/SpotBugs findings that are visible in the relevant build.

Run focused tests first, then normally the relevant broader test/build command for the scope.

## Documentation and durable project context

Use `$software-project-context`, `$kst4contest-documentation`, and `$praktimarc-writing-style` as relevant.

Do not perform a full manual or website audit after every implementation.

After a completed change:

1. perform a short documentation-impact classification;
2. if user-visible behaviour is plausibly affected, search only the relevant German and English manual sections under `github_docs/`;
3. keep both language versions semantically aligned when an update is required;
4. check README and website feature text only when the changed feature/configuration is represented there or is likely to need representation;
5. identify screenshots that are likely stale instead of fabricating replacements;
6. update `docs/PROJECT_CONTEXT.md` for significant architectural, protocol, state/persistence, operational, integration, deployment, workaround, or long-lived behavioural decisions;
7. keep `Related Projects / Integration Points` current when KST4Contest, its website, hamradioonline infrastructure or planned propagation services affect one another.

A full documentation audit is reserved for explicit audit requests, major release preparation, broad UI/workflow changes, or evidence that documentation is broadly stale.

## Website

The repository contains an Eleventy-based website under `website/` with its own tests/build logic.

Do not assume website deployment/update-feed details; inspect current scripts/workflows before changing them.

## Change scope and Git

- No unrelated refactoring.
- No production dependency without prior approval.
- No automatic version bump.
- No commit/push/merge/tag/release/deploy without separate explicit authorization.
- Preserve deliberate test data and easter eggs unless explicitly changed.
- Never overwrite unrelated working-tree changes.

## Completion

After implementation, report in German:

- understanding fulfilled;
- changed files;
- important design decisions;
- tests/builds and exact results;
- documentation-impact classification;
- manual/website/README/context updates made or why none were necessary;
- related-project impact when relevant;
- remaining uncertainty;
- suggested next action, without performing it automatically.
