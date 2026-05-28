package com.ganehtheking66.dragonOverhaul;

import com.ganehtheking66.dragonOverhaul.commands.DragonChatCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class DragonOverhaul implements ModInitializer {

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            DragonChatCommand.register(dispatcher);
        });
    }
}
