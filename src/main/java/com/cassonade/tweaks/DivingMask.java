package com.cassonade.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

import com.cassonade.item.CassonadeItems;

/**
 * The diving mask, worn in place of a helmet and enchanted like one: under water, its glass keeps the view sharp and
 * twice as long (UnderwaterSight), and its snorkel lets a player whose head is just under the surface breathe, as long
 * as the open air is right above it.
 */
public final class DivingMask {
	/** How far above the eyes the snorkel's end reaches. */
	private static final double SNORKEL = 0.5;

	private DivingMask() {
	}

	public static boolean worn(Entity entity) {
		return entity instanceof LivingEntity living && living.getItemBySlot(EquipmentSlot.HEAD).is(CassonadeItems.DIVING_MASK);
	}

	/** Whether the player breathes through the snorkel: its end is out of the water, in the open air. */
	public static boolean breathes(LivingEntity entity) {
		if (!(entity instanceof Player) || !worn(entity)) {
			return false;
		}
		Level level = entity.level();
		double top = entity.getEyeY() + SNORKEL;
		BlockPos pos = BlockPos.containing(entity.getX(), top, entity.getZ());
		FluidState water = level.getFluidState(pos);
		if (water.is(FluidTags.WATER) && top < pos.getY() + water.getHeight(level, pos)) {
			return false;
		}
		// Not up against a ceiling or a boat's hull
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}
}
