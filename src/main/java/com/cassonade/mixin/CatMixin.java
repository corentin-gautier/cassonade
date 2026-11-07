package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.CatLieOnBlockGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;

import com.cassonade.cat.CatDozeGoal;
import com.cassonade.cat.CatNapGoal;
import com.cassonade.cat.CatProwlGoal;
import com.cassonade.cat.CatVillageGoal;

/**
 * Cats, stray or tamed, hunt the spiders (cave spiders too) they see, as they hunt rabbits: the game's own pounce and attack
 * goals run the chase. A sitting cat stays put, its sitting goal coming first. Spiders don't fight back (SpiderMixin).
 */
@Mixin(Cat.class)
public abstract class CatMixin extends TamableAnimal {
	protected CatMixin(EntityType<? extends TamableAnimal> type, Level level) {
		super(type, level);
	}

	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void cassonade$huntSpiders(CallbackInfo ci) {
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Spider.class, true));
	}

	/**
	 * Cats sleep by day and go out at night. The game's own lying goal (tamed cats on beds, at any hour) gives way to the nap,
	 * which takes stray cats too; sleeping on a sleeping owner (priority 3) stays as it is.
	 */
	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void cassonade$sleepByDay(CallbackInfo ci) {
		Cat cat = (Cat) (Object) this;
		this.goalSelector.removeAllGoals(goal -> goal instanceof CatLieOnBlockGoal);
		CatNapGoal nap = new CatNapGoal(cat);
		this.goalSelector.addGoal(5, nap);
		this.goalSelector.addGoal(7, new CatDozeGoal(cat, nap));
		this.goalSelector.addGoal(10, new CatVillageGoal(cat));
		this.goalSelector.addGoal(11, new CatProwlGoal(cat));
	}
}
