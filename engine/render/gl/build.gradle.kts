plugins {
    id("threekt.multiplatform-library")
}

kotlin {
    js {
        outputModuleName = "threekt-render-gl"
    }
    sourceSets {
        commonTest.dependencies {
            implementation(project(":engine:geometry"))
        }
        commonMain.dependencies {
            api(project(":engine:core"))
        }
        jvmMain.dependencies {
            implementation(libs.lwjgl)
            implementation(libs.lwjgl.opengles)
            implementation(libs.lwjgl.egl)
        }
    }
}
