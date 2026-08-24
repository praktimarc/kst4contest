# Manual and website context

## Manual location

KST4Contest documentation is maintained in `github_docs/` with English and German Markdown pages plus screenshots.

The documentation build is automated through repository workflows.

## Audit workflow established with Marc

The normal review method is:

1. compare documentation with actual code/behaviour;
2. propose exact changes;
3. note missing/outdated screenshots and their intended repo location;
4. if code must change to match the manual, stop and confirm that code change first;
5. keep German and English content aligned;
6. prefer one thorough update over many cosmetic iterations.

With Codex editing locally, the old copy/paste insertion-guide step is replaced by direct edits, but the approval logic remains.

## Examples and easter eggs

Deliberate examples/test strings must not be "cleaned up" merely because they are informal.

A known example uses:

```text
DO5AMF
Testing DXC-Spot: Congrats, you donated $100!
```

Preserve such deliberate easter eggs unless Marc explicitly asks to remove or replace them.

## Website

The website under `website/` uses Eleventy/Nunjucks.

Style direction:

- modern and concise;
- technically focused;
- no promotional tone;
- English primary where appropriate;
- documentation remains the detailed source; website text should not duplicate entire manual sections.

Current website architecture/scripts must be inspected before changes.
