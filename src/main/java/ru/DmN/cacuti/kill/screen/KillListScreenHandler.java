package ru.DmN.cacuti.kill.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import server.screen.InterfaceFactory;
import server.screen.InterfaceScreenHandler;
import server.screen.ViewOnlyInventory;

import static ru.DmN.cacuti.kill.Kill.KILL_LIST;

public class KillListScreenHandler extends InterfaceScreenHandler {
    public KillListScreenHandler(int syncId, PlayerEntity player) {
        super(ScreenHandlerType.GENERIC_9X6, 6, syncId, new Inventory(player), player.getInventory());
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        super.onSlotClick(slotIndex, button, actionType, player);
        if (KILL_LIST.size() > slotIndex)
            player.openHandledScreen(new InterfaceFactory(new LiteralText("Информация о заказе"), (syncId1, inv, player1) -> new KillInfoScreenHandler(syncId1, player1, KILL_LIST.get(slotIndex))));
    }

    public static final class Inventory extends ViewOnlyInventory {
        public final PlayerEntity player;

        public Inventory(PlayerEntity player) {
            this.player = player;
        }

        @Override
        public int size() {
            return 27;
        }

        @Override
        public ItemStack getStack(int slot) {
            if (KILL_LIST.size() <= slot) {
                var stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
                stack.setCustomName(LiteralText.EMPTY);
                return stack;
            } else {
                var kill = KILL_LIST.get(slot);
                if (this.player.getUuid() == kill.killer) {
                    var stack = new ItemStack(Items.GREEN_STAINED_GLASS_PANE);
                    stack.setCustomName(new LiteralText("Заказ выполнен"));
                    return stack;
                } else if (kill.killer != null) {
                    var stack = new ItemStack(Items.RED_STAINED_GLASS_PANE);
                    stack.setCustomName(new LiteralText("Заказ выполнен не вами"));
                    return stack;
                } else {
                    var stack = new ItemStack(Items.YELLOW_STAINED_GLASS_PANE);
                    stack.setCustomName(new LiteralText("Заказ ещё не выполнен"));
                    return stack;
                }
            }
        }
    }
}
