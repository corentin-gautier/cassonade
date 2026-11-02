#!/bin/bash
# Prints what VS Code's Problems panel shows for the given Java files, javac not reporting all of it (an unused private method,
# for one): the Eclipse compiler its Java extension runs, at its settings. Prints nothing without that extension.
compiler=$(ls ~/.vscode/extensions/redhat.java-*/server/plugins/org.eclipse.jdt.core.compiler.batch_*.jar 2>/dev/null | tail -n 1)
[ -z "$compiler" ] || [ $# -eq 0 ] && exit 0
java=$(ls -d ~/.gradle/jdks/*25*/*/Contents/Home/bin/java 2>/dev/null | head -n 1)
[ -z "$java" ] && exit 0

classpath=$(./gradlew -I .claude/hooks/java-classpath.gradle printJavaClasspath --console=plain --no-configuration-cache 2>/dev/null | sed -n 's/^CLASSPATH=//p')
list=$(mktemp)
trap 'rm -f "$list"' EXIT
git ls-files -- '*.java' > "$list"
git ls-files --others --exclude-standard -- '*.java' >> "$list"

# Every source is read, the files asked about only being filtered on, or one calling another would count as unused.
# VS Code leaves out unused lambda parameters and leaks, here nearly all false: the game's Minecraft and level are Closeable.
# The batch compiler has no switch for the first: they are filtered below.
"$java" -jar "$compiler" -25 -proc:none -d none -warn:-resource,unusedArgument -cp "$classpath" "@$list" 2>&1 |
	WANTED="$(printf '%s\n' "$@")" awk '
		BEGIN { n = split(ENVIRON["WANTED"], wanted, "\n") }
		/^[0-9]+\. (WARNING|ERROR) in / {
			show = 0
			for (i = 1; i <= n; i++) if (index($0, wanted[i] " (at line")) show = 1
			header = $0
			sub(/^[0-9]+\. /, "", header)
			line = 0
			next
		}
		show && !/^-+$/ && ++line == 3 && !/lambda parameter/ { print header; print "  " $0 }
	'
exit 0
