package ru.DmN.cacuti.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow @Final private MinecraftServer server;

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
}
