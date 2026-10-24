package com.vanillasugar.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.platform.InputConstants;

import com.vanillasugar.client.ClientSettings;
import com.vanillasugar.client.VanillaSugarClient;
import com.vanillasugar.client.map.WorldMapScreen;
import com.vanillasugar.client.mixin.AbstractContainerScreenAccessor;
import com.vanillasugar.client.mixin.ImageButtonAccessor;
import com.vanillasugar.client.quest.AchievementBanner;
import com.vanillasugar.client.quest.AdvancementsListScreen;
import com.vanillasugar.client.quest.QuestJournalScreen;
import com.vanillasugar.client.quest.QuestLog;
import com.vanillasugar.client.quest.QuestOptInScreen;
import com.vanillasugar.compass.PlayerMarkers;
import com.vanillasugar.gear.GearSlot;
import com.vanillasugar.gear.PlayerGear;
import com.vanillasugar.hopper.HopperFilterMenu;
import com.vanillasugar.item.VanillaSugarItems;

/**
 * The website's screenshots (site/assets/screenshots), in a generated world by a plains village: run
 * on demand only, with {@code ./gradlew runClientGameTest -PsiteScreenshots}. Each is copied to the
 * site as {@code <id>.png}.
 */
public class SiteScreenshotsClientGameTest implements FabricClientGameTest {
	private static final String SEED = "vanillasugar";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		String folder = System.getProperty("vanillasugar.siteScreenshots");
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
			// Sunset, the weather running so the clock gets its forecast
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
			world.getServer().runCommand("kill @e[type=!player,type=!villager,type=!iron_golem,distance=..30]");

			// The windows next, without the current quest over them
			context.runOnClient(client -> ClientSettings.setQuestTrackerDisplay(ClientSettings.QuestTrackerDisplay.HIDDEN));

			// The backpack open from its key, full of a trip's finds
			teleport(context, world, village.offset(-28, 0, -22), -55, 8);
			context.getInput().pressKey(VanillaSugarClient.OPEN_BACKPACK);
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
			context.waitTicks(5);
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
			context.runOnClient(client -> ClientSettings.setQuestTrackerDisplay(ClientSettings.QuestTrackerDisplay.FULL));
		}
		context.runOnClient(client -> {
			client.options.chatVisibility().set(ChatVisiblity.FULL);
			client.options.guiScale().set(0);
			client.resizeGui();
		});
	}

	private static void settings(WorldCreationUiState state) {
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
			ItemStack backpack = new ItemStack(VanillaSugarItems.BACKPACK);
			backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(finds));
			player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player)
					.with(GearSlot.BACKPACK, backpack)
					.with(GearSlot.COMPASS, new ItemStack(Items.COMPASS))
					.with(GearSlot.MAP, new ItemStack(VanillaSugarItems.WORLD_MAP))
					.with(GearSlot.CLOCK, new ItemStack(Items.CLOCK))
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
			return new double[] {(accessor.vanillasugar$getLeftPos() + slot.x + 8) * scale, (accessor.vanillasugar$getTopPos() + slot.y + 8) * scale};
		});
		context.getInput().setCursorPos(center[0], center[1]);
		context.waitTicks(3);
	}

	private static ImageButton recipeButton(Minecraft client) {
		return client.gui.screen().children().stream()
				.filter(widget -> widget instanceof ImageButton button
						&& ((ImageButtonAccessor) button).vanillasugar$getSprites() == RecipeBookComponent.RECIPE_BUTTON_SPRITES)
				.map(ImageButton.class::cast).findFirst().orElseThrow();
	}

	/** Without the banners and toasts of what just happened, copied to the site under its id. */
	private static void shot(ClientGameTestContext context, Path site, String id) {
		context.runOnClient(client -> {
			AchievementBanner.clear();
			client.gui.toastManager().clear();
		});
		context.waitTicks(3);
		Path taken = context.takeScreenshot("site-" + id);
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
