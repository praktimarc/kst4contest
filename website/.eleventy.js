module.exports = function (eleventyConfig) {
    eleventyConfig.addPassthroughCopy({ "src/assets": "assets" });

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