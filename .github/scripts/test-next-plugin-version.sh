#!/usr/bin/env bash
set -euo pipefail

script_path="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/next-plugin-version.sh"
test_root="$(mktemp -d)"
trap 'rm -rf "$test_root"' EXIT

create_repository() {
  local repository="$1"
  local base_version="$2"
  local release_tag="${3:-}"

  mkdir -p "$repository/.github/scripts"
  cp "$script_path" "$repository/.github/scripts/next-plugin-version.sh"
  printf '%s\n' "$base_version" > "$repository/version.txt"
  git -C "$repository" init -q
  git -C "$repository" config user.name "Version test"
  git -C "$repository" config user.email "version-test@example.invalid"
  git -C "$repository" add version.txt
  git -C "$repository" commit -qm "Set base version"
  if [[ -n "$release_tag" ]]; then
    git -C "$repository" tag "v$release_tag"
  fi
}

assert_release_version() {
  local repository="$1"
  local expected="$2"
  shift 2
  local actual
  actual="$(cd "$repository" && bash .github/scripts/next-plugin-version.sh "$@")"
  if [[ "$actual" != "$expected" || "$(tr -d '[:space:]' < "$repository/version.txt")" != "$expected" ]]; then
    echo "Expected release version and version.txt to be $expected; got $actual." >&2
    exit 1
  fi
}

assert_rejected_without_changing_version() {
  local repository="$1"
  local original_version
  original_version="$(tr -d '[:space:]' < "$repository/version.txt")"
  shift
  if (cd "$repository" && bash .github/scripts/next-plugin-version.sh "$@") >/dev/null 2>&1; then
    echo "Expected version request '$*' to be rejected." >&2
    exit 1
  fi
  if [[ "$(tr -d '[:space:]' < "$repository/version.txt")" != "$original_version" ]]; then
    echo "Rejected version request changed version.txt." >&2
    exit 1
  fi
}

auto_repository="$test_root/auto"
create_repository "$auto_repository" "0.1.0" "0.1.0"
assert_release_version "$auto_repository" "0.1.1"

requested_repository="$test_root/requested"
create_repository "$requested_repository" "0.1.0" "0.1.0"
assert_release_version "$requested_repository" "0.2.0" "0.2.0"
assert_rejected_without_changing_version "$requested_repository" "0.2.0"
assert_rejected_without_changing_version "$requested_repository" "0.1.0"
assert_rejected_without_changing_version "$requested_repository" "v0.3.0"
assert_rejected_without_changing_version "$requested_repository" "0.3.0" "extra-argument"

version_floor_repository="$test_root/version-floor"
create_repository "$version_floor_repository" "0.2.0" "0.1.0"
assert_rejected_without_changing_version "$version_floor_repository" "0.1.1"

initial_repository="$test_root/initial"
create_repository "$initial_repository" "0.1.0"
assert_release_version "$initial_repository" "0.1.0"

echo "Plugin version script tests passed."
