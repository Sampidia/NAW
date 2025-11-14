plugins {
    kotlin("plugin.serialization")
}

group = "com.naijaayo.worldwide"
version = "1.0-SNAPSHOT"

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
