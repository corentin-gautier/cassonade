package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;

/**
 * Spiders are the cats' prey (CatMixin). A spider runs from a cat that comes close, as it runs from an armadillo, at its walking
 * pace: a cat closing in for the kill is faster, so the hunt becomes a chase the cat wins. Hurt by a cat, it doesn't turn on it:
 * with its 16 health against the cat's 10, it would win, and a pet would die defending the house. It keeps answering anyone else.
 */
@Mixin(Spider.class)
public abstract class SpiderMixin extends Monster {
	/** Close enough for the cat to break into a run: farther, it creeps up unseen. */
	@Unique
	private static final float CASSONADE_FLEE = 4;

	protected SpiderMixin(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void cassonade$fleeCats(CallbackInfo ci) {
		this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Cat.class, CASSONADE_FLEE, 1.0, 1.0));
	}

	@ModifyArg(method = "registerGoals", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/target/HurtByTargetGoal;<init>(Lnet/minecraft/world/entity/PathfinderMob;[Ljava/lang/Class;)V"), index = 1)
	private Class<?>[] cassonade$ignoreCats(Class<?>[] ignored) {
		return new Class<?>[] { Cat.class };
	}
}
