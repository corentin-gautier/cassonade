package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.OcelotAttackGoal;
import net.minecraft.world.entity.monster.spider.Spider;

/**
 * A cat reaches the spider it hunts (CatMixin): the goal strikes within twice the cat's width from center to center, made for
 * a rabbit, and a spider's center stays farther than that however close the cat stands. Against a spider, it strikes as other
 * mobs do, once its reach touches the spider's hitbox.
 */
@Mixin(OcelotAttackGoal.class)
public abstract class OcelotAttackGoalMixin {
	@Shadow
	@Final
	private Mob mob;

	@Shadow
	private LivingEntity target;

	@ModifyVariable(method = "tick", at = @At("STORE"), ordinal = 1)
	private double cassonade$reachSpiders(double distance) {
		return this.target instanceof Spider && this.mob.isWithinMeleeAttackRange(this.target) ? 0 : distance;
	}
}
