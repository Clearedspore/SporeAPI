pluginManagement {
    // The shared module setup lives in build-logic (plugin id "sporeapi.module")
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "SporeAPI"

include("common", "paper", "extra", "acf", "velocity")
project(":paper").name = "SporeAPI-Paper"
project(":common").name = "SporeAPI-Shared"
project(":extra").name = "SporeAPI-extra"
project(":acf").name = "SporeAPI-ACF"
project(":velocity").name = "SporeAPI-Velocity"
