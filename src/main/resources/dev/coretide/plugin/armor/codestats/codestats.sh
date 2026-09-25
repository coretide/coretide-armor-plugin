#!/usr/bin/env bash
# CODE-STATS-HOOKS-MANAGED
#
# Copyright 2025 Kushal Patel
# Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
#
# Installed by CodeArmor's armorCodeStatsInstall task.
#
# Turns one commit into a Code::Stats pulse and drains the delivery queue.
# The router invokes this detached from the commit, so nothing here can slow
# down or fail a commit. No token or repository path is embedded in this file.

set -u
unset CDPATH

# Overridable so the test suite can point somewhere that is deliberately not
# HTTPS, which the guard below then refuses -- no request is ever made, and no
# test can put XP on a real profile.
readonly CODESTATS_ENDPOINT="${CODESTATS_ENDPOINT:-https://codestats.net/api/my/pulses}"
# Resolve through symlinks without readlink -f, which BSD and older macOS lack.
_resolve_dir() {
  local d f link guard=0
  d="$(dirname "$1")"; f="$(basename "$1")"
  while [ -L "$d/$f" ] && [ "$guard" -lt 40 ]; do
    link="$(readlink "$d/$f")" || break
    case "$link" in
      /*) d="$(dirname "$link")" ;;
      *)  d="$(cd -- "$d" && cd -- "$(dirname "$link")" && pwd -P)" ;;
    esac
    f="$(basename "$link")"; guard=$((guard + 1))
  done
  (cd -- "$d" && pwd -P)
}
PKG_DIR="$(cd -- "$(_resolve_dir "${BASH_SOURCE[0]}")/.." && pwd -P)"
readonly PKG_DIR

# Repository paths matching these never produce a pulse. Editor scratch clones
# and vendored dependencies are not work worth counting.
readonly DEFAULT_IGNORES='
*/node_modules/*
*/.cache/*
*/.codex/*
*/.claude/*
*/.local/share/*
*/.tmp/*
*/vendor/*
'

config_home() {
  if [ -n "${XDG_CONFIG_HOME:-}" ]; then
    printf '%s\n' "$XDG_CONFIG_HOME"
  elif [ -n "${HOME:-}" ]; then
    printf '%s\n' "$HOME/.config"
  else
    return 1
  fi
}

state_home() {
  if [ -n "${XDG_STATE_HOME:-}" ]; then
    printf '%s\n' "$XDG_STATE_HOME"
  elif [ -n "${HOME:-}" ]; then
    printf '%s\n' "$HOME/.local/state"
  else
    return 1
  fi
}

# The first readable candidate wins.
token_path() {
  local cfg candidate
  cfg="$(config_home 2>/dev/null || true)"
  for candidate in \
    "${CODESTATS_TOKEN_FILE:-}" \
    "${cfg:+$cfg/code-stats-hooks/token}"; do
    [ -n "$candidate" ] || continue
    if [ -r "$candidate" ]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  return 1
}

# Identities that count as your own work.
#
# There is no reliable way to derive this. A developer may commit under several
# emails, a hostname-derived address like name@their-laptop.local, or a GitHub
# noreply address, and may have no global user.email at all -- while their
# checkouts are full of other people's history. So the set is configured
# explicitly, one entry per line, matched against the commit author's email or
# name. Globs are allowed, which is how a whole work domain is covered.
#
# With no identities configured, every locally created commit counts, which is
# the behaviour this had before the setting existed.
identity_files() {
  local cfg
  cfg="$(config_home 2>/dev/null || true)"
  printf '%s\n' "$PKG_DIR/identities"
  [ -n "$cfg" ] && printf '%s\n' "$cfg/code-stats-hooks/identities"
}

lower() { printf '%s' "$1" | LC_ALL=C tr '[:upper:]' '[:lower:]'; }

commit_is_mine() {
  local repo commit email name pattern file configured
  repo="$1"
  commit="$2"
  configured=0

  email="$(lower "$(git -C "$repo" log -1 --format='%ae' "$commit" 2>/dev/null)")"
  name="$(lower "$(git -C "$repo" log -1 --format='%an' "$commit" 2>/dev/null)")"

  while IFS= read -r file; do
    if [ -z "$file" ] || [ ! -r "$file" ]; then continue; fi
    while IFS= read -r pattern || [ -n "$pattern" ]; do
      case "$pattern" in ''|'#'*) continue ;; esac
      pattern="$(lower "$pattern")"
      configured=1
      # the pattern is deliberately a glob.
      # shellcheck disable=SC2254
      case "$email" in $pattern) return 0 ;; esac
      # the pattern is deliberately a glob.
      # shellcheck disable=SC2254
      case "$name" in $pattern) return 0 ;; esac
    done < "$file"
  done <<< "$(identity_files)"

  [ "$configured" -eq 0 ]
}

repo_ignored() {
  local repo pattern ignore_file cfg
  repo="$1"

  while IFS= read -r pattern; do
    [ -n "$pattern" ] || continue
    # the pattern is deliberately a glob.
    # shellcheck disable=SC2254
    case "$repo" in $pattern) return 0 ;; esac
  done <<< "$DEFAULT_IGNORES"

  cfg="$(config_home 2>/dev/null || true)"
  for ignore_file in "$PKG_DIR/ignore" "${cfg:+$cfg/code-stats-hooks/ignore}"; do
    if [ -z "$ignore_file" ] || [ ! -r "$ignore_file" ]; then continue; fi
    while IFS= read -r pattern; do
      case "$pattern" in ''|'#'*) continue ;; esac
      # the pattern is deliberately a glob.
      # shellcheck disable=SC2254
      case "$repo" in $pattern) return 0 ;; esac
    done < "$ignore_file"
  done
  return 1
}

language_for_path() {
  local path lower base
  path="$1"
  lower="$(printf '%s' "$path" | LC_ALL=C tr '[:upper:]' '[:lower:]')"
  base="${lower##*/}"

  case "$base" in
    dockerfile|dockerfile.*) printf '%s\n' "Docker"; return ;;
    makefile|gnumakefile) printf '%s\n' "Makefile"; return ;;
    jenkinsfile) printf '%s\n' "Groovy"; return ;;
    package-lock.json|composer.lock) printf '%s\n' "JSON"; return ;;
    gradlew|mvnw) printf '%s\n' "Shell"; return ;;
    .gitignore|.gitattributes|.gitmodules) printf '%s\n' "Git Config"; return ;;
    .editorconfig) printf '%s\n' "EditorConfig"; return ;;
    .env|.env.*) printf '%s\n' "Dotenv"; return ;;
  esac

  case "$lower" in
    *.gradle.kts) printf '%s\n' "Kotlin" ;;
    *.d.ts|*.ts|*.tsx) printf '%s\n' "TypeScript" ;;
    *.js|*.jsx|*.mjs|*.cjs) printf '%s\n' "JavaScript" ;;
    *.java) printf '%s\n' "Java" ;;
    *.kt|*.kts) printf '%s\n' "Kotlin" ;;
    *.groovy|*.gradle) printf '%s\n' "Groovy" ;;
    *.sh|*.bash|*.zsh|*.fish) printf '%s\n' "Shell" ;;
    *.py|*.pyw) printf '%s\n' "Python" ;;
    *.rb) printf '%s\n' "Ruby" ;;
    *.php) printf '%s\n' "PHP" ;;
    *.go) printf '%s\n' "Go" ;;
    *.rs) printf '%s\n' "Rust" ;;
    *.c|*.h) printf '%s\n' "C" ;;
    *.cc|*.cpp|*.cxx|*.hh|*.hpp|*.hxx) printf '%s\n' "C++" ;;
    *.cs) printf '%s\n' "C#" ;;
    *.m|*.mm) printf '%s\n' "Objective-C" ;;
    *.swift) printf '%s\n' "Swift" ;;
    *.dart) printf '%s\n' "Dart" ;;
    *.scala|*.sc) printf '%s\n' "Scala" ;;
    *.clj|*.cljs|*.cljc|*.edn) printf '%s\n' "Clojure" ;;
    *.ex|*.exs|*.heex|*.eex) printf '%s\n' "Elixir" ;;
    *.erl|*.hrl) printf '%s\n' "Erlang" ;;
    *.lua) printf '%s\n' "Lua" ;;
    *.r) printf '%s\n' "R" ;;
    *.sql) printf '%s\n' "SQL" ;;
    *.proto) printf '%s\n' "Protocol Buffers" ;;
    *.graphql|*.gql) printf '%s\n' "GraphQL" ;;
    *.tf|*.tfvars|*.hcl) printf '%s\n' "HCL" ;;
    *.html|*.htm) printf '%s\n' "HTML" ;;
    *.css) printf '%s\n' "CSS" ;;
    *.scss|*.sass) printf '%s\n' "SCSS" ;;
    *.less) printf '%s\n' "Less" ;;
    *.vue) printf '%s\n' "Vue" ;;
    *.svelte) printf '%s\n' "Svelte" ;;
    *.xml|*.xsd|*.xsl|*.xslt|*.svg) printf '%s\n' "XML" ;;
    *.json|*.jsonc) printf '%s\n' "JSON" ;;
    *.yaml|*.yml) printf '%s\n' "YAML" ;;
    *.toml) printf '%s\n' "TOML" ;;
    *.ini) printf '%s\n' "INI" ;;
    *.properties) printf '%s\n' "Properties" ;;
    *.md|*.markdown) printf '%s\n' "Markdown" ;;
    *.adoc|*.asciidoc) printf '%s\n' "AsciiDoc" ;;
    *) printf '%s\n' "Plain text" ;;
  esac
}

local_timestamp() {
  local raw offset prefix_length
  # One date call keeps the wall clock and offset on the same side of a DST
  # transition. Code::Stats requires a colon in the RFC 3339 offset.
  raw="$(date '+%Y-%m-%dT%H:%M:%S%z')" || return 1
  case "$raw" in
    *[+-][0-9][0-9][0-9][0-9])
      prefix_length=$((${#raw} - 5))
      offset="${raw:$prefix_length:5}"
      printf '%s%s:%s\n' "${raw:0:$prefix_length}" "${offset:0:3}" "${offset:3:2}"
      ;;
    *) return 1 ;;
  esac
}

queue_pulse() (
  local repo commit parents_line stats_file added deleted path old_path language xp
  local state_dir queue_dir payload_tmp payload epoch coded_at home
  repo="$1"
  commit="$2"

  repo_ignored "$repo" && return 0
  git -C "$repo" cat-file -e "${commit}^{commit}" 2>/dev/null || return 0

  # A merge's changes have normally already been counted by the commits it joins.
  parents_line="$(git -C "$repo" rev-list --parents -n 1 "$commit" 2>/dev/null)" || return 0
  # hashes are deliberately split on spaces here.
  # shellcheck disable=SC2086
  set -- $parents_line
  [ "$#" -le 2 ] || return 0

  # A cherry-pick or `git commit --author=...` creates a local commit carrying
  # somebody else's authorship. Those are their XP, not yours.
  commit_is_mine "$repo" "$commit" || return 0

  home="$(state_home)" || return 0
  state_dir="$home/code-stats-hooks"
  queue_dir="$state_dir/queue"
  umask 077
  mkdir -p "$queue_dir" 2>/dev/null || return 0
  chmod 700 "$state_dir" "$queue_dir" 2>/dev/null || true

  stats_file="$(mktemp "$state_dir/.stats.XXXXXX")" || return 0
  trap 'rm -f "$stats_file"' EXIT

  # --numstat gives a deterministic commit-delta approximation. With -z, even
  # paths containing whitespace/newlines are unambiguous. A rename has an empty
  # first path followed by its old and new names as separate NUL fields.
  while IFS=$'\t' read -r -d '' added deleted path; do
    if [ -z "$path" ]; then
      # A rename emits an empty path, then the old and new names as separate
      # NUL fields. The old name is read only to step past it.
      # shellcheck disable=SC2034
      IFS= read -r -d '' old_path || break
      IFS= read -r -d '' path || break
    fi

    case "$added" in
      ''|*[!0-9]*) continue ;;
    esac
    case "$deleted" in
      ''|*[!0-9]*) continue ;;
    esac
    xp=$((10#$added + 10#$deleted))
    [ "$xp" -gt 0 ] || continue
    language="$(language_for_path "$path")"
    printf '%s\t%s\n' "$language" "$xp" >> "$stats_file"
  done < <(git -C "$repo" diff-tree --root --numstat --find-renames \
    --no-commit-id -r -z "$commit" 2>/dev/null)

  [ -s "$stats_file" ] || return 0
  coded_at="$(local_timestamp)" || return 0
  epoch="$(date '+%s')" || return 0
  payload_tmp="$(mktemp "$queue_dir/.pulse.XXXXXX")" || return 0

  # Code::Stats currently treats any one XP item above 1000 as 1 XP. Emit
  # repeated entries of at most 1000 so large commits remain accurate.
  if ! awk -F '\t' -v coded_at="$coded_at" '
    { totals[$1] += $2 }
    END {
      printf "{\"coded_at\":\"%s\",\"xps\":[", coded_at
      separator = ""
      for (language in totals) {
        remaining = totals[language]
        while (remaining > 0) {
          chunk = remaining > 1000 ? 1000 : remaining
          printf "%s{\"language\":\"%s\",\"xp\":%d}", separator, language, chunk
          separator = ","
          remaining -= chunk
        }
      }
      print "]}"
    }
  ' "$stats_file" > "$payload_tmp"; then
    rm -f "$payload_tmp"
    return 0
  fi

  payload="$queue_dir/pulse-${epoch}-${commit}-$$.ready.json"
  chmod 600 "$payload_tmp" 2>/dev/null || true
  mv "$payload_tmp" "$payload" || { rm -f "$payload_tmp"; return 0; }
  printf '%s\n' "$payload"
)

send_queued_pulses() (
  local home state_dir queue_dir token_file token
  local stale claimed original payload http_status curl_status sent claim_epoch
  local now_epoch base created_part created_epoch claim_part claim_started age
  local ready_found delivery_failed

  home="$(state_home)" || return 0
  state_dir="$home/code-stats-hooks"
  queue_dir="$state_dir/queue"

  [ -d "$queue_dir" ] || return 0
  now_epoch="$(date '+%s')" || return 0

  # The creation and claim times live in each filename. Unlike mtime, the
  # original creation time survives the atomic rename used to claim a pulse.
  for stale in "$queue_dir"/pulse-*.ready.json "$queue_dir"/pulse-*.sending-*.json; do
    [ -f "$stale" ] || continue
    base="${stale##*/}"
    created_part="${base#pulse-}"
    created_epoch="${created_part%%-*}"
    case "$created_epoch" in
      ''|*[!0-9]*) continue ;;
    esac
    age=$((10#$now_epoch - 10#$created_epoch))
    # Code::Stats rejects pulses more than one week old.
    [ "$age" -le 604800 ] || rm -f "$stale"
  done

  # Claim files by atomically renaming them in the same directory. This lets
  # hooks from different repositories drain one shared queue without sending
  # the same pulse twice. Recover claims left by a killed worker after five
  # minutes; each HTTP attempt itself is bounded to eight seconds.
  for stale in "$queue_dir"/pulse-*.sending-*.json; do
    [ -f "$stale" ] || continue
    claim_part="${stale##*.sending-}"
    claim_started="${claim_part%%-*}"
    case "$claim_started" in
      ''|*[!0-9]*) continue ;;
    esac
    age=$((10#$now_epoch - 10#$claim_started))
    [ "$age" -gt 300 ] || continue
    original="${stale%%.sending-*}.ready.json"
    if [ -e "$original" ]; then
      rm -f "$stale"
    else
      mv "$stale" "$original" 2>/dev/null || true
    fi
  done

  ready_found=0
  for payload in "$queue_dir"/pulse-*.ready.json; do
    [ -f "$payload" ] || continue
    ready_found=1
    break
  done
  [ "$ready_found" -eq 1 ] || return 0

  token_file="$(token_path)" || return 1
  # A token file written without a trailing newline makes `read` fail even
  # though it read the token, which would leave every pulse queued.
  IFS= read -r token < "$token_file" || [ -n "$token" ] || return 1
  case "$token" in
    ''|*[!A-Za-z0-9._~-]*) return 1 ;;
  esac
  # Anything but HTTPS would put the token on the wire in clear text. Refuse
  # before curl is even reached; queued pulses simply stay queued.
  case "$CODESTATS_ENDPOINT" in
    https://*) ;;
    *) return 1 ;;
  esac
  command -v curl >/dev/null 2>&1 || return 0

  sent=0
  delivery_failed=0
  for payload in "$queue_dir"/pulse-*.ready.json; do
    # A concurrent worker may have claimed this entry between the glob and now.
    [ -f "$payload" ] || continue
    claim_epoch="$(date '+%s')" || break
    claimed="${payload%.ready.json}.sending-${claim_epoch}-$$.json"
    mv "$payload" "$claimed" 2>/dev/null || continue

    # --disable must be curl's first option: a user .curlrc must not weaken TLS,
    # enable redirects, or add a destination that receives the token header.
    http_status="$(curl --disable --silent --show-error --output /dev/null \
      --write-out '%{http_code}' --request POST --url "$CODESTATS_ENDPOINT" \
      --header 'Content-Type: application/json' \
      --data-binary "@$claimed" --connect-timeout 2 --max-time 8 \
      --proto '=https' \
      --config <(printf 'header = "X-API-Token: %s"\n' "$token") 2>/dev/null)"
    curl_status=$?

    if [ "$curl_status" -eq 0 ] && [ "$http_status" = "201" ]; then
      rm -f "$claimed"
    elif [ "$curl_status" -eq 0 ] && { [ "$http_status" = "400" ] || [ "$http_status" = "422" ]; }; then
      # These are permanent payload failures; retrying cannot repair them.
      rm -f "$claimed"
    else
      # Retain network, authentication, rate-limit and server failures. The
      # next commit retries them, up to the API's seven-day limit.
      mv "$claimed" "$payload" 2>/dev/null || true
      delivery_failed=1
      break
    fi

    sent=$((sent + 1))
    [ "$sent" -lt 10 ] || break
  done
  [ "$delivery_failed" -eq 0 ] || return 1
  for payload in "$queue_dir"/pulse-*.ready.json; do
    [ -f "$payload" ] && return 1
  done
  return 0
)

case "${1:-}" in
  pulse)
    repo="${2:-}"
    commit="${3:-}"
    [ -n "$repo" ] && [ -n "$commit" ] || exit 0
    command -v git >/dev/null 2>&1 || exit 0
    command -v awk >/dev/null 2>&1 || exit 0
    command -v mktemp >/dev/null 2>&1 || exit 0
    queue_pulse "$repo" "$commit" >/dev/null
    send_queued_pulses || true
    exit 0
    ;;
  flush)
    send_queued_pulses
    exit $?
    ;;
  token-path)
    token_path
    exit $?
    ;;
  *)
    printf 'Usage: %s {pulse REPO COMMIT|flush|token-path}\n' "${0##*/}" >&2
    exit 2
    ;;
esac
