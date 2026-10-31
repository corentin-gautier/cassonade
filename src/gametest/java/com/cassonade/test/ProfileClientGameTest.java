package com.cassonade.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Arrays;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;

import com.cassonade.Cassonade;
import com.cassonade.client.HiddenEntities;
import com.cassonade.client.map.WorldMapScreen;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;

/**
 * Frame rates on the explored world, each situation with a Flight Recorder file of its own in
 * build/profile, to read with tools/profile_report.py; only with -Pprofile. Measures, checks nothing:
 * the numbers depend on the machine.
 */
public class ProfileClientGameTest implements FabricClientGameTest {
	private static final int SECONDS = 12;
	private static final int RENDER_DISTANCE = 12;
	private static final String STEP = "execute as @a at @s run tp @s ~0.2 ~ ~";

	@Override
	public void runTest(ClientGameTestContext context) {
		String folder = System.getProperty("cassonade.profile");
		String explored = System.getProperty("cassonade.exploredWorld");
		if (folder == null || explored == null || TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		context.runOnClient(client -> {
			client.options.enableVsync().set(false);
			client.options.framerateLimit().set(260);
			client.options.renderDistance().set(RENDER_DISTANCE);
			// Measures an unfocused window too, which the game would otherwise hold back
			client.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
		});
		try (TestSingleplayerContext world = ExploredWorldClientGameTest.exploredWorld(context, Path.of(explored))) {
			context.waitTicks(20);
			// The quest journal's offer: an open screen would pause the game; a choice is per world, so made once in it
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			// The view and entity distances of a game, which the tests turn down
			context.runOnClient(client -> {
				client.options.entityDistanceScaling().set(1.0);
				client.options.broadcastOptions();
			});
			world.getServer().runOnServer(server -> server.getPlayerList().setViewDistance(RENDER_DISTANCE));
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear 100000");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.getAbilities().mayfly = true;
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
				BlockPos forest = nearestForest(server.overworld());
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a " + forest.getX() + " 200 " + forest.getZ());
			});
			settle(context);
			world.getServer().runCommand("execute as @a at @s positioned over motion_blocking run tp @s ~ ~2 ~ 0 20");
			settle(context);
			context.waitTicks(100);

			Path output = Path.of(folder);
			measure(context, output, "still-no-gear", () -> { });
			equip(world);
			context.waitTicks(40);
			measure(context, output, "still-gear", () -> { });
			context.runOnClient(client -> client.gui.hud.toggle());
			measure(context, output, "still-gear-no-hud", () -> { });
			context.runOnClient(client -> client.gui.hud.toggle());

			measure(context, output, "moving", () -> world.getServer().runCommand(STEP));
			world.getServer().runCommand("item replace entity @a weapon.mainhand with torch");
			context.waitTicks(40);
			measure(context, output, "still-torch", () -> { });
			measure(context, output, "moving-torch", () -> world.getServer().runCommand(STEP));
			world.getServer().runCommand("item replace entity @a weapon.mainhand with air");
			context.waitTicks(40);
			settle(context);

			cows(context, world, 0, 6);
			measure(context, output, "still-200-cows", () -> { });
			world.getServer().runCommand("kill @e[type=cow]");
			context.waitTicks(40);

			context.runOnClient(client -> client.gui.setScreen(new InventoryScreen(client.player)));
			context.waitForScreen(InventoryScreen.class);
			measure(context, output, "inventory", () -> { });
			context.setScreen(WorldMapScreen::new);
			context.waitForScreen(WorldMapScreen.class);
			measure(context, output, "world-map", () -> { });
			context.setScreen(() -> null);
			context.waitTicks(2);

			// Last, it leaves its stone: a mine, the player in a closed room and the same cows in another straight
			// ahead, in view but behind rock. The sections the eye can reach are only worked out anew when it changes section
			world.getServer().runCommand("gamerule max_block_modifications 1000000");
			world.getServer().runCommand("execute as @a at @s run fill ~-48 ~-32 ~-32 ~48 ~32 ~80 stone");
			world.getServer().runCommand("execute as @a at @s run fill ~-3 ~-1 ~-3 ~3 ~3 ~3 air");
			world.getServer().runCommand("execute as @a at @s run fill ~-20 ~-8 ~24 ~20 ~8 ~60 air");
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 0");
			context.waitTicks(40);
			settle(context);
			measure(context, output, "mine-no-cows", () -> { });
			cows(context, world, 0, 28);
			world.getServer().runCommand("execute as @a at @s run tp @s ~1 ~ ~");
			context.waitTicks(10);
			world.getServer().runCommand("execute as @a at @s run tp @s ~-17 ~ ~");
			context.waitTicks(20);
			world.getServer().runCommand("execute as @a at @s run tp @s ~16 ~ ~");
			context.waitTicks(40);
			settle(context);
			measure(context, output, "mine-200-cows-behind-rock", () -> { });
			context.runOnClient(client -> {
				int cows = 0;
				int hidden = 0;
				for (Entity entity : client.level.entitiesForRendering()) {
					if (entity.is(EntityTypes.COW)) {
						cows++;
						hidden += HiddenEntities.isHidden(entity, entity.getBoundingBox()) ? 1 : 0;
					}
				}
				Cassonade.LOGGER.info("Profile mine: {} of {} cows behind rock hidden", hidden, cows);
			});
		}
	}

	/** 200 motionless cows ahead of the player, so that the server has no say in the frame rate. */
	private static void cows(ClientGameTestContext context, TestSingleplayerContext world, int y, int z) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			for (int i = 0; i < 200; i++) {
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withEntity(player).withPosition(player.position()),
						"summon cow ~" + (i % 20 - 10) + " ~" + y + " ~" + (z + i / 10) + " {NoAI:1b}");
			}
		});
		context.waitTicks(40);
	}

	/** Every chunk in view loaded and compiled. waitForChunksRender never returns on this world, which is ready within seconds. */
	private static void settle(ClientGameTestContext context) {
		context.waitFor(client -> {
			int distance = client.options.getEffectiveRenderDistance();
			int centerX = client.player.chunkPosition().x();
			int centerZ = client.player.chunkPosition().z();
			for (int dz = -distance; dz <= distance; dz++) {
				for (int dx = -distance; dx <= distance; dx++) {
					if (!client.level.hasChunk(centerX + dx, centerZ + dz)) {
						return false;
					}
				}
			}
			return client.levelRenderer.hasRenderedAllSections();
		}, 1200);
	}

	/** The spawn is out at sea: the nearest forest on the explored route, a scene with leaves and without water. */
	private static BlockPos nearestForest(ServerLevel level) {
		for (int radius = 0; radius <= 1600; radius += 64) {
			for (int x = -radius; x <= radius; x += 64) {
				for (int z = -radius; z <= radius; z += 64) {
					BlockPos pos = new BlockPos(x, 80, z);
					if (level.getBiome(pos).is(Biomes.FOREST)) {
						return pos;
					}
				}
			}
		}
		throw new AssertionError("no forest on the explored route");
	}

	private static void equip(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player)
					.with(GearSlot.COMPASS, new ItemStack(Items.COMPASS))
					.with(GearSlot.MAP, new ItemStack(CassonadeItems.WORLD_MAP))
					.with(GearSlot.CLOCK, new ItemStack(Items.CLOCK))
					.with(GearSlot.BACKPACK, new ItemStack(CassonadeItems.BACKPACK)));
		});
	}

	/** The median of the frame rate the game counts each second, the first seconds dropped while it settles. */
	private static void measure(ClientGameTestContext context, Path output, String name, Runnable everyTick) {
		// Without this the game holds a window left idle at 30, then 10 frames per second
		context.runOnClient(client -> client.getFramerateLimitTracker().onInputReceived());
		for (int tick = 0; tick < 40; tick++) {
			everyTick.run();
			context.waitTick();
		}
		int[] fps = new int[SECONDS];
		try (Recording recording = new Recording(Configuration.getConfiguration("profile"))) {
			recording.start();
			for (int second = 0; second < SECONDS; second++) {
				for (int tick = 0; tick < 20; tick++) {
					everyTick.run();
					context.waitTick();
				}
				fps[second] = context.computeOnClient(client -> client.getFps());
			}
			recording.stop();
			Files.createDirectories(output);
			recording.dump(output.resolve(name + ".jfr"));
		} catch (IOException | ParseException e) {
			throw new AssertionError(e);
		}
		Arrays.sort(fps);
		Cassonade.LOGGER.info("Profile {}: median {} fps, from {} to {}", name, fps[SECONDS / 2], fps[0], fps[SECONDS - 1]);
	}
}
