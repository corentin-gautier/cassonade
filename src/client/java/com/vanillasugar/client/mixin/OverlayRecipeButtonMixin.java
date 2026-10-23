package com.vanillasugar.client.mixin;

import java.util.List;

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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import com.vanillasugar.client.RecipeVariant;

/**
 * A card of the recipe's variants shows what it makes at full size, rather than its grid with the
 * ingredients a third of their size; the grid is shown full size by RecipeSheet while it is hovered.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent$OverlayRecipeButton")
public abstract class OverlayRecipeButtonMixin extends AbstractWidget implements RecipeVariant {
	/** The grid's cells are seven pixels apart, from three in (OverlayRecipeButton.createGridPos). */
	@Unique
	private static final int CELL_OFFSET = 3;
	@Unique
	private static final int CELL_STEP = 7;
	@Unique
	private static final int ITEM_INSET = 4;

	@Shadow
	@Final
	private boolean isCraftable;

	@Shadow
	@Final
	private List<?> slots;

	@Unique
	private ItemStack vanillasugar$result = ItemStack.EMPTY;
	@Unique
	private boolean vanillasugar$smelting;

	private OverlayRecipeButtonMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Shadow
	protected abstract Identifier getSprite(boolean isCraftable);

	@Override
	public void vanillasugar$setResult(ItemStack result, boolean smelting) {
		vanillasugar$result = result;
		vanillasugar$smelting = smelting;
	}

	@Override
	public ItemStack vanillasugar$result() {
		return vanillasugar$result;
	}

	@Override
	public boolean vanillasugar$smelting() {
		return vanillasugar$smelting;
	}

	@Override
	public boolean vanillasugar$craftable() {
		return isCraftable;
	}

	@Override
	public List<Ingredient> vanillasugar$ingredients() {
		return slots.stream().map(OverlayRecipePosAccessor.class::cast)
				.map(pos -> new Ingredient((pos.vanillasugar$getX() - CELL_OFFSET) / CELL_STEP, (pos.vanillasugar$getY() - CELL_OFFSET) / CELL_STEP,
						pos.vanillasugar$getIngredients()))
				.toList();
	}

	@Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
	private void vanillasugar$drawResult(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (vanillasugar$result.isEmpty()) {
			return;
		}
		ci.cancel();
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, getSprite(isCraftable), getX(), getY(), width, height);
		graphics.fakeItem(vanillasugar$result, getX() + ITEM_INSET, getY() + ITEM_INSET);
		graphics.itemDecorations(Minecraft.getInstance().font, vanillasugar$result, getX() + ITEM_INSET, getY() + ITEM_INSET);
	}
}
