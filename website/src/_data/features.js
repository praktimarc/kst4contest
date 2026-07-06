module.exports = [
    {
        slug: "priority-score",
        title: "Priority Score System",
        icon: "🎯",
        category: "Contest Workflow",
        summary: "Find the most promising stations faster using contest-aware candidate scoring.",
        description: "KST4Contest ranks stations by contest-relevant signals such as activity, direction, sked context, bands, frequencies and operator workflow.",
        keywords: ["ON4KST", "contest", "priority candidates", "VHF", "UHF", "SHF"],
        related: ["airscout", "sked-reminder", "timeline"]
    },
    {
        slug: "airscout",
        title: "AirScout Integration",
        icon: "✈️",
        category: "Aircraft Scatter",
        summary: "Use airplane scatter awareness directly inside your ON4KST contest workflow.",
        description: "AirScout information helps operators evaluate AP windows, aircraft scatter timing and candidate stations during VHF/UHF/SHF contests.",
        keywords: ["AirScout", "airplane scatter", "aircraft scatter", "VHF contest"],
        related: ["timeline", "priority-score", "sked-reminder"]
    },
    {
        slug: "timeline",
        title: "Timeline View",
        icon: "⏱️",
        category: "Contest Awareness",
        summary: "See upcoming AP windows and candidate timing in a compact contest timeline.",
        description: "The timeline view helps operators understand short propagation windows and candidate timing during active contest operation.",
        keywords: ["AP timeline", "airplane scatter", "contest timing"],
        related: ["airscout", "priority-score"]
    },
    {
        slug: "sked-reminder",
        title: "Sked Reminder",
        icon: "🔔",
        category: "Sked Management",
        summary: "Keep scheduled contacts visible and avoid missing time-critical skeds.",
        description: "KST4Contest keeps skeds in focus while the operator handles chat traffic, logging, bands and aircraft scatter timing.",
        keywords: ["sked", "ON4KST sked", "contest reminder"],
        related: ["priority-score", "airscout"]
    },
    {
        slug: "log-sync",
        title: "Log Synchronization",
        icon: "🔄",
        category: "Logger Integration",
        summary: "Connect ON4KST chat workflow with Win-Test, UCXLog and contest logging state.",
        description: "Log synchronization helps KST4Contest understand worked stations, active bands and contest context.",
        keywords: ["Win-Test", "UCXLog", "contest logger", "ON4KST"],
        related: ["priority-score"]
    },
    {
        slug: "dual-chat",
        title: "Dual Chat Categories",
        icon: "💬",
        category: "ON4KST Chat",
        summary: "Operate in two ON4KST categories at once, for example VHF/UHF and microwave.",
        description: "Dual category support gives contest operators better overview when working across multiple ON4KST chat rooms.",
        keywords: ["ON4KST", "dual chat", "microwave contest"],
        related: ["priority-score", "log-sync"]
    }
];