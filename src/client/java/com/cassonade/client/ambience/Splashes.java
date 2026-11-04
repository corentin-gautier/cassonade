package com.cassonade.client.ambience;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Where falling water lands, on the ground or in a pool, it splashes: drops and white spray thrown up from the foot of the
 * fall, more of them the higher it falls, and a little mist at the foot of a tall one. The area around the player is looked
 * through a row of columns a tick, so a fall that appears or dries up is seen within a few seconds.
 */
public final class Splashes {
	private static final int REACH = 24;
	/** Above and below the player's eyes: a waterfall in a cave below or on a cliff above is seen as well. */
	private static final int DEPTH = 16;
	private static final int ROWS = REACH * 2 + 1;
	/** Water running down a single step, as it does along every river bank, is not a fall. */
	private static final int MIN_DROP = 2;
	/** Past it, a higher fall splashes no harder. */
	private static final int FULL_DROP = 10;
	private static final int MIST_DROP = 6;
	/** How high up the column the pool around its foot can rise. */
	private static final int MAX_POOL = 8;
	private static final int MAX_LANDINGS = 96;
	/** Smaller than the game's, which are made for a fish or a boat. */
	private static final float SPLASH_SCALE = 0.5F;
	private static final float SPRAY_SCALE = 0.4F;
	/** A tick, at the foot of a full fall. */
	private static final int DROPS = 2;
	private static final int SPRAY = 3;
	private static final int SPRAY_LIFE = 12;
	private static final RandomSource RANDOM = RandomSource.create();
	private static final List<Landing> LANDINGS = new ArrayList<>();

	/** The foot of a fall: the centre of its column, the height of the surface it lands on, and how far it falls. */
	public record Landing(int x, float y, int z, int drop) {
	}

	private static int row;

	private Splashes() {
	}

	static void tick(Minecraft minecraft, ClientLevel level) {
		ParticleStatus particles = minecraft.options.particles().get();
		if (!Ambience.overworld(level) || particles == ParticleStatus.MINIMAL) {
			LANDINGS.clear();
			return;
		}
		BlockPos eyes = BlockPos.containing(minecraft.player.getEyePosition());
		LANDINGS.removeIf(landing -> Math.abs(landing.x - eyes.getX()) > REACH || Math.abs(landing.z - eyes.getZ()) > REACH);
		row = (row + 1) % ROWS;
		look(level, eyes.getX() - REACH + row, eyes);
		boolean reduced = particles == ParticleStatus.DECREASED;
		for (Landing landing : LANDINGS) {
			splash(minecraft, landing, reduced);
		}
	}

	/** Looks through the columns of one row, from the player's side to the other, replacing what was found there before. */
	private static void look(ClientLevel level, int x, BlockPos eyes) {
		LANDINGS.removeIf(landing -> landing.x == x);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int z = eyes.getZ() - REACH; z <= eyes.getZ() + REACH && LANDINGS.size() < MAX_LANDINGS; z++) {
			// From the bottom up, each fall is met by its foot first
			for (int y = eyes.getY() - DEPTH; y <= eyes.getY() + DEPTH; y++) {
				if (falling(level.getFluidState(pos.set(x, y, z))) && !falling(level.getFluidState(pos.set(x, y - 1, z)))) {
					Landing landing = landing(level, x, y, z);
					if (landing != null) {
						LANDINGS.add(landing);
					}
				}
			}
		}
	}

	/** The foot of the fall whose lowest falling block is at the given height, if it falls far enough. */
	private static @Nullable Landing landing(ClientLevel level, int x, int bottom, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, bottom - 1, z);
		FluidState below = level.getFluidState(pos);
		// Still falling through the air: it has landed nowhere yet
		if (!below.is(FluidTags.WATER) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
			return null;
		}
		// On still water, its surface; on the ground, the ground
		float surface = below.is(FluidTags.WATER) ? bottom - 1 + below.getHeight(level, pos) : bottom;
		// The water spreading around the foot, or a pool it fills, rises up the column: it lands on top of it
		for (int y = bottom; y < bottom + MAX_POOL; y++) {
			float around = pool(level, pos.set(x, y, z));
			if (around < 0) {
				break;
			}
			surface = Math.max(surface, y + around);
		}
		int top = bottom;
		while (top < bottom + DEPTH * 2 && falling(level.getFluidState(pos.set(x, top + 1, z)))) {
			top++;
		}
		int drop = Mth.floor(top + 1 - surface);
		return drop >= MIN_DROP ? new Landing(x, surface, z, drop) : null;
	}

	/** How high the water stands beside the column at this height, or -1 if none does. */
	private static float pool(ClientLevel level, BlockPos.MutableBlockPos pos) {
		float highest = -1;
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos beside = pos.relative(side);
			FluidState water = level.getFluidState(beside);
			if (water.is(FluidTags.WATER) && !falling(water)) {
				highest = Math.max(highest, water.getHeight(level, beside));
			}
		}
		return highest;
	}

	private static boolean falling(FluidState state) {
		return state.is(FluidTags.WATER) && state.getValue(FlowingFluid.FALLING);
	}

	private static void splash(Minecraft minecraft, Landing landing, boolean reduced) {
		float strength = Mth.clamp((float) landing.drop / FULL_DROP, 0.25F, 1);
		if (reduced) {
			strength /= 2;
		}
		double x = landing.x + 0.5;
		double z = landing.z + 0.5;
		for (int i = 0; i < DROPS; i++) {
			if (RANDOM.nextFloat() >= strength) {
				continue;
			}
			// Any upward speed but zero: the particle then picks its own leap, higher than the fixed hop it makes otherwise
			Particle drop = minecraft.particleEngine.createParticle(ParticleTypes.SPLASH, x + spread(), landing.y, z + spread(), 0, 0.1, 0);
			if (drop != null) {
				drop.scale(SPLASH_SCALE);
			}
		}
		// The game's splash is blue through and through and gets lost on the water: white chips make it read as spray
		for (int i = 0; i < SPRAY; i++) {
			if (RANDOM.nextFloat() >= strength) {
				continue;
			}
			Particle spray = minecraft.particleEngine.createParticle(ParticleTypes.ITEM_SNOWBALL, x + spread(), landing.y, z + spread(), 0, 0, 0);
			if (spray != null) {
				float angle = RANDOM.nextFloat() * Mth.TWO_PI;
				float out = Mth.randomBetween(RANDOM, 0.02F, 0.07F);
				spray.setParticleSpeed(Mth.cos(angle) * out, Mth.randomBetween(RANDOM, 0.12F, 0.22F + 0.1F * strength), Mth.sin(angle) * out);
				spray.setLifetime(SPRAY_LIFE);
				spray.scale(SPRAY_SCALE);
			}
		}
		if (landing.drop >= MIST_DROP && RANDOM.nextFloat() < strength * 0.15F) {
			Particle mist = minecraft.particleEngine.createParticle(ParticleTypes.WHITE_SMOKE, x + spread() * 2, landing.y + 0.2, z + spread() * 2,
					Mth.randomBetween(RANDOM, -0.01F, 0.01F), 0.02, Mth.randomBetween(RANDOM, -0.01F, 0.01F));
			if (mist != null) {
				mist.scale(1.5F);
			}
		}
	}

	private static double spread() {
		return (RANDOM.nextDouble() - 0.5) * 0.8;
	}

	public static List<Landing> landings() {
		return List.copyOf(LANDINGS);
	}
}
