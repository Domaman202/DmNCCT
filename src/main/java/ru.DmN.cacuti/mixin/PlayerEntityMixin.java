package ru.DmN.cacuti.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.DmN.cacuti.Main;

import java.util.Random;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerEntityMixin extends PlayerEntity {
    @Shadow @Final public MinecraftServer server;

    @Shadow protected abstract boolean isBedTooFarAway(BlockPos pos, Direction direction);

    @Shadow protected abstract boolean isBedObstructed(BlockPos pos, Direction direction);

    @Shadow public abstract void setSpawnPoint(RegistryKey<World> dimension, @Nullable BlockPos pos, float angle, boolean forced, boolean sendMessage);

    @Override
    public Text getName() {
        var name = this.getGameProfile() != null && Main.prefixes.containsKey(this.getGameProfile().getId()) ? Main.prefixes.get(this.getGameProfile().getId()) + this.getGameProfile().getName() : this.getGameProfile().getName();
        String prefix;
        if ((prefix = Main.checkPrefix(this.getGameProfile().getName(), Main.permissions)) != null)
            name = prefix + name;
        return new LiteralText(name);
    }

    /**
     * @author DomamaN202
     */
    @Overwrite
    public @Nullable Text getPlayerListName() {
        return this.getName();
    }

    /**
     * @author DomamaN202
     */
    @Inject(method = "trySleep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isDay()Z", shift = At.Shift.BEFORE), cancellable = true)
    public void trySleep(BlockPos pos, CallbackInfoReturnable<Either<SleepFailureReason, Unit>> cir) {
        if (this.world.isDay()) {
            super.sleep(pos);
            var pm = this.server.getPlayerManager();
            int i = (int) pm.getPlayerList().stream().filter(LivingEntity::isSleeping).count();
            int j = pm.getCurrentPlayerCount() / 4;
            System.out.println(i);
            System.out.println(j);
            if (i > j) {
                ((ServerWorld) this.world).setTimeOfDay(13000);
                var rand = new Random();
                var t = rand.nextBoolean();
                ((ServerWorld) this.world).setWeather(0, 0, rand.nextBoolean() || t, t);
            }
            cir.setReturnValue(Either.right(Unit.INSTANCE));
            cir.cancel();
        }
    }

    @Override
    public void sleep(BlockPos pos) {
        super.sleep(pos);
        var pm = this.server.getPlayerManager();
        int i = (int) pm.getPlayerList().stream().filter(LivingEntity::isSleeping).count();
        int j = pm.getCurrentPlayerCount() / 4;
        System.out.println(i);
        System.out.println(j);
        if (i > j) {
            ((ServerWorld) this.world).setTimeOfDay(0);
            ((ServerWorld) this.world).setWeather(0, 0, false, false);
        }
    }

    ///

    public PlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }
}
