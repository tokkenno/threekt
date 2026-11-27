plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("multiplatformLibrary") {
            id = "threekt.multiplatform-library"
            implementationClass = "MultiplatformLibraryConventionPlugin"
        }
    }
}
