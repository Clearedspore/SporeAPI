plugins {
    id("sporeapi.module")
}

// Velocity proxy module. Depending on this also gives you everything in common.
dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")

    // Velocity already ships Adventure and Gson
    api(project(":SporeAPI-Shared")) {
        exclude(group = "net.kyori")
        exclude(group = "com.google.code.gson")
    }

    api(platform("org.incendo:cloud-bom:2.1.0"))
    api(platform("org.incendo:cloud-minecraft-bom:2.0.0"))

    api("org.incendo:cloud-core")
    api("org.incendo:cloud-annotations")
    api("org.incendo:cloud-velocity")
    api("org.incendo:cloud-kotlin-coroutines-annotations")
}
