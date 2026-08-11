const fs = require("fs");
const path = require("path");

const REPO = "praktimarc/kst4contest";
const API = `https://api.github.com/repos/${REPO}`;
const LABEL_MAP = { enhancement: "added", bug: "fixed" };

const GITHUB_API_ATTEMPTS = 3;

function wait(milliseconds) {
    return new Promise(
        (resolve) => setTimeout(resolve, milliseconds)
    );
}

async function githubGet(urlPath) {
    const headers = {
        Accept: "application/vnd.github+json"
    };

    if (process.env.GITHUB_TOKEN) {
        headers.Authorization =
            `Bearer ${process.env.GITHUB_TOKEN}`;
    }

    let lastError = null;

    for (
        let attempt = 1;
        attempt <= GITHUB_API_ATTEMPTS;
        attempt++
    ) {
        try {
            const res = await fetch(
                `${API}${urlPath}`,
                {
                    headers,
                    signal: AbortSignal.timeout(15000)
                }
            );

            if (res.ok) {
                return res.json();
            }

            const rateLimitRemaining =
                res.headers.get("x-ratelimit-remaining");

            const rateLimitReset =
                res.headers.get("x-ratelimit-reset");

            const rateLimitInfo =
                rateLimitRemaining === null
                    ? ""
                    : `, rate limit remaining `
                    + rateLimitRemaining
                    + (
                        rateLimitReset
                            ? `, reset ${rateLimitReset}`
                            : ""
                    );

            lastError = new Error(
                `GitHub API ${urlPath} failed with `
                + `HTTP ${res.status}${rateLimitInfo}`
            );

            // Authentication and permission errors do not
            // become valid by retrying.
            if (
                ![408, 429].includes(res.status)
                && res.status < 500
            ) {
                throw lastError;
            }
        } catch (err) {
            lastError = err;

            // Do not hide an invalid or expired token behind
            // repeated requests.
            if (
                /HTTP (401|403|404)/.test(err.message)
            ) {
                throw err;
            }
        }

        if (attempt < GITHUB_API_ATTEMPTS) {
            await wait(attempt * 500);
        }
    }

    throw lastError
    || new Error(`GitHub API ${urlPath} failed`);
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
    try {
        const historyEntries = loadHistoryEntries();

        const rawReleases =
            await githubGet("/releases?per_page=100");

        if (!Array.isArray(rawReleases)) {
            throw new Error(
                "GitHub releases response was not an array"
            );
        }

        const releases = rawReleases
            .map((release) => ({
                tagName: release.tag_name,
                publishedAt: release.published_at || "",
                name:
                    release.name
                    || release.tag_name,
                body: release.body || "",
                isPrerelease: release.prerelease,
                isDraft: release.draft
            }))
            .sort(
                (first, second) =>
                    first.publishedAt
                    < second.publishedAt
                        ? 1
                        : -1
            );

        const stableReleases = releases.filter(
            (release) =>
                !release.isPrerelease
                && !release.isDraft
        );

        const stable = stableReleases[0];

        if (
            !stable
            || !stable.tagName
            || !stable.publishedAt
        ) {
            throw new Error(
                "GitHub did not return "
                + "a published Stable release"
            );
        }

        const ghVersions = new Set(
            stableReleases.map(
                (release) =>
                    toAppVersionNumber(
                        release.tagName
                    )
            )
        );

        const parts = [];
        parts.push('<?xml version="1.0" encoding="UTF-8" standalone="no"?>');
        parts.push("<praktiKST>");

        const latestTag = stable ? stable.tagName : null;

        parts.push("    <latestVersion>");
        if (stable) {
            const latestIssues = await getIssuesForRelease(
                "1970-01-01T00:00:00Z",
                stable.publishedAt
            );

            // Kept for application versions that still expect the old numeric format.
            parts.push(
                `        <versionNumber>${escapeXml(toAppVersionNumber(latestTag))}</versionNumber>`
            );

            // Used by current application versions for correct semantic comparison
            // and display, for example 1.41.1 instead of 1.411.
            parts.push(
                `        <semanticVersion>${escapeXml(toSemanticVersion(latestTag))}</semanticVersion>`
            );

            parts.push("        <adminMessage></adminMessage>");
            parts.push(
                `        <majorChanges>${escapeXml(latestIssues.added.slice(0, 300))}</majorChanges>`
            );

            // The release page is deliberately platform-neutral. The application
            // cannot know whether the user needs Windows, Linux or macOS packages.
            parts.push(
                `        <latestVersionPathOnWebserver>https://github.com/${REPO}/releases/tag/${latestTag}</latestVersionPathOnWebserver>`
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
        throw new Error(
            "[versionInfo] Could not generate "
            + "a complete update feed. "
            + "The website build has been aborted "
            + "so that the existing production feed "
            + "remains untouched: "
            + err.message,
            { cause: err }
        );
    }
};
