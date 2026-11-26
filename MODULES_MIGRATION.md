# Module migration plan

Split the single `engine` Gradle module into a renderer-agnostic core and a WebGL/OpenGL ES renderer, so that
future renderers (WebGPU, native backends) can be added as separate modules without touching the core.

## Target layout

| Folder | Gradle path | Kotlin base package | npm package | Contents |
|---|---|---|---|---|
| `engine/core` | `:engine:core` | `com.aitorgf.threekt.core` | `@threekt/core` | Everything that does not talk to a graphics API: math, core, objects, cameras, lights, materials, textures, geometries, extras, curves, helpers, loaders, exporters, controls, scenes, utils, the renderer-neutral `Renderer` API and `CoordinateSystem`. Browser adapters such as `DomInputSurface`, `ImageTexture`, `VideoTexture` and `DocumentPageVisibility` stay in its `jsMain`. |
| `engine/render/gl` | `:engine:render:gl` | `com.aitorgf.threekt.gl` | `@threekt/gl` | `WebGLRenderer`, `WebGLCubeRenderTarget`, every `renderers/webgl` class (`GLContext`, `GL`, `WebGLAttributes`, `WebGLGeometries`, `WebGLPrograms`…), `renderers/shaders/ShaderLib`, `WebGL2Context` (jsMain), `GLES30Context` and `EglHeadlessContext` (jvmMain), and the LWJGL dependencies. |

Dependencies only go from `gl` to `core`. `core` must compile without `gl`.

Packages keep the upstream folder names below the module's base package, as they do today below
`com.aitorgf.threekt`:

| Today | Core module | GL module |
|---|---|---|
| `com.aitorgf.threekt.math.Vector3` | `com.aitorgf.threekt.core.math.Vector3` | |
| `com.aitorgf.threekt.objects.Mesh` | `com.aitorgf.threekt.core.objects.Mesh` | |
| `com.aitorgf.threekt.core.Object3D` | `com.aitorgf.threekt.core.core.Object3D` | |
| `com.aitorgf.threekt.renderers.CoordinateSystem` | `com.aitorgf.threekt.core.renderers.CoordinateSystem` | |
| `com.aitorgf.threekt.renderers.WebGLRenderer` | | `com.aitorgf.threekt.gl.renderers.WebGLRenderer` |
| `com.aitorgf.threekt.renderers.webgl.GLContext` | | `com.aitorgf.threekt.gl.renderers.webgl.GLContext` |

**Open decision:** the upstream `core` folder becomes `com.aitorgf.threekt.core.core`. Confirm this, or choose
a flattened alternative (for example `com.aitorgf.threekt.core` for the upstream `core` classes and
`com.aitorgf.threekt.core.<folder>` for the rest), before step 3.

## Steps

Each step is one or more local commits. After each step: `./gradlew` tests of every existing module pass,
`npm run build` succeeds, all 86 gallery routes load without console errors in headless Chrome, and
`./gradlew :jvmExamples:run --args="--example webgl_geometry_cube"` renders.

### 1. Remove renderer types and GL values from the core code (inside the current `engine` module)

1. Add a renderer-neutral `Renderer` interface in `renderers` and make `WebGLRenderer` implement it.
   `Object3D.onBeforeRender`/`onAfterRender` take `Renderer` instead of `WebGLRenderer`.
2. Remove `glValue` from `Usage`, `BlendFactor`, `BlendEquation` and `TextureFilter`. Port upstream
   `WebGLUtils.convert()` into `renderers/webgl/WebGLUtils.kt` and translate the enums there.
3. Add a `@ThreektRendererApi` opt-in annotation (`@RequiresOptIn`) and use it for the members that renderers
   need but applications should not use. Today these are `internal` and used from `renderers`:
   `BufferAttribute.attributeVersion`, `bytesPerElement`, `ranges`, `read`, `readRaw`, `writeRaw`,
   `createEmpty`, `InterleavedBuffer.storage`, `InterleavedBuffer.bytesPerElement` and
   `CubeCamera.Face.axis`/`sign`. Make them public with the annotation; renderer code opts in at file level.
4. Make sure nothing outside `renderers` imports `renderers.webgl`, `renderers.shaders`, `WebGLRenderer` or
   `WebGLCubeRenderTarget`. `CubeCamera` keeps rendering through a callback; the cube render target becomes a
   core `RenderTarget` once render targets are ported.

### 2. Create the Gradle modules with shared build logic

1. Add an included build `build-logic` with a convention plugin for the shared Kotlin Multiplatform setup:
   `jvmToolchain(21)`, JVM target, JS `browser()` library with ES modules, `es2015` target, TypeScript
   definitions and the `kotlin.js.ExperimentalJsExport` opt-in.
2. `settings.gradle.kts`: include `:engine:core` and `:engine:render:gl` with their folders, and remove
   `:engine`.
3. `engine/render/gl` depends on `project(":engine:core")` as an `api` dependency, plus LWJGL on the JVM.
   Karma configuration (`karma.config.d`) moves with the browser rendering tests.
4. Configure `outputModuleName` as `threekt-core` and `threekt-gl` and the npm package names as
   `@threekt/core` and `@threekt/gl`.

### 3. Move the code

1. `git mv` the sources into `engine/core/src/...` and `engine/render/gl/src/...` so that history follows the
   files.
2. Rewrite package declarations and imports with the new base packages, following the table above.
3. Tests follow their code: rendering tests (`jsTest/.../renderers`) and `WebGLRenderListTest` move to `gl`;
   everything else stays in `core`.

### 4. JavaScript packaging

1. Prototype first: build `@threekt/gl` as a Kotlin/JS library and confirm that its output imports
   `threekt-core.mjs` instead of embedding the core. The project already uses per-module output (separate
   `kotlin-kotlin-stdlib.mjs`).
2. Publish layout: `@threekt/core` holds `threekt-core.mjs`, the Kotlin runtime files and `threekt-core.d.mts`.
   `@threekt/gl` holds only `threekt-gl.mjs` and `threekt-gl.d.mts`, with `@threekt/core` as a dependency.
   A small Gradle task in the `gl` module rewrites the relative imports of `threekt-core.mjs` and of the
   runtime files into `@threekt/core` imports and drops the duplicated files.
3. Verify in a browser test that a `Mesh` created through `@threekt/core` renders with a `WebGLRenderer` from
   `@threekt/gl`, which proves that both packages share one copy of each class.
4. Fallback if the rewrite is not reliable: `@threekt/gl` re-exports the core API from its own bundle and
   `@threekt/core` is only used by code that never renders. Decide before step 5.

### 5. Consumers

1. Root `package.json` workspaces point at the two distribution folders; `npm run build:engine` builds both.
2. `webExamples` imports scene classes from `@threekt/core` and `WebGLRenderer`/`WebGL2Context` from
   `@threekt/gl`. Update all examples and `vite` configuration.
3. `jvmExamples` depends on `:engine:render:gl`.
4. CI (`.github/workflows/tests.yml`) runs the tests of both modules and uploads both report folders.

### 6. Documentation

1. `README.md`: new build commands, module layout and npm packages.
2. `AGENTS.md`: replace the single-module rules (test command, source set paths) with the module rules.
3. `PROGRESS.md`: note the migration.
4. Delete this file.

## Risks

- **Gradle build cache and Kotlin/JS klibs.** A cached test klib compiled against an older main klib produced
  mismatched JS names (`… is not a function`) during the core port. With more modules this gets more likely.
  Run tests with `--no-build-cache` while migrating, and consider disabling caching for the Kotlin/JS compile
  tasks if it reappears.
- **Exported names across modules.** `@JsExport` names must stay unique across both modules, and each module
  generates its own `.d.mts`, which must reference core types through `@threekt/core`.
- **Package rename churn.** Every Kotlin file, test and JVM example changes its imports; do the rename in a
  commit of its own, without behavior changes, so that it is easy to review.
