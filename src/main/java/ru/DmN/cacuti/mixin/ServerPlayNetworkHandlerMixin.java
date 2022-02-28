package ru.DmN.cacuti.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    public ServerPlayerEntity player;

    @Shadow
    private int ticks;

    @Shadow
    public abstract void syncWithPlayerPosition();

    @Shadow
    private @Nullable Vec3d requestedTeleportPos;

    @Shadow
    private int teleportRequestTick;

    @Shadow
    public abstract void requestTeleport(double x, double y, double z, float yaw, float pitch);

    @Shadow
    private static double clampHorizontal(double d) {
        return 0;
    }

    @Shadow
    private static double clampVertical(double d) {
        return 0;
    }

    @Shadow
    private double lastTickX;

    @Shadow
    private double lastTickY;

    @Shadow
    private double lastTickZ;

    @Shadow
    private int movePacketsCount;

    @Shadow
    private double updatedX;

    @Shadow
    private double updatedY;

    @Shadow
    private double updatedZ;

    @Shadow
    protected abstract boolean isPlayerNotCollidingWithBlocks(WorldView world, Box box);

    @Shadow
    private boolean floating;

    @Shadow
    protected abstract boolean isEntityOnAir(Entity entity);

    @Shadow
    private @Nullable Entity topmostRiddenEntity;

    @Shadow
    @Final
    public ClientConnection connection;

    @Shadow
    private double updatedRiddenX;

    @Shadow
    private double updatedRiddenY;

    @Shadow
    private double updatedRiddenZ;

    @Shadow
    private boolean vehicleFloating;

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    public void disconnect(Text reason, CallbackInfo ci) {
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("TIMER STARTED!");
                Thread.sleep(15 * 1000);
                System.out.println("TIMER COMPLETE!");
                if (this.server.getPlayerManager().getCurrentPlayerCount() == 0) {
                    System.out.println("STOPPING SERVER!");
                    this.server.getCommandManager().getDispatcher().execute("stop", this.server.getCommandSource());
                    System.exit(1);
                }
            } catch (InterruptedException | CommandSyntaxException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * @author DomamaN202
     */
    @Overwrite
    public void onPlayerMove(PlayerMoveC2SPacket packet) {
        boolean bl;
        NetworkThreadUtils.forceMainThread((Packet) packet, (PacketListener) this, this.player.getWorld());
        ServerWorld serverWorld = this.player.getWorld();
        if (this.player.notInAnyWorld)
            return;
        if (this.ticks == 0)
            this.syncWithPlayerPosition();
        if (this.requestedTeleportPos != null) {
            if (this.ticks - this.teleportRequestTick > 20) {
                this.teleportRequestTick = this.ticks;
                this.requestTeleport(this.requestedTeleportPos.x, this.requestedTeleportPos.y, this.requestedTeleportPos.z, this.player.getYaw(), this.player.getPitch());
            }
            return;
        }
        this.teleportRequestTick = this.ticks;
        double d = clampHorizontal(packet.getX(this.player.getX()));
        double e = clampVertical(packet.getY(this.player.getY()));
        double f = clampHorizontal(packet.getZ(this.player.getZ()));
        float g = MathHelper.wrapDegrees(packet.getYaw(this.player.getYaw()));
        float h = MathHelper.wrapDegrees(packet.getPitch(this.player.getPitch()));
        if (this.player.hasVehicle()) {
            this.player.updatePositionAndAngles(this.player.getX(), this.player.getY(), this.player.getZ(), g, h);
            this.player.getWorld().getChunkManager().updatePosition(this.player);
            return;
        }
        double i = this.player.getX();
        double j = this.player.getY();
        double k = this.player.getZ();
        double l = this.player.getY();
        double m = d - this.lastTickX;
        double n = e - this.lastTickY;
        double o = f - this.lastTickZ;
        double q = m * m + n * n + o * o;
        if (this.player.isSleeping()) {
            if (q > 1.0)
                this.requestTeleport(this.player.getX(), this.player.getY(), this.player.getZ(), g, h);
            return;
        }
        ++this.movePacketsCount;
        Box box = this.player.getBoundingBox();
        m = d - this.updatedX;
        n = e - this.updatedY;
        o = f - this.updatedZ;
        bl = n > 0.0;
        if (this.player.isOnGround() && !packet.isOnGround() && bl)
            this.player.jump();
        this.player.move(MovementType.PLAYER, new Vec3d(m, n, o));
        double t = n;
        m = d - this.player.getX();
        n = e - this.player.getY();
        if (n > -0.5 || n < 0.5)
            n = 0.0;
        o = f - this.player.getZ();
        q = m * m + n * n + o * o;
        boolean bl22 = !this.player.isInTeleportationState() && q > 0.0625 && !this.player.isSleeping() && !this.player.interactionManager.isCreative() && this.player.interactionManager.getGameMode() != GameMode.SPECTATOR;
        this.player.updatePositionAndAngles(d, e, f, g, h);
        if (!this.player.noClip && !this.player.isSleeping() && (bl22 && serverWorld.isSpaceEmpty(this.player, box) || this.isPlayerNotCollidingWithBlocks(serverWorld, box))) {
            this.requestTeleport(i, j, k, g, h);
            return;
        }
        this.floating = t >= -0.03125 && this.player.interactionManager.getGameMode() != GameMode.SPECTATOR && !this.server.isFlightEnabled() && !this.player.getAbilities().allowFlying && !this.player.hasStatusEffect(StatusEffects.LEVITATION) && !this.player.isFallFlying() && this.isEntityOnAir(this.player);
        this.player.getWorld().getChunkManager().updatePosition(this.player);
        this.player.handleFall(this.player.getY() - l, packet.isOnGround());
        this.player.setOnGround(packet.isOnGround());
        if (bl)
            this.player.onLanding();
        this.player.increaseTravelMotionStats(this.player.getX() - i, this.player.getY() - j, this.player.getZ() - k);
        this.updatedX = this.player.getX();
        this.updatedY = this.player.getY();
        this.updatedZ = this.player.getZ();
    }

    /**
     * @author DomamaN202
     */
    @Overwrite
    public void onVehicleMove(VehicleMoveC2SPacket packet) {
        NetworkThreadUtils.forceMainThread(packet, (ServerPlayNetworkHandler) (Object) this, this.player.getWorld());
        Entity entity = this.player.getRootVehicle();
        if (entity != this.player && entity.getPrimaryPassenger() == this.player && entity == this.topmostRiddenEntity) {
            ServerWorld serverWorld = this.player.getWorld();
            double d = entity.getX();
            double e = entity.getY();
            double f = entity.getZ();
            double g = clampHorizontal(packet.getX());
            double h = clampVertical(packet.getY());
            double i = clampHorizontal(packet.getZ());
            float j = MathHelper.wrapDegrees(packet.getYaw());
            float k = MathHelper.wrapDegrees(packet.getPitch());

            boolean bl = serverWorld.isSpaceEmpty(entity, entity.getBoundingBox().contract(0.0625D));
            double l = g - this.updatedRiddenX;
            double m = h - this.updatedRiddenY - 1.0E-6D;
            double n = i - this.updatedRiddenZ;
            entity.move(MovementType.PLAYER, new Vec3d(l, m, n));

            entity.updatePositionAndAngles(g, h, i, j, k);
            boolean bl3 = serverWorld.isSpaceEmpty(entity, entity.getBoundingBox().contract(0.0625D));
            if (bl && !bl3) {
                entity.updatePositionAndAngles(d, e, f, j, k);
                this.connection.send(new VehicleMoveS2CPacket(entity));
                return;
            }

            this.player.getWorld().getChunkManager().updatePosition(this.player);
            this.player.increaseTravelMotionStats(this.player.getX() - d, this.player.getY() - e, this.player.getZ() - f);
            this.vehicleFloating = m >= -0.03125D && !this.server.isFlightEnabled() && this.isEntityOnAir(entity);
            this.updatedRiddenX = entity.getX();
            this.updatedRiddenY = entity.getY();
            this.updatedRiddenZ = entity.getZ();
        }
    }
}