package ru.DmN.cacuti.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import ru.DmN.cacuti.Main;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerEntityMixin extends PlayerEntity {
    @Shadow @Final public MinecraftServer server;

    @Override
    public Text getName() {
        var name = this.getGameProfile() != null && Main.prefixes.containsKey(this.getGameProfile().getId()) ? Main.prefixes.get(this.getGameProfile().getId()) + this.getGameProfile().getName() : this.getGameProfile().getName();
        String prefix;
        if ((prefix = Main.checkPrefix(this.getGameProfile().getName(), Main.permissions)) != null)
            name = prefix + name;
        return new LiteralText(name);
    }

    /**
     * @author DomamaN202
     */
    @Overwrite
    public @Nullable Text getPlayerListName() {
        return this.getName();
    }

    @Override
    public void sleep(BlockPos pos) {
        super.sleep(pos);
        var pm = this.server.getPlayerManager();
        int i = (int) pm.getPlayerList().stream().filter(LivingEntity::isSleeping).count();
        int j = pm.getCurrentPlayerCount() / 4;
        System.out.println(i);
        System.out.println(j);
        if (i > j)
            ((ServerWorld) this.world).setTimeOfDay(0);
    }

    ///

    public PlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }
}
