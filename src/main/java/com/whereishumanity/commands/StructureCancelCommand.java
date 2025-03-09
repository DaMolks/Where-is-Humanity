package com.whereishumanity.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.whereishumanity.commands.StructureRecordCommand.RecordingSession;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.UUID;

/**
 * Commande pour annuler l'enregistrement d'une structure
 */
public class StructureCancelCommand {

    /**
     * Enregistre la commande dans le dispatcher
     * @param dispatcher Le dispatcher de commandes
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("wih")
                .requires(source -> source.hasPermission(2)) // Niveau op 2 minimum
                .then(Commands.literal("structure")
                    .then(Commands.literal("cancel")
                        .executes(StructureCancelCommand::cancelRecording)
                    )
                )
        );
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
        if (!StructureRecordCommand.hasActiveSession(playerId)) {
            context.getSource().sendFailure(Component.literal("Vous n'avez pas de session d'enregistrement active."));
            return 0;
        }
        
        // Supprimer les marqueurs de la zone de construction
        RecordingSession session = StructureRecordCommand.getActiveSession(playerId);
        clearBuildingArea(player, session);
        
        // Supprimer la session
        StructureRecordCommand.removeSession(playerId);
        
        context.getSource().sendSuccess(() -> Component.literal("Session d'enregistrement annulée."), true);
        
        return 1;
    }
    
    /**
     * Supprime les blocs de marquage de la zone de construction
     * @param player Joueur
     * @param session Session d'enregistrement
     */
    private static void clearBuildingArea(ServerPlayer player, RecordingSession session) {
        ServerLevel level = player.serverLevel();
        
        // Restaurer tous les blocs d'origine qui ont été remplacés par de la laine rouge
        for (Map.Entry<BlockPos, net.minecraft.world.level.block.state.BlockState> entry : session.originalBlocks.entrySet()) {
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
