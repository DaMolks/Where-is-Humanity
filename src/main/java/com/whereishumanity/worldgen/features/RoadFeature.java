package com.whereishumanity.worldgen.features;

import com.mojang.serialization.Codec;
import com.whereishumanity.WhereIsHumanity;
import com.whereishumanity.worldgen.RoadGenerator;
import com.whereishumanity.worldgen.UrbanGridGenerator;
import com.whereishumanity.worldgen.UrbanGridGenerator.RoadType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Feature pour générer des routes en béton noir dans les biomes urbains
 */
public class RoadFeature extends Feature<NoneFeatureConfiguration> {

    public RoadFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos pos = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        
        // Déterminer aléatoirement le type de route
        RoadType roadType = getRandomRoadType(random);
        
        // Déterminer aléatoirement l'état de dégradation
        RoadType roadState = getRandomRoadState(random);
        
        // Taille typique d'une route
        int roadLength = 16; // Longueur standard d'un chunk
        
        // Orientation de la route (0 = horizontale, 1 = verticale)
        boolean isHorizontal = random.nextBoolean();
        
        WhereIsHumanity.LOGGER.debug("Génération d'une route en béton noir à {} ({})", pos, isHorizontal ? "horizontale" : "verticale");
        
        if (isHorizontal) {
            // Générer une route horizontale
            RoadGenerator.generateHorizontalRoad(level, level.getChunk(pos).getPos(), pos.getX(), pos.getZ(), roadType, roadState);
        } else {
            // Générer une route verticale
            RoadGenerator.generateVerticalRoad(level, level.getChunk(pos).getPos(), pos.getX(), pos.getZ(), roadType, roadState);
        }
        
        // Générer une intersection si aléatoire
        if (random.nextFloat() < 0.3f) {
            RoadType crossRoadType = getRandomRoadType(random);
            RoadGenerator.generateIntersection(level, level.getChunk(pos).getPos(), pos.getX(), pos.getZ(), roadType, crossRoadType, roadState);
        }
        
        return true;
    }
    
    /**
     * Obtient un type de route aléatoire avec des probabilités pondérées
     * @param random Source de nombres aléatoires
     * @return Type de route
     */
    private RoadType getRandomRoadType(RandomSource random) {
        float value = random.nextFloat();
        
        if (value < 0.1f) {
            return RoadType.HIGHWAY; // 10% d'autoroutes
        } else if (value < 0.4f) {
            return RoadType.BOULEVARD; // 30% de boulevards
        } else {
            return RoadType.STREET; // 60% de rues ordinaires
        }
    }
    
    /**
     * Obtient un état de dégradation aléatoire avec des probabilités pondérées
     * @param random Source de nombres aléatoires
     * @return État de la route
     */
    private RoadType getRandomRoadState(RandomSource random) {
        float value = random.nextFloat();
        
        if (value < 0.05f) {
            return RoadType.DESTROYED; // 5% de routes détruites
        } else if (value < 0.25f) {
            return RoadType.HEAVILY_DAMAGED; // 20% de routes très endommagées
        } else if (value < 0.55f) {
            return RoadType.SLIGHTLY_DAMAGED; // 30% de routes légèrement endommagées
        } else {
            return RoadType.INTACT; // 45% de routes intactes
        }
    }
}
