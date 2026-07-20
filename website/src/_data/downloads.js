const REPO = "praktimarc/kst4contest";
const API = `https://api.github.com/repos/${REPO}`;
const LATEST_RELEASE_URL = `https://github.com/${REPO}/releases/latest`;

async function fetchLatestReleaseTag() {
    const headers = { Accept: "application/vnd.github+json" };

    if (process.env.GITHUB_TOKEN) {
        headers.Authorization = `Bearer ${process.env.GITHUB_TOKEN}`;
    }

    try {
        const res = await fetch(`${API}/releases/latest`, { headers });

        if (!res.ok) {
            throw new Error(
                `GitHub API /releases/latest failed: ${res.status}`
            );
        }

        const release = await res.json();

        if (!release.tag_name) {
            throw new Error(
                "GitHub API response did not contain a release tag"
            );
        }

        return release.tag_name;
    } catch (err) {
        console.warn(
            `[downloads] Could not load the latest release tag. ` +
            `Download buttons will open the latest release page instead: ${err.message}`
        );

        return null;
    }
}

/**
 * Creates a direct link to an asset from the latest Stable release.
 *
 * If the GitHub API was unavailable during the build, the function deliberately
 * returns the generic latest-release page instead of using a hard-coded and
 * increasingly outdated fallback version.
 */
function createAssetUrl(latestTag, filenameTemplate) {
    if (!latestTag) {
        return LATEST_RELEASE_URL;
    }

    const filename = filenameTemplate.replace(/\{tag\}/g, latestTag);

    return `https://github.com/${REPO}/releases/download/${latestTag}/${filename}`;
}

/**
 * Builds the download list from the latest Stable GitHub release.
 *
 * The filenames must remain aligned with the artifacts produced by
 * .github/workflows/tagged-release.yml.
 */
module.exports = async function () {
    const latestTag = await fetchLatestReleaseTag();
    const assetUrl = (filenameTemplate) =>
        createAssetUrl(latestTag, filenameTemplate);

    return [
        {
            os: "Windows",
            format: "ZIP x64",
            icon: "🪟",
            recommended: true,
            note: "Extract the complete archive, then start praktiKST.exe. No separate Java installation is required.",
            url: assetUrl("praktiKST-{tag}-windows-x64.zip")
        },
        {
            os: "Linux",
            format: "Flatpak (.flatpakref)",
            icon: "🐧",
            recommended: true,
            note: "Installs through Flatpak and can be updated through Flatpak or the desktop software centre.",
            url: assetUrl("de.x08.KST4Contest.flatpakref")
        },
        {
            os: "Linux",
            format: "AppImage x86_64",
            icon: "🐧",
            recommended: false,
            note: "Portable build without package installation. Make the downloaded file executable before the first launch.",
            url: assetUrl("KST4Contest-{tag}-linux-x86_64.AppImage")
        },
        {
            os: "Debian / Ubuntu",
            format: "DEB amd64",
            icon: "📦",
            recommended: false,
            note: "Native package for Debian, Ubuntu and distributions based on them.",
            url: assetUrl("KST4Contest-{tag}-debian-amd64.deb")
        },
        {
            os: "Fedora",
            format: "RPM x86_64",
            icon: "📦",
            recommended: false,
            note: "Native package built for Fedora and compatible RPM-based systems.",
            url: assetUrl("KST4Contest-{tag}-fedora-x86_64.rpm")
        },
        {
            os: "Arch Linux",
            format: "pkg.tar.zst",
            icon: "📦",
            recommended: false,
            note: "Release package for direct installation with pacman. AUR alternatives are described in the installation guide.",
            url: assetUrl(
                "KST4Contest-{tag}-archlinux-x86_64.pkg.tar.zst"
            )
        },
        {
            os: "macOS Apple Silicon",
            format: "DMG arm64",
            icon: "🍎",
            recommended: false,
            note: "Best-effort build for Apple Silicon Macs. The application is not currently notarized by Apple.",
            url: assetUrl("KST4Contest-{tag}-macos-arm64.dmg")
        },
        {
            os: "macOS Intel",
            format: "DMG x86_64",
            icon: "🍎",
            recommended: false,
            note: "Best-effort build for Intel Macs. The application is not currently notarized by Apple.",
            url: assetUrl("KST4Contest-{tag}-macos-x86_64.dmg")
        },
        {
            os: "Manual English",
            format: "PDF",
            icon: "📘",
            recommended: false,
            note: "English PDF manual included with the Stable release.",
            url: assetUrl("KST4Contest-{tag}-manual-en.pdf")
        },
        {
            os: "Manual German",
            format: "PDF",
            icon: "📘",
            recommended: false,
            note: "German PDF manual included with the Stable release.",
            url: assetUrl("KST4Contest-{tag}-manual-de.pdf")
        }
    ];
};