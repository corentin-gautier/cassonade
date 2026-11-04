# Captures d'écran du site

Chaque emplacement du site affiche un cadre « Screenshot coming soon » tant que son image n'existe pas. Pour le remplacer, déposer ici un fichier au nom de l'emplacement, en `.avif` (de préférence), `.webp`, `.png` ou `.jpg`, en 16:9 (idéalement 1920×1080) :

| Fichier | Section |
|---|---|
| `hero` | Grande image en haut de la page |
| `backpack` | Sac à dos |
| `minimap` | Mini-map au compas |
| `worldmap` | Carte du monde et marqueurs |
| `clock` | Montre |
| `inventory` | Rangement, tri, aperçus |
| `harvest` | Récolte au clic droit |
| `elytra` | Slot élytre |
| `hopper` | Entonnoir filtrant |
| `quests` | Journal de quête et progrès |
| `advancements` | Écran des progrès (deuxième image du carrousel des quêtes) |
| `combat` | Barre de vie, « ! » et œil |
| `spyglass` | Un cheval à travers la longue-vue de naturaliste, son panneau sous le viseur |
| `torch` | La torche sculk de nuit, les têtes de creeper sur l'herbe où des monstres peuvent apparaître |
| `interface` | Interface retravaillée |
| `interface-info` | Infobulle de l'icône « ! » d'un four (carrousel de l'interface) |
| `interface-loading` | Chargement d'un monde, la carte explorée en sépia et les chunks chargés en couleur (carrousel de l'interface) |
| `interface-worlds` | Choix du monde, trois mondes dont celui des captures (carrousel de l'interface) |
| `worldmap-biome` | Biome sous le pointeur, avec Maj (carrousel de la carte du monde) |
| `inventory-food` | Cuisses de poulet qui clignotent pour l'aliment en main (carrousel du rangement) |
| `spyglass-bees` | Une ruche vue à la longue-vue de naturaliste (carrousel de la longue-vue) |
| `woodcutter` | La fenêtre du tailleur de bois |
| `tweaks` | Puissance d'un fil de redstone |
| `tweaks-copper`, `tweaks-chest`, `tweaks-bookshelf`, `tweaks-armor-stand` | Cuivre ciré, coffre renommé, étagère sculptée, support d'armure (carrousel des petits détails) |
| `tweaks-pirate-booty` | Un butin de pirate dans une grotte luxuriante (carrousel des petits détails) |
| `tweaks-town` | Un bourg vu d'en haut, distance de rendu poussée (carrousel des petits détails) |
| `ambience-aurora` | Aurores sur une plaine enneigée, de nuit : le fond de l'en-tête de la page d'accueil, en 2000×1254 |
| `graphics` | Le soleil qui se couche derrière la mer (lumière, ciel et mer) |
| `graphics-underwater` | Le soleil vu de sous la mer (carrousel de lumière, ciel et mer) |
| `sea` | Sous la mer sans masque, un noyé qui surgit du trouble |
| `sea-mask` | La même vue à travers le masque de plongée (carrousel de la mer) |

La tâche VS Code « Prendre les captures d'écran » les reprend toutes : `./gradlew runClientGameTest -PsiteScreenshots` (`SiteScreenshotsClientGameTest`) les dépose ici en `.png` (en 1920×1080, l'interface un cran sous l'échelle automatique ; le butin, le bourg, la mer, les aurores, le coucher de soleil et la vue sous l'eau, sans interface, dans un monde neuf de graine fixe, `vanillasugar`, où ces lieux reviennent à chaque fois ; les autres dans un monde préparé à la main en jeu : sa scène, l'équipement du joueur, ses waystones et sa carte explorée. Il est gardé dans `.test-worlds/site-world`, chaque lancement travaille sur une copie, et il ne se refait pas seul : ne pas supprimer ce dossier) ; puis `tools/convert-screenshots.sh` les convertit avec l'outil d'images de macOS et supprime les `.png` :

- pour le site, `<id>.avif` et deux copies réduites, `<id>-640.avif` et `<id>-1280.avif`. La page les affiche à la taille qu'elles y prennent (une colonne d'environ 580 pixels, toute la largeur sous 960) ; l'image en 1920 ne se charge qu'en grand, dans la visionneuse ;
- pour les pages Modrinth et CurseForge, `store/screenshots/<id>.jpg`.

Le script se relance seul sur des `.png` déposés ici à la main.

Les icônes des blocs des recettes, dans `../items` (paratonnerre, capteur sculk, tailleur de bois, pierre de voyage, briques de pierre taillée), ne viennent pas des captures : `./gradlew runClientGameTest -PsiteIcons` les fait dessiner par le jeu, à ne relancer que si l'un de ces blocs change.
