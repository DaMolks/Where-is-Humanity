# Structure du projet "Where is Humanity"

Ce document explique l'architecture globale du mod et comment les différents systèmes interagissent.

## Vue d'ensemble

Le mod est divisé en plusieurs systèmes principaux :

1. **Système de biomes urbains** - Gestion des nouveaux biomes de villes abandonnées
2. **Système de génération de structures** - Création procédurale des bâtiments et routes
3. **Système d'IA des zombies** - Comportements améliorés des zombies
4. **Système de détection sonore** - Mécanisme pour attirer les zombies basé sur le bruit
5. **Système de loot** - Distribution des ressources dans les structures

## Architecture des packages

```
com.whereishumanity
├── WhereIsHumanity.java (classe principale)
├── config/
│   ├── ModConfig.java
│   └── BiomeConfig.java
├── biomes/
│   ├── BiomeRegistry.java
│   ├── MetropolisBiome.java
│   ├── CityBiome.java
│   └── VillageBiome.java
├── worldgen/
│   ├── structures/
│   │   ├── StructureRegistry.java
│   │   ├── buildings/
│   │   │   ├── BuildingType.java
│   │   │   ├── SkyscraperStructure.java
│   │   │   ├── HouseStructure.java
│   │   │   ├── PoliceStationStructure.java
│   │   │   └── MilitaryBaseStructure.java
│   │   └── roads/
│   │       ├── RoadGenerator.java
│   │       └── RoadPiece.java
│   └── features/
│       ├── FeatureRegistry.java
│       └── UrbanFeatures.java
├── entities/
│   ├── EntityRegistry.java
│   ├── SmartZombieEntity.java
│   ├── RunnerZombieEntity.java
│   ├── BruteZombieEntity.java
│   ├── ScreamerZombieEntity.java
│   └── ai/
│       ├── goals/
│       │   ├── BreakBlocksGoal.java
│       │   ├── AlertOthersGoal.java
│       │   └── FollowSoundGoal.java
│       └── SmartZombieAI.java
└── sound/
    ├── SoundRegistry.java
    └── SoundDetectionSystem.java
```

## Flux de données et interactions

1. **Génération du monde** :
   - `BiomeRegistry` enregistre les nouveaux biomes
   - `StructureRegistry` définit les structures qui peuvent apparaître
   - Les structures sont placées dans les biomes appropriés
   - `RoadGenerator` crée des connexions entre les zones urbaines

2. **Interactions avec les zombies** :
   - `SoundDetectionSystem` capture les actions du joueur qui génèrent du bruit
   - Les sons sont propagés dans l'environnement
   - `SmartZombieEntity` réagit aux sons via `FollowSoundGoal`
   - Les zombies peuvent alerter d'autres zombies via `AlertOthersGoal`
   - Les zombies peuvent détruire certains blocs avec `BreakBlocksGoal`

## Configuration du mod

Le mod utilise un système de configuration flexible via `ModConfig` permettant aux joueurs de modifier :

- Fréquence des biomes urbains
- Densité des structures
- Seuils de détection sonore
- Comportements des zombies
- Tables de loot

## Extensibilité

Le mod est conçu pour être facilement étendu :

- Nouveaux types de bâtiments peuvent être ajoutés en implémentant l'interface `BuildingType`
- Nouveaux comportements de zombies peuvent être créés en étendant la classe `Goal`
- Le système de loot peut être enrichi en modifiant les tables de loot JSON

## Ajout d'entités zombies

Les zombies intelligents sont au cœur du mod et peuvent être étendus :

1. Créer une classe qui étend `SmartZombieEntity`
2. Définir les attributs spécifiques via `createAttributes()`
3. Personnaliser les objectifs AI dans `registerGoals()`
4. Enregistrer l'entité dans `EntityRegistry`

Exemple :

```java
public class NewZombieType extends SmartZombieEntity {
    public NewZombieType(EntityType<? extends NewZombieType> type, Level level) {
        super(type, level);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return SmartZombieEntity.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.25D); // Plus rapide
    }
    
    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Ajouter des objectifs personnalisés
        this.goalSelector.addGoal(1, new CustomGoal(this));
    }
}
```

## Ajout de biomes urbains

Pour créer un nouveau type de biome urbain :

1. Créer une classe similaire à `MetropolisBiome` ou `CityBiome`
2. Définir les caractéristiques du biome dans `createBiome()`
3. Ajouter le biome au `BiomeRegistry`

## Système de détection sonore

Le système sonore est l'élément central du gameplay. Pour l'étendre :

1. Identifier les événements qui produisent du son
2. Ajouter des gestionnaires d'événements dans `SoundDetectionSystem`
3. Définir le niveau sonore approprié (1, 2 ou 3)

Pour plus de détails, voir [sound-system.md](sound-system.md).

## Cycle de développement

1. **Modifier le code** - Apporter des changements en suivant l'architecture
2. **Tester en développement** - Utiliser la configuration client de Forge
3. **Équilibrer** - Ajuster via `ModConfig` pour l'expérience de jeu optimale
4. **Documenter** - Mettre à jour la documentation pour refléter les changements

## Bonnes pratiques

- Utiliser le logger du mod pour les messages importants
- Maintenir la compatibilité avec les autres mods populaires
- Suivre le style de code existant
- Ajouter des commentaires clairs pour les fonctionnalités complexes
- Documenter les changements dans les fichiers appropriés