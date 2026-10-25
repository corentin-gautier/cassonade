package com.cassonade.client.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;

import com.cassonade.client.RecipeSheet;
import com.cassonade.client.RecipeVariant;

/** The popup of a recipe's variants tells each card what it makes, and details the one hovered. */
@Mixin(OverlayRecipeComponent.class)
public abstract class OverlayRecipeComponentMixin {
	/** As vanilla lays the popup out: rows of four cards, or five past sixteen, 25 pixels apart, in a 4 pixel border. */
	@Unique
	private static final int CARD_STEP = 25;
	@Unique
	private static final int BORDER = 4;

	@Shadow
	@Final
	private List<?> recipeButtons;

	@Shadow
	@Final
	private boolean isFurnaceMenu;

	@Shadow
	private boolean isVisible;

	@Shadow
	private int x;

	@Shadow
	private int y;

	@WrapOperation(method = "init", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
	private boolean cassonade$giveResult(List<Object> buttons, Object button, Operation<Boolean> original,
			@Local RecipeDisplayEntry recipe, @Local(argsOnly = true) ContextMap context) {
		((RecipeVariant) button).cassonade$setResult(recipe.display().result().resolveForFirstStack(context), isFurnaceMenu);
		return original.call(buttons, button);
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void cassonade$detailHovered(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (!isVisible) {
			return;
		}
		for (Object button : recipeButtons) {
			if (((AbstractWidget) button).isHovered() && !((RecipeVariant) button).cassonade$result().isEmpty()) {
				int columns = Math.min(recipeButtons.size(), recipeButtons.size() <= 16 ? 4 : 5);
				Minecraft minecraft = Minecraft.getInstance();
				RecipeSheet.draw(graphics, minecraft.font, (RecipeVariant) button, x, x + columns * CARD_STEP + 2 * BORDER, y,
						minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
				return;
			}
		}
	}
}
