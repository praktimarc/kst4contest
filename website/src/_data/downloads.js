const latestTag = "v1.41.0";
const base = `https://github.com/praktimarc/kst4contest/releases/download/${latestTag}`;

module.exports = [
    {
        os: "Windows",
        format: "ZIP x64",
        icon: "🪟",
        recommended: true,
        url: `${base}/praktiKST-${latestTag}-windows-x64.zip`
    },
    {
        os: "Linux",
        format: "AppImage x86_64",
        icon: "🐧",
        recommended: true,
        url: `${base}/KST4Contest-${latestTag}-linux-x86_64.AppImage`
    },
    {
        os: "Debian / Ubuntu",
        format: "DEB amd64",
        icon: "📦",
        recommended: false,
        url: `${base}/KST4Contest-${latestTag}-debian-amd64.deb`
    },
    {
        os: "Fedora",
        format: "RPM x86_64",
        icon: "📦",
        recommended: false,
        url: `${base}/KST4Contest-${latestTag}-fedora-x86_64.rpm`
    },
    {
        os: "Arch Linux",
        format: "pkg.tar.zst",
        icon: "📦",
        recommended: false,
        url: `${base}/KST4Contest-${latestTag}-archlinux-x86_64.pkg.tar.zst`
    },
    {
        os: "macOS Apple Silicon",
        format: "DMG arm64",
        icon: "🍎",
        recommended: false,
        url: `${base}/KST4Contest-${latestTag}-macos-arm64.dmg`
    },
    {
        os: "macOS Intel",
        format: "DMG x86_64",
        icon: "🍎",
        recommended: false,
        url: `${base}/KST4Contest-${latestTag}-macos-x86_64.dmg`
    },
    {
        os: "Manual English",
        format: "PDF",
        icon: "📘",
        recommended: false,
        url: `${base}/KST4Contest-${latestTag}-manual-en.pdf`
    },
    {
        os: "Manual German",
        format: "PDF",
        icon: "📘",
        recommended: false,
        url: `${base}/KST4Contest-${latestTag}-manual-de.pdf`
    }
];