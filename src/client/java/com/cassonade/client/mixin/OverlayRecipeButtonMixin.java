package com.cassonade.client.mixin;

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

import com.cassonade.client.RecipeVariant;

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
	private ItemStack cassonade$result = ItemStack.EMPTY;
	@Unique
	private boolean cassonade$smelting;

	private OverlayRecipeButtonMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Shadow
	protected abstract Identifier getSprite(boolean isCraftable);

	@Override
	public void cassonade$setResult(ItemStack result, boolean smelting) {
		cassonade$result = result;
		cassonade$smelting = smelting;
	}

	@Override
	public ItemStack cassonade$result() {
		return cassonade$result;
	}

	@Override
	public boolean cassonade$smelting() {
		return cassonade$smelting;
	}

	@Override
	public boolean cassonade$craftable() {
		return isCraftable;
	}

	@Override
	public List<Ingredient> cassonade$ingredients() {
		return slots.stream().map(OverlayRecipePosAccessor.class::cast)
				.map(pos -> new Ingredient((pos.cassonade$getX() - CELL_OFFSET) / CELL_STEP, (pos.cassonade$getY() - CELL_OFFSET) / CELL_STEP,
						pos.cassonade$getIngredients()))
				.toList();
	}

	@Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
	private void cassonade$drawResult(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (cassonade$result.isEmpty()) {
			return;
		}
		ci.cancel();
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, getSprite(isCraftable), getX(), getY(), width, height);
		graphics.fakeItem(cassonade$result, getX() + ITEM_INSET, getY() + ITEM_INSET);
		graphics.itemDecorations(Minecraft.getInstance().font, cassonade$result, getX() + ITEM_INSET, getY() + ITEM_INSET);
	}
}
