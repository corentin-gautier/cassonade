"""Writes the towns from the game's villages: a structure per biome (data/cassonade/worldgen/structure/town_<biome>.json)
reaching farther and deeper than the village, and its own pools for houses and streets, swapped in through the
structure's pool aliases so the village's templates and jigsaws stay vanilla's:

- houses: the village's, without the empty slots, so every spot along a street that has room gets a house;
- streets: the village's, each as much likelier as it has spots for houses along it (read from its template's
  jigsaws), so the town is mostly streets lined with houses on both sides. Making the crossroads likelier instead
  was tried: streets ran into each other and stopped, the town ended smaller.

The zombie village's houses and streets get the same. Run it again after changing it, never edit its output by hand.

Usage:
    python3 tools/town_pools.py [path to the Minecraft common jar]
"""

import gzip
import io
import json
import pathlib
import struct
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
DATA = ROOT / "src/main/resources/data/cassonade/worldgen"
BIOMES = ["plains", "desert", "savanna", "snowy", "taiga"]
# How much deeper and farther than the village (depth 6, 80 blocks): 116 is the most a structure whose terrain is
# adapted to it may reach
DEPTH = 10
REACH = 116


def common_jar():
	jars = sorted(p for p in (ROOT / ".gradle/loom-cache/minecraftMaven/net/minecraft").glob("minecraft-common-*/*/*.jar")
		if not p.name.endswith("-sources.jar"))
	if not jars:
		sys.exit("No Minecraft jar found: run ./gradlew build once, or pass the jar's path.\n\n" + __doc__)
	return jars[-1]


def dump(path, data):
	path.parent.mkdir(parents=True, exist_ok=True)
	path.write_text(json.dumps(data, indent=2) + "\n")


def houses(pool):
	pool["elements"] = [e for e in pool["elements"] if e["element"]["element_type"] != "minecraft:empty_pool_element"]
	return pool


def nbt(data):
	"""A structure template's compound, as plain dicts and lists."""
	stream = io.BytesIO(gzip.decompress(data))

	def unpack(fmt):
		return struct.unpack(fmt, stream.read(struct.calcsize(fmt)))[0]

	def string():
		return stream.read(unpack(">H")).decode()

	def payload(tag):
		if tag in (1, 2, 3, 4, 5, 6):
			return unpack(">" + "bhiqfd"[tag - 1])
		if tag in (7, 11, 12):
			count = unpack(">i")
			return stream.read(count * {7: 1, 11: 4, 12: 8}[tag])
		if tag == 8:
			return string()
		if tag == 9:
			kind = stream.read(1)[0]
			return [payload(kind) for _ in range(unpack(">i"))]
		compound = {}
		while (kind := stream.read(1)[0]) != 0:
			name = string()
			compound[name] = payload(kind)
		return compound

	stream.read(1)
	string()
	return payload(10)


def house_spots(template):
	"""How many jigsaws along the street lead to a house."""
	return sum(1 for block in template["blocks"] if block.get("nbt", {}).get("pool", "").endswith("/houses"))


def streets(pool, jar):
	for e in pool["elements"]:
		location = e["element"].get("location")
		if location:
			path = "data/minecraft/structure/" + location.split(":")[1] + ".nbt"
			e["weight"] *= max(1, house_spots(nbt(jar.read(path))))
	return pool


def main(jar_path):
	with zipfile.ZipFile(jar_path) as jar:
		def read(path):
			return json.loads(jar.read("data/minecraft/worldgen/" + path))

		for biome in BIOMES:
			aliases = []
			for variant in ("", "zombie/"):
				for name, change in (("houses", houses), ("streets", lambda pool: streets(pool, jar))):
					vanilla = f"village/{biome}/{variant}{name}"
					town = f"town/{biome}/{variant}{name}"
					dump(DATA / f"template_pool/{town}.json", change(read(f"template_pool/{vanilla}.json")))
					aliases.append({"type": "minecraft:direct", "alias": f"minecraft:{vanilla}", "target": f"cassonade:{town}"})
			structure = read(f"structure/village_{biome}.json")
			structure["size"] = DEPTH
			structure["max_distance_from_center"] = REACH
			structure["pool_aliases"] = aliases
			dump(DATA / f"structure/town_{biome}.json", structure)
	print("Towns written for", ", ".join(BIOMES))


if __name__ == "__main__":
	main(sys.argv[1] if len(sys.argv) > 1 else common_jar())
