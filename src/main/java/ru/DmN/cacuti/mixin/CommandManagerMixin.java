package ru.DmN.cacuti.mixin;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.DmN.cacuti.Main;

@Mixin(CommandManager.class)
public class CommandManagerMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    public void executeInject(ServerCommandSource commandSource, String command, CallbackInfoReturnable<Integer> cir) {
        try {
            if (Main.checkAccess(commandSource.getPlayer().getGameProfile().getName(), command))
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
