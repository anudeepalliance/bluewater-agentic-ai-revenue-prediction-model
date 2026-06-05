plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    application
}

group = "com.bluewater"
version = "1.0.0"

application {
    mainClass.set("com.bluewater.revenuepredictor.AppKt")
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.4.0")
    implementation("io.ktor:ktor-server-netty-jvm:3.4.0")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.4.0")
    implementation("io.ktor:ktor-server-call-logging-jvm:3.4.0")
    implementation("io.ktor:ktor-server-cors-jvm:3.4.0")
    implementation("io.ktor:ktor-server-default-headers-jvm:3.4.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.4.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")

    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.6.4")
    implementation("org.mongodb:bson-kotlinx:5.6.4")

    implementation("ai.koog:koog-agents:0.8.0")

    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
