# Weather App 🌤️

Application météo Android développée pour afficher les informations météorologiques en temps réel.

## Fonctionnalités

- Recherche de ville
- Affichage de la météo actuelle
- Carte interactive avec OpenStreetMap
- Sélection de ville via la carte
- Informations détaillées : température, humidité, vent

## Configuration du projet

### Prérequis

- Android Studio
- SDK Android 24 ou supérieur
- Clé API OpenWeatherMap

### Installation

1. **Cloner le projet**
   ```bash
   git clone <url-du-repository>
   cd "Weather App"
   ```

2. **Configurer la clé API**
   
   Copiez le fichier d'exemple :
   ```bash
   cp local.properties.example local.properties
   ```
   
   Éditez `local.properties` et ajoutez votre clé API OpenWeatherMap :
   ```properties
   OPENWEATHER_API_KEY=votre_clé_api_ici
   ```
   
   Pour obtenir une clé API gratuite :
   - Visitez [OpenWeatherMap](https://openweathermap.org/api)
   - Créez un compte
   - Générez une clé API

3. **Synchroniser le projet**
   - Ouvrez le projet dans Android Studio
   - Laissez Gradle synchroniser les dépendances

4. **Compiler et lancer**
   - Connectez un appareil Android ou lancez un émulateur
   - Cliquez sur Run ▶️

## Sécurité

⚠️ **Important** : Le fichier `local.properties` contient votre clé API et ne doit **JAMAIS** être commité sur Git. Il est déjà inclus dans `.gitignore`.

## Technologies utilisées

- Java
- Android SDK
- OpenWeatherMap API
- OSMDroid (cartes)
- Volley (requêtes HTTP)

## Structure du projet

```
app/
├── src/main/
│   ├── java/essths/li3/weatherapp/
│   │   ├── MainActivity.java      # Activité principale
│   │   └── WelcomeActivity.java   # Écran de bienvenue
│   ├── res/                        # Ressources (layouts, drawables, etc.)
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## Auteur

Développé par l'équipe ESSTHS LI3

## Licence

Ce projet est développé à des fins éducatives.

