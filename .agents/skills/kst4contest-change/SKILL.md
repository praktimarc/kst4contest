---
name: kst4contest-change
description: Analyze or implement KST4Contest Java/JavaFX changes, bug fixes, refactorings, protocol handling, contest workflow behaviour, callsign/band logic, AirScout/logging/rotor/DXCluster integrations, threading and state management. Use current code as source of truth and follow the mandatory concept-and-question gate before edits.
---

# KST4Contest change workflow

Read the relevant reference files before proposing a concept.

## Phase 1: read-only analysis

- Inspect the current code and tests.
- Identify the current data flow and thread ownership.
- Identify user-visible and protocol-visible behaviour.
- Check relevant `docs/PROJECT_CONTEXT.md` sections when they exist.
- Check whether the task overlaps a known invariant in the references.
- Do not modify files.

## Phase 2: report understanding in German

Explain:

- what Marc wants changed;
- what must remain unchanged;
- which components appear affected;
- what evidence in the current code supports that understanding;
- any conflict between current code and historical project context.

Never resolve a conflict by guessing.

## Phase 3: questions and final concept in German

Before finalizing the concept:

- identify all material implementation choices;
- ask Marc questions that are not already answered by code, project instructions or prior confirmed decisions;
- wait for answers when needed.

Then present:

- intended data/control flow;
- exact behavioural changes;
- compatibility impact;
- thread/UI impact;
- persistence impact;
- test strategy;
- likely documentation impact;
- likely durable project-context impact;
- related-project impact when relevant.

Ask for explicit concept approval and wait before editing.

## Phase 4: implementation

After approval:

- implement the smallest coherent change;
- preserve unrelated behaviour;
- keep comments/Javadoc in English;
- add/update focused tests;
- keep external protocol parsing defensive;
- avoid hidden defaulting for unknown values.

## Phase 5: verification and documentation impact

Run focused checks, then appropriate broader checks.

Because KST4Contest build configuration may ignore failures/findings, inspect summaries and reports rather than only command exit status.

Then use `$software-project-context`:

- do a low-cost documentation-impact classification;
- inspect only likely affected manual/README/website sections;
- update targeted documentation when clearly required by the approved implementation;
- update `docs/PROJECT_CONTEXT.md` for significant durable technical decisions/state changes;
- do not perform a full manual audit unless there is a specific trigger.

## Phase 6: report

Report in German:

- files changed;
- implementation summary;
- tests/checks;
- warnings/findings;
- documentation-impact result;
- documentation/context updates or why none were required;
- related-project impact when relevant;
- unresolved issues;
- no Git publication action unless explicitly requested.
