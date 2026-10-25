package com.cassonade.client.mixin;

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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

import com.cassonade.client.BackpackPanel;
import com.cassonade.client.PaperEditBox;
import com.cassonade.crafting.CraftAllPayload;
import com.cassonade.gear.GearLayout;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {
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

	/** The search field is written on the page, in ink (tools/recipe_book_textures.py). */
	@Inject(method = "initVisuals", at = @At("TAIL"))
	private void cassonade$searchOnPaper(CallbackInfo ci) {
		((PaperEditBox) searchBox).cassonade$writeOnPaper(SEARCH_HINT);
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
		if (menu instanceof AbstractCraftingMenu && cassonade$craftAllDown() && ClientPlayNetworking.canSend(CraftAllPayload.TYPE)) {
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
