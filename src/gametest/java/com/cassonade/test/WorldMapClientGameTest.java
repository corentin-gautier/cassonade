package com.cassonade.test;

import java.nio.file.Files;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;

import com.cassonade.client.ClientSettings;
import com.cassonade.client.CassonadeClient;
import com.cassonade.client.quest.AchievementBanner;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

import net.minecraft.world.level.material.MapColor;
import com.cassonade.Cassonade;
import com.cassonade.client.map.DeathMarker;
import com.cassonade.client.map.MapRegion;
import com.cassonade.client.map.Minimap;
import com.cassonade.client.map.WorldMap;
import com.cassonade.client.map.WorldMapScreen;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.compass.PlayerMarkers;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;

/**
 * The compass and the world map worn: the minimap, with the status effects below it, turning with the player and
 * zoomed out; a strip explored, then a marker named and colored on the world map, seen in the world, removed;
 * the red X where the player died. Markers and explored chunks kept through a reload.
 */
public class WorldMapClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.COMPASS, new ItemStack(Items.COMPASS))
						.with(GearSlot.MAP, new ItemStack(CassonadeItems.WORLD_MAP)));
			});
			context.waitTicks(5);
			// Compass: minimap on the HUD
			context.setScreen(() -> null);
			world.getServer().runCommand("effect give @a speed 60");
			// Some relief on the flat test world so the minimap has something to show
			world.getServer().runCommand("execute as @a at @s run fill ~4 ~-1 ~-12 ~16 ~-1 ~-2 water");
			world.getServer().runCommand("execute as @a at @s run fill ~-14 ~ ~2 ~-6 ~3 ~8 stone");
			world.getServer().runCommand("execute as @a at @s run fill ~-12 ~4 ~4 ~-8 ~5 ~6 stone");
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-4-minimap");

			// Status effects below the minimap, framed like it: beneficial ones, then harmful ones, a beacon's tinted
			world.getServer().runCommand("effect give @a weakness 30");
			world.getServer().runCommand("effect give @a jump_boost 5");
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
					.addEffect(new MobEffectInstance(MobEffects.HASTE, 200, 0, true, true)));
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-4-effects");
			// Next to the inventory, where vanilla lists them, in the same frame
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			context.runOnClient(client -> AchievementBanner.clear());
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-4-effects-inventory");
			context.setScreen(() -> null);
			world.getServer().runCommand("effect clear @a speed");
			world.getServer().runCommand("effect clear @a weakness");
			world.getServer().runCommand("effect clear @a haste");
			world.getServer().runCommand("effect clear @a jump_boost");

			// Turning with the player: looking west, west is up and north on the right
			context.runOnClient(client -> ClientSettings.setMinimapRotates(true));
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ 90 0");
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-4-minimap-rotating");
			context.runOnClient(client -> ClientSettings.setMinimapRotates(false));
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 0");

			context.runOnClient(client -> {
				Minimap.zoomOut();
				Minimap.zoomOut();
			});
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-5-minimap-zoomed-out");
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
			Screenshots.take(context, "cassonade-6-world-map");

			// Zoomed out, more regions show than the cache holds: none may be evicted while it draws them
			MapRegion farRegion = context.computeOnClient(client -> {
				MapRegion first = WorldMap.region(client.level.dimension(), 1000, 1000, true);
				for (int i = 0; i < 64; i++) {
					WorldMap.region(client.level.dimension(), 1000 + i, 1001, true);
				}
				return first;
			});
			context.waitTicks(2);
			check(context.computeOnClient(client -> WorldMap.region(client.level.dimension(), 1000, 1000, false) == farRegion),
					"regions stay loaded while the world map shows them");

			// Right click 40 GUI pixels right of the player (40 blocks at the default zoom), name the marker, pick blue, Enter
			rightClickMap(context, 40);
			ScreenInput.typeOnScreen(context, "Maison");
			// The swatches sit under the name box, centered under the click: blue is the second one
			context.runOnClient(client -> client.gui.screen().mouseClicked(new MouseButtonEvent(
					client.getWindow().getGuiScaledWidth() / 2.0 + 40 - 80 + 15, client.getWindow().getGuiScaledHeight() / 2.0 - 2 + 10 + 20 + 5,
					new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false));
			context.waitTicks(1);
			Screenshots.take(context, "cassonade-7-world-map-naming");
			ScreenInput.pressOnScreen(context, InputConstants.KEY_RETURN);
			world.getConnection().waitForServerboundPackets();
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				var markers = PlayerMarkers.of(server.getPlayerList().getPlayers().getFirst()).markers();
				check(markers.size() == 1 && markers.getFirst().name().equals("Maison") && markers.getFirst().color() == DyeColor.BLUE,
						"named blue marker placed from the world map");
			});
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-7-world-map-marker");

			// Closed map: the marker shows in the world, the player looking east towards it
			context.setScreen(() -> null);
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ -90 10");
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-8-marker-in-world");

			// A right click on the marker removes it; without a compass, a right click does nothing
			context.setScreen(WorldMapScreen::new);
			context.waitForScreen(WorldMapScreen.class);
			rightClickMap(context, 40);
			world.getConnection().waitForServerboundPackets();
			context.waitTicks(2);
			world.getServer().runOnServer(server -> check(PlayerMarkers.of(server.getPlayerList().getPlayers().getFirst()).markers().isEmpty(),
					"marker removed from the world map"));
			rightClickMap(context, 40);
			ScreenInput.typeOnScreen(context, "Mine");
			ScreenInput.pressOnScreen(context, InputConstants.KEY_RETURN);
			world.getConnection().waitForServerboundPackets();
			context.waitTicks(2);

			farZoomFollowsExploration(context, world);
			farZoomOnAVeryExploredWorld(context);

			// Icons only in the world, then back to icons and text
			context.setScreen(() -> null);
			context.getInput().pressKey(CassonadeClient.MARKER_DISPLAY);
			context.waitTicks(2);
			check(context.computeOnClient(client -> ClientSettings.markerDisplay() == ClientSettings.MarkerDisplay.ICON),
					"marker display key moves to icons only");
			Screenshots.take(context, "cassonade-9-marker-icon-only");
			context.runOnClient(client -> ClientSettings.setMarkerDisplay(ClientSettings.MarkerDisplay.ICON_AND_TEXT));

			// Death: a red X where the player died, on the minimap and in the world, its gear kept for the test.
			// Coming back to it puts it away
			world.getServer().runCommand("gamerule keep_inventory true");
			BlockPos death = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().blockPosition());
			world.getServer().runCommand("kill @a");
			context.waitForScreen(DeathScreen.class);
			context.runOnClient(client -> client.player.respawn());
			context.waitForScreen(null);
			world.getServer().runCommand("execute as @a run tp @s " + (death.getX() + 24) + " " + death.getY() + " " + death.getZ() + " 90 10");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(10);
			check(context.computeOnClient(client -> DeathMarker.here(client.player).filter(death::equals).isPresent()),
					"a red X where the player died");
			Screenshots.take(context, "cassonade-9-death-marker");
			world.getServer().runCommand("execute as @a run tp @s " + death.getX() + " " + death.getY() + " " + death.getZ());
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(5);
			check(context.computeOnClient(client -> DeathMarker.here(client.player).isEmpty()), "coming back puts the red X away");
			world.getServer().runCommand("kill @a");
			context.waitForScreen(DeathScreen.class);
			context.runOnClient(client -> client.player.respawn());
			context.waitForScreen(null);
			context.waitTicks(5);
			check(context.computeOnClient(client -> DeathMarker.here(client.player).filter(death::equals).isPresent()),
					"a new death at the same place shows its marker again");
			world.getServer().runCommand("gamerule keep_inventory false");
			save = world.getWorldSave();
		}

		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runOnServer(server -> {
				check(PlayerMarkers.of(server.getPlayerList().getPlayers().getFirst()).markers().size() == 1, "markers kept after reloading the world");
				check(Files.exists(server.getWorldPath(LevelResource.ROOT).resolve("cassonade/worldmap/minecraft/overworld/r.0.0.bin")),
						"explored chunks saved with the world");
			});
		}
	}

	/** Right click on the open world map, some GUI pixels right of its center (the player). */
	private static void rightClickMap(ClientGameTestContext context, int offsetX) {
		context.runOnClient(client -> client.gui.screen().mouseClicked(new MouseButtonEvent(client.getWindow().getGuiScaledWidth() / 2.0 + offsetX,
				client.getWindow().getGuiScaledHeight() / 2.0 - 2, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0)), false));
	}

	/**
	 * The far zoom shows what was just explored, not what the region's file held: a change to a region
	 * already drawn, then a region reached with the map closed and not saved yet.
	 */
	private static void farZoomFollowsExploration(ClientGameTestContext context, TestSingleplayerContext world) {
		openAtFarthestZoom(context);
		context.waitFor(client -> WorldMap.overviewColor(client.level.dimension(), client.player.getBlockX(), client.player.getBlockZ()) != 0, 100);
		world.getServer().runCommand("execute as @a at @s run fill ~-8 ~-1 ~-8 ~8 ~-1 ~8 gold_block");
		context.waitFor(client -> WorldMap.overviewColor(client.level.dimension(), client.player.getBlockX(), client.player.getBlockZ()) >> 2 == MapColor.GOLD.id, 200);

		context.setScreen(() -> null);
		world.getServer().runCommand("execute as @a at @s run tp @s ~1024 ~ ~");
		world.getConnection().waitForChunksRender();
		context.waitTicks(40);
		openAtFarthestZoom(context);
		context.waitFor(client -> WorldMap.overviewColor(client.level.dimension(), client.player.getBlockX(), client.player.getBlockZ()) != 0, 100);
		context.setScreen(() -> null);
		world.getServer().runCommand("execute as @a at @s run tp @s ~-1024 ~ ~");
		world.getConnection().waitForChunksRender();
	}

	private static void openAtFarthestZoom(ClientGameTestContext context) {
		context.setScreen(WorldMapScreen::new);
		context.waitForScreen(WorldMapScreen.class);
		context.runOnClient(client -> {
			for (int i = 0; i < 3; i++) {
				client.gui.screen().mouseScrolled(client.getWindow().getGuiScaledWidth() / 2.0, client.getWindow().getGuiScaledHeight() / 2.0, 0, -1);
			}
		});
	}

	/**
	 * 1024 explored regions, about 16 × 16 km, seen at the farthest zoom: none is loaded in full, every
	 * frame stays fast once they are read, in the smallest interface. The checkerboard shows any region drawn with another's texture.
	 */
	private static void farZoomOnAVeryExploredWorld(ClientGameTestContext context) {
		Path folder = context.computeOnClient(client -> client.getSingleplayerServer().getWorldPath(LevelResource.ROOT))
				.resolve("cassonade/worldmap/minecraft/overworld");
		List<Path> written = new ArrayList<>();
		try {
			Files.createDirectories(folder);
			byte[] colors = new byte[MapRegion.BLOCKS * MapRegion.BLOCKS];
			for (int regionZ = -16; regionZ < 16; regionZ++) {
				for (int regionX = -16; regionX < 16; regionX++) {
					Path file = folder.resolve("r." + regionX + "." + regionZ + ".bin");
					if (!Files.exists(file)) {
						Arrays.fill(colors, (byte) (((regionX + regionZ & 1) == 0 ? 1 : 12) * 4 + 2));
						try (OutputStream out = new DeflaterOutputStream(Files.newOutputStream(file))) {
							out.write(colors);
						}
						written.add(file);
					}
				}
			}

			// The smallest interface shows the most regions at once
			int guiScale = context.computeOnClient(client -> client.options.guiScale().get());
			context.runOnClient(client -> {
				client.options.guiScale().set(1);
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
			context.waitFor(client -> WorldMap.pendingOverviews(client.level.dimension()) == 0 && WorldMap.drawnOverviews(client.level.dimension()) >= 400, 400);
			context.waitTicks(20);
			int fps = context.computeOnClient(client -> client.getFps());
			Cassonade.LOGGER.info("World map at the farthest zoom: {} fps, {} overviews drawn", fps,
					context.computeOnClient(client -> WorldMap.drawnOverviews(client.level.dimension())));
			Screenshots.take(context, "cassonade-6-world-map-far-zoom");
			check(context.computeOnClient(client -> WorldMap.loadedRegions(client.level.dimension())) - loadedBefore <= 8,
					"the far zoom does not load the regions in full");
			check(fps >= 20, "the far zoom stays fast on a very explored world");
			context.setScreen(() -> null);
			context.runOnClient(client -> {
				client.options.guiScale().set(guiScale);
				client.resizeGui();
			});
			context.waitTicks(1);
			for (Path file : written) {
				Files.delete(file);
			}
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
