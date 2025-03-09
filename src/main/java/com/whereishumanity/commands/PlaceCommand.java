package com.whereishumanity.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.whereishumanity.WhereIsHumanity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * Commande pour placer une structure enregistrée
 */
public class PlaceCommand {

    /**
     * Enregistre la commande dans le dispatcher
     * @param dispatcher Le dispatcher de commandes
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("wih")
                .requires(source -> source.hasPermission(2)) // Niveau op 2 minimum
                .then(Commands.literal("place")
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
                            .executes(PlaceCommand::placeStructure)
                            .then(Commands.argument("rotation", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("0");
                                    builder.suggest("90");
                                    builder.suggest("180");
                                    builder.suggest("270");
                                    return builder.buildFuture();
                                })
                                .executes(PlaceCommand::placeStructureWithRotation)
                            )
                        )
                    )
                )
        );
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
        
        if (!Files.exists(structurePath)) {
            context.getSource().sendFailure(Component.literal("La structure '" + name + "' n'existe pas dans le dossier " + type + "."));
            return 0;
        }
        
        try {
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
            
            // Charger directement le template à partir du fichier NBT
            CompoundTag nbt = NbtIo.readCompressed(structurePath.toFile());
            
            WhereIsHumanity.LOGGER.info("Chargement de la structure: {} (taille NBT: {})", name, nbt.toString().length());
            
            // Approche plus directe pour charger la structure
            StructureTemplateManager templateManager = level.getStructureManager();
            StructureTemplate template = templateManager.readStructure(nbt);
            
            if (template == null) {
                context.getSource().sendFailure(Component.literal("Échec du chargement de la structure. Template null."));
                return 0;
            }
            
            WhereIsHumanity.LOGGER.info("Structure chargée: {} (taille: {}x{}x{})", 
                    name, template.getSize().getX(), template.getSize().getY(), template.getSize().getZ());
            
            // Placer la structure
            BlockPos placementPos = playerPos.below(); // Placer légèrement plus bas pour éviter les problèmes de flottement
            template.placeInWorld(level, placementPos, placementPos, placeSettings, level.random, 2);
            
            // Afficher les logs de débogage
            WhereIsHumanity.LOGGER.info("Structure placée à: {}, {}, {}", 
                    placementPos.getX(), placementPos.getY(), placementPos.getZ());
            
            context.getSource().sendSuccess(() -> Component.literal("Structure '" + name + "' placée avec succès à la position " + 
                    placementPos.getX() + ", " + placementPos.getY() + ", " + placementPos.getZ() + 
                    (rotationDegrees > 0 ? " avec rotation de " + rotationDegrees + "°" : "") + "."), true);
            
            return 1;
        } catch (Exception e) {
            WhereIsHumanity.LOGGER.error("Erreur lors du placement de la structure", e);
            context.getSource().sendFailure(Component.literal("Erreur lors du placement de la structure: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
}