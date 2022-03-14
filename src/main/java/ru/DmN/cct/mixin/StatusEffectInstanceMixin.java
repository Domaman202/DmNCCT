package ru.DmN.cct.mixin;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StatusEffectInstance.class)
public class StatusEffectInstanceMixin {
    @Shadow private boolean showParticles;

    @Inject(method = "<init>(Lnet/minecraft/entity/effect/StatusEffect;IIZZZLnet/minecraft/entity/effect/StatusEffectInstance;)V", at = @At("RETURN"))
    void init(StatusEffect type, int duration, int amplifier, boolean ambient, boolean showParticles, boolean showIcon, StatusEffectInstance hiddenEffect, CallbackInfo ci) {
        if (type == StatusEffects.INVISIBILITY)
            this.showParticles = false;
    }
}
