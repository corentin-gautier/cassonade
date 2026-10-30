"""Draws the upgraded gear items (textures/item) from the vanilla items they are crafted from:

- the barometer (barometer.png): the clock, its gold turned to the copper of the lightning rod it is
  crafted with, a cloud in its sky instead of the sun;
- the sculk compass (sculk_compass.png): the compass, its dial in the dark teal of sculk, lit by the
  cyan specks of an active sculk sensor;
- the naturalist's spyglass (naturalist_spyglass.png, and naturalist_spyglass_model.png for the one in
  hand): the spyglass, its grip in the brown leather of the book it is crafted with, its lens in the dark
  teal of the sculk sensor that senses for it, lit with its cyan like the sculk compass's dial;
- the sculk torch (sculk_torch.png): the torch, its flame in the teal and cyan of an active sculk sensor's
  tendrils, the top of its stick wrapped in the sensor's dark sculk.
- the organized backpack (organized_backpack.png): the backpack in a darker leather, its corners shod in the
  iron of the hopper it is upgraded with;
- the backpack upgrade template (backpack_upgrade_smithing_template.png): the netherite upgrade's template,
  in the backpack's leather, its arrow in iron.

Colors come from the game's own textures. The website shows the same icons (site/assets/items).

Usage:
    python3 tools/gear_item_textures.py ~/Library/Application\\ Support/minecraft/versions/26.3/26.3.jar
"""

import pathlib
import sys
import zipfile

from pngio import read_png, write_png

ROOT = pathlib.Path(__file__).resolve().parent.parent
TARGETS = [ROOT / "src/main/resources/assets/cassonade/textures/item", ROOT / "site/assets/items"]


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


# The spyglass's grip, from its lightest tone to its darkest, in the item and in the model held in hand
SPYGLASS_GRIP = [hex_color(c) for c in ("a85f58", "7e3f39", "552925")]
SPYGLASS_MODEL_GRIP = [hex_color(c) for c in ("773832", "552925")]
SPYGLASS_LENS = [hex_color(c) for c in ("68bdbd", "339090", "1e6566")]


def tones(pixels):
	return sorted({pixel[:3] for row in pixels for pixel in row if pixel[3]}, key=luminance, reverse=True)


def naturalist_spyglass(spyglass, model, book, tendril_active):
	# The book's browns, not its pages' grays
	leather = [color for color in tones(book) if max(color) - min(color) > 30]
	glow = tones(tendril_active)
	grip = dict(zip(SPYGLASS_GRIP, [leather[0], leather[1], leather[3]]))
	# The lens's light tone lit in the sensor's cyan, the others in the sculk compass's dial
	lens = dict(zip(SPYGLASS_LENS, [glow[1], *COMPASS_DIAL.values()]))
	return recolor(spyglass, grip | lens), recolor(model, dict(zip(SPYGLASS_MODEL_GRIP, [leather[1], leather[3]])))


# The torch's flame, its tip then its base (the white heart stays), and the two pixels of stick just under it
TORCH_FLAME = [hex_color(c) for c in ("ffd800", "ff8f00", "ffff97")]
TORCH_STICK_TOP = [hex_color(c) for c in ("9f7f50", "6d5736")]


def sculk_torch(torch, tendril_active):
	glow = tones(tendril_active)
	pixels = recolor(torch, dict(zip(TORCH_FLAME, [glow[1], glow[3], glow[0]])))
	# The flame stands on the stick's first row: that row only, wrapped in sculk
	top = next(y for y, row in enumerate(pixels) if any(pixel[3] and pixel[:3] in TORCH_STICK_TOP for pixel in row))
	pixels[top] = recolor([pixels[top]], dict(zip(TORCH_STICK_TOP, [glow[4], glow[5]])))[0]
	return pixels


def organized_backpack(backpack, hopper):
	tones = sorted({pixel[:3] for row in hopper for pixel in row if pixel[3]}, key=luminance, reverse=True)
	light, dark = tones[len(tones) // 4], tones[3 * len(tones) // 4]
	outline = min((pixel[:3] for row in backpack for pixel in row if pixel[3]), key=luminance)
	pixels = [[(pixel if not pixel[3] or pixel[:3] == outline else tuple(c * 3 // 4 for c in pixel[:3]) + (pixel[3],)) for pixel in row] for row in backpack]
	# Iron on the four corners of the body, each lit from the top left
	for y, x, tone in ((4, 2, light), (4, 3, light), (5, 2, dark), (4, 12, light), (4, 13, dark), (5, 13, dark),
			(13, 2, light), (14, 2, dark), (14, 3, dark), (13, 13, dark), (14, 12, dark), (14, 13, dark)):
		pixels[y][x] = tone + (255,)
	return pixels


def backpack_template(template, backpack):
	"""The plate in the backpack's leather, the arrow kept apart from it, in iron: both ramps sorted on their own."""
	arrow = {hex_color(c) for c in ("209ab8", "4bc9c9", "60e8e8")}
	plate = sorted({pixel[:3] for row in template for pixel in row if pixel[3]} - arrow, key=luminance, reverse=True)
	# The leather's tones only, without the clasp's beige
	leather = sorted({pixel[:3] for row in backpack for pixel in row if pixel[3] and pixel[0] > pixel[1] * 3 / 2 or pixel[3] and luminance(pixel[:3]) < 40}, key=luminance, reverse=True)
	mapping = {tone: leather[i * len(leather) // len(plate)] for i, tone in enumerate(plate)}
	mapping.update(zip(sorted(arrow, key=luminance), (hex_color("8a8f96"), hex_color("c6ccd6"), hex_color("ffffff"))))
	return recolor(template, mapping)


def main(jar_path):
	with zipfile.ZipFile(jar_path) as jar:
		clock = read_png(jar.read("assets/minecraft/textures/item/clock_00.png"))
		lightning_rod = read_png(jar.read("assets/minecraft/textures/block/lightning_rod.png"))
		copper_ingot = read_png(jar.read("assets/minecraft/textures/item/copper_ingot.png"))
		compass = read_png(jar.read("assets/minecraft/textures/item/compass_16.png"))
		tendril = read_png(jar.read("assets/minecraft/textures/block/sculk_sensor_tendril_active.png"))
		spyglass = read_png(jar.read("assets/minecraft/textures/item/spyglass.png"))
		spyglass_model = read_png(jar.read("assets/minecraft/textures/item/spyglass_model.png"))
		book = read_png(jar.read("assets/minecraft/textures/item/book.png"))
		torch = read_png(jar.read("assets/minecraft/textures/block/torch.png"))
		hopper = read_png(jar.read("assets/minecraft/textures/block/hopper_outside.png"))
		template = read_png(jar.read("assets/minecraft/textures/item/netherite_upgrade_smithing_template.png"))
	backpack = read_png((TARGETS[0] / "backpack.png").read_bytes())
	item, model = naturalist_spyglass(spyglass, spyglass_model, book, tendril)
	write_png(TARGETS[0] / "naturalist_spyglass_model.png", model)
	for target in TARGETS:
		target.mkdir(parents=True, exist_ok=True)
		write_png(target / "barometer.png", barometer(clock, lightning_rod, copper_ingot))
		write_png(target / "sculk_compass.png", sculk_compass(compass, tendril))
		write_png(target / "naturalist_spyglass.png", item)
		write_png(target / "sculk_torch.png", sculk_torch(torch, tendril))
		write_png(target / "organized_backpack.png", organized_backpack(backpack, hopper))
		write_png(target / "backpack_upgrade_smithing_template.png", backpack_template(template, backpack))


if __name__ == "__main__":
	main(sys.argv[1])
