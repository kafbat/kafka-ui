import { LaunchOptions, chromium, firefox, webkit } from "@playwright/test";

const options: LaunchOptions = {
    headless: process.env.HEAD !== "true",
    args: ['--lang=en-US'],
}

export const invokeBrowser = () => {
    const browserType = process.env.npm_config_BROWSER || "chrome";

    switch (browserType) {
        case "chrome":
            return chromium.launch(options);
        case "firefox":
            return firefox.launch(options);
        case "webkit":
            return webkit.launch(options);
        default:
            throw new Error("Please set the proper browser!")
    }

}
