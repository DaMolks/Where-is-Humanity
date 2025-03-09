# Documentation des commandes du mod "Where is Humanity"

## Organisation logique des commandes

### 1. Commandes liées aux structures individuelles (sous `/wih structure`)

Toutes les commandes liées à la manipulation des structures individuelles devraient être regroupées sous le préfixe `/wih structure`:

- **Enregistrement de structures**:
  - `/wih structure record <type>` - Commence l'enregistrement d'une structure avec les dimensions par défaut
  - `/wih structure record <type> <width> <length>` - Commence l'enregistrement avec dimensions personnalisées
  - `/wih structure setentrance` - Définit la position de l'entrée de la structure
  - `/wih structure save` - Sauvegarde la structure enregistrée
  - `/wih structure cancel` - Annule l'enregistrement en cours

- **Manipulation de structures existantes**:
  - `/wih structure place <type> <nom> [rotation]` - Place une structure existante
  - `/wih structure delete <type> <nom>` - Supprime une structure existante

### 2. Commandes liées à la génération de villages (sous `/wih generate`)

Les commandes pour générer des zones urbaines complètes devraient être regroupées sous `/wih generate`:

- `/wih generate <type>` - Génère une zone urbaine à la position du joueur
- `/wih generate <type> <taille>` - Génère une zone urbaine avec un multiplicateur de taille
- `/wih generate <type> <taille> <rotation>` - Génère une zone urbaine avec taille et rotation

Où `<type>` peut être: village, city, metropolis.

## Structure des fichiers

Pour maintenir cette organisation, le projet devrait avoir:

1. `StructureCommand.java` - Contenant toutes les commandes sous `/wih structure`
2. `GenerateCommand.java` - Contenant toutes les commandes sous `/wih generate`
3. `CommandUtils.java` - Contenant les méthodes utilitaires partagées entre les commandes

## Enregistrement des commandes

Dans la classe principale `WhereIsHumanity.java`, seules les deux classes de commandes principales devraient être enregistrées:

```java
@SubscribeEvent
public void onRegisterCommands(RegisterCommandsEvent event) {
    LOGGER.info("Enregistrement des commandes du mod");
    
    // Enregistrer la commande de structure
    StructureCommand.register(event.getDispatcher());
    
    // Enregistrer la commande de génération
    GenerateCommand.register(event.getDispatcher());
}
```

## Nommage automatique incrémental

Lors de l'enregistrement d'une structure, le mod génère automatiquement un nom incrémental basé sur:
- Le type de structure
- Le nombre de structures existantes du même type

Par exemple: `small_house_1`, `small_house_2`, etc.

## Génération de villages

Le système de génération de villages doit:
1. Créer une grille selon le type de zone urbaine (VILLAGE, CITY, METROPOLIS)
2. Rechercher les structures disponibles dans les dossiers correspondants
3. Placer aléatoirement les structures selon leur type et les distances du centre
4. Générer des routes entre les structures

Chaque type de zone urbaine utilise des types de structures spécifiques:
- VILLAGE: residential, commercial, infrastructure
- CITY: residential, commercial, government, infrastructure
- METROPOLIS: residential, commercial, government, infrastructure, military

## Distribution des structures

Pour les zones urbaines, la distribution des types de bâtiments varie selon la distance au centre:

### METROPOLIS
- Centre-ville (< 30% de distance): 
  - 60% commercial
  - 30% gouvernemental
  - 10% infrastructure
- Zone intermédiaire (30-70% de distance):
  - 40% commercial
  - 30% résidentiel
  - 20% gouvernemental
  - 10% infrastructure
- Périphérie (> 70% de distance):
  - 70% résidentiel
  - 10% commercial
  - 10% militaire
  - 10% infrastructure

### CITY
- Centre-ville (< 40% de distance):
  - 50% commercial
  - 30% gouvernemental
  - 20% infrastructure
- Périphérie (> 40% de distance):
  - 70% résidentiel
  - 20% commercial
  - 10% infrastructure

### VILLAGE
- Distribution générale:
  - 70% résidentiel
  - 20% commercial
  - 10% infrastructure