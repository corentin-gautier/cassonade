package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;

import com.cassonade.crafting.ChainCrafting;

/** Before the recipe book places a recipe, what it is missing is crafted if it can be, see {@link ChainCrafting}. */
@Mixin(AbstractCraftingMenu.class)
public abstract class AbstractCraftingMenuMixin {
	@Inject(method = "handlePlacement", at = @At("HEAD"))
	private void cassonade$craftMissing(boolean useMaxItems, boolean allowDroppingItemsToClear, RecipeHolder<?> recipe, ServerLevel level, Inventory inventory,
			CallbackInfoReturnable<RecipeBookMenu.PostPlaceAction> info) {
		ChainCrafting.prepare((AbstractCraftingMenu) (Object) this, inventory, recipe, level);
	}
}
