package com.whereishumanity.worldgen;

import com.whereishumanity.WhereIsHumanity;
import com.whereishumanity.worldgen.structures.StructureType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Générateur de villes basé sur une grille orthogonale
 * Similaire aux villes américaines ou à celles de Unturned
 */
public class UrbanGridGenerator {
    
    // Paramètres de grille
    private static final int DEFAULT_GRID_BLOCK_SIZE = 16; // Taille par défaut d'un bloc de grille en blocs Minecraft
    
    // Type de biome pour adapter la génération
    private final BiomeType biomeType;
    
    // Taille de la grille (en nombre de cellules)
    private final int gridWidth;
    private final int gridLength;
    
    // Taille d'un bloc de grille
    private final int gridBlockSize;
    
    // Grille de cellules (structures, routes, etc.)
    private final CityCell[][] cityGrid;
    
    // Maillage routier
    private final RoadType[][] horizontalRoads;
    private final RoadType[][] verticalRoads;
    
    // Seed de génération
    private final long seed;
    private final RandomSource random;
    
    /**
     * Constructeur principal
     * @param biomeType Type de biome urbain
     * @param gridWidth Largeur de la grille en cellules
     * @param gridLength Longueur de la grille en cellules
     * @param gridBlockSize Taille d'un bloc de grille en blocs Minecraft
     * @param seed Graine de génération
     */
    public UrbanGridGenerator(BiomeType biomeType, int gridWidth, int gridLength, int gridBlockSize, long seed) {
        this.biomeType = biomeType;
        this.gridWidth = gridWidth;
        this.gridLength = gridLength;
        this.gridBlockSize = gridBlockSize;
        this.seed = seed;
        this.random = RandomSource.create(seed);
        
        // Initialiser les grilles
        this.cityGrid = new CityCell[gridWidth][gridLength];
        this.horizontalRoads = new RoadType[gridWidth][gridLength + 1];
        this.verticalRoads = new RoadType[gridWidth + 1][gridLength];
        
        // Générer le plan de la ville
        generateCityLayout();
    }
    
    /**
     * Constructeur avec la taille de bloc de grille par défaut
     */
    public UrbanGridGenerator(BiomeType biomeType, int gridWidth, int gridLength, long seed) {
        this(biomeType, gridWidth, gridLength, DEFAULT_GRID_BLOCK_SIZE, seed);
    }
    
    /**
     * Génère le plan de base de la ville (routes et emplacements des structures)
     */
    private void generateCityLayout() {
        WhereIsHumanity.LOGGER.info("Génération du plan urbain pour un biome " + biomeType.name() + 
                " de taille " + gridWidth + "x" + gridLength);
        
        // 1. Générer le réseau routier principal
        generateMainRoadNetwork();
        
        // 2. Générer les routes secondaires
        generateSecondaryRoadNetwork();
        
        // 3. Appliquer des dommages aléatoires aux routes (aspect abandonné)
        applyRoadDamage();
        
        // 4. Attribuer des types de bâtiments à chaque cellule
        assignBuildingTypes();
        
        // 5. Placer les bâtiments spéciaux (commissariats, hôpitaux, etc.)
        placeSpecialBuildings();
        
        WhereIsHumanity.LOGGER.info("Plan urbain généré avec succès");
    }
    
    /**
     * Génère le réseau routier principal (boulevards/avenues)
     */
    private void generateMainRoadNetwork() {
        // Fréquence des routes principales (différente selon le type de biome)
        int mainRoadFrequency = switch(biomeType) {
            case METROPOLIS -> 3; // Une avenue tous les 3 blocs
            case CITY -> 4; // Une avenue tous les 4 blocs
            case VILLAGE -> 6; // Une avenue tous les 6 blocs
        };
        
        // Initialiser toutes les routes comme des routes locales
        for (int x = 0; x <= gridWidth; x++) {
            for (int z = 0; z <= gridLength; z++) {
                if (x < gridWidth) {
                    horizontalRoads[x][z] = RoadType.STREET;
                }
                if (z < gridLength) {
                    verticalRoads[x][z] = RoadType.STREET;
                }
            }
        }
        
        // Ajouter les routes principales (boulevards)
        for (int x = 0; x < gridWidth; x++) {
            for (int z = 0; z < gridLength; z++) {
                // Routes horizontales principales
                if (z % mainRoadFrequency == 0 && x < gridWidth) {
                    horizontalRoads[x][z] = RoadType.BOULEVARD;
                }
                
                // Routes verticales principales
                if (x % mainRoadFrequency == 0 && z < gridLength) {
                    verticalRoads[x][z] = RoadType.BOULEVARD;
                }
            }
        }
        
        // Ajouter une route circulaire autour de la ville (si assez grande)
        if (gridWidth >= 12 && gridLength >= 12) {
            addRingRoad();
        }
    }
    
    /**
     * Ajoute une route circulaire (périphérique) autour de la ville
     */
    private void addRingRoad() {
        // Distance depuis le bord
        int borderDistance = 2;
        
        // Route horizontale en haut
        for (int x = borderDistance; x < gridWidth - borderDistance; x++) {
            horizontalRoads[x][borderDistance] = RoadType.HIGHWAY;
        }
        
        // Route horizontale en bas
        for (int x = borderDistance; x < gridWidth - borderDistance; x++) {
            horizontalRoads[x][gridLength - borderDistance] = RoadType.HIGHWAY;
        }
        
        // Route verticale à gauche
        for (int z = borderDistance; z < gridLength - borderDistance; z++) {
            verticalRoads[borderDistance][z] = RoadType.HIGHWAY;
        }
        
        // Route verticale à droite
        for (int z = borderDistance; z < gridLength - borderDistance; z++) {
            verticalRoads[gridWidth - borderDistance][z] = RoadType.HIGHWAY;
        }
    }
    
    /**
     * Génère les routes secondaires
     */
    private void generateSecondaryRoadNetwork() {
        // Le réseau secondaire est déjà initialisé comme des rues locales
        // On peut ajouter des variations ici si nécessaire
    }
    
    /**
     * Applique des dégâts aléatoires aux routes pour l'aspect abandonné
     */
    private void applyRoadDamage() {
        // Probabilités de dégâts selon le type de biome
        float slightDamageChance, heavyDamageChance, destroyedChance;
        
        switch(biomeType) {
            case METROPOLIS:
                slightDamageChance = 0.3f;
                heavyDamageChance = 0.15f;
                destroyedChance = 0.05f;
                break;
            case CITY:
                slightDamageChance = 0.4f;
                heavyDamageChance = 0.2f;
                destroyedChance = 0.1f;
                break;
            case VILLAGE:
                slightDamageChance = 0.5f;
                heavyDamageChance = 0.25f;
                destroyedChance = 0.15f;
                break;
            default:
                slightDamageChance = 0.3f;
                heavyDamageChance = 0.15f;
                destroyedChance = 0.05f;
        }
        
        // Appliquer des dégâts aux routes horizontales
        for (int x = 0; x < gridWidth; x++) {
            for (int z = 0; z <= gridLength; z++) {
                if (random.nextFloat() < destroyedChance) {
                    horizontalRoads[x][z] = RoadType.DESTROYED;
                } else if (random.nextFloat() < heavyDamageChance) {
                    horizontalRoads[x][z] = RoadType.HEAVILY_DAMAGED;
                } else if (random.nextFloat() < slightDamageChance) {
                    horizontalRoads[x][z] = RoadType.SLIGHTLY_DAMAGED;
                }
            }
        }
        
        // Appliquer des dégâts aux routes verticales
        for (int x = 0; x <= gridWidth; x++) {
            for (int z = 0; z < gridLength; z++) {
                if (random.nextFloat() < destroyedChance) {
                    verticalRoads[x][z] = RoadType.DESTROYED;
                } else if (random.nextFloat() < heavyDamageChance) {
                    verticalRoads[x][z] = RoadType.HEAVILY_DAMAGED;
                } else if (random.nextFloat() < slightDamageChance) {
                    verticalRoads[x][z] = RoadType.SLIGHTLY_DAMAGED;
                }
            }
        }
    }
    
    /**
     * Attribue des types de bâtiments à chaque cellule selon sa position dans la ville
     */
    private void assignBuildingTypes() {
        // Identifier le centre de la ville
        int centerX = gridWidth / 2;
        int centerZ = gridLength / 2;
        
        // Pour chaque cellule de la grille
        for (int x = 0; x < gridWidth; x++) {
            for (int z = 0; z < gridLength; z++) {
                // Calculer la distance au centre (Manhattan distance)
                int distanceToCenter = Math.abs(x - centerX) + Math.abs(z - centerZ);
                
                // Selon le type de biome et la distance, attribuer différents types de bâtiments
                CellType cellType = determineCellType(distanceToCenter, x, z);
                
                // Créer la cellule
                cityGrid[x][z] = new CityCell(cellType, getRandomStructureForCellType(cellType));
            }
        }
    }
    
    /**
     * Détermine le type de cellule selon la distance au centre et les paramètres du biome
     */
    private CellType determineCellType(int distanceToCenter, int x, int z) {
        // Paramètres selon le type de biome
        int downtownRadius, commercialRadius, residentialRadius;
        float parkChance, emptyChance;
        
        switch(biomeType) {
            case METROPOLIS:
                downtownRadius = 3;
                commercialRadius = 6;
                residentialRadius = 10;
                parkChance = 0.05f;
                emptyChance = 0.02f;
                break;
            case CITY:
                downtownRadius = 2;
                commercialRadius = 4;
                residentialRadius = 8;
                parkChance = 0.1f;
                emptyChance = 0.05f;
                break;
            case VILLAGE:
                downtownRadius = 1;
                commercialRadius = 2;
                residentialRadius = 5;
                parkChance = 0.15f;
                emptyChance = 0.1f;
                break;
            default:
                downtownRadius = 2;
                commercialRadius = 5;
                residentialRadius = 9;
                parkChance = 0.1f;
                emptyChance = 0.05f;
        }
        
        // Ajouter des variations aléatoires pour éviter des cercles parfaits
        float randomFactor = random.nextFloat() * 0.5f + 0.75f; // Entre 0.75 et 1.25
        float effectiveDistance = distanceToCenter * randomFactor;
        
        // Déterminer le type de cellule en fonction de la distance
        if (effectiveDistance <= downtownRadius) {
            return CellType.DOWNTOWN;
        } else if (effectiveDistance <= commercialRadius) {
            return CellType.COMMERCIAL;
        } else if (effectiveDistance <= residentialRadius) {
            // Chance d'avoir un parc ou un espace vide dans les zones résidentielles
            float roll = random.nextFloat();
            if (roll < parkChance) {
                return CellType.PARK;
            } else if (roll < parkChance + emptyChance) {
                return CellType.EMPTY;
            } else {
                return CellType.RESIDENTIAL;
            }
        } else {
            // Zone rurale en périphérie
            float roll = random.nextFloat();
            if (roll < 0.6f) {
                return CellType.EMPTY;
            } else if (roll < 0.8f) {
                return CellType.RURAL;
            } else {
                return CellType.PARK;
            }
        }
    }
    
    /**
     * Choisit une structure aléatoire adaptée au type de cellule
     */
    private StructureType getRandomStructureForCellType(CellType cellType) {
        Map<StructureType, Integer> weightedStructures = new HashMap<>();
        
        switch(cellType) {
            case DOWNTOWN:
                addWeightedStructure(weightedStructures, StructureType.SKYSCRAPER, 50);
                addWeightedStructure(weightedStructures, StructureType.OFFICE_BUILDING, 30);
                addWeightedStructure(weightedStructures, StructureType.GOVERNMENT_BUILDING, 10);
                addWeightedStructure(weightedStructures, StructureType.APARTMENT_COMPLEX, 5);
                addWeightedStructure(weightedStructures, StructureType.MALL, 5);
                break;
                
            case COMMERCIAL:
                addWeightedStructure(weightedStructures, StructureType.OFFICE_BUILDING, 30);
                addWeightedStructure(weightedStructures, StructureType.MALL, 20);
                addWeightedStructure(weightedStructures, StructureType.APARTMENT_BUILDING, 20);
                addWeightedStructure(weightedStructures, StructureType.WAREHOUSE, 15);
                addWeightedStructure(weightedStructures, StructureType.FACTORY, 10);
                addWeightedStructure(weightedStructures, StructureType.GAS_STATION, 5);
                break;
                
            case RESIDENTIAL:
                addWeightedStructure(weightedStructures, StructureType.APARTMENT_COMPLEX, 25);
                addWeightedStructure(weightedStructures, StructureType.APARTMENT_BUILDING, 25);
                addWeightedStructure(weightedStructures, StructureType.MEDIUM_HOUSE, 20);
                addWeightedStructure(weightedStructures, StructureType.LARGE_HOUSE, 15);
                addWeightedStructure(weightedStructures, StructureType.SMALL_HOUSE, 10);
                addWeightedStructure(weightedStructures, StructureType.SCHOOL, 5);
                break;
                
            case RURAL:
                addWeightedStructure(weightedStructures, StructureType.SMALL_HOUSE, 40);
                addWeightedStructure(weightedStructures, StructureType.MEDIUM_HOUSE, 30);
                addWeightedStructure(weightedStructures, StructureType.LARGE_HOUSE, 15);
                addWeightedStructure(weightedStructures, StructureType.GAS_STATION, 10);
                addWeightedStructure(weightedStructures, StructureType.WAREHOUSE, 5);
                break;
                
            case PARK:
                addWeightedStructure(weightedStructures, StructureType.PARK, 100);
                break;
                
            case EMPTY:
                addWeightedStructure(weightedStructures, StructureType.PARKING_LOT, 50);
                addWeightedStructure(weightedStructures, StructureType.RUINS, 30);
                addWeightedStructure(weightedStructures, StructureType.STREET_PROPS, 20);
                break;
                
            case SPECIAL:
                // Les bâtiments spéciaux sont attribués séparément
                return null;
        }
        
        // Sélectionner une structure aléatoire basée sur les poids
        return selectWeightedRandom(weightedStructures);
    }
    
    /**
     * Ajoute une structure avec un poids à la map
     */
    private void addWeightedStructure(Map<StructureType, Integer> map, StructureType type, int weight) {
        map.put(type, weight);
    }
    
    /**
     * Sélectionne une structure aléatoire basée sur les poids
     */
    private StructureType selectWeightedRandom(Map<StructureType, Integer> weightedStructures) {
        int totalWeight = weightedStructures.values().stream().mapToInt(Integer::intValue).sum();
        int randomValue = random.nextInt(totalWeight);
        
        int cumulativeWeight = 0;
        for (Map.Entry<StructureType, Integer> entry : weightedStructures.entrySet()) {
            cumulativeWeight += entry.getValue();
            if (randomValue < cumulativeWeight) {
                return entry.getKey();
            }
        }
        
        // Ne devrait jamais arriver, mais au cas où
        return weightedStructures.keySet().iterator().next();
    }
    
    /**
     * Place des bâtiments spéciaux dans la ville
     */
    private void placeSpecialBuildings() {
        // Nombre de bâtiments spéciaux selon le type de biome
        int policeStations, fireStations, hospitals, militaryBases;
        
        switch(biomeType) {
            case METROPOLIS:
                policeStations = 3;
                fireStations = 3;
                hospitals = 2;
                militaryBases = 1;
                break;
            case CITY:
                policeStations = 2;
                fireStations = 2;
                hospitals = 1;
                militaryBases = random.nextFloat() < 0.5f ? 1 : 0;
                break;
            case VILLAGE:
                policeStations = 1;
                fireStations = 1;
                hospitals = random.nextFloat() < 0.5f ? 1 : 0;
                militaryBases = 0;
                break;
            default:
                policeStations = 2;
                fireStations = 2;
                hospitals = 1;
                militaryBases = 0;
        }
        
        // Place les commissariats
        placeSpecialBuilding(StructureType.POLICE_STATION, policeStations);
        
        // Place les casernes de pompiers
        placeSpecialBuilding(StructureType.FIRE_STATION, fireStations);
        
        // Place les hôpitaux
        placeSpecialBuilding(StructureType.HOSPITAL, hospitals);
        
        // Place les bases militaires (si nécessaire)
        placeSpecialBuilding(StructureType.MILITARY_BASE, militaryBases);
    }
    
    /**
     * Place un type de bâtiment spécial un certain nombre de fois
     */
    private void placeSpecialBuilding(StructureType structureType, int count) {
        for (int i = 0; i < count; i++) {
            boolean placed = false;
            int attempts = 0;
            
            while (!placed && attempts < 100) {
                attempts++;
                
                // Sélectionner une position aléatoire
                int x = random.nextInt(gridWidth);
                int z = random.nextInt(gridLength);
                
                // Vérifier si la cellule n'est pas déjà spéciale et si elle est appropriée
                if (cityGrid[x][z].cellType != CellType.SPECIAL && isAppropriateForSpecialBuilding(x, z, structureType)) {
                    // Vérifier l'espace pour les structures multi-blocs
                    if (structureType.getGridWidth(gridBlockSize) > 1 || structureType.getGridLength(gridBlockSize) > 1) {
                        if (!hasSpaceForMultiBlockStructure(x, z, structureType)) {
                            continue;
                        }
                    }
                    
                    // Marquer comme spécial et assigner la structure
                    cityGrid[x][z].cellType = CellType.SPECIAL;
                    cityGrid[x][z].structureType = structureType;
                    placed = true;
                    
                    // Pour les structures multi-blocs, réserver l'espace
                    reserveSpaceForMultiBlockStructure(x, z, structureType);
                }
            }
        }
    }
    
    /**
     * Vérifie si une position est appropriée pour un bâtiment spécial
     */
    private boolean isAppropriateForSpecialBuilding(int x, int z, StructureType structureType) {
        // Les bâtiments gouvernementaux vont bien au centre-ville
        if (structureType == StructureType.POLICE_STATION || 
            structureType == StructureType.FIRE_STATION || 
            structureType == StructureType.HOSPITAL) {
            return cityGrid[x][z].cellType == CellType.DOWNTOWN || 
                   cityGrid[x][z].cellType == CellType.COMMERCIAL;
        }
        // Les bases militaires devraient être en périphérie
        else if (structureType == StructureType.MILITARY_BASE) {
            return cityGrid[x][z].cellType == CellType.EMPTY || 
                   cityGrid[x][z].cellType == CellType.RURAL;
        }
        
        return true;
    }
    
    /**
     * Vérifie s'il y a assez d'espace pour une structure multi-blocs
     */
    private boolean hasSpaceForMultiBlockStructure(int startX, int startZ, StructureType structureType) {
        int blocksWidth = structureType.getGridWidth(gridBlockSize);
        int blocksLength = structureType.getGridLength(gridBlockSize);
        
        // Vérifier les limites de la grille
        if (startX + blocksWidth > gridWidth || startZ + blocksLength > gridLength) {
            return false;
        }
        
        // Vérifier si les cellules sont disponibles
        for (int x = startX; x < startX + blocksWidth; x++) {
            for (int z = startZ; z < startZ + blocksLength; z++) {
                if (cityGrid[x][z].cellType == CellType.SPECIAL) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Réserve l'espace pour une structure multi-blocs
     */
    private void reserveSpaceForMultiBlockStructure(int startX, int startZ, StructureType structureType) {
        int blocksWidth = structureType.getGridWidth(gridBlockSize);
        int blocksLength = structureType.getGridLength(gridBlockSize);
        
        // Marquer toutes les cellules comme partie de cette structure
        for (int x = startX; x < startX + blocksWidth; x++) {
            for (int z = startZ; z < startZ + blocksLength; z++) {
                if (x != startX || z != startZ) { // Sauf la cellule principale déjà marquée
                    cityGrid[x][z].cellType = CellType.SPECIAL;
                    cityGrid[x][z].structureType = null; // Pas de structure propre, partie d'une plus grande
                    cityGrid[x][z].isPartOfMultiBlock = true;
                    cityGrid[x][z].multiBlockOrigin = new BlockPos(startX, 0, startZ);
                }
            }
        }
    }
    
    /**
     * Types de cellules dans la grille urbaine
     */
    public enum CellType {
        DOWNTOWN,    // Centre-ville (gratte-ciels, bureaux)
        COMMERCIAL,  // Zone commerciale
        RESIDENTIAL, // Zone résidentielle
        RURAL,       // Zone rurale en périphérie
        PARK,        // Parc ou espace vert
        EMPTY,       // Terrain vague, parking
        SPECIAL      // Bâtiment spécial (commissariat, hôpital, etc.)
    }
    
    /**
     * Types de routes
     */
    public enum RoadType {
        HIGHWAY,           // Autoroute
        BOULEVARD,         // Boulevard principal
        STREET,            // Rue locale
        ALLEY,             // Ruelle
        INTACT,            // État intact
        SLIGHTLY_DAMAGED,  // Légèrement endommagée
        HEAVILY_DAMAGED,   // Très endommagée
        DESTROYED          // Détruite/impraticable
    }
    
    /**
     * Types de biomes urbains
     */
    public enum BiomeType {
        METROPOLIS, // Grande ville avec gratte-ciels
        CITY,       // Ville moyenne
        VILLAGE     // Petit village
    }
    
    /**
     * Cellule de la grille urbaine
     */
    public class CityCell {
        public CellType cellType;
        public StructureType structureType;
        public boolean isPartOfMultiBlock;
        public BlockPos multiBlockOrigin;
        
        public CityCell(CellType cellType, StructureType structureType) {
            this.cellType = cellType;
            this.structureType = structureType;
            this.isPartOfMultiBlock = false;
            this.multiBlockOrigin = null;
        }
    }
}