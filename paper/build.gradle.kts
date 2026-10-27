plugins {
    id("sporeapi.module")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")

    api(project(":SporeAPI-Shared")) {
        exclude(group = "net.kyori")
        exclude(group = "com.google.code.gson")
    }

    api("xyz.xenondevs.invui:invui:2.3.0")
    api("xyz.xenondevs.invui:invui-kotlin:2.3.0")

    api(platform("org.incendo:cloud-bom:2.1.0"))
    api(platform("org.incendo:cloud-minecraft-bom:2.0.0"))

    api("org.incendo:cloud-core")
    api("org.incendo:cloud-annotations")
    api("org.incendo:cloud-paper")
    api("org.incendo:cloud-brigadier")
    api("org.incendo:cloud-kotlin-coroutines-annotations")

    implementation("org.jetbrains.kotlin:kotlin-reflect:2.4.21")
}
