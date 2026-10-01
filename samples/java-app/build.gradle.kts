// A Java application with unit and integration tests, as a service team might set it up.
plugins {
    application
    id("dev.coretide.plugin.armor")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "com.example.orders.OrdersApplication"
}

codeArmor {
    coverage {
        minimum = 0.8
        classMinimum = 0.6
    }
    tests {
        integrationTests = true
    }
    spotbugs {
        effort = "MAX"
    }
    // The sample lives in CodeArmor's own repository, whose hooks are not the sample's to install.
    gitHooks {
        enabled = false
    }
}
