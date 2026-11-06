package com.cassonade.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

import com.cassonade.Cassonade;

/**
 * Walking faster on dirt paths, as on a road: a player, or the mount a player rides, gets 15% more speed while on one.
 * A movement speed modifier, as Soul Speed gives on soul sand, rather than the block's speed factor: that one applies to
 * the speed every tick, so friction compounds it and it cannot be tuned. Added by the server alone, and sent to the
 * client with the other attributes: without the mod on the server, nothing changes.
 */
public final class PathSpeed {
	private static final AttributeModifier BONUS = new AttributeModifier(Cassonade.id("path_speed"), 0.15,
			AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

	private PathSpeed() {
	}

	/** Each tick of a living entity on the server. */
	public static void tick(LivingEntity entity) {
		AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		// The block under its middle, not the one holding it up: a horse on a path one block wide rests on the grass
		// beside it, a whole block high where the path is 15/16. Not only on the ground either: a jump along the path
		// keeps it, rather than sending the attribute twice a jump.
		BlockPos below = BlockPos.containing(entity.getX(), entity.getY() - 0.5, entity.getZ());
		boolean onPath = led(entity) && entity.level().getBlockState(below).is(Blocks.DIRT_PATH);
		if (onPath && !speed.hasModifier(BONUS.id())) {
			speed.addTransientModifier(BONUS);
		} else if (!onPath && speed.hasModifier(BONUS.id())) {
			speed.removeModifier(BONUS.id());
		}
	}

	/** A player on foot, or a mount a player steers: mobs keep their pace, so a path changes no fight. */
	private static boolean led(LivingEntity entity) {
		return entity instanceof Player player ? !player.isPassenger() : entity.getControllingPassenger() instanceof Player;
	}
}
