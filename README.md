# Vanilla Sugar

Un mod Fabric pour Minecraft 26.3 qui corrige d'un coup les petits agacements du jeu, en restant au plus près de l'expérience vanilla : pas de nouveaux minerais, pas de magie, pas de machines. Uniquement ce qui manque ou ce qui est incohérent.

## Fonctionnalités

| # | Fonctionnalité | État |
|---|---|---|
| 1 | **Sac à dos** : craft 4 cuir + 4 ficelles + 1 bundle. Équipé dans un slot dédié, il s'ouvre avec un onglet en haut de l'inventaire, dans un panneau en cuir sur le côté gauche, à la place du livre de recettes, et offre 54 emplacements sur 9 colonnes (deux fois l'inventaire principal, comme un grand coffre). | Fait |
| 2 | **Slot compas** : un compas équipé affiche une mini-map, permet de poser des marqueurs et affiche les coordonnées et le biome. | Fait |
| 3 | **Slot carte** : une carte du monde équipée débloque la carte du monde (touche `M`), qui montre tous les chunks déjà visités par le joueur. | Fait |
| 4 | **Rééquilibrage des recettes** : quantités produites cohérentes avec la matière utilisée (voir ci-dessous). | Fait |
| 5 | **Slot montre** : une montre équipée affiche en haut à gauche l'heure, la météo, la prochaine météo, le jour et quand on peut dormir. | Fait |
| 6 | **Rangement** : trier un coffre ou l'inventaire (bouton ou clic molette), transférer plusieurs piles d'un glissement avec Maj, remplir ou vider un bundle d'un glissement, sons des gestes d'inventaire, ranger dans le coffre ouvert les objets qu'il contient déjà, aperçu du contenu des boîtes de Shulker et du sac au survol, aperçu des cartes remplies, main réapprovisionnée depuis l'inventaire, fabrication en masse depuis le livre de recettes. | Fait |
| 7 | **Récolte au clic droit** : un clic droit sur une culture mûre la récolte et la replante. | Fait |
| 9 | **Slot élytre** : une élytre se porte dans son propre slot, en plus du plastron. | Fait |
| 10 | **Petits ajustements** : villageois en laisse, bateaux qui franchissent les petites marches, table d'enchantement qui annonce tous les enchantements, Raccommodage et Infinité ensemble, feuilles qui tombent dès qu'on coupe l'arbre, écran de mort sans attente, enclume jamais « Trop chère », portes doubles, poudre d'os sur la canne à sucre et le cactus, sommeil deux fois plus rapide, composteur prêt sans attente qui donne 8 poudres d'os. | Fait |
| 11 | **Entonnoir filtrant** : chaque entonnoir a un emplacement de filtre ; avec un objet dedans, il ne laisse passer que cet objet. Fini les trieurs en redstone. | Fait |
| 12 | **Journal de quête et progrès** : un journal façon RPG, proposé en début de partie, guide le joueur d'un progrès à l'autre avec la quête en cours sur le côté de l'écran ; un écran Progrès liste tous les progrès ; chaque progrès obtenu est célébré au centre de l'écran. | Fait |
| 13 | **Combat** : la vie des monstres en combat s'affiche au-dessus de leur tête, un « ! » signale ceux qui prennent le joueur en chasse, et un œil en haut à gauche indique qu'on est pris pour cible. | Fait |
| 8 | **Interface retravaillée** : une GUI plus soignée et professionnelle, sans dénaturer le style du jeu. Une direction artistique commune au HUD et à toutes les fenêtres : bois et emplacements bruns, un peu plus ornementés que le vanilla tout en restant sobres (voir ci-dessous). | En cours |

Les slots dédiés sont un principe central : les nouvelles fonctions se débloquent en équipant un objet vanilla (compas, montre) ou fabriqué à partir d'un objet vanilla (carte du monde, sac à dos), pas par magie. La progression vanilla reste donc la même.

### Slots d'équipement

Les slots sont rangés par usage. Les instruments qui affichent quelque chose à l'écran sont dans la colonne au-dessus de la main secondaire : compas, carte du monde, montre. Ce qui se porte sur le corps suit la main secondaire sur la rangée du bas : l'élytre (voir [Élytre](#élytre)) puis le sac à dos.

- Un clic avec Maj sur un de ces objets dans l'inventaire l'équipe. Si le sac à dos est ouvert, les autres objets vont dedans (l'armure et le bouclier gardent la priorité vanilla) ; fermé, le clic avec Maj reste celui du vanilla. Les objets ramassés vont toujours dans l'inventaire.
- Des onglets en haut de l'inventaire ouvrent et referment, d'un clic, le livre de recettes (à la place du bouton vanilla), le sac à dos et, avec une carte du monde équipée, la carte du monde. L'onglet du panneau ouvert est plus clair et s'ouvre dans l'inventaire. Le livre ouvert se referme aussi avec son signet, le ruban qui dépasse en haut, et le sac avec la sangle de son rabat, bouclée par-dessus le panneau, ou encore avec la touche `B` (qui l'ouvre aussi) ou en fermant l'inventaire. Le sac et le livre de recettes ne tiennent pas ensemble à l'écran : ouvrir l'un ferme l'autre.
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

Avec un compas équipé, une mini-map s'affiche en haut à droite de l'écran, dans un cadre en bois, avec les couleurs des cartes vanilla : le terrain autour du joueur, nord en haut. Les touches `+` et `-` zooment et dézooment, de 32 à 256 blocs de côté (96 par défaut, retenu d'une partie à l'autre). Le nom du biome est écrit en dessous dans un petit cadre, puis les coordonnées (`X 12, Y 64, Z -30`) ; les effets de potion se rangent à gauche de la mini-map. Dans le Nether, la carte montre le sol à la hauteur du joueur plutôt que le plafond de bedrock. Le bouton « N » à gauche de la mini-map (cliquable quand l'inventaire ou le chat est ouvert) choisit entre garder le nord en haut ou faire tourner la carte avec le joueur, sa direction de regard vers le haut ; dans ce cas, un « N » rouge sur le bord indique le nord. Ce choix est retenu dans `config/vanillasugar.properties`.

Le compas affiche aussi les marqueurs dans le monde : leur bannière, et au-dessus une ligne « nom · distance », apparaissent à l'écran là où ils se trouvent, même à travers le relief. La touche `N`, ou le bouton à gauche de la mini-map (cliquable quand l'inventaire ou le chat est ouvert), passe d'un affichage à l'autre : icône et texte, icône seule, ou rien. Ce choix est retenu dans `config/vanillasugar.properties`. Sans compas, les marqueurs ne s'affichent pas dans le monde (ni la mini-map), mais restent enregistrés.

### Carte du monde

La carte du monde est un objet du mod, fabriqué à l'établi avec une carte vierge entourée de huit bâtons. La recette se débloque dès qu'on a une carte vierge. C'est le seul objet accepté par le slot carte : une carte vierge ou remplie ne s'y équipe pas.

```
 B B B     V = carte vierge, B = bâton
 B V B
 B B B
```

Sa texture, le terrain d'une carte remplie dans un cadre de bâtons, est générée à partir des textures du jeu par `tools/world_map_texture.py`.

Avec une carte du monde équipée, la touche `M` ouvre la carte du monde, en plein écran dans le même cadre que la mini-map : tous les chunks explorés dans la dimension actuelle, avec la position du joueur et ses marqueurs ; le reste est couvert d'un brouillard de guerre sombre au grain léger, posé sur la grille des chunks, comme dans les jeux de stratégie. Le titre et la position sous le curseur sont posés sur la carte dans de petits cadres, l'aide est dans un bandeau sous la carte, et le nom de chaque marqueur est écrit sous sa bannière. On l'ouvre aussi avec son onglet en haut de l'inventaire. On la déplace en glissant avec la souris, les touches de zoom de la mini-map, `+` / `-` tapés ou la molette zooment, `Espace` recentre sur le joueur et la position sous le curseur est affichée en haut à gauche. Elle se ferme avec `Échap`, la touche qui l'ouvre ou celle de l'inventaire (`E`), comme le rappelle l'aide en bas de l'écran.

Les marqueurs se gèrent depuis cette carte : un clic droit demande un nom (facultatif) et une couleur parmi les 16 teintures, `Entrée` pose le marqueur à cet endroit (au niveau du sol) et `Échap` annule. Un clic droit sur un marqueur existant le retire ; le survoler affiche ses coordonnées et ce rappel. Ils apparaissent comme des bannières de couleur sur la carte du monde, sur la mini-map avec leur nom (collés au bord quand ils sont hors de portée) et dans le monde avec un compas. Ils sont enregistrés sur le joueur (64 au maximum, toutes dimensions confondues) et conservés à la mort.

L'exploration est enregistrée en permanence, même sans carte du monde équipée : en équiper une plus tard montre tout ce qui a déjà été parcouru. Les chunks à moins de 6 chunks du joueur comptent comme explorés. Les données restent côté client : dans le dossier du monde en solo (`vanillasugar/worldmap`), dans `vanillasugar/worldmap/servers/<adresse>` du dossier du jeu en multijoueur.

Toutes ces touches se changent dans Options → Commandes → Vanilla Sugar. Minecraft enregistre les touches par position sur un clavier QWERTY ; `M`, `B`, `N`, `+` et `-` suivent plutôt le caractère imprimé sur le clavier du joueur, pour qu'en AZERTY la carte s'ouvre bien avec `M`. Si la touche correspondante sert déjà à autre chose, le zoom prend une autre touche : sur Mac AZERTY, `+` est la touche de commande, le zoom avant passe donc sur `)`, juste à côté du `-` ; sur PC AZERTY, `-` est le 6 de la barre d'objets, le zoom arrière passe sur le `-` du pavé numérique.

### Montre

Avec une montre équipée dans son slot, sous la carte, un encadré s'affiche en haut à gauche de l'écran, dans le même cadre que la mini-map :

- l'heure sur 24 heures (`--:--` dans le Nether, où la montre s'affole) et une icône du temps qu'il fait : soleil ou lune, pluie, neige selon le biome, nuages là où il ne pleut jamais (désert), orage ;
- le jour, en comptant à partir du jour 1 ;
- un petit lit après l'heure quand un lit permet de dormir à l'endroit où se trouve le joueur : la nuit ou pendant un orage, jamais dans le Nether ni l'End ;
- la prévision : après l'icône du temps actuel, une flèche et une petite icône montrent le prochain changement de temps. Le serveur l'envoie chaque seconde, d'après ses compteurs de météo. Elle n'est pas affichée sur un serveur sans le mod, ni quand la météo est figée (`advance_weather` à `false`). Quand l'orage peut éclater ou cesser avant le prochain changement de pluie, le jeu le tire au hasard plus tard : la prévision annonce alors simplement de la pluie.

Les icônes de météo et le lit sont dans l'image des textures d'interface (voir [Interface](#interface)).

### Rangement

De petits boutons en bois, à droite des titres, trient et rangent :

- **Coffres, tonneaux, coffres de l'Ender et boîtes de Shulker** : trier le contenant, ranger dedans les objets de l'inventaire qu'il contient déjà, et trier l'inventaire à droite du titre « Inventaire ».
- **Inventaire** : trier l'inventaire. Ranger demande d'ouvrir le coffre : il n'y a pas de rangement à distance.
- **Sac à dos ouvert** : le trier, et y ranger les objets qu'il contient déjà.

Le tri regroupe les piles identiques puis les range par objet. Il touche les 27 emplacements de l'inventaire, jamais la barre d'objets ni l'armure ; le rangement ne prend pas non plus dans la barre d'objets. En survie, un clic molette (la touche « Choisir le bloc ») sur un emplacement trie la partie de la fenêtre qui le contient.

Après un clic avec Maj sur une pile, les piles survolées tant que Maj et le bouton restent enfoncés sont transférées à leur tour, comme si on cliquait sur chacune avec Maj : il suffit de glisser le curseur sur une rangée pour la vider dans le coffre. Le transfert s'arrête dès qu'on relâche Maj ou le bouton.

De même, avec un bundle tenu au curseur, un clic gauche sur une pile la range dedans, et les piles survolées tant que le bouton reste enfoncé suivent, jusqu'à ce que le bundle soit plein. Un clic droit sur une case vide en sort une pile, et chaque case vide survolée tant que le bouton droit reste enfoncé en reçoit une autre, jusqu'à ce que le bundle soit vide. Ailleurs, les clics gardent leur effet du jeu (poser le bundle sur une case vide, ranger une pile au clic droit).

Les gestes de l'inventaire font un léger bruit, là où le jeu reste muet : prendre une pile ou la moitié d'une pile, la poser, l'échanger avec celle du curseur, la déplacer avec Maj ou une touche de la barre d'objets, la jeter hors de la fenêtre, la répartir en glissant, trier ou ranger ; changer d'emplacement dans la barre d'objets, à la molette ou au clavier, fait un petit clic. Ce sont les sons du bundle, à des hauteurs différentes selon le geste. Dans l'inventaire du mode créatif, le clic avec Maj garde son comportement habituel.

Au survol, un bundle, une boîte de Shulker ou un sac à dos montre son contenu dans la grille de l'inventaire : neuf cases par rangée à partir de la gauche, trois rangées au plus avec le nombre des autres objets dans la dernière case, et une barre de remplissage ambrée dans le style de la barre d'expérience. Toutes les infobulles ont une marge intérieure de 6 pixels au lieu de 3. Une boîte de Shulker de butin pas encore ouverte garde le « ??? » du jeu.

Au survol, une carte remplie montre la carte elle-même, en petit dans le cadre de la mini-map, avec son numéro écrit en petit dessous. Le serveur n'envoie le contenu d'une carte que lorsqu'elle est dans l'inventaire du joueur : une carte jamais portée, vue dans un coffre, garde l'infobulle du jeu.

Quand la pile tenue en main s'épuise en s'en servant (dernier bloc posé, dernier aliment mangé, dernier objet lancé ou donné à un animal, outil ou arme qui casse), une pile du même objet prise dans l'inventaire la remplace, en préférant une pile identique (mêmes enchantements, même usure). Seul l'inventaire est fouillé, jamais le sac à dos. Jeter une pile ou la déplacer dans l'inventaire ne déclenche rien.

Dans le livre de recettes de l'inventaire ou de l'établi, un clic avec `Ctrl` (ou `Cmd` sur Mac) sur une recette la fabrique autant de fois que les ingrédients de l'inventaire le permettent, en une fois, et range le résultat dans l'inventaire. Si l'inventaire est plein, ce qui reste est laissé dans la grille. Le clic avec Maj garde son effet du jeu : remplir la grille au maximum sans fabriquer.

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
- **Sommeil** : la nuit passe au bout de deux secondes et demie au lit au lieu de cinq, et l'écran s'assombrit deux fois plus vite.
- **Composteur** : une fois plein, il donne sa poudre d'os aussitôt, sans la seconde d'attente, et en donne 8 au lieu d'une. Un entonnoir dessous les prend toutes d'un coup, dès qu'il a la place pour les 8 : le composteur ne garde pas de reste.

### Combat

- **Vie des monstres** : une barre de vie s'affiche au-dessus de la tête d'un monstre qui chasse le joueur ou qui vient d'être blessé, et reste 5 secondes après le dernier coup. La vie perdue reste un instant en clair derrière la barre puis s'écoule, pour lire le coup porté. La barre se voit jusqu'à 32 blocs, seulement si le joueur voit le monstre, et rapetisse avec la distance.
- **Repéré** : un « ! » doré surgit au-dessus de la barre quand un monstre prend le joueur pour cible, et reste tant qu'il le poursuit.
- **Œil** : tant qu'au moins un monstre poursuit le joueur, même hors de vue, un œil s'affiche en haut à gauche de l'écran, à droite de l'encadré de la montre quand elle est équipée, dans le même cadre que les boutons du coin en bas à droite. Avec plusieurs poursuivants, leur nombre s'inscrit dans son coin ; l'œil rebondit chaque fois qu'un nouveau monstre repère le joueur.

Seul le serveur sait qui un monstre cible : il envoie au joueur la liste des monstres qui le chassent dans un rayon de 48 blocs. Sur un serveur sans le mod, seules les barres des monstres blessés s'affichent, sans « ! » ni œil.

Les sprites de la barre, du « ! » et de l'œil sont générés par `tools/combat_textures.py`.

### Journal de quête et progrès

**Célébration.** Un progrès obtenu s'annonce au centre de l'écran, à la place du petit encadré du coin : son icône dans un médaillon, au-dessus d'un cadre orné qui donne son type (« Progrès réalisé ! », « Objectif atteint ! », « Défi accompli ! »), son titre en grand et sa description. Plus le progrès est grand, plus la bannière l'est : cadre en bois et son de montée de niveau pour un progrès, cadre doré et rayons de lumière pour un objectif, cadre violet, titre trois fois plus grand et fanfare pour un défi. Elle s'affiche aussi par-dessus un inventaire ouvert ; plusieurs progrès obtenus ensemble passent l'un après l'autre.

**Boutons.** En bas à droite de l'écran, deux boutons restent affichés : le journal (un livre) et les progrès (une coupe), chacun avec sa touche écrite dans le coin. Ils restent au premier plan dans l'inventaire et se cliquent, même avec le livre de recettes ou le sac ouvert. Ils se cliquent dès que le curseur est libre (inventaire, coffre ou chat ouvert). En jeu, la touche `J` ouvre le journal et la touche des progrès du jeu (`L`) le nouvel écran Progrès. Chacun se ferme avec `Échap`, sa propre touche ou celle de l'inventaire (`E`), comme les fenêtres du jeu.

**Écran Progrès.** Il remplace l'arbre du jeu, aussi depuis le menu pause, dans le même livre que le journal : les onglets sur la page de gauche (Minecraft, Nether, End, Aventure, Agriculture, puis ceux des datapacks) et leurs progrès sur la page de droite, avec leur description et le compte des progrès obtenus. Un filtre montre tout, ce qui reste à faire ou ce qui est obtenu. Un progrès à plusieurs critères affiche une barre d'avancement ; un clic dessus liste ce qui reste, par exemple les biomes encore à visiter. Les progrès cachés restent des « Progrès secrets » jusqu'à ce qu'ils soient obtenus.

Le jeu n'envoie au joueur que les progrès proches de ceux déjà obtenus. Avec le mod, le serveur les envoie tous (en solo, le serveur intégré a le mod), pour que la liste et le journal montrent tout ce qu'il y a à faire. Sur un serveur sans le mod, la liste se limite à ce qu'il envoie.

**Journal de quête.** À la première partie dans un monde, le jeu propose de suivre le journal ; le choix se change à tout moment dans le journal. Le journal est un livre ouvert, relié de cuir comme le livre de recettes. À gauche, douze chapitres qui suivent une partie, des premiers pas aux défis légendaires, avec leur avancement. À droite, les quêtes du chapitre :

- d'abord les quêtes **ouvertes**, qui disent où aller et quoi faire ;
- puis les quêtes **verrouillées**, grisées, qui indiquent après quel progrès elles s'ouvrent ;
- enfin, sous un titre « Terminées », les quêtes **obtenues**, cochées.

Une quête s'ouvre quand le progrès qu'elle suit dans l'arbre du jeu est obtenu. La quête suivie s'affiche en haut à droite, sous la mini-map et de la même largeur (à sa place sans compas), marquée d'une étiquette « Quête en cours » (les effets de potion restent à sa gauche) ; elle reste au premier plan dans l'inventaire. Là où le curseur est libre (inventaire, coffres, chat), elle s'éclaire au survol et un clic ouvre le journal sur son chapitre, la liste défilée jusqu'à elle. Elle montre son chapitre, ce qu'il faut faire et, s'il y a plusieurs choses à faire, une barre d'avancement. C'est la quête choisie avec « Suivre » dans le journal, sinon la première quête ouverte. Une fois obtenue, elle reste quelques secondes : son étiquette devient « Quête terminée ! » en vert, son titre se barre et une coche s'affiche sur son icône. Elle glisse ensuite hors de l'écran, et la quête ouverte suivante arrive à sa place. La touche `K` la réduit à son titre, puis la masque, puis la raffiche en entier ; ce choix est retenu dans `config/vanillasugar.properties`.

Le mod ajoute ses propres progrès, dans un onglet « Vanilla Sugar » : équiper un sac à dos, un compas (mini-map), une carte du monde, et poser un marqueur sur la carte du monde. Le serveur les accorde en regardant l'équipement et les marqueurs du joueur chaque seconde, même pour un équipement porté avant l'ajout de ces progrès. Ils forment le chapitre « Équipement d'explorateur », après l'âge du fer.

Les 126 progrès du jeu et les 4 du mod sont répartis dans les chapitres de [chapters.json](src/main/resources/assets/vanillasugar/quests/chapters.json). Le titre et l'introduction des chapitres et les indices des quêtes sont dans les fichiers de langue (`quest.vanillasugar.*`). Une quête sans indice montre la description du progrès. Le choix de suivre le journal et la quête suivie sont enregistrés par monde, dans `vanillasugar/quests.properties`. Les textures (cadres, médaillons, rayons, icônes des boutons, couverture du journal) sont dessinées par [tools/quest_textures.py](tools/quest_textures.py).

## Interface

Toute l'interface suit une même direction artistique, sobre : des panneaux unis en bois mauve, des emplacements presque noirs, du cuir brun-rouge pour le sac et les livres, du papier crème pour les pages. Ce qu'on survole s'entoure de crème rosé (boutons, onglets, champs, emplacements, options d'enchantement, recettes du tailleur de pierre, motifs du métier à tisser, pouvoirs de la balise) ; ce qui est sélectionné s'encadre de vert, comme l'emplacement choisi de la barre d'objets. Sur le papier, le survol est rouge ruban.

- **HUD** : barre d'objets aux emplacements encadrés, sélection en cadre vert épais, barre d'expérience en baguette de bois cerclée aux extrémités et au milieu (sa progression en vert), petit viseur en croix, cœurs roses, effets de potion dans le même cadre que la mini-map, en haut à droite (à sa gauche quand elle est affichée), les bénéfiques sur une rangée et les néfastes sur la suivante, sur fond bleu-vert pour les effets d'une balise ; à côté de l'inventaire, ils gardent leur place et prennent le même cadre (`tools/panel_textures.py`). Les popups en haut à droite (nouvelles recettes, astuces du tutoriel, messages système, musique) prennent aussi ce cadre, titre doré et texte clair comme la quête suivie.
- **Fenêtres** (inventaire, coffres, fours, établi, enclume, villageois, mode créatif…) : les gris vanilla deviennent bois mauve et emplacements sombres. Les titres sont écrits en crème.
- **Widgets** : boutons, curseurs, cases à cocher, onglets et infobulles reprennent les mêmes bruns. Les champs de texte (carte du monde, enclume, chat…) sont creusés comme des emplacements, fond sombre et cadre qui s'éclaire quand on écrit.
- **Table d'enchantement** : le livre au-dessus des emplacements est dessiné en pixel art isométrique, relié de cuir rouge aux coins dorés. Il s'ouvre quand un objet peut être enchanté, tourne ses pages quand l'objet change, et des glyphes violets s'en élèvent.

Les icônes vanilla (flèches, flammes, dessins) gardent leur forme.

Le livre de recettes ressemble à un livre : une couverture en cuir cousue, comme le sac à dos, autour de pages crème dont on voit la tranche, avec des onglets bordés de cuir : celui de la catégorie ouverte tient une page, son papier traverse la couverture et se joint aux pages, les autres restent en papier. Les emplacements de recettes et les flèches prennent des tons de papier, et le numéro de page est écrit à l'encre. Le champ de recherche est écrit à l'encre sur la page. Dans l'établi et les fours, le bouton qui ouvre le livre n'est plus que le livre, sans fond de bouton.

Le clic droit sur une recette ouvre ses variantes sur une feuille reliée de cuir : chaque carte montre l'objet obtenu en taille réelle, avec sa quantité, sur fond rose quand il manque des ingrédients. Au survol d'une carte, une fiche s'ouvre à côté avec le nom de l'objet, la disponibilité des ingrédients, la grille de fabrication (ou l'emplacement du four) en taille réelle, une flèche et le résultat.

Toutes les textures d'interface (HUD, fenêtres, widgets, livre de recettes, sac à dos…) sont réunies dans une seule image, [textures/gui.png](textures/gui.png), rangée par sections titrées. Il suffit de la modifier dans un logiciel de dessin puis de relancer le jeu : le build la redécoupe en fichiers selon [textures/gui.json](textures/gui.json). Chaque texture est rognée à ses pixels visibles (une texture vide garde toute sa zone, pour pouvoir la redessiner plus tard) et entourée d'un cadre rose vif qui montre où sa zone s'arrête, avec son nom écrit au-dessus : on peint à l'intérieur du cadre. Les cadres, les noms et l'espace entre les textures sont ignorés. `python3 tools/gui_atlas.py` sans nouvelle texture réorganise l'image.

Pour changer les teintes d'un coup, `python3 tools/gui_psd.py` écrit `textures/gui.psd` : la même image, ses pixels triés en calques par famille de couleur (bois, fonds sombres, cuir, papier, crème des survols, vert des sélections, or, améthyste, rouge, icônes, divers), plus un calque verrouillé pour les cadres roses et les noms. On ajoute un réglage Teinte/Saturation (ou Courbes) écrêté sur le calque à changer, puis on exporte l'image aplatie en PNG, transparence conservée, à la place de `textures/gui.png`. Le PSD n'est pas relu par le build : après une modification de `gui.png` faite ailleurs, on le régénère, et les réglages ajoutés sont à refaire.

Les couleurs de départ sont réunies dans [tools/gui_theme.py](tools/gui_theme.py). Après modification, cette commande redessine toutes les textures et remplace l'image (les retouches faites à la main dans l'image sont alors perdues) :

```sh
python3 tools/build_gui.py
```

Sans argument, elle prend le jar téléchargé par Gradle (lancer `./gradlew build` une première fois). Les formes restent dans les scripts de dessin : [tools/gui_style.py](tools/gui_style.py) (HUD, fenêtres, widgets), [tools/gui_textures.py](tools/gui_textures.py) (sac à dos, signet) , [tools/recipe_book_textures.py](tools/recipe_book_textures.py) (livre de recettes) et [tools/enchanting_book_textures.py](tools/enchanting_book_textures.py) (livre de la table d'enchantement, rendu en 3D isométrique). Pour ajouter une texture à l'image, la déposer à son emplacement sous `src/main/resources/assets` puis lancer `python3 tools/gui_atlas.py`.

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
- `./gradlew runClientGameTest` : lance le jeu en mode test automatique (`src/gametest`). Il crée un monde, équipe le sac à dos, le compas et la carte du monde, explore et ouvre la carte du monde, y pose un marqueur nommé puis le retire, change l'affichage des marqueurs, ouvre et ferme le sac avec `B`, vérifie le réapprovisionnement de la main, la fabrication en masse et l'aperçu des cartes, photographie les fenêtres habillées, règle le filtre d'un entonnoir, vérifie la sauvegarde et la mort, puis ferme le jeu. Les captures d'écran, en 1920×1080 avec l'échelle d'interface automatique, sont dans `build/run/clientGameTest/screenshots/`.

Le projet demande Java 25. Gradle le télécharge automatiquement pour ce projet (voir `gradle/gradle-daemon-jvm.properties`), il n'y a rien à installer.

Le code est réparti entre `src/main` (commun client et serveur : objets, slots, recettes), `src/client` (rendu : HUD, mini-map, écrans) et `src/gametest` (tests automatiques).

L'icône du mod, un morceau de sucre sur un panneau en bois, est dessinée par [tools/icon_texture.py](tools/icon_texture.py), qui en écrit aussi la copie du site.

## Publier une version

Les messages de commit suivent [Conventional Commits](https://www.conventionalcommits.org/fr/) (`feat(hopper): …`, `fix(map): …`), à partir desquels [git-cliff](https://git-cliff.org) écrit le [CHANGELOG](CHANGELOG.md) (réglages dans [cliff.toml](cliff.toml)). Seuls `feat`, `fix` et `perf` y apparaissent.

Une version se publie depuis GitHub, sans rien installer : onglet Actions → Release → Run workflow, en choisissant le type de version :

| Choix | Effet |
|---|---|
| `alpha`, `beta` | pré-version suivante : 0.1.0-alpha.1 → 0.1.0-alpha.2, puis → 0.1.0-beta.1 |
| `release` | version finale d'une pré-version : 0.1.0-beta.2 → 0.1.0 |
| `patch`, `minor`, `major` | 0.1.0 → 0.1.1, 0.2.0, 1.0.0 |

Le workflow ([release.yml](.github/workflows/release.yml)) met à jour la version dans `gradle.properties`, ajoute l'entrée du CHANGELOG, construit le mod, commite les deux fichiers, pose le tag `v<version>` et publie les jars dans une release GitHub, avec l'entrée du CHANGELOG pour notes. Une pré-version est marquée comme telle. Le calcul de la version est dans [tools/next-version.sh](tools/next-version.sh). Pour voir l'entrée qui sera ajoutée : `git cliff --unreleased` (git-cliff s'installe avec `brew install git-cliff`).

Chaque push sur `main` et chaque pull request construisent aussi le mod ([build.yml](.github/workflows/build.yml)), le jar reste téléchargeable dans l'onglet Actions.

## Site

Le site du mod est dans [site/](site/), publié sur GitHub Pages à chaque push sur `main` qui le modifie ([pages.yml](.github/workflows/pages.yml)). Dans les réglages du dépôt, Pages → Source doit être sur « GitHub Actions ». Une seule page, en anglais (`/`) et en français (`/fr/`) : le texte est dans [site/_data/en.yml](site/_data/en.yml) et [site/_data/fr.yml](site/_data/fr.yml), la mise en page dans [site/_layouts/home.html](site/_layouts/home.html). Les captures d'écran se déposent dans [site/assets/screenshots/](site/assets/screenshots/), sous le nom indiqué sur chaque emplacement ; les liens Modrinth et CurseForge s'ajoutent dans [site/_data/mod.yml](site/_data/mod.yml).
