plugins {
    id("sporeapi.module")
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    api("org.reflections:reflections:0.10.2")
    api("com.google.code.gson:gson:2.14.0")

    api("net.kyori:adventure-api:5.2.0")
    api("net.kyori:adventure-text-minimessage:5.2.0")
    api("net.kyori:adventure-text-serializer-legacy:5.2.0")

    compileOnly(platform("org.incendo:cloud-bom:2.1.0"))
    compileOnly(platform("org.incendo:cloud-minecraft-bom:2.0.0"))
    compileOnly("org.incendo:cloud-core")
    compileOnly("org.incendo:cloud-annotations")
    compileOnly("org.incendo:cloud-minecraft-extras")

    implementation("com.github.ben-manes.caffeine:caffeine:3.3.1")
}
