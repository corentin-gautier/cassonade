package com.cassonade.test;

import java.lang.reflect.Field;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.renderer.ShaderManager;

import com.cassonade.Cassonade;
import com.cassonade.test.mixin.PostChainCacheAccessor;

/**
 * The blur under water is compiled before the eyes ever go under (ShaderManagerMixin): compiled at the first dive, it could
 * freeze the game while many sections were being built, in a flooded cave reached flying fast.
 */
public class UnderwaterBlurReadyClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			boolean ready = context.computeOnClient(client -> {
				// Its cache's class is private, out of an accessor's reach: the field is read by reflection
				Object cache;
				try {
					Field field = ShaderManager.class.getDeclaredField("postChains");
					field.setAccessible(true);
					cache = field.get(client.getShaderManager());
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException(e);
				}
				var chain = ((PostChainCacheAccessor) cache).cassonade$getPostChains().get(Cassonade.id("underwater"));
				return chain != null && chain.isPresent();
			});
			if (!ready) {
				throw new AssertionError("the blur under water compiled as the shaders load");
			}
		}
	}
}
