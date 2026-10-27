#!/usr/bin/env bash
# Converts the website's screenshots, left as PNG by the screenshots test (-PsiteScreenshots), with macOS's
# image tool: for the site, <id>.avif and its <id>-640.avif and <id>-1280.avif copies (see site/_includes/shot.html),
# and for the Modrinth and CurseForge pages, store/screenshots/<id>.jpg. The PNGs are removed.
#
# Usage: tools/convert_screenshots.sh
set -euo pipefail

cd "$(dirname "$0")/.."
site=site/assets/screenshots
store=store/screenshots
scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT

shopt -s nullglob
pngs=("$site"/*.png)
if [[ ${#pngs[@]} -eq 0 ]]; then
	echo "no screenshot to convert in $site" >&2
	exit 1
fi

mkdir -p "$store"
for png in "${pngs[@]}"; do
	id=$(basename "$png" .png)
	sips -s format avif -s formatOptions 80 "$png" --out "$site/$id.avif" >/dev/null
	for width in 640 1280; do
		sips --resampleWidth "$width" "$png" --out "$scratch/$id-$width.png" >/dev/null
		sips -s format avif -s formatOptions 80 "$scratch/$id-$width.png" --out "$site/$id-$width.avif" >/dev/null
	done
	sips -s format jpeg -s formatOptions 85 "$png" --out "$store/$id.jpg" >/dev/null
	rm "$png"
	echo "$id"
done
