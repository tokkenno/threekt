// Browser tests draw with WebGL2. Headless Chrome on machines without a GPU, such as CI runners, only
// provides WebGL through SwiftShader when it is explicitly allowed, so the tests always use that software
// renderer. This also keeps the pixels the same locally and in CI.
config.set({
    browsers: ['ChromeHeadlessWebGL'],
    customLaunchers: {
        ChromeHeadlessWebGL: {
            base: 'ChromeHeadless',
            flags: [
                '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist',
                // AppArmor on Ubuntu 24.04 runners can block Chrome's sandbox; CI machines are disposable.
                ...(process.env.CI ? ['--no-sandbox'] : []),
            ],
        },
    },
});
