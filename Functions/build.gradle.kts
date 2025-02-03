import java.net.URI

plugins {
    kotlin("multiplatform") version "2.0.20"
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
//            artifactId = "KotLLMs-functions"
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

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation(project(":"))
                implementation("com.github.Mylo-Softworks.GBNF-Kotlin:GBNF-Kotlin:8c5eb3c4f7")
                implementation(kotlin("reflect"))
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}