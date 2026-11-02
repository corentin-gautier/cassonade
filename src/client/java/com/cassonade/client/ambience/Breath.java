package com.cassonade.client.ambience;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** In the cold, outdoors, players and villagers near the player breathe out a small puff now and then. */
public final class Breath {
	private static final double REACH = 24;
	/** Between two breaths, each creature at its own moment. */
	private static final int PERIOD = 70;
	private static final double AHEAD = 0.45;
	private static final double SELF_AHEAD = 0.75;
	private static final double BELOW = 0.28;
	private static final int PUFFS = 2;
	private static final RandomSource RANDOM = RandomSource.create();

	private Breath() {
	}

	static void tick(Minecraft minecraft, ClientLevel level) {
		if (!Ambience.overworld(level)) {
			return;
		}
		for (Entity entity : level.entitiesForRendering()) {
			if ((entity instanceof Player || entity instanceof AbstractVillager) && !entity.isSpectator() && entity.distanceToSqr(minecraft.player) < REACH * REACH
					&& (level.getGameTime() + entity.getId() * 13L) % PERIOD == 0 && breathesMist(level, entity)) {
				puff(minecraft, level, entity);
			}
		}
	}

	/** For the development command: everyone around at once, anywhere. */
	static void puffAround(Minecraft minecraft) {
		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if ((entity instanceof Player || entity instanceof AbstractVillager) && entity.distanceToSqr(minecraft.player) < REACH * REACH) {
				puff(minecraft, minecraft.level, entity);
			}
		}
	}

	/** Cold enough to snow, out in the open air, head above water. */
	public static boolean breathesMist(ClientLevel level, Entity entity) {
		BlockPos pos = BlockPos.containing(entity.getEyePosition());
		return level.getBiome(pos).value().coldEnoughToSnow(pos, level.getSeaLevel()) && level.canSeeSky(pos) && !entity.isUnderWater();
	}

	private static void puff(Minecraft minecraft, ClientLevel level, Entity entity) {
		// Farther off the camera for the player's own breath, which would otherwise fill the view
		double ahead = entity == minecraft.player ? SELF_AHEAD : AHEAD;
		Vec3 at = entity.getEyePosition().add(entity.getLookAngle().scale(ahead)).add(0, -BELOW, 0);
		Vec3 look = entity.getLookAngle();
		for (int i = 0; i < PUFFS; i++) {
			level.addParticle(ParticleTypes.WHITE_SMOKE, at.x, at.y, at.z, look.x * 0.012 + Mth.randomBetween(RANDOM, -0.003F, 0.003F), 0.004, look.z * 0.012 + Mth.randomBetween(RANDOM, -0.003F, 0.003F));
		}
	}
}
