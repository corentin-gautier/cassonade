package com.cassonade.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

/**
 * The first packet goes over every tab, not only those the player has progress in: with every
 * advancement sent (AdvancementVisibilityEvaluatorMixin), a new player gets the End's and
 * husbandry's too.
 */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
	@Shadow
	private boolean isFirstPacket;
	@Shadow
	@Final
	private Set<AdvancementNode> rootsToUpdate;
	@Shadow
	private AdvancementTree tree;

	@Inject(method = "flushDirty", at = @At("HEAD"))
	private void cassonade$updateEveryTab(ServerPlayer player, boolean showAdvancements, CallbackInfo ci) {
		if (isFirstPacket) {
			tree.roots().forEach(rootsToUpdate::add);
		}
	}
}
