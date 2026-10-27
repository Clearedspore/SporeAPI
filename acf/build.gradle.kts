// ACF (Aikar's Command Framework) support for older projects. Includes paper (and so common).
// Your main class extends SporeAcfPlugin instead of SporePlugin.
dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")

    api(project(":SporeAPI-Paper"))

    api("co.aikar:acf-paper:0.5.1-SNAPSHOT")
}
