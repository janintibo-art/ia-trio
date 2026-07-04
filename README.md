# IA Trio 🖼🎵💻

Une app Android avec **3 IA locales entraînables directement sur ton téléphone** :
- **IA Images** : montre-lui des photos, donne un nom, elle apprend à les reconnaître
- **IA Musique/Sons** : enregistre des sons au micro, elle apprend à les distinguer
- **IA Code/Texte** : colle-lui du code, elle apprend ton style et complète

Les 3 partagent un **orchestrateur** : le bouton "Penser ensemble" combine ce que
la vision et l'ouïe ont détecté, et le cerveau code écrit à partir de ça.

Tout est 100% local : aucun serveur, aucun internet nécessaire. Les cerveaux
sont sauvegardés dans la mémoire de l'app et gardent ce qu'ils ont appris.

## Arborescence

```
ia-trio/
├── .github/workflows/build.yml   ← GitHub Actions construit l'APK
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/moi/iatrio/
│           ├── MainActivity.kt   ← l'interface
│           ├── NeuralNet.kt      ← réseau de neurones entraînable
│           └── Brains.kt         ← les 3 IA + l'orchestrateur
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Étapes dans Termux

### 1. Installer les outils (une seule fois)
```bash
pkg update && pkg install git gh unzip
```

### 2. Décompresser le zip
```bash
cd ~/storage/downloads   # (fais `termux-setup-storage` avant si besoin)
unzip ia-trio.zip -d ~/ia-trio
cd ~/ia-trio
```

### 3. Se connecter à GitHub
```bash
gh auth login
```
Choisis : GitHub.com → HTTPS → Login with a web browser, puis suis le lien.

### 4. Créer le dépôt et envoyer les fichiers
```bash
git init
git add .
git commit -m "IA Trio"
gh repo create ia-trio --public --source=. --push
```

### 5. Attendre la construction de l'APK (~3-5 min)
```bash
gh run watch
```

### 6. Télécharger et installer l'APK
```bash
gh run download -n apk
termux-open app-debug.apk
```
Android te demandera d'autoriser l'installation depuis Termux : accepte.

## Comment entraîner tes IA

1. **Images** : choisis une photo de chat, écris "chat", appuie sur Apprendre.
   Recommence avec 3-4 photos de chats, puis pareil avec "chien", etc.
   Plus tu donnes d'exemples, mieux elle devine !
2. **Sons** : enregistre-toi en sifflant, écris "sifflement", Apprendre.
   Recommence plusieurs fois, puis avec d'autres sons.
3. **Code** : colle un de tes fichiers de code, Apprendre. Puis tape un début
   de ligne et appuie sur Compléter.
4. **Penser ensemble** : après avoir fait "Deviner" une image et un son,
   appuie sur ce bouton pour voir les 3 cerveaux collaborer.

## Pour aller plus loin

Ce projet utilise un vrai réseau de neurones (rétropropagation) écrit en Kotlin,
volontairement simple pour que tu puisses le lire et le modifier
(`NeuralNet.kt` fait ~100 lignes). Pistes d'amélioration : ajouter TensorFlow
Lite pour des modèles plus puissants, plus de caractéristiques audio (FFT),
ou des images en couleur.
