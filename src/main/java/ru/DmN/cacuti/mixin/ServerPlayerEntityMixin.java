package ru.DmN.cacuti.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.DmN.cacuti.Helper;
import ru.DmN.cacuti.Main;

import java.util.List;

import static ru.DmN.cacuti.Main.*;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    @Shadow @Final public MinecraftServer server;

    @Shadow public abstract ServerWorld getWorld();

    @Shadow public abstract void sendMessage(Text message, boolean actionBar);

    @Override
    public Text getName() {
        var profile = this.getGameProfile();
        var name = new StringBuilder(nicks.getOrDefault(this.getGameProfile().getId(), profile.getName()));

        var team = this.getScoreboardTeam();
        if (team != null)
            name = new StringBuilder(Main.toNormaString(team.decorateName(new LiteralText(name.toString()))));

        var prefix = Main.prefixes.getOrDefault(this.getGameProfile().getId(), null);
        if (prefix != null)
            name = new StringBuilder(prefix).append(name);

        var pprefix = checkPrefix(this.getGameProfile().getName(), Main.permissions);
        if (pprefix != null)
            name = new StringBuilder(pprefix).append(name);

        return new LiteralText(name.toString());
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
            if ((int) pm.getPlayerList().stream().filter(LivingEntity::isSleeping).count() > (pm.getCurrentPlayerCount() / 4)) {
                ((ServerWorld) this.world).setTimeOfDay(13000);
                var t = Math.random() < 0.25;
                ((ServerWorld) this.world).setWeather(0, 0, Math.random() < 0.5 || t, t);
            }
            cir.setReturnValue(Either.right(Unit.INSTANCE));
            cir.cancel();
        }
    }

    @Override
    public void sleep(BlockPos pos) {
        super.sleep(pos);
        var pm = this.server.getPlayerManager();
        if ((int) pm.getPlayerList().stream().filter(LivingEntity::isSleeping).count() > pm.getCurrentPlayerCount() / 4) {
            ((ServerWorld) this.world).setTimeOfDay(0);
            var t = Math.random() < 0.25;
            ((ServerWorld) this.world).setWeather(0, 0, Math.random() < 0.5 || t, t);
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        var x = super.damage(source, amount);
        var y = source.name.equals("arrow");
        if (x) {
            if (source == DamageSource.IN_FIRE || source == DamageSource.ON_FIRE || source == DamageSource.LAVA || y) {
                this.removeStatusEffect(StatusEffects.INVISIBILITY);
                this.server.getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, List.of((ServerPlayerEntity) (Object) this)));
            }

            if ((y && source.getAttacker().isPlayer()) || source.name.equals("player")) {
                var stack = this.getInventory().armor.get(2);
                if (stack.getItem() == Items.ELYTRA) {
                    this.getInventory().removeOne(stack);
                    if (!this.giveItemStack(stack))
                        Block.dropStack(this.getWorld(), this.getBlockPos(), stack);
                }

                if (this.getHealth() - amount > 0)
                    Main.runCooldown((ServerPlayerEntity) (Object) this);
            }
        }
        return x;
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    public void onDeath(DamageSource source, CallbackInfo ci) {
        if (Main.coolDownPlayerList.containsKey(this.getGameProfile().getId()))
            unsafe.putIntVolatile(Main.coolDownPlayerList.get(this.getGameProfile().getId()), Helper.OFFSET_I, 0);
    }

    @Override
    public boolean hasCustomName() {
        return true;
    }

    @Nullable
    @Override
    public Text getCustomName() {
        return this.getName();
    }

    @Override
    public Text getDisplayName() {
        return this.getName();
    }

    ///

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }
}
