# Vanilla Sugar

Un mod Fabric pour Minecraft 26.3 qui corrige d'un coup les petits agacements du jeu, en restant au plus près de l'expérience vanilla : pas de nouveaux minerais, pas de magie, pas de machines. Uniquement ce qui manque ou ce qui est incohérent.

## Fonctionnalités

| # | Fonctionnalité | État |
|---|---|---|
| 1 | **Sac à dos** : craft 4 cuir + 4 ficelles + 1 bundle. Équipé dans un slot dédié, il s'ouvre avec un onglet collé au bord droit de l'inventaire, dans un panneau en cuir sur le côté droit, et offre 54 emplacements sur 9 colonnes (deux fois l'inventaire principal, comme un grand coffre). | Fait |
| 2 | **Slot compas** : un compas équipé affiche une mini-map, permet de poser des marqueurs et affiche les coordonnées et le biome. | Fait |
| 3 | **Slot carte** : une carte équipée débloque la carte du monde (touche `M`), qui montre tous les chunks déjà visités par le joueur. | Fait |
| 4 | **Rééquilibrage des recettes** : quantités produites cohérentes avec la matière utilisée (voir ci-dessous). | Fait |
| 5 | **Interface retravaillée** : une GUI plus soignée et professionnelle, sans dénaturer le style du jeu. Chaque fenêtre prend discrètement la teinte et le grain du bloc qu'elle représente (voir ci-dessous). | En cours |

Les slots dédiés sont un principe central : les nouvelles fonctions se débloquent en équipant un objet vanilla (compas, carte), pas par magie. La progression vanilla reste donc la même.

### Slots d'équipement

Trois slots sont ajoutés dans la colonne au-dessus de la main secondaire : sac à dos, compas, carte (vierge ou remplie).

- Un clic avec Maj sur un de ces objets dans l'inventaire l'équipe. Si le sac à dos est ouvert, les autres objets vont dedans (l'armure et le bouclier gardent la priorité vanilla) ; fermé, le clic avec Maj reste celui du vanilla. Les objets ramassés vont toujours dans l'inventaire.
- Le sac à dos s'ouvre avec l'onglet en cuir collé au bord droit de l'inventaire, ou directement avec la touche `B` (qui le referme aussi), et se ferme avec la croix en haut à droite du panneau ou en fermant l'inventaire. Avec une carte équipée, un second onglet ouvre la carte du monde. Le sac et le livre de recettes ne tiennent pas ensemble à l'écran : ouvrir l'un ferme l'autre.
- Le contenu du sac à dos est stocké dans l'objet lui-même, comme une boîte de Shulker : un sac posé dans un coffre ou jeté au sol garde tout ce qu'il contient. On ne peut pas mettre un sac à dos ni une boîte de Shulker dans un sac à dos.
- À la mort, l'équipement tombe au sol comme le reste de l'inventaire, sauf si `keepInventory` est activé.
- En mode créatif, ces slots ne sont pas affichés.

```
 S C S     C = cuir, S = ficelle, B = bundle
 S B S
 C C C
```

La texture en cuir du panneau et du bouton est générée par [tools/gui_textures.py](tools/gui_textures.py) (`python3 tools/gui_textures.py`), comme le cadre de la mini-map.

### Compas

Avec un compas équipé, une mini-map s'affiche en haut à droite de l'écran, dans un cadre en métal, avec les couleurs des cartes vanilla : le terrain autour du joueur, nord en haut. Les touches `+` et `-` zooment et dézooment, de 32 à 256 blocs de côté (96 par défaut, retenu d'une partie à l'autre). Les coordonnées et le biome sont écrits en petit en dessous, et les effets de potion descendent sous la mini-map. Dans le Nether, la carte montre le sol à la hauteur du joueur plutôt que le plafond de bedrock.

Le compas affiche aussi les marqueurs dans le monde : leur bannière, et au-dessus une ligne « nom · distance », apparaissent à l'écran là où ils se trouvent, même à travers le relief. La touche `N`, ou le bouton à gauche de la mini-map (cliquable quand l'inventaire ou le chat est ouvert), passe d'un affichage à l'autre : icône et texte, icône seule, ou rien. Ce choix est retenu dans `config/vanillasugar.properties`. Sans compas, les marqueurs ne s'affichent pas dans le monde (ni la mini-map), mais restent enregistrés.

### Carte

Avec une carte équipée (vierge ou remplie), la touche `M` ouvre la carte du monde : tous les chunks explorés dans la dimension actuelle, avec la position du joueur et ses marqueurs. On l'ouvre aussi avec l'onglet de l'inventaire. On la déplace en glissant avec la souris, les touches de zoom de la mini-map, `+` / `-` tapés ou la molette zooment, `Espace` recentre sur le joueur et la position sous le curseur est affichée en bas.

Les marqueurs se gèrent depuis cette carte : un clic droit demande un nom (facultatif) et une couleur parmi les 16 teintures, `Entrée` pose le marqueur à cet endroit (au niveau du sol) et `Échap` annule. Un clic droit sur un marqueur existant le retire ; le survoler affiche son nom, ses coordonnées et ce rappel. Ils apparaissent comme des bannières de couleur sur la carte du monde, sur la mini-map avec leur nom (collés au bord quand ils sont hors de portée) et dans le monde avec un compas. Ils sont enregistrés sur le joueur (64 au maximum, toutes dimensions confondues) et conservés à la mort.

L'exploration est enregistrée en permanence, même sans carte équipée : en équiper une plus tard montre tout ce qui a déjà été parcouru. Les chunks à moins de 6 chunks du joueur comptent comme explorés. Les données restent côté client : dans le dossier du monde en solo (`vanillasugar/worldmap`), dans `vanillasugar/worldmap/servers/<adresse>` du dossier du jeu en multijoueur.

Toutes ces touches se changent dans Options → Commandes → Vanilla Sugar. Minecraft enregistre les touches par position sur un clavier QWERTY ; `M`, `B`, `N`, `+` et `-` suivent plutôt le caractère imprimé sur le clavier du joueur, pour qu'en AZERTY la carte s'ouvre bien avec `M`. Si la touche correspondante sert déjà à autre chose, le zoom prend une autre touche : sur Mac AZERTY, `+` est la touche de commande, le zoom avant passe donc sur `)`, juste à côté du `-` ; sur PC AZERTY, `-` est le 6 de la barre d'objets, le zoom arrière passe sur le `-` du pavé numérique.

## Interface

Chaque fenêtre de bloc a sa propre teinte, posée sur les gris vanilla pour garder les mêmes contrastes, et un grain à peine visible de la matière du bloc. Les icônes vanilla (flèches, flammes, dessins) et les titres ne changent pas.

| Grain | Fenêtres |
|---|---|
| Fil du bois, sans planches | établi, coffre, tonneau, métier à tisser, fumoir |
| Pierre | four, distributeur, dropper, alambic, tailleur de pierre, meule, crafter, coffre de l'Ender, boîte de Shulker, table d'enchantement |
| Métal brossé | enclume, table de forge, entonnoir, haut fourneau |
| Papier | table de cartographie |
| Toile | inventaire du joueur (et ses slots d'équipement) |

Le tonneau et le coffre de l'Ender partagent la fenêtre du coffre en vanilla, et le dropper celle du distributeur : `ContainerThemes.java` leur donne leur texture d'après le titre de la fenêtre. Renommé à l'enclume, un de ces blocs garde la texture partagée.

Les textures sont générées depuis le jar vanilla par [tools/container_themes.py](tools/container_themes.py). Pour changer une teinte ou un grain, modifier le tableau `THEMES` puis relancer :

```sh
python3 tools/container_themes.py ~/Library/Application\ Support/minecraft/versions/26.3/26.3.jar
```

## Recettes rééquilibrées

| Objet | Vanilla | Vanilla Sugar |
|---|---|---|
| Boutons (bois, pierre, blackstone polie) | 1 bloc → 1 | 1 bloc → 16 |
| Trappes en bois | 6 planches → 2 | 6 planches → 8 |
| Plaques de pression (sauf pondérées) | 2 blocs → 1 | 2 blocs → 8 |
| Échelles | 7 bâtons → 3 | 7 bâtons → 8 |
| Escaliers (établi) | 6 blocs → 4 | 6 blocs → 8 |
| Bois / hyphes (écorce) | 4 bûches → 3 | 4 bûches → 4 |

Les recettes sont générées depuis le jar vanilla par [tools/rebalance_recipes.py](tools/rebalance_recipes.py). Seule la quantité produite est modifiée : formes, groupes du livre de recettes et déblocages restent ceux du vanilla. Pour ajuster un chiffre, il suffit de modifier le tableau `RULES` puis de relancer le script :

```sh
python3 tools/rebalance_recipes.py ~/Library/Application\ Support/minecraft/versions/26.3/26.3.jar
```

## Tester le mod

Pas besoin de connaître Java ni d'installer quoi que ce soit, il y a deux façons de lancer le jeu avec le mod :

- **Depuis le Finder** : double-cliquer sur `Lancer le jeu.command`. Au premier lancement, macOS peut refuser d'ouvrir le fichier : faire alors clic droit → Ouvrir → Ouvrir.
- **Depuis VS Code** : `Cmd+Maj+B`. Les autres actions (tests automatiques, construction du jar) sont dans `Cmd+Maj+P` → « Tasks: Run Task ».

Le jeu s'ouvre avec le pseudo `Corentin`, hors ligne : pas besoin de compte Microsoft, mais le multijoueur en ligne n'est pas disponible. Les mondes de test sont sauvegardés dans le dossier `run/` et réapparaissent à chaque lancement. Le premier lancement prend quelques minutes (téléchargement de Java 25 et de Minecraft), les suivants environ 30 secondes. Les modifications du code sont prises en compte au lancement suivant.

## Développement

- `./gradlew runClient` : lance le jeu avec le mod.
- `./gradlew build` : produit le jar dans `build/libs/`.
- `./gradlew runClientGameTest` : lance le jeu en mode test automatique (`src/gametest`). Il crée un monde, équipe le sac à dos, le compas et la carte, explore et ouvre la carte du monde, y pose un marqueur nommé puis le retire, change l'affichage des marqueurs, ouvre et ferme le sac avec `B`, photographie les fenêtres habillées, vérifie la sauvegarde et la mort, puis ferme le jeu. Les captures d'écran sont dans `build/run/clientGameTest/screenshots/`.

Le projet demande Java 25. Gradle le télécharge automatiquement pour ce projet (voir `gradle/gradle-daemon-jvm.properties`), il n'y a rien à installer.

Le code est réparti entre `src/main` (commun client et serveur : objets, slots, recettes), `src/client` (rendu : HUD, mini-map, écrans) et `src/gametest` (tests automatiques).
