package com.vanillasugar.test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

import com.vanillasugar.client.BackpackPanel;
import com.vanillasugar.client.ClientSettings;
import com.vanillasugar.client.VanillaSugarClient;
import com.vanillasugar.client.mixin.AbstractRecipeBookScreenAccessor;
import com.vanillasugar.client.map.Minimap;
import com.vanillasugar.client.map.WorldMapScreen;
import com.vanillasugar.compass.PlayerMarkers;
import com.vanillasugar.gear.GearLayout;
import com.vanillasugar.gear.GearSlot;
import com.vanillasugar.gear.PlayerGear;
import com.vanillasugar.item.VanillaSugarItems;

public class GearClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try {
			run(context);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private void run(ClientGameTestContext context) throws ReflectiveOperationException {
		// A common player's window, 1080p with the automatic GUI scale, rather than the small default
		context.getInput().resizeWindow(1920, 1080);
		TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("give @a vanillasugar:backpack");
			world.getServer().runCommand("give @a compass");
			world.getServer().runCommand("give @a map");
			world.getServer().runCommand("give @a diamond 64");
			world.getServer().runCommand("give @a oak_planks 64");
			world.getServer().runCommand("give @a diamond_chestplate");
			context.waitTicks(5);

			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			context.takeScreenshot("vanillasugar-1-inventory");

			// Hotbar 0..2: backpack, compass, map -> shift-click equips them
			for (int hotbar = 0; hotbar < 3; hotbar++) {
				quickMove(context, InventoryMenu.USE_ROW_SLOT_START + hotbar);
			}
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			context.takeScreenshot("vanillasugar-2-gear-equipped");

			// Backpack closed: diamonds move from the hotbar to the main inventory, like vanilla
			quickMove(context, InventoryMenu.USE_ROW_SLOT_START + 3);
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(PlayerGear.of(player).backpack().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream()
						.findAny().isEmpty(), "closed backpack left empty");
				check(player.getInventory().getItem(InventoryMenu.INV_SLOT_START).is(Items.DIAMOND), "diamonds moved to the main inventory");
			});

			// Backpack open: diamonds, planks go into it, the chestplate still goes to the armor slot
			context.runOnClient(client -> BackpackPanel.setOpen(true));
			context.waitTicks(2);
			quickMove(context, InventoryMenu.INV_SLOT_START);
			for (int hotbar = 4; hotbar < 6; hotbar++) {
				quickMove(context, InventoryMenu.USE_ROW_SLOT_START + hotbar);
			}
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			context.runOnClient(client -> client.gui.toastManager().clear());
			context.waitTicks(1);
			context.takeScreenshot("vanillasugar-3-backpack-filled");

			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				PlayerGear gear = PlayerGear.of(player);
				check(gear.compass().is(Items.COMPASS), "compass equipped");
				check(gear.map().is(Items.MAP), "map equipped");
				ItemContainerContents contents = gear.backpack().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
				check(contents.nonEmptyItemCopyStream().anyMatch(stack -> stack.is(Items.DIAMOND) && stack.getCount() == 64), "diamonds in backpack");
				check(contents.nonEmptyItemCopyStream().anyMatch(stack -> stack.is(Items.OAK_PLANKS)), "planks in backpack");
				check(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE), "chestplate worn");
			});


			// Compass: minimap on the HUD
			context.setScreen(() -> null);
			world.getServer().runCommand("effect give @a speed 60");
			// Some relief on the flat test world so the minimap has something to show
			world.getServer().runCommand("execute as @a at @s run fill ~4 ~-1 ~-12 ~16 ~-1 ~-2 water");
			world.getServer().runCommand("execute as @a at @s run fill ~-14 ~ ~2 ~-6 ~3 ~8 stone");
			world.getServer().runCommand("execute as @a at @s run fill ~-12 ~4 ~4 ~-8 ~5 ~6 stone");
			context.waitTicks(5);
			context.takeScreenshot("vanillasugar-4-minimap");

			// Turning with the player: looking west, west is up and north on the right
			context.runOnClient(client -> ClientSettings.setMinimapRotates(true));
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ 90 0");
			context.waitTicks(5);
			context.takeScreenshot("vanillasugar-4-minimap-rotating");
			context.runOnClient(client -> ClientSettings.setMinimapRotates(false));
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 0");

			// Clock anywhere in the inventory: time, weather, day and forecast in the top left corner
			world.getServer().runCommand("item replace entity @a inventory.20 with clock");
			world.getServer().runCommand("gamerule advance_weather true");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather rain 3000");
			context.waitTicks(40);
			context.takeScreenshot("vanillasugar-4-clock-rain");
			world.getServer().runCommand("time set 16000");
			world.getServer().runCommand("weather clear 1000");
			context.waitTicks(40);
			context.takeScreenshot("vanillasugar-4-clock-night");
			world.getServer().runCommand("clear @a clock");
			world.getServer().runCommand("time set 6000");
			context.runOnClient(client -> {
				Minimap.zoomOut();
				Minimap.zoomOut();
			});
			context.waitTicks(5);
			context.takeScreenshot("vanillasugar-5-minimap-zoomed-out");
			context.runOnClient(client -> {
				Minimap.zoomIn();
				Minimap.zoomIn();
			});

			// Map: explore a strip to the east, then open the world map back home
			for (int step = 0; step < 3; step++) {
				world.getServer().runCommand("execute as @a at @s run tp @s ~64 ~ ~");
				world.getConnection().waitForChunksRender();
				context.waitTicks(30);
			}
			world.getServer().runCommand("execute as @a at @s run tp @s ~-192 ~ ~");
			world.getConnection().waitForChunksRender();
			context.waitTicks(10);
			context.setScreen(WorldMapScreen::new);
			context.waitForScreen(WorldMapScreen.class);
			context.waitTicks(2);
			context.takeScreenshot("vanillasugar-6-world-map");

			// Right click 40 GUI pixels right of the player (40 blocks at the default zoom), name the marker, pick blue, Enter
			rightClickMap(context, 40);
			typeOnScreen(context, "Maison");
			// The swatches sit under the name box, centered under the click: blue is the second one
			context.runOnClient(client -> client.gui.screen().mouseClicked(new MouseButtonEvent(
					client.getWindow().getGuiScaledWidth() / 2.0 + 40 - 80 + 15, client.getWindow().getGuiScaledHeight() / 2.0 - 2 + 10 + 20 + 5,
					new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false));
			context.waitTicks(1);
			context.takeScreenshot("vanillasugar-7-world-map-naming");
			pressOnScreen(context, InputConstants.KEY_RETURN);
			world.getConnection().waitForServerboundPackets();
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				var markers = PlayerMarkers.of(server.getPlayerList().getPlayers().getFirst()).markers();
				check(markers.size() == 1 && markers.getFirst().name().equals("Maison") && markers.getFirst().color() == DyeColor.BLUE,
						"named blue marker placed from the world map");
			});
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			context.takeScreenshot("vanillasugar-7-world-map-marker");

			// Closed map: the marker shows in the world, the player looking east towards it
			context.setScreen(() -> null);
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ -90 10");
			context.waitTicks(5);
			context.takeScreenshot("vanillasugar-8-marker-in-world");

			// A right click on the marker removes it; without a compass, a right click does nothing
			context.setScreen(WorldMapScreen::new);
			context.waitForScreen(WorldMapScreen.class);
			rightClickMap(context, 40);
			world.getConnection().waitForServerboundPackets();
			context.waitTicks(2);
			world.getServer().runOnServer(server -> check(PlayerMarkers.of(server.getPlayerList().getPlayers().getFirst()).markers().isEmpty(),
					"marker removed from the world map"));
			rightClickMap(context, 40);
			typeOnScreen(context, "Mine");
			pressOnScreen(context, InputConstants.KEY_RETURN);
			world.getConnection().waitForServerboundPackets();
			context.waitTicks(2);

			// Icons only in the world, then back to icons and text
			context.setScreen(() -> null);
			context.getInput().pressKey(VanillaSugarClient.MARKER_DISPLAY);
			context.waitTicks(2);
			check(context.computeOnClient(client -> ClientSettings.markerDisplay() == ClientSettings.MarkerDisplay.ICON),
					"marker display key moves to icons only");
			context.takeScreenshot("vanillasugar-9-marker-icon-only");
			context.runOnClient(client -> ClientSettings.setMarkerDisplay(ClientSettings.MarkerDisplay.ICON_AND_TEXT));

			// The backpack key opens the inventory on the backpack, with the map tab and the marker display button
			context.runOnClient(client -> BackpackPanel.setOpen(false));
			context.getInput().pressKey(VanillaSugarClient.OPEN_BACKPACK);
			context.waitForScreen(InventoryScreen.class);
			context.waitTicks(2);
			check(context.computeOnClient(client -> BackpackPanel.isOpen()), "backpack key opens the backpack");
			context.takeScreenshot("vanillasugar-10-backpack-key");
			// Its tab, the second along the top after the recipe book's, closes it and the inventory stays
			// open; the key opens it again
			click(context, client -> (client.getWindow().getGuiScaledWidth() - 176) / 2 + GearLayout.PANEL_SHIFT + GearLayout.tabX(1) + GearLayout.TAB_WIDTH / 2,
					client -> (client.getWindow().getGuiScaledHeight() - 166) / 2 + GearLayout.TAB_Y + 8);
			check(!context.computeOnClient(client -> BackpackPanel.isOpen()), "the backpack tab closes the backpack");
			check(context.computeOnClient(client -> client.gui.screen() instanceof InventoryScreen), "the backpack tab leaves the inventory open");
			context.getInput().pressKey(VanillaSugarClient.OPEN_BACKPACK);
			context.waitTicks(2);
			check(context.computeOnClient(client -> BackpackPanel.isOpen()), "backpack key opens the backpack in the inventory");
			// The strap on top of the panel closes it too
			click(context, client -> (client.getWindow().getGuiScaledWidth() - 176) / 2 + GearLayout.PANEL_SHIFT + GearLayout.STRAP_X + GearLayout.STRAP_WIDTH / 2,
					client -> (client.getWindow().getGuiScaledHeight() - 166) / 2 + GearLayout.STRAP_Y + 2);
			check(!context.computeOnClient(client -> BackpackPanel.isOpen()), "the strap closes the backpack");
			context.getInput().pressKey(VanillaSugarClient.OPEN_BACKPACK);
			context.waitTicks(2);
			check(context.computeOnClient(client -> BackpackPanel.isOpen()), "backpack key opens the backpack again");
			context.getInput().pressKey(VanillaSugarClient.OPEN_BACKPACK);
			context.waitTicks(2);
			check(context.computeOnClient(client -> client.gui.screen() == null), "backpack key closes the inventory");
			check(!context.computeOnClient(client -> BackpackPanel.isOpen()), "closing the inventory closes the backpack");

			// The recipe book tab, first along the top of the inventory, opens the book, which moves the
			// inventory to the right: the tabs must follow it; the book's ribbon closes it
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			click(context, client -> (client.getWindow().getGuiScaledWidth() - 176) / 2 + GearLayout.tabX(0) + GearLayout.TAB_WIDTH / 2,
					client -> (client.getWindow().getGuiScaledHeight() - 166) / 2 + GearLayout.TAB_Y + 8);
			check(context.computeOnClient(GearClientGameTest::recipeBookOpen), "the recipe book tab opens the book");
			context.takeScreenshot("vanillasugar-11-recipe-book-tabs");
			click(context, client -> (client.getWindow().getGuiScaledWidth() - 147) / 2 - 86 + GearLayout.RIBBON_X + GearLayout.RIBBON_WIDTH / 2,
					client -> (client.getWindow().getGuiScaledHeight() - 166) / 2 + GearLayout.RIBBON_Y + 3);
			check(!context.computeOnClient(GearClientGameTest::recipeBookOpen), "the ribbon closes the recipe book");

			// Storage: a chest next to the player with its stacks scattered. A middle click on one of its
			// slots sorts it, its deposit button puts away the inventory's dirt
			context.setScreen(() -> null);
			world.getServer().runCommand("execute as @a at @s run setblock ~2 ~ ~ chest{Items:[{Slot:3b,id:\"minecraft:dirt\",count:5},"
					+ "{Slot:10b,id:\"minecraft:stone\",count:7},{Slot:20b,id:\"minecraft:dirt\",count:9}]}");
			world.getServer().runCommand("item replace entity @a inventory.5 with dirt 20");
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.openMenu((MenuProvider) player.level().getBlockEntity(player.blockPosition().east(2)));
			});
			context.waitForScreen(ContainerScreen.class);
			context.waitTicks(2);
			context.takeScreenshot("vanillasugar-13-chest-storage-buttons");
			middleClick(context, client -> (client.getWindow().getGuiScaledWidth() - 176) / 2 + 8 + 4 * 18 + 8,
					client -> (client.getWindow().getGuiScaledHeight() - 168) / 2 + 18 + 18 + 8);
			world.getConnection().waitForServerboundPackets();
			world.getServer().runOnServer(server -> {
				Container chest = chestNextTo(server);
				check(chest.getItem(0).is(Items.STONE) && chest.getItem(0).getCount() == 7 && chest.getItem(1).is(Items.DIRT)
						&& chest.getItem(1).getCount() == 14 && chest.getItem(2).isEmpty(), "middle click sorts the chest, stacks merged");
			});
			click(context, client -> (client.getWindow().getGuiScaledWidth() - 176) / 2 + 176 - 7 - 2 * 11 - 2 + 5,
					client -> (client.getWindow().getGuiScaledHeight() - 168) / 2 + 4 + 5);
			world.getConnection().waitForServerboundPackets();
			world.getServer().runOnServer(server -> {
				check(chestNextTo(server).countItem(Items.DIRT) == 34, "the deposit button puts the inventory's dirt in the chest");
				check(server.getPlayerList().getPlayers().getFirst().getInventory().countItem(Items.DIRT) == 0, "no dirt left in the inventory");
			});
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);

			// A shulker box's contents shown in its tooltip like a bundle's
			world.getServer().runCommand("item replace entity @a inventory.0 with shulker_box[container=[{slot:0,item:{id:\"minecraft:diamond\",count:5}},"
					+ "{slot:1,item:{id:\"minecraft:apple\",count:3}},{slot:4,item:{id:\"minecraft:oak_log\",count:64}}]]");
			context.waitTicks(2);
			double[] slotCenter = context.computeOnClient(client -> {
				double scale = client.getWindow().getGuiScale();
				return new double[] {((client.getWindow().getGuiScaledWidth() - 176) / 2 + 8 + 8) * scale,
						((client.getWindow().getGuiScaledHeight() - 166) / 2 + 84 + 8) * scale};
			});
			context.getInput().setCursorPos(slotCenter[0], slotCenter[1]);
			context.waitTicks(3);
			context.takeScreenshot("vanillasugar-14-shulker-preview");
			world.getServer().runCommand("item replace entity @a inventory.1 with bundle[bundle_contents=[{id:\"minecraft:arrow\",count:16},"
					+ "{id:\"minecraft:bread\",count:5},{id:\"minecraft:torch\",count:12},{id:\"minecraft:string\",count:4},{id:\"minecraft:flint\",count:2}]]");
			context.waitTicks(2);
			double[] bundleCenter = context.computeOnClient(client -> {
				double scale = client.getWindow().getGuiScale();
				return new double[] {((client.getWindow().getGuiScaledWidth() - 176) / 2 + 8 + 18 + 8) * scale,
						((client.getWindow().getGuiScaledHeight() - 166) / 2 + 84 + 8) * scale};
			});
			context.getInput().setCursorPos(bundleCenter[0], bundleCenter[1]);
			context.waitTicks(3);
			context.takeScreenshot("vanillasugar-14-bundle-preview");

			// A right click on ripe wheat harvests it and plants it again
			context.setScreen(() -> null);
			world.getServer().runCommand("execute as @a at @s run setblock ~ ~-1 ~2 farmland");
			world.getServer().runCommand("execute as @a at @s run setblock ~ ~ ~2 wheat[age=7]");
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 35");
			context.waitTicks(3);
			context.getInput().pressKey(options -> options.keyUse);
			world.getConnection().waitForServerboundPackets();
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				BlockState wheat = player.level().getBlockState(player.blockPosition().south(2));
				check(wheat.is(Blocks.WHEAT) && wheat.getValue(CropBlock.AGE) == 0, "right click replants ripe wheat");
				check(!player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4),
						item -> item.getItem().is(Items.WHEAT)).isEmpty(), "right click harvests the wheat");
			});
			world.getServer().runCommand("execute as @a at @s run setblock ~2 ~ ~ air");

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
			themedScreen(context, "anvil", inventory -> new AnvilScreen(new AnvilMenu(100, inventory), inventory,
					Component.translatable("container.repair")));
			themedScreen(context, "shulker", inventory -> new ShulkerBoxScreen(new ShulkerBoxMenu(100, inventory), inventory,
					Component.translatable("container.shulkerBox")));

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
			context.takeScreenshot("vanillasugar-12-creative-inventory");
			context.setScreen(() -> null);
			save = world.getWorldSave();
		}

		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runOnServer(server -> {
				PlayerGear gear = PlayerGear.of(server.getPlayerList().getPlayers().getFirst());
				check(gear.compass().is(Items.COMPASS) && gear.map().is(Items.MAP), "gear kept after reloading the world");
				check(PlayerMarkers.of(server.getPlayerList().getPlayers().getFirst()).markers().size() == 1, "markers kept after reloading the world");
				check(Files.exists(server.getWorldPath(LevelResource.ROOT).resolve("vanillasugar/worldmap/minecraft/overworld/r.0.0.bin")),
						"explored chunks saved with the world");
				check(gear.backpack().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream()
						.anyMatch(stack -> stack.is(Items.DIAMOND)), "backpack contents kept after reloading the world");
			});
			check(context.computeOnClient(client -> PlayerGear.isWearing(client.player, GearSlot.COMPASS)), "gear synced to the client");

			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("kill @a");
			context.waitTicks(5);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				for (GearSlot slot : GearSlot.values()) {
					check(!PlayerGear.isWearing(player, slot), "gear removed on death");
				}
				check(!player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(64),
						item -> item.getItem().is(VanillaSugarItems.BACKPACK)).isEmpty(), "backpack dropped on death");
			});
		}
	}

	/** Right click on the open world map, some GUI pixels right of its center (the player). */
	private static void rightClickMap(ClientGameTestContext context, int offsetX) {
		context.runOnClient(client -> client.gui.screen().mouseClicked(new MouseButtonEvent(client.getWindow().getGuiScaledWidth() / 2.0 + offsetX,
				client.getWindow().getGuiScaledHeight() / 2.0 - 2, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0)), false));
	}

	private static void typeOnScreen(ClientGameTestContext context, String text) {
		context.runOnClient(client -> text.codePoints().forEach(codepoint -> client.gui.screen().charTyped(new CharacterEvent(codepoint))));
	}

	private static void pressOnScreen(ClientGameTestContext context, int key) {
		context.runOnClient(client -> client.gui.screen().keyPressed(new KeyEvent(key, 0, 0)));
	}

	private static void themedScreen(ClientGameTestContext context, String name, Function<Inventory, Screen> screen) {
		context.runOnClient(client -> client.gui.setScreen(screen.apply(client.player.getInventory())));
		context.waitTicks(2);
		context.takeScreenshot("vanillasugar-theme-" + name);
	}

	/** Middle click at a GUI position computed on the client. */
	private static void middleClick(ClientGameTestContext context, ToIntFunction<Minecraft> x, ToIntFunction<Minecraft> y) {
		context.runOnClient(client -> client.gui.screen().mouseClicked(new MouseButtonEvent(x.applyAsInt(client), y.applyAsInt(client),
				new MouseButtonInfo(InputConstants.MOUSE_BUTTON_MIDDLE, 0)), false));
		context.waitTicks(2);
	}

	/** The chest placed two blocks east of the player. */
	private static Container chestNextTo(MinecraftServer server) {
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		return (Container) player.level().getBlockEntity(player.blockPosition().east(2));
	}

	/** Left click at a GUI position computed on the client. */
	private static void click(ClientGameTestContext context, ToIntFunction<Minecraft> x, ToIntFunction<Minecraft> y) {
		context.runOnClient(client -> {
			MouseButtonInfo left = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
			client.gui.screen().mouseClicked(new MouseButtonEvent(x.applyAsInt(client), y.applyAsInt(client), left), false);
			client.gui.screen().mouseReleased(new MouseButtonEvent(x.applyAsInt(client), y.applyAsInt(client), left));
		});
		context.waitTicks(2);
	}

	private static boolean recipeBookOpen(Minecraft client) {
		return client.gui.screen() instanceof InventoryScreen screen
				&& ((AbstractRecipeBookScreenAccessor) screen).vanillasugar$getRecipeBookComponent().isVisible();
	}

	private static void quickMove(ClientGameTestContext context, int slot) {
		context.runOnClient(client -> client.gameMode.handleContainerInput(
				client.player.inventoryMenu.containerId, slot, 0, ContainerInput.QUICK_MOVE, client.player));
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
