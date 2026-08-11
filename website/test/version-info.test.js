const assert = require("node:assert/strict");
const test = require("node:test");

const generateVersionInfo =
    require("../src/_data/versionInfo");

const {
    validateVersionInfo
} = require("../scripts/validate-version-info");

const VALID_XML =
    `<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<praktiKST>
    <latestVersion>
        <versionNumber>1.411</versionNumber>
        <semanticVersion>1.41.1</semanticVersion>
        <adminMessage></adminMessage>
        <majorChanges>Hotfix</majorChanges>
        <latestVersionPathOnWebserver>https://github.com/praktimarc/kst4contest/releases/tag/v1.41.1</latestVersionPathOnWebserver>
    </latestVersion>
    <needUpdateSinceLastVersion>
        <filename>nothing</filename>
    </needUpdateSinceLastVersion>
    <changeLog>
        <changedVersionNumber>1.41.1</changedVersionNumber>
        <date>2026-07-08</date>
        <description>Hotfix</description>
        <added></added>
        <changed></changed>
        <fixed>Text input handling</fixed>
        <removed></removed>
    </changeLog>
</praktiKST>`;

test("accepts a complete update feed", () => {
    const result =
        validateVersionInfo(VALID_XML, "v1.41.1");

    assert.equal(result.semanticVersion, "1.41.1");
    assert.equal(result.changeLogEntries, 1);
});

test("rejects the former empty fallback", () => {
    assert.throws(
        () =>
            validateVersionInfo(
                '<?xml version="1.0" '
                + 'encoding="UTF-8"?>'
                + "<praktiKST></praktiKST>"
            ),
        /empty or implausibly small/
    );
});

test(
    "rejects a feed which does not contain "
    + "the expected Stable release",
    () => {
        assert.throws(
            () =>
                validateVersionInfo(
                    VALID_XML,
                    "v1.42.0"
                ),
            /does not match expected release/
        );
    }
);

test(
    "aborts generation when GitHub rejects "
    + "the API request",
    async () => {
        const originalFetch = global.fetch;

        global.fetch = async () => ({
            ok: false,
            status: 401,
            headers: {
                get: () => null
            }
        });

        try {
            await assert.rejects(
                generateVersionInfo(),
                /website build has been aborted.*HTTP 401/i
            );
        } finally {
            global.fetch = originalFetch;
        }
    }
);