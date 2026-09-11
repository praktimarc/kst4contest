# Server-side website statistics

This directory contains installation examples for the confirmed Ubuntu 24.04
server baseline and privacy-conscious traffic statistics. Nothing here
installs or activates the production service automatically.

The design has two separate outputs:

- private static GoAccess HTML and JSON reports for each registered project
  subdomain and for all registered project subdomains combined;
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

## Files

- `generate-reports.js` validates configuration and state, runs GoAccess and
  publishes outputs atomically.
- `sites.example.json` is the registry template.
- `goaccess.conf.template` is rendered per report with a private database path
  and the configured GeoIP2 Country database.
- `nginx/` contains the reduced log format, request filters, public endpoint
  and protected report-vhost examples.
- `systemd/` contains a hardened oneshot service and hourly timer.
- `logrotate/` retains 14 daily analytics-log rotations.

## Prerequisites

- Node.js 18.19.1 or newer;
- GoAccess with GeoIP2/MMDB support;
- one GeoIP2 **Country** database, not a City database;
- Nginx;
- an unprivileged service account, shown as `hamradio-analytics` in the
  examples.

No npm package is required by the generator. GoAccess is the only external
program it starts.

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
  /var/lib/hamradioonline-analytics/public \
  /var/lib/hamradioonline-analytics/public/kst4contest
```

Generated HTML and JSON reports use mode `0640`. The public
`visitor-count.json` uses `0644`. Private GoAccess databases and
`public-counter-state.json` remain owned by `hamradio-analytics` and unreadable
by Nginx. The service unit uses `StateDirectoryMode=0711` to retain this
boundary after systemd has prepared the state directory.

Create the analytics log only when it does not already exist. Running
`install /dev/null` unconditionally would empty an existing log:

```sh
if [ ! -e /var/log/nginx/kst4contest-analytics.log ]; then
  sudo install -o www-data -g hamradio-analytics -m 0640 /dev/null \
    /var/log/nginx/kst4contest-analytics.log
fi
sudo stat -c '%U:%G %a %n' /var/log/nginx/kst4contest-analytics.log
```

The resulting log owner and mode must be
`www-data:hamradio-analytics 640`. Logrotate preserves that ownership. The
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
- a `publicCounter` switch;
- the private `reportOutputDirectory`;
- a `publicJsonPath` when the public counter is enabled.

The top-level `combined.reportOutputDirectory` receives the combined report.
Only registered sites are included. The generator rejects
`stats.hamradioonline.de`, so the report host cannot accidentally become part
of the project statistics.

To add another project subdomain later, add one registry entry and one matching
dedicated `access_log` line to its Nginx server block. Do not enable a public
counter unless that site should publish one.

Treat `activatedOn` as persistent data. Once counting has started, changing it
would change the meaning of the total. The generator refuses to combine a new
activation date with existing counter state.

The generator derives the optional `.1` path from `analyticsLog`. It is valid
for `.1` not to exist before the first rotation. Do not enter a rotation or a
compressed `.gz` file in the registry.

## Nginx logging

Install the log-format and filter maps from `nginx/` in the `http` context.
Then add a dedicated analytics `access_log` to every registered project server
block. Keep the existing operational access log unless its replacement has
been reviewed separately. If the operational log is inherited from the
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

It does not contain a referrer, query string or authenticated user name. The
filter accepts only eligible page `GET` requests. It excludes the update feed,
public counter, sitemap, robots file, favicons, CSS, JavaScript, images, fonts,
source maps, manual assets and the listed monitoring paths. Known crawler user
agents are rejected before logging. GoAccess applies its own crawler list as a
second layer and treats unknown browsers or operating systems as crawlers.

Review the monitoring-path list against the real server before activation.
When a new health endpoint or asset family is added, update the filter first.

Test the complete Nginx configuration before reloading it:

```sh
sudo nginx -t
```

## GoAccess reports

The template enables IP anonymisation before persistent aggregation, ignores
crawlers, keeps 395 days, and uses a separate persistent database for every
site and the combined report. It leaves only the panels needed here: visits by
day, requested pages, countries, HTTP status codes and virtual hosts. Host,
remote-user, referrer, keyphrase, operating-system, browser and other detailed
panels are disabled.

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
regular hourly run, and importing them is a separate maintenance task outside
this repository workflow. Do not add an unstable decompression pipeline to
the timer service.

If the generator is unavailable for longer than the uncompressed rotation
window, the regular run cannot recover entries found only in older `.gz`
files. Preserve those files under the raw-log retention policy and plan any
necessary historical import separately before resuming normal processing.

## Checking and running

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
exit code 2, an active production lock uses exit code 3, and generation or
publication errors use exit code 1.

Before the first production run, seed any earlier daily values which must be
preserved into `public-counter-state.json`. There is no honest way to recreate
history which is no longer present in the raw logs. Back up this state file: it
is the durable source for public totals older than the detailed retention
window.

## Scheduling and report access

Install the systemd files as local units after adapting paths and permissions.
The timer runs hourly, catches up after downtime and adds a small random delay.
The service has no network access and only the documented read/write paths.
If the installed GoAccess build unexpectedly requires network access, find the
reason before weakening that restriction; local log processing and a local
Country database do not require it.

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

Choose the account name locally and enter the password interactively. Never
store the resulting password hash in this repository or the installation ZIP.
The example opens no GoAccess WebSocket and no additional GoAccess port. Its
own access log is disabled and responses use a private, no-store cache policy.

Do not activate the final HTTPS vhost before its certificate files exist.
First install the temporary HTTP bootstrap without changing the parallel apt
Certbot installation or either renewal timer:

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

Use this rollout order:

1. Install the server files with explicit modes. Prepare directories, log
   ownership, filters and still-inactive Nginx and systemd configuration.
2. Push the website changes, including the Privacy Policy, and let the existing
   deployment cron job publish them. Until the JSON endpoint exists, the
   visitor count remains hidden automatically.
3. Verify the published Privacy Policy. Only then activate analytics logging,
   the report generator and timer, the public counter location and the
   protected statistics vhost.
4. Run `nginx -t` before every Nginx reload and perform the real `--check` and
   `--dry-run` on the server before the first production generation.

A short period in which the Privacy Policy is already visible but logging is
not yet active is acceptable. Starting analytics logging before publishing the
updated policy is not.

## Retention and recovery

- Dedicated analytics raw logs: 14 days through the Logrotate example.
- Anonymised detailed GoAccess aggregates: rolling 395 days.
- Public daily counter values: retained from activation onward.

Back up the counter state and, if fast report recovery matters, the GoAccess
database directories. Reports themselves are derived output. To recover, stop
the timer, restore the state and database directories with their ownership,
run `--check`, then run `--dry-run` before publishing again.

The repository contains no password, password hash, MaxMind download key,
server IP address, TLS private key or private backup destination. Keep it that
way.
