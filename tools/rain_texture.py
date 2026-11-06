#!/usr/bin/env python3
"""Draws the rain from vanilla's (textures/environment/rain.png): the same streaks, each as see-through as before, but
in a pale grey barely touched with blue instead of vanilla's deep blue, since real rain only takes the color of the sky
it falls through. White shows more than blue against the sky, so the streaks are a little more see-through.

The drops bursting on the ground get greys of their own too (textures/particle/rain_<n>.png, which
assets/minecraft/particles/rain.json gives the rain particle): vanilla's splash sprites, shared with fish, boats and
falls, stay blue.

Usage:
    python3 tools/rain_texture.py [path/to/minecraft-26.3.jar], then open it in src/textures/world.psd (tools/atlas_psd.py)

Without a jar, the client jar Loom downloaded for the build is used (run ./gradlew build once first).
"""

import sys
import zipfile
from pathlib import Path

from minecraft_jar import loom_jar
from pngio import read_png, write_png

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets"
TARGET = ASSETS / "minecraft/textures/environment/rain.png"
DROPS = ASSETS / "cassonade/textures/particle"
# Vanilla's three splash blues, from light to dark, each to its grey
DROP_GREYS = {(71, 106, 204): (214, 220, 228), (24, 72, 204): (186, 194, 206), (0, 54, 204): (160, 170, 184)}
RAIN = (196, 204, 214)
OPACITY = 0.8


def recolor(pixels):
	return [[(*RAIN, round(a * OPACITY)) if a else (0, 0, 0, 0) for _, _, _, a in row] for row in pixels]


def main(jar):
	with zipfile.ZipFile(jar) as archive:
		pixels = read_png(archive.read("assets/minecraft/textures/environment/rain.png"))
		drops = [read_png(archive.read(f"assets/minecraft/textures/particle/splash_{n}.png")) for n in range(4)]
	TARGET.parent.mkdir(parents=True, exist_ok=True)
	write_png(TARGET, recolor(pixels))
	DROPS.mkdir(parents=True, exist_ok=True)
	for n, drop in enumerate(drops):
		write_png(DROPS / f"rain_{n}.png", [[(*DROP_GREYS[(r, g, b)], a) if a else (r, g, b, a) for r, g, b, a in row] for row in drop])


if __name__ == "__main__":
	main(sys.argv[1] if len(sys.argv) > 1 else str(loom_jar()))
