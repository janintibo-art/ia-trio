# Nouveautés v3 — Graphisme

- **Icône d'application** : ton icône s'affiche maintenant sur l'écran d'accueil
  (toutes densités mdpi→xxxhdpi + version ronde).
- **Logo en en-tête** dans l'app.
- **Interface entièrement redessinée** : fond dégradé, cartes blanches arrondies
  avec liseré coloré par IA (bleu images, violet sons, orange code), boutons
  "pilule" en dégradé, champs de saisie stylés, boutons "Oublier" discrets en rouge.

# v2 — Intelligence

- Images en couleur (RGB) + data augmentation (miroir, clair, sombre).
- Audio par FFT (32 bandes de fréquences + fenêtre de Hann).
- Boutons "Oublier" par IA, compteurs d'exemples, orchestrateur plus malin.

## Mettre à jour ton dépôt

Dans Termux, après avoir décompressé ce zip par-dessus l'ancien dossier :

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v3.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v3 graphisme + icone"
git push
```

GitHub reconstruit l'APK automatiquement.
