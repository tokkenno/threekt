plugins {
    kotlin("js") version "1.4.32"
}

group = "com.aitorgf"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-js"))
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
