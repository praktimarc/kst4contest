const REPO = "praktimarc/kst4contest";
const API = `https://api.github.com/repos/${REPO}`;
const FALLBACK_TAG = "v1.41.1";

async function fetchLatestReleaseTag() {
    const headers = { Accept: "application/vnd.github+json" };
    if (process.env.GITHUB_TOKEN) {
        headers.Authorization = `Bearer ${process.env.GITHUB_TOKEN}`;
    }

    try {
        const res = await fetch(`${API}/releases/latest`, { headers });
        if (!res.ok) {
            throw new Error(`GitHub API /releases/latest failed: ${res.status}`);
        }
        const release = await res.json();
        return release.tag_name || FALLBACK_TAG;
    } catch (err) {
        console.warn(`[downloads] Could not load latest release tag, falling back to ${FALLBACK_TAG}: ${err.message}`);
        return FALLBACK_TAG;
    }
}

// Builds the download list from the latest GitHub release tag so it never
// drifts out of sync with what tagged-release.yml actually published.
module.exports = async function () {
    const latestTag = await fetchLatestReleaseTag();
    const base = `https://github.com/${REPO}/releases/download/${latestTag}`;

    return [
        {
            os: "Windows",
            format: "ZIP x64",
            icon: "🪟",
            recommended: true,
            note: "Best choice for most Windows users.",
            url: `${base}/praktiKST-${latestTag}-windows-x64.zip`
        },
        {
            os: "Linux",
            format: "Flatpak (.flatpakref)",
            icon: "🐧",
            recommended: true,
            note: "Recommended for most Linux users. Open the file with GNOME Software / Discover, or run: flatpak install de.x08.KST4Contest.flatpakref",
            url: `${base}/de.x08.KST4Contest.flatpakref`
        },
        {
            os: "Linux",
            format: "AppImage x86_64",
            icon: "🐧",
            recommended: false,
            note: "Portable Linux build without package installation.",
            url: `${base}/KST4Contest-${latestTag}-linux-x86_64.AppImage`
        },
        {
            os: "Debian / Ubuntu",
            format: "DEB amd64",
            icon: "📦",
            recommended: false,
            note: "Native package for Debian and Ubuntu based systems.",
            url: `${base}/KST4Contest-${latestTag}-debian-amd64.deb`
        },
        {
            os: "Fedora",
            format: "RPM x86_64",
            icon: "📦",
            recommended: false,
            note: "Native package for Fedora/RPM based systems.",
            url: `${base}/KST4Contest-${latestTag}-fedora-x86_64.rpm`
        },
        {
            os: "Arch Linux",
            format: "pkg.tar.zst",
            icon: "📦",
            recommended: false,
            note: "Package build for Arch Linux users.",
            url: `${base}/KST4Contest-${latestTag}-archlinux-x86_64.pkg.tar.zst`
        },
        {
            os: "macOS Apple Silicon",
            format: "DMG arm64",
            icon: "🍎",
            recommended: false,
            note: "For Apple Silicon Macs.",
            url: `${base}/KST4Contest-${latestTag}-macos-arm64.dmg`
        },
        {
            os: "macOS Intel",
            format: "DMG x86_64",
            icon: "🍎",
            recommended: false,
            note: "For Intel-based Macs.",
            url: `${base}/KST4Contest-${latestTag}-macos-x86_64.dmg`
        },
        {
            os: "Manual English",
            format: "PDF",
            icon: "📘",
            recommended: false,
            note: "English PDF manual.",
            url: `${base}/KST4Contest-${latestTag}-manual-en.pdf`
        },
        {
            os: "Manual German",
            format: "PDF",
            icon: "📘",
            recommended: false,
            note: "German PDF manual.",
            url: `${base}/KST4Contest-${latestTag}-manual-de.pdf`
        }
    ];
};
