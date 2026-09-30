# CodeArmor settings

Every setting of the `codeArmor { }` block, by block. Each is a Gradle property, so it takes a value or a provider:

```kotlin
codeArmor {
    coverage { minimum = 0.8 }
    owasp { nvdApiKey = providers.environmentVariable("NVD_API_KEY") }
}
```

A list takes `add("…")` to keep its defaults, or `= listOf(…)` to replace them. A setting changed after CodeArmor has
read it, as from an `afterEvaluate` block, fails the build. The flat settings of 0.4.0, such as `coverageMinimum`,
still work until 1.0.0; the [changelog](../CHANGELOG.md#upgrading-from-04x) maps each one to its block.

`./gradlew armorInfo` shows what these add up to in a project. A test checks this page against the plugin: every
setting is listed, with its default.

## Top level

| Setting | Type | Default | What it does |
|---|---|---|---|
| `projectType` | `ProjectType` | unset | The kind of project, such as `ProjectType.KOTLIN_LIBRARY`. Unset, CodeArmor detects it from the plugins and the sources. |
| `logLevel` | `ArmorLogLevel` | `ESSENTIAL` | `VERBOSE`, `ESSENTIAL` or `STEALTH`. |
| `secretScan` | Boolean | `false` | gitleaks: a pre-commit hook that blocks a commit adding a secret, and `armorSecretScan` over the whole history in `fullAnalysis`. |
| `versionFromGit` | Boolean | `true` | Sets the project version from the latest git tag, where the build sets none. |
| `resourceProcessing` | Boolean | `true` | In applications, replaces `@version@`-style tokens in `src/main/resources`. |

## `coverage`

| Setting | Type | Default | What it does |
|---|---|---|---|
| `enabled` | Boolean | `true` | Measures coverage, with JaCoCo or Kover, and verifies it in the build tier. |
| `kover` | Boolean | `false` | Kover instead of JaCoCo in Kotlin projects. Java projects keep JaCoCo. |
| `minimum` | Double | `0.3` | The share of instructions (lines, with Kover) tests must cover, from 0 to 1. |
| `classMinimum` | Double | `0.25` | The share of lines tests must cover in each class. |
| `inclusions` | List | empty | Class patterns to measure; empty measures every class. |
| `exclusions` | List | empty | Class patterns to leave out, besides the defaults, matched anywhere in a class name or as a package. |
| `defaultExclusions` | Boolean | `true` | Leaves out entry points, configuration and generated code, as `logExclusionInfo` lists them. |

## `diffCoverage`

| Setting | Type | Default | What it does |
|---|---|---|---|
| `enabled` | Boolean | `true` | `armorDiffCoverage`, in the build tier: the coverage of the lines changed since the base branch. |
| `minimum` | Double | unset | The share of changed lines tests must cover, or the build fails. Unset, it only reports. |
| `base` | String | unset | The branch to compare with. Unset, the pull request's target on common CI services, then `origin/HEAD`, `main` or `master`. |

## `tests`

| Setting | Type | Default | What it does |
|---|---|---|---|
| `junitPlatform` | Boolean | `true` | Moves test tasks still on Gradle's default JUnit 4 runner to the JUnit Platform. |
| `flakyRetries` | Int | `2` | On CI, re-runs a failed test up to this many times, and lists it as flaky if it then passes. 0 turns retries off. |
| `slowThresholdMillis` | Long | `2000` | Lists the tests that take at least this long. 0 turns the list off. |
| `integrationTests` | Boolean | `false` | An `integrationTest` suite in `src/integrationTest`, run in the build tier and counted in coverage. |
| `architectureTests` | Boolean | `false` | ArchUnit on the test classpath, and `armorScaffoldArchitectureTests` to write a first test. |

## `mutationTesting`

| Setting | Type | Default | What it does |
|---|---|---|---|
| `enabled` | Boolean | `false` | PIT mutation testing, run on demand with `./gradlew pitest`. |
| `threshold` | Int | `0` | The mutation score, in percent, `pitest` fails below. 0 only reports. |

## `compilation`

| Setting | Type | Default | What it does |
|---|---|---|---|
| `strict` | Boolean | `false` | Compiler warnings in production code fail the build; explicit API mode for Kotlin libraries. |
| `errorProne` | Boolean | `false` | Error Prone on the Java sources. Needs a JDK 21 compiler. |
| `nullAway` | Boolean | `false` | NullAway, which fails the build where Java code may dereference null. Turns on Error Prone. |

## `spotbugs`

| Setting | Type | Default | What it does |
|---|---|---|---|
| `enabled` | Boolean | `true` | SpotBugs on the main sources; `spotbugsMain` is in the build tier. |
| `toolVersion` | String | `4.10.3` | The SpotBugs version. |
| `effort` | String | `MAX` | `MIN`, `LESS`, `DEFAULT`, `MORE` or `MAX`. |
| `reportLevel` | String | `HIGH` | The lowest confidence reported: `LOW`, `MEDIUM`, `DEFAULT` or `HIGH`. |
| `ignoreFailures` | Boolean | `false` | Reports findings without failing the build. |
| `showStackTraces` | Boolean | `true` | |
| `showProgress` | Boolean | `true` | |
| `excludeFile` | String | unset | An exclude filter of the build's own. Unset, CodeArmor's default filter. |
| `baselineFile` | String | `config/spotbugs/baseline.xml` | The findings `spotbugsMain` accepts, written by `armorSpotbugsBaseline`. Used when it exists. |
| `includeFile` | String | unset | An include filter: only what it matches is reported. |
| `xmlReports` | Boolean | `true` | |
| `htmlReports` | Boolean | `true` | |
| `textReports` | Boolean | `false` | |
| `sarifReports` | Boolean | `true` | For GitHub code scanning; see `armorSarifReport`. |
| `maxHeap` | String | unset | The SpotBugs JVM's maximum heap, such as `1g`. |
| `timeout` | Int | unset | Milliseconds a SpotBugs task may run before it fails. |
| `bugCategories` | List | empty | Only these detectors run, when set. |
| `extraArgs` | List | empty | More SpotBugs arguments. |

## `detekt`

| Setting | Type | Default | What it does |
|---|---|---|---|
| `enabled` | Boolean | `true` | detekt 2.0 (an alpha, the only release that reads current Kotlin), in projects that apply the Kotlin JVM plugin. |
| `typeResolution` | Boolean | `false` | `detektMain`, with type resolution, instead of `detekt`. Slower; more rules run. |

## `owasp`

| Setting | Type | Default | What it does |
|---|---|---|---|
| `enabled` | Boolean | `true` | OWASP Dependency-Check on `runtimeClasspath`, in the CI tier. |
| `failBuildOnCvss` | Double | `9.0` | The CVSS score at or above which a finding fails the build. |
| `suppressionFile` | String | unset | A suppression file of the build's own. CodeArmor suppresses nothing itself. |
| `autoUpdate` | Boolean | `true` | Downloads the NVD data when there is none, and refreshes it after `nvdValidForHours`. |
| `nvdApiKey` | String | unset | The NVD API key. Unset, the `nvd.api.key` Gradle property, then `NVD_API_KEY`. |
| `nvdApiDelay` | Int | `4000` | Milliseconds between requests to the NVD API. |
| `nvdMaxRetryCount` | Int | `10` | How often a failed NVD request is retried. |
| `nvdValidForHours` | Int | `24` | How many hours the NVD data counts as current. |

## `dependencyHealth`

| Setting | Type | Default | What it does |
|---|---|---|---|
| `updates` | Boolean | `true` | `dependencyUpdates`, in the CI tier: dependencies with a newer release. |
| `sbom` | Boolean | `true` | A CycloneDX SBOM, and `armorLicenseReport`, in the CI tier. |
| `forbiddenLicenses` | List | empty | Licences, as SPDX ids or names, that fail `armorLicenseReport`. |
| `analysis` | Boolean | `false` | The dependency-analysis plugin's `projectHealth`: unused, undeclared and misplaced dependencies. |

## `libraryApi`

| Setting | Type | Default | What it does |
|---|---|---|---|
| `baseline` | String | unset | A released version, `1.4.0` or `group:name:1.4.0`, that `armorApiCheck` checks binary compatibility against. |
| `kotlinAbiValidation` | Boolean | `false` | The Kotlin Gradle plugin's ABI validation for Kotlin libraries, in the build tier. |

## `sonarqube`

| Setting | Type | Default | What it does |
|---|---|---|---|
| `enabled` | Boolean | `true` | `sonar`, in the CI tier once a server or token is configured. |
| `hostUrl` | String | unset | The server; `SONAR_HOST_URL` takes precedence. Unset, SonarQube Cloud. |
| `projectKey` | String | unset | Unset, `group:name`. |
| `projectName` | String | unset | Unset, the project's name. |
| `token` | String | unset | Unset, the SonarScanner reads `SONAR_TOKEN`. |
| `qualityGateWait` | Boolean | `false` | Waits for the quality gate, and fails when it does. |
| `javaVersion` | String | unset | Unset, the project's toolchain or compatibility settings. |

## `gitHooks`

| Setting | Type | Default | What it does |
|---|---|---|---|
| `enabled` | Boolean | `true` | Registers `armorInstallGitHooks` and `armorUninstallGitHooks`. Nothing is installed until they run. |
| `prePush` | Boolean | `true` | A pre-push hook that runs `checks.prePush` and blocks a push when it fails. |
| `conventionalCommits` | Boolean | `false` | A commit-msg hook that rejects messages that are not Conventional Commits. |
| `conventionalCommitTypes` | List | `feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert` | The types a commit message may start with. |

## `checks`

| Setting | Type | Default | What it does |
|---|---|---|---|
| `prePush` | List | `quickBuild` | The tasks the pre-push hook runs. |
| `build` | List | the tools switched on | The tasks `codeQuality`, and so `build`, runs: SpotBugs, detekt, coverage, diff coverage and more. |
| `ci` | List | the tools switched on | The tasks `fullAnalysis` adds: OWASP, dependency updates, the licence report, and SonarQube once configured. |

## `codeStats`

| Setting | Type | Default | What it does |
|---|---|---|---|
| `enabled` | Boolean | `false` | Reports commit activity to Code::Stats from git hooks. See the README's Code Stats section. |
| `scope` | `CodeStatsScope` | `REPO` | `REPO`, or `GLOBAL`, which only a developer's own settings can choose. |

## `toolVersions`

| Setting | Type | Default | What it does |
|---|---|---|---|
| `jacoco` | String | `0.8.15` | |
| `pitest` | String | `1.30.0` | |
| `errorProne` | String | `2.50.0` | |
| `nullAway` | String | `0.14.2` | |
| `archUnit` | String | `1.5.1` | |
