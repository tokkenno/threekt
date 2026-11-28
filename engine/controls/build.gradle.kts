plugins {
    id("threekt.multiplatform-library")
}

kotlin {
    js {
        outputModuleName = "threekt-controls"
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":engine:core"))
        }
    }
}
