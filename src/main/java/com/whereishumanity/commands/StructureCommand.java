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

    /**
     * Démarre l'enregistrement d'une nouvelle structure avec des dimensions personnalisées
     * @param context Contexte de la commande
     * @return Code de résultat
     */
    private static int startRecordingCustom(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String typeArg = StringArgumentType.getString(context, "type");
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
        
        // Générer un nom incrément basé sur le nombre de structures existantes
        String structureName = generateIncrementalName(structureType);
        
        return startRecording(context, structureType, structureName, width, length);
    }

    /**
     * Démarre l'enregistrement d'une nouvelle structure (logique commune)
     * @param context Contexte de la commande
     * @param structureType Type de structure
     * @param structureName Nom de la structure
     * @param width Largeur personnalisée
     * @param length Longueur personnalisée
     * @return Code de résultat
     */
    private static int startRecording(CommandContext<CommandSourceStack> context, StructureType structureType, 
                                      String structureName, int width, int length) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerId = player.getUUID();
        
        // Vérifier si le joueur a déjà une session active
        if (activeSessions.containsKey(playerId)) {
            context.getSource().sendFailure(Component.literal("Vous avez déjà une session d'enregistrement active. Utilisez /wih structure cancel pour l'annuler d'abord."));
            return 0;
        }
        
        // Obtenir la position au sol au lieu des pieds du joueur
        BlockPos playerPos = player.blockPosition();
        BlockPos groundPos = findGroundPosition(player.level(), playerPos);
        
        // Créer une nouvelle session d'enregistrement avec dimensions personnalisées
        RecordingSession session = new RecordingSession(structureType, structureName, groundPos, width, length);
        activeSessions.put(playerId, session);
        
        // Afficher la zone de construction au sol
        displayBuildingArea(player, session);
        
        context.getSource().sendSuccess(() -> Component.literal("Session d'enregistrement démarrée pour une structure de type " + 
                structureType.name() + " nommée '" + structureName + "'."), true);
        context.getSource().sendSuccess(() -> Component.literal("Dimensions au sol: " + 
                width + "x" + length + " (hauteur libre)"), true);
        context.getSource().sendSuccess(() -> Component.literal("Construisez votre structure dans la zone indiquée, puis utilisez /wih structure setentrance pour définir l'entrée."), true);
        context.getSource().sendSuccess(() -> Component.literal("Enfin, utilisez /wih structure save pour enregistrer la structure."), true);
                
        return 1;
    }

    /**
     * Trouve la position du sol sous le joueur
     * @param level Le niveau
     * @param playerPos Position du joueur
     * @return Position du sol
     */
    private static BlockPos findGroundPosition(Level level, BlockPos playerPos) {
        // Descendre jusqu'à trouver un bloc solide
        BlockPos groundPos = playerPos;
        while (groundPos.getY() > 0 && level.getBlockState(groundPos.below()).isAir()) {
            groundPos = groundPos.below();
        }
        // Maintenant, descendre d'un bloc supplémentaire pour être dans le sol plutôt que dessus
        return groundPos.below();
    }

    /**
     * Annule la session d'enregistrement active
     * @param context Contexte de la commande
     * @return Code de résultat
     */
    private static int cancelRecording(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerId = player.getUUID();
        
        // Vérifier si le joueur a une session active
        if (!activeSessions.containsKey(playerId)) {
            context.getSource().sendFailure(Component.literal("Vous n'avez pas de session d'enregistrement active."));
            return 0;
        }
        
        // Supprimer les marqueurs de la zone de construction
        RecordingSession session = activeSessions.get(playerId);
        clearBuildingArea(player, session);
        
        // Supprimer la session
        activeSessions.remove(playerId);
        
        context.getSource().sendSuccess(() -> Component.literal("Session d'enregistrement annulée."), true);
        
        return 1;
    }

    /**
     * Définit la position de l'entrée de la structure
     * @param context Contexte de la commande
     * @return Code de résultat
     */
    private static int setEntrancePosition(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerId = player.getUUID();
        
        // Vérifier si le joueur a une session active
        if (!activeSessions.containsKey(playerId)) {
            context.getSource().sendFailure(Component.literal("Vous n'avez pas de session d'enregistrement active."));
            return 0;
        }
        
        RecordingSession session = activeSessions.get(playerId);
        
        // Utiliser le bloc sur lequel le joueur se trouve
        BlockPos entrancePos = player.blockPosition();
        
        // Vérifier si la position est dans la zone de construction au sol
        if (!isWithinBuildingAreaXZ(entrancePos, session)) {
            context.getSource().sendFailure(Component.literal("La position d'entrée doit être dans la zone de construction (vérification au sol uniquement)."));
            return 0;
        }
        
        // Enregistrer la position relative par rapport au coin de la structure
        BlockPos relativePos = entrancePos.subtract(session.startPos);
        session.entrancePos = relativePos;
        
        // Marquer l'entrée avec un bloc spécial
        player.level().setBlock(entrancePos, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        
        context.getSource().sendSuccess(() -> Component.literal("Position d'entrée définie à " + 
                relativePos.getX() + ", " + relativePos.getY() + ", " + relativePos.getZ() + " (relative à l'origine)."), true);
        
        return 1;
    }