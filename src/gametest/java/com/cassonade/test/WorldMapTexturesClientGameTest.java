package com.cassonade.test;

import java.lang.reflect.Field;
import java.util.Map;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import com.cassonade.Cassonade;
import com.cassonade.client.map.WorldMap;
import com.cassonade.client.map.WorldMapScreen;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;

/** Leaving a world frees the world map's textures, though the game leaves it from the network thread. */
public class WorldMapTexturesClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getConnection().waitForChunksRender();
			context.waitTicks(40);
			context.setScreen(WorldMapScreen::new);
			context.waitForScreen(WorldMapScreen.class);
			context.waitTicks(1);
			context.runOnClient(client -> {
				for (int i = 0; i < 3; i++) {
					client.gui.screen().mouseScrolled(client.getWindow().getGuiScaledWidth() / 2.0, client.getWindow().getGuiScaledHeight() / 2.0, 0, -1);
				}
			});
			context.waitFor(client -> WorldMap.pendingOverviews(client.level.dimension()) == 0, 400);
			context.waitTicks(5);
			check(context.computeOnClient(WorldMapTexturesClientGameTest::mapTextures) > 0, "the world map draws from textures of its own");
			context.setScreen(() -> null);
		}
		context.waitTicks(5);
		long left = context.computeOnClient(WorldMapTexturesClientGameTest::mapTextures);
		check(left == 0, "leaving the world frees the world map's textures, " + left + " left");
	}

	@SuppressWarnings("unchecked")
	private static long mapTextures(Minecraft client) {
		try {
			Field field = TextureManager.class.getDeclaredField("byPath");
			field.setAccessible(true);
			return ((Map<Identifier, ?>) field.get(client.getTextureManager())).keySet().stream()
					.filter(id -> id.getNamespace().equals(Cassonade.MOD_ID) && id.getPath().startsWith("worldmap/")).count();
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
