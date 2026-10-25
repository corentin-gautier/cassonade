package com.cassonade.client.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategories;

import com.cassonade.Cassonade;

/**
 * The recipe book's tabs (tools/recipe_book_textures.py), from the first row of recipes down (RecipeBookComponentMixin),
 * glued: the others tucked in up against the pages, the selected one standing out two pixels, told
 * by its page, drawn wider so it crosses the book's cover and its paper joins the pages. Each shows an icon
 * engraved in it rather than items.
 */
@Mixin(RecipeBookTabButton.class)
public abstract class RecipeBookTabButtonMixin extends AbstractWidget {
	/** Short, just around the icon; a pixel more than the step between tabs, the bottom edge hiding under the next tab's top edge. */
	@Unique
	private static final int HEIGHT = 22;
	/** Standing out 24 pixels left of the book, and running 5 pixels under its cover. */
	@Unique
	private static final int WIDTH = 29;
	/** From the tab's left edge to just inside the pages, past the cover and its shadow. */
	@Unique
	private static final int SELECTED_WIDTH = 34;
	/** How far vanilla moves the selected tab and its icon to the left. */
	@Unique
	private static final int STEP_OUT = 2;
	/** How far the other tabs, and their icons, move right: in, up against the pages. */
	@Unique
	private static final int TUCK_IN = 2;
	/** The icon, its cut centered in the leather (or the page) that shows of the tab, between its lit top edge and its shadow. */
	@Unique
	private static final int ICON_SIZE = 16;
	@Unique
	private static final int ICON_LEFT = 5;
	@Unique
	private static final int ICON_TOP = 4;

	@Shadow
	private boolean selected;

	@Shadow
	@Final
	private RecipeBookComponent.TabInfo tabInfo;

	private RecipeBookTabButtonMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void cassonade$resize(CallbackInfo ci) {
		setSize(WIDTH, HEIGHT);
	}

	@ModifyArg(method = "extractContents", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"),
			index = 2)
	private int cassonade$stayInLine(int x) {
		return selected ? x + STEP_OUT : x + TUCK_IN;
	}

	@ModifyArg(method = "extractContents", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"),
			index = 4)
	private int cassonade$reachPages(int width) {
		return selected ? SELECTED_WIDTH : width;
	}

	@Inject(method = "extractIcon", at = @At("HEAD"), cancellable = true)
	private void cassonade$engravedIcon(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		String icon = cassonade$icon(tabInfo.category());
		if (icon == null) {
			return;
		}
		ci.cancel();
		Identifier sprite = Cassonade.id("recipe_book/tab_icon/" + icon + (selected ? "_selected" : ""));
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX() + ICON_LEFT + (selected ? 0 : TUCK_IN), getY() + ICON_TOP, ICON_SIZE, ICON_SIZE);
	}

	/** A tab of another mod keeps its items, moved with the tab. */
	@ModifyVariable(method = "extractIcon", at = @At("STORE"), ordinal = 0)
	private int cassonade$iconInLine(int moveLeft) {
		return selected ? 0 : TUCK_IN;
	}

	/** The engraved icon of a vanilla tab, by what it lists; null for the tabs of other mods. */
	@Unique
	private static @Nullable String cassonade$icon(ExtendedRecipeBookCategory category) {
		if (category instanceof SearchRecipeBookCategory) {
			return "search";
		}
		if (category == RecipeBookCategories.CRAFTING_EQUIPMENT || category == RecipeBookCategories.BLAST_FURNACE_MISC) {
			return "equipment";
		}
		if (category == RecipeBookCategories.CRAFTING_BUILDING_BLOCKS || category == RecipeBookCategories.FURNACE_BLOCKS) {
			return "blocks";
		}
		if (category == RecipeBookCategories.BLAST_FURNACE_BLOCKS) {
			return "ores";
		}
		if (category == RecipeBookCategories.CRAFTING_REDSTONE) {
			return "redstone";
		}
		if (category == RecipeBookCategories.FURNACE_FOOD || category == RecipeBookCategories.SMOKER_FOOD) {
			return "food";
		}
		if (category == RecipeBookCategories.CRAFTING_MISC || category == RecipeBookCategories.FURNACE_MISC) {
			return "misc";
		}
		return null;
	}
}
