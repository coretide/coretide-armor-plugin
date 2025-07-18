plugins {
    kotlin("jvm") version "1.9.25"
    `java-gradle-plugin`
    `maven-publish`
    signing
    id("org.jreleaser") version "1.18.0"
    id("com.gradle.plugin-publish") version "1.3.1"
}

group = "dev.coretide.plugin"

fun extractVersionFromGitTag(): String {
    val tagRef = System.getenv("CI_COMMIT_TAG")
    if (!tagRef.isNullOrBlank()) {
        return tagRef.trim()
    }
    val githubRef = System.getenv("GITHUB_REF")
    if (githubRef?.startsWith("refs/tags/") == true) {
        return githubRef.replace("refs/tags/", "").trim()
    }
    val gitDir = File(".git")
    if (!gitDir.exists()) {
        println("Warning: Not in a git repository (.git directory not found)")
        return "0.1.0-alpha"
    }
    return try {
        val exactTagProcess =
            ProcessBuilder("git", "describe", "--tags", "--exact-match", "HEAD")
                .directory(projectDir)
                .start()
        exactTagProcess.waitFor()
        if (exactTagProcess.exitValue() == 0) {
            val gitTag =
                exactTagProcess.inputStream
                    .bufferedReader()
                    .readText()
                    .trim()
            return gitTag
        }
        val latestTagProcess =
            ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
                .directory(projectDir)
                .start()
        latestTagProcess.waitFor()
        if (latestTagProcess.exitValue() == 0) {
            val gitTag =
                latestTagProcess.inputStream
                    .bufferedReader()
                    .readText()
                    .trim()
            "$gitTag-SNAPSHOT"
        } else {
            "0.1.0-alpha"
        }
    } catch (e: Exception) {
        println("Warning: Could not extract version from Git tag: ${e.message}")
        "0.1.0-alpha"
    }
}

version = extractVersionFromGitTag()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.2.2")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.1.0")
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:6.2.0.5505")
    implementation("org.owasp:dependency-check-gradle:12.1.3")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.1.1")
    testImplementation(kotlin("test"))
}

gradlePlugin {
    website = "https://github.com/coretide/coretide-armor-plugin"
    vcsUrl = "https://github.com/coretide/coretide-armor-plugin"

    plugins {
        create("codeArmor") {
            id = "dev.coretide.armor"
            implementationClass = "dev.coretide.armor.CodeArmorPlugin"
            displayName = "CodeArmor Plugin"
            description = "Comprehensive code quality and security plugin for Java/Kotlin projects"
            tags = listOf("code-quality", "security", "formatting", "kotlin", "java")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "code-armor-plugin"
            from(components["java"])

            pom {
                name = "CodeArmor Plugin"
                description = "Comprehensive code quality and security plugin for Java/Kotlin projects"
                url = "https://github.com/coretide/coretide-armor-plugin"

                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }

                developers {
                    developer {
                        id = "coretide"
                        name = "Kushal Patel"
                        email = "contact@coretide.dev"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/coretide/coretide-armor-plugin.git"
                    developerConnection = "scm:git:ssh://github.com/coretide/coretide-armor-plugin.git"
                    url = "https://github.com/coretide/coretide-armor-plugin"
                }
            }
        }

        withType<MavenPublication> {
            pom {
                description = "Comprehensive code quality and security plugin for Java/Kotlin projects"
                url = "https://github.com/coretide/coretide-armor-plugin"

                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }

                developers {
                    developer {
                        id = "coretide"
                        name = "Kushal Patel"
                        email = "contact@coretide.dev"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/coretide/coretide-armor-plugin.git"
                    developerConnection = "scm:git:ssh://github.com/coretide/coretide-armor-plugin.git"
                    url = "https://github.com/coretide/coretide-armor-plugin"
                }
            }
        }
    }

    repositories {
        maven {
            name = "staging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

signing {
    isRequired =
        gradle.taskGraph.hasTask("jreleaserDeploy") ||
        gradle.taskGraph.hasTask("publishToSonatype") ||
        gradle.taskGraph.hasTask("publishPlugins")
    if (System.getenv("CI") == "true") {
        val signingKey = System.getenv("GPG_PRIVATE_KEY") ?: System.getenv("JRELEASER_GPG_SECRET_KEY")
        val signingPassword = System.getenv("GPG_PASSPHRASE") ?: System.getenv("JRELEASER_GPG_PASSPHRASE")
        if (signingKey != null && signingPassword != null) {
            useInMemoryPgpKeys(signingKey, signingPassword)
        } else {
            useGpgCmd()
        }
    } else {
        useGpgCmd()
    }
    sign(publishing.publications)
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    val signingTasks = tasks.withType<Sign>()
    mustRunAfter(signingTasks)
}

tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.withType<Sign>())
}

tasks.named("publishPlugins") {
    dependsOn(tasks.withType<Sign>())
}

tasks.named<Jar>("jar") {
    archiveFileName.set("code-armor-plugin-$version.jar")
}

jreleaser {
    project {
        name.set("code-armor-plugin")
        description.set("Comprehensive code quality and security plugin for Java/Kotlin projects")
        authors.set(listOf("Kushal Patel"))
        license.set("Apache-2.0")
        copyright.set("2025 Kushal Patel")

        links {
            homepage.set("https://github.com/coretide/coretide-armor-plugin")
            bugTracker.set("https://github.com/coretide/coretide-armor-plugin/issues")
        }

        inceptionYear.set("2025")
        vendor.set("Kushal Patel")
    }

    signing {
        active.set(org.jreleaser.model.Active.RELEASE)
        armored.set(true)
    }

    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(org.jreleaser.model.Active.RELEASE)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    connectTimeout.set(60)
                    readTimeout.set(300)
                    retryDelay.set(10)
                    stagingRepositories.add("build/staging-deploy")
                    maxRetries.set(3)
                }
            }
        }
    }

    release {
        github {
            enabled.set(false)
            name.set("coretide-armor-plugin")
            repoUrl.set("https://github.com/coretide/coretide-armor-plugin")
            tagName.set("{{projectVersion}}")
            releaseName.set("{{projectVersion}}")
        }
    }
}

tasks.register("createStagingDir") {
    doLast {
        layout.buildDirectory
            .dir("staging-deploy")
            .get()
            .asFile
            .mkdirs()
    }
}

tasks.named("jreleaserDeploy") {
    dependsOn("createStagingDir")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

tasks.configureEach {
    when {
        name.contains("dependencyCheck") -> {
            notCompatibleWithConfigurationCache("OWASP dependency check uses runtime project access")
        }

        name.contains("sonar") -> {
            notCompatibleWithConfigurationCache("SonarQube uses runtime project access")
        }

        name.contains("veracode") -> {
            notCompatibleWithConfigurationCache("Veracode uses runtime project access")
        }
    }
}

gradle.taskGraph.whenReady {
    val hasConfigurationCacheIndicators =
        System.getProperty("org.gradle.configuration-cache") != null ||
            System.getProperty("org.gradle.unsafe.configuration-cache") != null ||
            System.getenv("GRADLE_OPTS")?.contains("configuration-cache") == true

    if (hasConfigurationCacheIndicators) {
        println("🔧 Configuration cache detected - third-party plugins may show compatibility warnings")
        println("💡 These warnings are from external plugins and don't affect your code quality")
    }
}
