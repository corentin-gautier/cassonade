package com.cassonade.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
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
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.util.Util;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;

import com.mojang.blaze3d.platform.InputConstants;

import com.cassonade.client.CassonadeClient;
import com.cassonade.client.ClientSettings;
import com.cassonade.client.InfoPanel;
import com.cassonade.client.WorldSelectionScreen;
import com.cassonade.client.ambience.Ambience;
import com.cassonade.client.ambience.Aurora;
import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Butterflies;
import com.cassonade.client.ambience.MorningMist;
import com.cassonade.client.map.WorldMapScreen;
import com.cassonade.client.mixin.AbstractContainerScreenAccessor;
import com.cassonade.client.mixin.ImageButtonAccessor;
import com.cassonade.client.quest.AchievementBanner;
import com.cassonade.client.quest.AdvancementsListScreen;
import com.cassonade.client.quest.QuestJournalScreen;
import com.cassonade.client.quest.QuestTracker;
import com.cassonade.client.waystone.WaystoneScreen;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.hopper.HopperFilterMenu;
import com.cassonade.item.CassonadeItems;
import com.cassonade.mixin.AbstractFurnaceBlockEntityAccessor;
import com.cassonade.waystone.WaystoneListPayload;

/**
 * The website's screenshots (promo/site/assets/screenshots), in a world prepared by hand (see siteWorld): run
 * on demand only, with {@code ./gradlew runClientGameTest -PsiteScreenshots}. Each is copied to the
 * site as {@code <id>.png}.
 */
public class SiteScreenshotsClientGameTest implements FabricClientGameTest {
	private static final String WORLD_NAME = "Plains Village";
	private static final long HOUR = 3_600_000;
	/** Where the player was left in the world, among what was prepared for the shots. */
	private static final BlockPos SCENE = new BlockPos(4965, 112, 5453);
	private static final BlockPos WAYSTONE = new BlockPos(4965, 112, 5449);
	private static final BlockPos HIVE = new BlockPos(4973, 112, 5447);
	/** The waxed copper, the chiseled bookshelf on top of it. */
	private static final BlockPos COPPER = new BlockPos(4973, 112, 5449);
	private static final BlockPos CHEST = new BlockPos(4973, 112, 5442);
	private static final BlockPos WIRE = new BlockPos(4966, 112, 5443);
	/** Flat open plains east of the scene, far from any torch, for the night. */
	private static final BlockPos PLAIN = new BlockPos(5000, 112, 5450);
	/** The densest and flattest forest explored, for the morning mist among its trees. */
	private static final BlockPos FOREST = new BlockPos(4120, 71, 5480);
	/** Open ground east of the scene, for what is summoned or placed for a shot. */
	private static final BlockPos ARENA = new BlockPos(4985, 112, 5452);
	/** Where a window's block is placed, looked at from the arena, the village behind it. */
	private static final BlockPos BENCH = ARENA.west(3);
	/** The hotbar as prepared: the sculk torch, and the slot in hand, empty. */
	private static final int TORCH_SLOT = 0;
	private static final int EMPTY_SLOT = 1;
	private static final int FOOD_SLOT = 6;
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
		// Just these, to retouch a shot without waiting for all the others
		String only = System.getProperty("cassonade.siteShots");
		Predicate<String> wanted = only == null ? id -> true : List.of(only.split(","))::contains;
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
			rules(world);
			// Where they stand, or they would walk out of a shot or into the next
			for (String mob : List.of("villager", "cat", "iron_golem")) {
				world.getServer().runCommand("execute as @e[type=" + mob + "] run data merge entity @s {NoAI:1b}");
			}
			fillInventory(world);
			world.getServer().runCommand("advancement grant @a until minecraft:story/iron_tools");
			world.getServer().runCommand("advancement grant @a until minecraft:adventure/sleep_in_bed");
			world.getServer().runCommand("recipe give @a *");

			if (wanted.test("worldmap")) {
				worldMap(context, world, site);
				context.setScreen(() -> null);
			}

			// The prepared scene, as the player was left in it: minimap, clock, the current quest
			if (wanted.test("hero")) {
				scene(context, world);
				shot(context, site, "hero");
			}
			if (wanted.test("minimap")) {
				teleport(context, world, new BlockPos(4992, 112, 5456), 100, 6);
				shot(context, site, "minimap");
			}
			// Sunset, the weather running so the barometer gets its forecast
			if (wanted.test("clock")) {
				world.getServer().runCommand("gamerule advance_weather true");
				world.getServer().runCommand("weather clear 2400");
				world.getServer().runCommand("time set 12600");
				teleport(context, world, new BlockPos(4992, 112, 5446), 90, -4);
				context.waitTicks(40);
				shot(context, site, "clock");
				world.getServer().runCommand("gamerule advance_weather false");
				world.getServer().runCommand("time set 1000");
			}

			// Worn on the back over the chestplate, seen from behind: on a lookout above the scene, so
			// the camera clears the ground
			if (wanted.test("elytra")) {
				BlockPos lookout = surface(world, new BlockPos(4960, 112, 5460)).above(8);
				world.getServer().runCommand(fill(lookout.offset(-1, -1, -1), lookout.offset(1, -1, 1), "spruce_planks"));
				context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
				world.getServer().runCommand("tp @a " + lookout.getX() + ".5 " + lookout.getY() + " " + lookout.getZ() + ".5 -147 18");
				settle(context);
				shot(context, site, "elytra");
				world.getServer().runCommand(fill(lookout.offset(-1, -1, -1), lookout.offset(1, -1, 1), "air"));
				context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			}

			// A field of ripe wheat, one row harvested and planted again
			if (wanted.test("harvest")) {
				BlockPos field = new BlockPos(5000, 112, 5445);
				world.getServer().runCommand(fill(field.offset(-4, -1, 2), field.offset(4, -1, 8), "farmland[moisture=7]"));
				world.getServer().runCommand(fill(field.offset(0, -1, 2), field.offset(0, -1, 8), "water"));
				world.getServer().runCommand(fill(field.offset(-4, 0, 2), field.offset(4, 3, 8), "air"));
				world.getServer().runCommand(fill(field.offset(-4, 0, 2), field.offset(-1, 0, 8), "wheat[age=7]"));
				world.getServer().runCommand(fill(field.offset(1, 0, 2), field.offset(4, 0, 8), "wheat[age=7]"));
				world.getServer().runCommand(fill(field.offset(-1, 0, 2), field.offset(-1, 0, 4), "wheat[age=0]"));
				world.getServer().runCommand("item replace entity @a weapon.mainhand with diamond_hoe");
				teleport(context, world, field, 0, 38);
				context.runOnClient(client -> {
					BlockPos wheat = field.offset(-1, 0, 5);
					client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(wheat), Direction.UP, wheat, false));
				});
				context.waitTicks(4);
				shot(context, site, "harvest");
				world.getServer().runCommand("item replace entity @a weapon.mainhand with air");
				// Back to a meadow: it stands between the next shots and what they look at
				world.getServer().runCommand(fill(field.offset(-4, 0, 2), field.offset(4, 0, 8), "air"));
				world.getServer().runCommand(fill(field.offset(-4, -1, 2), field.offset(4, -1, 8), "grass_block"));
				world.getServer().runCommand("kill @e[type=item]");
			}

			// A fight: a creeper and a husk after the player, a spider hurt
			if (wanted.test("combat")) {
				// At noon: the morning sun would stand in the east, right over them
				world.getServer().runCommand("time set 6000");
				teleport(context, world, ARENA, -90, 4);
				world.getServer().runCommand("summon creeper " + pos(ARENA.offset(7, 0, -2)) + " {NoAI:1b,Rotation:[90f,0f]}");
				world.getServer().runCommand("summon husk " + pos(ARENA.offset(9, 0, 3)) + " {NoAI:1b,Rotation:[100f,0f]}");
				world.getServer().runCommand("summon spider " + pos(ARENA.offset(5, 0, 1)) + " {NoAI:1b,Rotation:[100f,0f]}");
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
				for (String mob : List.of("creeper", "husk", "spider")) {
					world.getServer().runCommand("kill @e[type=" + mob + "]");
				}
				// What they dropped as they died
				world.getServer().runCommand("kill @e[type=item]");
				world.getServer().runCommand("time set 1000");
			}

			// A horse in the meadow, seen through the naturalist's spyglass, once the fight's mobs are gone: far enough
			// that the zoom shows it whole, the look aimed at its middle
			if (wanted.test("spyglass")) {
				BlockPos from = ARENA.west(6);
				BlockPos grazing = surface(world, ARENA.east(24));
				world.getServer().runCommand("summon horse " + pos(grazing) + " {NoAI:1b,Rotation:[0f,0f],Variant:3,"
						+ "attributes:[{id:\"minecraft:movement_speed\",base:0.29},{id:\"minecraft:jump_strength\",base:0.82},{id:\"minecraft:max_health\",base:26}]}");
				// The flowers and grass on the way would catch the look
				world.getServer().runCommand(fill(from.east(), grazing.west(), "air"));
				world.getServer().runCommand("item replace entity @a weapon.mainhand with cassonade:naturalist_spyglass");
				// Raised at the sky first: used on the horse, it would ride it
				teleport(context, world, from, -90, -60);
				context.waitTicks(3);
				context.getInput().holdKey(options -> options.keyUse);
				context.waitTicks(3);
				float down = (float) Math.toDegrees(Math.atan2(from.getY() + 1.62 - (grazing.getY() + 1.0), 30));
				world.getServer().runCommand("tp @a " + from.getX() + ".5 " + from.getY() + " " + from.getZ() + ".5 -90 " + down);
				context.waitTicks(40);
				shot(context, site, "spyglass");
				context.getInput().releaseKey(options -> options.keyUse);
				world.getServer().runCommand("item replace entity @a weapon.mainhand with air");
				world.getServer().runCommand("kill @e[type=horse]");
			}

			// The sculk torch at night on the plains, out of reach of the village's torches: the faces on the grass where monsters could spawn, none by the torch
			if (wanted.test("torch")) {
				world.getServer().runCommand("time set 18000");
				select(context, world, TORCH_SLOT);
				// One torch off to the side, its light taking the faces off part of the ground only
				BlockPos lit = PLAIN.offset(10, 0, -8);
				world.getServer().runCommand("setblock " + pos(lit) + " torch");
				teleport(context, world, PLAIN, -90, 35);
				context.waitTicks(30);
				shot(context, site, "torch");
				world.getServer().runCommand("setblock " + pos(lit) + " air");
				select(context, world, EMPTY_SLOT);
				world.getServer().runCommand("time set 1000");
			}

			// The windows next, without the current quest over them
			context.runOnClient(client -> ClientSettings.setQuestTrackerDisplay(ClientSettings.QuestTrackerDisplay.HIDDEN));

			// The backpack open from its key, full of a trip's finds
			if (wanted.test("backpack")) {
				scene(context, world);
				context.getInput().pressKey(CassonadeClient.OPEN_BACKPACK);
				context.waitForScreen(InventoryScreen.class);
				context.waitTicks(2);
				shot(context, site, "backpack");
				context.setScreen(() -> null);
			}

			// A chest, a shulker box's contents shown in its tooltip
			if (wanted.test("inventory")) {
				teleport(context, world, ARENA, 90, 0);
				BlockPos chest = BENCH;
				world.getServer().runCommand("setblock " + pos(chest) + " chest{Items:[{Slot:0b,id:\"minecraft:cobblestone\",count:64},"
						+ "{Slot:1b,id:\"minecraft:cobblestone\",count:64},{Slot:2b,id:\"minecraft:andesite\",count:41},{Slot:3b,id:\"minecraft:coal\",count:23},"
						+ "{Slot:4b,id:\"minecraft:iron_ingot\",count:12},{Slot:9b,id:\"minecraft:oak_log\",count:64},{Slot:10b,id:\"minecraft:oak_log\",count:30},"
						+ "{Slot:11b,id:\"minecraft:birch_log\",count:18},{Slot:12b,id:\"minecraft:wheat_seeds\",count:20},{Slot:18b,id:\"minecraft:bread\",count:9},"
						+ "{Slot:19b,id:\"minecraft:carrot\",count:14},{Slot:20b,id:\"minecraft:redstone\",count:27}]}");
				world.getServer().runCommand("item replace entity @a inventory.4 with shulker_box[container=[{slot:0,item:{id:\"minecraft:diamond\",count:7}},"
						+ "{slot:1,item:{id:\"minecraft:emerald\",count:12}},{slot:2,item:{id:\"minecraft:golden_apple\",count:2}},"
						+ "{slot:3,item:{id:\"minecraft:ender_pearl\",count:5}},{slot:5,item:{id:\"minecraft:lapis_lazuli\",count:30}},"
						+ "{slot:9,item:{id:\"minecraft:experience_bottle\",count:16}}]]");
				read(context, world, ARENA, chest, 0.5);
				world.getServer().runOnServer(server -> player(server).openMenu((MenuProvider) server.overworld().getBlockEntity(chest)));
				context.waitForScreen(ContainerScreen.class);
				hover(context, 27 + 4);
				shot(context, site, "inventory");
				context.runOnClient(client -> client.player.closeContainer());
				world.getServer().runCommand("setblock " + pos(chest) + " air");
				world.getServer().runCommand("item replace entity @a inventory.4 with air");
			}

			// The hopper's filter, stone only, under a chest of stone and dirt
			if (wanted.test("hopper")) {
				teleport(context, world, ARENA, 90, 0);
				BlockPos hopper = BENCH;
				world.getServer().runCommand("setblock " + pos(hopper) + " hopper");
				world.getServer().runCommand("setblock " + pos(hopper.above()) + " chest{Items:[{Slot:0b,id:\"minecraft:dirt\",count:3},"
						+ "{Slot:1b,id:\"minecraft:stone\",count:3},{Slot:2b,id:\"minecraft:dirt\",count:2}]}");
				read(context, world, ARENA, hopper, 0.5);
				world.getServer().runOnServer(server -> {
					ServerPlayer player = player(server);
					player.openMenu((MenuProvider) server.overworld().getBlockEntity(hopper));
					player.containerMenu.getSlot(HopperFilterMenu.FILTER_SLOT).set(new ItemStack(Items.STONE));
				});
				context.waitTicks(40);
				shot(context, site, "hopper");
				context.runOnClient(client -> client.player.closeContainer());
				world.getServer().runCommand(fill(hopper, hopper.above(), "air"));
			}

			// The crafting table's recipe book, a recipe searched
			if (wanted.test("interface")) {
				teleport(context, world, ARENA, 90, 0);
				BlockPos table = BENCH;
				world.getServer().runCommand("setblock " + pos(table) + " crafting_table");
				read(context, world, ARENA, table, 0.5);
				world.getServer().runOnServer(server -> player(server).openMenu(server.overworld().getBlockState(table).getMenuProvider(server.overworld(), table)));
				context.waitForScreen(CraftingScreen.class);
				context.waitTicks(2);
				context.runOnClient(client -> recipeButton(client).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0)));
				// The recipes seen for the first time bounce in the book a moment: still before the shot
				context.waitTicks(40);
				shot(context, site, "interface");
				context.runOnClient(client -> client.player.closeContainer());
				world.getServer().runCommand("setblock " + pos(table) + " air");
			}

			// The quest journal, some way into the story
			if (wanted.test("quests")) {
				context.setScreen(QuestJournalScreen::new);
				context.waitForScreen(QuestJournalScreen.class);
				shot(context, site, "quests");
				context.setScreen(() -> null);
			}

			// The advancements screen, with what is left for each
			if (wanted.test("advancements")) {
				context.setScreen(() -> new AdvancementsListScreen(null));
				context.waitForScreen(AdvancementsListScreen.class);
				shot(context, site, "advancements");
				context.setScreen(() -> null);
			}

			if (wanted.test("ambience-butterfly")) {
				butterflies(context, world, site);
			}
			details(context, world, site, wanted);
			ambience(context, world, site, SCENE, wanted);
			context.runOnClient(client -> ClientSettings.setQuestTrackerDisplay(ClientSettings.QuestTrackerDisplay.FULL));
		}
		if (wanted.test("interface-worlds")) {
			worlds(context, site);
		}
		graphics(context, site, wanted);
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
	private static void details(ClientGameTestContext context, TestSingleplayerContext world, Path site, Predicate<String> wanted) {
		// The woodcutter, a stack of planks in, its stairs chosen
		if (wanted.test("woodcutter")) {
			teleport(context, world, ARENA, 90, 0);
			BlockPos bench = BENCH;
			world.getServer().runCommand("setblock " + pos(bench) + " cassonade:woodcutter");
			read(context, world, ARENA, bench, 0.5);
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
			// Taken out first, or closing would hand them back to the inventory
			world.getServer().runOnServer(server -> player(server).containerMenu.getSlot(0).set(ItemStack.EMPTY));
			context.runOnClient(client -> client.player.closeContainer());
			world.getServer().runCommand("setblock " + pos(bench) + " air");
		}

		// A furnace at work, the cursor on the "!" by its title
		if (wanted.test("interface-info")) {
			teleport(context, world, ARENA, 90, 0);
			BlockPos at = BENCH;
			world.getServer().runCommand("setblock " + pos(at) + " furnace");
			read(context, world, ARENA, at, 0.5);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) player.level().getBlockEntity(at);
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
			world.getServer().runCommand("setblock " + pos(at) + " air");
		}

		if (wanted.test("interface-loading")) {
			teleport(context, world, SCENE, 0, 0);
			loading(context, site);
		}
		if (wanted.test("worldmap-biome")) {
			worldMapBiome(context, world, site);
		}

		// Steak in hand on a low hunger bar: the many drumsticks it would restore blink
		if (wanted.test("inventory-food")) {
			discard(world, List.of(EntityTypes.VILLAGER, EntityTypes.BEE));
			scene(context, world);
			select(context, world, FOOD_SLOT);
			world.getServer().runOnServer(server -> player(server).getFoodData().setFoodLevel(6));
			context.waitTicks(8);
			// A third into their 1200 ms blink once the shot is taken: half faded, neither full nor empty
			context.waitFor(client -> Util.getMillis() % 1200 >= 200 && Util.getMillis() % 1200 < 280, 20 * 5);
			shot(context, site, "inventory-food");
			world.getServer().runOnServer(server -> player(server).getFoodData().setFoodLevel(20));
			select(context, world, EMPTY_SLOT);
		}

		// What the crosshair reads and what the spyglass aims at: nobody and no bee in the way, taken away
		// without the smoke of dying; nor the grass and the flower right before the blocks read
		discard(world, List.of(EntityTypes.VILLAGER, EntityTypes.CAT, EntityTypes.BEE));
		world.getServer().runCommand(fill(WIRE.offset(-4, 0, -2), WIRE.offset(-1, 0, 2), "air replace short_grass"));
		world.getServer().runCommand("setblock " + pos(COPPER.west(2)) + " air");
		world.getServer().runCommand(fill(HIVE.west(4), HIVE.west(), "air"));

		// Bees inside a hive, through the naturalist's spyglass; some of the world's may have gone home already
		if (wanted.test("spyglass-bees")) {
			BlockPos from = new BlockPos(4949, 112, 5447);
			world.getServer().runOnServer(server -> {
				BeehiveBlockEntity entity = (BeehiveBlockEntity) server.overworld().getBlockEntity(HIVE);
				while (entity.getOccupantCount() < 2) {
					entity.storeBee(BeehiveBlockEntity.Occupant.create(100));
				}
			});
			world.getServer().runCommand("item replace entity @a weapon.mainhand with cassonade:naturalist_spyglass");
			teleport(context, world, from, -90, -60);
			context.waitTicks(3);
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(3);
			float toHive = (float) Math.toDegrees(Math.atan2(1.62 - 0.5, HIVE.getX() - from.getX() - 0.5));
			world.getServer().runCommand("tp @a " + from.getX() + ".5 " + from.getY() + " " + from.getZ() + ".5 -90 " + toHive);
			context.waitTicks(40);
			shot(context, site, "spyglass-bees");
			context.getInput().releaseKey(options -> options.keyUse);
			world.getServer().runCommand("item replace entity @a weapon.mainhand with air");
		}

		// A wire's power, redstone dust in hand, by the torch that feeds it
		if (wanted.test("tweaks")) {
			world.getServer().runCommand("item replace entity @a weapon.mainhand with redstone");
			read(context, world, WIRE.west(3), WIRE, 0.1);
			shot(context, site, "tweaks");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with air");
		}

		// Waxed copper, a honeycomb in hand
		if (wanted.test("tweaks-copper")) {
			world.getServer().runCommand("item replace entity @a weapon.mainhand with honeycomb");
			read(context, world, COPPER.west(3), COPPER, 0.5);
			shot(context, site, "tweaks-copper");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with air");
		}

		// A waystone among the flowers, then its window with a row for each dimension
		if (wanted.test("waystone")) {
			// Aimed slantwise, the morning sun out from behind it; the command aims from the feet, so lower than what is looked at
			teleport(context, world, WAYSTONE.offset(-3, 0, 4), 0, 0);
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ facing " + WAYSTONE.getX() + ".5 " + (WAYSTONE.getY() - 1.2) + " " + WAYSTONE.getZ() + ".5");
			context.waitTicks(30);
			shot(context, site, "waystone");
		}
		if (wanted.test("waystone-list")) {
			context.runOnClient(client -> WaystoneScreen.open(client, new WaystoneListPayload(WAYSTONE, "Main Village", true, List.of(
					new WaystoneListPayload.Destination(GlobalPos.of(Level.OVERWORLD, new BlockPos(5103, 96, 4978)), "Snowy Village"),
					new WaystoneListPayload.Destination(GlobalPos.of(Level.NETHER, new BlockPos(610, 67, 715)), "Crimson Biome"),
					new WaystoneListPayload.Destination(GlobalPos.of(Level.END, new BlockPos(1040, 62, 8)), "End City")))));
			context.waitForScreen(WaystoneScreen.class);
			// Off the rows: the one under the cursor would show its coordinates over the next
			context.getInput().setCursorPos(100, 1000);
			context.waitTicks(3);
			shot(context, site, "waystone-list");
			context.setScreen(() -> null);
		}

		// The chest renamed at the anvil, an empty hand
		if (wanted.test("tweaks-chest")) {
			read(context, world, CHEST.west(3), CHEST, 0.5);
			shot(context, site, "tweaks-chest");
		}

		// The chiseled bookshelf, the crosshair on the middle book of its top row
		if (wanted.test("tweaks-bookshelf")) {
			read(context, world, COPPER.west(3), COPPER, 1.75);
			shot(context, site, "tweaks-bookshelf");
		}

		// An armor stand with arms, as Bedrock places it, dressed for the shot
		if (wanted.test("tweaks-armor-stand")) {
			String[][] worn = {{"armor.head", "iron_helmet"}, {"armor.chest", "iron_chestplate"}, {"armor.legs", "iron_leggings"}, {"armor.feet", "iron_boots"}, {"weapon.mainhand", "iron_sword"}};
			for (String[] piece : worn) {
				world.getServer().runCommand("item replace entity @e[type=armor_stand] " + piece[0] + " with " + piece[1]);
			}
			world.getServer().runCommand("data merge entity @e[type=armor_stand,limit=1] {Pose:{RightArm:[-15f,0f,10f],LeftArm:[10f,0f,-10f]}}");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with shears");
			world.getServer().runCommand("tp @a 4967.5 112 5450.5 180 4");
			context.waitTicks(10);
			shot(context, site, "tweaks-armor-stand");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with air");
		}
	}

	/**
	 * A flock passing, its wait and the time set by hand, the time standing still in this world.
	 */
	private static void ambience(ClientGameTestContext context, TestSingleplayerContext world, Path site, BlockPos home, Predicate<String> wanted) {
		context.runOnClient(client -> Birds.setSpawning(false));
		if (wanted.test("ambience")) {
			birds(context, world, site, home);
		}
		// The Milky Way, before midnight: the moon still low in the east and the Milky Way above and to its north
		if (wanted.test("ambience-night")) {
			teleport(context, world, PLAIN, -112, -30);
			world.getServer().runCommand("time set 15000");
			context.waitTicks(40);
			shot(context, site, "ambience-night");
		}
		if (wanted.test("ambience-mist")) {
			teleport(context, world, FOREST, 0, -3);
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

	/** Taken away without the smoke of dying. */
	private static void discard(TestSingleplayerContext world, List<EntityType<?>> types) {
		world.getServer().runOnServer(server -> {
			List<Entity> around = new ArrayList<>();
			server.overworld().getAllEntities().forEach(around::add);
			around.stream().filter(entity -> types.contains(entity.getType())).forEach(Entity::discard);
		});
	}

	/** Butterflies released over the nearest flowers of the scene, seen from where the player was left. */
	private static void butterflies(ClientGameTestContext context, TestSingleplayerContext world, Path site) {
		discard(world, List.of(EntityTypes.VILLAGER, EntityTypes.BEE));
		world.getServer().runCommand("time set 6000");
		world.getServer().runCommand("gamemode spectator @a");
		scene(context, world);
		context.runOnClient(client -> Butterflies.release(client.level, List.of(new Vec3(4966.8, 112.7, 5452), new Vec3(4967.7, 112.8, 5452.1), new Vec3(4967, 112.9, 5451.1),
				new Vec3(4968.6, 112.7, 5451.9), new Vec3(4968.1, 113, 5451), new Vec3(4969.5, 112.8, 5452.1))));
		context.waitTicks(10);
		shot(context, site, "ambience-butterfly");
		world.getServer().runCommand("gamemode survival @a");
		world.getServer().runCommand("time set 1000");
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

	/**
	 * The player's gear is as prepared, the backpack and the pockets filled as after a trip. The slot in hand stays
	 * empty for what each shot holds, and inventory.4 for the Shulker box of the chest's shot.
	 */
	private static void fillInventory(TestSingleplayerContext world) {
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
			player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.BACKPACK, backpack));
			Object[][] pockets = {{2, Items.NETHERITE_SWORD, 1}, {3, Items.NETHERITE_PICKAXE, 1}, {4, Items.NETHERITE_AXE, 1}, {5, Items.DIAMOND_SHOVEL, 1},
					{6, Items.COOKED_BEEF, 32}, {7, Items.FIREWORK_ROCKET, 48}, {8, Items.WATER_BUCKET, 1},
					{9, Items.COBBLESTONE, 64}, {10, Items.COBBLESTONE, 64}, {11, Items.DEEPSLATE, 48}, {12, Items.OAK_PLANKS, 52},
					{14, Items.IRON_INGOT, 23}, {15, Items.GOLD_INGOT, 9}, {16, Items.DIAMOND, 6}, {18, Items.TORCH, 48},
					{19, Items.ENDER_PEARL, 7}, {21, Items.BOW, 1}, {22, Items.ARROW, 44}, {24, Items.REDSTONE, 31},
					{27, Items.ROTTEN_FLESH, 12}, {28, Items.BONE, 8}, {29, Items.STRING, 13}, {31, Items.WHEAT_SEEDS, 17}, {33, Items.OAK_SAPLING, 5}};
			for (Object[] pocket : pockets) {
				player.getInventory().setItem((Integer) pocket[0], new ItemStack((Item) pocket[1], (Integer) pocket[2]));
			}
		});
	}

	/**
	 * The shots' world, prepared by hand in the game (its scene, gear, waystones and explored map) and kept in
	 * .test-worlds/site-world: it cannot be made again, so a run works on a copy and leaves it as it is.
	 */
	private static TestSingleplayerContext siteWorld(ClientGameTestContext context, Path cache) {
		if (!Files.isRegularFile(cache.resolve("level.dat"))) {
			throw new AssertionError("Expected: the shots' world, prepared by hand, in " + cache);
		}
		try {
			// A flat world, quick to make, for its folder: the one kept takes its place
			TestWorldSave save;
			try (TestSingleplayerContext placeholder = context.worldBuilder().adjustSettings(state -> state.setName(WORLD_NAME)).create()) {
				save = placeholder.getWorldSave();
			}
			replace(cache, save.getSaveDirectory());
			return save.open();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
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

	/**
	 * The light, the sky and the water, in places the prepared world lacks: a world of a fixed seed made for them, where
	 * the same places come back each time. A pirate's booty in a lush cave, auroras over snow (the home page's backdrop,
	 * in its own size), the sun setting behind the sea, and the sun seen from under water. Without the HUD, for the
	 * world alone.
	 */
	private static void graphics(ClientGameTestContext context, Path site, Predicate<String> wanted) {
		if (Stream.of("villages-pirate-booty", "villages", "ambience-aurora", "graphics", "graphics-lichen", "sea-sunset", "sea-underwater", "sea", "sea-mask").noneMatch(wanted)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(state -> {
			state.setSeed("vanillasugar");
			state.setWorldType(state.getNormalPresetList().getFirst());
			state.setAllowCommands(true);
		}).create()) {
			settle(context);
			rules(world);
			world.getServer().runCommand("gamemode spectator @a");
			context.runOnClient(client -> client.gui.hud.toggle());
			if (wanted.test("villages-pirate-booty")) {
				List<BlockPos> hoards = world.getServer().computeOnServer(server -> PirateBootyClientGameTest.hoards(server.overworld()));
				PirateBootyClientGameTest.view(context, world, hoards.getFirst());
				settle(context);
				shot(context, site, "villages-pirate-booty");
				world.getServer().runCommand("effect clear @a");
			}
			if (wanted.test("villages")) {
				// The town on the flattest ground around, in the morning, from high behind its meeting point to the south,
				// looking north down its streets: far enough to take them all in, every chunk drawn first
				BlockPos heart = world.getServer().computeOnServer(server -> TownClientGameTest.showcaseTown(server.overworld()).getPieces().getFirst().getBoundingBox().getCenter());
				world.getServer().runCommand("time set 1500");
				world.getServer().runCommand("tp @a " + heart.getX() + " " + (heart.getY() + 60) + " " + (heart.getZ() + 55) + " facing " + heart.getX() + " "
						+ heart.getY() + " " + (heart.getZ() - 15));
				viewDistance(context, world, 20);
				// The server sends the farther chunks a few at a time: until every one in the view distance is there,
				// then drawn, or the land ends on a straight edge
				int radius = 20;
				try {
					context.waitFor(client -> client.level.getChunkSource().getLoadedChunksCount() >= (2 * radius + 1) * (2 * radius + 1) * 3 / 4, 20 * 180);
				} catch (AssertionError tooLong) {
					// As many as came
				}
				System.out.println("Town shot: " + context.computeOnClient(client -> client.level.getChunkSource().getLoadedChunksCount()) + " chunks loaded of about "
						+ (2 * radius + 1) * (2 * radius + 1) * 3 / 4);
				try {
					context.waitFor(client -> client.levelRenderer.hasRenderedAllSections(), 20 * 180);
				} catch (AssertionError tooLong) {
					// Drawn enough for a picture
				}
				context.waitTicks(100);
				shot(context, site, "villages");
				viewDistance(context, world, 10);
			}
			if (wanted.test("ambience-aurora")) {
				// The fifth night, one they show (every other) under a crescent moon, early: the moon still low in the
				// east. Facing north-east, between the auroras to the north and the moon, a little upwards, with the
				// Milky Way above
				BlockPos snow = surface(world, nearest(world, Biomes.SNOWY_PLAINS));
				world.getServer().runCommand("tp @a " + snow.getX() + ".5 " + (snow.getY() + 2) + " " + snow.getZ() + ".5 -135 -10");
				world.getServer().runCommand("time set " + (5 * Ambience.TICKS_PER_DAY + 14000));
				context.getInput().resizeWindow(2000, 1254);
				context.waitFor(client -> Aurora.strength() >= 1, 20 * 20);
				settle(context);
				shot(context, site, "ambience-aurora");
				context.getInput().resizeWindow(1920, 1080);
			}
			if (wanted.test("sea-sunset")) {
				// Out at sea facing west, deep water all the way to the horizon, the sun touching it
				BlockPos sea = world.getServer().computeOnServer(server -> openSea(server.overworld()));
				world.getServer().runCommand("time set 12850");
				world.getServer().runCommand("tp @a " + sea.getX() + ".5 " + (sea.getY() + 2) + " " + sea.getZ() + ".5 90 -3");
				// A little farther than the other shots, every chunk up to the horizon made and drawn first, or the sea stops short
				viewDistance(context, world, 12);
				context.waitTicks(40);
				try {
					context.waitFor(client -> client.levelRenderer.hasRenderedAllSections(), 20 * 180);
				} catch (AssertionError tooLong) {
					// Drawn enough for a picture
				}
				context.waitTicks(100);
				shot(context, site, "sea-sunset");
				viewDistance(context, world, 10);
			}
			if (wanted.test("sea-underwater")) {
				// In front of a shallow reef, looking east at it and up towards the morning sun through the surface
				BlockPos reef = world.getServer().computeOnServer(server -> reef(server.overworld()));
				world.getServer().runCommand("time set 3000");
				// Down on the floor and looking well up, the corals in front and the window of light in the surface above
				int floor = world.getServer().computeOnServer(server -> floor(server.overworld(), reef.west(12)));
				world.getServer().runCommand("tp @a " + (reef.getX() - 12) + ".5 " + floor + " " + reef.getZ() + ".5 -90 -25");
				settle(context);
				// The eyes used to the water, as after a few seconds in it
				context.waitTicks(100);
				shot(context, site, "sea-underwater");
			}
			if (wanted.test("graphics")) {
				// A mine gallery deep down, seen down its length: timber frames, a track, and lights as a player would
				// leave them, each tinting the rock around it, a lantern on the floor warm, a redstone torch red, a soul
				// lantern blue, a copper torch green, and amethyst growing at the far end
				world.getServer().runCommand("forceload add -16 -32 16 16");
				world.getServer().runCommand("tp @a 0 -40 0 180 5");
				settle(context);
				String[] mine = {
						"fill ~-4 ~-1 ~-27 ~4 ~5 ~3 deepslate",
						"fill ~-2 ~ ~-26 ~2 ~3 ~3 air",
						"fill ~ ~ ~-25 ~ ~ ~3 rail",
						"fill ~-3 ~ ~-27 ~3 ~4 ~-27 amethyst_block",
						"setblock ~1 ~ ~-26 amethyst_cluster",
						"setblock ~-2 ~ ~-26 large_amethyst_bud",
						"setblock ~-1 ~3 ~-26 amethyst_cluster[facing=down]",
						"setblock ~2 ~1 ~-26 medium_amethyst_bud[facing=west]",
						"setblock ~-1 ~ ~-1 lantern",
						"setblock ~2 ~2 ~-8 redstone_wall_torch[facing=west]",
						"setblock ~1 ~2 ~-15 soul_lantern[hanging=true]",
						"setblock ~-2 ~2 ~-20 copper_wall_torch[facing=east]" };
				for (String command : mine) {
					world.getServer().runCommand("execute as @a at @s run " + command);
				}
				// Timber frames every six blocks: two posts and a beam
				for (int z = -3; z >= -21; z -= 6) {
					world.getServer().runCommand("execute as @a at @s run fill ~-2 ~ ~" + z + " ~-2 ~2 ~" + z + " oak_log");
					world.getServer().runCommand("execute as @a at @s run fill ~2 ~ ~" + z + " ~2 ~2 ~" + z + " oak_log");
					world.getServer().runCommand("execute as @a at @s run fill ~-2 ~3 ~" + z + " ~2 ~3 ~" + z + " oak_planks");
				}
				// From the gallery's mouth, at eye level, the game's brightness up so the colors read on a page
				world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~2 180 3");
				double gamma = context.computeOnClient(client -> client.options.gamma().get());
				context.runOnClient(client -> client.options.gamma().set(1.0));
				settle(context);
				shot(context, site, "graphics");
				context.runOnClient(client -> client.options.gamma().set(gamma));
				world.getServer().runCommand("forceload remove all");
			}
			if (wanted.test("graphics-lichen")) {
				// The cave with the most glow lichen, lit by nothing else: at night, so no daylight comes in through an
				// opening to the surface
				BlockPos[] view = world.getServer().computeOnServer(server -> cave(server.overworld()));
				world.getServer().runCommand("time set 18000");
				world.getServer().runCommand("tp @a " + view[0].getX() + ".5 " + view[0].getY() + " " + view[0].getZ() + ".5 facing " + view[1].getX() + ".5 "
						+ view[1].getY() + " " + view[1].getZ() + ".5");
				settle(context);
				shot(context, site, "graphics-lichen");
			}
			if (wanted.test("sea") || wanted.test("sea-mask")) {
				// Twelve blocks down in the open sea at noon, looking west into the murk: a drowned coming out of it
				// fourteen blocks away, another one only a shadow behind; then the same through the diving mask
				BlockPos sea = world.getServer().computeOnServer(server -> openSea(server.overworld()));
				int y = sea.getY() - 12;
				world.getServer().runCommand("time set 6000");
				world.getServer().runCommand("summon drowned " + (sea.getX() - 14) + ".5 " + (y - 1) + " " + sea.getZ()
						+ ".5 {NoAI:1b,PersistenceRequired:1b,Rotation:[90f,0f],equipment:{mainhand:{id:\"minecraft:trident\"}}}");
				world.getServer().runCommand("summon drowned " + (sea.getX() - 26) + ".5 " + (y - 1) + " " + (sea.getZ() + 3)
						+ ".5 {NoAI:1b,PersistenceRequired:1b,Rotation:[100f,0f]}");
				world.getServer().runCommand("tp @a " + sea.getX() + ".5 " + y + " " + sea.getZ() + ".5 90 0");
				if (wanted.test("sea")) {
					settle(context);
					// The eyes used to the water, as after a few seconds in it
					context.waitTicks(100);
					shot(context, site, "sea");
				}
				if (wanted.test("sea-mask")) {
					world.getServer().runCommand("item replace entity @a armor.head with cassonade:diving_mask");
					settle(context);
					context.waitTicks(100);
					shot(context, site, "sea-mask");
					world.getServer().runCommand("item replace entity @a armor.head with air");
				}
				world.getServer().runCommand("kill @e[type=drowned]");
			}
			context.runOnClient(client -> client.gui.hud.toggle());
		}
	}

	/**
	 * A big cave around the spawn, scored with its neighbors since a cave spans several chunks: the most glow lichen
	 * among those with a lot of open space and no lava anywhere near, its light alone. Then a spot across the cave, in
	 * the open, that sees the heart of the lichen.
	 * Returns where to stand and what to look at.
	 */
	private static BlockPos[] cave(ServerLevel level) {
		int r = 24;
		int size = 2 * r + 1;
		int[][] lichen = new int[size][size];
		int[][] open = new int[size][size];
		int[][] lava = new int[size][size];
		boolean[][] other = new boolean[size][size];
		long[][][] sums = new long[size][size][3];
		for (int cx = -r; cx <= r; cx++) {
			for (int cz = -r; cz <= r; cz++) {
				var chunk = level.getChunk(cx, cz);
				int i = cx + r;
				int k = cz + r;
				// Lichen grows down to 13 blocks under the surface
				int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR, cx * 16 + 8, cz * 16 + 8) - 13;
				for (int y = -58; y < top; y++) {
					for (int x = 0; x < 16; x++) {
						for (int z = 0; z < 16; z++) {
							BlockPos pos = new BlockPos(cx * 16 + x, y, cz * 16 + z);
							BlockState state = chunk.getBlockState(pos);
							if (state.isAir()) {
								open[i][k]++;
							} else if (state.is(Blocks.GLOW_LICHEN)) {
								lichen[i][k]++;
								sums[i][k][0] += pos.getX();
								sums[i][k][1] += pos.getY();
								sums[i][k][2] += pos.getZ();
							} else if (state.is(Blocks.LAVA)) {
								lava[i][k]++;
							} else if (state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.OAK_PLANKS) || state.is(Blocks.DARK_OAK_PLANKS) || state.is(Blocks.SCULK)) {
								// A lush cave, a mineshaft or the deep dark: not the bare stone cave wanted
								other[i][k] = true;
							}
						}
					}
				}
			}
		}
		BlockPos best = null;
		int bestLichen = 0;
		for (int i = 2; i < size - 2; i++) {
			for (int k = 2; k < size - 2; k++) {
				int around = 0;
				int space = 0;
				int near = 0;
				boolean mixed = false;
				long[] sum = new long[3];
				for (int di = -1; di <= 1; di++) {
					for (int dk = -1; dk <= 1; dk++) {
						around += lichen[i + di][k + dk];
						space += open[i + di][k + dk];
						mixed |= other[i + di][k + dk];
						for (int a = 0; a < 3; a++) {
							sum[a] += sums[i + di][k + dk][a];
						}
					}
				}
				for (int di = -2; di <= 2; di++) {
					for (int dk = -2; dk <= 2; dk++) {
						near += lava[i + di][k + dk];
					}
				}
				if (mixed || space < 30000 || around <= bestLichen || near > 0) {
					continue;
				}
				// The lichen's middle, then the lichen block nearest it: what the view is turned to
				BlockPos middle = new BlockPos((int) (sum[0] / around), (int) (sum[1] / around), (int) (sum[2] / around));
				BlockPos nearest = null;
				for (BlockPos pos : BlockPos.betweenClosed(middle.offset(-12, -12, -12), middle.offset(12, 12, 12))) {
					if (level.getBlockState(pos).is(Blocks.GLOW_LICHEN) && (nearest == null || pos.distSqr(middle) < nearest.distSqr(middle))) {
						nearest = pos.immutable();
					}
				}
				if (nearest != null) {
					bestLichen = around;
					best = nearest;
				}
			}
		}
		if (best == null) {
			throw new AssertionError("Expected a big cave with glow lichen and no lava");
		}
		// Across the cave: the most open spot 10 to 24 blocks away and a few above, from which the target shows
		BlockPos stand = null;
		int widest = 0;
		Vec3 target = Vec3.atCenterOf(best);
		for (BlockPos pos : BlockPos.betweenClosed(best.offset(-24, 2, -24), best.offset(24, 10, 24))) {
			double distance = Math.sqrt(pos.distSqr(best));
			if (distance < 10 || distance > 24 || !level.getBlockState(pos).isAir()) {
				continue;
			}
			int around = 0;
			for (BlockPos near : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 2, 3))) {
				if (level.getBlockState(near).isAir()) {
					around++;
				}
			}
			if (around <= widest) {
				continue;
			}
			BlockHitResult hit = level.clip(new ClipContext(Vec3.atCenterOf(pos), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
			if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().distSqr(best) <= 2) {
				widest = around;
				stand = pos.immutable();
			}
		}
		if (stand == null) {
			throw new AssertionError("Expected a spot that sees the cave at " + best);
		}
		System.out.println("Cave shot: " + bestLichen + " glow lichen around, looking at " + best.toShortString() + " from "
				+ stand.toShortString() + " (" + widest + " open around)");
		return new BlockPos[] { stand, best };
	}

	/**
	 * A spot on the surface of a deep ocean with deep water a long way west of it, so the sun sets behind open sea
	 * rather than behind land or over a shallow floor.
	 */
	private static BlockPos openSea(ServerLevel level) {
		BlockPos found = nearest(level, Biomes.DEEP_OCEAN);
		int sea = level.getSeaLevel();
		for (int radius = 0; radius <= 512; radius += 32) {
			for (int dx = -radius; dx <= radius; dx += 32) {
				for (int dz = -radius; dz <= radius; dz += 32) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
						continue;
					}
					BlockPos spot = found.offset(dx, 0, dz);
					// Deep all around the camera and ahead of it, where the floor would show through the near water; then
					// no land up to the horizon
					boolean open = true;
					for (int x = -24; x <= 96 && open; x += 4) {
						for (int z = -24; z <= 24 && open; z += 8) {
							open = floor(level, spot.offset(-x, 0, z)) < sea - 16;
						}
					}
					for (int west = 96; west <= 256 && open; west += 16) {
						open = floor(level, spot.west(west)) < sea - 6;
					}
					if (open) {
						return spot.atY(sea);
					}
				}
			}
		}
		throw new AssertionError("Expected open sea around " + found);
	}

	private static int floor(ServerLevel level, BlockPos column) {
		level.getChunk(column);
		return level.getHeight(Heightmap.Types.OCEAN_FLOOR, column.getX(), column.getZ());
	}

	/** Coral blocks around a spot of the floor, to tell a reef from a lone block. */
	private static int corals(ServerLevel level, BlockPos ground) {
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(ground.offset(-4, -2, -4), ground.offset(4, 2, 4))) {
			if (level.getBlockState(pos).is(BlockTags.CORAL_BLOCKS)) {
				count++;
			}
		}
		return count;
	}

	/** A reef on the floor of a warm ocean a few blocks under the surface, with open water west of it for the camera. */
	private static BlockPos reef(ServerLevel level) {
		BlockPos found = nearest(level, Biomes.WARM_OCEAN);
		int sea = level.getSeaLevel();
		for (int radius = 0; radius <= 128; radius += 4) {
			for (int dx = -radius; dx <= radius; dx += 4) {
				for (int dz = -radius; dz <= radius; dz += 4) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
						continue;
					}
					BlockPos column = found.offset(dx, 0, dz);
					int top = floor(level, column);
					BlockPos ground = new BlockPos(column.getX(), top - 1, column.getZ());
					if (top >= sea - 10 && top <= sea - 5 && level.getBlockState(ground).is(BlockTags.CORAL_BLOCKS)
							&& floor(level, column.west(8)) <= top && corals(level, ground) >= 12) {
						return ground.above();
					}
				}
			}
		}
		throw new AssertionError("Expected a coral reef around " + found);
	}

	private static BlockPos nearest(TestSingleplayerContext world, ResourceKey<Biome> biome) {
		return world.getServer().computeOnServer(server -> nearest(server.overworld(), biome));
	}

	private static BlockPos nearest(ServerLevel level, ResourceKey<Biome> biome) {
		Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(holder -> holder.is(biome), BlockPos.ZERO, 6400, 32, 64);
		if (found == null) {
			throw new AssertionError("Expected " + biome.identifier() + " in the graphics' world");
		}
		return found.getFirst();
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

	/** Where the player was left, looking as they were. */
	private static void scene(ClientGameTestContext context, TestSingleplayerContext world) {
		teleport(context, world, SCENE, -147.7f, 8.7f);
	}

	private static void select(ClientGameTestContext context, TestSingleplayerContext world, int slot) {
		world.getServer().runOnServer(server -> player(server).getInventory().setSelectedSlot(slot));
		context.runOnClient(client -> client.player.getInventory().setSelectedSlot(slot));
	}

	private static void teleport(ClientGameTestContext context, TestSingleplayerContext world, BlockPos column, float yaw, float pitch) {
		BlockPos ground = world.getServer().computeOnServer(server -> surface(server, column));
		world.getServer().runCommand("tp @a " + ground.getX() + ".5 " + ground.getY() + " " + ground.getZ() + ".5 " + yaw + " " + pitch);
		settle(context);
	}

	/**
	 * How far the world is sent and drawn: the server sends as far as the client asked, which is only told again when
	 * the options are, so setting the option alone leaves the land stopping at the old distance.
	 */
	private static void viewDistance(ClientGameTestContext context, TestSingleplayerContext world, int chunks) {
		context.runOnClient(client -> {
			client.options.renderDistance().set(chunks);
			client.options.broadcastOptions();
		});
		world.getServer().runOnServer(server -> server.getPlayerList().setViewDistance(chunks));
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
			// Dropped rather than waited for, as the quest test does: the first advancement's banner, out of the icon
			context.runOnClient(client -> AchievementBanner.clear());
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
	private static void worldMapBiome(ClientGameTestContext context, TestSingleplayerContext world, Path site) {
		// The world map with Shift held over a place: the name of its biome
		teleport(context, world, SCENE, 0, 0);
		context.setScreen(WorldMapScreen::new);
		context.waitForScreen(WorldMapScreen.class);
		context.getInput().holdShift();
		context.getInput().setCursorPos(1180, 560);
		context.waitTicks(5);
		shot(context, site, "worldmap-biome");
		context.getInput().releaseShift();
		context.setScreen(() -> null);
	}

	/** The map open on the scene, with the markers and waystones of the world. */
	private static void worldMap(ClientGameTestContext context, TestSingleplayerContext world, Path site) {
		teleport(context, world, SCENE, 0, 0);
		context.setScreen(WorldMapScreen::new);
		context.waitForScreen(WorldMapScreen.class);
		// On the map, so its corner shows the position under the cursor wherever the real pointer is
		context.getInput().setCursorPos(1180, 560);
		context.waitTicks(5);
		shot(context, site, "worldmap");
	}

	private static void shot(ClientGameTestContext context, Path site, String id) {
		context.runOnClient(client -> {
			AchievementBanner.clear();
			client.gui.toastManager().clear();
			// Not the quest the advancements just granted completed, but the one now current
			QuestTracker.reset();
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
