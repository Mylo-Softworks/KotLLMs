import java.net.URI

val ktor_version: String by project

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.1.0"
    id("maven-publish")
}

group = "com.mylosoftworks"
version = "1.0"

repositories {
    mavenCentral()
    maven {
        url = URI("https://jitpack.io")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            groupId = "com.mylosoftworks"
            artifactId = "KotLLMs"
            version = "1.0"
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.Mylo-Softworks.GBNF-Kotlin:GBNF-Kotlin:a1eeb2dd1e")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0-RC")
}



tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}