package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces;

@Mixin(MineshaftPieces.MineShaftCorridor.class)
public interface MineShaftCorridorAccessor {
	@Accessor("hasRails")
	boolean cassonade$hasRails();

	@Accessor("spiderCorridor")
	boolean cassonade$spiderCorridor();

	@Accessor("numSections")
	int cassonade$numSections();
}
