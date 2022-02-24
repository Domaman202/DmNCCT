package ru.DmN.cacuti;

import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.awt.dnd.DropTarget;

public class Fake extends ScreenHandler {
    public Fake(ScreenHandlerType<?> type, int syncId, Inventory inv, PlayerInventory playerInventory, int rows) {
        super(type, syncId);
        int i = (rows - 4) * 18;
        for (int j = 0; j < rows; ++j) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(inv, k + j * 9, 8 + k * 18, 18 + j * 18));
            }
        }
        for (int j = 0; j < 3; ++j) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(playerInventory, k + j * 9 + 9, 8 + k * 18, 103 + j * 18 + i));
            }
        }
        for (int j = 0; j < 9; ++j) {
            this.addSlot(new Slot(playerInventory, j, 8 + j * 18, 161 + i));
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        player.sendMessage(new LiteralText("slot -> " + slotIndex + "\nbutton -> " + button + "\nact -> " + actionType), false);
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    public static class Slot extends net.minecraft.screen.slot.Slot {
        public Slot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return false;
        }

        @Override
        public boolean canTakePartial(PlayerEntity player) {
            return false;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }
    }

    public static class Factory implements NamedScreenHandlerFactory {
        @Override
        public Text getDisplayName() {
            return new LiteralText("[X3]");
        }

        @Nullable
        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
            return new Fake(ScreenHandlerType.GENERIC_9X6, syncId, new SimpleInventory(), inv,6);
        }
    }

    public static class FakeDataTracker extends DataTracker {
        public DataTracker source;

        public FakeDataTracker(DataTracker source, Entity entity) {
            super(entity);
            this.source = source;
        }

        @Override
        public <T> T get(TrackedData<T> data) {
            try {
                var x = super.get(data);
                return x == null ? source.get(data) : x;
            } catch (Exception e) {
                return source.get(data);
            }
        }

        @Override
        public <T> void set(TrackedData<T> key, T value) {
            try {
                super.set(key, value);
            } catch (Exception e) {
                source.set(key, value);
            }
        }
    }
}
