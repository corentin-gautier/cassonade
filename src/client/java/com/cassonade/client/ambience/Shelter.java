package com.cassonade.client.ambience;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Indoors, the rain drums on the roof and the thunder rolls outside: their highs fade as a roof closes over the player, on the
 * same filter as the water's (UnderwaterSound). Leaves are no roof, the rain falls through them.
 */
public final class Shelter {
	/** Ticks between two looks at the roof, and the ease from out in the rain to sheltered. */
	private static final int LOOK_EVERY = 10;
	private static final float EASE = 0.1F;
	/** The columns looked at, this far apart around the player: under a lone overhang, the rain falling beside stays clear. */
	private static final int SPREAD = 3;
	private static final float ROOFED_HIGHS = 0.15F;
	private static volatile float roofed;
	private static float target;
	private static int nextLook;

	private Shelter() {
	}

	/** On the render thread, from the sound engine's tick. Tells whether the sources playing must follow. */
	static boolean tick(Minecraft minecraft) {
		if (--nextLook <= 0) {
			nextLook = LOOK_EVERY;
			target = minecraft.level == null ? 0 : roofedAround(minecraft.level, BlockPos.containing(minecraft.gameRenderer.mainCamera().position()));
		}
		float next = Mth.approach(roofed, target, EASE);
		boolean changed = next != roofed;
		roofed = next;
		return changed;
	}

	/** The share of the columns around with a block above the head. */
	private static float roofedAround(ClientLevel level, BlockPos eye) {
		int under = 0;
		int all = 0;
		for (int dx = -SPREAD; dx <= SPREAD; dx += SPREAD) {
			for (int dz = -SPREAD; dz <= SPREAD; dz += SPREAD) {
				all++;
				if (level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, eye.getX() + dx, eye.getZ() + dz) > eye.getY() + 1) {
					under++;
				}
			}
		}
		return (float) under / all;
	}

	public static float roofed() {
		return roofed;
	}

	/** How much of a sound's highs the roof leaves: only the weather's, the rest is inside with the player. */
	static float highs(SoundInstance instance) {
		String path = instance.getIdentifier().getPath();
		boolean weather = path.startsWith("weather.rain") || path.equals("entity.lightning_bolt.thunder");
		return weather ? Mth.lerp(roofed, 1, ROOFED_HIGHS) : 1;
	}
}
