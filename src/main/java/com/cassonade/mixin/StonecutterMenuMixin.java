package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import com.cassonade.woodcutter.Woodcutter;
import com.cassonade.woodcutter.WoodcutterMenu;

/**
 * The stonecutter and the woodcutter share a menu and the stonecutting recipes: a plank is for the woodcutter
 * only, anything else for the stonecutter only. Both sides run it, the client having only each recipe's input.
 */
@Mixin(StonecutterMenu.class)
public abstract class StonecutterMenuMixin {
	@Redirect(method = "setupRecipeList", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/crafting/SelectableRecipe$SingleInputSet;selectByInput(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/crafting/SelectableRecipe$SingleInputSet;"))
	private SelectableRecipe.SingleInputSet<StonecutterRecipe> cassonade$selectOwn(
			SelectableRecipe.SingleInputSet<StonecutterRecipe> recipes, ItemStack input) {
		return cassonade$handles(input) ? recipes.selectByInput(input) : SelectableRecipe.SingleInputSet.empty();
	}

	@Redirect(method = "quickMoveStack", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/crafting/SelectableRecipe$SingleInputSet;acceptsInput(Lnet/minecraft/world/item/ItemStack;)Z"))
	private boolean cassonade$acceptsOwn(SelectableRecipe.SingleInputSet<StonecutterRecipe> recipes, ItemStack input) {
		return cassonade$handles(input) && recipes.acceptsInput(input);
	}

	private boolean cassonade$handles(ItemStack input) {
		return Woodcutter.isWoodInput(input) == (((Object) this) instanceof WoodcutterMenu);
	}
}
