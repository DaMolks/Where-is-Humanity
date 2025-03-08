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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Commande pour enregistrer et gérer les structures personnalisées
 */
public class StructureCommand {

    // Stockage des sessions d'enregistrement actives
    private static final Map<UUID, RecordingSession> activeSessions = new HashMap<>();

    // Méthode register() et autres méthodes précédentes omises pour plus de clarté

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
            // Charger la structure
            StructureTemplateManager templateManager = level.getStructureManager();
            StructureTemplate template = templateManager.getOrCreate(new ResourceLocation(WhereIsHumanity.MOD_ID, name));
            
            if (template == null) {
                // Charger manuellement depuis le fichier
                CompoundTag nbt = NbtIo.readCompressed(structurePath.toFile());
                template = new StructureTemplate();
                template.load(nbt);
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