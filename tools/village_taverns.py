#!/usr/bin/env python3
"""Writes the villages' taverns, built in place of their churches: structure templates saved over vanilla's temple
templates (data/minecraft/structure/village/<biome>/houses/<biome>_temple_<n>.nbt), so the village pools, their weights
and the zombie villages' processors stay the game's own.

A tavern is a timber-framed hall in the village's materials (flat-roofed in the desert): a bar along one side with
kegs set in the wall behind it, some holding the tavern's stores and one the temple's loot, the brewing stand in the
counter for the tavern keeper (the cleric), tables with stair chairs, a campfire in a brick fireplace whose chimney
goes through the roof, and
lanterns hanging from the ridge. Like the church, it has a tower, rising above the roof: the porch, at the front,
which leads into the hall. The entrance faces west (x = 0), the side the jigsaw connects to the street.

Where the biome has a small church and a large one, the small one becomes an inn instead, so the two still look
apart: the same hall without the tower, its door under an awning on two posts, a short counter with three kegs and
a single table by the fire.

Usage: python3 tools/village_taverns.py
"""
import gzip
import io
import pathlib
import struct

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/data/minecraft/structure/village"
DATA_VERSION = 5023
TEMPLE_LOOT = "minecraft:chests/village/village_temple"
KEG_LOOT = "cassonade:chests/tavern_keg"
PORCH = 4  # the tower's depth inside its walls, minus one: it stands from x = 1 to x = PORCH + 1, the hall's front wall

BIOMES = {
	"plains": dict(step="cobblestone_stairs", base="cobblestone", frame="oak_log", fill="white_terracotta", gable="oak_planks", floor="oak_planks", wood="oak", roof="oak"),
	"taiga": dict(step="cobblestone_stairs", base="cobblestone", frame="spruce_log", fill="spruce_planks", gable="spruce_planks", floor="spruce_planks", wood="spruce", roof="spruce"),
	"snowy": dict(step="spruce_stairs", base="diorite", frame="stripped_spruce_log", fill="spruce_planks", gable="spruce_planks", floor="spruce_planks", wood="spruce", roof="spruce"),
	"savanna": dict(step="acacia_stairs", base="acacia_planks", frame="acacia_log", fill="yellow_terracotta", gable="acacia_planks", floor="acacia_planks", wood="acacia", roof="acacia"),
	"desert": dict(step="smooth_sandstone_stairs", base="cut_sandstone", frame="cut_sandstone", fill="smooth_sandstone", gable=None, floor="smooth_sandstone", wood="jungle", roof=None),
}

STREETS = "minecraft:village/plains/streets"
# Each temple replaced: its biome, the hall's inside width (odd, along z) and length (along x), and its jigsaw's
# target pool, as vanilla's template had it
TEMPLES = [
	("plains/houses/plains_temple_3", "plains", 5, 8, STREETS),
	("plains/houses/plains_temple_4", "plains", 7, 10, "minecraft:empty"),
	("desert/houses/desert_temple_1", "desert", 5, 8, "minecraft:empty"),
	("desert/houses/desert_temple_2", "desert", 7, 10, "minecraft:empty"),
	("savanna/houses/savanna_temple_1", "savanna", 7, 10, "minecraft:empty"),
	("savanna/houses/savanna_temple_2", "savanna", 5, 8, "minecraft:empty"),
	("snowy/houses/snowy_temple_1", "snowy", 5, 10, "minecraft:empty"),
	("taiga/houses/taiga_temple_1", "taiga", 7, 10, "minecraft:empty"),
	("taiga/zombie/houses/taiga_temple_1", "taiga", 7, 10, "minecraft:empty"),
]


def tavern(m, width, length, pool, inn=False):
	"""The tavern's blocks, {(x, y, z): (id, properties, block entity)}; the inn's with inn."""
	blocks = {}

	def put(x, y, z, name, nbt=None, **props):
		blocks[(x, y, z)] = ("minecraft:" + name, {k: str(v).lower() for k, v in props.items()}, nbt)

	wood = m["wood"]
	flat = m["roof"] is None
	back = length + 2  # back wall; the front wall is x = 1
	side = width + 2  # south wall; the north wall is z = 1
	zc = (width + 3) // 2  # the door's and the ridge's row
	top = 5 if flat else 4 + zc  # the flat roof, or the ridge
	tower = top + 4  # the tower's roof, above the ridge
	# The inn's front is one block deep, under its awning, rather than the tower
	front = 1 if inn else PORCH
	size = (length + 4 + front, (top if inn else tower) + 4, width + 4)

	# Inside, cleared up to the roof
	for x in range(2, back):
		for z in range(2, side):
			ceiling = top if flat else 4 + min(z, width + 3 - z)
			for y in range(1, ceiling):
				put(x, y, z, "air")
	# Floor, plinth, frame and walls
	for x in range(1, back + 1):
		for z in range(1, side + 1):
			wall = x in (1, back) or z in (1, side)
			put(x, 0, z, m["base"] if wall else m["floor"])
			if not wall:
				continue
			corner = x in (1, back) and z in (1, side)
			log = m["frame"].endswith("_log")
			post = corner or (z in (1, side) and (x - 1) % 4 == 0)
			for y in range(1, 5):
				if post:
					put(x, y, z, m["frame"], **({"axis": "y"} if log else {}))
				elif y == 4:
					put(x, y, z, m["frame"], **({"axis": "x" if z in (1, side) else "z"} if log else {}))
				elif y == 1:
					put(x, y, z, m["base"])
				else:
					put(x, y, z, m["fill"])
	# Windows: one between each pair of posts on the sides
	for x in range(2, back):
		if (x - 1) % 4 == 2:
			for y in (2, 3):
				put(x, y, side, "glass_pane", east=True, west=True)
			put(x, 3, 1, "glass_pane", east=True, west=True)

	# Roof
	if flat:
		for x in range(1, back + 1):
			for z in range(1, side + 1):
				put(x, top, z, m["fill"])
				if x in (1, back) or z in (1, side):
					put(x, top + 1, z, "smooth_sandstone_slab", type="bottom")
	else:
		roof = m["roof"]
		for k in range(zc + 1):
			y = 4 + k
			for x in range(0, back + 2):
				if k == zc:
					put(x, y, k, roof + "_slab", type="bottom")
				else:
					put(x, y, k, roof + "_stairs", facing="south", half="bottom")
					put(x, y, width + 3 - k, roof + "_stairs", facing="north", half="bottom")
			# Gable ends, above the walls
			for z in range(k + 1, width + 3 - k) if k > 0 else ():
				for x in (1, back):
					put(x, y, z, m["gable"])

	# Bar: kegs in the north wall, the lower row holding the tavern's stores every other one and the church's loot
	# in the last; a lane behind the counter, open at the door's end
	# The inn's counter is short, three kegs behind it: two of stores and the church's loot
	first = length - 2 if inn else 4
	for x in range(first, length + 1):
		for y in (1, 2):
			loot = None
			if y == 1 and x == length:
				loot = {"id": "minecraft:barrel", "LootTable": TEMPLE_LOOT}
			elif y == 1 and (inn or x % 2 == 0):
				loot = {"id": "minecraft:barrel", "LootTable": KEG_LOOT}
			put(x, y, 1, "barrel", loot, facing="south")
	for x in range(first - 1 if inn else 3, length + 1):
		put(x, 1, 3, wood + "_stairs", facing="north", half="top")
	# In the counter's line rather than on it: a villager only takes a workstation it can stand level with
	stand = first - 1 if inn else 6
	put(stand, 1, 3, "brewing_stand", {"id": "minecraft:brewing_stand"})
	put(stand + 1 if inn else 4, 2, 3, "lantern", hanging=False)
	put(length - 1, 2, 3, "candle", candles=3, lit=True)
	# Tables against the south side, a chair on each side; the inn's one, by the fire
	row = width + 1 - (width - 5) // 2
	for tx in ([length - 1] if inn else range(3, length, 4)):
		put(tx, 1, row, wood + "_fence")
		put(tx, 2, row, wood + "_pressure_plate")
		put(tx - 1, 1, row, wood + "_stairs", facing="west", half="bottom")
		put(tx + 1, 1, row, wood + "_stairs", facing="east", half="bottom")
		put(tx, 1, row - 1, wood + "_stairs", facing="north", half="bottom")
		if row + 1 < side:
			put(tx, 1, row + 1, wood + "_stairs", facing="south", half="bottom")
	# Lanterns hanging over the aisle
	for x in range(3, length, 4):
		put(x, 4, zc, "lantern", hanging=True)
		for y in range(5, top):
			put(x, y, zc, "iron_chain", axis="y")
	# Fireplace and chimney at the back
	fire = length + 1
	put(fire, 1, zc, "campfire", {"id": "minecraft:campfire"}, facing="west", lit=True, signal_fire=False)
	put(fire, 0, zc, m["base"])
	# Beside the fire, then from above it a breast of bricks around the flue, up through the roof
	for y in range(1, top + 3):
		for dx in (-1, 0, 1):
			for dz in (-1, 0, 1):
				if (dx, dz) != (0, 0) and (y >= 3 or dx == 0 or (dx, dz) == (1, 0)):
					put(fire + dx, y, zc + dz, "bricks")
		if y >= 2:
			put(fire, y, zc, "air")
	if inn:
		return size, awning(m, blocks, zc, top, pool)
	# The hall's door becomes the way into it from the tower
	put(1, 1, zc, "air")
	put(1, 2, zc, "air")

	# The tower stands before the hall, as the church's did: it is the porch, its door on the street
	hall = blocks.copy()
	blocks.clear()
	for (x, y, z), block in hall.items():
		blocks[(x + PORCH, y, z)] = block
	log = m["frame"].endswith("_log")
	for x in range(1, PORCH + 2):
		for z in range(zc - 2, zc + 3):
			wall = x in (1, PORCH + 1) or z in (zc - 2, zc + 2)
			put(x, 0, z, m["base"] if wall else m["floor"])
			for y in range(1, tower):
				if not wall:
					put(x, y, z, "air")
				elif x in (1, PORCH + 1) and z in (zc - 2, zc + 2):
					put(x, y, z, m["frame"], **({"axis": "y"} if log else {}))
				elif y % 4 == 0:
					put(x, y, z, m["frame"], **({"axis": "z" if x in (1, PORCH + 1) else "x"} if log else {}))
				elif y == 1:
					put(x, y, z, m["base"])
				else:
					put(x, y, z, m["fill"])
	# Its door, the way into the hall behind, and windows high up on its four sides
	put(1, 1, zc, wood + "_door", half="lower", facing="east", hinge="right")
	put(1, 2, zc, wood + "_door", half="upper", facing="east", hinge="right")
	put(PORCH + 1, 1, zc, "air")
	put(PORCH + 1, 2, zc, "air")
	middle = 1 + PORCH // 2
	for y in (tower - 3, tower - 2):
		put(1, y, zc, "glass_pane", north=True, south=True)
		put(PORCH + 1, y, zc, "glass_pane", north=True, south=True)
		put(middle, y, zc - 2, "glass_pane", east=True, west=True)
		put(middle, y, zc + 2, "glass_pane", east=True, west=True)
	put(middle, tower - 1, zc, "lantern", hanging=True)
	put(0, 0, zc, "jigsaw", {"id": "minecraft:jigsaw", "joint": "aligned", "name": "minecraft:building_entrance",
		"target": "minecraft:building_entrance", "pool": pool, "final_state": "minecraft:" + m["step"] + "[facing=east]"}, orientation="west_up")
	for y in (1, 2, 3):
		put(0, y, zc, "air")
	# Its roof: flat behind a parapet in the desert, a pyramid elsewhere, over the walls by one block
	if flat:
		for x in range(1, PORCH + 2):
			for z in range(zc - 2, zc + 3):
				put(x, tower, z, m["fill"])
				if x in (1, PORCH + 1) or z in (zc - 2, zc + 2):
					corner = x in (1, PORCH + 1) and z in (zc - 2, zc + 2)
					put(x, tower + 1, z, m["frame"] if corner else "smooth_sandstone_slab", **({} if corner else {"type": "bottom"}))
	else:
		roof = m["roof"]
		for k in range(4):
			x0, x1, z0, z1 = k, PORCH + 2 - k, zc - 3 + k, zc + 3 - k
			for x in range(x0, x1 + 1):
				for z in range(z0, z1 + 1):
					if k == 3:
						put(x, tower + k, z, roof + "_slab", type="bottom")
					elif x == x0:
						put(x, tower + k, z, roof + "_stairs", facing="east", half="bottom")
					elif x == x1:
						put(x, tower + k, z, roof + "_stairs", facing="west", half="bottom")
					elif z == z0:
						put(x, tower + k, z, roof + "_stairs", facing="south", half="bottom")
					elif z == z1:
						put(x, tower + k, z, roof + "_stairs", facing="north", half="bottom")
					else:
						put(x, tower + k, z, m["gable"])
	return size, blocks


def awning(m, hall, zc, top, pool):
	"""The inn's front: the hall moved back a block, its door in the front wall under an awning on two posts."""
	blocks = {}

	def put(x, y, z, name, nbt=None, **props):
		blocks[(x, y, z)] = ("minecraft:" + name, {k: str(v).lower() for k, v in props.items()}, nbt)

	for (x, y, z), block in hall.items():
		blocks[(x + 1, y, z)] = block
	wood = m["wood"]
	put(2, 1, zc, wood + "_door", half="lower", facing="east", hinge="right")
	put(2, 2, zc, wood + "_door", half="upper", facing="east", hinge="right")
	# The step before the door, the posts at the awning's corners, the awning and a lantern under it
	for z in range(zc - 2, zc + 3):
		put(1, 0, z, m["base"])
		for y in (1, 2):
			put(1, y, z, wood + "_fence" if z in (zc - 2, zc + 2) else "air")
		put(1, 3, z, wood + "_slab", type="bottom")
	# Beside the way in rather than over it, where it would be in the way of whoever walks in
	put(1, 2, zc + 1, "lantern", hanging=True)
	put(0, 0, zc, "jigsaw", {"id": "minecraft:jigsaw", "joint": "aligned", "name": "minecraft:building_entrance",
		"target": "minecraft:building_entrance", "pool": pool, "final_state": "minecraft:" + m["step"] + "[facing=east]"}, orientation="west_up")
	for y in (1, 2, 3):
		put(0, y, zc, "air")
	return blocks


def nbt(value, kind=None):
	"""A tag's payload: dict → compound, list of int → int list (sizes, positions), str, int."""
	if isinstance(value, dict):
		out = b""
		for key, item in value.items():
			tag, payload = nbt(item)
			name = key.encode()
			out += struct.pack(">bH", tag, len(name)) + name + payload
		return 10, out + b"\0"
	if isinstance(value, list):
		if all(isinstance(item, int) for item in value) and value:
			return 9, struct.pack(">bi", 3, len(value)) + b"".join(struct.pack(">i", item) for item in value)
		payloads = [nbt(item)[1] for item in value]
		return 9, struct.pack(">bi", 10, len(value)) + b"".join(payloads)
	if isinstance(value, str):
		data = value.encode()
		return 8, struct.pack(">H", len(data)) + data
	if isinstance(value, int):
		return 3, struct.pack(">i", value)
	raise TypeError(value)


def save(path, size, blocks):
	palette = []
	index = {}
	entries = []
	for (x, y, z), (name, props, entity) in sorted(blocks.items(), key=lambda item: (item[0][1], item[0][2], item[0][0])):
		key = (name, tuple(sorted(props.items())))
		if key not in index:
			index[key] = len(palette)
			palette.append({"id": name, "properties": props} if props else {"id": name})
		entry = {"pos": [x, y, z], "state": index[key]}
		if entity:
			entry["nbt"] = entity
		entries.append(entry)
	root = {"size": list(size), "entities": [], "blocks": entries, "palette": palette, "DataVersion": DATA_VERSION}
	_, payload = nbt(root)
	buffer = io.BytesIO()
	with gzip.GzipFile(fileobj=buffer, mode="wb", mtime=0) as file:
		file.write(struct.pack(">bH", 10, 0) + payload)
	path.parent.mkdir(parents=True, exist_ok=True)
	path.write_bytes(buffer.getvalue())


def main():
	for location, biome, width, length, pool in TEMPLES:
		# The smaller of a biome's two churches
		small = sum(1 for other in TEMPLES if other[1] == biome and "/zombie/" not in other[0]) > 1 and width == 5
		size, blocks = tavern(BIOMES[biome], width, length, pool, inn=small)
		save(ROOT / (location + ".nbt"), size, blocks)
		print(location, size, len(blocks), "blocks")


if __name__ == "__main__":
	main()
