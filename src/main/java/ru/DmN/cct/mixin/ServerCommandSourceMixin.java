package ru.DmN.cct.mixin;

import com.mojang.brigadier.ResultConsumer;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommandSource.class)
public class ServerCommandSourceMixin {
    @Mutable
    @Shadow
    @Final
    private int level;

    @Inject(at = @At("RETURN"), method = "<init>(Lnet/minecraft/server/command/CommandOutput;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec2f;Lnet/minecraft/server/world/ServerWorld;ILjava/lang/String;Lnet/minecraft/text/Text;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/entity/Entity;ZLcom/mojang/brigadier/ResultConsumer;Lnet/minecraft/command/argument/EntityAnchorArgumentType$EntityAnchor;)V")
    void ServerCommandSource(CommandOutput output, Vec3d pos, Vec2f rot, ServerWorld world, int level, String name, Text displayName, MinecraftServer server, Entity entity, boolean silent, ResultConsumer<?> consumer, EntityAnchorArgumentType.EntityAnchor entityAnchor, CallbackInfo ci) {
        this.level = 999;
    }
}
