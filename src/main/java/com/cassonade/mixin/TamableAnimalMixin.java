package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * Dogs, cats and parrots go around fire, as iron golems do (IronGolemMixin): vanilla only makes it costly, and a pet following
 * its owner walks through and burns. Lava is already impassable to them, and they never teleport next to it or to fire.
 */
@Mixin(TamableAnimal.class)
public abstract class TamableAnimalMixin extends Animal {
	protected TamableAnimalMixin(EntityType<? extends Animal> type, Level level) {
		super(type, level);
	}

	/** Read at each path rather than set once, so the setting reaches the mobs already there. */
	@Override
	public float getPathfindingMalus(PathType pathType) {
		return pathType == PathType.FIRE && Settings.on(Setting.PETS_AVOID_FIRE) ? -1.0F : super.getPathfindingMalus(pathType);
	}
}
