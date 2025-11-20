plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "com.naijaayo"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}
