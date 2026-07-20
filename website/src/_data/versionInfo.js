const fs = require("fs");
const path = require("path");

const REPO = "praktimarc/kst4contest";
const API = `https://api.github.com/repos/${REPO}`;
const LABEL_MAP = { enhancement: "added", bug: "fixed" };

async function githubGet(urlPath) {
    const headers = { Accept: "application/vnd.github+json" };
    if (process.env.GITHUB_TOKEN) {
        headers.Authorization = `Bearer ${process.env.GITHUB_TOKEN}`;
    }
    const res = await fetch(`${API}${urlPath}`, { headers });
    if (!res.ok) {
        throw new Error(`GitHub API ${urlPath} failed: ${res.status}`);
    }
    return res.json();
}

// UpdateChecker.java parses <versionNumber> with Double.parseDouble() and
// compares it against ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER,
// which encodes patch releases by appending the patch digit(s) instead of a
// third dot (e.g. tag v1.41.1 -> app version 1.411, v1.41.0 -> 1.41). See
// commit 38ef50c ("...set to 1.41.1 for upcoming hotfix" -> 1.411).
function toAppVersionNumber(tag) {
    const cleaned = tag.replace(/^v/, "");
    const [major, minor, ...rest] = cleaned.split(".");
    if (rest.length === 0) return cleaned;
    const patch = rest.join("");
    return /^0*$/.test(patch) ? `${major}.${minor}` : `${major}.${minor}${patch}`;
}

function toSemanticVersion(tag) {
    return tag
        .replace(/^v/, "")
        .split("-")[0]
        .split("+")[0];
}

function escapeXml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
}

// Strips GitHub's auto-generated release-notes markdown noise (headings,
// "Full Changelog" links, PR author attributions) down to plain text.
function cleanReleaseBody(body) {
    const lines = [];
    for (let line of (body || "").split("\n")) {
        if (/^#{1,3}\s/.test(line)) continue;
        if (line.trim().startsWith("**Full Changelog**")) continue;
        line = line.replace(/\s+by @\S+ in https:\/\/\S+/, "");
        line = line.replace(/^\* /, "- ");
        if (line.trim()) lines.push(line);
    }
    return lines.join("\n").replace(/\n{3,}/g, "\n\n").trim();
}

// Groups closed issues (excluding PRs) in the (since, until] window by
// label into "added" (enhancement) / "fixed" (bug) bullet lists.
async function getIssuesForRelease(since, until) {
    const buckets = { added: [], fixed: [] };
    let page = 1;
    for (;;) {
        const items = await githubGet(`/issues?state=closed&per_page=100&page=${page}`);
        if (!items.length) break;
        for (const issue of items) {
            if (issue.pull_request) continue;
            const closed = issue.closed_at || "";
            if (!(closed > since && closed <= until)) continue;
            const labels = issue.labels.map((l) => l.name);
            const bucket = Object.keys(LABEL_MAP).find((l) => labels.includes(l));
            if (bucket) buckets[LABEL_MAP[bucket]].push(`- ${issue.title}`);
        }
        if (items.length < 100) break;
        page++;
    }
    return { added: buckets.added.join("\n"), fixed: buckets.fixed.join("\n") };
}

// version-history.xml holds hand-written <changeLog> entries for releases
// published before GitHub Releases existed; re-emitted verbatim here.
function loadHistoryEntries() {
    const historyPath = path.join(__dirname, "version-history.xml");
    const xml = fs.readFileSync(historyPath, "utf-8");
    return [...xml.matchAll(/<changeLog>[\s\S]*?<\/changeLog>/g)].map((m) => m[0]);
}

function historyEntryVersion(entryXml) {
    const m = entryXml.match(/<changedVersionNumber>([\s\S]*?)<\/changedVersionNumber>/);
    return m ? m[1].trim() : null;
}

// Static <roadmap>/<bugsReported> sections carried over from the previous
// hand-maintained XML on do5amf.funkerportal.de. UpdateChecker.java doesn't
// parse these, kept only so the migration doesn't drop existing content.
function loadLegacySections() {
    const legacyPath = path.join(__dirname, "version-legacy-sections.xml");
    const xml = fs.readFileSync(legacyPath, "utf-8");
    const roadmap = xml.match(/<roadmap>[\s\S]*?<\/roadmap>/);
    const bugsReported = xml.match(/<bugsReported>[\s\S]*?<\/bugsReported>/);
    return [roadmap ? roadmap[0] : null, bugsReported ? bugsReported[0] : null].filter(Boolean);
}

// Builds kst4ContestVersionInfo.xml (the update-checker feed read by
// UpdateChecker.java) from GitHub releases + closed issues, falling back to
// version-history.xml for releases that predate GitHub Releases.
module.exports = async function () {
    const fallback = '<?xml version="1.0" encoding="UTF-8" standalone="no"?>\n<praktiKST></praktiKST>';

    try {
        const historyEntries = loadHistoryEntries();

        const rawReleases = await githubGet("/releases?per_page=100");
        const releases = rawReleases
            .map((r) => ({
                tagName: r.tag_name,
                publishedAt: r.published_at || "",
                name: r.name || r.tag_name,
                body: r.body || "",
                isPrerelease: r.prerelease
            }))
            .sort((a, b) => (a.publishedAt < b.publishedAt ? 1 : -1));

        const stableReleases = releases.filter((r) => !r.isPrerelease);
        const stable = stableReleases[0];
        const ghVersions = new Set(stableReleases.map((r) => toAppVersionNumber(r.tagName)));

        const parts = [];
        parts.push('<?xml version="1.0" encoding="UTF-8" standalone="no"?>');
        parts.push("<praktiKST>");

        const latestTag = stable ? stable.tagName : null;
        const winFilename = latestTag ? `praktiKST-${latestTag}-windows-x64.zip` : "";

        parts.push("    <latestVersion>");
        if (stable) {
            const latestIssues = await getIssuesForRelease("1970-01-01T00:00:00Z", stable.publishedAt);
            parts.push(`        <versionNumber>${escapeXml(toAppVersionNumber(latestTag))}</versionNumber>`);
            parts.push(
                `        <latestVersionPathOnWebserver>https://github.com/${REPO}/releases/tag/${latestTag}</latestVersionPathOnWebserver>`
            );
            parts.push("        <adminMessage></adminMessage>");
            parts.push(`        <majorChanges>${escapeXml(latestIssues.added.slice(0, 300))}</majorChanges>`);
            parts.push(
                `        <latestVersionPathOnWebserver>https://github.com/${REPO}/releases/download/${latestTag}/${winFilename}</latestVersionPathOnWebserver>`
            );
        }
        parts.push("    </latestVersion>");

        // UpdateChecker.parseUpdateXMLFile() never actually reads this field
        // (setNeedUpdateResourcesSinceLastVersion is never called); the
        // previous server always hardcoded "nothing" here, so we match that.
        parts.push("    <needUpdateSinceLastVersion>");
        parts.push("        <filename>nothing</filename>");
        parts.push("    </needUpdateSinceLastVersion>");

        for (const section of loadLegacySections()) {
            parts.push(section);
        }

        for (let i = 0; i < stableReleases.length; i++) {
            const rel = stableReleases[i];
            const until = rel.publishedAt;
            const since = stableReleases[i + 1] ? stableReleases[i + 1].publishedAt : "1970-01-01T00:00:00Z";
            const issues = await getIssuesForRelease(since, until);

            parts.push("    <changeLog>");

            parts.push(`        <changedVersionNumber>${escapeXml(toSemanticVersion(rel.tagName))}</changedVersionNumber>`);
            parts.push(`        <date>${escapeXml(until.slice(0, 10))}</date>`);
            parts.push(`        <description>${escapeXml(rel.name)}</description>`);
            parts.push(`        <added>${escapeXml(issues.added)}</added>`);
            parts.push(`        <changed>${escapeXml(cleanReleaseBody(rel.body))}</changed>`);
            parts.push(`        <fixed>${escapeXml(issues.fixed)}</fixed>`);
            parts.push("        <removed></removed>");
            parts.push("    </changeLog>");
        }

        for (const entry of historyEntries) {
            const version = historyEntryVersion(entry);
            if (version && ghVersions.has(version)) continue;
            parts.push(entry);
        }

        parts.push("</praktiKST>");
        return parts.join("\n");
    } catch (err) {
        console.warn(`[versionInfo] Could not generate version info XML, using empty fallback: ${err.message}`);
        return fallback;
    }
};
