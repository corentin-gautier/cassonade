package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.cassonade.client.RecipeUses;
import com.cassonade.client.mixin.AbstractRecipeBookScreenAccessor;
import com.cassonade.test.mixin.RecipeBookComponentPageAccessor;
import com.cassonade.test.mixin.RecipeBookPageAccessor;

/** The uses key narrows the recipe book to the recipes asking for the item, and the same item again brings it all back. */
public class RecipeUsesClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			world.getServer().runCommand("recipe give @a *");
			context.waitTicks(5);
			context.getInput().resizeWindow(1920, 1080);
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitTicks(2);
			ItemStack stick = new ItemStack(Items.STICK);
			context.runOnClient(client -> uses(client).cassonade$showUses(stick));
			context.waitTicks(2);
			List<RecipeCollection> shown = context.computeOnClient(RecipeUsesClientGameTest::shown);
			check(!shown.isEmpty(), "recipes using sticks are shown");
			check(shown.stream().allMatch(collection -> collection.getRecipes().stream().anyMatch(recipe -> recipe.craftingRequirements()
					.map(ingredients -> ingredients.stream().anyMatch(ingredient -> ingredient.test(stick))).orElse(false))), "only recipes using sticks");
			int narrowed = shown.size();
			// A long name, cut short in the search field
			context.runOnClient(client -> uses(client).cassonade$showUses(new ItemStack(Items.CHERRY_PLANKS)));
			Screenshots.take(context, "cassonade-recipe-uses");
			context.runOnClient(client -> uses(client).cassonade$showUses(stick));
			context.waitTicks(2);

			context.runOnClient(client -> uses(client).cassonade$showUses(stick));
			context.waitTicks(2);
			check(context.computeOnClient(client -> uses(client).cassonade$uses().isEmpty()), "the same item again brings the whole book back");
			check(context.computeOnClient(RecipeUsesClientGameTest::shown).size() > narrowed, "more recipes once back");
			context.setScreen(() -> null);
		}
	}

	private static RecipeBookComponent<?> book(Minecraft client) {
		return ((AbstractRecipeBookScreenAccessor) client.gui.screen()).cassonade$getRecipeBookComponent();
	}

	private static RecipeUses uses(Minecraft client) {
		return (RecipeUses) book(client);
	}

	private static List<RecipeCollection> shown(Minecraft client) {
		return ((RecipeBookPageAccessor) ((RecipeBookComponentPageAccessor) book(client)).cassonade$getRecipeBookPage()).cassonade$getRecipeCollections();
	}
}
