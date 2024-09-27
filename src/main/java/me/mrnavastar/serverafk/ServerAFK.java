package me.mrnavastar.serverafk;

import me.mrnavastar.serverafk.commands.AutoClickCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class ServerAFK implements ModInitializer {

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(AutoClickCommand::register);
    }
}
