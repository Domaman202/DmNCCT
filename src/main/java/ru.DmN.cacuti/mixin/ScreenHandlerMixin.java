package ru.DmN.cacuti.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.DmN.cacuti.Main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {
    @Shadow
    public abstract DefaultedList<ItemStack> getStacks();

    private FileOutputStream __dmn_out = null;

    @Inject(method = "onSlotClick", at = @At("HEAD"))
    void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) throws IOException {
        if (Main.logList.contains(player.getName().asString()))
            this.log(slotIndex, button, actionType, player);
    }

    @Inject(method = "close", at = @At("HEAD"))
    void close(PlayerEntity player, CallbackInfo ci) throws IOException {
        if (__dmn_out != null) {
            __dmn_out.flush();
            __dmn_out.close();
            __dmn_out = null;
        }
    }

    synchronized void log(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) throws IOException {
        StringBuilder sb = new StringBuilder();
        if (__dmn_out == null) {
            if (!Files.exists(Paths.get("./logs/inv/")))
                Files.createDirectory(Paths.get("./logs/inv/"));
            __dmn_out = new FileOutputStream("./logs/inv/" + Files.list(new File("./logs/inv/").toPath()).count());
            sb.append("Player pos => ").append(player.getPos()).append("\nBlock pos => ").append(player.raycast(64, 0, false).getPos());
        }
        sb.append('\n').append(slotIndex).append('/').append(button).append('/').append(actionType).append("\nINVENTORY:\n");
        for (int i = 0; i < this.getStacks().size(); i++) {
            if (this.getStacks().get(i).isEmpty()) continue;
            sb.append('[').append(i).append(']').append(this.getStacks().get(i).getItem()).append('<').append(this.getStacks().get(i).getCount()).append(">\n");
        }
        sb.append("\nPLAYER:\n");
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).isEmpty()) continue;
            sb.append('[').append(i).append(']').append(player.getInventory().getStack(i).getItem()).append('<').append(player.getInventory().getStack(i).getCount()).append(">\n");
        }
        __dmn_out.write(sb.append('\n').toString().getBytes(StandardCharsets.UTF_8));
    }
}
