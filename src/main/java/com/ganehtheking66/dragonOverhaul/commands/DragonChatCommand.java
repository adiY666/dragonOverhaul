package com.ganehtheking66.dragonOverhaul.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Registers custom chat commands for the dragon.
 *
 * @author GanehtHEkinG66
 */
public class DragonChatCommand {

    /**
     * Registers the command with the server dispatcher.
     *
     * @param dispatcher the server command dispatcher
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dragon")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("goto")
                        .then(CommandManager.argument("targetPositon", Vec3ArgumentType.vec3())
                                .then(CommandManager.argument("targetYaw", FloatArgumentType.floatArg())
                                        .executes(context -> {
                                            Vec3d pos = Vec3ArgumentType.getPosArgument(context, "targetPositon").toAbsolutePos(context.getSource());
                                            float yaw = FloatArgumentType.getFloat(context, "targetYaw");

                                            ServerWorld world = context.getSource().getWorld();
                                            BlockPos blockPos = BlockPos.ofFloored(pos);

                                            world.setBlockState(blockPos, Blocks.BEDROCK.getDefaultState());

                                            GoToCommand.overrideTarget(pos, yaw);
                                            context.getSource().sendFeedback(() -> Text.literal("Target set to: " + pos + " | Bedrock placed!"), true);

                                            return 1;
                                        })
                                )
                        )
                )
        );
    }
}
