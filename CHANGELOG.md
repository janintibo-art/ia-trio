# Nouveautés v23 — Performances ⚡

Les IA deviennent nettement plus intelligentes, avec les mêmes données.

- **🔁 Replay anti-oubli (LE gros correctif)** : jusqu'ici, chaque nouvel
  exemple était appris intensément... en écrasant partiellement les anciens
  (« oubli catastrophique »). Désormais, à chaque apprentissage, l'IA révise
  automatiquement 24 anciens souvenirs — sa mémoire reste stable et
  s'améliore au lieu de se dégrader.
- **🏋️ Ré-entraîner tout** (carte Bilan) : consolidation complète — 8 passes
  mélangées sur toute la mémoire avec taux d'apprentissage dégressif (la
  bonne méthode). À lancer après une grosse session de scan : le score
  d'examen grimpe visiblement. Les souvenirs sont maintenant gardés en
  mémoire vive (cache), donc c'est rapide.
- **🥇🥈🥉 Top 3 des devinettes** : « Je pense : chat (72%) — Aussi possible :
  chien 18% · voiture 5% ». Parfait pour repérer les confusions et savoir
  quoi entraîner.
- **📝 IA code à contexte long** : modèle à repli 5→4→3 caractères — elle
  essaie d'abord le contexte le plus précis, se replie sinon. Complétions
  bien plus cohérentes. + plafond de corpus (500 Ko, garde le plus récent)
  pour rester rapide même après des mois d'exploration.
- Hyperparamètres rééquilibrés (le replay compense des epochs réduits :
  apprentissage individuel plus rapide ET meilleur).

Conseil : après la mise à jour, appuie une fois sur « 🏋️ Ré-entraîner tout »
puis « Passer l'examen » — compare avec ton dernier score. 📈

# v22 — Créations & mémoire  # v21 — Scan Turbo  # v20-v2 — Fondations

## Mettre à jour

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v23.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v23 performances replay consolidation"
git push
```
