package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Dogs, cats and parrots go around fire, as iron golems do (IronGolemMixin): vanilla only makes it costly, and a pet following
 * its owner walks through and burns. Lava is already impassable to them, and they never teleport next to it or to fire.
 */
@Mixin(TamableAnimal.class)
public abstract class TamableAnimalMixin extends Animal {
	protected TamableAnimalMixin(EntityType<? extends Animal> type, Level level) {
		super(type, level);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void cassonade$avoidFire(EntityType<? extends TamableAnimal> type, Level level, CallbackInfo ci) {
		this.setPathfindingMalus(PathType.FIRE, -1.0F);
	}
}
