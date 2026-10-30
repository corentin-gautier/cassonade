package com.cassonade.mixin;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;

import com.cassonade.woodcutter.Woodcutter;
import com.cassonade.woodcutter.WoodcutterMenu;

/**
 * The stonecutter and the woodcutter share a menu and the stonecutting recipes: a plank is for the woodcutter
 * only, anything else for the stonecutter only. Both sides run it, the client having only each recipe's input.
 * The server also remembers the piece picked and picks it again for the next stack, or the same shape in another material.
 */
@Mixin(StonecutterMenu.class)
public abstract class StonecutterMenuMixin {
	@Shadow
	@Final
	private DataSlot selectedRecipeIndex;
	@Shadow
	@Final
	private Level level;
	@Shadow
	@Final
	public Container container;
	@Shadow
	private SelectableRecipe.SingleInputSet<StonecutterRecipe> recipesForInput;

	@Unique
	private Item cassonade$picked;

	@Shadow
	private void setupResultSlot(int index) {
	}

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

	@Inject(method = "clickMenuButton", at = @At("RETURN"))
	private void cassonade$rememberPiece(Player player, int buttonId, CallbackInfoReturnable<Boolean> info) {
		if (!level.isClientSide() && selectedRecipeIndex.get() == buttonId && buttonId >= 0) {
			cassonade$picked = cassonade$result(recipesForInput.entries().get(buttonId), container.getItem(0)).orElse(null);
		}
	}

	@Inject(method = "setupRecipeList", at = @At("TAIL"))
	private void cassonade$pickAgain(ItemStack input, CallbackInfo info) {
		if (level.isClientSide() || cassonade$picked == null || input.isEmpty()) {
			return;
		}
		int index = cassonade$indexOfPiece(input);
		if (index >= 0) {
			selectedRecipeIndex.set(index);
			// The client reset its selection on its own when the stack changed, whatever the server last told it
			((AbstractContainerMenuAccessor) this).cassonade$getRemoteDataSlots().set(0, -1);
			setupResultSlot(index);
		}
	}

	/** The piece picked itself if this input gives it, or else the closest piece of the same shape (stairs, slab...). */
	@Unique
	private int cassonade$indexOfPiece(ItemStack input) {
		List<SelectableRecipe.SingleInputEntry<StonecutterRecipe>> entries = recipesForInput.entries();
		List<Item> results = entries.stream().map(entry -> cassonade$result(entry, input).orElse(null)).toList();
		if (results.contains(cassonade$picked)) {
			return results.indexOf(cassonade$picked);
		}
		String shape = cassonade$shape(cassonade$picked);
		// The shortest name is the plainest piece: stone stairs rather than stone brick stairs
		return results.stream()
				.filter(result -> result != null && cassonade$shape(result).equals(shape))
				.min(Comparator.comparingInt(result -> BuiltInRegistries.ITEM.getKey(result).getPath().length()))
				.map(results::indexOf)
				.orElse(-1);
	}

	@Unique
	private static Optional<Item> cassonade$result(SelectableRecipe.SingleInputEntry<StonecutterRecipe> entry, ItemStack input) {
		return entry.recipe().recipe().map(recipe -> recipe.value().assemble(new SingleRecipeInput(input)).getItem());
	}

	/** The last word of an item's name: oak_stairs and spruce_stairs are both stairs. */
	@Unique
	private static String cassonade$shape(Item item) {
		String path = BuiltInRegistries.ITEM.getKey(item).getPath();
		return path.substring(path.lastIndexOf('_') + 1);
	}

	private boolean cassonade$handles(ItemStack input) {
		return Woodcutter.isWoodInput(input) == (((Object) this) instanceof WoodcutterMenu);
	}
}
