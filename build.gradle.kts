plugins {
    // Puts the convention plugin, and with it the Kotlin Gradle plugin, on the root build classpath once, so that
    // every subproject loads the same Kotlin plugin classes.
    id("threekt.multiplatform-library") apply false
}
