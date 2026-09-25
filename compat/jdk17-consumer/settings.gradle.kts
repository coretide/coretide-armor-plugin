// A minimal consumer build. CI publishes the plugin to Maven local, then runs this build with
// the oldest supported Gradle (9.0) on JDK 17 to prove the plugin, which is built with JDK 21,
// still loads there. See the jdk17-consumer job in .github/workflows/ci.yml.
pluginManagement {
    val armorVersion = providers.gradleProperty("armorVersion").get()
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
    plugins {
        id("dev.coretide.plugin.armor") version armorVersion
    }
}

rootProject.name = "jdk17-consumer"
