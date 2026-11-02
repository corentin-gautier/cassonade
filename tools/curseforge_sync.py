#!/usr/bin/env python3
"""Brings the CurseForge page in line with store/, as tools/modrinth_sync.py does for Modrinth. CurseForge's API offers nothing
for a project's page, so this calls the API of its authors' site from a page of that site, in a Chrome of its own, where the author signs in by hand; run on demand,
from the author's machine. The profile keeps the sign-in from one time to the next. See "Releasing" in CONTRIBUTING.md.

    python3 -m venv ~/.cache/cassonade-store && ~/.cache/cassonade-store/bin/pip install websocket-client
    PY=~/.cache/cassonade-store/bin/python

    $PY tools/curseforge_sync.py open       starts that Chrome; if asked, sign in there by hand
    $PY tools/curseforge_sync.py check      what the gallery holds, against store/gallery.json
    $PY tools/curseforge_sync.py text       the description and the summary
    $PY tools/curseforge_sync.py gallery    replaces every image; run it again if it stops
    $PY tools/curseforge_sync.py close      closes that Chrome: while it runs, any local program can drive the session
"""

import base64
import json
import os
import pathlib
import subprocess
import sys
import time
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
PROJECT = "1763242"
SITE = f"https://authors.curseforge.com/#/projects/{PROJECT}/"
PORT = 9334
ORIGIN = f"http://localhost:{PORT}"
PROFILE = pathlib.Path.home() / "Library/Application Support/cassonade-store-chrome"
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
API = f"/_api/image-attachments/{PROJECT}"
API_UPLOAD = f"/_api/image-attachments/image/{PROJECT}"
PUBLIC = "https://www.curseforge.com/minecraft/mc-mods/cassonade/gallery"
FEATURED = 4
# The most the site takes in one upload
BATCH = 20


def button(label):
	return f"[...document.querySelectorAll('button')].find(b=>b.innerText.trim()=={json.dumps(label)}&&b.offsetWidth>0)"


class Tab:
	def __init__(self):
		import websocket
		self.websocket = websocket
		pages = [page for page in json.load(urllib.request.urlopen(f"{ORIGIN}/json/list")) if page["type"] == "page"]
		self.ws = websocket.create_connection(pages[0]["webSocketDebuggerUrl"], origin=ORIGIN, timeout=60)
		self.id = 0

	def call(self, method, **params):
		self.id += 1
		self.ws.send(json.dumps({"id": self.id, "method": method, "params": params}))
		while True:
			reply = json.loads(self.ws.recv())
			if reply.get("id") == self.id:
				if "error" in reply:
					raise RuntimeError(reply["error"])
				return reply.get("result", {})

	def js(self, expression):
		result = self.call("Runtime.evaluate", expression=expression, returnByValue=True, awaitPromise=True)
		if "exceptionDetails" in result:
			raise RuntimeError(str(result["exceptionDetails"])[:400])
		return result["result"].get("value")

	def load(self, page):
		# The site keeps its page across hash changes: reloaded, or it shows what it held before
		self.call("Page.reload")
		time.sleep(7)
		self.call("Page.navigate", url=SITE + page)
		time.sleep(4)
		self.mouse(1, 1)

	def mouse(self, x, y):
		self.call("Input.dispatchMouseEvent", type="mouseMoved", x=x, y=y)
		time.sleep(.3)

	def center(self, element):
		return json.loads(self.js("(()=>{const r=%s.getBoundingClientRect();return JSON.stringify([r.x+r.width/2,r.y+r.height/2])})()" % element))

	def set_value(self, element, value, kind):
		# Through the prototype's setter, for the site's React to notice
		self.js("(()=>{const a=%s;Object.getOwnPropertyDescriptor(%s.prototype,'value').set.call(a,%s);a.dispatchEvent(new Event('input',{bubbles:true}));a.dispatchEvent(new Event('change',{bubbles:true}));})()" % (element, kind, json.dumps(value)))


def shots():
	return json.loads((ROOT / "store/gallery.json").read_text(encoding="utf-8"))


def api(tab, method, path="", body=None):
	"""A call to the site's own API, from its page, which holds the session."""
	options = {"method": method, "headers": {"Content-Type": "application/json"}}
	if body is not None:
		options["body"] = json.dumps(body)
	reply = json.loads(tab.js(f"fetch('{API}{path}',{json.dumps(options)}).then(async r=>JSON.stringify([r.status,await r.text()]))"))
	if reply[0] != 200:
		raise RuntimeError(f"{method} {path}: {reply[0]} {reply[1][:200]}")
	return json.loads(reply[1])


def listed(tab, reads=30):
	"""Everything the site lists. Its servers each answer from a copy that can be minutes old, holding images long gone: only
	several reads together can be trusted to hold all there is, and none to hold only what there is."""
	found = {}
	for read in range(reads):
		for image in api(tab, "GET", f"?range=%5B0%2C199%5D&_={time.time_ns()}"):
			found[image["id"]] = image
	return found


def send(tab, wanted):
	"""Sends these images in one call, and returns what the site made of them."""
	files = [{"name": shot["file"], "data": base64.b64encode((ROOT / "store/screenshots" / shot["file"]).read_bytes()).decode()} for shot in wanted]
	reply = json.loads(tab.js(f"""(async()=>{{const form=new FormData();
		for(const f of {json.dumps(files)})form.append('files',new File([Uint8Array.from(atob(f.data),c=>c.charCodeAt(0))],f.name,{{type:'image/jpeg'}}));
		const r=await fetch('{API_UPLOAD}',{{method:'POST',body:form}});return JSON.stringify([r.status,await r.text()])}})()"""))
	if reply[0] != 201:
		raise RuntimeError(f"upload: {reply[0]} {reply[1][:200]}")
	results = json.loads(reply[1])["results"]
	if len(results) != len(wanted) or not all(result["success"] for result in results):
		raise RuntimeError(f"upload: {reply[1][:300]}")
	return results


def gallery(tab):
	"""Removes every image, the site ignoring bytes it already holds, sends the new ones by batches, then writes their texts, the
	featured ones and the order. Run it again if it stops: it starts over, in under a minute."""
	wanted = shots()
	tab.load("media")
	old = listed(tab)
	for image in old.values():
		api(tab, "DELETE", f"/{image['id']}/{image['type']}")
	print(f"removed {len(old)} images", flush=True)
	images = []
	for start in range(0, len(wanted), BATCH):
		batch = wanted[start:start + BATCH]
		sent = send(tab, batch)
		images += sent
		print(f"sent {len(images)}/{len(wanted)}", flush=True)
	# The site renames the files, but keeps the original's name in their address
	by_name = {image["url"].rsplit("/", 1)[1].replace("-jpg.jpg", ".jpg"): image for image in images}
	order = []
	for index, shot in enumerate(wanted):
		image = by_name[shot["file"]]
		written = {"id": image["id"], "url": image["url"], "type": 1, "title": shot["title"], "description": shot["description"],
				"isFeatured": index < FEATURED, "displayIndex": index}
		api(tab, "PUT", "", written)
		order.append(written)
	api(tab, "PUT", "/update-display-order", order)
	print("texts, featured and order written", flush=True)


def check(tab):
	"""From the public page: the authors' site lists images long gone (see listed)."""
	wanted = [shot["file"].replace(".jpg", "-jpg.jpg") for shot in shots()]
	descriptions = [shot["description"] for shot in shots()]
	target = tab.call("Target.createTarget", url=PUBLIC)["targetId"]
	time.sleep(10)
	page = next(page for page in json.load(urllib.request.urlopen(f"{ORIGIN}/json/list")) if page["id"] == target)
	public = Tab.__new__(Tab)
	public.ws, public.id = tab.websocket.create_connection(page["webSocketDebuggerUrl"], origin=ORIGIN, timeout=60), 0
	found = json.loads(public.js("JSON.stringify([...document.querySelectorAll('img[src*=attachments]')].map(i=>[i.src.split('/').pop(),i.alt,+i.src.split('/').at(-3)*1000+ +i.src.split('/').at(-2)]))"))
	public.ws.close()
	tab.call("Target.closeTarget", targetId=target)
	shown = [image[1] for image in found]
	print("oldest image", min(int(image[2]) for image in found), "newest", max(int(image[2]) for image in found), "(the public page can lag behind)")
	print("descriptions missing", [text[:40] for text in descriptions if not any(text in alt for alt in shown)])
	found = [image[0] for image in found]
	print(f"{len(found)} images, {len(wanted)} wanted; same order: {found == wanted}")
	print("missing", [name for name in wanted if name not in found])
	print("not wanted", [name for name in found if name not in wanted])


def text(tab):
	body = (ROOT / "store/description.md").read_text(encoding="utf-8")
	tab.load("description")
	field = "[...document.querySelectorAll('textarea')].find(x=>x.offsetWidth>0)"
	# The site rewrites a few characters on saving, so this usually saves again: no harm done
	if tab.js(field + ".value").strip() == body.strip():
		print("description: same")
	else:
		tab.set_value(field, body, "HTMLTextAreaElement")
		time.sleep(1)
		tab.js(button("Save") + ".click()")
		time.sleep(3)
		print("description: saved")
	summary = (ROOT / "store/summary.txt").read_text(encoding="utf-8").strip()
	tab.load("general")
	# The field has no name of its own: the one under the label of that name
	field = "[...document.querySelectorAll('label,span,p,div')].filter(e=>e.children.length==0&&/^Summary/.test(e.innerText||'')).map(e=>{let n=e;for(let i=0;i<4&&n;i++){const f=n.querySelector('input,textarea');if(f)return f;n=n.parentElement}}).find(f=>f)"
	if tab.js(f"(()=>{{const f={field};return f?f.value:null}})()") == summary:
		print("summary: same")
	else:
		if not tab.js("!!" + field):
			raise RuntimeError("no summary field found: set store/summary.txt by hand on the General tab")
		kind = tab.js(field + ".tagName") == "TEXTAREA" and "HTMLTextAreaElement" or "HTMLInputElement"
		tab.set_value(field, summary, kind)
		time.sleep(1)
		tab.js(button("Save") + ".click()")
		time.sleep(3)
		print("summary: saved")


def start():
	PROFILE.mkdir(parents=True, exist_ok=True)
	subprocess.Popen([CHROME, "--no-default-browser-check", "--no-first-run", f"--remote-debugging-port={PORT}", f"--remote-allow-origins={ORIGIN}",
			f"--user-data-dir={PROFILE}", "--window-size=1400,1000", SITE + "media"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
	print("Chrome started: if the site asks, sign in there by hand")


def close():
	import websocket
	url = json.load(urllib.request.urlopen(f"{ORIGIN}/json/version"))["webSocketDebuggerUrl"]
	connection = websocket.create_connection(url, origin=ORIGIN)
	connection.send(json.dumps({"id": 1, "method": "Browser.close"}))
	connection.recv()


def main():
	command = sys.argv[1] if len(sys.argv) > 1 else ""
	if command == "open":
		start()
	elif command == "close":
		close()
	elif command in ("check", "text", "gallery"):
		{"check": check, "text": text, "gallery": gallery}[command](Tab())
	else:
		sys.exit(__doc__)


if __name__ == "__main__":
	main()
