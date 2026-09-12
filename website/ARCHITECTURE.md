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

The production server keeps a checkout at `/srv/git/kst4contest`. A root cron
job runs `/srv/scripts/deploy-kst4contest-website.sh` every five minutes. The
script resets the checkout to `origin/main`, installs the locked npm
dependencies, builds and validates the Eleventy site, and synchronises the
result to `/srv/www/kst4contest/current` with the ownership required by Nginx.

This deploy path updates only the static website. Server-side analytics,
Nginx, systemd, Logrotate, GeoIP, Basic Auth and Certbot configuration are
installed and maintained separately as described in
[`ops/analytics/README.md`](ops/analytics/README.md).
