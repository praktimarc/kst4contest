const fs = require("fs");
const path = require("path");
const markdownIt = require("markdown-it");
const markdownItAnchor = require("markdown-it-anchor");

function rewriteManualLinks(content, lang) {
    return (content || "")
        // GitHub-Wiki-Links im Stil [[de-Installation]] oder [[en-Installation]]
        .replace(/\[\[(en|de)-([^\]|]+)\]\]/g, (match, linkLang, slug) => {
            return `[${slug.replace(/-/g, " ")}](/manual/${linkLang}/${slug.toLowerCase()}/)`;
        })

        // GitHub-Wiki-Links mit Label: [[Text|de-Installation]]
        .replace(/\[\[([^\]|]+)\|(en|de)-([^\]]+)\]\]/g, (match, label, linkLang, slug) => {
            return `[${label}](/manual/${linkLang}/${slug.toLowerCase()}/)`;
        })

        // Markdown-Links auf de-/en-Dateien ohne .md
        .replace(/\]\((en|de)-([^)#]+)\)/g, (match, linkLang, slug) => {
            return `](/manual/${linkLang}/${slug.toLowerCase()}/)`;
        })

        // Markdown-Links auf de-/en-Dateien mit .md
        .replace(/\]\((en|de)-([^)#]+)\.md\)/g, (match, linkLang, slug) => {
            return `](/manual/${linkLang}/${slug.toLowerCase()}/)`;
        });
}

module.exports = function (eleventyConfig) {

    eleventyConfig.addFilter("dateToIso", function (dateObj) {
        return new Date(dateObj).toISOString().split("T")[0];
    });

    eleventyConfig.addFilter("dateToRfc822", function (dateObj) {
        return new Date(dateObj).toUTCString();
    });

    eleventyConfig.addFilter("readableDate", function (dateObj) {
        return new Intl.DateTimeFormat("en", {
            year: "numeric",
            month: "long",
            day: "numeric"
        }).format(dateObj);
    });

    eleventyConfig.addCollection("sortedFeatures", function (collectionApi) {
        return collectionApi.getFilteredByTag("features").sort((a, b) => {
            return (a.data.order || 999) - (b.data.order || 999);
        });
    });

    eleventyConfig.addCollection("featuredFeatures", function (collectionApi) {
        return collectionApi.getFilteredByTag("features")
            .filter(item => item.data.featured)
            .sort((a, b) => {
                return (a.data.order || 999) - (b.data.order || 999);
            });
    });

    eleventyConfig.addFilter("whereTag", function(collection, tag) {
        return collection.filter(item => item.data.tags && item.data.tags.includes(tag));
    });

    eleventyConfig.addPassthroughCopy({ "src/assets": "assets" });

    const md = markdownIt({
        html: true,
        linkify: true,
        typographer: true
    }).use(markdownItAnchor, {
        permalink: markdownItAnchor.permalink.headerLink()
    });

    eleventyConfig.addFilter("markdown", function (value) {
        return md.render(value || "");
    });

    eleventyConfig.addCollection("manualPages", function () {
        const docsDir = path.join(__dirname, "..", "github_docs");

        if (!fs.existsSync(docsDir)) {
            return [];
        }

        return fs.readdirSync(docsDir)
            .filter(file => /^(en|de)-.+\.md$/.test(file))
            .map(file => {
                const fullPath = path.join(docsDir, file);
                const raw = fs.readFileSync(fullPath, "utf8");

                const lang = file.startsWith("de-") ? "de" : "en";
                const slug = file
                    .replace(/^en-/, "")
                    .replace(/^de-/, "")
                    .replace(/\.md$/, "")
                    .toLowerCase();

                const title = file
                    .replace(/^en-/, "")
                    .replace(/^de-/, "")
                    .replace(/\.md$/, "")
                    .replace(/-/g, " ");

                return {
                    file,
                    lang,
                    slug,
                    title,
                    content: rewriteManualLinks(raw, lang)
                };
            });
    });

    return {
        dir: {
            input: "src",
            output: "_site",
            includes: "_includes",
            layouts: "_layouts"
        },
        markdownTemplateEngine: "njk",
        htmlTemplateEngine: "njk"
    };
};
