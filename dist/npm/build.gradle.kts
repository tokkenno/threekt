import groovy.json.JsonOutput

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

/** Engine modules shipped in the npm package, as Gradle path to the ES module name set by `outputModuleName`. */
val engineModules = mapOf(
    ":engine:core" to "threekt-core",
)

/** Engine modules with the folders holding their sources, used to place type-only declarations. */
val engineModuleSources = engineModules.toList()

kotlin {
    js {
        // Linking this module emits one ES module per engine module and per Kotlin runtime library
        // (per-module granularity, the Kotlin/JS default), so modules share one copy of the runtime.
        outputModuleName = "threekt-npm"
        browser()
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            target.set("es2015")
        }
    }
    sourceSets {
        jsMain.dependencies {
            engineModules.keys.forEach { api(project(it)) }
        }
    }
}

/**
 * Splits the TypeScript declarations of one distribution into one `.d.mts` per ES module.
 *
 * Kotlin writes the declarations of every linked module into `threekt-npm.d.mts`. The split gives the Kotlin
 * runtime and each engine module their own declaration file, which imports the types it uses from the others.
 */
fun registerDeclarationSplit(distribution: String) =
    tasks.register<SplitTypeScriptDeclarations>("split${distribution}Declarations") {
        val directory = layout.buildDirectory.dir("dist/js/${distribution.replaceFirstChar { it.lowercase() }}Library")
        dependsOn("jsBrowser${distribution}LibraryDistribution")
        modulesDirectory.set(directory)
        declarations.set(directory.map { it.file("threekt-npm.d.mts") })
        engineModules.set(engineModuleSources.map { (path, module) ->
            objects.newInstance<SplitTypeScriptDeclarations.EngineModule>().apply {
                name.set(module)
                sources.from(rootProject.layout.projectDirectory.dir(path.removePrefix(":").replace(':', '/') + "/src"))
            }
        })
        outputDirectory.set(layout.buildDirectory.dir("declarations/${distribution.lowercase()}"))
    }

/**
 * Copies the linked ES modules of one distribution and their declarations into an npm package.
 *
 * The package exports each engine module under its own subpath (`threekt/core`), and the root entry point
 * resolves to the core. The Kotlin runtime modules ship next to them as private files that the engine modules
 * import with relative paths. The `threekt-npm` entry module of this build is dropped.
 */
fun registerNpmPackage(name: String, distribution: String, declarations: TaskProvider<SplitTypeScriptDeclarations>) =
    tasks.register<Sync>(name) {
    group = "distribution"
    description = "Assembles the npm package from the $distribution Kotlin/JS distribution."
    dependsOn("jsBrowser${distribution}LibraryDistribution")
    val sourceDir = layout.buildDirectory.dir("dist/js/${distribution.replaceFirstChar { it.lowercase() }}Library")
    from(sourceDir) {
        include("*.mjs", "*.mjs.map")
        exclude("threekt-npm.mjs", "threekt-npm.mjs.map")
    }
    from(declarations)
    into(layout.buildDirectory.dir("packages/${distribution.lowercase()}"))

    // Same rule as the Kotlin/JS package.json: npm needs a semantic version.
    val packageVersion = project.version.toString().let { if (it == "unspecified") "0.0.0-unspecified" else it }
    val modules = engineModules.values.toList()
    doLast {
        val exports = linkedMapOf<String, Any>(
            "." to mapOf("types" to "./threekt-core.d.mts", "default" to "./threekt-core.mjs"),
        )
        modules.forEach { module ->
            exports["./" + module.removePrefix("threekt-")] =
                mapOf("types" to "./$module.d.mts", "default" to "./$module.mjs")
        }
        val manifest = linkedMapOf(
            "name" to "threekt",
            "version" to packageVersion,
            "type" to "module",
            "main" to "threekt-core.mjs",
            "types" to "threekt-core.d.mts",
            "exports" to exports,
        )
        destinationDir.resolve("package.json").writeText(JsonOutput.prettyPrint(JsonOutput.toJson(manifest)) + "\n")
    }
}

registerNpmPackage("npmDevelopmentPackage", "Development", registerDeclarationSplit("Development"))
registerNpmPackage("npmProductionPackage", "Production", registerDeclarationSplit("Production"))
