# Nouveautés v2

- **Images en couleur (RGB)** : reconnaissance basée sur les couleurs, pas juste le gris.
- **Data augmentation** : chaque image apprise génère 3 variantes (miroir, plus claire,
  plus sombre) → meilleure généralisation avec peu de photos.
- **Audio par FFT** : sons analysés par transformée de Fourier (32 bandes + fenêtre de
  Hann). Distingue notes, timbres, instruments — plus seulement le volume.
- **Boutons "Oublier"** par IA : efface une mémoire sans toucher aux autres.
- **Compteurs d'exemples** : chaque IA affiche "chat:5, chien:3" pour voir où il manque
  des données.
- **Orchestrateur plus malin** : annonce quand vision et ouïe sont d'accord, sinon
  conseille d'ajouter des exemples.

## Mettre à jour ton dépôt existant

Dans Termux, après avoir décompressé ce zip par-dessus l'ancien dossier :

```bash
cd ~/ia-trio
git add .
git commit -m "v2 couleur FFT oublier compteurs"
git push
```

GitHub reconstruira l'APK automatiquement (gh run watch pour suivre).
