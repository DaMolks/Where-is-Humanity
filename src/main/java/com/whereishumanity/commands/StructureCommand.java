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
                                .executes(StructureCommand::startRecordingDefault)
                                .then(Commands.argument("width", IntegerArgumentType.integer(1, 64))
                                    .then(Commands.argument("length", IntegerArgumentType.integer(1, 64))
                                        .executes(StructureCommand::startRecordingCustom)
                                    )
                                )
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
            
            // Obtenir les dimensions au sol (X, Z)
            int width = session.width;
            int length = session.length;
            
            // Détecter la hauteur réelle de la structure
            int height = detectStructureHeight(level, session.startPos, width, length);
            
            // Créer un template de structure
            StructureTemplateManager templateManager = level.getStructureManager();
            ResourceLocation structureId = new ResourceLocation(WhereIsHumanity.MOD_ID, session.structureName);
            StructureTemplate template = templateManager.getOrCreate(structureId);
            
            // Définir la zone à enregistrer avec la hauteur détectée
            BlockPos endPos = session.startPos.offset(width - 1, height - 1, length - 1);
            template.fillFromWorld(level, session.startPos, new BlockPos(width, height, length), true, Blocks.AIR);
            
            // Enregistrer la structure
            Path structurePath = structuresDir.resolve(session.structureName + ".nbt");
            CompoundTag nbt = template.save(new CompoundTag());
            NbtIo.writeCompressed(nbt, structurePath.toFile());
            
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
            context.getSource().sendSuccess(() -> Component.literal("Hauteur détectée automatiquement: " + height + " blocs"), true);
            
            return 1;
        } catch (IOException e) {
            WhereIsHumanity.LOGGER.error("Erreur lors de l'enregistrement de la structure", e);
            context.getSource().sendFailure(Component.literal("Erreur lors de l'enregistrement de la structure: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Détecte la hauteur maximale utilisée par la structure
     * @param level Le niveau du serveur
     * @param startPos Position de départ
     * @param width Largeur
     * @param length Longueur
     * @return Hauteur maximale détectée (min 5 blocs)
     */
    private static int detectStructureHeight(ServerLevel level, BlockPos startPos, int width, int length) {
        int maxHeight = 1; // Au moins un bloc de haut
        int scanHeight = 256; // Hauteur de scan maximale
        
        // Scanner la zone pour trouver le bloc le plus haut
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                for (int y = 0; y < scanHeight; y++) {
                    BlockPos pos = startPos.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).is(Blocks.RED_WOOL)) {
                        maxHeight = Math.max(maxHeight, y + 1);
                    }
                }
            }
        }
        
        // Assurer une hauteur minimale de 5 blocs
        return Math.max(maxHeight, 5);
    }

    /**
     * Affiche la zone de construction avec des blocs de laine rouge dans le sol
     * @param player Joueur
     * @param session Session d'enregistrement
     */
    private static void displayBuildingArea(ServerPlayer player, RecordingSession session) {
        ServerLevel level = player.serverLevel();
        BlockPos startPos = session.startPos;
        int width = session.width;
        int length = session.length;
        
        // Placer de la laine rouge directement dans le sol sur le périmètre
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                if (x == 0 || x == width - 1 || z == 0 || z == length - 1) {
                    // Calculer la position exacte
                    BlockPos blockPos = startPos.offset(x, 0, z);
                    
                    // Sauvegarder le bloc existant pour pouvoir le restaurer plus tard
                    session.originalBlocks.put(blockPos, level.getBlockState(blockPos));
                    
                    // Remplacer par la laine rouge
                    level.setBlock(blockPos, Blocks.RED_WOOL.defaultBlockState(), 3);
                }
            }
        }
    }

    /**
     * Supprime les blocs de marquage de la zone de construction
     * @param player Joueur
     * @param session Session d'enregistrement
     */
    private static void clearBuildingArea(ServerPlayer player, RecordingSession session) {
        ServerLevel level = player.serverLevel();
        
        // Restaurer tous les blocs d'origine qui ont été remplacés par de la laine rouge
        for (Map.Entry<BlockPos, BlockState> entry : session.originalBlocks.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), 3);
        }
        
        // Retirer le bloc marqueur d'entrée si présent
        if (session.entrancePos != null) {
            BlockPos entranceWorldPos = session.startPos.offset(session.entrancePos);
            if (level.getBlockState(entranceWorldPos).is(Blocks.GOLD_BLOCK)) {
                level.setBlock(entranceWorldPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /**
     * Vérifie si une position est dans la zone de construction (vérifie uniquement X et Z)
     * @param pos Position à vérifier
     * @param session Session d'enregistrement
     * @return true si la position est dans la zone au sol
     */
    private static boolean isWithinBuildingAreaXZ(BlockPos pos, RecordingSession session) {
        BlockPos startPos = session.startPos;
        int width = session.width;
        int length = session.length;
        
        return pos.getX() >= startPos.getX() && pos.getX() < startPos.getX() + width &&
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
        public final int width;
        public final int length;
        // Stockage pour les blocs originaux remplacés par la laine rouge
        public final Map<BlockPos, BlockState> originalBlocks = new HashMap<>();
        
        public RecordingSession(StructureType structureType, String structureName, BlockPos startPos, int width, int length) {
            this.structureType = structureType;
            this.structureName = structureName;
            this.startPos = startPos;
            this.width = width;
            this.length = length;
        }
    }
}