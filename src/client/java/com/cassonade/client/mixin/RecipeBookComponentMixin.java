package com.cassonade.client.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

import com.cassonade.client.BackpackPanel;
import com.cassonade.client.PaperEditBox;
import com.cassonade.client.RecipeUses;
import com.cassonade.crafting.CraftAllPayload;
import com.cassonade.gear.GearLayout;
import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin implements RecipeUses {
	/**
	 * The tabs start below the search field, level with the first row of recipes, 21 pixels apart
	 * rather than vanilla's 27 from the cover's top edge. They stand out less from the book, 24 pixels
	 * rather than 30, and are shorter (RecipeBookTabButtonMixin).
	 */
	@Unique
	private static final int TABS_DOWN = 28;
	@Unique
	private static final int TABS_IN = 6;
	@Unique
	private static final int TAB_STEP = 21;

	@Shadow
	@Final
	private static Component SEARCH_HINT;

	@Shadow
	@Final
	protected RecipeBookMenu menu;

	@Shadow
	private @Nullable EditBox searchBox;

	@Shadow
	@Final
	private List<RecipeBookTabButton> tabButtons;

	@Shadow
	private @Nullable RecipeBookTabButton selectedTab;

	@Unique
	private ItemStack cassonade$uses = ItemStack.EMPTY;

	@Shadow
	public abstract boolean isVisible();

	@Shadow
	protected abstract void setVisible(boolean visible);

	@Shadow
	private void updateCollections(boolean resetPage, boolean isFiltering) {
		throw new AssertionError();
	}

	@Shadow
	private boolean isFiltering() {
		throw new AssertionError();
	}

	@Shadow
	private void replaceSelected(RecipeBookTabButton tab) {
		throw new AssertionError();
	}

	/** The search field is written on the page, in ink (tools/recipe_book_textures.py), naming the item whose uses are shown. */
	@Inject(method = "initVisuals", at = @At("TAIL"))
	private void cassonade$searchOnPaper(CallbackInfo ci) {
		cassonade$writeHint();
	}

	/** The item's name alone, cut short to stay within the field: the player just asked for its uses. */
	@Unique
	private void cassonade$writeHint() {
		Component hint = SEARCH_HINT;
		if (!cassonade$uses.isEmpty()) {
			Font font = Minecraft.getInstance().font;
			String name = cassonade$uses.getHoverName().getString();
			int width = searchBox.getInnerWidth();
			hint = Component.literal(font.width(name) <= width ? name : font.plainSubstrByWidth(name, width - font.width("…")) + "…");
		}
		((PaperEditBox) searchBox).cassonade$writeOnPaper(hint);
	}

	@Override
	public void cassonade$showUses(ItemStack stack) {
		boolean same = !stack.isEmpty() && ItemStack.isSameItem(stack, cassonade$uses);
		cassonade$uses = same ? ItemStack.EMPTY : stack.copyWithCount(1);
		if (!isVisible()) {
			setVisible(true);
		}
		if (!cassonade$uses.isEmpty()) {
			// Every tab's recipes, and no search over them
			searchBox.setValue("");
			if (selectedTab != tabButtons.getFirst()) {
				replaceSelected(tabButtons.getFirst());
			}
		}
		cassonade$writeHint();
		updateCollections(true, isFiltering());
	}

	@Override
	public ItemStack cassonade$uses() {
		return cassonade$uses;
	}

	/** Only the recipes asking for the item, among those the tab, the search and the filter leave. */
	@WrapOperation(method = "updateCollections", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookPage;updateCollections(Ljava/util/List;ZZ)V"))
	private void cassonade$onlyUses(RecipeBookPage page, List<RecipeCollection> collections, boolean resetPage, boolean isFiltering, Operation<Void> original) {
		if (!cassonade$uses.isEmpty()) {
			ItemStack uses = cassonade$uses;
			collections.removeIf(collection -> collection.getRecipes().stream().noneMatch(recipe -> recipe.craftingRequirements()
					.map(ingredients -> ingredients.stream().anyMatch(ingredient -> ingredient.test(uses))).orElse(false)));
		}
		original.call(page, collections, resetPage, isFiltering);
	}

	/** Typing a search, or closing the book, brings the whole book back. */
	@Inject(method = "checkSearchStringUpdate", at = @At("HEAD"))
	private void cassonade$searchInstead(CallbackInfo ci) {
		if (!cassonade$uses.isEmpty() && !searchBox.getValue().isEmpty()) {
			cassonade$uses = ItemStack.EMPTY;
			cassonade$writeHint();
		}
	}

	@Inject(method = "setVisible", at = @At("HEAD"))
	private void cassonade$forgetUses(boolean visible, CallbackInfo ci) {
		if (!visible) {
			cassonade$uses = ItemStack.EMPTY;
		}
	}

	// The first int stored is the tabs' x, the second their y
	@ModifyVariable(method = "updateTabs", at = @At("STORE"), ordinal = 0)
	private int cassonade$closerTabs(int x) {
		return x + TABS_IN;
	}

	@ModifyVariable(method = "updateTabs", at = @At("STORE"), ordinal = 1)
	private int cassonade$lowerTabs(int y) {
		return y + TABS_DOWN;
	}

	@ModifyConstant(method = "updateTabs", constant = @Constant(intValue = 27))
	private int cassonade$tabPerRow(int step) {
		return TAB_STEP;
	}

	/** The inventory screen asks the recipe book where to sit: with the backpack open, make room for it on the left. */
	@Inject(method = "updateScreenPosition", at = @At("RETURN"), cancellable = true)
	private void cassonade$makeRoomForBackpack(int width, int imageWidth, CallbackInfoReturnable<Integer> cir) {
		if (menu instanceof InventoryMenu && BackpackPanel.isOpen()) {
			cir.setReturnValue(cir.getReturnValueI() + GearLayout.PANEL_SHIFT);
		}
	}

	/** A control click (command on a Mac) on a recipe crafts it as many times as the inventory allows, see {@link CraftAllPayload}. */
	@WrapOperation(method = "tryPlaceRecipe", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handlePlaceRecipe(ILnet/minecraft/world/item/crafting/display/RecipeDisplayId;Z)V"))
	private void cassonade$craftAll(MultiPlayerGameMode gameMode, int containerId, RecipeDisplayId recipe, boolean useMaxItems, Operation<Void> original) {
		if (menu instanceof AbstractCraftingMenu && cassonade$craftAllDown() && ClientPlayNetworking.canSend(CraftAllPayload.TYPE)
				&& Settings.on(Setting.BULK_CRAFTING)) {
			ClientPlayNetworking.send(new CraftAllPayload(containerId, recipe));
		} else {
			original.call(gameMode, containerId, recipe, useMaxItems);
		}
	}

	@Unique
	private static boolean cassonade$craftAllDown() {
		// Control, or command (left and right super keys)
		return Minecraft.getInstance().hasControlDown() || InputConstants.isKeyDown(227) || InputConstants.isKeyDown(231);
	}
}
