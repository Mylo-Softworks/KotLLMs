import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
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
//        create<MavenPublication>("maven") {
//            from(components["kotlin"])
//            groupId = "com.mylosoftworks"
//            artifactId = "KotLLMs"
//            version = "1.0"
//        }
    }
}

kotlin {
    jvm()
    js {
        browser()
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        commonMain {
            dependencies {
                implementation("com.github.Mylo-Softworks.GBNF-Kotlin:GBNF-Kotlin:8c5eb3c4f7")
                implementation("com.github.Mylo-Softworks.Klex:Klex:3ed101743f") // Klex is used for partial json parsing

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0-RC")
            }
        }

        jvmMain {
            dependencies {
//                implementation("io.ktor:ktor-client-cio:$ktor_version") // CIO sse is broken
//                implementation("io.ktor:ktor-client-okhttp:$ktor_version") // OKHttp supports http 2, websockets, and works on android. Currently does not allow for successive streams
//                implementation("io.ktor:ktor-client-apache:$ktor_version") // Apache supports http 2 (for apache 5), doesn't support websockets or android. Successfully handles successive streams

                // Incompatible with websocket streaming, if an api requires it, add.
                implementation("io.ktor:ktor-client-android:$ktor_version") // Android does not support http 2 or websockets, but supports android.
            }
        }

        jsMain {
            dependencies {
                implementation("io.ktor:ktor-client-js:$ktor_version") // The JS client is recommended for javascript runtimes, and supports http 2 and websockets
            }
        }
    }
}