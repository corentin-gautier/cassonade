package com.cassonade.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;

import com.cassonade.Cassonade;
import com.cassonade.client.map.WorldMap;
import com.cassonade.client.map.WorldMapScreen;

/**
 * The world map's farthest zoom on a world a player truly explored: generated terrain walked in a spiral,
 * more regions than the cache holds. Exploring takes minutes, so the world is kept between runs, like the
 * website's; only with -PexploredWorld.
 */
public class ExploredWorldClientGameTest implements FabricClientGameTest {
	private static final String SEED = "vanillasugar";
	private static final int STOPS = 260;
	/** In chunks between stops: 6 around each count as explored, so they overlap. */
	private static final int STEP = 10;

	@Override
	public void runTest(ClientGameTestContext context) {
		String folder = System.getProperty("cassonade.exploredWorld");
		if (folder == null || TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = exploredWorld(context, Path.of(folder))) {
			context.waitTicks(20);
			Path regions = context.computeOnClient(client -> client.getSingleplayerServer().getWorldPath(LevelResource.ROOT))
					.resolve("cassonade/worldmap/minecraft/overworld");
			Set<String> missing = new LinkedHashSet<>();
			for (int[] stop : route()) {
				String name = "r." + (stop[0] >> 5) + "." + (stop[1] >> 5) + ".bin";
				if (!Files.exists(regions.resolve(name))) {
					missing.add(name);
				}
			}
			check(missing.isEmpty(), "every region walked through is saved, missing " + missing);
			farZoom(context, "explored-world", 0);
			farZoom(context, "explored-world-smallest-interface", 1);
			loadingScreen(context, "loading-explored-world", 0, 0);
			loadingScreen(context, "loading-new-world", 4000, 4000);
			context.setScreen(() -> new GenericMessageScreen(Component.translatable("selectWorld.data_read")));
			context.waitForScreen(GenericMessageScreen.class);
			context.waitTicks(2);
			Screenshots.take(context, "loading-preparing");
			context.setScreen(() -> new GenericMessageScreen(Component.translatable("menu.savingLevel")));
			context.waitTicks(2);
			Screenshots.take(context, "loading-saving");
			// A change of dimension has no chunk view and follows no connection
			context.setScreen(() -> new LevelLoadingScreen(new LevelLoadTracker(), LevelLoadingScreen.Reason.OTHER));
			context.waitForScreen(LevelLoadingScreen.class);
			context.waitTicks(2);
			Screenshots.take(context, "changing-dimension-stays-vanilla");
			context.setScreen(() -> null);
			context.waitTicks(2);
		}
	}

	private static void farZoom(ClientGameTestContext context, String name, int guiScale) {
		int previous = context.computeOnClient(client -> client.options.guiScale().get());
		context.runOnClient(client -> {
			client.options.guiScale().set(guiScale);
			client.resizeGui();
		});
		context.setScreen(WorldMapScreen::new);
		context.waitForScreen(WorldMapScreen.class);
		context.waitTicks(1);
		long loadedBefore = context.computeOnClient(client -> WorldMap.loadedRegions(client.level.dimension()));
		context.runOnClient(client -> {
			for (int i = 0; i < 3; i++) {
				client.gui.screen().mouseScrolled(client.getWindow().getGuiScaledWidth() / 2.0, client.getWindow().getGuiScaledHeight() / 2.0, 0, -1);
			}
		});
		context.waitFor(client -> WorldMap.pendingOverviews(client.level.dimension()) == 0, 400);
		// Without this the game holds a window left idle at 30, then 10 frames per second
		context.runOnClient(client -> client.getFramerateLimitTracker().onInputReceived());
		context.waitTicks(30);
		int fps = context.computeOnClient(client -> client.getFps());
		Cassonade.LOGGER.info("{}: {} fps, {} overviews drawn", name, fps, context.computeOnClient(client -> WorldMap.drawnOverviews(client.level.dimension())));
		Screenshots.take(context, name + "-far-zoom");
		check(context.computeOnClient(client -> WorldMap.loadedRegions(client.level.dimension())) - loadedBefore <= 8,
				"the far zoom does not load the regions in full");
		check(fps >= 20, "the far zoom stays fast, " + fps + " fps");
		context.setScreen(() -> null);
		context.runOnClient(client -> {
			client.options.guiScale().set(previous);
			client.resizeGui();
		});
		context.waitTicks(2);
	}

	/** The world loading screen around a chunk, half of its chunks loaded. */
	private static void loadingScreen(ClientGameTestContext context, String name, int chunkX, int chunkZ) {
		LevelLoadTracker tracker = new LevelLoadTracker();
		tracker.setServerChunkStatusView(new ChunkLoadStatusView() {
			@Override
			public void moveTo(ResourceKey<Level> dimension, ChunkPos center) {
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
		tracker.updateFocus(Level.OVERWORLD, new ChunkPos(chunkX, chunkZ));
		for (LevelLoadListener.Stage stage : LevelLoadListener.Stage.values()) {
			tracker.start(stage, 100);
			tracker.update(stage, 60, 100);
		}
		context.setScreen(() -> new LevelLoadingScreen(tracker, LevelLoadingScreen.Reason.OTHER));
		context.waitForScreen(LevelLoadingScreen.class);
		context.waitTicks(40);
		Screenshots.take(context, name);
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	private static TestSingleplayerContext exploredWorld(ClientGameTestContext context, Path cache) {
		String key = SEED + '|' + STOPS + '|' + STEP + '|' + SharedConstants.getCurrentVersion().name();
		Path keyFile = cache.resolve(SiteScreenshotsClientGameTest.CACHE_KEY_FILE);
		try {
			TestWorldSave save;
			if (Files.isRegularFile(keyFile) && Files.readString(keyFile).equals(key)) {
				// A flat world, quick to make, for its folder: the one kept takes its place
				try (TestSingleplayerContext placeholder = context.worldBuilder().create()) {
					save = placeholder.getWorldSave();
				}
				SiteScreenshotsClientGameTest.replace(cache, save.getSaveDirectory());
				return save.open();
			}
			try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(state -> {
				state.setSeed(SEED);
				state.setWorldType(state.getNormalPresetList().getFirst());
				state.setAllowCommands(true);
			}).create()) {
				save = world.getWorldSave();
				context.waitTicks(40);
				context.setScreen(() -> null);
				// Above the ground and out of harm's way, the chunks generating as for a walker
				world.getServer().runCommand("gamemode spectator @a");
				int done = 0;
				for (int[] stop : route()) {
					visit(context, world, stop[0], stop[1]);
					if (++done % 50 == 0) {
						Cassonade.LOGGER.info("Explored {} stops of {}", done, STOPS);
					}
				}
			}
			SiteScreenshotsClientGameTest.replace(save.getSaveDirectory(), cache);
			Files.writeString(keyFile, key);
			return save.open();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	/** The stops' chunks, a square spiral from the spawn. */
	private static int[][] route() {
		int[][] route = new int[STOPS][];
		int x = 0;
		int z = 0;
		int dx = 1;
		int dz = 0;
		int leg = 1;
		int stop = 0;
		while (stop < STOPS) {
			for (int turn = 0; turn < 2; turn++) {
				for (int i = 0; i < leg && stop < STOPS; i++) {
					route[stop++] = new int[] {x * STEP, z * STEP};
					x += dx;
					z += dz;
				}
				int turned = dx;
				dx = -dz;
				dz = turned;
			}
			leg++;
		}
		return route;
	}

	private static void visit(ClientGameTestContext context, TestSingleplayerContext world, int chunkX, int chunkZ) {
		world.getServer().runCommand("tp @a " + (chunkX * 16 + 8) + " 200 " + (chunkZ * 16 + 8));
		context.waitFor(client -> {
			for (int dz = -5; dz <= 5; dz++) {
				for (int dx = -5; dx <= 5; dx++) {
					// Loaded with its blocks: the client holds chunks before their blocks arrive, which the map skips
					if (dx * dx + dz * dz <= 25 && (!client.level.hasChunk(chunkX + dx, chunkZ + dz)
							|| client.level.getHeight(Heightmap.Types.WORLD_SURFACE, (chunkX + dx) * 16 + 8, (chunkZ + dz) * 16 + 8) <= client.level.getMinY())) {
						return false;
					}
				}
			}
			return client.player.blockPosition().getX() >> 4 == chunkX && client.player.blockPosition().getZ() >> 4 == chunkZ;
		}, 20 * 120);
		context.waitTicks(18);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
