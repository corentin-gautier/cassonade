#!/bin/zsh
# Double-cliquer ce fichier dans le Finder lance Minecraft avec le mod.
# Le premier lancement prend quelques minutes (téléchargements), les suivants environ 30 secondes.
cd "$(dirname "$0")" || exit 1
./gradlew runClient
echo
echo "Minecraft est fermé. Vous pouvez fermer cette fenêtre."
