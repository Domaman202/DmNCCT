package ru.DmN.cacuti.mixin;

import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.DmN.cacuti.access.ITickTimeAccess;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin implements ITickTimeAccess {
    @Shadow protected abstract void tickTime();

    @Override
    public void call_tickTime() {
        this.tickTime();
    }
}
