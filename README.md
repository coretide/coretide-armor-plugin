# 🛡️ CodeArmor Plugin

[![Status](https://img.shields.io/badge/status-alpha-orange?style=flat-square)]()
[![Latest Release](https://img.shields.io/github/v/release/coretide/coretide-armor-plugin?include_prereleases&style=flat-square&logo=github)](https://github.com/coretide/coretide-armor-plugin/releases)
[![Version](https://img.shields.io/badge/version-0.1.4--alpha-blue?style=flat-square)](https://github.com/coretide/coretide-armor-plugin)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue?style=flat-square)](LICENSE)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/dev.coretide.plugin.armor?style=flat-square&logo=gradle)](https://plugins.gradle.org/plugin/dev.coretide.plugin.armor)
[![Maven Central](https://img.shields.io/maven-central/v/dev.coretide.plugin/code-armor-plugin?style=flat-square&logo=apache-maven)](https://central.sonatype.com/artifact/dev.coretide.plugin/code-armor-plugin)
[![Build Status](https://img.shields.io/github/actions/workflow/status/coretide/coretide-armor-plugin/ci.yml?style=flat-square&logo=github-actions)](https://github.com/coretide/coretide-armor-plugin/actions)

> **Comprehensive code quality and security plugin for Java/Kotlin projects**

> ⚠️ **Status:** Alpha — This plugin is under active development (version: 0.1.4-alpha). Expect breaking changes and frequent updates until 1.0.0.

CodeArmor is a powerful Gradle plugin that integrates multiple code quality and security tools into a unified, easy-to-use solution. It provides automated project detection, intelligent configuration, and optimized development workflows for both single-module and multi-module projects.

## ✨ Features

- 🔍 **Comprehensive Code Quality**: JaCoCo, SpotBugs, SonarQube integration
- 🔒 **Security Analysis**: OWASP Dependency Check (**Veracode integration in development**)
- 🚀 **Optimized Workflows**: Custom tasks for different development stages
- 🪝 **Git Hooks on Request**: A blocking pre-push hook for the basic checks, installed only when you run `armorInstallGitHooks`
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

## 🆕 What's New in 0.1.4-alpha

### 🎯 **Streamlined Focus**
- **Removed unstable integrations**: Spotless and Checkstyle removed for better reliability
- **Enhanced core tools**: Improved JaCoCo, SpotBugs, SonarQube, and OWASP integration
- **Simplified workflows**: Focus on tools that provide consistent value

### 📁 **Improved Code Organization**
- **Package Restructuring**: Reorganized internal packages for better maintainability
  - `utils` → `util`
  - `tasks` → `task` 
  - `configurators` → `configurator`
- **Enhanced Configuration Classes**: Streamlined configuration for remaining tools
- **Better Type Safety**: Improved enumerations and validation

---

## 🚀 Quick Start

### Requirements

- **Gradle 9.0** or newer
- **JDK 17** or newer to run Gradle

### Installation

Add the plugin to your `build.gradle.kts`:
```kotlin
plugins {
  id("dev.coretide.plugin.armor") version "0.1.4-alpha"
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
    owasp = true
    sonarqube = true
    veracode = false
    
    // Coverage settings
    coverageMinimum = 0.80
    coverageClassMinimum = 0.75
    
    // Git integration
    enableGitHooks = true
    enableVersionFromGit = true
    
    // Resource processing
    enableResourceProcessing = true
    prePushEnabled = true
}
```


## 📋 Supported Project Types

CodeArmor automatically detects and supports:

- **JAVA_APPLICATION** - Java applications
- **JAVA_LIBRARY** - Java libraries
- **KOTLIN_APPLICATION** - Kotlin applications
- **KOTLIN_LIBRARY** - Kotlin libraries
- **MIXED_APPLICATION** - Java + Kotlin applications
- **MIXED_LIBRARY** - Java + Kotlin libraries

## 🎯 Available Tasks

CodeArmor organizes its checks in three tiers, from fastest to most thorough:

| Tier | Runs in | Default checks | Needs |
|---|---|---|---|
| Basic | the pre-push hook (`quickBuild`) | compile + unit tests | nothing |
| Local | `./gradlew build` (through `codeQuality`) | SpotBugs, JaCoCo report + coverage verification | nothing |
| CI | `fullAnalysis` | the local tier + OWASP Dependency Check + SonarQube | network, a SonarQube server |

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
- **Dependencies**: the local tier, by default `spotbugsMain`, `jacocoTestReport`, `jacocoTestCoverageVerification`
- **Use Case**: Every build; `check` (and so `build`) depends on it
- **Reports Generated**:
    - JaCoCo coverage: `build/reports/jacoco/test/html/index.html`
    - SpotBugs: `build/reports/spotbugs/main.html`

#### `fullAnalysis`
🔒 Complete security + quality analysis for CI/CD
```shell script
./gradlew fullAnalysis
```


- **Purpose**: Comprehensive analysis including security scans
- **Dependencies**: `codeQuality`, then the CI tier, by default `dependencyCheckAnalyze` and `sonar`, plus `veracodeUpload` (if configured)
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

### Debug and Information Tasks

#### `logExclusionInfo`
📋 Log coverage exclusion information for debugging
```shell script
./gradlew logExclusionInfo
```


- **Purpose**: Display coverage exclusion patterns and settings
- **Dependencies**: None
- **Use Case**: Debugging coverage issues, understanding exclusions

## 📝 Resource Processing

CodeArmor automatically processes application configuration files and replaces tokens with build information:

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
    coverageExclusions = mutableListOf("**/generated/**")
    coverageIncludeDefaultExclusions = true   // Include common exclusions
}
```


#### SpotBugs
```kotlin
codeArmor {
    spotbugs = true
    
    // Configure SpotBugs settings
    spotbugs {
        effort = "MAX"                        // MIN, DEFAULT, MAX
        reportLevel = "HIGH"                  // LOW, MEDIUM, HIGH
        excludeFile = "config/spotbugs/spotbugs-exclude.xml"
    }
}
```

Without `excludeFile`, CodeArmor generates a default filter under `build/codearmor/`. To start from
that default and customize it, run `./gradlew armorScaffoldConfigs`: it writes
`config/spotbugs/spotbugs-exclude.xml` and `config/owasp/suppressions.xml` (never overwriting existing
files), which you then point `excludeFile` and `owaspSuppressionFile` at.


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


#### SonarQube
```kotlin
codeArmor {
    sonarqube = true
    sonarHostUrl = "http://localhost:9000"
    sonarProjectKey = "my-project"
    sonarProjectName = "My Project"
    sonarToken = "your-sonar-token"
    sonarQualityGateWait = false             // Wait for quality gate result
    sonarJavaVersion = "21"
}
```


#### Veracode (In Development)
> ⚠️ **Note:** Veracode integration is not yet fully implemented. CodeArmor does not create the `veracodeUpload` task itself: `fullAnalysis` runs it only when a separately applied Veracode Gradle plugin provides it and `VERACODE_USERNAME`/`VERACODE_PASSWORD` are set. Without such a plugin, CodeArmor logs a warning and skips the Veracode scan.
```kotlin
codeArmor {
    veracode = true
    // Configuration will be available in future releases
}
```

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
            "spotbugsMain", "jacocoTestReport", "jacocoTestCoverageVerification",
        )
        ci = listOf("dependencyCheckAnalyze", "sonar")     // network/server: added by fullAnalysis
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
}
```

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

CodeArmor generates comprehensive reports in the `build/reports/` directory:

```
build/reports/
├── jacoco/test/html/index.html          # Coverage report
├── spotbugs/main.html                   # Bug analysis
├── dependency-check/                    # Security vulnerabilities
│   └── dependency-check-report.html
└── tests/test/index.html               # Test results
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
