# Vanilla Sugar

Un mod Fabric pour Minecraft 26.3 qui corrige d'un coup les petits agacements du jeu, en restant au plus près de l'expérience vanilla : pas de nouveaux minerais, pas de magie, pas de machines. Uniquement ce qui manque ou ce qui est incohérent.

## Fonctionnalités

| # | Fonctionnalité | État |
|---|---|---|
| 1 | **Sac à dos** : craft 4 cuir + 4 ficelles + 1 bundle. Équipé dans un slot dédié, il s'ouvre avec un onglet en haut de l'inventaire, dans un panneau en cuir sur le côté gauche, à la place du livre de recettes, et offre 54 emplacements sur 9 colonnes (deux fois l'inventaire principal, comme un grand coffre). | Fait |
| 2 | **Slot compas** : un compas équipé affiche une mini-map, permet de poser des marqueurs et affiche les coordonnées et le biome. | Fait |
| 3 | **Slot carte** : une carte équipée débloque la carte du monde (touche `M`), qui montre tous les chunks déjà visités par le joueur. | Fait |
| 4 | **Rééquilibrage des recettes** : quantités produites cohérentes avec la matière utilisée (voir ci-dessous). | Fait |
| 5 | **Slot montre** : une montre équipée affiche en haut à gauche l'heure, la météo, la prochaine météo et le jour. | Fait |
| 6 | **Rangement** : trier un coffre ou l'inventaire (bouton ou clic molette), transférer plusieurs piles d'un glissement avec Maj, ranger dans le coffre ouvert les objets qu'il contient déjà, aperçu du contenu des boîtes de Shulker et du sac au survol. | Fait |
| 7 | **Récolte au clic droit** : un clic droit sur une culture mûre la récolte et la replante. | Fait |
| 9 | **Slot élytre** : une élytre se porte dans son propre slot, en plus du plastron. | Fait |
| 10 | **Petits ajustements** : villageois en laisse, bateaux qui franchissent les petites marches, table d'enchantement qui annonce tous les enchantements, Raccommodage et Infinité ensemble, feuilles qui tombent dès qu'on coupe l'arbre, écran de mort sans attente, enclume jamais « Trop chère », portes doubles, poudre d'os sur la canne à sucre et le cactus. | Fait |
| 11 | **Entonnoir filtrant** : chaque entonnoir a un emplacement de filtre ; avec un objet dedans, il ne laisse passer que cet objet. Fini les trieurs en redstone. | Fait |
| 8 | **Interface retravaillée** : une GUI plus soignée et professionnelle, sans dénaturer le style du jeu. Une direction artistique commune au HUD et à toutes les fenêtres : bois et emplacements bruns, un peu plus ornementés que le vanilla tout en restant sobres (voir ci-dessous). | En cours |

Les slots dédiés sont un principe central : les nouvelles fonctions se débloquent en équipant un objet vanilla (compas, carte, montre), pas par magie. La progression vanilla reste donc la même.

### Slots d'équipement

Les slots sont rangés par usage. Les instruments qui affichent quelque chose à l'écran sont dans la colonne au-dessus de la main secondaire : compas, carte (vierge ou remplie), montre. Ce qui se porte sur le corps suit la main secondaire sur la rangée du bas : l'élytre (voir [Élytre](#élytre)) puis le sac à dos.

- Un clic avec Maj sur un de ces objets dans l'inventaire l'équipe. Si le sac à dos est ouvert, les autres objets vont dedans (l'armure et le bouclier gardent la priorité vanilla) ; fermé, le clic avec Maj reste celui du vanilla. Les objets ramassés vont toujours dans l'inventaire.
- Des onglets en haut de l'inventaire ouvrent et referment, d'un clic, le livre de recettes (à la place du bouton vanilla), le sac à dos et, avec une carte équipée, la carte du monde. L'onglet du panneau ouvert est plus clair et s'ouvre dans l'inventaire. Le livre ouvert se referme aussi avec son signet, le ruban qui dépasse en haut, et le sac avec sa lanière, la poignée en cuir posée sur le panneau, ou encore avec la touche `B` (qui l'ouvre aussi) ou en fermant l'inventaire. Le sac et le livre de recettes ne tiennent pas ensemble à l'écran : ouvrir l'un ferme l'autre.
- Le contenu du sac à dos est stocké dans l'objet lui-même, comme une boîte de Shulker : un sac posé dans un coffre ou jeté au sol garde tout ce qu'il contient. On ne peut pas mettre un sac à dos ni une boîte de Shulker dans un sac à dos.
- À la mort, l'équipement tombe au sol comme le reste de l'inventaire, sauf si `keepInventory` est activé.
- En mode créatif, ces slots ne sont pas affichés.

```
 S C S     C = cuir, S = ficelle, B = bundle
 S B S
 C C C
```

La texture en cuir du panneau est générée avec les autres textures d'interface (voir [Interface](#interface)).

### Compas

Avec un compas équipé, une mini-map s'affiche en haut à droite de l'écran, dans un cadre en bois, avec les couleurs des cartes vanilla : le terrain autour du joueur, nord en haut. Les touches `+` et `-` zooment et dézooment, de 32 à 256 blocs de côté (96 par défaut, retenu d'une partie à l'autre). Le nom du biome est écrit en dessous dans un petit cadre, puis les coordonnées (`X 12, Y 64, Z -30`), et les effets de potion descendent sous la mini-map. Dans le Nether, la carte montre le sol à la hauteur du joueur plutôt que le plafond de bedrock. Le bouton « N » à gauche de la mini-map (cliquable quand l'inventaire ou le chat est ouvert) choisit entre garder le nord en haut ou faire tourner la carte avec le joueur, sa direction de regard vers le haut ; dans ce cas, un « N » rouge sur le bord indique le nord. Ce choix est retenu dans `config/vanillasugar.properties`.

Le compas affiche aussi les marqueurs dans le monde : leur bannière, et au-dessus une ligne « nom · distance », apparaissent à l'écran là où ils se trouvent, même à travers le relief. La touche `N`, ou le bouton à gauche de la mini-map (cliquable quand l'inventaire ou le chat est ouvert), passe d'un affichage à l'autre : icône et texte, icône seule, ou rien. Ce choix est retenu dans `config/vanillasugar.properties`. Sans compas, les marqueurs ne s'affichent pas dans le monde (ni la mini-map), mais restent enregistrés.

### Carte

Avec une carte équipée (vierge ou remplie), la touche `M` ouvre la carte du monde, en plein écran dans le même cadre que la mini-map : tous les chunks explorés dans la dimension actuelle, avec la position du joueur et ses marqueurs ; le reste est couvert d'un brouillard de guerre sombre au grain léger, posé sur la grille des chunks, comme dans les jeux de stratégie. Le titre et la position sous le curseur sont posés sur la carte dans de petits cadres, l'aide est dans un bandeau sous la carte, et le nom de chaque marqueur est écrit sous sa bannière. On l'ouvre aussi avec son onglet en haut de l'inventaire. On la déplace en glissant avec la souris, les touches de zoom de la mini-map, `+` / `-` tapés ou la molette zooment, `Espace` recentre sur le joueur et la position sous le curseur est affichée en haut à gauche. Elle se ferme avec `Échap` ou la touche qui l'ouvre, comme le rappelle l'aide en bas de l'écran.

Les marqueurs se gèrent depuis cette carte : un clic droit demande un nom (facultatif) et une couleur parmi les 16 teintures, `Entrée` pose le marqueur à cet endroit (au niveau du sol) et `Échap` annule. Un clic droit sur un marqueur existant le retire ; le survoler affiche ses coordonnées et ce rappel. Ils apparaissent comme des bannières de couleur sur la carte du monde, sur la mini-map avec leur nom (collés au bord quand ils sont hors de portée) et dans le monde avec un compas. Ils sont enregistrés sur le joueur (64 au maximum, toutes dimensions confondues) et conservés à la mort.

L'exploration est enregistrée en permanence, même sans carte équipée : en équiper une plus tard montre tout ce qui a déjà été parcouru. Les chunks à moins de 6 chunks du joueur comptent comme explorés. Les données restent côté client : dans le dossier du monde en solo (`vanillasugar/worldmap`), dans `vanillasugar/worldmap/servers/<adresse>` du dossier du jeu en multijoueur.

Toutes ces touches se changent dans Options → Commandes → Vanilla Sugar. Minecraft enregistre les touches par position sur un clavier QWERTY ; `M`, `B`, `N`, `+` et `-` suivent plutôt le caractère imprimé sur le clavier du joueur, pour qu'en AZERTY la carte s'ouvre bien avec `M`. Si la touche correspondante sert déjà à autre chose, le zoom prend une autre touche : sur Mac AZERTY, `+` est la touche de commande, le zoom avant passe donc sur `)`, juste à côté du `-` ; sur PC AZERTY, `-` est le 6 de la barre d'objets, le zoom arrière passe sur le `-` du pavé numérique.

### Montre

Avec une montre équipée dans son slot, sous la carte, un encadré s'affiche en haut à gauche de l'écran, dans le même cadre que la mini-map :

- l'heure sur 24 heures (`--:--` dans le Nether, où la montre s'affole) et une icône du temps qu'il fait : soleil ou lune, pluie, neige selon le biome, nuages là où il ne pleut jamais (désert), orage ;
- le jour, en comptant à partir du jour 1 ;
- la prévision : après l'icône du temps actuel, une flèche et une petite icône montrent le prochain changement de temps. Le serveur l'envoie chaque seconde, d'après ses compteurs de météo. Elle n'est pas affichée sur un serveur sans le mod, ni quand la météo est figée (`advance_weather` à `false`). Quand l'orage peut éclater ou cesser avant le prochain changement de pluie, le jeu le tire au hasard plus tard : la prévision annonce alors simplement de la pluie.

Les icônes de météo sont dans l'image des textures d'interface (voir [Interface](#interface)).

### Rangement

De petits boutons en bois, à droite des titres, trient et rangent :

- **Coffres, tonneaux, coffres de l'Ender et boîtes de Shulker** : trier le contenant, ranger dedans les objets de l'inventaire qu'il contient déjà, et trier l'inventaire à droite du titre « Inventaire ».
- **Inventaire** : trier l'inventaire. Ranger demande d'ouvrir le coffre : il n'y a pas de rangement à distance.
- **Sac à dos ouvert** : le trier, et y ranger les objets qu'il contient déjà.

Le tri regroupe les piles identiques puis les range par objet. Il touche les 27 emplacements de l'inventaire, jamais la barre d'objets ni l'armure ; le rangement ne prend pas non plus dans la barre d'objets. En survie, un clic molette (la touche « Choisir le bloc ») sur un emplacement trie la partie de la fenêtre qui le contient.

Après un clic avec Maj sur une pile, les piles survolées tant que Maj reste enfoncée sont transférées à leur tour, comme si on cliquait sur chacune avec Maj : il suffit de glisser le curseur sur une rangée pour la vider dans le coffre. Le transfert s'arrête quand on relâche Maj. Dans l'inventaire du mode créatif, le clic avec Maj garde son comportement habituel.

Au survol, un bundle, une boîte de Shulker ou un sac à dos montre son contenu dans la grille de l'inventaire : neuf cases par rangée à partir de la gauche, trois rangées au plus avec le nombre des autres objets dans la dernière case, et une barre de remplissage ambrée dans le style de la barre d'expérience. Toutes les infobulles ont une marge intérieure de 6 pixels au lieu de 3. Une boîte de Shulker de butin pas encore ouverte garde le « ??? » du jeu.

### Récolte au clic droit

Un clic droit sur une culture mûre (blé, carottes, pommes de terre, betteraves, verrues du Nether, cacao) la récolte et la replante aussitôt. Elle donne ce que la casser aurait donné, enchantements de l'outil compris, moins la graine remise en terre. Accroupi, le clic droit reste celui du jeu.

### Élytre

Une élytre se porte dans son slot, à droite de la main secondaire, et laisse la place du plastron libre : on vole avec son armure. Un clic droit avec une élytre en main, ou un clic avec Maj dans l'inventaire, la met dans ce slot. Elle vole, s'use, se répare avec Raccommodage et dessine ses ailes (au-dessus du plastron, à la place de la cape) comme une élytre portée en plastron ; les autres joueurs la voient aussi. Une élytre portée en plastron fonctionne toujours comme dans le jeu.

### Entonnoir filtrant

Chaque entonnoir a, à gauche de ses cinq emplacements, un emplacement de filtre : l'objet qu'on y pose est le seul que l'entonnoir accepte, qu'il le prenne dans le contenant au-dessus de lui, le ramasse au sol ou qu'un autre entonnoir le lui envoie. Les autres objets restent où ils sont. Pour un trieur, il suffit de poser des entonnoirs avec un filtre sous une rangée d'entonnoirs qui se passent les objets : chacun prend son objet au passage et laisse continuer le reste. Comme dans le jeu, un entonnoir ne prend qu'un objet tous les 8 ticks : un flot plus rapide d'un même objet en laisse filer une partie jusqu'au bout de la rangée.

- Le filtre compare l'objet, pas ses enchantements ni son nom : un livre enchanté laisse passer tous les livres enchantés. Sans filtre, il se comporte comme un entonnoir ordinaire.
- Le filtre est un vrai objet, un seul : il est rendu quand on casse l'entonnoir. Un clic avec Maj depuis l'inventaire pose d'abord le filtre s'il est vide, puis remplit l'entonnoir.
- Pour le reste, l'entonnoir reste celui du jeu : même vitesse, bloqué par un signal de redstone, lu par un comparateur (sans compter le filtre).
- Un entonnoir avec un filtre se reconnaît à la grille en fer au fond de sa cuve et aux rivets sur ses flancs. Ces textures sont dérivées de celles de l'entonnoir par [tools/filtered_hopper_textures.py](tools/filtered_hopper_textures.py).

### Petits ajustements

- **Villageois en laisse** : une laisse s'attache aux villageois, pour les mener jusqu'à un comptoir d'échange ou un champ. Comme pour les autres animaux, un clic sur un villageois tenu en laisse le détache ; les marchands ambulants ne se tiennent toujours pas en laisse.
- **Bateaux sur terre** : un bateau franchit les marches de deux pixels au plus, laissées par les blocs un peu plus bas qu'un bloc plein : chemin de terre ou terre labourée vers l'herbe, sable des âmes ou boue vers la pierre, tapis, fine couche de neige. Dans le jeu, un seul pixel suffit à le bloquer.
- **Table d'enchantement** : l'infobulle de chaque option liste tous les enchantements qu'elle donnera, et non plus un seul suivi de « ... ? ». Le client les calcule comme le serveur, à partir de la graine d'enchantement que le serveur lui envoie en entier (le jeu ne la transmet que sur 16 bits, ce qui ne suffit pas). Sur un serveur sans le mod, l'infobulle reste celle du jeu.
- **Raccommodage et Infinité** sont compatibles sur un même arc.
- **Feuilles** : quand on coupe le tronc d'un arbre, ses feuilles disparaissent aussitôt, de proche en proche, au lieu de s'éteindre une à une pendant plusieurs minutes. Elles donnent toujours leurs pousses, bâtons et pommes ; les feuilles posées par un joueur ne disparaissent jamais.
- **Écran de mort** : les boutons « Réapparaître » et « Écran titre » sont cliquables tout de suite, sans la seconde d'attente du jeu.
- **Enclume** : plus de « Trop cher ! ». Le coût s'affiche quel qu'il soit, en rouge s'il manque des niveaux, et l'enclume accepte toute réparation que le joueur peut payer.
- **Portes doubles** : ouvrir ou fermer une porte en bois ouvre ou ferme aussi celle d'à côté, quand les deux forment une porte double (même bois, même sens, charnières opposées).
- **Poudre d'os sur la canne à sucre et le cactus** : elle ajoute un bloc en haut de la tige, jusqu'aux trois blocs qu'elle atteint seule. Un distributeur de poudre d'os fait de même.

## Interface

Toute l'interface suit une même direction artistique, sobre et un peu plus ornementée que le vanilla : des emplacements brun foncé encadrés de bois clair, sur des panneaux en bois.

- **HUD** : barre d'objets aux emplacements encadrés, sélection en cadre clair épais, barre d'expérience en baguette de bois cerclée aux extrémités et au milieu (sa progression en vert), petit viseur en croix, cœurs roses, cadres d'effets en bois.
- **Fenêtres** (inventaire, coffres, fours, établi, enclume, villageois, mode créatif…) : les gris vanilla deviennent bois et emplacements sombres, avec une rainure sculptée le long du bord. Les titres sont écrits en crème.
- **Widgets** : boutons, champs de texte, curseurs, cases à cocher, onglets et infobulles reprennent les mêmes bruns.

Les icônes vanilla (flèches, flammes, dessins) gardent leur forme.

Le livre de recettes ressemble à un livre : une couverture en cuir cousue, comme le sac à dos, autour de pages crème dont on voit la tranche, avec des onglets bordés de cuir : celui de la catégorie ouverte tient une page, son papier traverse la couverture et se joint aux pages, les autres restent sur fond brun. Les emplacements de recettes et les flèches prennent des tons de papier, et le numéro de page est écrit à l'encre.

Toutes les textures d'interface (HUD, fenêtres, widgets, livre de recettes, sac à dos…) sont réunies dans une seule image, [textures/gui.png](textures/gui.png), rangée par sections titrées. Il suffit de la modifier dans un logiciel de dessin puis de relancer le jeu : le build la redécoupe en fichiers selon [textures/gui.json](textures/gui.json). Chaque texture est rognée à ses pixels visibles et entourée d'un cadre rose vif qui montre où sa zone s'arrête, avec son nom écrit au-dessus : on peint à l'intérieur du cadre. Les cadres, les noms et l'espace entre les textures sont ignorés. `python3 tools/gui_atlas.py` sans nouvelle texture réorganise l'image.

Les couleurs de départ sont réunies dans [tools/gui_theme.py](tools/gui_theme.py). Après modification, cette commande redessine toutes les textures et remplace l'image (les retouches faites à la main dans l'image sont alors perdues) :

```sh
python3 tools/build_gui.py
```

Sans argument, elle prend le jar téléchargé par Gradle (lancer `./gradlew build` une première fois). Les formes restent dans les scripts de dessin : [tools/gui_style.py](tools/gui_style.py) (HUD, fenêtres, widgets), [tools/gui_textures.py](tools/gui_textures.py) (sac à dos, signet) et [tools/recipe_book_textures.py](tools/recipe_book_textures.py) (livre de recettes). Pour ajouter une texture à l'image, la déposer à son emplacement sous `src/main/resources/assets` puis lancer `python3 tools/gui_atlas.py`.

## Recettes rééquilibrées

| Objet | Vanilla | Vanilla Sugar |
|---|---|---|
| Boutons (bois, pierre, blackstone polie) | 1 bloc → 1 | 1 bloc → 16 |
| Trappes en bois | 6 planches → 2 | 6 planches → 24 (4 par planche) |
| Plaques de pression (sauf pondérées) | 2 blocs → 1 | 2 blocs → 16 (8 par bloc) |
| Bâtons (planches) | 2 planches → 4 | 2 planches → 32 (16 par planche) |
| Échelles | 7 bâtons → 3 | 7 bâtons → 8 |
| Escaliers (établi) | 6 blocs → 4 | 6 blocs → 8 |
| Bois / hyphes (écorce) | 4 bûches → 3 | 4 bûches → 4 |
| Panneaux | 6 planches + 1 bâton → 3 | 6 planches + 1 bâton → 12 |
| Portes en bois | 6 planches → 3 | 6 planches → 12 (2 par planche) |
| Bols | 3 planches → 4 | 3 planches → 12 (4 par planche) |

Les escaliers et les dalles se recraftent en blocs, sans perte : 4 escaliers en carré donnent 3 blocs, 2 dalles côte à côte en donnent 1 (deux dalles l'une sur l'autre gardent leur recette vanilla, comme la pierre taillée sculptée). Ces recettes apparaissent dans le livre de recettes dès qu'on ramasse un escalier ou une dalle.

Les recettes sont générées depuis le jar vanilla par [tools/rebalance_recipes.py](tools/rebalance_recipes.py). Seule la quantité produite est modifiée : formes, groupes du livre de recettes et déblocages restent ceux du vanilla. Le même script écrit les recettes escaliers et dalles → blocs dans `data/vanillasugar/recipe/uncraft`. Pour ajuster un chiffre, il suffit de modifier le tableau `RULES` puis de relancer le script :

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
- `./gradlew runClientGameTest` : lance le jeu en mode test automatique (`src/gametest`). Il crée un monde, équipe le sac à dos, le compas et la carte, explore et ouvre la carte du monde, y pose un marqueur nommé puis le retire, change l'affichage des marqueurs, ouvre et ferme le sac avec `B`, photographie les fenêtres habillées, règle le filtre d'un entonnoir, vérifie la sauvegarde et la mort, puis ferme le jeu. Les captures d'écran, en 1920×1080 avec l'échelle d'interface automatique, sont dans `build/run/clientGameTest/screenshots/`.

Le projet demande Java 25. Gradle le télécharge automatiquement pour ce projet (voir `gradle/gradle-daemon-jvm.properties`), il n'y a rien à installer.

Le code est réparti entre `src/main` (commun client et serveur : objets, slots, recettes), `src/client` (rendu : HUD, mini-map, écrans) et `src/gametest` (tests automatiques).
