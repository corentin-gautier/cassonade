package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

import com.cassonade.client.FuelTooltip;
import com.cassonade.client.SmallTooltip;

/**
 * A furnace, blast furnace or smoker tells, in a small tooltip where the anvil writes its cost (SmallTooltip),
 * how long until the items put in are all cooked, and how many more items its fuel will cook: the fuel
 * burning, and the fuel waiting in its slot. The timers come with the window from any server; the
 * fuel waiting needs its burn time from the mod's server (FuelTooltip), else only the burning one counts.
 * <p>
 * The three share the furnace's flame and arrow, drawn on a clear background over their window's own
 * (tools/furnace_progress_textures.py): vanilla's are alike, only their file differs.
 */
@Mixin(AbstractFurnaceScreen.class)
public abstract class AbstractFurnaceScreenMixin<T extends AbstractFurnaceMenu> extends AbstractContainerScreen<T> {
	// The menu's data slots (AbstractFurnaceBlockEntity)
	@Unique
	private static final int LIT_TIME_REMAINING = 0;
	@Unique
	private static final int COOKING_TIMER = 2;
	@Unique
	private static final int COOKING_TOTAL_TIME = 3;
	@Unique
	private static final int SECONDS_PER_MINUTE = 60;
	/** The furnace's window has its arrow lower than the anvil's: the line goes down with it. */
	@Unique
	private static final int LOWER = 3;

	@Unique
	private static final Identifier LIT_PROGRESS = Identifier.withDefaultNamespace("container/furnace/lit_progress");
	@Unique
	private static final Identifier BURN_PROGRESS = Identifier.withDefaultNamespace("container/furnace/burn_progress");

	private AbstractFurnaceScreenMixin(T menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private static Identifier cassonade$furnaceFlame(Identifier litProgressSprite) {
		return LIT_PROGRESS;
	}

	@ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 2)
	private static Identifier cassonade$furnaceArrow(Identifier burnProgressSprite) {
		return BURN_PROGRESS;
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void cassonade$times(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		ContainerData data = ((AbstractFurnaceMenuAccessor) menu).cassonade$getData();
		int perItem = data.get(COOKING_TOTAL_TIME);
		if (perItem <= 0) {
			return;
		}
		ItemStack fuel = menu.getSlot(AbstractFurnaceMenu.FUEL_SLOT).getItem();
		// The fuel's items count for a furnace: a blast furnace or smoker burns twice as fast and cooks twice as fast
		int fuelItems = (int) (data.get(LIT_TIME_REMAINING) / (double) perItem + fuel.getCount() * FuelTooltip.itemsSmelted(fuel));
		int toCook = menu.getSlot(AbstractFurnaceMenu.INGREDIENT_SLOT).getItem().getCount();
		Component line;
		if (toCook > 0 && menu.isLit()) {
			int seconds = (toCook * perItem - data.get(COOKING_TIMER) + 19) / 20;
			String time = seconds / SECONDS_PER_MINUTE + ":" + String.format("%02d", seconds % SECONDS_PER_MINUTE);
			line = Component.translatable("gui.cassonade.furnace.times", time, fuelItems);
		} else if (fuelItems > 0) {
			line = Component.translatable("gui.cassonade.furnace.fuel", fuelItems);
		} else {
			return;
		}
		SmallTooltip.draw(graphics, font, line, leftPos + imageWidth - SmallTooltip.RIGHT, topPos + SmallTooltip.LINE_MIDDLE + LOWER, SmallTooltip.GOLD);
	}
}
