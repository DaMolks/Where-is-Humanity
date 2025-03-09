package com.whereishumanity.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.whereishumanity.WhereIsHumanity;
import com.whereishumanity.commands.StructureRecordCommand.RecordingSession;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

import java.util.UUID;

/**
 * Commande pour définir la direction de la façade d'une structure en cours d'enregistrement
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
                        .then(Commands.argument("direction", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                builder.suggest("north");
                                builder.suggest("south");
                                builder.suggest("east");
                                builder.suggest("west");
                                return builder.buildFuture();
                            })
                            .executes(StructureSetEntranceCommand::setEntranceFacing)
                        )
                    )
                )
        );
    }

    /**
     * Définit la direction de la façade de la structure
     * @param context Contexte de la commande
     * @return Code de résultat
     */
    private static int setEntranceFacing(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerId = player.getUUID();
        
        // Vérifier si le joueur a une session active
        if (!StructureRecordCommand.hasActiveSession(playerId)) {
            context.getSource().sendFailure(Component.literal("Vous n'avez pas de session d'enregistrement active."));
            return 0;
        }
        
        RecordingSession session = StructureRecordCommand.getActiveSession(playerId);
        
        // Obtenir la direction depuis l'argument
        String directionArg = StringArgumentType.getString(context, "direction").toLowerCase();
        Direction direction;
        
        // Convertir la chaîne de caractères en direction
        switch (directionArg) {
            case "north":
                direction = Direction.NORTH;
                break;
            case "south":
                direction = Direction.SOUTH;
                break;
            case "east":
                direction = Direction.EAST;
                break;
            case "west":
                direction = Direction.WEST;
                break;
            default:
                context.getSource().sendFailure(Component.literal("Direction invalide. Utilisez north, south, east ou west."));
                return 0;
        }
        
        // Calculer la position de la façade en fonction de la direction
        // Nous allons utiliser le milieu du bord correspondant à la direction
        BlockPos facadePos;
        
        // Largeur et longueur de la structure
        int width = session.width;
        int length = session.length;
        
        // Hauteur approximative pour le marqueur
        int approxHeight = 2;
        
        switch (direction) {
            case NORTH:
                facadePos = new BlockPos(session.startPos.getX() + width / 2, session.startPos.getY() + approxHeight, session.startPos.getZ());
                break;
            case SOUTH:
                facadePos = new BlockPos(session.startPos.getX() + width / 2, session.startPos.getY() + approxHeight, session.startPos.getZ() + length - 1);
                break;
            case EAST:
                facadePos = new BlockPos(session.startPos.getX() + width - 1, session.startPos.getY() + approxHeight, session.startPos.getZ() + length / 2);
                break;
            case WEST:
                facadePos = new BlockPos(session.startPos.getX(), session.startPos.getY() + approxHeight, session.startPos.getZ() + length / 2);
                break;
            default:
                facadePos = session.startPos; // Ne devrait jamais arriver
        }
        
        // Calculer la position relative par rapport au coin de la structure
        BlockPos relativePos = facadePos.subtract(session.startPos);
        
        // Sauvegarder la direction et la position
        session.entrancePos = relativePos;
        session.entranceDirection = direction;
        
        // Marquer la façade avec un bloc d'or
        player.level().setBlock(facadePos, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        
        context.getSource().sendSuccess(() -> Component.literal("Façade définie en direction: " + directionArg + 
                " (position relative: " + relativePos.getX() + ", " + relativePos.getY() + ", " + relativePos.getZ() + ")"), true);
        
        return 1;
    }
}
