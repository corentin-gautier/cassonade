# Vanilla Sugar

A Fabric mod for Minecraft 26.3 that fixes the game's small annoyances in one go, while staying as close as possible to the vanilla experience: no new ores, no magic, no machines. Only what is missing or inconsistent.

**[Website](https://corentin-gautier.github.io/mc-vanilla-sugar/)** · [Modrinth](https://modrinth.com/mod/vanilla-sugar-mod) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/vanilla-sugar) · [GitHub releases](https://github.com/corentin-gautier/mc-vanilla-sugar/releases)

## Features

| # | Feature | Status |
|---|---|---|
| 1 | **Backpack**: crafted from 4 leather + 4 string + 1 bundle. Equipped in a dedicated slot, it opens from a tab at the top of the inventory, in a leather panel on the left side where the recipe book usually sits, and holds 54 slots over 9 columns (twice the main inventory, like a large chest). | Done |
| 2 | **Compass slot**: an equipped compass shows a minimap, lets you place markers and shows the coordinates and the biome. | Done |
| 3 | **Map slot**: an equipped world map unlocks the world map (`M` key), which shows every chunk the player has visited. | Done |
| 4 | **Rebalanced recipes**: output amounts that match the materials used (see below). | Done |
| 5 | **Clock slot**: an equipped clock shows, in the top-left corner, the time, the weather, the next weather, the day and when you can sleep. | Done |
| 6 | **Storage**: sort a chest or the inventory (button or middle click), move several stacks in one Shift-drag, fill or empty a bundle in one drag, sounds for inventory gestures, stash into the open chest the items it already holds, preview the contents of Shulker boxes and of the backpack on hover, preview filled maps, refill the hand from the inventory, bulk crafting from the recipe book. | Done |
| 7 | **Right-click harvest**: right-clicking a ripe crop harvests and replants it. | Done |
| 8 | **Reworked interface**: a more polished, professional GUI that keeps the game's style. One art direction for the HUD and every window: wood and brown slots, a little more ornate than vanilla while staying understated (see below). | Done |
| 9 | **Elytra slot**: elytra are worn in their own slot, on top of the chestplate. | Done |
| 10 | **Small tweaks**: villagers on a lead, boats that climb small steps, an enchanting table that shows every enchantment, Mending and Infinity together, leaves that fall as soon as the tree is cut, no wait on the death screen, instant Nether portal, an anvil that is never "Too Expensive!", double doors, bone meal on sugar cane and cactus, sleeping twice as fast, a composter that is ready at once and gives 8 bone meal. | Done |
| 11 | **Filtered hopper**: every hopper has a filter slot; with an item in it, only that item goes through. No more redstone item sorters. | Done |
| 12 | **Quest journal and advancements**: an RPG-style journal, offered at the start of the game, guides the player from one advancement to the next with the current quest on the side of the screen; an Advancements screen lists every advancement; each earned advancement is celebrated in the middle of the screen. | Done |
| 13 | **Combat**: the health of mobs in a fight shows above their head, a "!" marks those that start chasing the player, and an eye in the top-left corner shows that you are being hunted. | Done |

Dedicated slots are a core principle: new features are unlocked by equipping a vanilla item (compass, clock) or one crafted from vanilla items (world map, backpack), not by magic. Vanilla progression stays the same.

### Equipment slots

The slots are grouped by use. Instruments that show something on screen are in the column above the offhand: compass, world map, clock. What is worn on the body follows the offhand along the bottom row: the elytra (see [Elytra](#elytra)) then the backpack.

- Shift-clicking one of these items in the inventory equips it. When the backpack is open, other items go into it (armor and shields keep their vanilla priority); when it is closed, Shift-click stays vanilla. Picked-up items always go to the inventory.
- Tabs at the top of the inventory open and close, in one click, the recipe book (in place of the vanilla button), the backpack and, with a world map equipped, the world map. The tab of the open panel is lighter and opens into the inventory. The open book also closes with its bookmark, the ribbon sticking out at the top, and the backpack with the strap of its flap, buckled over the panel, or with the `B` key (which also opens it), or by closing the inventory. The backpack and the recipe book do not fit on screen together: opening one closes the other.
- The backpack's contents are stored in the item itself, like a Shulker box: a backpack placed in a chest or dropped on the ground keeps everything inside. A backpack or a Shulker box cannot go into a backpack.
- On death, equipment drops on the ground like the rest of the inventory, unless `keepInventory` is on.
- In Creative mode, these slots are not shown.
- On a server without the mod, which has nowhere to keep them, these slots, the backpack and the sort buttons are not offered: the inventory, Shift-click and the elytra stay vanilla.

```
 S L S     L = leather, S = string, B = bundle
 S B S
 L L L
```

The panel's leather texture is generated with the other interface textures (see [Interface](#interface)).

### Compass

With a compass equipped, a minimap shows in the top-right corner of the screen, in a wooden frame, with the colors of vanilla maps: the terrain around the player, north up. The `+` and `-` keys zoom in and out, from 32 to 256 blocks across (96 by default, remembered between sessions). The biome name is written below in a small frame, then the coordinates (`X 12, Y 64, Z -30`); potion effects line up to the left of the minimap. In the Nether, the map shows the floor at the player's height rather than the bedrock ceiling. The "N" button to the left of the minimap (clickable when the inventory or chat is open) switches between keeping north up and rotating the map with the player, their facing direction up; in that mode, a red "N" on the edge points north. This choice is kept in `config/vanillasugar.properties`.

The compass also shows markers in the world: their banner, with a "name · distance" line above it, appears on screen where they are, even through terrain. The `N` key, or the button to the left of the minimap (clickable when the inventory or chat is open), cycles through the displays: icon and text, icon only, or nothing. This choice is kept in `config/vanillasugar.properties`. Without a compass, markers are not shown in the world (nor is the minimap), but they stay saved.

### World map

The world map is an item added by the mod, crafted at a crafting table from an empty map surrounded by eight sticks. The recipe unlocks as soon as you have an empty map. It is the only item the map slot accepts: an empty or filled map cannot be equipped there.

```
 S S S     E = empty map, S = stick
 S E S
 S S S
```

Its texture, the terrain of a filled map in a frame of sticks, is generated from the game's textures by `tools/world_map_texture.py`.

With a world map equipped, the `M` key opens the world map, full screen in the same frame as the minimap: every chunk explored in the current dimension, with the player's position and markers; the rest is covered by a dark, lightly grained fog of war laid on the chunk grid, as in strategy games. The title and the position under the cursor sit on the map in small frames, the help is in a strip below the map, and each marker's name is written under its banner. It also opens from its tab at the top of the inventory. Drag with the mouse to move it; the minimap zoom keys, typing `+` / `-` or the mouse wheel zoom; `Space` recenters on the player, and the position under the cursor is shown in the top-left corner. It closes with `Esc`, the key that opens it or the inventory key (`E`), as the help at the bottom of the screen reminds you.

Markers are managed from this map: a right click asks for a name (optional) and a color among the 16 dyes, `Enter` places the marker at that spot (at ground level) and `Esc` cancels. Right-clicking an existing marker removes it; hovering it shows its coordinates and that reminder. They appear as colored banners on the world map, on the minimap with their name (pinned to the edge when out of range) and in the world with a compass. They are saved on the player (64 at most, across all dimensions) and kept on death.

Exploration is recorded at all times, even without a world map equipped: equipping one later shows everything already traveled. Chunks within 6 chunks of the player count as explored. The data stays on the client: in the world folder in singleplayer (`vanillasugar/worldmap`), in `vanillasugar/worldmap/servers/<address>` in the game folder in multiplayer.

All these keys can be changed in Options → Controls → Key Binds → Vanilla Sugar. Minecraft saves keys by their position on a QWERTY keyboard; `M`, `B`, `N`, `+` and `-` follow the character printed on the player's keyboard instead, so that on AZERTY the map does open with `M`. If the matching key is already used for something else, the zoom takes another key: on a Mac AZERTY keyboard, `+` is the command key, so zooming in moves to `)`, right next to `-`; on a PC AZERTY keyboard, `-` is hotbar slot 6, so zooming out moves to the numpad `-`.

### Clock

With a clock equipped in its slot, below the map, a panel shows in the top-left corner of the screen, in the same frame as the minimap:

- the time in 24-hour format (`--:--` in the Nether, where the clock spins) and an icon for the current weather: sun or moon, rain, snow depending on the biome, clouds where it never rains (desert), thunderstorm;
- the day, counting from day 1;
- a small bed after the time when a bed would let you sleep where the player stands: at night or during a thunderstorm, never in the Nether or the End;
- the forecast: after the current weather icon, an arrow and a small icon show the next weather change. The server sends it every second, from its weather timers. It is not shown on a server without the mod, nor when the weather is frozen (`advance_weather` set to `false`). When a thunderstorm could start or stop before the next rain change, the game rolls for it later: the forecast then simply says rain.

The weather icons and the bed are in the interface texture image (see [Interface](#interface)).

### Storage

Small wooden buttons, to the right of the titles, sort and stash:

- **Chests, barrels, ender chests and Shulker boxes**: sort the container, stash into it the inventory items it already holds, and sort the inventory, to the right of the "Inventory" title.
- **Inventory**: sort the inventory. Stashing needs the chest open: there is no remote stashing.
- **Open backpack**: sort it, and stash into it the items it already holds.

Sorting merges identical stacks then orders them by item. It touches the 27 inventory slots, never the hotbar or armor; stashing does not take from the hotbar either. In Survival, a middle click (the "Pick Block" key) on a slot sorts the part of the window that holds it.

After Shift-clicking a stack, the stacks hovered while Shift and the button stay held are moved in turn, as if each were Shift-clicked: just drag the cursor across a row to empty it into the chest. The transfer stops as soon as Shift or the button is released.

Likewise, with a bundle held on the cursor, a left click on a stack puts it inside, and the stacks hovered while the button stays held follow, until the bundle is full. A right click on an empty slot takes a stack out, and each empty slot hovered while the right button stays held gets another one, until the bundle is empty. Elsewhere, clicks keep their vanilla effect (placing the bundle on an empty slot, putting a stack in with a right click).

Inventory gestures make a soft sound where the game stays silent: picking up a stack or half of one, placing it, swapping it with the one on the cursor, moving it with Shift or a hotbar key, throwing it out of the window, spreading it by dragging, sorting or stashing; changing the selected hotbar slot, with the wheel or the keyboard, makes a small click. These are the bundle sounds, at different pitches depending on the gesture. In the Creative inventory, Shift-click keeps its usual behavior.

On hover, a bundle, a Shulker box or a backpack shows its contents in the inventory grid: nine slots per row from the left, three rows at most with the count of the other items in the last slot, and an amber fill bar in the style of the experience bar. All tooltips have 6 pixels of inner padding instead of 3. A loot Shulker box that has not been opened yet keeps the game's "???".

On hover, a filled map shows the map itself, small, in the minimap frame, with its number written small below. The server only sends a map's contents when it is in the player's inventory: a map never carried, seen in a chest, keeps the vanilla tooltip.

When the stack held in hand runs out from use (last block placed, last food eaten, last item thrown or fed to an animal, tool or weapon breaking), a stack of the same item taken from the inventory replaces it, preferring an identical stack (same enchantments, same wear). Only the inventory is searched, never the backpack. Dropping a stack or moving it in the inventory does not trigger anything.

In the recipe book of the inventory or the crafting table, a `Ctrl`-click (or `Cmd` on Mac) on a recipe crafts it as many times as the inventory's ingredients allow, at once, and puts the result in the inventory. If the inventory is full, what is left stays in the grid. Shift-click keeps its vanilla effect: filling the grid as much as possible without crafting.

### Right-click harvest

Right-clicking a ripe crop (wheat, carrots, potatoes, beetroots, nether wart, cocoa) harvests it and replants it right away. It drops what breaking it would have dropped, tool enchantments included, minus the seed put back in the ground. While sneaking, right click stays vanilla.

### Elytra

Elytra are worn in their own slot, to the right of the offhand, leaving the chestplate slot free: you fly in your armor. Right-clicking with elytra in hand, or Shift-clicking them in the inventory, puts them in this slot. They fly, wear out, are repaired by Mending and draw their wings (over the chestplate, in place of the cape) just like elytra worn as a chestplate; other players see them too. Elytra worn as a chestplate still work as in vanilla.

### Filtered hopper

Every hopper has, to the left of its five slots, a filter slot: the item placed there is the only one the hopper accepts, whether it pulls it from the container above, picks it up from the ground or receives it from another hopper. Other items stay where they are. For a sorter, just place filtered hoppers under a row of hoppers passing items along: each one takes its item on the way and lets the rest continue. As in vanilla, a hopper only takes one item every 8 ticks: a faster flow of the same item lets some of it slip through to the end of the row.

- The filter compares the item, not its enchantments or name: an enchanted book lets every enchanted book through. Without a filter, it behaves like a regular hopper.
- The filter is a real item, a single one: it is given back when the hopper is broken. Shift-clicking from the inventory fills the filter first if it is empty, then the hopper.
- Otherwise the hopper stays vanilla: same speed, locked by a redstone signal, read by a comparator (not counting the filter).
- A hopper with a filter is recognizable by the iron grate at the bottom of its bowl and the rivets on its sides. These textures are derived from the hopper's by [tools/filtered_hopper_textures.py](tools/filtered_hopper_textures.py).

### Small tweaks

- **Villagers on a lead**: a lead attaches to villagers, to walk them to a trading hall or a field. As with other animals, clicking a leashed villager unties it; wandering traders still cannot be leashed.
- **Boats on land**: a boat climbs steps of two pixels at most, left by blocks slightly lower than a full block: dirt path or farmland to grass, soul sand or mud to stone, carpets, a thin layer of snow. In vanilla, a single pixel is enough to stop it.
- **Enchanting table**: the tooltip of each option lists every enchantment it will give, instead of just one followed by "...?". The client computes them like the server, from the enchantment seed the server sends it in full (the game only sends 16 bits of it, which is not enough). On a server without the mod, the tooltip stays vanilla.
- **Mending and Infinity** are compatible on the same bow.
- **Leaves**: when a tree's trunk is cut, its leaves disappear right away, spreading outward, instead of decaying one by one over several minutes. They still drop their saplings, sticks and apples; leaves placed by a player never disappear.
- **Death screen**: the "Respawn" and "Title Screen" buttons can be clicked right away, without the game's one-second wait.
- **Nether portal**: it takes you through as soon as you step in, without the game's four-second wait, sound or warping screen.
- **Anvil**: no more "Too Expensive!". The cost is shown whatever it is, in red if levels are missing, and the anvil accepts any repair the player can pay for.
- **Double doors**: opening or closing a wooden door also opens or closes the one next to it, when the two form a double door (same wood, same facing, opposite hinges).
- **Bone meal on sugar cane and cactus**: it adds a block at the top of the stalk, up to the three blocks it reaches on its own. A dispenser of bone meal does the same.
- **Sleeping**: the night passes after two and a half seconds in bed instead of five, and the screen darkens twice as fast.
- **Composter**: once full, it gives its bone meal right away, without the one-second wait, and gives 8 instead of one. A hopper underneath takes them all at once, as soon as it has room for all 8: the composter keeps nothing back.

### Combat

- **Mob health**: a health bar shows above the head of a mob that is chasing the player or has just been hurt, and stays 5 seconds after the last hit. The lost health lingers a moment in a lighter shade behind the bar, then drains away, so you can read the hit you landed. The bar shows up to 32 blocks away, only if the player can see the mob, and shrinks with distance.
- **Spotted**: a golden "!" pops up above the bar when a mob targets the player, and stays as long as it is chasing.
- **Eye**: while at least one mob is chasing the player, even out of sight, an eye shows in the top-left corner of the screen, to the right of the clock panel when a clock is equipped, in the same frame as the buttons in the bottom-right corner. With several pursuers, their number is written in its corner; the eye bounces each time a new mob spots the player.

Only the server knows what a mob is targeting: it sends the player the list of mobs chasing them within 48 blocks. On a server without the mod, only the bars of hurt mobs are shown, with no "!" or eye.

The sprites for the bar, the "!" and the eye are generated by `tools/combat_textures.py`.

### Quest journal and advancements

**Celebration.** An earned advancement is announced in the middle of the screen, instead of the small toast in the corner: its icon in a medallion, above an ornate frame giving its type ("Advancement Made!", "Goal Reached!", "Challenge Complete!"), its title in large letters and its description. The bigger the advancement, the bigger the banner: a wooden frame and the level-up sound for an advancement, a golden frame and light rays for a goal, a purple frame, a title three times larger and a fanfare for a challenge. It also shows over an open inventory; several advancements earned together play one after the other.

**Buttons.** In the bottom-right corner of the screen, two buttons stay on screen: the journal (a book) and the advancements (a trophy), each with its key written in the corner. They stay on top in the inventory and can be clicked, even with the recipe book or the backpack open. They can be clicked whenever the cursor is free (inventory, chest or chat open). In game, the `J` key opens the journal and the game's advancements key (`L`) the new Advancements screen. Each closes with `Esc`, its own key or the inventory key (`E`), like the game's windows.

**Advancements screen.** It replaces the game's tree, from the pause menu too, in the same book as the journal: the tabs on the left page (Minecraft, Nether, The End, Adventure, Husbandry, then those of datapacks) and their advancements on the right page, with their description and the count of advancements earned. A filter shows everything, what is left to do or what is made. An advancement with several criteria shows a progress bar; clicking it lists what is left, for example the biomes still to visit. Hidden advancements stay "Secret advancement" until they are earned.

The game only sends the player the advancements close to those already earned. With the mod, the server sends them all (in singleplayer, the integrated server has the mod), so the list and the journal show everything there is to do. On a server without the mod, the list is limited to what it sends.

**Quest journal.** The first time you play in a world, the game offers to follow the journal; the choice can be changed at any time in the journal. The journal is an open book, bound in leather like the recipe book. On the left, twelve chapters that follow a playthrough, from the first steps to the legendary challenges, with their progress. On the right, the chapter's quests:

- first the **open** quests, which say where to go and what to do;
- then the **locked** quests, grayed out, which say which advancement opens them;
- finally, under a "Done" heading, the **earned** quests, checked.

A quest opens when the advancement it follows in the game's tree is earned. The followed quest shows in the top-right corner, below the minimap and as wide as it (in its place without a compass), marked with a "Current quest" label (potion effects stay to its left); it stays on top in the inventory. Wherever the cursor is free (inventory, chests, chat), it lights up on hover and a click opens the journal on its chapter, the list scrolled to it. It shows its chapter, what to do and, when there are several things to do, a progress bar. It is the quest chosen with "Follow" in the journal, otherwise the first open quest. Once earned, it stays a few seconds: its label turns into a green "Quest complete!", its title is struck through and a check mark shows on its icon. It then slides off screen, and the next open quest takes its place. The `K` key shrinks it to its title, then hides it, then shows it in full again; this choice is kept in `config/vanillasugar.properties`.

The mod adds its own advancements, in a "Vanilla Sugar" tab: equip a backpack, a compass (minimap), a world map, and place a marker on the world map. The server grants them by checking the player's equipment and markers every second, even for equipment worn before these advancements were added. They make up the "Explorer's Gear" chapter, after The Iron Age.

The game's 126 advancements and the mod's 4 are spread across the chapters in [chapters.json](src/main/resources/assets/vanillasugar/quests/chapters.json). The chapters' titles and introductions and the quests' hints are in the language files (`quest.vanillasugar.*`). A quest without a hint shows the advancement's description. The choice to follow the journal and the followed quest are saved per world, in `vanillasugar/quests.properties`. The textures (frames, medallions, rays, button icons, journal cover) are drawn by [tools/quest_textures.py](tools/quest_textures.py).

## Interface

The whole interface follows one understated art direction: plain panels of sepia wood, with plum shadows and golden edges, near-black slots, tan leather for the backpack and the books, cream paper for the pages. What you hover gets a cream outline (buttons, tabs, fields, slots, enchanting options, stonecutter recipes, loom patterns, beacon powers); what is selected gets a green frame, like the selected hotbar slot. On paper, hover is ribbon red.

- **HUD**: hotbar with framed slots, a thick green frame for the selection, an experience bar shaped like a wooden rod with rings at the ends and in the middle (its progress in green), a small cross crosshair, pink hearts, potion effects in the same frame as the minimap, top right (to its left when it is shown), beneficial ones on one row and harmful ones on the next, on a teal background for beacon effects; next to the inventory, they keep their place and take the same frame (`tools/panel_textures.py`). The pop-ups in the top-right corner (new recipes, tutorial hints, system messages, music) take that frame too, with a golden title and light text like the followed quest.
- **Windows** (inventory, chests, furnaces, crafting table, anvil, villagers, Creative mode…): the vanilla grays become sepia wood and dark slots. Titles are written in cream.
- **Window background**: behind windows and journals, the world is darkened and blurred, as behind the pause menu; the blur follows the game's menu blur setting.
- **Widgets**: buttons, sliders, checkboxes, tabs and tooltips use the same browns. Text fields (world map, anvil, chat…) are recessed like slots, with a dark background and a frame that lights up while typing.
- **Enchanting table**: the book above the slots is drawn in isometric pixel art, bound in red leather with golden corners. It opens when an item can be enchanted, turns its pages when the item changes, and purple glyphs rise from it.

Vanilla icons (arrows, flames, drawings) keep their shape.

The recipe book looks like a book: a stitched leather cover, like the backpack, around cream pages whose edge shows, with leather-trimmed tabs: the open category's tab holds a page, its paper runs through the cover and joins the pages, the others stay paper. The recipe slots and arrows take paper tones, and the page number is written in ink. The search field is written in ink on the page. In the crafting table and furnaces, the button that opens the book is just the book, without a button background. Wherever it opens (inventory, crafting table, furnaces), the open book also closes with its bookmark, the ribbon sticking out at the top.

Right-clicking a recipe opens its variants on a leather-bound sheet: each card shows the resulting item at full size, with its count, on a pink background when ingredients are missing. Hovering a card opens a sheet beside it with the item's name, which ingredients are available, the crafting grid (or the furnace slot) at full size, an arrow and the result.

All interface textures (HUD, windows, widgets, recipe book, backpack…) are gathered in a single image, [textures/gui.png](textures/gui.png), arranged in titled sections. Just edit it in a drawing program and relaunch the game: the build cuts it back into files according to [textures/gui.json](textures/gui.json). Each texture is cropped to its visible pixels (an empty texture keeps its whole area, so it can be drawn later) and surrounded by a bright pink frame that shows where its area ends, with its name written above: paint inside the frame. Frames, names and the space between textures are ignored. `python3 tools/gui_atlas.py` with no new texture rearranges the image.

To change the hues all at once, `python3 tools/gui_psd.py` writes `textures/gui.psd`: the same image, its pixels sorted into layers by color family (wood, dark backgrounds, leather, paper, hover cream, selection green, gold, amethyst, red, icons, misc), plus a locked layer for the pink frames and the names. Add a Hue/Saturation (or Curves) adjustment clipped to the layer to change, then export the flattened image as PNG, transparency kept, in place of `textures/gui.png`. The build does not read the PSD back: after a change to `gui.png` made elsewhere, regenerate it, and the adjustments added must be redone.

The starting colors are gathered in [tools/gui_theme.py](tools/gui_theme.py). After changing them, this command redraws every texture and replaces the image (hand edits made in the image are then lost):

```sh
python3 tools/build_gui.py
```

Without arguments, it uses the jar downloaded by Gradle (run `./gradlew build` once first). The shapes stay in the drawing scripts: [tools/gui_style.py](tools/gui_style.py) (HUD, windows, widgets), [tools/gui_textures.py](tools/gui_textures.py) (backpack, bookmark), [tools/recipe_book_textures.py](tools/recipe_book_textures.py) (recipe book) and [tools/enchanting_book_textures.py](tools/enchanting_book_textures.py) (enchanting table book, rendered in isometric 3D). To add a texture to the image, drop it at its place under `src/main/resources/assets` then run `python3 tools/gui_atlas.py`.

## Rebalanced recipes

| Item | Vanilla | Vanilla Sugar |
|---|---|---|
| Buttons (wood, stone, polished blackstone) | 1 block → 1 | 1 block → 16 |
| Wooden trapdoors | 6 planks → 2 | 6 planks → 24 (4 per plank) |
| Pressure plates (except weighted) | 2 blocks → 1 | 2 blocks → 16 (8 per block) |
| Sticks (planks) | 2 planks → 4 | 2 planks → 32 (16 per plank) |
| Ladders | 7 sticks → 3 | 7 sticks → 8 |
| Stairs (crafting table) | 6 blocks → 4 | 6 blocks → 8 |
| Wood / hyphae (bark) | 4 logs → 3 | 4 logs → 4 |
| Signs | 6 planks + 1 stick → 3 | 6 planks + 1 stick → 12 |
| Wooden doors | 6 planks → 3 | 6 planks → 12 (2 per plank) |
| Bowls | 3 planks → 4 | 3 planks → 12 (4 per plank) |

Stairs and slabs can be crafted back into blocks, without loss: 4 stairs in a square give 3 blocks, 2 slabs side by side give 1 (two slabs stacked keep their vanilla recipe, like chiseled stone). These recipes show in the recipe book as soon as you pick up a stair or a slab.

The recipes are generated from the vanilla jar by [tools/rebalance_recipes.py](tools/rebalance_recipes.py). Only the output amount changes: shapes, recipe book groups and unlocks stay vanilla. The same script writes the stairs and slabs → blocks recipes in `data/vanillasugar/recipe/uncraft`. To adjust a number, edit the `RULES` table and run the script again:

```sh
python3 tools/rebalance_recipes.py ~/Library/Application\ Support/minecraft/versions/26.3/26.3.jar
```

## Trying the mod

No need to know Java or to install anything; there are two ways to launch the game with the mod:

- **From the Finder**: double-click `Lancer le jeu.command`. On first launch, macOS may refuse to open the file: then right-click → Open → Open.
- **From VS Code**: `Cmd+Shift+B`. The other actions (automated tests, building the jar) are under `Cmd+Shift+P` → "Tasks: Run Task".

The game opens with the username `Corentin`, offline: no Microsoft account needed, but online multiplayer is not available. Test worlds are saved in the `run/` folder and come back at each launch. The first launch takes a few minutes (downloading Java 25 and Minecraft), the next ones about 30 seconds. Code changes are picked up at the next launch.

## Development

- `./gradlew runClient`: launches the game with the mod.
- `./gradlew build`: builds the jar in `build/libs/`.
- `./gradlew runClientGameTest`: launches the game in automated test mode (`src/gametest`). It creates a world, equips the backpack, the compass and the world map, explores and opens the world map, places a named marker on it then removes it, changes the marker display, opens and closes the backpack with `B`, checks hand refill, bulk crafting and map previews, takes pictures of the restyled windows, sets a hopper's filter, checks saving and death, then closes the game. The screenshots, in 1920×1080 with the automatic GUI scale, are in `build/run/clientGameTest/screenshots/`.

The project needs Java 25. Gradle downloads it automatically for this project (see `gradle/gradle-daemon-jvm.properties`); there is nothing to install.

The code is split between `src/main` (shared by client and server: items, slots, recipes), `src/client` (rendering: HUD, minimap, screens) and `src/gametest` (automated tests).

The mod's icon, a sugar cube on a wooden panel, is drawn by [tools/icon_texture.py](tools/icon_texture.py), which also writes the website's copy.

## Publishing a version

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat(hopper): …`, `fix(map): …`), from which [git-cliff](https://git-cliff.org) writes the [CHANGELOG](CHANGELOG.md) (settings in [cliff.toml](cliff.toml)). Only `feat`, `fix` and `perf` appear in it.

A version is published from GitHub, with nothing to install: Actions tab → Release → Run workflow, choosing the version type:

| Choice | Effect |
|---|---|
| `alpha`, `beta` | next pre-release: 0.1.0-alpha.1 → 0.1.0-alpha.2, then → 0.1.0-beta.1 |
| `release` | final version of a pre-release: 0.1.0-beta.2 → 0.1.0 |
| `patch`, `minor`, `major` | 0.1.0 → 0.1.1, 0.2.0, 1.0.0 |

The workflow ([release.yml](.github/workflows/release.yml)) updates the version in `gradle.properties`, adds the CHANGELOG entry, builds the mod, commits both files, tags `v<version>` and publishes the jars in a GitHub release, with the CHANGELOG entry as notes. A pre-release is marked as such. The jar is then uploaded to Modrinth and CurseForge, each once its project id is set as a repository variable (`MODRINTH_ID`, `CURSEFORGE_ID`) with its token as a secret (`MODRINTH_TOKEN`, `CURSEFORGE_TOKEN`). The version is computed in [tools/next-version.sh](tools/next-version.sh). To see the entry that will be added: `git cliff --unreleased` (install git-cliff with `brew install git-cliff`).

Every push to `main` and every pull request also build the mod ([build.yml](.github/workflows/build.yml)); the jar can be downloaded from the Actions tab.

## Website

The mod's website is in [site/](site/), published on GitHub Pages on every push to `main` that changes it ([pages.yml](.github/workflows/pages.yml)). In the repository settings, Pages → Source must be set to "GitHub Actions". A single page, in English (`/`) and French (`/fr/`): the text is in [site/_data/en.yml](site/_data/en.yml) and [site/_data/fr.yml](site/_data/fr.yml), the layout in [site/_layouts/home.html](site/_layouts/home.html). Screenshots go in [site/assets/screenshots/](site/assets/screenshots/), under the name shown on each placeholder; the Modrinth and CurseForge links go in [site/_data/mod.yml](site/_data/mod.yml).

## License

Vanilla Sugar is released under the [GPL-3.0](LICENSE): you can include it in a modpack, change it or reuse its code, as long as the resulting version stays under the GPL-3.0, credits its author and publishes its source. Version 0.1.0-alpha.1, released under CC0, remains so.
