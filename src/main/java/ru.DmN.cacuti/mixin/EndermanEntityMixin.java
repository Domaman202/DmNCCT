package ru.DmN.cacuti.mixin;

import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(EndermanEntity.class)
public abstract class EndermanEntityMixin extends LivingEntity {
    protected EndermanEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * @author DomamaN202
     */
    @Overwrite
    private boolean teleportTo(double x, double y, double z) {
        BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);
        while (mutable.getY() > this.world.getBottomY() && !this.world.getBlockState(mutable).getMaterial().blocksMovement())
            mutable.move(Direction.DOWN);
        BlockState blockState = this.world.getBlockState(mutable);
        if (!blockState.getMaterial().blocksMovement() || blockState.getFluidState().isIn(FluidTags.WATER) || blockState.getBlock() == Blocks.AIR || blockState.getBlock() instanceof LeavesBlock || blockState.getBlock() instanceof SlabBlock || blockState.getBlock() instanceof StairsBlock || blockState.getBlock() instanceof CarpetBlock)
            return false;
        boolean bl3 = this.teleport(x, y, z, true);
        if (bl3 && !this.isSilent()) {
            this.world.playSound(null, this.prevX, this.prevY, this.prevZ, SoundEvents.ENTITY_ENDERMAN_TELEPORT, this.getSoundCategory(), 1.0f, 1.0f);
            this.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }
        return bl3;
    }
}
