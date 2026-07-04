# Nouveautés v13 — TensorFlow 🧠 & Multi-API ☁️

## TensorFlow Lite (transfer learning)
- L'IA images utilise désormais **MobileNet**, un réseau pré-entraîné sur
  1,2 million d'images, embarqué dans l'APK (~4 Mo, téléchargé automatiquement
  par GitHub Actions pendant la construction).
- **Elle reconnaît ~1000 objets dès l'installation** : la ligne « Base
  MobileNet » sous « Deviner » montre ce qu'elle voit toute seule.
- **Ton entraînement se fait par-dessus** (transfer learning sur 1001
  caractéristiques de haut niveau au lieu des pixels bruts) : précision
  nettement supérieure avec moins d'exemples.
- Repli automatique sur le mode pixels si le modèle est absent. Les anciennes
  mémoires pixels restent intactes (fichiers séparés).

## Cerveau distant multi-fournisseurs
- Liste déroulante de **5 fournisseurs** : Gemini (Google, gratuit),
  **Groq (gratuit !)**, Claude (Anthropic), OpenAI, Mistral.
- **Chaque fournisseur garde sa propre clé API** — change de fournisseur, la
  bonne clé se recharge automatiquement.
- Bascule automatique de modèle par fournisseur si un modèle est retiré.
- Où obtenir les clés : aistudio.google.com (Gemini), console.groq.com (Groq),
  console.anthropic.com (Claude), platform.openai.com (OpenAI),
  console.mistral.ai (Mistral).

# v12 — Examen & Voix  # v11 — Vision & Croisements  # v10 — Scan complet
# v9 — IA Enfant  # v8 — Cerveau distant  # v7-v2 — Fondations

## Mettre à jour

```bash
unzip -o /storage/emulated/0/Download/ia-trio-v13.zip -d ~/
cd ~/ia-trio
git add .
git commit -m "v13 tensorflow multi-api"
git push
```
