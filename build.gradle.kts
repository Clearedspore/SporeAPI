import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.0"
    `java-library`
    `maven-publish`
}

group = "eu.sporedev"
version = "9.8"

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

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.json:json:20250517")
    compileOnly("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("org.reflections:reflections:0.10.2")

    implementation("xyz.xenondevs.invui:invui:2.3.0")
    implementation("xyz.xenondevs.invui:invui-kotlin:2.3.0")

    implementation(platform("org.incendo:cloud-bom:2.1.0"))
    implementation(platform("org.incendo:cloud-minecraft-bom:2.0.0"))

    implementation("org.incendo:cloud-core")
    implementation("org.incendo:cloud-annotations")
    implementation("org.incendo:cloud-paper")
    implementation("org.incendo:cloud-brigadier")
    implementation("org.incendo:cloud-kotlin-coroutines-annotations")

    implementation("org.jetbrains.kotlin:kotlin-reflect:2.4.0")

    compileOnly("com.github.ben-manes.caffeine:caffeine:3.2.2")

    compileOnly("org.mongodb:mongodb-driver-sync:5.8.0")
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