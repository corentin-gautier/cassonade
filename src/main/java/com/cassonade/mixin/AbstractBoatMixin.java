package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.Level;

/**
 * Boats on land climb the small steps left by blocks a little lower than a full one: from a dirt path
 * or farmland onto grass, from soul sand or mud onto stone, onto a carpet or a thin snow layer.
 * Vanilla boats cannot climb at all, so a single pixel stops them.
 */
@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin extends Entity {
	/** Two pixels: the lowest of those blocks, soul sand and mud, are 14 pixels high. */
	@Unique
	private static final float STEP_HEIGHT = 2 / 16f;

	private AbstractBoatMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Override
	public float maxUpStep() {
		return STEP_HEIGHT;
	}
}
