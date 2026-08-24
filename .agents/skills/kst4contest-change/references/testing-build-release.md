# Build, tests, static analysis and release safety

## Maven

Use the repository Maven wrapper.

Windows:

```text
.\mvnw.cmd test
.\mvnw.cmd package
```

Run narrower tests first when possible.

## Important Surefire behaviour

The project has used:

```xml
<testFailureIgnore>true</testFailureIgnore>
```

Therefore an exit code of zero is not sufficient evidence that all tests passed.

Always inspect:

- test counts;
- failures;
- errors;
- skipped tests;
- Surefire report output when necessary.

State exact results in the completion report.

## PMD and SpotBugs

PMD and SpotBugs are integrated, but their findings have historically not always failed the build.

Do not say "static analysis clean" unless the relevant reports/output were actually checked.

## Packaging

The build contains packaging/module-list consistency logic.

Changes involving modules, JavaFX modules, jpackage or `module-info.java` must check:

- `pom.xml`;
- `packaging/` helpers;
- module requirements;
- packaging verification output.

Do not manually update only one copy of a generated/synchronized module list.

## Website

The website is Eleventy-based and has Node tests.

Inspect `website/package.json`, `website/test/` and current scripts before choosing exact commands.

Historical website validation included Node tests for generated version/update information.

## Documentation build

GitHub Actions generates documentation/PDF and site artefacts.

A local code build does not prove documentation/site CI will pass.

## Versioning

Do not change project version, semantic version, update feed, tag or release metadata unless explicitly requested.

## Git

Each of these needs separate authorization:

- stage;
- commit;
- push;
- PR;
- merge;
- tag;
- release.

When asked to commit, use a concise English commit message.

Do not stage unrelated files.

## Release communication

When a release is explicitly in scope, check:

- current changelog;
- GitHub release/tag;
- website download/update feed;
- documentation;
- HamRadioOnline download/manual destinations;
- any SourceForge publication workflow currently used.

Do not assume an older deployment pipeline is still active.
