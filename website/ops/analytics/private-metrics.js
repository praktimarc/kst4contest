"use strict";

const fs = require("node:fs");
const path = require("node:path");
const zlib = require("node:zlib");

const PRIVATE_STATE_SCHEMA_VERSION = 1;
const UPDATE_INFO_PATH = "/kst4ContestVersionInfo.xml";
const UNKNOWN_COUNTRY = "Unknown";
const MAX_IMPORT_BYTES = 1024 * 1024 * 1024;

function parseIsoDate(value) {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value || "");
    if (!match) return null;
    const date = new Date(Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3])));
    return date.getUTCFullYear() === Number(match[1])
        && date.getUTCMonth() === Number(match[2]) - 1
        && date.getUTCDate() === Number(match[3])
        ? value
        : null;
}

function dateRange(from, through) {
    const result = [];
    const current = new Date(`${from}T12:00:00Z`);
    const end = new Date(`${through}T12:00:00Z`);
    while (current <= end) {
        result.push(current.toISOString().slice(0, 10));
        current.setUTCDate(current.getUTCDate() + 1);
    }
    return result;
}

function shiftDate(date, days) {
    const value = new Date(`${date}T12:00:00Z`);
    value.setUTCDate(value.getUTCDate() + days);
    return value.toISOString().slice(0, 10);
}

function localParts(timestamp, timeZone) {
    const instant = timestamp instanceof Date ? timestamp : new Date(timestamp);
    if (Number.isNaN(instant.getTime())) return null;
    const parts = Object.fromEntries(new Intl.DateTimeFormat("en-CA", {
        timeZone,
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        hourCycle: "h23"
    }).formatToParts(instant).filter(part => part.type !== "literal")
        .map(part => [part.type, part.value]));
    return { date: `${parts.year}-${parts.month}-${parts.day}`, hour: parts.hour };
}

function normalizePath(value) {
    if (typeof value !== "string" || /[\r\n\0]/.test(value)) return null;
    const withoutQuery = value.split(/[?#]/, 1)[0];
    if (!withoutQuery.startsWith("/")) return null;
    const collapsed = withoutQuery.replace(/\/{2,}/g, "/");
    const trailingSlash = collapsed.length > 1 && collapsed.endsWith("/");
    const normalized = path.posix.normalize(collapsed);
    return trailingSlash && normalized !== "/" && !normalized.endsWith("/")
        ? `${normalized}/`
        : normalized;
}

function unescapeNginx(value) {
    return value.replace(/\\x([0-9A-Fa-f]{2})/g, (_match, hex) => String.fromCharCode(Number.parseInt(hex, 16)))
        .replace(/\\"/g, "\"")
        .replace(/\\\\/g, "\\");
}

function parseAnalyticsLine(line, timeZone = "Europe/Berlin") {
    const fields = line.split("\t");
    if (fields.length !== 9) return null;
    const status = Number(fields[6]);
    const bytes = fields[7] === "-" ? 0 : Number(fields[7]);
    const local = localParts(fields[2], timeZone);
    const requestPath = normalizePath(fields[4]);
    if (!local || !requestPath || !/^\d{3}$/.test(fields[6])
        || !Number.isSafeInteger(bytes) || bytes < 0) return null;
    let userAgent = fields[8];
    if (userAgent.startsWith("\"") && userAgent.endsWith("\"")) {
        userAgent = userAgent.slice(1, -1);
    }
    const record = {
        host: fields[0],
        ip: fields[1],
        timestamp: fields[2],
        date: local.date,
        hour: local.hour,
        method: fields[3],
        path: requestPath,
        protocol: fields[5],
        status,
        bytes,
        userAgent: unescapeNginx(userAgent)
    };
    record.line = toAnalyticsLine(record);
    return record;
}

const NGINX_MONTHS = {
    Jan: "01", Feb: "02", Mar: "03", Apr: "04", May: "05", Jun: "06",
    Jul: "07", Aug: "08", Sep: "09", Oct: "10", Nov: "11", Dec: "12"
};

function parseCombinedLine(line, hostname, timeZone = "Europe/Berlin") {
    const match = /^(\S+) \S+ \S+ \[(\d{2})\/([A-Za-z]{3})\/(\d{4}):(\d{2}):(\d{2}):(\d{2}) ([+-]\d{4})\] "([A-Z]+) ([^ ]+) ([^"]+)" (\d{3}) (\d+|-) "(?:[^"\\]|\\.)*" "((?:[^"\\]|\\.)*)"(?: .*)?$/.exec(line);
    if (!match || !NGINX_MONTHS[match[3]]) return null;
    const timestamp = `${match[4]}-${NGINX_MONTHS[match[3]]}-${match[2]}T${match[5]}:${match[6]}:${match[7]}${match[8]}`;
    const local = localParts(timestamp, timeZone);
    const requestPath = normalizePath(match[10]);
    const status = Number(match[12]);
    const bytes = match[13] === "-" ? 0 : Number(match[13]);
    if (!local || !requestPath || !Number.isSafeInteger(bytes) || bytes < 0) return null;
    const userAgent = unescapeNginx(match[14]);
    const canonicalTimestamp = timestamp.replace(/([+-]\d{2})(\d{2})$/, "$1:$2");
    return {
        host: hostname,
        ip: match[1],
        timestamp: canonicalTimestamp,
        date: local.date,
        hour: local.hour,
        method: match[9],
        path: requestPath,
        protocol: match[11],
        status,
        bytes,
        userAgent,
        line: toAnalyticsLine({
            host: hostname,
            ip: match[1],
            timestamp: canonicalTimestamp,
            method: match[9],
            path: requestPath,
            protocol: match[11],
            status,
            bytes,
            userAgent
        })
    };
}

function escapeLogField(value) {
    return value.replace(/\\/g, "\\\\").replace(/"/g, "\\\"")
        .replace(/[\t\r\n]/g, character => `\\x${character.charCodeAt(0).toString(16).padStart(2, "0")}`);
}

function toAnalyticsLine(record) {
    return [record.host, record.ip, record.timestamp, record.method, record.path,
        record.protocol, record.status, record.bytes, `"${escapeLogField(record.userAgent)}"`].join("\t");
}

function isKnownBot(userAgent) {
    return /(?:bot|crawler|spider|slurp|headless|monitor|healthcheck|uptime|wget|curl)/i.test(userAgent);
}

function isEligibleWebsiteRequest(record) {
    if (record.method !== "GET" || isKnownBot(record.userAgent)) return false;
    if (["/visitor-count.json", UPDATE_INFO_PATH, "/sitemap.xml", "/robots.txt", "/favicon.ico",
        "/assets/favicon.svg", "/health", "/healthz", "/ping", "/status"].includes(record.path)) {
        return false;
    }
    return !/^\/(?:assets|manual\/assets)\//i.test(record.path)
        && !/\.(?:css|js|mjs|map|json|png|jpe?g|gif|svg|webp|avif|ico|woff2?|ttf|otf|eot|xml|txt|pdf|zip|gz|wasm|mp4|webm)$/i.test(record.path);
}

function isUpdateRequest(record) {
    return record.method === "GET" && record.status === 200 && record.path === UPDATE_INFO_PATH;
}

function clientGroup(userAgent) {
    if (!userAgent || userAgent === "-") return "Missing user agent";
    if (/KST4Contest/i.test(userAgent)) return "KST4Contest (explicit)";
    if (/(?:Edg|Edge)\//i.test(userAgent)) return "Microsoft Edge";
    if (/Firefox\//i.test(userAgent)) return "Firefox";
    if (/(?:Chrome|Chromium)\//i.test(userAgent)) return "Chrome/Chromium";
    if (/Safari\//i.test(userAgent)) return "Safari";
    if (/^Java\//i.test(userAgent)) return "Java runtime (application unknown)";
    if (/curl\//i.test(userAgent)) return "curl";
    if (/Wget\//i.test(userAgent)) return "Wget";
    if (/(?:bot|crawler|spider|slurp)/i.test(userAgent)) return "Bot/crawler";
    return "Other or unrecognised";
}

function readLogFile(filePath) {
    const stats = fs.statSync(filePath);
    if (!stats.isFile()) throw new Error(`log input is not a file: ${filePath}`);
    if (stats.size > MAX_IMPORT_BYTES) throw new Error(`log input exceeds the 1 GiB safety limit: ${filePath}`);
    const content = fs.readFileSync(filePath);
    try {
        return filePath.toLowerCase().endsWith(".gz")
            ? zlib.gunzipSync(content, { maxOutputLength: MAX_IMPORT_BYTES }).toString("utf8")
            : content.toString("utf8");
    } catch (error) {
        throw new Error(`could not read compressed log ${filePath}: ${error.message}`);
    }
}

function parseLogFiles(filePaths, parser, { deduplicateAcrossFiles = false, label = "log" } = {}) {
    const parsedFiles = [];
    for (const filePath of filePaths) {
        const records = [];
        const lines = readLogFile(filePath).split(/\r?\n/);
        for (let index = 0; index < lines.length; index += 1) {
            if (lines[index] === "" && index === lines.length - 1) continue;
            if (lines[index].trim() === "") continue;
            const record = parser(lines[index]);
            if (!record) throw new Error(`${label} has an invalid line at ${filePath}:${index + 1}`);
            records.push(record);
        }
        parsedFiles.push(records);
    }
    if (!deduplicateAcrossFiles) return parsedFiles.flat();

    const maxima = new Map();
    for (const records of parsedFiles) {
        const counts = new Map();
        for (const record of records) counts.set(record.line, (counts.get(record.line) || 0) + 1);
        for (const [line, count] of counts) maxima.set(line, Math.max(maxima.get(line) || 0, count));
    }
    const byLine = new Map(parsedFiles.flat().map(record => [record.line, record]));
    const result = [];
    for (const [line, count] of maxima) {
        for (let index = 0; index < count; index += 1) result.push({ ...byLine.get(line) });
    }
    return result;
}

function addCount(values, key, count) {
    const value = (Object.hasOwn(values, key) ? values[key] : 0) + count;
    Object.defineProperty(values, key, {
        value,
        writable: true,
        enumerable: true,
        configurable: true
    });
}

function countPanel(report, panelName) {
    const panel = report[panelName];
    if (!panel || !Array.isArray(panel.data)) throw new Error(`GoAccess JSON report has no ${panelName} panel`);
    const values = {};
    for (const row of panel.data) {
        const rawName = typeof row.data === "string" ? row.data.trim() : "";
        const name = !rawName || /^(?:unknown|n\/a|-|\(not set\))$/i.test(rawName)
            ? UNKNOWN_COUNTRY
            : rawName;
        const count = row && row.hits && row.hits.count;
        if (!Number.isSafeInteger(count) || count < 0) throw new Error(`GoAccess ${panelName} panel contains invalid data`);
        addCount(values, name, count);
    }
    return values;
}

function countCountries(report) {
    const panel = report.geolocation;
    if (!panel || !Array.isArray(panel.data)) {
        throw new Error("GoAccess JSON report has no geolocation panel");
    }
    const rows = panel.data.flatMap(row => Array.isArray(row.items) && row.items.length
        ? row.items
        : [row]);
    return countPanel({ geolocation: { data: rows } }, "geolocation");
}

function sumValues(values) {
    return Object.values(values).reduce((sum, value) => sum + value, 0);
}

function aggregateGoAccessReport(report, kind) {
    const total = report && report.general && report.general.total_requests;
    if (!Number.isSafeInteger(total) || total < 0) throw new Error("GoAccess report contains an invalid total request count");
    const countries = countCountries(report);
    const countryTotal = sumValues(countries);
    if (countryTotal > total) throw new Error("GoAccess country total exceeds the request total");
    if (countryTotal < total) addCount(countries, UNKNOWN_COUNTRY, total - countryTotal);

    if (kind === "updateInfo") return { requests: total, countries };
    const paths = countPanel(report, "requests");
    if (sumValues(paths) !== total) {
        throw new Error("website page total differs from the GoAccess request-panel definition");
    }
    return { pageViews: total, countries, paths };
}

function renderMetricsConfig(template, dbPath, geoIpCountryDatabase, includeCrawlers) {
    let rendered = template
        .replaceAll("{{DB_PATH}}", dbPath)
        .replaceAll("{{RESTORE_DIRECTIVE}}", "# private daily aggregation uses a fresh database")
        .replaceAll("{{GEOIP_COUNTRY_DATABASE}}", geoIpCountryDatabase)
        .replace(/^max-items\s+\d+$/m, "max-items 1000000")
        .replace(/^persist true$/m, "# persistence is disabled for private daily aggregation");
    if (includeCrawlers) {
        rendered = rendered.replace(/^ignore-crawlers true$/m, "ignore-crawlers false")
            .replace(/^unknowns-as-crawlers true$/m, "unknowns-as-crawlers false");
    }
    return rendered;
}

function aggregateDays({ records, dates, kind, site, context }) {
    const byDate = new Map(dates.map(date => [date, []]));
    for (const record of records) {
        if (byDate.has(record.date)) byDate.get(record.date).push(record);
    }
    const daily = {};
    for (const date of dates) {
        const dayRecords = byDate.get(date);
        if (dayRecords.length === 0) {
            daily[date] = kind === "website"
                ? { pageViews: 0, countries: {}, paths: {} }
                : { requests: 0, countries: {}, hours: {}, clients: {} };
            continue;
        }
        const jobId = `metrics-${site.id}-${kind}-${date}`;
        const jobDirectory = path.join(context.runDirectory, jobId);
        const dbPath = path.join(jobDirectory, "db");
        const inputPath = path.join(jobDirectory, "input.log");
        const outputJson = path.join(jobDirectory, "report.json");
        const runConfig = path.join(jobDirectory, "goaccess.conf");
        fs.mkdirSync(dbPath, { recursive: true });
        fs.writeFileSync(inputPath, `${dayRecords.map(record => record.line).join("\n")}\n`, { mode: 0o600 });
        fs.writeFileSync(runConfig, renderMetricsConfig(
            context.configTemplate,
            dbPath,
            context.registry.geoIpCountryDatabase,
            kind === "updateInfo"
        ), { mode: 0o600 });
        context.runGoAccess({
            binary: context.goaccessBinary,
            args: [inputPath, "--no-global-config", "--config-file", runConfig, "--output", outputJson],
            id: jobId,
            metricKind: kind,
            date,
            outputJson,
            dbPath
        });
        let report;
        try {
            report = JSON.parse(fs.readFileSync(outputJson, "utf8"));
        } catch (error) {
            throw new Error(`private metric GoAccess JSON is invalid for ${site.id}/${date}: ${error.message}`);
        }
        daily[date] = aggregateGoAccessReport(report, kind);
        if (kind === "updateInfo") {
            const hours = {};
            const clients = {};
            for (const record of dayRecords) {
                addCount(hours, record.hour, 1);
                const group = clientGroup(record.userAgent);
                addCount(clients, group, 1);
            }
            if (sumValues(hours) !== daily[date].requests || sumValues(clients) !== daily[date].requests) {
                throw new Error(`update detail total differs from GoAccess for ${site.id}/${date}`);
            }
            daily[date].hours = hours;
            daily[date].clients = clients;
        }
    }
    return daily;
}

function loadState(statePath) {
    if (!fs.existsSync(statePath)) return { schemaVersion: PRIVATE_STATE_SCHEMA_VERSION, sites: {} };
    let state;
    try {
        state = JSON.parse(fs.readFileSync(statePath, "utf8"));
    } catch (error) {
        throw new Error(`private metrics state is not valid JSON: ${error.message}`);
    }
    if (!state || state.schemaVersion !== PRIVATE_STATE_SCHEMA_VERSION || !state.sites
        || typeof state.sites !== "object" || Array.isArray(state.sites)) {
        throw new Error("private metrics state has an unsupported structure");
    }
    return state;
}

function containsAtLeast(candidate, current) {
    const keys = new Set([...Object.keys(candidate), ...Object.keys(current)]);
    return [...keys].every(key => (candidate[key] || 0) >= (current[key] || 0));
}

function sameCounts(left, right) {
    const keys = new Set([...Object.keys(left), ...Object.keys(right)]);
    return [...keys].every(key => (left[key] || 0) === (right[key] || 0));
}

function mergeDay(current, candidate, totalKey, label) {
    if (!current) return candidate;
    const currentTotal = current[totalKey];
    const candidateTotal = candidate[totalKey];
    if (candidateTotal === currentTotal) {
        if (!sameCounts(current.countries, candidate.countries)
            || (totalKey === "pageViews" && !sameCounts(current.paths, candidate.paths))) {
            throw new Error(`overlapping ${label} aggregates disagree at equal totals`);
        }
        return { ...candidate, ...(current.hours ? { hours: current.hours, clients: current.clients } : {}) };
    }
    const candidateLarger = candidateTotal > currentTotal;
    const larger = candidateLarger ? candidate : current;
    const smaller = candidateLarger ? current : candidate;
    if (!containsAtLeast(larger.countries, smaller.countries)
        || (totalKey === "pageViews" && !containsAtLeast(larger.paths, smaller.paths))) {
        throw new Error(`overlapping ${label} aggregates are not monotonic`);
    }
    return larger;
}

function mergeSeries(target, incoming, totalKey, label) {
    for (const [date, candidate] of Object.entries(incoming)) {
        target[date] = mergeDay(target[date], candidate, totalKey, `${label}/${date}`);
    }
}

function purgeDetails(siteState, today, retentionDays) {
    const firstRetained = shiftDate(today, -(retentionDays - 1));
    for (const [date, value] of Object.entries(siteState.updateInfo.daily)) {
        if (date < firstRetained) {
            delete value.hours;
            delete value.clients;
        }
    }
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, character => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;"
    })[character]);
}

function page(title, body) {
    return `<!doctype html>\n<html lang="en"><head><meta charset="utf-8">\n`
        + `<meta name="viewport" content="width=device-width,initial-scale=1">\n`
        + `<meta name="robots" content="noindex,nofollow"><title>${escapeHtml(title)}</title>\n`
        + `<style>body{font:16px/1.5 system-ui,sans-serif;max-width:1100px;margin:2rem auto;padding:0 1rem;color:#18202a}`
        + `nav a{margin-right:1rem}table{border-collapse:collapse;width:100%;margin:1rem 0 2rem}`
        + `th,td{border:1px solid #ccd2d8;padding:.35rem .5rem;text-align:left;vertical-align:top}`
        + `th{background:#eef1f4}td.num{text-align:right;font-variant-numeric:tabular-nums}`
        + `.note{color:#4a5560}.scroll{overflow-x:auto}</style></head><body>`
        + `<nav><a href="/combined/">GoAccess combined</a><a href="/metrics/">Private metrics</a></nav>`
        + body + `</body></html>\n`;
}

function table(headers, rows) {
    return `<div class="scroll"><table><thead><tr>${headers.map(value => `<th>${escapeHtml(value)}</th>`).join("")}</tr></thead>`
        + `<tbody>${rows.length ? rows.map(row => `<tr>${row.map((value, index) => `<td${index === row.length - 1 ? " class=\"num\"" : ""}>${escapeHtml(value)}</td>`).join("")}</tr>`).join("") : `<tr><td colspan="${headers.length}">No data</td></tr>`}</tbody></table></div>`;
}

function sortedEntries(values) {
    return Object.entries(values).sort(([left], [right]) => left.localeCompare(right, "en"));
}

function annualTotals(daily, totalKey) {
    const years = {};
    for (const [date, value] of Object.entries(daily)) {
        const year = date.slice(0, 4);
        if (!years[year]) years[year] = { total: 0, countries: {}, paths: {} };
        years[year].total += value[totalKey];
        for (const [country, count] of Object.entries(value.countries)) {
            addCount(years[year].countries, country, count);
        }
        for (const [requestPath, count] of Object.entries(value.paths || {})) {
            addCount(years[year].paths, requestPath, count);
        }
    }
    return years;
}

function coverageText(series) {
    return series.firstCoveredOn
        ? `The first covered day is ${escapeHtml(series.firstCoveredOn)}. Earlier dates are unknown, not zero.`
        : "No covered day has been recorded yet.";
}

function renderDaily(site, kind, visitSite = null) {
    const series = site[kind];
    const dailyVisits = visitSite ? visitSite.dailyVisits : {};
    const totalKey = kind === "website" ? "pageViews" : "requests";
    const label = kind === "website" ? "Website page views" : "Update-information requests";
    const dates = [...new Set([
        ...Object.keys(series.daily),
        ...(kind === "website" ? Object.keys(dailyVisits) : [])
    ])].sort();
    const totals = dates.map(date => kind === "website"
        ? [date, series.daily[date] ? series.daily[date][totalKey] : "unknown", dailyVisits[date] ?? "unknown"]
        : [date, series.daily[date][totalKey]]);
    const metricDates = Object.keys(series.daily).sort();
    const countries = metricDates.flatMap(date => sortedEntries(series.daily[date].countries)
        .map(([country, count]) => [date, country, count]));
    let details = table(kind === "website"
        ? ["Date", label, "Approximate visits"]
        : ["Date", label], totals) + `<h2>Countries by day</h2>`
        + table(["Date", "Country", kind === "website" ? "Page views" : "Requests"], countries);
    if (kind === "website") {
        const paths = metricDates.flatMap(date => sortedEntries(series.daily[date].paths)
            .map(([requestPath, count]) => [date, requestPath, count]));
        details += `<h2>Pages by day</h2>${table(["Date", "Path", "Page views"], paths)}`;
    } else {
        const hours = dates.flatMap(date => sortedEntries(series.daily[date].hours || {})
            .map(([hour, count]) => [date, `${hour}:00–${hour}:59`, count]));
        const clients = dates.flatMap(date => sortedEntries(series.daily[date].clients || {})
            .map(([client, count]) => [date, client, count]));
        details += `<h2>Hourly detail (last 14 days)</h2>${table(["Date", "Hour (Europe/Berlin)", "Requests"], hours)}`
            + `<h2>Recognisable client groups (last 14 days)</h2>`
            + `<p class="note">Existing KST4Contest versions do not send a reliable application-specific user agent. A Java user agent therefore identifies only a Java runtime, not a KST4Contest start or user.</p>`
            + table(["Date", "Client group", "Requests"], clients);
    }
    const visitCoverage = kind === "website" && visitSite
        ? ` Approximate visits have their own coverage beginning ${escapeHtml(visitSite.since)}.`
        : "";
    return page(`${label} by day`, `<h1>${label}: daily view</h1><p class="note">${coverageText(series)}${visitCoverage}</p>${details}`);
}

function renderAnnual(site, kind, visitSite = null) {
    const series = site[kind];
    const dailyVisits = visitSite ? visitSite.dailyVisits : {};
    const totalKey = kind === "website" ? "pageViews" : "requests";
    const label = kind === "website" ? "Website page views" : "Update-information requests";
    const years = annualTotals(series.daily, totalKey);
    const visitYears = {};
    for (const [date, count] of Object.entries(dailyVisits)) {
        visitYears[date.slice(0, 4)] = (visitYears[date.slice(0, 4)] || 0) + count;
    }
    const allYears = [...new Set([
        ...Object.keys(years),
        ...(kind === "website" ? Object.keys(visitYears) : [])
    ])].sort();
    const summary = allYears.map(year => kind === "website"
        ? [year, years[year] ? years[year].total : "unknown", visitYears[year] ?? "unknown"]
        : [year, years[year].total]);
    const allDates = [...new Set([
        ...Object.keys(series.daily),
        ...(kind === "website" ? Object.keys(dailyVisits) : [])
    ])].sort();
    const trend = allDates.map(date => kind === "website"
        ? [date, series.daily[date] ? series.daily[date][totalKey] : "unknown", dailyVisits[date] ?? "unknown"]
        : [date, series.daily[date][totalKey]]);
    const countries = sortedEntries(years).flatMap(([year, value]) => sortedEntries(value.countries)
        .map(([country, count]) => [year, country, count]));
    let details = table(kind === "website"
        ? ["Year", label, "Approximate visits"]
        : ["Year", label], summary) + `<h2>Daily values by year</h2>`
        + table(kind === "website"
            ? ["Date", label, "Approximate visits"]
            : ["Date", label], trend) + `<h2>Country totals by year</h2>`
        + table(["Year", "Country", kind === "website" ? "Page views" : "Requests"], countries);
    if (kind === "website") {
        const paths = sortedEntries(years).flatMap(([year, value]) => sortedEntries(value.paths)
            .map(([requestPath, count]) => [year, requestPath, count]));
        details += `<h2>Page totals by year</h2>${table(["Year", "Path", "Page views"], paths)}`;
    }
    const visitCoverage = kind === "website" && visitSite
        ? ` Approximate visits have their own coverage beginning ${escapeHtml(visitSite.since)}.`
        : "";
    return page(`${label} by year`, `<h1>${label}: annual view</h1><p class="note">${coverageText(series)}${visitCoverage}</p>${details}`);
}

function renderReports(state, registry, now, visitState = { sites: {} }) {
    const files = [];
    const siteLinks = [];
    for (const siteConfig of registry.sites) {
        const site = state.sites[siteConfig.id];
        const visitSite = Object.hasOwn(visitState.sites, siteConfig.id)
            ? visitState.sites[siteConfig.id]
            : null;
        const base = path.join(registry.privateMetrics.reportOutputDirectory, siteConfig.id);
        files.push({ destination: path.join(base, "website", "daily", "report.html"), content: renderDaily(site, "website", visitSite) });
        files.push({ destination: path.join(base, "website", "yearly", "report.html"), content: renderAnnual(site, "website", visitSite) });
        files.push({ destination: path.join(base, "updates", "daily", "report.html"), content: renderDaily(site, "updateInfo") });
        files.push({ destination: path.join(base, "updates", "yearly", "report.html"), content: renderAnnual(site, "updateInfo") });
        siteLinks.push(`<h2>${escapeHtml(siteConfig.hostname)}</h2><ul>`
            + `<li><a href="/metrics/${encodeURIComponent(siteConfig.id)}/website/daily/">Website: daily</a></li>`
            + `<li><a href="/metrics/${encodeURIComponent(siteConfig.id)}/website/yearly/">Website: annual</a></li>`
            + `<li><a href="/metrics/${encodeURIComponent(siteConfig.id)}/updates/daily/">Update information: daily</a></li>`
            + `<li><a href="/metrics/${encodeURIComponent(siteConfig.id)}/updates/yearly/">Update information: annual</a></li></ul>`);
    }
    files.push({
        destination: path.join(registry.privateMetrics.reportOutputDirectory, "report.html"),
        content: page("Private website metrics", `<h1>Private website metrics</h1>`
            + `<p>Generated ${escapeHtml(now.toISOString())}. Visits, page views and update-information requests are separate measures.</p>`
            + `<p class="note">An update-information request is a successful GET request for the exact XML path. Browsers and bots may request it too; it is neither a program-start count nor a user count.</p>${siteLinks.join("")}`)
    });
    return files;
}

function validateImportOptions(options, registry) {
    const hasImport = Array.isArray(options.importLogs) && options.importLogs.length > 0;
    const companions = [options.importFormat, options.importSite, options.coverageFrom, options.coverageThrough];
    if (!hasImport && companions.some(Boolean)) throw new Error("historical import options require at least one --import-log");
    if (!hasImport) return null;
    if (!options.importFormat || !["nginx-combined", "analytics-tsv"].includes(options.importFormat)) {
        throw new Error("--import-format must be nginx-combined or analytics-tsv");
    }
    const site = registry.sites.find(entry => entry.id === options.importSite);
    if (!site) throw new Error("--import-site must identify a registered site");
    if (!parseIsoDate(options.coverageFrom) || !parseIsoDate(options.coverageThrough)
        || options.coverageFrom > options.coverageThrough) {
        throw new Error("--coverage-from and --coverage-through must define a valid inclusive range");
    }
    const importLogs = options.importLogs.map(filePath => path.resolve(filePath));
    for (const filePath of importLogs) fs.accessSync(filePath, fs.constants.R_OK);
    return { ...options, site, importLogs };
}

function validCountMap(values) {
    return values && typeof values === "object" && !Array.isArray(values)
        && Object.entries(values).every(([key, value]) => key !== ""
            && Number.isSafeInteger(value) && value >= 0);
}

function validateStoredSeries(series, totalKey, label) {
    if (!series || typeof series !== "object" || !parseIsoDate(series.liveSince)
        || (series.firstCoveredOn !== null && !parseIsoDate(series.firstCoveredOn))
        || (series.lastSuccessfulOn !== null && !parseIsoDate(series.lastSuccessfulOn))
        || !series.daily || typeof series.daily !== "object" || Array.isArray(series.daily)) {
        throw new Error(`private metrics state for ${label} is invalid`);
    }
    for (const [date, value] of Object.entries(series.daily)) {
        if (!parseIsoDate(date) || !value || !Number.isSafeInteger(value[totalKey])
            || value[totalKey] < 0 || !validCountMap(value.countries)
            || sumValues(value.countries) !== value[totalKey]) {
            throw new Error(`private metrics state value for ${label}/${date} is invalid`);
        }
        if (totalKey === "pageViews") {
            if (!validCountMap(value.paths) || sumValues(value.paths) !== value[totalKey]) {
                throw new Error(`private metrics path value for ${label}/${date} is invalid`);
            }
        } else if ((value.hours !== undefined || value.clients !== undefined)
            && (!validCountMap(value.hours) || !validCountMap(value.clients)
                || sumValues(value.hours) !== value[totalKey]
                || sumValues(value.clients) !== value[totalKey])) {
            throw new Error(`private metrics detail value for ${label}/${date} is invalid`);
        }
    }
}

function ensureSiteState(state, site) {
    const current = Object.hasOwn(state.sites, site.id) ? state.sites[site.id] : null;
    if (current) {
        validateStoredSeries(current.website, "pageViews", `${site.id}/website`);
        validateStoredSeries(current.updateInfo, "requests", `${site.id}/update information`);
        if (current.hostname !== site.hostname
            || current.website.liveSince !== site.websiteMetricsSince
            || current.updateInfo.liveSince !== site.updateInfo.metricsSince) {
            throw new Error(`private metrics identity for ${site.id} differs from existing state`);
        }
    }
    if (!current) {
        state.sites[site.id] = {
            hostname: site.hostname,
            website: { liveSince: site.websiteMetricsSince, firstCoveredOn: null, lastSuccessfulOn: null, daily: {} },
            updateInfo: { liveSince: site.updateInfo.metricsSince, firstCoveredOn: null, lastSuccessfulOn: null, daily: {} }
        };
    }
    return state.sites[site.id];
}

function liveDates(series, today, historicalBridge = null) {
    // Re-read the last successful day as well. The first run after midnight must
    // still pick up requests which arrived after the final run of the previous day.
    const start = series.lastSuccessfulOn || series.liveSince;
    const oldestAvailable = shiftDate(today, -1);
    if (start < oldestAvailable) {
        const dayBeforeHandover = shiftDate(oldestAvailable, -1);
        const bridgeIsComplete = historicalBridge
            && historicalBridge.coverageFrom <= start
            && historicalBridge.coverageThrough >= dayBeforeHandover;
        if (!bridgeIsComplete) {
            throw new Error(
                `private metrics gap begins ${start}; regular current/.1 processing starts no earlier than ${oldestAvailable}`
            );
        }
        return dateRange(oldestAvailable, today);
    }
    return dateRange(start > today ? today : start, today);
}

function generatePrivateMetrics({ registry, analyticsLogsBySite, updateLogsBySite, options, context, visitState }) {
    const state = loadState(registry.privateMetrics.statePath);
    const today = localParts(context.now, registry.privateMetrics.timeZone).date;
    const imported = validateImportOptions(options, registry);
    if (imported && imported.coverageThrough > today) {
        throw new Error("historical import coverage must not extend into the future");
    }

    for (const site of registry.sites) {
        const siteState = ensureSiteState(state, site);
        if (site.websiteMetricsSince > today || site.updateInfo.metricsSince > today) {
            throw new Error(`private metrics live coverage for ${site.id} must not begin in the future`);
        }
        const historicalBridge = imported && imported.site.id === site.id ? imported : null;
        const websiteDates = liveDates(siteState.website, today, historicalBridge);
        const updateDates = liveDates(siteState.updateInfo, today, historicalBridge);
        const websiteRecords = parseLogFiles(
            analyticsLogsBySite.get(site.id),
            line => parseAnalyticsLine(line, registry.privateMetrics.timeZone),
            { label: "website analytics log" }
        ).filter(isEligibleWebsiteRequest);
        const updateRecords = parseLogFiles(
            updateLogsBySite.get(site.id),
            line => parseAnalyticsLine(line, registry.privateMetrics.timeZone),
            { label: "update-information log" }
        ).filter(isUpdateRequest);
        mergeSeries(siteState.website.daily, aggregateDays({
            records: websiteRecords,
            dates: websiteDates,
            kind: "website",
            site,
            context
        }), "pageViews", "website");
        mergeSeries(siteState.updateInfo.daily, aggregateDays({
            records: updateRecords,
            dates: updateDates,
            kind: "updateInfo",
            site,
            context
        }), "requests", "update information");
        for (const series of [siteState.website, siteState.updateInfo]) {
            series.firstCoveredOn = !series.firstCoveredOn || series.liveSince < series.firstCoveredOn ? series.liveSince : series.firstCoveredOn;
            series.lastSuccessfulOn = today;
        }
    }

    if (imported) {
        const parser = imported.importFormat === "nginx-combined"
            ? line => parseCombinedLine(line, imported.site.hostname, registry.privateMetrics.timeZone)
            : line => parseAnalyticsLine(line, registry.privateMetrics.timeZone);
        const allRecords = parseLogFiles(imported.importLogs, parser, { deduplicateAcrossFiles: true, label: "historical import log" });
        const dates = dateRange(imported.coverageFrom, imported.coverageThrough);
        const siteState = state.sites[imported.site.id];
        const website = aggregateDays({ records: allRecords.filter(isEligibleWebsiteRequest), dates, kind: "website", site: imported.site, context });
        const updates = aggregateDays({ records: allRecords.filter(isUpdateRequest), dates, kind: "updateInfo", site: imported.site, context });
        mergeSeries(siteState.website.daily, website, "pageViews", "website import");
        mergeSeries(siteState.updateInfo.daily, updates, "requests", "update-information import");
        for (const series of [siteState.website, siteState.updateInfo]) {
            series.firstCoveredOn = !series.firstCoveredOn || imported.coverageFrom < series.firstCoveredOn
                ? imported.coverageFrom : series.firstCoveredOn;
        }
    }

    for (const site of Object.values(state.sites)) {
        purgeDetails(site, today, registry.privateMetrics.detailRetentionDays);
    }
    return {
        state,
        files: renderReports(state, registry, context.now, visitState),
        imported: Boolean(imported)
    };
}

module.exports = {
    UPDATE_INFO_PATH,
    aggregateGoAccessReport,
    clientGroup,
    escapeHtml,
    generatePrivateMetrics,
    isEligibleWebsiteRequest,
    isUpdateRequest,
    localParts,
    mergeDay,
    normalizePath,
    parseAnalyticsLine,
    parseCombinedLine,
    parseLogFiles,
    purgeDetails,
    renderReports,
    validateImportOptions
};
