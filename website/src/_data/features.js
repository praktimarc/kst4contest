module.exports = [
    {
        slug: "priority-score",
        title: "Priority Score System",
        icon: "🎯",
        category: "Contest Workflow",
        since: "1.40",
        summary: "Find the most promising stations faster using contest-aware candidate scoring.",
        description: "KST4Contest ranks stations by activity, direction, sked context, band information, QRG hints and operator workflow signals.",
        tags: ["ON4KST", "VHF", "UHF", "SHF", "contest", "priority candidates"],
        related: ["timeline", "airscout", "sked-reminder"]
    },
    {
        slug: "airscout",
        title: "AirScout Integration",
        icon: "✈️",
        category: "Aircraft Scatter",
        since: "1.26",
        summary: "Use aircraft scatter awareness directly inside your ON4KST contest workflow.",
        description: "AirScout information helps operators evaluate AP windows, aircraft scatter timing and candidate stations during VHF/UHF/SHF contests.",
        tags: ["AirScout", "airplane scatter", "aircraft scatter", "VHF contest"],
        related: ["timeline", "priority-score", "sked-reminder"]
    },
    {
        slug: "timeline",
        title: "Timeline View",
        icon: "⏱️",
        category: "Contest Awareness",
        since: "1.40",
        summary: "See upcoming AP windows and candidate timing in a compact contest timeline.",
        description: "The timeline view helps operators understand short propagation windows and candidate timing during active contest operation.",
        tags: ["timeline", "AP windows", "airplane scatter"],
        related: ["airscout", "priority-score"]
    },
    {
        slug: "sked-reminder",
        title: "Sked Reminder",
        icon: "🔔",
        category: "Sked Management",
        since: "1.40",
        summary: "Keep scheduled contacts visible and avoid missing time-critical skeds.",
        description: "KST4Contest keeps skeds in focus while the operator handles chat traffic, logging, bands and aircraft scatter timing.",
        tags: ["sked", "ON4KST", "contest reminder"],
        related: ["priority-score", "airscout", "timeline"]
    },
    {
        slug: "log-sync",
        title: "Log Synchronization",
        icon: "🔄",
        category: "Logger Integration",
        since: "1.31",
        summary: "Connect ON4KST chat workflow with Win-Test, UCXLog and contest logging state.",
        description: "Log synchronization helps KST4Contest understand worked stations, active bands and contest context.",
        tags: ["Win-Test", "UCXLog", "contest logger"],
        related: ["priority-score", "dual-chat"]
    },
    {
        slug: "dual-chat",
        title: "Dual Chat Categories",
        icon: "💬",
        category: "ON4KST Chat",
        since: "1.26",
        summary: "Operate in two ON4KST categories at once, for example VHF/UHF and microwave.",
        description: "Dual category support gives contest operators better overview when working across multiple ON4KST chat rooms.",
        tags: ["ON4KST", "dual chat", "microwave contest"],
        related: ["priority-score", "log-sync"]
    },
    {
        slug: "dx-cluster",
        title: "DXCluster Server",
        icon: "📡",
        category: "Radio Workflow",
        since: "1.23",
        summary: "Use DXCluster information as part of the wider contest operating workflow.",
        description: "KST4Contest includes DXCluster functionality to support situational awareness during contest operation.",
        tags: ["DXCluster", "ham radio", "contest"],
        related: ["log-sync", "priority-score"]
    },
    {
        slug: "macros",
        title: "Macros and Variables",
        icon: "⚡",
        category: "Operator Speed",
        since: "1.0",
        summary: "Send recurring contest messages faster with configurable macros and variables.",
        description: "Macros reduce repetitive typing and help operators respond quickly during active ON4KST contest sessions.",
        tags: ["macros", "ON4KST", "operator workflow"],
        related: ["sked-reminder", "dual-chat"]
    }
];