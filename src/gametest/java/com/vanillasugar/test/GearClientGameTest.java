package com.vanillasugar.test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
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
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.vanillasugar.client.BackpackPanel;
import com.vanillasugar.client.ClockHud;
import com.vanillasugar.client.HopperFilterScreen;
import com.vanillasugar.client.ClientSettings;
import com.vanillasugar.client.VanillaSugarClient;
import com.vanillasugar.client.combat.CombatHud;
import com.vanillasugar.client.mixin.AbstractRecipeBookScreenAccessor;
import com.vanillasugar.client.mixin.ImageButtonAccessor;
import com.vanillasugar.client.mixin.RecipeBookComponentAccessor;
import com.vanillasugar.client.quest.AchievementBanner;
import com.vanillasugar.client.map.Minimap;
import com.vanillasugar.client.map.MapTooltip;
import com.vanillasugar.client.map.WorldMapScreen;
import com.vanillasugar.client.mixin.AbstractContainerScreenAccessor;
import com.vanillasugar.client.quest.QuestLog;
import com.vanillasugar.client.quest.QuestOptInScreen;
import com.vanillasugar.clock.WeatherForecast;
import com.vanillasugar.compass.PlayerMarkers;
import com.vanillasugar.crafting.CraftAllPayload;
import com.vanillasugar.gear.ElytraGear;
import com.vanillasugar.gear.GearLayout;
import com.vanillasugar.gear.GearSlot;
import com.vanillasugar.gear.PlayerGear;
import com.vanillasugar.hopper.HopperFilter;
import com.vanillasugar.hopper.HopperFilterMenu;
import com.vanillasugar.item.VanillaSugarItems;
import com.vanillasugar.storage.MapPreview;

public class GearClientGameTest implements FabricClientGameTest {
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
		// A common player's window, 1080p with the automatic GUI scale, rather than the small default
		context.getInput().resizeWindow(1920, 1080);
		TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			// Declines the quest journal offered in each new world, which would cover the screens below
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("give @a vanillasugar:backpack");
			world.getServer().runCommand("give @a compass");
			world.getServer().runCommand("give @a vanillasugar:world_map");
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
				check(gear.map().is(VanillaSugarItems.WORLD_MAP), "map equipped");
				check(!GearSlot.MAP.accepts(new ItemStack(Items.MAP)) && !GearSlot.MAP.accepts(new ItemStack(Items.FILLED_MAP)),
						"only a world map fits the map slot");
				ItemContainerContents contents = gear.backpack().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
				check(contents.nonEmptyItemCopyStream().anyMatch(stack -> stack.is(Items.DIAMOND) && stack.getCount() == 64), "diamonds in backpack");
				check(contents.nonEmptyItemCopyStream().anyMatch(stack -> stack.is(Items.OAK_PLANKS)), "planks in backpack");
				check(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE), "chestplate worn");
			});
			// With the backpack open, a shift click dragged over the inventory's stacks moves them all into it, and back
			world.getServer().runCommand("item replace entity @a inventory.20 with brick 5");
			world.getServer().runCommand("item replace entity @a inventory.21 with clay_ball 5");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			shiftSweep(context, InventoryMenu.INV_SLOT_START + 20, InventoryMenu.INV_SLOT_START + 21, true);
			world.getConnection().waitForServerboundPackets();
			check(context.computeOnClient(client -> client.player.getInventory().countItem(Items.BRICK) == 0
					&& client.player.getInventory().countItem(Items.CLAY_BALL) == 0), "a shift click dragged fills the backpack");
			int[] packed = context.computeOnClient(client -> {
				List<Slot> slots = client.player.inventoryMenu.slots;
				int brick = -1;
				int clay = -1;
				for (int i = GearLayout.BACKPACK_START; i < GearLayout.BACKPACK_END; i++) {
					brick = slots.get(i).getItem().is(Items.BRICK) ? i : brick;
					clay = slots.get(i).getItem().is(Items.CLAY_BALL) ? i : clay;
				}
				return new int[] {brick, clay};
			});
			shiftSweep(context, packed[0], packed[1], true);
			world.getConnection().waitForServerboundPackets();
			check(context.computeOnClient(client -> client.player.getInventory().countItem(Items.BRICK) == 5
					&& client.player.getInventory().countItem(Items.CLAY_BALL) == 5), "a shift click dragged empties the backpack");


			// Compass: minimap on the HUD
			context.setScreen(() -> null);
			world.getServer().runCommand("effect give @a speed 60");
			// Some relief on the flat test world so the minimap has something to show
			world.getServer().runCommand("execute as @a at @s run fill ~4 ~-1 ~-12 ~16 ~-1 ~-2 water");
			world.getServer().runCommand("execute as @a at @s run fill ~-14 ~ ~2 ~-6 ~3 ~8 stone");
			world.getServer().runCommand("execute as @a at @s run fill ~-12 ~4 ~4 ~-8 ~5 ~6 stone");
			context.waitTicks(5);
			context.takeScreenshot("vanillasugar-4-minimap");

			// Status effects below the minimap, framed like it: beneficial ones, then harmful ones, a beacon's tinted
			world.getServer().runCommand("effect give @a weakness 30");
			world.getServer().runCommand("effect give @a jump_boost 5");
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
					.addEffect(new MobEffectInstance(MobEffects.HASTE, 200, 0, true, true)));
			context.waitTicks(5);
			context.takeScreenshot("vanillasugar-4-effects");
			// Next to the inventory, where vanilla lists them, in the same frame
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			context.runOnClient(client -> AchievementBanner.clear());
			context.waitTicks(2);
			context.takeScreenshot("vanillasugar-4-effects-inventory");
			context.setScreen(() -> null);
			world.getServer().runCommand("effect clear @a speed");
			world.getServer().runCommand("effect clear @a weakness");
			world.getServer().runCommand("effect clear @a haste");
			world.getServer().runCommand("effect clear @a jump_boost");

			// Turning with the player: looking west, west is up and north on the right
			context.runOnClient(client -> ClientSettings.setMinimapRotates(true));
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ 90 0");
			context.waitTicks(5);
			context.takeScreenshot("vanillasugar-4-minimap-rotating");
			context.runOnClient(client -> ClientSettings.setMinimapRotates(false));
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 0");

			// A clock in the inventory does nothing; worn in its gear slot, it shows the time, weather and day
			// in the top left corner, and the barometer made from it the next weather too
			world.getServer().runCommand("item replace entity @a hotbar.8 with clock");
			context.waitTicks(2);
			check(!context.computeOnClient(client -> ClockHud.isShown()), "a clock in the inventory shows no panel");
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			quickMove(context, InventoryMenu.USE_ROW_SLOT_START + 8);
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			context.takeScreenshot("vanillasugar-4-clock-slot");
			world.getServer().runOnServer(server -> check(PlayerGear.isWearing(server.getPlayerList().getPlayers().getFirst(), GearSlot.CLOCK),
					"shift click equips the clock"));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamerule advance_weather true");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather rain 3000");
			context.waitTicks(40);
			context.takeScreenshot("vanillasugar-4-clock-rain");
			check(context.computeOnClient(client -> WeatherForecast.UNKNOWN.equals(forecast())), "a clock gets no weather forecast");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.CLOCK, new ItemStack(VanillaSugarItems.BAROMETER)));
			});
			context.waitTicks(30);
			check(context.computeOnClient(client -> !WeatherForecast.UNKNOWN.equals(forecast())), "a barometer gets the weather forecast");
			context.takeScreenshot("vanillasugar-4-barometer-rain");
			world.getServer().runCommand("time set 16000");
			world.getServer().runCommand("weather clear 1000");
			context.waitTicks(40);
			context.takeScreenshot("vanillasugar-4-barometer-night");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.CLOCK, ItemStack.EMPTY));
			});
			world.getServer().runCommand("time set 6000");

			// Combat, with a sculk compass only: a mob chasing the player shows its health and a "!"; one left
			// alone shows nothing. The clock worn again, for the eye right of its panel
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.CLOCK, new ItemStack(Items.CLOCK)));
			});
			world.getServer().runCommand("execute as @a at @s run summon husk ~-2 ~ ~9 {NoAI:1b,Tags:[\"hunter\"]}");
			world.getServer().runCommand("execute as @a at @s run summon husk ~3 ~ ~10 {NoAI:1b,Tags:[\"idle\"]}");
			context.waitTicks(5);
			int[] husks = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				List<Husk> found = player.level().getEntitiesOfClass(Husk.class, player.getBoundingBox().inflate(16));
				Husk hunter = found.stream().filter(husk -> husk.entityTags().contains("hunter")).findFirst().orElseThrow();
				Husk idle = found.stream().filter(husk -> husk.entityTags().contains("idle")).findFirst().orElseThrow();
				hunter.setTarget(player);
				return new int[] {hunter.getId(), idle.getId()};
			});
			// The banners of the advancements made so far would hide the mobs' heads
			context.runOnClient(client -> AchievementBanner.clear());
			context.waitTicks(13);
			context.takeScreenshot("vanillasugar-5-combat-compass");
			check(context.computeOnClient(client -> !CombatHud.hasSpotted(husks[0]) && !CombatHud.isShown(husks[0]) && !CombatHud.isHuntedShown(client)),
					"a compass senses no mob, its health, \"!\" or eye");
			// The sculk compass worn, its advancement celebrated, the mob starts the chase again
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				((Mob) server.overworld().getEntity(husks[0])).setTarget(null);
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.COMPASS, new ItemStack(VanillaSugarItems.SCULK_COMPASS)));
			});
			// The advancement comes within a second
			context.waitTicks(30);
			context.runOnClient(client -> AchievementBanner.clear());
			world.getServer().runOnServer(server -> ((Mob) server.overworld().getEntity(husks[0]))
					.setTarget(server.getPlayerList().getPlayers().getFirst()));
			context.waitTicks(8);
			context.takeScreenshot("vanillasugar-5-combat-spotted");
			context.waitTicks(10);
			check(context.computeOnClient(client -> CombatHud.isHuntedShown(client) && Minimap.isShown(client)),
					"a sculk compass shows the minimap and the eye");
			context.takeScreenshot("vanillasugar-5-combat-hunted");
			check(context.computeOnClient(client -> CombatHud.hasSpotted(husks[0]) && CombatHud.isShown(husks[0])),
					"a mob chasing the player shows its health and that it spotted them");
			check(context.computeOnClient(client -> !CombatHud.isShown(husks[1]) && !CombatHud.hasSpotted(husks[1])),
					"a mob left alone shows nothing");
			// A second one: the eye next to the hotbar counts them
			world.getServer().runOnServer(server -> ((Mob) server.overworld().getEntity(husks[1]))
					.setTarget(server.getPlayerList().getPlayers().getFirst()));
			context.waitTicks(10);
			context.takeScreenshot("vanillasugar-5-combat-hunted-twice");
			check(context.computeOnClient(client -> CombatHud.hasSpotted(husks[1])), "a second mob chasing the player is counted");
			world.getServer().runOnServer(server -> ((Mob) server.overworld().getEntity(husks[1])).setTarget(null));
			context.waitTicks(10);
			world.getServer().runCommand("damage @e[type=husk,tag=hunter,limit=1] 8");
			world.getServer().runCommand("damage @e[type=husk,tag=idle,limit=1] 5");
			context.waitTicks(2);
			context.takeScreenshot("vanillasugar-5-combat-hurt");
			check(context.computeOnClient(client -> CombatHud.isShown(husks[1]) && !CombatHud.hasSpotted(husks[1])),
					"a mob just hurt shows its health");
			world.getServer().runOnServer(server -> ((Mob) server.overworld().getEntity(husks[0])).setTarget(null));
			context.waitTicks(30);
			context.takeScreenshot("vanillasugar-5-combat-drained");
			check(context.computeOnClient(client -> !CombatHud.hasSpotted(husks[0])), "a mob that gave up the chase loses its \"!\"");
			world.getServer().runCommand("kill @e[type=husk]");
			// A ghast aims from farther than any other mob: one chasing the player 56 blocks away shows too
			world.getServer().runCommand("execute as @a at @s run summon ghast ~ ~6 ~56 {NoAI:1b}");
			context.waitTicks(5);
			int ghast = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				Ghast found = player.level().getEntitiesOfClass(Ghast.class, player.getBoundingBox().inflate(64)).getFirst();
				found.setTarget(player);
				return found.getId();
			});
			context.waitTicks(5);
			check(context.computeOnClient(client -> CombatHud.hasSpotted(ghast) && CombatHud.isShown(ghast) && CombatHud.isHuntedShown(client)),
					"a ghast chasing the player from afar shows its health, \"!\" and eye");
			world.getServer().runCommand("kill @e[type=ghast]");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.CLOCK, ItemStack.EMPTY)
						.with(GearSlot.COMPASS, new ItemStack(Items.COMPASS)));
			});

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
					client -> chestTop(client) + 18 + 18 + 8);
			world.getConnection().waitForServerboundPackets();
			world.getServer().runOnServer(server -> {
				Container chest = chestNextTo(server);
				check(chest.getItem(0).is(Items.STONE) && chest.getItem(0).getCount() == 7 && chest.getItem(1).is(Items.DIRT)
						&& chest.getItem(1).getCount() == 14 && chest.getItem(2).isEmpty(), "middle click sorts the chest, stacks merged");
			});
			click(context, client -> (client.getWindow().getGuiScaledWidth() - 176) / 2 + 176 - 7 - 2 * 11 - 2 + 5,
					client -> chestTop(client) + 4 + 5);
			world.getConnection().waitForServerboundPackets();
			world.getServer().runOnServer(server -> {
				check(chestNextTo(server).countItem(Items.DIRT) == 34, "the deposit button puts the inventory's dirt in the chest");
				check(server.getPlayerList().getPlayers().getFirst().getInventory().countItem(Items.DIRT) == 0, "no dirt left in the inventory");
			});
			// Moving stacks in a window makes a sound, which vanilla does not
			List<String> sounds = new CopyOnWriteArrayList<>();
			SoundEventListener listener = (sound, event, range) -> sounds.add(sound.getIdentifier().getPath());
			context.runOnClient(client -> client.getSoundManager().addListener(listener));
			// A shift click on a stack, then the next one hovered with shift and the button still held: both move to the chest
			world.getServer().runCommand("item replace entity @a inventory.6 with cobblestone 10");
			world.getServer().runCommand("item replace entity @a inventory.7 with sand 10");
			context.waitTicks(2);
			shiftSweep(context, 27 + 6, 27 + 7, true);
			world.getServer().runOnServer(server -> {
				Container chest = chestNextTo(server);
				check(chest.countItem(Items.COBBLESTONE) == 10 && chest.countItem(Items.SAND) == 10,
						"shift click then hovering with shift held moves both stacks (cobblestone " + chest.countItem(Items.COBBLESTONE)
								+ ", sand " + chest.countItem(Items.SAND) + ")");
			});
			context.runOnClient(client -> client.getSoundManager().removeListener(listener));
			check(sounds.contains("item.bundle.insert"), "moving stacks makes a sound (" + sounds + ")");
			// The button released at once, the sweep stops with it
			world.getServer().runCommand("item replace entity @a inventory.6 with gravel 10");
			world.getServer().runCommand("item replace entity @a inventory.7 with clay 10");
			context.waitTicks(2);
			shiftSweep(context, 27 + 6, 27 + 7, false);
			world.getServer().runOnServer(server -> {
				Container chest = chestNextTo(server);
				check(chest.countItem(Items.GRAVEL) == 10 && chest.countItem(Items.CLAY) == 0,
						"releasing the button ends the sweep (gravel " + chest.countItem(Items.GRAVEL) + ", clay " + chest.countItem(Items.CLAY) + ")");
			});
			// A bundle carried, a right click dragged over empty slots takes a stack out in each
			world.getServer().runCommand("item replace entity @a inventory.7 with air");
			world.getServer().runCommand("item replace entity @a inventory.8 with bundle[bundle_contents=[{id:\"minecraft:apple\",count:5},"
					+ "{id:\"minecraft:bread\",count:3}]]");
			context.waitTicks(2);
			click(context, client -> inventoryRowSlotX(client, 8), client -> inventoryRowSlotY(client));
			context.runOnClient(client -> {
				MouseButtonInfo right = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0);
				MouseButtonEvent first = new MouseButtonEvent(inventoryRowSlotX(client, 6), inventoryRowSlotY(client), right);
				MouseButtonEvent second = new MouseButtonEvent(inventoryRowSlotX(client, 7), inventoryRowSlotY(client), right);
				client.gui.screen().mouseClicked(first, false);
				client.gui.screen().mouseDragged(second, 18, 0);
				client.gui.screen().mouseReleased(second);
			});
			world.getConnection().waitForServerboundPackets();
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ItemStack bundle = player.containerMenu.getCarried();
				check(player.getInventory().getItem(15).is(Items.APPLE) && player.getInventory().getItem(16).is(Items.BREAD)
						&& bundle.is(Items.BUNDLE) && bundle.get(DataComponents.BUNDLE_CONTENTS).isEmpty(),
						"a right click dragged empties the bundle in the slots (" + player.getInventory().getItem(15) + ", "
								+ player.getInventory().getItem(16) + ")");
			});
			// And a left click dragged over stacks puts them back in it
			context.runOnClient(client -> {
				MouseButtonInfo left = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
				MouseButtonEvent first = new MouseButtonEvent(inventoryRowSlotX(client, 6), inventoryRowSlotY(client), left);
				MouseButtonEvent second = new MouseButtonEvent(inventoryRowSlotX(client, 7), inventoryRowSlotY(client), left);
				client.gui.screen().mouseClicked(first, false);
				client.gui.screen().mouseDragged(second, 18, 0);
				client.gui.screen().mouseReleased(second);
			});
			world.getConnection().waitForServerboundPackets();
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ItemStack bundle = player.containerMenu.getCarried();
				check(player.getInventory().getItem(15).isEmpty() && player.getInventory().getItem(16).isEmpty()
						&& bundle.get(DataComponents.BUNDLE_CONTENTS).size() == 2, "a left click dragged fills the bundle with the stacks");
			});
			// Put down in the slot left empty
			click(context, client -> inventoryRowSlotX(client, 8), client -> inventoryRowSlotY(client));
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

			// A boat on a dirt path climbs onto the grass, a sixteenth of a block higher
			world.getServer().runCommand("execute as @a at @s run fill ~4 ~-1 ~-1 ~7 ~-1 ~1 dirt_path");
			world.getServer().runCommand("execute as @a at @s run summon oak_boat ~5.5 ~ ~0.5");
			context.waitTicks(10);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				AbstractBoat boat = player.level().getEntitiesOfClass(AbstractBoat.class, player.getBoundingBox().inflate(8)).getFirst();
				double startX = boat.getX();
				// Pushed east, and down by gravity as every tick on land
				for (int step = 0; step < 8; step++) {
					boat.move(MoverType.SELF, new Vec3(0.5, -0.04, 0));
				}
				check(boat.getX() > startX + 3.5 && boat.getY() == player.getY(), "a boat climbs from a dirt path onto grass (moved "
						+ (boat.getX() - startX) + ", at y " + boat.getY() + " for the grass at " + player.getY() + ")");
				boat.discard();
			});

			// A lead ties a villager
			world.getServer().runCommand("execute as @a at @s run summon villager ~ ~ ~-2 {NoAI:1b}");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with lead");
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				Villager villager = player.level().getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(8)).getFirst();
				villager.interact(player, InteractionHand.MAIN_HAND, villager.position());
				check(villager.getLeashHolder() == player, "a lead ties a villager");
				villager.discard();
			});
			world.getServer().runCommand("clear @a lead");

			// The last block placed from the hand is replaced from the inventory, and only from the inventory
			world.getServer().runCommand("execute as @a at @s run setblock ~2 ~-1 ~ stone");
			world.getServer().runCommand("execute as @a at @s run setblock ~2 ~ ~ air");
			world.getServer().runCommand("item replace entity @a hotbar.0 with cobblestone 1");
			world.getServer().runCommand("item replace entity @a inventory.10 with cobblestone 10");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.getInventory().setSelectedSlot(0);
				BlockPos ground = player.blockPosition().east(2).below();
				player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false));
				check(player.level().getBlockState(ground.above()).is(Blocks.COBBLESTONE), "the last cobblestone of the hand placed");
				check(player.getMainHandItem().is(Items.COBBLESTONE) && player.getMainHandItem().getCount() == 10
						&& player.getInventory().getItem(Inventory.SELECTION_SIZE + 10).isEmpty(), "the hand refilled from the inventory");
			});
			world.getServer().runCommand("clear @a cobblestone");
			world.getServer().runCommand("execute as @a at @s run setblock ~2 ~ ~ air");

			// A control click on a recipe crafts it until the ingredients run out, over more than one full grid
			world.getServer().runCommand("clear @a oak_log");
			world.getServer().runCommand("clear @a oak_planks");
			world.getServer().runCommand("recipe give @a minecraft:oak_planks");
			world.getServer().runCommand("give @a oak_log 70");
			context.waitTicks(2);
			RecipeDisplayId planks = world.getServer().computeOnServer(server -> {
				List<RecipeDisplayId> ids = new ArrayList<>();
				server.getRecipeManager().listDisplaysForRecipe(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("oak_planks")),
						entry -> ids.add(entry.id()));
				return ids.getFirst();
			});
			context.runOnClient(client -> ClientPlayNetworking.send(new CraftAllPayload(client.player.inventoryMenu.containerId, planks)));
			world.getConnection().waitForServerboundPackets();
			world.getServer().runOnServer(server -> {
				Inventory inventory = server.getPlayerList().getPlayers().getFirst().getInventory();
				check(inventory.countItem(Items.OAK_LOG) == 0 && inventory.countItem(Items.OAK_PLANKS) > 70,
						"craft all turns every log into planks (logs " + inventory.countItem(Items.OAK_LOG) + ", planks "
								+ inventory.countItem(Items.OAK_PLANKS) + ")");
			});
			world.getServer().runCommand("clear @a oak_planks");

			// A filled map in the inventory shows itself in its tooltip once the server sent its contents; held a moment to draw it
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.getInventory().setSelectedSlot(0);
				player.getInventory().setItem(0, MapItem.create(player.level(), player.getBlockX(), player.getBlockZ(), (byte) 0, true, false));
			});
			context.waitTicks(40);
			check(context.computeOnClient(client -> {
				ItemStack map = client.player.getInventory().getItem(0);
				return map.getTooltipImage().orElse(null) instanceof MapPreview preview && new MapTooltip(preview).getHeight(client.font) > 0;
			}), "a filled map's tooltip shows the map");
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			double[] mapCenter = context.computeOnClient(client -> {
				AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
				Slot slot = screen.getMenu().getSlot(InventoryMenu.USE_ROW_SLOT_START);
				AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
				double scale = client.getWindow().getGuiScale();
				return new double[] {(accessor.vanillasugar$getLeftPos() + slot.x + 8) * scale, (accessor.vanillasugar$getTopPos() + slot.y + 8) * scale};
			});
			context.getInput().setCursorPos(mapCenter[0], mapCenter[1]);
			context.waitTicks(3);
			context.takeScreenshot("vanillasugar-14-map-preview");
			context.setScreen(() -> null);
			world.getServer().runCommand("clear @a filled_map");

			// A hopper whose filter is set to stone, then put under a chest of stone and dirt, only takes the stone
			world.getServer().runCommand("execute as @a at @s run setblock ~-3 ~ ~ hopper");
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(!player.level().getBlockState(player.blockPosition().west(3)).getValue(HopperFilter.FILTERED), "a placed hopper has no filter");
				player.openMenu((MenuProvider) player.level().getBlockEntity(player.blockPosition().west(3)));
				player.containerMenu.getSlot(HopperFilterMenu.FILTER_SLOT).set(new ItemStack(Items.STONE));
			});
			context.waitForScreen(HopperFilterScreen.class);
			world.getServer().runCommand("execute as @a at @s run setblock ~-3 ~1 ~ chest{Items:[{Slot:0b,id:\"minecraft:dirt\",count:3},"
					+ "{Slot:1b,id:\"minecraft:stone\",count:3},{Slot:2b,id:\"minecraft:dirt\",count:2}]}");
			context.waitTicks(40);
			context.takeScreenshot("vanillasugar-15-hopper-filter");
			check(context.computeOnClient(client -> client.level.getBlockState(client.player.blockPosition().west(3)).getValue(HopperFilter.FILTERED)),
					"the client sees the hopper with its filter's look");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				Container hopper = (Container) player.level().getBlockEntity(player.blockPosition().west(3));
				Container chest = (Container) player.level().getBlockEntity(player.blockPosition().west(3).above());
				check(hopper.countItem(Items.STONE) == 3 && hopper.countItem(Items.DIRT) == 0 && chest.countItem(Items.DIRT) == 5,
						"the hopper takes the stone and leaves the dirt (stone " + hopper.countItem(Items.STONE) + ", dirt "
								+ hopper.countItem(Items.DIRT) + ")");
				check(!player.containerMenu.getSlot(0).mayPlace(new ItemStack(Items.DIRT)), "the hopper's slots refuse dirt");
			});
			context.runOnClient(client -> client.player.closeContainer());
			world.getServer().runCommand("execute as @a at @s run setblock ~-3 ~1 ~ air");
			world.getServer().runCommand("execute as @a at @s run setblock ~-3 ~ ~ air destroy");
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				List<ItemEntity> drops = player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(5));
				int stone = drops.stream().filter(drop -> drop.getItem().is(Items.STONE)).mapToInt(drop -> drop.getItem().getCount()).sum();
				check(stone == 4 && drops.stream().anyMatch(drop -> drop.getItem().is(Items.HOPPER)),
						"a broken hopper drops itself, its contents and its filter (stone " + stone + ")");
				drops.forEach(ItemEntity::discard);
			});

			// A composter filled to its last layer is ready at once, and the hopper below takes its eight bone meal together
			world.getServer().runCommand("execute as @a at @s run setblock ~-5 ~ ~ hopper");
			world.getServer().runCommand("execute as @a at @s run setblock ~-5 ~1 ~ composter[level=7]");
			context.waitTicks(12);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				Container hopper = (Container) player.level().getBlockEntity(player.blockPosition().west(5));
				int level = player.level().getBlockState(player.blockPosition().west(5).above()).getValue(ComposterBlock.LEVEL);
				check(hopper.countItem(Items.BONE_MEAL) == 8 && level == 0,
						"the hopper takes the composter's eight bone meal (" + hopper.countItem(Items.BONE_MEAL) + ", level " + level + ")");
			});
			// Emptied by hand, it gives the eight too
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				BlockPos composter = player.blockPosition().west(5).above();
				BlockState ready = Blocks.COMPOSTER.defaultBlockState().setValue(ComposterBlock.LEVEL, ComposterBlock.READY);
				player.level().setBlockAndUpdate(composter, ready);
				ComposterBlock.extractProduce(player, ready, player.level(), composter);
				List<ItemEntity> drops = player.level().getEntitiesOfClass(ItemEntity.class, new AABB(composter).inflate(2));
				int boneMeal = drops.stream().filter(drop -> drop.getItem().is(Items.BONE_MEAL)).mapToInt(drop -> drop.getItem().getCount()).sum();
				check(boneMeal == 8, "an emptied composter gives eight bone meal (" + boneMeal + ")");
				drops.forEach(ItemEntity::discard);
			});
			world.getServer().runCommand("execute as @a at @s run setblock ~-5 ~1 ~ air");
			world.getServer().runCommand("execute as @a at @s run setblock ~-5 ~ ~ air");

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
			themedScreen(context, "grindstone", inventory -> new GrindstoneScreen(new GrindstoneMenu(100, inventory), inventory,
					Component.translatable("container.grindstone_title")));
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

			// The enchanting table's book opens when an item can be enchanted
			context.runOnClient(client -> {
				EnchantmentMenu menu = new EnchantmentMenu(100, client.player.getInventory());
				menu.costs[0] = 3;
				menu.costs[1] = 12;
				client.gui.setScreen(new EnchantmentScreen(menu, client.player.getInventory(), Component.translatable("container.enchant")));
			});
			context.waitTicks(10);
			quiet(context);
			context.takeScreenshot("vanillasugar-theme-enchanting-open");

			// The crafting table's recipe book: its button is the bare book, its search written on the
			// page, and a right click on the planks shows all their recipes
			world.getServer().runCommand("recipe give @a *");
			context.runOnClient(client -> client.gui.setScreen(new CraftingScreen(new CraftingMenu(100, client.player.getInventory()),
					client.player.getInventory(), Component.translatable("container.crafting"))));
			context.waitTicks(2);
			click(context, client -> recipeButton(client).getX() + 10, client -> recipeButton(client).getY() + 9);
			check(context.computeOnClient(client -> ((AbstractRecipeBookScreenAccessor) client.gui.screen())
					.vanillasugar$getRecipeBookComponent().isVisible()), "the bare book opens the crafting table's recipe book");
			// Its ribbon closes it, as in the inventory
			click(context, client -> recipeBookX(client) + GearLayout.RIBBON_X + GearLayout.RIBBON_WIDTH / 2,
					client -> recipeBookY(client) + GearLayout.RIBBON_Y + 3);
			check(!context.computeOnClient(client -> ((AbstractRecipeBookScreenAccessor) client.gui.screen())
					.vanillasugar$getRecipeBookComponent().isVisible()), "the ribbon closes the crafting table's recipe book");
			click(context, client -> recipeButton(client).getX() + 10, client -> recipeButton(client).getY() + 9);
			click(context, client -> recipeBookX(client) + 40, client -> recipeBookY(client) + 18);
			typeOnScreen(context, "planks");
			context.waitTicks(2);
			quiet(context);
			context.takeScreenshot("vanillasugar-17-recipe-book-search");
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
			quiet(context);
			context.takeScreenshot("vanillasugar-17-recipe-book-variants");

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

			// An elytra with Mending goes to its gear slot, the chestplate still worn; experience repairs it
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("item replace entity @a hotbar.6 with elytra[enchantments={\"minecraft:mending\":1},damage=200]");
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			quickMove(context, InventoryMenu.USE_ROW_SLOT_START + 6);
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			context.takeScreenshot("vanillasugar-16-elytra-slot");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(ElytraGear.worn(player).is(Items.ELYTRA), "elytra equipped in its gear slot");
				check(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE), "chestplate worn with the elytra");
			});
			context.setScreen(() -> null);
			world.getServer().runCommand("execute as @a at @s run summon experience_orb ~ ~ ~ {Value:20}");
			context.waitTicks(10);
			world.getServer().runOnServer(server -> check(ElytraGear.worn(server.getPlayerList().getPlayers().getFirst()).getDamageValue() < 200,
					"Mending repairs the elytra of the gear slot"));

			// High up, it glides and wears down; other players see its wings
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~60 ~ 0 0");
			context.waitTicks(2);
			world.getServer().runOnServer(server -> check(server.getPlayerList().getPlayers().getFirst().tryToStartFallFlying(),
					"glides with the elytra of the gear slot"));
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(25);
			context.takeScreenshot("vanillasugar-16-elytra-gliding");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(player.isFallFlying(), "still gliding");
				check(ElytraGear.worn(player).getDamageValue() > 0, "gliding wears the elytra down");
			});
			check(context.computeOnClient(client -> ElytraGear.shown(client.player).is(Items.ELYTRA)), "the worn elytra synced for the wings");
			context.setScreen(() -> null);
			save = world.getWorldSave();
		}

		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runOnServer(server -> {
				PlayerGear gear = PlayerGear.of(server.getPlayerList().getPlayers().getFirst());
				check(gear.compass().is(Items.COMPASS) && gear.map().is(VanillaSugarItems.WORLD_MAP) && gear.elytra().is(Items.ELYTRA), "gear kept after reloading the world");
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
		quiet(context);
		context.takeScreenshot("vanillasugar-theme-" + name);
	}

	/** Clears the advancements' banners and the recipes' toasts, which would cover the screens. */
	private static void quiet(ClientGameTestContext context) {
		context.runOnClient(client -> {
			AchievementBanner.clear();
			client.gui.toastManager().clear();
		});
		context.waitTicks(1);
	}

	/** The button opening the recipe book of the screen open. */
	private static ImageButton recipeButton(Minecraft client) {
		return client.gui.screen().children().stream()
				.filter(widget -> widget instanceof ImageButton button
						&& ((ImageButtonAccessor) button).vanillasugar$getSprites() == RecipeBookComponent.RECIPE_BUTTON_SPRITES)
				.map(ImageButton.class::cast).findFirst().orElseThrow();
	}

	private static int recipeBookX(Minecraft client) {
		return ((RecipeBookComponentAccessor) ((AbstractRecipeBookScreenAccessor) client.gui.screen()).vanillasugar$getRecipeBookComponent())
				.vanillasugar$getXOrigin();
	}

	private static int recipeBookY(Minecraft client) {
		return ((RecipeBookComponentAccessor) ((AbstractRecipeBookScreenAccessor) client.gui.screen()).vanillasugar$getRecipeBookComponent())
				.vanillasugar$getYOrigin();
	}

	/**
	 * A shift click on a slot of the screen open, the cursor then dragged onto another, the button
	 * released before the drag or after it.
	 */
	private static void shiftSweep(ClientGameTestContext context, int from, int to, boolean holdButton) {
		context.getInput().holdShift();
		// The test's mouse buttons carry no modifiers, nor drag the cursor: the events go to the screen directly
		context.runOnClient(client -> {
			MouseButtonEvent click = slotEvent(client, from, InputConstants.MOD_SHIFT);
			MouseButtonEvent drag = slotEvent(client, to, InputConstants.MOD_SHIFT);
			client.gui.screen().mouseClicked(click, false);
			if (!holdButton) {
				client.gui.screen().mouseReleased(click);
			}
			client.gui.screen().mouseDragged(drag, drag.x() - click.x(), drag.y() - click.y());
			if (holdButton) {
				client.gui.screen().mouseReleased(drag);
			}
		});
		context.getInput().releaseShift();
		context.waitTicks(2);
	}

	/** A left button event at the center of a slot of the screen open, in GUI pixels. */
	private static MouseButtonEvent slotEvent(Minecraft client, int slotIndex, int modifiers) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
		Slot slot = screen.getMenu().getSlot(slotIndex);
		AbstractContainerScreenAccessor position = (AbstractContainerScreenAccessor) screen;
		return new MouseButtonEvent(position.vanillasugar$getLeftPos() + slot.x + 8, position.vanillasugar$getTopPos() + slot.y + 8,
				new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, modifiers));
	}

	/** The center of a slot of the chest screen's first inventory row, in GUI pixels. */
	private static int inventoryRowSlotX(Minecraft client, int column) {
		return (client.getWindow().getGuiScaledWidth() - 176) / 2 + 8 + column * 18 + 8;
	}

	private static int inventoryRowSlotY(Minecraft client) {
		return chestTop(client) + ((AbstractContainerScreen<?>) client.gui.screen()).getMenu().getSlot(27).y + 8;
	}

	/** The top of the chest screen open, in GUI pixels: the room around its inventory makes it taller than vanilla's. */
	private static int chestTop(Minecraft client) {
		return ((AbstractContainerScreenAccessor) client.gui.screen()).vanillasugar$getTopPos();
	}

	/** Middle click at a GUI position computed on the client. */
	private static void middleClick(ClientGameTestContext context, ToIntFunction<Minecraft> x, ToIntFunction<Minecraft> y) {
		context.runOnClient(client -> client.gui.screen().mouseClicked(new MouseButtonEvent(x.applyAsInt(client), y.applyAsInt(client),
				new MouseButtonInfo(InputConstants.MOUSE_BUTTON_MIDDLE, 0)), false));
		context.waitTicks(2);
	}

	/** Window position of the center of the slot in a column of the three row chest screen, its top in GUI pixels. */
	private static double[] chestScreenSlot(ClientGameTestContext context, int column, int top) {
		return context.computeOnClient(client -> {
			double scale = client.getWindow().getGuiScale();
			return new double[] {((client.getWindow().getGuiScaledWidth() - 176) / 2 + 8 + column * 18 + 8) * scale,
					(chestTop(client) + top + 8) * scale};
		});
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

	/** The forecast the clock's panel last received. */
	private static WeatherForecast forecast() {
		try {
			Field field = ClockHud.class.getDeclaredField("forecast");
			field.setAccessible(true);
			return (WeatherForecast) field.get(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
