plugins {
    kotlin("jvm") version "1.6.0"
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")
    apply(plugin = "com.github.johnrengelman.shadow")

    group = "me.hwiggy.kommander"
    version = "1.7.2"

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://nexus.mcdevs.us/repository/mcdevs")
    }

    dependencies {
        compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")
        testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }

    publishing {
        repositories {
            mavenLocal()
            when (project.findProperty("deploy") ?: "local") {
                "local" -> return@repositories
                "remote" -> maven {
                    if (project.version.toString().endsWith("-SNAPSHOT")) {
                        setUrl("https://nexus.mcdevs.us/repository/mcdevs-snapshots/")
                        mavenContent { snapshotsOnly() }
                    } else {
                        setUrl("https://nexus.mcdevs.us/repository/mcdevs-releases/")
                        mavenContent { releasesOnly() }
                    }
                    credentials {
                        username = System.getenv("NEXUS_USERNAME")
                        password = System.getenv("NEXUS_PASSWORD")
                    }
                }
            }
        }
        publications {
            create<MavenPublication>("assembly") {
                from(components["java"])
            }
        }
    }

    tasks.compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    tasks.test { useJUnitPlatform() }
}

tasks.shadowJar { enabled = false }