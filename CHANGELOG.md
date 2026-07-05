# Nouveautés v35 — Marathon & grande bibliothèque 🏃📚

## Des morceaux de plus de 5 minutes
- **Nouveaux formats** : Épique (64 mesures ≈ 3 min) et **Marathon
  (128 mesures ≈ 6 min)** — le remix tient la distance : les motifs se
  RENOUVELLENT toutes les 16 mesures (jamais lassant), l'arc
  intro/pont/final s'étire proportionnellement, l'harmonie continue de
  tourner.
- **Ingénierie mémoire** pour y arriver :
  - buffers audio en Float (2× moins de RAM que Double) ;
  - mastering en deux passes SANS tableaux intermédiaires ;
  - grande mémoire Android activée (largeHeap) ;
  - **lecture en streaming** par blocs — l'ancien mode statique plantait
    au-delà d'une minute, celui-ci joue n'importe quelle durée.
- Un Marathon se génère en ~1-2 minutes sur téléphone : laisse l'app
  ouverte, le toast et le statut arrivent à la fin. Le WAV fait ~22 Mo,
  le MIDI reste minuscule. (Le synthé de repli reste limité à 64 mesures.)

## Pensé pour tes 15 000 musiques
- **Scan complet : 6000 sons par passage** (au lieu de 3000) et jusqu'à
  **300 000 fichiers visités** ; scan de dossier : 2000 sons.
- **Banque de remix : 2000 extraits réels** (au lieu de 400, ~32 Mo) —
  ta discothèque devient une palette géante, et chaque remix pioche
  parmi 32 extraits.
- Rappel : les scans sont CUMULATIFS (Stop quand tu veux, reprends plus
  tard) et un passage sur 6000 musiques prend du temps (~1-2 s par
  fichier) — parfait pendant une charge de nuit. 🔌🌙

# v34 — Les marges  # v33 — Genres  # v32 — Groove  # v31-v2

## Mettre à jour

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v35.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v35 marathon grande bibliotheque"
git push
```
