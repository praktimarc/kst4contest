const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");

const {
    formatGoAccessCheck,
    generateReports,
    parseArguments,
    parseGoAccessVersion,
    validateReport
} = require("../ops/analytics/generate-reports");

const GOACCESS_WITHOUT_ZLIB = {
    version: "1.8.1",
    geoIpMmdb: true,
    openSsl: true,
    zlib: false
};

function goAccessReport(dailyVisits, combined = false) {
    const report = {
        general: { total_requests: 1 },
        visitors: {
            data: Object.entries(dailyVisits).map(([date, count]) => ({
                data: date.replaceAll("-", ""),
                visitors: { count }
            }))
        },
        requests: { data: [] },
        status_codes: { data: [] },
        geolocation: { data: [] }
    };
    if (combined) {
        report.vhosts = { data: [] };
    }
    return report;
}

function analyticsLine({
    host = "alpha.example.test",
    ip = "192.0.2.1",
    timestamp = "2026-09-11T09:00:00+02:00",
    method = "GET",
    requestPath = "/",
    status = 200,
    userAgent = "Mozilla/5.0 Firefox/130.0"
} = {}) {
    return `${host}\t${ip}\t${timestamp}\t${method}\t${requestPath}\tHTTP/1.1\t${status}\t123\t"${userAgent}"\n`;
}

function fixture(siteDefinitions) {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "kst4-analytics-test-"));
    const stateDirectory = path.join(root, "state");
    const geoIpCountryDatabase = path.join(root, "GeoLite2-Country.mmdb");
    const configTemplatePath = path.join(root, "goaccess.conf.template");
    const registryPath = path.join(root, "sites.json");
    fs.writeFileSync(geoIpCountryDatabase, "test database placeholder");
    fs.copyFileSync(
        path.join(__dirname, "../ops/analytics/goaccess.conf.template"),
        configTemplatePath
    );

    const sites = siteDefinitions.map((definition, index) => {
        const id = definition.id || `site-${index}`;
        const log = path.join(root, `${id}.log`);
        const updateLog = path.join(root, `${id}-update-information.log`);
        fs.writeFileSync(log, analyticsLine({ host: definition.hostname || `${id}.example.test` }));
        fs.writeFileSync(updateLog, analyticsLine({
            host: definition.hostname || `${id}.example.test`,
            requestPath: "/kst4ContestVersionInfo.xml",
            userAgent: "Java/21.0.1"
        }));
        if (definition.rotated !== false) {
            fs.writeFileSync(`${log}.1`, analyticsLine({
                host: definition.hostname || `${id}.example.test`,
                timestamp: "2026-09-11T08:00:00+02:00",
                requestPath: "/privacy/"
            }));
            fs.writeFileSync(`${updateLog}.1`, analyticsLine({
                host: definition.hostname || `${id}.example.test`,
                timestamp: "2026-09-11T08:00:00+02:00",
                requestPath: "/kst4ContestVersionInfo.xml",
                userAgent: "Mozilla/5.0 Firefox/130.0"
            }));
        }
        if (definition.compressed) {
            fs.writeFileSync(`${log}.2.gz`, "compressed placeholder\n");
        }
        return {
            id,
            hostname: definition.hostname || `${id}.example.test`,
            analyticsLog: log,
            activatedOn: definition.activatedOn || "2026-01-01",
            websiteMetricsSince: "2026-09-11",
            updateInfo: {
                path: "/kst4ContestVersionInfo.xml",
                analyticsLog: updateLog,
                metricsSince: "2026-09-11"
            },
            publicCounter: definition.publicCounter,
            reportOutputDirectory: path.join(stateDirectory, "reports", id),
            ...(definition.publicCounter
                ? { publicJsonPath: path.join(stateDirectory, "public", `${id}.json`) }
                : {})
        };
    });
    const registry = {
        schemaVersion: 1,
        stateDirectory,
        counterStatePath: path.join(stateDirectory, "counter-state.json"),
        lockFile: path.join(root, "run", "generator.lock"),
        geoIpCountryDatabase,
        privateMetrics: {
            statePath: path.join(stateDirectory, "private-metrics-state.json"),
            reportOutputDirectory: path.join(stateDirectory, "reports", "metrics"),
            timeZone: "Europe/Berlin",
            detailRetentionDays: 14
        },
        combined: {
            reportOutputDirectory: path.join(stateDirectory, "reports", "combined")
        },
        sites
    };
    fs.mkdirSync(stateDirectory, { recursive: true });
    fs.mkdirSync(registry.combined.reportOutputDirectory, { recursive: true });
    fs.mkdirSync(registry.privateMetrics.reportOutputDirectory, { recursive: true });
    for (const site of sites) {
        fs.mkdirSync(site.reportOutputDirectory, { recursive: true });
        if (site.publicCounter) {
            fs.mkdirSync(path.dirname(site.publicJsonPath), { recursive: true });
        }
    }
    fs.writeFileSync(registryPath, JSON.stringify(registry));

    return {
        root,
        registry,
        registryPath,
        configTemplatePath,
        cleanup: () => fs.rmSync(root, { recursive: true, force: true })
    };
}

function fakeGoAccess(reports, calls, failureId) {
    return invocation => {
        calls.push(invocation);
        if (invocation.id === failureId) {
            throw new Error("simulated GoAccess failure");
        }
        if (invocation.metricKind) {
            const sourceLines = fs.readFileSync(invocation.args[0], "utf8").trim().split(/\r?\n/);
            invocation.inputLines = sourceLines;
            const lines = invocation.metricKind === "websiteClassifier"
                ? sourceLines.filter(line => {
                    const fields = line.split("\t");
                    return fields[6] !== "404" && !line.includes("UnknownScanner/1.0");
                })
                : sourceLines;
            const paths = {};
            for (const line of lines) {
                const requestPath = line.split("\t")[4];
                paths[requestPath] = (paths[requestPath] || 0) + 1;
            }
            const report = {
                general: { total_requests: lines.length, valid_requests: lines.length },
                requests: {
                    data: Object.entries(paths).map(([requestPath, count]) => ({
                        data: requestPath,
                        hits: { count }
                    }))
                },
                geolocation: { data: [{ data: "Test Country", hits: { count: lines.length } }] }
            };
            fs.writeFileSync(invocation.outputJson, JSON.stringify(report));
            return;
        }
        fs.writeFileSync(
            invocation.outputJson,
            JSON.stringify(reports[invocation.id])
        );
        fs.writeFileSync(
            invocation.outputHtml,
            `<!doctype html><html><body>${"report".repeat(30)}</body></html>`
        );
        fs.writeFileSync(path.join(invocation.dbPath, "persisted.db"), invocation.id);
    };
}

test("uses GoAccess client classification before aggregating website paths and countries", () => {
    const testFixture = fixture([{ id: "alpha", publicCounter: true }]);
    const calls = [];
    const reports = {
        alpha: goAccessReport({ "2026-09-11": 2 }),
        combined: goAccessReport({ "2026-09-11": 2 }, true)
    };
    fs.appendFileSync(testFixture.registry.sites[0].analyticsLog, analyticsLine({
        requestPath: "/scanner/",
        userAgent: "UnknownScanner/1.0"
    }));
    fs.appendFileSync(testFixture.registry.sites[0].analyticsLog, analyticsLine({
        requestPath: "/missing/",
        status: 404
    }));
    fs.appendFileSync(testFixture.registry.sites[0].analyticsLog, analyticsLine({
        requestPath: "/cached/",
        status: 304
    }));
    fs.appendFileSync(testFixture.registry.sites[0].analyticsLog, analyticsLine({
        requestPath: "/page.html",
        userAgent: "Mozilla/5.0 Chrome/140.0"
    }));
    try {
        generateReports({
            registryPath: testFixture.registryPath,
            configTemplatePath: testFixture.configTemplatePath,
            goaccessBinary: "fake-goaccess"
        }, {
            checkGoAccess: () => GOACCESS_WITHOUT_ZLIB,
            runGoAccess: fakeGoAccess(reports, calls),
            now: () => new Date("2026-09-11T12:00:00Z"),
            skipLock: true
        });

        const state = JSON.parse(fs.readFileSync(testFixture.registry.privateMetrics.statePath, "utf8"));
        assert.equal(state.sites.alpha.website.daily["2026-09-11"].pageViews, 4);
        assert.equal(state.sites.alpha.website.daily["2026-09-11"].paths["/scanner/"], undefined);
        assert.equal(state.sites.alpha.website.daily["2026-09-11"].paths["/missing/"], undefined);
        assert.equal(state.sites.alpha.website.daily["2026-09-11"].paths["/cached/"], 1);
        assert.equal(state.sites.alpha.website.daily["2026-09-11"].paths["/page.html"], 1);
        const classifierCall = calls.find(call => call.metricKind === "websiteClassifier");
        const aggregationCall = calls.find(call => call.metricKind === "website");
        assert.equal(classifierCall.inputLines.some(line => line.includes("UnknownScanner/1.0")), true);
        assert.equal(classifierCall.inputLines.every(line => line.split("\t")[4].startsWith("/__request/")), true);
        assert.equal(classifierCall.inputLines.some(line => line.split("\t")[4].endsWith(".html")), true);
        assert.equal(aggregationCall.inputLines.some(line => line.includes("UnknownScanner/1.0")), false);
        assert.equal(aggregationCall.inputLines.some(line => line.split("\t")[6] === "404"), false);
    } finally {
        testFixture.cleanup();
    }
});

function run(testFixture, reports, calls = [], failureId) {
    return generateReports({
        registryPath: testFixture.registryPath,
        configTemplatePath: testFixture.configTemplatePath,
        goaccessBinary: "fake-goaccess"
    }, {
        checkGoAccess: () => GOACCESS_WITHOUT_ZLIB,
        runGoAccess: fakeGoAccess(reports, calls, failureId),
        now: () => new Date("2026-09-11T07:00:00Z"),
        skipLock: true
    });
}

test("replaces repeated daily values and retains older public days", () => {
    const testFixture = fixture([{ id: "alpha", publicCounter: true }]);
    try {
        fs.mkdirSync(path.dirname(testFixture.registry.counterStatePath), { recursive: true });
        fs.writeFileSync(testFixture.registry.counterStatePath, JSON.stringify({
            schemaVersion: 1,
            sites: {
                alpha: {
                    hostname: "alpha.example.test",
                    since: "2026-01-01",
                    dailyVisits: { "2026-01-01": 7 }
                }
            }
        }));

        run(testFixture, {
            alpha: goAccessReport({ "2026-09-11": 3 }),
            combined: goAccessReport({ "2026-09-11": 3 }, true)
        });
        run(testFixture, {
            alpha: goAccessReport({ "2026-09-11": 5 }),
            combined: goAccessReport({ "2026-09-11": 5 }, true)
        });

        const state = JSON.parse(fs.readFileSync(
            testFixture.registry.counterStatePath,
            "utf8"
        ));
        const published = JSON.parse(fs.readFileSync(
            testFixture.registry.sites[0].publicJsonPath,
            "utf8"
        ));
        assert.deepEqual(state.sites.alpha.dailyVisits, {
            "2026-01-01": 7,
            "2026-09-11": 5
        });
        assert.deepEqual(published, {
            schemaVersion: 1,
            visits: 12,
            since: "2026-01-01",
            updatedAt: "2026-09-11T07:00:00Z"
        });
    } finally {
        testFixture.cleanup();
    }
});

test("keeps valid outputs unchanged when a GoAccess job fails", () => {
    const testFixture = fixture([{ id: "alpha", publicCounter: true }]);
    try {
        const reportDirectory = testFixture.registry.sites[0].reportOutputDirectory;
        fs.mkdirSync(reportDirectory, { recursive: true });
        fs.mkdirSync(path.dirname(testFixture.registry.counterStatePath), { recursive: true });
        fs.mkdirSync(path.dirname(testFixture.registry.sites[0].publicJsonPath), { recursive: true });
        fs.writeFileSync(path.join(reportDirectory, "report.json"), "old-json");
        fs.writeFileSync(path.join(reportDirectory, "report.html"), "old-html");
        fs.writeFileSync(testFixture.registry.counterStatePath, "old-state");
        fs.writeFileSync(testFixture.registry.sites[0].publicJsonPath, "old-public");

        assert.throws(() => run(testFixture, {
            alpha: goAccessReport({ "2026-09-11": 3 }),
            combined: goAccessReport({ "2026-09-11": 3 }, true)
        }, [], "combined"), /simulated GoAccess failure/);

        assert.equal(fs.readFileSync(path.join(reportDirectory, "report.json"), "utf8"), "old-json");
        assert.equal(fs.readFileSync(path.join(reportDirectory, "report.html"), "utf8"), "old-html");
        assert.equal(fs.readFileSync(testFixture.registry.counterStatePath, "utf8"), "old-state");
        assert.equal(fs.readFileSync(testFixture.registry.sites[0].publicJsonPath, "utf8"), "old-public");
    } finally {
        testFixture.cleanup();
    }
});

test("keeps all published outputs unchanged when private metric generation fails", () => {
    const testFixture = fixture([{ id: "alpha", publicCounter: true }]);
    const site = testFixture.registry.sites[0];
    const metricReport = path.join(testFixture.registry.privateMetrics.reportOutputDirectory, "report.html");
    const normalRunner = fakeGoAccess({
        alpha: goAccessReport({ "2026-09-11": 3 }),
        combined: goAccessReport({ "2026-09-11": 3 }, true)
    }, []);
    try {
        fs.writeFileSync(path.join(site.reportOutputDirectory, "report.html"), "old-html");
        fs.writeFileSync(site.publicJsonPath, "old-public");
        fs.writeFileSync(metricReport, "old-metrics");

        assert.throws(() => generateReports({
            registryPath: testFixture.registryPath,
            configTemplatePath: testFixture.configTemplatePath,
            goaccessBinary: "fake-goaccess"
        }, {
            checkGoAccess: () => GOACCESS_WITHOUT_ZLIB,
            runGoAccess: invocation => {
                if (invocation.metricKind) throw new Error("simulated private metric failure");
                normalRunner(invocation);
            },
            now: () => new Date("2026-09-11T07:00:00Z"),
            skipLock: true
        }), /simulated private metric failure/);

        assert.equal(fs.readFileSync(path.join(site.reportOutputDirectory, "report.html"), "utf8"), "old-html");
        assert.equal(fs.readFileSync(site.publicJsonPath, "utf8"), "old-public");
        assert.equal(fs.readFileSync(metricReport, "utf8"), "old-metrics");
        assert.equal(fs.existsSync(testFixture.registry.privateMetrics.statePath), false);
    } finally {
        testFixture.cleanup();
    }
});

test("processes subdomains separately and together without publishing disabled counters", () => {
    const testFixture = fixture([
        { id: "alpha", publicCounter: true, compressed: true },
        { id: "bravo", publicCounter: false, rotated: false, compressed: true }
    ]);
    const calls = [];
    try {
        const result = run(testFixture, {
            alpha: goAccessReport({ "2026-09-11": 3 }),
            bravo: goAccessReport({ "2026-09-11": 4 }),
            combined: goAccessReport({ "2026-09-11": 6 }, true)
        }, calls);

        assert.equal(result.reports, 3);
        const reportCalls = calls.filter(call => !call.metricKind);
        assert.deepEqual(reportCalls.map(call => call.id), ["alpha", "bravo", "combined"]);
        const alphaLogs = [
            `${testFixture.registry.sites[0].analyticsLog}.1`,
            testFixture.registry.sites[0].analyticsLog
        ];
        const bravoLogs = [testFixture.registry.sites[1].analyticsLog];
        assert.deepEqual(reportCalls[0].args.slice(0, 2), alphaLogs);
        assert.deepEqual(reportCalls[1].args.slice(0, 1), bravoLogs);
        assert.deepEqual(
            reportCalls[2].args.slice(0, 3),
            [...alphaLogs, ...bravoLogs]
        );
        assert.equal(reportCalls[0].args.includes("--enable-panel=VIRTUAL_HOSTS"), false);
        assert.equal(reportCalls[1].args.includes("--enable-panel=VIRTUAL_HOSTS"), false);
        assert.equal(reportCalls[2].args.includes("--enable-panel=VIRTUAL_HOSTS"), true);
        assert.equal(reportCalls.some(call => call.args.some(argument => argument.endsWith(".gz"))), false);
        assert.equal(fs.existsSync(path.join(
            testFixture.registry.stateDirectory,
            "public",
            "bravo.json"
        )), false);
        assert.equal(fs.existsSync(path.join(
            testFixture.registry.combined.reportOutputDirectory,
            "report.html"
        )), true);
        assert.match(fs.readFileSync(path.join(
            testFixture.registry.combined.reportOutputDirectory,
            "report.html"
        ), "utf8"), /href="\/metrics\/"/);
        const privateState = JSON.parse(fs.readFileSync(
            testFixture.registry.privateMetrics.statePath,
            "utf8"
        ));
        assert.equal(privateState.sites.alpha.website.daily["2026-09-11"].pageViews, 2);
        assert.equal(privateState.sites.alpha.updateInfo.daily["2026-09-11"].requests, 2);
    } finally {
        testFixture.cleanup();
    }
});

test("validates GoAccess 1.8.1 panel names strictly", () => {
    const siteReport = goAccessReport({ "2026-09-11": 3 });
    const combinedReport = goAccessReport({ "2026-09-11": 3 }, true);

    assert.doesNotThrow(() => validateReport(siteReport, false));
    assert.doesNotThrow(() => validateReport(combinedReport, true));

    const missingGeolocation = goAccessReport({ "2026-09-11": 3 });
    delete missingGeolocation.geolocation;
    missingGeolocation.geo_location = { data: [] };
    assert.throws(
        () => validateReport(missingGeolocation, false),
        /GoAccess JSON report has no geolocation panel/
    );

    const missingVhosts = goAccessReport({ "2026-09-11": 3 }, true);
    delete missingVhosts.vhosts;
    missingVhosts.virtual_hosts = { data: [] };
    assert.throws(
        () => validateReport(missingVhosts, true),
        /GoAccess JSON report has no vhosts panel/
    );
});

test("dry-run validates generated data without changing production paths", () => {
    const testFixture = fixture([{ id: "alpha", publicCounter: true }]);
    try {
        const blockedLockParent = path.join(testFixture.root, "blocked-lock-parent");
        fs.writeFileSync(blockedLockParent, "not a directory");
        testFixture.registry.lockFile = path.join(blockedLockParent, "generator.lock");
        fs.writeFileSync(testFixture.registryPath, JSON.stringify(testFixture.registry));

        const result = generateReports({
            registryPath: testFixture.registryPath,
            configTemplatePath: testFixture.configTemplatePath,
            goaccessBinary: "fake-goaccess",
            dryRun: true
        }, {
            checkGoAccess: () => GOACCESS_WITHOUT_ZLIB,
            runGoAccess: fakeGoAccess({
                alpha: goAccessReport({ "2026-09-11": 3 }),
                combined: goAccessReport({ "2026-09-11": 3 }, true)
            }, []),
            now: () => new Date("2026-09-11T07:00:00Z")
        });

        assert.equal(result.dryRun, true);
        assert.equal(fs.statSync(blockedLockParent).isFile(), true);
        assert.equal(fs.readdirSync(
            testFixture.registry.sites[0].reportOutputDirectory
        ).length, 0);
        assert.equal(fs.existsSync(testFixture.registry.sites[0].publicJsonPath), false);
        assert.equal(fs.existsSync(testFixture.registry.counterStatePath), false);
        assert.equal(fs.existsSync(testFixture.registry.privateMetrics.statePath), false);
    } finally {
        testFixture.cleanup();
    }
});

test("configuration check accepts and explains a GoAccess build without Zlib", () => {
    const testFixture = fixture([{ id: "alpha", publicCounter: true, rotated: false }]);
    try {
        const versionOutput = [
            "GoAccess - 1.8.1.",
            "Build configure arguments:",
            "  --enable-geoip=mmdb",
            "  --with-openssl"
        ].join("\n");
        const capabilities = parseGoAccessVersion(versionOutput);
        const result = generateReports({
            registryPath: testFixture.registryPath,
            configTemplatePath: testFixture.configTemplatePath,
            goaccessBinary: "fake-goaccess",
            check: true
        }, {
            checkGoAccess: () => capabilities,
            runGoAccess: () => {
                throw new Error("configuration check must not generate reports");
            }
        });
        const message = formatGoAccessCheck(result.goAccess);

        assert.deepEqual(capabilities, GOACCESS_WITHOUT_ZLIB);
        assert.equal(result.checked, true);
        assert.match(message, /Zlib not detected/);
        assert.match(message, /no Zlib support/);
        assert.match(message, /valid for regular mode/);
        assert.match(message, /optional uncompressed \.1 rotation/);
        assert.match(message, /older \.gz logs are not imported/);
    } finally {
        testFixture.cleanup();
    }
});

test("detects Zlib support when it is present", () => {
    const capabilities = parseGoAccessVersion([
        "GoAccess - 1.8.1.",
        "Build configure arguments:",
        "  --enable-geoip=mmdb --with-openssl --with-zlib"
    ].join("\n"));

    assert.equal(capabilities.zlib, true);
    assert.match(formatGoAccessCheck(capabilities), /Zlib enabled/);
    assert.doesNotMatch(formatGoAccessCheck(capabilities), /older \.gz logs/);
});

test("configuration check rejects GoAccess without GeoIP2 MMDB support", () => {
    const testFixture = fixture([{ id: "alpha", publicCounter: true }]);
    try {
        assert.throws(() => generateReports({
            registryPath: testFixture.registryPath,
            configTemplatePath: testFixture.configTemplatePath,
            check: true
        }, {
            checkGoAccess: () => ({
                version: "1.8.1",
                geoIpMmdb: false,
                openSsl: true,
                zlib: false
            })
        }), error => error.exitCode === 2
            && /must be built with GeoIP2\/MMDB support/.test(error.message));
    } finally {
        testFixture.cleanup();
    }
});

test("configuration check verifies analytics-log readability", () => {
    const testFixture = fixture([{ id: "alpha", publicCounter: true, rotated: false }]);
    const originalAccessSync = fs.accessSync;
    try {
        fs.accessSync = (filePath, mode) => {
            if (filePath === testFixture.registry.sites[0].analyticsLog
                && mode === fs.constants.R_OK) {
                const error = new Error("permission denied");
                error.code = "EACCES";
                throw error;
            }
            return originalAccessSync(filePath, mode);
        };

        assert.throws(() => generateReports({
            registryPath: testFixture.registryPath,
            configTemplatePath: testFixture.configTemplatePath,
            check: true
        }, {
            checkGoAccess: () => GOACCESS_WITHOUT_ZLIB
        }), /analytics log is not readable by the current user/);
    } finally {
        fs.accessSync = originalAccessSync;
        testFixture.cleanup();
    }
});

test("configuration check rejects a missing update-information log", () => {
    const testFixture = fixture([{ id: "alpha", publicCounter: true }]);
    try {
        fs.rmSync(testFixture.registry.sites[0].updateInfo.analyticsLog);
        assert.throws(() => generateReports({
            registryPath: testFixture.registryPath,
            configTemplatePath: testFixture.configTemplatePath,
            check: true
        }, {
            checkGoAccess: () => GOACCESS_WITHOUT_ZLIB
        }), /analytics log is not readable/);
    } finally {
        testFixture.cleanup();
    }
});

test("configuration check reports missing output directories", () => {
    const testFixture = fixture([{ id: "alpha", publicCounter: true }]);
    try {
        fs.rmSync(testFixture.registry.sites[0].reportOutputDirectory, {
            recursive: true,
            force: true
        });

        assert.throws(() => generateReports({
            registryPath: testFixture.registryPath,
            configTemplatePath: testFixture.configTemplatePath,
            check: true
        }, {
            checkGoAccess: () => GOACCESS_WITHOUT_ZLIB
        }), /report output directory for alpha does not exist/);
    } finally {
        testFixture.cleanup();
    }
});

test("rejects a compressed log as the regular analytics input", () => {
    const testFixture = fixture([{ id: "alpha", publicCounter: true }]);
    try {
        testFixture.registry.sites[0].analyticsLog += ".2.gz";
        fs.writeFileSync(testFixture.registryPath, JSON.stringify(testFixture.registry));

        assert.throws(() => generateReports({
            registryPath: testFixture.registryPath,
            configTemplatePath: testFixture.configTemplatePath,
            check: true
        }, {
            checkGoAccess: () => GOACCESS_WITHOUT_ZLIB
        }), /must identify the current uncompressed log/);
    } finally {
        testFixture.cleanup();
    }
});

test("Nginx filters exclude non-page traffic before analytics logging", () => {
    const source = fs.readFileSync(path.join(
        __dirname,
        "../ops/analytics/nginx/analytics-filters.conf.example"
    ), "utf8");

    for (const excluded of [
        "/visitor-count.json",
        "/kst4ContestVersionInfo.xml",
        "/sitemap.xml",
        "/robots.txt",
        "manual/assets",
        "healthz"
    ]) {
        assert.match(source, new RegExp(excluded.replaceAll("/", "\\/"), "i"));
    }
    assert.doesNotMatch(source, /^\s*=\//m);
    assert.match(source, /^\s*\/visitor-count\.json\s+0;/m);
    assert.match(source, /^\s*\/health\s+0;/m);
    assert.match(source, /\|map\|/);
    assert.match(source, /\$uri/);
    assert.match(source, /known_bot/);
    assert.match(source, /map "\$request_method:\$uri:\$status" \$hamradioonline_update_information_loggable/);
    assert.match(source, /"GET:\/kst4ContestVersionInfo\.xml:200" 1;/);
});

test("historical import is repeatable and overlaps live aggregation without addition", () => {
    const testFixture = fixture([{ id: "alpha", publicCounter: true }]);
    const importPath = path.join(testFixture.root, "access.log.2.gz");
    const historical = [
        '192.0.2.10 - - [10/Sep/2026:12:00:00 +0200] "GET /privacy/?x=1 HTTP/1.1" 200 42 "-" "Firefox/130"',
        '192.0.2.11 - - [10/Sep/2026:12:01:00 +0200] "GET /kst4ContestVersionInfo.xml HTTP/1.1" 200 43 "-" "Java/21.0.1"'
    ].join("\n");
    const reports = {
        alpha: goAccessReport({ "2026-09-11": 3 }),
        combined: goAccessReport({ "2026-09-11": 3 }, true)
    };
    try {
        testFixture.registry.sites[0].websiteMetricsSince = "2026-09-08";
        testFixture.registry.sites[0].updateInfo.metricsSince = "2026-09-08";
        fs.writeFileSync(testFixture.registryPath, JSON.stringify(testFixture.registry));
        fs.writeFileSync(importPath, require("node:zlib").gzipSync(`${historical}\n`));
        const options = {
            registryPath: testFixture.registryPath,
            configTemplatePath: testFixture.configTemplatePath,
            goaccessBinary: "fake-goaccess",
            importLogs: [importPath],
            importFormat: "nginx-combined",
            importSite: "alpha",
            coverageFrom: "2026-09-08",
            coverageThrough: "2026-09-10"
        };
        const dependencies = {
            checkGoAccess: () => GOACCESS_WITHOUT_ZLIB,
            runGoAccess: fakeGoAccess(reports, []),
            now: () => new Date("2026-09-11T07:00:00Z"),
            skipLock: true
        };

        generateReports(options, dependencies);
        generateReports(options, dependencies);

        const state = JSON.parse(fs.readFileSync(testFixture.registry.privateMetrics.statePath, "utf8"));
        assert.equal(state.sites.alpha.website.daily["2026-09-10"].pageViews, 1);
        assert.equal(state.sites.alpha.updateInfo.daily["2026-09-10"].requests, 1);
        assert.equal(state.sites.alpha.website.daily["2026-09-11"].pageViews, 2);
        assert.equal(state.sites.alpha.updateInfo.daily["2026-09-11"].requests, 2);
        assert.equal(state.sites.alpha.website.firstCoveredOn, "2026-09-08");
        assert.equal(state.sites.alpha.updateInfo.firstCoveredOn, "2026-09-08");
        assert.equal(fs.existsSync(path.join(
            testFixture.registry.privateMetrics.reportOutputDirectory,
            "alpha", "updates", "yearly", "report.html"
        )), true);
    } finally {
        testFixture.cleanup();
    }
});

test("first run after midnight refreshes the previous day without double counting", () => {
    const testFixture = fixture([{ id: "alpha", publicCounter: true }]);
    const reports = {
        alpha: goAccessReport({ "2026-09-11": 3 }),
        combined: goAccessReport({ "2026-09-11": 3 }, true)
    };
    const baseOptions = {
        registryPath: testFixture.registryPath,
        configTemplatePath: testFixture.configTemplatePath,
        goaccessBinary: "fake-goaccess"
    };
    try {
        generateReports(baseOptions, {
            checkGoAccess: () => GOACCESS_WITHOUT_ZLIB,
            runGoAccess: fakeGoAccess(reports, []),
            now: () => new Date("2026-09-11T20:00:00Z"),
            skipLock: true
        });
        fs.appendFileSync(testFixture.registry.sites[0].analyticsLog, analyticsLine({
            timestamp: "2026-09-11T23:59:00+02:00",
            requestPath: "/news/"
        }));
        generateReports(baseOptions, {
            checkGoAccess: () => GOACCESS_WITHOUT_ZLIB,
            runGoAccess: fakeGoAccess(reports, []),
            now: () => new Date("2026-09-12T00:30:00Z"),
            skipLock: true
        });

        const state = JSON.parse(fs.readFileSync(testFixture.registry.privateMetrics.statePath, "utf8"));
        assert.equal(state.sites.alpha.website.daily["2026-09-11"].pageViews, 3);
        assert.equal(state.sites.alpha.website.daily["2026-09-12"].pageViews, 0);
    } finally {
        testFixture.cleanup();
    }
});

test("historical import CLI requires explicit format, site and coverage", () => {
    const options = parseArguments([
        "--registry", "/etc/hamradioonline-analytics/sites.json",
        "--config-template", "/etc/hamradioonline-analytics/goaccess.conf.template",
        "--import-site", "kst4contest",
        "--import-format", "nginx-combined",
        "--coverage-from", "2026-09-01",
        "--coverage-through", "2026-09-10",
        "--import-log", "/protected/access.log.2.gz",
        "--import-log", "/protected/access.log.1"
    ]);
    assert.deepEqual(options.importLogs, [
        "/protected/access.log.2.gz",
        "/protected/access.log.1"
    ]);
    assert.equal(options.importFormat, "nginx-combined");
    assert.equal(options.importSite, "kst4contest");
    assert.equal(options.coverageFrom, "2026-09-01");
    assert.equal(options.coverageThrough, "2026-09-10");
});

test("server templates use the production GeoIP path and permission model", () => {
    const registry = JSON.parse(fs.readFileSync(path.join(
        __dirname,
        "../ops/analytics/sites.example.json"
    ), "utf8"));
    const service = fs.readFileSync(path.join(
        __dirname,
        "../ops/analytics/systemd/hamradioonline-analytics.service.example"
    ), "utf8");
    const generator = fs.readFileSync(path.join(
        __dirname,
        "../ops/analytics/generate-reports.js"
    ), "utf8");

    assert.equal(registry.geoIpCountryDatabase, "/var/lib/GeoIP/GeoLite2-Country.mmdb");
    assert.match(service, /\/var\/lib\/GeoIP\/GeoLite2-Country\.mmdb/);
    assert.match(service, /^StateDirectoryMode=0711$/m);
    assert.match(generator, /fs\.fchmodSync\(handle, mode\)/);
    assert.match(generator, /publicFile\.destination, publicFile\.content, 0o644/);
});

test("public counter template disables logging and sets cache and type headers", () => {
    const source = fs.readFileSync(path.join(
        __dirname,
        "../ops/analytics/nginx/public-counter.conf.example"
    ), "utf8");

    assert.match(source, /^\s*access_log off;$/m);
    assert.match(source, /Cache-Control "public, max-age=3600" always/);
    assert.match(source, /X-Content-Type-Options "nosniff" always/);
});

test("statistics vhost preserves ACME renewal and protects HTTPS reports", () => {
    const source = fs.readFileSync(path.join(
        __dirname,
        "../ops/analytics/nginx/stats-vhost.conf.example"
    ), "utf8");
    const bootstrap = fs.readFileSync(path.join(
        __dirname,
        "../ops/analytics/nginx/stats-vhost-http-bootstrap.conf.example"
    ), "utf8");
    const httpsOffset = source.search(/server\s*\{\s*listen 443 ssl http2;/);
    assert.notEqual(httpsOffset, -1);
    const httpSource = source.slice(0, httpsOffset);
    const httpsSource = source.slice(httpsOffset);

    assert.match(httpSource, /^\s*listen 80;$/m);
    assert.match(httpSource, /^\s*access_log off;$/m);
    assert.match(httpSource, /location \^~ \/\.well-known\/acme-challenge\//);
    assert.match(httpSource, /root \/var\/lib\/letsencrypt;/);
    assert.match(httpSource, /return 301 https:\/\/\$host\$request_uri;/);
    assert.match(httpsSource, /^\s*listen 443 ssl http2;$/m);
    assert.match(httpsSource, /^\s*auth_basic "Private project statistics";$/m);
    assert.match(httpsSource, /^\s*access_log off;$/m);
    assert.match(httpsSource, /location = \/[\s\S]*return 302 \/combined\/;/);
    assert.match(httpsSource, /include \/etc\/letsencrypt\/options-ssl-nginx\.conf;/);
    assert.match(httpsSource, /ssl_dhparam \/etc\/letsencrypt\/ssl-dhparams\.pem;/);
    assert.doesNotMatch(source, /listen \[::\]/);
    assert.match(bootstrap, /^\s*listen 80;$/m);
    assert.doesNotMatch(bootstrap, /ssl_certificate/);
});

test("logrotate uses the Ubuntu Nginx rotation action", () => {
    const source = fs.readFileSync(path.join(
        __dirname,
        "../ops/analytics/logrotate/hamradioonline-analytics.example"
    ), "utf8");

    assert.match(source, /invoke-rc\.d nginx rotate >\/dev\/null 2>&1/);
    assert.match(source, /^\s*delaycompress$/m);
    assert.match(source, /^\s*rotate 14$/m);
    assert.match(source, /^\s*create 0640 www-data hamradio-analytics$/m);
    assert.match(source, /\*-update-information\.log/);
});

test("website package records the Node.js 18.19.1 baseline", () => {
    const packageJson = JSON.parse(fs.readFileSync(path.join(__dirname, "../package.json")));
    const packageLock = JSON.parse(fs.readFileSync(path.join(__dirname, "../package-lock.json")));

    assert.equal(packageJson.engines.node, ">=18.19.1");
    assert.equal(packageLock.packages[""].engines.node, ">=18.19.1");
});
