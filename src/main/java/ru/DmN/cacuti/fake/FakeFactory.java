package ru.DmN.cacuti.fake;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public final class FakeFactory implements NamedScreenHandlerFactory {
    public static final FakeFactory INSTANCE = new FakeFactory();

    private FakeFactory() {
    }

    @Override
    public Text getDisplayName() {
        return new LiteralText("§4§kNIGGER");
    }

    @Override
    public @NotNull ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new FakeScreenHandler(syncId, inv);
    }
}
