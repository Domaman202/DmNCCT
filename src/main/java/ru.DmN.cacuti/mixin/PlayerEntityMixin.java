package ru.DmN.cacuti.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import ru.DmN.cacuti.Main;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerEntityMixin extends PlayerEntity {
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

    ///

    public PlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }
}
