import java.net.URI

val ktor_version: String by project

plugins {
    kotlin("multiplatform") version "2.0.20"
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

kotlin {
    jvm()
    js {
        browser()
        nodejs()
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        commonMain {
            dependencies {
                implementation("com.github.Mylo-Softworks.GBNF-Kotlin:GBNF-Kotlin:1f0211e0cb")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0-RC")
            }
        }

        jvmMain {
            dependencies {
                implementation("io.ktor:ktor-client-cio:$ktor_version")
            }
        }

        jsMain {
            dependencies {
                implementation("io.ktor:ktor-client-js:$ktor_version")
            }
        }
    }
}