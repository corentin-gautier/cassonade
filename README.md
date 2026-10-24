# Vanilla Sugar

**Minecraft, minus the friction.** A Fabric mod for Minecraft 26.3 that fixes the hundred small annoyances of survival and leaves everything else alone. No new ores, no magic, no machines: the same game you know, just smoother.

**[Website](https://corentin-gautier.github.io/mc-vanilla-sugar/)** · [Modrinth](https://modrinth.com/mod/vanilla-sugar-mod) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/vanilla-sugar) · [GitHub releases](https://github.com/corentin-gautier/mc-vanilla-sugar/releases)

![Vanilla Sugar](site/assets/og.jpg)

## The idea

- **Same progression.** No new resources, tiers or dimensions. Every new feature is unlocked by equipping an item you already know (a compass, a clock) or one crafted from vanilla items (a backpack, a world map), in its own equipment slot.
- **Every small annoyance, fixed.** Leaves that take minutes to decay, "Too Expensive!", boats stuck on a pixel, the enchanting table's "…?": each fix is small, together they change how the game feels.
- **Feels like it was always there.** The interface keeps Minecraft's pixel style and sounds, features only show up once you equip their item, and every key can be remapped.

What Vanilla Sugar will never add: new ores, metals or gear tiers; magic, machines or tech trees; new mobs, biomes or dimensions; anything that makes the game easier to beat rather than nicer to play.

## Features

### Backpack

![Backpack](site/assets/screenshots/backpack-1280.avif)

54 more slots, as much as a large chest. Craft it from 4 leather, 4 string and a bundle:

```
 S L S     L = leather, S = string, B = bundle
 S B S
 L L L
```

- Wear it in its own equipment slot (Shift-click it in the inventory), then open it with `B` or from the tab at the top of the inventory. It opens in a leather panel where the recipe book usually sits; opening one closes the other.
- While it is open, Shift-click sends items straight into it.
- Like a Shulker box, it keeps its contents when placed in a chest or dropped. A backpack or a Shulker box cannot go inside a backpack.
- On death, it drops like the rest of your inventory, unless `keepInventory` is on.

### Compass minimap

![Compass minimap](site/assets/screenshots/minimap-1280.avif)

Equip a compass in its slot to get a minimap in the top-right corner, drawn with the colors of vanilla maps.

- `+` and `-` zoom from 32 to 256 blocks across. The biome and your coordinates are written underneath.
- The "N" button next to the minimap switches between north-up and a map that turns with you.
- Your markers show in the world, through terrain, with their name and distance. `N` cycles between icon and text, icon only, or hidden.
- In the Nether, the map shows the floor you stand on, not the bedrock ceiling.

### World map and markers

![World map and markers](site/assets/screenshots/worldmap-1280.avif)

Craft a world map from an empty map surrounded by eight sticks, and equip it in the map slot:

```
 S S S     E = empty map, S = stick
 S E S
 S S S
```

- `M` (or the tab at the top of the inventory) opens a full-screen map of every chunk you have explored in this dimension, under a fog of war.
- Drag to move, `+` / `-` or the mouse wheel to zoom, `Space` to recenter on yourself.
- Right-click to place a marker, with an optional name and one of the 16 dye colors; right-click a marker to remove it. Markers show on the world map, on the minimap and in the world. You can have up to 64, and they are kept on death.
- Exploration is recorded from the first minute, even before you craft the map.

### Clock

![Clock](site/assets/screenshots/clock-1280.avif)

Equip a clock to see, in the top-left corner:

- the time and the current weather;
- **the next weather change**, read from the server's own weather timers;
- the day count, and a small bed when you can sleep.

In the Nether, the clock spins, just like in vanilla.

### An inventory that keeps up

![Inventory](site/assets/screenshots/inventory-1280.avif)

- **Sort** a chest, barrel, ender chest, Shulker box, your backpack or your inventory with the small buttons next to the titles, or middle-click a slot.
- **Stash**: one click puts into the open chest the items it already holds.
- **Shift-drag** across a row to move every stack you pass over.
- **Bundles**: drag a bundle over items to fill it, or right-drag over empty slots to empty it.
- **Previews**: see inside bundles, Shulker boxes, backpacks and filled maps on hover.
- **Hand refill**: when the stack in your hand runs out (last block placed, last food eaten, tool broken), a matching stack from your inventory takes its place.
- **Bulk crafting**: `Ctrl`-click (`Cmd` on Mac) a recipe in the recipe book to craft as many as your inventory allows.
- Soft sounds on inventory clicks, where vanilla stays silent.

### Right-click harvest

![Right-click harvest](site/assets/screenshots/harvest-1280.avif)

Right-click a ripe crop to harvest and replant it: wheat, carrots, potatoes, beetroots, nether wart and cocoa. You get the same drops as breaking it, Fortune included, minus the seed that goes back in the ground. Sneak to use the vanilla right-click.

### Elytra slot

![Elytra slot](site/assets/screenshots/elytra-1280.avif)

Elytra get their own slot, next to the offhand: fly without taking your chestplate off. Right-click them or Shift-click them in the inventory to equip them. They fly, wear out and take Mending exactly as in vanilla, and other players see your wings over your armor.

### Filtered hopper

![Filtered hopper](site/assets/screenshots/hopper-1280.avif)

Every hopper gets a filter slot: put one item in, and the hopper only accepts that item. A row of hoppers with filtered hoppers underneath makes a full item sorter, no redstone needed.

- The filter compares the item, not its enchantments or name.
- Speed, redstone locking and comparators stay vanilla.
- Filtered hoppers are recognizable by the iron grate inside.

### Quest journal and advancements

![Quest journal](site/assets/screenshots/quests-1280.avif)

- **Quest journal** (`J`): twelve chapters that walk you from your first night to the toughest challenges, following Minecraft's own advancements. It is offered when you start a world, and you can turn it on or off at any time.
- **Current quest**: shown under the minimap with its progress. Click it to open the journal; `K` shrinks it, hides it or shows it again.
- **Advancements screen** (`L`): every advancement in one book, with what is left for each (biomes to visit, mobs to kill…) and a filter for what is done or still to do.
- **Celebrations**: earned advancements are announced in the middle of the screen, bigger for goals and challenges.

![Advancements screen](site/assets/screenshots/advancements-1280.avif)

### Combat awareness

![Combat awareness](site/assets/screenshots/combat-1280.avif)

- A health bar over mobs in a fight, which shows the damage you just dealt.
- A golden "!" when a mob starts chasing you.
- An eye in the corner of the screen while anything is hunting you, even out of sight, with the number of pursuers.

### Small tweaks

| | Vanilla | With Vanilla Sugar |
|---|---|---|
| Cutting down a tree | Leaves decay one by one for minutes | Leaves fall right away, drops included |
| Repairing at the anvil | "Too Expensive!" | Any repair you can pay for |
| Enchanting table | One enchantment, then "…?" | Every enchantment the option will give |
| Mending and Infinity | Incompatible on a bow | Work together |
| Boats on land | Stuck on a 1-pixel step | Glide over paths, farmland, carpets, snow |
| Double doors | Open each door separately | Both open together |
| Composter | 1 bone meal, after a delay | 8 bone meal, right away |
| Bone meal | Does nothing on sugar cane and cactus | Grows them by one block |
| Going to bed | 5 seconds to skip the night | 2.5 seconds |
| Death screen | Buttons locked for a second | Respawn immediately |
| Nether portal | 4 seconds of warping and noise | Step in, you're through |
| Moving villagers | Boats, rails, trapping | Put them on a lead |

### Fair recipes

Some vanilla recipes eat a whole block for a single button. Vanilla Sugar only changes how many items a recipe gives, never its shape or when you unlock it.

| Item | Vanilla | Vanilla Sugar |
|---|---|---|
| Buttons | 1 block → 1 | 1 block → 16 |
| Pressure plates (except weighted) | 2 blocks → 1 | 2 blocks → 16 |
| Wooden trapdoors | 6 planks → 2 | 6 planks → 24 |
| Sticks | 2 planks → 4 | 2 planks → 32 |
| Ladders | 7 sticks → 3 | 7 sticks → 8 |
| Stairs | 6 blocks → 4 | 6 blocks → 8 |
| Wood (bark) | 4 logs → 3 | 4 logs → 4 |
| Signs | 6 planks + 1 stick → 3 | 6 planks + 1 stick → 12 |
| Wooden doors | 6 planks → 3 | 6 planks → 12 |
| Bowls | 3 planks → 4 | 3 planks → 12 |

Stairs and slabs can also be crafted back into full blocks, without loss: 4 stairs give 3 blocks, 2 slabs side by side give 1.

### A refined interface

![A refined interface](site/assets/screenshots/interface-1280.avif)

A cleaner, warmer look for every screen, still in Minecraft's style: sepia wood panels, dark slots and cream text across the HUD, inventories, buttons and tooltips; the world softly blurred behind open windows; a recipe book that looks like a book, and an animated book on the enchanting table.

## Controls

Every key can be changed in Options → Controls → Key Binds → Vanilla Sugar. `M`, `B`, `N`, `+` and `-` follow the letter printed on your keyboard, so they work on AZERTY and other layouts too.

| Key | Action | Needs |
|---|---|---|
| `B` | Open / close the backpack | Backpack |
| `M` | Open the world map | World map |
| `+` / `-` | Zoom the minimap | Compass |
| `N` | Show / hide markers in the world | Compass |
| `J` | Open the quest journal | |
| `L` | Open the advancements screen | |
| `K` | Shrink / hide the current quest | |
| Middle click | Sort the part of the window under the cursor (Survival) | |
| `Ctrl` / `Cmd` + click | Craft a recipe as many times as possible | |

Display choices (minimap zoom, map rotation, marker display, quest tracker) are remembered between sessions.

## Install

Requires **Minecraft 26.3**, **[Fabric Loader](https://fabricmc.net/use/) 0.20.1** or later, **[Fabric API](https://modrinth.com/mod/fabric-api)** and **Java 25**.

1. Install the Fabric Loader for Minecraft 26.3.
2. Download Fabric API and Vanilla Sugar from [Modrinth](https://modrinth.com/mod/vanilla-sugar-mod), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/vanilla-sugar) or the [GitHub releases](https://github.com/corentin-gautier/mc-vanilla-sugar/releases).
3. Put both `.jar` files in the `mods` folder of your Minecraft directory.
4. Launch the game with the Fabric profile.

**Multiplayer:** install the mod on the server and on every player's game. In singleplayer, there is nothing else to do. If you join a server without the mod, the game stays playable and falls back to vanilla where the server is needed: no extra slots, backpack or sort buttons, no weather forecast, no "!" or eye in combat, and the vanilla enchanting and advancement information.

## Questions and support

- **Found a bug or have an idea?** [Open an issue](https://github.com/corentin-gautier/mc-vanilla-sugar/issues).
- **Can I use it in my modpack?** Yes, see the license below.
- **Enjoying the mod?** You can support its development with a coffee on [Ko-fi](https://ko-fi.com/corentingautier).

Want to build the mod, fix something or translate it? See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Vanilla Sugar is released under the [GPL-3.0](LICENSE): you can include it in a modpack, change it or reuse its code, as long as the resulting version stays under the GPL-3.0, credits its author and publishes its source. Version 0.1.0-alpha.1, released under CC0, remains so.

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
