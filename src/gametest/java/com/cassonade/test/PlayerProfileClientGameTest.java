package com.cassonade.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

import com.cassonade.Cassonade;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;

/**
 * A player's usual minutes, short: a new world, everything of the mod worn, running over chunks being made, at a common
 * render distance then a far one. Frame times and each slow frame's moment in the log, Flight Recorder files in build/profile;
 * only with -Pprofile -Ptests=PlayerProfile. Measures, checks nothing.
 */
public class PlayerProfileClientGameTest implements FabricClientGameTest {
	private static final int SECONDS = 25;
	private static final long SLOW_NANOS = 33_000_000;
	/** A sprint, in blocks a tick. */
	private static final double STEP = 0.28;
	/** The two runs together, with some to spare. */
	private static final int LAND_AHEAD = 320;
	private static final long[] FRAME_TIMES = new long[1 << 16];
	private static final List<String> slowFrames = new ArrayList<>();
	private static volatile int frames;
	private static volatile long lastFrame;

	@Override
	public void runTest(ClientGameTestContext context) {
		String folder = System.getProperty("cassonade.profile");
		if (folder == null || TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		context.runOnClient(_ -> HudElementRegistry.addLast(Cassonade.id("player_profile_frames"), (_, _) -> {
			long now = System.nanoTime();
			if (lastFrame != 0 && frames < FRAME_TIMES.length) {
				FRAME_TIMES[frames++] = now - lastFrame;
				if (now - lastFrame > SLOW_NANOS) {
					slowFrames.add(Instant.now() + " " + (now - lastFrame) / 1_000_000 + "ms");
				}
			}
			lastFrame = now;
		}));
		context.runOnClient(client -> {
			client.options.enableVsync().set(false);
			client.options.framerateLimit().set(260);
			client.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
		});
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(state -> {
			state.setSeed("vanillasugar");
			state.setWorldType(state.getNormalPresetList().getFirst());
			state.setAllowCommands(true);
		}).create()) {
			JournalOffer.follow(context);
			world.getServer().runCommand("gamerule send_command_feedback false");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.getAbilities().mayfly = true;
				player.getAbilities().flying = true;
				player.getAbilities().invulnerable = true;
				player.onUpdateAbilities();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player)
						.with(GearSlot.COMPASS, new ItemStack(CassonadeItems.SCULK_COMPASS))
						.with(GearSlot.MAP, new ItemStack(CassonadeItems.WORLD_MAP))
						.with(GearSlot.CLOCK, new ItemStack(CassonadeItems.BAROMETER))
						.with(GearSlot.BACKPACK, new ItemStack(CassonadeItems.ORGANIZED_BACKPACK)));
			});
			world.getServer().runCommand("item replace entity @a weapon.mainhand with cassonade:sculk_torch");
			// The spawn can be at the sea's edge: somewhere with land all the way
			world.getServer().runOnServer(server -> {
				BlockPos start = landAhead(server.overworld());
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a " + start.getX() + " 150 " + start.getZ() + " -90 10");
			});
			context.waitTicks(60);
			run(context, world, Path.of(folder), "player-12-chunks", 12);
			run(context, world, Path.of(folder), "player-26-chunks", 26);
		}
	}

	/** Along the ground and not its trees, as nothing here presses the keys. */
	private static void step(MinecraftServer server) {
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		double x = player.getX() + STEP;
		int ground = server.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(player.getZ()));
		player.teleportTo(x, ground > server.overworld().getMinY() ? ground : player.getY(), player.getZ());
	}

	private static BlockPos landAhead(ServerLevel level) {
		for (int radius = 0; radius <= 3200; radius += 64) {
			for (int x = -radius; x <= radius; x += 64) {
				for (int z = -radius; z <= radius; z += 64) {
					if (isLandAhead(level, x, z)) {
						return new BlockPos(x, 80, z);
					}
				}
			}
		}
		throw new AssertionError("no land around the spawn");
	}

	private static boolean isLandAhead(ServerLevel level, int x, int z) {
		for (int ahead = 0; ahead <= LAND_AHEAD; ahead += 16) {
			Holder<Biome> biome = level.getBiome(new BlockPos(x + ahead, 80, z));
			if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_BEACH)) {
				return false;
			}
		}
		return true;
	}

	private static void run(ClientGameTestContext context, TestSingleplayerContext world, Path folder, String name, int distance) {
		context.runOnClient(client -> {
			client.options.renderDistance().set(distance);
			client.options.entityDistanceScaling().set(1.0);
			client.options.broadcastOptions();
			client.getFramerateLimitTracker().onInputReceived();
		});
		world.getServer().runOnServer(server -> server.getPlayerList().setViewDistance(distance));
		frames = 0;
		slowFrames.clear();
		try (Recording recording = new Recording(Configuration.getConfiguration("profile"))) {
			recording.start();
			for (int tick = 0; tick < SECONDS * 20; tick++) {
				world.getServer().runOnServer(PlayerProfileClientGameTest::step);
				context.waitTick();
				if (tick % 100 == 0) {
					String at = context.computeOnClient(client -> client.player.blockPosition().toShortString());
					Cassonade.LOGGER.info("Profile at {}", at);
				}
			}
			recording.stop();
			Files.createDirectories(folder);
			recording.dump(folder.resolve(name + ".jfr"));
		} catch (IOException | ParseException e) {
			throw new AssertionError(e);
		}
		long[] times = Arrays.copyOf(FRAME_TIMES, frames);
		Arrays.sort(times);
		long slow = Arrays.stream(times).filter(time -> time > SLOW_NANOS).count();
		String position = context.computeOnClient(client -> client.player.blockPosition().toShortString());
		Cassonade.LOGGER.info("Profile {}: {} fps on average; frames: median {} ms, 99th percentile {} ms, slowest {} ms, {} over 33 ms; ended at {}", name, frames / SECONDS,
				String.format("%.1f", times[times.length / 2] / 1e6), String.format("%.1f", times[times.length * 99 / 100] / 1e6),
				String.format("%.1f", times[times.length - 1] / 1e6), slow, position);
		slowFrames.forEach(frame -> Cassonade.LOGGER.info("Slow frame {}", frame));
		Screenshots.take(context, name);
	}
}
