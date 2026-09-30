// A Kotlin library, held to a library's standards: every warning fails the build, and so does untested code.
plugins {
    // In the same script as CodeArmor, so detekt can see the Kotlin plugin's classes.
    kotlin("jvm") version "2.2.21"
    `java-library`
    id("dev.coretide.plugin.armor")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

codeArmor {
    coverage {
        kover = true
        minimum = 0.9
    }
    compilation {
        strict = true
    }
    detekt {
        typeResolution = true
    }
    // The sample lives in CodeArmor's own repository, whose hooks are not the sample's to install.
    gitHooks {
        enabled = false
    }
}
