package ru.DmN.cacuti.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
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
import ru.DmN.cacuti.Main;
import ru.DmN.cacuti.access.INormalWakeUpAccess;
import ru.DmN.cacuti.access.ITickTimeAccess;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static ru.DmN.cacuti.Main.*;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements INormalWakeUpAccess {
    @Shadow @Final public MinecraftServer server;

    @Shadow public abstract ServerWorld getWorld();

    @Shadow public abstract void sendMessage(Text message, boolean actionBar);

    @Shadow protected abstract boolean isBedTooFarAway(BlockPos pos, Direction direction);

    @Shadow protected abstract boolean isBedObstructed(BlockPos pos, Direction direction);

    @Shadow public abstract void setSpawnPoint(RegistryKey<World> dimension, @Nullable BlockPos pos, float angle, boolean forced, boolean sendMessage);

    @Shadow public ServerPlayNetworkHandler networkHandler;

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
    @Overwrite
    public void wakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers) {
    }

    @Override
    public void call_normalWakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers) {
        if (this.isSleeping()) {
            this.getWorld().getChunkManager().sendToNearbyPlayers(this, new EntityAnimationS2CPacket(this, EntityAnimationS2CPacket.WAKE_UP));
        }
        super.wakeUp(skipSleepTimer, updateSleepingPlayers);
        if (this.networkHandler != null) {
            this.networkHandler.requestTeleport(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
        }
    }

    /**
     * @author DomamaN202
     */
    @Inject(method = "trySleep", at = @At("HEAD"), cancellable = true)
    public void trySleep(BlockPos pos, CallbackInfoReturnable<Either<SleepFailureReason, Unit>> cir) {
        Direction direction = this.world.getBlockState(pos).get(HorizontalFacingBlock.FACING);
        if (this.isSleeping() || !this.isAlive()) {
            cir.setReturnValue(Either.left(PlayerEntity.SleepFailureReason.OTHER_PROBLEM));
            cir.cancel();
        }
        if (!this.world.getDimension().isNatural()) {
            cir.setReturnValue(Either.left(PlayerEntity.SleepFailureReason.NOT_POSSIBLE_HERE));
            cir.cancel();
        }
        if (!this.isBedTooFarAway(pos, direction)) {
            cir.setReturnValue(Either.left(PlayerEntity.SleepFailureReason.TOO_FAR_AWAY));
            cir.cancel();
        }
        if (this.isBedObstructed(pos, direction)) {
            cir.setReturnValue(Either.left(PlayerEntity.SleepFailureReason.OBSTRUCTED));
            cir.cancel();
        }
        this.setSpawnPoint(this.world.getRegistryKey(), pos, this.getYaw(), false, true);
        if (!this.isCreative()) {
            Vec3d vec3d = Vec3d.ofBottomCenter(pos);
            List<HostileEntity> list = this.world.getEntitiesByClass(HostileEntity.class, new Box(vec3d.getX() - 8.0, vec3d.getY() - 5.0, vec3d.getZ() - 8.0, vec3d.getX() + 8.0, vec3d.getY() + 5.0, vec3d.getZ() + 8.0), hostileEntity -> hostileEntity.isAngryAt(this));
            if (!list.isEmpty()) {
                cir.setReturnValue(Either.left(PlayerEntity.SleepFailureReason.NOT_SAFE));
                cir.cancel();
            }
        }
        Either<PlayerEntity.SleepFailureReason, Unit> either = super.trySleep(pos).ifRight(unit -> {
            this.incrementStat(Stats.SLEEP_IN_BED);
            Criteria.SLEPT_IN_BED.trigger((ServerPlayerEntity) (Object) this);
            unsafe.loadFence();
            if (unsafe.getByte(sleepThreadLock) == 0) {
                unsafe.putByte(sleepThreadLock, (byte) 1);
                var task = new AtomicReference<ScheduledFuture<?>>();
                task.set(pool.scheduleAtFixedRate(() -> {
                    boolean sleep = false;
                    for (var player : this.world.getPlayers())
                        sleep |= player.isSleeping();
                    if (sleep) {
                        ((ITickTimeAccess) this.getWorld()).call_tickTime();
                        this.getWorld().setWeather(0, 0, Math.random() >= 0.5, Math.random() >= 0.5);
                    } else {
                        unsafe.putByte(sleepThreadLock, (byte) 0);
                        task.get().cancel(false);
                        unsafe.storeFence();
                    }
                }, 10, 1, TimeUnit.MILLISECONDS));
            }
        });
        if (!this.getWorld().isSleepingEnabled()) {
            this.sendMessage(new TranslatableText("sleep.not_possible"), true);
        }
        ((ServerWorld)this.world).updateSleepingPlayers();
        cir.setReturnValue(either);
        cir.cancel();
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
        if (Main.coolDownPlayerList.containsKey(this.getGameProfile().getId())) {
            unsafe.putInt(Main.coolDownPlayerList.get(this.getGameProfile().getId()), 0);
            unsafe.storeFence();
        }
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
