# Nouveautés v7 — Accès Internet

- **Nouvelle carte Internet** (onglet Entraîner) :
  - **Texte → IA code** : colle l'URL d'une page (article, doc, blog) ; le texte
    est extrait du HTML et nourrit les complétions.
  - **Images → IA images** : colle l'URL d'une page ; jusqu'à 8 images sont
    téléchargées et apprises avec l'étiquette que tu donnes. Fonctionne aussi
    avec l'URL directe d'une image.
  - **Wikipédia** : tape juste un sujet (« chat », « guitare »...) et l'article
    français est appris automatiquement.
- Extraction HTML maison (scripts/styles supprimés, entités décodées), limite de
  3 Mo par téléchargement et 20 000 caractères par page, suivi des redirections,
  minuteries pour ne jamais bloquer l'app.
- Nouvelle section « Bien utiliser Internet » dans le tutoriel.
- Permission INTERNET ajoutée (c'est la permission Android standard, non
  dangereuse — aucune donnée de tes IA ne quitte le téléphone : l'app télécharge,
  elle n'envoie rien).

# v6 — Onglets, Profils & Tutoriel  # v5 — Formats élargis
# v4 — Entraînement massif  # v3 — Graphisme  # v2 — Intelligence

## Mettre à jour

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v7.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v7 acces internet"
git push
```
