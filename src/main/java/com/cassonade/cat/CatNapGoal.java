package com.cassonade.cat;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * By day a cat, stray or tamed, walks to a soft spot close by (a free bed, a hay bale, wool) and sleeps on it, as the game's
 * own lying goal does for tamed cats only and at any hour. It wakes up at dusk, when something to hunt comes by, or when its
 * owner walks away. A nap lasts one to three minutes (the stay of MoveToBlockGoal), then the cat stays up for a while: it
 * sleeps about two thirds of the day.
 */
public class CatNapGoal extends MoveToBlockGoal {
	private final Cat cat;
	/** Game time until which the cat stays up after a nap, here or on the ground (CatDozeGoal). */
	private long awakeUntil;

	public CatNapGoal(Cat cat) {
		super(cat, 1.0, 12, 4);
		this.cat = cat;
		this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
	}

	/** Whether the cat may sleep now: by day, rested from its last nap, free to (not told to sit, on a lead or hunting), its owner close by. */
	boolean maySleep() {
		return this.cat.level().getGameTime() >= this.awakeUntil && maySleep(this.cat);
	}

	/** Keeps the cat up for 30 to 90 seconds once it woke up. */
	void wokeUp() {
		this.awakeUntil = this.cat.level().getGameTime() + 600 + this.cat.getRandom().nextInt(1200);
	}

	private static boolean maySleep(Cat cat) {
		if (!cat.level().isBrightOutside() || cat.isOrderedToSit() || cat.isLeashed() || cat.getTarget() != null || cat.isInWater()) {
			return false;
		}
		// Far from its owner, a tamed cat follows rather than sleeps: the follow goal starts at 10 blocks
		LivingEntity owner = cat.getOwner();
		return owner == null || cat.distanceToSqr(owner) < 8 * 8;
	}

	@Override
	public boolean canUse() {
		return this.maySleep() && super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		// Rested only once it woke up, so not while asleep
		return maySleep(this.cat) && super.canContinueToUse();
	}

	@Override
	protected int nextStartTick(PathfinderMob mob) {
		// Looks again soon, once rested: maySleep keeps it up meanwhile
		return reducedTickDelay(100 + mob.getRandom().nextInt(100));
	}

	@Override
	public void start() {
		super.start();
		this.cat.setInSittingPose(false);
	}

	@Override
	public void stop() {
		super.stop();
		if (this.isReachedTarget()) {
			this.wokeUp();
		}
		this.cat.setLying(false);
	}

	@Override
	public void tick() {
		super.tick();
		this.cat.setInSittingPose(false);
		this.cat.setLying(this.isReachedTarget());
	}

	@Override
	protected boolean isValidTarget(LevelReader level, BlockPos pos) {
		if (!level.isEmptyBlock(pos.above())) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		boolean soft = state.is(BlockTags.BEDS) ? !state.getValue(BedBlock.OCCUPIED) : state.is(Blocks.HAY_BLOCK) || state.is(BlockTags.WOOL);
		if (!soft) {
			return false;
		}
		// One cat per spot
		return this.cat.level().getEntitiesOfClass(Cat.class, new AABB(pos.above()), other -> other != this.cat && other.isLying()).isEmpty();
	}
}
