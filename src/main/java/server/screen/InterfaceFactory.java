package server.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerFactory;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class InterfaceFactory implements NamedScreenHandlerFactory {
    public Text text;
    public ScreenHandlerFactory factory;

    public InterfaceFactory(Text text, ScreenHandlerFactory factory) {
        this.text = text;
        this.factory = factory;
    }

    @Override
    public Text getDisplayName() {
        return this.text;
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return this.factory.createMenu(syncId, inv, player);
    }
}
