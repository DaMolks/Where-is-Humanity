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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Commande pour enregistrer une nouvelle structure
 */
public class StructureRecordCommand {

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
                            .executes(StructureRecordCommand::startRecordingDefault)
                            .then(Commands.argument("width", IntegerArgumentType.integer(1, 64))
                                .then(Commands.argument("length", IntegerArgumentType.integer(1, 64))
                                    .executes(StructureRecordCommand::startRecordingCustom)
                                )
                            )
                        )
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
        context.getSource().sendSuccess(() -> Component.literal("Construisez votre structure dans la zone indiquée, puis utilisez /wih structure setentrance <direction> pour définir la façade."), true);
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
     * Classe interne pour stocker les informations de session d'enregistrement
     */
    public static class RecordingSession {
        public final StructureType structureType;
        public final String structureName;
        public final BlockPos startPos;
        public BlockPos entrancePos;
        public Direction entranceDirection;
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
            this.entranceDirection = Direction.NORTH; // Direction par défaut
        }
    }
    
    /**
     * Obtient la session active d'un joueur
     * @param playerId UUID du joueur
     * @return La session active ou null si aucune session n'existe
     */
    public static RecordingSession getActiveSession(UUID playerId) {
        return activeSessions.get(playerId);
    }
    
    /**
     * Supprime une session active
     * @param playerId UUID du joueur
     */
    public static void removeSession(UUID playerId) {
        activeSessions.remove(playerId);
    }
    
    /**
     * Vérifie si le joueur a une session active
     * @param playerId UUID du joueur
     * @return true si le joueur a une session active
     */
    public static boolean hasActiveSession(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }
}
