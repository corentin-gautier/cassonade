#!/bin/bash
# Removes the imports never used in the uncommitted Java files, and prints them. javac does not report these.
# Not run after each edit: an import is often added just before the code that uses it.
# A name that only appears in a comment counts as used, as Javadoc links do.
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0

{ git diff --name-only --diff-filter=d HEAD -- '*.java'; git ls-files --others --exclude-standard -- '*.java'; } |
while IFS= read -r file; do
	grep -E '^import (static )?[A-Za-z0-9_.]+;' "$file" | while IFS= read -r line; do
		name=$(sed -E 's/.*\.([A-Za-z0-9_]+);.*/\1/' <<< "$line")
		if ! grep -v '^import ' "$file" | grep -qw -- "$name"; then
			grep -vxF -- "$line" "$file" > "$file.tmp" && cat "$file.tmp" > "$file"
			rm -f "$file.tmp"
			echo "$file: $line"
		fi
	done
done
