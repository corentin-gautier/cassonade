"""Draws the upgraded gear items (textures/item) from the vanilla items they are crafted from:

- the barometer (barometer.png): the clock, its gold turned to the copper of the lightning rod it is
  crafted with, a cloud in its sky instead of the sun;
- the sculk compass (sculk_compass.png): the compass, its dial in the dark teal of sculk, lit by the
  cyan specks of an active sculk sensor.

Colors come from the game's own textures.

Usage:
    python3 tools/gear_item_textures.py ~/Library/Application\\ Support/minecraft/versions/26.3/26.3.jar
"""

import pathlib
import sys
import zipfile

from pngio import read_png, write_png

ITEM = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/vanillasugar/textures/item"


def luminance(color):
	return 0.299 * color[0] + 0.587 * color[1] + 0.114 * color[2]


def ramp(pixels, colors):
	"""The colors among those given found in the texture, from the lightest to the darkest."""
	found = {pixel[:3] for row in pixels for pixel in row if pixel[3]}
	return sorted((color for color in colors if color in found), key=luminance, reverse=True)


def recolor(pixels, mapping):
	return [[mapping.get(pixel[:3], pixel[:3]) + (pixel[3],) if pixel[3] else pixel for pixel in row] for row in pixels]


def hex_color(value):
	return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


# The clock's gold, its sun, and the compass's dial, as drawn by the game
CLOCK_GOLD = [hex_color(c) for c in ("fdf55f", "fad64a", "e9b115", "dc9613", "b26411", "752802")]
CLOCK_SUN = {hex_color("ffff00"): hex_color("ffffff"), hex_color("cccc00"): hex_color("c6ccd6")}
COMPASS_DIAL = {hex_color("353535"): hex_color("07323b"), hex_color("2f2f2f"): hex_color("052a32")}


def barometer(clock, lightning_rod, copper_ingot):
	gold = ramp(clock, CLOCK_GOLD)
	# The rod's tones, outlined with the ingot's darkest one: the rod's own is too light for an outline
	copper = sorted({pixel[:3] for row in lightning_rod for pixel in row if pixel[3]}, key=luminance, reverse=True)
	outline = min((pixel[:3] for row in copper_ingot for pixel in row if pixel[3]), key=luminance)
	copper = copper[:len(gold) - 1] + [outline]
	mapping = dict(zip(gold, copper))
	mapping.update(CLOCK_SUN)
	return recolor(clock, mapping)


def sculk_compass(compass, tendril_active):
	pixels = recolor(compass, COMPASS_DIAL)
	glow = sorted({pixel[:3] for row in tendril_active for pixel in row if pixel[3]}, key=luminance, reverse=True)
	# Specks on the dial, either side of the needle, like the tips of a sensor's tendrils
	for x, y, shade in ((4, 6, 0), (11, 6, 1), (5, 4, 1), (10, 8, 0)):
		if pixels[y][x][:3] in COMPASS_DIAL.values():
			pixels[y][x] = glow[shade] + (255,)
	return pixels


def main(jar_path):
	with zipfile.ZipFile(jar_path) as jar:
		clock = read_png(jar.read("assets/minecraft/textures/item/clock_00.png"))
		lightning_rod = read_png(jar.read("assets/minecraft/textures/block/lightning_rod.png"))
		copper_ingot = read_png(jar.read("assets/minecraft/textures/item/copper_ingot.png"))
		compass = read_png(jar.read("assets/minecraft/textures/item/compass_16.png"))
		tendril = read_png(jar.read("assets/minecraft/textures/block/sculk_sensor_tendril_active.png"))
	write_png(ITEM / "barometer.png", barometer(clock, lightning_rod, copper_ingot))
	write_png(ITEM / "sculk_compass.png", sculk_compass(compass, tendril))


if __name__ == "__main__":
	main(sys.argv[1])
