# Project behaviour catalog

This catalog summarizes behaviour established during prior KST4Contest work. It is context for analysis, not permission to overwrite newer code. Always inspect the current implementation before modifying a listed area.

## Core purpose

KST4Contest is an ON4KST-oriented desktop client optimized for VHF/UHF/microwave contest workflows.

Core areas developed over time include:

- simultaneous ON4KST chat handling;
- priority candidates;
- sked workflow and timeline;
- worked-state synchronization;
- logging integrations;
- DXCluster;
- AirScout / airplane-scatter assistance;
- rotor/control integrations;
- map/path visualization;
- automated replies/beacons;
- user filtering and reachability;
- documentation and website/update-feed integration.

## Two chat categories

The application is designed around two simultaneous relevant chat categories in normal operation.

Important consequences:

- same-looking calls in different categories are not automatically the same chat identity;
- category is part of message/member identity;
- category-specific QRG/band settings must not leak into the other category;
- unsupported categories must not crash shared logic.

## Priority candidates

Priority scoring has included factors such as:

- QTF match;
- recent activity;
- message count;
- positive signal indications;
- sked rate.

Do not change weighting/meaning as collateral work. Treat it as user-facing contest logic.

## Timeline / skeds

The sked timeline has used 30-minute lanes and visualized airplane-scatter probability windows.

Known historical AP strength levels:

- 100%;
- 75%;
- 50%.

Sked reminder presets have covered short contest-relevant lead times.

Do not hardcode historical display constants into new code without verifying the current view/model.

## Worked state

Worked state is loaded from persistence and updated live from supported logging inputs.

The simplified UI meaning has been "worked any" where the locator/worked indicator is concerned.

Worked state is base-call-wide across suffix variants where established.

When changing persistence or logging synchronization, verify:

- startup DB load;
- live update;
- suffix/base-call mapping;
- band mapping;
- 50/70 MHz support where applicable;
- UI projection.

## Known active bands / B+

Known-active-band information is derived consistently across the application from available hints.

Historical work unified:

- band mentions in user names;
- band mentions in text;
- manual/global band information;
- `B+`-style availability.

Explicit `NOT-QRV` overrides positive hints.

Avoid introducing a second parser with different semantics.

## Selection and send workflow

Established fast-workflow behaviour includes:

- selecting a new station prefills `/cq callsign`;
- send text is geared toward minimal contest interaction;
- if no target chat category is selected, Main is the established fallback.

These are intentional workflow decisions, not incidental UI details.

## DXCluster

DXCluster support has included:

- integrated display;
- copyable lines;
- `/cq` workflow support;
- beacon monitoring;
- QTF/bearing-related presentation.

Preserve locator semantics and avoid sender/receiver field confusion.

## Map / path view

Map work has included:

- Leaflet 1.9.4 in JavaFX WebView;
- terrain/path information;
- airplane-scatter integration;
- target-station selection;
- path-analysis visibility;
- station-count/status information;
- target reset that does not reset zoom.

A persistent right-side station-information panel has been reduced/removed in favour of more compact presentation in later UI work.

Before changing layout, inspect the current version because this area has been actively iterated.

## Reachability and filters

Filter work has separated reachability concerns from generic filters.

A Reset Filter control must reset the actual filter predicates, not just visual controls.

UI sorting/filtering must remain stable when backing data changes.

## Autoanswer and beacons

Automated messaging exists to reduce repetitive contest chat work without creating spam or feedback loops.

Important principles:

- conservative timing;
- bounded text;
- no loop on own automated markers;
- only consume cooldown after a valid queued reply;
- category/callsign-safe identity;
- QRG requests handled with the intended precedence.

## Connection handling

Network reliability is contest-critical.

Work has explicitly targeted:

- accurate connected/disconnected state;
- server disconnect detection;
- reconnect behaviour on unstable links;
- no infinite loop on initial connection failure;
- visible connection-state indication in the UI.

Do not regress connection state into "socket object exists therefore connected".

## Historic messages

Historic/non-current chat senders may not have a complete live `ChatMember`.

Highlighting, display and parsing must tolerate users not currently logged in.

## Website and documentation

The application repository also contains:

- bilingual manual content;
- documentation images;
- automated documentation PDF build;
- Eleventy website;
- download/update metadata generation;
- release-oriented website automation.

A user-visible feature change may therefore affect more than Java source.
