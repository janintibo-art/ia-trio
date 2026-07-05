# Nouveautés v22 — Créations nourries par la mémoire 🎨🧠

## L'IA crée avec ce qu'elle a appris
- **Mémoire des couleurs** : à chaque image apprise, l'IA mémorise désormais
  la moyenne des couleurs 8x8 par étiquette (fichier palette_img.txt, cumulé
  sur tous tes scans et apprentissages).
- **🖼 Image** : si ton texte contient un mot qu'elle connaît, l'image est
  peinte à 70% avec les VRAIES couleurs de tes photos apprises + une
  mosaïque fantôme du souvenir en fond. Le toast t'indique quels souvenirs
  ont servi : « peinte avec mes souvenirs de : chat, plage ».
- **🎵 Musique** : la mélodie CHANTE la pensée de l'IA ! Sa complétion de
  texte (issue de tout ce qu'elle a lu) est convertie note par note :
  chaque caractère = un degré de la gamme, les voyelles durent plus
  longtemps, les espaces respirent, les majuscules montent à l'octave.
  48 notes (~15-20 s de musique). Repli sur la mélodie aléatoire si la
  mémoire est vide.
- **💻 Code** : utilisait déjà la mémoire (IA code locale) — inchangé.

## Plafond des sons débloqué 🔊
- Scan de dossier : **1000 sons** (au lieu de 200).
- Scan complet : **3000 sons** (au lieu de 400) — comme les images.
- Textes aussi relevés : 300 / 500.
- Note : décoder 3000 musiques prend du temps (~1-2 s par fichier) ; le
  bouton Stop reste là et tout est cumulatif entre les scans.

# v21 — Scan Turbo  # v20 — Le Créateur  # v19-v2 — Fondations

## Mettre à jour

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v22.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v22 creations memoire sons 3000"
git push
```
