# Samples

Real builds that apply CodeArmor as a project would. Each takes the plugin from this repository through
`includeBuild`, so it always runs the code next to it, and CI builds each one on every pull request.

| Sample | What it shows |
|---|---|
| [`java-app`](java-app) | A Java application: coverage thresholds, an integration test suite, SpotBugs at full effort, and an entry point coverage leaves out |
| [`kotlin-library`](kotlin-library) | A Kotlin library held to a library's standards: Kover, strict compilation with explicit API mode, and detekt with type resolution |
| [`multi-module`](multi-module) | CodeArmor at the root of a build with a library and an application module, with the combined coverage and test reports |

Build one with this repository's wrapper:

```shell script
./gradlew -p samples/java-app build armorInfo
```

`armorInfo` shows what CodeArmor checks there and what needs attention. The samples switch the git hooks off: they
live in CodeArmor's own repository, whose hooks are not theirs to install.
