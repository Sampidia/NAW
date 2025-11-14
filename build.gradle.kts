tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

plugins {
    kotlin("jvm") apply false
    kotlin("plugin.serialization") apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
}
