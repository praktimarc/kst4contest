#!/usr/bin/env python3
"""Which methods of Kst4ContestApplication can reach Compose state.

The question this answers is "is this block of JavaFX construction safe to delete",
and it is harder than it looks. Three weaker versions of it were tried first, and
each missed something the next one found:

  - grepping a method body for the word "compose" misses menu handlers, which open
    Compose windows by calling SettingsWindow and its siblings by name;
  - grepping a method body for those names catches that, but misses a method whose
    body is clean and which calls a helper that writes;
  - only following the calls finds all four.

So: follow the calls, and key on the names of the things that hold Compose state,
not on the word "compose".

Usage, from the repository root:

    python3 docs/superpowers/notes/javafx-compose-sweep.py

Prints one line per init… method. A method reported clean is clean to depth 3; the
depth is a limit, not a proof.
"""

import re
import sys

SOURCE = "app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java"

# Everything that is, holds, or opens Compose state. Extend this when a new Compose
# window or table appears; a name missing here is a method wrongly reported clean.
COMPOSE_TOKENS = (
    "composeMainWindowState",
    "composeMemberListener",
    "MainWindowHost",
    "SettingsWindow",
    "MonitorWindow",
    "UpdateWindow",
    "StationMapWindow",
    "OperatorProfilePickerWindow",
    "DataTableState",
    "replaceRows",
    "replaceSkeds",
    "replaceCandidates",
    # The feeds are the Compose sinks since part 2: the mixed init methods write
    # Compose state through feed.push(...) rather than through a replace… call
    # directly. Without these names the sweep reports those methods clean and would
    # wave through a deletion that empties a Compose table with a green build.
    "timelineFeed",
    "selectedStationMessagesFeed",
    "connectionStateFeed",
)

MAX_DEPTH = 3

METHOD_DECLARATION = re.compile(
    r"^\t(?:(?:public|private|protected|static|final|synchronized)\s+)+"
    r"[\w<>,\[\]\. ]+\s+(\w+)\s*\("
)
CALL = re.compile(r"\b(\w+)\s*\(")


def read_methods(lines):
    """Maps a method name to the line ranges of its declarations."""
    methods = {}
    for index, line in enumerate(lines):
        match = METHOD_DECLARATION.match(line)
        if not match or line.strip().startswith("//"):
            continue
        end = next(
            (j + 1 for j in range(index + 1, len(lines)) if lines[j].startswith("\t}")),
            len(lines),
        )
        methods.setdefault(match.group(1), []).append((index + 1, end))
    return methods


def reaches_compose(name, methods, lines, seen=frozenset(), depth=0):
    """The path by which `name` reaches Compose state, or None."""
    if depth > MAX_DEPTH or name in seen:
        return None
    seen = seen | {name}

    for start, end in methods.get(name, []):
        for k in range(start - 1, end):
            if lines[k].strip().startswith("//"):
                continue
            for token in COMPOSE_TOKENS:
                if token in lines[k]:
                    return [f"{name} @{k + 1}:{token}"]

        for k in range(start - 1, end):
            if lines[k].strip().startswith("//"):
                continue
            for callee in set(CALL.findall(lines[k])):
                if callee in methods and callee != name:
                    path = reaches_compose(callee, methods, lines, seen, depth + 1)
                    if path:
                        return [f"{name} -> "] + path
    return None


def main():
    try:
        lines = open(SOURCE, encoding="utf-8").read().split("\n")
    except OSError as unreadable:
        print(f"cannot read {SOURCE}: {unreadable}", file=sys.stderr)
        print("run this from the repository root", file=sys.stderr)
        return 1

    methods = read_methods(lines)
    inits = sorted(n for n in methods if n.startswith("init") and n[4:5].isupper())

    live = 0
    for name in inits:
        path = reaches_compose(name, methods, lines)
        if path:
            live += 1
            print(f"{name:42s} REACHES COMPOSE: {''.join(path)}")
        else:
            print(f"{name:42s} clean to depth {MAX_DEPTH}")

    print(f"\n{live} of {len(inits)} init methods reach Compose state.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
