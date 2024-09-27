package me.mrnavastar.serverafk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class AutoClickCommand {

    public static final AttachmentType<Integer> CPS = AttachmentRegistry.create(Identifier.of("serverafk", "cps"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("autoclick")
                .then(CommandManager.literal("on")
                        .then(CommandManager.argument("cps", IntegerArgumentType.integer())
                                .executes(ctx -> on(ctx, IntegerArgumentType.getInteger(ctx, "cps")))))

                .then(CommandManager.literal("off")
                        .executes(AutoClickCommand::off))
        );

        ServerTickEvents.START_SERVER_TICK.register(server -> server.getPlayerManager().getPlayerList().forEach(player -> {
            Integer cps = player.getAttached(CPS);
            if (cps == null || cps == 0) return;

            if (server.getTicks() % (21 - cps) == 0){
                player.swingHand(Hand.MAIN_HAND, true);

                HitResult hit = entityRayCast(player);
                if (hit == null || !hit.getType().equals(HitResult.Type.ENTITY)) return;

                player.attack(((EntityHitResult) hit).getEntity());
            }
        }));
    }

    private static int on(CommandContext<ServerCommandSource> ctx, int cps) {
        if (!ctx.getSource().isExecutedByPlayer() || cps > 20) return 1;
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        player.setAttached(CPS, cps);
        return 0;
    }

    private static int off(CommandContext<ServerCommandSource> ctx) {
        if (!ctx.getSource().isExecutedByPlayer()) return 1;
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        player.setAttached(CPS, 0);
        return 0;
    }

    private static EntityHitResult entityRayCast(ServerPlayerEntity player) {
        double range = player.getAttributeValue(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE);
        range = range * range;

        Vec3d cameraPos = player.getCameraPosVec(1.0f);
        Vec3d rot = player.getRotationVec(1.0f);
        Vec3d rayCastContext = cameraPos.add(rot.x * range, rot.y * range, rot.z * range);
        Box box = player.getBoundingBox().stretch(rot.multiply(range)).expand(5d, 5d, 5d);
        return ProjectileUtil.raycast(player, cameraPos, rayCastContext, box, (entity -> !entity.isSpectator() && !entity.isInvulnerable() && !entity.isPlayer()), range);
    }

    private static BlockHitResult blockRayCast(ServerPlayerEntity player) {
        return (BlockHitResult) player.raycast(player.getAttributeValue(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE), 1, false);
    }
}