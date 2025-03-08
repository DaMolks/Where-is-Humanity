package com.whereishumanity.biomes;

import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Biome vide pour permettre la compilation sans accès aux holders
 */
public class EmptyBiome {
    
    /**
     * Crée un biome vide pour la compilation
     * @return Un biome vide avec des valeurs par défaut
     */
    public static Biome createEmptyBiome() {
        // Paramètres de spawn minimaux
        MobSpawnSettings mobSpawnSettings = new MobSpawnSettings.Builder()
            .creatureGenerationProbability(0.0F)
            .build();
        
        // Paramètres de génération minimaux - sans holders pour le moment
        BiomeGenerationSettings biomeGenSettings = BiomeGenerationSettings.EMPTY;
        
        // Effets spéciaux vides
        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .fogColor(12638463)
                .waterColor(4159204)
                .waterFogColor(329011)
                .skyColor(7907327)
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                .build();
        
        // Créer le biome avec des valeurs minimales en utilisant l'API correcte
        return new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .downfall(0.5f)
                .temperature(0.5f)
                .specialEffects(effects)
                .mobSpawnSettings(mobSpawnSettings)
                .generationSettings(biomeGenSettings)
                .build();
    }
}