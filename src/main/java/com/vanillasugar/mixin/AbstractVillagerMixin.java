package com.vanillasugar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;

/** Villagers take a lead, to walk them to a trading hall. Wandering traders still do not. */
@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {
	@Inject(method = "canBeLeashed", at = @At("HEAD"), cancellable = true)
	private void vanillasugar$leashVillagers(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Villager) {
			cir.setReturnValue(true);
		}
	}
}
