const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const {
    formatVisitorCount,
    loadVisitorCount,
    validateVisitorCount
} = require("../src/assets/js/visitor-count");

const VALID_DATA = {
    schemaVersion: 1,
    visits: 1234,
    since: "2026-09-11",
    updatedAt: "2026-09-11T07:00:00Z"
};

function element() {
    return { hidden: true, textContent: "" };
}

test("formats valid data for the English website", async () => {
    const target = element();
    let request;
    const shown = await loadVisitorCount({
        element: target,
        fetchImpl: async (url, options) => {
            request = { url, options };
            return { ok: true, json: async () => VALID_DATA };
        }
    });

    assert.equal(shown, true);
    assert.equal(target.hidden, false);
    assert.equal(target.textContent, "Visits since 11 September 2026: 1,234");
    assert.equal(request.url, "/visitor-count.json");
    assert.equal(request.options.credentials, "omit");
});

test("rejects unsupported schemas and invalid visit counts", () => {
    for (const changes of [
        { schemaVersion: 2 },
        { visits: -1 },
        { visits: 1.5 },
        { visits: Number.MAX_SAFE_INTEGER + 1 }
    ]) {
        assert.equal(validateVisitorCount({ ...VALID_DATA, ...changes }), null);
    }
});

test("rejects invalid dates and timestamps without timezone shifts", () => {
    for (const changes of [
        { since: "2026-02-30" },
        { since: "11-09-2026" },
        { updatedAt: "2026-09-11" },
        { updatedAt: "2026-02-30T07:00:00Z" },
        { updatedAt: "2026-09-11T25:00:00Z" },
        { updatedAt: "not-a-timestamp" }
    ]) {
        assert.equal(formatVisitorCount({ ...VALID_DATA, ...changes }), null);
    }
});

test("keeps the element hidden for HTTP and fetch failures", async () => {
    for (const fetchImpl of [
        async () => ({ ok: false }),
        async () => { throw new Error("network failure"); }
    ]) {
        const target = element();
        assert.equal(await loadVisitorCount({ element: target, fetchImpl }), false);
        assert.equal(target.hidden, true);
        assert.equal(target.textContent, "");
    }
});

test("aborts a slow request and keeps the element hidden", async () => {
    const target = element();
    let aborted = false;
    const fetchImpl = (url, options) => new Promise((resolve, reject) => {
        options.signal.addEventListener("abort", () => {
            aborted = true;
            reject(new Error("aborted"));
        });
    });

    assert.equal(await loadVisitorCount({
        element: target,
        fetchImpl,
        timeoutMs: 5
    }), false);
    assert.equal(aborted, true);
    assert.equal(target.hidden, true);
});

test("uses textContent and loads the script only for the home page", () => {
    const script = fs.readFileSync(path.join(
        __dirname,
        "../src/assets/js/visitor-count.js"
    ), "utf8");
    const home = fs.readFileSync(path.join(__dirname, "../src/index.njk"), "utf8");
    const layout = fs.readFileSync(path.join(
        __dirname,
        "../src/_layouts/base.njk"
    ), "utf8");
    const sourceRoot = path.join(__dirname, "../src");
    const otherPages = fs.readdirSync(sourceRoot, { recursive: true })
        .filter(entry => entry.endsWith(".njk") && entry !== "index.njk");

    assert.doesNotMatch(script, /innerHTML/);
    assert.match(script, /textContent/);
    assert.match(home, /^visitorCount: true$/m);
    assert.match(layout, /\{% if visitorCount %\}/);
    assert.equal(
        otherPages.some(entry => fs.readFileSync(path.join(sourceRoot, entry), "utf8")
            .includes("visitorCount: true")),
        false
    );
});
