package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;

import com.cassonade.client.combat.ThroughPlants;

/** An attack aimed at a plant with a mob behind it goes to the mob (see {@link ThroughPlants}). */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "startAttack", at = @At("HEAD"))
	private void cassonade$attackThroughPlants(CallbackInfoReturnable<Boolean> cir) {
		Minecraft minecraft = (Minecraft) (Object) this;
		if (minecraft.player != null && !minecraft.player.isSpectator() && minecraft.hitResult instanceof BlockHitResult aimed) {
			ThroughPlants.behind(minecraft.player, aimed).ifPresent(hit -> {
				minecraft.hitResult = hit;
				minecraft.crosshairPickEntity = hit.getEntity();
			});
		}
	}
}
