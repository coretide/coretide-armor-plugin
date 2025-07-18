# 🛡️ CodeArmor Plugin

[![Status](https://img.shields.io/badge/status-alpha-orange?style=flat-square)]()
[![Latest Release](https://img.shields.io/github/v/release/coretide/coretide-armor-plugin?style=flat-square&logo=github)](https://github.com/coretide/coretide-armor-plugin/releases)
[![Version](https://img.shields.io/badge/version-0.1.0--alpha-blue?style=flat-square)](https://github.com/coretide/coretide-armor-plugin)
[![License](https://img.shields.io/github/license/coretide/coretide-armor-plugin?style=flat-square)](LICENSE)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/dev.coretide.armor?style=flat-square&logo=gradle)](https://plugins.gradle.org/plugin/dev.coretide.armor)
[![Build Status](https://img.shields.io/github/actions/workflow/status/coretide/coretide-armor-plugin/ci.yml?style=flat-square&logo=github-actions)](https://github.com/coretide/coretide-armor-plugin/actions)

> **Comprehensive code quality and security plugin for Java/Kotlin projects**

> ⚠️ **Status:** Alpha — This plugin is under active development (version: 0.1.0-alpha). Expect breaking changes and frequent updates until 1.0.0.

CodeArmor is a powerful Gradle plugin that integrates multiple code quality, security, and formatting tools into a unified, easy-to-use solution. It provides automated project detection, intelligent configuration, and optimized development workflows for both single-module and multi-module projects.

## ✨ Features

- 🔍 **Comprehensive Code Quality**: JaCoCo, Checkstyle, SpotBugs, SonarQube integration
- 🎨 **Code Formatting**: Spotless with configurable Kotlin formatters (KtLint/KtFmt)
- 🔒 **Security Analysis**: OWASP Dependency Check (**Veracode integration in development**)
- 🚀 **Optimized Workflows**: Custom tasks for different development stages
- 🪝 **Smart Git Hooks**: Enhanced pre-commit and pre-push quality checks with file filtering
- 📋 **Version Management**: Automatic versioning from Git tags + resource token replacement
- 🎯 **Smart Detection**: Automatic project type detection (Java/Kotlin/Mixed)
- 🏗️ **Multi-Module Support**: Seamless configuration for complex projects
- ⚡ **Configuration Cache**: Optimized for Gradle's configuration cache
- 📝 **Resource Processing**: Automatic token replacement in application configuration files

## 🚀 Quick Start

### Installation

Add the plugin to your `build.gradle.kts`:
```kotlin
plugins {
  id("dev.coretide.armor") version "0.1.0-alpha"
}
```

Or using the legacy plugin application:
```kotlin
buildscript {
    dependencies {
        classpath("dev.coretide:coretide-armor-plugin:0.1.0-alpha")
    }
}

apply(plugin = "dev.coretide.armor")
```

### Basic Usage

The plugin works out of the box with zero configuration:

```kotlin
// No configuration needed - auto-detection enabled by default
```

For custom configuration:
```kotlin
codeArmor {
    // Project configuration
    autoDetect = true
    projectType = ProjectType.KOTLIN_APPLICATION
    isMultiModule = false
    
    // Tool enablement
    jacoco = true
    checkstyle = true
    spotbugs = true
    spotless = true
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
    preCommitEnabled = true
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

CodeArmor provides optimized tasks for different development workflows:

### Development Tasks

#### `quickBuild`
⚡ Fast development build (compile + test only, no quality checks)
```
./gradlew quickBuild
```

- **Purpose**: Rapid development iteration
- **Dependencies**: `assemble`, `test`
- **Use Case**: Local development, quick feedback

#### `formatCode`
🎨 Quick code formatting using Spotless
```
./gradlew formatCode
```

- **Purpose**: Apply code formatting
- **Dependencies**: `spotlessApply`
- **Use Case**: Before committing code

### Quality Assurance Tasks

#### `codeQuality`
🔍 Comprehensive quality checks
```
./gradlew codeQuality
```

- **Purpose**: Comprehensive quality validation
- **Dependencies**: `checkstyleMain`, `checkstyleTest`, `spotlessCheck`, `spotbugsMain`, `jacocoTestReport`, `jacocoTestCoverageVerification`, `sonar`
- **Use Case**: Before pushing to SCM
- **Reports Generated**:
  - JaCoCo coverage: `build/reports/jacoco/test/html/index.html`
  - SpotBugs: `build/reports/spotbugs/main.html`
  - Checkstyle: `build/reports/checkstyle/main.html`

#### `fullAnalysis`
🔒 Complete security + quality analysis for CI/CD
```
./gradlew fullAnalysis
```

- **Purpose**: Comprehensive analysis including security scans
- **Dependencies**: `codeQuality`, `dependencyCheckAnalyze`, `veracodeUpload` (if configured)
- **Use Case**: CI/CD pipelines, release preparation
- **Reports Generated**:
  - All quality reports from `codeQuality`
  - OWASP: `build/reports/dependency-check/dependency-check-report.html`
  - Veracode scan results (if configured)

### Debug and Information Tasks

#### `logExclusionInfo`
📋 Log coverage exclusion information for debugging
```
./gradlew logExclusionInfo
```

- **Purpose**: Display coverage exclusion patterns and settings
- **Dependencies**: None
- **Use Case**: Debugging coverage issues, understanding exclusions
- **Options**:
  - `--verbose`: Show detailed exclusion patterns
  - `--debug`: Enable debug logging with full pattern details

**Sample output:**
```
🛡️ CodeArmor Exclusion Information
============================================================
📊 Summary:
  • Default exclusions: 20
  • User exclusions: 3
  • Total patterns: 23

💡 Use --verbose or enable debug logging to see detailed patterns

📈 Coverage Settings:
  • Minimum coverage: 80%
  • Class minimum coverage: 75%
============================================================
```


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


**application-prod.properties:**
```properties
app.version=@appVersion@
app.git.commit=@gitVersion@
```


**application-local.yaml:**
```yaml
info:
  build:
    version: "@appVersion@"
    git:
      commit: "@gitVersion@"
```


Resource processing is enabled by default for application projects and can be controlled via:
```kotlin
codeArmor {
    enableResourceProcessing = true  // Enable/disable resource processing
}
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

#### Checkstyle
```kotlin
codeArmor {
    checkstyle = true
    checkstyleConfigFile = "config/checkstyle/checkstyle.xml"
    checkstyleSuppressionFile = "config/checkstyle/suppressions.xml"
    checkstyleMaxWarnings = 0
}
```

#### SpotBugs
```kotlin
codeArmor {
    spotbugs = true
    spotbugsExcludeFile = "config/spotbugs/exclude.xml"
    spotbugsEffort = "MAX"                    // MIN, DEFAULT, MAX
    spotbugsReportLevel = "HIGH"              // LOW, MEDIUM, HIGH
}
```

#### Spotless Code Formatting
```kotlin
codeArmor {
    spotless = true
    kotlinFormatter = KotlinFormatter.KTLINT  // KTLINT or KTFMT
    ktlintVersion = "1.6.0"
    ktfmtVersion = "0.46"
    ktfmtStyle = KtfmtStyle.KOTLINLANG       // KOTLINLANG, GOOGLE, META
    spotlessApplyLicenseHeader = false
    
    // Configure additional formats
    spotlessFormats {
        json = true
        yaml = true
        xml = true
        markdown = true
    }
    
    // Configure KtLint rules
    ktlintRules {
        "ktlint_standard_no-wildcard-imports" = "disabled"
        "ktlint_standard_max-line-length" = "disabled"
    }
}
```

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
    sonarJavaVersion = "11"
}
```

#### Veracode (In Development)
> ⚠️ **Note:** Veracode integration is not yet fully implemented. Functionality will be available in a future release once official API/community access is granted.
```kotlin
codeArmor {
    veracode = true
    veracodeUsername = "your-username"        // (planned)
    veracodePassword = "your-password"        // (planned)
}
```


### Git Integration

#### Git Hooks
```kotlin
codeArmor {
    enableGitHooks = true
    preCommitEnabled = true                   // Enhanced pre-commit with file filtering
    prePushEnabled = true                     // Run tests + fullAnalysis
}
```


#### Version Management
```kotlin
codeArmor {
    enableVersionFromGit = true               // Auto-version from Git tags
    enableResourceProcessing = true          // Enable token replacement
}
```

The version manager supports:
- **CI/CD Integration**: `CI_COMMIT_TAG` (GitLab), `GITHUB_REF` (GitHub Actions)
- **Git Tag Detection**: Automatic extraction from tags (removes "v" prefix)
- **Fallback Strategy**: Latest tag + "-SNAPSHOT" if not on exact tag

## 🏗️ Multi-Module Projects

CodeArmor automatically detects multi-module projects and applies appropriate configurations:

```kotlin
codeArmor {
  isMultiModule = true  // Override auto-detection if needed
}
```

For multi-module projects, the plugin:
- Configures root project with aggregation tasks
- Applies appropriate configurations to subprojects
- Provides consolidated reporting
- Optimizes task execution across modules

## 🪝 Enhanced Git Hooks

When enabled, CodeArmor automatically creates intelligent Git hooks with file filtering and staged file management:

### Pre-commit Hook
**Smart file filtering and formatting:**
```shell script
# Only processes relevant files: .java, .kt, .kts, .xml, .json, .js, .ts, .py, .go, .gradle
# Skips execution if no relevant files are staged
# Runs Spotless formatting and re-adds formatted files to staging
# Non-blocking checkstyle validation with warnings
```


**Features:**
- ✅ **File filtering**: Only processes relevant file types
- ✅ **Staged files detection**: Skips if no relevant files staged
- ✅ **Graceful handling**: Continues with warnings, doesn't block commits
- ✅ **Automatic re-staging**: Re-adds formatted files to staging area
- ✅ **Fast execution**: Uses `--daemon` for faster builds

### Pre-push Hook
**Comprehensive testing and security:**
```shell script
# Runs full test suite (blocking)
# Runs fullAnalysis for security and quality (non-blocking warnings)
# Provides detailed feedback on what was checked
```


**Features:**
- ✅ **Test validation**: Blocks push if tests fail
- ✅ **Security analysis**: Runs OWASP dependency check and quality scans
- ✅ **Non-blocking quality**: Warns about quality issues but doesn't block
- ✅ **Comprehensive reporting**: Shows all generated reports

### Hook Management
Hooks are automatically created in `.git/hooks/` when enabled and provide:
- 🎨 **Clear feedback**: Emoji-rich output with progress indicators
- 📋 **Detailed logging**: Shows which files are being processed
- ⚡ **Performance optimization**: Uses Gradle daemon for faster execution
- 🔧 **Graceful error handling**: Continues with warnings where appropriate

## 🔧 Environment Variables

CodeArmor supports various environment variables for CI/CD integration:

### Version Management
- `CI_COMMIT_TAG` - GitLab CI tag reference
- `GITHUB_REF` - GitHub Actions reference

### Veracode Integration
- `VERACODE_USERNAME` - Veracode username
- `VERACODE_PASSWORD` - Veracode password

### Configuration Cache
- `GRADLE_OPTS` - Gradle options (configuration cache detection)
- `org.gradle.configuration-cache` - Configuration cache property
- `org.gradle.unsafe.configuration-cache` - Unsafe configuration cache property

## 📊 Reports and Output

CodeArmor generates comprehensive reports in the `build/reports/` directory:

```
build/reports/
├── jacoco/test/html/index.html          # Coverage report
├── spotbugs/main.html                   # Bug analysis
├── checkstyle/main.html                 # Style violations
├── dependency-check/                    # Security vulnerabilities
│   └── dependency-check-report.html
└── tests/test/index.html               # Test results
```


## 🎯 Best Practices

### Development Workflow
1. **Local Development**: Use `quickBuild` for rapid iteration
2. **Before Committing**: Pre-commit hook automatically formats code
3. **Before Pushing**: Pre-push hook runs tests and security checks
4. **CI/CD Pipeline**: Use `fullAnalysis` for complete validation

### Configuration Tips
1. **Start Simple**: Use default configuration initially
2. **Gradual Adoption**: Enable tools progressively
3. **Team Alignment**: Ensure team agrees on formatting and quality standards
4. **CI Integration**: Configure appropriate thresholds for automated builds
5. **Resource Processing**: Use token replacement for build information in configs

### Performance Optimization
1. **Configuration Cache**: Enable Gradle's configuration cache
2. **Parallel Execution**: Use `--parallel` flag for multi-module projects
3. **Incremental Analysis**: Tools support incremental analysis where possible
4. **Git Hooks**: Leverage file filtering to minimize unnecessary processing

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup
1. Clone the repository
2. Run `./gradlew build` to build the plugin
3. Run `./gradlew publishToMavenLocal` to publish locally
4. Test in a sample project

### Reporting Issues
Please use the [GitHub Issues](https://github.com/coretide/coretide-armor-plugin/issues) page to report bugs or request features.

## 📄 License

This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details.

### Third-Party Components

CodeArmor integrates with and depends on several open-source components. See [NOTICE](NOTICE) for detailed attribution and licensing information for all third-party components.

### License Summary

```
Copyright 2025 Kushal Patel

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```


## 🙏 Acknowledgments

CodeArmor integrates and builds upon several excellent tools:
- [SpotBugs](https://spotbugs.github.io/) - Static analysis for Java
- [Spotless](https://github.com/diffplug/spotless) - Code formatting
- [SonarQube](https://www.sonarqube.org/) - Continuous code quality
- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/) - Vulnerability detection
- [JaCoCo](https://www.jacoco.org/) - Code coverage analysis
- [Checkstyle](https://checkstyle.sourceforge.io/) - Coding standard checks

---

**Developed by [Coretide](https://github.com/coretide)**
