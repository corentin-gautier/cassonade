# Captures d'écran du site

Chaque emplacement du site affiche un cadre « Screenshot coming soon » tant que son image n'existe pas. Pour le remplacer, déposer ici un fichier au nom de l'emplacement, en `.webp` (de préférence), `.png` ou `.jpg`, en 16:9 (idéalement 1920×1080) :

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
| `combat` | Barre de vie, « ! » et œil |
| `interface` | Interface retravaillée |

`./gradlew runClientGameTest` produit déjà des captures en 1920×1080 dans `build/run/clientGameTest/screenshots/`. Pour convertir en WebP : `cwebp -q 85 capture.png -o backpack.webp`.
