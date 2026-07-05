# Nouveautés v28 — Réglages musicaux & fichiers visibles 🎚

- **🎚 Réglages de création** (deux listes au-dessus des boutons) :
  - **Tempo** : Auto, ou fixe de 70 à 160 BPM.
  - **Longueur** : Court (4 mesures), Normal (8), Long (16), Très long (32)
    — en 70 BPM, « Très long » ≈ 2 minutes de musique.
  - Appliqués au REMIX comme au synthé ; la structure AABA (le pont
    contrasté) s'adapte automatiquement à la longueur choisie.
- **💾 Fichiers enfin visibles** : après chaque génération, le statut affiche
  les noms EXACTS créés — « musique_0705_1432.wav (audio) +
  musique_0705_1432.mid (partition MIDI) → Download/IATrio/creations/ ».
- **⚠ Fini les sauvegardes silencieusement ratées** : si la permission
  « Accès à tous les fichiers » manque, le statut te le dit clairement et
  t'indique où l'activer (avant, les fichiers n'étaient simplement pas
  écrits, sans prévenir — c'est pour ça que tu ne trouvais pas le MIDI !).
- Nouvelle section tutoriel : « Où sont mes fichiers ? ».

# v27 — Remix  # v26 — Composition  # v25 — MIDI  # v24-v2 — Fondations

## Mettre à jour

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v28.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v28 reglages tempo longueur fichiers"
git push
```
