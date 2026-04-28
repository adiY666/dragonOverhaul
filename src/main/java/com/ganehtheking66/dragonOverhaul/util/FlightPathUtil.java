package com.ganehtheking66.dragonOverhaul.util;

import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.ChargingPlayerPhase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.util.math.Vec3d;

public class FlightPathUtil {

    /**
     * Commands the dragon to smoothly fly to a specific coordinate.
     * Can be called repeatedly to update the target mid-flight.
     *
     * @param dragon The Ender Dragon entity.
     * @param target The exact XYZ coordinates to fly to.
     */
    public static void flyTo(EnderDragonEntity dragon, Vec3d target) {

        // 1. Check if the dragon is already in our hijacked phase.
        // If not, switch to it so the dragon's native engine starts listening for a Vec3d target.
        if (dragon.getPhaseManager().getCurrent().getType() != PhaseType.CHARGING_PLAYER) {
            dragon.getPhaseManager().setPhase(PhaseType.CHARGING_PLAYER);
        }

        // 2. Grab the active phase and cast it so we can access its specific methods
        ChargingPlayerPhase chargePhase = (ChargingPlayerPhase) dragon.getPhaseManager().getCurrent();

        // 3. Inject our custom coordinates!
        // The vanilla engine will immediately force the dragon to look at this point
        // and smoothly slither its way over.
        chargePhase.setPathTarget(target);
    }
}