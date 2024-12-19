import java.net.URI

plugins {
    kotlin("jvm")
}

group = "com.mylosoftworks"
version = "1.0"

repositories {
    mavenCentral()
    maven {
        url = URI("https://jitpack.io")
    }
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    implementation(project(":"))

    implementation("com.github.Mylo-Softworks.GBNF-Kotlin:GBNF-Kotlin:a1eeb2dd1e")

    implementation(kotlin("reflect"))
}

tasks.test {
    useJUnitPlatform()
}