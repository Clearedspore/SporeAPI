plugins {
    id("sporeapi.module")
}

dependencies {
    api(project(":SporeAPI-Shared")) {
        exclude(group = "net.kyori")
        exclude(group = "com.google.code.gson")
    }

    api("org.mongodb:mongodb-driver-sync:5.13.0")

    compileOnly("com.google.code.gson:gson:2.15.0")

    api("redis.clients:jedis:8.0.2") {
        exclude(group = "com.google.code.gson")
    }
}
