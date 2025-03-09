package com.whereishumanity.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.whereishumanity.WhereIsHumanity;
import com.whereishumanity.worldgen.structures.StructureType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Commande pour enregistrer et gérer les structures personnalisées
 * Cette classe gère l'enregistrement et la sauvegarde des structures
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
                            .executes(StructureCommand::startRecordingDefault)
                            .then(Commands.argument("width", IntegerArgumentType.integer(1, 64))
                                .then(Commands.argument("length", IntegerArgumentType.integer(1, 64))
                                    .executes(StructureCommand::startRecordingCustom)
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
                )
        );
    }

    /**
     * Démarre l'enregistrement d'une nouvelle structure avec les dimensions par défaut
     * @param context Contexte de la commande
     * @return Code de résultat
     */
    private static int startRecordingDefault(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String typeArg = StringArgumentType.getString(context, "type");
        
        // Valider le type de structure
        StructureType structureType;
        try {
            structureType = StructureType.valueOf(typeArg.toUpperCase());
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal("Type de structure invalide: " + typeArg));
            return 0;
        }
        
        // Générer un nom incrément basé sur le nombre de structures existantes
        String structureName = generateIncrementalName(structureType);
        
        // Utiliser les dimensions par défaut
        return startRecording(context, structureType, structureName, structureType.getWidth(), structureType.getLength());
    }

    /**
     * Génère un nom incrémental basé sur le nombre de structures existantes du même type
     * @param structureType Type de structure
     * @return Nom incrémental (ex: "residential_house_1")
     */
    private static String generateIncrementalName(StructureType structureType) {
        String baseTypeName = structureType.name().toLowerCase();
        String category = structureType.getCategory().toLowerCase();
        int count = 1;
        
        // Compter les structures existantes pour déterminer le prochain numéro
        Path structuresDir = Paths.get("config", WhereIsHumanity.MOD_ID, "structures", category);
        if (Files.exists(structuresDir)) {
            try (Stream<Path> paths = Files.list(structuresDir)) {
                // Compter combien de fichiers commencent par le même type
                String prefix = baseTypeName + "_";
                count = (int) paths
                    .filter(path -> path.toString().endsWith(".nbt"))
                    .map(path -> path.getFileName().toString())
                    .filter(filename -> filename.startsWith(prefix))
                    .count() + 1;
            } catch (IOException e) {
                WhereIsHumanity.LOGGER.error("Erreur lors du comptage des structures existantes", e);
            }
        }
        
        return baseTypeName + "_" + count;
    }