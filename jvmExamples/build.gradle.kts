plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

kotlin {
    jvmToolchain(21)
}

val lwjglNatives = run {
    val os = System.getProperty("os.name").lowercase()
    val arm = System.getProperty("os.arch").let { it == "aarch64" || it.startsWith("arm") }
    when {
        os.contains("win") -> if (arm) "natives-windows-arm64" else "natives-windows"
        os.contains("mac") -> if (arm) "natives-macos-arm64" else "natives-macos"
        else -> if (arm) "natives-linux-arm64" else "natives-linux"
    }
}

dependencies {
    implementation(project(":engine:render:gl"))
    implementation(project(":engine:controls"))
    implementation(project(":engine:geometry"))
    implementation(project(":engine:helpers"))
    implementation(project(":engine:loaders"))
    implementation(libs.lwjgl)
    implementation(libs.lwjgl.egl)
    implementation(libs.lwjgl.glfw)
    implementation(libs.lwjgl.opengles)
    runtimeOnly(variantOf(libs.lwjgl) { classifier(lwjglNatives) })
    runtimeOnly(variantOf(libs.lwjgl.glfw) { classifier(lwjglNatives) })
    runtimeOnly(variantOf(libs.lwjgl.opengles) { classifier(lwjglNatives) })
}

application {
    mainClass = "com.aitorgf.threekt.examples.MainKt"
    // GLFW must run on the first thread on macOS.
    if (System.getProperty("os.name").lowercase().contains("mac")) applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
}

// Use the same upstream assets as the browser example and package them in the application.
tasks.processResources {
    from(rootProject.layout.projectDirectory.dir("webExamples/src/examples/assets/male02")) {
        into("models/male02")
    }
    from(rootProject.layout.projectDirectory.file("webExamples/src/examples/assets/square-outline-textured.png")) {
        into("textures")
    }
}
