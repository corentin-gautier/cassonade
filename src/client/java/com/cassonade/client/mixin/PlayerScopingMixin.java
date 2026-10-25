package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.player.Player;

import com.cassonade.item.CassonadeItems;

/**
 * The naturalist's spyglass in use zooms like the vanilla one, scope and slower aim included: the game only
 * knows its own spyglass. The server never asks whether a player is scoping, so this only touches what the
 * client draws.
 */
@Mixin(Player.class)
public abstract class PlayerScopingMixin {
	@Inject(method = "isScoping", at = @At("RETURN"), cancellable = true)
	private void cassonade$naturalistSpyglass(CallbackInfoReturnable<Boolean> cir) {
		Player player = (Player) (Object) this;
		if (!cir.getReturnValue() && player.isUsingItem() && player.getUseItem().is(CassonadeItems.NATURALIST_SPYGLASS)) {
			cir.setReturnValue(true);
		}
	}
}
