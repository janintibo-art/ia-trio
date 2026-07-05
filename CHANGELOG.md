# Nouveautés v25 — Export MIDI 🎹

Chaque composition est maintenant récupérable en fichier MIDI standard pour
la retravailler dans n'importe quel logiciel de musique.

- **Fichier .mid généré à chaque création** (à côté du .wav, dans
  Download/IATrio/creations/) : format MIDI standard (SMF format 0).
- **Contenu de la partition** :
  - **Mélodie sur le canal 1** (piano) — toutes les notes que l'IA chante,
    avec leurs vraies durées (les voyelles longues !) et vélocités.
  - **Basse sur le canal 2** (basse électrique), dosée par tes graves.
  - **Tempo inclus** — celui dérivé de la sonorité de ta musique.
- **Compatible partout** : FL Studio, Ableton, GarageBand, Cubase, MuseScore,
  LMMS (gratuit sur PC)... Change les instruments, corrige des notes, ajoute
  des pistes, quantise : l'IA compose, TU produis.
- Techniquement : la synthèse capture désormais chaque note comme un
  événement (hauteur MIDI la plus proche, départ, durée, vélocité, canal),
  et l'app écrit le binaire MIDI complet (delta-times varint, meta tempo,
  program changes) — zéro dépendance externe.

Astuce : le .wav te donne le rendu avec la SONORITÉ de ta musique (v24),
le .mid te donne la PARTITION pour la réinterpréter — les deux ensemble,
c'est un vrai brouillon de morceau.

# v24 — Sonorités  # v23 — Performances  # v22-v2 — Fondations

## Mettre à jour

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v25.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v25 export midi"
git push
```
