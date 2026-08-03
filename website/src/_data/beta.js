const REPO = "praktimarc/kst4contest";
const API = `https://api.github.com/repos/${REPO}`;
const RELEASES_URL = `https://github.com/${REPO}/releases`;

/**
 * Beta builds are published by .github/workflows/tagged-release.yml whenever
 * a tag starting with "beta-" is pushed: it creates a GitHub prerelease with
 * the same asset naming as a Stable release (based on the tag), except for
 * the flatpakref, which is named ...beta.flatpakref instead of .flatpakref.
 *
 * There is no dedicated "beta" marker in the GitHub API beyond the
 * prerelease flag, but that flag is exactly what this workflow sets, and
 * nothing else in this repository publishes prereleases, so it is a safe
 * signal to use here.
 */
async function fetchLatestBetaRelease() {
    const headers = { Accept: "application/vnd.github+json" };

    if (process.env.GITHUB_TOKEN) {
        headers.Authorization = `Bearer ${process.env.GITHUB_TOKEN}`;
    }

    try {
        const res = await fetch(`${API}/releases?per_page=10`, { headers });

        if (!res.ok) {
            throw new Error(`GitHub API /releases failed: ${res.status}`);
        }

        const releases = await res.json();

        return releases.find((release) => release.prerelease && !release.draft) || null;
    } catch (err) {
        console.warn(
            `[beta] Could not load the latest beta release. ` +
            `The Beta tab will show its empty state instead: ${err.message}`
        );

        return null;
    }
}

function unavailable() {
    return {
        available: false,
        releasesUrl: RELEASES_URL,
        items: []
    };
}

/**
 * Builds the Beta download list from the most recent GitHub prerelease.
 * If there is currently no Beta release, the Beta tab is expected to show
 * nothing but an explanatory empty state rather than guessed-at links.
 */
module.exports = async function () {
    const release = await fetchLatestBetaRelease();

    if (!release) {
        return unavailable();
    }

    const tag = release.tag_name;
    const assetUrl = (filenameTemplate) =>
        `https://github.com/${REPO}/releases/download/${tag}/${filenameTemplate.replace(/\{tag\}/g, tag)}`;

    const items = [
        {
            os: "Windows",
            format: "ZIP x64",
            icon: "🪟",
            note: "Extract the archive, then start praktiKST.exe. No separate Java installation is required.",
            url: assetUrl("praktiKST-{tag}-windows-x64.zip")
        },
        {
            os: "Linux",
            format: "Flatpak (.flatpakref)",
            icon: "🐧",
            note: "Installs the beta branch of the Flatpak repo through Flatpak or the desktop software centre.",
            url: assetUrl("de.x08.KST4Contest.beta.flatpakref")
        },
        {
            os: "Linux",
            format: "AppImage x86_64",
            icon: "🐧",
            note: "Portable build without package installation. Make the downloaded file executable before the first launch.",
            url: assetUrl("KST4Contest-{tag}-linux-x86_64.AppImage")
        },
        {
            os: "Debian / Ubuntu",
            format: "DEB amd64",
            icon: "📦",
            note: "Native package for Debian, Ubuntu and distributions based on them.",
            url: assetUrl("KST4Contest-{tag}-debian-amd64.deb")
        },
        {
            os: "Fedora",
            format: "RPM x86_64",
            icon: "📦",
            note: "Native package built for Fedora and compatible RPM-based systems.",
            url: assetUrl("KST4Contest-{tag}-fedora-x86_64.rpm")
        },
        {
            os: "Arch Linux",
            format: "pkg.tar.zst",
            icon: "📦",
            note: "Release package for direct installation with pacman.",
            url: assetUrl("KST4Contest-{tag}-archlinux-x86_64.pkg.tar.zst")
        },
        {
            os: "macOS Apple Silicon",
            format: "DMG arm64",
            icon: "🍎",
            note: "Best-effort build for Apple Silicon Macs. Not currently notarized by Apple.",
            url: assetUrl("KST4Contest-{tag}-macos-arm64.dmg")
        },
        {
            os: "macOS Intel",
            format: "DMG x86_64",
            icon: "🍎",
            note: "Best-effort build for Intel Macs. Not currently notarized by Apple.",
            url: assetUrl("KST4Contest-{tag}-macos-x86_64.dmg")
        }
    ];

    return {
        available: true,
        releasesUrl: RELEASES_URL,
        tag,
        name: release.name,
        publishedAt: release.published_at,
        releaseUrl: release.html_url,
        items
    };
};
