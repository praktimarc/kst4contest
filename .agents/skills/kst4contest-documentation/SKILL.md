---
name: kst4contest-documentation
description: Perform targeted KST4Contest documentation-impact checks and update affected German/English manuals, README, website feature text, changelog, release notes and durable project context. Avoid full audits by default; keep documentation aligned with implemented behaviour and use the praktimarc-writing-style skill.
---

# KST4Contest documentation workflow

Use `$software-project-context` and `$praktimarc-writing-style`.

## Default behaviour

Do not read the complete manual or website after every code change.

Start with a documentation-impact classification and search for the affected feature, setting, UI label, protocol/integration or operational concept.

Escalate to a broader audit only when:

- Marc explicitly requests it;
- a major release is being prepared;
- the change is broad across UI/workflows;
- multiple targeted checks reveal wider drift.

## Before editing documentation

1. Inspect the actual implementation or approved specification.
2. Explain in German what documentation is probably affected.
3. Ask only unresolved behaviour/scope questions.
4. If documentation updates are already part of an approved implementation concept, no second approval is required for obvious synchronisation.
5. If documentation reveals a new material product decision, stop and ask Marc.

## Manuals

When user-facing behaviour is affected:

- search English and German manual content under `github_docs/` for the relevant feature/labels first;
- inspect surrounding sections only;
- keep both language versions semantically equivalent;
- do not translate mechanically; English must be idiomatic;
- preserve exact UI labels, values, callsigns, ports and protocol terminology;
- document current behaviour only;
- if code and manual disagree and it is unclear which behaviour is intended, report the conflict;
- identify outdated/missing screenshots explicitly.

## README / website

Check only when the changed feature, installation, configuration, capability or compatibility is represented there or should reasonably be represented there.

- Keep feature descriptions concise.
- Explain real contest/operating benefit, not marketing slogans.
- Avoid duplicating large manual sections on the website.
- Keep the main manual/download destinations consistent with the current site strategy.

## Durable project context

Update `docs/PROJECT_CONTEXT.md` for significant:

- architectural decisions;
- threading/state ownership;
- callsign/category semantics;
- protocol/integration contracts;
- persistence/configuration changes;
- durable workarounds;
- deployment/website relationships;
- cross-project dependencies;
- planned propagation/API architecture when it becomes concrete.

Keep current-state sections current rather than using the file as a raw changelog.

## Changelog / release notes

- Compact factual bullets.
- Include user-visible changes and important reliability/compatibility fixes.
- Do not invent version scope; derive it from actual commits/changelog/release context.
- Keep release posts short and operationally relevant.
