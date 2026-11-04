package com.cassonade.mushroom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Two blocks high, a pale ringed stalk under a violet puff that now and then lets out a breath of spores. */
public class PuffstalkBlock extends DoublePlantBlock {
	/** One tick in this many, a puff breathes out. */
	private static final int PUFF_CHANCE = 40;

	public PuffstalkBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return MyceliumPlantBlock.grows(state);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(HALF) != DoubleBlockHalf.UPPER || random.nextInt(PUFF_CHANCE) != 0) {
			return;
		}
		for (int i = 0; i < 6; i++) {
			level.addParticle(ParticleTypes.MYCELIUM, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.4 + random.nextDouble() * 0.4,
					pos.getZ() + 0.2 + random.nextDouble() * 0.6, (random.nextDouble() - 0.5) * 0.02, 0.02, (random.nextDouble() - 0.5) * 0.02);
		}
	}
}
