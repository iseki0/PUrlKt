@file:OptIn(ExperimentalWasmDsl::class, ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import java.net.URI
import java.util.*

plugins {
    kotlin("multiplatform") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    id("org.jetbrains.dokka") version "2.0.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
    `maven-publish`
    signing
}

allprojects {
    group = "space.iseki.purlkt"
    if (version == "unspecified") version = "0.0.1-SNAPSHOT"
    repositories {
        mavenCentral()
    }
}

kotlin {
    abiValidation {
        enabled = true
    }
    jvmToolchain(17)
    jvm {}
    js {
        browser()
        nodejs()
    }
    wasmJs {
        browser()
        nodejs()
        d8()
    }
    wasmWasi {
        nodejs()
    }

    if (System.getenv("CI") == "true") {
        // Tier 1
        macosX64()
        macosArm64()
        iosSimulatorArm64()
        iosX64()
        iosArm64()

        // Tier 2
        linuxX64()
        linuxArm64()
        watchosArm32()
        watchosArm64()
        watchosX64()
        watchosSimulatorArm64()
        tvosSimulatorArm64()
        tvosX64()
        tvosArm64()

        // Tier 3
        androidNativeArm32()
        androidNativeArm64()
        androidNativeX64()
        androidNativeX86()
        mingwX64()
        watchosDeviceArm64()
    }
}

dependencies {
    commonTestImplementation(kotlin("test"))
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")
    commonTestImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
}

tasks.named("jvmTest") {
    this as Test
    useJUnitPlatform()
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

publishing {
    repositories {
        maven {
            name = "Central"
            afterEvaluate {
                url = if (version.toString().endsWith("SNAPSHOT")) {
                    // uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
                    uri("https://central.sonatype.com/repository/maven-snapshots/")
                } else {
                    // uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
                    // uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                    uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
                }
            }
            credentials {
                username = properties["ossrhUsername"]?.toString() ?: System.getenv("OSSRH_USERNAME")
                password = properties["ossrhPassword"]?.toString() ?: System.getenv("OSSRH_PASSWORD")
            }
        }
        if (!System.getenv("GITHUB_TOKEN").isNullOrBlank()) {
            maven {
                name = "GitHubPackages"
                url = URI.create("https://maven.pkg.github.com/iseki0/PurlKt")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")!!
                    password = System.getenv("GITHUB_TOKEN")!!
                }
            }
        }
    }
    publications {
        withType<MavenPublication> {
            val pubName = name.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            val emptyJavadocJar by tasks.register<Jar>("emptyJavadocJar$pubName") {
                archiveClassifier = "javadoc"
                archiveBaseName = artifactId
            }
            artifact(emptyJavadocJar)
            pom {
                name = "PurlKt-${project.name}"
                val projectUrl = "https://github.com/iseki0/PurlKt"
                description = "A library for purl parsing and building, in Kotlin multiplatform"
                url = projectUrl
                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        id = "iseki0"
                        name = "iseki zero"
                        email = "iseki@iseki.space"
                    }
                }
                inceptionYear = "2025"
                scm {
                    connection = "scm:git:$projectUrl.git"
                    developerConnection = "scm:git:$projectUrl.git"
                    url = projectUrl
                }
                issueManagement {
                    system = "GitHub"
                    url = "$projectUrl/issues"
                }
                ciManagement {
                    system = "GitHub"
                    url = "$projectUrl/actions"
                }
            }
        }
    }
}

dokka {
    dokkaSourceSets.configureEach {
        includes.from(rootProject.layout.projectDirectory.file("module.md"))
        sourceLink {
            localDirectory = project.layout.projectDirectory.dir("src").asFile
            val p =
                project.layout.projectDirectory.dir("src").asFile.relativeTo(rootProject.layout.projectDirectory.asFile)
                    .toString()
                    .replace('\\', '/')
            remoteUrl = URI.create("https://github.com/iseki0/PurlKt/tree/master/$p")
            remoteLineSuffix = "#L"
        }
        externalDocumentationLinks.create("") {
            url = URI.create("https://kotlinlang.org/api/kotlinx.serialization/")
        }
    }
}
