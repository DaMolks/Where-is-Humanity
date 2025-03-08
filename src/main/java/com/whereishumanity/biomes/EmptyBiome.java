package com.whereishumanity.biomes;

import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * Biome vide pour permettre la compilation sans accès aux holders
 */
public class EmptyBiome {
    
    /**
     * Crée un biome vide pour la compilation
     * @return Un biome vide avec des valeurs par défaut
     */
    public static Biome createEmptyBiome() {
        // Paramètres de génération minimaux
        BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder();
        
        // Paramètres de spawn minimaux
        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
        spawnBuilder.creatureGenerationProbability(0.0F);
        
        // Effets spéciaux vides
        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .fogColor(12638463)
                .waterColor(4159204)
                .waterFogColor(329011)
                .skyColor(7907327)
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                .build();
        
        // Créer le biome avec des valeurs minimales
        // Utilisation de la méthode de construction compatible avec 1.20.1
        return new Biome.Builder()
                .precipitation(Biome.Precipitation.RAIN)
                .temperature(0.5F)
                .downfall(0.5F)
                .specialEffects(effects)
                .mobSpawnSettings(spawnBuilder.build())
                .generationSettings(biomeBuilder.build())
                .build();
    }
}