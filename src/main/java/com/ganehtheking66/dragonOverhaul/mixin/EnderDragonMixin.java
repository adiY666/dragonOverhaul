package com.ganehtheking66.dragonOverhaul.mixin;

import com.ganehtheking66.dragonOverhaul.attacks.ToxicRoostAttack;
import com.ganehtheking66.dragonOverhaul.commands.ForwardFlightCommand;
import com.ganehtheking66.dragonOverhaul.commands.GoToCommand;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonEntity.class)
public class EnderDragonMixin {

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTick(CallbackInfo info) {
        EnderDragonEntity thisDragon = (EnderDragonEntity) (Object) this;

        ToxicRoostAttack.execute(thisDragon);

        GoToCommand.execute(thisDragon);

        ForwardFlightCommand.execute(thisDragon);
    }
}
