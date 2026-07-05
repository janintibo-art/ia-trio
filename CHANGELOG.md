# Nouveautés v27 — REMIX de tes vrais morceaux 🎛

Fini l'imitation : l'IA réutilise maintenant TES fichiers audio.

- **Banque d'extraits réels** : à chaque musique apprise, les 3 tranches les
  plus énergiques (0,5 s de VRAI audio) sont conservées — jusqu'à 400
  extraits de tes morceaux, par profil, effacés par « Oublier les sons ».
- **Moteur de remix façon sampler MPC** (utilisé en priorité dès que la
  banque contient 3 extraits ou plus) :
  - **Chops rythmiques** : tes sons découpés sur la grille (structure AABA,
    motifs syncopés), pitchés en varispeed (±7 demi-tons selon la
    créativité), parfois INVERSÉS si le profil est créatif ;
  - **Nappe de fond** : un de tes extraits ralenti d'une octave, bouclé ;
  - **Basse** : ton propre son pitché à -17 demi-tons sur les temps ;
  - la pensée de l'IA choisit quels extraits jouer et quand ;
  - écho croisé stéréo + saturation douce au mastering.
  RIEN n'est synthétisé : chaque son du morceau vient de TES fichiers.
- **Ciblage** : « rock » → remix de ton dossier rock ; autre texte → mélange
  de toute ta bibliothèque. Le toast indique le nombre d'extraits utilisés
  et la taille de la banque.
- **Sortie STÉRÉO** (lecture et WAV) ; le synthé v26 (44,1 kHz, accords,
  batterie, réverbe) reste en repli si la banque est vide.
- ⚠ La banque se remplit À L'APPRENTISSAGE : rescanne ton dossier musique
  une fois après cette mise à jour.

# v26 — Composition  # v25 — MIDI  # v24 — Sonorités  # v23-v2

## Mettre à jour

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v27.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v27 remix vrais morceaux"
git push
```
