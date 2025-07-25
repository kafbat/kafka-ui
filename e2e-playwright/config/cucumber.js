/* eslint-disable */
module.exports = {
    default: {
        timeout: 60000,
        tags: process.env.npm_config_TAGS || "",
        formatOptions: {
            snippetInterface: "async-await"
        },
        paths: [
            "src/features/"
        ],
        dryRun: false,
        require: [
            "src/steps/*.ts",
            "src/hooks/hooks.ts",
            "src/support/customWorld.ts"
        ],
        requireModule: [
            "ts-node/register"
        ],
        format: [
            "html:test-results/cucumber-report.html",
            "json:test-results/cucumber-report.json",
            "rerun:@rerun.txt"
        ],
        parallel: 1
    },
    rerun: {
        formatOptions: {
            snippetInterface: "async-await"
        },
        publishQuiet: true,
        dryRun: false,
        require: [
            "src/steps/*.ts",
            "src/hooks/hooks.ts",
            "src/support/customWorld.ts"
        ],
        requireModule: [
            "ts-node/register"
        ],
        format: [
            "html:test-results/cucumber-report.html",
            "json:test-results/cucumber-report.json",
            "rerun:@rerun.txt"
        ],
        parallel: 1
    }
}
