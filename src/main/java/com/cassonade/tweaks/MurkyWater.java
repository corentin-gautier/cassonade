package com.cassonade.tweaks;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Water is as murky for monsters as for the player, who sees blurred in it and nothing past about 30 blocks: when the
 * mob or the one it looks at has its head under water, it notices them only within 16 blocks, the way sneaking or
 * a mob's head already shortens it, and loses track of them past 30.
 */
public final class MurkyWater {
	/** How near someone must be to be noticed through the water. */
	private static final double NOTICE = 16;
	/** How far a mob keeps track of its target through the water, where the player's own view ends. */
	private static final double TRACK = 30;

	private MurkyWater() {
	}

	/** Whether water lies between the two: either one's eyes are in it. */
	public static boolean between(Entity looker, Entity seen) {
		return looker.isUnderWater() || seen.isUnderWater();
	}

	/** How visible someone is to the looker, given vanilla's share of its range. */
	public static double visibility(double visibility, LivingEntity seen, Entity looker) {
		if (looker instanceof Mob mob && between(mob, seen)) {
			AttributeInstance range = mob.getAttribute(Attributes.FOLLOW_RANGE);
			if (range != null && range.getValue() > NOTICE) {
				return visibility * NOTICE / range.getValue();
			}
		}
		return visibility;
	}

	/** How far a mob keeps chasing its target. */
	public static double followDistance(Mob mob, double distance) {
		LivingEntity target = mob.getTarget();
		return target != null && between(mob, target) ? Math.min(distance, TRACK) : distance;
	}
}
