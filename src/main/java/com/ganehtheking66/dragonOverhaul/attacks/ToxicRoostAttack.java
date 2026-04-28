package com.ganehtheking66.dragonOverhaul.attacks;

import com.ganehtheking66.dragonOverhaul.util.FlightPathUtil;
import java.util.List;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * A toxic roost attack instance behavior for the dragon.
 *
 * @author GanehtHEkinG66
 */
public class ToxicRoostAttack {

    private static final int BASE_COOLDOWN_TICKS = 600;
    private static final int RANDOM_COOLDOWN_BOUND = 1801;
    private static final int PERCENTAGE_MAX = 100;
    private static final int SHOOTING_COOLDOWN_TICKS = 10;
    private static final int MAX_SHOTS_FIRED = 6;
    private static final int MAX_ROOST_TICKS = 300;
    private static final int CONTINUE_ATTACK_CHANCE = 70;
    private static final int STATE_FLYING = 0;
    private static final int STATE_SHOOTING = 1;
    private static final double SEARCH_RADIUS = 1500.0;
    private static final double HOVER_HEIGHT_OFFSET = 6.0;
    private static final double FLIGHT_SPEED = 2.5;
    private static final double Y_FIREBALL_OFFSET = 1.0;

    private static boolean isActive = false;
    private static int state = STATE_FLYING;
    private static EndCrystalEntity targetTower = null;
    private static int ticksInState = 0;
    private static int shotsFired = 0;
    private static int cooldownTicks = BASE_COOLDOWN_TICKS;

    /**
     * Executes the toxic roost attack sequence.
     *
     * @param dragon the dragon executing the attack
     */
    public static void execute(EnderDragonEntity dragon) {
        if(!(dragon.getWorld() instanceof ServerWorld world)) return;

        if(!isActive) {
            if(cooldownTicks > 0) {
                cooldownTicks--;
                return;
            }
            isActive = true;
            state = STATE_FLYING;
            targetTower = null;
            shotsFired = 0;
            ticksInState = 0;
            dragon.getPhaseManager().setPhase(PhaseType.HOVER);
        }

        if(state == STATE_FLYING) {
            if(targetTower == null || targetTower.isRemoved()) {
                Box searchArea = dragon.getBoundingBox().expand(SEARCH_RADIUS);
                List<EndCrystalEntity> crystals = world.getEntitiesByClass(EndCrystalEntity.class, searchArea, e -> true);

                if(crystals.isEmpty()) {
                    endAttack(dragon, world);
                    return;
                }
                targetTower = crystals.get(world.random.nextInt(crystals.size()));
            }

            Vec3d targetPos = targetTower.getPos().add(0, HOVER_HEIGHT_OFFSET, 0);
            PlayerEntity player = world.getClosestPlayer(dragon, SEARCH_RADIUS);
            Vec3d lookTarget = player != null ? player.getPos() : targetPos;
            boolean arrived = true;//FlightPathUtil.flyToWithApproach(dragon, targetPos, lookTarget, FLIGHT_SPEED);

            if(arrived) {
                state = STATE_SHOOTING;
                ticksInState = 0;
            }
            return;
        }

        if(state == STATE_SHOOTING) {
            dragon.setVelocity(Vec3d.ZERO);
            ticksInState++;

            if(ticksInState % SHOOTING_COOLDOWN_TICKS == 0) {
                PlayerEntity player = world.getClosestPlayer(dragon, SEARCH_RADIUS);

                if(player != null && !player.isCreative() && !player.isSpectator()) {
                    Vec3d aim = player.getPos().subtract(dragon.getPos()).normalize();
                    DragonFireballEntity fireball = new DragonFireballEntity(world, dragon, aim);

                    fireball.setPosition(dragon.getX(), dragon.getY() - Y_FIREBALL_OFFSET, dragon.getZ());
                    world.spawnEntity(fireball);
                    shotsFired++;
                }

                if(player != null && (player.isCreative() || player.isSpectator())) {
                    endAttack(dragon, world);
                    return;
                }
            }

            // THE FIX: Move on if we fired 6 shots OR if 15 seconds have passed!
            if(shotsFired >= MAX_SHOTS_FIRED || ticksInState >= MAX_ROOST_TICKS) {
                if(world.random.nextInt(PERCENTAGE_MAX) < CONTINUE_ATTACK_CHANCE) {
                    state = STATE_FLYING;
                    targetTower = null;
                    shotsFired = 0;
                    ticksInState = 0;
                    return;
                }

                endAttack(dragon, world);
            }
        }
    }

    /**
     * Ends the attack and returns the dragon to its normal phase.
     *
     * @param dragon the dragon executing the attack
     * @param world  the world the dragon is in
     */
    private static void endAttack(EnderDragonEntity dragon, ServerWorld world) {
        isActive = false;
        dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
        cooldownTicks = BASE_COOLDOWN_TICKS + world.random.nextInt(RANDOM_COOLDOWN_BOUND);
    }
}
