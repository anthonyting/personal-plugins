#!/usr/bin/env bash
set -euo pipefail

if [[ $# -gt 1 ]]; then
  echo "Usage: $0 [major.minor.patch]" >&2
  exit 2
fi

requested_version="${1:-}"
base_version="$(tr -d '[:space:]' < version.txt)"
version_pattern='^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$'
if [[ ! "$base_version" =~ $version_pattern ]]; then
  echo "version.txt must contain a major.minor.patch version." >&2
  exit 1
fi

tag_versions=()
while IFS= read -r tag; do
  tag_version="${tag#v}"
  if [[ "$tag_version" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
    tag_versions+=("$tag_version")
  fi
done < <(git tag --list 'v*')

if [[ ${#tag_versions[@]} -eq 0 ]]; then
  latest_tag_version=""
  latest_version="$base_version"
else
  latest_tag_version="$(printf '%s\n' "${tag_versions[@]}" | sort -V | tail -n 1)"
  latest_version="$(printf '%s\n%s\n' "$base_version" "$latest_tag_version" | sort -V | tail -n 1)"
fi

if [[ -n "$requested_version" ]]; then
  if [[ ! "$requested_version" =~ $version_pattern ]]; then
    echo "Requested version must be a major.minor.patch version." >&2
    exit 1
  fi

  if [[ "$requested_version" == "$latest_version" ]] ||
    [[ "$(printf '%s\n%s\n' "$requested_version" "$latest_version" | sort -V | tail -n 1)" != "$requested_version" ]]; then
    echo "Requested version $requested_version must be greater than the current version $latest_version." >&2
    exit 1
  fi

  next_version="$requested_version"
elif [[ -z "$latest_tag_version" ]]; then
  next_version="$base_version"
elif [[ "$latest_version" == "$base_version" && "$latest_tag_version" != "$base_version" ]]; then
  next_version="$base_version"
else
  if [[ ! "$latest_version" =~ $version_pattern ]]; then
    echo "Could not determine the latest major.minor.patch version." >&2
    exit 1
  fi

  major="${BASH_REMATCH[1]}"
  minor="${BASH_REMATCH[2]}"
  patch="${BASH_REMATCH[3]}"
  next_version="${major}.${minor}.$((patch + 1))"
fi

printf '%s\n' "$next_version" > version.txt

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  echo "version=$next_version" >> "$GITHUB_OUTPUT"
else
  echo "$next_version"
fi
