import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.0"
    `maven-publish`
}

group = "eu.sporedev"
version = "8.5"

repositories {
    mavenCentral()
    maven("https://repo.xenondevs.xyz/releases")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    implementation(kotlin("stdlib"))
    implementation("org.json:json:20250517")
    compileOnly("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("org.reflections:reflections:0.10.2")

    implementation("xyz.xenondevs.invui:invui:2.3.0")
    implementation("xyz.xenondevs.invui:invui-kotlin:2.3.0")

    implementation("org.incendo:cloud-core:2.0.0")
    implementation("org.incendo:cloud-annotations:2.0.0")
    implementation("org.incendo:cloud-paper:2.0.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
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
            artifactId = "sporeapi"
        }
    }
}