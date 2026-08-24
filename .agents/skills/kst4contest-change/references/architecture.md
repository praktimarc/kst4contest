# Architecture context

## Current package shape

The repository currently contains major packages under `src/main/java/kst4contest/` including:

- `controller`
- `locatorUtils`
- `logic`
- `model`
- `service`
- `test`
- `utils`
- `view`

Do not treat package names alone as proof of clean MVC boundaries. Inspect actual dependencies.

## Preferred message/member data flow

The established target architecture for active chat members is:

```text
ON4KST / network
        |
        v
MessageBusManagementThread
        |
        v
ChatController
        |
        v
thread-safe active-member domain state
(ConcurrentMap; identity includes callsign + category)
        |
        v
JavaFX ObservableList UI mirror
        |
        v
FilteredList / SortedList / TableView / selection
```

Key rule:

`ObservableList` is a JavaFX UI projection, not the canonical store for worker-thread logic.

`MessageBusManagementThread` must not directly read or modify the UI list.

## JavaFX boundary

UI-visible mutations belong on the JavaFX Application Thread.

Prefer controller-owned helpers such as an existing `runOnFxThread` abstraction when available; otherwise use `Platform.runLater` consistently.

Do not move business/data access into the FX thread merely to silence a threading problem.

## Parser/service separation

For protocol receivers, the preferred direction is:

```text
Receiver (I/O only)
    -> Parser (wire data -> DTO)
    -> Service (domain/persistence logic)
    -> Controller (UI coordination)
    -> Observable UI model
    -> View
```

A previous concrete example for UCXLog was:

```text
UcxUdpReceiver
    -> UcxPacketParser
    -> DTO
    -> UcxLogService
    -> ChatController
    -> UI projection
```

This is architectural guidance, not permission for a broad refactor. Apply only when it is in scope and approved.

## DTO preference

Prefer explicit DTO classes over records when introducing protocol/transport data structures in this project, unless the approved concept intentionally changes that convention.

## Null safety

Chat members can be incomplete, especially:

- fallback members;
- historic message senders;
- server-derived partial members.

Values such as QRB and QTF can be absent.

Rules:

- absence stays absence;
- do not map `null` to numeric zero;
- UI must render unavailable state safely;
- sorting/filtering/calculation code must tolerate missing values;
- unexpected missing values must not terminate worker or UI threads.
