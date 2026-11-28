plugins {
    id("threekt.multiplatform-library")
}

kotlin {
    js {
        outputModuleName = "threekt-exporters"
    }
    sourceSets {
        commonTest.dependencies {
            implementation(project(":engine:geometry"))
            implementation(project(":engine:loaders"))
        }
        commonMain.dependencies {
            api(project(":engine:core"))
        }
    }
}
