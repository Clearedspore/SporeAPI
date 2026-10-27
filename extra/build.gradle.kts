dependencies {
    api(project(":SporeAPI-Shared")) {
        exclude(group = "net.kyori")
        exclude(group = "com.google.code.gson")
    }

    api("org.mongodb:mongodb-driver-sync:5.8.0")

    compileOnly("com.google.code.gson:gson:2.14.0")

    api("redis.clients:jedis:7.2.0") {
        exclude(group = "com.google.code.gson")
    }
}
