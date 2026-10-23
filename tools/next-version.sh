#!/usr/bin/env bash
# Prints the version that follows the current one, for the release workflow (.github/workflows/release.yml).
#
# Usage: tools/next-version.sh <current> <alpha|beta|release|patch|minor|major>
#   alpha, beta   the next pre-release: 0.1.0-alpha.1 → 0.1.0-alpha.2, 0.1.0-alpha.2 → 0.1.0-beta.1,
#                 and from a final version, the first pre-release of the next patch: 0.1.0 → 0.1.1-alpha.1
#   release       the final version of a pre-release: 0.1.0-beta.3 → 0.1.0
#   patch, minor, major   a final version: 0.1.0 → 0.1.1, 0.2.0, 1.0.0 (from a pre-release, its base is bumped)
set -euo pipefail

current=$1 bump=$2
if [[ ! $current =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)(-([a-z]+)\.([0-9]+))?$ ]]; then
	echo "not a version: $current" >&2
	exit 1
fi
major=${BASH_REMATCH[1]} minor=${BASH_REMATCH[2]} patch=${BASH_REMATCH[3]}
stage=${BASH_REMATCH[5]} number=${BASH_REMATCH[6]}

case $bump in
	alpha | beta)
		if [[ -z $stage ]]; then
			echo "$major.$minor.$((patch + 1))-$bump.1"
		elif [[ $stage == "$bump" ]]; then
			echo "$major.$minor.$patch-$bump.$((number + 1))"
		elif [[ $stage == alpha && $bump == beta ]]; then
			echo "$major.$minor.$patch-beta.1"
		else
			echo "cannot go back from $stage to $bump" >&2
			exit 1
		fi
		;;
	release)
		if [[ -z $stage ]]; then
			echo "$current is already a final version" >&2
			exit 1
		fi
		echo "$major.$minor.$patch"
		;;
	patch) echo "$major.$minor.$((patch + 1))" ;;
	minor) echo "$major.$((minor + 1)).0" ;;
	major) echo "$((major + 1)).0.0" ;;
	*)
		echo "unknown bump: $bump" >&2
		exit 1
		;;
esac
