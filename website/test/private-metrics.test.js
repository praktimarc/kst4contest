const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");
const zlib = require("node:zlib");

const {
    aggregateGoAccessReport,
    clientGroup,
    isEligibleWebsiteRequest,
    isUpdateRequest,
    localParts,
    mergeDay,
    normalizePath,
    parseAnalyticsLine,
    parseCombinedLine,
    parseLogFiles,
    purgeDetails,
    renderReports
} = require("../ops/analytics/private-metrics");

function record(overrides = {}) {
    return {
        method: "GET",
        status: 200,
        path: "/kst4ContestVersionInfo.xml",
        userAgent: "Java/21.0.1",
        ...overrides
    };
}

test("counts only exact successful GET requests for the update-information path", () => {
    assert.equal(isUpdateRequest(record()), true);
    assert.equal(isUpdateRequest(record({ method: "HEAD" })), false);
    assert.equal(isUpdateRequest(record({ status: 304 })), false);
    assert.equal(isUpdateRequest(record({ status: 404 })), false);
    assert.equal(isUpdateRequest(record({ path: "/kst4ContestVersionInfo.xml/" })), false);
    assert.equal(isUpdateRequest(record({ path: "/Kst4ContestVersionInfo.xml" })), false);
});

test("keeps website page and bot exclusions separate from update requests", () => {
    assert.equal(isEligibleWebsiteRequest(record({ path: "/privacy/", userAgent: "Firefox/130" })), true);
    assert.equal(isEligibleWebsiteRequest(record({ path: "/privacy/?source=test", userAgent: "Firefox/130" })), true);
    assert.equal(isEligibleWebsiteRequest(record({ path: "/assets/site.css", userAgent: "Firefox/130" })), false);
    assert.equal(isEligibleWebsiteRequest(record({ path: "/manual/assets/page.png", userAgent: "Firefox/130" })), false);
    assert.equal(isEligibleWebsiteRequest(record({ path: "/privacy/", userAgent: "ExampleBot/1" })), false);
    assert.equal(isEligibleWebsiteRequest(record()), false);
    assert.equal(normalizePath("//docs/../privacy/?source=test"), "/privacy/");
});

test("parses analytics and regular Nginx combined records without query strings", () => {
    const analytics = parseAnalyticsLine(
        'kst4contest.hamradioonline.de\t192.0.2.1\t2026-09-14T23:30:00+02:00\tGET\t/privacy/?x=1\tHTTP/1.1\t200\t42\t"Firefox/130"'
    );
    assert.equal(analytics.date, "2026-09-14");
    assert.equal(analytics.hour, "23");
    assert.equal(analytics.path, "/privacy/");

    const emptyPath = parseAnalyticsLine(
        'kst4contest.hamradioonline.de\t192.0.2.1\t2026-09-14T23:30:00+02:00\tGET\t\tHTTP/1.1\t400\t166\t"-"'
    );
    assert.equal(emptyPath.path, null);
    assert.equal(isEligibleWebsiteRequest(emptyPath), false);

    const combined = parseCombinedLine(
        '192.0.2.2 - - [14/Sep/2026:23:31:00 +0200] "GET /news/?x=1 HTTP/1.1" 200 43 "-" "Mozilla/5.0 Firefox/130"',
        "kst4contest.hamradioonline.de"
    );
    assert.equal(combined.date, "2026-09-14");
    assert.equal(combined.path, "/news/");
    assert.match(combined.line, /\t\/news\/\t/);
    assert.equal(parseCombinedLine("broken", "example.test"), null);
});

test("uses Europe/Berlin across both daylight-saving transitions", () => {
    assert.deepEqual(localParts("2026-03-29T00:30:00Z", "Europe/Berlin"), {
        date: "2026-03-29", hour: "01"
    });
    assert.deepEqual(localParts("2026-03-29T01:30:00Z", "Europe/Berlin"), {
        date: "2026-03-29", hour: "03"
    });
    assert.deepEqual(localParts("2026-10-25T00:30:00Z", "Europe/Berlin"), {
        date: "2026-10-25", hour: "02"
    });
    assert.deepEqual(localParts("2026-10-25T01:30:00Z", "Europe/Berlin"), {
        date: "2026-10-25", hour: "02"
    });
});

test("keeps absolute countries including Switzerland, United Kingdom and unknown", () => {
    const website = aggregateGoAccessReport({
        general: { total_requests: 2245, valid_requests: 5 },
        geolocation: { data: [
            { data: "Europe", hits: { count: 3 }, items: [
                { data: "Germany", hits: { count: 1 } },
                { data: "Switzerland", hits: { count: 1 } },
                { data: "United Kingdom", hits: { count: 1 } }
            ] },
            { data: "Unknown", hits: { count: 1 } }
        ] },
        requests: { data: [
            { data: "/", hits: { count: 2 } },
            { data: "/privacy/", hits: { count: 2 } },
            { data: "/news/", hits: { count: 1 } }
        ] }
    }, "website");
    assert.deepEqual(website.countries, {
        Germany: 1,
        Switzerland: 1,
        "United Kingdom": 1,
        Unknown: 2
    });
    assert.deepEqual(website.paths, { "/": 2, "/privacy/": 2, "/news/": 1 });
    assert.throws(() => aggregateGoAccessReport({
        general: { total_requests: 10, valid_requests: 2 },
        geolocation: { data: [{ data: "Germany", hits: { count: 2 } }] },
        requests: { data: [{ data: "/", hits: { count: 1 } }] }
    }, "website"), /valid-request count differs from the request-panel definition/);

    const updates = aggregateGoAccessReport({
        general: { total_requests: 9, valid_requests: 2 },
        geolocation: { data: [{ data: "Germany", hits: { count: 2 } }] },
        requests: { data: [{ data: "/kst4ContestVersionInfo.xml", hits: { count: 2 } }] }
    }, "updateInfo");
    assert.equal(updates.requests, 2);
    assert.throws(() => aggregateGoAccessReport({
        general: { total_requests: 2, valid_requests: 1 },
        geolocation: { data: [{ data: "Germany", hits: { count: 2 } }] },
        requests: { data: [{ data: "/", hits: { count: 1 } }] }
    }, "website"), /country total exceeds/);
    assert.throws(() => aggregateGoAccessReport({
        general: { total_requests: 1, valid_requests: 1 },
        geolocation: { data: [{ data: "Germany", hits: { count: 1 } }] },
        requests: { data: [{ data: "/unexpected", hits: { count: 1 } }] }
    }, "updateInfo"), /unexpected request path/);
});

test("deduplicates overlapping import files and reads gzip without GoAccess Zlib", () => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "kst4-private-import-"));
    const line = '192.0.2.2 - - [14/Sep/2026:23:31:00 +0200] "GET /news/ HTTP/1.1" 200 43 "-" "Firefox/130"';
    const plain = path.join(root, "access.log.1");
    const compressed = path.join(root, "access.log.2.gz");
    try {
        fs.writeFileSync(plain, `${line}\n${line}\n`);
        fs.writeFileSync(compressed, zlib.gzipSync(`${line}\n`));
        const parsed = parseLogFiles(
            [compressed, plain],
            value => parseCombinedLine(value, "kst4contest.hamradioonline.de"),
            { deduplicateAcrossFiles: true }
        );
        assert.equal(parsed.length, 2);
    } finally {
        fs.rmSync(root, { recursive: true, force: true });
    }
});

test("rejects malformed and unreadable historical input", () => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "kst4-private-invalid-"));
    const invalid = path.join(root, "access.log");
    const invalidGzip = path.join(root, "access.log.gz");
    try {
        fs.writeFileSync(invalid, "not an access-log record\n");
        fs.writeFileSync(invalidGzip, "not gzip");
        assert.throws(() => parseLogFiles(
            [invalid],
            value => parseCombinedLine(value, "kst4contest.hamradioonline.de"),
            { label: "historical import log" }
        ), /invalid line/);
        assert.throws(() => parseLogFiles(
            [invalidGzip],
            value => parseCombinedLine(value, "kst4contest.hamradioonline.de")
        ), /could not read compressed log/);
        assert.throws(() => parseLogFiles(
            [path.join(root, "missing.log")],
            value => parseCombinedLine(value, "kst4contest.hamradioonline.de")
        ));
    } finally {
        fs.rmSync(root, { recursive: true, force: true });
    }
});

test("merges repeated and overlapping daily aggregates without addition", () => {
    const current = { pageViews: 2, countries: { Germany: 2 }, paths: { "/": 2 } };
    assert.deepEqual(mergeDay(current, { ...current }, "pageViews", "test"), current);
    assert.deepEqual(mergeDay({
        pageViews: 2,
        countries: { Germany: 1, Switzerland: 1 },
        paths: { "/": 1, "/privacy/": 1 }
    }, {
        pageViews: 2,
        countries: { Switzerland: 1, Germany: 1 },
        paths: { "/privacy/": 1, "/": 1 }
    }, "pageViews", "test").pageViews, 2);
    assert.deepEqual(mergeDay(current, {
        pageViews: 3,
        countries: { Germany: 2, Switzerland: 1 },
        paths: { "/": 2, "/privacy/": 1 }
    }, "pageViews", "test"), {
        pageViews: 3,
        countries: { Germany: 2, Switzerland: 1 },
        paths: { "/": 2, "/privacy/": 1 }
    });
    assert.throws(() => mergeDay(current, {
        pageViews: 3,
        countries: { Germany: 1, Switzerland: 2 },
        paths: { "/": 1, "/privacy/": 2 }
    }, "pageViews", "test"), /not monotonic/);
});

test("removes hourly and client detail after 14 days but preserves daily totals", () => {
    const site = {
        updateInfo: {
            daily: {
                "2026-08-31": { requests: 2, countries: { Germany: 2 }, hours: { "10": 2 }, clients: { Java: 2 } },
                "2026-09-01": { requests: 3, countries: { Germany: 3 }, hours: { "11": 3 }, clients: { Java: 3 } },
                "2026-09-14": { requests: 1, countries: { Unknown: 1 }, hours: { "12": 1 }, clients: { Other: 1 } }
            }
        }
    };
    purgeDetails(site, "2026-09-14", 14);
    assert.equal(site.updateInfo.daily["2026-08-31"].requests, 2);
    assert.equal(site.updateInfo.daily["2026-08-31"].hours, undefined);
    assert.deepEqual(site.updateInfo.daily["2026-09-01"].hours, { "11": 3 });
    assert.deepEqual(site.updateInfo.daily["2026-09-14"].clients, { Other: 1 });
});

test("renders annual sums and escapes all dynamic report labels", () => {
    const root = path.join(os.tmpdir(), "metrics-report-output");
    const state = { sites: { alpha: {
        hostname: "alpha.example.test",
        website: {
            firstCoveredOn: "2025-12-31",
            daily: {
                "2025-12-31": { pageViews: 2, countries: { Germany: 2 }, paths: { "/": 2 } },
                "2026-01-01": { pageViews: 3, countries: { Switzerland: 3 }, paths: { "/<script>": 3 } }
            }
        },
        updateInfo: {
            firstCoveredOn: "2025-12-31",
            daily: {
                "2026-01-01": {
                    requests: 1,
                    countries: { "<img src=x onerror=alert(1)>": 1 },
                    hours: { "00": 1 },
                    clients: { "<script>alert(1)</script>": 1 }
                }
            }
        }
    } } };
    const registry = {
        privateMetrics: { reportOutputDirectory: root },
        sites: [{ id: "alpha", hostname: "alpha.example.test" }]
    };
    const files = renderReports(state, registry, new Date("2026-01-02T00:00:00Z"));
    const html = files.map(file => file.content).join("\n");
    const websiteAnnual = files.find(file => file.destination.endsWith(
        path.join("website", "yearly", "report.html")
    )).content;
    assert.match(html, /2025/);
    assert.match(html, /2026/);
    assert.match(html, /Switzerland/);
    assert.doesNotMatch(html, /<script>alert\(1\)<\/script>/);
    assert.doesNotMatch(html, /<img src=x/);
    assert.match(html, /&lt;script&gt;/);
    assert.match(websiteAnnual, /<td>2025<\/td><td>2<\/td>/);
    assert.match(websiteAnnual, /<td>2026<\/td><td>3<\/td>/);
});

test("uses conservative client groups and never presents Java as KST4Contest", () => {
    assert.equal(clientGroup("Java/21.0.1"), "Java runtime (application unknown)");
    assert.equal(clientGroup("KST4Contest/2.0"), "KST4Contest (explicit)");
    assert.equal(clientGroup("<script>alert(1)</script>"), "Other or unrecognised");
});

test("privacy notice distinguishes page statistics from private update aggregation", () => {
    const privacy = fs.readFileSync(path.join(__dirname, "../src/privacy/index.njk"), "utf8");
    assert.match(privacy, /exact path\s*<code>\/kst4ContestVersionInfo\.xml<\/code>/);
    assert.match(privacy, /do not increase its page views or the public visitor count/);
    assert.match(privacy, /retained for no more than 14 days/);
    assert.match(privacy, /does\s+not establish a program start, a single user/);
    assert.match(privacy, /No visitor address is sent to an external location service/);
});
