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

`./gradlew runClientGameTest -PsiteScreenshots` les reprend toutes (`SiteScreenshotsClientGameTest`) : un monde généré près d'un village de plaine, en 1920×1080, l'interface un cran sous l'échelle automatique. Chaque image est déposée ici en `.png` ; les vues du monde gagnent à passer en JPEG, les écrans en aplats restent plus légers en PNG :

```
sips -s format jpeg -s formatOptions 88 hero.png --out hero.jpg && rm hero.png
```
