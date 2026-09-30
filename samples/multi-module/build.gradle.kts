// CodeArmor at the root configures every module, and adds the combined coverage and test reports.
plugins {
    id("dev.coretide.plugin.armor")
}

codeArmor {
    coverage {
        minimum = 0.7
    }
    // The sample lives in CodeArmor's own repository, whose hooks are not the sample's to install.
    gitHooks {
        enabled = false
    }
}
