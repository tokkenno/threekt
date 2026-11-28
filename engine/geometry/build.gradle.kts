plugins {
    id("threekt.multiplatform-library")
}

kotlin {
    js {
        outputModuleName = "threekt-geometry"
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":engine:core"))
        }
    }
}
