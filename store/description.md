![Cassonade](https://corentin-gautier.github.io/cassonade/assets/og.jpg)

## Minecraft, minus the friction.

**Cassonade** is a vanilla+ quality-of-life mod for **Minecraft 26.3 on Fabric**. It fixes the hundred small annoyances of survival and leaves everything else alone. No new ores, no magic, no machines: the same game you know, just smoother.

**[Website](https://corentin-gautier.github.io/cassonade/)** (screenshots and every feature in detail) · **[Source code](https://github.com/corentin-gautier/cassonade)** · **[Report a bug](https://github.com/corentin-gautier/cassonade/issues)**

## The idea

*   **Same progression.** No new resources, tiers or dimensions. Every new feature is unlocked by equipping an item you already know (a compass, a clock) or one crafted from vanilla items (a backpack, a world map, a barometer), in its own equipment slot.
*   **Every small annoyance, fixed.** Leaves that take minutes to decay, "Too Expensive!", boats stuck on a pixel, the enchanting table's "…?": each fix is small, together they change how the game feels.
*   **Feels like it was always there.** The interface keeps Minecraft's pixel style and sounds, features only show up once you equip their item, and every key can be remapped.

## Features

### [Backpack](https://corentin-gautier.github.io/cassonade/features/backpack/)

54 more slots, as much as a large chest, crafted from 4 leather, 4 string and a bundle.

*   Worn in its own equipment slot, opened with `B` or from a tab at the top of the inventory, in a leather panel where the recipe book usually sits.
*   Shift-click sends items straight into the open backpack.
*   Like a Shulker box, it keeps its contents when placed in a chest or dropped.

### [Compass minimap](https://corentin-gautier.github.io/cassonade/features/minimap/)

Equip a compass to get a minimap in the top-right corner, drawn with the colors of vanilla maps.

*   Zoom from 32 to 256 blocks across, north-up or turning with you.
*   The biome and your coordinates underneath.
*   Your waypoints shown in the world, through terrain, with their name and distance.
*   In the Nether, the map shows the floor you stand on, not the bedrock ceiling.

### [World map and waypoints](https://corentin-gautier.github.io/cassonade/features/world-map/)

Craft a world map from an empty map and eight sticks, and open a full-screen map of every chunk you have explored, under a fog of war.

*   Right-click to place a named waypoint in any of the 16 dye colors. Waypoints show on the world map, the minimap and in the world.
*   A red X where you last died, until you get back there; the world's spawn chunk outlined; the lodestones your lodestone compasses point to.
*   **Ping** a spot or a mob with `Shift` + middle click: every player in the dimension sees it marked with your name for a few seconds.
*   Coordinates written in the chat become links that open the map there, ready to place a waypoint.
*   Exploration is recorded from the first minute, even before you craft the map.

### [Clock and barometer](https://corentin-gautier.github.io/cassonade/features/clock/)

Equip a clock to see the time, the weather, the day count and the moon's phase, with a small bed and a soft chime when you can sleep. Combine it with a lightning rod to make a **barometer**: it also shows the next weather change, read from the server's own weather timers.

### [An inventory that keeps up](https://corentin-gautier.github.io/cassonade/features/inventory/)

*   **Sort** a chest, a Shulker box, your backpack or your inventory in one click, or with a middle-click.
*   **Stash** into the open chest the items it already holds.
*   **Shift-drag** across a row to move every stack you pass over.
*   **Lock slots** with `Alt`\-click: sorting, stashing and Shift-click leave them alone, and `Q` won't throw them away.
*   **Rotate your rows** into the hotbar with `G`, so each row can hold a set: tools, building blocks, food and torches.
*   **Bundles**: drag one over items to fill it, or right-drag over empty slots to empty it.
*   **Previews** of bundles, Shulker boxes, backpacks and filled maps on hover, and the hunger and saturation food restores.
*   **Tooltips that tell more**: what each enchantment does, the anvil penalty of a worked item, how many items a fuel smelts.
*   **Picked up**: what you pick up is listed on the right of the screen, and marked with a small golden dot in your inventory until you hover it.
*   **Hand refill** when the stack in your hand runs out.
*   **Durability warning** before an enchanted tool or armor piece breaks.
*   **Bulk crafting**: `Ctrl`\-click a recipe to craft as many as your inventory allows.

### [Right-click harvest](https://corentin-gautier.github.io/cassonade/features/right-click-harvest/)

Right-click a ripe crop to harvest and replant it: wheat, carrots, potatoes, beetroots, nether wart and cocoa. Same drops as breaking it, Fortune included, minus the seed that goes back in the ground.

### [Elytra slot](https://corentin-gautier.github.io/cassonade/features/elytra-slot/)

Elytra get their own slot, next to the offhand: fly without taking your chestplate off. They fly, wear out and take Mending exactly as in vanilla.

### [Filtered hopper](https://corentin-gautier.github.io/cassonade/features/hopper-filter/)

Every hopper gets a filter slot: put one item in, and the hopper only accepts that item. Put a bundle or a Shulker box in the filter to let through every item it holds. A row of filtered hoppers makes a full item sorter, no redstone needed.

### [Quest journal and advancements](https://corentin-gautier.github.io/cassonade/features/quests/)

*   A quest journal in twelve chapters walks you from your first night to the toughest challenges, following Minecraft's own advancements.
*   The current quest shows under the minimap, with its progress.
*   A new advancements screen shows what is left for each one (biomes to visit, mobs to kill…).

### [Combat awareness](https://corentin-gautier.github.io/cassonade/features/combat/)

Combine a compass with a sculk sensor to make a **sculk compass**: health bars over the mobs you fight, a "!" when a mob starts chasing you, and an eye in the corner of the screen while anything is hunting you, even out of sight. The minimap shows where the mobs hunting you come from, and the other players around you.

### [Naturalist's spyglass](https://corentin-gautier.github.io/cassonade/features/naturalist-spyglass/)

Every spyglass gets a round lens and a finer zoom with the scroll wheel. Combine one with a book and a sculk sensor to make a **naturalist's spyglass**, which reads what you look at: a horse's speed, jump and health, when animals can breed or grow up, a villager's level and sold-out trades, how far crops have grown, a beehive's honey.

### [Sculk torch](https://corentin-gautier.github.io/cassonade/features/sculk-torch/)

Combine a torch with a sculk sensor to make a **sculk torch**. Held in either hand, it lights around you and shows every block nearby where monsters can spawn, at any time or only at night. Light those spots up and the marks go away.

### [A refined interface](https://corentin-gautier.github.io/cassonade/features/interface/)

Sepia wood panels, dark slots and cream text across the HUD, the chat, inventories and tooltips; status effects with their time left; a recipe book that looks like a book, and an enchanting table that shows every enchantment an option will give.

## Small tweaks

|                        |Vanilla                               |With Cassonade                                                          |
| ---------------------- |------------------------------------- |----------------------------------------------------------------------- |
| Cutting down a tree    |Leaves decay one by one for minutes   |Leaves fall right away, drops included                                  |
| Walking under trees    |Leaves at head height                 |Taller trunks, two blocks of air under the canopy                     |
| Repairing at the anvil |"Too Expensive!"                      |Any repair you can pay for                                              |
| Enchanting table       |One enchantment, then "…?"            |Every enchantment the option will give                                  |
| Mending and Infinity   |Incompatible on a bow                 |Work together                                                           |
| Boats on land          |Stuck on a 1-pixel step               |Glide over paths, farmland, carpets, snow                               |
| Double doors           |Open each door separately             |Both open together                                                      |
| A sign on a chest      |Clicking it edits the sign            |Clicking it opens the chest (sneak to edit the sign)                    |
| Composter              |1 bone meal, after a delay            |8 bone meal, right away                                                 |
| Bone meal              |Does nothing on sugar cane and cactus |Grows them by one block                                                 |
| Daytime                |10 minutes, then night falls          |20 minutes, the night is unchanged                                      |
| Going to bed           |5 seconds to skip the night           |2.5 seconds                                                             |
| Death screen           |Buttons locked for a second           |Respawn immediately                                                     |
| Nether portal          |4 seconds of warping and noise        |Step in, you're through                                                 |
| Moving villagers       |Boats, rails, trapping                |Put them on a lead                                                      |
| Getting hit            |The screen flashes red                |A red arc points where the hit came from                                |
| A mob in tall grass    |The hit cuts the grass                |The hit goes through grass, flowers and crops to the mob                |
| Sweeping a sword       |Also hits your own wolf or horse      |Spares the animals you tamed                                            |
| Torch in hand          |Lights nothing until placed           |Lights up around you                                                    |
| Item frames            |Always show their frame               |Shears hide the frame, the item stays on the wall                       |
| Ladders                |Placed one by one, from the bottom    |Right-click a ladder with one: it extends up, or down when looking down |
| Farmland               |Turns back to dirt when you jump on it|Only mobs trample it, never players                                     |
| Furnaces               |Watch the arrow fill                  |How long until everything is cooked, and how much more the fuel cooks   |
| Grindstone             |The experience is a surprise          |Shown before you take the item                                          |
| Note blocks            |Count the clicks to know the note     |Look at one to see its note                                             |
| Armor stands           |Take each piece off, one by one       |Sneak-click with an empty hand: your whole armor swaps with the stand's |
| Your name in the chat  |Easy to miss                          |A soft bell, and your name in gold                                      |

## Fair recipes

Some vanilla recipes eat a whole block for a single button. Cassonade only changes how many items a recipe gives, never its shape or when you unlock it: 16 buttons or pressure plates, 32 sticks from 2 planks, 8 stairs, 12 signs or doors, 8 chests or barrels, 6 paper from 3 sugar cane. Stairs and slabs craft back into full blocks without loss, and wool back into its 4 string.

## What Cassonade will never add

New ores, metals or gear tiers; magic, machines or tech trees; new mobs, biomes or dimensions; anything that makes the game easier to beat rather than nicer to play.

## Requirements

*   Minecraft **26.3**
*   Fabric Loader **0.20.1** or later
*   **[Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)**
*   Java **25**

**Multiplayer:** install the mod on the server and on every player's game. In singleplayer, there is nothing else to do. If you join a server without the mod, the game stays playable and falls back to vanilla where the server is needed.

## Modpacks

Yes, you can include Cassonade in your modpack. It is released under the GPL-3.0: you can also change it or reuse its code, as long as your version stays under the GPL-3.0, credits its author and publishes its source.

If Cassonade makes your worlds a little nicer, you can support its development with a coffee on **[Ko-fi](https://ko-fi.com/corentingautier)**.

_Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft._