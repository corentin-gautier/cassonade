package com.cassonade.test;

import java.lang.reflect.Method;
import java.util.function.Function;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import com.cassonade.client.mixin.AbstractRecipeBookScreenAccessor;
import com.cassonade.client.mixin.ImageButtonAccessor;
import com.cassonade.client.mixin.RecipeBookComponentAccessor;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.gear.GearLayout;

/**
 * Windows in the mod's style: every container screen, the enchanting table's book open, the crafting table's
 * recipe book with its search and variants, the creative inventory.
 */
public class ScreensClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try {
			run(context);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private void run(ClientGameTestContext context) throws ReflectiveOperationException {
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");
			// Container windows in the mod's style, opened on client-side menus just to be seen
			themedScreen(context, "crafting", inventory -> new CraftingScreen(new CraftingMenu(100, inventory), inventory,
					Component.translatable("container.crafting")));
			themedScreen(context, "furnace", inventory -> new FurnaceScreen(new FurnaceMenu(100, inventory), inventory,
					Component.translatable("container.furnace")));
			themedScreen(context, "chest", inventory -> new ContainerScreen(ChestMenu.sixRows(100, inventory), inventory,
					Component.translatable("container.chestDouble")));
			themedScreen(context, "barrel", inventory -> new ContainerScreen(ChestMenu.threeRows(100, inventory), inventory,
					Component.translatable("container.barrel")));
			themedScreen(context, "ender-chest", inventory -> new ContainerScreen(ChestMenu.threeRows(100, inventory), inventory,
					Component.translatable("container.enderchest")));
			themedScreen(context, "cartography", inventory -> new CartographyTableScreen(new CartographyTableMenu(100, inventory), inventory,
					Component.translatable("container.cartography_table")));
			themedScreen(context, "enchanting", inventory -> new EnchantmentScreen(new EnchantmentMenu(100, inventory), inventory,
					Component.translatable("container.enchant")));
			// A sword alone, nothing to make of it: its arrow crossed out
			themedScreen(context, "anvil", inventory -> {
				AnvilMenu anvil = new AnvilMenu(100, inventory);
				anvil.getSlot(0).set(new ItemStack(Items.IRON_SWORD));
				return new AnvilScreen(anvil, inventory, Component.translatable("container.repair"));
			});
			// A worn sword and an ingot to mend it: the cost in its tooltip
			themedScreen(context, "anvil-cost", inventory -> {
				AnvilMenu anvil = new AnvilMenu(100, inventory);
				ItemStack sword = new ItemStack(Items.IRON_SWORD);
				sword.setDamageValue(100);
				anvil.getSlot(0).set(sword);
				anvil.getSlot(1).set(new ItemStack(Items.IRON_INGOT));
				return new AnvilScreen(anvil, inventory, Component.translatable("container.repair"));
			});
			themedScreen(context, "beacon", inventory -> new BeaconScreen(new BeaconMenu(100, inventory), inventory,
					Component.translatable("container.beacon")));
			themedScreen(context, "shulker", inventory -> new ShulkerBoxScreen(new ShulkerBoxMenu(100, inventory), inventory,
					Component.translatable("container.shulkerBox")));
			themedScreen(context, "blast-furnace", inventory -> new BlastFurnaceScreen(new BlastFurnaceMenu(100, inventory), inventory,
					Component.translatable("container.blast_furnace")));
			themedScreen(context, "smoker", inventory -> new SmokerScreen(new SmokerMenu(100, inventory), inventory,
					Component.translatable("container.smoker")));
			themedScreen(context, "crafter", inventory -> new CrafterScreen(new CrafterMenu(100, inventory), inventory,
					Component.translatable("container.crafter")));
			themedScreen(context, "dispenser", inventory -> new DispenserScreen(new DispenserMenu(100, inventory), inventory,
					Component.translatable("container.dispenser")));
			// Stone, nothing to grind off: its arrow crossed out
			themedScreen(context, "grindstone", inventory -> {
				GrindstoneMenu grindstone = new GrindstoneMenu(100, inventory);
				grindstone.getSlot(0).set(new ItemStack(Items.STONE));
				return new GrindstoneScreen(grindstone, inventory, Component.translatable("container.grindstone_title"));
			});
			themedScreen(context, "smithing", inventory -> new SmithingScreen(new SmithingMenu(100, inventory), inventory,
					Component.translatable("container.upgrade")));
			themedScreen(context, "stonecutter", inventory -> new StonecutterScreen(new StonecutterMenu(100, inventory), inventory,
					Component.translatable("container.stonecutter")));
			themedScreen(context, "loom", inventory -> new LoomScreen(new LoomMenu(100, inventory), inventory,
					Component.translatable("container.loom")));
			themedScreen(context, "brewing", inventory -> new BrewingStandScreen(new BrewingStandMenu(100, inventory), inventory,
					Component.translatable("container.brewing")));
			themedScreen(context, "hopper", inventory -> new HopperScreen(new HopperMenu(100, inventory), inventory,
					Component.translatable("container.hopper")));
			// Enough trades to scroll, the selected one out of stock: its arrow crossed out
			themedScreen(context, "villager", inventory -> {
				MerchantMenu menu = new MerchantMenu(100, inventory);
				MerchantOffers offers = new MerchantOffers();
				for (int i = 0; i < 9; i++) {
					offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, i + 1), new ItemStack(Items.BREAD), 12, 2, 0.05f));
				}
				offers.get(0).setToOutOfStock();
				menu.setOffers(offers);
				return new MerchantScreen(menu, inventory, Component.translatable("entity.minecraft.villager"));
			});
			// A donkey with a chest: the saddle and armor slots, and the chest's
			themedScreen(context, "donkey", inventory -> {
				Donkey donkey = EntityTypes.DONKEY.create(inventory.player.level(), EntitySpawnReason.LOAD);
				// Never added to the level: the screen still reads its ID
				donkey.setId(-1);
				donkey.setChest(true);
				// Tamed, or it has no saddle slot
				donkey.setTamed(true);
				int columns = donkey.getInventoryColumns();
				HorseInventoryMenu menu = new HorseInventoryMenu(100, inventory,
						new SimpleContainer(AbstractMountInventoryMenu.getInventorySize(columns)), donkey, columns);
				return new HorseInventoryScreen(menu, inventory, donkey, columns);
			});

			// The enchanting table's book opens when an item can be enchanted; with enough lapis and levels
			// for the first option only, both states of the options show
			world.getServer().runCommand("xp set @a 10 levels");
			context.waitTicks(2);
			context.runOnClient(client -> {
				EnchantmentMenu menu = new EnchantmentMenu(100, client.player.getInventory());
				menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 2));
				menu.costs[0] = 3;
				menu.costs[1] = 12;
				client.gui.setScreen(new EnchantmentScreen(menu, client.player.getInventory(), Component.translatable("container.enchant")));
			});
			context.waitTicks(10);
			ScreenInput.quiet(context);
			Screenshots.take(context, "cassonade-theme-enchanting-open");

			// The crafting table's recipe book: its button is the bare book, its search written on the
			// page, and a right click on the planks shows all their recipes
			world.getServer().runCommand("recipe give @a *");
			context.runOnClient(client -> client.gui.setScreen(new CraftingScreen(new CraftingMenu(100, client.player.getInventory()),
					client.player.getInventory(), Component.translatable("container.crafting"))));
			context.waitTicks(2);
			ScreenInput.click(context, client -> recipeButton(client).getX() + 10, client -> recipeButton(client).getY() + 9);
			check(context.computeOnClient(client -> ((AbstractRecipeBookScreenAccessor) client.gui.screen())
					.cassonade$getRecipeBookComponent().isVisible()), "the bare book opens the crafting table's recipe book");
			// Its ribbon closes it, as in the inventory
			ScreenInput.click(context, client -> recipeBookX(client) + GearLayout.RIBBON_X + GearLayout.RIBBON_WIDTH / 2,
					client -> recipeBookY(client) + GearLayout.RIBBON_Y + 3);
			check(!context.computeOnClient(client -> ((AbstractRecipeBookScreenAccessor) client.gui.screen())
					.cassonade$getRecipeBookComponent().isVisible()), "the ribbon closes the crafting table's recipe book");
			ScreenInput.click(context, client -> recipeButton(client).getX() + 10, client -> recipeButton(client).getY() + 9);
			ScreenInput.click(context, client -> recipeBookX(client) + 40, client -> recipeBookY(client) + 18);
			ScreenInput.typeOnScreen(context, "planks");
			context.waitTicks(2);
			ScreenInput.quiet(context);
			Screenshots.take(context, "cassonade-17-recipe-book-search");
			context.runOnClient(client -> {
				MouseButtonInfo right = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0);
				MouseButtonEvent event = new MouseButtonEvent(recipeBookX(client) + 11 + 12, recipeBookY(client) + 31 + 12, right);
				client.gui.screen().mouseClicked(event, false);
				client.gui.screen().mouseReleased(event);
			});
			context.waitTicks(2);
			// Hovering the second variant details it beside the popup
			double[] card = context.computeOnClient(client -> new double[] {
					(recipeBookX(client) + 11 + 4 + 25 + 12) * client.getWindow().getGuiScale(),
					(recipeBookY(client) + 31 + 5 + 12) * client.getWindow().getGuiScale()});
			context.getInput().setCursorPos(card[0], card[1]);
			context.waitTicks(2);
			ScreenInput.quiet(context);
			Screenshots.take(context, "cassonade-17-recipe-book-variants");

			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode creative @a");
			context.waitTicks(5);
			context.runOnClient(client -> client.gui.setScreen(new CreativeModeInventoryScreen(client.player,
					client.player.connection.enabledFeatures(), false)));
			context.waitForScreen(CreativeModeInventoryScreen.class);
			context.<ReflectiveOperationException>runOnClient(client -> {
				// selectTab is private; the test only needs to land on the survival inventory tab
				Method selectTab = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
				selectTab.setAccessible(true);
				selectTab.invoke(client.gui.screen(), BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.INVENTORY));
			});
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-12-creative-inventory");
		}
	}

	private static void themedScreen(ClientGameTestContext context, String name, Function<Inventory, Screen> screen) {
		context.runOnClient(client -> client.gui.setScreen(screen.apply(client.player.getInventory())));
		context.waitTicks(2);
		ScreenInput.quiet(context);
		Screenshots.take(context, "cassonade-theme-" + name);
	}

	/** The button opening the recipe book of the screen open. */
	private static ImageButton recipeButton(Minecraft client) {
		return client.gui.screen().children().stream()
				.filter(widget -> widget instanceof ImageButton button
						&& ((ImageButtonAccessor) button).cassonade$getSprites() == RecipeBookComponent.RECIPE_BUTTON_SPRITES)
				.map(ImageButton.class::cast).findFirst().orElseThrow();
	}

	private static int recipeBookX(Minecraft client) {
		return ((RecipeBookComponentAccessor) ((AbstractRecipeBookScreenAccessor) client.gui.screen()).cassonade$getRecipeBookComponent())
				.cassonade$getXOrigin();
	}

	private static int recipeBookY(Minecraft client) {
		return ((RecipeBookComponentAccessor) ((AbstractRecipeBookScreenAccessor) client.gui.screen()).cassonade$getRecipeBookComponent())
				.cassonade$getYOrigin();
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
