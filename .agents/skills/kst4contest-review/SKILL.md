---
name: kst4contest-review
description: Review current KST4Contest local changes or a proposed diff before commit. Check regressions, null safety, JavaFX threading, callsign/category semantics, band handling, protocol compatibility, tests, targeted documentation impact, durable project context and unintended scope. Report findings in German and do not modify files unless explicitly asked after the review.
---

# KST4Contest review

Review first; do not edit during the review.

Read the relevant KST4Contest change references.

## Review priorities

1. Behaviour matches the approved concept.
2. No unrelated changes.
3. Full callsign/category identity remains correct.
4. Base-call normalization is used only where intended.
5. Null/unknown values are not converted to fake defaults.
6. Worker threads do not manipulate JavaFX UI collections.
7. FX-thread boundaries are correct.
8. Protocol framing, CR/LF, XML and frequency formatting are unchanged unless explicitly intended.
9. External malformed input cannot kill long-running threads.
10. Tests cover the changed behaviour.
11. Maven test output was interpreted correctly despite ignored-failure settings.
12. A documentation-impact assessment was performed.
13. Any likely affected manual/README/website sections match the implementation.
14. `docs/PROJECT_CONTEXT.md` is updated when the change introduces a durable architectural/protocol/state/operational/integration decision.
15. Comments/Javadoc are English.
16. No unintended dependency/version/release changes.

Do not demand a full manual audit for an internal-only change when the impact assessment reasonably concludes there is no documentation effect.

## Report format

Report in German, ordered by severity.

For each finding include:

- affected file/location;
- concrete problem;
- consequence;
- recommended correction.

Then include:

- verification gaps;
- documentation-impact result;
- durable-context gaps;
- related-project gaps when relevant;
- overall assessment.

Do not fix findings until Marc explicitly asks for implementation and the normal concept gate has been satisfied for the fixes.
