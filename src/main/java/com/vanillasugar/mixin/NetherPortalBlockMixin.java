package com.vanillasugar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.portal.TeleportTransition;

/** Nether portals teleport at once and silently, the client side drops the distortion (see LocalPlayerMixin). */
@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin {
	@Inject(method = "getPortalTransitionTime", at = @At("HEAD"), cancellable = true)
	private void vanillasugar$noPortalDelay(ServerLevel level, Entity entity, CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(0);
	}

	@Redirect(method = "getExitPortal", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/portal/TeleportTransition;PLAY_PORTAL_SOUND:Lnet/minecraft/world/level/portal/TeleportTransition$PostTeleportTransition;"), require = 2)
	private TeleportTransition.PostTeleportTransition vanillasugar$noTravelSound() {
		return entity -> {};
	}
}
