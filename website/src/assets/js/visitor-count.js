"use strict";

const VISITOR_COUNT_ENDPOINT = "/visitor-count.json";
const VISITOR_COUNT_TIMEOUT_MS = 2500;

function parseIsoDate(value) {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);

    if (!match) {
        return null;
    }

    const year = Number(match[1]);
    const month = Number(match[2]);
    const day = Number(match[3]);
    const date = new Date(Date.UTC(year, month - 1, day));

    if (date.getUTCFullYear() !== year
        || date.getUTCMonth() !== month - 1
        || date.getUTCDate() !== day) {
        return null;
    }

    return date;
}

function isIsoTimestamp(value) {
    if (typeof value !== "string") {
        return false;
    }

    const match = /^(\d{4}-\d{2}-\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.\d+)?(Z|[+-](\d{2}):(\d{2}))$/.exec(value);

    if (!match || !parseIsoDate(match[1])) {
        return false;
    }

    const hour = Number(match[2]);
    const minute = Number(match[3]);
    const second = Number(match[4]);
    const offsetHour = match[6] === undefined ? 0 : Number(match[6]);
    const offsetMinute = match[7] === undefined ? 0 : Number(match[7]);

    return hour <= 23
        && minute <= 59
        && second <= 59
        && offsetHour <= 23
        && offsetMinute <= 59
        && Number.isFinite(Date.parse(value));
}

function validateVisitorCount(data) {
    if (!data || typeof data !== "object" || Array.isArray(data)) {
        return null;
    }

    if (data.schemaVersion !== 1
        || !Number.isSafeInteger(data.visits)
        || data.visits < 0
        || !isIsoTimestamp(data.updatedAt)) {
        return null;
    }

    const since = typeof data.since === "string"
        ? parseIsoDate(data.since)
        : null;

    return since ? { visits: data.visits, since } : null;
}

function formatVisitorCount(data) {
    const valid = validateVisitorCount(data);

    if (!valid) {
        return null;
    }

    const date = new Intl.DateTimeFormat("en-GB", {
        day: "numeric",
        month: "long",
        year: "numeric",
        timeZone: "UTC"
    }).format(valid.since);
    const visits = new Intl.NumberFormat("en-GB").format(valid.visits);

    return `Visits since ${date}: ${visits}`;
}

async function loadVisitorCount({
    element,
    fetchImpl = globalThis.fetch,
    AbortControllerImpl = globalThis.AbortController,
    timeoutMs = VISITOR_COUNT_TIMEOUT_MS
}) {
    if (!element || typeof fetchImpl !== "function"
        || typeof AbortControllerImpl !== "function") {
        return false;
    }

    element.hidden = true;
    element.textContent = "";

    const controller = new AbortControllerImpl();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);

    try {
        const response = await fetchImpl(VISITOR_COUNT_ENDPOINT, {
            credentials: "omit",
            signal: controller.signal
        });

        if (!response.ok) {
            return false;
        }

        const text = formatVisitorCount(await response.json());

        if (!text) {
            return false;
        }

        element.textContent = text;
        element.hidden = false;
        return true;
    } catch {
        return false;
    } finally {
        clearTimeout(timeout);
    }
}

if (typeof document !== "undefined") {
    const element = document.querySelector("[data-visitor-count]");

    if (element) {
        loadVisitorCount({ element });
    }
}

if (typeof module !== "undefined" && module.exports) {
    module.exports = {
        formatVisitorCount,
        loadVisitorCount,
        parseIsoDate,
        validateVisitorCount
    };
}
