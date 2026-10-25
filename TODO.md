# To do

Features planned for Vanilla Sugar, in the spirit of the mod: nothing new to mine or fight, nothing that makes the game easier to beat, each feature unlocked by equipping a vanilla item or one crafted from vanilla items. A feature that needs the server stays hidden on a server without the mod.

When a feature ships, remove it from here and describe it in the [README](README.md).

## Planned

### Inventory and containers

- [ ] **Durability warning**
  - Unlocked: always.
  - When an enchanted tool, weapon or armor piece runs low (about 5 % or 10 uses left), a soft sound, a message above the hotbar and the item blinking. Once more right before it breaks.
  - Server: none, the client knows the durability.
- [ ] **Search in containers**
  - Unlocked: always.
  - A search field on paper, like the recipe book's, in chests, barrels, the ender chest, Shulker boxes and the backpack. Slots that don't match darken. It matches names, and what Shulker boxes and bundles hold.
  - Server: none.
- [ ] **Food values**
  - Unlocked: always.
  - In the tooltip, the hunger restored as small drumsticks, the saturation below it or with `Shift`.
  - Server: none.

### Clock and barometer

- [ ] **Moon phases**
  - Unlocked: the clock or the barometer worn.
  - The moon's phase in the clock panel, next to the weather; its name on hover when the cursor is free.
  - Server: none, the client knows the world's time.
- [ ] **Chime**
  - Unlocked: the clock or the barometer worn.
  - A soft sound at dusk, when monsters are about to spawn, maybe at dawn too. Can be turned off.
  - Server: none.
- [ ] **Tiredness**
  - Unlocked: the clock or the barometer worn.
  - A small icon in the clock panel, next to the bed telling when you can sleep, in three states read at a glance: rested (less than a night without sleep), tired (one or two), exhausted (three or more, phantoms may come). The number of nights on hover.
  - Server: the time since the last rest is a server statistic, sent to the client like the barometer's weather.

### Map and navigation

- [ ] **Death marker**
  - Unlocked: the compass (minimap and world) or the world map worn, like the other markers.
  - On death, a marker of its own (a small grave) where and in the dimension you died. It goes away when you come back to it, or by hand. Only the latest one; it does not count toward the 64 markers.
  - Server: none, the client knows where it died.

### Small tweaks

- [ ] **Ladders up and down**
  - Unlocked: always (the README's "Small tweaks" table).
  - Right-clicking a ladder with a ladder extends the column at its end, upward, or downward while sneaking. It stops at a taken spot or where the wall ends.
  - Server: yes, it is a placement rule; without the mod, placing stays vanilla.

### Combat

- [ ] **Damage direction**
  - Unlocked: always, the player feels where the hit came from.
  - A curved mark on the screen's edge, on the side the hit came from, fading in a second or two. For mobs, arrows and explosions; not for falls or hunger.
  - Server: none, the game already tells the client where a hit came from.

## Set aside

- **Horse stats**: speed, jump height and health in the horse, donkey, mule and llama window (strength for the llama), in the player's units and against the best possible. Unlocked by a measuring item to decide: the spyglass (judge the horse by looking at it), a crafted measuring stick, or the saddle (but you want to pick a horse before saddling it).

## Ideas

- **Notable places recorded on their own**: bed, Nether portals walked through, spawn points.
- **Nether ↔ Overworld coordinates** on the world map, to place a portal.
- **Shared markers in multiplayer**: copying your world map passes its markers on.
- **Worn spyglass**: a key to zoom without holding it.
- **Breeding**: holding an animal's food shows which ones are ready to breed.
- **Villager profession and restock** on hover.
- **Farmland** no longer trampled by the player.
- **Invisible item frames** with shears.
- **Signs on chests**: clicking the sign opens the chest.
- **Repair cost** already piled up, in the tooltip.
- **Mod settings screen**, from the Options menu.

### To discuss

- **Death chest**: items wait in a chest instead of despawning. Comfortable, but it takes away part of the death penalty.
- **Dynamic lighting**: a held or worn lantern lights up around the player. Much asked for, but it changes the balance of caves.
