# Nouveautés v33 — La base des genres musicaux 🥁

L'IA connaît maintenant les codes rythmiques de 12 genres réels.

- **Nouveau fichier MusicStyles.kt** : une base de connaissances avec, pour
  chaque genre, son tempo authentique, ses patterns de grosse caisse /
  caisse claire / charley (grille de 16 doubles-croches), son swing, ses
  motifs mélodiques types et le placement de sa basse :
  - **Hip-hop** 85-95 BPM (boom-bap swingué) · **Trap** 130-150 (caisse en
    half-time, charleys en 16es) · **House** 120-128 (four-on-the-floor,
    charley ouvert à contretemps) · **Techno** 125-135 · **Reggae** 70-80
    (one drop : kick+caisse ensemble sur le 3, skank à contretemps) ·
    **Reggaeton** 90-100 (dembow) · **Funk** 95-112 (syncopes en 16es) ·
    **Rock** 110-140 · **Drum'n'bass** 165-178 · **Lo-fi** 75-88 (swing
    prononcé, batterie douce) · **Afrobeat** 100-115 · **Pop** 100-120.
- **Trois façons de choisir** : la liste déroulante (avec les BPM affichés),
  un mot-clé dans ton texte (« un beat reggae posé », « ambiance lofi »),
  ou 🎲 auto (la graine décide). Le tempo manuel garde la priorité.
- **Grille en doubles-croches** (16 pas par mesure au lieu de 8) :
  indispensable pour les charleys trap, le dembow et les syncopes funk.
- Le toast annonce le résultat : « REMIX Reggae à 74 BPM (24 extraits) ».
- Tout le reste est conservé : rôles, justesse, arrangement, ducking,
  riser, swing (celui du genre + ta créativité), export WAV+MIDI, partage.

# v32 — Groove  # v31 — Arrangement  # v30 — Collage  # v29-v2

## Mettre à jour

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v33.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v33 genres musicaux"
git push
```
