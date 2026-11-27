plugins {
    id("threekt.multiplatform-library")
}

kotlin {
    js {
        outputModuleName = "threekt-core"
    }
    sourceSets {
        // The OpenGL ES renderer still lives in this module until it moves to `engine/render/gl`.
        jvmMain.dependencies {
            implementation(libs.lwjgl)
            implementation(libs.lwjgl.opengles)
            implementation(libs.lwjgl.egl)
        }
    }
}
