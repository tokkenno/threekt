import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Shared Kotlin Multiplatform setup for every engine module.
 *
 * Each module compiles to a JVM jar and a Kotlin/JS klib. The JS target is declared as a browser library with
 * ES modules and TypeScript definitions so that the module can be tested on its own, but the npm packages are
 * assembled by `:dist:npm`, which links every module once with per-module granularity.
 */
class MultiplatformLibraryConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        extensions.configure(KotlinMultiplatformExtension::class.java) {
            jvmToolchain(21)
            jvm()
            js {
                browser()
                binaries.library()
                generateTypeScriptDefinitions()
                compilerOptions {
                    target.set("es2015")
                }
            }
            sourceSets.all {
                languageSettings.optIn("kotlin.js.ExperimentalJsExport")
            }
            sourceSets.getByName("commonTest").dependencies {
                implementation(libs.findLibrary("kotlin-test").get())
            }
        }
    }
}
