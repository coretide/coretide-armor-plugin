/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.coretide.plugin.armor.config

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/** The git hooks `armorInstallGitHooks` installs. Nothing is installed until it runs. */
abstract class GitHooksConfig {
    /** Whether `armorInstallGitHooks` and `armorUninstallGitHooks` exist. */
    abstract val enabled: Property<Boolean>

    /** A pre-push hook that runs the `checks.prePush` tasks and blocks a push when they fail. */
    abstract val prePush: Property<Boolean>

    /**
     * A commit-msg hook that rejects a commit whose message is not a Conventional Commit: `type(scope)!: description`,
     * with a type from [conventionalCommitTypes]. Opt-in.
     */
    abstract val conventionalCommits: Property<Boolean>

    /** The types a Conventional Commit may start with. */
    abstract val conventionalCommitTypes: ListProperty<String>
}
