package ru.DmN.cacuti.kill;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Kill {
    public static final List<Kill> KILL_LIST = new ArrayList<>();
    public final List<ItemStack> rewards;
    public final UUID target;
    public UUID killer;

    public Kill(UUID target, UUID killer, List<ItemStack> rewards) {
        this.target = target;
        this.killer = killer;
        this.rewards = rewards;
    }
}
