# Nouveautés v5 — Formats élargis

- **Audio compressé** : MP3, FLAC, OGG, M4A, AAC, Opus décodés avec le décodeur
  natif d'Android (MediaCodec). Ta bibliothèque musicale entière devient une base
  d'entraînement ! (2 premières secondes de chaque morceau, mono, garde-fou de 8 s
  par fichier pour ne jamais bloquer le scan)
- **Fichiers 3D** : OBJ et STL (ASCII et binaire). Le modèle est projeté en 3 vues
  — dessus, face, côté — codées dans les canaux Rouge/Vert/Bleu d'une mini-image,
  que l'IA images apprend avec le nom du dossier comme étiquette. Elle peut donc
  reconnaître des FORMES 3D.
- **Formats code/texte élargis** (~33) : ajout de ts, jsx, tsx, php, rb, go, rs,
  swift, sql, yml, yaml, csv, ini, log, gradle, properties, bat, h, hpp...
- **Formats image élargis** : ajout de bmp et gif.
- Le compteur de scan affiche maintenant les modèles 3D séparément.

# v4 — Entraînement massif (dossier téléphone/carte SD)
# v3 — Graphisme (icône, logo, interface)
# v2 — Intelligence (RGB, FFT, oublier, compteurs)

## Mettre à jour

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v5.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v5 mp3 flac ogg 3d formats"
git push
```
