#!/usr/bin/env python3
"""Brings the Modrinth page in line with store/: the description from store/description.md, the gallery from
store/screenshots/. Run by .github/workflows/store.yml. A gallery image can't be replaced in place, so only what changed is
sent: an image is known by its title, made from its file's name, and one with any other title is removed.

    MODRINTH_ID=<id or slug> MODRINTH_TOKEN=<token with PROJECT_WRITE> python3 tools/modrinth_sync.py [changed file...]
    MODRINTH_ID=<id or slug> python3 tools/modrinth_sync.py --dry-run [changed file...]

The changed files are those of store/screenshots/ to send again though already in the gallery.
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
# Shown first and on the project's card
FEATURED = "hero"


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
		sys.exit(f"{method} {path}: {error.code} {error.read().decode(errors='replace')[:300]}")


def title(path):
	return path.stem.replace("-", " ").capitalize()


def main():
	arguments = sys.argv[1:]
	dry_run = "--dry-run" in arguments
	changed = {pathlib.Path(argument).stem for argument in arguments if argument != "--dry-run"}
	project = json.loads(request("GET", ""))
	body = (ROOT / "store/description.md").read_text(encoding="utf-8")
	shots = sorted((ROOT / "store/screenshots").glob("*.jpg"), key=lambda path: (path.stem != FEATURED, path.stem))
	too_big = [path.name for path in shots if path.stat().st_size > MAX_BYTES]
	if too_big:
		sys.exit(f"Over Modrinth's 5 MiB: {', '.join(too_big)}")

	gallery = {image["title"]: image for image in project["gallery"]}
	wanted = {title(path): path for path in shots}
	stale = {name: image for name, image in gallery.items() if name not in wanted or wanted[name].stem in changed}

	if project["body"].strip() != body.strip():
		print("description: updated")
		if not dry_run:
			request("PATCH", "", data=json.dumps({"body": body}).encode(), content_type="application/json")

	# Added before anything is removed: the page never shows an empty gallery, even if this stops halfway
	for ordering, path in enumerate(shots):
		name = title(path)
		if name not in gallery or name in stale:
			print(f"gallery: send {path.name}")
			if not dry_run:
				request("POST", "/gallery", {"ext": "jpg", "featured": str(path.stem == FEATURED).lower(), "title": name, "ordering": ordering},
						path.read_bytes(), "image/jpeg")
		elif gallery[name]["ordering"] != ordering or gallery[name]["featured"] != (path.stem == FEATURED):
			print(f"gallery: reorder {path.name}")
			if not dry_run:
				request("PATCH", "/gallery", {"url": gallery[name]["url"], "ordering": ordering, "featured": str(path.stem == FEATURED).lower()})
	for name, image in stale.items():
		print(f"gallery: remove {name!r}")
		if not dry_run:
			request("DELETE", "/gallery", {"url": image["url"]})


if __name__ == "__main__":
	main()
