package com.cassonade.client.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;

import com.cassonade.Cassonade;
import com.cassonade.client.BackpackPanel;
import com.cassonade.gear.GearLayout;

/**
 * Every screen with a recipe book (inventory, crafting table, furnaces): the open book closes with its
 * ribbon, out of the top of the book, as the button that opened it would.
 */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin extends AbstractContainerScreen<RecipeBookMenu> {
	@Unique
	private static final WidgetSprites RIBBON_SPRITES = new WidgetSprites(
			Cassonade.id("container/recipe_book_ribbon"), Cassonade.id("container/recipe_book_ribbon_highlighted"));

	@Shadow
	@Final
	private RecipeBookComponent<?> recipeBookComponent;

	@Unique
	private @Nullable ImageButton cassonade$ribbon;

	private AbstractRecipeBookScreenMixin(RecipeBookMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void cassonade$addRibbon(CallbackInfo ci) {
		cassonade$ribbon = null;
		for (var widget : children()) {
			if (widget instanceof ImageButton button
					&& ((ImageButtonAccessor) button).cassonade$getSprites() == RecipeBookComponent.RECIPE_BUTTON_SPRITES) {
				// Pressing the book's button, even hidden (the inventory's tab stands for it), also moves the window back
				cassonade$ribbon = addRenderableWidget(new ImageButton(0, 0, GearLayout.RIBBON_WIDTH, GearLayout.RIBBON_HEIGHT,
						RIBBON_SPRITES, pressed -> button.onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0))));
				cassonade$ribbon.setTooltip(Tooltip.create(Component.translatable("gui.cassonade.recipe_book.close")));
				cassonade$placeRibbon();
				return;
			}
		}
	}

	/** Outside the inventory, the book opens and closes without the widgets being rebuilt: the ribbon follows it each frame. */
	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void cassonade$followBook(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		cassonade$placeRibbon();
	}

	@Unique
	private void cassonade$placeRibbon() {
		if (cassonade$ribbon == null) {
			return;
		}
		cassonade$ribbon.visible = recipeBookComponent.isVisible();
		if (cassonade$ribbon.visible) {
			RecipeBookComponentAccessor book = (RecipeBookComponentAccessor) recipeBookComponent;
			cassonade$ribbon.setPosition(book.cassonade$getXOrigin() + GearLayout.RIBBON_X, book.cassonade$getYOrigin() + GearLayout.RIBBON_Y);
		}
	}

	/** Clicks on the backpack panel must not count as clicks outside the window, which would drop the carried item. */
	@Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
	private void cassonade$insideBackpack(double mx, double my, int xo, int yo, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof InventoryScreen && BackpackPanel.contains(mx, my, xo, yo)) {
			cir.setReturnValue(false);
		}
	}
}
