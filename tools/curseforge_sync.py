#!/usr/bin/env python3
"""Brings the CurseForge page in line with store/, as tools/modrinth_sync.py does for Modrinth. CurseForge's API offers nothing
for a project's page, so this drives its authors' site in a Chrome of its own, where the author signs in by hand; run on demand,
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
# The site's own class names and labels: what to look at first should it change
ITEMS = "[...document.querySelectorAll('.gallery-item-container')]"
AREA = "[...document.querySelectorAll('textarea')].find(a=>a.offsetWidth>0&&a.getAttribute('aria-hidden')!=='true'&&!a.readOnly)"
TITLE = "[...document.querySelectorAll('input')].filter(i=>i.offsetWidth>0).find(i=>i.type=='text'&&document.elementFromPoint(i.getBoundingClientRect().x+5,i.getBoundingClientRect().y+5)===i)"
FEATURED = 4


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

	def hover(self, element):
		self.js(element + '.scrollIntoView({block:"center"})')
		time.sleep(.4)
		self.mouse(1, 1)
		self.mouse(*self.center(element))

	def click(self, element):
		x, y = self.center(element)
		self.mouse(x, y)
		for kind in ("mousePressed", "mouseReleased"):
			self.call("Input.dispatchMouseEvent", type=kind, x=x, y=y, button="left", clickCount=1)

	def set_value(self, element, value, kind):
		# Through the prototype's setter, for the site's React to notice
		self.js("(()=>{const a=%s;Object.getOwnPropertyDescriptor(%s.prototype,'value').set.call(a,%s);a.dispatchEvent(new Event('input',{bubbles:true}));a.dispatchEvent(new Event('change',{bubbles:true}));})()" % (element, kind, json.dumps(value)))

	def names(self):
		return json.loads(self.js(f"JSON.stringify({ITEMS}.map(g=>g.innerText.trim()))"))

	def starred(self):
		return json.loads(self.js(f"JSON.stringify({ITEMS}.filter(g=>g.querySelector('[aria-label*=eatured]')).map(g=>g.innerText.trim()))"))

	def wait_chooser(self, seconds):
		end = time.time() + seconds
		self.ws.settimeout(1)
		try:
			while time.time() < end:
				try:
					message = json.loads(self.ws.recv())
				except self.websocket.WebSocketTimeoutException:
					continue
				if message.get("method") == "Page.fileChooserOpened":
					return message["params"]
		finally:
			self.ws.settimeout(60)
		return None


def shots():
	return json.loads((ROOT / "store/gallery.json").read_text(encoding="utf-8"))


def delete(tab, index):
	names = tab.names()
	item = f"{ITEMS}[{index}]"
	tab.hover(item)
	tab.click(item + '.querySelector(".delete-button")')
	time.sleep(.8)
	tab.js(f"[...[...document.querySelectorAll('[role=dialog]')].pop().querySelectorAll('button')].find(b=>b.innerText.trim()=='OK').click()")
	for _ in range(20):
		time.sleep(.5)
		if len(tab.names()) < len(names):
			print(f"removed {names[index]}", flush=True)
			# The list draws itself again for a moment
			time.sleep(2)
			return
	raise RuntimeError(f"not removed: {names[index]}")


def upload(tab, shot):
	"""Sends an image, then fills its title and description. False if the site ignored it: the gallery holds these very bytes."""
	# An earlier try that lost a click after sending: still under its file's name
	for index, name in enumerate(tab.names()):
		if name.endswith(".jpg"):
			delete(tab, index)
			break
	before = len(tab.names())
	tab.js("window.scrollTo(0,0);document.scrollingElement.scrollTop=0")
	time.sleep(.5)
	tab.call("DOM.enable")
	tab.call("Page.enable")
	tab.call("Page.setInterceptFileChooserDialog", enabled=True)
	tab.js("[...document.querySelectorAll('button')].find(b=>/Add media/.test(b.innerText)).click()")
	time.sleep(.8)
	tab.click("[...document.querySelector('[role=dialog]').querySelectorAll('button')].find(b=>/Upload or Drag/.test(b.innerText))")
	chooser = tab.wait_chooser(8)
	if not chooser:
		raise RuntimeError(f"no file chooser for {shot['file']}")
	tab.call("DOM.setFileInputFiles", files=[str(ROOT / "store/screenshots" / shot["file"])], backendNodeId=chooser["backendNodeId"])
	tab.call("Page.setInterceptFileChooserDialog", enabled=False)
	for _ in range(60):
		time.sleep(.5)
		if len(tab.names()) > before:
			break
	else:
		return False
	last = ITEMS + ".pop()"
	tab.hover(last)
	tab.click(last + '.querySelector(".edit-button")')
	time.sleep(1.2)
	tab.set_value(TITLE, shot["title"], "HTMLInputElement")
	tab.set_value(AREA, shot["description"], "HTMLTextAreaElement")
	time.sleep(.4)
	tab.js(button("Apply") + ".click()")
	time.sleep(1.5)
	if tab.js("!!" + button("Cancel")):
		tab.js(button("Cancel") + ".click()")
		time.sleep(.8)
	print(f"sent {shot['title']}", flush=True)
	time.sleep(2)
	return True


def attempts(action, *arguments):
	"""The site loses a click now and then: once more from a fresh page."""
	tab = arguments[0]
	for attempt in range(3):
		try:
			return action(*arguments)
		except RuntimeError as error:
			if attempt == 2:
				raise
			print(f"again, after: {str(error)[:120]}", flush=True)
			if tab.js("!!" + button("Cancel")):
				tab.js(button("Cancel") + ".click()")
			tab.load("media")


def gallery(tab):
	"""Removes the old images first, the site ignoring bytes it already holds; the first stays until its replacement is there, so the
	gallery is never empty. Nothing tells an image's old version from its new one, so a second run starts over."""
	wanted = shots()
	tab.load("media")
	while len(tab.names()) > 1:
		attempts(delete, tab, 1)
	if attempts(upload, tab, wanted[0]):
		attempts(delete, tab, 0)
	else:
		print(f"unchanged {wanted[0]['title']}", flush=True)
	for shot in wanted[1:]:
		if not attempts(upload, tab, shot):
			raise RuntimeError(f"ignored: {shot['file']}")
	for index, name in reversed(list(enumerate(tab.names()))):
		if name.endswith(".jpg"):
			delete(tab, index)
	stars(tab)
	check(tab)


def stars(tab):
	"""The featured images, which the public page shows first; only clicks made from a fresh page hold."""
	wanted = [shot["title"] for shot in shots()[:FEATURED]]
	tab.load("media")
	for name in wanted:
		if name not in tab.starred():
			item = f"{ITEMS}.find(g=>g.innerText.trim()=={json.dumps(name)})"
			tab.hover(item)
			tab.click(item + ".querySelector('[aria-label*=eatured]')")
			time.sleep(3)
			tab.load("media")
	print("featured", tab.starred(), flush=True)


def check(tab):
	wanted = shots()
	tab.load("media")
	names = tab.names()
	print(f"{len(names)} images, {len(wanted)} wanted; same order: {names == [shot['title'] for shot in wanted]}")
	print("missing", [shot["title"] for shot in wanted if shot["title"] not in names])
	print("not wanted", [name for name in names if name not in [shot["title"] for shot in wanted]])
	print("featured", tab.starred())


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
	elif command in ("check", "text", "gallery", "stars"):
		{"check": check, "text": text, "gallery": gallery, "stars": stars}[command](Tab())
	else:
		sys.exit(__doc__)


if __name__ == "__main__":
	main()
