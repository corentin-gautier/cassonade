#!/usr/bin/env python3
"""Writes the woodcutter's recipes, stonecutting recipes with a plank for input, from the vanilla
recipes, as rebalanced by tools/rebalance_recipes.py (data/minecraft/recipe): stairs, slabs, fences, fence gates,
doors, trapdoors, signs, buttons and pressure plates of every wood.

A piece gives what the crafting table gives per plank, rounded down and at least one, so cutting is never
worse than crafting and never a way to get more out of the same wood: that is, without loss.

Usage: python3 tools/woodcutter_recipes.py <path/to/minecraft-<version>.jar>
"""
import json
import pathlib
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/data"
SOURCE = ROOT / "minecraft/recipe"
TARGET = ROOT / "cassonade/recipe/woodcutter"
PIECES = ["stairs", "slab", "fence", "fence_gate", "door", "trapdoor", "sign", "button", "pressure_plate"]


def planks_used(recipe, plank):
    if recipe["type"] == "minecraft:crafting_shapeless":
        return sum(1 for ingredient in recipe["ingredients"] if ingredient == plank)
    keys = [key for key, ingredient in recipe["key"].items() if ingredient == plank]
    return sum(row.count(key) for row in recipe["pattern"] for key in keys)


def load_recipes(jar_path):
    """Every recipe by name: vanilla's, replaced by Cassonade's where it changes the output."""
    recipes = {}
    with zipfile.ZipFile(jar_path) as jar:
        for name in jar.namelist():
            if name.startswith("data/minecraft/recipe/") and name.endswith(".json"):
                recipes[pathlib.Path(name).stem] = json.loads(jar.read(name))
    for path in SOURCE.glob("*.json"):
        recipes[path.stem] = json.loads(path.read_text())
    return recipes


def main(jar_path):
    recipes = load_recipes(jar_path)
    for old in TARGET.glob("*.json"):
        old.unlink()
    TARGET.mkdir(parents=True, exist_ok=True)
    written = 0
    for name in sorted(recipes):
        if not name.endswith("_planks"):
            continue
        wood = name[:-len("_planks")]
        plank = f"minecraft:{wood}_planks"
        for piece in PIECES:
            recipe = recipes.get(f"{wood}_{piece}")
            if recipe is None:
                continue
            used = planks_used(recipe, plank)
            if used == 0:
                continue
            count = max(1, recipe["result"].get("count", 1) // used)
            result = recipe["result"]["id"]
            (TARGET / f"{wood}_{piece}.json").write_text(json.dumps({
                "type": "minecraft:stonecutting",
                "ingredient": plank,
                "result": {"count": count, "id": result},
            }, indent=2) + "\n")
            written += 1
    print(f"{written} woodcutter recipes written")


if __name__ == "__main__":
    main(sys.argv[1])
