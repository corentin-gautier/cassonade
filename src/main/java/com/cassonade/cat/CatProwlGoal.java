package com.cassonade.cat;

import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.feline.Cat;

/** At night a cat is out and about: it wanders about four times as often as the game's own stroll, a bit faster. */
public class CatProwlGoal extends WaterAvoidingRandomStrollGoal {
	public CatProwlGoal(Cat cat) {
		super(cat, 0.9);
		this.setInterval(30);
	}

	@Override
	public boolean canUse() {
		return this.mob.level().isDarkOutside() && super.canUse();
	}
}
