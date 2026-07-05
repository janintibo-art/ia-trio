# Nouveautés v24 — La sonorité de TA musique 🎸

La musique générée sonne maintenant comme la tienne.

- **Mémoire des timbres** : à chaque son/musique appris, l'IA mémorise son
  empreinte spectrale (32 bandes de fréquences), moyennée par étiquette
  (fichier timbre_aud.txt). Tes dossiers rock/, jazz/, piano/... deviennent
  des palettes sonores.
- **Synthèse façonnée par tes morceaux** : l'empreinte contrôle
  - les **harmoniques** de chaque note (synthèse additive 8 harmoniques) :
    musique brillante → notes cristallines, musique grave → notes chaudes ;
  - la **hauteur** de base (centre spectral : brillant = plus aigu) ;
  - le **tempo** (musique énergique dans les aigus = plus rapide) ;
  - l'**attaque/déclin** (brillant = percussif, sombre = nappes) ;
  - le **volume de la basse** (dosé sur les graves de ta bibliothèque).
- **Ciblage intelligent** : écris « rock » → sonorité exacte de ton dossier
  rock ; texte sans correspondance → mélange de la couleur générale de ta
  bibliothèque (6 empreintes au hasard). Le toast t'indique la sonorité
  utilisée.
- La mélodie continue de « chanter la pensée » de l'IA (v22) — maintenant
  avec TON grain sonore.

⚠ Important : les empreintes se capturent À L'APPRENTISSAGE. Rescanne ton
dossier musique une fois après cette mise à jour (les plafonds sont à 1000/
3000 sons depuis la v22, ça passe large).

# v23 — Performances  # v22 — Créations & mémoire  # v21-v2 — Fondations

## Mettre à jour

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v24.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v24 sonorites de ma musique"
git push
```
