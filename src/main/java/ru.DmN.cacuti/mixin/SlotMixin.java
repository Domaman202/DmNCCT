package ru.DmN.cacuti.mixin;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.DmN.cacuti.ISHAccess;
import ru.DmN.cacuti.ISlotAccess;
import ru.DmN.cacuti.Main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Mixin(Slot.class)
public class SlotMixin implements ISlotAccess {
    @Shadow
    @Final
    public Inventory inventory;
    @Shadow
    @Final
    private int index;
    ScreenHandler __dmn;

    @Inject(method = "takeStack", at = @At("HEAD"))
    void take(int amount, CallbackInfoReturnable<ItemStack> cir) throws IOException {
        if (__dmn != null) {
            var gp = ((ISHAccess) __dmn).getDmN0().getGameProfile();
            if (gp != null && Main.logList.contains(gp.getName())) {
                ((ISHAccess) __dmn).getDmN().write(("id -> " + index + "\ncount -> " + amount + "\nitem -> " + this.inventory.getStack(index).getItem() + "\ninv -> " + inventory + '\n').getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;I)Lnet/minecraft/item/ItemStack;", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;getStack()Lnet/minecraft/item/ItemStack;"))
    void insert(ItemStack stack, int count, CallbackInfoReturnable<ItemStack> cir) throws IOException {
        if (__dmn != null) {
            var gp = ((ISHAccess) __dmn).getDmN0().getGameProfile();
            if (gp != null && Main.logList.contains(gp.getName())) {
                ((ISHAccess) __dmn).getDmN().write(("id -> " + index + "\ncount -> " + count + "\nitem -> " + stack.getItem() + "\ninv -> " + inventory + '\n').getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    @Override
    public void setDmN(ScreenHandler __dmn) {
        this.__dmn = __dmn;
    }
}
