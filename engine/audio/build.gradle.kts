plugins {
    id("threekt.multiplatform-library")
}

kotlin {
    js {
        outputModuleName = "threekt-audio"
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":engine:core"))
        }
    }
}
