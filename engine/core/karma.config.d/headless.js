// Chrome's sandbox may be unavailable on the CI runner.
config.set({
    browsers: ['ChromeHeadlessCore'],
    customLaunchers: {
        ChromeHeadlessCore: {
            base: 'ChromeHeadless',
            flags: process.env.CI ? ['--no-sandbox'] : [],
        },
    },
});
