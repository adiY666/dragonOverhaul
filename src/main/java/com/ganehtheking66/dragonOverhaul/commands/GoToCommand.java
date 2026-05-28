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
 * A go to command instance utilizing PID and feedforward control for both distance and rotation.
 *
 * @author GanehtHEkinG66
 */
public class GoToCommand {

    // Distance Control Constants
    private static final double DIST_K_P = 0.05;
    private static final double DIST_K_I = 0.0;
    private static final double DIST_K_D = 0.02;
    private static final double DIST_K_S = 0.2;
    private static final double DIST_K_V = 0.1;
    private static final double DIST_K_A = 0.0;
    private static final double MAX_VELOCITY = 1.0;

    // Rotation Control Constants
    private static final double ROT_K_P = 0.1;
    private static final double ROT_K_I = 0.0;
    private static final double ROT_K_D = 0.01;
    private static final double ROT_K_S = 0.5;
    private static final double ROT_K_V = 0.05;
    private static final double ROT_K_A = 0.0;

    // Turn Curve Math Constants
    private static final double TURN_CURVE_NUMERATOR = 150.0;
    private static final double TURN_CURVE_DENOMINATOR = 15.0;
    private static final float MAX_TURN_CAP = 10.0F;
    private static final float MIN_TURN_CAP = 1.0F;

    private static final double ARRIVAL_TOLERANCE_HORIZONTAL = 3.0;
    private static final double ARRIVAL_TOLERANCE_VERTICAL = 6.0;
    private static final double HALF_CIRCLE_DEGREES = 180.0;
    private static final float RIGHT_ANGLE_DEGREES = 90.0F;
    private static final int REQUIRED_SETTLE_TICKS = 30;
    private static final int TIMEOUT_TICKS = 600;

    private static boolean isActive = false;
    private static Vec3d targetPos = null;
    private static float targetYaw = -1.0F;
    private static int ticksAtTarget = 0;
    private static int ticksRunning = 0;

    private static final PidController distPid = new PidController(DIST_K_P, DIST_K_I, DIST_K_D);
    private static final FeedForwardController distFf = new FeedForwardController(DIST_K_S, DIST_K_V, DIST_K_A);

    private static final PidController yawPid = new PidController(ROT_K_P, ROT_K_I, ROT_K_D);
    private static final FeedForwardController yawFf = new FeedForwardController(ROT_K_S, ROT_K_V, ROT_K_A);

    private static final PidController pitchPid = new PidController(ROT_K_P, ROT_K_I, ROT_K_D);
    private static final FeedForwardController pitchFf = new FeedForwardController(ROT_K_S, ROT_K_V, ROT_K_A);

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
            ForwardFlightCommand.setFlying(false, 0.0);
            dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
            broadcastMessage(dragon, "[GoToCommand] FAILED: Dragon timed out before reaching the target!");
            return;
        }

        dragon.getPhaseManager().setPhase(PhaseType.HOVER);
        Vec3d currentPos = dragon.getPos();

        double diffX = targetPos.x - currentPos.x;
        double diffY = targetPos.y - currentPos.y;
        double diffZ = targetPos.z - currentPos.z;
        double horizontalError = Math.sqrt(diffX * diffX + diffZ * diffZ);
        double verticalError = Math.abs(diffY);
        double currentError = currentPos.distanceTo(targetPos);

        if(horizontalError <= ARRIVAL_TOLERANCE_HORIZONTAL && verticalError <= ARRIVAL_TOLERANCE_VERTICAL) {
            ForwardFlightCommand.setFlying(false, 0.0);
            dragon.setVelocity(Vec3d.ZERO);
            ticksAtTarget++;

            float finalYawDiff = MathHelper.wrapDegrees(targetYaw - dragon.getYaw());
            finalYawDiff = MathHelper.clamp(finalYawDiff, -MAX_TURN_CAP, MAX_TURN_CAP);

            dragon.setYaw(dragon.getYaw() + finalYawDiff);
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

        // 1. Drivetrain Control (Distance)
        double distFeedForward = Math.min(distFf.calculate(currentError), MAX_VELOCITY);
        double distPidOutput = distPid.calculate(0.0, currentError);
        double targetSpeed = Math.min(distFeedForward + distPidOutput, MAX_VELOCITY);

        // 2. Trajectory Math
        Vec3d direction = targetPos.subtract(currentPos).normalize();
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float targetYawAngle = (float) (MathHelper.atan2(direction.z, direction.x) * (HALF_CIRCLE_DEGREES / Math.PI)) + RIGHT_ANGLE_DEGREES;
        float targetPitchAngle = (float) -(MathHelper.atan2(direction.y, horizontalDistance) * (HALF_CIRCLE_DEGREES / Math.PI));

        double yawError = MathHelper.wrapDegrees(targetYawAngle - dragon.getYaw());
        double pitchError = MathHelper.wrapDegrees(targetPitchAngle - dragon.getPitch());

        // 3. Rotation Control (Yaw & Pitch PID + FF)
        double yawOutput = yawPid.calculate(0.0, yawError) + yawFf.calculate(yawError);
        double pitchOutput = pitchPid.calculate(0.0, pitchError) + pitchFf.calculate(pitchError);

        // 4. Dynamic Slew Rate: Continuous Mathematical Curve
        float maxTurnStep = (float) (TURN_CURVE_NUMERATOR / (currentError + TURN_CURVE_DENOMINATOR));
        maxTurnStep = MathHelper.clamp(maxTurnStep, MIN_TURN_CAP, MAX_TURN_CAP);

        // Apply the mathematically clamped step to the final PID outputs
        float yawDiff = MathHelper.clamp((float) yawOutput, -maxTurnStep, maxTurnStep);
        float pitchDiff = MathHelper.clamp((float) pitchOutput, -maxTurnStep, maxTurnStep);

        dragon.setYaw(dragon.getYaw() + yawDiff);
        dragon.setPitch(dragon.getPitch() + pitchDiff);

        dragon.setBodyYaw(dragon.getYaw());
        dragon.setHeadYaw(dragon.getYaw());

        // 5. Fire the engine
        ForwardFlightCommand.setFlying(true, targetSpeed);
    }

    public static void overrideTarget(Vec3d pos, float yaw) {
        targetPos = pos;
        targetYaw = yaw;
        isActive = true;
        ticksAtTarget = 0;
        ticksRunning = 0;

        distPid.reset();
        yawPid.reset();
        pitchPid.reset();
    }

    private static void broadcastMessage(EnderDragonEntity dragon, String message) {
        if(dragon.getWorld() instanceof ServerWorld world) {
            if(world.getServer() != null) {
                world.getServer().getPlayerManager().broadcast(Text.literal(message), false);
            }
        }
    }
}
