package com.cassonade.cat;

import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.feline.Cat;

/**
 * By day, a cat with no soft spot close by (CatNapGoal) lies down where it stands, for one to three minutes, and then stays up
 * for a while as after a nap.
 */
public class CatDozeGoal extends Goal {
	private final Cat cat;
	private final CatNapGoal nap;
	private int ticks;

	public CatDozeGoal(Cat cat, CatNapGoal nap) {
		this.cat = cat;
		this.nap = nap;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return this.cat.onGround() && this.cat.getRandom().nextInt(reducedTickDelay(100)) == 0 && this.nap.maySleep();
	}

	@Override
	public boolean canContinueToUse() {
		return this.ticks > 0 && this.cat.onGround() && this.nap.maySleep();
	}

	@Override
	public void start() {
		this.ticks = this.adjustedTickDelay(1200 + this.cat.getRandom().nextInt(2400));
		this.cat.getNavigation().stop();
		this.cat.setInSittingPose(false);
		this.cat.setLying(true);
	}

	@Override
	public void tick() {
		this.ticks--;
	}

	@Override
	public void stop() {
		this.nap.wokeUp();
		this.cat.setLying(false);
	}
}
