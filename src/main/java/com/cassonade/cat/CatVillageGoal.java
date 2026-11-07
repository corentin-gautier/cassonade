package com.cassonade.cat;

import net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal;
import net.minecraft.world.entity.animal.feline.Cat;

/**
 * A stray cat that wandered out of a village walks back into it, as villagers do. Only within two sections (32 blocks) of
 * the village, so a cat far from any (a witch's, one from an egg) roams freely; a tamed cat follows its owner instead.
 */
public class CatVillageGoal extends MoveBackToVillageGoal {
	private final Cat cat;

	public CatVillageGoal(Cat cat) {
		super(cat, 0.8, false);
		this.cat = cat;
	}

	@Override
	public boolean canUse() {
		return !this.cat.isTame() && !this.cat.isLeashed() && super.canUse();
	}
}
