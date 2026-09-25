# Changelog

All notable changes to CodeArmor. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow [Semantic Versioning](https://semver.org/), and while in alpha, a minor version may break things.

## [0.2.0-alpha] - Unreleased

The first release built for Gradle 9. It changes when checks and git hooks run, so read
[Upgrading from 0.1.x](#upgrading-from-01x) first.

### ⚠️ Breaking changes
- **Gradle 9.0+ and JDK 17+ are required.** 0.1.4-alpha was built for Java 11 against Gradle 8.14. The
  plugin is now built with JDK 21 and compiled for Java 17, so a Gradle 9 build can run on JDK 17 or newer.
- **Git hooks are installed only on request.** 0.1.x wrote `.git/hooks/pre-push` whenever Gradle
  configured a build, on every machine and CI agent. Run `./gradlew armorInstallGitHooks` instead.
- **The pre-push hook blocks a failing push, and runs the basic checks.** By default that is `quickBuild`
  (compile and unit tests), instead of `test` followed by a non-blocking `fullAnalysis`.
- **`./gradlew build` runs the local checks.** `check` now depends on `codeQuality` (SpotBugs, the JaCoCo
  report and coverage verification), so a build below `coverageMinimum` or `coverageClassMinimum` fails.
- **`codeQuality` no longer runs SonarQube.** It needs a server, so it moved to `fullAnalysis`.
- **Default tool configs are generated under `build/codearmor/`,** no longer written into `config/` while
  configuring. `./gradlew armorScaffoldConfigs` copies them into `config/` for customizing.
- **`sonarJavaVersion` defaults to `21`** instead of `11`.

### Added
- **Check tiers:** `codeArmor { checks { prePush / build / ci } }` sets which tasks the pre-push hook,
  `codeQuality` (and so `build`) and `fullAnalysis` run. The build and CI defaults follow the tool switches.
- **`armorInstallGitHooks` / `armorUninstallGitHooks`.** They never overwrite a hook CodeArmor did not
  write, install into the repository's own hooks directory (shared by linked worktrees), run from the
  Gradle root when it is a subdirectory of the repository, and upgrade or remove the hooks 0.1.x wrote.
- **Code stats:** optional reporting of commit activity to [Code::Stats](https://codestats.net), off by
  default. A build script can enable it for its repository; only a developer's own settings can enable
  it machine-wide. Existing hooks keep running, an existing standalone code-stats-hooks install is left
  alone, and nothing is installed on CI. Tasks: `armorCodeStatsInstall`, `armorCodeStatsUninstall`,
  `armorCodeStatsStatus`, `armorCodeStatsFlush`.
- **`armorScaffoldConfigs`,** which writes the default SpotBugs and OWASP configs into `config/`, never
  overwriting.
- **Warnings for Veracode without a Veracode plugin, and for `core.hooksPath` pointing elsewhere.**
  With `veracode = true` but no plugin providing `veracodeUpload`, CodeArmor warns. When
  `core.hooksPath` points somewhere that bypasses the pre-push hook, `armorInstallGitHooks` warns.

### Changed
- **Configuration cache:** CodeArmor no longer invalidates the cache by writing into `config/` or
  `.git/hooks` while configuring.
- **Tool versions:**
  - SpotBugs 4.10.3
  - JaCoCo 0.8.15
  - SpotBugs Gradle plugin 6.5.9
  - SonarScanner for Gradle 7.3.1.8318
  - OWASP dependency-check-gradle 12.2.2
- **Java-only checks:** the check tiers are only created in projects with a Java plugin.

### Fixed
- Versions from git tags lost every `v`, not just the leading one: `v1.2.0-dev` became `1.2.0-de`.
- `veracode = true` with Veracode credentials set failed `fullAnalysis` ("Task with name 'veracodeUpload'
  not found").
- Applying CodeArmor to a project without a Java plugin, including a non-Java module of a multi-module
  build, failed configuration ("Task with name 'jacocoTestReport' not found").
- `validatePlugins` failed on `LogExclusionInfoTask`.
- Gradle system properties set during a build could leak into later builds in the same daemon.
- The documented plugin ID was wrong: it is `dev.coretide.plugin.armor`.

### Removed
- The unused `config/checkstyle/checkstyle.xml` (Checkstyle support was removed in 0.1.4-alpha).
- `logExclusionInfo`'s `--verbose` option, which was never read.

### Project
- **CI** runs on every pull request:
  - the build and tests on Linux, macOS and Windows;
  - a check that the published plugin loads in Gradle 9.0 on JDK 17;
  - ShellCheck on the code stats scripts.
- `publishToMavenLocal` no longer needs a GPG key; only releases are signed.
- The publish workflow fails when publishing fails, instead of reporting success.

### Upgrading from 0.1.x
1. Use Gradle 9.0 or newer, running on JDK 17 or newer.
2. Run `./gradlew armorInstallGitHooks` once per clone. It replaces 0.1.x's pre-push hook and removes its
   obsolete pre-commit hook.
3. If `./gradlew build` now fails on coverage, lower `coverageMinimum` / `coverageClassMinimum`, or
   leave `jacocoTestCoverageVerification` out of `checks.build`.
4. Run SonarQube through `fullAnalysis`, not `codeQuality`.
5. If you customized the generated tool configs, run `./gradlew armorScaffoldConfigs` and point
   `spotbugs { excludeFile }` and `owaspSuppressionFile` at the files in `config/`.

## [0.1.4-alpha] - 2025-09-29
### Removed
- Spotless and Checkstyle support.

### Changed
- Simplified task creation.

## [0.1.3-alpha] - 2025-07-21
### Added
- A lightweight `validateCodeStyle` task and a pre-commit hook.

## [0.1.2-alpha] - 2025-07-21
### Added
- Line-ending configuration, for cross-platform consistency.

## [0.1.1-alpha] - 2025-07-20
### Changed
- Packages restructured under `dev.coretide.plugin.armor` (`util`, `task`, `configurator`).
- Code formatting, configuration controls and git-derived versioning improved.

## [0.1.0-alpha] - 2025-07-18
### Added
- First release: JaCoCo, SpotBugs, Spotless, Checkstyle, OWASP Dependency Check, SonarQube and Veracode
  wiring, project type detection, git hooks and git-derived versions, published to Maven Central and
  the Gradle Plugin Portal.

[0.2.0-alpha]: https://github.com/coretide/coretide-armor-plugin/compare/0.1.4-alpha...HEAD
[0.1.4-alpha]: https://github.com/coretide/coretide-armor-plugin/compare/0.1.3-alpha...0.1.4-alpha
[0.1.3-alpha]: https://github.com/coretide/coretide-armor-plugin/compare/0.1.2-alpha...0.1.3-alpha
[0.1.2-alpha]: https://github.com/coretide/coretide-armor-plugin/compare/0.1.1-alpha...0.1.2-alpha
[0.1.1-alpha]: https://github.com/coretide/coretide-armor-plugin/compare/0.1.0-alpha...0.1.1-alpha
[0.1.0-alpha]: https://github.com/coretide/coretide-armor-plugin/releases/tag/0.1.0-alpha
