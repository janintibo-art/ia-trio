# Correctifs v21 — Scan Turbo 🔧

Le scan trouvait peu de contenu : quatre causes identifiées et corrigées.

- **📸 Photos HEIC/HEIF reconnues** : les Samsung (et beaucoup d'autres)
  enregistrent les photos en .heic par défaut — format qui n'était PAS dans
  la liste ! C'était la cause principale. Corrigé.
- **📈 Plafonds fortement relevés** :
  - Scan d'un dossier : 1000 images, 200 textes, 200 sons, 60 modèles 3D
    (au lieu de 300/60/60/40), jusqu'à 25 000 fichiers visités.
  - Scan complet : **3000 images, 400 textes, 400 sons, 100 modèles 3D**,
    jusqu'à 100 000 fichiers visités.
- **💾 Détection de carte SD fiabilisée** : en plus de /storage, l'app remonte
  maintenant depuis ses propres dossiers sur chaque volume (méthode Android
  officielle) — la carte SD est trouvée même sur les appareils qui la cachent.
  Le scan annonce désormais ce qu'il a détecté : « 2 stockages détectés :
  téléphone, carte SD (1A2B-3C4D) — je fouille... »
- **⏳ Feedback continu** : le compteur se met à jour tous les 250 fichiers
  parcourus, même sans trouvaille — fini l'impression que l'app est bloquée.
- **🎬 Bonus : les VIDÉOS deviennent des sources audio** ! La piste son des
  .mp4, .3gp, .webm, .mkv est extraite (2 premières secondes) et apprise —
  tes vidéos de concert nourrissent l'IA sons.

Note : un scan complet apprend maintenant BEAUCOUP plus — il peut durer
plusieurs minutes. Le bouton Stop reste là, et tout est cumulatif.

# v20 — Le Créateur  # v19 — Le Chat  # v18 — Exploration  # v17-v2

## Mettre à jour

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v21.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v21 scan turbo heic sd"
git push
```
