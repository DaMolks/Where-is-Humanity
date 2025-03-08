package com.whereishumanity.biomes;

import com.whereishumanity.entities.EntityRegistry;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Biome de métropole abandonnée
 * Zone urbaine dense avec de grands bâtiments
 */
public class MetropolisBiome {
    
    /**
     * Crée et configure le biome de métropole abandonnée
     * @param placedFeatureGetter Le getter pour les features placées
     * @param carverGetter Le getter pour les carvers configurés
     * @return Le biome configuré
     */
    public static Biome createBiome(HolderGetter<PlacedFeature> placedFeatureGetter, 
                                   HolderGetter<ConfiguredWorldCarver<?>> carverGetter) {
        MobSpawnSettings.Builder spawnBuilder = createMobSpawnSettings();
        BiomeGenerationSettings.Builder biomeBuilder = createBiomeGenerationSettings(placedFeatureGetter, carverGetter);
        
        return createBiome(spawnBuilder, biomeBuilder);
    }
    
    /**
     * Crée les paramètres de spawn pour le biome de métropole abandonnée
     * @return Les paramètres de spawn
     */
    public static MobSpawnSettings.Builder createMobSpawnSettings() {
        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
        
        // Ajouter des zombies intelligents avec une haute fréquence
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityRegistry.SMART_ZOMBIE.get(), 100, 4, 8));
        
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityRegistry.RUNNER_ZOMBIE.get(), 70, 2, 5));
        
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityRegistry.BRUTE_ZOMBIE.get(), 40, 1, 3));
        
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityRegistry.SCREAMER_ZOMBIE.get(), 30, 1, 2));
        
        // Réduire les spawns vanilla
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityType.ZOMBIE, 20, 1, 2));
        
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityType.SKELETON, 20, 1, 2));
        
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityType.CREEPER, 10, 1, 1));
        
        // Désactiver les spawns d'animaux
        spawnBuilder.creatureGenerationProbability(0.0F);
        
        return spawnBuilder;
    }

    /**
     * Crée les paramètres de génération pour le biome de métropole abandonnée
     * @param placedFeatureGetter Le getter pour les features placées
     * @param carverGetter Le getter pour les carvers configurés
     * @return Les paramètres de génération
     */
    public static BiomeGenerationSettings.Builder createBiomeGenerationSettings(
            HolderGetter<PlacedFeature> placedFeatureGetter, 
            HolderGetter<ConfiguredWorldCarver<?>> carverGetter) {
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
        
        // Très peu de végétation dans les zones urbaines denses
        biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, 
                VegetationPlacements.PATCH_GRASS_PLAIN);
        
        return biomeBuilder;
    }

    /**
     * Crée le biome complet avec les paramètres spécifiés
     * @param spawnBuilder Constructeur de paramètres de spawn
     * @param biomeBuilder Constructeur de paramètres de génération
     * @return Le biome configuré
     */
    public static Biome createBiome(MobSpawnSettings.Builder spawnBuilder, BiomeGenerationSettings.Builder biomeBuilder) {
        // Effets spéciaux du biome - aspect post-apocalyptique
        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .fogColor(12638463) // Brume grisâtre
                .waterColor(4159204) // Eau sale
                .waterFogColor(329011) // Brume d'eau dense
                .skyColor(calculateSkyColor(0.5F)) // Ciel grisâtre
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS) // Sons d'ambiance inquiétants
                .backgroundMusic(null) // Pas de musique
                .build();
        
        return new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(0.5F) // Température modérée
                .downfall(0.5F) // Précipitations modérées
                .specialEffects(effects)
                .mobSpawnSettings(spawnBuilder.build())
                .generationSettings(biomeBuilder.build())
                .build();
    }
    
    /**
     * Calcule une couleur de ciel appropriée basée sur la température
     * Formule adaptée pour donner un aspect plus urbain et pollué
     * @param temperature Température du biome
     * @return Couleur du ciel (format RGB)
     */
    private static int calculateSkyColor(float temperature) {
        float adjustedTemp = Mth.clamp(temperature / 3.0F, -1.0F, 1.0F);
        // Tons gris-bleutés pour un ciel urbain
        return Mth.hsvToRgb(0.6F - adjustedTemp * 0.05F, 0.3F, 0.7F);
    }
}