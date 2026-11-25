package com.aitorgf.threekt.examples

import java.io.File
import kotlin.system.exitProcess

/**
 * Opens the example menu, or runs one example directly with `--example <id>`. With
 * `--screenshot <file.png>`, the example closes after two seconds and saves its last frame.
 */
fun main(args: Array<String>) {
    val options = args.toList().chunked(2).associate { it[0] to it.getOrNull(1) }
    val id = options["--example"]
    if (id == null) {
        Launcher().run()
        exitProcess(0)
    }
    val entry = Examples.find(id) ?: run {
        System.err.println("Unknown example '$id'. Available: ${Examples.all.joinToString { it.id }}")
        exitProcess(1)
    }
    ExampleWindow(entry, screenshot = options["--screenshot"]?.let(::File)).run()
}
