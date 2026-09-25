# Contributing to CodeArmor

Thank you for your interest in contributing to CodeArmor! 🛡️ We welcome contributions from the community and are excited to work with you.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Contribute](#how-to-contribute)
- [Development Guidelines](#development-guidelines)
- [Testing](#testing)
- [Documentation](#documentation)
- [Submitting Changes](#submitting-changes)
- [Releasing](#releasing)
- [License](#license)

## 🤝 Code of Conduct 

This project adheres to a code of conduct that we expect all contributors to follow. Please be respectful and professional in all interactions.

### Our Standards

- Use welcoming and inclusive language
- Be respectful of differing viewpoints and experiences
- Gracefully accept constructive criticism
- Focus on what is best for the community
- Show empathy towards other community members

## 🚀 Getting Started

### Prerequisites

- **JDK 21** to build the plugin. The Gradle wrapper (`./gradlew`) provides Gradle itself.
- **Git** for version control

The plugin you build runs in projects on **Gradle 9.0+** with **JDK 17+**.
- **IntelliJ IDEA** (recommended) or any IDE with Gradle support

### Development Setup

1. **Fork the repository**
   ```bash
   # Clone your fork
   git clone https://github.com/YOUR-USERNAME/coretide-armor-plugin.git
   cd coretide-armor-plugin
   ```

2. **Set up development environment**
   ```bash
   # Build the plugin
   ./gradlew build
   
   # Run tests
   ./gradlew test
   
   # Publish to local Maven repository for testing (no GPG key needed; only releases are signed)
   ./gradlew publishToMavenLocal
   ```

3. **Test the plugin in a sample project**

   Resolve the plugin from Maven local in the sample project's `settings.gradle.kts`:
   ```kotlin
   pluginManagement {
       repositories {
           mavenLocal()
           gradlePluginPortal()
       }
   }
   ```
   Then apply it in its `build.gradle.kts`, using the version `./gradlew properties` prints:
   ```kotlin
   plugins {
       id("dev.coretide.plugin.armor") version "0.1.0-SNAPSHOT"
   }
   ```

## 🔧 How to Contribute

### Reporting Issues

Before creating an issue, please:

1. **Search existing issues** to avoid duplicates
2. **Check the documentation** - your question might already be answered
3. **Test with the latest version** to ensure the issue still exists

When creating an issue, include:
- Clear description of the problem
- Steps to reproduce
- Expected vs actual behavior
- Environment details (OS, Java version, Gradle version)
- Relevant logs or error messages

### Suggesting Features

We welcome feature suggestions! Please:

1. **Check existing issues** for similar requests
2. **Describe the use case** - why is this feature needed?
3. **Provide examples** - how would this feature work?
4. **Consider backwards compatibility** - will this break existing functionality?

### Code Contributions

1. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**
    - Follow our coding standards
    - Add tests for new functionality
    - Update documentation as needed

3. **Test thoroughly**
   ```bash
   ./gradlew build
   ```

4. **Commit your changes**
   ```bash
   git commit -m "feat: add new feature description"
   ```

5. **Push and create a pull request**
   ```bash
   git push origin feature/your-feature-name
   ```

## 📝 Development Guidelines

### Code Style

- **Kotlin**: Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Java**: Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- **Formatting**: Use IntelliJ IDEA with the committed code style in `.idea/codeStyles/` (Ctrl+Alt+L / Cmd+Alt+L)

### Commit Messages

We follow [Conventional Commits](https://www.conventionalcommits.org/):
```
<type>(<scope>): <description>

<body>

<footer>
```
**Types:**
- `feat`: New features
- `fix`: Bug fixes
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Test additions or modifications
- `chore`: Maintenance tasks

**Examples:**
```bash
feat(jacoco): add support for custom coverage exclusions
fix(sonarqube): resolve configuration cache compatibility issue
docs(readme): update installation instructions
```

### Code Structure

```
src/
├── main/kotlin/dev/coretide/plugin/armor/
│   ├── CodeArmorPlugin.kt           # Main plugin class
│   ├── CodeArmorExtension.kt        # Configuration DSL
│   ├── ProjectType.kt               # Project type enum
│   ├── codestats/                   # Code stats: settings, installs, file locations
│   ├── config/                      # Nested configuration (SpotBugsConfig, ChecksConfig, CodeStatsConfig)
│   ├── configurator/                # Tool configuration
│   │   ├── JacocoConfigurator.kt
│   │   ├── SpotbugsConfigurator.kt
│   │   └── ...
│   ├── enumeration/                 # ArmorLogLevel, CodeStatsScope
│   ├── git/                         # Git hooks and git-derived versioning
│   │   ├── GitHooksManager.kt
│   │   ├── GitValueSource.kt
│   │   └── VersionManager.kt
│   ├── task/                        # Custom tasks and task creation
│   │   ├── TaskCreator.kt
│   │   ├── MultiModuleTaskCreator.kt
│   │   └── ...
│   └── util/                        # Utility classes
│       ├── ProjectDetector.kt
│       ├── ConfigurationCacheUtil.kt
│       └── ...
├── main/resources/dev/coretide/plugin/armor/codestats/   # The code stats hook scripts (POSIX sh, bash)
└── test/kotlin/dev/coretide/plugin/armor/   # Gradle TestKit tests
compat/jdk17-consumer/               # Consumer build CI runs on Gradle 9.0 + JDK 17
```


### Adding New Features

1. **Create a configurator** for new tools in `configurator/`
2. **Update the extension** to include configuration options
3. **Add project detection** logic if needed
4. **Create custom tasks** if required
5. **Update documentation** (README, KDoc comments)
6. **Add comprehensive tests**

### Configuration Cache Compatibility

All new code must be compatible with Gradle's configuration cache:

- Use `Provider<T>` for lazy evaluation
- Avoid accessing `Project` at execution time
- Use `ConfigurationCacheUtil.configureTaskForConfigurationCache()` for tasks that need project access

## 🧪 Testing

### Running Tests

```shell script
# All tests, plus plugin validation
./gradlew build

# Tests only
./gradlew test
```

CI runs `./gradlew build` on every pull request, and also applies the published plugin in
`compat/jdk17-consumer` using Gradle 9.0 on JDK 17, the oldest supported setup.

### Test Structure

Most tests are Gradle TestKit tests: they write a throwaway project to a temporary directory, apply
the plugin to it and run a real build. `ArmorTestFixture` builds those projects.

- **Functional tests** (`CodeArmorPluginFunctionalTest`): tasks, detection, tool wiring
- **Configuration cache tests** (`ConfigurationCacheTest`): a second run must reuse the cache
- **Check tiers and git hooks** (`CheckTiersTest`, `GitHooksTest`): what each tier runs; hooks installed, upgraded and removed only by their tasks, and a real `git push` through the hook
- **Code stats** (`CodeStatsTest`, `CodeStatsSettingsTest`): installs and real commits through the hooks,
  each in a throwaway home directory and git configuration, and who may choose which setting
- **Focused tests** (`GitVersionTest`, `BytecodeTargetTest`): a single behaviour each

The code stats scripts are linted in CI with ShellCheck, and the POSIX router is parsed with dash and
busybox. Run the same locally with `shellcheck -s sh _router` and `shellcheck -s bash codestats.sh`.

### Writing Tests

```kotlin
@Test
fun `logExclusionInfo reports default exclusions`(
    @TempDir dir: File,
) {
    ArmorTestFixture.writeProject(dir)

    val result = ArmorTestFixture.run(dir, "logExclusionInfo")

    assertContains(result.output, "CodeArmor Exclusion Information")
}
```


## 📚 Documentation

### Code Documentation

- **KDoc comments** for public APIs
- **Inline comments** for complex logic
- **README updates** for new features
- **Configuration examples** for new options

### Documentation Standards

```kotlin
/**
 * Detects the project type based on applied plugins and project structure.
 * 
 * @param project The Gradle project to analyze
 * @return The detected project type
 * @throws IllegalStateException if project type cannot be determined
 */
fun detectProjectType(project: Project): ProjectType
```


## 📤 Submitting Changes

### Pull Request Process

1. **Ensure tests pass**
    ```shell script
    ./gradlew check
    ```

2. **Update documentation** if needed

3. **Create a descriptive PR title**
    ```
    feat(jacoco): add support for custom coverage exclusions
    ```

4. **Fill out the PR template** with:
    - Description of changes
    - Testing performed
    - Breaking changes (if any)
    - Related issues

5. **Request review** from maintainers

### Review Process

- All PRs require at least one review
- Automated checks must pass
- Documentation must be updated for new features
- Breaking changes require discussion

### Merge Criteria

- ✅ All tests pass
- ✅ Code follows style guidelines
- ✅ Documentation is updated
- ✅ No breaking changes (unless approved)
- ✅ Reviewer approval

## 🚢 Releasing

For maintainers. Versions come from git tags, so there is no version to bump in the build files.

1. **Prepare the release:** in `CHANGELOG.md`, replace the version's `Unreleased` with the release date,
   and check the versions in the README. Merge that.
2. **Create the release in GitHub's web UI** (Releases → Draft a new release) with a **new tag** named
   exactly the version, without a `v`: for example `0.2.0-alpha`. `tag-validation.yml` accepts
   `{major}.{minor}.{patch}` with an optional `-alpha`, `-beta`, `-gamma`, `-dev` or `-rc.{N}` suffix.
3. **Fill in the details:** tick **Set as a pre-release** for alpha, beta and rc versions, and paste the
   version's section of `CHANGELOG.md` as the release notes.
4. **Publish the release.** `publish.yml` then publishes to Maven Central and the Gradle Plugin Portal.
   The run fails if either one fails.

Create the release in the web UI rather than pushing the tag. A pushed tag makes `tag-validation.yml`
create the release with the workflow's own token, and GitHub does not start other workflows for events
made with that token, so `publish.yml` would never run. Created in the UI, the release starts
`publish.yml`, and `tag-validation.yml` attaches the jar to it.

## 📄 License

By contributing to CodeArmor, you agree that your contributions will be licensed under the Apache License, Version 2.0.

### Contributor License Agreement

When you submit a pull request, you are agreeing to the following:

1. You have the right to submit the contribution
2. Your contribution is original work or properly attributed
3. You grant the project maintainers a perpetual, worldwide, non-exclusive license to your contribution
4. Your contribution will be distributed under the Apache License, Version 2.0

### License Headers

Add the following header to new files:

```kotlin
/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```


## 🎯 Development Priorities

Current focus areas where contributions are especially welcome:

### High Priority
- 🔒 **Veracode Integration**: Complete implementation once API access is available
- 🚀 **Performance Optimization**: Improve build times and memory usage
- 🔧 **Configuration Cache**: Enhance compatibility with more tools
- 📊 **Reporting**: Improve report generation and aggregation

### Medium Priority
- 🎨 **IDE Integration**: Better IntelliJ IDEA support
- 📱 **Android Support**: Add Android-specific configurations
- 🌐 **CI/CD Templates**: Provide templates for popular CI systems
- 📈 **Metrics**: Add build performance metrics

### Low Priority
- 🎭 **Custom Rules**: Support for custom quality rules
- 🔌 **Plugin Ecosystem**: Integration with additional tools
- 🏗️ **Build Variants**: Support for different build configurations

## 🆘 Getting Help

- **GitHub Issues**: For bug reports and feature requests
- **GitHub Discussions**: For questions and community discussions
- **Email**: [contact@coretide.dev](mailto:contact@coretide.dev) for security issues

## 🙏 Recognition

Contributors will be recognized in:
- Project README
- Release notes
- GitHub contributors page

Thank you for helping make CodeArmor better! 🛡️

---

**Developed by [Coretide](https://github.com/coretide)**
