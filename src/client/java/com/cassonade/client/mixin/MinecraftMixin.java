package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;

import com.cassonade.client.combat.ThroughPlants;
import com.cassonade.client.map.Pings;

/**
 * An attack aimed at a plant with a mob behind it goes to the mob (see {@link ThroughPlants}). Shift and a middle
 * click points out what is looked at instead of picking the block (see {@link Pings}).
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "startAttack", at = @At("HEAD"))
	// Minecraft is AutoCloseable, but this is the running game, not something to close
	@SuppressWarnings("resource")
	private void cassonade$attackThroughPlants(CallbackInfoReturnable<Boolean> cir) {
		Minecraft minecraft = (Minecraft) (Object) this;
		if (minecraft.player != null && !minecraft.player.isSpectator() && minecraft.hitResult instanceof BlockHitResult aimed) {
			ThroughPlants.behind(minecraft.player, aimed).ifPresent(hit -> {
				minecraft.hitResult = hit;
				minecraft.crosshairPickEntity = hit.getEntity();
			});
		}
	}

	@Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
	private void cassonade$ping(CallbackInfo ci) {
		if (Pings.point((Minecraft) (Object) this)) {
			ci.cancel();
		}
	}
}
