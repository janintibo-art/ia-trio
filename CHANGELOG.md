# Nouveautés v4 — Entraînement massif

- **Nouvelle carte "Entraînement massif"** (verte) : choisis n'importe quel dossier
  du téléphone ou de la carte SD, et l'app apprend automatiquement tout ce qu'elle
  y trouve :
  - **Images** (.jpg .png .webp) → étiquette = nom du dossier parent.
    Astuce : organise tes photos en dossiers `chats/`, `chiens/`, `plage/`...
    et l'IA apprend toutes les catégories d'un coup !
  - **Textes & code** (.txt .md .kt .java .py .js .html .css .json .xml .c .cpp .sh)
    → nourrissent l'IA code.
  - **Sons** (.wav) → étiquette = nom du dossier parent.
- **Bouton Stop** pour interrompre un scan en cours.
- **Progression en direct** : "Scan... 42 img, 3 txt, 1 wav".
- Limites de sécurité pour rester fluide : 300 images, 60 textes, 60 wav max
  par scan (relance un scan sur un autre dossier pour continuer).
- La sélection de dossier utilise le sélecteur officiel Android : fonctionne avec
  la carte SD, aucune permission dangereuse, et tout reste 100% local.

# v3 — Graphisme
- Icône d'app (toutes densités + ronde), logo en en-tête, interface redessinée.

# v2 — Intelligence
- Images RGB + data augmentation, audio FFT, boutons Oublier, compteurs.

## Mettre à jour

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v4.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v4 entrainement massif"
git push
```
