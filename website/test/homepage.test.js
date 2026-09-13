const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const eleventyConfig = require("../.eleventy.js");

test("selects the newest news article without relying on a version", () => {
    const older = { date: new Date("2026-08-22"), data: { title: "Older" } };
    const newest = { date: new Date("2026-09-03"), data: { title: "Newest" } };

    assert.deepEqual(
        eleventyConfig.selectLatestNews([older, newest]),
        [newest]
    );
});

test("an empty news collection omits the latest-news item safely", () => {
    assert.deepEqual(eleventyConfig.selectLatestNews([]), []);
    assert.deepEqual(eleventyConfig.selectLatestNews(), []);
});

test("homepage keeps the statistical result tied to its limits", () => {
    const source = fs.readFileSync(
        path.join(__dirname, "../src/index.njk"),
        "utf8"
    );

    assert.match(source, /almost 30% higher chat reply rate/);
    assert.match(source, /chat replies, not completed QSOs/);
    assert.match(source, /correlation within[\s\S]*not general proof of causation/);
    assert.match(source, /July 2025 contest/);
    assert.match(source, /144\/432 MHz and Microwave categories/);
});

test("background page links all public papers and no ODT source", () => {
    const source = fs.readFileSync(
        path.join(__dirname, "../src/background/index.njk"),
        "utf8"
    );

    for (const name of [
        "kst4contest-ghz-tagung-2025-de.pdf",
        "kst4contest-ghz-tagung-2025-en.pdf",
        "kst4contest-ghz-tagung-2026-de.pdf",
        "kst4contest-ghz-tagung-2026-en.pdf"
    ]) {
        assert.match(source, new RegExp(name.replaceAll(".", "\\.")));
    }
    assert.doesNotMatch(source, /\.odt\b/i);
});

test("homepage contains one selected function grid and a complete-overview link", () => {
    const source = fs.readFileSync(
        path.join(__dirname, "../src/index.njk"),
        "utf8"
    );

    assert.equal((source.match(/<h2>The parts most often used during a contest<\/h2>/g) || []).length, 1);
    assert.match(source, /href="\/features\/"[^>]*>Open the complete function overview/);
    assert.doesNotMatch(source, /Observe|Evaluate|>Act<|One workflow, shared context/);
});

test("screenshot page uses the four approved originals as full-size links", () => {
    const page = fs.readFileSync(
        path.join(__dirname, "../src/screenshots/index.njk"),
        "utf8"
    );
    const css = fs.readFileSync(
        path.join(__dirname, "../src/assets/css/main.css"),
        "utf8"
    );
    const screenshots = require("../src/_data/screenshots.js");

    assert.equal(screenshots.length, 4);
    assert.deepEqual(
        screenshots.map(({ image }) => image),
        [
            "/assets/completeViewLight.png",
            "/assets/completeViewDark.png",
            "/assets/timeline.png",
            "/assets/complete_incl_AS.png"
        ]
    );
    assert.ok(screenshots.every(({ alt, caption }) => alt && caption));
    assert.equal(screenshots.find(({ id }) => id === "timeline").wide, true);
    assert.doesNotMatch(JSON.stringify(screenshots), /Priority Candidates/);
    assert.match(page, /href="{{ shot\.image }}"[\s\S]*<img[^>]*src="{{ shot\.image }}"/);
    assert.match(page, /Version-like text inside chat messages is[\s\S]*not an announcement/);
    assert.match(css, /\.screenshot-image\s*\{[\s\S]*?width:\s*100%;[\s\S]*?height:\s*auto;/);
    assert.match(css, /\.screenshot-card--wide\s*\{\s*grid-column:\s*1\s*\/\s*-1;/);
});

test("footer and download page expose background and conference papers", () => {
    const layout = fs.readFileSync(
        path.join(__dirname, "../src/_layouts/base.njk"),
        "utf8"
    );
    const download = fs.readFileSync(
        path.join(__dirname, "../src/download/index.njk"),
        "utf8"
    );

    assert.match(layout, /<p><a href="\/background\/">Background<\/a><\/p>/);
    for (const name of [
        "kst4contest-ghz-tagung-2025-de.pdf",
        "kst4contest-ghz-tagung-2025-en.pdf",
        "kst4contest-ghz-tagung-2026-de.pdf",
        "kst4contest-ghz-tagung-2026-en.pdf"
    ]) {
        assert.match(download, new RegExp(`/assets/papers/${name.replaceAll(".", "\\.")}`));
    }
    assert.match(download, /href="\/background\/#paper-2025"/);
    assert.match(download, /href="\/background\/#paper-2026"/);
    assert.match(download, /historical conference papers[\s\S]*not a complete description of the current feature set/);
    assert.ok(
        download.indexOf("Background from GHz-Tagung Dorsten") >
        download.indexOf('<div class="channel-panel" data-channel="nightly">')
    );
});
