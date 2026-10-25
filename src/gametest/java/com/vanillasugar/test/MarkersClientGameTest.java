package com.vanillasugar.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import com.vanillasugar.client.map.WorldMapScreen;
import com.vanillasugar.client.quest.QuestLog;
import com.vanillasugar.client.quest.QuestOptInScreen;
import com.vanillasugar.compass.PlayerMarkers;
import com.vanillasugar.gear.GearSlot;
import com.vanillasugar.gear.PlayerGear;

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
			context.takeScreenshot("vanillasugar-markers");

			// The same diamonds on the world map
			context.setScreen(WorldMapScreen::new);
			context.waitTicks(10);
			context.takeScreenshot("vanillasugar-markers-map");
		}
	}
}
