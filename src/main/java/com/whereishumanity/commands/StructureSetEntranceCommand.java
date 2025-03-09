package com.whereishumanity.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.whereishumanity.commands.StructureRecordCommand.RecordingSession;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

import java.util.UUID;

/**
 * Commande pour définir la position de l'entrée d'une structure en cours d'enregistrement
 */
public class StructureSetEntranceCommand {

    /**
     * Enregistre la commande dans le dispatcher
     * @param dispatcher Le dispatcher de commandes
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("wih")
                .requires(source -> source.hasPermission(2)) // Niveau op 2 minimum
                .then(Commands.literal("structure")
                    .then(Commands.literal("setentrance")
                        .executes(StructureSetEntranceCommand::setEntrancePosition)
                    )
                )
        );
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
        if (!StructureRecordCommand.hasActiveSession(playerId)) {
            context.getSource().sendFailure(Component.literal("Vous n'avez pas de session d'enregistrement active."));
            return 0;
        }
        
        RecordingSession session = StructureRecordCommand.getActiveSession(playerId);
        
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
}
