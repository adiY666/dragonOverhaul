package com.ganehtheking66.dragonOverhaul.attacks;

import com.ganehtheking66.dragonOverhaul.util.FlightPathUtil;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * A testing class to isolate and verify the flight mechanics.
 *
 * @author GanehtHEkinG66
 */
public class TestFlight {

    private static boolean isTesting = false;
    private static boolean hasArrived = false;
    private static Vec3d targetPos = null;

    /**
     * Executes the isolated flight test.
     *
     * @param dragon the dragon executing the test
     */
    public static void execute(EnderDragonEntity dragon) {
        if(!(dragon.getWorld() instanceof ServerWorld world)) return;

        if(!isTesting) {
            targetPos = new Vec3d(0.0, 90.0, 0.0);
            isTesting = true;
            hasArrived = false;
            dragon.getPhaseManager().setPhase(PhaseType.HOVER);
        }

        if(hasArrived) return;

        boolean arrived = true;//FlightPathUtil.flyToWithApproach(dragon, targetPos, targetPos, 1.5);

        if(arrived) {
            hasArrived = true;
            world.playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5.0F, 1.0F);
        }
    }
}
