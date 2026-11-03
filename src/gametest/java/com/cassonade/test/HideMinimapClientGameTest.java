package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.cassonade.client.CassonadeClient;
import com.cassonade.client.ClientSettings;
import com.cassonade.client.map.Minimap;
import com.cassonade.client.quest.QuestTracker;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;

/** Its key puts the minimap away, with the biome and coordinates, the current quest moving up to the corner, and brings it back. */
public class HideMinimapClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(state -> {
			state.setSeed("vanillasugar");
			state.setWorldType(state.getNormalPresetList().getFirst());
			state.setAllowCommands(true);
		}).create()) {
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.COMPASS, new ItemStack(Items.COMPASS)));
			});
			context.waitTicks(40);
			check(context.computeOnClient(Minimap::isShown), "a compass worn shows the minimap");

			context.getInput().pressKey(CassonadeClient.HIDE_MINIMAP);
			context.waitTicks(2);
			check(context.computeOnClient(client -> !Minimap.isShown(client) && ClientSettings.minimapHidden()), "the key hides the minimap");
			check(context.computeOnClient(client -> QuestTracker.bottom(client) < Minimap.height(client)), "what was under the minimap moves up");
			Screenshots.take(context, "cassonade-minimap-hidden");

			context.getInput().pressKey(CassonadeClient.HIDE_MINIMAP);
			context.waitTicks(2);
			check(context.computeOnClient(client -> Minimap.isShown(client) && !ClientSettings.minimapHidden()), "the key shows the minimap again");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
