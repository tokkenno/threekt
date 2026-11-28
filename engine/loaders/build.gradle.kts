plugins {
    id("threekt.multiplatform-library")
}

kotlin {
    js {
        outputModuleName = "threekt-loaders"
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":engine:core"))
            api(project(":engine:geometry"))
        }
    }
}
