package com.cassonade.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;

import com.cassonade.client.ClientSettings;
import com.cassonade.client.CassonadeClient;
import com.cassonade.client.map.WorldMapScreen;
import com.cassonade.client.mixin.AbstractContainerScreenAccessor;
import com.cassonade.client.InfoPanel;
import com.cassonade.client.WorldSelectionScreen;
import com.cassonade.client.mixin.ImageButtonAccessor;
import com.cassonade.client.quest.AchievementBanner;
import com.cassonade.client.quest.AdvancementsListScreen;
import com.cassonade.client.quest.QuestJournalScreen;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.compass.PlayerMarkers;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.hopper.HopperFilterMenu;
import com.cassonade.item.CassonadeItems;
import com.cassonade.mixin.AbstractFurnaceBlockEntityAccessor;
import com.cassonade.woodcutter.Woodcutter;

/**
 * The website's screenshots (site/assets/screenshots), in a generated world by a plains village: run
 * on demand only, with {@code ./gradlew runClientGameTest -PsiteScreenshots}. Each is copied to the
 * site as {@code <id>.png}.
 */
public class SiteScreenshotsClientGameTest implements FabricClientGameTest {
	// The mod's former name: the seed places the village and the shots' positions, so it stays
	private static final String SEED = "vanillasugar";
	private static final String WORLD_NAME = "Plains Village";
	private static final long HOUR = 3_600_000;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		String folder = System.getProperty("cassonade.siteScreenshots");
		if (folder == null) {
			return;
		}
		Path site = Path.of(folder);
		context.getInput().resizeWindow(1920, 1080);
		context.runOnClient(client -> {
			client.options.chatVisibility().set(ChatVisiblity.HIDDEN);
			client.options.tutorialStep = TutorialSteps.NONE;
			client.options.renderDistance().set(10);
			client.options.bobView().set(false);
			// A notch below the automatic scale of 1080p, so the HUD's panels keep clear of each other
			client.options.guiScale().set(3);
			client.resizeGui();
		});
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(SiteScreenshotsClientGameTest::settings).create()) {
			settle(context);
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(true));
			context.setScreen(() -> null);
			// The blocks among the recipes' ingredients have no flat texture: drawn by the game as in the inventory, at its
			// smallest scale, so they match the other items' 16 pixels; once in a world, where items have their components
			blockIcons(context, site.resolveSibling("items"), Items.LIGHTNING_ROD.weathering().unaffected(), Items.SCULK_SENSOR, Woodcutter.ITEM);
			for (String rule : List.of("send_command_feedback false", "show_advancement_messages false", "advance_time false",
					"advance_weather false", "spawn_mobs false", "spawn_monsters false", "spawn_patrols false", "spawn_wandering_traders false")) {
				world.getServer().runCommand("gamerule " + rule);
			}
			world.getServer().runCommand("time set 1000");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamemode survival @a");

			BlockPos village = world.getServer().computeOnServer(server -> server.overworld()
					.findNearestMapStructure(StructureTags.ON_PLAINS_VILLAGE_MAPS, BlockPos.ZERO, 100, false));
			if (village == null) {
				throw new AssertionError("Expected: a plains village near the spawn of seed " + SEED);
			}
			equip(world);
			world.getServer().runCommand("advancement grant @a until minecraft:story/iron_tools");
			world.getServer().runCommand("advancement grant @a until minecraft:adventure/sleep_in_bed");
			world.getServer().runCommand("recipe give @a *");

			// The world map, explored along a loop around the village first
			int[][] route = {{0, 0}, {96, 0}, {192, 48}, {240, 144}, {192, 240}, {96, 288}, {0, 240}, {-96, 192}, {-144, 96}, {-96, 0},
					{-48, -96}, {48, -144}, {144, -96}};
			for (int[] step : route) {
				teleport(context, world, village.offset(step[0], 0, step[1]), 0, 0);
				context.waitTicks(40);
			}
			BlockPos home = village.offset(20, 0, 20);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				PlayerMarkers.place(player, GlobalPos.of(Level.OVERWORLD, surface(server, village.offset(110, 0, 70))), "Mine", DyeColor.ORANGE);
				PlayerMarkers.place(player, GlobalPos.of(Level.OVERWORLD, surface(server, village.offset(-120, 0, -50))), "Tower", DyeColor.BLUE);
				PlayerMarkers.place(player, GlobalPos.of(Level.OVERWORLD, surface(server, village.offset(-40, 0, 45))), "Farm", DyeColor.LIME);
			});
			teleport(context, world, home, 0, 0);
			context.setScreen(WorldMapScreen::new);
			context.waitForScreen(WorldMapScreen.class);
			shot(context, site, "worldmap");
			context.setScreen(() -> null);

			// The village from its edge: minimap, clock, the current quest, a marker in the world
			teleport(context, world, village.offset(-28, 0, -22), -55, 8);
			shot(context, site, "hero");
			teleport(context, world, village.offset(30, 0, -26), 45, 12);
			shot(context, site, "minimap");
			// Sunset, the weather running so the barometer gets its forecast
			world.getServer().runCommand("gamerule advance_weather true");
			world.getServer().runCommand("weather clear 2400");
			world.getServer().runCommand("time set 12600");
			teleport(context, world, village.offset(-24, 0, 26), -140, 4);
			context.waitTicks(40);
			shot(context, site, "clock");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("time set 1000");

			// Worn on the back over the chestplate, seen from behind: on a lookout above the hillside, so
			// the camera clears the ground
			BlockPos lookout = surface(world, village.offset(-60, 0, -10)).above(8);
			world.getServer().runCommand(fill(lookout.offset(-6, 0, -6), lookout.offset(6, 6, 6), "air"));
			world.getServer().runCommand(fill(lookout.offset(-1, -1, -1), lookout.offset(1, -1, 1), "spruce_planks"));
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			world.getServer().runCommand("tp @a " + lookout.getX() + ".5 " + lookout.getY() + " " + lookout.getZ() + ".5 -90 18");
			settle(context);
			shot(context, site, "elytra");
			world.getServer().runCommand(fill(lookout.offset(-1, -1, -1), lookout.offset(1, -1, 1), "air"));
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));

			// A field of ripe wheat, one row harvested and planted again
			BlockPos field = surface(world, village.offset(-40, 0, 40));
			world.getServer().runCommand(fill(field.offset(-4, -1, 2), field.offset(4, -1, 8), "farmland[moisture=7]"));
			world.getServer().runCommand(fill(field.offset(0, -1, 2), field.offset(0, -1, 8), "water"));
			world.getServer().runCommand(fill(field.offset(-4, 0, 2), field.offset(4, 3, 8), "air"));
			world.getServer().runCommand(fill(field.offset(-4, 0, 2), field.offset(-1, 0, 8), "wheat[age=7]"));
			world.getServer().runCommand(fill(field.offset(1, 0, 2), field.offset(4, 0, 8), "wheat[age=7]"));
			world.getServer().runCommand(fill(field.offset(-1, 0, 2), field.offset(-1, 0, 4), "wheat[age=0]"));
			world.getServer().runCommand("item replace entity @a hotbar.0 with diamond_hoe");
			teleport(context, world, field, 0, 38);
			context.runOnClient(client -> {
				BlockPos wheat = field.offset(-1, 0, 5);
				client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(wheat), Direction.UP, wheat, false));
			});
			context.waitTicks(4);
			shot(context, site, "harvest");
			world.getServer().runCommand("item replace entity @a hotbar.0 with iron_sword");

			// A fight: a creeper and a husk after the player, a spider hurt
			BlockPos arena = surface(world, village.offset(-60, 0, -10));
			teleport(context, world, arena, -90, 4);
			world.getServer().runCommand("summon creeper " + pos(arena.offset(7, 0, -2)) + " {NoAI:1b,Rotation:[90f,0f]}");
			world.getServer().runCommand("summon husk " + pos(arena.offset(9, 0, 3)) + " {NoAI:1b,Rotation:[100f,0f]}");
			world.getServer().runCommand("summon spider " + pos(arena.offset(5, 0, 1)) + " {NoAI:1b,Rotation:[100f,0f]}");
			context.waitTicks(5);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16),
						mob -> mob.getType() == EntityTypes.CREEPER || mob.getType() == EntityTypes.HUSK).forEach(mob -> mob.setTarget(player));
			});
			world.getServer().runCommand("damage @e[type=spider,limit=1,sort=nearest] 7");
			world.getServer().runCommand("damage @e[type=husk,limit=1,sort=nearest] 4");
			context.waitTicks(25);
			shot(context, site, "combat");
			// Around the player: without it, the distance would count from the world's spawn
			world.getServer().runCommand("execute at @a run kill @e[type=!player,type=!villager,type=!iron_golem,distance=..30]");

			// A horse in the meadow, seen through the naturalist's spyglass, once the fight's mobs are gone: far enough
			// that the zoom shows it whole, the look aimed at its middle
			BlockPos grazing = surface(world, arena.offset(30, 0, 0));
			world.getServer().runCommand("summon horse " + pos(grazing) + " {NoAI:1b,Rotation:[0f,0f],Variant:3,"
					+ "attributes:[{id:\"minecraft:movement_speed\",base:0.29},{id:\"minecraft:jump_strength\",base:0.82},{id:\"minecraft:max_health\",base:26}]}");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with cassonade:naturalist_spyglass");
			// Raised at the sky first: used on the horse, it would ride it
			teleport(context, world, arena, -90, -60);
			context.waitTicks(3);
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(3);
			float down = (float) Math.toDegrees(Math.atan2(arena.getY() + 1.62 - (grazing.getY() + 1.0), 30));
			world.getServer().runCommand("tp @a " + arena.getX() + ".5 " + arena.getY() + " " + arena.getZ() + ".5 -90 " + down);
			context.waitTicks(40);
			// What the fight's mobs dropped as they died
			world.getServer().runCommand("execute at @a run kill @e[type=item,distance=..30]");
			shot(context, site, "spyglass");
			context.getInput().releaseKey(options -> options.keyUse);
			world.getServer().runCommand("item replace entity @a weapon.mainhand with iron_sword");
			world.getServer().runCommand("execute at @a run kill @e[type=horse,distance=..30]");

			// The sculk torch at night in the meadow: the faces on the grass where monsters could spawn, none by the torches
			world.getServer().runCommand("time set 18000");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with cassonade:sculk_torch");
			world.getServer().runCommand("execute at @a run setblock ~4 ~ ~-3 torch");
			teleport(context, world, arena, -90, 35);
			context.waitTicks(30);
			shot(context, site, "torch");
			world.getServer().runCommand("execute at @a run setblock ~4 ~ ~-3 air");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with iron_sword");
			world.getServer().runCommand("time set 1000");

			// The windows next, without the current quest over them
			context.runOnClient(client -> ClientSettings.setQuestTrackerDisplay(ClientSettings.QuestTrackerDisplay.HIDDEN));

			// The backpack open from its key, full of a trip's finds
			teleport(context, world, village.offset(-28, 0, -22), -55, 8);
			context.getInput().pressKey(CassonadeClient.OPEN_BACKPACK);
			context.waitForScreen(InventoryScreen.class);
			context.waitTicks(2);
			shot(context, site, "backpack");
			context.setScreen(() -> null);

			// A chest, a shulker box's contents shown in its tooltip
			BlockPos chest = surface(world, village.offset(-30, 0, -20));
			world.getServer().runCommand("setblock " + pos(chest) + " chest{Items:[{Slot:0b,id:\"minecraft:cobblestone\",count:64},"
					+ "{Slot:1b,id:\"minecraft:cobblestone\",count:64},{Slot:2b,id:\"minecraft:andesite\",count:41},{Slot:3b,id:\"minecraft:coal\",count:23},"
					+ "{Slot:4b,id:\"minecraft:iron_ingot\",count:12},{Slot:9b,id:\"minecraft:oak_log\",count:64},{Slot:10b,id:\"minecraft:oak_log\",count:30},"
					+ "{Slot:11b,id:\"minecraft:birch_log\",count:18},{Slot:12b,id:\"minecraft:wheat_seeds\",count:20},{Slot:18b,id:\"minecraft:bread\",count:9},"
					+ "{Slot:19b,id:\"minecraft:carrot\",count:14},{Slot:20b,id:\"minecraft:redstone\",count:27}]}");
			world.getServer().runCommand("item replace entity @a inventory.4 with shulker_box[container=[{slot:0,item:{id:\"minecraft:diamond\",count:7}},"
					+ "{slot:1,item:{id:\"minecraft:emerald\",count:12}},{slot:2,item:{id:\"minecraft:golden_apple\",count:2}},"
					+ "{slot:3,item:{id:\"minecraft:ender_pearl\",count:5}},{slot:5,item:{id:\"minecraft:lapis_lazuli\",count:30}},"
					+ "{slot:9,item:{id:\"minecraft:experience_bottle\",count:16}}]]");
			context.waitTicks(2);
			world.getServer().runOnServer(server -> player(server).openMenu((MenuProvider) server.overworld().getBlockEntity(chest)));
			context.waitForScreen(ContainerScreen.class);
			hover(context, 27 + 4);
			shot(context, site, "inventory");
			context.runOnClient(client -> client.player.closeContainer());

			// The hopper's filter, stone only, under a chest of stone and dirt
			BlockPos hopper = surface(world, village.offset(-32, 0, -20));
			world.getServer().runCommand("setblock " + pos(hopper) + " hopper");
			world.getServer().runCommand("setblock " + pos(hopper.above()) + " chest{Items:[{Slot:0b,id:\"minecraft:dirt\",count:3},"
					+ "{Slot:1b,id:\"minecraft:stone\",count:3},{Slot:2b,id:\"minecraft:dirt\",count:2}]}");
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				player.openMenu((MenuProvider) server.overworld().getBlockEntity(hopper));
				player.containerMenu.getSlot(HopperFilterMenu.FILTER_SLOT).set(new ItemStack(Items.STONE));
			});
			context.waitTicks(40);
			shot(context, site, "hopper");
			context.runOnClient(client -> client.player.closeContainer());

			// The crafting table's recipe book, a recipe searched
			BlockPos table = surface(world, village.offset(-34, 0, -20));
			world.getServer().runCommand("setblock " + pos(table) + " crafting_table");
			context.waitTicks(2);
			world.getServer().runOnServer(server -> player(server).openMenu(server.overworld().getBlockState(table).getMenuProvider(server.overworld(), table)));
			context.waitForScreen(CraftingScreen.class);
			context.waitTicks(2);
			context.runOnClient(client -> recipeButton(client).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0)));
			// The recipes seen for the first time bounce in the book a moment: still before the shot
			context.waitTicks(40);
			shot(context, site, "interface");
			context.runOnClient(client -> client.player.closeContainer());

			// The quest journal, some way into the story
			context.setScreen(QuestJournalScreen::new);
			context.waitForScreen(QuestJournalScreen.class);
			shot(context, site, "quests");

			// The advancements screen, with what is left for each
			context.setScreen(() -> new AdvancementsListScreen(null));
			context.waitForScreen(AdvancementsListScreen.class);
			shot(context, site, "advancements");
			context.setScreen(() -> null);

			details(context, world, site, arena, home, village);
			context.runOnClient(client -> ClientSettings.setQuestTrackerDisplay(ClientSettings.QuestTrackerDisplay.FULL));
		}
		worlds(context, site);
		context.runOnClient(client -> {
			client.options.chatVisibility().set(ChatVisiblity.FULL);
			client.options.guiScale().set(0);
			client.resizeGui();
		});
	}

	/**
	 * What came after the first release's shots: the woodcutter, the furnace's "!" tooltip, the biome under the
	 * cursor on the world map, bees seen through the spyglass, and the small tweaks read from where the crosshair points.
	 */
	private static void details(ClientGameTestContext context, TestSingleplayerContext world, Path site, BlockPos arena, BlockPos home, BlockPos village) {
		teleport(context, world, arena, -90, 0);
		// The woodcutter, a stack of planks in, its stairs chosen
		BlockPos bench = surface(world, arena.offset(3, 0, 0));
		world.getServer().runCommand("setblock " + pos(bench) + " cassonade:woodcutter");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.openMenu(server.overworld().getBlockState(bench).getMenuProvider(server.overworld(), bench));
			player.containerMenu.getSlot(0).set(new ItemStack(Items.SPRUCE_PLANKS, 24));
			player.containerMenu.slotsChanged(null);
			player.containerMenu.clickMenuButton(player, 0);
		});
		context.waitForScreen(StonecutterScreen.class);
		context.waitTicks(4);
		hover(context, 1);
		shot(context, site, "woodcutter");
		context.runOnClient(client -> client.player.closeContainer());
		world.getServer().runCommand("setblock " + pos(bench) + " air");

		// A furnace at work, the cursor on the "!" by its title
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			BlockPos pos = surface(server, arena.offset(3, 0, 0));
			player.level().setBlockAndUpdate(pos, Blocks.FURNACE.defaultBlockState());
			AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) player.level().getBlockEntity(pos);
			((Container) furnace).setItem(AbstractFurnaceMenu.INGREDIENT_SLOT, new ItemStack(Items.RAW_IRON, 12));
			((Container) furnace).setItem(AbstractFurnaceMenu.FUEL_SLOT, new ItemStack(Items.COAL, 3));
			((AbstractFurnaceBlockEntityAccessor) furnace).cassonade$getRecipesUsed()
					.addTo(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("iron_ingot_from_smelting_raw_iron")), 9);
			player.openMenu((MenuProvider) furnace);
		});
		context.waitForScreen(FurnaceScreen.class);
		context.waitTicks(30);
		double[] icon = context.computeOnClient(client -> {
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
			AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
			int[] position = InfoPanel.iconPosition(new InfoPanel.Window(accessor.cassonade$getLeftPos(), accessor.cassonade$getTopPos(),
					accessor.cassonade$getImageWidth(), accessor.cassonade$getImageHeight(), screen.width, screen.height));
			double scale = client.getWindow().getGuiScale();
			return new double[] {(position[0] + 5) * scale, (position[1] + 5) * scale};
		});
		context.getInput().setCursorPos(icon[0], icon[1]);
		context.waitTicks(3);
		shot(context, site, "interface-info");
		context.runOnClient(client -> client.player.closeContainer());
		world.getServer().runCommand("setblock " + pos(surface(world, arena.offset(3, 0, 0)).below()) + " grass_block");

		// The world map with Shift held over a place: the name of its biome
		teleport(context, world, home, 0, 0);
		context.setScreen(WorldMapScreen::new);
		context.waitForScreen(WorldMapScreen.class);
		context.getInput().holdShift();
		context.getInput().setCursorPos(1180, 560);
		context.waitTicks(5);
		shot(context, site, "worldmap-biome");
		context.getInput().releaseShift();
		context.setScreen(() -> null);

		// Bread in hand on a half-empty hunger bar: the drumsticks it would restore blink
		teleport(context, world, village.offset(30, 0, -26), 45, 12);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.getFoodData().setFoodLevel(9);
			player.getInventory().setSelectedSlot(5);
		});
		context.runOnClient(client -> client.player.getInventory().setSelectedSlot(5));
		context.waitTicks(8);
		shot(context, site, "inventory-food");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.getFoodData().setFoodLevel(20);
			player.getInventory().setSelectedSlot(0);
		});
		context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));

		// A flat stage in the meadow, 30 blocks of it to the east
		world.getServer().runCommand(fill(arena.offset(-2, -4, -7), arena.offset(30, -2, 7), "dirt"));
		world.getServer().runCommand(fill(arena.offset(-2, -1, -7), arena.offset(30, -1, 7), "grass_block"));
		world.getServer().runCommand(fill(arena.offset(-2, 0, -7), arena.offset(30, 8, 7), "air"));
		world.getServer().runCommand("execute at @a run kill @e[type=item,distance=..30]");

		// Bees inside a hive, through the naturalist's spyglass
		BlockPos hive = arena.offset(24, 2, 0);
		world.getServer().runCommand(fill(hive.below(2), hive.below(), "oak_fence"));
		world.getServer().runCommand("setblock " + pos(hive) + " beehive[facing=west,honey_level=3]");
		context.waitTicks(3);
		world.getServer().runOnServer(server -> {
			BeehiveBlockEntity entity = (BeehiveBlockEntity) server.overworld().getBlockEntity(hive);
			entity.storeBee(BeehiveBlockEntity.Occupant.create(100));
			entity.storeBee(BeehiveBlockEntity.Occupant.create(100));
		});
		world.getServer().runCommand("item replace entity @a weapon.mainhand with cassonade:naturalist_spyglass");
		world.getServer().runCommand("tp @a " + pos(arena) + " -90 -60");
		context.waitTicks(3);
		context.getInput().holdKey(options -> options.keyUse);
		context.waitTicks(3);
		float toHive = (float) Math.toDegrees(Math.atan2(1.62 - 2.5, 24));
		world.getServer().runCommand("tp @a " + arena.getX() + ".5 " + arena.getY() + " " + arena.getZ() + ".5 -90 " + toHive);
		context.waitTicks(40);
		shot(context, site, "spyglass-bees");
		context.getInput().releaseKey(options -> options.keyUse);
		world.getServer().runCommand("item replace entity @a weapon.mainhand with iron_sword");
		world.getServer().runCommand(fill(hive.below(2), hive, "air"));

		// What the crosshair reads at arm's length, an item in hand or not: the same stage, a block at a time
		// A wire's power, redstone dust in hand, away from a block of redstone
		BlockPos at = arena.offset(3, 0, 0);
		world.getServer().runCommand("setblock " + pos(at.offset(0, 0, 3)) + " redstone_block");
		world.getServer().runCommand(fill(at.offset(0, 0, 0), at.offset(0, 0, 2), "redstone_wire"));
		world.getServer().runCommand("item replace entity @a weapon.mainhand with redstone");
		read(context, world, arena, at.offset(0, 0, 1), 0.1);
		shot(context, site, "tweaks");
		world.getServer().runCommand(fill(at.offset(0, 0, 0), at.offset(0, 0, 3), "air"));

		// Waxed copper among its unwaxed neighbors, a honeycomb in hand
		world.getServer().runCommand("setblock " + pos(at.offset(0, 0, -1)) + " exposed_copper");
		world.getServer().runCommand("setblock " + pos(at) + " waxed_exposed_copper");
		world.getServer().runCommand("setblock " + pos(at.offset(0, 0, 1)) + " weathered_copper");
		world.getServer().runCommand("item replace entity @a weapon.mainhand with honeycomb");
		read(context, world, arena, at, 0.5);
		shot(context, site, "tweaks-copper");
		world.getServer().runCommand(fill(at.offset(0, 0, -1), at.offset(0, 0, 1), "air"));

		// A block left on grass turns it to dirt
		String grass = fill(arena.offset(0, -1, -5), arena.offset(10, -1, 5), "grass_block");
		world.getServer().runCommand(grass);
		// A double chest renamed at the anvil, an empty hand
		world.getServer().runCommand("setblock " + pos(at) + " chest[type=left,facing=west]");
		world.getServer().runCommand("setblock " + pos(at.offset(0, 0, -1)) + " chest[type=right,facing=west]{CustomName:\"Ores\"}");
		world.getServer().runCommand("item replace entity @a weapon.mainhand with air");
		read(context, world, arena, at, 0.5);
		shot(context, site, "tweaks-chest");
		world.getServer().runCommand(fill(at.offset(0, 0, -1), at, "air"));

		// A chiseled bookshelf, enchanted books and a written one
		String enchanted = "{id:\"minecraft:enchanted_book\",count:1,components:{\"minecraft:stored_enchantments\":{\"minecraft:%s\":%d}}}";
		String shelf = "{Slot:0b,id:\"minecraft:written_book\",count:1,components:{\"minecraft:written_book_content\":{title:\"Field Notes\",author:\"Alex\",pages:[]}}}";
		StringBuilder books = new StringBuilder(shelf);
		String[][] stored = {{"mending", "1"}, {"efficiency", "5"}, {"unbreaking", "3"}, {"fortune", "3"}, {"sharpness", "5"}};
		for (int slot = 0; slot < stored.length; slot++) {
			books.append(",").append(enchanted.formatted(stored[slot][0], Integer.parseInt(stored[slot][1])).replace("{id", "{Slot:" + (slot + 1) + "b,id"));
		}
		world.getServer().runCommand("setblock " + pos(at) + " chiseled_bookshelf[facing=west,slot_0_occupied=true,slot_1_occupied=true,slot_2_occupied=true,slot_3_occupied=true,slot_4_occupied=true,slot_5_occupied=true]{Items:[" + books + "]}");
		world.getServer().runCommand("item replace entity @a weapon.mainhand with iron_sword");
		world.getServer().runCommand("tp @a " + (arena.getX() + 0.5) + " " + arena.getY() + " " + (arena.getZ() + 0.5) + " -90 "
				+ (float) Math.toDegrees(Math.atan2(1.62 - 0.75, 2.5)));
		context.waitTicks(8);
		shot(context, site, "tweaks-bookshelf");
		world.getServer().runCommand("setblock " + pos(at) + " air");

		world.getServer().runCommand(grass);
		// An armor stand with arms, as Bedrock places it, wearing armor and a sword
		world.getServer().runCommand("summon armor_stand " + (at.getX() + 2.5) + " " + at.getY() + " " + (at.getZ() + 0.5)
				+ " {ShowArms:1b,Rotation:[90f,0f],Pose:{RightArm:[-15f,0f,10f],LeftArm:[10f,0f,-10f]},"
				+ "equipment:{head:{id:\"minecraft:iron_helmet\",count:1},chest:{id:\"minecraft:iron_chestplate\",count:1},"
				+ "legs:{id:\"minecraft:iron_leggings\",count:1},feet:{id:\"minecraft:iron_boots\",count:1},"
				+ "mainhand:{id:\"minecraft:iron_sword\",count:1}}}");
		world.getServer().runCommand("item replace entity @a weapon.mainhand with shears");
		world.getServer().runCommand("tp @a " + (arena.getX() + 0.5) + " " + arena.getY() + " " + (arena.getZ() + 0.5) + " -90 4");
		context.waitTicks(10);
		shot(context, site, "tweaks-armor-stand");
		world.getServer().runCommand("kill @e[type=armor_stand]");
		world.getServer().runCommand("item replace entity @a weapon.mainhand with iron_sword");
	}

	/** Standing at the stage's start, the crosshair on a block, for what it reads once the panel is there. */
	private static void read(ClientGameTestContext context, TestSingleplayerContext world, BlockPos from, BlockPos target, double height) {
		double dx = target.getX() - from.getX();
		double dz = target.getZ() - from.getZ();
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) Math.toDegrees(Math.atan2(1.62 - height, Math.hypot(dx, dz)));
		world.getServer().runCommand("tp @a " + from.getX() + ".5 " + from.getY() + " " + from.getZ() + ".5 " + yaw + " " + pitch);
		context.waitTicks(10);
	}

	private static void settings(WorldCreationUiState state) {
		state.setName(WORLD_NAME);
		state.setSeed(SEED);
		state.setWorldType(state.getNormalPresetList().getFirst());
		state.setGenerateStructures(true);
		state.setAllowCommands(true);
		state.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
	}

	/** Everything the mod's features need, and a lived-in hotbar. */
	private static void equip(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			List<ItemStack> finds = new ArrayList<>();
			for (Object[] find : new Object[][] {{Items.OAK_LOG, 64}, {Items.OAK_LOG, 37}, {Items.COBBLESTONE, 64}, {Items.COBBLESTONE, 64},
					{Items.DEEPSLATE, 48}, {Items.COAL, 34}, {Items.RAW_IRON, 21}, {Items.RAW_COPPER, 15}, {Items.RAW_GOLD, 6},
					{Items.REDSTONE, 28}, {Items.LAPIS_LAZULI, 17}, {Items.DIAMOND, 4}, {Items.FLINT, 9}, {Items.STRING, 13},
					{Items.BONE, 8}, {Items.GUNPOWDER, 5}, {Items.ARROW, 32}, {Items.BREAD, 11}, {Items.CARROT, 19}, {Items.APPLE, 6},
					{Items.WHEAT_SEEDS, 24}, {Items.SUGAR_CANE, 12}, {Items.LEATHER, 7}, {Items.FEATHER, 10}, {Items.CLAY_BALL, 16},
					{Items.SAND, 40}, {Items.GRAVEL, 22}, {Items.AMETHYST_SHARD, 8}}) {
				finds.add(new ItemStack((Item) find[0], (Integer) find[1]));
			}
			ItemStack backpack = new ItemStack(CassonadeItems.BACKPACK);
			backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(finds));
			player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player)
					.with(GearSlot.BACKPACK, backpack)
					.with(GearSlot.COMPASS, new ItemStack(CassonadeItems.SCULK_COMPASS))
					.with(GearSlot.MAP, new ItemStack(CassonadeItems.WORLD_MAP))
					.with(GearSlot.CLOCK, new ItemStack(CassonadeItems.BAROMETER))
					.with(GearSlot.ELYTRA, new ItemStack(Items.ELYTRA)));
			player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
			player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
			player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
			Item[] hotbar = {Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE, Items.STONE_SHOVEL, Items.TORCH, Items.BREAD,
					Items.OAK_PLANKS, Items.WATER_BUCKET, Items.CRAFTING_TABLE};
			int[] counts = {1, 1, 1, 1, 48, 14, 52, 1, 1};
			for (int i = 0; i < hotbar.length; i++) {
				player.getInventory().setItem(i, new ItemStack(hotbar[i], counts[i]));
			}
			player.getInventory().setSelectedSlot(0);
			player.setExperienceLevels(23);
		});
	}

	/** On the ground at that column, looking that way, once the chunks are drawn. */
	private static void teleport(ClientGameTestContext context, TestSingleplayerContext world, BlockPos column, float yaw, float pitch) {
		BlockPos ground = world.getServer().computeOnServer(server -> surface(server, column));
		world.getServer().runCommand("tp @a " + ground.getX() + ".5 " + ground.getY() + " " + ground.getZ() + ".5 " + yaw + " " + pitch);
		settle(context);
	}

	/**
	 * The chunks drawn, or a minute at most: a generated world's lighting may never settle all the way,
	 * which Fabric's waitForChunksRender waits for.
	 */
	private static void settle(ClientGameTestContext context) {
		try {
			context.waitFor(client -> client.levelRenderer.hasRenderedAllSections(), 20 * 60);
		} catch (AssertionError tooLong) {
			// Drawn enough for a picture
		}
		context.waitTicks(40);
	}

	private static BlockPos surface(MinecraftServer server, BlockPos column) {
		ServerLevel level = server.overworld();
		level.getChunk(column);
		return new BlockPos(column.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ()), column.getZ());
	}

	private static BlockPos surface(TestSingleplayerContext world, BlockPos column) {
		return world.getServer().computeOnServer(server -> surface(server, column));
	}

	private static String pos(BlockPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ();
	}

	private static String fill(BlockPos from, BlockPos to, String block) {
		return "fill " + pos(from) + " " + pos(to) + " " + block;
	}

	/** The cursor over a slot of the open window, for its tooltip. */
	private static void hover(ClientGameTestContext context, int slotIndex) {
		double[] center = context.computeOnClient(client -> {
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
			Slot slot = screen.getMenu().getSlot(slotIndex);
			AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
			double scale = client.getWindow().getGuiScale();
			return new double[] {(accessor.cassonade$getLeftPos() + slot.x + 8) * scale, (accessor.cassonade$getTopPos() + slot.y + 8) * scale};
		});
		context.getInput().setCursorPos(center[0], center[1]);
		context.waitTicks(3);
	}

	private static ImageButton recipeButton(Minecraft client) {
		return client.gui.screen().children().stream()
				.filter(widget -> widget instanceof ImageButton button
						&& ((ImageButtonAccessor) button).cassonade$getSprites() == RecipeBookComponent.RECIPE_BUTTON_SPRITES)
				.map(ImageButton.class::cast).findFirst().orElseThrow();
	}

	/** A screen of one plain color, the icons drawn on it in a row, for blockIcons to cut them out. */
	private static final class IconSheet extends Screen {
		static final int KEY = 0xFFFF00FF;
		static final int STEP = 24;
		private final Item[] items;

		IconSheet(Item[] items) {
			super(Component.empty());
			this.items = items;
		}

		@Override
		public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
			graphics.fill(0, 0, width, height, KEY);
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
			for (int i = 0; i < items.length; i++) {
				graphics.item(new ItemStack(items[i]), STEP + i * STEP, STEP);
			}
		}
	}

	/** Each item drawn by the game at 16 pixels, cut out of a screenshot, saved as <id>.png with a clear background. */
	private static void blockIcons(ClientGameTestContext context, Path folder, Item... items) {
		int scale = context.computeOnClient(client -> client.options.guiScale().get());
		context.runOnClient(client -> {
			client.options.guiScale().set(1);
			client.resizeGui();
		});
		context.setScreen(() -> new IconSheet(items));
		context.waitTicks(3);
		Path sheet = Screenshots.take(context, "site-icons");
		try (NativeImage image = NativeImage.read(Files.readAllBytes(sheet))) {
			Files.createDirectories(folder);
			for (int i = 0; i < items.length; i++) {
				try (NativeImage icon = new NativeImage(16, 16, true)) {
					for (int y = 0; y < 16; y++) {
						for (int x = 0; x < 16; x++) {
							int color = image.getPixel(IconSheet.STEP + i * IconSheet.STEP + x, IconSheet.STEP + y);
							icon.setPixel(x, y, color == IconSheet.KEY ? 0 : color);
						}
					}
					icon.writeToFile(folder.resolve(BuiltInRegistries.ITEM.getKey(items[i]).getPath() + ".png"));
				}
			}
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		context.setScreen(() -> null);
		context.runOnClient(client -> {
			client.options.guiScale().set(scale);
			client.resizeGui();
		});
	}

	/**
	 * The world selection: the shots' world, with two more beside it, played on other days and for longer than
	 * the tests do, so that the screen shows what a player's would.
	 */
	private static void worlds(ClientGameTestContext context, Path site) {
		extraWorld(context, "Creative Builds", "cassonade builds", WorldCreationUiState.SelectedGameMode.CREATIVE);
		extraWorld(context, "Hardcore Run", "cassonade hardcore", WorldCreationUiState.SelectedGameMode.HARDCORE);
		long now = System.currentTimeMillis();
		Path saves = context.computeOnClient(client -> client.getLevelSource().getBaseDir());
		stage(saves, WORLD_NAME, now - 5 * 60_000, 42 * HOUR + 17 * 60_000);
		stage(saves, "Creative Builds", now - 2 * 24 * HOUR - 3 * HOUR, 13 * HOUR + 40 * 60_000);
		stage(saves, "Hardcore Run", now - 9 * 24 * HOUR, 6 * HOUR + 5 * 60_000);
		context.setScreen(() -> new SelectWorldScreen(new TitleScreen()));
		context.waitForScreen(WorldSelectionScreen.class);
		context.waitTicks(20);
		shot(context, site, "interface-worlds");
		context.setScreen(TitleScreen::new);
	}

	private static void extraWorld(ClientGameTestContext context, String name, String seed, WorldCreationUiState.SelectedGameMode mode) {
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(state -> {
			state.setName(name);
			state.setSeed(seed);
			state.setGameMode(mode);
		}).create()) {
			settle(context);
			context.waitForScreen(QuestOptInScreen.class);
			// Dropped rather than waited for, as the quest test does: the first advancement's banner, out of the icon
			context.runOnClient(client -> AchievementBanner.clear());
			context.setScreen(() -> null);
			// The game takes the world's icon a moment after joining: as soon as it is saved
			context.waitFor(client -> client.getSingleplayerServer().getWorldScreenshotFile().map(Files::isRegularFile).orElse(true), 20 * 10);
		}
	}

	/** Dates the world named so, in its level.dat, and sets the time played in its statistics. */
	private static void stage(Path saves, String name, long lastPlayed, long playedMillis) {
		try (Stream<Path> folders = Files.list(saves)) {
			for (Path folder : (Iterable<Path>) folders::iterator) {
				Path levelDat = folder.resolve("level.dat");
				if (!Files.isRegularFile(levelDat)) {
					continue;
				}
				CompoundTag root = NbtIo.readCompressed(levelDat, NbtAccounter.unlimitedHeap());
				CompoundTag data = root.getCompoundOrEmpty("Data");
				if (!name.equals(data.getStringOr("LevelName", ""))) {
					continue;
				}
				data.putLong("LastPlayed", lastPlayed);
				root.put("Data", data);
				NbtIo.writeCompressed(root, levelDat);
				try (Stream<Path> files = Files.list(folder.resolve(LevelResource.PLAYER_STATS_DIR.id()))) {
					for (Path file : (Iterable<Path>) files::iterator) {
						JsonObject stats = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
						stats.getAsJsonObject("stats").getAsJsonObject("minecraft:custom").addProperty("minecraft:play_time", playedMillis / 50);
						Files.writeString(file, stats.toString());
					}
				}
				return;
			}
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		throw new AssertionError("Expected: a saved world named " + name);
	}

	/** Without the banners and toasts of what just happened, copied to the site under its id. */
	private static void shot(ClientGameTestContext context, Path site, String id) {
		context.runOnClient(client -> {
			AchievementBanner.clear();
			client.gui.toastManager().clear();
		});
		context.waitTicks(3);
		Path taken = Screenshots.take(context, "site-" + id);
		try {
			Files.createDirectories(site);
			Files.copy(taken, site.resolve(id + ".png"), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}
}
