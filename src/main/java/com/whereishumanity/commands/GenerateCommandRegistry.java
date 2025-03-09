package com.whereishumanity.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.whereishumanity.WhereIsHumanity;
import net.minecraft.commands.CommandSourceStack;

/**
 * Registre pour les commandes de génération de villes/villages
 */
public class GenerateCommandRegistry {

    /**
     * Enregistre toutes les commandes de génération dans le dispatcher
     * @param dispatcher Le dispatcher de commandes
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        WhereIsHumanity.LOGGER.info("Enregistrement des commandes de génération");
        
        // Cette méthode peut enregistrer plusieurs commandes liées à la génération
        // Pour l'instant, elle n'inclut que la commande principale de génération
        
        WhereIsHumanity.LOGGER.info("Commandes de génération enregistrées avec succès");
    }
}
