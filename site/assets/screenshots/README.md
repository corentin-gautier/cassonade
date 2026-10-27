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

`./gradlew runClientGameTest -PsiteScreenshots` les reprend toutes (`SiteScreenshotsClientGameTest`), et redessine au passage les icônes des blocs des recettes dans `../items` (paratonnerre, capteur sculk) : un monde généré près d'un village de plaine, en 1920×1080, l'interface un cran sous l'échelle automatique. Chaque image est déposée ici en `.png`, à convertir en AVIF avec l'outil d'images de macOS :

```
for f in *.png; do sips -s format avif -s formatOptions 80 "$f" --out "${f%.png}.avif" && rm "$f"; done
```

La page les affiche à la taille qu'elles y prennent (une colonne d'environ 580 pixels, toute la largeur sous 960), depuis deux copies réduites, `<id>-640.avif` et `<id>-1280.avif` ; l'image en 1920 ne se charge qu'en grand, dans la visionneuse. Pour les refaire :

```
for f in $(ls *.avif | grep -v -E -- '-[0-9]+\.avif$'); do for w in 640 1280; do
	sips --resampleWidth $w -s format png "$f" --out "/tmp/${f%.avif}-$w.png" && sips -s format avif -s formatOptions 80 "/tmp/${f%.avif}-$w.png" --out "${f%.avif}-$w.avif"
done; done
```
