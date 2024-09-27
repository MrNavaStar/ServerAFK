package me.mrnavastar.serverafk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class AFKCommand {

    public static final AttachmentType<Boolean> AFK = AttachmentRegistry.createDefaulted(Identifier.of("serverafk", "afk"), () -> false);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("afk").executes(AFKCommand::afk));

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;

            Boolean afk = player.getAttached(AFK);
            if (afk != null && afk) {
                player.networkHandler.disconnect(Text.of("You took damage while afk!"));
                return false;
            }
            return true;
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> server.getPlayerManager().getPlayerList().forEach(player -> {
            Boolean afk = player.getAttached(AFK);
            if (afk == null || !afk) return;
            if (player.prevHorizontalSpeed != player.horizontalSpeed) afk(player);
        }));
    }

    private static int afk(CommandContext<ServerCommandSource> ctx) {
        if (!ctx.getSource().isExecutedByPlayer()) return 1;
        afk(ctx.getSource().getPlayer());
        return 0;
    }

    private static void afk(ServerPlayerEntity player) {
        Boolean afk = player.getAttached(AFK);
        if (afk == null) afk = false;

        player.setAttached(AFK, !afk);

        if (!afk) player.sendMessage(Text.of("You are now afk."));
        else player.sendMessage(Text.of("You are no longer afk."));
    }
}