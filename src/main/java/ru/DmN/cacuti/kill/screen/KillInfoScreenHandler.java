package ru.DmN.cacuti.kill.screen;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import ru.DmN.cacuti.kill.Kill;
import server.screen.InterfaceScreenHandler;
import server.screen.ViewOnlyInventory;

import static ru.DmN.cacuti.kill.Kill.KILL_LIST;

public class KillInfoScreenHandler extends InterfaceScreenHandler {
    public final PlayerEntity player;
    public final Kill kill;

    public KillInfoScreenHandler(int syncId, PlayerEntity player, Kill kill) {
        super(ScreenHandlerType.GENERIC_9X6, 6, syncId, new Inventory(player, kill), player.getInventory());
        this.player = player;
        this.kill = kill;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        super.onSlotClick(slotIndex, button, actionType, player);
        if (slotIndex == 13 && this.kill.killer == player.getUuid()) {
            for (var stack : this.kill.rewards)
                if (!player.giveItemStack(stack))
                    Block.dropStack(player.world, player.getBlockPos(), stack);
            this.kill.rewards.clear();
            KILL_LIST.remove(this.kill);
        }
    }

    public static class Inventory extends ViewOnlyInventory {
        public final PlayerEntity player;
        public final Kill kill;

        public Inventory(PlayerEntity player, Kill kill) {
            this.player = player;
            this.kill = kill;
        }

        @Override
        public int size() {
            return 9 * 6;
        }

        @Override
        public ItemStack getStack(int slot) {
            if (slot == 10) {
                var stack = new ItemStack(Items.SKELETON_SKULL);
                stack.setCustomName(player.getServer().getPlayerManager().getPlayer(this.kill.target).getName());
                return stack;
            } else if (slot == 13) {
                if (this.kill.killer == this.player.getUuid()) {
                    var stack = new ItemStack(Items.GREEN_STAINED_GLASS_PANE);
                    stack.setCustomName(new LiteralText("Забрать награду"));
                    return stack;
                } else if (this.kill.killer == null) {
                    var stack = new ItemStack(Items.YELLOW_STAINED_GLASS_PANE);
                    stack.setCustomName(new LiteralText("Заказ не выполнен"));
                    return stack;
                } else {
                    var stack = new ItemStack(Items.RED_STAINED_GLASS_PANE);
                    stack.setCustomName(new LiteralText("Заказ выполнен не вами"));
                    return stack;
                }
            } else if (slot == 16) {
                if (this.kill.killer == null) {
                    var stack = new ItemStack(Items.RED_STAINED_GLASS_PANE);
                    stack.setCustomName(new LiteralText("Заказ не выполнен"));
                    return stack;
                }

                var stack = new ItemStack(Items.PLAYER_HEAD);
                stack.setCustomName(player.getServer().getPlayerManager().getPlayer(this.kill.killer).getName());
                return stack;
            } else if (slot > 27 && slot != 35 && slot != 36 && slot < 44) {
                var i = slot - 28;
                if (slot > 36)
                    i -= 2;
                if (i < this.kill.rewards.size())
                return this.kill.rewards.get(i);
                return ItemStack.EMPTY;
            }

            var stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            stack.setCustomName(LiteralText.EMPTY);
            return stack;
        }
    }
}
