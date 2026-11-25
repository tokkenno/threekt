# Threekt

ThreeKt (Provisional name) is 3D Engine based on ThreeJs and implemented as Kotlin Multiplatform library.

The goal is not to achieve a 1:1 port, but to modularize and write more maintainable code, to serve as a code base that can be run on the web (WebGL), desktop (OpenGL, Vulkan) and headless environments (Software) for simulations.

## Motivations
I've been working with Three.js for years, and although it's a great framework for the web, I've always run into a roadblock when trying to use it on certain platforms. Specifically, I currently need to render simulations in headless environments and record them, and it seems easier to me to port the engine itself than all the software I've built on top of it.

For now, this project will be for personal use, and we’ll see how far I can get with the help of AI. If you find it useful for anything else, let me know, and we can see about building a more robust project. This is a WORK IN PROGRESS. Its use is discouraged.

## Run the Web examples

From the repository root, build the Kotlin/JS package before installing the npm workspace dependencies.

```sh
npm run build:engine
npm ci
npm start
```

Vite prints both URLs. From another device on the same network, open
`http://<development-machine-ip>:8080` or `https://<development-machine-ip>:8443`.

HTTPS uses a self-signed certificate generated locally with OpenSSL. To avoid browser trust errors, trust
the public certificate printed at startup on the device you are testing from, or set
`THREEKT_HTTPS_CERT` and `THREEKT_HTTPS_KEY` to the absolute paths of your own PEM certificate and key
before starting the app.

To build the examples for production, run `npm run build`.

## Run the JVM examples

```sh
./gradlew :jvmApp:run
```

The menu lists the available examples. To open one directly, pass its upstream ID, for example `./gradlew :jvmApp:run --args="--example webgl_buffergeometry_rawshader"`.

### Parameters
- `--headless` runs without a window.
- `--software` use software rendering without a GPU.
  - `--angle-dir <path>` on Windows points to the directory with the matching `libEGL.dll` and `libGLESv2.dll` for your JVM architecture. Include that directory in `PATH` so Windows can resolve any dependencies of the DLLs.
- `--width <pixels>` framebuffer width.
- `--height <pixels>` framebuffer height.
- `--time <seconds>` passes a timestamp to the example's frame callback and defaults to zero.
- `--screenshot <file>` saves a screenshot after two seconds and closes the app.

### Render without a window

Can run the JVM app without a window to render offscreen and save a screenshot.

#### Dependencies
- Linux: Mesa EGL and OpenGL ES
   - Debian/Ubuntu: `libegl1 libegl-mesa0 libgles2 libgl1-mesa-dri`
- Windows: ANGLE with D3D11 and OpenGL ES 3 support
