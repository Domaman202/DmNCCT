package ru.DmN.cacuti.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.DmN.cacuti.mixin_.ISHAccess;
import ru.DmN.cacuti.mixin_.ISlotAccess;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin implements ISHAccess {
    private FileOutputStream DmNFOS = null;
    private PlayerEntity DmNPE = null;

    @Inject(method = "onSlotClick", at = @At("HEAD"))
    void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        DmNPE = player;
    }

    @Inject(method = "addSlot", at = @At("RETURN"))
    void test(Slot slot, CallbackInfoReturnable<Slot> cir) {
        ((ISlotAccess) slot).setDmN((ScreenHandler) (Object) this);
    }

    @Inject(method = "close", at = @At("HEAD"))
    void close(PlayerEntity player, CallbackInfo ci) throws IOException {
        if (DmNFOS != null) {
            DmNFOS.flush();
            DmNFOS.close();
            DmNFOS = null;
        }
    }

    @Override
    public FileOutputStream getDmN() throws IOException {
        if (DmNFOS == null) {
            if (!Files.exists(Paths.get("./logs/inv/")))
                Files.createDirectory(Paths.get("./logs/inv/"));
            DmNFOS = new FileOutputStream("./logs/inv/" + Files.list(new File("./logs/inv/").toPath()).count());
            DmNFOS.write(("Player => " + DmNPE.getGameProfile().getName() + "Player pos => " + DmNPE.getPos() + "\nBlock pos => " + DmNPE.raycast(64, 0, false).getPos() + '\n').getBytes(StandardCharsets.UTF_8));
        }
        return DmNFOS;
    }

    @Override
    public PlayerEntity getDmN0() {
        return DmNPE;
    }

    @Override
    protected void finalize() throws Throwable {
        if (DmNFOS != null) {
            DmNFOS.flush();
            DmNFOS.close();
            DmNFOS = null;
        }
        super.finalize();
    }
}
