package com.whereishumanity.biomes;

import com.whereishumanity.WhereIsHumanity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registre de biomes pour le mod "Where is Humanity"
 */
public class BiomeRegistry {
    // Registre différé pour les biomes
    public static final DeferredRegister<Biome> BIOMES = DeferredRegister.create(
            ForgeRegistries.BIOMES, WhereIsHumanity.MOD_ID);
    
    // Clés de ressources pour chaque biome (utilisées pour la génération du monde)
    public static final ResourceKey<Biome> ABANDONED_METROPOLIS_KEY = ResourceKey.create(
            Registries.BIOME, 
            new ResourceLocation(WhereIsHumanity.MOD_ID, "abandoned_metropolis"));
    
    public static final ResourceKey<Biome> ABANDONED_CITY_KEY = ResourceKey.create(
            Registries.BIOME, 
            new ResourceLocation(WhereIsHumanity.MOD_ID, "abandoned_city"));
    
    public static final ResourceKey<Biome> ABANDONED_VILLAGE_KEY = ResourceKey.create(
            Registries.BIOME, 
            new ResourceLocation(WhereIsHumanity.MOD_ID, "abandoned_village"));
    
    // Pour l'instant, enregistrons des biomes temporaires (nous les remplacerons par la suite)
    // Cela permet au moins de compiler le code
    public static final RegistryObject<Biome> ABANDONED_METROPOLIS = BIOMES.register(
            "abandoned_metropolis", 
            () -> EmptyBiome.createEmptyBiome());
    
    public static final RegistryObject<Biome> ABANDONED_CITY = BIOMES.register(
            "abandoned_city", 
            () -> EmptyBiome.createEmptyBiome());
    
    public static final RegistryObject<Biome> ABANDONED_VILLAGE = BIOMES.register(
            "abandoned_village", 
            () -> EmptyBiome.createEmptyBiome());

    /**
     * Enregistre les biomes sur l'event bus du mod
     * @param eventBus L'event bus du mod
     */
    public static void register(IEventBus eventBus) {
        BIOMES.register(eventBus);
        WhereIsHumanity.LOGGER.info("BiomeRegistry: Enregistrement des biomes");
    }
}