package com.ganehtheking66.dragonOverhaul.commands;

import com.ganehtheking66.dragonOverhaul.util.FeedForwardController;
import com.ganehtheking66.dragonOverhaul.util.PidController;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * A go to command instance utilizing PID and feedforward control.
 *
 * @author GanehtHEkinG66
 */
public class GoToCommand {

    private static final double K_P = 0.15;
    private static final double K_I = 0.0;
    private static final double K_D = 0.05;
    private static final double K_S = 0.0;
    private static final double K_V = 0.8;
    private static final double K_A = 0.0;
    private static final double MAX_VELOCITY = 2.5;
    private static final double MAX_ACCELERATION = 0.15;
    private static final double ARRIVAL_TOLERANCE = 2.0;
    private static final double HALF_CIRCLE_DEGREES = 180.0;
    private static final float RIGHT_ANGLE_DEGREES = 90.0F;
    private static final float ROTATION_SPEED = 0.2F;
    private static final int REQUIRED_SETTLE_TICKS = 30;
    private static final int TIMEOUT_TICKS = 600;

    private static boolean isActive = false;
    private static Vec3d targetPos = null;
    private static float targetYaw = -1.0F;
    private static int ticksAtTarget = 0;
    private static int ticksRunning = 0;
    private static final PidController pid = new PidController(K_P, K_I, K_D);
    private static final FeedForwardController ff = new FeedForwardController(K_S, K_V, K_A);

    /**
     * Executes the flight path overriding normal AI.
     *
     * @param dragon the dragon to steer
     */
    public static void execute(EnderDragonEntity dragon) {
        if(!(dragon.getWorld() instanceof ServerWorld)) return;

        if(!isActive || targetPos == null) return;

        ticksRunning++;

        if(ticksRunning > TIMEOUT_TICKS) {
            isActive = false;
            dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
            broadcastMessage(dragon, "[GoToCommand] FAILED: Dragon timed out before reaching the target!");
            return;
        }

        dragon.getPhaseManager().setPhase(PhaseType.HOVER);
        Vec3d currentPos = dragon.getPos();
        double currentError = currentPos.distanceTo(targetPos);

        if(currentError <= ARRIVAL_TOLERANCE) {
            dragon.setVelocity(Vec3d.ZERO);
            ticksAtTarget++;

            float yawDiff = MathHelper.wrapDegrees(targetYaw - dragon.getYaw());

            dragon.setYaw(dragon.getYaw() + yawDiff * ROTATION_SPEED);
            dragon.setBodyYaw(dragon.getYaw());
            dragon.setHeadYaw(dragon.getYaw());

            if(ticksAtTarget >= REQUIRED_SETTLE_TICKS) {
                isActive = false;
                dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
                broadcastMessage(dragon, "[GoToCommand] SUCCESS: Dragon has safely arrived at the target!");
            }
            return;
        }

        ticksAtTarget = 0;

        double feedForward = Math.min(ff.calculate(currentError), MAX_VELOCITY);
        double pidOutput = pid.calculate(0.0, currentError);
        double targetSpeed = Math.min(feedForward + pidOutput, MAX_VELOCITY);
        Vec3d direction = targetPos.subtract(currentPos).normalize();
        Vec3d desiredVelocity = direction.multiply(targetSpeed);
        Vec3d currentVelocity = dragon.getVelocity();

        Vec3d smoothVelocity = new Vec3d(
                MathHelper.lerp(MAX_ACCELERATION, currentVelocity.x, desiredVelocity.x),
                MathHelper.lerp(MAX_ACCELERATION, currentVelocity.y, desiredVelocity.y),
                MathHelper.lerp(MAX_ACCELERATION, currentVelocity.z, desiredVelocity.z)
        );

        dragon.setVelocity(smoothVelocity);

        double horizontalVelocity = Math.sqrt(smoothVelocity.x * smoothVelocity.x + smoothVelocity.z * smoothVelocity.z);
        float calculatedYaw = (float) (MathHelper.atan2(smoothVelocity.z, smoothVelocity.x) * (HALF_CIRCLE_DEGREES / Math.PI)) - RIGHT_ANGLE_DEGREES;
        float calculatedPitch = (float) -(MathHelper.atan2(smoothVelocity.y, horizontalVelocity) * (HALF_CIRCLE_DEGREES / Math.PI));
        float yawDiff = MathHelper.wrapDegrees(calculatedYaw - dragon.getYaw());

        dragon.setYaw(dragon.getYaw() + yawDiff * ROTATION_SPEED);

        float pitchDiff = MathHelper.wrapDegrees(calculatedPitch - dragon.getPitch());

        dragon.setPitch(dragon.getPitch() + pitchDiff * ROTATION_SPEED);
        dragon.setBodyYaw(dragon.getYaw());
        dragon.setHeadYaw(dragon.getYaw());
    }

    /**
     * Triggered by the chat command to inject a new target.
     *
     * @param pos the new vector target
     * @param yaw the final facing angle
     */
    public static void overrideTarget(Vec3d pos, float yaw) {
        targetPos = pos;
        targetYaw = yaw;
        isActive = true;
        ticksAtTarget = 0;
        ticksRunning = 0;
        pid.reset();
    }

    /**
     * Broadcasts a message to the server chat.
     *
     * @param dragon  the dragon entity
     * @param message the message to broadcast
     */
    private static void broadcastMessage(EnderDragonEntity dragon, String message) {
        if(dragon.getWorld() instanceof ServerWorld world) {
            if(world.getServer() != null) {
                world.getServer().getPlayerManager().broadcast(Text.literal(message), false);
            }
        }
    }
}
