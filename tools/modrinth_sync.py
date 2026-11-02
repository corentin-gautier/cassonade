#!/usr/bin/env python3
"""Brings the Modrinth page in line with promo/store/: the short description from promo/store/summary.txt, the long one from promo/store/description.md, the gallery from
promo/store/screenshots/, in the order and with the titles and captions of promo/store/gallery.json, the first being featured. Run by
.github/workflows/store.yml. A gallery image can't be replaced in place, so only what changed is sent: an image is known by its
title, and one with any other title is removed.

    MODRINTH_ID=<id or slug> MODRINTH_TOKEN=<token with PROJECT_WRITE> python3 tools/modrinth_sync.py [changed file...]
    MODRINTH_ID=<id or slug> MODRINTH_TOKEN=<token> python3 tools/modrinth_sync.py --dry-run [changed file...]
    ... --replace-all sends the whole gallery again

The changed files are those of promo/store/screenshots/ to send again though already in the gallery.
"""

import json
import os
import pathlib
import sys
import urllib.error
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
API = "https://api.modrinth.com/v2/project/"
USER_AGENT = "corentin-gautier/cassonade (store sync)"
MAX_BYTES = 5 * 1024 * 1024
MAX_SUMMARY = 256


class Duplicate(Exception):
	"""The gallery already holds these very bytes, whatever their title."""


class Full(Exception):
	"""The gallery holds as many images as Modrinth allows."""


def request(method, path, query=None, data=None, content_type=None):
	url = API + urllib.parse.quote(os.environ["MODRINTH_ID"]) + path
	if query:
		url += "?" + urllib.parse.urlencode(query)
	headers = {"User-Agent": USER_AGENT}
	if "MODRINTH_TOKEN" in os.environ:
		headers["Authorization"] = os.environ["MODRINTH_TOKEN"]
	if content_type:
		headers["Content-Type"] = content_type
	try:
		with urllib.request.urlopen(urllib.request.Request(url, data, headers, method=method)) as response:
			return response.read()
	except urllib.error.HTTPError as error:
		message = error.read().decode(errors="replace")[:300]
		if "duplicate" in message:
			raise Duplicate from error
		if "maximum of gallery images" in message:
			raise Full from error
		sys.exit(f"{method} {path}: {error.code} {message}")


def main():
	arguments = sys.argv[1:]
	dry_run = "--dry-run" in arguments
	# A title says nothing of what an image shows: this sends them all again, should the gallery have drifted from promo/store/
	replace_all = "--replace-all" in arguments
	changed = {pathlib.Path(argument).name for argument in arguments if not argument.startswith("--")}
	project = json.loads(request("GET", ""))
	body = (ROOT / "promo/store/description.md").read_text(encoding="utf-8")
	shots = json.loads((ROOT / "promo/store/gallery.json").read_text(encoding="utf-8"))
	folder = ROOT / "promo/store/screenshots"
	listed = {shot["file"] for shot in shots}
	unlisted = sorted(path.name for path in folder.glob("*.jpg") if path.name not in listed)
	missing = sorted(name for name in listed if not (folder / name).exists())
	too_big = sorted(name for name in listed - set(missing) if (folder / name).stat().st_size > MAX_BYTES)
	if unlisted or missing or too_big:
		sys.exit(f"Not in promo/store/gallery.json: {unlisted}; no such file: {missing}; over 5 MiB: {too_big}")

	# A list: images of an earlier gallery can share a title, or have none
	kept = {}
	stale = []
	titles = {shot["title"]: shot for shot in shots}
	for image in project["gallery"]:
		name = image["title"]
		if replace_all or name not in titles or name in kept or titles[name]["file"] in changed:
			stale.append(image)
		else:
			kept[name] = image

	summary = (ROOT / "promo/store/summary.txt").read_text(encoding="utf-8").strip()
	if len(summary) > MAX_SUMMARY:
		sys.exit(f"promo/store/summary.txt is over Modrinth's {MAX_SUMMARY} characters: {len(summary)}")
	if project["body"].strip() != body.strip() or project["description"] != summary:
		print("description: updated")
		if not dry_run:
			request("PATCH", "", data=json.dumps({"body": body, "description": summary}).encode(), content_type="application/json")

	def remove(image):
		print(f"gallery: remove {image['title']!r}")
		if not dry_run:
			request("DELETE", "/gallery", {"url": image["url"]})

	def send(shot, ordering):
		query = {"ext": "jpg", "featured": str(ordering == 0).lower(), "title": shot["title"], "description": shot["description"], "ordering": ordering}
		while True:
			try:
				request("POST", "/gallery", query, (folder / shot["file"]).read_bytes(), "image/jpeg")
				return
			except Full:
				# Too many images changed at once to add them all first: an old one makes room
				if not stale:
					sys.exit(f"The gallery is full, with nothing left to remove: {len(shots)} images in promo/store/gallery.json are too many")
				remove(stale.pop(0))

	# Added before anything is removed, as far as the gallery's room goes: the page never shows an empty gallery, even if this stops halfway
	refused = []
	for ordering, shot in enumerate(shots):
		image = kept.get(shot["title"])
		if image is None:
			print(f"gallery: send {shot['file']}")
			if not dry_run:
				try:
					send(shot, ordering)
				except Duplicate:
					refused.append((shot, ordering))
		elif image["ordering"] != ordering or image["featured"] != (ordering == 0) or image["description"] != shot["description"]:
			print(f"gallery: update {shot['file']}")
			if not dry_run:
				request("PATCH", "/gallery", {"url": image["url"], "ordering": ordering, "featured": str(ordering == 0).lower(), "description": shot["description"]})
	while stale:
		remove(stale.pop(0))
	# Those the same image under an old title kept out, now removed
	failed = []
	for shot, ordering in refused:
		print(f"gallery: send {shot['file']} again")
		try:
			send(shot, ordering)
		except Duplicate:
			failed.append(shot["file"])
	if failed:
		sys.exit(f"Refused as duplicates of images not listed in the gallery: {failed}")


if __name__ == "__main__":
	main()
