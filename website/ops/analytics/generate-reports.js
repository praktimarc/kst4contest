#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { spawnSync } = require("node:child_process");
const {
    UPDATE_INFO_PATH,
    generatePrivateMetrics,
    validateImportOptions
} = require("./private-metrics");

const STATE_SCHEMA_VERSION = 1;
const PUBLIC_SCHEMA_VERSION = 1;
const MAX_SAFE_INTEGER = Number.MAX_SAFE_INTEGER;
const RESERVED_STATS_HOST = "stats.hamradioonline.de";

class ConfigurationError extends Error {
    constructor(message) {
        super(message);
        this.name = "ConfigurationError";
        this.exitCode = 2;
    }
}

class LockError extends Error {
    constructor(message) {
        super(message);
        this.name = "LockError";
        this.exitCode = 3;
    }
}

function readJson(filePath, label) {
    let parsed;

    try {
        parsed = JSON.parse(fs.readFileSync(filePath, "utf8"));
    } catch (error) {
        throw new ConfigurationError(`${label} is not valid JSON: ${error.message}`);
    }

    return parsed;
}

function parseIsoDate(value) {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);

    if (!match) {
        return null;
    }

    const year = Number(match[1]);
    const month = Number(match[2]);
    const day = Number(match[3]);
    const date = new Date(Date.UTC(year, month - 1, day));

    if (date.getUTCFullYear() !== year
        || date.getUTCMonth() !== month - 1
        || date.getUTCDate() !== day) {
        return null;
    }

    return `${match[1]}-${match[2]}-${match[3]}`;
}

function requireAbsolutePath(value, label) {
    if (typeof value !== "string" || /[\r\n\0]/.test(value) || !path.isAbsolute(value)) {
        throw new ConfigurationError(`${label} must be an absolute path`);
    }

    return path.normalize(value);
}

function isWithin(parent, candidate) {
    const relative = path.relative(parent, candidate);
    return relative !== ""
        && relative !== ".."
        && !relative.startsWith(`..${path.sep}`)
        && !path.isAbsolute(relative);
}

function validateRegistry(registry) {
    if (!registry || typeof registry !== "object" || Array.isArray(registry)) {
        throw new ConfigurationError("registry must be an object");
    }
    if (registry.schemaVersion !== 1) {
        throw new ConfigurationError("registry schemaVersion must be 1");
    }
    if (!Array.isArray(registry.sites) || registry.sites.length === 0) {
        throw new ConfigurationError("registry.sites must contain at least one site");
    }

    const stateDirectory = requireAbsolutePath(
        registry.stateDirectory,
        "registry.stateDirectory"
    );
    const counterStatePath = requireAbsolutePath(
        registry.counterStatePath,
        "registry.counterStatePath"
    );
    if (!isWithin(stateDirectory, counterStatePath)) {
        throw new ConfigurationError("counterStatePath must be below stateDirectory");
    }

    const ids = new Set();
    const hostnames = new Set();
    const analyticsLogPaths = new Set();
    const outputDirectories = new Set();
    const publicJsonPaths = new Set();
    const sites = registry.sites.map((site, index) => {
        const label = `registry.sites[${index}]`;

        if (!site || typeof site !== "object" || Array.isArray(site)) {
            throw new ConfigurationError(`${label} must be an object`);
        }
        if (typeof site.id !== "string" || !/^[a-z0-9][a-z0-9-]{0,62}$/.test(site.id)) {
            throw new ConfigurationError(`${label}.id is invalid`);
        }
        if (site.id === "combined") {
            throw new ConfigurationError(`${label}.id is reserved`);
        }
        if (ids.has(site.id)) {
            throw new ConfigurationError(`duplicate site id: ${site.id}`);
        }
        ids.add(site.id);

        if (typeof site.hostname !== "string"
            || !/^[a-z0-9.-]+$/.test(site.hostname)
            || site.hostname.includes("..")) {
            throw new ConfigurationError(`${label}.hostname is invalid`);
        }
        const hostname = site.hostname.toLowerCase();
        if (hostname === RESERVED_STATS_HOST) {
            throw new ConfigurationError(`${RESERVED_STATS_HOST} must not be registered`);
        }
        if (hostnames.has(hostname)) {
            throw new ConfigurationError(`duplicate hostname: ${hostname}`);
        }
        hostnames.add(hostname);

        const activatedOn = parseIsoDate(site.activatedOn);
        if (!activatedOn) {
            throw new ConfigurationError(`${label}.activatedOn must be a valid ISO date`);
        }
        if (typeof site.publicCounter !== "boolean") {
            throw new ConfigurationError(`${label}.publicCounter must be boolean`);
        }
        const analyticsLog = requireAbsolutePath(
            site.analyticsLog,
            `${label}.analyticsLog`
        );
        if (/\.(?:\d+|gz)$/i.test(path.basename(analyticsLog))) {
            throw new ConfigurationError(
                `${label}.analyticsLog must identify the current uncompressed log`
            );
        }
        if (analyticsLogPaths.has(analyticsLog)) {
            throw new ConfigurationError(`analytics log is registered more than once: ${analyticsLog}`);
        }
        analyticsLogPaths.add(analyticsLog);

        const websiteMetricsSince = parseIsoDate(site.websiteMetricsSince);
        if (!websiteMetricsSince) {
            throw new ConfigurationError(`${label}.websiteMetricsSince must be a valid ISO date`);
        }
        if (!site.updateInfo || typeof site.updateInfo !== "object" || Array.isArray(site.updateInfo)) {
            throw new ConfigurationError(`${label}.updateInfo must be an object`);
        }
        if (site.updateInfo.path !== UPDATE_INFO_PATH) {
            throw new ConfigurationError(`${label}.updateInfo.path must be ${UPDATE_INFO_PATH}`);
        }
        const updateInfoLog = requireAbsolutePath(
            site.updateInfo.analyticsLog,
            `${label}.updateInfo.analyticsLog`
        );
        if (/\.(?:\d+|gz)$/i.test(path.basename(updateInfoLog))) {
            throw new ConfigurationError(
                `${label}.updateInfo.analyticsLog must identify the current uncompressed log`
            );
        }
        if (analyticsLogPaths.has(updateInfoLog)) {
            throw new ConfigurationError(`analytics log is registered more than once: ${updateInfoLog}`);
        }
        analyticsLogPaths.add(updateInfoLog);
        const updateMetricsSince = parseIsoDate(site.updateInfo.metricsSince);
        if (!updateMetricsSince) {
            throw new ConfigurationError(`${label}.updateInfo.metricsSince must be a valid ISO date`);
        }

        const reportOutputDirectory = requireAbsolutePath(
            site.reportOutputDirectory,
            `${label}.reportOutputDirectory`
        );
        if (!isWithin(stateDirectory, reportOutputDirectory)) {
            throw new ConfigurationError(`${label}.reportOutputDirectory must be below stateDirectory`);
        }
        if (outputDirectories.has(reportOutputDirectory)) {
            throw new ConfigurationError(`duplicate report output directory: ${reportOutputDirectory}`);
        }
        outputDirectories.add(reportOutputDirectory);

        const publicJsonPath = site.publicCounter
            ? requireAbsolutePath(site.publicJsonPath, `${label}.publicJsonPath`)
            : null;
        if (publicJsonPath && !isWithin(stateDirectory, publicJsonPath)) {
            throw new ConfigurationError(`${label}.publicJsonPath must be below stateDirectory`);
        }
        if (publicJsonPath === counterStatePath) {
            throw new ConfigurationError(`${label}.publicJsonPath conflicts with counterStatePath`);
        }
        if (publicJsonPath && publicJsonPaths.has(publicJsonPath)) {
            throw new ConfigurationError(`duplicate public JSON path: ${publicJsonPath}`);
        }
        if (publicJsonPath) {
            publicJsonPaths.add(publicJsonPath);
        }

        return {
            id: site.id,
            hostname,
            activatedOn,
            publicCounter: site.publicCounter,
            analyticsLog,
            websiteMetricsSince,
            updateInfo: {
                path: UPDATE_INFO_PATH,
                analyticsLog: updateInfoLog,
                metricsSince: updateMetricsSince
            },
            reportOutputDirectory,
            publicJsonPath
        };
    });

    if (!registry.combined || typeof registry.combined !== "object") {
        throw new ConfigurationError("registry.combined must be an object");
    }

    const combinedReportOutputDirectory = requireAbsolutePath(
        registry.combined.reportOutputDirectory,
        "registry.combined.reportOutputDirectory"
    );
    if (!isWithin(stateDirectory, combinedReportOutputDirectory)) {
        throw new ConfigurationError(
            "registry.combined.reportOutputDirectory must be below stateDirectory"
        );
    }
    if (outputDirectories.has(combinedReportOutputDirectory)) {
        throw new ConfigurationError(
            `duplicate report output directory: ${combinedReportOutputDirectory}`
        );
    }

    if (!registry.privateMetrics || typeof registry.privateMetrics !== "object"
        || Array.isArray(registry.privateMetrics)) {
        throw new ConfigurationError("registry.privateMetrics must be an object");
    }
    const privateMetricsStatePath = requireAbsolutePath(
        registry.privateMetrics.statePath,
        "registry.privateMetrics.statePath"
    );
    if (!isWithin(stateDirectory, privateMetricsStatePath)
        || privateMetricsStatePath === counterStatePath) {
        throw new ConfigurationError("private metrics statePath must be a distinct path below stateDirectory");
    }
    const privateMetricsReportOutputDirectory = requireAbsolutePath(
        registry.privateMetrics.reportOutputDirectory,
        "registry.privateMetrics.reportOutputDirectory"
    );
    if (!isWithin(stateDirectory, privateMetricsReportOutputDirectory)
        || outputDirectories.has(privateMetricsReportOutputDirectory)
        || privateMetricsReportOutputDirectory === combinedReportOutputDirectory) {
        throw new ConfigurationError("private metrics reportOutputDirectory must be distinct below stateDirectory");
    }
    if (registry.privateMetrics.timeZone !== "Europe/Berlin") {
        throw new ConfigurationError("registry.privateMetrics.timeZone must be Europe/Berlin");
    }
    if (registry.privateMetrics.detailRetentionDays !== 14) {
        throw new ConfigurationError("registry.privateMetrics.detailRetentionDays must be 14");
    }

    return {
        schemaVersion: 1,
        stateDirectory,
        counterStatePath,
        lockFile: requireAbsolutePath(registry.lockFile, "registry.lockFile"),
        geoIpCountryDatabase: requireAbsolutePath(
            registry.geoIpCountryDatabase,
            "registry.geoIpCountryDatabase"
        ),
        combined: {
            reportOutputDirectory: combinedReportOutputDirectory
        },
        privateMetrics: {
            statePath: privateMetricsStatePath,
            reportOutputDirectory: privateMetricsReportOutputDirectory,
            timeZone: "Europe/Berlin",
            detailRetentionDays: 14
        },
        sites
    };
}

function resolveAnalyticsLogs(site) {
    const logs = [];
    const candidates = [
        { path: `${site.analyticsLog}.1`, required: false },
        { path: site.analyticsLog, required: true }
    ];

    for (const candidate of candidates) {
        let stats;
        try {
            stats = fs.statSync(candidate.path);
        } catch (error) {
            if (!candidate.required && error.code === "ENOENT") {
                continue;
            }
            throw new ConfigurationError(`analytics log is not readable: ${candidate.path}`);
        }
        if (!stats.isFile()) {
            throw new ConfigurationError(`analytics log is not a file: ${candidate.path}`);
        }
        try {
            fs.accessSync(candidate.path, fs.constants.R_OK);
        } catch (error) {
            throw new ConfigurationError(
                `analytics log is not readable by the current user: ${candidate.path}`
            );
        }
        logs.push(candidate.path);
    }

    return logs;
}

function requireWritableDirectory(directory, label) {
    let stats;
    try {
        stats = fs.statSync(directory);
    } catch (error) {
        throw new ConfigurationError(`${label} does not exist: ${directory}`);
    }
    if (!stats.isDirectory()) {
        throw new ConfigurationError(`${label} is not a directory: ${directory}`);
    }
    try {
        fs.accessSync(directory, fs.constants.W_OK | fs.constants.X_OK);
    } catch (error) {
        throw new ConfigurationError(
            `${label} is not writable by the current user: ${directory}`
        );
    }
}

function validateInputs(registry, configTemplate) {
    const requiredConfigParts = [
        "{{DB_PATH}}",
        "{{RESTORE_DIRECTIVE}}",
        "{{GEOIP_COUNTRY_DATABASE}}",
        "persist true",
        "anonymize-ip true",
        "ignore-crawlers true",
        "unknowns-as-crawlers true",
        "keep-last 395"
    ];

    for (const required of requiredConfigParts) {
        if (!configTemplate.includes(required)) {
            throw new ConfigurationError(`GoAccess template is missing: ${required}`);
        }
    }

    const analyticsLogsBySite = new Map(registry.sites.map(site => [
        site.id,
        resolveAnalyticsLogs(site)
    ]));
    const updateLogsBySite = new Map(registry.sites.map(site => [
        site.id,
        resolveAnalyticsLogs({ analyticsLog: site.updateInfo.analyticsLog })
    ]));

    let geoStats;
    try {
        geoStats = fs.statSync(registry.geoIpCountryDatabase);
    } catch (error) {
        throw new ConfigurationError(
            `GeoIP Country database is not readable: ${registry.geoIpCountryDatabase}`
        );
    }
    if (!geoStats.isFile() || /city/i.test(path.basename(registry.geoIpCountryDatabase))) {
        throw new ConfigurationError("geoIpCountryDatabase must be a Country database file");
    }
    try {
        fs.accessSync(registry.geoIpCountryDatabase, fs.constants.R_OK);
    } catch (error) {
        throw new ConfigurationError(
            `GeoIP Country database is not readable by the current user: `
            + registry.geoIpCountryDatabase
        );
    }

    requireWritableDirectory(registry.stateDirectory, "stateDirectory");
    requireWritableDirectory(
        registry.combined.reportOutputDirectory,
        "combined report output directory"
    );
    requireWritableDirectory(
        registry.privateMetrics.reportOutputDirectory,
        "private metrics report output directory"
    );
    for (const site of registry.sites) {
        requireWritableDirectory(
            site.reportOutputDirectory,
            `report output directory for ${site.id}`
        );
        if (site.publicCounter) {
            requireWritableDirectory(
                path.dirname(site.publicJsonPath),
                `public output directory for ${site.id}`
            );
        }
    }

    return { analyticsLogsBySite, updateLogsBySite };
}

function renderGoAccessConfig(template, dbPath, restore, geoIpCountryDatabase) {
    const rendered = template
        .replaceAll("{{DB_PATH}}", dbPath)
        .replaceAll(
            "{{RESTORE_DIRECTIVE}}",
            restore ? "restore true" : "# restore is disabled until a database exists"
        )
        .replaceAll("{{GEOIP_COUNTRY_DATABASE}}", geoIpCountryDatabase);

    if (/{{[A-Z_]+}}/.test(rendered)) {
        throw new ConfigurationError("GoAccess template contains an unknown placeholder");
    }

    return rendered;
}

function defaultRunGoAccess({ binary, args }) {
    const result = spawnSync(binary, args, {
        encoding: "utf8",
        maxBuffer: 1024 * 1024,
        timeout: 30 * 60 * 1000,
        windowsHide: true
    });

    if (result.error) {
        throw new Error(`could not start GoAccess: ${result.error.message}`);
    }
    if (result.status !== 0) {
        const detail = (result.stderr || result.stdout || "no diagnostic output").trim();
        throw new Error(`GoAccess failed with exit code ${result.status}: ${detail}`);
    }
}

function defaultCheckGoAccess(binary) {
    const result = spawnSync(binary, ["--version"], {
        encoding: "utf8",
        timeout: 10000,
        windowsHide: true
    });

    if (result.error || result.status !== 0) {
        const detail = result.error
            ? result.error.message
            : (result.stderr || result.stdout || "no diagnostic output").trim();
        throw new ConfigurationError(`GoAccess is not available: ${detail}`);
    }

    return parseGoAccessVersion(`${result.stdout || ""}\n${result.stderr || ""}`);
}

function parseGoAccessVersion(output) {
    const versionMatch = /GoAccess\s+-\s+([0-9]+(?:\.[0-9]+)+)/i.exec(output);

    return {
        version: versionMatch ? versionMatch[1] : "unknown",
        geoIpMmdb: /--enable-geoip=mmdb\b/i.test(output),
        openSsl: /--with-openssl\b/i.test(output),
        zlib: /--with-zlib\b/i.test(output)
    };
}

function formatGoAccessCheck(capabilities) {
    const detected = [
        `GeoIP2/MMDB ${capabilities.geoIpMmdb ? "enabled" : "not detected"}`,
        `OpenSSL ${capabilities.openSsl ? "enabled" : "not detected"}`,
        `Zlib ${capabilities.zlib ? "enabled" : "not detected"}`
    ].join(", ");
    const summary = `GoAccess ${capabilities.version}: ${detected}.`;

    if (capabilities.zlib) {
        return summary;
    }

    return `${summary} This build has no Zlib support. That is valid for regular mode: `
        + "the current analytics log and its optional uncompressed .1 rotation are read "
        + "directly; older .gz logs are not imported.";
}

function parseGoAccessDate(value) {
    if (typeof value !== "string") {
        return null;
    }
    if (parseIsoDate(value)) {
        return value;
    }
    if (/^\d{8}$/.test(value)) {
        return parseIsoDate(`${value.slice(0, 4)}-${value.slice(4, 6)}-${value.slice(6, 8)}`);
    }

    const match = /^(\d{2})\/([A-Za-z]{3})\/(\d{4})$/.exec(value);
    const months = {
        Jan: "01", Feb: "02", Mar: "03", Apr: "04", May: "05", Jun: "06",
        Jul: "07", Aug: "08", Sep: "09", Oct: "10", Nov: "11", Dec: "12"
    };

    return match && months[match[2]]
        ? parseIsoDate(`${match[3]}-${months[match[2]]}-${match[1]}`)
        : null;
}

function validateReport(report, combined) {
    if (!report || typeof report !== "object" || Array.isArray(report)) {
        throw new Error("GoAccess JSON report must be an object");
    }
    const requiredPanels = ["visitors", "requests", "status_codes", "geolocation"];
    if (combined) {
        requiredPanels.push("vhosts");
    }
    if (!report.general || typeof report.general !== "object") {
        throw new Error("GoAccess JSON report has no general summary");
    }
    for (const panel of requiredPanels) {
        if (!report[panel] || !Array.isArray(report[panel].data)) {
            throw new Error(`GoAccess JSON report has no ${panel} panel`);
        }
    }

    extractDailyVisits(report);
    return report;
}

function readAndValidateReport(jsonPath, htmlPath, combined) {
    let report;
    try {
        report = JSON.parse(fs.readFileSync(jsonPath, "utf8"));
    } catch (error) {
        throw new Error(`GoAccess JSON output is invalid: ${error.message}`);
    }
    validateReport(report, combined);

    const html = fs.readFileSync(htmlPath, "utf8");
    if (html.length < 100 || !/<html(?:\s|>)/i.test(html)) {
        throw new Error("GoAccess HTML output is missing or implausibly small");
    }

    return report;
}

function extractDailyVisits(report) {
    const daily = {};

    for (const row of report.visitors.data) {
        const date = parseGoAccessDate(row && row.data);
        const visits = row && row.visitors && row.visitors.count;

        if (!date || !Number.isSafeInteger(visits) || visits < 0) {
            throw new Error("GoAccess visitors panel contains invalid daily data");
        }
        if (Object.hasOwn(daily, date)) {
            throw new Error(`GoAccess visitors panel contains duplicate date ${date}`);
        }
        daily[date] = visits;
    }

    return daily;
}

function loadCounterState(statePath) {
    if (!fs.existsSync(statePath)) {
        return { schemaVersion: STATE_SCHEMA_VERSION, sites: {} };
    }

    const state = readJson(statePath, "counter state");
    if (state.schemaVersion !== STATE_SCHEMA_VERSION
        || !state.sites || typeof state.sites !== "object" || Array.isArray(state.sites)) {
        throw new ConfigurationError("counter state has an unsupported structure");
    }

    for (const [siteId, site] of Object.entries(state.sites)) {
        if (!site || typeof site !== "object" || !parseIsoDate(site.since)
            || !site.dailyVisits || typeof site.dailyVisits !== "object"
            || Array.isArray(site.dailyVisits)) {
            throw new ConfigurationError(`counter state for ${siteId} is invalid`);
        }
        for (const [date, visits] of Object.entries(site.dailyVisits)) {
            if (!parseIsoDate(date) || !Number.isSafeInteger(visits) || visits < 0) {
                throw new ConfigurationError(`counter state value for ${siteId}/${date} is invalid`);
            }
        }
    }

    return state;
}

function updateCounterState(state, site, report) {
    const current = state.sites[site.id];
    if (current && current.since !== site.activatedOn) {
        throw new ConfigurationError(
            `activation date for ${site.id} differs from the existing counter state`
        );
    }
    if (current && current.hostname !== site.hostname) {
        throw new ConfigurationError(
            `hostname for ${site.id} differs from the existing counter state`
        );
    }

    const dailyVisits = current ? { ...current.dailyVisits } : {};
    for (const [date, visits] of Object.entries(extractDailyVisits(report))) {
        if (date >= site.activatedOn) {
            dailyVisits[date] = visits;
        }
    }
    state.sites[site.id] = {
        hostname: site.hostname,
        since: site.activatedOn,
        dailyVisits
    };
}

function publicPayload(state, site, now) {
    const siteState = state.sites[site.id];
    let visits = 0;

    for (const [date, value] of Object.entries(siteState.dailyVisits)) {
        if (date >= site.activatedOn) {
            visits += value;
            if (!Number.isSafeInteger(visits) || visits > MAX_SAFE_INTEGER) {
                throw new Error(`public visit total for ${site.id} exceeds the safe integer range`);
            }
        }
    }

    return {
        schemaVersion: PUBLIC_SCHEMA_VERSION,
        visits,
        since: site.activatedOn,
        updatedAt: now.toISOString().replace(/\.\d{3}Z$/, "Z")
    };
}

function atomicWriteFile(destination, content, mode = 0o640) {
    const directory = path.dirname(destination);
    const temporary = path.join(
        directory,
        `.${path.basename(destination)}.${process.pid}.${Date.now()}.tmp`
    );
    const handle = fs.openSync(temporary, "wx", mode);

    try {
        fs.writeFileSync(handle, content);
        fs.fchmodSync(handle, mode);
        fs.fsyncSync(handle);
    } finally {
        fs.closeSync(handle);
    }
    try {
        fs.renameSync(temporary, destination);
    } catch (error) {
        fs.rmSync(temporary, { force: true });
        throw error;
    }
}

function atomicCopyFile(source, destination) {
    atomicWriteFile(destination, fs.readFileSync(source));
}

function addPrivateMetricsLink(html) {
    const link = '<p style="margin:1rem"><a href="/metrics/">Private daily and annual metrics</a></p>';
    return /<\/body>/i.test(html) ? html.replace(/<\/body>/i, `${link}</body>`) : `${html}\n${link}\n`;
}

function replaceDirectory(source, destination) {
    fs.mkdirSync(path.dirname(destination), { recursive: true });
    const backup = `${destination}.previous-${process.pid}`;
    const hadDestination = fs.existsSync(destination);

    if (fs.existsSync(backup)) {
        throw new Error(`stale database backup blocks replacement: ${backup}`);
    }

    try {
        if (hadDestination) {
            fs.renameSync(destination, backup);
        }
        fs.renameSync(source, destination);
        if (hadDestination) {
            fs.rmSync(backup, { recursive: true, force: true });
        }
    } catch (error) {
        if (!fs.existsSync(destination) && fs.existsSync(backup)) {
            fs.renameSync(backup, destination);
        }
        throw error;
    }
}

function acquireLock(lockPath) {
    fs.mkdirSync(path.dirname(lockPath), { recursive: true });
    let descriptor;

    try {
        descriptor = fs.openSync(lockPath, "wx", 0o640);
        fs.writeFileSync(descriptor, `${process.pid}\n`);
    } catch (error) {
        throw new LockError(`another analytics run is active (${lockPath})`);
    }

    return () => {
        fs.closeSync(descriptor);
        fs.rmSync(lockPath, { force: true });
    };
}

function createReportJob(id, logs, outputDirectory, combined) {
    return { id, logs, outputDirectory, combined };
}

function prepareReport(job, context) {
    const jobDirectory = path.join(context.runDirectory, job.id);
    const stagedDb = path.join(jobDirectory, "db");
    const currentDb = path.join(context.registry.stateDirectory, "db", job.id);
    const outputJson = path.join(jobDirectory, "report.json");
    const outputHtml = path.join(jobDirectory, "report.html");
    const runConfig = path.join(jobDirectory, "goaccess.conf");
    let hasDatabase = false;

    if (fs.existsSync(currentDb)) {
        if (!fs.statSync(currentDb).isDirectory()) {
            throw new Error(`GoAccess database path is not a directory: ${currentDb}`);
        }
        hasDatabase = fs.readdirSync(currentDb).length > 0;
    }

    fs.mkdirSync(jobDirectory, { recursive: true });
    if (hasDatabase) {
        fs.cpSync(currentDb, stagedDb, { recursive: true, errorOnExist: true });
    } else {
        fs.mkdirSync(stagedDb);
    }
    fs.writeFileSync(runConfig, renderGoAccessConfig(
        context.configTemplate,
        stagedDb,
        hasDatabase,
        context.registry.geoIpCountryDatabase
    ));

    const args = [
        ...job.logs,
        "--no-global-config",
        "--config-file", runConfig
    ];
    if (job.combined) {
        args.push("--enable-panel=VIRTUAL_HOSTS");
    }
    args.push(
        "--output", outputJson,
        "--output", outputHtml
    );
    context.runGoAccess({
        binary: context.goaccessBinary,
        args,
        id: job.id,
        combined: job.combined,
        outputJson,
        outputHtml,
        dbPath: stagedDb
    });

    return {
        ...job,
        report: readAndValidateReport(outputJson, outputHtml, job.combined),
        outputJson,
        outputHtml,
        stagedDb,
        currentDb
    };
}

function generateReports(options, dependencies = {}) {
    const registryPath = path.resolve(options.registryPath);
    const configTemplatePath = path.resolve(options.configTemplatePath);
    const registry = validateRegistry(readJson(registryPath, "site registry"));
    const configTemplate = fs.readFileSync(configTemplatePath, "utf8");
    const runGoAccess = dependencies.runGoAccess || defaultRunGoAccess;
    const checkGoAccess = dependencies.checkGoAccess || defaultCheckGoAccess;
    const now = dependencies.now ? dependencies.now() : new Date();

    const { analyticsLogsBySite, updateLogsBySite } = validateInputs(registry, configTemplate);
    try {
        validateImportOptions(options, registry);
    } catch (error) {
        throw new ConfigurationError(error.message);
    }
    const goAccess = checkGoAccess(options.goaccessBinary || "goaccess");
    if (!goAccess || !goAccess.geoIpMmdb) {
        throw new ConfigurationError(
            "GoAccess must be built with GeoIP2/MMDB support (--enable-geoip=mmdb)"
        );
    }
    if (options.check) {
        return { checked: true, sites: registry.sites.length, goAccess };
    }

    const releaseLock = options.dryRun || dependencies.skipLock
        ? () => {}
        : acquireLock(registry.lockFile);
    let runDirectory;

    try {
        runDirectory = fs.mkdtempSync(path.join(
            options.dryRun ? os.tmpdir() : registry.stateDirectory,
            ".analytics-run-"
        ));

        const jobs = registry.sites.map(site => createReportJob(
            site.id,
            analyticsLogsBySite.get(site.id),
            site.reportOutputDirectory,
            false
        ));
        jobs.push(createReportJob(
            "combined",
            registry.sites.flatMap(site => analyticsLogsBySite.get(site.id)),
            registry.combined.reportOutputDirectory,
            true
        ));

        const context = {
            registry,
            configTemplate,
            goaccessBinary: options.goaccessBinary || "goaccess",
            runGoAccess,
            runDirectory
        };
        const prepared = jobs.map(job => prepareReport(job, context));
        const state = loadCounterState(registry.counterStatePath);

        for (const site of registry.sites.filter(entry => entry.publicCounter)) {
            const generated = prepared.find(entry => entry.id === site.id);
            updateCounterState(state, site, generated.report);
        }

        const publicFiles = registry.sites
            .filter(site => site.publicCounter)
            .map(site => ({
                destination: site.publicJsonPath,
                content: `${JSON.stringify(publicPayload(state, site, now), null, 2)}\n`
            }));

        const privateMetrics = generatePrivateMetrics({
            registry,
            analyticsLogsBySite,
            updateLogsBySite,
            options,
            visitState: state,
            context: {
                ...context,
                now
            }
        });

        if (!options.dryRun) {
            for (const report of prepared) {
                atomicCopyFile(report.outputJson, path.join(report.outputDirectory, "report.json"));
                atomicWriteFile(
                    path.join(report.outputDirectory, "report.html"),
                    addPrivateMetricsLink(fs.readFileSync(report.outputHtml, "utf8"))
                );
            }
            for (const report of prepared) {
                replaceDirectory(report.stagedDb, report.currentDb);
            }
            atomicWriteFile(
                registry.counterStatePath,
                `${JSON.stringify(state, null, 2)}\n`
            );
            for (const publicFile of publicFiles) {
                atomicWriteFile(publicFile.destination, publicFile.content, 0o644);
            }
            atomicWriteFile(
                registry.privateMetrics.statePath,
                `${JSON.stringify(privateMetrics.state, null, 2)}\n`
            );
            for (const report of privateMetrics.files) {
                fs.mkdirSync(path.dirname(report.destination), { recursive: true });
                atomicWriteFile(report.destination, report.content);
            }
        }

        return {
            checked: false,
            dryRun: Boolean(options.dryRun),
            sites: registry.sites.length,
            reports: prepared.length,
            publicCounters: publicFiles.length,
            privateMetricReports: privateMetrics.files.length,
            historicalImport: privateMetrics.imported
        };
    } finally {
        if (runDirectory && fs.existsSync(runDirectory)) {
            fs.rmSync(runDirectory, { recursive: true, force: true });
        }
        releaseLock();
    }
}

function parseArguments(argv) {
    const options = {
        check: false,
        dryRun: false,
        goaccessBinary: "goaccess",
        importLogs: []
    };

    for (let index = 0; index < argv.length; index += 1) {
        const argument = argv[index];
        if (argument === "--check") {
            options.check = true;
        } else if (argument === "--dry-run") {
            options.dryRun = true;
        } else if ([
            "--registry", "--config-template", "--goaccess", "--import-log", "--import-format",
            "--import-site", "--coverage-from", "--coverage-through"
        ].includes(argument)) {
            const value = argv[index + 1];
            if (!value) {
                throw new ConfigurationError(`${argument} requires a value`);
            }
            index += 1;
            if (argument === "--registry") options.registryPath = value;
            if (argument === "--config-template") options.configTemplatePath = value;
            if (argument === "--goaccess") options.goaccessBinary = value;
            if (argument === "--import-log") options.importLogs.push(value);
            if (argument === "--import-format") options.importFormat = value;
            if (argument === "--import-site") options.importSite = value;
            if (argument === "--coverage-from") options.coverageFrom = value;
            if (argument === "--coverage-through") options.coverageThrough = value;
        } else {
            throw new ConfigurationError(`unknown argument: ${argument}`);
        }
    }

    if (!options.registryPath || !options.configTemplatePath) {
        throw new ConfigurationError(
            "usage: generate-reports.js --registry FILE --config-template FILE "
            + "[--goaccess FILE] [--check|--dry-run] "
            + "[--import-log FILE ... --import-format nginx-combined|analytics-tsv "
            + "--import-site ID --coverage-from DATE --coverage-through DATE]"
        );
    }
    if (options.check && options.dryRun) {
        throw new ConfigurationError("--check and --dry-run are mutually exclusive");
    }

    return options;
}

if (require.main === module) {
    try {
        const result = generateReports(parseArguments(process.argv.slice(2)));
        const action = result.checked ? "Configuration check" : "Analytics generation";
        if (result.checked) {
            process.stdout.write(`${formatGoAccessCheck(result.goAccess)}\n`);
        }
        process.stdout.write(`${action} completed: ${JSON.stringify(result)}\n`);
    } catch (error) {
        process.stderr.write(`analytics: ${error.message}\n`);
        process.exitCode = error.exitCode || 1;
    }
}

module.exports = {
    ConfigurationError,
    extractDailyVisits,
    formatGoAccessCheck,
    generateReports,
    parseGoAccessVersion,
    parseArguments,
    publicPayload,
    updateCounterState,
    validateRegistry,
    validateReport
};
