# KST4Contest

KST4Contest (also known as pratiKST) is a Java-based chat client for ON4KST, focused on VHF/UHF/SHF contest operation.

## Documentation

The full user documentation is maintained in the project wiki:

- https://github.com/praktimarc/kst4contest/wiki

Direct entry points:

- German start page: https://github.com/praktimarc/kst4contest/wiki/de-Home
- English start page: https://github.com/praktimarc/kst4contest/wiki/en-Home

## Build

Compile locally with Maven Wrapper:

```bash
./mvnw -B -DskipTests compile
```

## Notes

- Source code is under `src/`.
- Documentation markdown pages for wiki/PDF are under `github_docs/`.

## Status of the latest CI:
Wiki Publishing:

[![Publish wiki](https://github.com/praktimarc/kst4contest/actions/workflows/github-wiki.yml/badge.svg)](https://github.com/praktimarc/kst4contest/actions/workflows/github-wiki.yml)

[![Docs PDF](https://github.com/praktimarc/kst4contest/actions/workflows/docs-pdf.yml/badge.svg)](https://github.com/praktimarc/kst4contest/actions/workflows/docs-pdf.yml)

Builds:

[![Nightly Runtime Artifacts](https://github.com/praktimarc/kst4contest/actions/workflows/nightly-artifacts.yml/badge.svg)](https://github.com/praktimarc/kst4contest/actions/workflows/nightly-artifacts.yml)