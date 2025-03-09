package com.whereishumanity.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.whereishumanity.WhereIsHumanity;
import com.whereishumanity.commands.StructureRecordCommand.RecordingSession;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

/**
 * Commande pour sauvegarder une structure enregistrée
 */
public class StructureSaveCommand {

    /**
     * Enregistre la commande dans le dispatcher
     * @param dispatcher Le dispatcher de commandes
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("wih")
                .requires(source -> source.hasPermission(2)) // Niveau op 2 minimum
                .then(Commands.literal("structure")
                    .then(Commands.literal("save")
                        .executes(StructureSaveCommand::saveStructure)
                    )
                )
        );
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
        if (!StructureRecordCommand.hasActiveSession(playerId)) {
            context.getSource().sendFailure(Component.literal("Vous n'avez pas de session d'enregistrement active."));
            return 0;
        }
        
        RecordingSession session = StructureRecordCommand.getActiveSession(playerId);
        
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
            
            // Modifier temporairement les blocs marqueurs pour qu'ils ne soient pas inclus dans la structure
            Map<BlockPos, BlockState> markerBlocks = new HashMap<>();
            for (BlockPos pos : session.originalBlocks.keySet()) {
                // Sauvegarder les marqueurs actuels
                markerBlocks.put(pos, level.getBlockState(pos));
                // Restaurer temporairement les blocs d'origine pour l'enregistrement
                level.setBlock(pos, session.originalBlocks.get(pos), 3);
            }
            
            // Si l'entrée est marquée par un bloc d'or, le remplacer temporairement
            BlockPos entranceWorldPos = session.startPos.offset(session.entrancePos);
            BlockState entranceBlockState = null;
            if (level.getBlockState(entranceWorldPos).is(Blocks.GOLD_BLOCK)) {
                entranceBlockState = level.getBlockState(entranceWorldPos);
                // Récupérer l'état du bloc sous la structure (généralement de l'air)
                level.setBlock(entranceWorldPos, Blocks.AIR.defaultBlockState(), 3);
            }
            
            // Enregistrer la structure avec les marqueurs cachés
            template.fillFromWorld(level, session.startPos, new BlockPos(width, height, length), true, Blocks.AIR);
            
            // Restaurer les marqueurs pour la visualisation
            for (Map.Entry<BlockPos, BlockState> entry : markerBlocks.entrySet()) {
                level.setBlock(entry.getKey(), entry.getValue(), 3);
            }
            
            // Restaurer le marqueur d'entrée
            if (entranceBlockState != null) {
                level.setBlock(entranceWorldPos, entranceBlockState, 3);
            }
            
            // Enregistrer la structure
            Path structurePath = structuresDir.resolve(session.structureName + ".nbt");
            CompoundTag nbt = template.save(new CompoundTag());
            NbtIo.writeCompressed(nbt, structurePath.toFile());
            
            // Enregistrer les métadonnées (entrée, etc.)
            Path metadataPath = structuresDir.resolve(session.structureName + ".json");
            String metadata = String.format(
                    "{\\n" +
                    "  \\\"type\\\": \\\"%s\\\",\\n" +
                    "  \\\"entrance\\\": {\\n" +
                    "    \\\"x\\\": %d,\\n" +
                    "    \\\"y\\\": %d,\\n" +
                    "    \\\"z\\\": %d\\n" +
                    "  },\\n" +
                    "  \\\"dimensions\\\": {\\n" +
                    "    \\\"width\\\": %d,\\n" +
                    "    \\\"height\\\": %d,\\n" +
                    "    \\\"length\\\": %d\\n" +
                    "  }\\n" +
                    "}",
                    session.structureType.name(),
                    session.entrancePos.getX(), session.entrancePos.getY(), session.entrancePos.getZ(),
                    width, height, length
            );
            Files.write(metadataPath, metadata.getBytes());
            
            // Nettoyer la zone
            clearBuildingArea(player, session);
            
            // Supprimer la session
            StructureRecordCommand.removeSession(playerId);
            
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
}
