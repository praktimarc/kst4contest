# UI behaviour and workflow invariants

These are known user-experience decisions. Verify the current implementation before changing them.

## Selection and send text

A new station selection should prefill the established command form:

```text
/cq callsign
```

This behaviour is deliberate even when prior input text existed, according to the established workflow.

If no category is selected for sending, the established fallback is the Main category.

Do not change either behaviour as a side effect of unrelated refactoring.

## Map view

Known decisions:

- reset clears the target/station selection;
- reset does not change the current zoom level;
- selected-station information was moved toward the compact status line rather than requiring a persistent right-side detail panel;
- path-analysis visibility is user-controllable and should not become undiscoverable;
- map controls must remain usable in dark/light modes.

## Leaflet / JavaFX WebView

With Leaflet 1.9.4 under Java 21, fragmented rendering/flicker was fixed by disabling Leaflet CSS 3D transforms before Leaflet loads:

```text
window.L_DISABLE_3D = true
```

Do not remove/reorder this workaround without reproducing the rendering problem and proving the replacement.

## Filters

Known UI direction:

- Reset Filter must actually clear relevant filter predicates;
- reachability controls are conceptually separate from generic filter controls;
- reset control should remain visually discoverable;
- truncated text should remain accessible through tooltips where implemented;
- clickable links should remain functional in both themes.

## Priority / timeline

The contest workflow includes:
- priority candidate presentation;
- sked timeline;
- activity/AP windows;
- sked reminders.

Avoid UI changes that damage quick contest operation merely to make layout code simpler.

## Null display

Missing data is not `0`.

For QRB/QTF/locator/derived values, follow the current UI convention for unavailable/empty state.

Do not show a plausible-looking number when the model value is actually unknown.
