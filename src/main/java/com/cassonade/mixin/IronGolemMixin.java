package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

/** Iron golems go around fire: vanilla only makes it costly, and a golem burns to death on its rounds. */
@Mixin(IronGolem.class)
public abstract class IronGolemMixin extends AbstractGolem {
	protected IronGolemMixin(EntityType<? extends AbstractGolem> type, Level level) {
		super(type, level);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void cassonade$avoidFire(EntityType<? extends IronGolem> type, Level level, CallbackInfo ci) {
		this.setPathfindingMalus(PathType.FIRE, -1.0F);
	}
}
