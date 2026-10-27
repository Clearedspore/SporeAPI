import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
}

group = "eu.sporedev"
version = "11.0.3"

val jvm = (findProperty("sporeJvm") as String?)?.toInt() ?: 25

repositories {
    mavenCentral()
    maven("https://repo.xenondevs.xyz/releases")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvm))
    }
    withSourcesJar()
}

kotlin {
    jvmToolchain(jvm)
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(jvm.toString()))
    }
}

publishing {
    repositories {
        maven {
            name = "sporeRepository"

            url = uri(
                if (version.toString().endsWith("-SNAPSHOT")) {
                    "https://repo.sporedev.eu/snapshots"
                } else {
                    "https://repo.sporedev.eu/releases"
                }
            )

            credentials {
                username = System.getenv("MAVEN_NAME")
                password = System.getenv("MAVEN_SECRET")
            }

            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "sporeapi-${project.projectDir.name}"
        }
    }
}
