# Nouveautés v18 — L'Exploration autonome 🧭

L'IA gagne sa liberté sur Internet :

- **Nouvelle carte « Exploration autonome »** (onglet Entraîner, indigo) :
  - **Sujet simple** (« volcans ») → elle part de l'article Wikipédia français
    (résolu via l'API de recherche) et suit les liens qui l'intriguent.
  - **URL complète** → elle explore ce site-là, de lien en lien (même domaine).
  - **Champ VIDE** → la curiosité pure : elle choisit ses sujets parmi ses
    propres étiquettes (ce qu'elle connaît l'intrigue), ou part d'une page
    Wikipédia au hasard si elle ne connaît encore rien.
- **Choix des liens "avec du caractère"** : à chaque page, elle privilégie un
  lien familier (contenant un mot qu'elle connaît) et deux découvertes pures —
  un vrai comportement de curiosité.
- **Journal de bord en direct** : « 📖 Je lis "Guitare électrique"... ✔ 8 432
  caractères appris... ✨ "Luthier" me fait de l'œil... »
- **Option 🖼** : apprendre aussi les images des pages, étiquetées avec le
  titre de l'article.
- **Garde-fous** : 5/10/20/30 pages max au choix, bouton Stop, pause polie de
  400 ms entre les pages, arrêt automatique en quittant l'app. Tout ce qu'elle
  apprend reste 100% local.

# v17 — La Totale  # v16 — Le vrai Punk  # v15-v2 — Fondations

## Mettre à jour

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v18.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v18 exploration autonome"
git push
```
