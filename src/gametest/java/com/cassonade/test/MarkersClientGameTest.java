package com.cassonade.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

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
import com.cassonade.client.map.WorldMapScreen;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.compass.PlayerMarkers;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;

/** Markers, a compass worn: banners in the world, smaller the farther they are, on the minimap and the world map. */
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
			world.getServer().runCommand("tp @a ~ ~ ~ 0 0");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.COMPASS, new ItemStack(Items.COMPASS)));
				PlayerMarkers.place(player, GlobalPos.of(Level.OVERWORLD, player.blockPosition().south(10).east(3)), "Home", DyeColor.RED);
				PlayerMarkers.place(player, GlobalPos.of(Level.OVERWORLD, player.blockPosition().south(60).west(10)), "Farm", DyeColor.LIME);
				PlayerMarkers.place(player, GlobalPos.of(Level.OVERWORLD, player.blockPosition().south(200).east(30)), "Mine", DyeColor.ORANGE);
			});
			// Once the advancement toasts and the chat have faded
			context.waitTicks(220);
			context.takeScreenshot("cassonade-markers");

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
			context.takeScreenshot("cassonade-markers-from-chat");
			context.setScreen(() -> null);

			// The same diamonds on the world map, with the spawn chunk outlined a little to the west
			world.getServer().runCommand("execute as @a at @s run setworldspawn ~-40 ~ ~20");
			world.getConnection().waitForClientboundPackets();
			context.setScreen(WorldMapScreen::new);
			context.waitTicks(10);
			context.takeScreenshot("cassonade-markers-map");
		}
	}

	private static CompoundTag coordinates(int x, int z) {
		CompoundTag at = new CompoundTag();
		at.putInt("x", x);
		at.putInt("z", z);
		return at;
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
