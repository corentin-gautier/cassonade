package com.cassonade.crafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * A recipe picked in the recipe book while missing what can be crafted first (planks, then sticks): those steps are
 * made from the inventory, then the game places the recipe as usual. Only recipes the player knows and the grid at hand
 * can make. It is worked out on counts first, so nothing is used up unless the whole chain succeeds, and a recipe still
 * out of reach shows its ghost as in vanilla.
 */
public final class ChainCrafting {
	/** Log, planks, stick, and a step on top: longer chains mean a recipe the player has probably not thought of. */
	private static final int MAX_DEPTH = 4;
	/** Items tried in all: a tag-heavy recipe tree must not stall the server tick on a click. */
	private static final int MAX_TRIES = 2000;

	private final ServerPlayer player;
	private final Map<Item, List<RecipeHolder<?>>> recipes = new HashMap<>();
	private final List<Step> steps = new ArrayList<>();
	private Map<Item, Integer> inventory = new HashMap<>();
	private Map<Item, Integer> grid = new HashMap<>();
	private int tries;

	private record Step(ItemStack result, Map<Item, Integer> used) {
	}

	private ChainCrafting(ServerPlayer player) {
		this.player = player;
	}

	/** Called before the game places a recipe. */
	public static void prepare(AbstractCraftingMenu menu, Inventory inventory, RecipeHolder<?> recipe, ServerLevel level) {
		if (!(inventory.player instanceof ServerPlayer player) || !(recipe.value() instanceof CraftingRecipe target)) {
			return;
		}
		StackedItemContents available = new StackedItemContents();
		inventory.fillStackedContents(available);
		menu.fillCraftSlotsStackedContents(available);
		if (available.canCraft(target, null)) {
			return;
		}
		ChainCrafting chain = new ChainCrafting(player);
		chain.read(menu, inventory, level);
		if (chain.satisfyAll(target, 0, new HashSet<>(), true)) {
			chain.apply(inventory);
		}
	}

	private void read(AbstractCraftingMenu menu, Inventory playerInventory, ServerLevel level) {
		for (ItemStack stack : playerInventory.getNonEquipmentItems()) {
			if (!stack.isEmpty() && Inventory.isUsableForCrafting(stack)) {
				inventory.merge(stack.getItem(), stack.getCount(), Integer::sum);
			}
		}
		for (Slot slot : menu.getInputGridSlots()) {
			ItemStack stack = slot.getItem();
			if (!stack.isEmpty() && Inventory.isUsableForCrafting(stack)) {
				grid.merge(stack.getItem(), stack.getCount(), Integer::sum);
			}
		}
		for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
			ItemStack result = stepResult(holder.value());
			if (result != null && player.getRecipeBook().contains(holder.id()) && fits(holder.value(), menu)) {
				recipes.computeIfAbsent(result.getItem(), item -> new ArrayList<>()).add(holder);
			}
		}
	}

	/** Only plain recipes, whose result doesn't depend on what goes in: dyeing or repairing has no place in a chain. */
	private static @Nullable ItemStack stepResult(Recipe<?> recipe) {
		return recipe instanceof ShapedRecipe shaped ? shaped.assemble(CraftingInput.EMPTY)
				: recipe instanceof ShapelessRecipe shapeless ? shapeless.assemble(CraftingInput.EMPTY) : null;
	}

	private static boolean fits(Recipe<?> recipe, AbstractCraftingMenu menu) {
		return recipe instanceof ShapedRecipe shaped ? shaped.getWidth() <= menu.getGridWidth() && shaped.getHeight() <= menu.getGridHeight()
				: recipe.placementInfo().ingredients().size() <= menu.getGridWidth() * menu.getGridHeight();
	}

	/** Takes every ingredient of a recipe off the counts, adding the steps it takes to have them. */
	private boolean satisfyAll(Recipe<?> recipe, int depth, Set<Item> making, boolean placed) {
		for (Ingredient ingredient : recipe.placementInfo().ingredients()) {
			if (!satisfy(ingredient, depth, making, placed)) {
				return false;
			}
		}
		return true;
	}

	private boolean satisfy(Ingredient ingredient, int depth, Set<Item> making, boolean placed) {
		List<Holder<Item>> items = ingredient.items().toList();
		for (Holder<Item> holder : items) {
			Item item = holder.value();
			// The grid only serves the recipe itself: the game moves it back and forth, the steps below use the inventory
			if (placed && grid.getOrDefault(item, 0) > 0) {
				grid.merge(item, -1, Integer::sum);
				return true;
			}
			if (inventory.getOrDefault(item, 0) > 0 && (placed || item.getCraftingRemainder() == null)) {
				inventory.merge(item, -1, Integer::sum);
				return true;
			}
		}
		if (depth >= MAX_DEPTH) {
			return false;
		}
		for (Holder<Item> holder : items) {
			Item item = holder.value();
			if (!making.contains(item) && craft(item, depth, making)) {
				inventory.merge(item, -1, Integer::sum);
				return true;
			}
		}
		return false;
	}

	/** One step making this item, its result added to the counts. */
	private boolean craft(Item item, int depth, Set<Item> making) {
		for (RecipeHolder<?> holder : recipes.getOrDefault(item, List.of())) {
			if (++tries > MAX_TRIES) {
				return false;
			}
			Map<Item, Integer> inventoryBefore = new HashMap<>(inventory);
			Map<Item, Integer> gridBefore = new HashMap<>(grid);
			int stepsBefore = steps.size();
			making.add(item);
			boolean made = satisfyAll(holder.value(), depth + 1, making, false);
			making.remove(item);
			if (made) {
				ItemStack result = stepResult(holder.value());
				inventory.merge(item, result.getCount(), Integer::sum);
				Map<Item, Integer> used = new HashMap<>();
				for (Map.Entry<Item, Integer> before : inventoryBefore.entrySet()) {
					// Steps below this one already took what they used, and left their results
					int spent = before.getValue() - inventory.getOrDefault(before.getKey(), 0);
					if (before.getKey() != item && spent > 0) {
						used.put(before.getKey(), spent);
					}
				}
				steps.add(new Step(result, used));
				return true;
			}
			inventory = inventoryBefore;
			grid = gridBefore;
			steps.subList(stepsBefore, steps.size()).clear();
		}
		return false;
	}

	private void apply(Inventory playerInventory) {
		for (Step step : steps) {
			step.used().forEach((item, count) -> take(playerInventory, item, count));
		}
		for (Step step : steps) {
			ItemStack result = step.result().copy();
			Item item = result.getItem();
			int count = result.getCount();
			result.onCraftedBy(player, count);
			player.awardStat(Stats.ITEM_CRAFTED.get(item), count);
			// The ingredients gone, it nearly always fits; what doesn't goes on the ground rather than vanish
			playerInventory.placeItemBackInInventory(result, Prediction.SERVER_ONLY);
		}
	}

	private static void take(Inventory playerInventory, Item item, int count) {
		List<ItemStack> stacks = playerInventory.getNonEquipmentItems();
		for (int i = 0; i < stacks.size() && count > 0; i++) {
			ItemStack stack = stacks.get(i);
			if (stack.is(item) && Inventory.isUsableForCrafting(stack)) {
				int taken = Math.min(count, stack.getCount());
				stack.shrink(taken);
				count -= taken;
			}
		}
		playerInventory.setChanged();
	}
}
