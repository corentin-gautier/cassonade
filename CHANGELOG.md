# Changelog

All notable changes to Vanilla Sugar. Each entry is written by [git-cliff](https://git-cliff.org) from the commit messages.

## [0.1.0-beta.1](https://github.com/corentin-gautier/mc-vanilla-sugar/releases/tag/v0.1.0-beta.1) - 2026-10-24

### Features

* **interface:** Dim and blur the world behind every in-game screen ([6ea68dd](https://github.com/corentin-gautier/mc-vanilla-sugar/commit/6ea68dd746c3fbb418fc644bbbbae0954ee5ba2e))
* **interface:** Split container windows into the block's tinted part and the inventory below, and line the recipe book tabs up with its rows ([9b60266](https://github.com/corentin-gautier/mc-vanilla-sugar/commit/9b60266d298ad90de33450aaad74fed957304c87))
* **interface:** Give the GUI a warmer sepia palette ([f57959c](https://github.com/corentin-gautier/mc-vanilla-sugar/commit/f57959cae9cbfd903ad25fb66703a14aee2f0566))
* **interface:** Make the recipe book tabs shorter ([96d43d4](https://github.com/corentin-gautier/mc-vanilla-sugar/commit/96d43d4e50fd34d362ab8871abcdd4290f88add2))
* **interface:** Close the recipe book with its ribbon in the crafting table and furnaces too ([e3449c0](https://github.com/corentin-gautier/mc-vanilla-sugar/commit/e3449c0ba0b291552e716a4d38636b79a1310cc3))

### Bug Fixes

* **quests:** Play a click when opening the journal from the followed quest ([cc97e76](https://github.com/corentin-gautier/mc-vanilla-sugar/commit/cc97e762db946d8ee812e36df30fbf21c53d316a))
* **backpack:** Close the backpack with a buckled flap strap instead of a briefcase handle ([7b344f5](https://github.com/corentin-gautier/mc-vanilla-sugar/commit/7b344f567d79a9dd77d99a6a1a9ef4eb7fd46260))
* Add depth to GUIs separations ([d0f2ad6](https://github.com/corentin-gautier/mc-vanilla-sugar/commit/d0f2ad65a65f44dbb13143aaa5bd9e3a826ad8e5))
* **gui:** Some weird pixels here and there ([577062a](https://github.com/corentin-gautier/mc-vanilla-sugar/commit/577062a6a9cc87a351a3450669095ad11606b7d7))
* **gui:** Make map fog uniform ([50e5a3c](https://github.com/corentin-gautier/mc-vanilla-sugar/commit/50e5a3ce4cb4ed41a6f62382fd530f59475dee49))
* **gui:** Coherence adjustments ([a4a2b50](https://github.com/corentin-gautier/mc-vanilla-sugar/commit/a4a2b5059ba24c22d2603053b43e3947fcda1a59))
* **inventory:** Keep vanilla's inventory on a server without the mod ([88748a5](https://github.com/corentin-gautier/mc-vanilla-sugar/commit/88748a541bc025715695c3299e09de114c48dcb3))

## [0.1.0-alpha.1](https://github.com/corentin-gautier/mc-vanilla-sugar/releases/tag/v0.1.0-alpha.1) - 2026-10-23

First public alpha, for Minecraft 26.3 with Fabric.

### Features

* **backpack:** a leather backpack worn in its own slot, with 54 slots, opened from a tab at the top of the inventory or with `B`
* **compass:** an equipped compass shows a minimap, the biome, the coordinates and your waypoints in the world
* **world map:** crafted from an empty map and eight sticks, it opens a map of every explored chunk (`M`) where you place named waypoints
* **clock:** an equipped clock shows the time, the weather and the next weather change, the day and when you can sleep
* **inventory:** sort and stash in one click, Shift-drag transfers, bundle dragging, inventory sounds, previews of bundles, shulker boxes, backpacks and maps, hand refill, bulk crafting from the recipe book
* **farming:** right-click a ripe crop to harvest and replant it
* **elytra:** elytra get their own slot, next to the chestplate
* **hopper:** every hopper gets a filter slot
* **quests:** a quest journal that follows the advancements, a quest tracker, a new advancements screen and centered celebrations
* **combat:** health bars over mobs in a fight, a “!” when a mob starts chasing you and an eye while you are hunted
* **recipes:** fair output for buttons, pressure plates, trapdoors, sticks, ladders, stairs, wood, signs, doors and bowls; stairs and slabs craft back into blocks
* **tweaks:** leashable villagers, boats over small steps, every enchantment shown at the enchanting table, Mending with Infinity, instant leaf decay, no respawn delay, no “Too Expensive!”, double doors, bone meal on sugar cane and cactus, faster sleep, composter giving 8 bone meal at once
* **interface:** a new look for the HUD, windows, widgets and recipe book (in progress)
