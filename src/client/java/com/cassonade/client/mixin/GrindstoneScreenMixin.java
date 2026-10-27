package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import com.cassonade.client.SmallTooltip;

/**
 * The experience the grindstone gives back, written where the anvil writes its cost and in the same
 * small tooltip (SmallTooltip), before the item is taken. The game draws it at random between half
 * the enchantments' worth and all of it: both ends are shown. Curses are worth nothing, as in vanilla.
 * Computed on the client from the items' enchantments, so it works on any server.
 */
@Mixin(GrindstoneScreen.class)
public abstract class GrindstoneScreenMixin extends AbstractContainerScreen<GrindstoneMenu> {
	private GrindstoneScreenMixin(GrindstoneMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void cassonade$experience(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (!menu.getSlot(GrindstoneMenu.RESULT_SLOT).hasItem()) {
			return;
		}
		int worth = cassonade$worth(menu.getSlot(GrindstoneMenu.INPUT_SLOT).getItem())
				+ cassonade$worth(menu.getSlot(GrindstoneMenu.ADDITIONAL_SLOT).getItem());
		if (worth <= 0) {
			return;
		}
		// GrindstoneMenu's draw: half the worth rounded up, plus a random number below that half
		int least = Mth.ceil(worth / 2.0);
		int most = 2 * least - 1;
		Component line = least == most
				? Component.translatable("gui.cassonade.grindstone.experience.one", least)
				: Component.translatable("gui.cassonade.grindstone.experience", least, most);
		SmallTooltip.draw(graphics, font, line, leftPos + imageWidth - SmallTooltip.RIGHT, topPos + SmallTooltip.LINE_MIDDLE, SmallTooltip.GOLD);
	}

	/** What an item's enchantments, curses aside, are worth to the grindstone (GrindstoneMenu.getExperienceFromItem). */
	@Unique
	private static int cassonade$worth(ItemStack stack) {
		int worth = 0;
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
			if (!entry.getKey().is(EnchantmentTags.CURSE)) {
				worth += entry.getKey().value().getMinCost(entry.getIntValue());
			}
		}
		return worth;
	}
}
