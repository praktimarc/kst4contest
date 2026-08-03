const REPO = "praktimarc/kst4contest";
const API = `https://api.github.com/repos/${REPO}`;
const WORKFLOW_FILE = "nightly-artifacts.yml";
const WORKFLOW_BASENAME = "nightly-artifacts";
const BRANCH = "main";
const WORKFLOW_RUNS_URL = `https://github.com/${REPO}/actions/workflows/${WORKFLOW_FILE}`;

/**
 * nightly.link (https://nightly.link) mirrors the most recent successful
 * GitHub Actions artifact for a given workflow/branch/artifact-name as a
 * plain public download, with no GitHub account required. It always follows
 * the latest successful run on its own, so these URLs stay correct without
 * this site having to resolve a run ID at build time.
 */
const NIGHTLY_LINK_BASE = `https://nightly.link/${REPO}/workflows/${WORKFLOW_BASENAME}/${BRANCH}`;

/**
 * Static metadata for the artifacts produced by
 * .github/workflows/nightly-artifacts.yml, keyed by the upload-artifact name
 * used in that workflow. Anything not listed here (e.g. the raw
 * flatpak-ostree-repo folder) is skipped on the nightly page.
 */
const ARTIFACT_INFO = {
    "windows-zip": {
        os: "Windows",
        format: "ZIP x64",
        icon: "🪟",
        note: "Extract the archive, then start praktiKST.exe. No separate Java installation is required.",
        order: 1
    },
    "linux-appimage": {
        os: "Linux",
        format: "AppImage x86_64",
        icon: "🐧",
        note: "Portable build. Make the downloaded file executable before the first launch.",
        order: 2
    },
    flatpakref: {
        os: "Linux",
        format: "Flatpak (.flatpakref, nightly branch)",
        icon: "🐧",
        note: "Adds the nightly branch of the KST4Contest Flatpak repo. See the installation guide for the flatpak CLI alternative.",
        order: 3
    },
    "linux-debian": {
        os: "Debian / Ubuntu",
        format: "DEB amd64",
        icon: "📦",
        note: "Native package for Debian, Ubuntu and distributions based on them.",
        order: 4
    },
    "linux-fedora": {
        os: "Fedora",
        format: "RPM x86_64",
        icon: "📦",
        note: "Native package built for Fedora and compatible RPM-based systems.",
        order: 5
    },
    "linux-arch": {
        os: "Arch Linux",
        format: "pkg.tar.zst",
        icon: "📦",
        note: "Package for direct installation with pacman.",
        order: 6
    },
    "macos-dmg-macos-latest": {
        os: "macOS Apple Silicon",
        format: "DMG arm64",
        icon: "🍎",
        note: "Best-effort build for Apple Silicon Macs. Not notarized by Apple.",
        order: 7
    },
    "macos-dmg-macos-15-intel": {
        os: "macOS Intel",
        format: "DMG x86_64",
        icon: "🍎",
        note: "Best-effort build for Intel Macs. Not notarized by Apple.",
        order: 8
    }
};

function buildItems() {
    return Object.entries(ARTIFACT_INFO)
        .map(([name, info]) => ({
            ...info,
            name,
            url: `${NIGHTLY_LINK_BASE}/${name}.zip`
        }))
        .sort((a, b) => a.order - b.order);
}

function authHeaders() {
    const headers = { Accept: "application/vnd.github+json" };

    if (process.env.GITHUB_TOKEN) {
        headers.Authorization = `Bearer ${process.env.GITHUB_TOKEN}`;
    }

    return headers;
}

function formatSize(bytes) {
    if (bytes < 1024 * 1024) {
        return `${(bytes / 1024).toFixed(0)} KB`;
    }

    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/**
 * Best-effort lookup of the run behind the current nightly.link downloads,
 * purely to show a commit/date on the page. The download links themselves
 * (buildItems) do not depend on this succeeding.
 */
async function fetchLatestRunMeta(headers) {
    const runsRes = await fetch(
        `${API}/actions/workflows/${WORKFLOW_FILE}/runs?branch=${BRANCH}&status=success&per_page=1`,
        { headers }
    );

    if (!runsRes.ok) {
        throw new Error(`workflow runs request failed: ${runsRes.status}`);
    }

    const runsData = await runsRes.json();
    const run = runsData.workflow_runs && runsData.workflow_runs[0];

    if (!run) {
        throw new Error("no successful nightly run found");
    }

    const meta = {
        runUrl: run.html_url,
        commitUrl: `https://github.com/${REPO}/commit/${run.head_sha}`,
        shortSha: run.head_sha.slice(0, 7),
        createdAt: run.created_at
    };

    try {
        const artifactsRes = await fetch(
            `${API}/actions/runs/${run.id}/artifacts?per_page=100`,
            { headers }
        );

        if (artifactsRes.ok) {
            const artifactsData = await artifactsRes.json();
            const sizeByName = {};

            for (const artifact of artifactsData.artifacts || []) {
                if (!artifact.expired) {
                    sizeByName[artifact.name] = formatSize(artifact.size_in_bytes);
                }
            }

            meta.sizeByName = sizeByName;
        }
    } catch {
        // Sizes are a nice-to-have; the run metadata above is still useful without them.
    }

    return meta;
}

module.exports = async function () {
    const items = buildItems();

    let meta = null;

    try {
        meta = await fetchLatestRunMeta(authHeaders());
    } catch (err) {
        console.warn(
            `[nightly] Could not load latest nightly run metadata (commit/date will be omitted). ` +
            `Download links are unaffected since they are served by nightly.link: ${err.message}`
        );
    }

    if (meta && meta.sizeByName) {
        for (const item of items) {
            if (meta.sizeByName[item.name]) {
                item.size = meta.sizeByName[item.name];
            }
        }
    }

    return {
        runsUrl: WORKFLOW_RUNS_URL,
        meta,
        items
    };
};
