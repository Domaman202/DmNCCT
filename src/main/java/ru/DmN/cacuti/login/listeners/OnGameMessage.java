package ru.DmN.cacuti.login.listeners;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import ru.DmN.cacuti.Main;

import java.util.concurrent.atomic.AtomicBoolean;

import static ru.DmN.cacuti.Main.checkAccess;

public class OnGameMessage {
    public static boolean canSendMessage(ServerPlayNetworkHandler networkHandler, ChatMessageC2SPacket packet) {
        ServerPlayerEntity player = networkHandler.player;
        AtomicBoolean playerLogin = Main.getPlayer.get(player);
        String message = packet.getChatMessage();
        // TODO: config to allow more commands when you're not logged
        var user = networkHandler.player.getGameProfile().getName();
        if (!playerLogin.get() && ((message.startsWith("/login") && checkAccess(user, "/login")) || (message.startsWith("/register") && checkAccess(user, "/register"))))
            return true;
        return playerLogin.get();
    }
}
