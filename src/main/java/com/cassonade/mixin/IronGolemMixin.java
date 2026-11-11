package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/** Iron golems go around fire: vanilla only makes it costly, and a golem burns to death on its rounds. */
@Mixin(IronGolem.class)
public abstract class IronGolemMixin extends AbstractGolem {
	protected IronGolemMixin(EntityType<? extends AbstractGolem> type, Level level) {
		super(type, level);
	}

	/** Read at each path rather than set once, so the setting reaches the mobs already there. */
	@Override
	public float getPathfindingMalus(PathType pathType) {
		return pathType == PathType.FIRE && Settings.on(Setting.GOLEMS_AVOID_FIRE) ? -1.0F : super.getPathfindingMalus(pathType);
	}
}
