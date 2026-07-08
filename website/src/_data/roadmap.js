const REPO = "praktimarc/kst4contest";
const API = `https://api.github.com/repos/${REPO}`;

async function githubGet(path) {
    const headers = { Accept: "application/vnd.github+json" };
    if (process.env.GITHUB_TOKEN) {
        headers.Authorization = `Bearer ${process.env.GITHUB_TOKEN}`;
    }
    const res = await fetch(`${API}${path}`, { headers });
    if (!res.ok) {
        throw new Error(`GitHub API ${path} failed: ${res.status}`);
    }
    return res.json();
}

// Builds the roadmap from all GitHub issues tagged "enhancement", grouped by
// their version milestone (open or already released). Issues without a
// milestone are shown as planned but with no assigned version yet. Issues
// closed as "not planned" are kept, struck through and labeled accordingly.
// Falls back to an empty list if the API is unreachable (e.g. offline build)
// so the site build never fails.
module.exports = async function () {
    try {
        const issues = await githubGet(
            "/issues?labels=enhancement&state=all&per_page=100"
        );

        const groups = new Map();

        for (const issue of issues) {
            if (issue.pull_request) continue;

            const milestone = issue.milestone;
            const key = milestone ? milestone.number : "unscheduled";

            if (!groups.has(key)) {
                groups.set(key, {
                    version: milestone ? milestone.title : null,
                    description: milestone ? milestone.description : null,
                    url: milestone ? milestone.html_url : null,
                    released: milestone ? milestone.state === "closed" : false,
                    dueOn: milestone && milestone.due_on ? new Date(milestone.due_on) : null,
                    releasedOn: milestone && milestone.closed_at ? new Date(milestone.closed_at) : null,
                    enhancements: []
                });
            }

            const notPlanned = issue.state === "closed" && issue.state_reason === "not_planned";

            groups.get(key).enhancements.push({
                title: issue.title,
                url: issue.html_url,
                done: issue.state === "closed" && !notPlanned,
                notPlanned
            });
        }

        const versions = [...groups.values()].filter((v) => v.enhancements.length > 0);

        // Order: upcoming dated versions first (soonest due date), then
        // upcoming undated versions, then already released versions (newest
        // first), unscheduled enhancements at the very end.
        versions.sort((a, b) => {
            if (!a.version) return 1;
            if (!b.version) return -1;
            if (a.released !== b.released) return a.released ? 1 : -1;

            if (!a.released) {
                if (a.dueOn && b.dueOn) return a.dueOn - b.dueOn;
                if (a.dueOn) return -1;
                if (b.dueOn) return 1;
                return a.version.localeCompare(b.version);
            }

            if (a.releasedOn && b.releasedOn) return b.releasedOn - a.releasedOn;
            return b.version.localeCompare(a.version);
        });

        return versions;
    } catch (err) {
        console.warn(`[roadmap] Could not load GitHub issues: ${err.message}`);
        return [];
    }
};
