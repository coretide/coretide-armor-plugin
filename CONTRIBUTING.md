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

- **Java 11** or higher
- **Gradle 7.0** or higher
- **Git** for version control
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
   
   # Publish to local Maven repository for testing
   ./gradlew publishToMavenLocal
   ```

3. **Test the plugin in a sample project**
   ```bash
   # Create a test project
   mkdir test-project
   cd test-project
   
   # Add to build.gradle.kts
   plugins {
       id("dev.coretide.armor") version "0.1.0-alpha"
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
   ./gradlew test
   ./gradlew integrationTest
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
- **Formatting**: Use the project's Spotless configuration (`./gradlew spotlessApply`)

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
fix(spotless): resolve configuration cache compatibility issue
docs(readme): update installation instructions
```

### Code Structure

```
src/
├── main/kotlin/dev/coretide/armor/
│   ├── CodeArmorPlugin.kt           # Main plugin class
│   ├── CodeArmorExtension.kt        # Configuration DSL
│   ├── ProjectType.kt               # Project type enum
│   ├── configurators/               # Tool configuration
│   │   ├── JacocoConfigurator.kt
│   │   ├── SpotlessConfigurator.kt
│   │   └── ...
│   ├── tasks/                       # Custom task creation
│   │   ├── TaskCreator.kt
│   │   └── MultiModuleTaskCreator.kt
│   ├── utils/                       # Utility classes
│   │   ├── ProjectDetector.kt
│   │   ├── ConfigurationCacheUtils.kt
│   │   └── ...
│   └── git/                         # Git integration
│       ├── GitHooksManager.kt
│       └── VersionManager.kt
└── test/                            # Test classes
```


### Adding New Features

1. **Create a configurator** for new tools in `configurators/`
2. **Update the extension** to include configuration options
3. **Add project detection** logic if needed
4. **Create custom tasks** if required
5. **Update documentation** (README, KDoc comments)
6. **Add comprehensive tests**

### Configuration Cache Compatibility

All new code must be compatible with Gradle's configuration cache:

- Use `Provider<T>` for lazy evaluation
- Avoid accessing `Project` at execution time
- Use `ConfigurationCacheUtils.configureTaskForConfigurationCache()` for tasks that need project access

## 🧪 Testing

### Running Tests

```shell script
# Unit tests
./gradlew test

# Integration tests
./gradlew integrationTest

# Test with configuration cache
./gradlew test --configuration-cache

# Test in different environments
./gradlew test -PjavaVersion=11
./gradlew test -PjavaVersion=17
```


### Test Structure

- **Unit tests**: Test individual components in isolation
- **Integration tests**: Test plugin behavior in real projects
- **Functional tests**: Test complete workflows

### Writing Tests

```kotlin
@Test
fun `should detect Kotlin application project`() {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("org.jetbrains.kotlin.jvm")
    project.plugins.apply("application")
    
    val result = ProjectDetector.detectProjectType(project)
    
    assertThat(result).isEqualTo(ProjectType.KOTLIN_APPLICATION)
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
    feat(spotless): add support for custom KtLint rules
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
