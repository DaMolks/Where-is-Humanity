package com.whereishumanity.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.whereishumanity.WhereIsHumanity;
import com.whereishumanity.worldgen.structures.StructureType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Commande pour enregistrer et gérer les structures personnalisées
 */
public class StructureCommand {

    // Stockage des sessions d'enregistrement actives
    private static final Map<UUID, RecordingSession> activeSessions = new HashMap<>();

    /**
     * Enregistre les commandes dans le dispatcher
     * @param dispatcher Le dispatcher de commandes
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("wih")
                .requires(source -> source.hasPermission(2)) // Niveau op 2 minimum
                .then(Commands.literal("structure")
                    // Commande d'enregistrement
                    .then(Commands.literal("record")
                        .then(Commands.argument("type", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                for (StructureType type : StructureType.values()) {
                                    builder.suggest(type.name().toLowerCase());
                                }
                                return builder.buildFuture();
                            })
                            .then(Commands.argument("name", StringArgumentType.word())
                                .executes(StructureCommand::startRecordingDefault)
                                .then(Commands.argument("width", IntegerArgumentType.integer(1, 64))
                                    .then(Commands.argument("length", IntegerArgumentType.integer(1, 64))
                                        .executes(StructureCommand::startRecordingCustom)
                                    )
                                )
                            )
                        )
                    )
                    // Commande d'annulation
                    .then(Commands.literal("cancel")
                        .executes(StructureCommand::cancelRecording)
                    )
                    // Commande de sauvegarde
                    .then(Commands.literal("save")
                        .executes(StructureCommand::saveStructure)
                    )
                    // Commande de définition d'entrée
                    .then(Commands.literal("setentrance")
                        .executes(StructureCommand::setEntrancePosition)
                    )
                    // Nouvelle commande de suppression
                    .then(Commands.literal("delete")
                        .then(Commands.argument("type", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                try {
                                    // Suggérer les types de structures pour lesquels des structures existent
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
                                        // Suggérer les noms de structures disponibles pour le type spécifié
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
                                .executes(StructureCommand::deleteStructure)
                            )
                        )
                    )
                    // Nouvelle commande de placement
                    .then(Commands.literal("place")
                        .then(Commands.argument("type", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                try {
                                    // Suggérer les types de structures pour lesquels des structures existent
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
                                        // Suggérer les noms de structures disponibles pour le type spécifié
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
                                .executes(StructureCommand::placeStructure)
                                .then(Commands.argument("rotation", StringArgumentType.word())
                                    .suggests((context, builder) -> {
                                        builder.suggest("0");
                                        builder.suggest("90");
                                        builder.suggest("180");
                                        builder.suggest("270");
                                        return builder.buildFuture();
                                    })
                                    .executes(StructureCommand::placeStructureWithRotation)
                                )
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
     * Démarre l'enregistrement d'une nouvelle structure avec les dimensions par défaut
     * @param context Contexte de la commande
     * @return Code de résultat
     */
    private static int startRecordingDefault(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String typeArg = StringArgumentType.getString(context, "type");
        String structureName = StringArgumentType.getString(context, "name");
        
        // Valider le type de structure
        StructureType structureType;
        try {
            structureType = StructureType.valueOf(typeArg.toUpperCase());
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal("Type de structure invalide: " + typeArg));
            return 0;
        }
        
        // Utiliser les dimensions par défaut
        return startRecording(context, structureType, structureName, structureType.getWidth(), structureType.getLength());
    }

    /**
     * Démarre l'enregistrement d'une nouvelle structure avec des dimensions personnalisées
     * @param context Contexte de la commande
     * @return Code de résultat
     */
    private static int startRecordingCustom(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String typeArg = StringArgumentType.getString(context, "type");
        String structureName = StringArgumentType.getString(context, "name");
        int width = IntegerArgumentType.getInteger(context, "width");
        int length = IntegerArgumentType.getInteger(context, "length");
        
        // Valider le type de structure
        StructureType structureType;
        try {
            structureType = StructureType.valueOf(typeArg.toUpperCase());
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal("Type de structure invalide: " + typeArg));
            return 0;
        }
        
        return startRecording(context, structureType, structureName, width, length);
    }

    // Les méthodes continueront dans la deuxième partie
