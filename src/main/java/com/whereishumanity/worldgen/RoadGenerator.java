package com.whereishumanity.worldgen;

import com.whereishumanity.WhereIsHumanity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;

/**
 * Générateur de routes en béton noir pour les environnements urbains
 */
public class RoadGenerator {

    // Blocs utilisés pour les routes
    private static final Block ROAD_BLOCK = Blocks.BLACK_CONCRETE;
    private static final Block DAMAGED_ROAD_BLOCK = Blocks.BLACK_CONCRETE_POWDER;
    private static final Block ROAD_SLAB = Blocks.BLACK_CONCRETE;  // Fallback pour une dalle
    private static final Block CURB_BLOCK = Blocks.SMOOTH_STONE_SLAB;
    
    // Largeurs typiques pour les différents types de routes (en blocs)
    private static final int HIGHWAY_WIDTH = 6;     // Autoroute
    private static final int BOULEVARD_WIDTH = 4;   // Boulevard
    private static final int STREET_WIDTH = 2;      // Rue résidentielle
    
    /**
     * Génère une route horizontale
     * @param level Le niveau où générer la route
     * @param chunkPos Position du chunk
     * @param startX Coordonnée X de départ
     * @param z Coordonnée Z
     * @param roadType Type de route
     * @param roadState État de dégradation
     */
    public static void generateHorizontalRoad(WorldGenLevel level, ChunkPos chunkPos, int startX, int z, 
                                             UrbanGridGenerator.RoadType roadType, 
                                             UrbanGridGenerator.RoadType roadState) {
        // Si la route est détruite, ne rien générer
        if (roadState == UrbanGridGenerator.RoadType.DESTROYED) {
            return;
        }
        
        // Obtenir la largeur selon le type de route
        int width = getRoadWidth(roadType);
        
        // Déterminer la position Y (hauteur)
        int y = getTerrainHeight(level, startX, z);
        
        // Générer la route (béton noir simple)
        for (int x = startX; x < startX + width; x++) {
            BlockPos roadPos = new BlockPos(x, y, z);
            
            // Si la route est endommagée, ajouter des variations
            if (roadState == UrbanGridGenerator.RoadType.HEAVILY_DAMAGED) {
                // Routes très endommagées: alternance de béton normal et de poudre de béton
                boolean useDamagedBlock = new Random().nextFloat() < 0.7f;
                level.setBlock(roadPos, useDamagedBlock ? 
                        DAMAGED_ROAD_BLOCK.defaultBlockState() : 
                        ROAD_BLOCK.defaultBlockState(), 2);
                
                // Ajouter des trous aléatoires
                if (new Random().nextFloat() < 0.3f) {
                    // Ne rien placer (laisser un trou)
                    continue;
                }
            } 
            else if (roadState == UrbanGridGenerator.RoadType.SLIGHTLY_DAMAGED) {
                // Routes légèrement endommagées: principalement du béton avec quelques blocs de poudre
                boolean useDamagedBlock = new Random().nextFloat() < 0.3f;
                level.setBlock(roadPos, useDamagedBlock ? 
                        DAMAGED_ROAD_BLOCK.defaultBlockState() : 
                        ROAD_BLOCK.defaultBlockState(), 2);
            }
            else {
                // Routes intactes: béton noir ordinaire
                level.setBlock(roadPos, ROAD_BLOCK.defaultBlockState(), 2);
            }
            
            // Ajouter un bloc de fondation en dessous de la route
            level.setBlock(roadPos.below(), Blocks.GRAVEL.defaultBlockState(), 2);
        }
        
        // Ajouter des bordures de trottoir pour les boulevards et autoroutes
        if (roadType == UrbanGridGenerator.RoadType.BOULEVARD || roadType == UrbanGridGenerator.RoadType.HIGHWAY) {
            BlockState curbState = CURB_BLOCK.defaultBlockState();
            if (CURB_BLOCK instanceof SlabBlock) {
                // Si c'est une dalle, s'assurer qu'elle est en position supérieure
                curbState = curbState.setValue(SlabBlock.TYPE, SlabType.TOP);
            }
            
            // Bordure gauche
            level.setBlock(new BlockPos(startX - 1, y, z), curbState, 2);
            
            // Bordure droite
            level.setBlock(new BlockPos(startX + width, y, z), curbState, 2);
        }
    }
    
    /**
     * Génère une route verticale
     * @param level Le niveau où générer la route
     * @param chunkPos Position du chunk
     * @param x Coordonnée X
     * @param startZ Coordonnée Z de départ
     * @param roadType Type de route
     * @param roadState État de dégradation
     */
    public static void generateVerticalRoad(WorldGenLevel level, ChunkPos chunkPos, int x, int startZ, 
                                          UrbanGridGenerator.RoadType roadType, 
                                          UrbanGridGenerator.RoadType roadState) {
        // Si la route est détruite, ne rien générer
        if (roadState == UrbanGridGenerator.RoadType.DESTROYED) {
            return;
        }
        
        // Obtenir la largeur selon le type de route
        int width = getRoadWidth(roadType);
        
        // Déterminer la position Y (hauteur)
        int y = getTerrainHeight(level, x, startZ);
        
        // Générer la route (béton noir simple)
        for (int z = startZ; z < startZ + width; z++) {
            BlockPos roadPos = new BlockPos(x, y, z);
            
            // Si la route est endommagée, ajouter des variations
            if (roadState == UrbanGridGenerator.RoadType.HEAVILY_DAMAGED) {
                // Routes très endommagées: alternance de béton normal et de poudre de béton
                boolean useDamagedBlock = new Random().nextFloat() < 0.7f;
                level.setBlock(roadPos, useDamagedBlock ? 
                        DAMAGED_ROAD_BLOCK.defaultBlockState() : 
                        ROAD_BLOCK.defaultBlockState(), 2);
                
                // Ajouter des trous aléatoires
                if (new Random().nextFloat() < 0.3f) {
                    // Ne rien placer (laisser un trou)
                    continue;
                }
            } 
            else if (roadState == UrbanGridGenerator.RoadType.SLIGHTLY_DAMAGED) {
                // Routes légèrement endommagées: principalement du béton avec quelques blocs de poudre
                boolean useDamagedBlock = new Random().nextFloat() < 0.3f;
                level.setBlock(roadPos, useDamagedBlock ? 
                        DAMAGED_ROAD_BLOCK.defaultBlockState() : 
                        ROAD_BLOCK.defaultBlockState(), 2);
            }
            else {
                // Routes intactes: béton noir ordinaire
                level.setBlock(roadPos, ROAD_BLOCK.defaultBlockState(), 2);
            }
            
            // Ajouter un bloc de fondation en dessous de la route
            level.setBlock(roadPos.below(), Blocks.GRAVEL.defaultBlockState(), 2);
        }
        
        // Ajouter des bordures de trottoir pour les boulevards et autoroutes
        if (roadType == UrbanGridGenerator.RoadType.BOULEVARD || roadType == UrbanGridGenerator.RoadType.HIGHWAY) {
            BlockState curbState = CURB_BLOCK.defaultBlockState();
            if (CURB_BLOCK instanceof SlabBlock) {
                // Si c'est une dalle, s'assurer qu'elle est en position supérieure
                curbState = curbState.setValue(SlabBlock.TYPE, SlabType.TOP);
            }
            
            // Bordure supérieure
            level.setBlock(new BlockPos(x, y, startZ - 1), curbState, 2);
            
            // Bordure inférieure
            level.setBlock(new BlockPos(x, y, startZ + width), curbState, 2);
        }
    }
    
    /**
     * Génère une intersection de routes
     * @param level Le niveau où générer l'intersection
     * @param chunkPos Position du chunk
     * @param x Coordonnée X
     * @param z Coordonnée Z
     * @param horizontalType Type de route horizontale
     * @param verticalType Type de route verticale
     * @param state État de dégradation
     */
    public static void generateIntersection(WorldGenLevel level, ChunkPos chunkPos, int x, int z,
                                          UrbanGridGenerator.RoadType horizontalType,
                                          UrbanGridGenerator.RoadType verticalType,
                                          UrbanGridGenerator.RoadType state) {
        // Si l'intersection est détruite, ne rien générer
        if (state == UrbanGridGenerator.RoadType.DESTROYED) {
            return;
        }
        
        // Obtenir les largeurs
        int horizontalWidth = getRoadWidth(horizontalType);
        int verticalWidth = getRoadWidth(verticalType);
        
        // Déterminer la position Y (hauteur)
        int y = getTerrainHeight(level, x, z);
        
        // Générer l'intersection (rectangle de béton noir)
        for (int dx = 0; dx < horizontalWidth; dx++) {
            for (int dz = 0; dz < verticalWidth; dz++) {
                BlockPos intersectionPos = new BlockPos(x + dx, y, z + dz);
                
                // Si l'intersection est endommagée, ajouter des variations
                if (state == UrbanGridGenerator.RoadType.HEAVILY_DAMAGED) {
                    // Intersections très endommagées: alternance de béton normal et de poudre de béton
                    boolean useDamagedBlock = new Random().nextFloat() < 0.7f;
                    level.setBlock(intersectionPos, useDamagedBlock ? 
                            DAMAGED_ROAD_BLOCK.defaultBlockState() : 
                            ROAD_BLOCK.defaultBlockState(), 2);
                    
                    // Ajouter des trous aléatoires
                    if (new Random().nextFloat() < 0.3f) {
                        // Ne rien placer (laisser un trou)
                        continue;
                    }
                } 
                else if (state == UrbanGridGenerator.RoadType.SLIGHTLY_DAMAGED) {
                    // Intersections légèrement endommagées: principalement du béton avec quelques blocs de poudre
                    boolean useDamagedBlock = new Random().nextFloat() < 0.3f;
                    level.setBlock(intersectionPos, useDamagedBlock ? 
                            DAMAGED_ROAD_BLOCK.defaultBlockState() : 
                            ROAD_BLOCK.defaultBlockState(), 2);
                }
                else {
                    // Intersections intactes: béton noir ordinaire
                    level.setBlock(intersectionPos, ROAD_BLOCK.defaultBlockState(), 2);
                }
                
                // Ajouter un bloc de fondation en dessous de l'intersection
                level.setBlock(intersectionPos.below(), Blocks.GRAVEL.defaultBlockState(), 2);
            }
        }
    }
    
    /**
     * Obtient la largeur d'une route selon son type
     * @param roadType Type de route
     * @return Largeur en blocs
     */
    private static int getRoadWidth(UrbanGridGenerator.RoadType roadType) {
        return switch(roadType) {
            case HIGHWAY -> HIGHWAY_WIDTH;
            case BOULEVARD -> BOULEVARD_WIDTH;
            case STREET, ALLEY -> STREET_WIDTH;
            default -> STREET_WIDTH;
        };
    }
    
    /**
     * Détermine la hauteur du terrain à une position donnée
     * @param level Le niveau
     * @param x Coordonnée X
     * @param z Coordonnée Z
     * @return Hauteur Y
     */
    private static int getTerrainHeight(WorldGenLevel level, int x, int z) {
        // On pourrait utiliser la heightmap, mais pour simplifier, on utilise une valeur fixe
        // Dans une implémentation complète, on adapterait la route au terrain
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
    }
}
