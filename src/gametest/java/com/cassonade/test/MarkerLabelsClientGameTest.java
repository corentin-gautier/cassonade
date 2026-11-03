package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import com.cassonade.client.BossBars;
import com.cassonade.client.CassonadeClient;
import com.cassonade.client.ClientSettings;
import com.cassonade.client.ClientSettings.MarkerDisplay;
import com.cassonade.client.map.LabelSpots;
import com.cassonade.client.map.WorldMapScreen;
import com.cassonade.compass.PlayerMarkers;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;

/** Markers side by side: the direction bar names one, and shown in the world or on the maps their names don't land on each other. */
public class MarkerLabelsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		LabelSpots spots = new LabelSpots();
		check(spots.place(0, 0, 20, 10, 10) == 0, "the first label keeps its place");
		check(spots.place(5, 0, 20, 10, 10) == 10, "the next moves a line away");
		check(spots.place(100, 0, 20, 10, 10) == 0, "one elsewhere stays");
		spots.place(0, 20, 20, 10, 10);
		spots.place(0, 30, 20, 10, 10);
		check(Float.isNaN(spots.place(0, 0, 20, 10, 10)), "one with no room left is left out");

		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 0");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.COMPASS, new ItemStack(Items.COMPASS)));
			});
			// No direction bar with nothing to show on it
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-direction-bar-empty");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				PlayerMarkers.place(player, GlobalPos.of(Level.OVERWORLD, player.blockPosition().south(20)), "Old mine", DyeColor.RED);
				PlayerMarkers.place(player, GlobalPos.of(Level.OVERWORLD, player.blockPosition().south(21).east(1)), "Second home", DyeColor.LIME);
				PlayerMarkers.place(player, GlobalPos.of(Level.OVERWORLD, player.blockPosition().south(22).west(1)), "Sheep farm", DyeColor.ORANGE);
				// Far and high up: on the bar with its arrow
				PlayerMarkers.place(player, GlobalPos.of(Level.OVERWORLD, player.blockPosition().south(60).west(15).above(30)), "Peak", DyeColor.BLUE);
			});
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-direction-bar");
			// The bar gives way to a boss bar, and a marker close enough to show in the world with its name, not under the bar
			world.getServer().runCommand("bossbar add cassonade:test \"Boss\"");
			world.getServer().runCommand("bossbar set cassonade:test players @a");
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~10 -20 0");
			context.waitTicks(3);
			check(context.computeOnClient(BossBars::isShown), "the boss bar shown, the direction bar hidden");
			Screenshots.take(context, "cassonade-direction-bar-boss");
			world.getServer().runCommand("bossbar remove cassonade:test");
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~-10 0 0");
			context.waitTicks(3);
			// Hidden, the key held still shows them in the world
			context.runOnClient(client -> ClientSettings.setMarkerDisplay(MarkerDisplay.NONE));
			context.getInput().holdKey(CassonadeClient.MARKERS_IN_WORLD);
			context.waitTicks(1);
			Screenshots.take(context, "cassonade-markers-key-held");
			context.getInput().releaseKey(CassonadeClient.MARKERS_IN_WORLD);
			context.runOnClient(client -> ClientSettings.setMarkerDisplay(MarkerDisplay.WORLD));
			context.waitTicks(1);
			Screenshots.take(context, "cassonade-marker-labels-world");
			context.runOnClient(client -> ClientSettings.setMarkerDisplay(MarkerDisplay.BAR));
			context.setScreen(WorldMapScreen::new);
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-marker-labels-map");
			context.setScreen(() -> null);
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
