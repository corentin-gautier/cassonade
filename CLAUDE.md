# Vanilla Sugar

Mod Fabric pour Minecraft 26.3 (Java 25, Gradle, Fabric API). Le [README](README.md), en anglais et écrit pour les joueurs, décrit chaque fonctionnalité : c'est la référence du comportement attendu. Les détails techniques (fonctionnement interne, textures, recettes, tests, releases) sont dans [CONTRIBUTING.md](CONTRIBUTING.md).

## Esprit du mod

Le mod corrige les petits agacements du jeu sans changer la façon de jouer. Avant d'ajouter quoi que ce soit :

- Pas de nouveaux minerais, paliers d'équipement, magie, machines, monstres, biomes ni dimensions. Rien qui rende le jeu plus facile à finir, seulement plus agréable.
- Une nouvelle fonction se débloque en équipant un objet vanilla (compas, montre) ou fabriqué à partir d'objets vanilla (sac à dos, carte du monde). La progression vanilla ne change pas.
- L'interface garde le style pixel et les sons de Minecraft, dans la direction artistique du mod (bois sépia, emplacements sombres, cuir, papier crème).
- Sur un serveur sans le mod, le client doit rester utilisable : les fonctions qui ont besoin du serveur se replient sur le comportement vanilla.

## Commandes

- `./gradlew build` : le jar dans `build/libs/`.
- `./gradlew runClient` : lance le jeu (pseudo `Corentin`, hors ligne, mondes dans `run/`).
- `./gradlew runClientGameTest` : test de bout en bout dans le vrai client ; captures en 1920×1080 dans `build/run/clientGameTest/screenshots/`. À lancer après un changement de comportement ou d'interface.
  - `-Ptests=Gear,Quest` ne lance que ces tests (nom de la classe sans `ClientGameTest` : `Gear`, `Quest`, `Interface`, `Clock`, `Tweaks`, `DedicatedServer`, `VanillaServer`). Ne lancer que ceux que le changement concerne ; pendant le développement d'une fonction, préférer un petit test dédié (comme `Clock`) au long test `Gear`.
  - `-PsiteScreenshots` reprend les captures du site (voir `site/assets/screenshots/README.md`).

## Organisation

- `src/main` : commun client et serveur (objets, slots, recettes, payloads réseau, mixins serveur). Un paquet par fonction (`gear`, `compass`, `hopper`, `combat`…), chacune s'enregistre par un `init()` appelé depuis `VanillaSugar`.
- `src/client` : rendu et écrans (HUD, mini-map, carte, journal), entrée `VanillaSugarClient`.
- `src/gametest` : tests automatiques du client.
- Les mixins sont déclarés dans `vanillasugar.mixins.json` (main) et `vanillasugar.client.mixins.json` (client).
- `src/main/resources/data/minecraft/recipe` : recettes rééquilibrées, générées par `tools/rebalance_recipes.py`, pas modifiées à la main.
- Tout texte affiché passe par les fichiers de langue : mettre à jour `en_us.json` et `fr_fr.json` ensemble.

## Textures

- **Ne jamais lancer `tools/build_gui.py`** : il redessine `textures/gui.png` et efface les retouches faites à la main.
- Les textures d'interface vivent toutes dans l'atlas `textures/gui.png` (disposition dans `textures/gui.json`), que Gradle redécoupe au build. Pour ajouter une texture : la déposer sous `src/main/resources/assets/…`, puis `python3 tools/gui_atlas.py`.
- Les autres textures sont dessinées par des scripts Python sans dépendance (`tools/*_textures.py`, `tools/icon_texture.py`, avec `tools/pngio.py`). Les couleurs viennent de `tools/gui_theme.py`.

## Code

- Indentation par tabulations, commentaires en anglais qui expliquent le pourquoi, dans la densité du code voisin.
- Suivre le style du fichier modifié plutôt que d'introduire de nouveaux motifs.

## Documentation

Un changement de comportement visible par le joueur met à jour, dans le même commit :

- le [README](README.md) (section de la fonction, et les tableaux si besoin) ;
- [CONTRIBUTING.md](CONTRIBUTING.md), si le fonctionnement interne décrit change ;
- le site, si la fonction y figure : [site/_data/en.yml](site/_data/en.yml) et [site/_data/fr.yml](site/_data/fr.yml), toujours les deux.

Le [CHANGELOG](CHANGELOG.md) n'est jamais modifié à la main : git-cliff l'écrit à partir des commits au moment de la release.

## Commits et versions

- Messages en anglais au format [Conventional Commits](https://www.conventionalcommits.org/) : `feat(<fonction>): <ce que le joueur gagne>`, `fix(<fonction>): <ce qui est corrigé>`. `perf` y apparaît aussi ; le reste (`refactor`, `docs`, `chore`, `ci`, `build`, `test`) n'y apparaît pas.
- La portée est la fonction concernée : `backpack`, `compass`, `world-map`, `clock`, `inventory`, `farming`, `elytra`, `hopper`, `quests`, `combat`, `recipes`, `tweaks`, `interface`. Les changements du site sont des `docs(site): …`, hors CHANGELOG.
- Le message décrit le résultat pour le joueur, pas l'implémentation : `feat(hopper): give every hopper a filter slot`.
- La version est dans `gradle.properties` et n'est changée que par le workflow de release, lancé à la main depuis l'onglet Actions (Release → Run workflow : `alpha`, `beta`, `release`, `patch`, `minor`, `major`). Il écrit l'entrée du CHANGELOG avec git-cliff ([cliff.toml](cliff.toml)), commite, tague `v<version>` et publie les jars sur GitHub, puis sur Modrinth et CurseForge dès que leurs identifiants de projet sont renseignés (variables `MODRINTH_ID`, `CURSEFORGE_ID`, secrets `MODRINTH_TOKEN`, `CURSEFORGE_TOKEN`). Ne pas poser de tag à la main.
