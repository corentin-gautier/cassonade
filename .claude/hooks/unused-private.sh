#!/bin/bash
# Prints the private methods never called in the given Java files. VS Code warns about these, javac does not at any -Xlint.
# One with an annotation is left out: a mixin's are called by name from elsewhere.
for file in "$@"; do
	grep -nE '^[[:space:]]*private[[:space:]][^=;]*[[:space:]][A-Za-z0-9_$]+\(.*\{[[:space:]]*$' "$file" | while IFS=: read -r number line; do
		previous=$(sed -n "$((number - 1))p" "$file")
		grep -Eq '^[[:space:]]*@|\)[[:space:]]*$' <<< "$previous" && continue
		name=$(sed -E 's/\(.*//; s/.*[[:space:]]//' <<< "$line")
		# Its own declaration is the only mention: :: references and recursive calls aside, nothing uses it
		[ "$(grep -cw -- "$name" "$file")" -le 1 ] && echo "$file:$number: private method $name is never used"
	done
done
exit 0
