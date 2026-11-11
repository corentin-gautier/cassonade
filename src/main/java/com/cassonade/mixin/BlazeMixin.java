package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Blaze;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * Blazes notice a player and shoot from 24 blocks instead of 48: from that far, deep in the Nether's fog, neither the
 * blaze nor its fireballs could be seen coming. Blazes saved with the game's range keep it.
 */
@Mixin(Blaze.class)
public abstract class BlazeMixin {
	@Unique
	private static final double CASSONADE_FOLLOW_RANGE = 24.0;

	@Inject(method = "createAttributes", at = @At("RETURN"))
	private static void cassonade$closer(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
		// The default attributes are made once, as the game starts
		if (Settings.local(Setting.BLAZE_RANGE)) {
			cir.getReturnValue().add(Attributes.FOLLOW_RANGE, CASSONADE_FOLLOW_RANGE);
		}
	}
}
