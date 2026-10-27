#!/usr/bin/env python3
"""Generate Cassonade's recipe overrides from the vanilla client jar.

Usage: python3 tools/rebalance_recipes.py <path/to/minecraft-<version>.jar>

Vanilla recipes are copied as-is and only their output count is changed, so
groups, categories and unlock advancements stay identical to vanilla. Re-run
after updating Minecraft to pick up new wood/stone types.
"""
import json
import pathlib
import shutil
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/data"
OVERRIDES = ROOT / "minecraft/recipe"
UNCRAFT_RECIPES = ROOT / "cassonade/recipe/uncraft"
UNCRAFT_ADVANCEMENT = ROOT / "cassonade/advancement/recipes/building_blocks/uncraft.json"
WOOL_ADVANCEMENT = ROOT / "cassonade/advancement/recipes/misc/uncraft_wool.json"

# (predicate on (name, recipe), vanilla count, new count, why). A new count given as
# per_block(n) is n for each full block of the recipe
def per_block(count):
    return lambda recipe: count * blocks(recipe)


RULES = [
    (lambda n, r: n.endswith("_button"), 1, 16,
     "a button is a tiny piece of one plank/stone"),
    (lambda n, r: r.get("group") == "wooden_trapdoor", 2, per_block(4),
     "a trapdoor is a thin panel: 4 per plank"),
    (lambda n, r: n.endswith("_stairs") and r["type"] == "minecraft:crafting_shaped", 4, 8,
     "a stair is 3/4 of a block: 6 blocks -> 8 stairs conserves material"),
    (lambda n, r: r.get("group") == "bark", 3, 4,
     "4 logs -> 4 wood, no material lost"),
    (lambda n, r: n == "ladder", 3, 8, "7 sticks for 3 ladders felt punitive"),
    (lambda n, r: n.endswith("_pressure_plate") and "weighted" not in n, 1, per_block(8),
     "a thin plate: 8 per block"),
    (lambda n, r: n == "stick", 4, per_block(16), "16 sticks per plank"),
    (lambda n, r: r.get("group") == "wooden_sign", 3, 12, "a sign is a thin board: 2 per plank"),
    (lambda n, r: r.get("group") == "wooden_door", 3, per_block(2),
     "a door is two trapdoors tall: 2 per plank"),
    (lambda n, r: n == "bowl", 4, per_block(4), "a bowl is a small carving: 4 per plank"),
    (lambda n, r: n == "paper", 3, 6, "3 sugar cane for 3 sheets ate whole farms for one bookshelf"),
    (lambda n, r: n == "chest", 1, 8, "a chest is a hollow box: 1 per plank"),
    (lambda n, r: n == "barrel", 1, 8, "a hollow barrel, 1 per plank like the chest"),
]

# Stairs and slabs crafted back into the block they come from, with no material lost: a stair is
# 3/4 of a block (6 blocks -> 8 stairs above), a slab half of one. The slab recipe lays the two
# slabs side by side, since vanilla already turns two stacked slabs into chiseled blocks.
UNCRAFTS = {
    "_stairs": (["##", "##"], 3),
    "_slab": (["##"], 1),
}


def blocks(recipe):
    """Ingredients in the pattern: these recipes only take full blocks."""
    return sum(len(row.replace(" ", "")) for row in recipe["pattern"])


def is_crafting(recipe):
    return recipe["type"] in ("minecraft:crafting_shaped", "minecraft:crafting_shapeless")


def source_block(name, recipe):
    """The block a vanilla stair or slab recipe is made of, or None if it isn't one."""
    if recipe["type"] != "minecraft:crafting_shaped" or len(recipe["key"]) != 1:
        return None
    suffix = next((suffix for suffix in UNCRAFTS if name.endswith(suffix)), None)
    if suffix is None:
        return None
    block = next(iter(recipe["key"].values()))
    if isinstance(block, list):
        # Quartz, purpur, sandstone: any variant makes the stair, it goes back to the plain block
        base = "minecraft:" + name.removesuffix(suffix)
        return next((b for b in (base, base + "_block") if b in block), None)
    return None if block.startswith("#") else block


def conflicts(pattern, item, recipes):
    """Vanilla crafting recipes already using this exact shape of this item."""
    return [
        name for name, recipe in recipes.items()
        if recipe["type"] == "minecraft:crafting_shaped"
        and [row.strip() for row in recipe["pattern"]] == pattern
        and list(recipe["key"].values()) == [item]
    ]


def write_uncrafts(recipes):
    shutil.rmtree(UNCRAFT_RECIPES, ignore_errors=True)
    UNCRAFT_RECIPES.mkdir(parents=True)

    written = {}
    for name, recipe in recipes.items():
        block = source_block(name, recipe)
        if block is None:
            continue
        pattern, count = next(v for suffix, v in UNCRAFTS.items() if name.endswith(suffix))
        item = recipe["result"]["id"]
        if clash := conflicts(pattern, item, recipes):
            print(f"skipped {item}: same shape as {', '.join(clash)}")
            continue
        uncraft = {
            "type": "minecraft:crafting_shaped",
            "category": "building",
            "group": "cassonade_uncraft",
            "key": {"#": item},
            "pattern": pattern,
            "result": {"count": count, "id": block},
        }
        (UNCRAFT_RECIPES / f"{name}.json").write_text(json.dumps(uncraft, indent=2) + "\n")
        written[item] = f"cassonade:uncraft/{name}"

    # One advancement unlocks them all in the recipe book, as soon as a stair or slab is picked up
    UNCRAFT_ADVANCEMENT.parent.mkdir(parents=True, exist_ok=True)
    UNCRAFT_ADVANCEMENT.write_text(json.dumps({
        "parent": "minecraft:recipes/root",
        "criteria": {
            "has_stairs_or_slab": {
                "conditions": {"items": [{"items": sorted(written)}]},
                "trigger": "minecraft:inventory_changed",
            },
        },
        "rewards": {"recipes": sorted(written.values())},
    }, indent=2) + "\n")
    print(f"{len(written)} uncraft recipes written")

    # Wool back into the 4 strings it takes: early on, string is otherwise only dropped by spiders
    # and cobwebs, while sheep are everywhere. Any color works, the dye is lost.
    (UNCRAFT_RECIPES / "wool.json").write_text(json.dumps({
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": ["#minecraft:wool"],
        "result": {"count": 4, "id": "minecraft:string"},
    }, indent=2) + "\n")
    WOOL_ADVANCEMENT.parent.mkdir(parents=True, exist_ok=True)
    WOOL_ADVANCEMENT.write_text(json.dumps({
        "parent": "minecraft:recipes/root",
        "criteria": {
            "has_wool": {
                "conditions": {"items": [{"items": "#minecraft:wool"}]},
                "trigger": "minecraft:inventory_changed",
            },
        },
        "rewards": {"recipes": ["cassonade:uncraft/wool"]},
    }, indent=2) + "\n")
    print("wool -> string recipe written")



def main(jar_path):
    shutil.rmtree(OVERRIDES, ignore_errors=True)
    OVERRIDES.mkdir(parents=True)

    with zipfile.ZipFile(jar_path) as jar:
        recipes = {
            entry.rsplit("/", 1)[1][:-5]: json.loads(jar.read(entry))
            for entry in sorted(jar.namelist())
            if entry.startswith("data/minecraft/recipe/") and entry.endswith(".json")
        }

    changed = {}
    for name, recipe in recipes.items():
        if not is_crafting(recipe):
            continue

        for matches, old, new, _ in RULES:
            if matches(name, recipe) and recipe["result"].get("count", 1) == old:
                new = new(recipe) if callable(new) else new
                recipe["result"]["count"] = new
                (OVERRIDES / f"{name}.json").write_text(json.dumps(recipe, indent=2) + "\n")
                changed[name] = f"{old} -> {new}"
                break

    for name, what in sorted(changed.items()):
        print(f"{name}: {what}")
    print(f"{len(changed)} recipes written")

    write_uncrafts(recipes)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
