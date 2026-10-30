package com.cassonade.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/**
 * The server remembers the pattern picked and picks it again once the loom has a banner and a dye back. Only the
 * server does, so that a client on a server without the mod doesn't show a result that isn't there.
 */
@Mixin(LoomMenu.class)
public abstract class LoomMenuMixin {
	@Shadow
	@Final
	private DataSlot selectedBannerPatternIndex;
	@Shadow
	@Final
	private Slot bannerSlot;
	@Shadow
	@Final
	private Slot dyeSlot;
	@Shadow
	private List<Holder<BannerPattern>> selectablePatterns;

	@Unique
	private Holder<BannerPattern> cassonade$picked;

	@Shadow
	private void setupResultSlot(Holder<BannerPattern> pattern) {
	}

	@Inject(method = "clickMenuButton", at = @At("RETURN"))
	private void cassonade$rememberPattern(Player player, int buttonId, CallbackInfoReturnable<Boolean> info) {
		if (info.getReturnValue() && !player.level().isClientSide()) {
			cassonade$picked = selectablePatterns.get(buttonId);
		}
	}

	@Inject(method = "slotsChanged", at = @At("TAIL"))
	private void cassonade$pickAgain(Container container, CallbackInfo info) {
		if (cassonade$picked == null || selectedBannerPatternIndex.get() != -1 || !bannerSlot.hasItem() || !dyeSlot.hasItem()) {
			return;
		}
		int index = selectablePatterns.indexOf(cassonade$picked);
		BannerPatternLayers layers = bannerSlot.getItem().getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
		if (index >= 0 && layers.layers().size() < 6) {
			selectedBannerPatternIndex.set(index);
			// The client reset its selection on its own when the banner changed, whatever the server last told it
			((AbstractContainerMenuAccessor) this).cassonade$getRemoteDataSlots().set(0, -1);
			setupResultSlot(cassonade$picked);
		}
	}
}
