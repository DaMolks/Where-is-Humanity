package com.whereishumanity.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.whereishumanity.WhereIsHumanity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Commandes utilitaires pour les structures (suppression, placement)
 * Cette classe est séparée de StructureCommand pour réduire la taille du code
 */
public class StructureUtilCommands {

    /**
     * Enregistre les commandes utilitaires
     * @return L'argument builder pour les commandes utilitaires
     */
    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        // Nouveau noeud de commande pour delete
        ArgumentBuilder<CommandSourceStack, ?> deleteCommand = Commands.literal("delete")
            .then(Commands.argument("type", StringArgumentType.word())
                .suggests((context, builder) -> {
                    try {
                        List<String> types = listStructureDirectories();
                        for (String type : types) {
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
                            List<String> structures = listStructureFiles(type);
                            for (String structure : structures) {
                                builder.suggest(structure);
                            }
                        } catch (Exception e) {
                            WhereIsHumanity.LOGGER.error("Erreur lors de la suggestion des noms de structures", e);
                        }
                        return builder.buildFuture();
                    })
                    .executes(StructureUtilCommands::deleteStructure)
                )
            );
        
        // Nouveau noeud de commande pour place
        ArgumentBuilder<CommandSourceStack, ?> placeCommand = Commands.literal("place")
            .then(Commands.argument("type", StringArgumentType.word())
                .suggests((context, builder) -> {
                    try {
                        List<String> types = listStructureDirectories();
                        for (String type : types) {
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
                            List<String> structures = listStructureFiles(type);
                            for (String structure : structures) {
                                builder.suggest(structure);
                            }
                        } catch (Exception e) {
                            WhereIsHumanity.LOGGER.error("Erreur lors de la suggestion des noms de structures", e);
                        }
                        return builder.buildFuture();
                    })
                    .executes(StructureUtilCommands::placeStructure)
                    .then(Commands.argument("rotation", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("0");
                            builder.suggest("90");
                            builder.suggest("180");
                            builder.suggest("270");
                            return builder.buildFuture();
                        })
                        .executes(StructureUtilCommands::placeStructureWithRotation)
                    )
                )
            );
        
        // Retourner directement les commandes delete et place sans le noeud "util"
        return Commands.literal("delete").executes(context -> 0)
            .then(deleteCommand)
            .then(placeCommand);
    }

    /**
     * Liste les dossiers de types de structures disponibles
     * @return Liste des noms de dossiers (types)
     * @throws IOException Si une erreur survient lors de la lecture des dossiers
     */
    private static List<String> listStructureDirectories() throws IOException {
        Path structuresDir = Paths.get("config", WhereIsHumanity.MOD_ID, "structures");
        if (!Files.exists(structuresDir)) {
            return Collections.emptyList();
        }

        List<String> directories = new ArrayList<>();
        try (Stream<Path> paths = Files.list(structuresDir)) {
            paths.filter(Files::isDirectory)
                 .map(path -> path.getFileName().toString())
                 .forEach(directories::add);
        }
        return directories;
    }

    /**
     * Liste les fichiers de structures pour un type donné
     * @param type Le type de structure
     * @return Liste des noms de structures (sans extension)
     * @throws IOException Si une erreur survient lors de la lecture des fichiers
     */
    private static List<String> listStructureFiles(String type) throws IOException {
        Path typeDir = Paths.get("config", WhereIsHumanity.MOD_ID, "structures", type.toLowerCase());
        if (!Files.exists(typeDir)) {
            return Collections.emptyList();
        }

        List<String> structures = new ArrayList<>();
        try (Stream<Path> paths = Files.list(typeDir)) {
            paths.filter(path -> path.toString().endsWith(".nbt"))
                 .map(path -> path.getFileName().toString().replace(".nbt", ""))
                 .forEach(structures::add);
        }
        return structures;
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

    /**
     * Place une structure enregistrée à la position du joueur
     * @param context Contexte de la commande
     * @return Code de résultat
     */
    private static int placeStructure(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return placeStructureWithRotationInternal(context, 0);
    }

    /**
     * Place une structure enregistrée à la position du joueur avec une rotation spécifiée
     * @param context Contexte de la commande
     * @return Code de résultat
     */
    private static int placeStructureWithRotation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String rotationStr = StringArgumentType.getString(context, "rotation");
        int rotation;
        
        try {
            rotation = Integer.parseInt(rotationStr);
            // Valider la rotation (0, 90, 180, 270)
            if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
                context.getSource().sendFailure(Component.literal("Rotation invalide. Utilisez 0, 90, 180 ou 270."));
                return 0;
            }
        } catch (NumberFormatException e) {
            context.getSource().sendFailure(Component.literal("Rotation invalide. Utilisez 0, 90, 180 ou 270."));
            return 0;
        }
        
        return placeStructureWithRotationInternal(context, rotation);
    }

    /**
     * Implémentation commune pour placer une structure avec une rotation
     * @param context Contexte de la commande
     * @param rotationDegrees Degrés de rotation (0, 90, 180, 270)
     * @return Code de résultat
     */
    private static int placeStructureWithRotationInternal(CommandContext<CommandSourceStack> context, int rotationDegrees) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        String type = StringArgumentType.getString(context, "type");
        String name = StringArgumentType.getString(context, "name");
        
        // Chemin vers les fichiers de structure
        Path structurePath = Paths.get("config", WhereIsHumanity.MOD_ID, "structures", type.toLowerCase(), name + ".nbt");
        Path metadataPath = Paths.get("config", WhereIsHumanity.MOD_ID, "structures", type.toLowerCase(), name + ".json");
        
        if (!Files.exists(structurePath)) {
            context.getSource().sendFailure(Component.literal("La structure '" + name + "' n'existe pas dans le dossier " + type + "."));
            return 0;
        }
        
        try {
            // Charger la structure depuis le gestionnaire
            StructureTemplateManager templateManager = level.getStructureManager();
            StructureTemplate template = null;
            
            try {
                // Essayer de charger à partir du gestionnaire
                ResourceLocation structureId = new ResourceLocation(WhereIsHumanity.MOD_ID, name);
                template = templateManager.getOrCreate(structureId);
            } catch (Exception e) {
                // Si ça échoue, charger manuellement depuis le fichier
                WhereIsHumanity.LOGGER.info("Chargement manuel de la structure: " + structurePath);
                CompoundTag nbt = NbtIo.readCompressed(structurePath.toFile());
                template = new StructureTemplate();
                // Utiliser le registre de blocs du niveau pour charger la structure
                template.load(level.registryAccess().registryOrThrow(Registries.BLOCK), nbt);
            }
            
            if (template == null) {
                context.getSource().sendFailure(Component.literal("Impossible de charger la structure: " + name));
                return 0;
            }
            
            // Obtenir la position du joueur comme point de départ
            BlockPos playerPos = player.blockPosition();
            
            // Convertir les degrés en rotation Minecraft
            Rotation rotation = Rotation.NONE;
            switch (rotationDegrees) {
                case 90:
                    rotation = Rotation.CLOCKWISE_90;
                    break;
                case 180:
                    rotation = Rotation.CLOCKWISE_180;
                    break;
                case 270:
                    rotation = Rotation.COUNTERCLOCKWISE_90;
                    break;
            }
            
            // Paramètres de placement
            StructurePlaceSettings placeSettings = new StructurePlaceSettings()
                    .setRotation(rotation)
                    .setMirror(Mirror.NONE)
                    .setIgnoreEntities(false);
            
            // Placer la structure
            template.placeInWorld(level, playerPos, playerPos, placeSettings, level.random, 2);
            
            context.getSource().sendSuccess(() -> Component.literal("Structure '" + name + "' placée avec succès à la position " + 
                    playerPos.getX() + ", " + playerPos.getY() + ", " + playerPos.getZ() + 
                    (rotationDegrees > 0 ? " avec rotation de " + rotationDegrees + "°" : "") + "."), true);
            
            return 1;
        } catch (Exception e) {
            WhereIsHumanity.LOGGER.error("Erreur lors du placement de la structure", e);
            context.getSource().sendFailure(Component.literal("Erreur lors du placement de la structure: " + e.getMessage()));
            return 0;
        }
    }
}