package com.whereishumanity.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.whereishumanity.WhereIsHumanity;
import com.whereishumanity.worldgen.structures.StructureType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
                    .then(Commands.literal("record")
                        .then(Commands.argument("type", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                for (StructureType type : StructureType.values()) {
                                    builder.suggest(type.name().toLowerCase());
                                }
                                return builder.buildFuture();
                            })
                            .then(Commands.argument("name", StringArgumentType.word())
                                .executes(StructureCommand::startRecording)
                            )
                        )
                    )
                    .then(Commands.literal("cancel")
                        .executes(StructureCommand::cancelRecording)
                    )
                    .then(Commands.literal("save")
                        .executes(StructureCommand::saveStructure)
                    )
                    .then(Commands.literal("setentrance")
                        .executes(StructureCommand::setEntrancePosition)
                    )
                )
        );
    }

    /**
     * Démarre l'enregistrement d'une nouvelle structure
     * @param context Contexte de la commande
     * @return Code de résultat
     */
    private static int startRecording(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerId = player.getUUID();
        
        // Récupérer les arguments
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
        
        // Vérifier si le joueur a déjà une session active
        if (activeSessions.containsKey(playerId)) {
            context.getSource().sendFailure(Component.literal("Vous avez déjà une session d'enregistrement active. Utilisez /wih structure cancel pour l'annuler d'abord."));
            return 0;
        }
        
        // Obtenir la position actuelle du joueur comme point de départ
        BlockPos startPos = player.blockPosition();
        
        // Créer une nouvelle session d'enregistrement
        RecordingSession session = new RecordingSession(structureType, structureName, startPos);
        activeSessions.put(playerId, session);
        
        // Afficher la zone de construction
        displayBuildingArea(player, session);
        
        context.getSource().sendSuccess(() -> Component.literal("Session d'enregistrement démarrée pour une structure de type " + 
                structureType.name() + " nommée '" + structureName + "'."), true);
        context.getSource().sendSuccess(() -> Component.literal("Dimensions: " + 
                structureType.getWidth() + "x" + structureType.getHeight() + "x" + structureType.getLength()), true);
        context.getSource().sendSuccess(() -> Component.literal("Construisez votre structure dans la zone indiquée, puis utilisez /wih structure setentrance pour définir l'entrée."), true);
        context.getSource().sendSuccess(() -> Component.literal("Enfin, utilisez /wih structure save pour enregistrer la structure."), true);
                
        return 1;
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
        
        // Vérifier si la position est dans la zone de construction
        if (!isWithinBuildingArea(entrancePos, session)) {
            context.getSource().sendFailure(Component.literal("La position d'entrée doit être dans la zone de construction."));
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

    /**
     * Enregistre la structure dans un fichier
     * @param context Contexte de la commande
     * @return Code de résultat
     */
    private static int saveStructure(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerId = player.getUUID();
        ServerLevel level = player.serverLevel();
        
        // Vérifier si le joueur a une session active
        if (!activeSessions.containsKey(playerId)) {
            context.getSource().sendFailure(Component.literal("Vous n'avez pas de session d'enregistrement active."));
            return 0;
        }
        
        RecordingSession session = activeSessions.get(playerId);
        
        // Vérifier si l'entrée a été définie
        if (session.entrancePos == null) {
            context.getSource().sendFailure(Component.literal("Vous devez d'abord définir la position d'entrée avec /wih structure setentrance"));
            return 0;
        }
        
        try {
            // Créer le dossier de structures s'il n'existe pas
            Path structuresDir = Paths.get("config", WhereIsHumanity.MOD_ID, "structures", 
                    session.structureType.getCategory().toLowerCase());
            Files.createDirectories(structuresDir);
            
            // Obtenir les dimensions
            int width = session.structureType.getWidth();
            int height = session.structureType.getHeight();
            int length = session.structureType.getLength();
            
            // Créer un template de structure
            StructureTemplateManager templateManager = level.getStructureManager();
            StructureTemplate template = templateManager.getOrCreate(session.structureName);
            
            // Définir la zone à enregistrer
            BlockPos endPos = session.startPos.offset(width - 1, height - 1, length - 1);
            template.fillFromWorld(level, session.startPos, new BlockPos(width, height, length), true, Blocks.AIR);
            
            // Enregistrer la structure
            Path structurePath = structuresDir.resolve(session.structureName + ".nbt");
            template.save(structurePath.toFile());
            
            // Enregistrer les métadonnées (entrée, etc.)
            Path metadataPath = structuresDir.resolve(session.structureName + ".json");
            String metadata = String.format(
                    "{\n" +
                    "  \"type\": \"%s\",\n" +
                    "  \"entrance\": {\n" +
                    "    \"x\": %d,\n" +
                    "    \"y\": %d,\n" +
                    "    \"z\": %d\n" +
                    "  },\n" +
                    "  \"dimensions\": {\n" +
                    "    \"width\": %d,\n" +
                    "    \"height\": %d,\n" +
                    "    \"length\": %d\n" +
                    "  }\n" +
                    "}",
                    session.structureType.name(),
                    session.entrancePos.getX(), session.entrancePos.getY(), session.entrancePos.getZ(),
                    width, height, length
            );
            Files.write(metadataPath, metadata.getBytes());
            
            // Nettoyer la zone
            clearBuildingArea(player, session);
            
            // Supprimer la session
            activeSessions.remove(playerId);
            
            context.getSource().sendSuccess(() -> Component.literal("Structure '" + session.structureName + 
                    "' enregistrée avec succès dans " + structurePath.toString()), true);
            
            return 1;
        } catch (IOException e) {
            WhereIsHumanity.LOGGER.error("Erreur lors de l'enregistrement de la structure", e);
            context.getSource().sendFailure(Component.literal("Erreur lors de l'enregistrement de la structure: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Affiche la zone de construction avec des blocs de bordure
     * @param player Joueur
     * @param session Session d'enregistrement
     */
    private static void displayBuildingArea(ServerPlayer player, RecordingSession session) {
        ServerLevel level = player.serverLevel();
        BlockPos startPos = session.startPos;
        int width = session.structureType.getWidth();
        int height = session.structureType.getHeight();
        int length = session.structureType.getLength();
        
        // Afficher les coins et arêtes
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                // Coins bas
                if ((x == 0 || x == width - 1) && (z == 0 || z == length - 1)) {
                    level.setBlock(startPos.offset(x, 0, z), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
                }
                // Arêtes bas
                else if (x == 0 || x == width - 1 || z == 0 || z == length - 1) {
                    level.setBlock(startPos.offset(x, 0, z), Blocks.IRON_BLOCK.defaultBlockState(), 3);
                }
                
                // Coins haut
                if ((x == 0 || x == width - 1) && (z == 0 || z == length - 1)) {
                    level.setBlock(startPos.offset(x, height - 1, z), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
                }
                // Arêtes haut
                else if (x == 0 || x == width - 1 || z == 0 || z == length - 1) {
                    level.setBlock(startPos.offset(x, height - 1, z), Blocks.IRON_BLOCK.defaultBlockState(), 3);
                }
            }
        }
        
        // Afficher les piliers verticaux aux coins
        for (int y = 1; y < height - 1; y++) {
            level.setBlock(startPos.offset(0, y, 0), Blocks.IRON_BLOCK.defaultBlockState(), 3);
            level.setBlock(startPos.offset(width - 1, y, 0), Blocks.IRON_BLOCK.defaultBlockState(), 3);
            level.setBlock(startPos.offset(0, y, length - 1), Blocks.IRON_BLOCK.defaultBlockState(), 3);
            level.setBlock(startPos.offset(width - 1, y, length - 1), Blocks.IRON_BLOCK.defaultBlockState(), 3);
        }
    }

    /**
     * Supprime les blocs de marquage de la zone de construction
     * @param player Joueur
     * @param session Session d'enregistrement
     */
    private static void clearBuildingArea(ServerPlayer player, RecordingSession session) {
        ServerLevel level = player.serverLevel();
        BlockPos startPos = session.startPos;
        int width = session.structureType.getWidth();
        int height = session.structureType.getHeight();
        int length = session.structureType.getLength();
        
        // Retirer tous les blocs de bordure
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                if (x == 0 || x == width - 1 || z == 0 || z == length - 1) {
                    BlockPos pos = startPos.offset(x, 0, z);
                    if (level.getBlockState(pos).is(Blocks.IRON_BLOCK) || level.getBlockState(pos).is(Blocks.DIAMOND_BLOCK)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                    
                    pos = startPos.offset(x, height - 1, z);
                    if (level.getBlockState(pos).is(Blocks.IRON_BLOCK) || level.getBlockState(pos).is(Blocks.DIAMOND_BLOCK)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        
        // Retirer les piliers verticaux
        for (int y = 1; y < height - 1; y++) {
            BlockPos pos = startPos.offset(0, y, 0);
            if (level.getBlockState(pos).is(Blocks.IRON_BLOCK)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            
            pos = startPos.offset(width - 1, y, 0);
            if (level.getBlockState(pos).is(Blocks.IRON_BLOCK)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            
            pos = startPos.offset(0, y, length - 1);
            if (level.getBlockState(pos).is(Blocks.IRON_BLOCK)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            
            pos = startPos.offset(width - 1, y, length - 1);
            if (level.getBlockState(pos).is(Blocks.IRON_BLOCK)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        
        // Retirer le bloc marqueur d'entrée si présent
        if (session.entrancePos != null) {
            BlockPos entranceWorldPos = startPos.offset(session.entrancePos);
            if (level.getBlockState(entranceWorldPos).is(Blocks.GOLD_BLOCK)) {
                level.setBlock(entranceWorldPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /**
     * Vérifie si une position est dans la zone de construction
     * @param pos Position à vérifier
     * @param session Session d'enregistrement
     * @return true si la position est dans la zone
     */
    private static boolean isWithinBuildingArea(BlockPos pos, RecordingSession session) {
        BlockPos startPos = session.startPos;
        int width = session.structureType.getWidth();
        int height = session.structureType.getHeight();
        int length = session.structureType.getLength();
        
        return pos.getX() >= startPos.getX() && pos.getX() < startPos.getX() + width &&
               pos.getY() >= startPos.getY() && pos.getY() < startPos.getY() + height &&
               pos.getZ() >= startPos.getZ() && pos.getZ() < startPos.getZ() + length;
    }

    /**
     * Classe interne pour stocker les informations de session d'enregistrement
     */
    private static class RecordingSession {
        public final StructureType structureType;
        public final String structureName;
        public final BlockPos startPos;
        public BlockPos entrancePos;
        
        public RecordingSession(StructureType structureType, String structureName, BlockPos startPos) {
            this.structureType = structureType;
            this.structureName = structureName;
            this.startPos = startPos;
        }
    }
}