# Changelog

All notable changes to CodeArmor. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow [Semantic Versioning](https://semver.org/), and while in alpha, a minor version may break things.

## [0.5.0-alpha] - Unreleased

### Deprecated
- **`veracode`**, which 1.0.0 removes. CodeArmor never uploaded to Veracode itself: the switch ran the
  `veracodeUpload` task of a Veracode Gradle plugin the build applies. The build log and `armorInfo` say so, with the
  replacement: `checks { ci.add("veracodeUpload") }`. That runs the upload whether or not `VERACODE_USERNAME` and
  `VERACODE_PASSWORD` are set.

### Fixed
- **`checks { ci.add("…") }` dropped the default checks.** The tiers' defaults were Gradle conventions, which `add()`
  replaces, so `fullAnalysis` ran only the added task. `add()` now adds to the defaults, for `build` and `prePush`
  too; `ci = listOf(…)` still replaces them.
- **SonarQube did not import the OWASP Dependency Check findings.** CodeArmor passed the XML and HTML reports, under
  names the Dependency-Check SonarQube plugin no longer reads. It now passes the JSON report, as
  `sonar.dependencyCheck.jsonReportPath`.

### Removed
- Analysis properties SonarQube ignores: `sonar.coverage.minimum`, `sonar.duplicated_lines_density`, the
  `sonar.maintainability_rating`, `sonar.reliability_rating` and `sonar.security_rating` ratings, and
  `sonar.java.coveragePlugin`. Quality gates, thresholds and ratings are set on the SonarQube server, so these never
  had an effect.

## [0.4.0-alpha] - 2026-09-30

A release about adopting CodeArmor in existing code and running it on CI: `armorInfo` shows what it checks and
what needs attention, diff coverage measures what a pull request adds, a SpotBugs baseline accepts the findings
already there, and gitleaks, the job summary and the dependency graph join the CI side. It also fixes OWASP
Dependency Check, which failed before scanning with the default settings, and scanned CodeArmor's own tools when it
did run; SonarQube ignoring `SONAR_TOKEN`; and the default SpotBugs filter skipping classes that merely have `Test` or
`Config` in their names. Read [Upgrading from 0.3.x](#upgrading-from-03x) first.

### ⚠️ Behaviour changes
- **OWASP Dependency Check runs with the default settings.** It failed before it scanned (see Fixed). `fullAnalysis`
  now downloads the NVD data on a machine that has none, which is slow without an API key, and fails the build on a
  finding at `owaspFailBuildOnCVSS` (9.0) or above. CodeArmor suppresses no findings itself any more.
- **SonarQube runs only once it is configured.** `fullAnalysis` includes `sonar` by default only when a server or
  a token is set: `sonarHostUrl` or `sonarToken`, the `SONAR_HOST_URL` or `SONAR_TOKEN` environment variables, or
  the `sonar.host.url` or `sonar.token` system properties. Without one, it leaves SonarQube out and says why,
  instead of failing against a server that is not there. Listing `sonar` in `checks.ci` still runs it, as a
  server set only in the build's own `sonar { }` block needs.
- **`sonarHostUrl` no longer defaults to `http://localhost:9000`.** Left unset, the SonarScanner picks its own
  default, SonarQube Cloud. A local server needs `sonarHostUrl = "http://localhost:9000"`.
- **`build` measures diff coverage.** `armorDiffCoverage` joins the build tier. It reports and never fails, unless
  `diffCoverageMinimum` is set; `diffCoverage = false` leaves it out.
- **OWASP Dependency Check scans only `runtimeClasspath`.** See Fixed: a vulnerability in a test-only or
  compile-only dependency, or in a build tool, no longer fails the build.
- **Narrower default SpotBugs filter.** It left out any class with `Test` or `Config` anywhere in its name, such as
  `TestimonialService`, `AbTestRouter`, `ConfigParser` or `ConfigurationLoader`. It now leaves out only names ending
  in `Test`, `Tests`, `IT` or `TestCase`, and in `Config` or `Configuration`, with the classes nested in them, as the
  coverage exclusions have since 0.3.0. SpotBugs can report findings in classes it skipped before.

### Added
**Setup and diagnostics**
- **`armorInfo`** prints what CodeArmor checks in a project: its version, what `build`, `fullAnalysis` and the
  pre-push hook run, and each tool, switched on or off, with its version and settings. It ends with what needs
  attention: SonarQube with no server, OWASP without an NVD API key, git hooks not installed, `secretScan` without
  gitleaks, Veracode without its plugin or credentials. In a multi-module build, each module shows its own.
- **Tool versions.** `toolVersions { jacoco; pitest; errorProne; nullAway; archUnit }` runs another version of a
  tool than the one CodeArmor was tested with, such as a newer release with a fix. SpotBugs keeps its own
  `spotbugs { toolVersion }`.

**Coverage and tests**
- **Diff coverage.** `armorDiffCoverage`, in the build tier, measures the share of the lines changed since the base
  branch that tests run, from the JaCoCo or Kover report, and names the untested ones. It also appears in the
  summary. It only reports until `diffCoverageMinimum` is set. The base branch is the pull request's target on
  common CI services, or `origin/HEAD`, `main` or `master`; `diffCoverageBase` overrides it. `diffCoverage = false`
  turns it off.
- **Integration tests.** `integrationTests = true` adds an `integrationTest` source set and task, run by `build`
  after the unit tests, with the unit tests' libraries and runner. Their coverage counts, and the summary has a line
  for them.

**Static analysis**
- **SpotBugs baseline.** `./gradlew armorSpotbugsBaseline` writes the findings already in the code to
  `config/spotbugs/baseline.xml`, and from then on `spotbugsMain` fails on new findings only. SpotBugs matches a
  finding by a hash that ignores line numbers, so edits around it keep it accepted. `spotbugs { baselineFile }`
  moves the file.
- **detekt with type resolution.** `detektTypeResolution = true` runs `detektMain` in place of `detekt`: the main
  sources against their compile classpath, so the rules that need types run too. Its baseline is
  `detekt-baseline-main.xml`, from `./gradlew detektBaselineMain`.

**CI**
- **Secret scanning in CI.** With `secretScan = true`, `fullAnalysis` also runs `armorSecretScan`: gitleaks over
  the whole history, failing on a secret, with a SARIF report that `armorSarifReport` gathers for code scanning. A
  multi-module build scans once, from the root.
- **GitHub Actions job summary.** On GitHub Actions, `armorReport` writes its summary to the job summary too.
- **Dependency graph and Dependabot scaffolding.** The workflow `armorScaffoldProject` writes gains a job that
  submits the resolved dependencies to GitHub's dependency graph, and it also writes `.github/dependabot.yml`, for
  weekly Gradle and GitHub Actions updates.

### Fixed
- **OWASP Dependency Check never ran with the default settings** (since 0.2.0-alpha). Without `owaspSuppressionFile`,
  CodeArmor gave it a default suppression file that a task was to write, but nothing ran that task first, so
  `dependencyCheckAnalyze`, and with it `fullAnalysis`, failed before scanning ("Querying the mapped value of …
  generateOwaspSuppressionFile … is not supported"). Past that, `owaspAutoUpdate` defaulted to `false`, so on a
  machine without NVD data, such as a fresh CI runner, it failed with "Autoupdate is disabled and the database does
  not exist". CodeArmor now passes a suppression file only when the build names one, and `owaspAutoUpdate` defaults
  to `true`, as in dependency-check itself.
- **The default OWASP suppressions.** That default file suppressed findings in Spring Boot starters, JUnit and
  Mockito, and dependency-check's own schema rejects it. A security check's defaults must hide nothing: CodeArmor now
  suppresses nothing itself, and `armorScaffoldConfigs` writes a file with no rules, only an example, to start from.
- **SonarQube ignored `SONAR_TOKEN`.** CodeArmor always set `sonar.token`, to an empty value when `sonarToken` was
  unset, and the SonarScanner reads the environment variable only while the property is unset. The usual way to
  pass a token on CI now works.
- **OWASP Dependency Check scanned CodeArmor's own tools.** With no configurations named, it scanned every one,
  SpotBugs', detekt's, PIT's, Error Prone's and JaCoCo's included, so a vulnerability in a tool could fail the
  build. It now scans `runtimeClasspath`, what ships, as the SBOM does.
- **OWASP settings leaked through the Gradle daemon.** The analyzer switches and NVD settings were JVM-wide system
  properties: in a multi-module build the last project configured won, and they outlived the build. They are now
  set on the dependency-check extension.
- **The `spotbugs { }` block failed in Groovy build scripts** ("Could not set unknown property"). It takes a
  Gradle `Action` now.
- **Log level after a configuration cache hit.** A build that reused the configuration cache in a new daemon
  logged at `VERBOSE`; it now falls back to the default, `ESSENTIAL`.

### Changed
- The README describes Veracode as what it is: `veracode = true` runs the `veracodeUpload` task of a Veracode
  Gradle plugin you apply, rather than an integration "in development".

### Upgrading from 0.3.x
1. **SonarQube.** If `fullAnalysis` sent the analysis to a local server through the old default, set
   `sonarHostUrl = "http://localhost:9000"`. On CI, pass `SONAR_TOKEN`, and `SONAR_HOST_URL` for your own server,
   as secrets; the token no longer needs to go through `sonarToken`. Without either, `fullAnalysis` skips
   SonarQube and says so.
2. **Diff coverage on CI.** It compares with the pull request's target branch, so check out the full history: on
   GitHub Actions, `actions/checkout` with `fetch-depth: 0`, as the scaffolded workflow does. Without it, it says so
   and passes.
3. **OWASP Dependency Check.** It now runs in `fullAnalysis` with the default settings. Set `NVD_API_KEY` on CI:
   without it, the first download of the NVD data is very slow. Findings in test-only and compile-only dependencies,
   and in those of SpotBugs, detekt, PIT, Error Prone or JaCoCo, no longer appear, so suppressions you added for them
   can go. A suppression file scaffolded earlier with `armorScaffoldConfigs` holds rules dependency-check rejects:
   delete them, or the file, and scaffold it again.
4. **SpotBugs findings in classes it used to skip.** Classes whose names only contain `Test` or `Config` are now
   analysed. Fix what it finds, or accept it with `./gradlew armorSpotbugsBaseline`. A filter scaffolded earlier with
   `armorScaffoldConfigs` keeps the old patterns: narrow them there, or delete the file and scaffold it again.
5. **Adopting the new checks.** `./gradlew armorSpotbugsBaseline` accepts the SpotBugs findings already in the code;
   with `detektTypeResolution = true`, `./gradlew detektBaselineMain` does the same for detekt. `./gradlew armorInfo`
   lists what else needs attention.

## [0.3.0-alpha] - 2026-09-29

A large release: tests and coverage, static analysis, dependency health, library API checks, commit hooks, code
scanning and a summary page. Most of it is opt-in, and what is on by default reports rather than fails. A few
defaults do change what `./gradlew build` does, so read [Upgrading from 0.2.x](#upgrading-from-02x) first.

### ⚠️ Behaviour changes
- **Kotlin projects run detekt in `./gradlew build`.** Findings fail the build. Run `./gradlew detektBaseline`
  once to accept the findings already in the code, or set `detekt = false`.
- **Narrower default coverage exclusions.** The defaults now leave out only entry points (`*Application`),
  configuration (`*Config`, `*Configuration`) and generated code (`generated` packages, MapStruct's
  `*MapperImpl`, JPA's `*_` metamodel classes), matched as a whole class-name suffix or package. They used to
  match anywhere in a name: `Error` left out `ErrorHandler`, `Config` left out `ConfigParser`, and `model`,
  `util` or `mapper` left out whole packages. Coverage can drop, and coverage verification can fail.
- **Failed tests are retried on CI.** With `CI=true`, a failed test runs up to twice more; one that then passes
  no longer fails the build, and is listed as flaky. `flakyTestRetries = 0` turns this off.
- **`fullAnalysis` does more.** Its CI tier adds `dependencyUpdates`, and `armorLicenseReport` with the SBOM.
  Neither fails the build unless `forbiddenLicenses` is set.
- **`build` ends with a summary.** `codeQuality` and `fullAnalysis` are finalized by `armorReport`, which prints
  a few lines and writes `build/reports/codearmor/index.html`.
- **Test tasks keep the framework they chose.** CodeArmor still moves test tasks on Gradle's default runner
  (JUnit 4) to the JUnit Platform, but no longer switches TestNG tasks, and no longer resets JUnit Platform
  settings. The new `junitPlatform = false` keeps JUnit 4.
- **Quieter test output.** Test tasks no longer print every passing test and all test output; failures are
  still shown with their full stack trace.
- **Applications are recognised by their plugins** (`application`, Spring Boot, Quarkus, Micronaut, Ktor),
  no longer by a project name ending in `-app` or `-service`. Only applications get resource processing.
- **`sonarJavaVersion` has no default.** The SonarScanner reads the Java version from the project; set it to
  override.
- **SpotBugs and OWASP Dependency Check also write SARIF.** `spotbugs { sarifReports = false }` turns SpotBugs'
  off.

### Added
**Tests and coverage**
- **Flaky and slow tests:** on CI, a failed test is retried (`flakyTestRetries`, default 2) and, if it then
  passes, named as flaky instead of failing the build. After every test run, the slowest tests over
  `slowTestThresholdMillis` (default 2s) are listed.
- **Kover** (`kover = true`, opt-in): coverage for Kotlin projects with Kover instead of JaCoCo, with the same
  thresholds and exclusions; SonarQube reads its report.
- **Mutation testing** (`mutationTesting = true`, opt-in): `./gradlew pitest` runs PIT on the project's own
  packages, with an optional `mutationThreshold`.
- **Combined reports for multi-module builds:** `allCodeQuality` also writes one coverage report
  (`testCodeCoverageReport`) and one test report (`testAggregateTestReport`) for the whole build on the root
  project. Coverage counts tests in one module that exercise another's code, and each module's SonarQube
  analysis reads it.

**Static analysis**
- **detekt for Kotlin projects:** detekt 2.0.0-alpha.6 joins the local tier in projects that apply the Kotlin
  JVM plugin, with HTML, SARIF and checkstyle reports, a baseline (`detektBaseline`), and its findings
  imported into SonarQube. It is left off, with a message, when the project's Kotlin release is newer than
  detekt can read, and a project that applies detekt 1.x keeps it.
- **`strictCompilation`** (opt-in): compiler warnings in production code fail the build. Java gets
  `-Xlint:all -Werror`; Kotlin gets `allWarningsAsErrors` and `-Xjsr305=strict`, and Kotlin libraries get
  explicit API mode.
- **Error Prone and NullAway** (`errorProne = true`, `nullAway = true`, opt-in): Error Prone checks the Java
  sources as they compile, and NullAway fails the build where production code may dereference null. Both need
  a JDK 21 compiler and are skipped, with a warning, on an older one.
- **Architecture tests** (`architectureTests = true`, opt-in): ArchUnit on the test classpath.
  `armorScaffoldArchitectureTests` writes a first `ArchitectureTest` in Java or Kotlin, covering package
  cycles, field injection, standard streams, generic exceptions and `java.util.logging`.

**Dependencies**
- **Dependency health**, in the CI tier:
  - `dependencyUpdates` lists newer releases; pre-releases are offered only for a dependency already on one.
  - `cyclonedxBom` writes a CycloneDX SBOM of `runtimeClasspath`.
  - `armorLicenseReport` groups the SBOM's dependencies by licence, and fails on `forbiddenLicenses`.
  - `dependencyAnalysis = true` (opt-in) adds the dependency-analysis plugin's `projectHealth` report.

**Libraries**
- **API checks** (opt-in, libraries only):
  - `apiBaseline = "1.4.0"` adds `armorApiCheck` to the CI tier. It runs japicmp against that release and
    fails on binary incompatible changes.
  - `kotlinAbiValidation = true` switches on the Kotlin Gradle plugin's ABI validation (Kotlin 2.2+) and adds
    `checkLegacyAbi` to the build tier.

**Git hooks** (opt-in, installed by `armorInstallGitHooks`)
- `conventionalCommits = true` adds a commit-msg hook that rejects messages that are not Conventional Commits,
  with the types in `conventionalCommitTypes`.
- `secretScan = true` adds a pre-commit hook that blocks a commit gitleaks finds a secret in. gitleaks is not
  bundled; without it, the hook warns and lets the commit through.

**Reports and CI**
- **Summary page:** `armorReport` writes `build/reports/codearmor/index.html` and a few console lines: tests,
  coverage, SpotBugs, detekt, PIT, OWASP, dependency updates, licences and the API check, each linked to its
  own report.
- **Code scanning:** `armorSarifReport` gathers the SpotBugs, detekt and OWASP SARIF reports of every project
  into `build/reports/sarif/`, each run in a category of its own, as GitHub code scanning requires.
- **`armorScaffoldProject`** writes an `.editorconfig` and a GitHub Actions workflow that runs the checks and
  uploads the SARIF to code scanning. Existing files are never overwritten.

### Fixed
- SonarQube never imported SpotBugs findings: it was pointed at `build/reports/spotbugs/main.xml`, while
  SpotBugs writes `spotbugsMain.xml`.
- SonarQube only saw `src/main/java` / `src/main/kotlin`: CodeArmor overrode the sources, tests and class
  directories the SonarScanner reads from the source sets, so generated and custom source sets were not
  analysed. It also set `sonar.kotlin.*` keys the scanner does not read.
- Mixed Java and Kotlin applications got no resource processing.

### Changed
- **Configuration cache support is declared** in the plugin's metadata, so the Gradle Plugin Portal
  lists CodeArmor as compatible with it.

### Project
- The CI and release workflows use the Node 24 releases of their actions, replacing the deprecated
  Node 20 ones. `setup-gradle` keeps the open-source basic cache provider.

### Upgrading from 0.2.x
1. **Kotlin projects:** `./gradlew build` now runs detekt. Run `./gradlew detektBaseline` once to accept the
   findings already in the code, or set `detekt = false`.
2. **Coverage:** classes the old defaults hid now count. If `build` fails coverage verification, or coverage
   drops in SonarQube, add tests, lower `coverageMinimum` / `coverageClassMinimum`, or bring back the old list.
   A build's own `coverageExclusions` match the way the old defaults did, anywhere in a name or as a package:
   ```kotlin
   codeArmor {
       coverageExclusions.addAll(
           listOf(
               "annotation", "model", "dto", "entity", "entities", "mapper", "util", "utils", "helper", "helpers",
               "config", "Application", "Config", "Configuration", "Repository", "generated", "Test", "Mock",
               "Stubs", "Dummy", "Fake", "Abstract", "Base", "Exception", "Error", "logging",
           ),
       )
   }
   ```
3. **CI:** failed tests are retried, and a test that passes on a retry is reported as flaky instead of
   failing the build. Set `flakyTestRetries = 0` to keep one run.
4. **`fullAnalysis`** also runs `dependencyUpdates` and `armorLicenseReport`. To keep the CI tier as it was,
   set `dependencyUpdates = false` and `sbom = false`, or list `checks.ci` yourself.
5. **New hooks:** after turning on `conventionalCommits` or `secretScan`, run `./gradlew armorInstallGitHooks`
   in each clone.

## [0.2.0-alpha] - 2026-09-25

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

[0.5.0-alpha]: https://github.com/coretide/coretide-armor-plugin/compare/0.4.0-alpha...HEAD
[0.4.0-alpha]: https://github.com/coretide/coretide-armor-plugin/compare/0.3.0-alpha...0.4.0-alpha
[0.3.0-alpha]: https://github.com/coretide/coretide-armor-plugin/compare/0.2.0-alpha...0.3.0-alpha
[0.2.0-alpha]: https://github.com/coretide/coretide-armor-plugin/compare/0.1.4-alpha...0.2.0-alpha
[0.1.4-alpha]: https://github.com/coretide/coretide-armor-plugin/compare/0.1.3-alpha...0.1.4-alpha
[0.1.3-alpha]: https://github.com/coretide/coretide-armor-plugin/compare/0.1.2-alpha...0.1.3-alpha
[0.1.2-alpha]: https://github.com/coretide/coretide-armor-plugin/compare/0.1.1-alpha...0.1.2-alpha
[0.1.1-alpha]: https://github.com/coretide/coretide-armor-plugin/compare/0.1.0-alpha...0.1.1-alpha
[0.1.0-alpha]: https://github.com/coretide/coretide-armor-plugin/releases/tag/0.1.0-alpha
