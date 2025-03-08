package com.whereishumanity.biomes;

import com.whereishumanity.entities.EntityRegistry;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Biome de village abandonné
 * Zone rurale avec principalement des maisons individuelles
 */
public class VillageBiome {
    
    /**
     * Crée et configure le biome de village abandonné
     * @param placedFeatureGetter Le getter pour les features placées
     * @param carverGetter Le getter pour les carvers configurés
     * @return Le biome configuré
     */
    public static Biome createBiome(HolderGetter<PlacedFeature> placedFeatureGetter, 
                                   HolderGetter<ConfiguredWorldCarver<?>> carverGetter) {
        // Paramètres de génération du biome
        BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder(
            placedFeatureGetter,
            carverGetter
        );
        
        // Caractéristiques de base minimales
        BiomeDefaultFeatures.addDefaultCarversAndLakes(biomeBuilder);
        BiomeDefaultFeatures.addDefaultCrystalFormations(biomeBuilder);
        BiomeDefaultFeatures.addDefaultMonsterRoom(biomeBuilder);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(biomeBuilder);
        BiomeDefaultFeatures.addDefaultSprings(biomeBuilder);
        
        // Beaucoup plus de végétation (la nature reprend ses droits)
        biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, 
                VegetationPlacements.PATCH_GRASS_JUNGLE);
        biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, 
                VegetationPlacements.FLOWER_DEFAULT);
        biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, 
                VegetationPlacements.PATCH_TALL_GRASS_2);
        
        // Quelques arbres clairsemés - utilisons une méthode existante compatible
        BiomeDefaultFeatures.addPlainGrass(biomeBuilder);
        BiomeDefaultFeatures.addPlainVegetation(biomeBuilder);
        
        // Paramètres de spawn des mobs
        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
        
        // Zombies intelligents moins nombreux que dans les zones urbaines
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityRegistry.SMART_ZOMBIE.get(), 70, 2, 5));
        
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityRegistry.RUNNER_ZOMBIE.get(), 40, 1, 3));
        
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityRegistry.BRUTE_ZOMBIE.get(), 20, 1, 1));
        
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityRegistry.SCREAMER_ZOMBIE.get(), 15, 1, 1));
        
        // Spawns vanilla plus nombreux
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityType.ZOMBIE, 50, 2, 4));
        
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityType.SKELETON, 50, 2, 4));
        
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityType.CREEPER, 30, 1, 2));
        
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityType.SPIDER, 30, 1, 2));
        
        // Plus d'animaux sauvages dans les zones rurales
        spawnBuilder.creatureGenerationProbability(0.2F);
        
        spawnBuilder.addSpawn(MobCategory.CREATURE, 
                new MobSpawnSettings.SpawnerData(EntityType.SHEEP, 10, 2, 4));
        
        spawnBuilder.addSpawn(MobCategory.CREATURE, 
                new MobSpawnSettings.SpawnerData(EntityType.PIG, 10, 2, 4));
        
        spawnBuilder.addSpawn(MobCategory.CREATURE, 
                new MobSpawnSettings.SpawnerData(EntityType.CHICKEN, 10, 2, 4));
        
        spawnBuilder.addSpawn(MobCategory.CREATURE, 
                new MobSpawnSettings.SpawnerData(EntityType.COW, 10, 2, 4));
        
        spawnBuilder.addSpawn(MobCategory.CREATURE, 
                new MobSpawnSettings.SpawnerData(EntityType.WOLF, 8, 1, 3));
        
        // Effets spéciaux du biome - aspect rural abandonné
        BiomeSpecialEffects.Builder effectsBuilder = new BiomeSpecialEffects.Builder()
                .fogColor(12638463) // Brume légère
                .waterColor(4159204) // Eau naturelle
                .waterFogColor(329011) // Brume d'eau légère
                .skyColor(calculateSkyColor(0.7F)) // Ciel plus clair
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS) // Sons d'ambiance
                .backgroundMusic(null); // Pas de musique
        
        // Construire et retourner le biome complet
        return new Biome.Builder()
                .temperature(0.7F) // Température plus élevée
                .downfall(0.7F) // Précipitations plus élevées
                .hasPrecipitation(true)
                .specialEffects(effectsBuilder.build())
                .mobSpawnSettings(spawnBuilder.build())
                .generationSettings(biomeBuilder.build())
                .build();
    }
    
    /**
     * Calcule une couleur de ciel appropriée basée sur la température
     * @param temperature Température du biome
     * @return Couleur du ciel (format RGB)
     */
    private static int calculateSkyColor(float temperature) {
        float adjustedTemp = Mth.clamp(temperature / 3.0F, -1.0F, 1.0F);
        // Ciel plus naturel pour les zones rurales, moins de pollution
        return Mth.hsvToRgb(0.62F - adjustedTemp * 0.05F, 0.2F, 0.8F);
    }
}