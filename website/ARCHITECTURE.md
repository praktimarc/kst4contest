# KST4Contest Website Architecture

## Goals

KST4Contest uses GitHub as the central source for code, documentation and releases.

The website is generated statically with Eleventy (11ty) and deployed to:

https://kst4contest.hamradioonline.de

## Documentation Source

The existing `github_docs/` directory remains the single source of truth for the manual.

It is used for:

- GitHub Wiki
- PDF manual generation
- Website manual pages

## Website Source

The `website/` directory contains the Eleventy project.

Manual content must not be duplicated inside `website/`.

Marketing, SEO, news, screenshots and download pages belong into `website/src/`.

## URL Structure

- `/`
- `/features/`
- `/download/`
- `/screenshots/`
- `/news/`
- `/manual/`
- `/manual/en/`
- `/manual/de/`
- `/faq/`

## Deployment

The generated static output is deployed to:

`/srv/www/kst4contest/current`

Nginx serves this directory.

## Build Strategy

GitHub Actions will later build the 11ty website and deploy the generated output to the server.