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
 * Biome de ville abandonnée avec banlieue
 * Zone urbaine mixte avec quelques grands bâtiments et des zones résidentielles
 */
public class CityBiome {
    
    /**
     * Crée et configure le biome de ville abandonnée
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
        
        // Plus de végétation que dans les métropoles (jardins, parcs)
        biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, 
                VegetationPlacements.PATCH_GRASS_PLAIN);
        biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, 
                VegetationPlacements.FLOWER_DEFAULT); // Quelques fleurs sauvages dans les jardins abandonnés
        
        // Paramètres de spawn des mobs
        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
        
        // Ajouter des zombies intelligents avec une fréquence moyenne
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityRegistry.SMART_ZOMBIE.get(), 90, 3, 6));
        
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityRegistry.RUNNER_ZOMBIE.get(), 60, 2, 4));
        
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityRegistry.BRUTE_ZOMBIE.get(), 30, 1, 2));
        
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityRegistry.SCREAMER_ZOMBIE.get(), 25, 1, 1));
        
        // Spawns vanilla
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityType.ZOMBIE, 30, 2, 3));
        
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityType.SKELETON, 30, 1, 3));
        
        spawnBuilder.addSpawn(MobCategory.MONSTER, 
                new MobSpawnSettings.SpawnerData(EntityType.CREEPER, 20, 1, 2));
        
        // Quelques animaux sauvages peuvent encore traîner dans les banlieues
        spawnBuilder.creatureGenerationProbability(0.1F);
        spawnBuilder.addSpawn(MobCategory.CREATURE, 
                new MobSpawnSettings.SpawnerData(EntityType.CAT, 5, 1, 2));
        
        spawnBuilder.addSpawn(MobCategory.CREATURE, 
                new MobSpawnSettings.SpawnerData(EntityType.WOLF, 5, 1, 2));
        
        // Effets spéciaux du biome - aspect urbain mais moins dense
        BiomeSpecialEffects.Builder effectsBuilder = new BiomeSpecialEffects.Builder()
                .fogColor(12638463) // Brume légère
                .waterColor(4159204) // Eau sale
                .waterFogColor(329011) // Brume d'eau
                .skyColor(calculateSkyColor(0.6F)) // Ciel un peu plus clair que la métropole
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS) // Sons d'ambiance
                .backgroundMusic(null); // Pas de musique
        
        // Construire et retourner le biome complet
        return new Biome.Builder()
                .temperature(0.6F) // Température légèrement plus élevée
                .downfall(0.6F) // Précipitations légèrement plus élevées
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
        // Tons gris-bleutés pour un ciel urbain, légèrement plus clair que la métropole
        return Mth.hsvToRgb(0.6F - adjustedTemp * 0.05F, 0.25F, 0.75F);
    }
}