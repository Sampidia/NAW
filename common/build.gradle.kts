plugins {
    kotlin("jvm")
}

group = "com.naijaayo"
version = "1.0-SNAPSHOT"

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}
