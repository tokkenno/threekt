plugins {
    id("threekt.multiplatform-library")
}

kotlin {
    js {
        outputModuleName = "threekt-helpers"
    }
    sourceSets {
        commonTest.dependencies {
            implementation(project(":engine:geometry"))
        }
        commonMain.dependencies {
            api(project(":engine:core"))
        }
    }
}
