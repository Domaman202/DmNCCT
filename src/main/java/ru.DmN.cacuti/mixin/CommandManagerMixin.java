package ru.DmN.cacuti.mixin;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.DmN.cacuti.Main;

import java.util.ArrayList;

import static ru.DmN.cacuti.Main.permissions;

@Mixin(CommandManager.class)
public class CommandManagerMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    public void executeInject(ServerCommandSource commandSource, String command, CallbackInfoReturnable<Integer> cir) {
        try {
            var user = commandSource.getPlayer().getGameProfile().getName();
            for (var permission : permissions)
                if (Main.checkAccess(user, command, permission, permissions, new ArrayList<>(), false))
                    return;
            if (commandSource.getServer().isSingleplayer())
                return;
            commandSource.getPlayer().sendMessage(new LiteralText("§CPermissions error!"), false);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        cir.setReturnValue(0);
        cir.cancel();
    }
}