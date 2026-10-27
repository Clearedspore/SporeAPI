import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version "2.4.0" apply false
}

allprojects {
    group = "eu.sporedev"
    version = "11.0.0"
}

subprojects {
    val jvm = (findProperty("sporeJvm") as String?)?.toInt() ?: 25

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
        maven("https://repo.xenondevs.xyz/releases")
        maven("https://repo.aikar.co/content/groups/aikar/")
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc-repo"
        }
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(jvm))
        }
        withSourcesJar()
    }

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(jvm)
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(jvm.toString()))
        }
    }

    extensions.configure<PublishingExtension> {
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
}
