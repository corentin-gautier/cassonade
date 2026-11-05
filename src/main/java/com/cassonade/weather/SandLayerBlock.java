package com.cassonade.weather;

import net.minecraft.world.level.block.SnowLayerBlock;

/**
 * A layer of sand, placed and stacked like snow, and like it gone when what holds it is: the game's snow layer without its
 * melting, which only comes with random ticks, left off this block's properties (SandDrifts).
 */
public class SandLayerBlock extends SnowLayerBlock {
	public SandLayerBlock(Properties properties) {
		super(properties);
	}
}
