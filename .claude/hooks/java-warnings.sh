#!/bin/bash
# PostToolUse hook: compiles after an edit to a Java file and hands that file's javac warnings back to Claude.
file=$(jq -r '.tool_response.filePath // .tool_input.file_path // empty')
case "$file" in
	*.java) ;;
	*) exit 0 ;;
esac

cd "$CLAUDE_PROJECT_DIR" || exit 0
# Compile errors are left out: an edit in a series can legitimately leave the code broken for a moment.
warnings=$(./gradlew -I .claude/hooks/java-lint.gradle compileJava compileClientJava compileGametestJava \
	--continue --console=plain --quiet 2>&1 | grep -F -A2 "$file:" | grep -A2 ': warning:')
warnings="$warnings$(.claude/hooks/java-problems.sh "${file#"$PWD"/}")"
[ -z "$warnings" ] && exit 0

jq -n --arg warnings "$warnings" \
	'{decision: "block", reason: ("javac warnings in the edited file, fix them:\n" + $warnings)}'
