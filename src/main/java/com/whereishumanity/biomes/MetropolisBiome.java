package com.whereishumanity.biomes;

import com.whereishumanity.WhereIsHumanity;
import com.whereishumanity.entities.EntityRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;

/**
 * Biome de métropole abandonnée
 * Zone urbaine dense avec de grands bâtiments
 */
public class MetropolisBiome {
    
    /**
     * Crée et configure le biome de métropole abandonnée
     * @return Le biome configuré
     */
    public static Biome createBiome() {
        // Paramètres de génération du biome
        BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder();
        
        // Caractéristiques de base minimales
        BiomeDefaultFeatures.addDefaultCarversAndLakes(biomeBuilder);
        BiomeDefaultFeatures.addDefaultCrystalFormations(biomeBuilder);
        BiomeDefaultFeatures.addDefaultMonsterRoom(biomeBuilder);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(biomeBuilder);
        BiomeDefaultFeatures.addDefaultSprings(biomeBuilder);
        
        // Très peu de végétation dans les métropoles
        biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, 
                VegetationPlacements.PATCH_GRASS_PLAIN); // Herbes urbaines clairsemées
        
        // Paramètres de spawn des mobs
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
        
        // Effets spéciaux du biome - aspect post-apocalyptique
        BiomeSpecialEffects.Builder effectsBuilder = new BiomeSpecialEffects.Builder()
                .fogColor(12638463) // Brume grisâtre
                .waterColor(4159204) // Eau sale
                .waterFogColor(329011) // Brume d'eau dense
                .skyColor(calculateSkyColor(0.5F)) // Ciel grisâtre
                .ambientMoodSound(new BiomeSpecialEffects.AmbientMoodSettings(
                        SoundEvents.AMBIENT_CAVE, 6000, 8, 2.0D)) // Sons d'ambiance inquiétants
                .backgroundMusic(null); // Pas de musique
        
        // Construire et retourner le biome complet
        return new Biome.Builder()
                .temperature(0.5F) // Température modérée
                .downfall(0.5F) // Précipitations modérées
                .hasPrecipitation(true)
                .specialEffects(effectsBuilder.build())
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