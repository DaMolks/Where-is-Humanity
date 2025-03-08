# Système de détection sonore

Ce document explique comment fonctionne le système de détection sonore, élément central du gameplay du mod "Where is Humanity".

## Principe

Inspiré du Warden de Minecraft, cette mécanique ajoute une couche stratégique au jeu. Les actions du joueur génèrent des sons qui peuvent attirer les zombies. Les joueurs doivent ainsi faire attention à leurs mouvements et actions pour éviter d'attirer des hordes.

## Catégories de sons

Le système classe les sons en trois catégories principales :

### Sons faibles (niveau 1)
- **Sources** : Marche normale, placement de blocs, ouverture douce de portes/coffres
- **Rayon de détection** : 8 blocs (configurable)
- **Réaction des zombies** : Attention momentanée, changement de direction
- **Visualisation** : Ondulation discrète (optionnelle, similaire au Warden)

### Sons moyens (niveau 2)
- **Sources** : Course, saut, casser du bois, combat rapproché
- **Rayon de détection** : 16 blocs (configurable)
- **Réaction des zombies** : Déplacement actif vers la source du son
- **Visualisation** : Ondulation modérée

### Sons forts (niveau 3)
- **Sources** : Explosions, coups de feu, bris de vitre, cris, chute de plus de 3 blocs
- **Rayon de détection** : 32 blocs (configurable)
- **Réaction des zombies** : Alerte complète, appel d'autres zombies
- **Visualisation** : Ondulation intense

## Implémentation technique

### Classe SoundEvent

La classe `SoundEvent` représente un son émis dans le monde :

```java
public class SoundEvent {
    private final BlockPos position;  // Position du son
    private final int soundLevel;     // 1 = faible, 2 = moyen, 3 = fort
    private final Entity source;      // Entité source (joueur, etc.)
    private int age;                  // Âge actuel du son
    private final int maxAge;         // Durée de vie maximale
    
    // Constructeur et méthodes...
}
```

### Propagation du son

Le système utilise ces étapes pour traiter les sons :

1. Un son est émis via `SoundDetectionSystem.emitSound()`
2. Le son est ajouté à la liste des sons actifs pour cette dimension
3. Les zombies dans le rayon de détection sont notifiés immédiatement
4. Le son persiste pendant un certain temps avant de disparaître
5. Les zombies réagissent en fonction du niveau sonore et de leur propre état

### Effets de l'environnement

Le système prend en compte plusieurs facteurs pour la propagation des sons :

- Les blocs solides atténuent la propagation
- Les zombies qui entendent un son peuvent alerter d'autres zombies
- Certains sons peuvent passer à travers des blocs (explosions, coups de feu)

## Méthodes principales

### Émission de son

Pour émettre un son dans le monde :

```java
SoundDetectionSystem.emitSound(level, position, soundLevel, sourceEntity);
```

Par exemple, pour émettre un son fort lors d'une explosion :

```java
SoundDetectionSystem.emitSound(level, explosionPos, 3, null);
```

### Détection par les zombies

Les zombies intelligents utilisent la méthode `onSoundDetected` pour réagir aux sons :

```java
zombie.onSoundDetected(soundPos, soundLevel);
```

Cette méthode est appelée automatiquement par le système lorsqu'un son est détecté à proximité d'un zombie.

## Configuration

Le système est entièrement configurable via `ModConfig` :

- `lowSoundDetectionRadius` : Rayon de détection des sons faibles
- `mediumSoundDetectionRadius` : Rayon de détection des sons moyens
- `loudSoundDetectionRadius` : Rayon de détection des sons forts

## Extension du système

Pour ajouter de nouveaux types de sons :

1. Identifiez l'événement qui génère le son (action du joueur, explosion, etc.)
2. Déterminez le niveau sonore approprié (1, 2 ou 3)
3. Ajoutez un gestionnaire d'événement dans `SoundDetectionSystem` qui appelle `emitSound()`

Exemple pour un nouveau type d'événement :

```java
@SubscribeEvent
public static void onCustomAction(CustomActionEvent event) {
    // Déterminer le niveau sonore selon le contexte
    int soundLevel = determineCustomActionSoundLevel(event);
    
    // Émettre le son
    emitSound(event.getLevel(), event.getPos(), soundLevel, event.getEntity());
}
```

## Conseils de jeu

Les joueurs doivent adapter leur comportement pour survivre :

- Éviter de courir sauf en cas d'absolue nécessité
- Privilégier les mouvements lents et méthodiques dans les zones dangereuses
- Se méfier particulièrement des actions bruyantes comme briser du verre
- Utiliser les sons forts intentionnellement pour créer des diversions
- Profiter des bâtiments pour s'isoler des sons extérieurs
- Favoriser les armes silencieuses plutôt que les armes à feu (qui attirent beaucoup de zombies)

## Interactions avec les zombies spéciaux

Chaque type de zombie interagit différemment avec le système sonore :

- **Smart Zombie** : Réaction standard aux sons
- **Runner Zombie** : Réagit plus rapidement et se déplace plus vite vers la source
- **Brute Zombie** : Moins sensible aux sons faibles mais dangereux une fois alerté
- **Screamer Zombie** : Peut alerter d'autres zombies dans un rayon plus large

## Développement futur

Pistes d'amélioration pour le système de son :

- Ajout de sources sonores environnementales (alarmes, etc.)
- Système de visualisation des ondes sonores (similaire au Warden)
- Objets spéciaux pour créer des diversions sonores
- Objets ou enchantements pour réduire les sons générés par le joueur
- Météo affectant la propagation du son (pluie réduisant la portée, etc.)

## Dépannage

Si le système de son ne fonctionne pas correctement :

1. Vérifiez que les événements sont correctement enregistrés dans `MinecraftForge.EVENT_BUS`
2. Assurez-vous que la méthode `tickSounds` est appelée régulièrement
3. Confirmez que les rayons de détection sont correctement configurés
4. Vérifiez que les zombies intelligents sont bien enregistrés et spawn dans le monde

Pour des problèmes spécifiques, consultez les logs ou créez une issue sur le dépôt GitHub.