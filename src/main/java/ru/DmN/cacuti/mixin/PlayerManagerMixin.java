package ru.DmN.cacuti.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.DmN.cacuti.Main;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Shadow @Final private MinecraftServer server;

    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "checkCanJoin", at = @At("HEAD"), cancellable = true)
    void connectJoin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) {
        if (Main.coolDownPlayerList.containsKey(profile.getName())) {
            Main.coolDownPlayerList.get(profile.getName()).getRight().interrupt();
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(1000);
                    Main.runCooldown(this.server.getPlayerManager().getPlayer(profile.getId()), LOGGER);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            cir.setReturnValue(null);
            cir.cancel();
        }
    }
}
