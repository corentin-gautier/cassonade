package com.cassonade.test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;

import com.cassonade.client.map.ChatCoordinates;
import com.cassonade.client.map.LodestoneMarkers;
import com.cassonade.client.map.Pings;
import com.cassonade.client.map.WorldMap;
import com.cassonade.client.map.WorldMapScreen;
import com.cassonade.client.quest.AchievementBanner;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.compass.PlayerMarkers;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;

/**
 * Markers, a compass worn: banners in the world, smaller the farther they are, on the minimap and the world map.
 * A spot pointed out with Shift and a middle click.
 */
public class MarkersClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("time set 6000");
			// Looking south at markers 10, 60 and 200 blocks away
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 0");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.COMPASS, new ItemStack(Items.COMPASS)));
				PlayerMarkers.place(player, GlobalPos.of(Level.OVERWORLD, player.blockPosition().south(10).east(3)), "Home", DyeColor.RED);
				PlayerMarkers.place(player, GlobalPos.of(Level.OVERWORLD, player.blockPosition().south(60).west(10)), "Farm", DyeColor.LIME);
				PlayerMarkers.place(player, GlobalPos.of(Level.OVERWORLD, player.blockPosition().south(200).east(30)), "Mine", DyeColor.ORANGE);
			});
			// Without the advancement banners, the toasts and the chat: dropped rather than waited for until they fade
			context.runOnClient(client -> {
				AchievementBanner.clear();
				client.gui.toastManager().clear();
				client.gui.hud.getChat().clearMessages(false);
			});
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-markers");

			// Shift and a middle click points out the spot looked at: a red point with the player's name, for a moment
			world.getServer().runCommand("execute as @a at @s run setblock ~4 ~1 ~30 stone");
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ -7.6 0");
			world.getConnection().waitForChunksRender();
			context.getInput().holdShift();
			context.waitTicks(1);
			context.getInput().pressKey(options -> options.keyPickItem);
			context.waitTicks(1);
			context.getInput().releaseShift();
			context.waitFor(client -> !Pings.shown().isEmpty(), 40);
			String pinged = context.computeOnClient(client -> Pings.shown().stream()
					.map(ping -> Math.round(ping.pos().z - client.player.getZ()) + " " + ping.player()).toList().toString());
			check(pinged.equals("[30 " + context.computeOnClient(client -> client.player.getScoreboardName()) + "]"),
					"the spot pointed out, 30 blocks south, shared under the player's name (" + pinged + ")");
			check(context.computeOnClient(client -> client.player.getInventory().countItem(Items.STONE)) == 0, "the middle click with Shift picks no block");
			Screenshots.take(context, "cassonade-ping");
			BlockPos beforeDimensionChange = world.getServer().computeOnServer(server ->
					server.getPlayerList().getPlayers().getFirst().blockPosition());
			world.getServer().runCommand("execute as @a in minecraft:the_nether run tp @s 0 80 0");
			context.waitFor(client -> client.level.dimension() == Level.NETHER, 100);
			context.waitFor(client -> Pings.shown().isEmpty(), 40);
			world.getServer().runCommand("execute as @a in minecraft:overworld run tp @s "
					+ beforeDimensionChange.getX() + " " + beforeDimensionChange.getY() + " " + beforeDimensionChange.getZ());
			context.waitFor(client -> client.level.dimension() == Level.OVERWORLD, 100);
			context.waitTicks(130);
			check(context.computeOnClient(client -> Pings.shown().isEmpty()), "the ping gone after a moment");
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 0");

			// A renamed lodestone compass carried marks its lodestone, named after the compass
			world.getServer().runCommand("execute as @a at @s run setblock ~25 ~ ~30 lodestone");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				BlockPos lodestone = player.blockPosition().offset(25, 0, 30);
				ItemStack compass = new ItemStack(Items.COMPASS);
				compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(Level.OVERWORLD, lodestone)), true));
				compass.set(DataComponents.CUSTOM_NAME, Component.literal("Base"));
				player.getInventory().setItem(9, compass);
			});
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			List<String> lodestones = context.computeOnClient(client -> LodestoneMarkers.here(client.player).stream()
					.map(at -> at.name().getString()).toList());
			check(lodestones.equals(List.of("Base")), "the compass's lodestone marked, under the compass's name (" + lodestones + ")");

			// With the world map worn, coordinates in the chat link to it: a click opens it there, naming a marker
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.MAP, new ItemStack(CassonadeItems.WORLD_MAP)));
			});
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			List<String> links = context.computeOnClient(client -> {
				List<String> found = new ArrayList<>();
				for (String text : List.of("Base at x: 100, y: 70, z: -200 come", "the village is at 120 64 -300", "I have 3 apples, 12 carrots")) {
					ChatCoordinates.link(Component.literal(text)).visit((style, piece) -> {
						if (style.getClickEvent() instanceof ClickEvent.Custom) {
							found.add(piece);
						}
						return Optional.empty();
					}, Style.EMPTY);
				}
				return found;
			});
			check(links.equals(List.of("x: 100, y: 70, z: -200", "120 64 -300")), "coordinates linked, not other numbers (" + links + ")");
			context.runOnClient(client -> ChatCoordinates.clicked(Optional.of(coordinates(120, -300))));
			context.waitForScreen(WorldMapScreen.class);
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-markers-from-chat");
			context.setScreen(() -> null);

			// The same diamonds on the world map, with the spawn chunk outlined a little to the west
			world.getServer().runCommand("execute as @a at @s run setworldspawn ~-40 ~ ~20");
			world.getConnection().waitForClientboundPackets();
			context.setScreen(WorldMapScreen::new);
			context.waitTicks(10);
			context.waitFor(client -> {
				BlockPos pos = client.player.blockPosition();
				return WorldMap.biomeAt(client.level.dimension(), pos.getX(), pos.getZ()) != null;
			}, 40);
			IdentifierAndBiome captured = context.computeOnClient(client -> {
				BlockPos pos = client.player.blockPosition();
				int sampleX = Math.floorDiv(pos.getX(), 4) * 4 + 2;
				int sampleZ = Math.floorDiv(pos.getZ(), 4) * 4 + 2;
				int surfaceY = client.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, sampleX, sampleZ) - 1;
				return new IdentifierAndBiome(WorldMap.biomeAt(client.level.dimension(), pos.getX(), pos.getZ()).toString(),
						client.level.getBiome(new BlockPos(sampleX, surfaceY, sampleZ)).unwrapKey().orElseThrow().identifier().toString());
			});
			check(captured.saved().equals(captured.expected()), "the world map samples the biome at its surface (" + captured + ")");
			context.runOnClient(client -> {
				WorldMap.close();
				WorldMap.open(client);
			});
			String reloaded = context.computeOnClient(client -> WorldMap.biomeAt(client.level.dimension(),
					client.player.getBlockX(), client.player.getBlockZ()).toString());
			check(reloaded.equals(captured.saved()), "the world map saves and reloads the surface biome (" + captured.saved() + ", " + reloaded + ")");
			Screenshots.take(context, "cassonade-markers-map");

			// A soft pulse marks the spot placed or removed, without a notification over the map
			int[] placed = context.computeOnClient(client -> {
				WorldMapScreen screen = (WorldMapScreen) client.gui.screen();
				double x = client.getWindow().getGuiScaledWidth() / 2.0 + 100;
				double y = client.getWindow().getGuiScaledHeight() / 2.0;
				MouseButtonInfo right = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0);
				screen.mouseClicked(new MouseButtonEvent(x, y, right), false);
				int markerX = get(screen, "pendingX");
				int markerZ = get(screen, "pendingZ");
				boolean naming = get(screen, "nameBox") != null;
				screen.keyPressed(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
				return new int[] {markerX, markerZ, naming ? 1 : 0, get(screen, "markerFeedback") != null ? 1 : 0};
			});
			check(placed[2] == 1 && placed[3] == 1, "placing a marker previews it and pulses its spot");
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			check(context.computeOnClient(client -> PlayerMarkers.of(client.player).markers().stream()
					.anyMatch(marker -> marker.pos().pos().getX() == placed[0] && marker.pos().pos().getZ() == placed[1])),
					"the marker placed from the map reaches the player");

			boolean removalPulsed = context.computeOnClient(client -> {
				WorldMapScreen screen = (WorldMapScreen) client.gui.screen();
				double x = client.getWindow().getGuiScaledWidth() / 2.0 + 100;
				double y = client.getWindow().getGuiScaledHeight() / 2.0;
				screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0)), false);
				return get(screen, "markerFeedback") != null;
			});
			check(removalPulsed, "removing a marker pulses its spot");
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			check(context.computeOnClient(client -> PlayerMarkers.of(client.player).markers().stream()
					.noneMatch(marker -> marker.pos().pos().getX() == placed[0] && marker.pos().pos().getZ() == placed[1])),
					"the marker removed from the map is gone");
		}
	}

	private record IdentifierAndBiome(String saved, String expected) {
	}

	private static CompoundTag coordinates(int x, int z) {
		CompoundTag at = new CompoundTag();
		at.putInt("x", x);
		at.putInt("z", z);
		return at;
	}

	@SuppressWarnings("unchecked")
	private static <T> T get(Object target, String field) {
		try {
			Field declared = target.getClass().getDeclaredField(field);
			declared.setAccessible(true);
			return (T) declared.get(target);
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
