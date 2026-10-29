package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;

import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.client.trader.TraderTimePanel;

public class WanderingTraderClientGameTest implements FabricClientGameTest {
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

			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = server.overworld();
				WanderingTrader trader = EntityTypes.WANDERING_TRADER.create(level, EntitySpawnReason.COMMAND);
				trader.snapTo(player.getX() + 2, player.getY(), player.getZ(), 0, 0);
				trader.setNoAi(true);
				trader.setDespawnDelay(12000);
				level.addFreshEntity(trader);
				trader.setTradingPlayer(player);
				trader.openTradingScreen(player, trader.getDisplayName(), 0);
			});
			context.waitTicks(45);
			String time = context.computeOnClient(client -> TraderTimePanel.shown());
			String screen = context.computeOnClient(client -> client.gui.screen() == null ? "none" : client.gui.screen().getClass().getSimpleName());
			String menu = world.getServer().computeOnServer(server ->
					server.getPlayerList().getPlayers().getFirst().containerMenu.getClass().getSimpleName());
			check(time.startsWith("Leaves in 10:") && screen.equals("MerchantScreen") && menu.equals("MerchantMenu"),
					"the wandering trader's time in its trade screen (" + time + ", " + screen + ", " + menu + ")");
			context.takeScreenshot("cassonade-wandering-trader");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
