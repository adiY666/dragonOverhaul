package com.ganehtheking66.dragonOverhaul.commands;

import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * A command instance that forces the dragon to fly constantly in the direction it is facing.
 *
 * @author GanehtHEkinG66
 */
public class ForwardFlightCommand {

    private static final float YAW_OFFSET = 180.0F;
    private static final double MAX_ACCELERATION = 0.3;

    private static boolean isFlying = false;
    private static double currentSpeed = 0.0;

    /**
     * Executes the forward flight logic.
     *
     * @param dragon the dragon to steer
     */
    public static void execute(EnderDragonEntity dragon) {
        if(!(dragon.getWorld() instanceof ServerWorld)) return;

        if(!isFlying) return;

        dragon.getPhaseManager().setPhase(PhaseType.HOVER);

        float pitch = dragon.getPitch();
        float yaw = dragon.getYaw() + YAW_OFFSET;

        Vec3d forwardDirection = Vec3d.fromPolar(pitch, yaw).normalize();
        Vec3d desiredVelocity = forwardDirection.multiply(currentSpeed);
        Vec3d currentVelocity = dragon.getVelocity();

        // Smoothly accelerate toward the desired speed/direction
        Vec3d smoothVelocity = new Vec3d(
                MathHelper.lerp(MAX_ACCELERATION, currentVelocity.x, desiredVelocity.x),
                MathHelper.lerp(MAX_ACCELERATION, currentVelocity.y, desiredVelocity.y),
                MathHelper.lerp(MAX_ACCELERATION, currentVelocity.z, desiredVelocity.z)
        );

        dragon.setVelocity(smoothVelocity);
    }

    /**
     * Toggles the forward flight state and sets the target speed.
     *
     * @param state true to start flying, false to stop
     * @param speed the desired flight speed
     */
    public static void setFlying(boolean state, double speed) {
        isFlying = state;
        currentSpeed = speed;
    }
}
