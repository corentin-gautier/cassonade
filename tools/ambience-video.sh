#!/usr/bin/env bash
# Turns a screen recording of the game into a website ambience loop: promo/site/assets/videos/<id>.mp4, a short silent
# loop that plays over the screenshot of the same id (see promo/site/_includes/shot.html). The HUD is cropped out (the top
# tenth: window bar, clock, direction bar; the bottom eighth: hotbar), keeping 16:10 like the screenshots. Without a
# screenshot of that id yet, a frame from the middle of the loop becomes one, converted with tools/convert-screenshots.sh.
#
# Usage: tools/ambience-video.sh <recording.mov> <start in seconds> <id> [length in seconds, 8 by default]
#    or: tools/ambience-video.sh <recording.mov> <from-to,from-to...> <id>, passages put end to end (a dive, then a look up)
# Needs ffmpeg (brew install ffmpeg).
set -euo pipefail

if [[ $# -lt 3 ]]; then
	echo "usage: $0 <recording.mov> <start|from-to,from-to...> <id> [length]" >&2
	exit 1
fi
recording=$1
start=$2
id=$3
length=${4:-8}

cd "$(dirname "$0")/.."
videos=promo/site/assets/videos
site=promo/site/assets/screenshots
mkdir -p "$videos"

# 78% of the height between the HUD's top and bottom, as wide as 16:10 allows, centered; even sizes for H.264
crop="crop=trunc(ih*0.78*1.6/2)*2:trunc(ih*0.78/2)*2:trunc((iw-ih*0.78*1.6)/4)*2:trunc(ih*0.10/2)*2"

# 1280 wide is as large as a panorama band opens; 30 fps and no sound keep it near a megabyte, and the bitrate's cap keeps
# a busy scene (leaves, grass) near 1.7 MB for 8 seconds
encode="$crop,scale=1280:800:flags=lanczos,fps=30"
if [[ $start == *-* ]]; then
	# Each passage trimmed and started from zero, then all end to end
	graph=""
	labels=""
	count=0
	IFS=, read -ra passages <<< "$start"
	for passage in "${passages[@]}"; do
		graph+="[0:v]trim=start=${passage%-*}:end=${passage#*-},setpts=PTS-STARTPTS[p$count];"
		labels+="[p$count]"
		count=$((count + 1))
	done
	input=(-i "$recording")
	filter=(-filter_complex "$graph${labels}concat=n=$count:v=1,$encode")
	# For the screenshot below: the middle of the first passage
	first=${passages[0]}
	start=${first%-*}
	length=$(python3 -c "print(${first#*-} - ${first%-*})")
else
	input=(-ss "$start" -t "$length" -i "$recording")
	filter=(-vf "$encode")
fi
ffmpeg -v error -y "${input[@]}" "${filter[@]}" \
	-c:v libx264 -profile:v high -preset slow -crf 26 -maxrate 1600k -bufsize 3200k -pix_fmt yuv420p -an -movflags +faststart \
	"$videos/$id.mp4"
echo "$videos/$id.mp4 ($(( $(stat -f %z "$videos/$id.mp4") / 1024 )) KB)"

if [[ ! -e "$site/$id.avif" ]]; then
	middle=$(python3 -c "print($start + $length / 2)")
	ffmpeg -v error -y -ss "$middle" -i "$recording" -frames:v 1 -vf "$crop,scale=2000:1250:flags=lanczos" "$site/$id.png"
	tools/convert-screenshots.sh
fi
