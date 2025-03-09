package com.whereishumanity.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.whereishumanity.WhereIsHumanity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Commande pour supprimer une structure enregistrée
 */
public class DeleteCommand {

    /**
     * Enregistre la commande dans le dispatcher
     * @param dispatcher Le dispatcher de commandes
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("wih")
                .requires(source -> source.hasPermission(2)) // Niveau op 2 minimum
                .then(Commands.literal("delete")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            try {
                                for (String type : CommandUtils.listStructureDirectories()) {
                                    builder.suggest(type);
                                }
                            } catch (Exception e) {
                                WhereIsHumanity.LOGGER.error("Erreur lors de la suggestion des types de structures", e);
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                try {
                                    String type = StringArgumentType.getString(context, "type");
                                    for (String structure : CommandUtils.listStructureFiles(type)) {
                                        builder.suggest(structure);
                                    }
                                } catch (Exception e) {
                                    WhereIsHumanity.LOGGER.error("Erreur lors de la suggestion des noms de structures", e);
                                }
                                return builder.buildFuture();
                            })
                            .executes(DeleteCommand::deleteStructure)
                        )
                    )
                )
        );
    }

    /**
     * Supprime une structure enregistrée
     * @param context Contexte de la commande
     * @return Code de résultat
     */
    private static int deleteStructure(CommandContext<CommandSourceStack> context) {
        String type = StringArgumentType.getString(context, "type");
        String name = StringArgumentType.getString(context, "name");
        
        Path structurePath = Paths.get("config", WhereIsHumanity.MOD_ID, "structures", type.toLowerCase(), name + ".nbt");
        Path metadataPath = Paths.get("config", WhereIsHumanity.MOD_ID, "structures", type.toLowerCase(), name + ".json");
        
        boolean structureDeleted = false;
        boolean metadataDeleted = false;
        
        try {
            if (Files.exists(structurePath)) {
                Files.delete(structurePath);
                structureDeleted = true;
            }
            
            if (Files.exists(metadataPath)) {
                Files.delete(metadataPath);
                metadataDeleted = true;
            }
            
            if (structureDeleted || metadataDeleted) {
                context.getSource().sendSuccess(() -> Component.literal("Structure '" + name + "' supprimée avec succès."), true);
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("La structure '" + name + "' n'existe pas dans le dossier " + type + "."));
                return 0;
            }
        } catch (IOException e) {
            WhereIsHumanity.LOGGER.error("Erreur lors de la suppression de la structure", e);
            context.getSource().sendFailure(Component.literal("Erreur lors de la suppression de la structure: " + e.getMessage()));
            return 0;
        }
    }
}