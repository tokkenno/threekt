plugins {
    id("threekt.multiplatform-library")
}

kotlin {
    js {
        outputModuleName = "threekt-core"
    }
    sourceSets {
        commonTest.dependencies {
            implementation(project(":engine:geometry"))
        }
    }
}
