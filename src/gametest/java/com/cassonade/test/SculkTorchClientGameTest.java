package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.data.AtlasIds;

import com.cassonade.Cassonade;
import com.cassonade.client.ambience.Birds;

/** The sculk torch's flame glows brighter and dimmer, slowly, as the sculk block's specks do: two shots of it in hand, two seconds apart. */
public class SculkTorchClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		boolean animated = context.computeOnClient(client -> client.getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).getSprite(Cassonade.id("item/sculk_torch")).contents().isAnimated());
		if (!animated) {
			throw new AssertionError("Expected the sculk torch's texture to be animated");
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("time set 18000");
			world.getServer().runCommand("item replace entity @p weapon.mainhand with cassonade:sculk_torch");
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
			Screenshots.take(context, "cassonade-sculk-torch-1");
			context.waitTicks(40);
			Screenshots.take(context, "cassonade-sculk-torch-2");
		}
	}
}
