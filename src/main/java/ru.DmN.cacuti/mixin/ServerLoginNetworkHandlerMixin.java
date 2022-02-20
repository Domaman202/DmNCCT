package ru.DmN.cacuti.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(ServerLoginNetworkHandler.class)
public class ServerLoginNetworkHandlerMixin {
    @Inject(method = "addToServer", at = @At("RETURN"))
    private void addToServer(ServerPlayerEntity player, CallbackInfo ci) {
        try {
            System.out.println(player.networkHandler.connection.getAddress());
            Field f = ClientConnection.class.getDeclaredField("field_11651");
            f.setAccessible(true);
            System.out.println(f.get(player.networkHandler.connection));
        } catch (Exception ignored) {
        }
    }
}
