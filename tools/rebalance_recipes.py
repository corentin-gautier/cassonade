#!/usr/bin/env python3
"""Generate Vanilla Sugar's recipe overrides from the vanilla client jar.

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

# (predicate on (name, recipe), vanilla count, new count, why)
RULES = [
    (lambda n, r: n.endswith("_button"), 1, 16,
     "a button is a tiny piece of one plank/stone"),
    (lambda n, r: r.get("group") == "wooden_trapdoor", 2, 8,
     "a trapdoor is a thin panel, 6 planks for 2 was absurd"),
    (lambda n, r: n.endswith("_stairs") and r["type"] == "minecraft:crafting_shaped", 4, 8,
     "a stair is 3/4 of a block: 6 blocks -> 8 stairs conserves material"),
    (lambda n, r: r.get("group") == "bark", 3, 4,
     "4 logs -> 4 wood, no material lost"),
    (lambda n, r: n == "ladder", 3, 8, "7 sticks for 3 ladders felt punitive"),
    (lambda n, r: n.endswith("_pressure_plate") and "weighted" not in n, 1, 8,
     "a thin plate should not cost two full blocks"),
]


def is_crafting(recipe):
    return recipe["type"] in ("minecraft:crafting_shaped", "minecraft:crafting_shapeless")


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
                recipe["result"]["count"] = new
                (OVERRIDES / f"{name}.json").write_text(json.dumps(recipe, indent=2) + "\n")
                changed[name] = f"{old} -> {new}"
                break

    for name, what in sorted(changed.items()):
        print(f"{name}: {what}")
    print(f"{len(changed)} recipes written")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
