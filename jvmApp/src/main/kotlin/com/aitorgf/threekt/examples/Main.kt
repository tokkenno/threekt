package com.aitorgf.threekt.examples

import java.io.File
import java.nio.file.Path
import kotlin.system.exitProcess

/** Opens the example menu, or runs an example in a window or an off-screen EGL context. */
fun main(args: Array<String>) {
    val flags = setOf("--headless", "--software")
    val parameters = setOf("--example", "--screenshot", "--width", "--height", "--time", "--angle-dir")
    val switches = mutableSetOf<String>()
    val options = mutableMapOf<String, String>()
    var index = 0
    while (index < args.size) {
        val option = args[index++]
        when (option) {
            in flags -> switches.add(option)
            in parameters -> {
                val value = args.getOrNull(index++)
                require(value != null && !value.startsWith("--")) { "$option requires a value." }
                options[option] = value
            }
            else -> error("Unknown option '$option'.")
        }
    }

    val headless = "--headless" in switches
    require(headless || ("--software" !in switches && options.keys.none { it in setOf("--width", "--height", "--time", "--angle-dir") })) {
        "Use --headless with --software, --width, --height, --time or --angle-dir."
    }
    val id = options["--example"]
    if (id == null) {
        require(!headless) { "--headless requires --example <id> and --screenshot <file.png>." }
        Launcher().run()
        exitProcess(0)
    }
    val entry = Examples.find(id) ?: run {
        System.err.println("Unknown example '$id'. Available: ${Examples.all.joinToString { it.id }}")
        exitProcess(1)
    }
    val screenshot = options["--screenshot"]?.let(::File)
    if (headless) {
        requireNotNull(screenshot) { "--headless requires --screenshot <file.png>." }
        val width = options["--width"]?.toInt() ?: 960
        val height = options["--height"]?.toInt() ?: 640
        val seconds = options["--time"]?.toDouble() ?: 0.0
        require(width > 0 && height > 0 && seconds.isFinite() && seconds >= 0) { "Invalid dimensions or time." }
        HeadlessExample(
            entry, width, height, seconds, "--software" in switches, options["--angle-dir"]?.let { Path.of(it) },
        ).save(screenshot)
    } else {
        ExampleWindow(entry, screenshot = screenshot).run()
    }
}
