# Nouveautés v10 — Scan complet & Sauvegardes

- **Bouton « 📱 TOUT scanner (téléphone + SD) »** : parcourt l'intégralité du
  stockage interne ET des cartes SD/USB, sans rien sélectionner. Android
  demandera la permission « Accès à tous les fichiers » (l'app ouvre
  directement le bon écran de réglages) : active-la puis relance.
  - Ignore les dossiers cachés et Android/ (système).
  - Limite de visite passée à 25 000 fichiers pour le scan complet.
  - Les étiquettes = noms des dossiers réels traversés.
- **💾 Sauvegarder / ♻️ Restaurer le profil actif** (onglet Profils) : copie les
  mémoires du profil dans Download/IATrio/<nom>/ — tu ne perds plus tes
  cerveaux si tu réinstalles l'app, et tu peux même copier ce dossier sur un
  autre téléphone !
- Le sélecteur de dossier classique reste disponible pour des scans ciblés.

# v9 — IA Enfant  # v8 — Cerveau distant  # v7 — Internet
# v6 — Profils  # v5 — Formats  # v4 — Scan  # v3 — Graphisme  # v2 — Intelligence

## Mettre à jour

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v10.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v10 scan complet sauvegardes"
git push
```
