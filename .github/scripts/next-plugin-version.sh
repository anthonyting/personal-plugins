#!/usr/bin/env bash
set -euo pipefail

base_version="$(tr -d '[:space:]' < version.txt)"
if [[ ! "$base_version" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
  echo "version.txt must contain a major.minor.patch SemVer version." >&2
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
  next_version="$base_version"
else
  latest_tag_version="$(printf '%s\n' "${tag_versions[@]}" | sort -V | tail -n 1)"
  latest_version="$(printf '%s\n%s\n' "$base_version" "$latest_tag_version" | sort -V | tail -n 1)"
  if [[ "$latest_version" == "$base_version" && "$latest_tag_version" != "$base_version" ]]; then
    next_version="$base_version"
  else
    if [[ ! "$latest_version" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
      echo "Could not determine the latest major.minor.patch version." >&2
      exit 1
    fi

    major="${BASH_REMATCH[1]}"
    minor="${BASH_REMATCH[2]}"
    patch="${BASH_REMATCH[3]}"
    next_version="${major}.${minor}.$((patch + 1))"
  fi
fi

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  echo "version=$next_version" >> "$GITHUB_OUTPUT"
else
  echo "$next_version"
fi
