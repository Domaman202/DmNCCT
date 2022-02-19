package ru.DmN.cacuti.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import ru.DmN.cacuti.Main;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Shadow @Final private GameProfile gameProfile;

    /**
     * @author DomamaN202
     */
    @Overwrite
    public Text getName() {
        var name = Main.prefixes.containsKey(this.gameProfile.getId()) ? Main.prefixes.get(this.gameProfile.getId()) + this.gameProfile.getName() : this.gameProfile.getName();
        String prefix;
        if ((prefix = Main.checkPrefix(this.gameProfile.getName(), Main.permissions)) != null)
            name = prefix + name;
        return new LiteralText(name);
    }
}
