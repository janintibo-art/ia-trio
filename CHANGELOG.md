# v17 — LA TOTALE 🎆 (9 nouveautés d'un coup)

## 🤘 Le punk prend vie
1. **Vraie animation de marche** : les 6 frames du cycle « Walking » de TON
   modèle 3D ont été rendues (skinning complet des 26 os) — le punk marche
   pour de vrai, jambes et bras animés, et se tourne vers toi pour penser.
2. **Widget d'écran d'accueil** : appui long sur ton bureau Android → Widgets
   → IA Trio. Le punk s'installe avec une pensée renouvelée (~30 min), tirée
   du cerveau code de ton profil. Toucher le widget ouvre l'app.

## 🧠 La révolution audio
3. **YAMNet** : l'équivalent de MobileNet pour les SONS. L'IA audio reconnaît
   ~520 sons dès l'installation (aboiement, guitare, sirène, pluie, rire...)
   — ligne « Base YAMNet » sous Deviner — et ton entraînement se fait
   par-dessus (transfer learning 521 caractéristiques). Repli FFT automatique.
4. **Rééchantillonnage 16 kHz** : tes MP3/FLAC/OGG/WAV sont convertis au bon
   format quelle que soit leur qualité d'origine.
5. **Étiquettes en français** : ~90 traductions des objets et sons les plus
   courants (tabby → chat tigré, bark → aboiement...).

## 🏗 La solidité
6. **APK signé en release** : clé de signature stable dans le dépôt — les
   mises à jour s'installent PAR-DESSUS sans désinstaller.
   ⚠ PREMIÈRE FOIS : désinstalle l'ancienne app (signature debug différente),
   ensuite plus jamais besoin.
7. **Releases GitHub automatiques** : chaque push crée une vraie Release avec
   l'APK attaché — lien direct partageable sur
   github.com/janintibo-art/ia-trio/releases
8. **Sauvegarde automatique** : le profil actif est copié dans Download/IATrio/
   à chaque fois que tu quittes l'app. Impossible de perdre tes cerveaux.

## 👶 Le social
9. **Partage d'enfants + rappel quotidien** : exporte un enfant
   (Download/IATrio/enfants/), envoie le dossier à un ami qui a l'app, il
   l'importe — ton IA débarque chez lui avec ses gènes et souvenirs !
   Et un interrupteur 🔔 le fait réclamer de l'attention chaque jour vers 18h.

## Mettre à jour
```bash
unzip -o /storage/emulated/0/Download/ia-trio-v17.zip -d ~/
cd ~/ia-trio && git add . && git commit -m "v17 la totale" && git push
```
Nouveau : l'APK est aussi sur la page Releases de ton dépôt !
