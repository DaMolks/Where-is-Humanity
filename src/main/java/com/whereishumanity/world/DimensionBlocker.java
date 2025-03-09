package com.whereishumanity.world;

import com.whereishumanity.WhereIsHumanity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Bloque l'accès au Nether et à l'End
 */
@Mod.EventBusSubscriber(modid = WhereIsHumanity.MOD_ID)
public class DimensionBlocker {

    /**
     * Empêche les joueurs de voyager vers le Nether ou l'End
     * @param event Événement de voyage entre dimensions
     */
    @SubscribeEvent
    public static void onDimensionTravel(EntityTravelToDimensionEvent event) {
        // Bloquer le voyage vers le Nether
        if (event.getDimension() == Level.NETHER) {
            event.setCanceled(true);
            
            // Notifier le joueur s'il s'agit d'un joueur
            if (event.getEntity() instanceof Player player) {
                player.displayClientMessage(Component.literal("Le Nether n'est plus accessible dans ce monde post-apocalyptique."), true);
            }
            
            WhereIsHumanity.LOGGER.debug("Tentative de voyage vers le Nether bloquée pour {}", event.getEntity().getName().getString());
        }
        
        // Bloquer le voyage vers l'End
        if (event.getDimension() == Level.END) {
            event.setCanceled(true);
            
            // Notifier le joueur s'il s'agit d'un joueur
            if (event.getEntity() instanceof Player player) {
                player.displayClientMessage(Component.literal("L'End n'est plus accessible dans ce monde post-apocalyptique."), true);
            }
            
            WhereIsHumanity.LOGGER.debug("Tentative de voyage vers l'End bloquée pour {}", event.getEntity().getName().getString());
        }
    }
    
    /**
     * Empêche la création de portails du Nether ou de l'End
     * @param event Événement de placement de bloc
     */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getPlacedBlock().getBlock().getDescriptionId().contains("portal")) {
            event.setCanceled(true);
            
            if (event.getEntity() instanceof Player player) {
                player.displayClientMessage(Component.literal("Les portails ne fonctionnent plus dans ce monde post-apocalyptique."), true);
            }
            
            WhereIsHumanity.LOGGER.debug("Tentative de création de portail bloquée");
        }
    }
}
