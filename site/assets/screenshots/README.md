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
| `worldmap-biome` | Biome sous le pointeur, avec Maj (carrousel de la carte du monde) |
| `inventory-food` | Cuisses de poulet qui clignotent pour l'aliment en main (carrousel du rangement) |
| `spyglass-bees` | Une ruche vue à la longue-vue de naturaliste (carrousel de la longue-vue) |
| `woodcutter` | La fenêtre du tailleur de bois |
| `tweaks` | Puissance d'un fil de redstone |
| `tweaks-copper`, `tweaks-chest`, `tweaks-bookshelf`, `tweaks-armor-stand` | Cuivre ciré, coffre renommé, étagère sculptée, support d'armure (carrousel des petits détails) |

La tâche VS Code « Prendre les captures d'écran » les reprend toutes : `./gradlew runClientGameTest -PsiteScreenshots` (`SiteScreenshotsClientGameTest`) les dépose ici en `.png` (un monde généré près d'un village de plaine, en 1920×1080, l'interface un cran sous l'échelle automatique) et redessine au passage les icônes des blocs des recettes dans `../items` (paratonnerre, capteur sculk, tailleur de bois) ; puis `tools/convert_screenshots.sh` les convertit avec l'outil d'images de macOS et supprime les `.png` :

- pour le site, `<id>.avif` et deux copies réduites, `<id>-640.avif` et `<id>-1280.avif`. La page les affiche à la taille qu'elles y prennent (une colonne d'environ 580 pixels, toute la largeur sous 960) ; l'image en 1920 ne se charge qu'en grand, dans la visionneuse ;
- pour les pages Modrinth et CurseForge, `store/screenshots/<id>.jpg`.

Le script se relance seul sur des `.png` déposés ici à la main.
