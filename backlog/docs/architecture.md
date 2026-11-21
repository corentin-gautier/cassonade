---
id: doc-4
title: "Architecture"
type: specification
---

Details per feature: CONTRIBUTING.md, "How the features work" (names the classes and mixins).

## Source sets

| Path | Content |
| --- | --- |
| `src/main` | Shared client and server code: items, slots, recipes, network payloads, server mixins. Entry point `Cassonade`. |
| `src/client` | Rendering and screens (HUD, minimap, map, journal). Entry point `CassonadeClient`. |
| `src/gametest` | Client game tests (Fabric entrypoints, not JUnit) |
| `src/textures` | The two texture atlases, cut into the game's files by Gradle |
| `tools/` | Dependency-free Python scripts: textures, recipes, stores, profiling |
| `promo/` | `site` (Jekyll website), `store` (Modrinth and CurseForge pages), `video` (not in git) |

## Feature packages

- One package per feature under `com.cassonade` (`gear`, `compass`, `hopper`, `combat`, `waystone`, `stable`, `storage`…), each with an `init()` called from `Cassonade`.
- `dev/`: development-only commands and tools.
- Mixins: `cassonade.mixins.json` (main) and `cassonade.client.mixins.json` (client), mostly named after the vanilla class they patch (`AnvilMenuMixin`). Search by that class to see what already changes it.

## Settings

- `settings/Setting`: one constant per feature; `Settings.on(Setting.X)` where it acts. A new feature adds its constant and `setting.cassonade.<id>` + `.tooltip` in the language files.
- `Setting.Side`: server settings are read on the server and sent to players (`SettingsPayload`); client settings are local.
- `Setting.Applies`: at once, at world load (data packs), at next game start (`Settings.local`), or on resource reload.
- Display choices (minimap zoom, markers) are separate: `cassonade.properties` (`client/ClientSettings`).
- `-Ptests=Settings` turns every setting off and checks vanilla behaviour returns.

## Equipment slots

- Instruments (compass, world map, clock) in a column above the offhand; worn items (elytra, backpack) along the bottom row.
- Stored in a player attachment (`PlayerGear`). Backpack contents live in the item (`DataComponents.CONTAINER`).
- Hidden in Creative and on a server without the mod.

## Textures

- Interface textures in `src/textures/gui.png` (layout `gui.json`), all others in `world.png` (`world.json`), split by Gradle at build time.
- The atlases are written from Photoshop files out of git (`gui.psd`, `world.psd`) by `tools/atlas_psd.py`; the build runs it when a PSD is newer. Never write the atlases by hand; every texture keeps its place.
- New texture: a layer named after its file in its folder's group, or drawn by a `tools/*_textures.py` script then added with `atlas_psd.py --add`. No PSD yet: `--init`.
- Colours: `tools/gui_theme.py`.

## Recipes

- `src/main/resources/setting_fair_recipes/…/recipe` is generated from the vanilla jar by `tools/rebalance_recipes.py`: only output amounts change. Never edit by hand.
- The same script writes the uncraft recipes (stairs, slabs, wool), loaded only while their setting is on.

## Languages

- All displayed text in `src/main/resources/assets/cassonade/lang/`: `en_us`, `fr_fr`, `es_es`, `pt_br`, `de_de`, `zh_cn`, `ru_ru`, `ja_jp`, with the same keys.

## File names

Lowercase kebab-case, except where a tool or language decides (Java PascalCase, Python and Minecraft resources snake_case, standard files). The site's page names are public URLs: never rename.
