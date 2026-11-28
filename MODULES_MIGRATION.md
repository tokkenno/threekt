# Renderer module architecture

`engine/core` owns the renderer-independent scene graph, data and math. The `Renderer` interface provides
`render(scene, camera)` and `dispose()`. Render callbacks receive the renderer instance through this interface.
`CubeCamera.update` supplies face cameras through a callback, so attachment allocation and rendering stay with
the backend.

`engine/render/gl` depends on the core and owns WebGL 2 and OpenGL ES 3.0 rendering. Its Kotlin packages
start at `com.aitorgf.threekt.render.gl`. The GL module implements the renderer contract, translates the
core's usage, blend and sampling enums, and provides GLSL materials and framebuffer textures. Its JS and
JVM adapters are `WebGL2Context` and `GLES30Context`. The JVM adapter uses LWJGL.

Additional renderers can live in `engine/render/<name>` and depend on the core independently. Put their
graphics API dependencies, shader representations and target lifecycles in those modules. Shared low-level
members used across the renderer module boundary opt in to `ThreektRendererApi`.

The JVM examples consume the GL module through Gradle. The web gallery imports `threekt/render-gl`, using the
combined distribution from `:engine:dist:npm`. It links all engine modules with one Kotlin runtime and emits
a TypeScript declaration file for each module.
