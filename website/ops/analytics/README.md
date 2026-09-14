# Server-side website statistics

This directory contains the maintained installation templates and the operator
runbook for the production analytics service on Ubuntu Server 24.04. Nothing in
the repository installs, updates or activates the server-side components
automatically.

The design has three separate outputs:

- private static GoAccess HTML and JSON reports for each registered project
  subdomain and for all registered project subdomains combined;
- private durable daily and annual views of website page views and successful
  requests for the update-information XML file;
- a small public `visitor-count.json` file for sites which explicitly enable
  the counter.

The public number is the sum of daily approximate unique visits since the
configured activation date. GoAccess treats requests with the same IP address,
date and user agent as one visit. That is useful for a rough trend. It is not a
count of people.

## Data flow

```text
eligible page request
        |
        v
dedicated Nginx analytics log (14 days)
        |
        v
GoAccess with IP anonymisation
        |
        +--> private per-site and combined HTML/JSON reports (395 days)
        |
        v
durable daily counter state --> public visitor-count.json
                                      |
                                      v
                         same-origin home-page request

private HTML reports --> Nginx Basic Auth --> stats.hamradioonline.de

exact GET + HTTP 200 for /kst4ContestVersionInfo.xml
        |
        v
separate Nginx update-information log (14 days)
        |
        v
durable daily totals and countries
        +--> hourly and client-group detail (14 days)

eligible page log --> durable daily page views, countries and paths
```

Nginx writes a dedicated, reduced log. For each site, the generator gives the
existing, uncompressed `.1` rotation and then the current log directly to
GoAccess. GoAccess uses its persistent state to skip entries already
processed. The generator does not use an incremental shell pipeline or
decompress older rotations. Reports, database updates and public counter data
are first prepared in a staging directory. GoAccess output is validated
before any published report changes. The last valid report therefore survives
a failed GoAccess run.

The counter state is deliberately separate from the 395-day report database.
Each day is replaced with the latest value reported by GoAccess instead of
being added again. This makes repeated runs idempotent. Values older than 395
days remain in the counter state and continue to contribute to the public
total.

The private metric state is separate again. It retains website page-view
totals, absolute country values and individual normalized paths per day, plus
the equivalent totals and countries for update-information requests. Hourly
and recognisable client-group detail for update requests is removed after 14
days. The generator derives annual totals from the durable daily values. It
does not build a permanent path-by-country table.

## Production platform

The confirmed production baseline is:

- Ubuntu Server 24.04;
- Nginx 1.24;
- Node.js 18.19.1;
- GoAccess 1.8.1 with GeoIP2/MMDB and OpenSSL support, but without Zlib;
- a local GeoLite2-Country database;
- systemd for the oneshot generator and its hourly timer;
- Logrotate for the dedicated website and update-information logs.

Missing Zlib support is intentional for this operating model. The generator
processes the current uncompressed analytics log and the optional uncompressed
`.1` rotation. It does not process `.gz` files during regular operation.

## Files

- `generate-reports.js` validates configuration and state, runs GoAccess and
  publishes outputs atomically.
- `private-metrics.js` parses the reduced logs, maintains durable daily
  aggregates and renders the protected daily and annual views.
- `sites.example.json` is the registry template.
- `goaccess.conf.template` is rendered per report with a private database path
  and the configured GeoIP2 Country database.
- `nginx/` contains the reduced log format, request filters, public endpoint
  and protected report-vhost examples.
- `systemd/` contains a hardened oneshot service and hourly timer.
- `logrotate/` retains 14 daily rotations for both reduced logs.

These repository files are templates and source files. Their productive
counterparts are installed separately:

- generator: `/opt/hamradioonline-analytics/generate-reports.js`;
- private metric module: `/opt/hamradioonline-analytics/private-metrics.js`;
- registry: `/etc/hamradioonline-analytics/sites.json`;
- GoAccess template:
  `/etc/hamradioonline-analytics/goaccess.conf.template`;
- service and timer: `/etc/systemd/system/hamradioonline-analytics.service`
  and `/etc/systemd/system/hamradioonline-analytics.timer`;
- Logrotate configuration: `/etc/logrotate.d/hamradioonline-analytics`;
- service state and derived outputs: `/var/lib/hamradioonline-analytics`.

## Prerequisites

- Node.js 18.19.1 or newer;
- GoAccess with GeoIP2/MMDB support;
- one GeoIP2 **Country** database, not a City database;
- Nginx;
- an unprivileged service account, shown as `hamradio-analytics` in the
  examples.

No npm package is required by the generator. GoAccess is the only external
program it starts. Historical `.gz` input is decompressed by Node.js and does
not require Zlib support in the installed GoAccess 1.8.1 binary.

The production compatibility baseline is GoAccess 1.8.1 built with
`--enable-geoip=mmdb` and `--with-openssl`, but without `--with-zlib`. Zlib is
not required for the regular operating mode because it reads only
uncompressed files. The configuration check reports the detected build
features and explicitly accepts this combination.

The confirmed production baseline is Node.js 18.19.1. The generator and tests
must remain compatible with it; upgrading Node.js is not part of this setup.

The service account needs read access to the dedicated analytics logs and
`/var/lib/GeoIP/GeoLite2-Country.mmdb`. It needs write access only to its state,
report and public-output directories. Access is group-based. The setup does
not depend on ACLs or `setfacl`. Nginx receives read access to reports and the
public counter through the `www-data` group, but no write access.

## Accounts and roles

`hamradio-analytics` is a local system account with its own group, no regular
home directory, `/usr/sbin/nologin` as its shell and a locked password. It runs
the generator and GoAccess, reads the reduced logs and configuration, and
writes only below the configured service-state directories.

Nginx runs as `www-data`. It writes the dedicated website and update-information
logs and reads the private reports and public counter. It must not be able to
read the private GoAccess databases, `public-counter-state.json` or
`private-metrics-state.json`, and it has no write access to generated output.

`stats-reader` is the current local Nginx Basic Auth username. It is not a
Linux service account and not an account with an external analytics provider.
Its password file is `/etc/nginx/htpasswd/hamradioonline-analytics`. Store the
password and hash only on the server and in a future protected backup, never in
Git, this runbook, screenshots or support output.

## Installation and permissions

Do not trust Unix modes stored in a ZIP created on Windows. Install every
script, configuration and unit with an explicit owner, group and mode. The
following commands assume that the package has been unpacked into the current
directory. Create the dedicated, unprivileged service account once:

```sh
if ! getent passwd hamradio-analytics >/dev/null; then
  sudo useradd --system --user-group --home-dir /nonexistent --no-create-home \
    --shell /usr/sbin/nologin hamradio-analytics
fi
```

Then install the generator, configuration and units:

```sh
sudo install -d -o root -g hamradio-analytics -m 0750 \
  /opt/hamradioonline-analytics /etc/hamradioonline-analytics
sudo install -o root -g hamradio-analytics -m 0750 \
  website/ops/analytics/generate-reports.js \
  /opt/hamradioonline-analytics/generate-reports.js
sudo install -o root -g hamradio-analytics -m 0640 \
  website/ops/analytics/private-metrics.js \
  /opt/hamradioonline-analytics/private-metrics.js
sudo install -o root -g hamradio-analytics -m 0640 \
  website/ops/analytics/goaccess.conf.template \
  /etc/hamradioonline-analytics/goaccess.conf.template
sudo install -o root -g hamradio-analytics -m 0640 \
  website/ops/analytics/sites.example.json \
  /etc/hamradioonline-analytics/sites.json
sudo install -o root -g root -m 0644 \
  website/ops/analytics/systemd/hamradioonline-analytics.service.example \
  /etc/systemd/system/hamradioonline-analytics.service
sudo install -o root -g root -m 0644 \
  website/ops/analytics/systemd/hamradioonline-analytics.timer.example \
  /etc/systemd/system/hamradioonline-analytics.timer
sudo install -o root -g root -m 0644 \
  website/ops/analytics/logrotate/hamradioonline-analytics.example \
  /etc/logrotate.d/hamradioonline-analytics
```

Create the writable tree deliberately. The state root is traversable but not
readable by Nginx. Only the report and public branches use the `www-data`
group and the set-group-ID bit:

```sh
sudo install -d -o hamradio-analytics -g hamradio-analytics -m 0711 \
  /var/lib/hamradioonline-analytics
sudo install -d -o hamradio-analytics -g hamradio-analytics -m 0750 \
  /var/lib/hamradioonline-analytics/db
sudo install -d -o hamradio-analytics -g www-data -m 2750 \
  /var/lib/hamradioonline-analytics/reports \
  /var/lib/hamradioonline-analytics/reports/combined \
  /var/lib/hamradioonline-analytics/reports/kst4contest \
  /var/lib/hamradioonline-analytics/reports/metrics \
  /var/lib/hamradioonline-analytics/public \
  /var/lib/hamradioonline-analytics/public/kst4contest
```

Generated HTML and JSON reports use mode `0640`. The public
`visitor-count.json` uses `0644`. Private GoAccess databases and
`public-counter-state.json` remain owned by `hamradio-analytics` and unreadable
by Nginx. The service unit uses `StateDirectoryMode=0711` to retain this
boundary after systemd has prepared the state directory.

The productive ownership and mode boundaries are:

- generator: `0750 root:hamradio-analytics`;
- private metric module: `0640 root:hamradio-analytics`;
- registry and GoAccess configuration: `0640 root:hamradio-analytics`;
- report directories, including `reports/combined`, `reports/kst4contest` and
  `reports/metrics`: `2750 hamradio-analytics:www-data`;
- report files: `0640 hamradio-analytics:www-data`;
- private database files: `0640 hamradio-analytics:hamradio-analytics`;
- `public-counter-state.json`: mode `0640`, owner and group
  `hamradio-analytics:hamradio-analytics`;
- `private-metrics-state.json`: mode `0640`, owner and group
  `hamradio-analytics:hamradio-analytics`;
- public output directories, including `public/kst4contest`: mode `2750`,
  owner and group `hamradio-analytics:www-data`;
- public `visitor-count.json`: `0644 hamradio-analytics:www-data`;
- Basic Auth password file: `0640 root:www-data`.

`/opt/hamradioonline-analytics` and `/etc/hamradioonline-analytics` are managed
by root and readable by the service group. `/var/lib/hamradioonline-analytics`
belongs to the service account. Its `0711` root allows Nginx to traverse known
paths without listing or reading private state. The service has a restrictive
`UMask=0027`; the generator therefore sets the public counter to `0644`
explicitly so Nginx can serve it. Missing output directories are a
configuration error. Create them deliberately with the shared group and
set-group-ID modes above rather than relying on recursive creation with an
unsuitable group.

Create the analytics log only when it does not already exist. Running
`install /dev/null` unconditionally would empty an existing log:

```sh
if [ ! -e /var/log/nginx/kst4contest-analytics.log ]; then
  sudo install -o www-data -g hamradio-analytics -m 0640 /dev/null \
    /var/log/nginx/kst4contest-analytics.log
fi
if [ ! -e /var/log/nginx/kst4contest-update-information.log ]; then
  sudo install -o www-data -g hamradio-analytics -m 0640 /dev/null \
    /var/log/nginx/kst4contest-update-information.log
fi
sudo stat -c '%U:%G %a %n' /var/log/nginx/kst4contest-analytics.log
sudo stat -c '%U:%G %a %n' \
  /var/log/nginx/kst4contest-update-information.log
```

The resulting log owner and mode must be
`www-data:hamradio-analytics 640` for both files. Logrotate preserves that ownership. The
generator's configuration check fails clearly if required output directories
are missing or if the executing user cannot read an analytics log or the
Country database.

## Registry

Copy `sites.example.json` outside the checkout and adjust it to the private
server layout. Each site entry contains:

- a stable `id` used for state and the GoAccess database;
- its exact `hostname`;
- the current, uncompressed `analyticsLog` path;
- the `activatedOn` date used by the public counter;
- `websiteMetricsSince`, the first day covered by the new live website
  page-view aggregation;
- an `updateInfo` object containing the exact XML path, its separate current
  log and the first day covered by live update-information aggregation;
- a `publicCounter` switch;
- the private `reportOutputDirectory`;
- a `publicJsonPath` when the public counter is enabled.

The top-level `privateMetrics` object defines its private state and report
paths, fixes the time zone to `Europe/Berlin` and fixes detailed retention to
14 days. The top-level `combined.reportOutputDirectory` receives the combined report.
Only registered sites are included. The generator rejects
`stats.hamradioonline.de`, so the report host cannot accidentally become part
of the project statistics.

The dates in `sites.example.json` document the repository snapshot; they are
not installation defaults. Before enabling the new logs, replace
`websiteMetricsSince` and `updateInfo.metricsSince` with the actual first live
coverage day. If collection started earlier than the current/`.1` handover,
import the covered rotations before the first regular run. Do not backdate a
field merely to obtain an earlier-looking report.

To add another project subdomain later, add one registry entry and one matching
dedicated `access_log` line to its Nginx server block. Do not enable a public
counter unless that site should publish one.

Treat `activatedOn`, `websiteMetricsSince` and `updateInfo.metricsSince` as
persistent data. Once counting has started, changing one of these values
would change the meaning of the total. The generator refuses to combine a new
date with the corresponding existing state.

The hostname is persistent identity as well. If an existing site state has a
different `activatedOn` or hostname, do not delete the state to make the next
run pass. Changing either value requires a deliberate migration or a
specifically approved reset of the public count.

The generator derives the optional `.1` paths from `analyticsLog` and
`updateInfo.analyticsLog`. It is valid for `.1` not to exist before the first
rotation. Do not enter a rotation or a compressed `.gz` file in the registry.

Adding another project subdomain also requires its own Nginx website and
update-information logs,
the corresponding Logrotate ownership, a prepared report directory and, when
enabled, a public-output directory and counter location. The combined report
uses the logs of every registered project site. The statistics vhost remains
outside the registry.

## Nginx logging

The relevant production configuration files are:

- `/etc/nginx/snippets/hamradioonline-analytics-filters.conf`;
- `/etc/nginx/conf.d/hamradioonline-analytics-log.conf`;
- `/etc/nginx/snippets/kst4contest-public-counter.conf`;
- `/etc/nginx/sites-available/kst4contest.conf`;
- `/etc/nginx/sites-available/stats.hamradioonline.de`.

Install the log-format and filter maps from `nginx/` in the `http` context.
Then add the dedicated website and update-information `access_log` directives
to every registered project server block. Keep the existing operational access
log unless its replacement has been reviewed separately. If the operational log is inherited from the
`http` context, repeat its directive in the server block before adding the
analytics log; an `access_log` at a lower level changes inheritance.

Install Nginx snippets explicitly as `root:root` with mode `0644`; do not copy
the modes from the ZIP. Nginx `map` exact-string keys use the path itself, for
example `/visitor-count.json`, without the location-modifier prefix `=`.

```sh
sudo install -o root -g root -m 0644 \
  website/ops/analytics/nginx/analytics-filters.conf.example \
  /etc/nginx/snippets/hamradioonline-analytics-filters.conf
sudo install -o root -g root -m 0644 \
  website/ops/analytics/nginx/analytics-log.conf.example \
  /etc/nginx/conf.d/hamradioonline-analytics-log.conf
sudo install -o root -g root -m 0644 \
  website/ops/analytics/nginx/public-counter.conf.example \
  /etc/nginx/snippets/kst4contest-public-counter.conf
```

Prepare the public-counter include without enabling it in the active site yet.
Likewise, keep the statistics vhost disabled until the certificate bootstrap
step below.

The analytics format contains only:

- server name;
- client IP address;
- timestamp;
- method;
- normalized path without query string;
- protocol;
- status;
- transferred body size;
- user agent.

The fields are tab-separated. The production website log is
`/var/log/nginx/kst4contest-analytics.log`; the separate XML log is
`/var/log/nginx/kst4contest-update-information.log`. Nginx writes both as
`www-data`; the `hamradio-analytics` group can read them. The confirmed owner and mode are
`www-data:hamradio-analytics 0640`. The analytics service receives read-only
access and must never truncate or otherwise modify this log.

The format uses `$uri`, not `$request_uri`, so query strings never enter the
analytics log. It also omits referrer and authenticated remote-user data. The
user agent is retained because GoAccess needs it for crawler classification
and its visit definition.

Only eligible `GET` requests enter the website analytics log. Assets, downloads, status and
monitoring paths, sitemap, robots file, favicons, the update feed and the
public counter endpoint are excluded. Known bots, crawlers, monitoring
clients, `wget` and `curl` are rejected before logging. GoAccess applies its
own crawler list as a second layer and treats unknown browser or operating
system combinations as crawlers. The public counter request therefore cannot
count itself, and the statistics vhost has no analytics logging of its own.

The update-information map is deliberately independent of the website and bot
filters. It logs only an exact case-sensitive `GET` request for
`/kst4ContestVersionInfo.xml` when the final response status is `200`. `HEAD`,
`304`, redirects and error responses do not enter this log. Browsers and bots
are not excluded, because the metric describes successful file requests, not
program starts or people. The XML remains excluded from the website log, so it
does not change website page views, visits or `visitor-count.json`.

Review the monitoring-path list against the real server before activation.
When a new health endpoint or asset family is added, update the filter first.

Test the complete Nginx configuration before reloading it:

```sh
sudo nginx -t
```

### Log rotation

`/etc/logrotate.d/hamradioonline-analytics` rotates both dedicated logs daily,
retains 14 rotations and compresses older files. `delaycompress`
is an operational requirement: it keeps the immediately preceding rotation
as an uncompressed `.1` file for the next generator run. The `create 0640
www-data hamradio-analytics` directive preserves the write/read boundary.
After rotation, `invoke-rc.d nginx rotate` makes Nginx reopen its logs.

The generator processes, in this order:

1. each optional, uncompressed `.1` rotation;
2. each corresponding current log.

A missing `.1` is normal, including before the first rotation. Older `.gz`
files are retained according to Logrotate but are not imported by the regular
generator. A registry path that identifies `.1`, another numbered rotation or
a `.gz` file is rejected.

## GoAccess reports

The template enables IP anonymisation at `anonymize-level 2` before persistent
aggregation, ignores crawlers, keeps 395 days, and uses a separate persistent
database for every site and the combined report. It leaves only the panels
needed here: visits by day, requested pages, countries, HTTP status codes and
virtual hosts. Host,
remote-user, referrer, keyphrase, operating-system, browser and other detailed
panels are disabled.

GoAccess 1.8.1 writes the Country panel under the JSON key `geolocation`.
Combined jobs explicitly pass `--enable-panel=VIRTUAL_HOSTS` and require the
resulting `vhosts` key. Site jobs do not enable that panel. The generator treats
either missing key as an invalid report rather than publishing incomplete
statistics.

The GoAccess 1.8.1 JSON keys are an interface invariant. `geo_location` and
`virtual_hosts` are invalid names and may appear in repository tests only as
deliberately rejected negative cases. The Country panel comes from the
configured MMDB; missing `geolocation` invalidates every report, and missing
`vhosts` invalidates the combined report.

The Country database is provided through the registry at
`/var/lib/GeoIP/GeoLite2-Country.mmdb`. A file whose name contains `City` is
rejected. Do not replace it with a City database merely because one happens to
be available.

The generator supplies `--persist`, conditionally supplies `--restore`, and
uses an isolated `--db-path` through the rendered template. It passes the
uncompressed `.1` rotation, when present, and then the current log as direct
GoAccess arguments. This chronological order also covers entries appended
shortly before rotation. GoAccess tracks the processed files in its persistent
state and processes only new entries on later runs. The first successful run
creates each database. Later runs copy the last valid database into staging,
restore it and persist the updated result only after all reports have
succeeded.

Logrotate must use `delaycompress`, as shown in the example. This leaves `.1`
uncompressed for one rotation cycle. Older `.gz` files are not part of the
regular hourly run. The explicit historical-import mode can read them through
Node.js; it never asks the GoAccess binary to decompress them. Do not add an
incremental decompression pipeline to the timer service.

If the generator is unavailable for longer than the uncompressed rotation
window, the regular run cannot recover entries found only in older `.gz`
files. Preserve those files under the raw-log retention policy and plan any
necessary historical import separately before resuming normal processing.

### Durable private aggregates

For each covered day, the generator rebuilds the relevant slice from the
available reduced logs and replaces or monotonically extends the stored value.
It does not add a whole hourly result to the previous result. Website page
views use the same Nginx page/bot filters and the same GoAccess crawler
classification as the existing reports. The generator verifies that the sum
of all path values equals GoAccess's request total; a discrepancy stops the run
instead of silently establishing a second page-view definition.

The website series stores page-view totals, absolute Country-panel values and
every normalized path per day. The update-information series stores successful
request totals and absolute Country-panel values per day. A missing Country
assignment is stored as `Unknown`; no city or exact location is inferred.
Hourly update-request totals and conservative client groups are kept only for
the latest 14 calendar days in `Europe/Berlin`. Raw user agents never enter the
durable metric state. A Java user agent is labelled as an unknown Java
application, not as KST4Contest.

Annual totals, annual Country totals and annual page totals are calculated from
the daily state when the static reports are generated. The first covered day
is shown for every series. Earlier dates are unknown and are not emitted as
zero. There is deliberately no permanent path-by-Country-by-request table.

### Visit and privacy boundary

GoAccess treats requests with the same IP address, date and user agent as one
visit. The public number is therefore an approximate visit total, not a count
of uniquely identified people. Page views remain a separate statistic and are
displayed separately from the durable daily visit values.

IP addresses are processed with the configured GoAccess anonymisation level.
Country resolution happens locally against GeoLite2-Country; City and host
statistics are not produced. No visitor address is sent to MaxMind or another
analytics service. The website sets no analytics cookie, embeds no external
tracking script and sends no visit to Google Analytics, Matomo Cloud or any
other analytics platform.

The home-page script requests only `/visitor-count.json` from the same origin.
The public file contains `schemaVersion`, `visits`, `since` and `updatedAt` and
no visitor-level or daily detail.

## Persistence and publication

The installation has five distinct persistence layers.

### Raw logs

The current website and update-information logs and their rotations are
short-lived input. Each current file and `.1` bridge requests across the most
recent rotation. These files contain individual IP addresses, timestamps and
raw user agents. Logrotate limits their retention to the published 14-day
policy; backups must not silently extend that period.

### GoAccess databases

Persistent detail state lives in:

- `/var/lib/hamradioonline-analytics/db/kst4contest`;
- `/var/lib/hamradioonline-analytics/db/combined`.

`--persist` writes the processing state and `--restore` loads it on later runs.
This lets GoAccess process new log content without adding the same input from
scratch on every hourly run. The `keep-last 395` setting limits detailed
aggregates to a rolling 395 days.

### Public counter state

`/var/lib/hamradioonline-analytics/public-counter-state.json` stores daily
visit values for each enabled site from `activatedOn` onward. The generator
replaces a day's value when it is processed again; it does not add the value a
second time. This file is the durable business source for the lifetime public
total, including days which have aged out of the GoAccess detail database.

The stored hostname and `activatedOn` must continue to match the registry.
Changing either value requires a planned migration or an approved reset, not
an ad-hoc edit or deletion of the state file.

### Private metric state

`/var/lib/hamradioonline-analytics/private-metrics-state.json` is the durable
source for daily website page views and update-information requests. It stores
daily totals and absolute Country values; website days also store normalized
path totals. Only update-information days within the latest 14-day window may
contain hourly and client-group aggregates. The file contains no individual
IP address, raw user agent or individual timestamp and remains unreadable by
Nginx.

Daily values are upserted, not added. Re-reading the current log, `.1` or an
overlapping historical import therefore does not multiply a day. A conflicting
or decreasing overlap stops the run for investigation. Keep this file with
the public counter state and GoAccess databases in the later separate
backup/recovery plan.

### Reports and public files

The derived outputs are:

- `/var/lib/hamradioonline-analytics/reports/kst4contest/report.html`;
- `/var/lib/hamradioonline-analytics/reports/kst4contest/report.json`;
- `/var/lib/hamradioonline-analytics/reports/combined/report.html`;
- `/var/lib/hamradioonline-analytics/reports/combined/report.json`;
- `/var/lib/hamradioonline-analytics/reports/metrics/report.html` and its
  per-site daily/yearly pages;
- `/var/lib/hamradioonline-analytics/public/kst4contest/visitor-count.json`.

The generator prepares every GoAccess job in a run directory, validates the
HTML and JSON outputs, and calculates counter updates before publication. A
GoAccess or report-validation failure therefore leaves the published files
unchanged. Report, state and public files are replaced atomically one file at a
time. Database directories are exchanged through a temporary backup name and
restored if that exchange fails. These replacements are not one filesystem
transaction across every report, database and counter file; after a storage or
permission failure during publication, inspect the complete set and rerun the
service after correcting the cause.

Counter state, private metric state and GoAccess databases are the important persistent sources.
HTML/JSON reports and `visitor-count.json` are derived and can be rebuilt when
their corresponding source state is available.

## Checking and running

Do not rely on `node --check` alone. First inspect the installed generator's
owner, mode and plausible non-zero size, then compare its SHA-256 digest with
the reviewed repository file:

```sh
sudo stat -c '%U:%G %a %s %n' \
  /opt/hamradioonline-analytics/generate-reports.js \
  /opt/hamradioonline-analytics/private-metrics.js
sha256sum /srv/git/kst4contest/website/ops/analytics/generate-reports.js \
  /opt/hamradioonline-analytics/generate-reports.js \
  /srv/git/kst4contest/website/ops/analytics/private-metrics.js \
  /opt/hamradioonline-analytics/private-metrics.js
/usr/bin/node --check /opt/hamradioonline-analytics/generate-reports.js
/usr/bin/node --check /opt/hamradioonline-analytics/private-metrics.js
```

A zero-byte JavaScript file is syntactically valid and exits successfully
without doing any work. File size, digest and the expected completion message
are therefore part of every recovery check.

Validate paths, registry values, the template contract and GoAccess
availability without producing reports:

```sh
sudo -u hamradio-analytics /usr/bin/node \
  /opt/hamradioonline-analytics/generate-reports.js \
  --registry /etc/hamradioonline-analytics/sites.json \
  --config-template /etc/hamradioonline-analytics/goaccess.conf.template \
  --check
```

The check prints the detected GoAccess version and whether GeoIP2/MMDB,
OpenSSL and Zlib build options are present. Missing GeoIP2/MMDB support is a
configuration error with exit code 2. OpenSSL remains informational. Missing
Zlib is supported for this operating mode and does not make the check fail.
The same message explains that only the current log and optional uncompressed
`.1` are processed and that older `.gz` files are not imported.

Exercise the complete GoAccess and output-validation path without changing
published reports, databases or counter state:

```sh
sudo -u hamradio-analytics /usr/bin/node \
  /opt/hamradioonline-analytics/generate-reports.js \
  --registry /etc/hamradioonline-analytics/sites.json \
  --config-template /etc/hamradioonline-analytics/goaccess.conf.template \
  --dry-run
```

Run without either flag to publish. A lock prevents concurrent production
runs. A dry-run uses a temporary working directory and deliberately neither
needs nor creates the production lock below `/run`. Configuration errors use
exit code 2, failure to acquire the production lock uses exit code 3, and
generation or publication errors use exit code 1.

Before the first production run, seed any earlier daily values which must be
preserved into `public-counter-state.json`. There is no honest way to recreate
history which is no longer present in the raw logs. Back up this state file: it
is the durable source for public totals older than the detailed retention
window.

### Historical import

Historical import is a manual maintenance operation. Do not add import options
to the systemd unit. First make a protected working copy of the still available
regular Nginx access logs, including `.1` and `.gz`, and inventory their actual
first and last records. Use only a continuous date range which is genuinely
covered by the selected files. A missing earlier file is missing history, not a
zero day.

The importer accepts either the standard Nginx combined format or the reduced
tab-separated analytics format. It rejects malformed lines, unreadable files,
unknown sites and invalid coverage ranges. Do not guess a production log
format: compare a redacted sample with the selected parser before the import.
For a standard combined-log import:

```sh
sudo -u hamradio-analytics /usr/bin/node \
  /opt/hamradioonline-analytics/generate-reports.js \
  --registry /etc/hamradioonline-analytics/sites.json \
  --config-template /etc/hamradioonline-analytics/goaccess.conf.template \
  --import-site kst4contest \
  --import-format nginx-combined \
  --coverage-from YYYY-MM-DD \
  --coverage-through YYYY-MM-DD \
  --import-log /protected/import/access.log.3.gz \
  --import-log /protected/import/access.log.2.gz \
  --import-log /protected/import/access.log.1
```

Use `analytics-tsv` only when the source is genuinely in the maintained
reduced format. Query strings are removed and paths normalized by the importer.
Website requests pass the same maintained page and known-bot filters and then
GoAccess's crawler classification. Update-information requests require exact
`GET`/`200` semantics and do not exclude bots.

The selected input set is aggregated by complete day. Duplicate copies of the
same records across selected files are collapsed while repeated identical
records within one source remain counted. Each imported day replaces or
monotonically extends its stored aggregate; rerunning the same command is
idempotent. An overlap which changes distributions without a consistent higher
total fails instead of adding uncertain data. Run once against a protected copy
of the private state or with `--dry-run`, inspect the first covered dates and
totals, then run productively. Keep a pre-import backup until a second identical
run confirms stable totals.

Node.js decompresses `.gz` inputs itself. The production GoAccess 1.8.1 binary
does not need Zlib support. Remove the protected import copies according to the
14-day raw-data limit after the verified import; do not put them in a durable
backup.

### Safe verification sequence

Use this order for a new installation, a recovered service or a material
generator/configuration update:

1. Check generator size, ownership, mode and SHA-256 against the reviewed
   checkout.
2. Run `/usr/bin/node --check` on the installed generator.
3. Run the generator with `--check` as `hamradio-analytics`.
4. Confirm the reported GoAccess version and GeoIP2/MMDB, OpenSSL and Zlib
   capability state. Missing Zlib is expected; missing MMDB support is not.
5. Record the hashes and timestamps of current reports, databases and counter
   files, then run `--dry-run`.
6. Confirm that the recorded production files did not change and that
   `/run/hamradioonline-analytics/generator.lock` was not created by the
   dry-run.
7. Start one productive run through the service unit:
   `sudo systemctl start hamradioonline-analytics.service`.
8. Inspect `Result` and `ExecMainStatus` and read the unit journal. A successful
   run ends with `Analytics generation completed`.
9. Check every generated file's path, owner, group, mode and timestamp.
10. Request the public JSON through HTTPS and validate its four fields.
11. Request the existing GoAccess URLs and `/metrics/` with Basic Auth. Follow
    its website and update-information daily/yearly links. Let the client prompt
    for the password; never put it directly on a command line.
12. Run the service a second time and confirm that the total and report values
    develop plausibly rather than multiplying the existing history.
13. Enable or re-enable the timer only after these checks pass.
14. Confirm the first automatic run in the journal and later verify the first
    real log rotation separately.

Useful service checks are:

```sh
systemctl show hamradioonline-analytics.service \
  -p Result -p ExecMainStatus
journalctl -u hamradioonline-analytics.service --since today
systemctl status hamradioonline-analytics.timer
systemctl list-timers hamradioonline-analytics.timer
```

## systemd operation

The production oneshot service runs as `hamradio-analytics` with
`UMask=0027`. Its sandbox exposes `/etc/hamradioonline-analytics`,
`/opt/hamradioonline-analytics`, the Nginx logs and the Country MMDB read-only.
`/var/lib/hamradioonline-analytics` is its only application-state write area;
`/run/hamradioonline-analytics` holds the production lock
`generator.lock`. The service has no network access and only the documented
read/write paths.

The timer uses `OnCalendar=hourly`, `Persistent=true`,
`RandomizedDelaySec=4m` and `AccuracySec=1m` and is permanently enabled in
production. Multiple automatic hourly runs have completed successfully. Each
run produces one site report, one combined report and one public counter,
takes roughly one second on the current installation and has shown stable
incremental behaviour without sudden duplicate counting.

If the installed GoAccess build unexpectedly requires network access, find the
reason before weakening that restriction; local log processing and a local
Country database do not require it.

## Report access

The statistics vhost serves static files over HTTPS and protects the complete
host with HTTP Basic Authentication. This includes `/`, its redirect to
`/combined/`, and every individual report. Store the password file outside
this repository. Prepare it so only root and the Nginx group can access it:

```sh
sudo install -d -o root -g www-data -m 0750 /etc/nginx/htpasswd
if [ -e /etc/nginx/htpasswd/hamradioonline-analytics ]; then
  sudo htpasswd /etc/nginx/htpasswd/hamradioonline-analytics stats-reader
else
  sudo htpasswd -c /etc/nginx/htpasswd/hamradioonline-analytics stats-reader
fi
sudo chown root:www-data /etc/nginx/htpasswd/hamradioonline-analytics
sudo chmod 0640 /etc/nginx/htpasswd/hamradioonline-analytics
```

Use the local account name `stats-reader` and enter its password interactively.
Never store the resulting password hash in this repository or an installation
ZIP.
The example opens no GoAccess WebSocket and no additional GoAccess port. Its
own access log is disabled and responses use a private, no-store cache policy.

The current private endpoints are:

- `https://stats.hamradioonline.de/`, which redirects an authenticated request
  to `/combined/`;
- `https://stats.hamradioonline.de/combined/`, which serves the combined
  report;
- `https://stats.hamradioonline.de/kst4contest/`, which serves the site report.
- `https://stats.hamradioonline.de/metrics/`, which links to the separate
  website and update-information daily/yearly views.

The generator adds a small `/metrics/` link to each generated GoAccess HTML
report. The authenticated `/` redirect to `/combined/` remains unchanged.

All HTTPS paths, including the redirect target, remain behind Basic Auth.
Reports use `Cache-Control: private, no-store`,
`X-Content-Type-Options: nosniff` and `X-Frame-Options: DENY`; dotfiles are
blocked. The vhost has no access log and no analytics log. HTTP remains open
only for the ACME webroot and permanently redirects every other request to
HTTPS. No IPv6 listener is configured while DNS AAAA operation remains
unconfirmed.

The final HTTP/HTTPS vhost is the production configuration. The HTTP-only
bootstrap template is retained solely for first provisioning or recovery when
the certificate files do not yet exist. In that situation, do not activate the
final HTTPS vhost first. Install the temporary bootstrap without changing the
parallel APT Certbot installation or either renewal timer:

```sh
sudo install -d -o root -g root -m 0755 /var/lib/letsencrypt
sudo install -o root -g root -m 0644 \
  website/ops/analytics/nginx/stats-vhost-http-bootstrap.conf.example \
  /etc/nginx/sites-available/stats.hamradioonline.de
if [ ! -e /etc/nginx/sites-enabled/stats.hamradioonline.de ] && \
   [ ! -L /etc/nginx/sites-enabled/stats.hamradioonline.de ]; then
  sudo ln -s /etc/nginx/sites-available/stats.hamradioonline.de \
    /etc/nginx/sites-enabled/stats.hamradioonline.de
fi
sudo nginx -t
sudo systemctl reload nginx
sudo /snap/bin/certbot certonly --webroot \
  --webroot-path /var/lib/letsencrypt \
  -d stats.hamradioonline.de
```

Only after Certbot has created the certificate, replace the bootstrap with the
final vhost. This does not remove HTTP completely. The final file keeps an
IPv4 port 80 block for `/.well-known/acme-challenge/` so the certificate issued
with `--webroot` can be renewed automatically. Every other HTTP request is
redirected permanently to the same URI on HTTPS. The HTTPS block uses the
existing Ubuntu/Certbot TLS files
`/etc/letsencrypt/options-ssl-nginx.conf` and
`/etc/letsencrypt/ssl-dhparams.pem`:

```sh
sudo install -o root -g root -m 0644 \
  website/ops/analytics/nginx/stats-vhost.conf.example \
  /etc/nginx/sites-available/stats.hamradioonline.de
sudo nginx -t
sudo systemctl reload nginx
```

After activation, verify both authentication and renewal from the server:

```sh
curl -I https://stats.hamradioonline.de/
curl -I -u stats-reader https://stats.hamradioonline.de/
sudo /snap/bin/certbot renew --dry-run \
  --cert-name stats.hamradioonline.de
```

The first HTTPS request must return `401`. The authenticated request must
return the redirect to `/combined/`. Enter the Basic Auth password
interactively; do not put it on the command line. The Certbot dry-run must
complete while the final vhost is active.

The templates listen on IPv4 only. Add an IPv6 listener later, after the AAAA
record for `stats.hamradioonline.de` has been confirmed and tested.

The public counter location serves only `visitor-count.json` through an exact
Nginx `alias`; it does not modify the deployed Eleventy release directory. Its
own access log is disabled. Responses use
`Cache-Control: public, max-age=3600` and
`X-Content-Type-Options: nosniff`. The file contains the schema version, total,
activation date and update time. It contains no IP address, user agent,
hostname or per-day detail.

The production URL is
`https://kst4contest.hamradioonline.de/visitor-count.json`. Before the first
successful generator run, a `404` is expected; afterwards it must return
`200` with `application/json`. The home page reveals the counter only after a
valid response. A missing, invalid or unavailable counter never prevents the
rest of the static site from working.

## Deployment boundary

GitHub is the source repository for the website and the reviewed analytics
templates. The production checkout is `/srv/git/kst4contest`. A root cron job
runs `/srv/scripts/deploy-kst4contest-website.sh` every five minutes. It fetches
Git, resets the checkout to `origin/main`, runs `npm ci`, builds the Eleventy
website, validates VersionInfo, synchronises the result to
`/srv/www/kst4contest/current` and finally restores the ownership expected by
Nginx.

The deployment credential is stored in `/etc/kst4contest-website.env`. Its
value and account assignment must never appear in documentation, logs or
support output.

This automatic deployment updates only the static website. It does not install
or overwrite:

- `/opt/hamradioonline-analytics`;
- `/etc/hamradioonline-analytics`;
- systemd units;
- Nginx or Logrotate configuration;
- the Basic Auth password file;
- GeoIP configuration;
- Certbot configuration.

Changes to those operational files require a separate review and manual
installation. The published privacy notice must remain in place whenever
analytics logging is active.

## Regular operation

Nginx continuously writes eligible website requests and exact successful
update-information requests to separate reduced logs. The systemd timer starts
the generator once per hour. Every successful run refreshes the per-site and
combined GoAccess reports, private daily/yearly views, daily visit counter
values and enabled public counters. Logrotate handles both raw logs once per
day and preserves each uncompressed `.1` handover file required by the
generator.

The normal operator signal is the service result and journal, not a permanently
running process: the generator is a short-lived oneshot service. There is no
GoAccess WebSocket process and no public GoAccess port.

## External services and local credentials

### MaxMind GeoLite2

MaxMind is used only to download and update GeoLite2-Country. The local updater
configuration is `/etc/GeoIP.conf` with mode `0600 root:root`; the local
database is `/var/lib/GeoIP/GeoLite2-Country.mmdb`. GeoLite is enabled and the
server has an Account ID and License Key, but neither value belongs in Git,
this runbook, screenshots, logs or ordinary diagnostic output.

`geoipupdate.service` and `geoipupdate.timer` download updates from MaxMind.
All visitor lookups then happen locally. The analytics application never sends
an individual visitor address to MaxMind.

If the MMDB is missing, stale or unreadable, inspect the timer and journal,
check the file mode and run the updater directly if required:

```sh
systemctl status geoipupdate.timer geoipupdate.service
journalctl -u geoipupdate.service --since today
sudo stat -c '%U:%G %a %s %y %n' \
  /etc/GeoIP.conf /var/lib/GeoIP/GeoLite2-Country.mmdb
sudo geoipupdate
sudo -u hamradio-analytics test -r \
  /var/lib/GeoIP/GeoLite2-Country.mmdb
```

When `mmdblookup` is installed, a lookup of a neutral public test address can
confirm database readability without using any visitor address:

```sh
sudo -u hamradio-analytics mmdblookup \
  --file /var/lib/GeoIP/GeoLite2-Country.mmdb \
  --ip 1.1.1.1 country iso_code
```

An unreadable Country MMDB or GoAccess without MMDB support makes `--check`
fail with exit code 2.

### Let's Encrypt

Let's Encrypt supplies the TLS certificate for `stats.hamradioonline.de`. Its
state is below `/etc/letsencrypt/live/stats.hamradioonline.de/`. The ACME
webroot is `/var/lib/letsencrypt`, and the final Nginx vhost permanently serves
`/.well-known/acme-challenge/` over IPv4 port 80 so webroot renewal continues
to work.

Use the Snap client explicitly as `/snap/bin/certbot` for this vhost.
Certificate issuance and a renewal dry-run have been confirmed. The server
currently also has an APT Certbot installation and both renewal timers. That
duplication is a separate server-maintenance issue; do not change either
installation as part of analytics maintenance.

ACME account data, private keys and certificate state live only on the server
and in a future protected backup. Validate renewal with:

```sh
sudo /snap/bin/certbot renew --dry-run \
  --cert-name stats.hamradioonline.de
```

### GitHub

GitHub supplies the source repository consumed by the website deployment. It
is not an analytics processor and receives no individual analytics request or
visit data. The deployment credential remains in
`/etc/kst4contest-website.env`; only the server-side deploy process needs it.

### No external analytics platform

Statistics are generated locally on the project server. There is no Google
Analytics, Matomo Cloud service, external tracking script or transfer of
individual visits to an analytics provider.

## Troubleshooting

Start with the safe verification sequence above. Keep secrets out of commands
and captured output, and do not weaken file modes merely to make a check pass.

### `GoAccess JSON report has no geo_location panel`

The expected GoAccess 1.8.1 key is `geolocation`. An installed older generator
which expects `geo_location` must be replaced with the reviewed repository
version.

### Missing Virtual Hosts panel

The combined report requires the JSON key `vhosts` and must start GoAccess with
`--enable-panel=VIRTUAL_HOSTS`. `virtual_hosts` is not a valid replacement.
Site reports do not require this panel.

The names `geo_location` and `virtual_hosts` are allowed in repository tests
only as deliberately invalid negative cases.

### Missing `.1` rotation

This is normal before the first rotation and whenever no previous rotation is
present. The current analytics log remains required.

### `.gz` rotations on a GoAccess build without Zlib

This is normal. Regular operation does not read `.gz` files. Do not configure
a compressed or rotated file as `analyticsLog` or `updateInfo.analyticsLog`.
Use `.gz` only through the explicit historical-import options; Node.js, not
GoAccess, decompresses that input.

### MMDB missing or unreadable

`--check` must fail. Verify `/etc/GeoIP.conf`, the GeoIP updater units,
`/var/lib/GeoIP/GeoLite2-Country.mmdb` and the service account's read access.
Run `geoipupdate` and a neutral local lookup as described above when needed.

### Analytics log missing or unreadable

Compare the registry path with the additional `access_log` directive in the
site vhost. Check the active Nginx configuration and verify
`www-data:hamradio-analytics 0640`. Test Nginx before reloading it. Do not
replace or repurpose the normal operational access log.

```sh
sudo nginx -t
sudo nginx -T
sudo stat -c '%U:%G %a %s %y %n' \
  /var/log/nginx/kst4contest-analytics.log \
  /var/log/nginx/kst4contest-update-information.log
sudo -u hamradio-analytics test -r \
  /var/log/nginx/kst4contest-analytics.log
sudo -u hamradio-analytics test -r \
  /var/log/nginx/kst4contest-update-information.log
```

### Private metrics gap

If the journal says that a private metric gap starts before the regular
current/`.1` handover window, stop the timer. Do not turn the missing dates
into zeros and do not delete `private-metrics-state.json`. Inventory the
remaining regular and reduced logs, make a protected state backup and use the
documented historical import only for a genuinely covered range. If no
reliable source remains, the first covered date must move forward through a
reviewed state migration; that is not an automatic repair.

### Historical import rejects a log

An invalid line normally means that the selected `nginx-combined` or
`analytics-tsv` parser does not match the real file, or that a file is damaged.
Inspect a redacted sample and the file boundaries. Do not delete failing lines
or switch formats until the actual Nginx log format is confirmed. A failed
import leaves the existing private state and reports unchanged.

### Public counter returns `404`

This is expected before the first successful generation. Afterwards inspect
the service journal, public-output directory, `0644` file mode, Nginx include
and exact alias path. A successful response is `200 application/json`.

### Statistics vhost returns `401` or `404`

`401` without credentials is correct. With the valid Basic Auth login, `/`
must redirect to `/combined/`. A `404` after authentication usually means that
the generator has not produced `report.html`, the URL and report directory do
not match, or Nginx cannot traverse/read the report path.

### Certificate error

Check the DNS A record, certificate paths, active Nginx vhost, ACME webroot and
Snap Certbot renewal. Immediately after an Nginx reload, wait briefly and
retry if observations conflict, then inspect `nginx -T`, the Nginx journal and
worker start times.

### Generator lock error

Exit code 3 means that the production lock could not be acquired. Check the
service, timer and running processes. Do not remove the lock blindly. Remove it
only after confirming that no generator process is active and that the lock is
genuinely stale.

### Counter identity mismatch

If `activatedOn` or the hostname differs from existing counter state, stop.
Do not repair this by deleting the state. Establish the cause and approve a
migration or reset explicitly.

### Exit code 0 but no output

Check the installed generator size, owner, mode and SHA-256 before anything
else. Also look for `Analytics generation completed` in the journal and inspect
the expected output files.

A known operator error is copying a shell prompt or continuation marker `>`
with a command. Bash interprets a stray `>` as output redirection. This can
truncate `generate-reports.js` to zero bytes and create empty files whose names
look like command options. A zero-byte JavaScript file still passes
`node --check` and exits with code 0 while doing nothing.

Identify such artifacts precisely before removing them. Reinstall the
generator from the reviewed checkout with the documented owner and mode,
compare its size and SHA-256, then repeat `--check` and `--dry-run`. Do not make
one-off artifact names or old checksums part of the permanent procedure.

## Monitoring

The systemd timer is active, and service results and errors are visible through
systemd and the journal. No separate alerting channel has been confirmed.
Routine checks should confirm:

- the timer is active and waiting with a future trigger;
- the last service result is `success` and `ExecMainStatus=0`;
- the journal contains `Analytics generation completed`;
- report and counter timestamps continue to advance;
- the public total changes plausibly;
- repeated runs do not multiply the existing history.

Central alerting can be handled later as part of the general server operations
plan.

## Backup and recovery

There is currently no comprehensive automated server backup plan. The
analytics installation must be included explicitly when that server-wide
backup and recovery design is implemented. Creating that backup system is not
part of this repository change.

### Data to include

At minimum, the later plan must cover:

- `/var/lib/hamradioonline-analytics/public-counter-state.json`;
- `/var/lib/hamradioonline-analytics/private-metrics-state.json`;
- `/var/lib/hamradioonline-analytics/db`;
- `/etc/hamradioonline-analytics`;
- the installed systemd units;
- Nginx analytics and vhost configuration;
- `/etc/logrotate.d/hamradioonline-analytics`;
- `/etc/nginx/htpasswd/hamradioonline-analytics`;
- Certbot and Let's Encrypt account/certificate state;
- `/etc/GeoIP.conf`;
- the website deployment configuration and credential storage.

Handle the Basic Auth hash, MaxMind Account ID and License Key, GitHub deploy
token, ACME account data and private TLS keys as secrets. Never copy them into
Git, public documentation, logs or ordinary support bundles.

The generator modules are recoverable from GitHub, and GeoLite2-Country can be
fetched again with `geoipupdate`. HTML/JSON reports can be rebuilt when their
GoAccess databases or durable state remain. The public JSON can be rebuilt
from the counter state; the private daily/yearly pages can be rebuilt from
`private-metrics-state.json`.

The accumulated public total is not fully recoverable without
`public-counter-state.json`. Older detailed aggregates are not recoverable
without the GoAccess databases, and historical raw requests disappear after
the 14-day rotation window.

The durable website page-view and update-information history is not fully
recoverable without `private-metrics-state.json`. This state is aggregated and
belongs in the protected backup plan. Do not let backups extend the published
raw-log retention by accident: exclude raw logs or enforce the same 14-day
maximum in backup storage. Counter state and anonymised or aggregated GoAccess
state can be governed separately.

### Recovery order

1. Install the operating-system packages and external dependencies.
2. Recreate the service account and group relationships.
3. Create the directories with the documented owners and modes.
4. Install the generator and non-secret configuration.
5. Restore secrets and certificate state from protected backup storage.
6. Restore GeoLite2-Country or download it again.
7. Restore the GoAccess databases, public counter state and private metric
   state.
8. Validate Nginx, systemd and Logrotate configuration.
9. Run the generator with `--check`.
10. Run `--dry-run` and verify that production state remains unchanged.
11. Start one productive run through the systemd service.
12. Verify reports, public counter and Basic Auth.
13. Enable the timer only after every preceding check succeeds.

## Outstanding operational checks

The first real rotation after installing the separate update-information log
still needs explicit observation. This is not a current service blocker. For
both reduced logs, confirm:

- a new current log exists;
- `.1` exists and remains uncompressed;
- both files retain the expected owners and modes;
- `hamradio-analytics` can read both files;
- the next generator run succeeds;
- values do not show duplicate counting;
- the older rotation is compressed on the following cycle as intended.

The general server backup/recovery implementation and any central alerting
remain separate future operations tasks.

The repository contains no password, password hash, MaxMind credential,
deployment token, server IP address, TLS private key or private backup
destination. Keep it that way.
