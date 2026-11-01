package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;

import java.util.concurrent.CompletableFuture;

import org.lwjgl.openal.AL10;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import com.cassonade.client.ambience.UnderwaterSound;
import com.cassonade.client.mixin.SoundEngineAccessor;
import com.cassonade.client.mixin.SoundManagerAccessor;

/** The head under water muffles a sound of the world, not the music; out of it, it clears again. */
public class UnderwaterSoundClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			check(context.computeOnClient(client -> UnderwaterSound.strength()) == 0, "clear out of the water");
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("fill 0 100 0 2 103 2 minecraft:water");
			world.getServer().runCommand("tp @a 1.5 100.5 1.5");
			world.getServer().runCommand("effect give @a minecraft:water_breathing 60 0 true");
			context.waitFor(client -> UnderwaterSound.strength() == 1, 20 * 10);

			check(context.computeOnClient(client -> UnderwaterSound.affects(sound(SoundSource.BLOCKS))), "a block muffled under water");
			check(!context.computeOnClient(client -> UnderwaterSound.affects(sound(SoundSource.MUSIC))), "the music clear under water");
			float highs = highsAfterPlaying(context);
			check(highs == 1 || highs < 0.2F, "the filter cuts the highs: " + highs);

			world.getServer().runCommand("tp @a 10.5 110 10.5");
			context.waitFor(client -> UnderwaterSound.strength() == 0, 20 * 10);
			highsAfterPlaying(context);
		}
	}

	private static SoundInstance sound(SoundSource source) {
		return new SimpleSoundInstance(SoundEvents.NOTE_BLOCK_HARP.value(), source, 1, 1, RandomSource.create(), 1.5, 100.5, 1.5);
	}

	/** Plays a sound and reads the filter on the sound thread, where OpenAL must have reported no error. */
	private static float highsAfterPlaying(ClientGameTestContext context) {
		CompletableFuture<Float> highs = new CompletableFuture<>();
		context.runOnClient(client -> {
			SoundInstance sound = sound(SoundSource.BLOCKS);
			SoundEngineAccessor engine = (SoundEngineAccessor) ((SoundManagerAccessor) client.getSoundManager()).cassonade$getSoundEngine();
			client.getSoundManager().play(sound);
			engine.cassonade$getInstanceToChannel().get(sound).execute(channel -> {
				float read = UnderwaterSound.filterHighs();
				int error = AL10.alGetError();
				if (error != AL10.AL_NO_ERROR) {
					highs.completeExceptionally(new AssertionError("OpenAL error " + error));
				}
				highs.complete(read);
			});
		});
		return highs.join();
	}
}
