#!/bin/bash
# PreToolUse hook: refuses a git commit while the Java files it may include have javac warnings.
command=$(jq -r '.tool_input.command // empty')
grep -Eq '(^|[;&|][[:space:]]*)git[[:space:]]+commit([[:space:]]|$)' <<< "$command" || exit 0

cd "$CLAUDE_PROJECT_DIR" || exit 0

# Refused rather than left to the commit: the removals are not staged yet.
removed=$(.claude/hooks/remove-unused-imports.sh)
if [ -n "$removed" ]; then
	jq -n --arg removed "$removed" \
		'{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: ("Unused imports removed, stage these files again and retry the commit:\n" + $removed)}}'
	exit 0
fi

# Compared to HEAD rather than the index, so that git commit -a is covered too; new files too, which git diff leaves out.
files=$({ git diff --name-only --diff-filter=d HEAD -- '*.java'; git ls-files --others --exclude-standard -- '*.java'; })
[ -z "$files" ] && exit 0

# No mapfile: macOS has bash 3.
changed=()
while IFS= read -r file; do changed+=("$file"); done <<< "$files"
problems=$(.claude/hooks/java-problems.sh "${changed[@]}")

# --rerun: an up-to-date task would print no warnings at all.
output=$(./gradlew -I .claude/hooks/java-lint.gradle compileJava --rerun compileClientJava --rerun compileGametestJava --rerun \
	--continue --console=plain --quiet 2>&1)
status=$?

warnings=$(grep -F -A2 -f <(sed "s|^|$PWD/|; s|\$|:|" <<< "$files") <<< "$output" | grep -A2 ': warning:')

if [ $status -ne 0 ]; then
	reason="The code does not compile, commit refused:
$(tail -n 40 <<< "$output")"
elif [ -n "$warnings$problems" ]; then
	reason="Warnings in the files being committed, fix them first:
$warnings
$problems"
else
	exit 0
fi

jq -n --arg reason "$reason" \
	'{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $reason}}'
