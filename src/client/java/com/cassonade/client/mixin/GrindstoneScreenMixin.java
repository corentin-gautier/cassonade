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
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import com.cassonade.client.map.SmallText;

/**
 * The experience the grindstone gives back, written where the anvil writes its cost and in the same
 * small tooltip (AnvilScreenMixin), before the item is taken. The game draws it at random between half
 * the enchantments' worth and all of it: both ends are shown. Curses are worth nothing, as in vanilla.
 * Computed on the client from the items' enchantments, so it works on any server.
 */
@Mixin(GrindstoneScreen.class)
public abstract class GrindstoneScreenMixin extends AbstractContainerScreen<GrindstoneMenu> {
	/** The mod's gold for experience levels (see AnvilScreenMixin). */
	@Unique
	private static final int COLOR = 0xFFFFD76A;
	@Unique
	private static final int CAPITAL_HEIGHT = 7;
	/** The middle of the line the anvil writes its cost on, a little lower, as AnvilScreenMixin places it. */
	@Unique
	private static final int LINE_MIDDLE = 69 + 4 + 3;
	@Unique
	private static final int RIGHT = 8 + 1 + 6;

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
		float scale = SmallText.scale();
		int width = SmallText.width(font, line);
		int height = Mth.ceil(CAPITAL_HEIGHT * scale);
		int left = leftPos + imageWidth - RIGHT - width;
		int top = topPos + LINE_MIDDLE - height / 2;
		TooltipRenderUtil.extractTooltipBackground(graphics, left, top, width, height, null);
		graphics.pose().pushMatrix();
		graphics.pose().translate(left, top);
		graphics.pose().scale(scale, scale);
		graphics.text(font, line, 0, 0, COLOR);
		graphics.pose().popMatrix();
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
