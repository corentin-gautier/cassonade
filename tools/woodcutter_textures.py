"""Draws the woodcutter's block textures (textures/block/woodcutter_top, _side, _bottom) from the stonecutter's:
each gray is replaced by the oak plank's color of the same lightness, so the block keeps the stonecutter's
shape and shading, in wood. The saw is the stonecutter's own.

Usage:
    python3 tools/woodcutter_textures.py ~/Library/Application\\ Support/minecraft/versions/26.3/26.3.jar
"""

import pathlib
import sys
import zipfile

from pngio import read_png, write_png

ROOT = pathlib.Path(__file__).resolve().parent.parent
TARGET = ROOT / "src/main/resources/assets/cassonade/textures/block"
FACES = ["top", "side", "bottom"]


def luminance(color):
	return 0.299 * color[0] + 0.587 * color[1] + 0.114 * color[2]


def main(jar_path):
	with zipfile.ZipFile(jar_path) as jar:
		planks = read_png(jar.read("assets/minecraft/textures/block/oak_planks.png"))
		faces = {face: read_png(jar.read(f"assets/minecraft/textures/block/stonecutter_{face}.png")) for face in FACES}
	wood = sorted({pixel[:3] for row in planks for pixel in row}, key=luminance, reverse=True)
	grays = [luminance(pixel[:3]) for pixels in faces.values() for row in pixels for pixel in row if pixel[3]]
	lightest, darkest = max(grays), min(grays)
	TARGET.mkdir(parents=True, exist_ok=True)
	for face, pixels in faces.items():
		painted = []
		for row in pixels:
			line = []
			for pixel in row:
				if pixel[3]:
					lightness = (luminance(pixel[:3]) - darkest) / max(lightest - darkest, 1)
					line.append(wood[round((1 - lightness) * (len(wood) - 1))] + (pixel[3],))
				else:
					line.append(pixel)
			painted.append(line)
		write_png(TARGET / f"woodcutter_{face}.png", painted)


if __name__ == "__main__":
	main(sys.argv[1])
