plugins {
    kotlin("js") version "1.5.0"
}

dependencies {
    implementation(project(":core"))
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport.enabled = false
            }
        }
        binaries.executable()
    }
}
