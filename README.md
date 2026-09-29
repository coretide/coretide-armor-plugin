# 🛡️ CodeArmor Plugin

[![Status](https://img.shields.io/badge/status-alpha-orange?style=flat-square)]()
[![Latest Release](https://img.shields.io/github/v/release/coretide/coretide-armor-plugin?include_prereleases&style=flat-square&logo=github)](https://github.com/coretide/coretide-armor-plugin/releases)
[![Version](https://img.shields.io/badge/version-0.4.0--alpha-blue?style=flat-square)](https://github.com/coretide/coretide-armor-plugin)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue?style=flat-square)](LICENSE)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/dev.coretide.plugin.armor?style=flat-square&logo=gradle)](https://plugins.gradle.org/plugin/dev.coretide.plugin.armor)
[![Maven Central](https://img.shields.io/maven-central/v/dev.coretide.plugin/code-armor-plugin?style=flat-square&logo=apache-maven)](https://central.sonatype.com/artifact/dev.coretide.plugin/code-armor-plugin)
[![Build Status](https://img.shields.io/github/actions/workflow/status/coretide/coretide-armor-plugin/ci.yml?style=flat-square&logo=github-actions)](https://github.com/coretide/coretide-armor-plugin/actions)

> **Comprehensive code quality and security plugin for Java/Kotlin projects**

> ⚠️ **Status:** Alpha — This plugin is under active development (version: 0.4.0-alpha). Expect breaking changes and frequent updates until 1.0.0.

CodeArmor is a powerful Gradle plugin that integrates multiple code quality and security tools into a unified, easy-to-use solution. It provides automated project detection, intelligent configuration, and optimized development workflows for both single-module and multi-module projects.

## ✨ Features

- 🔍 **Comprehensive Code Quality**: JaCoCo, SpotBugs, detekt (Kotlin), SonarQube integration
- 📋 **One Summary Page**: every check's result on one page and a few console lines, linking each tool's report
- 🏛️ **Architecture Tests on Request**: ArchUnit, with a first set of rules scaffolded for Java or Kotlin
- 🧾 **Code Scanning Ready**: SARIF from SpotBugs, detekt and OWASP, gathered for GitHub code scanning, and a workflow to start from
- 🐞 **Bug and Null Checks on Request**: Error Prone and NullAway on Java sources as they compile
- 🧪 **Test Insights**: flaky tests retried on CI and named, slowest tests listed; Kover and PIT mutation testing on request
- 🔒 **Security Analysis**: OWASP Dependency Check, and Veracode through your own Veracode Gradle plugin
- 🔌 **Library API Checks on Request**: binary compatibility with your last release, and Kotlin ABI dumps
- 📦 **Dependency Health**: newer versions, a CycloneDX SBOM and a licence report on CI; unused dependencies on request
- 🚀 **Optimized Workflows**: Custom tasks for different development stages
- 🪝 **Git Hooks on Request**: A blocking pre-push hook for the basic checks, and optional Conventional Commits and secret-scanning hooks, installed only when you run `armorInstallGitHooks`
- 🧱 **Check Tiers**: Basic checks before a push, local checks in every build, network checks on CI
- 📈 **Code Stats**: Optional [Code::Stats](https://codestats.net) reporting of your commit activity, per repository or machine-wide
- 📋 **Version Management**: Automatic versioning from Git tags + resource token replacement
- 🎯 **Smart Detection**: Automatic project type detection (Java/Kotlin/Mixed)
- 🏗️ **Multi-Module Support**: Seamless configuration for complex projects
- ⚡ **Configuration Cache**: Optimized for Gradle's configuration cache
- 📝 **Resource Processing**: Automatic token replacement in application configuration files

## 🚫 Removed Features (Important Changes)

### ❌ **Spotless and Checkstyle Removed**

Starting with version 0.1.4-alpha, CodeArmor no longer includes Spotless or Checkstyle integration due to:

#### **Spotless Issues:**
- **Breaking changes in 8.0.0**: Complete removal of API access, forcing complex reflection-based configuration
- **Version compatibility problems**: Frequent incompatibilities between Spotless, ktlint, and Kotlin versions
- **Maintenance overhead**: Constantly adapting to breaking changes and API removals
- **Inconsistent behavior**: Different results across environments and project setups

#### **Checkstyle Issues:**
- **Limited Kotlin support**: Primarily Java-focused, not ideal for mixed Java/Kotlin projects
- **Configuration complexity**: Difficult to maintain consistent rules across teams
- **Modern IDE alternatives**: IntelliJ IDEA and other modern IDEs provide superior formatting and style checking

### 💡 **Recommended Alternatives**

**For Code Formatting:**
- **IntelliJ IDEA Built-in Formatting**: Use Ctrl+Alt+L (or Cmd+Alt+L on Mac)
- **`.editorconfig`**: Define consistent formatting rules across your team
- **Save Actions Plugin**: Automatically format code on save in IntelliJ
- **Direct ktlint integration**: Use the ktlint Gradle plugin directly if needed

**For Style Checking:**
- **IntelliJ IDEA Inspections**: Comprehensive built-in code analysis
- **SonarQube**: Included in CodeArmor, provides excellent code quality analysis
- **SpotBugs**: Included in CodeArmor, focuses on actual bugs rather than style

**Why This Is Better:**
- ✅ More reliable and stable tooling
- ✅ Better IDE integration
- ✅ Less build complexity
- ✅ Faster development workflow
- ✅ No version compatibility issues

---

## 🆕 What's New in 0.4.0-alpha

- 🛡️ **`armorInfo`**: what CodeArmor checks in a project, with each tool's version, and what needs attention.
- 📐 **Diff coverage**: every build reports how much of the lines changed since the base branch tests cover; a
  minimum on request.
- 🐛 **Adopting checks**: a SpotBugs baseline accepts the findings already there, and detekt can run with type
  resolution.
- 🧪 **Integration tests**: an `integrationTest` suite on request, in `build` and in coverage.
- 🔑 **CI**: gitleaks over the history, the summary on the GitHub Actions run page, and a scaffolded workflow that
  submits the dependency graph, with Dependabot.
- 🔧 **Tool versions**: run a newer or older JaCoCo, PIT, Error Prone, NullAway or ArchUnit than the default.

SonarQube now runs only once a server or token is configured, and `SONAR_TOKEN` finally works. See the
[changelog](CHANGELOG.md) for everything, including [upgrading from 0.3.x](CHANGELOG.md#upgrading-from-03x).

---

## 🚀 Quick Start

### Requirements

- **Gradle 9.0** or newer
- **JDK 17** or newer to run Gradle

### Installation

Add the plugin to your `build.gradle.kts`:
```kotlin
plugins {
  id("dev.coretide.plugin.armor") version "0.4.0-alpha"
}
```


### Basic Usage

The plugin works out of the box with zero configuration:

```kotlin
// No configuration needed - auto-detection enabled by default
```


For custom configuration:
```kotlin
import dev.coretide.plugin.armor.ProjectType

codeArmor {
    // Project configuration
    autoDetect = false                        // projectType is only used when autoDetect = false
    projectType = ProjectType.KOTLIN_APPLICATION
    isMultiModule = false
    
    // Tool enablement
    jacoco = true
    spotbugs = true
    detekt = true                             // Kotlin projects only
    owasp = true
    sonarqube = true                          // In fullAnalysis once a server or token is configured
    veracode = false
    
    // Coverage and tests
    coverageMinimum = 0.80
    coverageClassMinimum = 0.75
    diffCoverage = true                       // Coverage of the lines changed since the base branch
    diffCoverageMinimum = null                // Set to fail below it
    flakyTestRetries = 2                      // On CI only
    
    // Dependency health, in fullAnalysis
    dependencyUpdates = true
    sbom = true                               // With a licence report
    forbiddenLicenses = mutableListOf("AGPL-3.0-only")
    
    // Opt-in extras
    kover = false                             // Kover instead of JaCoCo, in Kotlin projects
    integrationTests = false                  // An integrationTest suite, in build
    detektTypeResolution = false              // detektMain instead of detekt
    mutationTesting = false                   // ./gradlew pitest
    strictCompilation = false                 // Warnings fail the build
    errorProne = false                        // Error Prone on Java sources
    nullAway = false                          // NullAway, with Error Prone
    architectureTests = false                 // ArchUnit
    dependencyAnalysis = false                // Unused and undeclared dependencies
    apiBaseline = null                        // A release to check binary compatibility against
    kotlinAbiValidation = false               // Kotlin ABI dumps
    conventionalCommits = false               // commit-msg hook
    secretScan = false                        // gitleaks pre-commit hook, and a history scan in fullAnalysis
    
    // Git integration
    enableGitHooks = true                     // registers armorInstallGitHooks / armorUninstallGitHooks
    prePushEnabled = true                     // armorInstallGitHooks includes the pre-push hook
    enableVersionFromGit = true
    
    // Check tiers: what the pre-push hook, build and fullAnalysis run
    checks {
        prePush = listOf("quickBuild")
    }
    
    // Code::Stats reporting, off by default
    codeStats {
        enabled = true
    }
    
    // Another version of a tool than the default
    toolVersions {
        jacoco = "0.8.15"
    }
    
    // Resource processing
    enableResourceProcessing = true
}
```

[Check Tiers](#check-tiers) lists the defaults for each tier. [Code Stats](#-code-stats) covers installing
it, machine-wide reporting and each developer's own opt-in or opt-out.


## 📋 Supported Project Types

CodeArmor automatically detects and supports:

- **JAVA_APPLICATION** - Java applications
- **JAVA_LIBRARY** - Java libraries
- **KOTLIN_APPLICATION** - Kotlin applications
- **KOTLIN_LIBRARY** - Kotlin libraries
- **MIXED_APPLICATION** - Java + Kotlin applications
- **MIXED_LIBRARY** - Java + Kotlin libraries

The language comes from the production sources (`src/main`) and the Kotlin plugin. A project is an
application when it applies `application`, Spring Boot, Quarkus, Micronaut's application plugin or Ktor;
otherwise it is a library.

## 🎯 Available Tasks

CodeArmor organizes its checks in three tiers, from fastest to most thorough:

| Tier | Runs in | Default checks | Needs |
|---|---|---|---|
| Basic | the pre-push hook (`quickBuild`) | compile + unit tests | nothing |
| Local | `./gradlew build` (through `codeQuality`) | SpotBugs, detekt (Kotlin projects), JaCoCo (or Kover) report + coverage verification, diff coverage | nothing |
| CI | `fullAnalysis` | the local tier + OWASP Dependency Check + dependency updates + SBOM and licence report + SonarQube, once a server or token is configured + the gitleaks history scan, with `secretScan` | network, a SonarQube server |

Each tier's task list is configurable; see [Check Tiers](#check-tiers).

### Development Tasks

#### `quickBuild`
⚡ Fast development build (compile + test only, no quality checks)
```shell script
./gradlew quickBuild
```


- **Purpose**: Rapid development iteration
- **Dependencies**: `assemble`, `test`
- **Use Case**: Local development, quick feedback; what the pre-push hook runs by default

### Quality Assurance Tasks

#### `codeQuality`
🔍 Local code quality checks, which `./gradlew build` also runs
```shell script
./gradlew codeQuality
```


- **Purpose**: Quality checks that need no network or server
- **Dependencies**: the local tier, by default `spotbugsMain`, `detekt` (Kotlin projects), `jacocoTestReport`, `jacocoTestCoverageVerification`
- **Use Case**: Every build; `check` (and so `build`) depends on it
- **Reports Generated**:
    - JaCoCo coverage: `build/reports/jacoco/test/html/index.html`
    - SpotBugs: `build/reports/spotbugs/spotbugsMain.html`
    - detekt: `build/reports/detekt/detekt.html` (also SARIF and checkstyle XML)

#### `fullAnalysis`
🔒 Complete security + quality analysis for CI/CD
```shell script
./gradlew fullAnalysis
```


- **Purpose**: Comprehensive analysis including security scans
- **Dependencies**: `codeQuality`, then the CI tier, by default `dependencyCheckAnalyze`, `dependencyUpdates`, `armorLicenseReport` and, once a SonarQube server or token is configured, `sonar`; plus `veracodeUpload` (if configured)
- **Use Case**: CI/CD pipelines, release preparation
- **Reports Generated**:
    - All quality reports from `codeQuality`
    - OWASP: `build/reports/dependency-check/dependency-check-report.html`
    - Veracode scan results (if configured)

### Git Hook Tasks

#### `armorInstallGitHooks` / `armorUninstallGitHooks`
🪝 Install or remove CodeArmor's git hooks. See [Git Hooks](#-git-hooks).
```shell script
./gradlew armorInstallGitHooks
```

### Code Stats Tasks

| Task | What it does |
|---|---|
| `armorCodeStatsInstall` | Applies the [code stats](#-code-stats) settings: installs, or opts this repository out when disabled |
| `armorCodeStatsUninstall` | Removes CodeArmor's install and restores the previous `core.hooksPath` |
| `armorCodeStatsStatus` | Shows the settings in effect, the token, queued pulses and what this repository reports through |
| `armorCodeStatsFlush` | Retries delivering queued pulses now |

### Code Scanning
SpotBugs, detekt, OWASP Dependency Check and the gitleaks scan write SARIF reports. `armorSarifReport` gathers them from every
project into `build/reports/sarif/`, ready for GitHub code scanning or another SARIF viewer:
```shell script
./gradlew build --continue
./gradlew armorSarifReport
```
Each run gets a category from where its report lies, such as `codearmor/module-a/spotbugs/spotbugsMain`: code
scanning rejects two runs of one tool in the same category, as with the SpotBugs runs of two modules.

`./gradlew armorScaffoldProject` writes a starting point, never overwriting what exists:
- `.editorconfig` in the Gradle root: UTF-8, LF, 4-space indents, 120 columns for Java and Kotlin, trailing
  commas for Kotlin.
- `.github/workflows/codearmor.yml` at the top of the repository. It runs `build --continue` and uploads the
  gathered SARIF with `github/codeql-action/upload-sarif`, so findings show in the Security tab and on pull
  requests. It runs from the Gradle root when that is a subdirectory, and has `fullAnalysis` ready to switch on.
  On pushes, a second job submits the resolved dependencies, transitive ones included, to GitHub's dependency
  graph with `gradle/actions/dependency-submission`, so Dependabot alerts cover everything the build uses.
- `.github/dependabot.yml`: weekly update pull requests for the Gradle dependencies and the workflows' actions.

On GitHub Actions, `armorReport` also writes its summary to the job summary (`GITHUB_STEP_SUMMARY`), so it shows
on the run's page.

### Debug and Information Tasks

#### `armorInfo`
🛡️ What CodeArmor checks in this project, and what needs attention
```shell script
./gradlew armorInfo
```

- **Shows**: the CodeArmor version, what `build`, `fullAnalysis` and the pre-push hook run, each tool with its
  version and settings, switched on or off
- **Needs attention**: SonarQube switched on with no server configured, OWASP without an NVD API key, git hooks
  not installed, `secretScan` without gitleaks, Veracode without its plugin or credentials
- **Multi-module builds**: the root shows the git hooks, and each module its own tiers and tools

#### `logExclusionInfo`
📋 Log coverage exclusion information for debugging
```shell script
./gradlew logExclusionInfo
```


- **Purpose**: Display coverage exclusion patterns and settings
- **Dependencies**: None
- **Use Case**: Debugging coverage issues, understanding exclusions

## 📝 Resource Processing

In application projects (see [Supported Project Types](#-supported-project-types)), CodeArmor processes
application configuration files and replaces tokens with build information:

### Supported File Patterns
- `application.yaml` / `application.yml`
- `application.properties`
- `application-*.yaml` / `application-*.yml` (profiles)
- `application-*.properties` (profiles)

### Available Tokens
- `@appVersion@` - Project version
- `@gitVersion@` - Git commit hash (short)

### Usage Examples

**application.yml:**
```yaml
app:
  version: "@appVersion@"
  git:
    commit: "@gitVersion@"
```


**application.properties:**
```properties
app.version=@appVersion@
app.git.commit=@gitVersion@
```


## ⚙️ Configuration Options

### Tool Configuration

#### JaCoCo Coverage
```kotlin
codeArmor {
    jacoco = true
    coverageMinimum = 0.80                    // Overall coverage threshold
    coverageClassMinimum = 0.75               // Per-class coverage threshold
    coverageInclusions = mutableListOf("com/example/**")
    coverageExclusions = mutableListOf("Dto", "legacy")   // Anywhere in a class name, or a package name
    coverageIncludeDefaultExclusions = true   // Entry points, configuration and generated code
    junitPlatform = true                      // Run default JUnit 4 test tasks on the JUnit Platform
}
```

The default exclusions leave out only entry points (`*Application`, `*ApplicationKt`), configuration
(`*Config`, `*Configuration`) and generated code (`generated` packages, MapStruct's `*MapperImpl`, JPA's `*_`
metamodel classes), matched as a whole class-name suffix or package, and a nested class with its outer class.
`coverageExclusions` adds your own, matched anywhere in a class name or as a package name: `"Dto"` leaves out
`UserDto`, and `"legacy"` the `legacy` package. The same exclusions apply to the reports, coverage verification,
Kover and SonarQube; `./gradlew logExclusionInfo` shows them.

With JaCoCo on, CodeArmor also sets up the test tasks:
- **Runner:** a test task still on Gradle's default runner (JUnit 4) runs on the JUnit Platform. A task that
  chose TestNG, or configured the JUnit Platform itself, keeps its choice. Set `junitPlatform = false` to
  keep JUnit 4.
- **Logging:** failed tests are shown with their full stack trace. Passing tests and test output stay quiet.

#### Diff Coverage
```kotlin
codeArmor {
    diffCoverage = true                       // armorDiffCoverage, in build
    diffCoverageMinimum = 0.80                // Optional: fail when tests cover less of the changed lines
    diffCoverageBase = "origin/develop"       // Optional: the branch to compare with
}
```

`armorDiffCoverage` measures what a pull request adds: the share of the lines changed since the base branch that
tests run, from the JaCoCo or Kover report the build already writes. Only lines with code count, in files the
report covers, so comments, tests and excluded classes are left out. It compares the working tree, so uncommitted
changes count too:

```
📐 Diff coverage: 75.0% of 12 changed lines since origin/main
   src/main/java/com/example/OrderService.java: untested lines 41, 42, 57
```

It reports and never fails, until `diffCoverageMinimum` is set. The base branch is `diffCoverageBase`, else the
pull request's target on GitHub Actions, GitLab, Jenkins, Azure Pipelines or Bitbucket, else `origin/HEAD`,
`origin/main`, `origin/master`, `main` or `master`. Without one, or without the history back to it, it says so and
passes: on GitHub Actions, check out with `fetch-depth: 0`, as the workflow `armorScaffoldProject` writes does.

#### Integration Tests
```kotlin
codeArmor {
    integrationTests = true                   // Opt-in
}
```

Adds an `integrationTest` source set, in `src/integrationTest/java` or `src/integrationTest/kotlin`, and a task
that runs it after the unit tests. The tests see the production code and every library the unit tests have, and
run on the same runner. `build` runs them, their coverage counts in the JaCoCo or Kover report, coverage
verification and diff coverage, and the summary has a line for them. Add libraries only they need with
`integrationTestImplementation(...)`.

#### Flaky and Slow Tests
```kotlin
codeArmor {
    flakyTestRetries = 2                      // On CI; 0 turns retries off
    slowTestThresholdMillis = 2000            // 0 turns the list off
}
```
- **Flaky tests:** on CI (`CI=true`), a failed test is re-run up to `flakyTestRetries` times. If it then passes, the
  build passes, but CodeArmor names it as flaky instead of letting it disappear. More than ten failures in one
  test task is treated as a broken build, and nothing is retried. Locally, nothing is retried.
- **Slow tests:** after each test run, the five slowest tests over the threshold are listed.

#### Kover (Kotlin Coverage)
```kotlin
codeArmor {
    kover = true                              // Opt-in
}
```
[Kover](https://github.com/Kotlin/kotlinx-kover) measures coverage instead of JaCoCo in projects that apply the
Kotlin JVM plugin. It understands Kotlin's inline functions and coroutines better. The same `coverageMinimum`,
`coverageClassMinimum` and exclusions apply.
- **Tasks:** `koverXmlReport`, `koverHtmlReport` and `koverVerify` join the local tier.
- **SonarQube:** reads `build/reports/kover/report.xml`.
- **Java-only projects:** they keep JaCoCo, since Kover only measures Kotlin projects.
- **Multi-module builds:** no combined coverage report while Kover is on.

#### Mutation Testing (PIT)
```kotlin
codeArmor {
    mutationTesting = true                    // Opt-in
    mutationThreshold = 60                    // Percent; 0 (the default) only reports
}
```
[PIT](https://pitest.org) makes small changes to the production code and reports which of them no test notices.
Run it with `./gradlew pitest`. It is slow, so it is in no check tier. It mutates the project's own packages, taken
from the sources, and writes `build/reports/pitest/index.html`. Tests on the JUnit Platform get PIT's JUnit 5
plugin.


#### SpotBugs
```kotlin
codeArmor {
    spotbugs = true
    
    // Configure SpotBugs settings
    spotbugs {
        effort = "MAX"                        // MIN, DEFAULT, MAX
        reportLevel = "HIGH"                  // LOW, MEDIUM, HIGH
        excludeFile = "config/spotbugs/spotbugs-exclude.xml"
        baselineFile = "config/spotbugs/baseline.xml"   // The default; used when it exists
    }
}
```

To adopt SpotBugs in existing code, accept the findings already there, and fail only on new ones:

```shell script
./gradlew armorSpotbugsBaseline   # Writes config/spotbugs/baseline.xml; commit it
```

`spotbugsMain` then leaves out the findings in the baseline, from its reports as well as from failing the build.
SpotBugs matches them by a hash of the bug pattern, class, method and names, not by line, so edits around a
finding keep it accepted. Run the task again to accept what is there now; delete the file to see every finding.

Without `excludeFile`, CodeArmor generates a default filter under `build/codearmor/`. To start from
that default and customize it, run `./gradlew armorScaffoldConfigs`: it writes
`config/spotbugs/spotbugs-exclude.xml` and `config/owasp/suppressions.xml` (never overwriting existing
files), which you then point `excludeFile` and `owaspSuppressionFile` at.

SpotBugs also writes a SARIF report (`sarifReports = true`, the default), for GitHub code scanning; see
[Code Scanning](#code-scanning).


#### detekt (Kotlin)
```kotlin
codeArmor {
    detekt = true                             // On by default in projects that apply the Kotlin JVM plugin
    detektTypeResolution = false              // Opt-in: detektMain, with type resolution, instead of detekt
}
```
[detekt](https://detekt.dev) is the Kotlin static analyser. In projects that apply the Kotlin JVM plugin,
CodeArmor applies detekt 2.0.0-alpha.6 and adds its `detekt` task to the local tier, so `./gradlew build`
fails on its findings. Java projects are left alone.

- **Adopting it:** `./gradlew detektBaseline` records the findings already in the code in
  `detekt-baseline.xml`. detekt then reports only new ones. Commit the baseline and shrink it over time.
- **Rules:** put overrides in `config/detekt/detekt.yml`. It only needs the rules you change; detekt's defaults
  apply to everything else.
- **Reports:** HTML, SARIF (for GitHub code scanning) and checkstyle XML in `build/reports/detekt/`.
  SonarQube imports the XML.
- **Type resolution:** with `detektTypeResolution = true`, `detektMain` takes `detekt`'s place in the local tier.
  It analyses the main sources against their compile classpath, so the rules that need types run too; it is
  slower. Its baseline is `detekt-baseline-main.xml`, from `./gradlew detektBaselineMain`, and its reports are
  `build/reports/detekt/main.*`.
- **Kotlin version:** detekt parses with its own Kotlin compiler, 2.4. In a project on a newer Kotlin release,
  CodeArmor leaves detekt off and says so, rather than fail on syntax detekt cannot read.
- **detekt 1.x:** a project that applies `io.gitlab.arturbosch.detekt` itself keeps it, and its `detekt`
  task joins the local tier instead.
- **The Kotlin plugin's classes:** detekt needs them, so declare the Kotlin plugin in the build script that
  applies CodeArmor, or in a parent one. In a multi-module build that applies CodeArmor at the root, use
  `kotlin("jvm") version "…" apply false` there. Otherwise CodeArmor leaves detekt off and explains why.

> ℹ️ detekt 2.0 is still an alpha, but it is the only release that reads current Kotlin: detekt 1.23 stops at
> Kotlin 2.0. `detekt = false` switches it off.

#### OWASP Dependency Check
```kotlin
codeArmor {
    owasp = true
    owaspFailBuildOnCVSS = 9.0               // Fail build on CVSS score
    owaspSuppressionFile = "config/owasp/suppressions.xml"
    owaspAutoUpdate = false                   // Auto-update vulnerability database
    owaspNvdApiKey = "your-nvd-api-key"      // NVD API key for faster updates
    owaspNvdApiDelay = 4000                  // Delay between API calls (ms)
    owaspNvdMaxRetryCount = 10               // Max retry attempts
    owaspNvdValidForHours = 24               // Cache validity period
}
```
It scans `runtimeClasspath`, what ships, and not the configurations of CodeArmor's own tools. The NVD API key comes
from `owaspNvdApiKey`, the `nvd.api.key` Gradle property, or the `NVD_API_KEY` environment variable.

#### Dependency Health
```kotlin
codeArmor {
    dependencyUpdates = true                  // On by default, CI tier
    sbom = true                               // On by default, CI tier
    forbiddenLicenses = mutableListOf("AGPL-3.0-only", "GPL-3.0-only")   // Empty by default
    dependencyAnalysis = true                 // Opt-in, CI tier
}
```
- **Newer versions:** `./gradlew dependencyUpdates` ([gradle-versions-plugin](https://github.com/ben-manes/gradle-versions-plugin))
  lists dependencies with a newer release in `build/dependencyUpdates/report.{txt,json,html}`. Pre-releases are only
  offered for a dependency that is already on one. It never fails the build. It runs without the configuration cache,
  which the versions plugin does not support.
- **SBOM:** `./gradlew cyclonedxBom` ([CycloneDX](https://github.com/CycloneDX/cyclonedx-gradle-plugin)) writes
  `build/reports/cyclonedx/bom.{json,xml}`. It covers `runtimeClasspath`, what ships, and so leaves out test
  dependencies and tools such as SpotBugs and JaCoCo. Applications are typed `application`, everything else
  `library`. Change either on the `cyclonedxDirectBom` task.
- **Licences:** `./gradlew armorLicenseReport` groups the SBOM's dependencies by licence in
  `build/reports/codearmor/licenses.txt`, with those that declare none listed last. With `forbiddenLicenses`, it fails
  on a dependency that can only be used under one of them. A dependency that offers a choice, such as several
  licences or `Apache-2.0 OR GPL-3.0-only`, fails only when every choice is forbidden. Licences are SPDX ids or names,
  matched ignoring case.
- **Dependency analysis:** `dependencyAnalysis = true` adds the
  [dependency-analysis plugin](https://github.com/autonomousapps/dependency-analysis-gradle-plugin)'s `projectHealth`:
  dependencies declared but not used, used but only there transitively, or on the wrong configuration. It reports
  and does not fail the build. It analyzes `java-library` and Kotlin JVM projects, and applications. For Kotlin, the
  Kotlin plugin must be loaded in the root build script, for example with `id("org.jetbrains.kotlin.jvm") apply false`.
#### Architecture Tests
```kotlin
codeArmor {
    architectureTests = true                  // Opt-in
}
```
Adds [ArchUnit](https://www.archunit.org) to the tests (and the JUnit Platform launcher Gradle 9 needs), for Java and
Kotlin alike: ArchUnit reads bytecode. `./gradlew armorScaffoldArchitectureTests` writes a first
`ArchitectureTest` into the project's top-level package, in Java or Kotlin:
- no package cycles between the packages directly under it;
- no field injection, no `System.out` or `System.err`, no generic exceptions thrown, no `java.util.logging`.

From then on it is an ordinary test of the project's: extend it, and a violated rule fails the build.

#### Library API Checks
```kotlin
codeArmor {
    apiBaseline = "1.4.0"                     // Opt-in: a released version, or "group:name:1.4.0"
    kotlinAbiValidation = true                // Opt-in: Kotlin libraries
}
```
Both are for libraries; applications have no API to keep, and are left alone.
- **Binary compatibility:** with `apiBaseline`, `./gradlew armorApiCheck` ([japicmp](https://github.com/melix/japicmp-gradle-plugin))
  compares the jar with that release and fails on binary incompatible changes, such as a removed or changed public
  method. Added API passes. It works on bytecode, so for Java and Kotlin alike. The release is resolved from the
  project's repositories; give full coordinates when it was published under another group or name. Reports:
  `build/reports/japicmp/api.{html,txt}`. Part of the CI tier.
- **Kotlin ABI:** `kotlinAbiValidation` switches on the Kotlin Gradle plugin's own ABI validation (Kotlin 2.2 or
  later). `./gradlew updateLegacyAbi` writes the public API to `api/`; commit it. `checkLegacyAbi` then joins the
  build tier and fails when the public API no longer matches the dump, so every API change shows up in review as
  a change to `api/`.

#### SonarQube
```kotlin
codeArmor {
    sonarqube = true
    sonarHostUrl = "https://sonar.example.com"   // Or leave unset and set SONAR_HOST_URL
    sonarProjectKey = "my-project"
    sonarProjectName = "My Project"
    sonarToken = "your-sonar-token"          // Or leave unset and set SONAR_TOKEN
    sonarQualityGateWait = false             // Wait for quality gate result
    sonarJavaVersion = "17"                  // Optional: taken from the project's Java settings when unset
}
```

The SonarScanner reads the sources, tests and compiled classes from the source sets, including Kotlin,
generated and custom ones. CodeArmor adds the JaCoCo coverage, SpotBugs and OWASP reports.

On CI, keep the token out of the build script: leave `sonarToken` unset and set the `SONAR_TOKEN` environment
variable, and `SONAR_HOST_URL` for the server.

`sonar` joins `fullAnalysis` only once a server or a token is configured: `sonarHostUrl` or `sonarToken`, the
`SONAR_HOST_URL` or `SONAR_TOKEN` environment variables, or the `sonar.host.url` or `sonar.token` system
properties. Without one, `fullAnalysis` leaves it out and says so. With a token and no server, the SonarScanner
uses its default, SonarQube Cloud; a local server needs `sonarHostUrl = "http://localhost:9000"`. Listing
`sonar` in `checks.ci` runs it regardless.


#### Veracode (Bring Your Own Plugin)
CodeArmor does not upload to Veracode itself. With `veracode = true`, `fullAnalysis` runs the `veracodeUpload` task
of a Veracode Gradle plugin you apply and configure, when `VERACODE_USERNAME` and `VERACODE_PASSWORD` are set.
Without such a plugin, CodeArmor logs a warning and skips the Veracode scan.
```kotlin
codeArmor {
    veracode = true                           // Runs your Veracode plugin's veracodeUpload in fullAnalysis
}
```

### Tool Versions

Each tool runs the version CodeArmor was tested with. To run another one, a newer release with a fix you need or
one that supports your JDK:

```kotlin
codeArmor {
    toolVersions {
        jacoco = "0.8.15"
        pitest = "1.30.0"
        errorProne = "2.50.0"
        nullAway = "0.14.2"
        archUnit = "1.5.1"
    }
}
```

SpotBugs has its own setting, `spotbugs { toolVersion = "…" }`. detekt, OWASP Dependency Check and Kover come
with their Gradle plugins, so their versions follow CodeArmor's. `./gradlew armorInfo` shows the versions in use.

### Strict Compilation
```kotlin
codeArmor {
    strictCompilation = true                  // Off by default
}
```
Compiler warnings in production code (the `main` source set) fail the build. Test code is left alone,
since tests often use deprecated APIs on purpose.
- **Java:** `-Xlint:all -Werror`, without the lint categories that flag the build setup rather than the code
  (`processing`, `serial`, `path`, `options`).
- **Kotlin:** `allWarningsAsErrors`, and `-Xjsr305=strict`, so nullability annotations on Java APIs become
  Kotlin types.
- **Kotlin libraries:** explicit API mode (`-Xexplicit-api=strict`), so every public declaration states its
  visibility and return type.

Arguments the build already passes, such as `kotlin { explicitApi() }`, are not repeated.

### Error Prone and NullAway
```kotlin
codeArmor {
    errorProne = true                         // Opt-in
    nullAway = true                           // Opt-in; turns on Error Prone as well
}
```
[Error Prone](https://errorprone.info) checks the Java sources as they compile, and its errors fail the build.
Its warnings only fail the build together with `strictCompilation`. Kotlin sources are not checked.
- **NullAway:** [NullAway](https://github.com/uber/NullAway) fails the build where production code may
  dereference null, or return or pass null where no `@Nullable` says it may. Any annotation named `Nullable`
  counts; [JSpecify](https://jspecify.dev)'s `org.jspecify:jspecify` is a good choice. It checks the packages
  declared under `src/main`, or only `@NullMarked` code when the sources declare none. Test code is not checked.
- **Generated code:** anything under `build/generated/` is skipped.
- **JDK:** Error Prone needs a JDK 21 compiler. On an older toolchain, compilation goes ahead without it and says so.
  `options.release` can still target an older Java.
- **Versions:** Error Prone 2.50.0 and NullAway 0.14.2. A newer version in your own `errorprone` dependencies
  takes precedence.

### Logging Configuration

CodeArmor provides configurable logging levels to control the verbosity of plugin output:

```kotlin
import dev.coretide.plugin.armor.enumeration.ArmorLogLevel

codeArmor {
    logLevel = ArmorLogLevel.ESSENTIAL        // VERBOSE, ESSENTIAL, STEALTH
}
```

**Available Log Levels:**

- **`VERBOSE`** - 📢 **All logs**: Shows detailed information about all plugin operations
- **`ESSENTIAL`** - ⚖️ **Default logging**: Shows important information, warnings, and errors
- **`STEALTH`** - 🤫 **No logs**: Suppresses all plugin output except critical errors

### Check Tiers

Each tier is a list of task names. The defaults follow the tool switches above:

```kotlin
codeArmor {
    checks {
        prePush = listOf("quickBuild")                     // basic: run by the pre-push hook
        build = listOf(                                    // local: run by codeQuality and build
            "spotbugsMain", "detekt", "jacocoTestReport", "jacocoTestCoverageVerification",
        )
        ci = listOf(                                       // network/server: added by fullAnalysis
            "dependencyCheckAnalyze", "dependencyUpdates", "armorLicenseReport",
            "sonar",                                       // by default only once a SonarQube server or token is configured
        )
    }
}
```

- Because the local tier includes `jacocoTestCoverageVerification`, `./gradlew build` fails when coverage
  is below `coverageMinimum` or a class is below `coverageClassMinimum`. Lower those thresholds, or
  leave `jacocoTestCoverageVerification` out of `build`, to adopt CodeArmor gradually.
- `build = listOf<String>()` keeps `./gradlew build` free of CodeArmor's checks. The SpotBugs plugin
  itself still adds its tasks to `check`; `spotbugs = false` removes those.
- The tiers apply to projects with a Java plugin; other projects, such as a docs module, are left alone.
- Tasks in `build` must not depend on `build` themselves (as `sonar` does), or `build` would depend on
  itself.

### Git Integration

#### Git Hooks
```kotlin
codeArmor {
    enableGitHooks = true                     // Register armorInstallGitHooks / armorUninstallGitHooks
    prePushEnabled = true                     // armorInstallGitHooks includes the pre-push hook
    conventionalCommits = false               // Opt-in: a commit-msg hook for Conventional Commits
    conventionalCommitTypes = mutableListOf("feat", "fix", "docs", "style", "refactor", "perf", "test", "build", "ci", "chore", "revert")
    secretScan = false                        // Opt-in: a pre-commit hook that runs gitleaks
}
```

#### Code Stats
```kotlin
codeArmor {
    codeStats {
        enabled = true                        // off by default; reports this repository only
    }
}
```
A developer's `~/.gradle/gradle.properties` can override this, and only it can choose machine-wide
reporting. See [Code Stats](#-code-stats).

#### Version Management
```kotlin
codeArmor {
    enableVersionFromGit = true               // Auto-version from Git tags
    enableResourceProcessing = true          // Enable token replacement
}
```

## 🏗️ Multi-Module Projects

CodeArmor automatically detects multi-module projects and applies appropriate configurations:

```kotlin
codeArmor {
  isMultiModule = true  // Override auto-detection if needed
}
```

Apply CodeArmor to the root project; it configures every module. `./gradlew allCodeQuality` runs each module's
`codeQuality`, then two reports for the whole build on the root:

| Report | Task | Where |
|---|---|---|
| Coverage | `testCodeCoverageReport` | `build/reports/jacoco/testCodeCoverageReport/` (HTML and XML) |
| Tests | `testAggregateTestReport` | `build/reports/tests/test/aggregated-results/` |

- **Cross-module coverage:** the combined coverage report counts a test in one module that exercises another
  module's code. It uses the same exclusions as each module's own report.
- **SonarQube:** each module's analysis reads the combined report as well as its own.
- **Which modules:** only modules with the Java plugin are included, so a docs or aggregator module is fine.
- **Repositories:** the root needs none of its own. It takes JaCoCo's reporting library from a module that
  already resolves it.
- **Switching it off:** `jacoco = false` leaves out the coverage report; the test report stays.

## 🪝 Git Hooks

CodeArmor installs hooks only when you ask it to; a build never writes them:

```shell script
./gradlew armorInstallGitHooks     # install, or update after changing settings
./gradlew armorUninstallGitHooks   # remove
```

### Pre-push Hook
- ✅ **Basic checks**: runs the `checks.prePush` tasks, by default `quickBuild` (compile + unit tests)
- ⛔ **Blocking**: a failure blocks the push; `git push --no-verify` skips the checks once
- 📁 **Monorepo-aware**: runs from the Gradle root, even when that is a subdirectory of the repository

The heavier checks run in `build` and in `fullAnalysis` on CI, not on every push.

### Commit Message Hook
With `conventionalCommits = true`, a commit-msg hook rejects a commit whose first line is not a
[Conventional Commit](https://www.conventionalcommits.org): `type(scope)!: description`, where the scope and `!`
are optional and the type is one of `conventionalCommitTypes`. Merge, revert, `fixup!`, `squash!` and `amend!`
commits pass, since git writes their subjects itself. `git commit --no-verify` skips the check once.

### Secret Scanning Hook
With `secretScan = true`, a pre-commit hook scans the staged changes with [gitleaks](https://github.com/gitleaks/gitleaks)
and blocks a commit that adds a secret. Mark a false positive with a `gitleaks:allow` comment or in `.gitleaksignore`.
- gitleaks is not bundled; install it from its releases or your package manager. Without it, the hook warns and
  lets the commit through.
- Works with gitleaks 8.19 and later (`gitleaks git --pre-commit --staged`) and older 8.x (`gitleaks protect --staged`).
- `git commit --no-verify` skips the scan once.

The hook only sees new commits, on machines that have gitleaks. For CI, `secretScan = true` also adds
`armorSecretScan` to `fullAnalysis`: gitleaks scans the whole history and fails on a secret, and its SARIF report
goes to code scanning with the other tools'. There it fails when gitleaks is missing, so install it in the
workflow first. A multi-module build scans once, from the root.

### Existing Hooks
- A hook CodeArmor did not write is never overwritten. `armorInstallGitHooks` tells you what to add to it
  instead.
- Hooks are installed into the repository's own hooks directory (shared by linked worktrees). If
  `core.hooksPath` points elsewhere, as with husky, Git runs hooks from there instead, and
  `armorInstallGitHooks` warns and names the hook to call from it.

### Upgrading from 0.1.x
0.1.x wrote hooks automatically while configuring every build. Run `./gradlew armorInstallGitHooks` once:
it replaces the old pre-push hook, which ran `fullAnalysis` on every push, and removes the obsolete
pre-commit hook, which called tasks that no longer exist.

## 📈 Code Stats

CodeArmor can report your commit activity to [Code::Stats](https://codestats.net), the free
programming-activity tracker. After each commit, it sends the number of added plus deleted lines per
language: an approximation of Code::Stats' usual editor-based XP.

It is **off by default**, and nothing is installed until you run `./gradlew armorCodeStatsInstall`.

### Enabling It

For this repository, in the build script (the team's choice):

```kotlin
codeArmor {
    codeStats {
        enabled = true                        // scope defaults to this repository only
    }
}
```

Each developer can override that in `~/.gradle/gradle.properties`, or with `-P` for one run:

```properties
codearmor.codestats.enabled=true              # or false, to opt out of the team's setting
codearmor.codestats.scope=GLOBAL              # every repository on this machine, including future clones
```

`GLOBAL` is only accepted from those developer settings. A build script or a project's `gradle.properties`
is shared by the whole team, and must not change every developer's global git configuration.

### Installing

Get your machine API token from https://codestats.net/my/machines, then run once:

```shell script
CODESTATS_API_TOKEN=<your token> ./gradlew armorCodeStatsInstall
```

The token is stored in `~/.config/code-stats-hooks/token`, readable only by you, and never in the
repository or the build files. Later runs reuse it.

- **Your existing hooks keep running.** Code stats takes over `core.hooksPath` and hands every hook over
  to whatever ran before: the repository's own hooks, husky, or a global hooks directory.
- **An existing standalone install is left alone.** If a code-stats-hooks install already covers the
  repository, CodeArmor uses it and changes nothing.
- **CI is skipped.** With `CI=true`, the install tasks do nothing.
- A tool that rewrites `core.hooksPath` (husky on `npm install`, say) switches code stats off for that
  repository; `armorCodeStatsStatus` shows it, and rerunning `armorCodeStatsInstall` fixes it.

### Opting Out

- Disabled for a project (`enabled = false`, or `codearmor.codestats.enabled=false`), running
  `armorCodeStatsInstall` removes CodeArmor's install from the repository, and opts the repository out of
  any global install (`git config codestats.enabled false`).
- `CODESTATS_DISABLE=1 git commit ...` skips a single commit.

### What Counts

- Only commits made on this machine, in repositories outside ignored paths such as `node_modules`,
  `.cache`, `vendor`. Add more patterns, one per line, to `~/.config/code-stats-hooks/ignore`.
- Merge commits and pure renames don't count.
- List the email addresses and names that are yours in `~/.config/code-stats-hooks/identities`, so a
  cherry-picked commit carrying someone else's authorship doesn't count as your work.
- A pulse contains only a timestamp, language names and XP counts: no source code, file paths,
  repository names or commit IDs. It is sent in the background over HTTPS and never delays or fails a
  commit; failed deliveries are retried for up to seven days.

On Windows, code stats runs in the bash that Git for Windows ships, so it needs nothing else installed.

## 📊 Reports and Output

`codeQuality`, and so `build`, and `fullAnalysis` end with `armorReport`: a summary on the console, and a page
at `build/reports/codearmor/index.html` with one line per tool and a link to its report:

```
📋 CodeArmor summary: build/reports/codearmor/index.html
   ✅ Tests: 214 tests
   ✅ Coverage (JaCoCo): 83.4% of lines, 71.2% of branches
   ✅ Diff coverage: 75.0% of 12 changed lines since origin/main
   ⚠️ SpotBugs: 2 findings
   ✅ Dependency updates: all dependencies up to date
```

It reads whatever reports exist and runs no tool itself, so `./gradlew armorReport` sums up the last run at any
time. In a multi-module build the root's page has a section per module. The tools' own reports:

```
build/reports/
├── codearmor/index.html                  # The summary, licenses.txt and diff-coverage.json
├── tests/test/index.html                 # Test results (tests/integrationTest with integrationTests)
├── jacoco/test/html/index.html           # Coverage (kover/html with Kover)
├── spotbugs/spotbugsMain.html            # SpotBugs (also XML and SARIF)
├── detekt/detekt.html                    # detekt, in Kotlin projects (also checkstyle XML and SARIF)
├── pitest/index.html                     # Mutation testing, when run
├── dependency-check/                     # OWASP Dependency Check (HTML, XML, JSON, SARIF)
├── cyclonedx/bom.json                    # The SBOM
├── japicmp/api.html                      # The API check, with apiBaseline
└── sarif/                                # Gathered by armorSarifReport
build/dependencyUpdates/report.html       # Dependency updates
```


## 🎯 Best Practices

### Development Workflow
1. **Local Development**: Use `quickBuild` for rapid iteration
2. **Code Formatting**: Use your IDE's built-in formatting (Ctrl+Alt+L)
3. **Every Build**: `./gradlew build` runs the local checks (SpotBugs, coverage verification)
4. **Before Pushing**: The pre-push hook runs the basic checks (`./gradlew armorInstallGitHooks` once)
5. **CI/CD Pipeline**: Use `fullAnalysis` for complete validation, including OWASP and SonarQube

### Configuration Tips
1. **Start Simple**: Use default configuration initially
2. **Gradual Adoption**: Enable tools progressively
3. **Team Alignment**: Use `.editorconfig` for consistent formatting
4. **CI Integration**: Configure appropriate thresholds for automated builds
5. **IDE Setup**: Configure your IDE for consistent code style

### Performance Optimization
1. **Configuration Cache**: Enable Gradle's configuration cache
2. **Parallel Execution**: Use `--parallel` flag for multi-module projects
3. **Incremental Analysis**: Tools support incremental analysis where possible

## 💡 Code Formatting Recommendations

Since CodeArmor no longer includes Spotless or Checkstyle, we recommend:

### IntelliJ IDEA Setup
1. **Import Code Style**: Use a shared code style configuration
2. **Enable Save Actions**: Install the "Save Actions" plugin for automatic formatting
3. **Configure .editorconfig**: Define consistent rules across your team
4. **Use Built-in Formatters**: Ctrl+Alt+L (Cmd+Alt+L on Mac)

### Team Consistency
1. **Share IDE Settings**: Commit `.idea/codeStyles/` to your repository
2. **Use .editorconfig**: Define basic formatting rules
3. **Document Standards**: Create a team coding standards document
4. **Regular Reviews**: Include code style in your review process

### Alternative Tools (Optional)
If you specifically need automated formatting in your build:
- **ktlint directly**: Add the ktlint Gradle plugin
- **Google Java Format**: Use the Google Java Format plugin
- **Prettier**: For JSON, YAML, and other formats

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup
1. Clone the repository
2. Run `./gradlew build` to build the plugin (building needs JDK 21; the plugin itself runs on JDK 17+)
3. Run `./gradlew publishToMavenLocal` to publish locally (no GPG key needed; only releases are signed)
4. Test in a sample project

### Reporting Issues
Please use the [GitHub Issues](https://github.com/coretide/coretide-armor-plugin/issues) page to report bugs or request features.

## 📄 License

This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details.

### Third-Party Components

CodeArmor integrates with and depends on several open-source components. See [NOTICE](NOTICE) for detailed attribution and licensing information for all third-party components.

## 🙏 Acknowledgments

CodeArmor integrates and builds upon several excellent tools:
- [SpotBugs](https://spotbugs.github.io/) - Static analysis for Java
- [SonarQube](https://www.sonarqube.org/) - Continuous code quality
- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/) - Vulnerability detection
- [JaCoCo](https://www.jacoco.org/) - Code coverage analysis

---

**Developed by [Coretide](https://github.com/coretide)**
