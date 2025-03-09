package com.whereishumanity.world;

import com.whereishumanity.WhereIsHumanity;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

/**
 * Système de suppression des structures vanilla non désirées
 */
@Mod.EventBusSubscriber(modid = WhereIsHumanity.MOD_ID)
public class StructureRemover {

    // Liste des structures à supprimer
    private static final Set<String> STRUCTURES_TO_REMOVE = new HashSet<>();
    
    static {
        // Strongholds (forteresses)
        STRUCTURES_TO_REMOVE.add("minecraft:stronghold");
        
        // Manoirs
        STRUCTURES_TO_REMOVE.add("minecraft:mansion");
        
        // Autres structures vanilla qu'on pourrait vouloir supprimer
        STRUCTURES_TO_REMOVE.add("minecraft:fortress"); // Forteresse du Nether
        STRUCTURES_TO_REMOVE.add("minecraft:bastion_remnant"); // Vestige de bastion
        STRUCTURES_TO_REMOVE.add("minecraft:end_city"); // Cité de l'End
        STRUCTURES_TO_REMOVE.add("minecraft:monument"); // Monument sous-marin
        STRUCTURES_TO_REMOVE.add("minecraft:temple"); // Temples de jungle/désert
        STRUCTURES_TO_REMOVE.add("minecraft:village"); // Villages (remplacés par nos biomes urbains)
        STRUCTURES_TO_REMOVE.add("minecraft:igloo"); // Igloos
        STRUCTURES_TO_REMOVE.add("minecraft:shipwreck"); // Épaves
        STRUCTURES_TO_REMOVE.add("minecraft:ruined_portal"); // Portails en ruine
    }

    /**
     * Initialisation du système de suppression des structures
     */
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            WhereIsHumanity.LOGGER.info("Initialisation du système de désactivation des structures vanilla");
            WhereIsHumanity.LOGGER.info("Structures désactivées: {}", STRUCTURES_TO_REMOVE);
        });
    }

    /**
     * Vérifie les structures lors du chargement des chunks pour désactiver celles non souhaitées
     * @param event Événement de chargement de chunk
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onChunkLoad(ChunkEvent.Load event) {
        // Cette méthode peut être utilisée pour supprimer des structures si nécessaire
        // Mais la plupart du temps, la suppression se fait via les configs ou l'initialisation
    }
}
