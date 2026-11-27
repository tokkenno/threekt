import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Splits the TypeScript declarations of a per-module Kotlin/JS link into one `.d.mts` per ES module.
 *
 * Kotlin writes the declarations of every linked module into the `.d.mts` of the entry module. This task assigns
 * each top-level declaration to the ES module that exports it at runtime, and writes `<module>.d.mts` next to each
 * `<module>.mjs`, with `import` statements for the declarations it uses from other modules. Exported interfaces
 * have no runtime value, so they are assigned to the engine module whose Kotlin sources declare them. The
 * non-exported helpers `Nullable` and `KtSingleton` are repeated in every file that uses them.
 *
 * The task fails when a declaration cannot be assigned to exactly one module.
 */
@CacheableTask
abstract class SplitTypeScriptDeclarations : DefaultTask() {
    /** The `.d.mts` that Kotlin generated for the entry module of the link. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val declarations: RegularFileProperty

    /** The linked distribution holding one `.mjs` per module. */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val modulesDirectory: DirectoryProperty

    /** Engine modules whose sources are searched for type-only declarations. */
    @get:Nested
    abstract val engineModules: org.gradle.api.provider.ListProperty<EngineModule>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    abstract class EngineModule {
        /** ES module name, for example `threekt-core`. */
        @get:Input
        abstract val name: Property<String>

        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val sources: ConfigurableFileCollection
    }

    @TaskAction
    fun split() {
        val exporters = mutableMapOf<String, MutableSet<String>>()
        modulesDirectory.get().asFile.listFiles { file -> file.name.endsWith(".mjs") }!!.sorted().forEach { file ->
            exportedNames(file.readText()).forEach {
                exporters.getOrPut(it) { mutableSetOf() } += file.name.removeSuffix(".mjs")
            }
        }
        val interfacePattern = Regex("""^\s*(?:(?:public|actual|expect|external|sealed|fun)\s+)*interface\s+(\w+)""", RegexOption.MULTILINE)
        val typeOnly = mutableMapOf<String, MutableSet<String>>()
        engineModules.get().forEach { module ->
            module.sources.asFileTree.matching { include("**/*.kt") }.forEach { file ->
                interfacePattern.findAll(file.readText()).forEach {
                    typeOnly.getOrPut(it.groupValues[1]) { mutableSetOf() } += module.name.get()
                }
            }
        }

        val owners = mutableMapOf<String, String>()
        val files = linkedMapOf<String, MutableList<Declaration>>()
        val helpers = mutableListOf<Declaration>()
        for (declaration in parse(declarations.get().asFile.readText())) {
            val name = declaration.name
            if (name == "Nullable" || name == "KtSingleton") {
                helpers += declaration
                continue
            }
            val candidates = exporters[name] ?: typeOnly[name].orEmpty()
            val module = candidates.singleOrNull()
                ?: error("Cannot tell which module declares $name (candidates: $candidates).")
            owners[name] = module
            files.getOrPut(module) { mutableListOf() } += declaration
        }

        val output = outputDirectory.get().asFile
        output.deleteRecursively()
        output.mkdirs()
        val identifier = Regex("""[A-Za-z_$][\w$]*""")
        for ((module, content) in files) {
            val local = content.mapTo(mutableSetOf()) { it.name }
            val used = content.flatMapTo(sortedSetOf()) { d -> identifier.findAll(d.text).map { it.value } }
            val imports = used.filter { it !in local && owners[it] != null && owners[it] != module }
                .groupBy { owners.getValue(it) }.toSortedMap()
            val text = buildString {
                helpers.filter { helper -> Regex("""\b${helper.name}\b""").containsMatchIn(content.joinToString("") { it.text }) }
                    .forEach { append(it.text) }
                imports.forEach { (from, names) -> append("import { ${names.joinToString()} } from './$from.mjs';\n") }
                content.forEach { append(it.text) }
            }
            File(output, "$module.d.mts").writeText(text)
        }
    }

    private class Declaration(val name: String, val text: String)

    private companion object {
        val start = Regex("""^(?:export )?(?:declare )?(?:abstract )?(?:class|interface|namespace|function|const|let|enum|type) ([A-Za-z_$][\w$]*)""")

        /** Splits a generated declaration file into top-level statements; continuation lines are indented or `}`. */
        fun parse(text: String): List<Declaration> {
            val result = mutableListOf<Declaration>()
            var name: String? = null
            val current = StringBuilder()
            fun flush() {
                name?.let { result += Declaration(it, current.toString()) }
                current.clear()
            }
            text.lineSequence().forEach { line ->
                if (line.isNotEmpty() && !line[0].isWhitespace() && !line.startsWith("}")) {
                    flush()
                    name = start.find(line)?.groupValues?.get(1) ?: error("Unexpected top-level line: $line")
                }
                if (name != null) current.append(line).append('\n')
            }
            flush()
            // Merge `class X` with its `namespace X` (and interface/namespace pairs) into one unit per name.
            return result.groupBy { it.name }.map { (n, parts) -> Declaration(n, parts.joinToString("") { it.text }) }
        }

        /**
         * Names in every `export { … }` block of a Kotlin/JS module. Besides its `@JsExport` API, a module also
         * exports internal declarations to the other linked modules, but those have mangled names that never
         * match a TypeScript declaration.
         */
        fun exportedNames(module: String): List<String> =
            Regex("""(?m)^export \{\n([\s\S]*?)\n};""").findAll(module).flatMap { block ->
                Regex(""" as ([A-Za-z_$][\w$]*),""").findAll(block.groupValues[1]).map { it.groupValues[1] }
            }.toList()
    }
}
