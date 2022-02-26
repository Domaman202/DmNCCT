package server.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

public class InterfaceScreenHandler extends ScreenHandler {
    protected InterfaceScreenHandler(ScreenHandlerType<?> type, int rows, int syncId, Inventory inv, PlayerInventory pInv) {
        super(type, syncId);
        int i = (rows - 4) * 18;
        for (int j = 0; j < rows; ++j)
            for (int k = 0; k < 9; ++k)
                this.addSlot(new InterfaceSlot(inv, k + j * 9, 8 + k * 18, 18 + j * 18));
        for (int j = 0; j < 3; ++j)
            for (int k = 0; k < 9; ++k)
                this.addSlot(new Slot(pInv, k + j * 9 + 9, 8 + k * 18, 103 + j * 18 + i));
        for (int j = 0; j < 9; ++j)
            this.addSlot(new Slot(pInv, j, 8 + j * 18, 161 + i));
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
