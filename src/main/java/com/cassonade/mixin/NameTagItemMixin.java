package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;

import com.cassonade.ServerSupport;
import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * A blank name tag, which does nothing in vanilla, silences a mob and is used up like a named one, the mob then staying
 * like a named one. Used again on that mob it gives it its voice back, and stays in hand: nothing is gained from that.
 */
@Mixin(NameTagItem.class)
public abstract class NameTagItemMixin {
	@Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
	private void cassonade$silence(ItemStack stack, Player player, LivingEntity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
		if (stack.has(DataComponents.CUSTOM_NAME) || !(target instanceof Mob mob) || !mob.getType().canSerialize() || !ServerSupport.has(player)
				|| !Settings.on(Setting.SILENT_NAME_TAG)) {
			return;
		}
		if (!player.level().isClientSide() && mob.isAlive()) {
			boolean silencing = !mob.isSilent();
			mob.setSilent(silencing);
			if (silencing) {
				mob.setPersistenceRequired();
				stack.shrink(1);
			}
			player.sendOverlayMessage(Component.translatable(silencing ? "gui.cassonade.name_tag.silenced" : "gui.cassonade.name_tag.voice_back", mob.getDisplayName()));
		}
		info.setReturnValue(InteractionResult.SUCCESS);
	}
}
