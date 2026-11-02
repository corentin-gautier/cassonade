package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;

import com.cassonade.client.ambience.AmbientSound;
import com.cassonade.client.ambience.UnderwaterSound;
import com.cassonade.client.mixin.SoundEngineAccessor;
import com.cassonade.client.mixin.SoundManagerAccessor;

/** Distance makes a sound fainter on a curve, duller if asked to, and silent from its range on. */
public class AmbientSoundClientGameTest implements FabricClientGameTest {
	private static final float RANGE = 60;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			world.getConnection().waitForChunksRender();
			check(AmbientSound.closeness(0, RANGE) == 1 && AmbientSound.closeness(RANGE, RANGE) == 0, "from full to silent");
			check(AmbientSound.closeness(RANGE / 2, RANGE) == 0.25F, "already a quarter halfway");

			float near = context.computeOnClient(client -> sound(10, false).play(client).getVolume());
			float far = context.computeOnClient(client -> sound(40, false).play(client).getVolume());
			check(far < near / 2, "fainter from afar: " + near + " then " + far);
			check(context.computeOnClient(client -> sound(100, false).play(client) == null), "silent beyond its range");

			float highs = highsAfterPlaying(context);
			// 1 on a device without EFX
			check(highs == 1 || (highs > 0.25F && highs < 0.5F), "the distance cuts the highs: " + highs);
		}
	}

	private static AmbientSound sound(float distance, boolean muffled) {
		AmbientSound sound = AmbientSound.of(SoundEvents.NOTE_BLOCK_HARP.value().location(), 0.5F).around(distance, 0).fading(RANGE);
		return muffled ? sound.muffled() : sound;
	}

	private static float highsAfterPlaying(ClientGameTestContext context) {
		CompletableFuture<Float> highs = new CompletableFuture<>();
		context.runOnClient(client -> {
			SoundInstance played = sound(40, true).play(client);
			SoundEngineAccessor engine = (SoundEngineAccessor) ((SoundManagerAccessor) client.getSoundManager()).cassonade$getSoundEngine();
			engine.cassonade$getInstanceToChannel().get(played).execute(channel -> highs.complete(UnderwaterSound.filterHighs()));
		});
		return highs.join();
	}
}
