package com.whereishumanity.entities;

import com.whereishumanity.WhereIsHumanity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/**
 * Système de filtrage des entités pour ne conserver que les zombies
 */
@Mod.EventBusSubscriber(modid = WhereIsHumanity.MOD_ID)
public class EntityFilterSystem {

    // Liste des types d'entités autorisés (zombies et variantes)
    private static final Set<EntityType<?>> ALLOWED_ENTITIES = new HashSet<>();
    
    static {
        // Ajouter les types de zombies autorisés
        ALLOWED_ENTITIES.add(EntityType.ZOMBIE);
        ALLOWED_ENTITIES.add(EntityType.ZOMBIE_VILLAGER);
        ALLOWED_ENTITIES.add(EntityType.DROWNED);
        ALLOWED_ENTITIES.add(EntityType.HUSK);
        ALLOWED_ENTITIES.add(EntityType.ZOMBIFIED_PIGLIN);
        
        // Ajouter nos zombies personnalisés
        // Note: Ces types seront enregistrés dans EntityRegistry
        // Ils seront automatiquement ajoutés quand EntityRegistry sera initialisé
    }
    
    /**
     * Ajoute un type d'entité à la liste des entités autorisées
     * @param entityType Type d'entité à autoriser
     */
    public static void addAllowedEntity(EntityType<?> entityType) {
        ALLOWED_ENTITIES.add(entityType);
    }
    
    /**
     * Filtre les entités lors de leur apparition dans le monde
     * @param event Événement d'apparition d'entité
     */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        
        // Ne pas filtrer les joueurs et les objets non-vivants (items, etc.)
        if (entity.getType() == EntityType.PLAYER || !(entity instanceof Monster || entity instanceof Animal || entity instanceof Villager || entity instanceof WanderingTrader)) {
            return;
        }
        
        // Si l'entité n'est pas dans la liste des entités autorisées, l'empêcher d'apparaître
        if (!ALLOWED_ENTITIES.contains(entity.getType()) && 
                !(entity instanceof Zombie || entity instanceof ZombieVillager || entity instanceof Drowned || entity instanceof Husk || entity instanceof ZombifiedPiglin)) {
            event.setCanceled(true);
            WhereIsHumanity.LOGGER.debug("Entité filtrée: {}", entity.getType().getDescriptionId());
        }
    }
    
    /**
     * Filtre les mobs avant leur spawn pour éviter les entités non autorisées
     * @param event Événement de spawn de mob
     */
    @SubscribeEvent
    public static void onMobSpawnCheck(MobSpawnEvent.FinalizeSpawn event) {
        Entity entity = event.getEntity();
        
        // Si l'entité n'est pas dans la liste des entités autorisées, empêcher son spawn
        if (!ALLOWED_ENTITIES.contains(entity.getType()) && 
                !(entity instanceof Zombie || entity instanceof ZombieVillager || entity instanceof Drowned || entity instanceof Husk || entity instanceof ZombifiedPiglin)) {
            event.setResult(Event.Result.DENY);
            WhereIsHumanity.LOGGER.debug("Spawn d'entité empêché: {}", entity.getType().getDescriptionId());
        }
    }
}
