# Nouveautés v32 — Groove & Justesse 🎶

Trois techniques de studio pour un remix nettement plus « pro ».

- **🎯 Extraits alignés sur l'attaque** : à la capture, l'app détecte la plus
  forte montée d'énergie (transitoire) et coupe PILE dessus. Avant, l'attaque
  pouvait tomber au milieu de la tranche → chops en retard, groove flou.
  Maintenant chaque chop frappe SUR le temps.
  ⚠ Rescanne ta musique une fois pour re-capturer des extraits alignés.
- **🎼 Mise en tonalité automatique** : la hauteur dominante de chaque extrait
  est détectée (autocorrélation, 60-500 Hz) ; la basse donne la tonique, et
  la nappe + les 3 chops sont TRANSPOSÉS pour rentrer dans sa tonalité
  (repli ±6 demi-tons, extraits trop bruités laissés intacts). Des extraits
  venant de morceaux différents sonnent enfin ENSEMBLE.
- **🕺 Swing** : les croches impaires arrivent un poil en retard (6 à 18%
  du temps selon la créativité du profil) — la différence entre un
  métronome et un groove humain.
- **🚀 Riser** : la mesure avant le pont, un chop INVERSÉ monte en crescendo
  — l'annonce classique du « drop ». + un accent brillant à l'entrée du pont.
- **🎧 Largeur stéréo (effet Haas)** : les chops sont décalés de 9 ms entre
  gauche et droite — le son « s'ouvre » au casque.

# v31 — Arrangement  # v30 — Collage  # v29 — Harmonie  # v28-v2

## Mettre à jour

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v32.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v32 groove justesse"
git push
```
