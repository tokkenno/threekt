rootProject.name = "threekt"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":engine:core")
include(":engine:controls")
include(":engine:exporters")
include(":engine:geometry")
include(":engine:helpers")
include(":engine:loaders")
include(":engine:audio")
include(":engine:render:gl")
include(":engine:dist:npm")
include(":jvmExamples")