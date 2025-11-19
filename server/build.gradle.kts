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

    // JavaMail for email
    implementation("javax.mail:javax.mail-api:1.6.2")
    implementation("com.sun.mail:javax.mail:1.6.2")

    implementation("io.ktor:ktor-server-core-jvm:2.3.10")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.10")
    implementation("io.ktor:ktor-server-websockets-jvm:2.3.10")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:2.3.10")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.10")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.10")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.10")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.10")

    // PostgreSQL and Exposed dependencies
    implementation("org.postgresql:postgresql:42.5.4")
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.61.0") // For Java 8+ datetime support
    implementation("com.zaxxer:HikariCP:5.0.1") // High-performance JDBC connection pool

    // Password hashing
    implementation("at.favre.lib:bcrypt:0.10.2")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.2.11")

    // Testing
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.10")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

kotlin {
    jvmToolchain(17)
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
