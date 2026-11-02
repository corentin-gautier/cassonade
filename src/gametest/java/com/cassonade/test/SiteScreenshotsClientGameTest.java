package com.cassonade.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import net.minecraft.SharedConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
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
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.server.level.progress.LevelLoadListener;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;

import com.cassonade.client.ClientSettings;
import com.cassonade.client.ambience.MorningMist;
import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Butterflies;
import com.cassonade.client.CassonadeClient;
import com.cassonade.client.map.MapRegion;
import com.cassonade.client.map.TerrainColors;
import com.cassonade.client.map.WorldMap;
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
import com.cassonade.client.waystone.WaystoneScreen;
import com.cassonade.waystone.WaystoneListPayload;
import com.cassonade.waystone.Waystones;
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
	/** Where the world map is explored, around the village, before the shots (see siteWorld). */
	private static final int[][] ROUTE = {{0, 0}, {96, 0}, {192, 48}, {240, 144}, {192, 240}, {96, 288}, {0, 240}, {-96, 192},
			{-144, 96}, {-96, 0}, {-48, -96}, {48, -144}, {144, -96}};
	/** Beside the world kept between runs: what it was made from, so that it is made again when that changes. */
	static final String CACHE_KEY_FILE = "cassonade-site-world.txt";

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
			// The test framework restores the keys as they were at startup, before LabeledKeys moved them to the
			// keyboard's layout: the world map's help would name the key at M's QWERTY position
			for (KeyMapping mapping : client.options.keyMappings) {
				if (mapping.getCategory() == CassonadeClient.CATEGORY) {
					mapping.setKey(mapping.getDefaultKey());
				}
			}
			KeyMapping.resetMapping();
		});
		try (TestSingleplayerContext world = siteWorld(context, Path.of(System.getProperty("cassonade.siteWorld")))) {
			settle(context);
			// The world is kept from one run to the next: nothing left lying around by an earlier one
			world.getServer().runCommand("kill @e[type=item,nbt={Item:{id:\"cassonade:waystone\"}}]");
			// The blocks among the recipes' ingredients have no flat texture: drawn by the game as in the inventory, at its
			// smallest scale, so they match the other items' 16 pixels; once in a world, where items have their components
			blockIcons(context, site.resolveSibling("items"), Items.LIGHTNING_ROD.weathering().unaffected(), Items.SCULK_SENSOR, Woodcutter.ITEM, Waystones.ITEM, Items.CHISELED_STONE_BRICKS);
			rules(world);
			BlockPos village = village(world);
			equip(world);
			world.getServer().runCommand("advancement grant @a until minecraft:story/iron_tools");
			world.getServer().runCommand("advancement grant @a until minecraft:adventure/sleep_in_bed");
			world.getServer().runCommand("recipe give @a *");

			BlockPos home = village.offset(20, 0, 20);
			String only = System.getProperty("cassonade.siteShots");
			if (only != null) {
				// Just these, to retouch a shot without waiting for all the others
				context.runOnClient(client -> ClientSettings.setQuestTrackerDisplay(ClientSettings.QuestTrackerDisplay.HIDDEN));
				ambience(context, world, site, home, List.of(only.split(","))::contains);
				context.runOnClient(client -> ClientSettings.setQuestTrackerDisplay(ClientSettings.QuestTrackerDisplay.FULL));
				return;
			}
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				PlayerMarkers.place(player, GlobalPos.of(Level.OVERWORLD, surface(server, village.offset(110, 0, 70))), "Mine", DyeColor.ORANGE);
				PlayerMarkers.place(player, GlobalPos.of(Level.OVERWORLD, surface(server, village.offset(-120, 0, -50))), "Tower", DyeColor.BLUE);
				PlayerMarkers.place(player, GlobalPos.of(Level.OVERWORLD, surface(server, village.offset(-40, 0, 45))), "Farm", DyeColor.LIME);
			});
			teleport(context, world, home, 0, 0);
			context.setScreen(WorldMapScreen::new);
			context.waitForScreen(WorldMapScreen.class);
			// On the map, so its corner shows the position under the cursor wherever the real pointer is
			context.getInput().setCursorPos(1180, 560);
			context.waitTicks(5);
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
			ambience(context, world, site, home, id -> true);
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
		loading(context, site);
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

		// A waystone on the lawn, then its window with a row for each dimension
		BlockPos stone = arena.offset(4, 0, 3);
		world.getServer().runCommand("item replace entity @a weapon.mainhand with air");
		world.getServer().runCommand("setblock " + pos(stone) + " cassonade:waystone");
		world.getServer().runCommand("setblock " + pos(stone.above()) + " cassonade:waystone[half=upper]");
		// Aimed slantwise, the morning sun out from behind it; the command aims from the feet, so lower than what is looked at
		teleport(context, world, arena, -90, 0);
		world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ facing " + stone.getX() + ".5 " + (stone.getY() - 1.2) + " " + stone.getZ() + ".5");
		context.waitTicks(30);
		shot(context, site, "waystone");
		context.runOnClient(client -> WaystoneScreen.open(client, new WaystoneListPayload(stone, "", true, List.of(
				new WaystoneListPayload.Destination(GlobalPos.of(Level.OVERWORLD, village), "Village"),
				new WaystoneListPayload.Destination(GlobalPos.of(Level.NETHER, new BlockPos(24, 70, -112)), "Fortress"),
				new WaystoneListPayload.Destination(GlobalPos.of(Level.END, new BlockPos(1040, 62, 8)), "")))));
		context.waitForScreen(WaystoneScreen.class);
		context.waitTicks(3);
		shot(context, site, "waystone-list");
		context.setScreen(() -> null);
		// The lower half first: taken away from under the top, nothing drops, whereas without its top it would break into an item
		world.getServer().runCommand("setblock " + pos(stone) + " air");
		world.getServer().runCommand("setblock " + pos(stone.above()) + " air");

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

	/**
	 * A flock passing, its wait and the time set by hand, the time standing still in this world.
	 */
	private static void ambience(ClientGameTestContext context, TestSingleplayerContext world, Path site, BlockPos home, Predicate<String> wanted) {
		context.runOnClient(client -> Birds.setSpawning(false));
		if (wanted.test("ambience")) {
			birds(context, world, site, home);
		}
		if (wanted.test("ambience-butterfly") || wanted.test("ambience-night")) {
			plain(context, world, site, home, wanted);
		}
		if (wanted.test("ambience-mist")) {
			BlockPos wood = farFrom(world, home, biome -> biome.is(BiomeTags.IS_FOREST), (level, ground) -> treesAround(level, ground) * 3 - bumps(level, ground) * 20 - Math.max(0, ground.getY() - 75) * 20);
			teleport(context, world, wood, 0, -3);
			world.getServer().runCommand("time set " + MistyMorning.sunrise());
			context.waitFor(client -> MorningMist.density() == 1, 20 * 20);
			shot(context, site, "ambience-mist");
		}
		world.getServer().runCommand("time set 1000");
		context.runOnClient(client -> Birds.setSpawning(true));
	}

	private static void birds(ClientGameTestContext context, TestSingleplayerContext world, Path site, BlockPos home) {
		teleport(context, world, home, 0, 0);
		world.getServer().runCommand("time set 2000");
		context.runOnClient(client -> Birds.release(client, 38));
		context.waitTicks(420);
		// From nearer than the ground: from there, they are specks
		world.getServer().runCommand("gamemode spectator @a");
		Vec3 flock = context.computeOnClient(client -> Birds.position());
		world.getServer().runCommand("tp @a " + flock.x + " " + (flock.y - 16) + " " + (flock.z - 12) + " 0 -50");
		context.waitTicks(4);
		shot(context, site, "ambience");
		world.getServer().runCommand("gamemode survival @a");
		teleport(context, world, home, 0, 0);
		context.runOnClient(client -> Birds.setSpawning(false));
	}

	private static void plain(ClientGameTestContext context, TestSingleplayerContext world, Path site, BlockPos home, Predicate<String> wanted) {
		// Far from the village, on open plains: butterflies and pollen among planted flowers at midday, the Milky Way and the moon by night;
		// the mist among the trees in the morning
		BlockPos plain = farFrom(world, home, biome -> biome.is(Biomes.PLAINS), (level, ground) -> -treesAround(level, ground) * 100 - bumps(level, ground) * 50);
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			Block[] flowers = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET};
			for (int x = -9; x <= 9; x++) {
				for (int z = -9; z <= 9; z++) {
					// Every other block or so, on the ground itself
					if (Math.floorMod(x * 7 + z * 13, 3) != 0) {
						BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, plain.offset(x, 0, z));
						level.setBlockAndUpdate(at, flowers[Math.floorMod(x * 3 + z * 5, flowers.length)].defaultBlockState());
					}
				}
			}
		});
		if (wanted.test("ambience-butterfly")) {
			butterfly(context, world, site, plain);
		}
		if (wanted.test("ambience-night")) {
			// Before midnight, the moon still low in the east and the Milky Way above and to its north, the horizon below both
			teleport(context, world, plain, -112, -30);
			world.getServer().runCommand("time set 15000");
			context.waitTicks(40);
			shot(context, site, "ambience-night");
		}
	}

	private static void butterfly(ClientGameTestContext context, TestSingleplayerContext world, Path site, BlockPos plain) {
		teleport(context, world, plain, 0, 0);
		world.getServer().runCommand("time set 6000");
		context.waitFor(client -> Butterflies.count() > 0, 20 * 60);
		context.waitTicks(60);
		// From just above one over the flowers planted, its wings open to the view, and not one off down a slope
		context.waitFor(client -> Butterflies.count() > 0 && Math.abs(Butterflies.first().y - plain.getY()) < 2.5, 20 * 60);
		world.getServer().runCommand("gamemode spectator @a");
		Vec3 butterfly = context.computeOnClient(client -> Butterflies.first());
		world.getServer().runCommand("tp @a " + butterfly.x + " " + (butterfly.y + 0.9 - 1.62) + " " + (butterfly.z - 0.7) + " 0 52");
		context.waitTicks(4);
		// It flits about: aimed at where it is by now, on the client, with no packet to wait for
		context.runOnClient(client -> {
			Vec3 to = Butterflies.first().subtract(client.player.getEyePosition());
			client.player.setYRot((float) Math.toDegrees(Math.atan2(-to.x, to.z)));
			client.player.setXRot(-(float) Math.toDegrees(Math.atan2(to.y, Math.hypot(to.x, to.z))));
		});
		shot(context, site, "ambience-butterfly");
		world.getServer().runCommand("gamemode survival @a");
	}

	/** The best ground far from a place, in the biomes given: searched around several points of a ring around it. */
	private static BlockPos farFrom(TestSingleplayerContext world, BlockPos from, Predicate<Holder<Biome>> biomes, ToIntBiFunction<ServerLevel, BlockPos> score) {
		return world.getServer().computeOnServer(server -> {
			ServerLevel level = server.overworld();
			BlockPos best = null;
			int bestScore = Integer.MIN_VALUE;
			for (int[] way : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}}) {
				var found = level.findClosestBiome3d(biomes, from.offset(way[0] * 500, 0, way[1] * 500), 800, 32, 64);
				if (found == null) {
					continue;
				}
				for (int x = -32; x <= 32; x += 16) {
					for (int z = -32; z <= 32; z += 16) {
						// Loaded first: an unloaded column reads as the bottom of the world
						level.getChunk(found.getFirst().offset(x, 0, z));
						BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, found.getFirst().offset(x, 0, z));
						if (ground.distSqr(from) > 200 * 200 && ground.getY() > level.getSeaLevel() && biomes.test(level.getBiome(ground))
								&& level.getFluidState(ground.below()).isEmpty() && score.applyAsInt(level, ground) > bestScore) {
							best = ground;
							bestScore = score.applyAsInt(level, ground);
						}
					}
				}
			}
			if (best == null) {
				throw new AssertionError("no such place near the village");
			}
			return best;
		});
	}

	/** How much the land around rises and falls, in blocks. */
	private static int bumps(ServerLevel level, BlockPos ground) {
		int bumps = 0;
		for (int x = -24; x <= 24; x += 8) {
			for (int z = -24; z <= 24; z += 8) {
				bumps += Math.abs(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ground.offset(x, 0, z)).getY() - ground.getY());
			}
		}
		return bumps;
	}

	private static int treesAround(ServerLevel level, BlockPos ground) {
		int trees = 0;
		for (int x = -28; x <= 28; x += 4) {
			for (int z = -28; z <= 28; z += 4) {
				BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, ground.offset(x, 0, z)).below();
				if (level.getBlockState(top).is(BlockTags.LEAVES) || level.getBlockState(top).is(BlockTags.LOGS)) {
					trees++;
				}
			}
		}
		return trees;
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

	/**
	 * The shots' world, generated and its map explored around the village once, then kept between runs (in
	 * .test-worlds/site-world): generating the terrain is most of the time the shots take. Kept as it was before the shots,
	 * which change it. Made again when the seed, the game's version, the route or the map's drawing change.
	 */
	private static TestSingleplayerContext siteWorld(ClientGameTestContext context, Path cache) {
		String key = cacheKey();
		Path keyFile = cache.resolve(CACHE_KEY_FILE);
		try {
			if (Files.isRegularFile(keyFile) && Files.readString(keyFile).equals(key)) {
				// A flat world, quick to make, for its folder: the one kept takes its place
				TestWorldSave save;
				try (TestSingleplayerContext placeholder = context.worldBuilder().adjustSettings(state -> state.setName(WORLD_NAME)).create()) {
					save = placeholder.getWorldSave();
				}
				replace(cache, save.getSaveDirectory());
				return save.open();
			}
			TestWorldSave save;
			try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false)
					.adjustSettings(SiteScreenshotsClientGameTest::settings).create()) {
				save = world.getWorldSave();
				settle(context);
				context.waitForScreen(QuestOptInScreen.class);
				context.runOnClient(client -> QuestLog.setEnabled(true));
				context.setScreen(() -> null);
				rules(world);
				BlockPos village = village(world);
				for (int[] step : ROUTE) {
					explore(context, world, village.offset(step[0], 0, step[1]));
				}
			}
			replace(save.getSaveDirectory(), cache);
			Files.writeString(keyFile, key);
			return save.open();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	/** The seed, the game's version, the route and the classes that record and draw the map. */
	private static String cacheKey() {
		StringBuilder key = new StringBuilder(SEED).append('|').append(SharedConstants.getCurrentVersion().name()).append('|')
				.append(Arrays.deepToString(ROUTE));
		for (Class<?> type : List.of(WorldMap.class, MapRegion.class, TerrainColors.class)) {
			try (InputStream bytes = type.getResourceAsStream(type.getSimpleName() + ".class")) {
				key.append('|').append(Arrays.hashCode(bytes.readAllBytes()));
			} catch (IOException e) {
				throw new AssertionError(e);
			}
		}
		return key.toString();
	}

	/** Replaces a world's folder with another's, but for the lock of the world open. */
	static void replace(Path from, Path to) throws IOException {
		if (Files.isDirectory(to)) {
			try (Stream<Path> files = Files.walk(to)) {
				for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
					if (!file.equals(to) && !file.getFileName().toString().equals(CACHE_KEY_FILE)) {
						Files.delete(file);
					}
				}
			}
		}
		try (Stream<Path> files = Files.walk(from)) {
			for (Path file : files.toList()) {
				String name = file.getFileName().toString();
				if (name.equals("session.lock") || name.equals(CACHE_KEY_FILE)) {
					continue;
				}
				Path target = to.resolve(from.relativize(file).toString());
				if (Files.isDirectory(file)) {
					Files.createDirectories(target);
				} else {
					Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	private static void rules(TestSingleplayerContext world) {
		for (String rule : List.of("send_command_feedback false", "show_advancement_messages false", "advance_time false",
				"advance_weather false", "spawn_mobs false", "spawn_monsters false", "spawn_patrols false", "spawn_wandering_traders false")) {
			world.getServer().runCommand("gamerule " + rule);
		}
		world.getServer().runCommand("time set 1000");
		world.getServer().runCommand("weather clear");
		world.getServer().runCommand("gamemode survival @a");
	}

	private static BlockPos village(TestSingleplayerContext world) {
		BlockPos village = world.getServer().computeOnServer(server -> server.overworld()
				.findNearestMapStructure(StructureTags.ON_PLAINS_VILLAGE_MAPS, BlockPos.ZERO, 100, false));
		if (village == null) {
			throw new AssertionError("Expected: a plains village near the spawn of seed " + SEED);
		}
		return village;
	}

	/**
	 * Standing at a column long enough for the world map to record what is around: the chunks it reads (6 around the
	 * player) loaded with their blocks, then two of its rounds over them (8 chunks a tick), without waiting for them to
	 * be drawn.
	 */
	private static void explore(ClientGameTestContext context, TestSingleplayerContext world, BlockPos column) {
		BlockPos ground = world.getServer().computeOnServer(server -> surface(server, column));
		world.getServer().runCommand("tp @a " + ground.getX() + ".5 " + ground.getY() + " " + ground.getZ() + ".5 0 0");
		int centerX = ground.getX() >> 4;
		int centerZ = ground.getZ() >> 4;
		context.waitFor(client -> {
			for (int dz = -6; dz <= 6; dz++) {
				for (int dx = -6; dx <= 6; dx++) {
					// Loaded with its blocks: the client holds chunks before their blocks arrive, which the map skips
					int x = (centerX + dx) * 16 + 8;
					int z = (centerZ + dz) * 16 + 8;
					if (dx * dx + dz * dz <= 36 && (!client.level.hasChunk(centerX + dx, centerZ + dz)
							|| client.level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) <= client.level.getMinY())) {
						return false;
					}
				}
			}
			return client.player.blockPosition().getX() >> 4 == centerX && client.player.blockPosition().getZ() >> 4 == centerZ;
		}, 20 * 30);
		context.waitTicks(32);
	}

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

	/** The loading screen around the player, its loading held halfway: the real one is over too fast to catch. */
	private static void loading(ClientGameTestContext context, Path site) {
		ChunkPos center = context.computeOnClient(client -> client.player.chunkPosition());
		LevelLoadTracker tracker = new LevelLoadTracker();
		tracker.setServerChunkStatusView(new ChunkLoadStatusView() {
			@Override
			public void moveTo(ResourceKey<Level> dimension, ChunkPos chunk) {
			}

			@Override
			public ChunkStatus get(int x, int z) {
				int distance = Math.max(Math.abs(x - radius()), Math.abs(z - radius()));
				return distance <= 4 ? ChunkStatus.FULL : distance <= 6 ? ChunkStatus.STRUCTURE_STARTS : null;
			}

			@Override
			public int radius() {
				return 8;
			}
		});
		tracker.updateFocus(Level.OVERWORLD, center);
		for (LevelLoadListener.Stage stage : LevelLoadListener.Stage.values()) {
			tracker.start(stage, 100);
			tracker.update(stage, 60, 100);
		}
		context.setScreen(() -> new LevelLoadingScreen(tracker, LevelLoadingScreen.Reason.OTHER));
		context.waitForScreen(LevelLoadingScreen.class);
		context.waitTicks(40);
		shot(context, site, "interface-loading");
		context.setScreen(() -> null);
		context.waitTicks(2);
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
		try (TestSingleplayerContext _ = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(state -> {
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
