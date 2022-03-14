package ru.DmN.cacuti.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.Blocks;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.DmN.cacuti.Main;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

import static ru.DmN.cacuti.Main.unsafe;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Inject(method = "checkCanJoin", at = @At("HEAD"), cancellable = true)
    void connectJoin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) {
        if (Main.coolDownPlayerList.containsKey(profile.getId())) {
            unsafe.putInt(Main.coolDownPlayerList.get(profile.getId()), 0);
            cir.setReturnValue(null);
            cir.cancel();
        }
    }

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    public void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        Main.getPlayer.get(player).set(false);
        player.setInvulnerable(true);
        player.sendMessage(new LiteralText("§9Welcome to the server, in order to play, you must log in.\n§eLog in using /login and register using /register"), false);
        player.networkHandler.sendPacket(new TitleS2CPacket(new LiteralText("§aIdentify yourself")));
        World world = player.world;
        if (world.getBlockState(player.getBlockPos()).getBlock() == Blocks.NETHER_PORTAL) {
            CompletableFuture.runAsync(() -> {
                try {
                    BlockPos pos = player.getBlockPos();
                    if (world.getBlockState(pos.up()).getBlock() == Blocks.NETHER_PORTAL)
                        world.setBlockState(pos.up(), Blocks.AIR.getDefaultState());
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    Thread.sleep(10000);
                    world.setBlockState(pos, Blocks.FIRE.getDefaultState());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Inject(method = "remove", at = @At("TAIL"))
    public void remove(ServerPlayerEntity player, CallbackInfo ci) {
        Main.getPlayer.get(player).set(false);
    }
}
