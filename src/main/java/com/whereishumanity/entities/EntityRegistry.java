package com.whereishumanity.entities;

import com.whereishumanity.WhereIsHumanity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registre des entités du mod "Where is Humanity"
 * Cette classe gère l'enregistrement de toutes les entités personnalisées
 */
public class EntityRegistry {
    // Registre différé pour toutes les entités du mod
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(
            ForgeRegistries.ENTITY_TYPES, WhereIsHumanity.MOD_ID);
    
    // Enregistrement du zombie intelligent de base
    public static final RegistryObject<EntityType<SmartZombieEntity>> SMART_ZOMBIE = ENTITIES.register(
            "smart_zombie",
            () -> EntityType.Builder.<SmartZombieEntity>of(SmartZombieEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build(new ResourceLocation(WhereIsHumanity.MOD_ID, "smart_zombie").toString())
    );
    
    // Enregistrement du zombie coureur (plus rapide)
    public static final RegistryObject<EntityType<RunnerZombieEntity>> RUNNER_ZOMBIE = ENTITIES.register(
            "runner_zombie",
            () -> EntityType.Builder.<RunnerZombieEntity>of(RunnerZombieEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build(new ResourceLocation(WhereIsHumanity.MOD_ID, "runner_zombie").toString())
    );
    
    // Enregistrement du zombie brute (plus fort, peut casser des blocs solides)
    public static final RegistryObject<EntityType<BruteZombieEntity>> BRUTE_ZOMBIE = ENTITIES.register(
            "brute_zombie",
            () -> EntityType.Builder.<BruteZombieEntity>of(BruteZombieEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.1F) // Légèrement plus grand
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build(new ResourceLocation(WhereIsHumanity.MOD_ID, "brute_zombie").toString())
    );
    
    // Enregistrement du zombie hurleur (qui alerte d'autres zombies)
    public static final RegistryObject<EntityType<ScreamerZombieEntity>> SCREAMER_ZOMBIE = ENTITIES.register(
            "screamer_zombie",
            () -> EntityType.Builder.<ScreamerZombieEntity>of(ScreamerZombieEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10) // Portée de tracking plus grande pour les hurleurs
                    .updateInterval(3)
                    .build(new ResourceLocation(WhereIsHumanity.MOD_ID, "screamer_zombie").toString())
    );
    
    /**
     * Enregistre toutes les entités sur l'event bus du mod
     * @param eventBus L'event bus du mod
     */
    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
        WhereIsHumanity.LOGGER.info("EntityRegistry: Enregistrement des entités");
    }
}