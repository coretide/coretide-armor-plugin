plugins {
    java
    id("dev.coretide.plugin.armor")
}

repositories {
    mavenCentral()
}

codeArmor {
    enableGitHooks = false
    enableVersionFromGit = false
}

val gradleJvm: String = System.getProperty("java.specification.version")
val gradleVersion: String = gradle.gradleVersion

tasks.register("verifyJdk17") {
    doLast {
        check(gradleJvm == "17") { "Expected Gradle to run on JDK 17, but it runs on JDK $gradleJvm" }
        println("Gradle $gradleVersion runs on JDK $gradleJvm and applied the CodeArmor plugin")
    }
}
