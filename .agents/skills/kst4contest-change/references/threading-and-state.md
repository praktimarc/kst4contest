# Threading and state management

## Canonical state vs UI state

Use a thread-safe canonical state for data consumed by worker/network threads.

The active-member UI list is only a projection.

Preferred conceptual model:

```text
ConcurrentMap<MemberKey, ChatMember> activeMembers
        |
        | FX-thread projection/update
        v
ObservableList<ChatMember> activeMembersUi
```

`MemberKey` semantics must preserve full callsign plus category unless the specific operation is intentionally base-call-wide.

## MessageBusManagementThread

Do not:

- iterate JavaFX `ObservableList` from the worker thread;
- add/remove JavaFX-list entries directly from the worker thread;
- use the FX thread as a substitute for proper domain state ownership.

Do:

- pass domain events/data to the controller/service boundary;
- modify canonical thread-safe state outside UI code as appropriate;
- project changes to JavaFX state on the FX thread.

## Controller boundary

`ChatController` is the preferred coordination boundary for UI-visible state.

Keep view-specific operations out of protocol receiver code.

## External receiver design

For receiver refactors, separate:

- socket/UDP/TCP I/O;
- parsing;
- DTO;
- domain/persistence;
- UI coordination.

## Error containment

Long-running threads must survive:

- malformed server records;
- incomplete members;
- unknown bands/categories;
- missing locators;
- null QRB/QTF;
- temporary socket failure.

Catch errors at meaningful boundaries and include enough context in English diagnostic logs/comments to trace the input and stage of failure.

Do not swallow errors silently.

## JavaFX selection/sorting

When updating backing data:

- preserve current selection where the feature expects it;
- avoid invalidating `FilteredList`/`SortedList` assumptions;
- do not create recursive UI updates;
- avoid accessing control state from worker threads.
