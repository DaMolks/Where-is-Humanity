package com.whereishumanity.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.whereishumanity.WhereIsHumanity;
import net.minecraft.commands.CommandSourceStack;

/**
 * Registre central pour toutes les commandes liées aux structures
 * Cette classe coordonne l'enregistrement de toutes les sous-commandes du système de structure
 */
public class StructureCommandRegistry {

    /**
     * Enregistre toutes les commandes de structure dans le dispatcher
     * @param dispatcher Le dispatcher de commandes
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        WhereIsHumanity.LOGGER.info("Enregistrement des commandes de structure");
        
        // Enregistrer les commandes individuelles
        StructureRecordCommand.register(dispatcher);
        StructureSetEntranceCommand.register(dispatcher);
        StructureCancelCommand.register(dispatcher);
        StructureSaveCommand.register(dispatcher);
        StructurePlaceCommand.register(dispatcher);
        StructureDeleteCommand.register(dispatcher);
        
        WhereIsHumanity.LOGGER.info("Commandes de structure enregistrées avec succès");
    }
}
