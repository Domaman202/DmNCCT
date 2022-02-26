package ru.DmN.cacuti.fake;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.registry.Registry;

import java.awt.*;

public class FakeScreenHandler extends ScreenHandler {
    protected FakeScreenHandler(int syncId, PlayerInventory pInv) {
        super(ScreenHandlerType.GENERIC_9X3, syncId);
        //
        var inv = new FakeInventory();
        //
        int i = (3 - 4) * 18;
        for (int j = 0; j < 3; ++j)
            for (int k = 0; k < 9; ++k)
                this.addSlot(new Slot(inv, k + j * 9, 8 + k * 18, 18 + j * 18));
        for (int j = 0; j < 3; ++j)
            for (int k = 0; k < 9; ++k)
                this.addSlot(new Slot(pInv, k + j * 9 + 9, 8 + k * 18, 103 + j * 18 + i));
        for (int j = 0; j < 9; ++j)
            this.addSlot(new Slot(pInv, j, 8 + j * 18, 161 + i));
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        super.onSlotClick(slotIndex, button, actionType, player);
        var item = Registry.ITEM.get(slotIndex);
        player.sendMessage(new LiteralText("§4Вы §7- §1долбаёб§7, а это ").append(item.getName().getWithStyle(Style.EMPTY.withColor(TextColor.fromRgb(Color.GREEN.getRGB()))).get(0)), false);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
