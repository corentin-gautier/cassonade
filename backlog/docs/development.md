---
id: doc-5
title: "Development and tests"
type: guide
---

## Running

- Java 25, downloaded by Gradle (`gradle/gradle-daemon-jvm.properties`): nothing to install.
- `./gradlew runClient`: the game with the mod, offline user `Corentin`, worlds in `run/`. First launch a few minutes, then ~30 s.
- `./gradlew build`: jar in `build/libs/`.
- VS Code: `Cmd+Shift+B` launches; `F5` ("Debug minecraft") attaches the debugger with hot code replace (new fields, signatures, mixins need a restart). Textures and languages: task "Update textures and languages", then `F3+T` in game.

## Dev commands

Only in `runClient`, in a world with cheats (`dev/DevCommands`). Full descriptions: CONTRIBUTING.md, "Getting started".

| Command | Key | Does |
| --- | --- | --- |
| `/dev kit` | `F6` | Best item in every gear slot, plus the mod's items |
| `/dev unequip` | `F7` | Empties the gear slots |
| `/dev goto biome\|structure <id>` | `F10` | Stands you at the nearest one |
| `/dev goto town`, `/dev goto tavern [type]` | | Nearest town or tavern |
| `/dev edit <template>\|all` | | Builds a template to edit; its structure block saves into `src/main/resources` |
| `/dev lights` | | Hall with every light, to compare |
| `/dev regen` | `F8` | Regenerates terrain at next opening; players and mod data kept |
| `/ambience <effect>` | `F9` | Triggers an ambience effect |
| | `F12` | Toggles the game's own rendering, to compare |

## Tests

- `./gradlew runClientGameTest`: end-to-end tests in the real client; 1920×1080 screenshots in `build/run/clientGameTest/screenshots/`. Run after any behaviour or interface change.
- `-Ptests=Gear,Quest`: only those classes (name without `ClientGameTest`). Give a new feature its own small test; split a test that covers several features.
- Some classes join a dedicated server with the mod (`DedicatedServer`) and a vanilla server (`VanillaServer`).
- Advancement banners and recipe toasts are off in tests.
- VS Code Testing view: link `tools/vscode-tests` as an extension (CONTRIBUTING.md, "Tests").

## Special runs

| Flag | Does |
| --- | --- |
| `-Pprofile` (+ `-PexploredWorld`) | Frame rates on an explored world, Flight Recorder files in `build/profile`, read with `tools/profile_report.py` |
| `-Pprofile -Ptests=PlayerProfile` | Short profile (~1 min) on a new world |
| `-PsiteScreenshots` | Retakes website screenshots in the hand-prepared world `.test-worlds/site-world` (irreplaceable: runs work on a copy) |
| `-PsiteScreenshots -PsiteShots=<ids>` | Only those shots |
| `-PsiteIcons` | Redraws the site's recipe icons |

`tools/convert-screenshots.sh` turns screenshots into the site's AVIF and the stores' JPG.

## Code

- Tabs; comments in English explaining why. Follow the file's style.
- A player-visible change updates in the same commit: README, CONTRIBUTING.md if internals change, the site (`promo/site/_data/en.yml` and `fr.yml`) if it covers the feature, and `promo/store/description.md`.
