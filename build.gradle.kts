import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

plugins {
    kotlin("jvm") version "1.9.25"
    `java-gradle-plugin`
    `maven-publish`
    signing
    id("org.jreleaser") version "1.18.0"
    id("com.gradle.plugin-publish") version "1.3.1"
}

group = "dev.coretide.plugin"

abstract class BuildscriptGitValueSource : ValueSource<String, BuildscriptGitValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val projectDir: Property<String>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val projectDirPath = parameters.projectDir.get()
        val projectDir = File(projectDirPath)

        val tagRef = System.getenv("CI_COMMIT_TAG")
        if (tagRef?.startsWith("v") == true) {
            return tagRef.replace("v", "")
        }

        val githubRef = System.getenv("GITHUB_REF")
        if (githubRef?.startsWith("refs/tags/v") == true) {
            return githubRef.replace("refs/tags/v", "")
        }

        val gitDir = File(projectDir, ".git")
        if (!gitDir.exists()) {
            return "0.1.0-SNAPSHOT"
        }

        return try {
            val exactTagOutput = ByteArrayOutputStream()
            val exactTagResult: ExecResult =
                execOperations.exec {
                    workingDir = projectDir
                    commandLine("git", "describe", "--tags", "--exact-match", "HEAD")
                    standardOutput = exactTagOutput
                    isIgnoreExitValue = true
                }
            if (exactTagResult.exitValue == 0) {
                val tag = exactTagOutput.toString().trim()
                return if (tag.startsWith("v")) tag.replace("v", "") else tag
            }
            val latestOutput = ByteArrayOutputStream()
            val latestTagResult: ExecResult =
                execOperations.exec {
                    workingDir = projectDir
                    commandLine("git", "describe", "--tags", "--abbrev=0")
                    standardOutput = latestOutput
                    isIgnoreExitValue = true
                }
            if (latestTagResult.exitValue == 0) {
                val tag = latestOutput.toString().trim()
                val version = if (tag.startsWith("v")) tag.replace("v", "") else tag
                "$version-SNAPSHOT"
            } else {
                "0.1.0-SNAPSHOT"
            }
        } catch (e: Exception) {
            println("   ❌ Git command failed: ${e.message}")
            "0.1.0-SNAPSHOT"
        }
    }
}

val gitVersionProvider: Provider<String> =
    providers.of(BuildscriptGitValueSource::class.java) {
        parameters.projectDir.set(project.projectDir.absolutePath)
    }

version = gitVersionProvider.get()

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
            id = "dev.coretide.plugin.armor"
            implementationClass = "dev.coretide.plugin.armor.CodeArmorPlugin"
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
    archiveFileName.set("code-armor-plugin-${gitVersionProvider.get()}.jar")
}

tasks.register("showVersion") {
    doLast {
        println("Current version: ${gitVersionProvider.get()}")
    }
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
