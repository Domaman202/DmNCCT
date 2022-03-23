package ru.DmN.cacuti.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.SleepManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(SleepManager.class)
public class SleepManagerMixin {
    /**
     * @author DomamaN202
     */
    @Overwrite
    public boolean canResetTime(int percentage, List<ServerPlayerEntity> players) {
        return false;
    }
}
