# Widget Cuizinote

Widget Android d'écran d'accueil qui affiche les calories mangées aujourd'hui sur l'objectif quotidien,
lues depuis [Cuizinote](https://cuizinote.vercel.app).

- Anneau de progression aux couleurs de Cuizinote (corail si l'objectif est dépassé)
- Mise à jour toutes les 30 minutes, et à l'ouverture de l'application
- Toucher le widget ouvre Cuizinote
- Accès en lecture seule, par une clé personnelle créée dans Cuizinote (Paramètres, Widget Android)

## Installation

1. Ouvrir la dernière version dans l'onglet **Releases** de ce dépôt, sur le téléphone.
2. Télécharger `widget-cuizinote.apk` et l'ouvrir. Autoriser l'installation depuis le navigateur si Android le demande.
3. Dans Cuizinote : Paramètres, Widget Android, « Créer la clé du widget », copier la clé.
4. Ouvrir l'application « Widget Cuizinote », coller la clé, « Enregistrer ».
5. Écran d'accueil : appui long, Widgets, Widget Cuizinote, glisser le widget.

## Compilation

Chaque envoi sur `main` compile l'APK avec GitHub Actions (`.github/workflows/apk.yml`) et publie une version.
Aucun outil Android n'est nécessaire sur l'ordinateur.

Pile : Kotlin, Android 8.0+ (API 26), WorkManager.
