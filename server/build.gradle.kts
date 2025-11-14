import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    application
    id("com.github.johnrengelman.shadow")
}

group = "com.naijaayo"
version = "0.0.1"

dependencies {
    // Common module dependency
    implementation(project(":common"))

    implementation("io.ktor:ktor-server-core-jvm:2.3.7")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.7")
    implementation("io.ktor:ktor-server-websockets-jvm:2.3.7")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.7")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.7")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.7")

    // MongoDB dependencies
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:4.11.0")
    implementation("org.mongodb:mongodb-driver-core:4.11.0")
    implementation("org.mongodb:bson:4.11.0")

    // Password hashing
    implementation("at.favre.lib:bcrypt:0.10.2")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Testing
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.7")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.21")
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("com.naijaayo.worldwide.ApplicationKt")
}

tasks.shadowJar {
    archiveBaseName.set("naija-ayo-server")
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "com.naijaayo.worldwide.ApplicationKt"
    }
}
