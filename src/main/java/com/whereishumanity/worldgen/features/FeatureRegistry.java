package com.whereishumanity.worldgen.features;

import com.whereishumanity.WhereIsHumanity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registre pour les features de génération du monde
 */
public class FeatureRegistry {
    
    // DeferredRegister pour les features
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(
            ForgeRegistries.FEATURES, WhereIsHumanity.MOD_ID);
    
    // Enregistrement de la feature de route en béton noir
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ROAD = FEATURES.register(
            "road", () -> new RoadFeature(NoneFeatureConfiguration.CODEC));
    
    // Clés de ressources pour les features
    public static final ResourceKey<Feature<?>> ROAD_KEY = ResourceKey.create(
            Registries.FEATURE,
            new ResourceLocation(WhereIsHumanity.MOD_ID, "road"));
    
    /**
     * Enregistre les features sur l'event bus du mod
     * @param eventBus L'event bus du mod
     */
    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
        WhereIsHumanity.LOGGER.info("FeatureRegistry: Enregistrement des features de génération");
    }
}
