package ru.DmN.cacuti.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.DmN.cacuti.Main;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Inject(method = "checkCanJoin", at = @At("HEAD"))
    void connect(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) throws InterruptedException {
        AtomicBoolean wait = new AtomicBoolean(false);
        synchronized (Main.coolDownMap) {
            Main.coolDownMap.forEach((playerEntity, atomicInteger) -> {
                if (profile.getName().equals(playerEntity.getGameProfile().getName())) {
                    atomicInteger.set(0);
                    try {
                        wait.set(true);
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        if (wait.get())
            Thread.sleep(100);
    }
}
